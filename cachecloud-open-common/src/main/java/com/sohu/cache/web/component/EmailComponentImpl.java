package com.sohu.cache.web.component;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.util.MailUtil;

/**
 * 邮件服务
 * @author leifu
 * @Date 2015-6-2
 * @Time 上午10:56:35
 */
public class EmailComponentImpl implements EmailComponent {
    private final Logger logger = LoggerFactory.getLogger(EmailComponentImpl.class);

    private String adminEmail = ConstUtils.EMAILS;

    @Override
    public boolean sendMailToAdmin(String title, String content) {
        //return MailUtil.sendMail(title, content, Arrays.asList(adminEmail));
        return MailUtil.sendMail(title, content, Arrays.asList("dengsong@jpush.cn"));
    }

    @Override
    public boolean sendMail(String title, String content, List<String> emailList) {
        //return MailUtil.sendMail(title, content, emailList);
        return MailUtil.sendMail(title, content, Arrays.asList("dengsong@jpush.cn"));
    }
    
    @Override
	public boolean sendMail(String title, String content, List<String> emailList, List<String> ccList) {
       /* String alertUrl = ConstUtils.EMAIL_ALERT_INTERFACE;
        if (StringUtils.isBlank(alertUrl)) {
            logger.error("emailAlertInterface url is empty!");
            return false;
        }
    	try {
            String charSet = "UTF-8";
            Map<String, String> postMap = new HashMap<String, String>();
            postMap.put("title", title);
            postMap.put("content", content);
            postMap.put("receiver", StringUtils.join(emailList, ","));
            if(ccList != null && ccList.size() > 0){
            	postMap.put("cc", StringUtils.join(ccList, ","));
            }
            String responseStr = HttpRequestUtil.doPost(alertUrl, postMap, charSet);
            if (responseStr == null) {
                logger.error("发送邮件失败 : url:{}", alertUrl);
            }
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }*/
    	//emailList.addAll(ccList);
    	
    	//只是为了控制是否发送邮件 start
    	String alertUrl = ConstUtils.EMAIL_ALERT_INTERFACE;
        if (StringUtils.isBlank(alertUrl)) {
            logger.error("emailAlertInterface url is empty!");
            return true;
        }
        //只是为了控制发送邮件 end
        
        
    	return MailUtil.sendMail(title, content, Arrays.asList("dengsong@jpush.cn"));
	}

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

	@Override
	public String getAdminEmail() {
		return adminEmail;
	}	

}
