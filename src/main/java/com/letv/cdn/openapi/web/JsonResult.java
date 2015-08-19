package com.letv.cdn.openapi.web;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.common.StringUtil;

/**
 * User: lichao
 * Date: 11-8-7
 * Time: 下午1:49
 */
public class JsonResult extends JSONObject {
    //private JSONObject jsonObject = new JSONObject();



    private JsonResult() {

    }



    public static JsonResult success(String msg,String transferedsize,String totalsize) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.add("status", 200);
        jsonResult.add("msg", StringUtil.unicode(msg));
        jsonResult.add("transferedsize",transferedsize);
        jsonResult.add("totalsize",totalsize);
        return jsonResult;
    }

    public static JsonResult success(String msg) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.add("status", 200);
        jsonResult.add("msg", StringUtil.unicode(msg));
        return jsonResult;
    }



    public static JsonResult gen(int status, String msg) {
        JsonResult jsonResult = new JsonResult();
        jsonResult.add("status", status);
        jsonResult.add("msg", msg);
        return jsonResult;
    }



    @Override
    public Object put(String k, Object v) {
        if ("status".equals(k) || "msg".equals(k)) return null;
        return super.put(k, v);
    }



    private Object add(String k, Object v) {
        return super.put(k, v);
    }
}
