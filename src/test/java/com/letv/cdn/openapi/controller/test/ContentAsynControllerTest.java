package com.letv.cdn.openapi.controller.test;


import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.pojo.PreDistTask;

/**
 * 异步提交测试用例
 *
 * @author gao.jun 
 * @date 2015年5月4日
 *
 */
public class ContentAsynControllerTest extends BasicControllerTest {
	
	private static final String DOMAINTAG = "apitest";
	
	private static final String LOCAL_URL = "http://localhost:8084/cdn/content";
	
	private static final String LOCAL_URI_PREFIX = LOCAL_URL.concat("/preloadedfile");
	
	private static final Logger log = LoggerFactory.getLogger(ContentAsynControllerTest.class);
	
	private static String KEY;
	
	/**
	 * 测试提交任务<br>
	 * 正常提交
	 * 2015年5月5日<br>
	 * @author gao.jun
	 */
	@Test
	public void testSubmit1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.3");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "POST", "/cdn/content/preloadedfile", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        
        JSONObject item = new JSONObject();
        item.put("src", "http://123.126.32.31:8888/ccnn.mp3");
        KEY = String.valueOf(System.currentTimeMillis());
        item.put("key", KEY);
        item.put("md5", "64435dfb994de19c8eb77ddddca36be7");
        JSONArray arr = new JSONArray();
        arr.add(item);
        
        jsonObj.put("items", arr);
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(),headers);
        
        ResponseEntity<String> resultEntity = restTemplate.postForEntity(LOCAL_URI_PREFIX, entity, String.class);
        
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        log.info("result: ".concat(resultBody));
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("tasktag"));
        Assert.assertTrue(!jobj.getJSONArray("result").isEmpty());
        Assert.assertTrue(!jobj.getJSONArray("links").isEmpty());
	}
	
	
	
	/**
	 * 测试进度查询<br>
	 * 正常提交
	 * 2015年5月5日<br>
	 * @author gao.jun
	 * @throws InterruptedException 
	 */
	@Test
	public void testQueryProgress1() throws InterruptedException {
		// 等待60秒，等待任务提交
		Thread.sleep(60000);
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.3");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/content/preloadedfiles", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        jsonObj.put("keys", KEY);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("s?")
        		.concat(generateGetReqParamStr(jsonObj).toString()), HttpMethod.GET, entity, String.class);
        
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        log.info("result: ".concat(resultBody));
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("result"));
        JSONArray jsonArray = jobj.getJSONArray("result");
        Assert.assertTrue(((JSONObject)jsonArray.get(0)).getByte("status") == PreDistTask.STATUS_ING);
	}
	
	/**
	 * 正常回调用户，用于测试后续用例
	 * 2015年5月8日<br>
	 * @author gao.jun
	 */
	@Test
	public void testCallback1() {
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("tag", DOMAINTAG);
        jsonObj.put("user", Constant.USERID);
        jsonObj.put("status", "200");
        jsonObj.put("outkey", Constant.USERID + "_" + KEY);
        jsonObj.put("fsize", "123");
        jsonObj.put("md5", "64435dfb994de19c8eb77ddddca36be7");
        jsonObj.put("storeurl", "138/44/48/acloud/127022/123.126.32.31:8080/ccnn.mp3");
        
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        ResponseEntity<String> resultEntity = restTemplate.getForEntity(LOCAL_URL + "/callback?"+strB,  String.class);
    	
    	Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	/**
	 * 测试任务删除<br>
	 * 正常删除
	 * 2015年5月5日<br>
	 * @author gao.jun
	 */
	@Test
	public void testDel1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.3");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/content/preloadedfile", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        JSONObject item = new JSONObject();
        item.put("src", "http://123.126.32.31:8888/ccnn.mp3");
        JSONArray arr = new JSONArray();
        arr.add(item);
        jsonObj.put("items", arr);
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(),headers);
        super.setDelRequestFactory();
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX, HttpMethod.DELETE, entity, String.class);
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        log.info("result: ".concat(resultBody));
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("result"));
        JSONArray items = jobj.getJSONArray("result");
        Assert.assertTrue(items.size() != 0);
        Assert.assertTrue(((JSONObject)items.get(0)).getByte("status") == 1);
        Assert.assertNotNull(jobj.get("links"));
        JSONArray links = jobj.getJSONArray("links");
        Assert.assertTrue(links.size() == 1);
	}
	
	/**
	 * 删除任务不存在
	 * 
	 * 2015年5月5日<br>
	 * @author gao.jun
	 */
	@Test
	public void testDel2() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.3");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/content/preloadedfile", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", DOMAINTAG);
        JSONObject item = new JSONObject();
        item.put("src", "http://123.126.32.31:8888/ccnn1.mp3");
        JSONArray arr = new JSONArray();
        arr.add(item);
        jsonObj.put("items", arr);
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(),headers);
        super.setDelRequestFactory();
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX, HttpMethod.DELETE, entity, String.class);
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        log.info("result: ".concat(resultBody));
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("result"));
        JSONArray items = jobj.getJSONArray("result");
        Assert.assertTrue(items.size() != 0);
        Assert.assertTrue(((JSONObject)items.get(0)).getByte("status") == 2);
        Assert.assertNotNull(jobj.get("links"));
        JSONArray links = jobj.getJSONArray("links");
        Assert.assertTrue(links.size() == 1);
	}
}
