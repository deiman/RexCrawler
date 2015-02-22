package org.rexcrawler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page wrapper
 * 
 * @author shake0
 *
 */
public class Page {
	
	public Page(HttpURLConnection connection){
		this.connection  = connection;
		this.pageContent = null;
		this.links       = null;
	}
	
	/**
	 * Get connection object
	 * @return
	 */
	public HttpURLConnection getConnection(){
		return this.connection;
	}
	
	/**
	 * Get page content
	 * @return string with the page content
	 * @throws IOException
	 */
	public String getContent() throws IOException{
		if(pageContent == null)
			this.pageContent = stringfyPage(this.connection.getInputStream());
		return this.pageContent;
	}
	
	/**
	 * Test the mime type of the response to be a text.
	 * Important for the <code>pageContent</code> to be readable.
	 * @return true if the mime is "text" otherwise false
	 */
	public boolean isCharacterContent(){
		String mime = this.connection.getContentType();
		if(mime.startsWith("text"))
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		return this.connection.getURL().toString();
	}
	
	/**
	 * Get all the hyperlinks of this page. This method may return
	 * an empty list if the content-type of this page is not a text.
	 * Links are normilized, therefore relative links (eg. "my/path")
	 * are appended to the page URL.
	 * 
	 * @return a list of all the links
	 * @throws IOException
	 */
	public List<String> getHyperLinks() throws IOException{
		if(links != null) return this.links;
		List<String> links   = new LinkedList<>();
		if(this.isCharacterContent()){
			Matcher matcher = Pattern.compile("href=\"([-a-zA-Z0-9+&/?=~_:,.]*)\"")
					.matcher(this.getContent());
			while(matcher.find()){
				String match  = matcher.group(1);
				URL    domain = this.getConnection().getURL();
				// normalize relative links
				if(! match.contains("://")){
					String base = domain.getProtocol() + "://" + domain.getAuthority();
					if(match.startsWith("/"))
						match = base + match;
					else
						match = base + domain.getPath() + match;
				}
				links.add(match);
			}
		}
		this.links = links;
		return links;
	}
	
	/**
	 * Save the page
	 * @param filepath location of the file
	 * @throws IOException
	 */
	public void save(String filepath) throws IOException{
		FileOutputStream fos = new FileOutputStream(filepath);
		if(this.pageContent != null){
			fos.write(this.pageContent.getBytes());
		}
		else{
			InputStream is  = this.connection.getInputStream();
			int c;
			while((c = is.read()) != -1){
				fos.write(c);
			}
			is.close();
		}
		fos.close();
	}
	
	private String stringfyPage(InputStream pageStream) throws IOException{
		StringBuilder sbuilder   = new StringBuilder();
		int c;
		while((c = pageStream.read()) > 0)
			sbuilder.append((char) c);
		return sbuilder.toString();
	}
	
	private HttpURLConnection connection;
	private String            pageContent;
	private List<String>      links;
}
