/**
 * 
 */
package com.sohu.cache.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author dengsong
 *
 */
public class IPUtil {
	public static String getInnerIp() {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                	InetAddress addr = addresses.nextElement();
                	if (addr instanceof Inet4Address && isInnerIP(addr.getHostAddress())){
                		return addr.getHostAddress();
                	}
                    
                }
            }
        } catch (SocketException e) {
        
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }
	
	private static boolean isInnerIP(String ipAddress){    
        boolean isInnerIp = false;    
        long ipNum = getIpNum(ipAddress);    
        /**   
        私有IP：A类  10.0.0.0-10.255.255.255   
               B类  172.16.0.0-172.31.255.255   
               C类  192.168.0.0-192.168.255.255   
        当然，还有127这个网段是环回地址   
        **/   
        long aBegin = getIpNum("10.0.0.0");    
        long aEnd = getIpNum("10.255.255.255");    
        long bBegin = getIpNum("172.16.0.0");    
        long bEnd = getIpNum("172.31.255.255");    
        long cBegin = getIpNum("192.168.0.0");    
        long cEnd = getIpNum("192.168.255.255");    
        isInnerIp = isInner(ipNum,aBegin,aEnd) || isInner(ipNum,bBegin,bEnd) || isInner(ipNum,cBegin,cEnd) ;    
        return isInnerIp; 
	
	}   
	
	private static long getIpNum(String ipAddress) {    
	    String [] ip = ipAddress.split("\\.");    
	    long a = Integer.parseInt(ip[0]);    
	    long b = Integer.parseInt(ip[1]);    
	    long c = Integer.parseInt(ip[2]);    
	    long d = Integer.parseInt(ip[3]);    
	   
	    long ipNum = a * 256 * 256 * 256 + b * 256 * 256 + c * 256 + d;    
	    return ipNum;    
	}   
	
	private static boolean isInner(long userIp,long begin,long end){    
	     return (userIp>=begin) && (userIp<=end);    
	}

}
