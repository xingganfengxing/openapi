package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import com.letv.cdn.openapi.dao.openapi.PreDistParamMapper;
import com.letv.cdn.openapi.dao.openapi.PreDistTaskMapper;
import com.letv.cdn.openapi.pojo.PreDistParam;
import com.letv.cdn.openapi.pojo.PreDistParamExample;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.utils.CDNHelper;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * 进行任务回调的service
 *
 * @author gao.jun 
 * @date 2015年6月9日
 *
 */
@Service
public class TaskCallbackService {
	
	private static final Logger log = LoggerFactory.getLogger(TaskCallbackService.class);
	
	@Resource
	PreDistParamMapper paramMapper;
	
	@Resource
	PreDistTaskMapper taskMapper;
	
	/**
	 * 中心节点分发回调
	 * 2015年6月12日<br>
	 * @author gao.jun
	 */
	public void centerDistCallback() {
		log.info("Center distrubition callback start...");
		Stopwatch watch = new Stopwatch();
		watch.start();
		PreDistParamExample paramExample = new PreDistParamExample();
		paramExample.createCriteria().andCallbackIsNotNull().andCallbackNotEqualTo("")
		.andCallbackModeEqualTo(PreDistParam.CALLBACK_MODE_CENTER); 
		Map<String,String> appkeys = new HashMap<String,String>();
		for(PreDistParam param : paramMapper.selectByExample(paramExample)) {
			String appkey = param.getAppkey();
			String callback = param.getCallback();
			if(StringUtils.isNotBlank(appkey) && StringUtils.isNotBlank(callback)) {
				appkeys.put(appkey,callback);
			}
		}
		if(!appkeys.isEmpty()) {
			PreDistTaskExample example = new PreDistTaskExample();
			example.createCriteria().andAppkeyIn(new ArrayList<String>(appkeys.keySet())) // appkey条件
				// 状态条件
				.andStatusIn(Arrays.asList(PreDistTask.STATUS_SUCCESS, PreDistTask.STATUS_FAILURE, 
						PreDistTask.STATUS_FAILURE_RETRY, PreDistTask.STATUS_SRC_REPEAT))
				// 未进行客户回到的任务
				.andClientCallbackedEqualTo(PreDistTask.CLIENT_CALLBACK_NONE);
			example.setLimitValue1(0);
			// 一次最多提交100个任务
			example.setLimitValue2(100);
			// 按创建时间排序
			example.setOrderByClause("create_time");
			List<Long> successCallbackTaskIds = new ArrayList<Long>();
			for(PreDistTask task : taskMapper.selectByExample(example)) {
				String url = appkeys.get(task.getAppkey());
				try {
					String result = clientCallback(url, StringUtils.substringAfter(task.getOutkey(), "_"), task.getStatus(), task.getSrc());
					if(result != null) {
						successCallbackTaskIds.add(task.getId());
					}
				} catch (IOException e) {
					log.error("Task centeral distribution callback failed,url:{},outkey:{}", new Object[]{url, task.getOutkey(), e});
				}
			}
			if(!successCallbackTaskIds.isEmpty()) {
				example.clear();
				example.createCriteria().andIdIn(successCallbackTaskIds);
				PreDistTask rec = new PreDistTask();
				rec.setClientCallbacked(PreDistTask.CLIENT_CALLBACK_CENTER);
				rec.setClientCallbackTime(new Date());
				taskMapper.updateByExampleSelective(rec, example);
			}
		}
		log.info("Center distrubition callback finish,cost:{}...", watch.elapsedMillis());
	}
	
	/**
	 * 全网分发回调
	 * 2015年6月12日<br>
	 * @author gao.jun
	 */
	public void globalDistCallback() {
		log.info("Global distrubition callback start...");
		Stopwatch watch = new Stopwatch();
		watch.start();
		PreDistParamExample paramExample = new PreDistParamExample();
		paramExample.createCriteria().andCallbackIsNotNull().andCallbackNotEqualTo("").andCallbackModeEqualTo(PreDistParam.CALLBACK_MODE_GLOBAL);
		Map<String,String> appkeys = new HashMap<String,String>();
		for(PreDistParam param : paramMapper.selectByExample(paramExample)) {
			String appkey = param.getAppkey();
			String callback = param.getCallback();
			if(StringUtils.isNotBlank(appkey) && StringUtils.isNotBlank(callback)) {
				appkeys.put(appkey,callback);
			}
		}
		if(!appkeys.isEmpty()) {
			PreDistTaskExample example = new PreDistTaskExample();
			example.createCriteria().andAppkeyIn(new ArrayList<String>(appkeys.keySet())) // appkey条件
				// 状态条件为分发成功的状态
				.andStatusEqualTo(PreDistTask.STATUS_SUCCESS)
				// 已进行中心节点分发完成回调
				.andClientCallbackedEqualTo(PreDistTask.CLIENT_CALLBACK_CENTER);
			example.setLimitValue1(0);
			// 一次最多提交100个任务
			example.setLimitValue2(100);
			// 按创建时间排序
			example.setOrderByClause("create_time");
			List<Long> successCallbackTaskIds = new ArrayList<Long>();
			for(PreDistTask task : taskMapper.selectByExample(example)) {
				String storePath = task.getStorePath();
				if(StringUtils.isNotBlank(storePath)) {
					Integer fileCopy = null;
					try {
						fileCopy = CDNHelper.getDistFileCopyNum(storePath);
					} catch (IOException e1) {
						log.error("Fetch CDN distributed progress failed,storepath:{}", storePath, e1);
					}
					// 当filecopy达到50时进行全网分发客户回调
					if(fileCopy != null && fileCopy >= 50) {
						String url = appkeys.get(task.getAppkey());
						try {
							String result = clientCallback(url, StringUtils.substringAfter(task.getOutkey(), "_"), PreDistTask.STATUS_GLOBAL_DIST, 
									task.getSrc());
							if(result != null) {
								successCallbackTaskIds.add(task.getId());
							}
						} catch (IOException e) {
							log.error("Task global distribution callback failed, url:{}, outkey:{}", new Object[]{url, task.getOutkey(), e});
						}
					}
				}
			}
			if(!successCallbackTaskIds.isEmpty()) {
				example.clear();
				example.createCriteria().andIdIn(successCallbackTaskIds);
				PreDistTask rec = new PreDistTask();
				rec.setClientCallbacked(PreDistTask.CLIENT_CALLBACK_GLOBAL);
				rec.setClientCallbackTime(new Date());
				taskMapper.updateByExampleSelective(rec, example);
			}
		}
		log.info("Global distrubition callback finish,cost:{}...", watch.elapsedMillis());
	}
	
	public String clientCallback(String url, String key, byte status, String src) throws IOException {
		JSONObject result = new JSONObject();
		result.put("version", "0.3");
		JSONArray jsonArray = new JSONArray();
		JSONObject obj = new JSONObject();
		obj.put("key", key);
		obj.put("status", status);
		obj.put("src", src);
		jsonArray.add(obj);
		result.put("preloadedfiles", jsonArray);
		return HttpClientUtil.postBodyWithOutParams(url, result.toJSONString(), HttpClientUtil.UTF_8);
	}
}
