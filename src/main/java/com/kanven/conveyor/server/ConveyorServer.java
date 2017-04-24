package com.kanven.conveyor.server;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.kanven.conveyor.collector.Collector;
import com.kanven.conveyor.utils.PropertiesLoader;
import com.kanven.conveyor.zk.Event;
import com.kanven.conveyor.zk.Register;
import com.kanven.conveyor.zk.ServerListener;

/**
 * 
 * @author kanven
 *
 */
public class ConveyorServer implements Server, ServerListener {

	private static final Logger log = LoggerFactory.getLogger(ConveyorServer.class);

	private static final String DEFAULT_CONVEYOR_CONF_PATH = "conf/conveyor.properties";

	private AtomicBoolean startup = new AtomicBoolean(false);

	private Collector collector;

	private String path;

	private Register register;

	@Inject
	public ConveyorServer(Collector collector, Register register) throws IOException {
		this.collector = collector;
		this.register = register;
		init();
	}

	public void start() {
		if (startup.get()) {
			log.info("服务已经启动...");
			return;
		}
		register.subscribe(path, this);
		startup.set(true);
	}

	private void init() throws IOException {
		Properties properties = PropertiesLoader.loadProperties(DEFAULT_CONVEYOR_CONF_PATH);
		String path = properties.getProperty("conveyor.zk.path");
		if (StringUtils.isBlank(path)) {
			throw new RuntimeException("没有指定zk注册根路径！");
		}
		this.path = path;
	}

	public void close() {
		if (!startup.get()) {
			log.info("服务已经关闭...");
			return;
		}
		collector.close();
		register.unsubscribe(path, this);
		startup.set(false);
	}

	private String seq;

	public void onExpired(Event event) {
		if (path.equals(event.getParent())) {
			if (log.isWarnEnabled()) {
				log.warn("服务zk连接过期！");
			}
			seq = null;
			collector.stop();
		}
	}

	public void onMaster(Event event) {
		if (event.getParent().equals(path) && event.getSeq().equals(seq)) {
			if (log.isInfoEnabled()) {
				log.info("服务被选举为主服务！");
			}
			collector.start();
		}
	}

	public void onCreated(Event event) {
		if (event.getParent().equals(path)) {
			this.seq = event.getSeq();
			if (log.isInfoEnabled()) {
				log.info("服务序号为：" + this.seq);
			}
		}
	}

}
