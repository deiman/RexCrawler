RexCrawler
==========

A minimalistic multithreaded crawler API for Java.

Dispate its minimalistic structure, this crawler is based on the Java
ForkJoin Framework which allows RexCrawler to create other deamon on demand
by partitioning its the local workload. A key benefit of this approach is a greater
control on granularity and less synchronization.

## Getting started
A brief overview on how to use RexCrawler. 
It is so simple that you can jump to the [sample](https://github.com/shake0/RexCrawler/tree/master/org/rexcrawler/CrawlerExample.java) 
or generate the Javadoc with `javadoc -d doc/ -protected -sourcepath src/ org.rexcrawler`


#### Define the handler
The handler (a class that extends the *CrawlerHandler*) allows you to control
the flow of the search. Specifically what you collect from the various HTTP responses
and which URLs will be submitted for continuing the search.

*Important:* due to its multithreaded nature, all collection used to store information
to be retrieved at the end of the search must be annotated with *@Reduced*

#### Use the parser
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
follow. By default this is 1.

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
