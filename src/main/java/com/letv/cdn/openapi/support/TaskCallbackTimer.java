package com.letv.cdn.openapi.support;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.letv.cdn.openapi.pojo.PreDistParam;
import com.letv.cdn.openapi.service.TaskCallbackService;
import com.letv.cdn.openapi.utils.Env;

/**
 * 定时回调任务，用于处理未自动回调的任务
 * <br>
 * 2015年4月28日
 * @author gao.jun
 *
 */
@Component
public class TaskCallbackTimer {
	
	private static final Logger log = LoggerFactory.getLogger(TaskCallbackTimer.class);
	
	private static final String ZK_ADDRESS = Env.get("zk_server_address");
	
	@Resource
	TaskCallbackService callbackService;
	
	private InterProcessSemaphoreMutex centerLock;
	
	private InterProcessSemaphoreMutex globalLock;
	
	private Thread centerDistCallbackThead;
	
	private Thread globalDistCallbackThead;
	
	@PostConstruct
	public void init() throws Exception {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(100, 3);
		CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, retryPolicy);
		client.start();
		if(client.checkExists().forPath("/openapi") == null) {
			client.create().forPath("/openapi");
		}
		centerLock = new InterProcessSemaphoreMutex(client, "/openapi/task-center-callback-lock");
		globalLock = new InterProcessSemaphoreMutex(client, "/openapi/task-global-callback-lock");
		centerDistCallbackThead = new Thread(new CallbackTask(callbackService, centerLock));
		centerDistCallbackThead.start();
		globalDistCallbackThead = new Thread(new CallbackTask(callbackService, globalLock, PreDistParam.CALLBACK_MODE_GLOBAL));
		globalDistCallbackThead.start();
		log.info("Callback task init...");
	}
	
	@PreDestroy
	public void destory() throws Exception {
		centerLock.release();
		centerDistCallbackThead.interrupt();
		globalLock.release();
		globalDistCallbackThead.interrupt();
		log.info("Callback task destroy...");
	}
	
	/**
	 * 提交任务
	 * <br>
	 * 2015年4月20日
	 * @author gao.jun
	 *
	 */
	private static class CallbackTask implements Runnable {
		
		private TaskCallbackService callbackService;
		
		private InterProcessSemaphoreMutex lock;
		
		private byte mode = PreDistParam.CALLBACK_MODE_CENTER;
		
		public CallbackTask(TaskCallbackService callbackService, InterProcessSemaphoreMutex lock) {
			this.lock = lock;
			this.callbackService = callbackService;
		}
		
		public CallbackTask(TaskCallbackService callbackService, InterProcessSemaphoreMutex lock, byte mode) {
			this.lock = lock;
			this.callbackService = callbackService;
			this.mode = mode;
		}

		@Override
		public void run() {
			try {
				lock.acquire();
				while(true) {
					try {
						log.info("Callback task start...");
						long now = System.currentTimeMillis();
						if(mode == PreDistParam.CALLBACK_MODE_CENTER) {
							callbackService.centerDistCallback();
						}else {
							callbackService.globalDistCallback();
						}
						log.info("Callback task finish,cost:{},task turn into sleep...", (System.currentTimeMillis() - now));
					} catch (Exception e) {
						log.error("Callback task failed...", e);
					} finally {
						// 间隔1分钟后再次执行任务
						Thread.sleep(60000);
					}
				}
			} catch (Exception e1) {
				log.error("Get ZK lock failed in callback task...", e1);
			}
		}
	}
}
