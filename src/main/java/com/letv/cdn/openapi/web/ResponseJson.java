package com.letv.cdn.openapi.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.utils.ErrorMsg;

/**
 * <br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年10月24日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
public class ResponseJson extends ResponseEntity<String>{
    
    private ResponseJson(Object body, MultiValueMap<String, String> headers, HttpStatus statusCode) {
        super(JSON.toJSONString(body), headers, statusCode);
    }
    
    private ResponseJson(String body, MultiValueMap<String, String> headers, HttpStatus statusCode) {
        super(body, headers, statusCode);
    }

    /**
     * 返回一个不缓存的响应<br/>
     * <b>Method</b>: ResponseJson#noCache <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param responseBody
     * @param statusCode
     * @return  ResponseJson
     */
    public static ResponseJson noCache (Object body, HttpStatus statusCode) {
        return new ResponseJson(body, getNoCacheHeaders(), statusCode);
    }
    
    /**
     * 返回一个不缓存的响应<br/>
     * <b>Method</b>: ResponseJson#noCache <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param jsonString
     * @param statusCode
     * @return  ResponseJson
     */
    public static ResponseJson noCache (String jsonString, HttpStatus statusCode) {
        return new ResponseJson(jsonString, getNoCacheHeaders(), statusCode);
    }
    
    /**
     * 返回一个不缓存的200响应<br/>
     * <b>Method</b>: ResponseJson#okWithNoCache <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param responseBody
     * @return  ResponseJson
     */
    public static ResponseJson okWithNoCache (Object body) {
        return noCache(body, HttpStatus.OK);
    }
    
    public static ResponseJson createdNoCache (Object body) {
        return noCache(body, HttpStatus.CREATED);
    }
    
    /**
     * 返回一个不缓存的202响应
     * <br>
     * 2015年3月23日
     * @author gao.jun
     * @param body
     * @return
     */
    public static ResponseJson acceptedWithNoCache(Object body) {
    	return noCache(body, HttpStatus.ACCEPTED);
    }
    
    /**
     * 返回一个404响应<br/>
     * <b>Method</b>: ResponseJson#okWithNoCache <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param responseBody
     * @return  ResponseJson
     */
    public static ResponseJson notFound () {
        JSONObject errorMsg = new JSONObject();
        errorMsg.put("msg", "The requested resource not found.");
        return noCache(errorMsg, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 返回一个400响应，表示客户端请求错误<br/>
     * <b>Method</b>: ResponseJson#badRequestWithNoCache <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param responseBody
     * @return  ResponseJson
     */
    public static ResponseJson badRequest (Object body) {
        return noCache(body, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 返回包含错误编码与信息的400响应
     * <br>
     * 2014年12月22日
     * @author gao.jun
     * @param msg
     * @return
     */
    public static ResponseJson badRequest (ErrorMsg msg) {
    	JSONObject msgObj = new JSONObject();
        msgObj.put("code", msg.getCode());
        msgObj.put("msg", msg.getMsg());
        JSONObject errorObj = new JSONObject();
        errorObj.put("error", msgObj);
        return noCache(errorObj, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 返回一个403响应，表示客户端无权访问此资源<br/>
     * <b>Method</b>: ResponseJson#forbidden <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param body
     * @return  ResponseJson
     */
    public static ResponseJson forbidden (Object body) {
        return noCache(body, HttpStatus.FORBIDDEN);
    }
    
    /**
     * 返回一个405相应，表示无法支持客户端的请求方法<br/>
     * <b>Method</b>: ResponseJson#methodNotAllowed <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param body
     * @return  ResponseJson
     */
    public static ResponseJson methodNotAllowed (Object body) {
        return noCache(body, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    /**
     * 返回一个500响应<br/>
     * <b>Method</b>: ResponseJson#internalServerErrorWithNoCache <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param msg
     * @return  ResponseJson
     */
    public static ResponseJson internalServerError (String msg) {
        JSONObject errorMsg = new JSONObject();
        errorMsg.put("msg", msg);
        return noCache(errorMsg, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 返回包含错误码和错误信息的500响应
     * <br>
     * 2014年12月22日
     * @author gao.jun
     * @return
     */
    public static ResponseJson internalServerError(ErrorMsg msg) {
        JSONObject msgObj = new JSONObject();
        msgObj.put("code", msg.getCode());
        msgObj.put("msg", msg.getMsg());
        JSONObject errorObj = new JSONObject();
        errorObj.put("error", msgObj);
        return noCache(errorObj, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 获取不缓存的响应头<br/>
     * <b>Method</b>: ResponseJson#getNoCacheHeaders <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @return  HttpHeaders
     */
    private static HttpHeaders getNoCacheHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json;charset=utf-8");
        headers.setPragma("no-cache");
        headers.setCacheControl("no-cache");
        headers.setExpires(0);
        return headers;
    }
    
    /**
     * 返回一个包含响应信息的不缓存的500响应<br/>
     * @author liuchangfu
     * @param responseBody
     * @return  ResponseJson
     */
    public static ResponseJson failWithNoCache (Object body) {
        return noCache(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public static ResponseJson notAcceptable() {
    	return  noCache("无法根据客户端请求的内容特性完成请求",HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    
    /**
     * 返回一个包含错误码和错误信息的404响应<br/>
     * @param emsg
     * @param msg
     * @return
     */
    public static ResponseJson  resourceNotFound (ErrorMsg emsg ,String  msg) {
    	JSONObject msgObj = new JSONObject();
        msgObj.put("code", emsg.getCode());
        msgObj.put("msg", msg);
        JSONObject errorObj = new JSONObject();
        errorObj.put("error", msgObj);
        return noCache(errorObj, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 返回一个包含错误码和错误信息的500响应<br/>
     * @param emsg
     * @param msg
     * @return
     */
    public static ResponseJson  serverError (ErrorMsg emsg ,String  msg) {
    	JSONObject msgObj = new JSONObject();
        msgObj.put("code", emsg.getCode());
        msgObj.put("msg", msg);
        JSONObject errorObj = new JSONObject();
        errorObj.put("error", msgObj);
        return noCache(errorObj, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
