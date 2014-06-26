package com.tejzp.jmxr;

import redis.clients.jedis.Jedis;


/**
 * Created by tepratap on 6/21/2014.
 */
public class SaveToCache {
    public static void save(String hostname,String loadedClasses,String numthreadCount,String usedHeap,String roundedCPUUsage,String permGenPercentageVal,String timestamp)
    {
        try {
            Jedis jedis = new Jedis("localhost");
            jedis.set(hostname,"[\"" + loadedClasses + "\",\"" + numthreadCount + "\",\"" + usedHeap + "\",\"" + roundedCPUUsage + "\",\"" + permGenPercentageVal + "\",\"" + timestamp + "\"]");
        }
        catch(Exception e)
        {

            e.printStackTrace();
        }
    }
}
