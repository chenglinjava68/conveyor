package com.kanven.conveyor.collector;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.client.impl.SimpleCanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.CanalEntry.Header;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.exception.CanalClientException;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.kanven.conveyor.entity.RecordProto.Record;
import com.kanven.conveyor.entity.RowEntityProto.RowEntity;
import com.kanven.conveyor.entity.RowEntityProto.RowEntity.Builder;
import com.kanven.conveyor.monitor.Monitor;
import com.kanven.conveyor.monitor.jmx.MBeanRegister;
import com.kanven.conveyor.monitor.jmx.bean.CollectorBean;
import com.kanven.conveyor.sender.Sender;
import com.kanven.conveyor.utils.PropertiesLoader;

/**
 * 
 * @author kanven
 *
 */
public class CanalCollector implements Collector, Observer<Record>, Runnable {

	private static final Logger log = LoggerFactory.getLogger(CanalCollector.class);

	private static final String DEFAULT_CANNEL_CONF_PATH = "conf/canal.properties";

	private Lock lock = new ReentrantLock();

	private CanalConnector connector;

	private Sender<Record> sender;

	private int batch = 1000;

	private long timeout = -1;

	private String regex;

	private Status status;

	private int retry = 6;

	private long batchId = -1;

	@Inject
	private Monitor monitor;

	private static enum Status {
		STARTED, RUNNING, STOPED, EXCEPTION, CLOSED
	}

	@Inject
	public CanalCollector(Sender<Record> sender) throws IOException {
		this.sender = sender;
		this.sender.attach(this);
		init();
	}

	private Message fetchMessage() {
		int times = 0;
		Message message = null;
		while (times <= retry) {
			try {
				message = connector.getWithoutAck(batch, timeout, TimeUnit.MILLISECONDS);
				break;
			} catch (CanalClientException e) {
				log.error("canal获取消息失败！", e);
				monitor.error("canal获取数据失败！");
				times++;
				try {
					Thread.sleep(3000 * times);
				} catch (InterruptedException ex) {
				}
				if (connector instanceof SimpleCanalConnector) {// simple模式兼容
					try {
						connector.disconnect();
						connector.connect();
					} catch (Exception ex) {
					}
				}
			}
		}
		return message;
	}

	public void run() {
		while (status == Status.RUNNING) {
			Message message = fetchMessage();
			if (message == null) {
				continue;
			}
			long id = message.getId();
			if (id <= 0) {
				continue;
			}
			this.batchId = id;
			List<Entry> entries = message.getEntries();
			if (entries == null || entries.size() == 0) {
				continue;
			}
			Record.Builder builder = Record.newBuilder();
			builder.setBatchId(id);
			for (Entry entry : entries) {
				EntryType type = entry.getEntryType();
				if (type != EntryType.ROWDATA) {
					continue;
				}
				parseEntry(entry, builder);
			}
			sender.send(builder.build());
		}
	}

	public void start() {
		lock.lock();
		try {
			if (log.isDebugEnabled()) {
				log.debug("收集器状态为:" + status);
			}
			if (status == Status.RUNNING) {
				log.warn("收集器已经被启动！");
			} else if (status == Status.STARTED) {
				if (log.isInfoEnabled()) {
					log.info("收集器开始启动...");
				}
				this.connector.connect();
				this.connector.subscribe(regex);
				this.connector.rollback();
				this.status = Status.RUNNING;
				Thread t = new Thread(this, "canal-collector-thread");
				t.setUncaughtExceptionHandler(new UncaughtExceptionMonitor());
				t.setDaemon(true);
				t.start();
				if (log.isInfoEnabled()) {
					log.info(MessageFormat.format("收集器启动完成，状态为：{0}", status));
				}
			} else if (status == Status.STOPED) {
				this.connector.subscribe(regex);
				this.connector.rollback();
				this.status = Status.RUNNING;
			} else if (status == Status.CLOSED) {
				throw new RuntimeException("收集器已经被关闭！");
			} else if (status == Status.EXCEPTION) {
				throw new RuntimeException("收集器处于异常状态！");
			}
		} finally {
			lock.unlock();
		}
	}

	public void stop() {
		lock.lock();
		try {
			if (status == Status.RUNNING) {
				this.status = Status.STOPED;
				this.connector.unsubscribe();
			}
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		lock.lock();
		try {
			if (this.status == Status.CLOSED) {
				if (log.isWarnEnabled()) {
					log.warn("收集器已经关闭！");
				}
				return;
			}
			this.status = Status.CLOSED;
			try {
				this.connector.disconnect();
			} catch (Exception e) {
				log.error("canal连接关闭出现异常！", e);
			}
			this.sender.close();
		} finally {
			lock.unlock();
		}
	}

	private void init() throws IOException {
		CollectorBean bean = new CollectorBean(this);
		MBeanRegister.getInstance().register(bean);
		Properties properties = PropertiesLoader.loadProperties(DEFAULT_CANNEL_CONF_PATH);
		String address = properties.getProperty("canal.address", "");
		if (StringUtils.isBlank(address)) {
			throw new IllegalArgumentException("没有指定cannal连接地址");
		}
		String destination = properties.getProperty("canal.destination", "");
		String username = properties.getProperty("canal.username", "");
		String password = properties.getProperty("canal.password", "");
		String batch = properties.getProperty("canal.batch");
		if (StringUtils.isNotBlank(batch)) {
			this.batch = Integer.parseInt(batch);
		}
		String timeout = properties.getProperty("canal.timeout", "-1");
		if (StringUtils.isNotBlank(timeout)) {
			this.timeout = Long.parseLong(timeout);
		}
		this.regex = properties.getProperty("canal.regex", ".*\\..*");
		List<InetSocketAddress> addresses = parseAddress(address);
		if (addresses.size() == 0) {
			throw new IllegalArgumentException("没有合法的连接地址可用！");
		}
		this.connector = addresses.size() == 1
				? CanalConnectors.newSingleConnector(addresses.get(0), destination, username, password)
				: CanalConnectors.newClusterConnector(addresses, destination, username, password);
		this.status = Status.STARTED;
	}

	private List<InetSocketAddress> parseAddress(String address) {
		List<InetSocketAddress> isas = new ArrayList<InetSocketAddress>();
		String[] addresses = address.split(",");
		for (String addr : addresses) {
			if (StringUtils.isBlank(addr)) {
				continue;
			}
			if (!addr.contains(":")) {
				log.error(MessageFormat.format("{0},地址不合法！", addr));
				continue;
			}
			String[] items = addr.split(":");
			if (items.length > 2) {
				log.error(MessageFormat.format("{0},地址不合法！", addr));
				continue;
			}
			String ip = items[0];
			String port = items[1];
			if (StringUtils.isBlank(ip)) {
				log.error(MessageFormat.format("{0},地址不合法！", addr));
				continue;
			}
			if (StringUtils.isBlank(port)) {
				log.error(MessageFormat.format("{0},地址不合法！", addr));
				continue;
			}
			InetSocketAddress isa = new InetSocketAddress(ip, Integer.parseInt(port));
			isas.add(isa);
		}
		return isas;
	}

	private void parseEntry(Entry entry, Record.Builder builder) {
		Header header = entry.getHeader();
		try {
			RowChange rc = RowChange.parseFrom(entry.getStoreValue());
			List<RowData> rds = rc.getRowDatasList();
			EventType event = rc.getEventType();
			switch (event) {
			case UPDATE:
			case INSERT:
				for (RowData rd : rds) {
					RowEntity entity = parseRow(header, rd.getAfterColumnsList());
					builder.addEntities(entity.toByteString());
				}
				break;
			case DELETE:
				for (RowData rd : rds) {
					RowEntity entity = parseRow(header, rd.getBeforeColumnsList());
					builder.addEntities(entity.toByteString());
				}
				break;
			default:
				break;
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}

	private RowEntity parseRow(Header header, List<Column> columns) {
		Builder builder = RowEntity.newBuilder();
		builder.setDb(header.getSchemaName());
		builder.setTable(header.getTableName());
		builder.setTime(header.getExecuteTime());
		for (Column column : columns) {
			String value = column.getValue();
			builder.putKvs(column.getName(), value);
			if (column.getIsKey()) {
				builder.setPrimaryKey(value);
			}
		}
		return builder.build();
	}

	private class UncaughtExceptionMonitor implements UncaughtExceptionHandler {
		public void uncaughtException(Thread t, Throwable e) {
			status = Status.EXCEPTION;
			String message = MessageFormat.format("线程（{0}）,收集器出现未知异常！", t.getName());
			log.error(message, e);
			try {
				connector.disconnect();
			} catch (Exception ex) {
				log.error("canal关闭连接出现异常！", e);
			}
			try {
				// TODO
				close();
			} catch (Exception ex) {
				log.error("服务关闭出现异常！", e);
			}
			monitor.error(message);
			CountDownLatch latch = CanalCollector.this.latch;
			if (latch != null) {
				latch.countDown();
			}
		}
	}

	public void observe(Record record) {
		long batchId = record.getBatchId();
		if (batchId > 0 && batchId <= this.batchId) {
			try {
				connector.ack(batchId);
				if (log.isInfoEnabled()) {
					log.info(MessageFormat.format("消息({0})确认成功！", batchId));
				}
			} catch (CanalClientException e) {
				String message = MessageFormat.format("{0}编号消息ack失败,记录为：{1}", batchId, record.toString());
				log.error(message, e);
				monitor.error(message);
			}
		} else {
			log.warn(MessageFormat.format("消息序号存在偏差,当前序号:{0}，确认序号:{1}", this.batchId, batchId));
		}
	}

	private CountDownLatch latch;

	@Override
	public void setCountDownLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public int getStatus() {
		if (status == null) {
			return -1;
		}
		return status.ordinal();
	}

	public long getBatchId() {
		return batchId;
	}

}
