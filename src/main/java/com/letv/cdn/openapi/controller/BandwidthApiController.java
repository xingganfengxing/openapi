/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : BandwidthAPIController.java 
 * Date Created: 2014年10月19日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月19日 下午1:27:04 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.service.UserService;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.utils.DateUtil;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * TODO:对外的带宽流量接口Controller
 * 
 * @author chenyuxin
 * @createDate 2014年10月19日
 */
@Controller
@RequestMapping("/cdn/domain")
public class BandwidthApiController extends BaseController{
    
    @Resource
    UserService userService;
    public static final String UNIT_B = "1";
    public static final String UNIT_K = "K";
    public static final String UNIT_M = "M";
    public static final String UNIT_G = "G";
    public static final String UNIT_T = "T";
    
    private static final Logger log = LoggerFactory.getLogger(BandwidthApiController.class);
    
    private final String GET_REPORT_BANDWIDTH = Env.get("get_report_bandwidth");
    private final String GET_REPORT_TRAFFIC = Env.get("get_report_traffic");
    private final String DOMAINTAG_PREFIX = "102.";
    
    /**
     * 获取带宽的接口
     * 
     * @method: BandwidthApiController getReportBandwidth
     * @param request
     * @param response
     * @param domaintag
     *        域名标识 注：表示的是gslb库中的tb_coop_domain表的usertag字段值
     * @param startday
     *        起始日期 格式:'yyyyMMdd'
     * @param endday
     *        结束日期 格式：'yyyyMMdd'
     * @param granularity
     *        数据粒度 :'day'、'5min'
     * @param userid
     *        用户id
     * @param sign
     *        MD5校验码
     * @throws Exception
     *         void
     * @createDate： 2014年10月21日
     * @2014, by chenyuxin.
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}/bandwidth", method = RequestMethod.GET)
    public ResponseJson getReportBandwidth(HttpServletRequest request, HttpServletResponse response,
            @PathVariable(value = "userid") String useridPath, @PathVariable String domaintag,
            @RequestParam(value = "startday", required = true) String startday,
            @RequestParam(value = "endday", required = true) String endday,
            @RequestParam(value = "granularity", required = true) String granularity,
            @RequestParam(value = "userid", required = true) String userid,
            @RequestParam(value = "ver", required = true) String ver,
            @RequestParam(value = "sign", required = true) String sign) throws Exception {
    
        Assert.isTrue(userid.equals(useridPath), "查询参数错误：uri中的userid与参数中的userid不一致");
        // 域名标识不能为空
        Assert.isTrue(!"".equals(domaintag), "查询参数错误：域名标识(domaintag)为空");
        String temp = this.getDate(response, domaintag, startday, endday, granularity, null, userid,
                GET_REPORT_BANDWIDTH);
        if ("{}".equals(temp)) {
            JSONObject j = new JSONObject();
            j.put("data", new ArrayList<Object>());
            j.put("unit", "Mbps");
            return ResponseJson.okWithNoCache(j);
        }
        JSONObject jsonResult = JSON.parseObject(temp);
        if (jsonResult.containsKey("msg")) {
            return ResponseJson.internalServerError((String) jsonResult.get("msg"));
        } else {
            return ResponseJson.okWithNoCache(jsonResult);
        }
    }
    
    /**
     * 带宽查询接口 <br>
     * 2014年12月22日
     * 
     * @author gao.jun
     * @param request
     *        HttpServletRequest
     * @param response
     *        HttpServletResponse
     * @param userid
     *        用户id
     * @param domaintag
     *        域名标识
     * @param startday
     *        起始日期 格式:'yyyyMMdd'
     * @param endday
     *        结束日期 格式:'yyyyMMdd'
     * @param granularity
     *        数据粒度 :'day'、'5min'
     * @return
     * @throws Exception
     * @since 0.2
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}/bandwidth", method = RequestMethod.GET, headers = { "Lecloud-api-version=0.2" })
    public ResponseJson getReportBandwidth(HttpServletRequest request, HttpServletResponse response,
            @PathVariable String userid, @PathVariable String domaintag,
            @RequestParam(value = "startday", required = true) String startday,
            @RequestParam(value = "endday", required = true) String endday,
            @RequestParam(value = "granularity", required = true) String granularity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestHeader(value = "Accept",required = false) String accept) throws Exception {
    	if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
    	// 域名标识不能为空
        Assert.isTrue(!"".equals(domaintag), "查询参数错误：域名标识(domaintag)为空");
        if (!UNIT_T.equalsIgnoreCase(unit) && !UNIT_G.equalsIgnoreCase(unit) && !UNIT_M.equalsIgnoreCase(unit) && !UNIT_K.equalsIgnoreCase(unit)
                && !UNIT_B.equalsIgnoreCase(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        String temp = this.getDate(response, domaintag, startday, endday, granularity, unit, userid,
                GET_REPORT_BANDWIDTH);
        if ("{}".equals(temp)) {
            JSONObject j = new JSONObject();
            j.put("data", new ArrayList<Object>());
            j.put("unit", unit + "bps");
            return ResponseJson.okWithNoCache(j);
        }
        JSONObject jsonResult = JSON.parseObject(temp);
        if (jsonResult.containsKey("msg")) {
            return ResponseJson.internalServerError((String) jsonResult.get("msg"));
        } else {
            return ResponseJson.okWithNoCache(jsonResult);
        }
    }
    
    /**
     * 按用户的userid查询带宽
     * 
     * @method: BandwidthApiController  getReportBandwidth
     * @param request
     * @param response
     * @param startday 起始日期 格式:'yyyyMMdd'
     * @param endday 结束日期 格式:'yyyyMMdd'
     * @param granularity 数据粒度 :'day'、'5min'
     * @param unit 返回数据的单位标识
     * @param accept
     * @return
     * @throws Exception  ResponseJson
     * @createDate： 2015年1月20日
     * @2015, by chenyuxin.
     */
    @OpenapiAuth
    @RequestMapping(value = "/bandwidth", method = RequestMethod.GET, headers = { "Lecloud-api-version=0.2" })
    public ResponseJson getReportBandwidth(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "startday", required = true) String startday,
            @RequestParam(value = "endday", required = true) String endday,
            @RequestParam(value = "granularity", required = true) String granularity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestHeader(value = "Accept",required = false) String accept) throws Exception {
        if(!ApiHelper.checkAcceptHeader(accept)) {
            return ResponseJson.notAcceptable();
        }
        String userid = ApiHelper.getUserid();
        if (!UNIT_T.equalsIgnoreCase(unit) && !UNIT_G.equalsIgnoreCase(unit) && !UNIT_M.equalsIgnoreCase(unit) && !UNIT_K.equalsIgnoreCase(unit)
                && !UNIT_B.equalsIgnoreCase(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        String temp = this.getDate(response, null, startday, endday, granularity, unit, userid,
                GET_REPORT_BANDWIDTH);
        if ("{}".equals(temp)) {
            JSONObject j = new JSONObject();
            j.put("data", new ArrayList<Object>());
            j.put("unit", unit + "bps");
            return ResponseJson.okWithNoCache(j);
        }
        JSONObject jsonResult = JSON.parseObject(temp);
        if (jsonResult.containsKey("msg")) {
            return ResponseJson.internalServerError((String) jsonResult.get("msg"));
        } else {
            return ResponseJson.okWithNoCache(jsonResult);
        }
    }
    
    /**
     * 获取流量的接口
     * 
     * @method: BandwidthApiController getReportTraffic
     * @param request
     * @param response
     * @param domaintag
     *        域名标识 注：表示的是gslb库中的tb_coop_domain表的usertag字段值
     * @param startday
     *        起始日期 格式:'yyyyMMdd'
     * @param endday
     *        结束日期 格式：'yyyyMMdd'
     * @param granularity
     *        数据粒度 :'day'、'5min'
     * @param userid
     *        用户id
     * @param sign
     *        MD5校验码
     * @throws Exception
     *         void
     * @createDate： 2014年10月21日
     * @2014, by chenyuxin.
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}/traffic", method = RequestMethod.GET)
    public ResponseJson getReportTraffic(HttpServletRequest request, HttpServletResponse response,
            @PathVariable(value = "userid") String useridPath, @PathVariable String domaintag,
            @RequestParam(value = "startday", required = true) String startday,
            @RequestParam(value = "endday", required = true) String endday,
            @RequestParam(value = "granularity", required = true) String granularity,
            @RequestParam(value = "userid", required = true) String userid,
            @RequestParam(value = "ver", required = true) String ver,
            @RequestParam(value = "sign", required = true) String sign) throws Exception {
    
        Assert.isTrue(userid.equals(useridPath), "查询参数错误：uri中的userid与参数中的userid不一致");
        // 域名标识不能为空
        Assert.isTrue(!"".equals(domaintag), "查询参数错误：域名标识(domaintag)为空");
        String temp = this.getDate(response, domaintag, startday, endday, granularity, null, userid, GET_REPORT_TRAFFIC);
        JSONObject jo = JSON.parseObject(temp);
        if ("{}".equals(temp)) {
            JSONObject j = new JSONObject();
            j.put("data", new ArrayList<Object>());
            j.put("unit", "Mb");
            return ResponseJson.okWithNoCache(j);
        }
        if (jo.containsKey("msg")) {
            return ResponseJson.internalServerError((String) jo.get("msg"));
        } else {
            return ResponseJson.okWithNoCache(jo);
        }
    }
    
    /**
     * 流量查询 <br>
     * 2014年12月22日
     * 
     * @author gao.jun
     * @param request
     *        HttpServletRequest
     * @param response
     *        HttpServletResponse
     * @param userid
     *        用户id
     * @param domaintag
     *        域名标识
     * @param startday
     *        起始日期 格式:'yyyyMMdd'
     * @param endday
     *        结束日期 格式：'yyyyMMdd'
     * @param granularity
     *        数据粒度 :'day'、'5min'
     * @return
     * @throws Exception
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}/traffic", method = RequestMethod.GET, headers = { "Lecloud-api-version=0.2" })
    public ResponseJson getReportTraffic(HttpServletRequest request, HttpServletResponse response,
            @PathVariable String userid, @PathVariable String domaintag,
            @RequestParam(value = "startday", required = true) String startday,
            @RequestParam(value = "endday", required = true) String endday,
            @RequestParam(value = "granularity", required = true) String granularity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestHeader(value = "Accept",required = false) String accept) throws Exception {
    	if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
    	// 域名标识不能为空
        Assert.isTrue(!"".equals(domaintag), "查询参数错误：域名标识(domaintag)为空");
        if (!UNIT_T.equalsIgnoreCase(unit) && !UNIT_G.equalsIgnoreCase(unit) && !UNIT_M.equalsIgnoreCase(unit) && !UNIT_K.equalsIgnoreCase(unit)
                && !UNIT_B.equalsIgnoreCase(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        String temp = this.getDate(response, domaintag, startday, endday, granularity, unit, userid, GET_REPORT_TRAFFIC);
        JSONObject jo = JSON.parseObject(temp);
        if ("{}".equals(temp)) {
            JSONObject j = new JSONObject();
            j.put("data", new ArrayList<Object>());
            j.put("unit", unit + "B");
            return ResponseJson.okWithNoCache(j);
        }
        if (jo.containsKey("msg")) {
            return ResponseJson.internalServerError((String) jo.get("msg"));
        } else {
            return ResponseJson.okWithNoCache(jo);
        }
    }
    
    /**
     * 按用户的userid查询流量
     * 
     * @method: BandwidthApiController  getReportTraffic
     * @param request
     * @param response
     * @param startday
     * @param endday
     * @param granularity
     * @param unit
     * @param accept
     * @return
     * @throws Exception  ResponseJson
     * @createDate： 2015年1月21日
     * @2015, by chenyuxin.
     */
    @OpenapiAuth
    @RequestMapping(value = "/traffic", method = RequestMethod.GET, headers = { "Lecloud-api-version=0.2" })
    public ResponseJson getReportTraffic(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "startday", required = true) String startday,
            @RequestParam(value = "endday", required = true) String endday,
            @RequestParam(value = "granularity", required = true) String granularity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestHeader(value = "Accept",required = false) String accept) throws Exception {
        if(!ApiHelper.checkAcceptHeader(accept)) {
            return ResponseJson.notAcceptable();
        }
        String userid = ApiHelper.getUserid();
        if (!UNIT_T.equalsIgnoreCase(unit) && !UNIT_G.equalsIgnoreCase(unit) && !UNIT_M.equalsIgnoreCase(unit) && !UNIT_K.equalsIgnoreCase(unit)
                && !UNIT_B.equalsIgnoreCase(unit)) {
            unit = UNIT_M;
        }
        unit = unit.toUpperCase();
        String temp = this.getDate(response, null, startday, endday, granularity, unit, userid, GET_REPORT_TRAFFIC);
        JSONObject jo = JSON.parseObject(temp);
        if ("{}".equals(temp)) {
            JSONObject j = new JSONObject();
            j.put("data", new ArrayList<Object>());
            j.put("unit", unit + "B");
            return ResponseJson.okWithNoCache(j);
        }
        if (jo.containsKey("msg")) {
            return ResponseJson.internalServerError((String) jo.get("msg"));
        } else {
            return ResponseJson.okWithNoCache(jo);
        }
    }
    
    /**
     * 调用report接口的方法
     * 
     * @method: BandwidthApiController getDate
     * @param response
     * @param as
     *        参数包装类
     * @param url
     *        report接口的url
     * @return
     * @throws Exception
     *         void
     * @createDate： 2014年10月20日
     * @2014, by chenyuxin.
     */
    private String getDate(HttpServletResponse response, String domaintag, String startday, String endday,
            String granularity, String unit, String userid, String url) throws Exception {
    
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Long startTime = sdf.parse(startday).getTime();
        Long endTime = sdf.parse(endday).getTime();
        Long comTime = sdf.parse("20140701").getTime();
        // 查询日期不能为空且是有效的日期
        Assert.isTrue(!"".equals(startday) && RegExpValidatorUtil.isDate(startday, "yyyyMMdd"),
                "查询参数错误：起始日期(startday)为空或不是一个有效的日期");
        Assert.isTrue(!"".equals(endday) && RegExpValidatorUtil.isDate(endday, "yyyyMMdd"),
                "查询参数错误：结束日期(endday)为空或不是一个有效的日期");
        // 判断日期是否有效
        Assert.isTrue(endTime.compareTo(DateUtil.now()) <= 0, "查询参数错误：结束日期(endday)大于今天");
        Assert.isTrue(startTime.compareTo(endTime) <= 0, "查询参数错误：起始日期(startday)大于结束日期(endday)");
        Assert.isTrue((DateUtil.now() - startTime) / 1000 / 60 / 60 / 24 <= 720, "查询参数错误：日期错误,不支持查询24个月之前的数据");
        Assert.isTrue(comTime.compareTo(startTime) <= 0, "查询参数错误：日期错误,不支持查询20140701之前的数据");
        // 将openapi的日期格式转换成与report的日期格式
        startday = DateUtil.changePattern(startday, "yyyyMMdd", "yyyy-MM-dd");
        endday = DateUtil.changePattern(endday, "yyyyMMdd", "yyyy-MM-dd");
        
        // 判断数据粒度是否有效
        Assert.isTrue(!"".equals(granularity), "查询参数错误：查询粒度(Granularity)为空");
        if (!"day".equals(granularity)) {
            // 当数据粒度为'5min'时，修改为'min'，与report项目的数据粒度一致
            if (!"5min".equals(granularity)) {
                throw new IllegalArgumentException("查询参数错误：查询粒度(Granularity)只能是'day'或'5min'");
            } else {
                granularity = "min";
            }
        }
        
        // 调用report的查询带宽的接口
        // 将参数存放到一个Map中
        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", userid);
        // 该域名标识表示的是gslb库中的tb_coop_domain表的usertag字段值,当调用report的带宽流量查询接口时须加上CDN域名标识的前缀"102."
        if(domaintag != null){
            params.put("domainTag", DOMAINTAG_PREFIX + domaintag);
        }
        params.put("startTime", startday);
        params.put("endTime", endday);
        params.put("dataType", granularity);
        if (unit != null) {
            params.put("unit", unit);
        }
        params.put("tag", "openapi");
        log.info("获取带宽数据：访问report接口：" + url);
        String jsonResult = HttpClientUtil.get(url, params, HttpClientUtil.UTF_8, HttpClientUtil.UTF_8);
        return jsonResult;
    }
    
}
