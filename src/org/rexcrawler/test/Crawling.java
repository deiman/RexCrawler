package org.rexcrawler.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.rexcrawler.Crawler;
import org.rexcrawler.CrawlerHandler;
import org.rexcrawler.Page;
import org.rexcrawler.Reduced;

public class Crawling {
	
	// Handler
	static class URLCollector extends CrawlerHandler {
		
		public URLCollector() {
			this.links = new TreeSet<>();
		}
		
		public Set<String> getLinks(){
			return this.links;
		}
		
		@Reduced
		Set<String> links;

		@Override
		public boolean parsePage(Page page) throws IOException {
			this.links.addAll(page.getHyperLinks());
			return true;
		}
	}
	
	
	@Before
	public void getTarget() throws MalformedURLException{
		this.root         = new URL         (System.getProperty("root")        );
		this.traversedUrl = Integer.parseInt(System.getProperty("traversedUrl"));
		this.handler      = new URLCollector();
	}
	
	@Test
	public void singleCrawling() throws MalformedURLException {
		Crawler crawler = new Crawler()
			.setHandler(handler);
		
		crawler.run(this.root);
		assertEquals(1, crawler.getFollowedLinkCount());
	}
	
	@Test
	public void listCrawling() throws MalformedURLException {
		Crawler crawler = new Crawler()
			.setHandler(handler);
		
		int length = 4;
		URL[] linkSubmitted = new URL[length];
		for (int i = 0; i < length; i++) 
			linkSubmitted[i] = this.root;
		
		crawler.run(linkSubmitted);
		assertEquals(length, crawler.getFollowedLinkCount());
	}
	
	@Test
	public void exhaustiveSerialCrawling() throws MalformedURLException {
		Crawler crawler = new Crawler()
			.setHandler(handler)
			.setSearchLength(traversedUrl *2);
		
		crawler.run(this.root);
		assertEquals(traversedUrl, crawler.getFollowedLinkCount());
	}
	
	@Test
	public void limitedSerialCrawling() throws MalformedURLException {
		if(traversedUrl / 2 == 0)
			fail("Too few expected findings to carry this test");
		
		Crawler crawler = new Crawler()
			.setSearchLength(traversedUrl /2)
			.setHandler(handler);
	
		crawler.run(this.root);
		assertEquals(traversedUrl /2, crawler.getFollowedLinkCount());
	}
	
	@Test
	public void exhaustiveParallelCrawling() throws MalformedURLException {
		Crawler crawler = new Crawler()
			.setHandler(handler)
			.setChunkSize(10)
			.setSearchLength(traversedUrl *2);
		
		crawler.run(this.root);
		assertEquals(traversedUrl, crawler.getFollowedLinkCount());
	}
	
	@Test
	public void limitedParallelCrawling() throws MalformedURLException {
		if(traversedUrl / 2 == 0)
			fail("Too few expected findings to carry this test");
		
		Crawler crawler = new Crawler()
			.setChunkSize(10)
			.setSearchLength(traversedUrl /2)
			.setHandler(handler);
	
		crawler.run(this.root);
		assertEquals(traversedUrl /2, crawler.getFollowedLinkCount());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void illegalSearchLength(){
		Crawler crawler = new Crawler();
		crawler.setSearchLength(0);
	}
	
	@Test
	public void illegalChunkSize(){
		Crawler crawler = new Crawler();
		crawler.setChunkSize(0);	
		assertEquals(-1, crawler.getChunkSize());
		final int csize = 15;
		crawler.setChunkSize(csize);
		crawler.setChunkSize(0);
		assertEquals(csize, crawler.getChunkSize());
		crawler.setChunkSize(-15);
		assertEquals(csize, crawler.getChunkSize());
	}

	private URL             root;
	private int             traversedUrl;
	private URLCollector    handler;
}
