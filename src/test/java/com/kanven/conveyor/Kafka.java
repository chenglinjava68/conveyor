package com.kanven.conveyor;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Kafka {

	public static void main(String[] args) {
		Map<String, Object> config = new HashMap<String, Object>();
		config.put("bootstrap.servers", "127.0.0.1:9092");
		config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = new KafkaProducer<String, String>(config);
		try{
		producer.send(new ProducerRecord<String, String>("topic", ""));
		}catch (Exception e) {
			System.out.println("====");
		}
	}
	
}
