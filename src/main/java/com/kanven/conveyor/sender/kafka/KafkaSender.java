package com.kanven.conveyor.sender.kafka;

import java.io.IOException;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.kanven.conveyor.entity.RowEntityProto.RowEntity;
import com.kanven.conveyor.sender.Sender;
import com.kanven.conveyor.utils.PropertiesLoader;

/**
 * 
 * @author kanven
 *
 */
public class KafkaSender implements Sender {

	private static final String DEFAULT_KAFKA_CONF_PATH = "conf/kafka.properties";

	private Producer<String, byte[]> producer;

	private String topic;

	public KafkaSender(String topic) throws IOException {
		this.topic = topic;
		this.producer = new KafkaProducer<String, byte[]>(
				PropertiesLoader.loadProperties(DEFAULT_KAFKA_CONF_PATH));
	}

	public KafkaSender(String topic, Properties properties) {
		this.topic = topic;
		this.producer = new KafkaProducer<String, byte[]>(properties);
	}

	public void send(RowEntity entity) {
		ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(
				topic, entity.toByteArray());
		producer.send(record);
	}

	public void close() {
		producer.close();
	}

}
