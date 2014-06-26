JMXr
====

JMXr is a JMX RMI based live host monitoring tool with email notifications for monitoring crucial system attributes.

Requirements
============
* Linux or Windows based OS
* JDK v 1.5 or above
* Python 2.7 
* Redis 

Features
========
*	Any number of users can monitor real time, without increasing thread count in the host server.
* View servers attributes of multiple hosts in a single webview.
* No need to install **server agents** for monitoring. 
* Sends email notifications
* Automatically reconnects to host server when they are restarted. 


Installation
============
* Make sure you have installed all the dependencies mentioned in the [DEPENDENCIES](https://github.com/tejzp/JMXr/blob/master/DEPENDENCIES) file
* Start the REDIS server at its defaullt port, i.e. 6379
* Copy [compiled_package/jmxr-1.0.0.zip](https://github.com/tejzp/JMXr/raw/master/compiled_package/jmxr-1.0.0.zip) to a suitable path in your server and extract it.
* Update hosts.properties with JMX url to the hosts you want to monitor.
* Update email.properties with email addresses you want the notifications to be sent.
* Make suitable changes to the following files according to your installation:
  * start-monitor.sh
  * start-server.sh
  * stop-monitor.sh
  * stop-server.sh
* run start-monitor.sh and start-server.sh
* Assocoated process ID's are stored in pid.txt and pid-server.txt
* The current packaged version i.e v1.0.0 was compiled using JDK 1.7, if you need this software to work with a lower JRE please pull the source and recompile. A pom-java5.xml is included with proper dependencies for Java 5.

Usage
=====
* Install all dependencies and cd into the installed directory
* Run the command **start-monitor.sh&** to start monitoring and for sending email notifications. 
* Run the command **start-server.sh&** to start the web frontend, which you can access at [http://localhost:9945](http://localhost:9945)
