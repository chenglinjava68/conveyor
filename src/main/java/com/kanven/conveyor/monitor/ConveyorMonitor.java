package com.kanven.conveyor.monitor;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.kanven.conveyor.utils.NetworkUtils;

public class ConveyorMonitor implements Monitor {

	private static final Logger log = LoggerFactory.getLogger(ConveyorMonitor.class);

	private Report report;

	@Inject
	public ConveyorMonitor(Report report) {
		this.report = report;
	}

	@Override
	public void error(String message) {
		try {
			String content = MessageFormat.format("IP地址：{0}\r\n;消息体:{1}", NetworkUtils.getHostAddress().getHostAddress(),
					message);
			report.send("【CONVEYOR】数据同异常报警", content);
		} catch (UnsupportedEncodingException e) {
			log.error(MessageFormat.format("数据同步异常，内容为：{0}", message), e);
		} catch (MessagingException e) {
			log.error(MessageFormat.format("数据同步异常，内容为：{0}", message), e);
		}
	}

}
