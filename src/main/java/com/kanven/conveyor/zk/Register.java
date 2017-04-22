package com.kanven.conveyor.zk;

/**
 * 
 * @author kanven
 *
 */
public interface Register {

	void subscribe(String path, ServerListener listener);

	void unsubscribe(String path, ServerListener listener);

}
