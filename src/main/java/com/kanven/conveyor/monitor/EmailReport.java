package com.kanven.conveyor.monitor;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.name.Named;

/**
 * 邮件发送
 * 
 * @author kanven
 *
 */
public class EmailReport implements Report {

	private static final String DEFAULT_CHARSET = "UTF-8";

	@Named("conveyor.mail.protocol")
	private String protocol;

	private String host;

	private boolean auth = false;

	private String user;

	private String password;

	private Session session;

	private Transport transport;

	private List<InternetAddress> addresses;

	public EmailReport(String protocol, String host, String user, String password, List<InternetAddress> addresses)
			throws NoSuchProviderException {
		this.protocol = protocol;
		this.host = host;
		this.user = user;
		if (StringUtils.isBlank(password)) {
			this.auth = false;
		} else {
			this.auth = true;
			this.password = password;
		}
		this.addresses = addresses;
		init();
	}

	private void check() {
		if (StringUtils.isBlank(protocol)) {
			throw new IllegalArgumentException("没有指定邮箱协议类型！");
		}
		if (StringUtils.isBlank(host)) {
			throw new IllegalArgumentException("没有指定邮箱服务器地址！");
		}
		if (StringUtils.isBlank(user)) {
			throw new IllegalArgumentException("没有指定邮件发送人！");
		}
		if (addresses == null || addresses.size() == 0) {
			throw new IllegalArgumentException("没有指定邮件接收人");
		}
	}

	private void init() throws NoSuchProviderException {
		check();
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", protocol);
		props.setProperty("mail.smtp.host", host);
		props.setProperty("mail.smtp.auth", String.valueOf(auth));
		session = Session.getDefaultInstance(props);
		// session.setDebug(true); // 启动调试模式
		transport = session.getTransport();
	}

	public void send(String topic, String content) throws UnsupportedEncodingException, MessagingException {
		send(topic, content, addresses);
	}

	public void send(String topic, String content, List<InternetAddress> addresses)
			throws UnsupportedEncodingException, MessagingException {
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(user, user.split("@")[0], DEFAULT_CHARSET));
		message.setRecipients(MimeMessage.RecipientType.TO, addresses.toArray(new InternetAddress[] {}));
		message.setSubject(topic, DEFAULT_CHARSET);
		message.setContent(content, "text/html;charset=" + DEFAULT_CHARSET);
		message.setSentDate(new Date());
		message.saveChanges();
		if (auth) {
			transport.connect(user, password);
		} else {
			transport.connect();
		}
		transport.sendMessage(message, message.getAllRecipients());
	}

	public void close() throws MessagingException {
		transport.close();
	}

}
