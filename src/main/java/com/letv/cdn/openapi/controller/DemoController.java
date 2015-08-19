package com.letv.cdn.openapi.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

@Controller
public class DemoController{
    
    
    
    @RequestMapping(value = {"/cdn/domain/enable"}, method = RequestMethod.GET, headers = "Lecloud-api-version=0.2")
    public ResponseJson domainEnable(HttpServletRequest request,
                                        @RequestHeader("Content-Type") String contentType,
                                        @RequestHeader("Authorization") String auth,
                                        @RequestHeader("Accept") String accept, 
                                        @RequestBody String data) throws IOException {
        JSONObject jo = JSON.parseObject(data);
        HttpClientUtil.get("http://fid.oss.letv.com/gslb/reload?cmd=22", HttpClientUtil.UTF_8);
        return ResponseJson.okWithNoCache(jo);
    }
    
    
    @OpenapiAuth
    @RequestMapping(value = {"/cdn/demo/preloadedfile"}, method = RequestMethod.POST, headers = "Lecloud-api-version=0.2")
    public ResponseJson submitFileApiII(HttpServletRequest request,
                                        @RequestHeader("Content-Type") String contentType,
                                        @RequestHeader("Authorization") String auth,
                                        @RequestHeader("Accept") String accept, 
                                        @RequestBody String data) {
        JSONObject jo = JSON.parseObject(data);
        String domain = jo.getString("domain");
        if (!StringUtils.hasLength(domain)) {
            throw new IllegalArgumentException("\"domain\" must not be null");
        }
        JSONArray ja = jo.getJSONArray("items");
        JSONObject rtnjo = new JSONObject();
        rtnjo.put("tasktag", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        JSONArray rstja = new JSONArray();
        for (Object o : ja) {
            JSONObject j = (JSONObject) o;
            JSONObject rsto = new JSONObject();
            String src = j.getString("src");
            if (!StringUtils.hasLength(src)) {
                throw new IllegalArgumentException("\"src\" must not be null");
            }
            String key = j.getString("key");
            rsto.put("key", key == null ? "" : key);
            rsto.put("src", src);
            rsto.put("status", "1");
            rstja.add(rsto);
        }
        rtnjo.put("result", rstja);
        return ResponseJson.okWithNoCache(jo);
    }
}

	