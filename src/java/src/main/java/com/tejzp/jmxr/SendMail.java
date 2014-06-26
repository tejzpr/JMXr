package com.tejzp.jmxr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.mail.*;
import javax.mail.internet.*;

public class SendMail
{
    public static String proppath = "";
    public static long minminutes = 5;
    public static Map<String,String> emailHosts = null;
    public static Map<String,HostCheck> emailHostCheck= new HashMap<String, HostCheck>();
    public static final Integer SIMPLE_ALERT = 0;
    public static final Integer HEAP_ALERT = 1;
    public static final Integer PERMGEN_ALERT = 2;
    public static final Integer LOW_THREADCOUNT_ALERT = 3;
    public static final Integer HIGH_THREADCOUNT_ALERT = 4;
    public static final Integer HIGH_CPU_ALERT = 5;
    public static void send(String newmessage,String hostname,Integer messageType)
    {
        if(emailHosts == null) {
            emailHosts = new HashMap<String, String>();
            Properties prop = new Properties();
            FileInputStream fis = null;

            try {
                File file = new File(proppath);
                fis = new FileInputStream(file);
                if (fis == null) {
                    System.out.println("Sorry, unable to find " + proppath);
                    return;
                }

                prop.load(fis);

                Enumeration<?> e = prop.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = prop.getProperty(key);
                    emailHosts.put(key, value);
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }

        if (emailHosts != null && !emailHosts.isEmpty()) {
            int i = 1;
            Iterator it = emailHosts.entrySet().iterator();
            if(checkIfCanSend(messageType,hostname)) {
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    String hostKey = pairs.getKey().toString();
                    String to = pairs.getValue().toString();
                    if (isValidEmailAddress(to)) {
                        String from = "perfalerts@localhost.com";
                        String host = "localhost";
                        Properties properties = System.getProperties();
                        properties.setProperty("mail.smtp.host", host);
                        Session session = Session.getDefaultInstance(properties);
                        try {
                            MimeMessage message = new MimeMessage(session);
                            message.setFrom(new InternetAddress(from));
                            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                            message.setSubject("Production Server Alert!");
                            message.setText(newmessage);
                            Transport.send(message);
                        } catch (MessagingException mex) {
                            mex.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    public static boolean checkIfCanSend(Integer messageType,String host)
    {
        if(messageType == 0)
        {
            return true;
        }

        String hostKey = host+messageType.toString();
        HostCheck hostMessageType = emailHostCheck.get(hostKey);
        if(hostMessageType == null)
        {
            emailHostCheck.put(hostKey,new HostCheck(host,new Date().getTime(),messageType));
            return true;
        }
        else
        {
            long diff = new Date().getTime() - hostMessageType.getTimestamp();
            long minutes = TimeUnit.MILLISECONDS.toSeconds(diff)/60;
            if(minutes > minminutes)
            {
                emailHostCheck.remove(hostKey);
                return true;
            }
            else
            {
                return false;
            }
        }
    }
    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }
    public static void main(String args[])
    {

    }
}