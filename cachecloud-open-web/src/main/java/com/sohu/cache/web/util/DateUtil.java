package com.sohu.cache.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author leifu
 * @Time 2014年8月31日
 */
public class DateUtil {
    private final static Logger logger = LoggerFactory.getLogger(DateUtil.class);
    
    /*
     * yyyyMMddHHmm格式format
     */
    public static String formatYYYYMMddHHMM(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        return sdf.format(date);
    }

    /*
     * yyyy-MM-dd HH:mm:ss格式format
     */
    public static String formatYYYYMMddHHMMSS(Date date) {
        if(date == null){
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static String formatYYYY_MM_dd(Date date) {
        if(date == null){
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }


    /*
     * yyyyMMddHHmm格式parse
     */
    public static Date parse(String dateStr, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(dateStr);
    }
    
    /*
     * yyyyMMddHHmm格式parse
     */
    public static Date parseYYYYMMddHHMM(String dateStr) throws ParseException {
        return parse(dateStr, "yyyyMMddHHmm");
    }

    /**
     * yyyyMMddHH格式parse
     *
     * @throws ParseException
     */
    public static Date parseYYYYMMddHH(String dateStr) throws ParseException {
        return parse(dateStr, "yyyyMMddHH");
    }


    /*
     * yyyy-MM-dd格式parse
     */
    public static Date parseYYYY_MM_dd(String dateStr) throws ParseException {
        return parse(dateStr, "yyyy-MM-dd");
    }

    /**
     * yyyyMMdd格式parse
     */
    public static Date parseYYYYMMdd(String dateStr) throws ParseException {
        return parse(dateStr, "yyyyMMdd");
    }


    public static Date getDateByFormat(String date, String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        Date result = null;
        try {
            result = sf.parse(date);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public static String formatDate(Date date, String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        return sf.format(date);
    }


    public static String formatYYYYMMdd(Date date) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        return sf.format(date);
    }

    public static String formatHHMM(Date date) {
        SimpleDateFormat sf = new SimpleDateFormat("HHmm");
        return sf.format(date);
    }

    public static Date getStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime();
    }

    public static Date getEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime();
    }
}
