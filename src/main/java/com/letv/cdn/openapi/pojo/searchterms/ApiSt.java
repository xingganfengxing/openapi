/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : ApiSt.java 
 * Date Created: 2014年10月19日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月19日 下午1:42:51 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.pojo.searchterms;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * TODO:对外接口的参数类
 * 
 * @author chenyuxin
 * @createDate 2014年10月19日
 */

public class ApiSt{
    
    /** 域名标识 */
    private String domaintag;
    /** 起始日期 格式：yyyyMMdd */
    private String startday;
    /** 结束日期 格式：yyyyMMdd */
    private String endday;
    /** 查询粒度： day、5min */
    private String granularity;
    /** 用户唯一标识码 */
    private String userid;
    /** 验证码 */
    private String sign;
    
    public String getDomaintag() {
    
        return domaintag;
    }
    
    public void setDomaintag(String domaintag) {
    
        this.domaintag = domaintag;
    }
    
    public String getStartday() {
    
        return startday;
    }
    
    public void setStartday(String startday) {
    
        this.startday = startday;
    }
    
    public String getEndday() {
    
        return endday;
    }
    
    public void setEndday(String endday) {
    
        this.endday = endday;
    }
    
    public String getGranularity() {
    
        return granularity;
    }
    
    public void setGranularity(String granularity) {
    
        this.granularity = granularity;
    }
    
    public String getUserid() {
    
        return userid;
    }
    
    public void setUserid(String userid) {
    
        this.userid = userid;
    }
    
    public String getSign() {
    
        return sign;
    }
    
    public void setSign(String sign) {
    
        this.sign = sign;
    }
    
    @Override
    public String toString() {
    
        JSONObject o = (JSONObject) JSON.toJSON(this);
        return o.toJSONString();
    }
}
