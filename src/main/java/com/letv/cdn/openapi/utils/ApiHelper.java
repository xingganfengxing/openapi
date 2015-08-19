package com.letv.cdn.openapi.utils;


import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.letv.cdn.openapiauth.utils.LetvApiHelper;

/**
 * API业务逻辑工具类
 * <br>
 * 2014年12月22日
 * @author gao.jun
 *
 */
public class ApiHelper {
	
	private static final String BASIC_AUTH = "Basic";
	
	private static final String JSON_ACCEPT_HEADER = "application/json";
	
	/**
	 * 从权限加密信息中获取userid，加密方式可参考0.2版本API文档
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 * @param auth 权限加密信息
	 * @return userid
	 * @since 0.2
	 */
	@Deprecated
	public static String getUserid(String auth) {
		String base64Str = auth.substring(auth.indexOf(BASIC_AUTH) + BASIC_AUTH.length());
    	
    	String[] encodeBase64StrArray = LetvApiHelper.decodeBase64(base64Str).split(":");
    	if(encodeBase64StrArray.length == 2) {
    		return encodeBase64StrArray[0];
    	}
    	
    	return null;
	}
	
	/**
	 * 从request中获得userid属性，该属性由openapiauth中的OpenapiAuthAspect设置
	 * <br>
	 * 2015年1月6日
	 * @author gao.jun
	 * @return
	 */
	public static String getUserid() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		if(request.getAttribute("userid") != null) {
			return request.getAttribute("userid").toString();
		}
		return null;
	}
	
	/**
	 * 从request中获得appkey属性，该属性由openapiauth中的OpenapiAuthAspect设置
	 * <br>
	 * 2015年1月6日
	 * @author gao.jun
	 * @return
	 */
	public static String getAppkey() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		if(request.getAttribute("appkey") != null) {
			return request.getAttribute("appkey").toString();
		}
		return null;
	}
	
	/**
	 * 校验请求头中的Accept不存在，或者为application/json
	 * <br>
	 * 2014年12月25日
	 * @author gao.jun
	 * @param accpet 求头中的Accept
	 * @return 
	 * @since 0.2
	 */
	public static boolean checkAcceptHeader(String accpet) {
		return accpet == null || accpet.contains(JSON_ACCEPT_HEADER);
	}
}
