package com.letv.cdn.openapi.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONObject;
import com.lecloud.commons.logging.annotation.Log;
import com.letv.cdn.openapi.service.ContentService;
import com.letv.cdn.openapi.service.DomainExtensionService;
import com.letv.cdn.openapi.service.TempFileSubmitService;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapi.web.ResponseJson;

@Controller
@RequestMapping("/content")
public class TempController{
    

    @Resource
    ContentService contentService;
    
    @Resource
    TempFileSubmitService tempSubmitService;
    
    @Resource
    DomainExtensionService domainExtService;
    
    //@Monitoring
    @Log(project = "openapi", module = "predist", function = "callback")
    @RequestMapping(value = "/callback")
    public ResponseJson callback(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String status, 
            @RequestParam(required = false) String outKey,
            @RequestParam(required = false) String fsize,
            @RequestParam(required = false) String md5,
            @RequestParam(required = false) String storeurl) throws IOException, DocumentException {
    
        // TODO 获取客户端的回调接口
        String uri = contentService.getClientCallbackUri(outKey);
        Map<String, String> params = new HashMap<String, String>();
        params.put("key", outKey);
        if (Integer.parseInt(status) == 200) {
            params.put("status", "success");
        } else {
            params.put("status", "failur");
        }
        // TODO 客户端的回调接口是否有响应
        if(uri != null){
            HttpClientUtil.post(uri, params, HttpClientUtil.UTF_8);
        }
        JSONObject jo = new JSONObject();
        jo.put("result", "success");
        return ResponseJson.okWithNoCache(jo);
    }
    
    @RequestMapping(value = "/domainext/action/refresh")
    public ResponseJson tempSubmit() {
    	domainExtService.init();
    	JSONObject jo = new JSONObject();
    	jo.put("success", true);
    	return ResponseJson.okWithNoCache(jo);
    }
}

	