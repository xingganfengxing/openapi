package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.cache.MemcacheManager;
import com.letv.cdn.openapi.dao.openapi.ApiDomainMapper;
import com.letv.cdn.openapi.exception.OpenapiFailException;
import com.letv.cdn.openapi.pojo.ApiDomain;
import com.letv.cdn.openapi.pojo.ApiDomainExample;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * 内容刷新service
 * <br>
 * 2015年3月25日
 * @author gao.jun
 *
 */
@Service
public class CachedFileService {
	
	private static final Logger log = LoggerFactory.getLogger(CachedFileService.class);
	
	private static final String FORBID_DEL_FILE = "crossdomain.xml";
	
	/**
	 * 进度查询URL
	 * private static final String PROGRESS_QUEREY_URL = "http://localhost:8084/cdn/content/cachedfile/progress?key=";
	 */
	private static final String PROGRESS_QUEREY_URL = "http://api.cdn.lecloud.com/cdn/content/cachedfile/progress?key=";
	
	/**
	 * 进度查询URL缓存时间为1天
	 */
	private static final int CACHE_TIME = 86400;
	
	@Resource
    ApiDomainMapper apiDomainMapper;

	/**
	 * 校验src和domain是否一致
	 * 
	 */ 
	public boolean checkSrcURL(String domain,String[] srcs) {
		String preSrc = "http://".concat(domain);
		for(String src : srcs) {
			if(!StringUtils.startsWithIgnoreCase(src, preSrc)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 通过domaintag获取域名
	 * 
	 */
	public String getDomain(String domaintag) {
		String domain = null;
		ApiDomainExample domainExampel = new ApiDomainExample();
		domainExampel.createCriteria().andDomaintagEqualTo(domaintag);
		List<ApiDomain> domains = apiDomainMapper.selectByExample(domainExampel);
		if(!domains.isEmpty()) {
			domain = domains.get(0).getDomain();
		}
		return domain;
	}
	
	/**
	 * 处理刷新请求
	 * 
	 */ 
	public JSONObject processCachedFile(String body,ProcessInfo info) {
		JSONObject paramObj = JSONObject.parseObject(body);
		Object domaintagObj = paramObj.get("domaintag");
		Assert.isTrue(domaintagObj != null && StringUtils.isNotBlank(domaintagObj.toString()), "The parameter \"domaintag\" in the request body is not blank!");
		Object srcsObj = paramObj.get("srcs");
		Assert.notNull(srcsObj, "The parameter \"srcs\" in request body is not present");
		JSONArray srcJsonArray = filterFile(srcsObj);
		String[] srcs = srcJsonArray.toArray(new String[]{});
		Assert.isTrue(srcs != null && srcs.length != 0, "The parameter \"srcs\" in the request body is not a string array!");
		String domain = this.getDomain(domaintagObj.toString());
		Assert.isTrue(domain != null, "The domaintag in the request body is not exist!");
		Assert.isTrue(this.checkSrcURL(domain, srcs), "The domaintag in the request body doesn't match with src!");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("files", StringUtils.join(srcs, "||"));
		String progressURL = null;
		try {
			progressURL = HttpClientUtil.post(info.processURL, params, HttpClientUtil.UTF_8);
		} catch (IOException e) {
			String failMsgStr = "Invoke '".concat(info.cdnInterfaceDesc).concat("' failed");
			log.error(failMsgStr.concat(",parameters: {}"), params.toString());
			throw new OpenapiFailException(failMsgStr, e);
		}
		String key = null;
		if(progressURL != null) {
			JSONObject urlObj = JSONObject.parseObject(progressURL);
			if(urlObj.getString("progress_url") != null) {
				// 使用UUID做为key将进度查询URL存入memcached缓存
				key = UUID.randomUUID().toString().replace("-", "");
				String url = urlObj.getString("progress_url");
				log.info("Cached '".concat(info.interfaceDesc).concat("' progress query URL: {}, key: {}"), url, key);
				MemcacheManager.saveToCache(key, CACHE_TIME, url);
			}
		}else {
			throw new OpenapiFailException("Invoked '".concat(info.cdnInterfaceDesc).concat("' cann't get result"));
		}
		
		JSONObject jsonObj = new JSONObject();
		JSONArray linksJsonArray = new JSONArray();
		JSONObject linkJsonObj = new JSONObject();
		linkJsonObj.put("href", PROGRESS_QUEREY_URL.concat(key));
		linkJsonObj.put("rel", "progress");
		linkJsonObj.put("method", "GET");
		linksJsonArray.add(linkJsonObj);
		jsonObj.put("links", linksJsonArray);
		return jsonObj;
	}
	
	/**
	 * 过滤刷新文件中的crossdomain.xml
	 * 
	 */ 
	private JSONArray filterFile(Object srcsObj) {
		JSONArray srcJsonArray = (JSONArray)srcsObj;
		Iterator<Object> it = srcJsonArray.iterator();
		Pattern p = Pattern.compile("http://.*/(.*)");
		Matcher m = null;
		while(it.hasNext()) {
			String src = it.next().toString();
			m = p.matcher(src);
			if(m.matches() && m.group(1).contains(FORBID_DEL_FILE)) {
				it.remove();
			}
			m.reset();
		}
		return srcJsonArray;
	}
	
	public static class ProcessInfo {
		
		public String processURL;
		
		public String cdnInterfaceDesc;
		
		public String interfaceDesc;
		
		public ProcessInfo(String processURL,String cdnInterfaceDesc,String interfaceDesc){
			this.processURL = processURL;
			this.cdnInterfaceDesc = cdnInterfaceDesc;
			this.interfaceDesc = interfaceDesc;
		}
	}
}
