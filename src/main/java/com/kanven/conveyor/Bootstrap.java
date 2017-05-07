package com.kanven.conveyor;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kanven.conveyor.module.ConveyorModule;
import com.kanven.conveyor.module.Setting;
import com.kanven.conveyor.server.Server;

/**
 * 
 * @author kanven
 *
 */
public class Bootstrap {

	private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

	private CountDownLatch latch;

	private Server server;

	public Bootstrap(CountDownLatch latch) {
		this.latch = latch;
	}

	void start() {
		try {
			log.info("模块初始化...");
			Setting setting = new Setting();
			Injector injector = Guice.createInjector(new ConveyorModule(setting));
			log.info("模块初始化完成...");
			server = injector.getInstance(Server.class);
			server.setCountDownLatch(latch);
			log.info("服务开始启动...");
			server.start();
			log.info("服务完成启动...");
			Runtime.getRuntime().addShutdownHook(new Thread("Thread-conveyor") {
				@Override
				public void run() {
					close();
					latch.countDown();
				}
			});
		} catch (Exception e) {
			log.error("服务出现异常！", e);
			latch.countDown();
		}
	}

	void close() {
		try {
			if (server != null) {
				log.info("服务开始关闭...");
				server.close();
				log.info("服务关闭...");
			}
		} catch (Exception e) {
			log.error("服务关闭出现异常！", e);
		}
	}

	public static void main(String[] args) {
		CountDownLatch latch = new CountDownLatch(1);
		Bootstrap bootstrap = new Bootstrap(latch);
		bootstrap.start();
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.error("服务启动失败！", e);
		}
		bootstrap.close();
	}

}
