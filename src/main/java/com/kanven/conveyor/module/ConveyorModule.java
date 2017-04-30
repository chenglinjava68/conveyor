package com.kanven.conveyor.module;

import java.io.IOException;

import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.kanven.conveyor.collector.CanalCollector;
import com.kanven.conveyor.collector.Collector;
import com.kanven.conveyor.entity.RecordProto.Record;
import com.kanven.conveyor.monitor.EmailSender;
import com.kanven.conveyor.sender.Sender;
import com.kanven.conveyor.sender.kafka.KafkaSender;
import com.kanven.conveyor.server.ConveyorServer;
import com.kanven.conveyor.server.Server;
import com.kanven.conveyor.zk.Register;
import com.kanven.conveyor.zk.ZookeeperRegister;

/**
 * 
 * @author kanven
 *
 */
public class ConveyorModule extends AbstractModule {

	private Setting setting;

	public ConveyorModule(Setting setting) {
		this.setting = setting;
	}

	@Override
	protected void configure() {
		bind(Server.class).to(ConveyorServer.class).asEagerSingleton();
		bind(Collector.class).to(CanalCollector.class).asEagerSingleton();
		try {
			bind(new TypeLiteral<Sender<Record>>() {
			}).toInstance(new KafkaSender(setting.get("conveyor.kafka.topic")));
		} catch (IOException e) {
			throw new RuntimeException("kafka初始化失败！", e);
		}
		bind(Register.class).to(ZookeeperRegister.class).asEagerSingleton();
		bind(ZkClient.class).toProvider(ZkClientProvider.class).asEagerSingleton();
		String em = setting.get("conveyor.mail.monitor");
		if (StringUtils.isNotBlank(em) && "1".equals(em)) {
			bind(EmailSender.class).toProvider(EmailSenderProvider.class).asEagerSingleton();
		}
	}

}
