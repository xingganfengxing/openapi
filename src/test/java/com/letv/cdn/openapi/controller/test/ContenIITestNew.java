package com.letv.cdn.openapi.controller.test;
/**
 * 刷新接口ver 0.2 测试用例
 */
import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
/**
 * 
 * @author liuchangfu
 *
 */
public class ContenIITestNew extends BasicControllerTest {
	
	
private static final Logger log = LoggerFactory.getLogger(ContentControllerIITest.class);
	
	private static final String DOMAIN_TAG = "cdntest";
	private static final String TEST_URL = "http://api.cdn.lecloud.com";
	//private static final String TEST_URL = "http://111.206.210.204:9002";
	//private static final String TEST_URL = "http://localhost:8084";
	
    private HttpEntity<String> getHttpEntity(String method, String uri, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add("Authorization", getBase64(Constant.USERID, method, uri, Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        return entity;
    }
    
	String[] srcs = new String[] {"http://123.126.32.31:8888/xml.rmvb ",
								  "http://123.126.32.31:8888/FF-15s-1080p.avi"
								  }; 
	String[] keys = new String[] {"",""}; 
	String[] md5s = new String[] {"bd99dfd5ed6be192a2986b105df9a858","74ec66f1fa1488baeb0c683793cdb093"};
	/**
	 * 1.刷新正常提交(包括有key和无key及key为空格)
	 */
	@Test
	public void testSubfile1() {
		//String[] keys = new String[] { "",""};
		String[] srcs = new String[] {"http://text-ad.kuaibo.com/yunfancdn.txt"};
		String[] keys = new String[] {""};
		String[] md5s = new String[] {"44bda20614c0bd0e34278c0506ae35ac"};
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
		String url = TEST_URL  + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try{
				ex = (RestTemplateException) e.getCause();
			}catch(Exception e1){
				log.error(e1.toString());
				return ;
			}
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			throw new RuntimeException();
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
		JSONObject jobj = JSONObject.parseObject(rst.getBody());
		log.info("response:" + jobj.toJSONString());
		JSONArray jsonArray = jobj.getJSONArray("result");
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jo = (JSONObject) jsonArray.get(i);
			// 是否所有任务都返回了正确的key
			if (keys[i].equals("")) {
				Assert.assertTrue(!"".equals(jo.getString("key")));
			} else {
				// Assert.assertEquals(keys[i], jo.getString("key"));
			}
		}
	}
	/**
	 * 重复提交任务 
	 */
	//@Test
	public void testSubfile2(){
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			//task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
		String url = TEST_URL  + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try{
				ex = (RestTemplateException) e.getCause();
			}catch(Exception e1){
				log.error(e1.toString());
				return ;
			}
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			throw new RuntimeException();
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
		JSONObject jobj = JSONObject.parseObject(rst.getBody());
		log.info("response:" + jobj.toJSONString());
		JSONArray jsonArray = jobj.getJSONArray("result");
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jo = (JSONObject) jsonArray.get(i);
			int status = jo.getIntValue("status");
			//状态码是否为2
			Assert.assertTrue(status==2);
			// 是否所有任务都返回了正确的key
			if (keys[i].equals("")) {
				Assert.assertTrue(!"".equals(jo.getString("key")));
			} else {
				// Assert.assertEquals(keys[i], jo.getString("key"));
			}
		}
	}
	//outKey重复
	//@Test
	public void testSubfile3() {
		String[] keys = new String[] { "2015test001"};
		String[] srcs = new String[] {"http://115.182.94.184/src/commontest001.apitest"};
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			// task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,
				rq.toJSONString());
		String url = TEST_URL + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try {
				ex = (RestTemplateException) e.getCause();
			} catch (Exception e1) {
				log.error(e1.toString());
				return;
			}
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body")
					.toString()));
			throw new RuntimeException();
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
		JSONObject jobj = JSONObject.parseObject(rst.getBody());
		log.info("response:" + jobj.toJSONString());
		JSONArray jsonArray = jobj.getJSONArray("result");
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jo = (JSONObject) jsonArray.get(i);
			int status = jo.getIntValue("status");
			// 状态码是否为2
			Assert.assertTrue(status == 5);
			// 是否所有任务都返回了正确的key
			if (keys[i].equals("")) {
				Assert.assertTrue(!"".equals(jo.getString("key")));
			} else {
				// Assert.assertEquals(keys[i], jo.getString("key"));
			}
		}
	}
	//测试刷新先删除在提交(前提status回调为1)
	//@Test
	public void testSubfile5() {
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			// task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,
				rq.toJSONString());
		String url = TEST_URL + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try {
				ex = (RestTemplateException) e.getCause();
			} catch (Exception e1) {
				log.error(e1.toString());
				return;
			}
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body")
					.toString()));
			throw new RuntimeException();
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
		JSONObject jobj = JSONObject.parseObject(rst.getBody());
		log.info("response:" + jobj.toJSONString());
		JSONArray jsonArray = jobj.getJSONArray("result");
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jo = (JSONObject) jsonArray.get(i);
			int status = jo.getIntValue("status");
			// 状态码是否为1
			Assert.assertTrue(status == 1);
			// 是否所有任务都返回了正确的key
			if (keys[i].equals("")) {
				Assert.assertTrue(!"".equals(jo.getString("key")));
			} else {
				// Assert.assertEquals(keys[i], jo.getString("key"));
			}
		}
	}
	//测试刷新删除失败返回失败(前提status回调为1)
	//代码内模拟 flag=false(模拟失败)
	//@Test
	public void testSubfile7() {
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			// task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,
				rq.toJSONString());
		String url = TEST_URL + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try {
				ex = (RestTemplateException) e.getCause();
			} catch (Exception e1) {
				log.error(e1.toString());
				return;
			}
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body")
					.toString()));
			throw new RuntimeException();
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
		JSONObject jobj = JSONObject.parseObject(rst.getBody());
		log.info("response:" + jobj.toJSONString());
		JSONArray jsonArray = jobj.getJSONArray("result");
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jo = (JSONObject) jsonArray.get(i);
			int status = jo.getIntValue("status");
			// 状态码是否为0
			Assert.assertTrue(status == 0);
			// 是否所有任务都返回了正确的key
			if (keys[i].equals("")) {
				Assert.assertTrue(!"".equals(jo.getString("key")));
			} else {
				// Assert.assertEquals(keys[i], jo.getString("key"));
			}
		}
	}
		
	
	//测试刷新先删除在提交(前提status回调为1，并修改文件大小)
	//@Test
	public void testSubfile6() {
		String[] keys = new String[] { "2015test002","","","",""};
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			// task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,
				rq.toJSONString());
		String url = TEST_URL + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try {
				ex = (RestTemplateException) e.getCause();
			} catch (Exception e1) {
				log.error(e1.toString());
				return;
			}
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body")
					.toString()));
			throw new RuntimeException();
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
		JSONObject jobj = JSONObject.parseObject(rst.getBody());
		log.info("response:" + jobj.toJSONString());
		JSONArray jsonArray = jobj.getJSONArray("result");
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject jo = (JSONObject) jsonArray.get(i);
			int status = jo.getIntValue("status");
			// 状态码是否为1
			Assert.assertTrue(status == 1);
			// 是否所有任务都返回了正确的key
			if (keys[i].equals("")) {
				Assert.assertTrue(!"".equals(jo.getString("key")));
			} else {
				// Assert.assertEquals(keys[i], jo.getString("key"));
			}
		}
	}

	/**
	 * 1.测试删除成功情况(有oldkey和无oldkey)
	 */
	//@Test
    public void deletefile() {
    	String[] oldkeys = new String[] { "",""};
        JSONArray items = new JSONArray();
        for (int i = 0; i < oldkeys.length; i++) {
            JSONObject task = new JSONObject();
            task.put("oldkey", oldkeys[i]);
            task.put("src", srcs[i]);
            items.add(task);
        }
        JSONObject rq = new JSONObject();
        rq.put("domaintag", DOMAIN_TAG);
        rq.put("items", items);
        String uri = "/cdn/content";
        HttpEntity<String> entity = getHttpEntity("DELETE", uri, rq.toJSONString());
        String url = TEST_URL  + uri;
        ResponseEntity<String> rst = null;
        try {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
                @Override
                protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
                    if (HttpMethod.DELETE == httpMethod) {
                        return new HttpEntityEnclosingDeleteRequest(uri);
                    }
                    return super.createHttpUriRequest(httpMethod, uri);
                }
            });
            rst = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
        	RestTemplateException ex = new RestTemplateException() ;
        	try{
        		ex = (RestTemplateException) e.getCause();
        	}catch(Exception e1){
        		log.error(e1.toString());
        		return ;
        	}
            JSONObject jo = new JSONObject(ex.properties);
            log.error(jo.toJSONString(), e);
            Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
            throw new RuntimeException();
        }
        // 删除是否成功
        Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

        JSONObject jo = JSONObject.parseObject(rst.getBody());
        JSONArray jsonArray = jo.getJSONArray("result");
        JSONArray jsonArray1 = jo.getJSONArray("links");
       String delkey = jsonArray.getJSONObject(0).getString("deletedKey");
       //delkey 是否有值
       Assert.assertNotNull(delkey);
       Assert.assertNotNull(jsonArray1);
        // 结果数量与提交任务数量是否一致
     	Assert.assertEquals(keys.length, jsonArray.size());
        log.info("response:" + jo.toJSONString());
    }
    /**
     * 测试重复删除
     */
	//@Test
    public void deletefile2(){
        JSONArray items = new JSONArray();
        for (int i = 0; i < keys.length; i++) {
            JSONObject task = new JSONObject();
            task.put("src", srcs[i]);
            task.put("key", keys[i]);
            items.add(task);
        }
        JSONObject rq = new JSONObject();
        rq.put("domaintag", DOMAIN_TAG);
        rq.put("items", items);
        String uri = "/cdn/content";
        HttpEntity<String> entity = getHttpEntity("DELETE", uri, rq.toJSONString());
        String url = TEST_URL  + uri;
        ResponseEntity<String> rst = null;
        try {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
                @Override
                protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
                    if (HttpMethod.DELETE == httpMethod) {
                        return new HttpEntityEnclosingDeleteRequest(uri);
                    }
                    return super.createHttpUriRequest(httpMethod, uri);
                }
            });
            rst = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
        	RestTemplateException ex = new RestTemplateException() ;
        	try{
        		ex = (RestTemplateException) e.getCause();
        	}catch(Exception e1){
        		log.error(e1.toString());
        		return ;
        	}
            JSONObject jo = new JSONObject(ex.properties);
            log.error(jo.toJSONString(), e);
            Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
            throw new RuntimeException();
        }
        // 删除是否成功
        Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

        JSONObject jo = JSONObject.parseObject(rst.getBody());
        JSONArray jsonArray = jo.getJSONArray("result");
        JSONArray jsonArray1 = jo.getJSONArray("links");
       String delkey = jsonArray.getJSONObject(0).getString("deletedKey");
       //delkey 是否有值
       Assert.assertEquals("[]", delkey);;
       Assert.assertNotNull(jsonArray1);
        // 结果数量与提交任务数量是否一致
     	Assert.assertEquals(keys.length, jsonArray.size());
        log.info("response:" + jo.toJSONString());
    }
	/**
     * 测试资源不存删除
     */
	//@Test
    public void deletefile3(){
		String[] keys = new String[] { "", ""};
		String[] oldkeys = new String[] { "", ""};
		String[] srcs = new String[] { "http://115.182.94.184/src/common001.apitest",//错误的资源
				   						"http://115.182.94.184/src/common002.apitest"//错误的资源
				   																	}; 
        JSONArray items = new JSONArray();
        for (int i = 0; i < oldkeys.length; i++) {
            JSONObject task = new JSONObject();
            task.put("oldkey", oldkeys[i]);
            task.put("src", srcs[i]);
            items.add(task);
        }
        JSONObject rq = new JSONObject();
        rq.put("domaintag", DOMAIN_TAG);
        rq.put("items", items);
        String uri = "/cdn/content";
        HttpEntity<String> entity = getHttpEntity("DELETE", uri, rq.toJSONString());
        String url = TEST_URL  + uri;
        ResponseEntity<String> rst = null;
        try {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
                @Override
                protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
                    if (HttpMethod.DELETE == httpMethod) {
                        return new HttpEntityEnclosingDeleteRequest(uri);
                    }
                    return super.createHttpUriRequest(httpMethod, uri);
                }
            });
            rst = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
        	RestTemplateException ex = new RestTemplateException() ;
        	try{
        		ex = (RestTemplateException) e.getCause();
        	}catch(Exception e1){
        		log.error(e1.toString());
        		return ;
        	}
            JSONObject jo = new JSONObject(ex.properties);
            log.error(jo.toJSONString(), e);
            Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
            throw new RuntimeException();
        }
        // 删除是否成功
        Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

        JSONObject jo = JSONObject.parseObject(rst.getBody());
        JSONArray jsonArray = jo.getJSONArray("result");
        // 结果数量与提交任务数量是否一致
     	Assert.assertEquals(keys.length, jsonArray.size());
        log.info("response:" + jo.toJSONString());
    }
	/**
	 * 查询分发任务存在时
	 */
	//@Test
	public void testQueryFile() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/content/127022_38e38478a56d457c861e35a8142445a4/status", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> resultEntity = null ;
        try{
        	resultEntity = restTemplate.exchange(TEST_URL +"//cdn/content/127022_38e38478a56d457c861e35a8142445a4/status", HttpMethod.GET, entity, String.class);
        }catch(Exception e ){
        	Assert.assertNotNull(e);
        	RestTemplateException ex = (RestTemplateException) e.getCause();
        	
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			return;
        }
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        //Assert.assertEquals(4, jobj.get("status"));
        log.info("response:" + jobj.toJSONString());
	}
	
	
	/**
	 * 查询分发任务不存在时
	 */
	//@Test
	public void testQueryFile1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/content/127022_abcdfrtt/status", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        try{
        	ResponseEntity<String> resultEntity = restTemplate.exchange(TEST_URL +"/cdn/content/127022_abcdfrtt/status", HttpMethod.GET, entity, String.class);
        }catch(Exception e ){
        	Assert.assertNotNull(e);
        	RestTemplateException ex = (RestTemplateException) e.getCause();
			JSONObject jo = new JSONObject(ex.properties);
			Assert.assertEquals(404, ex.properties.get("code"));
			log.info("response:" + jo.toJSONString());
        }
	}
	//测试刷新 参数各种异常情况 (修改相关参数) 
	//@Test
	public void testSubfile8() {
		String[] keys = new String[] { "","  ","","",""};
		String[] srcs = new String[] { "shdjshsh","","","",""};
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			//task.put("md5", md5s[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
		String url = TEST_URL  + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = new RestTemplateException();
			try{
				ex = (RestTemplateException) e.getCause();
			}catch(Exception e1){
				log.error(e1.toString());
				return ;
			}
			log.info("response:" + ex.properties.get("body").toString());
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			// 返回状态码及信息
			Assert.assertEquals(400, ex.properties.get("code"));
		}
	}
	//测试删除 参数各种异常情况 (修改相关参数) 
	//@Test
	public void deletefile4(){
		String[] keys = new String[] { "","  ","","",""};
		String[] srcs = new String[] { "  ","","","",""};
        JSONArray items = new JSONArray();
        for (int i = 0; i < keys.length; i++) {
            JSONObject task = new JSONObject();
            task.put("src", srcs[i]);
            task.put("key", keys[i]);
            items.add(task);
        }
        JSONObject rq = new JSONObject();
        rq.put("domaintag", DOMAIN_TAG);
        rq.put("items", items);
        String uri = "/cdn/content";
        HttpEntity<String> entity = getHttpEntity("DELETE", uri, rq.toJSONString());
        String url = TEST_URL  + uri;
        ResponseEntity<String> rst = null;
        try {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
                @Override
                protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
                    if (HttpMethod.DELETE == httpMethod) {
                        return new HttpEntityEnclosingDeleteRequest(uri);
                    }
                    return super.createHttpUriRequest(httpMethod, uri);
                }
            });
            rst = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
        	RestTemplateException ex = new RestTemplateException();
			try{
				ex = (RestTemplateException) e.getCause();
			}catch(Exception e1){
				log.error(e1.toString());
				return ;
			}
			log.info("response:" + ex.properties.get("body").toString());
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			// 返回状态码及信息
			Assert.assertEquals(400, ex.properties.get("code"));
        }
    }
	
	public static class HttpEntityEnclosingDeleteRequest extends HttpEntityEnclosingRequestBase  {  
	    public HttpEntityEnclosingDeleteRequest(final URI uri) {  
	        super();  
	        setURI(uri);  
	    }  

	    public String getMethod() {  
	        return "DELETE";  
	    }  
	}
}
