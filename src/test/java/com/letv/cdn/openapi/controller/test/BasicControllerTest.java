package com.letv.cdn.openapi.controller.test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.controller.test.ContentControllerIITest.HttpEntityEnclosingDeleteRequest;
import com.letv.cdn.openapiauth.utils.LetvApiHelper;
import com.letv.cdn.openapiauth.utils.MD5;

@RunWith(SpringJUnit4ClassRunner.class)  //使用junit4进行测试  
@ContextConfiguration({"/spring-base.xml"}) //加载配置文件
@WebAppConfiguration
public abstract class BasicControllerTest {
	
	@Resource
	RestTemplate restTemplate;
	
	@Before
	public void setup() {
		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			
			private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();
			
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				return errorHandler.hasError(response);
			}
			
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
				String theString = IOUtils.toString(response.getBody());
				RestTemplateException exception = new RestTemplateException();
			    Map<String, Object> properties = new HashMap<String, Object>();
			    properties.put("code", response.getStatusCode().value());
			    properties.put("body", theString);
			    properties.put("header", response.getHeaders());
			    exception.properties = properties;
			    throw exception;
				
			}
		});
		
	}
	
	String getBase64(String userid, String method, String uri, String appkey) {
		
		String md5 = MD5.md5(userid + method + uri + appkey);

		return "Basic ".concat(LetvApiHelper.encodeBase64(userid + ":" + md5));
	}
	
	StringBuilder generateGetReqParamStr(JSONObject jsonObj) {
		boolean first = true;
    	StringBuilder strB = new StringBuilder();
    	for(Entry<String, Object> entry : jsonObj.entrySet()) {
    		if(first) {
    			first = false;
    		}else {
    			strB.append("&");
    		}
    		strB.append(entry.getKey()).append("=").append(entry.getValue());
    	}
		return strB;
	}
	
	void setDelRequestFactory() {
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
}
