package com.kanven.conveyor.sender;

import com.kanven.conveyor.entity.RowEntityProto.RowEntity;

/**
 * 
 * @author kanven
 *
 */
public interface Sender {
	
	void send(RowEntity entity);
	
	void close();
	
}
