RexCrawler
==========

A minimalistic multithreaded crawler API for Java.

Dispate its minimalistic structure, this crawler is based on the Java
ForkJoin Framework which allows RexCrawler to create other deamons on demand
by partitioning its the local workload. A key benefit of this approach is a greater
control on granularity and less synchronization.

## Getting started
A brief overview on how to use RexCrawler. 

It is so simple that you can jump to the [sample](https://github.com/shake0/RexCrawler/blob/master/src/org/rexcrawler/test/CrawlerExample.java) 
and generate the Javadoc with `javadoc -d doc/ -protected -sourcepath src/ org.rexcrawler`

#### Define the handler
The handler (a class that extends the *CrawlerHandler*) allows you to control
the flow of the search. Specifically what you collect from the various HTTP responses
and which URLs will be submitted for continuing the search.

For parsing strings, a simple [handler](https://github.com/shake0/RexCrawler/blob/master/src/org/rexcrawler/handler/RexHandler.java) 
is provided.

The simplest implementation of the CrawlerHandler looks like:
```
public class MyHandler extends CrawlerHandler {

	public MyHandler(){
		this.myBuffer = new LinkedList<>();
	}

	@Override
	public boolean parsePage(Page page){
		// Do parsing ...
		return true;
	}

	@Reduced
	private List<String> myBuffer;
}
```

*Important:* due to its multithreaded nature, all collection used to store information
to be retrieved at the end of the search must be annotated with *@Reduced*

#### Use the crawler
Assuming you defined an handler _myHandler_ then you can run it
by calling:
```
Crawler myCrawler = new Crawler();
myCrawler.setHandler(myHandler);
myCrawler.run(target);
```
in this example _target_ is a URL object for the search root.

#### Wait that's not multithreaded
By default it's not, but enabling multithreading is as simple as
adding the following 2 lines.
```java
Crawler myCrawler = new Crawler();
myCrawler.setChunkSize(15);
myCrawler.setSearchDepth(200);
myCrawler.setHandler(myHandler);
myCrawler.run(target);
```
*setChunkSize* allows you to set the granularity of the opertation
or in other word, how many URL a single thread will handle by itself.

*setSearchDepth* is the upper limit for how many link the crawler will
follow. By default this is limited to the number of targets.

## Advanced
For those who like to start from scratch! 

RexCrawler idea is very simple:
 1. submit a list of URLs
 2. if URL list > chunkSize
 3.  fork                         (clone())
 4. foreach URL
 5.  retrieve URL                 (makeConnection(String))
 6.  parse Page                   (parsePage(Page page))
 7.  add hyperLinks in LINKS      (filterLinks(Page page))
 8. merge collection with master  (reduce(CrawlerHandler))
 9. jump to 1 (URL = LINKS)

Notice that fork is not a system call and the child will effectivelly
start from 1.

I tried to give a general implementation for every step, but
it can be the case that this do not fit your purpose. Therefore
I suggest you redefine `CrawlerHandler.parse(List<String>)` which
cover step 4 to 7 and optionally `CrawlerHandler.reduce(CrawlerHandler)`
for step 8.

Few tips for implementing the Handler
 * All field are shallow copied when forked.
 You should synchronized shared object or replace them in the `clone` method.
 * `reduce` is always call by the master, then you should reassign the field of 
 `this` object (as in `this.myField.add(other.myField);`)
 * `parse` receive the list of URL to parse and return the URL list that will
 be parsed. Returning null will abort the search


## License

The MIT License (MIT)

Copyright (c) 2015 Rodrigo Coniglione

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
