package com.letv.cdn.openapi.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.service.PreDistTaskService;
import com.letv.cdn.openapi.service.ReportApiService;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * TODO: report 调用 Api接口 Controller
 * 
 * @author liuchangfu
 * @createDate 2015年1月4日
 */
@Controller
@RequestMapping("/cdn/content")
public class ReportApiController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ReportApiController.class);
    @Resource
    ReportApiService reportApiService;
    @Resource
    PreDistTaskService pdts;

    /**
     * 查询信息列表
     * 
     * @param request
     * @param response
     * @param userid
     * @param domaintag
     * @param src
     * @param status
     * @param page
     * @param rows
     * @param tasktag
     * @return
     * @throws IOException
     */
    @OpenapiAuth
    @RequestMapping(value = {"/pretasks", "/preloadedfiles"}, method = RequestMethod.GET, headers = "Lecloud-api-version=0.2")
    public ResponseJson getProgress(HttpServletRequest request,HttpServletResponse response,
	    			    @RequestParam(required = true) String domaintag,
	    			    @RequestParam(required = false) String src,
	    			    @RequestParam(required = false) Integer status,
	    			    @RequestParam(required = false) Integer page,
	    			    @RequestParam(required = false) Integer rows,
	    			    @RequestParam(required = false) String tasktag) throws IOException {
        Integer userid = Integer.parseInt(ApiHelper.getUserid());
	    log.info("调用查询预热任务件接口start:--请求参数userid:{}--domaintag:{}--src:{}--status:{}--page:{}--rows:{}--tasktag:{}",
		new Object[] { userid, domaintag, src, status, page, rows,tasktag });
		if ((page == null && rows != null) || (page != null && rows == null)) {
			throw new IllegalArgumentException("page rows 参数错误");
		}
		if (page != null && rows != null) {
			Assert.isTrue(page > 0, "page参数错误");
			Assert.isTrue(rows > 0, "rows参数错误");
		}
		if (!StringUtils.hasLength(src) && !StringUtils.hasLength(tasktag) && status == null && page == null && rows == null) {
		    page = 1;
		    rows = 100;
		}
		List<String> tagList = reportApiService.getTagList(domaintag);
		int count = this.pdts.selectTotalSize(src, status, tasktag, tagList,userid);
		List<PreDistTask> selectBySrcAndstatus = this.pdts.selectBySrcAndstatus(src, status, page, rows, tasktag,tagList, userid);
		JSONObject jsonResult = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		if (selectBySrcAndstatus.size() > 0) {
			for (PreDistTask pdt : selectBySrcAndstatus) {
				JSONObject jo = new JSONObject();
				jo.put("src", pdt.getSrc());
				jo.put("status", pdt.getStatus());
				jo.put("domaintag", pdt.getDomaintag());
				String outkey = pdt.getOutkey();
				// 截取不带userid的key
				String key = outkey.substring(outkey.indexOf("_") + 1,outkey.length());
				jo.put("key", key);
				jo.put("ptime", pdt.getCreateTime());
				jo.put("tasktag", pdt.getTasktag());
				jo.put("md5", pdt.getMd5());
				jsonArray.add(jo);
			}
		}
		jsonResult.put("result", jsonArray);
		jsonResult.put("totalSize", count);
		return ResponseJson.okWithNoCache(jsonResult);
    }
    
    /**
	 * 查询分发进度
	 * @author gao.jun
	 * @param request
	 * @param response
	 * @param data
	 * @param accept
	 * @return
	 * @throws IOException 
	 * @since v0.3
	 */
	@OpenapiAuth
    @RequestMapping(value = {"/preloadedfiles"}, method = RequestMethod.GET, headers = "Lecloud-api-version=0.3")
	public ResponseJson getProgress(HttpServletRequest request,
			HttpServletResponse response, @RequestParam String domaintag,  @RequestParam(required = false) String tasktag,
			 @RequestParam(required = false) String keys,
			 @RequestHeader("Accept") String accept) throws IOException {
		if (!ApiHelper.checkAcceptHeader(accept)) {
			return ResponseJson.notAcceptable();
		}
		Assert.isTrue(org.apache.commons.lang.StringUtils.isNotBlank(tasktag) || org.apache.commons.lang.StringUtils.isNotBlank(keys), 
				"The parameter tasktag must not be null or the parameter keys must not be null");
		String baseUserid = ApiHelper.getUserid();
		String[] keyArray = null;
		if(org.apache.commons.lang.StringUtils.isNotBlank(keys)) {
			keyArray = keys.split(",");
			for(int i = 0,len = keyArray.length; i < len; ++i) {
				keyArray[i] = baseUserid.concat("_").concat(keyArray[i]);
			}
		}
		return ResponseJson.okWithNoCache(pdts.generateProgress(tasktag, keyArray));
	}
}
