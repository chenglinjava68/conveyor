package com.kanven.conveyor.sender.kafka;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kanven.conveyor.collector.Observer;
import com.kanven.conveyor.entity.RecordProto.Record;
import com.kanven.conveyor.sender.Sender;
import com.kanven.conveyor.utils.Ping;
import com.kanven.conveyor.utils.PropertiesLoader;

/**
 * 
 * @author kanven
 * 
 */
public class KafkaSender implements Sender<Record> {

	private static final Logger log = LoggerFactory.getLogger(KafkaSender.class);

	private static final String DEFAULT_KAFKA_CONF_PATH = "conf/kafka.properties";

	private Set<RecordCallback> callbacks = new ConcurrentSkipListSet<RecordCallback>();

	private Producer<String, byte[]> producer;

	private String topic;

	public KafkaSender(String topic) throws IOException {
		this.topic = topic;
		Properties properties = PropertiesLoader.loadProperties(DEFAULT_KAFKA_CONF_PATH);
		String address = properties.getProperty("bootstrap.servers");
		if (!ping(address)) {
			throw new RuntimeException("kafka无法连接,请检查地址(" + address + ")是否有效!");
		}
		this.producer = new KafkaProducer<String, byte[]>(properties);
	}

	public KafkaSender(String topic, Properties properties) {
		this.topic = topic;
		this.producer = new KafkaProducer<String, byte[]>(properties);
	}

	public void send(Record record) {
		ProducerRecord<String, byte[]> message = new ProducerRecord<String, byte[]>(topic, record.toByteArray());
		try {
			producer.send(message, new RecordCallback(record));
			if (log.isInfoEnabled()) {
				log.info("消息体：" + record.toString());
			}
		} catch (Throwable t) {
			log.error("消息发送失败！", t);
		}
	}

	public void close() {
		while (callbacks.size() > 0) {
			if (log.isWarnEnabled()) {
				log.warn("还有（" + callbacks.size() + "）消息没有发送！");
				for (RecordCallback callback : callbacks) {
					log.warn(callback.record.toString());
				}
			}
			try {

				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		producer.close();
	}

	private boolean ping(String address) {
		if (StringUtils.isBlank(address)) {
			return false;
		}
		try {
			String[] hosts = address.split(",");
			for (String host : hosts) {
				String[] items = host.split(":");
				if (items == null || items.length != 2) {
					log.error("host地址(" + host + ")不合法！");
					return false;
				}
				String h = items[0];
				int port = Integer.parseInt(items[1]);
				boolean flag = Ping.ping(h, port);
				if (!flag) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			log.error("ping操作出现异常！", e);
			return false;
		}
	}

	private class RecordCallback implements Callback, Comparable<RecordCallback> {

		private Record record;

		public RecordCallback(Record record) {
			this.record = record;
			callbacks.add(this);
		}

		public void onCompletion(RecordMetadata metadata, Exception exception) {
			if (metadata == null) {
				log.error("消息发送失败,消息体：" + record.toString(), exception);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("消息发送成功,消息体:" + record.toString());
				}
			}
			KafkaSender.this.notify(record);
			callbacks.remove(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			return record.equals(obj);
		}

		@Override
		public int hashCode() {
			return record.hashCode();
		}

		public int compareTo(RecordCallback callback) {
			return this.record.getBatchId() >= callback.record.getBatchId() ? 1 : -1;
		}

	}

	private List<Observer<Record>> observers = new ArrayList<Observer<Record>>();

	@Override
	public void attach(Observer<Record> observer) {
		this.observers.add(observer);
	}

	@Override
	public void detach(Observer<Record> observer) {
		this.observers.remove(observer);
	}

	@Override
	public void notify(Record subject) {
		for (Observer<Record> observer : observers) {
			observer.observe(subject);
		}
	}

}
