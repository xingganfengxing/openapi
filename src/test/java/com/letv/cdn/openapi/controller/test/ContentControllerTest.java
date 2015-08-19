package com.letv.cdn.openapi.controller.test;

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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.controller.ContentApiIIController;
import com.letv.cdn.openapi.utils.Env;

public class ContentControllerTest extends BasicControllerTest {

    private static final Logger log = LoggerFactory.getLogger(ContentApiIIController.class);
	
	private static final String DOMAIN_TAG = "apitest";
	
    private HttpEntity<String> getHttpEntity(String method, String uri, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add("Authorization", getBase64(Constant.USERID, method, uri, Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        return entity;
    }
    
	/*
	 * 1. 正常提交多个批量任务（包括有key和无key的情况）
	 * 2. 提交重复任务
	 * 3. 正常提交多个批量刷新任务（包括只有key和只有src的情况）
	 * 4. 提交不存在的刷新任务
	 * 5. 提交未成功的刷新任务
	 * 6. 正常删除任务
	 * 7. 测试删除失败的情况
	 * 8. 测试删除成功提交失败的情况
	 */
	
    String[] keys = new String[] {"addtest11",
                                  "addtest12",
                                  "",
                                  "",
                                  "addtest15",
                                  "addtest16",
                                  "addtest17"};
    String[] srcs = new String[] {"http://115.182.94.184/v/00.flv",
                                  "http://115.182.94.184/v/01.mp4",
                                  "http://115.182.94.184/v/02.mp4",
                                  "http://115.182.94.184/v/03.mp4",
                                  "http://115.182.94.184/v/04.mp4",
                                  "http://115.182.94.184/v/11.mp4",  // 错误的地址404
                                  "http://115.182.94.184/v/00.mp4"}; // 错误的地址404
    String[] md5s = new String[] {"0b2a7608be9806361f145bfc9e11e093",
                                  "b3c0143812bbe26396663d071275f644",
                                  "36c8337f29270df20797cd4bcb425cc7",
                                  "f2be1f55cca40557c1858176bd31f751", //错误的md5
                                  "7a21c0b68a00c1b601459e8d70abacd1", //错误的md5
                                  "9026bbac93a059d01c4241403fa7b642",
                                  "280eb715f841e622719d0cc630170a60"};
    
    /**
     * 删除任务
     * <b>Method</b>: ContentControllerTest#deletefile <br/>
     * <b>Create Date</b> : 2014年12月26日
     * @author Chen Hao  void
     */
    @Test
    public void deletefile() {
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
        HttpEntity<String> entity = getHttpEntity("DELETE", uri, rq.toJSONString());
        String url = Env.get("openapi_url") + uri;
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
    }
    
	/**
	 * 正常提交任务
	 * <b>Method</b>: ContentControllerTest#testSubfile1 <br/>
	 * <b>Create Date</b> : 2014年12月26日
	 * @author Chen Hao  void
	 */
    //@Test
	public void testSubfile1() {
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
	    HttpEntity<String> entity = getHttpEntity("POST", uri, rq.toJSONString());
	    String url = Env.get("openapi_url") + uri;
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
                //Assert.assertEquals(keys[i], jo.getString("key"));
            }
        }
	}

	
	
	
	//@Test
	public void sub(){
		subFile();
	}
	
	//@Test
	public void del(){
		deleteFile();
	}
	/**
	 * 测试正常返回
	 */
	public void subFile(){
		HttpEntity<String> entity = preparedSubFileHttpEntity();
        ResponseEntity<String> resultEntity = restTemplate.postForEntity("http://localhost:8084/cdn/content", entity, String.class);
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        String array = jobj.getString("result");
        JSONArray jsonArray = JSON.parseArray(array);
        Assert.assertNotNull(jsonArray);
        Assert.assertEquals("http://xz.cr173.com/soft1/lltskb_gr.zip", jsonArray.getJSONObject(0).getString("src"));
        System.out.println(jsonArray.toJSONString());
	}
	
	//删除
	public void deleteFile()  {  
		 HttpHeaders headers = new HttpHeaders();
	        headers.add("Lecloud-api-version", "0.2");
	        headers.add("Authorization", getBase64(Constant.USERID, "DELETE", "/cdn/content", Constant.APPKEY));
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        
	        JSONArray ja = new JSONArray();
	        JSONObject jo1 = new JSONObject();
	        jo1.put("src", "http://umcdn.uc.cn/down/4014/bclhfxtfb2dbbz2ecbcbbzkbbxfbkzbbm67imp32i7uevput.exe");
	        jo1.put("oldkey", "");
	        
	        ja.add(jo1);
	        JSONObject jo2 = new JSONObject();
	        jo2.put("src", "http://xz.cr173.com/soft1/lltskb_gr.zip");
	        jo2.put("oldkey", "");
	        ja.add(jo2);
	        
	        JSONObject jsonObj = new JSONObject();
			jsonObj.put("domaintag", "apitest");
			jsonObj.put("items", ja);
			log.info(jsonObj.toJSONString());
	        
			HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
		
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
	        @Override
	        protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
	            if (HttpMethod.DELETE == httpMethod) {
	                return new HttpEntityEnclosingDeleteRequest(uri);
	            }
	            return super.createHttpUriRequest(httpMethod, uri);
	        }
	    });

		ResponseEntity<String> exchange = restTemplate.exchange("http://localhost:8084/cdn/content", HttpMethod.DELETE,entity,String.class);
		Assert.assertEquals(200, exchange.getStatusCode().value());
		String delResultBody = exchange.getBody();
        JSONObject delJobj = JSONObject.parseObject(delResultBody);
        String array = delJobj.getString("result");
        JSONArray jsonArray = JSON.parseArray(array);
        Assert.assertNotNull(jsonArray);
	}  
	
	//提交分发文件设置参数
	private HttpEntity<String> preparedSubFileHttpEntity() {
		// 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add("Authorization", "Basic MTM3NTg3OmNiMGNmMzUxYzA2MTM2MTA3MmFlY2Y0NzE4MGI1YzYw");
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        JSONArray ja = new JSONArray();
        JSONObject jo1 = new JSONObject();
        jo1.put("src", "http://xz.cr173.com/soft1/lltskb_gr.zip");
        jo1.put("md5", "");
        ja.add(jo1);
        JSONObject jo2 = new JSONObject();
        jo2.put("src", "http://umcdn.uc.cn/down/4014/bclhfxtfb2dbbz2ecbcbbzkbbxfbkzbbm67imp32i7uevput.exe");
        jo2.put("md5", "");
        ja.add(jo2);
        
        
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("domaintag", "letvcloudtest");
        jsonObj.put("method", "submit");
        jsonObj.put("items", ja);
        
        log.info("提交请求参数----"+jsonObj.toJSONString());
        
        HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(),headers);
		return entity;
	}
	
	// 更新分发文件设置参数
	private HttpEntity<String> preparedUpdateFileHttpEntity() {
		// 准备请求头
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add("Authorization",getBase64(Constant.USERID, "DELETE", "/cdn/content", Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONArray ja = new JSONArray();
		JSONObject jo1 = new JSONObject();
		jo1.put("src", "http://xz.cr173.com/soft1/lltskb_gr.zip");
		jo1.put("md5", "");
		ja.add(jo1);
		JSONObject jo2 = new JSONObject();
		jo1.put("src","http://umcdn.uc.cn/down/4014/bclhfxtfb2dbbz2ecbcbbzkbbxfbkzbbm67imp32i7uevput.exe");
		jo1.put("md5", "");
		ja.add(jo2);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", "letvcloudtest");
		jsonObj.put("method", "refresh");
		jsonObj.put("items", ja);

		HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
		return entity;
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
