package com.tejzp.jmxr;

/**
 * Created by tepratap on 6/23/2014.
 */
public class HostCheck{
    public HostCheck(String hostname,Long timestamp,Integer messageType)
    {
        this.hostname = hostname;
        this.messageType=messageType;
        this.timestamp=timestamp;
    }
    public String hostname = null;
    public Long timestamp = null;
    public Integer messageType = null;

    public String toString()
    {
        return hostname+","+timestamp.toString()+","+messageType.toString();
    }
    public Integer getMessageType()
    {
        return messageType;
    }
    public Long getTimestamp()
    {
        return timestamp;
    }
    public String getHostname()
    {
        return hostname;
    }
}