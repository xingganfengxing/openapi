package com.letv.cdn.openapi.support;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.letv.cdn.openapi.cache.MemcacheManager;
import com.letv.cdn.openapi.dao.openapi.PreDistTaskMapper;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.service.DomainService;
import com.letv.cdn.openapi.service.PreDistTaskService;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * 定时重新提交未回调的任务
 * <br>
 * 2015年4月20日
 * @author gao.jun
 *
 */
@Component
public class SubmitUnCallbackTaskTimer {
	
	private static final Logger log = LoggerFactory.getLogger(SubmitUnCallbackTaskTimer.class);
	
	private static final String ZK_ADDRESS = Env.get("zk_server_address");
	
	private InterProcessSemaphoreMutex lock;
	
	@Resource
	PreDistTaskMapper taskMapper;
	
	@Resource
	DomainService coopDomainService;
	
	@Resource
	PreDistTaskService pdts;
	
	private Thread thead;
	
	@PostConstruct
	public void init() throws Exception {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(100, 3);
		CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, retryPolicy);
		client.start();
		if(client.checkExists().forPath("/openapi") == null) {
			client.create().forPath("/openapi");
		}
		lock = new InterProcessSemaphoreMutex(client, "/openapi/redist-task-lock");
		thead = new Thread(new FeedbackTask(taskMapper,pdts,coopDomainService, lock));
		thead.start();
		log.info("Re-submit task init...");
	}
	
	@PreDestroy
	public void destory() throws Exception {
		lock.release();
		thead.interrupt();
		log.info("Re-submit task destroy...");
	}
	
	/**
	 * 重新提交任务
	 * <br>
	 * 2015年4月20日
	 * @author gao.jun
	 *
	 */
	private static class FeedbackTask implements Runnable {
		
		/**
		 * 一小时对应的毫秒数
		 */
		private static final int HOUR_MILLISECONDS = 3600000;
		
		/**
		 * 三天对应的毫秒数
		 */
		private static final int DAY_MILLISECONDS = 259200000;
		
		private static final int DELAY = 1800000;

		PreDistTaskMapper taskMapper;
		
		DomainService coopDomainService;
		
		@Resource
		PreDistTaskService pdts;
		
		private InterProcessSemaphoreMutex lock;
		
		public FeedbackTask(PreDistTaskMapper taskMapper,PreDistTaskService pdts, DomainService coopDomainService, InterProcessSemaphoreMutex lock) {
			this.taskMapper = taskMapper;
			this.pdts = pdts;
			this.coopDomainService = coopDomainService;
			this.lock = lock;
		}

		@Override
		public void run() {
			try {
				lock.acquire();
				while(true) {
					try {
						log.info("Re-submit task start...");
						PreDistTaskExample example = new PreDistTaskExample();
						long now = System.currentTimeMillis();
						example.createCriteria().andStatusIn(Arrays.asList((byte)2,(byte)3)).andCreateTimeBetween(now - DAY_MILLISECONDS, now - HOUR_MILLISECONDS);
						List<PreDistTask> feedbackTasks = taskMapper.selectByExample(example);
						if(feedbackTasks != null && !feedbackTasks.isEmpty()) {
							Map<String,CoopDomain> domainCache = new HashMap<String,CoopDomain>();
							for(PreDistTask task : feedbackTasks) {
								try {
                                    CoopDomain coopDomain = null;
                                    if(domainCache.containsKey(task.getDomaintag())) {
                                    	coopDomain = domainCache.get(task.getDomaintag());
                                    }else {
                                    	coopDomain = coopDomainService.selectByUseridAndDomaintag(task.getUserid().toString(), task.getDomaintag());
                                    	domainCache.put(task.getDomaintag(), coopDomain);
                                    }
                                    if(StringUtils.isNotBlank(task.getAppkey())) {
                                    	log.info("Distributing task in timer,outkey:{}", task.getOutkey());
                                    	String userid = task.getUserid().toString();
                                    	// 如果存在version信息，则增加version参数
                                    	Object versionObj = MemcacheManager.getFromCache(task.getOutkey().concat("_version"));
                                    	if(versionObj != null) {
                                    		task.setVersion(Long.valueOf(versionObj.toString()));
                                    	} 
                                    	String tmpResult = HttpClientUtil.postXml(Env.get("submit_file_uri"),
                                    			pdts.subParams(userid, InetAddress.getLocalHost().getHostAddress(), coopDomain.getSubDomain(), task.getAppkey(), 40),
                                    			pdts.generateSubmitXml(Arrays.asList(task),task.getDomaintag(), coopDomain.getSubDomain(), coopDomain.getSetHost()), HttpClientUtil.UTF_8);
                                    	if (tmpResult == null) {
                                    		log.error("Server exception happens when re-submit task submit task");
                                    	}
                                    	if ("403".equals(tmpResult)) {
                                    		log.info("Server IP isn't in white-list when re-submit task submit task");
                                    	}
                                    }
                                } catch (Exception e) {
                                    log.error("", e);
                                }
							}
						}
						log.info("Re-submit task finished,cost:{},task turn into sleep...", (System.currentTimeMillis() - now));
					} catch (Exception e) {
						log.error("Re-submit task failed...", e);
					} finally {
						// 间隔1分钟后再次执行任务提交
						Thread.sleep(DELAY);
					}
				}
			} catch (Exception e) {
				log.error("Get ZK lock failed in Re-submit task...",e);
			}
		}
	}
}
