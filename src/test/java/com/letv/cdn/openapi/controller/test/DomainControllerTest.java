package com.letv.cdn.openapi.controller.test;

import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.utils.ErrorMsg;
import com.letv.cdn.openapiauth.utils.MD5;


/**
 * {@link DomainController}测试用例<br>
 * 通过单元测试用例的顺序控制，将测试用例产生的脏数据清除<br>
 * @author gao.jun
 *
 */
public class DomainControllerTest extends BasicControllerTest {
	
//	private static final String LOCAL_URI_PREFIX = "http://api.cdn.lecloud.com/cdn/domain";
	private static final String LOCAL_URI_PREFIX = "http://localhost:8084/cdn/domain";
//	private static final String LOCAL_URI_PREFIX = "http://111.206.210.204:9002/cdn/domain";

	private static final String DOMAINTAG = "alexgaotest";
	
	private static final String KEY_URI = Constant.USERID + "_" + DOMAINTAG;
	
	/**
	 * 测试新增域名配置-V0.2版本<br>
	 * 正常保存
	 */
	@Test
	public void testAdd1() {
		HttpEntity<String> entity = preparedAddHttpEntity();
        ResponseEntity<String> resultEntity = 
        		restTemplate.postForEntity(LOCAL_URI_PREFIX, entity, String.class);
        Assert.assertEquals(201, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
		Assert.assertEquals(DOMAINTAG, jobj.get("domaintag"));
	}
	
	
	/**
	 * 测试新增域名配置-V0.2版本<br>
	 * domaintag重复
	 */
	@Test
	public void testAdd2() {
		
		HttpEntity<String> entity = preparedAddHttpEntity();
        
        // 发送post请求，并获取返回结果
        try {
        	restTemplate.postForObject(LOCAL_URI_PREFIX, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(400,ex.properties.get("code"));
		}
 		
	}
	
	/**
	 * 启用域名加速
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 */
	@Test
	public void testEnable() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "PUT", "/cdn/domain/" + KEY_URI
						+ "/flag", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("flag", "1");
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI)
				.concat("/flag"), HttpMethod.PUT, entity, String.class);
		
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
		Assert.assertEquals(1, jobj.get("flag"));
	}
	
	/**
	 * 测试查询域名配置信息-V0.2版本<br>
	 * 正常获取域名配置信息
	 */
	@Test
	public void testQuery1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/domain/".concat(KEY_URI), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), HttpMethod.GET, entity, String.class);
	
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("alexgaotest", jobj.get("domaintag"));
        Assert.assertEquals("cdn.alexgaotest.net", jobj.get("domain"));
        Assert.assertEquals("www.alexgaotest.net", jobj.get("source"));
        Assert.assertEquals(9, jobj.get("type"));
        Assert.assertEquals(1, jobj.get("flag"));
        Assert.assertEquals("测试用例", jobj.get("remark"));
        Assert.assertEquals("coop.gslb.letv.com", jobj.get("cname"));
	}
	
	/**
	 * 测试查询域名配置信息-V0.2版本<br>
	 * 未查询到域名配置信息
	 */
	@Test
	public void testQuery2() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/domain/".concat(KEY_URI).concat("1"), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        try {
        	restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI).concat("1"), HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(404,ex.properties.get("code"));
		}
	}
	
	
	
	/**
	 * 测试删除域名配置信息-V0.2版本<br>
	 * <br>
	 * 未查询到域名信息
	 * 2014年12月19日
	 * @author gao.jun
	 */
	@Test
	public void testDel1() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/domain/".concat(KEY_URI).concat("1"), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        try {
        	restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI).concat("1"), HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(404,ex.properties.get("code"));
		}
	}
	
	/**
	 * 测试删除域名配置信息-V0.2版本<br>
	 * <br>
	 * 域名配置现为启用状态，只有禁用的域名方可删除
	 * 2014年12月19日
	 * @author gao.jun
	 */
	@Test
	public void testDel3() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/domain/".concat(KEY_URI), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        try {
        	restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(400,ex.properties.get("code"));
			JSONObject errorObj = JSONObject.parseObject(ex.properties.get("body").toString());
			Assert.assertNotNull(errorObj.get("error"));
			JSONObject msgObj = (JSONObject)errorObj.get("error");
			Assert.assertEquals(ErrorMsg.DOMAIN_IS_ENABLED.getCode(),msgObj.get("code"));
			
		}
	}
	
	
	
	/**
	 * 正常修改域名配置-V0.2版本<br>
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 */
	@Test
	public void testUpdate1() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "PUT", "/cdn/domain/".concat(KEY_URI), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domain", "cdn.alexgaotest1.net");
        jsonObj.put("source", "www.alexgaotest1.net");
        jsonObj.put("remark", "测试用例1");
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), HttpMethod.PUT, entity, String.class);
        
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("alexgaotest", jobj.get("domaintag"));
        Assert.assertEquals("cdn.alexgaotest1.net", jobj.get("domain"));
        Assert.assertEquals("www.alexgaotest1.net", jobj.get("source"));
        Assert.assertEquals(9, jobj.get("type"));
        Assert.assertEquals(1, jobj.get("flag"));
        Assert.assertEquals("测试用例1", jobj.get("remark"));
        Assert.assertEquals("coop.gslb.letv.com", jobj.get("cname"));
	}
	
	/**
	 * 未找到要修改的域名配置信息-V0.2版本<br>
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 */
	@Test
	public void testUpdate3() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "PUT", "/cdn/domain/".concat(KEY_URI).concat("1"), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domain", "cdn.alexgaotest.net");
        jsonObj.put("source", "www.alexgaotest.net");
        jsonObj.put("remark", "测试用例");
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
        try {
        	restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI).concat("1"), HttpMethod.PUT, entity, String.class);
        } catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(404,ex.properties.get("code"));
		}
	}
	
	/**
	 * 还原修改前的域名配置-V0.2版本<br>
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 */
	@Test
	public void testUpdate2() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "PUT", "/cdn/domain/".concat(KEY_URI), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domain", "cdn.alexgaotest.net");
        jsonObj.put("source", "www.alexgaotest.net");
        jsonObj.put("remark", "测试用例");
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), HttpMethod.PUT, entity, String.class);
        
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("alexgaotest", jobj.get("domaintag"));
        Assert.assertEquals("cdn.alexgaotest.net", jobj.get("domain"));
        Assert.assertEquals("www.alexgaotest.net", jobj.get("source"));
        Assert.assertEquals(9, jobj.get("type"));
        Assert.assertEquals(1, jobj.get("flag"));
        Assert.assertEquals("测试用例", jobj.get("remark"));
        Assert.assertEquals("coop.gslb.letv.com", jobj.get("cname"));
	}
	
	/**
	 * 禁用域名加速测试-V0.2版本<br>
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 */
	@Test
	public void testDisable1() {
		
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "PUT", "/cdn/domain/".concat(KEY_URI)
						+ "/flag", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("flag", "0");
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(),headers);
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI).concat("/flag"), HttpMethod.PUT, entity, String.class);
		
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
		Assert.assertEquals(0, jobj.get("flag"));
	}
	
	/**
	 * 测试删除域名配置信息-V0.2版本<br>
	 * <br>
	 * 正常删除域名配置信息
	 * 2014年12月19日
	 * @author gao.jun
	 */
	@Test
	public void testDel2() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/domain/".concat(KEY_URI), Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), HttpMethod.DELETE, entity, String.class);
        
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
	}
	
	/**
	 * 测试新增域名配置 v0.1
	 * <br>
	 * 正常新增域名
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testAdd3() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", "alexgaotest");
        jsonObj.put("domain", "cdn.alexgaotest.net");
        jsonObj.put("source", "www.alexgaotest.net");
        jsonObj.put("remark", "测试用例");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        ResponseEntity<String> resultEntity = 
        		restTemplate.postForEntity(LOCAL_URI_PREFIX, entity, String.class);
        Assert.assertEquals(201, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
		Assert.assertEquals("alexgaotest", jobj.get("domaintag"));
	}
	
	/**
	 * 测试新增域名配置 v0.1
	 * <br>
	 * 域名重复
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testAdd4() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", "alexgaotest");
        jsonObj.put("domain", "cdn.alexgaotest.net");
        jsonObj.put("source", "www.alexgaotest.net");
        jsonObj.put("remark", "测试用例");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        try {
        		restTemplate.postForEntity(LOCAL_URI_PREFIX, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(500,ex.properties.get("code"));
		}
	}
	
	/**
	 * 修改域名配置 v0.1
	 * <br>
	 * 正常修改
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testUpdate4() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domain", "cdn.alexgaotest1.net");
        jsonObj.put("source", "www.alexgaotest1.net");
        jsonObj.put("remark", "测试用例");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), 
        		HttpMethod.POST, entity, String.class);
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("cdn.alexgaotest1.net", jobj.get("domain"));
        Assert.assertEquals("www.alexgaotest1.net", jobj.get("source"));
	}
	
	/**
	 * 修改域名配置 v0.1
	 * <br>
	 * 未找到对应域名信息
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testUpdate5() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domain", "cdn.alexgaotest1.net");
        jsonObj.put("source", "www.alexgaotest1.net");
        jsonObj.put("remark", "测试用例");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        try {
	        restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI).concat("1"), 
	        		HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(404,ex.properties.get("code"));
		}
	}
	
	/**
	 * 修改域名配置 v0.1
	 * <br>
	 * 还原修改
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testUpdate6() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("domain", "cdn.alexgaotest.net");
        jsonObj.put("source", "www.alexgaotest.net");
        jsonObj.put("remark", "测试用例");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
        HttpEntity<String> entity = new HttpEntity<String>(strB.toString(),headers);
        
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI), 
        		HttpMethod.POST, entity, String.class);
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("cdn.alexgaotest.net", jobj.get("domain"));
        Assert.assertEquals("www.alexgaotest.net", jobj.get("source"));
	}
	
	/**
	 * 查询域名配置信息 v0.1
	 * <br>
	 * 正常查询
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testQuery3() {
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI)
				.concat("?").concat(strB.toString()), HttpMethod.GET, null, String.class);

		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertEquals("alexgaotest", jobj.get("domaintag"));
        Assert.assertEquals("cdn.alexgaotest.net", jobj.get("domain"));
        Assert.assertEquals("www.alexgaotest.net", jobj.get("source"));
	}
	
	/**
	 * 查询域名配置信息  v0.1
	 * <br>
	 * 未查询到域名信息
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testQuery4() {
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
    	try {
			restTemplate.exchange(LOCAL_URI_PREFIX.concat("/").concat(KEY_URI)
					.concat("1?").concat(strB.toString()), HttpMethod.GET, null, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(404,ex.properties.get("code"));
		}finally {
			this.testDisable1();
			this.testDel2();
		}
    	
	}

	private HttpEntity<String> preparedAddHttpEntity() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add("Authorization", getBase64(Constant.USERID, "POST", "/cdn/domain", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", "alexgaotest");
        jsonObj.put("domain", "cdn.alexgaotest.net");
        jsonObj.put("source", "www.alexgaotest.net");
        jsonObj.put("type", 9);
        jsonObj.put("remark", "测试用例");
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(),headers);
		return entity;
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


