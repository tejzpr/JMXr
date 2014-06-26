JMXr
====

JMXr is a JMX RMI based live host monitoring tool with email notifications for monitoring crucial system attributes.

Requirements
============
* Linux or Windows based OS
* JDK v 1.5 or above
* Python 2.7 
* Redis 

Installation
============
* Copy compiled_package/jmxr.zip to a suitable path in your server and extract it.
* Update hosts.properties with JMX url to the hosts you want to monitor.
* Update email.properties with email addresses you want the notifications to be sent.
* Make suitable changes to the following files according to your paths:
  * start-monitor.sh
  * start-server.sh
  * stop-monitor.sh
  * stop-server.sh
* run start-monitor.sh and start-server.sh
* Assocoated process ID's are stored in pid.txt and pid-server.txt

Usage
=====
The start-monitor.sh will start the monitoring and send email notifications. The webview requires the start-server.sh to be started, once it is run you can access the web frontent at [http://localhost:9945](http://localhost:9945)
