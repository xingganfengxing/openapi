/*
 * Created on Dec 12, 2007
 */
package com.letv.cdn.openapi.web;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.common.MapUtil;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.common.XXTEA;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author lichao
 */
public class RequestUtils {

    private static transient final Logger log = Logger.getLogger(RequestUtils.class);


    public static <T> T convertWith(HttpServletRequest req, Class<T> clazz, List<String> properties) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        T obj = clazz.newInstance();
        Map<String, String[]> propertiesMap = new HashMap<String, String[]>();
        for (String property : properties) {
            String beanName;
            String paramName;
            if (property.contains(":")) {
                String[] split = property.split(":", 2);
                beanName = split[0];
                paramName = split[1];
            } else {
                beanName = property;
                paramName = property;
            }
            propertiesMap.put(beanName, req.getParameterValues(paramName));
        }
        BeanUtils.populate(obj, propertiesMap);
        return obj;
    }


    /**
     * 获得一个字符串的长整型值
     *
     * @param longStr
     * @return
     */
    public static Long getLong(String longStr) {
        Long longval = null;
        try {
            longval = Long.parseLong(longStr);
        } catch (NumberFormatException nfe) {
            log.debug("error:" + nfe.getMessage());
        }
        return longval;
    }


    /**
     * 获得长整型的参数值
     *
     * @param request
     * @param paramName
     * @return
     */
    public static Long getLong(HttpServletRequest request, String paramName) {
        return getLong(request.getParameter(paramName));
    }


    /**
     * 获得一个字符串的字节型值
     *
     * @param byteStr
     * @return
     */
    public static byte getByte(String byteStr) {
        byte byteval = 0;
        try {
            byteval = Byte.parseByte(byteStr);
        } catch (NumberFormatException nfe) {
            log.warn("error:" + nfe.getMessage());
        }
        return byteval;
    }
    
    /**
     * 取客户端ip地址
     *
     * @param request
     * @return
     */
    public static String getRemoteAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (notFound(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (notFound(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (notFound(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 取客户端ip地址
     *
     * @param request
     * @return
     */
    public static String getNewRemoteAddr(HttpServletRequest request) {

        String ip = request.getHeader("x-forwarded-for");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if("127.0.0.1".equals(ip)){
            	//根据网卡取本机配置的ip
            	try {
					InetAddress inet = InetAddress.getLocalHost();
					ip = inet.getHostAddress();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
            }
        }
        //对于通过多个代理的情况，第一个IP为真实的IP，多个IP按照','分割
        if(ip != null && ip.length() > 15){
        	if(ip.indexOf(",") > 0){
        		ip = ip.substring(0, ip.indexOf(","));
        	}
        }
        return ip;
    
    	
/*        String ip = request.getHeader("x-forwarded-for");
        if (notFound(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (notFound(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (notFound(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;*/
    }

    /**
     * 取客户端ip地址
     *
     * @param request
     * @return
     */
    public static String getRemoteAddr(HttpServletRequest request, String defaultParam) {
        String ip = null;
        if (notFound(ip)) {
            ip = request.getParameter(defaultParam);
        }
        if (notFound(ip)) {
            ip = getRemoteAddr(request);
        }
        return ip;
    }


    private static boolean notFound(String ip) {
        return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip) || "0.0.0.0".equals(ip);
    }


    public static String getRequestParams(HttpServletRequest req, List<String> exceptParams) {
        StringBuilder strBuffer = new StringBuilder();
        Enumeration parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {

            if (strBuffer.length() > 0) {
                strBuffer.append("&");
            }

            String name = (String) parameterNames.nextElement();

            if (exceptParams.contains(name)) continue;

            String value = req.getParameter(name);

            strBuffer.append(name).append("=").append(value);
        }
        return strBuffer.toString();
    }


    public static Map<String, String> getRequestParamMap(HttpServletRequest req, List<String> exceptParams) {
        return getRequestParamMap(req, exceptParams, false);
    }


    public static Map<String, String> getRequestParamMap(HttpServletRequest req, List<String> exceptParams, boolean filterEmpty) {
        Map<String, String> paramMap = new HashMap<String, String>();
        Enumeration parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = (String) parameterNames.nextElement();
            if (exceptParams.contains(name)) continue;
            String value = req.getParameter(name);
            if (filterEmpty && StringUtil.isEmpty(value)) {
                continue;
            }
            paramMap.put(name, value);
        }
        return paramMap;
    }


    public static String toParam(Map<String, String> params, String encode) throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            if (encode == null) {
                stringBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            } else {
                stringBuilder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), encode));
            }

        }
        return stringBuilder.toString();
    }


    public static Map parseQuery(HttpServletRequest req) throws ParseException {
        Map<String, String> requestParamMap = getRequestParamMap(req, Collections.<String>emptyList(), true);
        JSONObject query = new JSONObject();

        String q_native = req.getParameter("q_native");
        if (StringUtil.notEmpty(q_native)) {
            return JSONObject.parseObject(q_native);
        }


        for (Map.Entry<String, String> entry : requestParamMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            //q_n_eq_name=xxxx
            String[] split = key.split("_");
            if (split.length != 4 || !split[0].equalsIgnoreCase("q")) {
                continue;
            }

            query.put(split[3], filedLimit(split[2], getValue(split[1], value)));
        }
        return query;
    }


    public static Map parseField(HttpServletRequest req) throws ParseException {
        String q_f = req.getParameter("q_f");
        if (StringUtil.notEmpty(q_f)) {
            String[] split = q_f.split(",");
            Map<String, Integer> field = new HashMap<String, Integer>();
            for (String s : split) {
                field.put(s, 1);
            }
            return field;
        } else {
            return null;
        }
    }


    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


    static public Object getValue(String typeKey, String value) throws ParseException {
        if (typeKey.equals("s")) {
            return value;
        }

        if (typeKey.equals("n")) {
            return Long.parseLong(value);
        }

        if (typeKey.equals("b")) {
            return Boolean.parseBoolean(value);
        }

        if (typeKey.equals("d")) {
            return dateFormat.parse(value).getTime();
        }

        throw new RuntimeException("type key [" + typeKey + "] not support");
    }


    public static Object filedLimit(String limitKey, Object value) throws ParseException {
        if (limitKey.equals("eq")) {
            return value;
        }

        if (limitKey.equals("ne")) {
            return MapUtil.getMap("$ne", value);
        }

        if (limitKey.equals("gt")) {
            return MapUtil.getMap("$gt", value);
        }

        if (limitKey.equals("gte")) {
            return MapUtil.getMap("$gte", value);
        }

        if (limitKey.equals("lt")) {
            return MapUtil.getMap("$lt", value);
        }

        if (limitKey.equals("lte")) {
            return MapUtil.getMap("$lte", value);
        }

        throw new RuntimeException("limit key [" + limitKey + "] not support");
    }


    public static Map<String, String> parseParam(String param) {
        Map<String, String> paramMap = new HashMap<String, String>();
        String[] split = param.split("&");
        for (String s : split) {
            String[] kv = s.split("=");
            if (kv.length == 2) {
                paramMap.put(kv[0], kv[1]);
            }
        }
        return paramMap;
    }


    public static String getJSONPCallback(HttpServletRequest req) {
        String jsonp = req.getParameter("callback");
        if (StringUtil.notEmpty(jsonp)) {
            return jsonp;
        }
        return "callback";
    }


    public static String getRedirect(HttpServletRequest req) {
        String jsonp = req.getParameter("redirect");
        if (StringUtil.notEmpty(jsonp)) {
            return jsonp;
        }
        return null;
    }


    public static String getFmt(HttpServletRequest req) {
        String fmt = req.getParameter("fmt");
        if (StringUtil.notEmpty(fmt)) {
            return fmt;
        }
        return "json";
    }


    public static void main(String[] args) {
        String decrypt = XXTEA.decrypt("NJq0x00dM8gJZCi9Xecuf58cUsJSknVFKRz_J-SUkGU-E9bYryFsXXxnmirlVqgIBHlWVaGZaIk~", "letv");
        System.out.println("decrypt = " + decrypt);

    }
}
