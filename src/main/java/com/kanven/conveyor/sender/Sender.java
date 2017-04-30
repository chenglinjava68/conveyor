package com.kanven.conveyor.sender;

import com.kanven.conveyor.collector.Observer;
import com.kanven.conveyor.entity.RecordProto.Record;

/**
 * 
 * @author kanven
 *
 */
public interface Sender<T> {
	
	void attach(Observer<T> observer);
	
	void detach(Observer<T> observer);
	
	void notify(T subject);

	void send(Record record);

	void close();

}
