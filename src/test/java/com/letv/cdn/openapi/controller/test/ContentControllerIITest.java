package com.letv.cdn.openapi.controller.test;
/**
 * 分发接口ver 0.2 测试用例
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
public class ContentControllerIITest extends BasicControllerTest {
	
	
private static final Logger log = LoggerFactory.getLogger(ContentControllerIITest.class);
	
	private static final String DOMAIN_TAG = "apitest";
	//private static final String TEST_URL = "http://api.cdn.lecloud.com";
	private static final String TEST_URL = "http://localhost:8084";
	
    private HttpEntity<String> getHttpEntity(String method, String uri, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add("Authorization", getBase64(Constant.USERID, method, uri, Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        return entity;
    }
    
    String rfKey = null ;
	String[] keys = new String[] { "", "", "", "",""}; 
	String[] srcs = new String[] { "http://115.182.94.184/src/common11.apitest",
								   "http://115.182.94.184/src/common12.apitest",
								   "http://115.182.94.184/src/common13.apitest",
								   "http://115.182.94.184/src/common14.apitest", 
								   "http://115.182.94.184/src/common15.apitest"}; 
	String[] md5s = new String[] {"0A840188266B31998C45112DCA1BCB02",
								  "AF0DAABB93F8C2F6841FF14AB12946AC",
								  "CCE83455F2FD970D485FAD74DA2586D7",
								  "C9F963BCF16EEDE4A659CCF1509D1A4E",
								  "B25B9810C43E4A61F21671FA1D342B47"};
	/**
	 * 1.正常提交有key无key,
	 */
	//@Test
	public void testSubfile1() {
		String[] keys = new String[] { "testapi0011", "testapi0012", "", "","testapi0015"}; 
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
		//rq.put("method", "refresh"); method 默认和传值2种情况
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
			return;
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
	 * 重复提交任务 包括outkey重复 cdn 返回重复
	 */
	//@Test
	public void testSubfile2(){
		 
		testSubfile1();
	}
	
	
	
	/**
	 * 1.刷新时测试删除和提交都成功情况
	 */
	//@Test
	public void updatefile1() {
		String[] keys = new String[] { "testapi001", "testapi002", "", "",""};
		String[] oldkeys = new String[] {"","","","","" };
//		String[] keys = new String[] { ""};
//		String[] oldkeys = new String[] {""};
	//	String[] srcs = new String[] {"http://115.182.94.184/src/common15.apitest"};
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("oldkey", oldkeys[i]);
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("method", "refresh");// 刷新
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,
				rq.toJSONString());
		String url = TEST_URL  + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException) e.getCause();
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			return;
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

		JSONObject jo = JSONObject.parseObject(rst.getBody());
		JSONArray jsonArray = jo.getJSONArray("result");
		String ststus = jsonArray.getJSONObject(3).getString("status");// 随机
		Assert.assertEquals("1", ststus);
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		log.info("response:" + jo.toJSONString());
	}
	
	/**
	 * 1.测试删除成功情况(有oldkey和无oldkey)
	 */
	//@Test
    public void deletefile() {
    	String[] oldkeys = new String[] { "testapi001", "testapi002", "", "",""};
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
            return;
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
     * 测试重复删除
     */
	//@Test
    public void deletefile2(){
    	deletefile();
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
            return;
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
	 * 1.测试刷新时 删除全部失败情况
	 */
	//@Test
    public void updatefile() {
        JSONArray items = new JSONArray();
        for (int i = 0; i < keys.length; i++) {
            JSONObject task = new JSONObject();
            task.put("key", keys[i]);
            task.put("src", srcs[i]);
            items.add(task);
        }
        JSONObject rq = new JSONObject();
        rq.put("domaintag", DOMAIN_TAG);
        rq.put("items", items);
        rq.put("method","refresh");//刷新
        String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
		String url = TEST_URL  + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException) e.getCause();
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			return;
		}
        // 删除是否成功
        Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
        JSONObject jo = JSONObject.parseObject(rst.getBody());
        JSONArray jsonArray = jo.getJSONArray("result");
        String status = jsonArray.getJSONObject(1).getString("status");
        Assert.assertEquals("0", status); //随机测试
        // 结果数量与提交任务数量是否一致
     	Assert.assertEquals(keys.length, jsonArray.size());
        log.info("response:" + jo.toJSONString());
    }
	
    
   
    /**
     * 1.测试刷新全部删除成功但全部提交失败(提交方法抛出异常)
     */
 	 //@Test
     public void updatefile2() {
         JSONArray items = new JSONArray();
         for (int i = 0; i < keys.length; i++) {
             JSONObject task = new JSONObject();
             task.put("key", keys[i]);//不传oldkey
             task.put("src", srcs[i]);
             items.add(task);
         }
         JSONObject rq = new JSONObject();
         rq.put("domaintag", DOMAIN_TAG);
         rq.put("method","refresh");//刷新
         rq.put("items", items);
         String uri = "/cdn/content";
         HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
 		String url = TEST_URL  + uri;
 		ResponseEntity<String> rst = null;
 		try {
 			rst = restTemplate.postForEntity(url, entity, String.class);
 		} catch (Exception e) {
 			RestTemplateException ex = (RestTemplateException) e.getCause();
 			JSONObject jo = new JSONObject(ex.properties);
 			log.error(jo.toJSONString(), e);
 			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
 			return;
 		}
         // 提交是否成功
         Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());
         

         JSONObject jo = JSONObject.parseObject(rst.getBody());
         JSONArray jsonArray = jo.getJSONArray("result");
         for(int i = 0 ; i < jsonArray.size();i++){
        	 String ststus = jsonArray.getJSONObject(i).getString("status");
             Assert.assertEquals("3", ststus);
         }
         // 结果数量与提交任务数量是否一致
      	Assert.assertEquals(keys.length, jsonArray.size());
         log.info("response:" + jo.toJSONString());
     }
 	/**
 	 * 提交成功情况
 	 */
    // @Test
     public void testSubfile4() {
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
 		//rq.put("method", "refresh"); method 默认和传值2种情况
 		String uri = "/cdn/content";
 		HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
 		String url = TEST_URL  + uri;
 		ResponseEntity<String> rst = null;
 		try {
 			rst = restTemplate.postForEntity(url, entity, String.class);
 		} catch (Exception e) {
 			RestTemplateException ex = (RestTemplateException) e.getCause();
 			JSONObject jo = new JSONObject(ex.properties);
 			log.error(jo.toJSONString(), e);
 			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
 			return;
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
      * 1.测试刷新时 部分删除成功和已成功的部分提交成功
      */
	//@Test
	public void updatefile3() {//测试结果1个删除失败 4个提交成功 
		String[] srcs = new String[] { "http://115.182.94.184/src/common44.apitest",//错误的src
				   "http://115.182.94.184/src/common12.apitest",
				   "http://115.182.94.184/src/common13.apitest",
				   "http://115.182.94.184/src/common14.apitest", 
				   "http://115.182.94.184/src/common15.apitest",}; 
		
		String[] oldkeys = new String[] { "apitest01011ffffffffff1",////错误的key
										  "13ad397b9b484e208cc6ca65e73dcbdd1",
										  "eb7cbde0a47744f7b91e107d7d554aef",
										  "18fa8b2eee81643e4809d37b42c672548",
										  "oldkey"
										  };//错误的oldkey 
		String[] keys = new String[] { "", "", "", "",""}; 
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			task.put("oldkey", oldkeys[i]);
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
			items.add(task);
		}
		JSONObject rq = new JSONObject();
		rq.put("domaintag", DOMAIN_TAG);
		rq.put("method", "refresh");// 刷新
		rq.put("items", items);
		String uri = "/cdn/content";
		HttpEntity<String> entity = getHttpEntity("POST", uri,rq.toJSONString());
		String url = TEST_URL  + uri;
		ResponseEntity<String> rst = null;
		try {
			rst = restTemplate.postForEntity(url, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException) e.getCause();
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			return;
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

		JSONObject jo = JSONObject.parseObject(rst.getBody());
		JSONArray jsonArray = jo.getJSONArray("result");
		int s = 0;
		int f = 0;
		int sf= 0 ;
		for (int i = 0; i < jsonArray.size(); i++) {
			String status = jsonArray.getJSONObject(i).getString("status");
			
			if(Integer.parseInt(status)==1){//提交成功
				s++;
				rfKey = jsonArray.getJSONObject(i).getString("key");
			}
			if(Integer.parseInt(status)==0){//删除提交失败
				f++;
			}
			if(Integer.parseInt(status)==3){//提交失败
				sf++ ;
			}
		}
		Assert.assertEquals("1", String.valueOf(f));
		Assert.assertEquals("4", String.valueOf(s));
		
		// 结果数量与提交任务数量是否一致
		Assert.assertEquals(keys.length, jsonArray.size());
		log.info("response:" + jo.toJSONString());
	}
	
	
	 /**
     * 1.测试提交时  key重复 1个key重复 提交失败 1个提交重复
     */
	//@Test
	public void subfile3() {//测试结果1个分发成功 1个outkey重复 
		
		String[] srcs = new String[] {"http://115.182.94.184/src/common6.apitest",//key重复失败 
									  "http://115.182.94.184/src/common12.apitest"}; //重复
		 String[] keys = new String[] { "b99b4b2f71a54ed0a574586e9551ba3d", //重复key
				 						""};//key值不传
		JSONArray items = new JSONArray();
		for (int i = 0; i < keys.length; i++) {
			JSONObject task = new JSONObject();
			
			task.put("key", keys[i]);
			task.put("src", srcs[i]);
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
			RestTemplateException ex = (RestTemplateException) e.getCause();
			JSONObject jo = new JSONObject(ex.properties);
			log.error(jo.toJSONString(), e);
			Assert.assertTrue(StringUtils.hasLength(ex.properties.get("body").toString()));
			return;
		}
		// 提交是否成功
		Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

		JSONObject jo = JSONObject.parseObject(rst.getBody());
		JSONArray jsonArray = jo.getJSONArray("result");
		int s = 0;
		int f = 0;
		for (int i = 0; i < jsonArray.size(); i++) {
			String status = jsonArray.getJSONObject(i).getString("status");
			
			if(Integer.parseInt(status)==0){//key重复 提交失败
				s++;
			}
			if(Integer.parseInt(status)==2){//提交重复
				f++ ;
			}
		}
		Assert.assertEquals("1", String.valueOf(f));
		Assert.assertEquals("1", String.valueOf(s));
		
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
				getBase64(Constant.USERID, "GET", "/cdn/content/127022_testapi0015/status", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> resultEntity = null ;
        try{
        	resultEntity = restTemplate.exchange(TEST_URL +"//cdn/content/127022_testapi0015/status", HttpMethod.GET, entity, String.class);
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
        Assert.assertEquals(4, jobj.get("status"));
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
	/**
	 * 最后删除
	 */
	@Test
	public void delete4(){
		String[] oldkeys = new String[] { "", "", "", "",""};
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
            return;
        }
        // 删除是否成功
        Assert.assertEquals(HttpStatus.OK.value(), rst.getStatusCode().value());

        JSONObject jo = JSONObject.parseObject(rst.getBody());
        JSONArray jsonArray = jo.getJSONArray("result");
        // 结果数量与提交任务数量是否一致
     	Assert.assertEquals(keys.length, jsonArray.size());
        log.info("response:" + jo.toJSONString());
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
