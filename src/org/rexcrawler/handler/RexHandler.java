package org.rexcrawler.handler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rexcrawler.CrawlerHandler;
import org.rexcrawler.Page;

/**
 * A common handler based on regular expression.
 * This class may parse the same page multiple times.
 * @author shake0
 *
 */
public class RexHandler extends CrawlerHandler{
	
	/*
	 * Structure
	 */
	private class Filter {
		
		public Filter(int group) {
			this.results = new LinkedList<>();
			this.group   = group;
		}
		
		List<String> results;
		int          group;
	}
	
	public RexHandler() {
		this.filters = new HashMap<>();
	}
	
	/**
	 * Add a filter
	 * @param pat pattern used to match
	 * @return the calling object
	 */
	public RexHandler addFilter(Pattern pat){
		return addFilter(pat, 0);
	}
	
	/**
	 * Add filter and collect the specified group
	 * @param pat pattern used to match
	 * @param group 
	 * @return the calling object
	 */
	public RexHandler addFilter(Pattern pat, int group){
		filters.put(pat, new Filter(group));
		return this;
	}
	
	/**
	 * Get the list of matches found for the pattern
	 * @param pat
	 * @return list of matches
	 */
	public List<String> getResult(Pattern pat){
		return this.filters.get(pat).results;
	}
	
	/**
	 * Get all the matches.
	 * @return
	 */
	public List<String> getResults(){
		List<String> results = new LinkedList<>();
		for(Filter value : this.filters.values())
			results.addAll(value.results);
		return results;
	}

	@Override
	public boolean parsePage(Page page) throws IOException {
		Matcher match;
		for(Entry<Pattern, Filter> filter : filters.entrySet()){
			Pattern      key     = filter.getKey();
			int          group   = filter.getValue().group;
			List<String> results = filter.getValue().results;
			
			match = key.matcher(page.getContent());
			while(match.find())
				results.add(match.group(group));
		}
		return true;
	}

	//---------------------------------------------
	// Forking and Reducing on using a Map
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		RexHandler handler =(RexHandler) super.clone();
		// A new Map for each thread 
		handler.filters    = new HashMap<>();
		return handler;
	}
	
	@Override
	protected void reduce(CrawlerHandler other) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		// Reducing the Map
		RexHandler handler = (RexHandler) other;
		for(Entry<Pattern, Filter> entry : handler.filters.entrySet()){
			if(this.filters.containsKey(entry.getKey()))
				this.filters.get(entry.getKey()).results.addAll(entry.getValue().results);
			else
				this.filters.put(entry.getKey(), entry.getValue());
		}
		super.reduce(other);
	}
	
	private Map<Pattern, Filter> filters;
}
