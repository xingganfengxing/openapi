package com.letv.cdn.openapi.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.letv.cdn.openapi.web.JsonResult;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapi.web.ResponseUtil;

/**
 * <br>
 * 
 * <b>Project</b> : uts-report<br>
 * <b>Create Date</b> : 2013-7-11<br>
 * <b>Company</b> : letv<br>
 * <b>Copyright @ 2013 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
public class BaseController{
    
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);
    
    /**
     * 异常处理方法
     * 
     * @param e
     * @param request
     * @param response
     */
    @ExceptionHandler(Exception.class)
    public ResponseJson exceptionHandler(Exception e, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
    
        log.error(e.getMessage(), e);
        return null;
    };
    
    /**
     * 获取当前请求的request
     * 
     * @return
     */
    protected final HttpServletRequest getCurrentRequest() {
    
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
    
    /**
     * 得到请求url
     * 
     * @param request
     * @return
     */
    protected final String getCurrentRequestUrl() {
    
        HttpServletRequest request = this.getCurrentRequest();
        String queryP = request.getQueryString();
        queryP = queryP == null ? "" : queryP;
        return request.getRequestURL().append(queryP).toString();
    }
    
    /**
     * 得到请求 contextPath
     * 
     * @param request
     * @return
     */
    protected final String getCurrentRequestContextPath() {
    
        return this.getCurrentRequest().getContextPath();
    }
    
    /**
     * redirect到指定url
     * 
     * @param paths
     * @return
     */
    protected final String redirectToUrl(String... paths) {
    
        StringBuffer s = new StringBuffer("");
        s = s.append(InternalResourceViewResolver.REDIRECT_URL_PREFIX);
        for (String p : paths) {
            s = s.append(p);
        }
        return s.toString();
    }
    
    /**
     * forward到指定url
     * 
     * @param paths
     * @return
     */
    protected final String forwardToUrl(String... paths) {
    
        StringBuffer s = new StringBuffer("");
        s = s.append(InternalResourceViewResolver.FORWARD_URL_PREFIX);
        for (String p : paths) {
            s = s.append(p);
        }
        return s.toString();
    }
    
    /**
     * 根据参数返回viewName
     * 
     * @param paths
     * @return
     */
    protected final String buildViewName(String... paths) {
    
        StringBuffer s = new StringBuffer("");
        for (String p : paths) {
            s = s.append(p);
        }
        return s.toString();
    }
    
    /**
     * 根据callback判断返回json还是jsonp
     * 
     * @param response
     * @param callback
     * @param jr
     * @throws IOException
     */
    protected void sendNoCache(HttpServletResponse response, String callback, JsonResult jr) throws IOException {
    
        if (callback == null || "".equals(callback)) {
            ResponseUtil.sendJsonNoCache(response, jr);
        } else {
            ResponseUtil.sendJsonpNoCache(response, callback, jr);
        }
    }
    
    /**
     * 根据e获取抛出此异常的方法名
     * 
     * @param e
     * @return
     */
    protected final String getThrowMethodName(Exception e) {
    
        StackTraceElement[] stes = e.getStackTrace();
        String thisClassName = this.getClass().getName();
        for (StackTraceElement ste : stes) {
            String throwClassName = ste.getClassName();
            if (throwClassName.startsWith(thisClassName)) {
                return ste.getMethodName();
            }
        }
        return "";
    }
    
    /**
     * 获取客户端的ip地址
     * 
     * @method: BaseController  getRemortIP
     * @param request
     * @return  String
     * @createDate： 2014年10月29日
     * @2014, by chenyuxin.
     */
    protected final String getRemortIP(HttpServletRequest request) {
    
        if (request.getHeader("x-forwarded-for") == null) {
            return request.getRemoteAddr();
        }
        return request.getHeader("x-forwarded-for");
    }
    
}
