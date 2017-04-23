package com.kanven.conveyor.sender.kafka;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kanven.conveyor.entity.RowEntityProto.RowEntity;
import com.kanven.conveyor.sender.Sender;
import com.kanven.conveyor.utils.PropertiesLoader;

/**
 * 
 * @author kanven
 *
 */
public class KafkaSender implements Sender {

	private static final Logger log = LoggerFactory.getLogger(KafkaSender.class);

	private static final String DEFAULT_KAFKA_CONF_PATH = "conf/kafka.properties";

	private ConcurrentSkipListSet<RecordCallback> callbacks = new ConcurrentSkipListSet<RecordCallback>();

	private Producer<String, byte[]> producer;

	private String topic;

	public KafkaSender(String topic) throws IOException {
		this.topic = topic;
		this.producer = new KafkaProducer<String, byte[]>(PropertiesLoader.loadProperties(DEFAULT_KAFKA_CONF_PATH));
	}

	public KafkaSender(String topic, Properties properties) {
		this.topic = topic;
		this.producer = new KafkaProducer<String, byte[]>(properties);
	}

	public void send(RowEntity entity) {
		ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(topic, entity.toByteArray());
		try {
			producer.send(record, new RecordCallback(entity));
			if (log.isInfoEnabled()) {
				log.info("消息体：" + entity.toString());
			}
		} catch (Throwable t) {
			log.error("消息发送失败！", t);
		}
	}

	@Override
	public void send(List<RowEntity> entities) {
		if (entities == null || entities.size() == 0) {
			return;
		}
		for (RowEntity entity : entities) {
			send(entity);
		}
	}

	public void close() {
		producer.close();
	}

	interface C<T> extends Callback, Comparable<T> {

	}

	private class RecordCallback implements Callback, Comparable<RecordCallback> {

		private RowEntity entity;

		public RecordCallback(RowEntity entity) {
			this.entity = entity;
			callbacks.add(this);
		}

		@Override
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			if (metadata == null) {
				log.error("消息发送失败,消息体：" + entity, exception);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("消息发送成功,消息体:" + entity);
				}
			}
			callbacks.remove(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			return entity.equals(obj);
		}

		@Override
		public int hashCode() {
			return entity.hashCode();
		}

		@Override
		public int compareTo(RecordCallback o) {
			return entity.getTime() >= o.entity.getTime() ? 1 : -1;
		}

	}

}
