package org.rexcrawler;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Crawler
 * 
 * This object act as a Runnable implementation for the 
 * Java forkjoin framework. Among the many (not implemented) features
 * includes multithreading which involve synchronization and 
 * shared objects (@Reduced).
 * 
 * <div><em>
 * This program contains material that may be disturbing to some viewers.<br/>
 * Viewer discretion is advised.
 * </em></div>
 * 
 * @author shake0
 *
 */
public class Crawler extends RecursiveAction {
	private static final long serialVersionUID = 1L;

	//--------------------------------------------
	// Constructors
	
	public Crawler(){
		this.urls             = new LinkedList<String>();
		this.master           = null;
		this.lock             = new AtomicInteger(0);
		this.linkFollowed     = new AtomicInteger(0);
		this.chunkSize        = NO_FORK;
		this.searchLength     = null;
		this.links = new LinkedList<String>();
	}
	
	/*
	 * Use to make child processes
	 */
	private Crawler(Crawler p) throws CloneNotSupportedException{
		this.urls             = null;
		this.master           = (p.master == null)?p:p.master;
		this.lock             = p.lock;
		this.linkFollowed     = p.linkFollowed;
		this.handler          = (CrawlerHandler) p.handler.clone();
		this.chunkSize        = p.chunkSize;
		this.searchLength     = p.searchLength;
		this.links = new LinkedList<String>();
		
		this.lock.incrementAndGet();
	}
	
	//--------------------------------------------
	// Forking
	
	/**
	 * Start crawling the targets using all available resources. Usually all the cores 
	 * available for the JVM.
	 * 
	 * @param targets list of the URL to parse
	 */
	public void run(URL ... targets){
		run(0, targets);
	}
	
	/**
	 * Start crawling the <code>targets</code> using <code>parallel</code> threads.
	 * The optimal solution will be to set <code>parallel</code> to the number
	 * of cores available. However this method allows to scale down the resources
	 * reserved for the parsing.
	 * 
	 * @param parallel number of thread to use
	 * @param targets list of the URL to parse
	 */
	public void run(int parallel, URL ... targets){
		// test for handler
		if(this.handler == null)
			throw new IllegalArgumentException("CrawlerHandler undefined");
		// load targets
		this.reinitialize();
		for(URL u : targets) this.urls.add(u.toString());
		// execute
		if(parallel > 0)
			new ForkJoinPool(parallel).invoke(this);
		else
			new ForkJoinPool().invoke(this);
	}
	
	@Override
	protected void compute() {
		
		while(! isFollowingLinkLimitExceeded()){
			try {
				// FORK
				List<String> delegatedSet = null;
				synchronized(linkFollowed){
					delegatedSet = splitWorkLoad();
					if(! isUnboundSearch()){
						final int remainingTasks = searchLength - linkFollowed.get();
						if(remainingTasks <= 0) break;
						if(this.urls.size() > remainingTasks) 
							this.urls = this.urls.subList(0, remainingTasks);
					}
					linkFollowed.addAndGet(this.urls.size());
				}
				if(delegatedSet != null && isForkingEnabled() && ! handler.abort.get()){
					Crawler child = new Crawler(this);
					child.urls = delegatedSet;
					child.fork();
				}

				// COMPUTE
				parse();
				reduce();
				
				// UPDATE
				this.urls  = this.links;
				this.links = new LinkedList<>();
				if(urls.isEmpty() || isUnboundSearch()) break;
			} catch (IllegalAccessException ex) {
				System.err.println(ex.getLocalizedMessage());
			} catch (IllegalArgumentException ex) {
				System.err.println(ex.getLocalizedMessage());
			} catch (InvocationTargetException ex) {
				System.err.println(ex.getLocalizedMessage());
			} catch (CloneNotSupportedException ex) {
				System.err.println(ex.getLocalizedMessage());
			}
		}
		
		// WAIT TERMINATION
		if(isMaster()){
			synchronized(lock){
				if(lock.get() != 0){
					try { lock.wait(); }
					catch (InterruptedException e) { e.printStackTrace(); }
				}
			}
		}
		else{
			synchronized(lock){
				if(lock.decrementAndGet() == 0)
					lock.notify();
			}
		}
	}
	
	private void reduce() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(! isMaster())
			synchronized (this.master.handler) {
				this.master.handler.reduce(this.handler);
			}
	}
	
	private List<String> splitWorkLoad(){
		if(isForkingEnabled()){
			if(this.urls.size() > chunkSize){
				List<String> delegatedSet = new ArrayList<>(this.urls.subList(chunkSize, this.urls.size()));
				this.urls = this.urls.subList(0, chunkSize);
				return delegatedSet;
			}
		}
		return null;
	}
	
	private void abort() {
		// TODO: Test
		this.handler.abort.set(true);
		this.urls.clear();
		this.links.clear();
	}
	
	@Override
	public void reinitialize() {
		this.handler.abort.set(false);
		this.lock             = new AtomicInteger(0);
		this.linkFollowed     = new AtomicInteger(0);
		this.urls             = new LinkedList<String>();
		this.links            = new LinkedList<>();
		super.reinitialize();
	}
	
	private boolean isMaster(){
		return this.master == null;
	}
	
	private boolean isForkingEnabled(){
		return this.chunkSize != NO_FORK;
	}
	
	private boolean isFollowingLinkLimitExceeded(){
		return linkFollowed.get() > ((isUnboundSearch())?this.urls.size():searchLength);
	}
	
	private boolean isUnboundSearch(){
		return this.searchLength == null;
	}
	
	//--------------------------------------------
	// Parsing
	
	private void parse(){
		List<String> newUrlSet = this.handler.parse(this.urls);
		if(newUrlSet == null)
			abort();
		else
			this.links.addAll(newUrlSet);
	}
	
	//--------------------------------------------
	// Mutators
	
	/**
	 * Get the total number of link followed in the last search.
	 * This number is reset at each new run().
	 * 
	 * @return number of links followed
	 */
	public int getFollowedLinkCount(){
		return linkFollowed.get();
	}
	
	/**
	 * Get the maximum number of links assigned to a single
	 * thread. 
	 * @return maximum links per thread
	 */
	public int getChunkSize(){
		return this.chunkSize;
	}
	
	/**
	 * Get the upperbound limit of the crawler.
	 * The search stops if the total number of followed links is 
	 * equal to this.
	 * @return limit followed links.
	 */
	public Integer getSearchLength(){
		return this.searchLength;
	}
	
	/**
	 * Get the current handler used by this crawler.
	 * 
	 * @return handler in use
	 */
	public CrawlerHandler getHandler(){
		return this.handler;
	}
	
	/**
	 * Set the number of URL per thread. By default this is set to <code>NO_FORK</code>
	 * which allows the sequential parsing of each URL. Setting the chunkSize > 0
	 * <strong>will enable multithreading</strong>.
	 * For <code>chunkSize > 0</code>, each thread exceeding <code>chunkSize</code>
	 * will fork, delegate the surplus to its child, and execute.
	 * 
	 * @param chunkSize minimum number of links to fork
	 * @return the calling object
	 */
	public Crawler setChunkSize(int chunkSize) {
		if(chunkSize > 0)
			this.chunkSize = chunkSize;
		return this;
	}

	/**
	 * Set the maximum number of links to follow before giving up.
	 * If the total number of links followed reach this limit, the search
	 * will terminate. By default only the submitted URL will be parsed.
	 * 
	 * @param searchLength maximum number of followed links
	 * @return the calling object
	 */
	public Crawler setSearchLength(int searchLength) {
		this.searchLength = (searchLength <= 0)?null:new Integer(searchLength);
		return this;
	}
	
	/**
	 * Set the handle for the parsing
	 * @param handler
	 * @return the calling object
	 */
	public Crawler setHandler(CrawlerHandler handler){
		this.handler = handler;
		return this;
	}
	
	//--------------------------------------------
	// Constraints
	
	// constraints
	private static final int NO_FORK        = -1;
	private int     chunkSize;
	private Integer searchLength;
	// states
	private AtomicInteger      lock;
	private AtomicInteger      linkFollowed;
	private Crawler            master;
	private CrawlerHandler     handler;
	// tasks
	private List<String>       urls;
	private List<String>       links;
}

