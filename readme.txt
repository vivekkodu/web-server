=== Webserver 1.0 ===
Author: Vivek Verma

== Description ==

Basic java based web server which can take GET and POST request.

== Prerequisites ==

Java Runtime Environment(JRE) 7

== Internals ==

All the requests are maintained in a queue. There is a thread pool which is waiting on the queue and requests present in the
queue is maintained by these threads.

== Usage ==
Update config.properties and log.properties.
Create package, mvn package.
Copy Run.bat to target(Output) folder.
Run Run.bat
Or
Run following command after creating package.
java -cp web-server-1.0-jar-with-dependencies.jar com.adobe.web.server.HTTPWebServer


	
	
	
