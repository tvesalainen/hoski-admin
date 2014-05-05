HoskiAdmin is a java application for sailing club back office work.


See Google App Engine documents at https://developers.google.com/appengine

See dependencies at pom.xml.

Compiling
---------
mvn install

Deployment
----------

Commit to git
-------------

Change <version>1.0.3</version> 

in pom.xml

Deploy to Maven Central Repository
----------------------------------

See http://central.sonatype.org/ how to create environment for deployment

Run:

mvn clean:clean javadoc:jar source:jar deploy
 
