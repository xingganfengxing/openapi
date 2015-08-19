package com.letv.cdn.openapi.controller.content;


import java.io.IOException;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.cache.MemcacheManager;
import com.letv.cdn.openapi.controller.BaseController;
import com.letv.cdn.openapi.exception.OpenapiFailException;
import com.letv.cdn.openapi.service.CachedFileService;
import com.letv.cdn.openapi.service.CachedFileService.ProcessInfo;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.utils.ErrorMsg;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * 内容管理中内容刷新相关接口
 * <br>
 * 2015年3月20日
 * @author gao.jun
 * @since 0.2
 */
@Controller
@RequestMapping("/cdn/content/cachedfile")
public class CachedFileController extends BaseController {
	
	private static final Logger log = LoggerFactory.getLogger(CachedFileController.class);
	
	/**
	 * CDN内容预取接口
	 */
	private static final String PRELOAD_FILE_URI = Env.get("cdn_postCachedfile");
	
	/**
	 * CDN内容刷新接口
	 */
	public static final String DELETE_CACHE_FILE_URI = Env.get("cdn_deleteCachedfile");
	
	/**
	 * CDN目录刷新接口
	 */
	private static final String DELETE_CACHE_DIR_URI = Env.get("cdn_deleteCachedDir");
	
	@Resource
	CachedFileService cachedFileService;
	
	/**
	 * 提交预取小文件
	 * <br>
	 * 2015年3月20日
	 * @author gao.jun
	 * @param body
	 * @param accept
	 * @return
	 */
	@OpenapiAuth
	@RequestMapping(method=RequestMethod.POST)
	public ResponseJson postCachedfile(HttpServletRequest request, @RequestBody String body,
            @RequestHeader(value = "Accept",required = false) String accept){
		if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
		log.info("postCachedfile, requestIP:{}, requestBody:{}", request.getRemoteAddr(), body);
		return ResponseJson.acceptedWithNoCache(cachedFileService.processCachedFile(body, 
				new ProcessInfo(PRELOAD_FILE_URI, "CDN file reload interface", "submit file reload")));
	}
	
	/**
	 * 提交小文件刷新
	 * <br>
	 * 2015年3月20日
	 * @author gao.jun
	 * @param body
	 * @param accept
	 * @return
	 */
	@OpenapiAuth
	@RequestMapping(method=RequestMethod.DELETE)
	public ResponseJson deleteCachedfile(HttpServletRequest request, @RequestBody String body,
            @RequestHeader(value = "Accept",required = false) String accept){
		if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
		log.info("deleteCachedfile, requestIP:{}, requestBody:{}", request.getRemoteAddr(), body);
		return ResponseJson.acceptedWithNoCache(cachedFileService.processCachedFile(body, new ProcessInfo(DELETE_CACHE_FILE_URI, "CDN file refresh interface", "submit file refresh")));
	}
	
	/**
	 * 提交目录刷新
	 * <br>
	 * 2015年3月20日
	 * @author gao.jun
	 * @param body
	 * @param accept
	 * @return
	 */
	@OpenapiAuth
	@RequestMapping(value="/directory",method=RequestMethod.DELETE)
	public ResponseJson deleteCachedDir(HttpServletRequest request, @RequestBody String body,
            @RequestHeader(value = "Accept",required = false) String accept){
		if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
		log.info("deleteCachedDir, requestIP:{}, requestBody:{}", request.getRemoteAddr(), body);
		return ResponseJson.acceptedWithNoCache(cachedFileService.processCachedFile(body, new ProcessInfo(DELETE_CACHE_DIR_URI, "CDN directory refresh interface", "submit directory refresh")));
	}
	
	/**
	 * 查询预取或刷新进度
	 * <br>
	 * 2015年3月20日
	 * @author gao.jun
	 * @param key
	 * @param accept
	 * @return
	 */
	@OpenapiAuth
	@RequestMapping(value="/progress",method=RequestMethod.GET)
	public ResponseJson quereyProgress(HttpServletRequest request,@RequestParam String key,
            @RequestHeader(value = "Accept",required = false) String accept){
		if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
		log.info("queryProgress, requestIP:{}, key:{}", request.getRemoteAddr(), key);
		Object urlObj = MemcacheManager.getFromCache(key);
		if(urlObj == null) {
			return ResponseJson.resourceNotFound(ErrorMsg.CACHED_FILE_PROGRESS_URL_NOT_FOUND, 
					ErrorMsg.CACHED_FILE_PROGRESS_URL_NOT_FOUND.getMsg());
		}
		Object progressResult = null;
		try {
			String url = urlObj.toString();
			log.info("quereyProgress URL:{}",url);
			progressResult = HttpClientUtil.get(url, HttpClientUtil.UTF_8);
		} catch (IOException e) {
			log.error("Invoke CDN prpgress query interface failed, url: {}" + urlObj);
			throw new OpenapiFailException("Invoke CDN prpgress query interface failed",e);
		}
		JSONObject fileJsonObj = new JSONObject();
		if(progressResult != null) {
			JSONObject paramObj = JSONObject.parseObject(progressResult.toString());
			for(Entry<String, Object> entry : paramObj.entrySet()) {
				String url = entry.getKey();
				if(StringUtils.isNotBlank(url)) {
					JSONObject progressResultObj = (JSONObject)entry.getValue();
					JSONObject progressJsonObj = new JSONObject();
					progressJsonObj.put("createdTime", progressResultObj.get("CreatedTimeGMT"));
					progressJsonObj.put("progress", progressResultObj.get("Progress"));
					progressJsonObj.put("passed", progressResultObj.get("Passed"));
					if(progressResultObj.get("EstimatedEndTimeGMT") != null) {
						progressJsonObj.put("estimatedEndTime", progressResultObj.get("EstimatedEndTimeGMT"));
					}else if(progressResultObj.get("FinishedTimeGMT") != null) {
						progressJsonObj.put("finishedTime", progressResultObj.get("FinishedTimeGMT"));
					}
					fileJsonObj.put(url, progressJsonObj);
				}
			}
		}else {
			throw new OpenapiFailException("Invoke CDN prpgress query interface cann't get result");
		}
		return ResponseJson.okWithNoCache(fileJsonObj);
	}
}
