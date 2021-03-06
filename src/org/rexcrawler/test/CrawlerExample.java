package org.rexcrawler.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.rexcrawler.Crawler;
import org.rexcrawler.CrawlerHandler;
import org.rexcrawler.Page;
import org.rexcrawler.Reduced;

public class CrawlerExample {
	static class MyHandler extends CrawlerHandler {
		
		public MyHandler() {
			this.javaSourceCode = new LinkedList<>();
		}
		
		@Reduced
		List<String> javaSourceCode;

		@Override
		public boolean parsePage(Page page) throws IOException {
			System.out.println("LINK: "+page);
			List<String> links = page.getHyperLinks();
			for(String link : links)
				if(link.endsWith(".java"))
					javaSourceCode.add(link);
			return true;
		}
	}
	
	public static void main(String[] args) throws MalformedURLException {
		// Create the crawler
		Crawler rexcrawler = new Crawler();
		
		// Set the handler
		MyHandler handler = new MyHandler();
		rexcrawler.setHandler(handler);
		
		//Enable multithreading
		rexcrawler.setChunkSize(15);     // Granularity: 15 URL per thread
		
		// Set search limit
		rexcrawler.setSearchLength(100); // Follow maximum 100 links
		
		// Make a target to parse
		URL target = new URL(args[0]);
		
		// Crawl
		rexcrawler.run(target);
		
		// Get results
		for(String link : handler.javaSourceCode)
			System.out.println(link);
	}
}
