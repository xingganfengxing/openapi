/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : TaskApiController.java 
 * Date Created: 2014年10月23日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月23日 下午5:20:00 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONObject;
import com.lecloud.commons.logging.annotation.Log;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.service.ContentService;
import com.letv.cdn.openapi.service.DomainService;
import com.letv.cdn.openapi.service.PreDistTaskService;
import com.letv.cdn.openapi.service.SubFileResultService;
import com.letv.cdn.openapi.service.TaskCallbackService;
import com.letv.cdn.openapi.service.UserService;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.LetvApiHelper;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * TODO:文件分发Api接口Controller
 * 
 * @author chenyuxin
 * @createDate 2014年10月23日
 */
@Controller
@RequestMapping("/cdn/content")
public class ContentApiController extends BaseController{
    
	private static final Logger log = LoggerFactory.getLogger(ContentApiController.class);
    
    @Resource
    UserService userService;
    @Resource
    DomainService domainService;
    @Resource
    ContentService contentService;
    @Resource
    SubFileResultService subFileResultService;
    @Resource
    PreDistTaskService pdts;
    
    @Resource
    TaskCallbackService callbackService;
    
    /**文件分发成功*/
    public static final int DISTRIBUTE_SUCCESS = 1;
    /**文件分发中*/
    public static final int DISTRIBUTE_ING = 2;
    /**文件分发失败*/
    public static final int DISTRIBUTE_FAILURE = 0;
    
    
    /**
     * 提交预分发文件的接口
     * 
     * @method: ContentApiController submitFile
     * @param request
     * @param response
     * @param domaintag
     *        域名标识
     * @param src
     *        经过url编码的文件源地址
     * @param key
     *        作为此次任务的唯一标识，用于回调
     * @param userid
     *        用户唯一标识码，由乐视网统一分配并提供
     * @param md5
     *        文件md5值
     * @param sign
     *        验证码
     * @return ResponseEntity<JSONObject>
     * @createDate： 2014年10月23日
     * @2014, by chenyuxin.
     * @throws Exception
     */
    @OpenapiAuth
    @RequestMapping(value = "/subfile", method = RequestMethod.POST)
    public ResponseJson submitFileApi(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "domaintag", required = true) String domaintag,
            @RequestParam(value = "src", required = true) String src,
            @RequestParam(value = "key", required = true) String key,
            @RequestParam(value = "userid", required = true) Integer userid,
            @RequestParam(value = "md5", required = false) String md5,
            @RequestParam(value = "ver", required = true) String ver,
            @RequestParam(value = "sign", required = true) String sign) throws Exception {
    
        Assert.hasLength(domaintag, "查询参数错误：域名标识(domaintag)不能为空");
        Assert.hasLength(key, "查询参数错误：任务的唯一标识(key)不能为空");
        
    	// 调用分发文件的方法
        boolean flag = contentService.subFile(domaintag, key, src, md5, userid, LetvApiHelper.getClientIpAddr(request),PreDistTask.TASK_SUB_TYPE);
        String msg = "提交预分发文件重复";
        if(flag){
            msg = "success";
        }
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("msg", msg);
        return ResponseJson.okWithNoCache(jsonResult);
    }
    
    /**
     * 提供给lecloud底层，当文件分发完成后回调
     * 
     * @method: ContentApiController  callback
     * @param request
     * @param response
     * @param tag
     * @param user 
     * @param status 状态码
     * @param outKey 提交分发任务的唯一标识,后台回调的是"userid_key"
     * @param fsize 文件大小
     * @param md5 文件的MD5
     * @param storeurl  void
     * @createDate： 2014年10月30日
     * @2014, by chenyuxin.
     * @throws IOException 
     * @throws DocumentException 
     */
    @Log(project = "openapi", module = "content", function = "callback")
    @RequestMapping(value = "/callback")
    public ResponseJson callback(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value ="tag",      required = false) String tag,
            @RequestParam(value ="user",     required = false) String user,
            @RequestParam(value ="status",   required = false) String status, 
            @RequestParam(value ="outkey",   required = false) String outkey,
            @RequestParam(value ="fsize",    required = false) String fsize,
            @RequestParam(value ="md5",      required = false) String md5,
            @RequestParam(value ="storeurl", required = false) String storeurl) throws IOException, DocumentException {
        
        PreDistTask pdt = pdts.selectByOutkey(outkey);
        if (pdt != null && pdt.getCallbackCode() != null
				&& pdt.getCallbackCode().equals(
						PreDistTask.CALLBACK_CODE_SUCCESS)) {
			log.info("任务已成功，不再接收回调");
		}else if(pdt == null){
			log.warn("未找到对应任务");
		}else {
			Long size = null;
	        try {
	            size = Long.parseLong(fsize);
	        } catch (NumberFormatException e) {
	            size = 0L;	
	        }
	        Byte taskStatus = null;
	        if (PreDistTask.CALLBACK_CODE_SUCCESS.equals(status)) {
	        	taskStatus = PreDistTask.STATUS_SUCCESS;
			} else if (PreDistTask.CALLBACK_CODE_FAILURE.equals(status)) {
				taskStatus = PreDistTask.STATUS_FAILURE;
			} else if (PreDistTask.CALLBACK_CODE_FAILURE_RETRY.equals(status)) {
				taskStatus = PreDistTask.STATUS_FAILURE_RETRY;
			}
	        log.info("callbacktest - {} - 查看回调地址", outkey);
			// 获取客户端的回调接口
			String uri = contentService.getClientCallbackUriByAppkey(pdt.getAppkey());
			byte clientCallbacked = 0;
			if(!StringUtils.isEmpty(uri) && taskStatus != null){
	            log.info("callbacktest - {} - 回调客户", outkey);
				String result = callbackService.clientCallback(uri, org.apache.commons.lang.StringUtils.substringAfter(outkey, "_"), taskStatus, pdt.getSrc());
				if(result != null) {
					clientCallbacked = 1;
				}
			}
            log.info("callbacktest - {} - 改任务状态", outkey);
			this.pdts.callback(pdt, status, taskStatus, size, storeurl, md5, clientCallbacked);
            log.info("callbacktest - {} - 改完", outkey);
		}
        // 返回给lecloud的确认收到回调信息
        JSONObject jo = new JSONObject();
        jo.put("result", "success");
        return ResponseJson.okWithNoCache(jo);
    }

    /**
     * 回调测试系统<br>
     * <b>Method</b>: ContentApiController#testCallback <br/>
     * <b>Create Date</b> : 2014年11月21日
     * @author Chen Hao
     * @param tag
     * @param user
     * @param status
     * @param outkey
     * @param fsize
     * @param md5
     * @param storeurl  void
     
    private void testCallback(String tag, String user, String status, String outkey, String fsize, String md5, String storeurl) {
        try {
            if (outkey != null && outkey.split("_")[0].equals("137587")) {
                String testCallbackUrl = Env.get("test_callback");
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", tag);
                params.put("user", user);
                params.put("status", status);
                params.put("outkey", outkey);
                params.put("fsize", fsize);
                params.put("md5", md5);
                params.put("storeurl", storeurl);
                HttpClientUtil.post(testCallbackUrl, params, HttpClientUtil.UTF_8);
            }
        } catch (Exception e) {
            log.warn("回调测试系统失败");
        }
    }*/
    
    /**
     * 查询分发任务进度的接口
     * 
     * @method: ContentApiController getProgress
     * @param request
     * @param response
     * @param src
     *        经过url编码的文件源地址
     * @param userid
     *        用户唯一标识码，由乐视网统一分配并提供
     * @param ver 版本号
     * @param sign
     *        验证码
     * @return ResponseEntity<JSONObject>
     * @createDate： 2014年10月23日
     * @2014, by chenyuxin.
     * @throws IOException
     */
    @OpenapiAuth
    @RequestMapping(value = "/progress", method = RequestMethod.GET)
    public ResponseJson getProgressApi(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "domaintag", required = false) String domaintag,
            @RequestParam(value = "src",       required = false) String src,
            @RequestParam(value = "key",       required = false) String key,
            @RequestParam(value = "md5",       required = false) String md5,
            @RequestParam(value = "userid",    required = true)  Integer userid,
            @RequestParam(value = "ver",       required = true)  String ver,
            @RequestParam(value = "sign",      required = true)  String sign) throws IOException {
        Assert.isTrue((StringUtils.hasLength(key) || StringUtils.hasLength(md5)), "key与md5应至少传一个");
        PreDistTask pdt = this.pdts.selectByOutkeyMd5Src(userid, key, md5, src);
        if (pdt == null) {
            return ResponseJson.notFound();
        }
        Byte status = pdt.getStatus();
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("status", status);
        return ResponseJson.okWithNoCache(jsonResult);
    }
    
    /**
     * 删除分发文件
     * 
     * @method: ContentApiController getProgress
     * @param request
     * @param response
     * @param src
     *        经过url编码的文件源地址
     * @param userid
     *        用户唯一标识码，由乐视网统一分配并提供
     * @param ver 版本号
     * @param sign
     *        验证码
     * @return ResponseEntity<JSONObject>
     * @createDate： 2014年10月23日
     * @2014, by chenyuxin.
     * @throws IOException
     */
    @OpenapiAuth
    @RequestMapping(value = "/delfile", method = RequestMethod.POST)
    public ResponseJson deleteFileApi(@RequestParam(value = "domaintag", required = false) String domaintag,
                                      @RequestParam(value = "src",       required = false) String src,
                                      @RequestParam(value = "key",       required = false) String key,
                                      @RequestParam(value = "md5",       required = false) String md5,
                                      @RequestParam(value = "userid",    required = true) Integer userid,
                                      @RequestParam(value = "ver",       required = true) String ver,
                                      @RequestParam(value = "sign",      required = true) String sign) throws IOException {
        boolean flag = contentService.deleteFile(userid, domaintag, src, key, md5);
        if (flag) {
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("msg", "success");
            return ResponseJson.okWithNoCache(jsonResult);
        }
        return ResponseJson.internalServerError("failure");
    }
    
    /**
     * 分发文件更新的接口
     * 
     * @method: ContentApiController submitFile
     * @param request
     * @param response
     * @param domaintag
     *        域名标识
     * @param src
     *        经过url编码的文件源地址
     * @param key
     *        作为此次任务的唯一标识，用于回调
     * @param userid
     *        用户唯一标识码，由乐视网统一分配并提供
     * @param md5
     *        文件md5值
     * @param ver 版本号
     * @param sign
     *        验证码
     * @return ResponseEntity<JSONObject>
     * @createDate： 2014年10月23日
     * @2014, by chenyuxin.
     * @throws IOException 
     * @throws DocumentException 
     * @throws Exception 
     */
    @OpenapiAuth
    @RequestMapping(value = "/updatefile", method = RequestMethod.POST)
    public ResponseJson updateFileApi(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "domaintag", required = true) String domaintag,
            @RequestParam(value = "src", required = true) String src,
            @RequestParam(value = "key", required = true) String key,
            @RequestParam(value = "userid", required = true) Integer userid, 
            @RequestParam(value = "md5", required = false) String md5,
            @RequestParam(value = "ver", required = true) String ver,
            @RequestParam(value = "sign", required = true) String sign) throws IOException, DocumentException{
        Assert.hasLength(domaintag, "查询参数错误：域名标识(domaintag)不能为空");
        Assert.hasLength(src, "查询参数错误：经过url编码的文件源地址(src)不能为空");
        Assert.hasLength(key, "查询参数错误：任务的唯一标识(key)不能为空");
        src = RegExpValidatorUtil.replaceBlank(src);
        // 先删除再重新分发
        boolean flag = contentService.deleteFile(userid, domaintag, src, key, md5);
        if (flag) {
            // 重新提交分发文件
            flag = contentService.subFile(domaintag, key, src, md5, userid, LetvApiHelper.getClientIpAddr(request),PreDistTask.TASK_REFRESH);
        }
        if(flag){
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("msg", "success");
            return ResponseJson.okWithNoCache(jsonResult);
        }
        return ResponseJson.internalServerError("failur");
    }
}
