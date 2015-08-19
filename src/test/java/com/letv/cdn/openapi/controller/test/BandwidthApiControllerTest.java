package com.letv.cdn.openapi.controller.test;

import java.util.Arrays;
import java.util.Map;

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

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.controller.BandwidthApiController;
import com.letv.cdn.openapiauth.utils.MD5;

/**
 * {@link BandwidthApiController}测试用例
 * <br>
 * 2014年12月22日
 * @author gao.jun
 *
 */
public class BandwidthApiControllerTest extends BasicControllerTest {
	
	private static final String LOCAL_URI_PREFIX = "http://localhost:8084/cdn/domain/";
	
//	private static final String LOCAL_URI_PREFIX = "http://api.cdn.lecloud.com/cdn/domain/";
	
	private static final String DOMAINTAG = "apitest";	
	private static final String KEY_URI = Constant.USERID + "_" + DOMAINTAG;
	private static final String UCLOUD_USERID = "136098"; // ucloud用户ID，用于测试数据正确性
	private static final String UCLOUD_APPKEY = "7f769d6d82f2863b56e71177cb857b5c";
	
	private static final Logger log = LoggerFactory.getLogger(BandwidthApiControllerTest.class);
	
	private static final String UNIT_B = "1";
    private static final String UNIT_K = "K";
    private static final String UNIT_M = "M";
    private static final String UNIT_G = "G";
    private static final String UNIT_T = "T";
	
    private String unit = "fsgasdg";
    
    /**
     * 测试按userid查询带宽 v0.2
     * 
     * @method: BandwidthApiControllerTest  testReportBandwidthByUserid  void
     * @createDate： 2015年1月21日
     * @2015, by chenyuxin.
     */
    @Test
    public void testReportBandwidthByUserid() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add(
                "Authorization",
                getBase64(UCLOUD_USERID, "GET", "/cdn/domain/bandwidth", UCLOUD_APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX +"/bandwidth?startday=20141212&endday=20141215&granularity=5min&unit=" + unit, 
                HttpMethod.GET, entity, String.class);
    
        Assert.assertEquals(HttpStatus.OK.value(), resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jo = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jo.get("data"));
        if (!UNIT_T.equals(unit) && !UNIT_G.equals(unit) && !UNIT_M.equals(unit) && !UNIT_K.equals(unit)
                && !UNIT_B.equals(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        Assert.assertEquals((UNIT_B.equals(unit) ? "" : unit) + "bps", jo.get("unit"));
        log.info("response:" + jo.toJSONString());
    }
    
	/**
	 * 测试带宽查询 v0.2
	 * <br>
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testReportBandwidth1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/domain/" + KEY_URI
						+ "/bandwidth", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX+ KEY_URI +"/bandwidth?startday=20141212&endday=20141215&granularity=5min&unit=" + unit, 
				HttpMethod.GET, entity, String.class);
	
		Assert.assertEquals(HttpStatus.OK.value(), resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jo = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jo.get("data"));
        if (!UNIT_T.equals(unit) && !UNIT_G.equals(unit) && !UNIT_M.equals(unit) && !UNIT_K.equals(unit)
                && !UNIT_B.equals(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        Assert.assertEquals((UNIT_B.equals(unit) ? "" : unit) + "bps", jo.get("unit"));
        log.info("response:" + jo.toJSONString());
	}
	
	/**
	 * 测试带宽查询 v0.1
	 * <br>
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testReportBandwidth2() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("startday", "20141212");
		jsonObj.put("endday", "20141215");
		jsonObj.put("granularity", "5min");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat(KEY_URI)
				.concat("/bandwidth?").concat(strB.toString()), HttpMethod.GET, null, String.class);

		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("data"));
        Assert.assertEquals("Mbps", jobj.get("unit"));
	}
	
	/**
	 * 按userid查询流量 v0.2
	 * 
	 * @method: BandwidthApiControllerTest  testReportTrafficByUserid  void
	 * @createDate： 2015年1月21日
	 * @2015, by chenyuxin.
	 */
	@Test
    public void testReportTrafficByUserid() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
        headers.add(
                "Authorization",
                getBase64(UCLOUD_USERID, "GET", "/cdn/domain/traffic", UCLOUD_APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX + "/traffic?startday=20141212&endday=20141215&granularity=5min&unit=" + unit, 
                HttpMethod.GET, entity, String.class);
    
        Assert.assertEquals(200, resultEntity.getStatusCode().value());
        String resultBody = resultEntity.getBody();
        JSONObject jo = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jo.get("data"));
        if (!UNIT_T.equals(unit) && !UNIT_G.equals(unit) && !UNIT_M.equals(unit) && !UNIT_K.equals(unit)
                && !UNIT_B.equals(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        Assert.assertEquals((UNIT_B.equals(unit) ? "" : unit) + "B", jo.get("unit"));
        log.info("response:" + jo.toJSONString());
    }
	
	/**
	 * 测试流量查询 v0.2
	 * <br>
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testReportTraffic1() {
		HttpHeaders headers = new HttpHeaders();
        headers.add("Lecloud-api-version", "0.2");
		headers.add(
				"Authorization",
				getBase64(Constant.USERID, "GET", "/cdn/domain/" + KEY_URI
						+ "/traffic", Constant.APPKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        
        HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX + KEY_URI + "/traffic?startday=20141212&endday=20141215&granularity=5min&unit=" + unit, 
				HttpMethod.GET, entity, String.class);
	
		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jo = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jo.get("data"));
        if (!UNIT_T.equals(unit) && !UNIT_G.equals(unit) && !UNIT_M.equals(unit) && !UNIT_K.equals(unit)
                && !UNIT_B.equals(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        Assert.assertEquals((UNIT_B.equals(unit) ? "" : unit) + "B", jo.get("unit"));
        log.info("response:" + jo.toJSONString());
	}
	
	/**
	 * 测试流量查询 v0.1
	 * <br>
	 * 2014年12月24日
	 * @author gao.jun
	 */
	@Test
	public void testReportTraffic2() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("startday", "20141212");
		jsonObj.put("endday", "20141215");
		jsonObj.put("granularity", "5min");
        jsonObj.put("userid", Constant.USERID);
        jsonObj.put("ver", "0.1");
        
        @SuppressWarnings("unchecked")
        Map<String,String> map = JSONObject.parseObject(jsonObj.toJSONString(), Map.class);
    	jsonObj.put("sign", sign(map, Constant.APPKEY));
    	
    	StringBuilder strB = generateGetReqParamStr(jsonObj);
    	
		ResponseEntity<String> resultEntity = restTemplate.exchange(LOCAL_URI_PREFIX.concat(KEY_URI)
				.concat("/traffic?").concat(strB.toString()), HttpMethod.GET, null, String.class);

		Assert.assertEquals(200, resultEntity.getStatusCode().value());
		String resultBody = resultEntity.getBody();
        JSONObject jobj = JSONObject.parseObject(resultBody);
        Assert.assertNotNull(jobj.get("data"));
        Assert.assertEquals("MB", jobj.get("unit"));
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
