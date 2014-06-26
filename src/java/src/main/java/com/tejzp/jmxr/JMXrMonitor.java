package com.tejzp.jmxr;

import au.com.bytecode.opencsv.CSVWriter;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.*;
import java.lang.management.ManagementFactory;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.net.MalformedURLException;
import java.rmi.UnknownHostException;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Created by tepratap on 6/10/2014.
 */
public class JMXrMonitor implements Runnable
{
    private static String OS = System.getProperty("os.name").toLowerCase();
    MBeanServerConnection server = null;

    private int sleepTime = 0;
    private int connectionErrorSleepTime = 600000;

    int connectionCounter = 0;
    int maxReconnectAttempt = 10;
    String host = "";
    String outFile = null;
    String hostKey = "";
    String jmxString = "";
    private int  availableProcessors = 0;
    private long lastSystemTime      = 0;
    private long lastProcessCpuTime  = 0;

    public JMXrMonitor(String server, String outFile, int sleepTime, String hostKey)
    {
        this.jmxString = server;
        this.hostKey = hostKey;
        this.sleepTime = sleepTime;
        this.outFile = outFile;
    }
    public double getCpuUsage() throws MalformedObjectNameException,MBeanException,AttributeNotFoundException,InstanceNotFoundException,ReflectionException,IOException
    {
        if ( lastSystemTime == 0 )
        {
            baselineCounters();
            return 0;
        }

        long systemTime     = System.nanoTime();
        long processCpuTime = 0;

        ObjectName OSbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        processCpuTime = (Long)server.getAttribute(OSbean,"ProcessCpuTime");



        double cpuUsage = (double) ( processCpuTime - lastProcessCpuTime ) / ( systemTime - lastSystemTime );

        lastSystemTime     = systemTime;
        lastProcessCpuTime = processCpuTime;

        return cpuUsage / availableProcessors;
    }

    private void baselineCounters() throws MalformedObjectNameException,MBeanException,AttributeNotFoundException,InstanceNotFoundException,ReflectionException,IOException
    {
        lastSystemTime = System.nanoTime();

        ObjectName OSbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        lastProcessCpuTime = (Long)server.getAttribute(OSbean,"ProcessCpuTime");
    }

    private MBeanServerConnection getServer(String connUrl)throws MalformedURLException,MalformedObjectNameException,ReflectionException,InstanceNotFoundException,AttributeNotFoundException,MBeanException,IOException
    {
        MBeanServerConnection server = null;
        JMXServiceURL url = new JMXServiceURL(connUrl);
        this.host = url.getURLPath();
        JMXConnector jmxc = JMXConnectorFactory.connect(url);
        server = jmxc.getMBeanServerConnection();
        ObjectName OSbean = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        availableProcessors = (Integer)server.getAttribute(OSbean,"AvailableProcessors");
        return server;
    }

    private int getPermGenPercentage()throws IOException
    {
        MemoryPoolMXBean permgenBean = null;
        int percentageUsed = 0;


        Set<ObjectInstance> beans = server.queryMBeans(null, null);
        for (ObjectInstance instance : beans) {
            //System.out.println(instance.getObjectName().getCanonicalName());
            if (instance.getObjectName().getCanonicalName().toLowerCase().indexOf("perm gen") >= 0) {
                permgenBean = ManagementFactory.newPlatformMXBeanProxy(server,
                        instance.getObjectName().toString(), MemoryPoolMXBean.class);
                break;
            }
        }


        if(permgenBean!=null) {
            MemoryUsage currentUsage = permgenBean.getUsage();
            percentageUsed = (int) ((currentUsage.getUsed() * 100)
                    / currentUsage.getMax());
        }
        return percentageUsed;
    }

    public void runMonitor() throws MalformedObjectNameException,MBeanException,AttributeNotFoundException,InstanceNotFoundException,ReflectionException,IOException
    {

            Double cpuUsage = getCpuUsage();
            String roundedCPUUsage = Double.toString(Math.round(cpuUsage * 100));

            Object classLoad = server.getAttribute(new ObjectName("java.lang:type=ClassLoading"), "LoadedClassCount");
            String loadedClasses = Integer.toString((Integer) classLoad);

            Object threadCount = server.getAttribute(new ObjectName("java.lang:type=Threading"), "ThreadCount");
            String numthreadCount = Integer.toString((Integer) threadCount);

            Object heap = server.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
            CompositeData heapd = (CompositeData) heap;
            Double tempUsedHeap = new Long((Long) heapd.get("used")).doubleValue();
            Double tempMaxHeap = new Long((Long) heapd.get("max")).doubleValue();
            String usedHeap = Double.toString(Math.round(((tempUsedHeap * 100)/tempMaxHeap))*100.0/100.0);

            String permGenPercentageVal = Integer.toString(getPermGenPercentage());

            java.util.Date date = new java.util.Date();

            CSVWriter writer = null;
            FileWriter w = null;
            try {
                if(outFile!=null) {
                    w = new FileWriter(outFile);
                    writer = new CSVWriter(w, ',');
                    writer.writeNext(new String[]{loadedClasses, numthreadCount, usedHeap, roundedCPUUsage, permGenPercentageVal, new Timestamp(date.getTime()).toString()});
                    writer.flush();
                }
            }
            catch(IOException e)
            {
                printStackTraceToSysout(e);
            }
            finally {
                if(writer != null)
                {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
                if(w != null)
                {
                    try {
                        w.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }

            notificationHandler(loadedClasses, numthreadCount, usedHeap, roundedCPUUsage, permGenPercentageVal);
            SaveToCache.save(this.hostKey, loadedClasses, numthreadCount, usedHeap, roundedCPUUsage, permGenPercentageVal, new Timestamp(date.getTime()).toString());

            if(connectionCounter>0)
            {
                System.out.println("Reconnected to >>"+this.jmxString);
                connectionCounter=0;
            }
    }

    private void notificationHandler(String loadedClasses,String numthreadCount,String usedHeap,String roundedCPUUsage,String permGenPercentageVal)
    {
        boolean sentMail = false;
        if(!isWindows()) {
            if (Double.valueOf(usedHeap) >= 90) {
                SendMail.send("High heap memory use in :" + this.host, this.host, SendMail.HEAP_ALERT);
            }
            if (Integer.valueOf(permGenPercentageVal) >= 98) {
                SendMail.send("PermGen usage >= 98% in :" + this.host, this.host, SendMail.PERMGEN_ALERT);
            }
            if (Integer.valueOf(numthreadCount) <= 0) {
                SendMail.send("Low thread count in :" + this.host, this.host, SendMail.LOW_THREADCOUNT_ALERT);
            }
            if (Integer.valueOf(numthreadCount) >= 200) {
                SendMail.send("High thread count in :" + this.host, this.host, SendMail.HIGH_THREADCOUNT_ALERT);
            }
            if (Double.valueOf(roundedCPUUsage) >= 90) {
                SendMail.send("High CPU Usage in :" + this.host, this.host, SendMail.HIGH_CPU_ALERT);
            }
        }
    }

    public static boolean isWindows() {

        return (OS.indexOf("win") >= 0);

    }

    public static boolean isMac() {

        return (OS.indexOf("mac") >= 0);

    }

    public static boolean isUnix() {

        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );

    }

    public static boolean isSolaris() {

        return (OS.indexOf("sunos") >= 0);

    }

    public void printStackTraceToSysout(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.out.println(sw.toString());
    }
    public String getStackTraceToString(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }


    public void runLoop() throws InterruptedException,MalformedObjectNameException,MBeanException,AttributeNotFoundException,InstanceNotFoundException,ReflectionException,IOException
    {
        while(true) {
            try {
                runMonitor();
                Thread.sleep(sleepTime);
            }
            catch(IOException e)
            {
                if(e instanceof java.rmi.ConnectException) {
                    if(connectionCounter < maxReconnectAttempt) {
                        System.out.println("Connection disconnected trying to reconnect to >>"+this.jmxString);
                        Thread.sleep(connectionErrorSleepTime);
                        try {
                            MBeanServerConnection newConn = getServer(this.jmxString);
                            if(newConn!=null)
                            {
                                this.server = newConn;
                            }
                        }
                        catch(Exception n)
                        {
                            System.out.println("Connection disconnected trying to reconnect to ("+connectionCounter+") >>"+this.jmxString);
                        }
                        connectionCounter++;
                    }
                    else
                    {
                        System.out.println("Reconnect Tries exceeded.. Exiting.");
                        throw new IOException(getStackTraceToString(e));
                    }
                }
                else
                {
                    throw new IOException(getStackTraceToString(e));
                }
            }
        }
    }

    public void run() {
        try
        {
            this.server = getServer(this.jmxString);
            System.out.println("Connected to >>" +this.jmxString);
            runLoop();
        }
        catch (UnknownHostException e)
        {
            printStackTraceToSysout(e);
            System.out.println("Could not connect to "+this.jmxString);
            Thread.currentThread().interrupt();
            return;
        }
        catch(MalformedObjectNameException e)
        {
            printStackTraceToSysout(e);
            Thread.currentThread().interrupt();
            return;
        }
        catch(ReflectionException e)
        {
            printStackTraceToSysout(e);
            Thread.currentThread().interrupt();
            return;
        }
        catch(InstanceNotFoundException e)
        {
            printStackTraceToSysout(e);
            Thread.currentThread().interrupt();
            return;
        }
        catch(AttributeNotFoundException e)
        {
            printStackTraceToSysout(e);
            Thread.currentThread().interrupt();
            return;
        }
        catch(MBeanException e)
        {
            printStackTraceToSysout(e);
            Thread.currentThread().interrupt();
            return;
        }
        catch (IOException e)
        {
            printStackTraceToSysout(e);
            System.out.println("Connection broke to (IOException) >>" + this.jmxString);
            Thread.currentThread().interrupt();
            return;
        }
        catch (Exception e) {
            printStackTraceToSysout(e);
            Thread.currentThread().interrupt();
            return;
        }
    }
}