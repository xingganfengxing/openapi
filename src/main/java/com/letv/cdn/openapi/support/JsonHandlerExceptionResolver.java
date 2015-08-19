package com.letv.cdn.openapi.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.FastJsonJsonView;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapiauth.exception.NoRightException;

/**
 * 使用json格式数据做错误相应
 * <br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年10月27日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
public class JsonHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

    private static final Logger log = LoggerFactory.getLogger(JsonHandlerExceptionResolver.class);
    
	public JsonHandlerExceptionResolver() {
		this.setOrder(-1);
		this.setPreventResponseCaching(true);
	}
	
    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        response.setContentType("application/json;charset=utf-8");
        
        String msg = "";
        String code = null;
        try {
            if (ex instanceof NoSuchRequestHandlingMethodException) {
                // pageNotFoundLogger.warn(ex.getMessage());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else if (ex instanceof HttpRequestMethodNotSupportedException) {
                // pageNotFoundLogger.warn(ex.getMessage());
                String[] supportedMethods = ((HttpRequestMethodNotSupportedException) ex).getSupportedMethods();
                if (supportedMethods != null) {
                    response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
                }
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            } else if (ex instanceof HttpMediaTypeNotSupportedException) {
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                List<MediaType> mediaTypes = ((HttpMediaTypeNotSupportedException) ex).getSupportedMediaTypes();
                if (!CollectionUtils.isEmpty(mediaTypes)) {
                    response.setHeader("Accept", MediaType.toString(mediaTypes));
                }
            } else if (ex instanceof HttpMediaTypeNotAcceptableException) {
                response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            } else if (ex instanceof MissingServletRequestParameterException || 
                       ex instanceof ServletRequestBindingException || 
                       ex instanceof TypeMismatchException || 
                       ex instanceof HttpMessageNotReadableException || 
                       ex instanceof MethodArgumentNotValidException || 
                       ex instanceof MissingServletRequestPartException || 
                       ex instanceof BindException || 
                       ex instanceof IllegalArgumentException) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else if (ex instanceof NoRightException) {
            	NoRightException noRightExp = (NoRightException)ex;
            	if(noRightExp.getHttpCode() != 0 && noRightExp.getCode() != null) {
            		response.setStatus(noRightExp.getHttpCode());
            		code = noRightExp.getCode();
            	}else {
            		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            	}
            } else if (ex instanceof ConversionNotSupportedException || ex instanceof HttpMessageNotWritableException) {
                request.setAttribute("javax.servlet.error.exception", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else{
                msg = "服务器内部错误";
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (Exception handlerException) {
            log.error("Handling of [" + ex.getClass().getName() + "] resulted in Exception", handlerException);
            msg = "服务器内部错误";
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        log.error(ex.getMessage(), ex);
        msg = StringUtil.isEmpty(msg) ? ex.getMessage() : msg;
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map m = req.getParameterMap();
        log.info("request");
        log.info("request par : {}", ((JSONObject)JSON.toJSON(m)).toJSONString());
        log.info("request parsize : {}", m.keySet().size());
        log.info("request method : {}", req.getMethod());
        log.info("request uri : {}", req.getRequestURI());
        log.info("request Content-Type {}", req.getHeader("Content-Type"));
        log.info("--------------------------------");
        try {
            InputStream is = req.getInputStream();
            BufferedReader bfr = new BufferedReader(new InputStreamReader(is));
            String ss = bfr.readLine();
            while (ss != null) {
                log.info(ss);
                ss = bfr.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            	
        }
        log.info("--------------------------------");
        return this.errorModelAndView(msg,code);
    }

    /**
     * 返回一个用于错误响应的ModelAndView
     * <b>Method</b>: ResponseJson#errorModelAndView <br/>
     * <b>Create Date</b> : 2014年10月27日
     * @author Chen Hao
     * @param msg
     * @return  ModelAndView
     */
    public ModelAndView errorModelAndView(String msg,String code) {
        Set<String> set = new HashSet<String>();
        set.add("error");
        FastJsonJsonView fjjv = new FastJsonJsonView();
        fjjv.setRenderedAttributes(set);
        Map<String, JSONObject> errMsg = new HashMap<String, JSONObject>();
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("code", code != null ? code : 100000);
        jsonObj.put("msg", msg);
        errMsg.put("error", jsonObj);
        ModelAndView mv = new ModelAndView();
        mv.setView(fjjv);
        mv.addAllObjects(errMsg);
        return mv;
    }

}
