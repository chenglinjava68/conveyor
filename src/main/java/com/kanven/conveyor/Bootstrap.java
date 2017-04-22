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

	public static void main(String[] args) {
		final CountDownLatch latch = new CountDownLatch(1);
		try {
			log.info("模块初始化...");
			Setting setting = new Setting();
			Injector injector = Guice.createInjector(new ConveyorModule(setting));
			log.info("模块初始化完成...");
			final Server server = injector.getInstance(Server.class);
			log.info("服务开始启动...");
			server.start();
			log.info("服务完成启动...");
			Runtime.getRuntime().addShutdownHook(new Thread("Thread-conveyor") {
				@Override
				public void run() {
					try {
						log.info("服务开始关闭...");
						server.close();
						log.info("服务关闭...");
					} catch (Exception e) {
						log.error("服务关闭出现异常！", e);
					} finally {
						latch.countDown();
					}
				}
			});

		} catch (Exception e) {
			log.error("服务出现异常！", e);
			latch.countDown();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.error("服务启动失败！", e);
		}
	}

}
