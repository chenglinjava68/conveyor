package com.kanven.conveyor.module;

import java.util.ArrayList;
import java.util.List;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Provider;
import com.kanven.conveyor.monitor.EmailSender;

public class EmailSenderProvider implements Provider<EmailSender> {

	private EmailSender sender;

	public EmailSenderProvider(Setting setting) throws NoSuchProviderException, AddressException {
		String protocol = setting.get("conveyor.mail.protocol");
		String host = setting.get("conveyor.mail.host");
		String user = setting.get("conveyor.mail.user");
		String password = setting.get("conveyor.mail.password");
		String addrs = setting.get("conveyor.mail.addresses");
		if (StringUtils.isBlank(addrs)) {
			throw new IllegalArgumentException("没有指定邮件接收人！");
		}
		String[] addresses = addrs.split(",");
		List<InternetAddress> ids = new ArrayList<InternetAddress>();
		for (int i = 0, len = addresses.length; i < len; i++) {
			ids.add(new InternetAddress(addresses[i]));
		}
		sender = new EmailSender(protocol, host, user, password, ids);
	}

	public EmailSender get() {
		return sender;
	}

}
