package com.letv.cdn.openapi.controller.test;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.controller.test.ContentControllerIITest.HttpEntityEnclosingDeleteRequest;

/**
 * 内容刷新接口的测试用例
 * <br>
 * 2015年3月20日
 * @author gao.jun
 *
 */
public class CachedFileControllerTest extends BasicControllerTest {
	
	private static final Logger log = LoggerFactory.getLogger(CachedFileControllerTest.class);
	
//	private static final String LOCAL_URI_PREFIX = "http://localhost:8084/cdn/content/cachedfile";
	
	private static final String LOCAL_URI_PREFIX = "http://111.206.210.204:9002/cdn/content/cachedfile";
	
	private static String progress_url; 
	
	/**
	 * 测试提交预取小文件
	 * <br>
	 * 正常提交单个文件
	 * <br>
	 * 2015年3月23日
	 * @author gao.jun
	 */
	@Test
	public void testPostCachedFileSingleFile() {
		postCachedFile(new String[] { "http://115.182.94.184/v/13.flv" });
	}

	/**
	 * 测试进度查询
	 * <br>
	 * 正常查询进度
	 * 2015年3月23日
	 * @author gao.jun
	 */
	@Test
	public void testQuereyProgress() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/content/cachedfile/progress",
						Constant.APPKEY));
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> resultEntity = restTemplate.exchange(progress_url, HttpMethod.GET, entity, String.class);
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
		Assert.assertNotNull(resultBody);
		JSONObject jsonObj = JSONObject.parseObject(resultBody);
		for(Entry<String, Object> entry : jsonObj.entrySet()) {
			JSONObject val = JSONObject.parseObject(entry.getValue().toString());
			Assert.assertNotNull(val.get("createdTime"));
			Assert.assertNotNull(val.get("progress"));
			Assert.assertTrue(val.get("estimatedEndTime") != null || val.get("finishedTime") != null);
		}
	}
	
	/**
	 * 测试提交预取小文件
	 * <br>
	 * 正常提交多个文件，并正常查询进度
	 * 2015年3月25日
	 * @author gao.jun
	 */
	@Test
	public void testPostCachedFileMutiFiles() {
		postCachedFile(new String[] { "http://115.182.94.184/v/13.flv", "http://115.182.94.184/v/14.flv" });
		testQuereyProgress();
	}
	
	/**
	 * 测试提交预取小文件
	 * <br>
	 * 提交失败，没有domaintag
	 * 2015年3月25日
	 * @author gao.jun
	 */
	@Test
	public void testPostCachedFileNoDomaintag() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "POST", "/cdn/content/cachedfile",
						Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("srcs", new String[] { "http://115.182.94.184/v/13.flv", "http://115.182.94.184/v/14.flv" });

		HttpEntity<String> entity = new HttpEntity<String>(
				jsonObj.toJSONString(), headers);
		try {
			restTemplate.postForEntity(
					LOCAL_URI_PREFIX, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(400,ex.properties.get("code"));
		}
	}
	
	/**
	 * 测试提交预取小文件
	 * <br>
	 * 提交失败，domaintag和srcs中文件路径不匹配
	 * 2015年3月25日
	 * @author gao.jun
	 */
	@Test
	public void testPostCachedFileNotMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "POST", "/cdn/content/cachedfile",
						Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", "apitest");
		jsonObj.put("srcs", new String[] { "http://d.biligame.com/v/13.flv", "http://d.biligame.com/v/14.flv" });

		HttpEntity<String> entity = new HttpEntity<String>(
				jsonObj.toJSONString(), headers);
		try {
			restTemplate.postForEntity(
					LOCAL_URI_PREFIX, entity, String.class);
		} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(400,ex.properties.get("code"));
		}
	}
	
	/**
	 * 提交小文件刷新
	 * <br>
	 * 正常刷新小文件，并查询刷新进度
	 * 2015年3月24日
	 * @author gao.jun
	 */
	@Test
	public void testDeleteCachedfile() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/content/cachedfile",
						Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", "apitest");
		jsonObj.put("srcs", new String[] { "http://115.182.94.184/v/13.flv" });
		HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
		setDeletReqEntity();
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX, HttpMethod.DELETE, entity, String.class);
		Assert.assertEquals(202, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
		Assert.assertNotNull(resultBody);
		log.info("testDeleteCachedFile,result: {}", resultBody);
		assertLinks(resultBody);
		testQuereyProgress();
	}
	
	/**
	 * 提交小文件刷新
	 * <br>
	 * 测试对crossdomain.xml文件的过滤
	 * 2015年3月24日
	 * @author gao.jun
	 */
	@Test
	public void testDeleteCachedfileFilter() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/content/cachedfile",
						Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", "apitest");
		jsonObj.put("srcs", new String[] { "http://115.182.94.184/v/13.flv","http://115.182.94.184/v/crossdomain.xml","http://115.182.94.184/v/crossdomain.xml?v=1" });
		HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
		setDeletReqEntity();
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX, HttpMethod.DELETE, entity, String.class);
		Assert.assertEquals(202, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
		Assert.assertNotNull(resultBody);
		log.info("testDeleteCachedFile,result: {}", resultBody);
		assertLinks(resultBody);
		
		HttpHeaders proHeaders = new HttpHeaders();
		proHeaders.add("Lecloud-api-version", "0.2");
		proHeaders.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/content/cachedfile/progress",
						Constant.APPKEY));
		HttpEntity<String> proEntity = new HttpEntity<String>(proHeaders);
		ResponseEntity<String> proResultEntity = restTemplate.exchange(progress_url, HttpMethod.GET, proEntity, String.class);
		Assert.assertEquals(200, proResultEntity.getStatusCode().value());
		String proResultBody = proResultEntity.getBody();
		Assert.assertNotNull(proResultBody);
		JSONObject proJsonObj = JSONObject.parseObject(proResultBody);
		Set<Map.Entry<String, Object>> set = proJsonObj.entrySet();
		Assert.assertTrue(set.size() == 1);
		for(Entry<String, Object> entry : set) {
			JSONObject val = JSONObject.parseObject(entry.getValue().toString());
			Assert.assertNotNull(val.get("createdTime"));
			Assert.assertNotNull(val.get("progress"));
			Assert.assertTrue(val.get("estimatedEndTime") != null || val.get("finishedTime") != null);
		}
	}
	
	/**
	 * 提交目录刷新
	 * <br>
	 * 正常刷新目录，并查询刷新进度
	 * 2015年3月25日
	 * @author gao.jun
	 */
	@Test
	public void testDeleteCachedDir() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "DELETE", "/cdn/content/cachedfile/directory",
						Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", "apitest");
		jsonObj.put("srcs", new String[] { "http://115.182.94.184/v/" });
		HttpEntity<String> entity = new HttpEntity<String>(jsonObj.toJSONString(), headers);
		setDeletReqEntity();
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat("/directory"), HttpMethod.DELETE, entity, String.class);
		Assert.assertEquals(202, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
		Assert.assertNotNull(resultBody);
		log.info("testDeleteCachedFile,result: {}", resultBody);
		assertLinks(resultBody);
		testQuereyProgress();
	}
	
	/**
	 * 测试缓存刷新之后返回的url格式是否正常，并保存进度查询的url
	 * <br>
	 * 2015年3月24日
	 * @author gao.jun
	 * @param resultBody
	 */
	private void assertLinks(String resultBody) {
		JSONArray jArray = JSONObject.parseObject(resultBody).getJSONArray("links");
		for(int i = 0,len = jArray.size(); i< len; ++i) {
			JSONObject obj = jArray.getJSONObject(i);
			if("progress".equals(obj.get("rel"))) {
				progress_url = obj.get("href").toString();
				break;
			}
		}
		Assert.assertNotNull(progress_url);
	}
	
	private void setDeletReqEntity() {
		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
            @Override
            protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
                if (HttpMethod.DELETE == httpMethod) {
                    return new HttpEntityEnclosingDeleteRequest(uri);
                }
                return super.createHttpUriRequest(httpMethod, uri);
            }
        });
	}
	
	private void postCachedFile(String[] srcs) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "POST", "/cdn/content/cachedfile",
						Constant.APPKEY));
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("domaintag", "apitest");
		jsonObj.put("srcs", srcs);

		HttpEntity<String> entity = new HttpEntity<String>(
				jsonObj.toJSONString(), headers);
		ResponseEntity<String> resultEntity = restTemplate.postForEntity(
				LOCAL_URI_PREFIX, entity, String.class);
		Assert.assertEquals(202, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
		Assert.assertNotNull(resultBody);
		log.info("testPostCachedFile,result: {}", resultBody);
		assertLinks(resultBody);
	}
}
