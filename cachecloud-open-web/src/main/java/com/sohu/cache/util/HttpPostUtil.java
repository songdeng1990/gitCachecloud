package com.sohu.cache.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.sohu.cache.entity.MachineResponse;
import com.sohu.cache.entity.MachineResponseRaw;

/**
 * Created by jiguang on 2016/9/9.
 */
public class HttpPostUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpPostUtil.class);
    
    public static String sendHttpGetRequest(String url, String jsonString) {

        HttpURLConnection httpUrlConn = null;
        String response = null;
        try {
            URL e = new URL(url+ "?" +jsonString);
            httpUrlConn = (HttpURLConnection)e.openConnection();
            httpUrlConn.setDoOutput(true);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);
            httpUrlConn.setRequestMethod("GET");
            //httpUrlConn.setRequestProperty("Content-Type", "application/json;charset=utf-8");

            httpUrlConn.connect();
            InputStream in = httpUrlConn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer tempStr = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                tempStr.append(tempLine);
                tempStr.append(crlf);
                tempLine = rd.readLine();
            }
            response = tempStr.toString();
            rd.close();
            in.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (httpUrlConn != null) {
                httpUrlConn.disconnect();
            }
        }
        return response;
    }
    
    public static String sendHttpPostRequest(String url, String jsonString) {

        HttpURLConnection httpUrlConn = null;
        String response = null;
        try {
            URL e = new URL(url);
            httpUrlConn = (HttpURLConnection)e.openConnection();
            httpUrlConn.setDoOutput(true);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);
            httpUrlConn.setRequestMethod("POST");
            //httpUrlConn.setRequestProperty("Content-Type", "application/json;charset=utf-8");

            OutputStream statusCode = httpUrlConn.getOutputStream();
            statusCode.write(jsonString.getBytes("UTF-8"));
            statusCode.flush();
            statusCode.close();

            InputStream in = httpUrlConn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer tempStr = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                tempStr.append(tempLine);
                tempStr.append(crlf);
                tempLine = rd.readLine();
            }
            response = tempStr.toString();
            rd.close();
            in.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (httpUrlConn != null) {
                httpUrlConn.disconnect();
            }
        }
        return response;
    }
    
    public static String sendHttpJsonPostRequest(String url, String jsonString) {

        HttpURLConnection httpUrlConn = null;
        String response = null;
        try {
            URL e = new URL(url);
            httpUrlConn = (HttpURLConnection)e.openConnection();
            httpUrlConn.setDoOutput(true);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);
            httpUrlConn.setRequestMethod("POST");
            httpUrlConn.setRequestProperty("Content-Type", "application/json;charset=utf-8");

            OutputStream statusCode = httpUrlConn.getOutputStream();
            statusCode.write(jsonString.getBytes("UTF-8"));
            statusCode.flush();
            statusCode.close();

            InputStream in = httpUrlConn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer tempStr = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                tempStr.append(tempLine);
                tempStr.append(crlf);
                tempLine = rd.readLine();
            }
            response = tempStr.toString();
            rd.close();
            in.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (httpUrlConn != null) {
                httpUrlConn.disconnect();
            }
        }
        return response;
    }
   

}
