package com.kanven.conveyor.module;

import java.util.ArrayList;
import java.util.List;

import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kanven.conveyor.monitor.EmailReport;

public class EmailReportProvider implements Provider<EmailReport> {

	private EmailReport sender;

	@Inject
	public EmailReportProvider(Setting setting) throws NoSuchProviderException, AddressException {
		String protocol = setting.get("conveyor.mail.protocol");
		String host = setting.get("conveyor.mail.host");
		String user = setting.get("conveyor.mail.user");
		String password = setting.get("conveyor.mail.password");
		String addrs = setting.get("conveyor.mail.addresses");
		String monitor = setting.get("conveyor.mail.monitor");
		if (StringUtils.isBlank(addrs)) {
			throw new IllegalArgumentException("没有指定邮件接收人！");
		}
		String[] addresses = addrs.split(",");
		List<InternetAddress> ids = new ArrayList<InternetAddress>();
		for (int i = 0, len = addresses.length; i < len; i++) {
			ids.add(new InternetAddress(addresses[i]));
		}
		boolean flag = false;
		if (StringUtils.isNotBlank(monitor) && "1".equals(monitor)) {
			flag = true;
		}
		sender = new EmailReport(protocol, host, user, password, ids, flag);
	}

	public EmailReport get() {
		return sender;
	}

}
