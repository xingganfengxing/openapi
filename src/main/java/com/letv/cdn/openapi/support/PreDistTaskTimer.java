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

import com.letv.cdn.openapi.service.ContentIIService;
import com.letv.cdn.openapi.utils.Env;

/**
 * 定时提交待提交的任务
 * <br>
 * 2015年4月28日
 * @author gao.jun
 *
 */
@Component
public class PreDistTaskTimer {
	
	private static final Logger log = LoggerFactory.getLogger(PreDistTaskTimer.class);
	
	private static final String ZK_ADDRESS = Env.get("zk_server_address");
	
	@Resource
	ContentIIService contentIIService;
	
	private InterProcessSemaphoreMutex lock;
	
	private Thread thead;
	
	@PostConstruct
	public void init() throws Exception {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(100, 3);
		CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, retryPolicy);
		client.start();
		if(client.checkExists().forPath("/openapi") == null) {
			client.create().forPath("/openapi");
		}
		lock = new InterProcessSemaphoreMutex(client, "/openapi/dist-task-lock");
		thead = new Thread(new SubmitTask(contentIIService, lock));
		thead.start();
		log.info("Submit task init...");
	}
	
	@PreDestroy
	public void destory() throws Exception {
		lock.release();
		thead.interrupt();
		log.info("Submit task destroy...");
	}
	
	/**
	 * 提交任务
	 * <br>
	 * 2015年4月20日
	 * @author gao.jun
	 *
	 */
	private static class SubmitTask implements Runnable {
		
		private ContentIIService contentService;
		
		private InterProcessSemaphoreMutex lock;
		
		public SubmitTask(ContentIIService contentService, InterProcessSemaphoreMutex lock) {
			this.lock = lock;
			this.contentService = contentService;
		}

		@Override
		public void run() {
				try {
					lock.acquire();
					while(true) {
						try {
							log.info("Submit task start...");
							long now = System.currentTimeMillis();
							contentService.submitTask();
							log.info("Submit task finish,cost:{},task turn into sleep...", (System.currentTimeMillis() - now));
						} catch (Exception e) {
							log.error("Submit task failed...", e);
						} finally {
							// 间隔10second后再次执行任务提交
							Thread.sleep(10000);
						}
					}
				} catch (Exception e1) {
					log.error("Get ZK lock failed in submit task...", e1);
				}
		}
	}
}
