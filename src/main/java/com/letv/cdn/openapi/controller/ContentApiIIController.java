package com.letv.cdn.openapi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.contentterms.Content;
import com.letv.cdn.openapi.service.CachedFileService;
import com.letv.cdn.openapi.service.ContentIIService;
import com.letv.cdn.openapi.service.DomainService;
import com.letv.cdn.openapi.service.PreDistTaskService;
import com.letv.cdn.openapi.support.Link;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.web.LetvApiHelper;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * TODO:文件分发Api接口ver 0.2 Controller
 * 
 * @author liuchangfu
 * @createDate 2014年12月2日
 */
@Controller
public class ContentApiIIController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(ContentApiIIController.class);

	/**
	 * 发送请求的消息体格式为json
	 */
	private static final String JSON_TYPE = "json";
	
	/**
	 * 发送请求的消息体格式为xml
	 */
	private static final String XML_TYPE = "xml";

	@Resource
	ContentIIService contentIIService;
	@Resource
	DomainService domainService;

	@Resource
	PreDistTaskService pdts;
	
	@Resource
	CachedFileService cachedFileService;
	
    /**
	 * 提交预分发文件 ver0.2
	 * 
	 * @param request
	 * @param contentType
	 * @param auth
	 * @param data
	 * @return
	 * @throws Exception
	 */
    @OpenapiAuth
    @RequestMapping(value = {"/cdn/content", "/cdn/content/preloadedfile"}, method = RequestMethod.POST, headers = "Lecloud-api-version=0.2")
    public ResponseJson submitFileApi(HttpServletRequest request, @RequestHeader("Content-Type") String contentType,
            @RequestHeader("Authorization") String auth, @RequestHeader("Accept") String accept,
            @RequestBody String data) throws Exception {
        log.info("调用提交预分发文件接口start:---- 请求参数contentType:{}----auth:{}----data:{}", new Object[] {contentType, auth, data});
        JSONObject jsonObject = new JSONObject();
        if (!ApiHelper.checkAcceptHeader(accept)) {
            return ResponseJson.notAcceptable();
        }
        List<Content> contentApiList = new ArrayList<Content>();
        String baseUserid = ApiHelper.getUserid();
        String cip = LetvApiHelper.getClientIpAddr(request);
        String paramaType = contentIIService.getContentType(contentType, data);
        String domaintag = null;
        String tasktag = UUID.randomUUID().toString().replace("-", "");
        if (JSON_TYPE.equals(paramaType)) {// json格式
            // 获取items参数集合
            contentApiList = contentIIService.parseJsonArrayBySub(data,auth,baseUserid,100);
            domaintag = contentIIService.parseJson(data, "domaintag");
            Integer userid = Integer.parseInt(baseUserid);
            jsonObject = contentIIService.submitFile(cip, contentApiList, userid, domaintag, tasktag); //刷新
        } else if (XML_TYPE.equals(paramaType)) {
            // TODO xml 待写
            throw new IllegalArgumentException("请求头的参数Content-Type设置错误!");
        } else {
            throw new IllegalArgumentException("请求头的Content-Type设置有误!");
        }
        return ResponseJson.okWithNoCache(jsonObject);
    }

	/**
	 * 提交预分发文件 ver0.3
	 * 
	 * @param request
	 * @param contentType
	 * @param auth
	 * @param data
	 * @return
	 * @throws Exception
	 */
	@OpenapiAuth
	@RequestMapping(value = {"/cdn/content/preloadedfile"}, method = RequestMethod.POST, headers = "Lecloud-api-version=0.3")
	public ResponseJson submitFileApiII(HttpServletRequest request,
			@RequestHeader("Content-Type") String contentType,
			@RequestHeader("Authorization") String auth,
			@RequestHeader("Accept") String accept, @RequestBody String data)
			throws Exception {
		log.info("调用提交预分发文件接口start:---- 请求参数contentType:{}----auth:{}----data:{}",
		new Object[] { contentType, auth, data });
        if (!ApiHelper.checkAcceptHeader(accept)) {
            return ResponseJson.notAcceptable();
        }
        String paramaType = contentIIService.getContentType(contentType, data);
        String tasktag = UUID.randomUUID().toString().replace("-", "");
        JSONObject jsonObject = new JSONObject();
        // json格式
        if (JSON_TYPE.equals(paramaType)) {
        	log.info("本次任务批次-tasktag----", tasktag);
        	String baseUserid = ApiHelper.getUserid();
            // 获取items参数集合
        	List<Content> contentApiList = contentIIService.parseJsonArrayBySub(data,auth,baseUserid,500);
        	String domaintag = contentIIService.parseJson(data, "domaintag");
            Integer userid = Integer.parseInt(baseUserid);
            jsonObject.put("tasktag", tasktag);
            jsonObject.put("result", contentIIService.insertPreDistTasks(contentApiList, userid, domaintag, tasktag, true));
            jsonObject.put("links", Arrays.asList(new Link("http://api.cdn.lecloud.com/cdn/content/preloadedfiles?domaintag=domaintag&tasktag=".concat(tasktag), 
            		"progress", "GET")));
        } else if (XML_TYPE.equals(paramaType)) {
            throw new IllegalArgumentException("请求头的参数Content-Type设置错误!");
        } else {
            throw new IllegalArgumentException("请求头的Content-Type设置有误!");
        }
        return ResponseJson.okWithNoCache(jsonObject);
	}

	/**
	 * 删除预分发文件ver0.2
	 * 
	 * @param request
	 * @param response
	 * @param data
	 * @return
	 * @throws IOException
	 */
	@OpenapiAuth
	@RequestMapping(value = {"/cdn/content", "/cdn/content/preloadedfile"}, method = RequestMethod.DELETE, headers = "Lecloud-api-version=0.2")
	public ResponseJson deleteFileApi(HttpServletRequest request,
			@RequestHeader("Content-Type") String contentType,
			@RequestHeader("Authorization") String auth,
			@RequestHeader("Accept") String accept, @RequestBody String data)
			throws IOException {
		log.info("调用删除预分发文件接口start:---- 请求参数contentType:{}----auth:{}----data:{}",
		new Object[] { contentType, auth, data });
		if (!ApiHelper.checkAcceptHeader(accept)) {
			return ResponseJson.notAcceptable();
		}
		List<Content> contentApiList = new ArrayList<Content>();
		JSONObject jsonObj = new JSONObject();
		String baseUserid = ApiHelper.getUserid();
		String paramaType = contentIIService.getContentType(contentType, data);
		if (JSON_TYPE.equals(paramaType)) {
			contentApiList = contentIIService.parseJsonArrayByDel(data,auth,baseUserid);
		} else if (XML_TYPE.equals(paramaType)) {
			// TODO xml 待后续扩展
			throw new IllegalArgumentException("请求头的Content-Type设置错误!");
		} else {
			throw new IllegalArgumentException("请求头的Content-Type设置有误!");
		}
		Integer userid = Integer.parseInt(baseUserid);
		String domaintag = contentIIService.parseJson(data, "domaintag");
		Map<String,Object> resultMap = contentIIService.delFileAPi(userid, contentApiList, domaintag);
		jsonObj.put("result", resultMap.get("deleteContents"));
		if(resultMap.get("links") != null) {
			jsonObj.put("links", resultMap.get("links"));
		}
		return ResponseJson.okWithNoCache(jsonObj);
	}
	
	/**
	 * 删除文件，增加对未提交文件的删除
	 * 2015年5月4日<br>
	 * @author gao.jun
	 * @param request
	 * @param contentType
	 * @param auth
	 * @param accept
	 * @param data
	 * @return
	 * @throws IOException
	 * @since v0.3
	 */
	@OpenapiAuth
	@RequestMapping(value = {"/cdn/content/preloadedfile"}, method = RequestMethod.DELETE, headers = "Lecloud-api-version=0.3")
	public ResponseJson deleteFile(HttpServletRequest request,
			@RequestHeader("Content-Type") String contentType,
			@RequestHeader("Authorization") String auth,
			@RequestHeader("Accept") String accept, @RequestBody String data)
			throws IOException {
		log.info("调用删除预分发文件接口start:---- 请求参数contentType:{}----auth:{}----data:{}",
		new Object[] { contentType, auth, data });
		if (!ApiHelper.checkAcceptHeader(accept)) {
			return ResponseJson.notAcceptable();
		}
		List<Content> contentApiList = new ArrayList<Content>();
		String baseUserid = ApiHelper.getUserid();
		String paramaType = contentIIService.getContentType(contentType, data);
		if (JSON_TYPE.equals(paramaType)) {
			contentApiList = contentIIService.parseJsonArrayByDel(data,auth,baseUserid);
		} else if (XML_TYPE.equals(paramaType)) {
			throw new IllegalArgumentException("请求头的Content-Type设置错误!");
		} else {
			throw new IllegalArgumentException("请求头的Content-Type设置有误!");
		}
		Integer userid = Integer.parseInt(baseUserid);
		String domaintag = contentIIService.parseJson(data, "domaintag");
		return ResponseJson.okWithNoCache(contentIIService.delFileByContents(userid, contentApiList, domaintag));
	}

	/**
	 * 查询预分发进度
	 * 
	 * @param request
	 * @param response
	 * @param userid
	 * @param key
	 * @return
	 */
	@OpenapiAuth
	@RequestMapping(value = {"/cdn/content/{userid}_{key}/status", "/cdn/content/preloadedfile/{userid}_{key}/status"}, method = RequestMethod.GET, headers = "Lecloud-api-version=0.2")
	public ResponseJson getProgressApi(HttpServletRequest request,
			@PathVariable() String userid, @PathVariable() String key,
			@RequestHeader("Accept") String accept) {
		log.info("查询预分发进度start:---- 请求参数userid:{}key:{}accept:{}",
		new Object[] { userid, key, accept });
		if (!ApiHelper.checkAcceptHeader(accept)) {
			return ResponseJson.notAcceptable();
		}
		String outkey = userid + "_" + key;
		PreDistTask pdt = this.pdts.selectByOutkey(outkey);
		if (pdt == null) {
			return ResponseJson.notFound();
		}
		Byte status = pdt.getStatus();
		JSONObject jsonResult = new JSONObject();
		jsonResult.put("status", status);
		return ResponseJson.okWithNoCache(jsonResult);
	}
	
	
}
