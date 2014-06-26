package com.tejzp.jmxr;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by tepratap on 6/11/2014.
 */
public class MonitorMain {
    @Option(name="-p",usage="Host polling interval in milliseconds.")
    private int sleepTime = 10000;

    @Option(name="-l",usage="Path to store monitor log files.")
    private String logpath =  null;

    @Option(name="-h",usage="Path to hosts list file.")
    private String hostsfile =  "./hosts.properties";

    @Option(name="-i",usage="Path to PID file.")
    private String pidfile =  "./pid.txt";

    @Option(name="-e",usage="Path to Email Properties.")
    private String emailProps =  "./email.properties";


    public static void main(String[] args) throws Exception {
        new MonitorMain().doMain(args);
    }
    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        Map<String,String> hosts = new HashMap<String, String>();

        FileWriter fw = null;
        BufferedWriter bw = null;
        try {

            String content = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

            File file = new File(pidfile);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
            bw.write(content);

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(bw!=null)
            {
                try {
                    bw.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
            if(fw!=null)
            {
                try {
                    fw.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }

        try {
            // parse the arguments.
            parser.parseArgument(args);
        }
        catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("java SampleMain [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }


        Properties prop = new Properties();
        FileInputStream fis = null;


        try {
            File file = new File(hostsfile);
            fis = new FileInputStream(file);
            if (fis == null) {
                System.out.println("Sorry, unable to find " + hostsfile);
                return;
            }

            prop.load(fis);

            Enumeration<?> e = prop.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = prop.getProperty(key);
                hosts.put(key,value);
            }

        } catch (IOException ex) {
            //ex.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }

        if(!JMXrMonitor.isWindows()) {
            SendMail.send("Started monitoring hosts", "", SendMail.SIMPLE_ALERT);
        }
        else {
            logpath = "./monitor-logs";
        }

        try
        {
            if(logpath!=null) {
                new File(logpath).mkdirs();
            }
        }
        catch (Exception e)
        {

        }

        SendMail.proppath = emailProps;

        Date dt=new Date();

        Date myDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM_dd_yyyy");
        String fileTimeStamp = sdf.format(myDate);
        ArrayList <Thread> hostThreads = new ArrayList<Thread>();

        try {
            if (hosts != null && !hosts.isEmpty()) {
                int i = 1;
                Iterator it = hosts.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    String fileName = null;
                    if(logpath!=null) {
                        fileName = logpath + "/host_no_" + i + "_log.csv";
                    }
                    String hostKey = pairs.getKey().toString();
                    Thread hostThread = new Thread(new JMXrMonitor(pairs.getValue().toString(), fileName, sleepTime,hostKey));
                    hostThread.start();
                    hostThreads.add(hostThread);
                    //prod1Thread.join();
                    i++;
                    it.remove(); // avoids a ConcurrentModificationException
                }
                for(Thread runningThread:hostThreads)
                {
                    runningThread.join();
                }
            } else {
                System.out.println("Please enter some host names in JMX format into hosts.txt");
            }
        }
        catch(Exception e)
        {

        }
    }
}
