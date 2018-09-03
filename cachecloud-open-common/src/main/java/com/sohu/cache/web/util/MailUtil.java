/**
 * 
 */
package com.sohu.cache.web.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.web.component.EmailComponentImpl;

/**
 * @author jiguang
 *
 */
public class MailUtil {
	
	 // 发件人电子邮箱
	private static String FROM = "alert@jpush.cn";
	
	// 邮箱服务器地址
	private static String HOST = "smtp.qiye.163.com";
	
	private static String USER = "alert@jpush.cn";
	
	private static String PWD = "VvLNJYDh2p8R";

	private static final Logger logger = LoggerFactory.getLogger(EmailComponentImpl.class);
	public static boolean sendMail(String title, String content, List<String> emailList) {
		
		/*String alertUrl = ConstUtils.EMAIL_ALERT_INTERFACE;
        if (StringUtils.isBlank(alertUrl)) {
            logger.error("emailAlertInterface url is empty!");
            return false;
        }
*/
		// 获取系统属性
		Properties properties = System.getProperties();

		//properties.setProperty("mail.debug", "true");
		
		// 设置邮件服务器
		properties.setProperty("mail.smtp.host", HOST);

		properties.setProperty("mail.smtp.auth", "true");

		properties.setProperty("mail.transport.protocol", "smtp");

		// 获取默认session对象
		Session session = Session.getDefaultInstance(properties);		

		try {
			// 创建默认的 MimeMessage 对象
			MimeMessage message = new MimeMessage(session);

			// Set From: 头部头字段
			message.setFrom(new InternetAddress(FROM));

			// Set Subject: 头部头字段
			message.setSubject(title);

			// 设置消息体
			message.setText(content);
			
			Transport sender = session.getTransport();
			sender.connect(USER, PWD);
			sender.sendMessage(message, toAddress(emailList));			
			return true;
		} catch (Exception mex) {
			mex.printStackTrace();
			return false;
		}
	}
	
	private static Address[] toAddress(List<String> emailList) throws AddressException
	{
		ArrayList<Address> address = new ArrayList<Address>();
		for (String str : emailList)
		{
			address.add(new InternetAddress(str));						
		}
		
		return address.toArray(new Address[0]);
	}

}
