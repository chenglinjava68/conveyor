package com.kanven.conveyor.monitor.jmx;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kanven.conveyor.monitor.jmx.bean.MBeanInfo;

/**
 * JMX 监控
 * 
 * @author kanven
 *
 */
public class MBeanRegister {

	private static final Logger log = LoggerFactory.getLogger(MBeanRegister.class);

	private static final String DOMAIN = "com.kanven.conveyor";

	private static final String DEAFULT_FORMAT = "{0}:type={1},name={2}";

	private MBeanServer beanServer;

	private MBeanRegister() {
		try {
			beanServer = ManagementFactory.getPlatformMBeanServer();
		} catch (Error e) {
			beanServer = MBeanServerFactory.createMBeanServer();
		}
	}

	public static MBeanRegister getInstance() {
		return MBeanRegisterHolder.INSTANCE;
	}

	private static class MBeanRegisterHolder {
		private static final MBeanRegister INSTANCE = new MBeanRegister();
	}

	public void register(MBeanInfo bean) {
		try {
			ObjectName name = createObjectName(bean);
			beanServer.registerMBean(bean, name);
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException e) {
			log.error(MessageFormat.format("MBean({0})注册失败！", bean.getName()), e);
		}
	}

	public void unregister(MBeanInfo bean) {
		try {
			ObjectName name = createObjectName(bean);
			beanServer.unregisterMBean(name);
		} catch (MalformedObjectNameException | MBeanRegistrationException | InstanceNotFoundException e) {
			log.error(MessageFormat.format("MBean({0})取消注册失败！", bean.getName()), e);
		}
	}

	private ObjectName createObjectName(MBeanInfo info) throws MalformedObjectNameException {
		String name = MessageFormat.format(DEAFULT_FORMAT, DOMAIN, info.getClass().getName(), info.getName());
		return new ObjectName(name);
	}

}
