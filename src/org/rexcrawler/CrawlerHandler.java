package org.rexcrawler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The handler is used by the crawler to operate on the
 * retrieved document.
 * Be aware this class implements the prototype pattern
 * needed for the forking. @see clone()
 * 
 * <em>
 * All Collection not annotated with @Reduced will not
 * be merge at the end of the search.
 * </em>
 * 
 * @author shake0
 *
 */
public abstract class CrawlerHandler implements Cloneable {

	//--------------------------------------------
	// Constructors
	
	public CrawlerHandler() {
		try {
			abort            = new AtomicBoolean(false);
			reduceCollection = Collection.class.getDeclaredMethod("addAll", new Class<?>[]{Collection.class});
			findReducedFields();
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Prototype constructor
	 * 
	 * This method should be redefined if 
	 * additional field are added for reducing.
	 * 
	 * @see #reduce(CrawlerHandler)
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		CrawlerHandler clone = (CrawlerHandler)super.clone();
		for(Field f: this.reducedFields)
			if(Collection.class.isAssignableFrom(f.getType())){
				try {
					f.set(clone, new LinkedList<>());
				} 
				catch (IllegalArgumentException e) { e.printStackTrace(); }
				catch (IllegalAccessException e) { e.printStackTrace();	}
			}
		return clone;
	}
	
	/*
	 * Get all the annotated @Reduce Collection
	 * to be merged at the end of the parsing.
	 * 
	 * Used by the crawler to join all the parallel searches.
	 */
	private void findReducedFields(){
		List<Field>    fields  = new LinkedList<>();
		List<Class<?>> classes = new LinkedList<>();
		Class<?>       _class  = this.getClass();
		while(! _class.equals(CrawlerHandler.class)){
			classes.add(_class);
			_class = _class.getSuperclass();
		}
		for(Class<?> cl : classes){
			Field[] ffs = cl.getDeclaredFields();
			for(Field f : ffs){
				if(f.isAnnotationPresent(Reduced.class)){
					if(Collection.class.isAssignableFrom(f.getType())) {
						f.setAccessible(true);
						fields.add(f);
					}
					else
						System.err.println("Type not supported for "+f.getName());
				}
			}
		}
		this.reducedFields = fields.toArray(new Field[0]);
	}
	
	//--------------------------------------------
	// Connection
	
	/**
	 * Perform a simple HTTP request to the target.
	 * If your target required a more elaborated connection (password, redirection)
	 * override this method.
	 * 
	 * <em>This method is provided as convenience</em>
	 * 
	 * @param url target location
	 * @return connection to the URL target 
	 * @throws MalformedURLException the URL is wrong
	 * @throws IOException Cannot open a connection
	 */
	protected HttpURLConnection makeConnection(String url) throws MalformedURLException, IOException{
		return (HttpURLConnection) new URL(url).openConnection();
	}
	
	//--------------------------------------------
	// Parsing
	
	/**
	 * This method contains the whole routine for the parser.<br/>
	 * 
	 * <code>urls</code> is the list of URL to parse, this list
	 * is 1 <= urls.length() <= chunkSize.
	 * The returned list will become the list of URL to parse, if
	 * this list exceed <code>chunkSize</code> elements only the first <code>chunkSize</code>
	 * element will be returned to this thread and the remaining URLs will
	 * be delagated to another thread. Returning null will be 
	 * interpreted as an abort signal.
	 * 
	 * @param urls list to parse
	 * @return the url list to be parsed or null to abort.
	 */
	protected List<String> parse(List<String> urls){
		List<String> links = new LinkedList<>();
		for(String url : urls){
			if(abort.get()){ return null; }
			
			try{
				HttpURLConnection connection = makeConnection(url);
				Page              page       = new Page(connection);
				synchronized (this) { // prevent master - reduce collisions
					if(abort.get() || ! parsePage(page))
					{ return null;}
				}
				links.addAll(filterLinks(page, page.getHyperLinks()));
			} catch (MalformedURLException e){
				System.err.println(e.getLocalizedMessage());
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		return links;
	}
	
	/**
	 * Parsing function
	 * 
	 * @param page current page retrieved
	 * @return false will abort the current search, true to continue
	 * @throws IOException
	 */
	public abstract boolean parsePage(Page page) throws IOException;
	
	//--------------------------------------------
	// Filters
	
	/**
	 * Remove all unwanted links. The returned list from this 
	 * method will be used to propagate the current search. It
	 * is useful to remove links that may be considered as noise
	 * for instance .css or .js file. But that's depend on the purpose of
	 * the search. By default this method remove all link that are not
	 * descendent of the current URL retrieved.
	 * 
	 * It is possible to insert new URL too, which is common for paging
	 * as in:
	 * <div>
	 * http://www.example.org?page=1
	 * </div>
	 * 
	 * <em>This method is provided as convenience</em>
	 * 
	 * @param page current page retrieved
	 * @param links all hyperlinks from this page
	 * @return list of accepted links
	 * @see #childOnly(URL, List)
	 */
	public List<String> filterLinks(Page page, List<String> links){
		return childOnly(page.getConnection().getURL(), links);
	}
	
	/**
	 * Remove all link that are not
	 * descendent of the current URL retrieved.
	 * <div>
	 * For instance, assuming the current page is 
	 * http://www.example.org/a/b/?hello="world"<br/>
	 * http://www.example.org/a/b/c/d/e is a valid descendent<br/>
	 * http://www.example.org/a/b/ is invalid
	 * </div>
	 * 
	 * <em>This method is provided as convenience</em>
	 * 
	 * @param domain the current URL
	 * @param links list of links
	 * @return list of links passing the constraint
	 */
	protected List<String> childOnly(URL domain, List<String> links){
		List<String> passing = new LinkedList<>();
		String       parent  = domain.getProtocol() +"://"+ domain.getAuthority() + domain.getPath();
		for(String link : links){
			if(link.startsWith(parent) && link.length() > parent.length())
				passing.add(link);
		}
		return passing;
	}
	
	//--------------------------------------------
	// RexCrawler
	
	/**
	 * Merge the shared collections.
	 * 
	 * <code>other.class</code> must match <code>this.class</code> 
	 * so they have the same Field objects. 
	 * This should be guarantee by the prototype pattern.
	 * 
	 * Since only Collections annotated with @Reduced are automatically reduced
	 * this method allows you to collect other fields. 
	 * <strong>
	 * Internally only the calling object's containers are retained, therefore
	 * the calling object should include the other's containers.
	 * </strong>
	 * 
	 * @param other crawlerHandler to include
	 */
	protected void reduce(CrawlerHandler other) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		for(Field f: this.reducedFields){
			if(Collection.class.isAssignableFrom(f.getType()))
				reduceCollection.invoke(f.get(this), f.get(other));
		}
	}
	
	/**
	 * Shared flag
	 * 
	 * This shared object is used to signaling the abortion of the
	 * current search.
	 */
	protected AtomicBoolean abort;
	private   Field[]       reducedFields;
	private   Method        reduceCollection;
}
