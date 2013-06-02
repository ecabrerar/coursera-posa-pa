coursera-posa-pa
================
h2. What is this project ?

This project *coursera-posa-pa* contains 2 simple examples that performs 2 *Echo Server* using *Java Netty*. This examples were part of the POSA  Course's Programming Assignment at Coursera (https://class.coursera.org/posa-001/class/index).

h2.  To start quickly, build the project :

<pre><code>cd coursera-posa-pa</code></pre>

To build *Programming assignment #3* 

-The purpose of this assignment is to deepen your understanding of the Wrapper Facade pattern, the Reactor pattern and the (Acceptor role of the) Acceptor-Connector pattern in the context of Java Netty. 

<pre><code>cd posa-pa-iii</code></pre>
<pre><code>mvn install package assembly:assembly -DskipTests=true</code></pre>

h2.  Now run the JAR file using

<pre><code>*java -jar posa-pa-iii-0.0.1-SNAPSHOT-jar-with-dependencies.jar*</code></pre>


To build *Programming assignment #4*

-The purpose of this assignment is to deepen your understanding of the Half-Sync/Half-Async pattern in the context of Java. 

<pre><code>cd posa-pa-iv</code></pre>

<pre><code>mvn install package assembly:assembly -DskipTests=true</code></pre>

<pre><code>java -jar posa-pa-iv-0.0.1-SNAPSHOT-jar-with-dependencies.jar</code></pre>


It is also possible to pass the port number and host name to connect 


*java -jar posa-pa-iv-0.0.1-SNAPSHOT-jar-with-dependencies.jar ** someHost port*

Default values are 8080, "localhost"

Echo Client can be invoked simply by executing telnet localhost 8080
