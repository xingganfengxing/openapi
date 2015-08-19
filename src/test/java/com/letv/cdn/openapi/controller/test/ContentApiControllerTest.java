package com.letv.cdn.openapi.controller.test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.client.ClientProtocolException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapiauth.utils.MD5;

/**
 * 预分发任务测试 v0.1
 * <br>
 * 2014年12月24日
 * @author gao.jun
 *
 */
public class ContentApiControllerTest extends BasicControllerTest {
	
	private static final String LOCAL_URI_PREFIX = "http://localhost:8084/cdn/content";
	
//	private static final String LOCAL_URI_PREFIX = "http://api.cdn.lecloud.com/cdn/content";
	
//	private static final String LOCAL_URI_PREFIX = "http://111.206.210.204:9002/cdn/content";

	private static String key;
	
	private static String newkey;
	
	private static final String DOMAINTAG = "apitest";
	
	/**
	 * 提交预分发任务
	 * <br>
	 * 正常提交
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testSubmitFileApi1() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/16.flv ");
        key = String.valueOf(System.currentTimeMillis());
        jsonObj.put("key", key);
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        ResponseEntity<String> resultEntity = restTemplate.postForEntity(LOCAL_URI_PREFIX + "/subfile", entity, String.class);
    	
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	/**
	 * 提交预分发任务
	 * <br>
	 * 提交预分发文件重复
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testSubmitFileApi2() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/16.flv ");
        jsonObj.put("key", key);
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        try {
        	restTemplate.postForEntity(LOCAL_URI_PREFIX + "/subfile", entity, String.class);
        } catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(500,ex.properties.get("code"));
		}
	}
	
	/**
	 * 查找分发进度
	 * <br>
	 * 正常查询
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testGetProgressApi1() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/16.flv ");
        jsonObj.put("key", key);
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX + "/progress"
				.concat("?").concat(strB.toString()), HttpMethod.GET, null, String.class);

		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("status"));
	}
	
	/**
	 * 查询分发进度
	 * <br>
	 * 分发文件不存在
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testGetProgressApi2() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/13ddd.flv");
        jsonObj.put("key", String.valueOf(System.currentTimeMillis()));
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "ac73952309a68d324e70a8869a708f4edd");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
    	try {
			restTemplate.exchange(LOCAL_URI_PREFIX + "/progress"
					.concat("?").concat(strB.toString()), HttpMethod.GET, null, String.class);
    	} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(404,ex.properties.get("code"));
		}
	}
	
	/**
	 * 删除预分发任务
	 * <br>
	 * 任务不存在
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testDeleteFileApi1() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/13ddd.flv");
        jsonObj.put("key", String.valueOf(System.currentTimeMillis()));
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "ac73952309a68d324e70a8869a708f4edd");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
    	try {
	        restTemplate.exchange(LOCAL_URI_PREFIX + "/delfile"
					.concat("?").concat(strB.toString()), HttpMethod.POST, null, String.class);
    	} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(500,ex.properties.get("code"));
			JSONObject jobj = JSONObject.parseObject(ex.properties.get("body").toString());
			Assert.assertEquals("failure", jobj.get("msg"));
		}
	}
	
	/**
	 * 刷新分发任务 v0.1
	 * <br>
	 * 任务不存在
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testUpdateFileApi1() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/13ddd.flv");
        jsonObj.put("key", String.valueOf(System.currentTimeMillis()));
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "ac73952309a68d324e70a8869a708f4edd");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
    	try {
	        restTemplate.exchange(LOCAL_URI_PREFIX + "/updatefile"
					.concat("?").concat(strB.toString()), HttpMethod.POST, null, String.class);
    	} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(500,ex.properties.get("code"));
			JSONObject jobj = JSONObject.parseObject(ex.properties.get("body").toString());
			Assert.assertEquals("failur", jobj.get("msg"));
		}
	}
	
	/**
	 * 测试回调
	 * <br>
	 * 正常回调
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testCallback1() {
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("tag", DOMAINTAG);
        jsonObj.put("user", Constant.USERID);
        jsonObj.put("status", "200");
        jsonObj.put("outkey", Constant.USERID + "_" + key);
        jsonObj.put("fsize", "123");
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("storeurl", "123/23/111/acloud/127022/v/16.flv ");
        
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        ResponseEntity<String> resultEntity = restTemplate.getForEntity(LOCAL_URI_PREFIX + "/callback?"+strB,  String.class);
    	
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	/**
	 * 刷新分发任务 v0.1
	 * <br>
	 * 正常刷新
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testUpdateFileApi2() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/16.flv ");
        newkey = String.valueOf(System.currentTimeMillis());
        jsonObj.put("key", newkey);
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        ResponseEntity<String> resultEntity = restTemplate.postForEntity(LOCAL_URI_PREFIX + "/updatefile", entity, String.class);
    	
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("success", jobj.get("msg"));
	}
	
	/**
	 * 删除任务
	 * <br>
	 * 删除回调后的任务
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testDeleteFileApi2() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/16.flv ");
        jsonObj.put("key", key);
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
    	ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX + "/delfile"
				.concat("?").concat(strB.toString()), HttpMethod.POST, null, String.class);
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	/**
	 * 测试回调
	 * <br>
	 * 正常回调待刷新的任务
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testCallback2() {
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("tag", DOMAINTAG);
        jsonObj.put("user", Constant.USERID);
        jsonObj.put("status", "200");
        jsonObj.put("outkey", Constant.USERID + "_" + newkey);
        jsonObj.put("fsize", "123");
        jsonObj.put("md5", "560c29f287a38916270e8c9ee48a5289");
        jsonObj.put("storeurl", "123/23/111/acloud/127022/v/16.flv ");
        
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        ResponseEntity<String> resultEntity = restTemplate.getForEntity(LOCAL_URI_PREFIX + "/callback?"+strB,  String.class);
    	
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	/**
	 * 删除任务
	 * <br>
	 * 删除待刷新的任务
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testDeleteFileApi3() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("src", "http://115.182.94.184/v/16.flv ");
        jsonObj.put("key", newkey);
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
    	ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX + "/delfile"
				.concat("?").concat(strB.toString()), HttpMethod.POST, null, String.class);
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	@Test
	public void testHeadRequest() {
		DateFormat format=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
    	format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Map<String, String> headers = null;
		try {
			headers = HttpClientUtil.head("http://cdn3.eastiming.com/201412/8466c458-d2e5-4a99-b225-0f81f93f8fc7_0.mp4", null, HttpClientUtil.UTF_8);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(headers == null) {
			System.out.println("header is null");
		}
		long version = 0l;
		String contentLength = headers.get("Content-Length");
		if(contentLength != null) {
			version += Long.valueOf(contentLength);
		}
		String lastModified = headers.get("Last-Modified");
		if(lastModified != null) {
			try {
				version += format.parse(lastModified).getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		System.out.println(version);
	}
	
	private static String sign(Map<String, String> m, String key) {
        String[] names = m.keySet().toArray(new String[m.size()]);
        Arrays.sort(names);

        StringBuilder buf = new StringBuilder();
        for (String name : names) {
            buf.append(name);
            buf.append(m.get(name));
        }
        buf.append(key);

        return MD5.md5(buf.toString());
    }
}
