package com.kanven.conveyor.monitor;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

public interface Report {

	void send(String topic, String content) throws UnsupportedEncodingException, MessagingException;
	
	void send(String topic, String content, List<InternetAddress> addresses) throws UnsupportedEncodingException, MessagingException;
	
	void close() throws MessagingException;
	
}
