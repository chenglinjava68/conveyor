package com.kanven.conveyor.collector;

import java.util.concurrent.CountDownLatch;

/**
 * 收集器
 * 
 * @author kanven
 *
 */
public interface Collector {

	/**
	 * 数据收集启动
	 */
	void start();

	/**
	 * 数据收集暂停
	 */
	void stop();

	/**
	 * 数据收集关闭
	 */
	void close();

	void setCountDownLatch(CountDownLatch latch);

}
