package com.kanven.conveyor.module;

import java.io.IOException;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kanven.conveyor.utils.PropertiesLoader;

/**
 * 
 * @author kanven
 *
 */
public class ZkClientProvider implements Provider<ZkClient> {

	private static final Logger log = LoggerFactory.getLogger(ZkClientProvider.class);

	private static final String DEFAULT_ZK_CONF_PATH = "conf/zk.properties";

	private static final int DEFAULT_CONNECTION_TIMEOUT = 3000;

	private static final int DEFAULT_SESSION_TIMEOUT = 5000;

	private Properties properties;

	@Inject
	public ZkClientProvider() throws IOException {
		this.properties = PropertiesLoader.loadProperties(DEFAULT_ZK_CONF_PATH);
	}

	public ZkClient get() {
		String address = properties.getProperty("zk.addresses");
		if (StringUtils.isEmpty(address)) {
			throw new IllegalArgumentException("没有指定zk连接地址！");
		}
		if (log.isWarnEnabled()) {
			String[] items = address.split(",");
			if (items.length == 1) {
				log.warn("zk不是集群，建议使用集群！");
			}
		}
		String connectionTimeout = properties.getProperty("zk.connectionTimeout");
		String sessionTimeout = properties.getProperty("zk.sessionTimeout");
		if (log.isDebugEnabled()) {
			log.debug("zk启动连接...");
		}
		ZkClient client = new ZkClient(address,
				StringUtils.isBlank(sessionTimeout) ? DEFAULT_SESSION_TIMEOUT : Integer.parseInt(sessionTimeout),
				StringUtils.isBlank(connectionTimeout) ? DEFAULT_CONNECTION_TIMEOUT
						: Integer.parseInt(connectionTimeout));
		if (log.isDebugEnabled()) {
			log.debug("zk连接完成...");
		}
		return client;
	}
}
