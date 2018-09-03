package com.sohu.cache.web.component;

import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.util.HttpRequestUtil;
import com.sohu.cache.web.util.JsonUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手机短信报警
 * @author leifu
 * @Date 2014年11月26日
 * @Time 上午10:11:26
 */
public class MobileAlertComponentImpl implements MobileAlertComponent {

    private static final Logger logger = LoggerFactory.getLogger(MobileAlertComponentImpl.class);

    /**
     * 管理员电话
     */
    private String adminPhones = ConstUtils.PHONES;

    @Override
    public void sendPhoneToAdmin(String message) {
        if (StringUtils.isBlank(message)) {
            logger.error("message is {}, maybe empty or adminPhones is {}, maybe empty", message, adminPhones);
        }
        sendPhone(message, null);
    }

    @Override
    public void sendPhone(String message, List<String> phoneList) {
        String alertUrl = ConstUtils.MOBILE_ALERT_INTERFACE;
        if (StringUtils.isBlank(alertUrl)) {
            logger.error("mobileAlertInterface url is empty!");
            return;
        }

        if (isTimeNow(ConstUtils.DISALBE_ALARM_TIME)){
            logger.warn("Alarm is disabled at %s ,message won't be send : %s",ConstUtils.DISALBE_ALARM_TIME,message);
            return;
        }

        /*if (StringUtils.isBlank(message) || phoneList == null || phoneList.isEmpty()) {
            logger.error("message is {}, phoneList is {} both maybe empty!", message, phoneList);
            return;
        }*/
        //String charSet = "UTF-8";
        //String phone = StringUtils.join(phoneList, ConstUtils.COMMA);
        Map<String, Object> postMap = new HashMap<String, Object>();
        message = ConstUtils.DEFAULT_IDC_NAME + " " + message;
        postMap.put("desc", message);
        postMap.put("code", "151");
        String responseStr = HttpRequestUtil.doJsonPost(alertUrl, JsonUtil.toJson(postMap));
        if (StringUtils.isBlank(responseStr)) {
            logger.error("发送短信失败 : url:{}", alertUrl);
        }
        logger.warn("send Done!");
    }

    public static void main(String[] args) {
        Map<String, Object> postMap = new HashMap<String, Object>();
        postMap.put("desc", "This is a test messge,ignore please.");
        postMap.put("code", "151");
        String responseStr = HttpRequestUtil.doJsonPost("http://alert.jpushoa.com/v1/alert/", JsonUtil.toJson(postMap));
        System.out.println(responseStr);
    }

    public void setAdminPhones(String adminPhones) {
        this.adminPhones = adminPhones;
    }

    private static boolean isTimeNow(String time) {

        if (StringUtils.isEmpty(time)){
            return false;
        }

        try {
            String start = time.split("-")[0];
            String end = time.split("-")[1];
            SimpleDateFormat sp = new SimpleDateFormat("HH:mm");
            Date startTime = sp.parse(start);
            Date endTime = sp.parse(end);
            Date now = sp.parse(sp.format(new Date()));
            return now.getTime() >= startTime.getTime() && now.getTime() <= endTime.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
