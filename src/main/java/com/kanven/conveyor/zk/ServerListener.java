package com.kanven.conveyor.zk;

/**
 * 
 * @author kanven
 *
 */
public interface ServerListener {

	void onExpired(Event event);

	void onMaster(Event event);

	void onCreated(Event event);

}
