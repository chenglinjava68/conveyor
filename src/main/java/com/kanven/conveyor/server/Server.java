package com.kanven.conveyor.server;

import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author kanven
 *
 */
public interface Server {

	void start();

	void close();

	void setCountDownLatch(CountDownLatch latch);

}
