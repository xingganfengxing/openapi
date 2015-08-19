package com.letv.cdn.openapi.controller.test;

import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.controller.LogDownloadApiController;
import com.letv.cdn.openapiauth.utils.MD5;

/**
 * {@link LogDownloadApiController}测试用例
 * <br>
 * 2014年12月22日
 * @author gao.jun
 *
 */
public class LogDownloadApiControllerTest extends BasicControllerTest {
	
//	private static final String LOCAL_URI_PREFIX = "http://localhost:8084/cdn/domain/";
	private static final String LOCAL_URI_PREFIX = "http://api.cdn.lecloud.com/cdn/domain/";

	private static final String DOMAINTAG = "apitest";
	
	private static final String KEY_URI = Constant.USERID + "_" + DOMAINTAG;
	
	
	/**
	 * 测试获取日志下载地址接口 v0.2
	 * 日志文件不存在时
	 */
	@Test
	public void getLogdownloadUrlTest1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/domain/" + KEY_URI
						+ "/logdownloadurl", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        try{
        	restTemplate.exchange(LOCAL_URI_PREFIX.concat(KEY_URI)
				.concat("/logdownloadurl?day=20121127"), HttpMethod.GET, entity, String.class);
        }catch(Exception e){
        	RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(400, ex.properties.get("code"));
			String resultBody = (String) ex.properties.get("body");
	        JSONObject jobj = JSONObject.parseObject(resultBody);
	        JSONObject result = jobj.getJSONObject("error");
			String code = result.getString("code");
	        Assert.assertNotNull(code);
	        Assert.assertEquals("100000", code);
        }
	}
	
	/**
	 * 测试获取日志下载地址接口 v0.1
	 * <br>
	 * 日志文件不存在
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void getLogdownloadUrlTest2() {
		JSONObject jsonObj = new JSONObject();
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        jsonObj.put("day", "20121127");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	try {
			restTemplate.exchange(LOCAL_URI_PREFIX.concat(KEY_URI)
					.concat("/logdownloadurl?").concat(strB.toString()), HttpMethod.GET, null, String.class);
    	} catch (Exception e) {
			RestTemplateException ex = (RestTemplateException)e.getCause();
			Assert.assertEquals(400,ex.properties.get("code"));
		}	
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
