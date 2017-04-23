package com.kanven.conveyor.sender;

import java.util.List;

import com.kanven.conveyor.entity.RowEntityProto.RowEntity;

/**
 * 
 * @author kanven
 *
 */
public interface Sender {
	
	void send(RowEntity entity);
	
	void send(List<RowEntity> entities);
	
	void close();
	
}
