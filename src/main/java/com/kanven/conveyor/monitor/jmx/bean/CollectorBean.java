package com.kanven.conveyor.monitor.jmx.bean;

import com.kanven.conveyor.collector.CanalCollector;

public class CollectorBean implements CollectorMXBean, MBeanInfo {

	private CanalCollector collector;

	public CollectorBean(CanalCollector collector) {
		this.collector = collector;
	}

	@Override
	public String getName() {
		return "collector";
	}

	@Override
	public int getState() {
		return collector.getStatus();
	}

	@Override
	public long getBatchId() {
		return collector.getBatchId();
	}

}
