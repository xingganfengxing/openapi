package com.letv.cdn.openapi.monitoring.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.protocol.HTTP;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.pojo.User;
import com.letv.cdn.openapi.service.UserService;
import com.letv.cdn.openapi.utils.AspectUtil;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapi.web.RequestUtils;

/**
 * 用于监控系统操作
 * @author liuchangfu
 *<b>Create Date</b> : 2014-10-24<br>
 */
@Aspect
@Component
public class MonitoringAspect {
	
	
	  private static final Logger log = LoggerFactory.getLogger(MonitoringAspect.class);
	  private final String INSERT_MONITORING_URL = Env.get("monitoring_data_url");
	  @Resource
	  UserService userService;
	  
	  
	
	@Around("execution(* *.*(..)) && @annotation(com.letv.cdn.openapi.monitoring.annotation.Monitoring)")
	public Object checkMonitoring(ProceedingJoinPoint j) throws Throwable{
		 // 获得传入的参数
        Object[] args = j.getArgs();
        // 方法对象
        Method m = AspectUtil.getMethod(j);
        
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> paramNames = new HashMap<String, String>();
        Map<String, String> MonitoringParam = new HashMap<String, String>();
        
        // 方法参数注解
        Annotation[][] parmAnns = m.getParameterAnnotations();
        
        // 将参数名排序后，将key和对应value拼接起来，再加上user对应的密钥，生成MD5校验码
        for (int i = 0; i < parmAnns.length; i++) {
            for (Annotation annotation : parmAnns[i]) {
                if (annotation instanceof RequestParam) {
                    // 获取方法的参数名称
                    String paramName = ((RequestParam) annotation).value();
                    if(args[i] != null){
                        paramNames.put(paramName, args[i].toString());
                    }
                }
//                if(annotation instanceof PathVariable){
//                	String paramName =  ((PathVariable)annotation).value();
//                	if(args[i] != null){
//                        paramNames.put(paramName, (String) args[i]);
//                    }
//                }
            }
        }
        //设置参数
        String urlParam = this.getParamAddress(request, paramNames);//url 参数
        String ip = RequestUtils.getNewRemoteAddr(request);//ip 参数
       // String ip = request.getRemoteAddr();
        String userid = (String) paramNames.get("userid");
        String username = null ;
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time =  df.format(date);
		if (userid != null) {
			if (RegExpValidatorUtil.IsNumber(userid)) {
				User userBySelect = userService.selectByUserid(Integer.parseInt(userid));
				if (userBySelect != null) {
					username = URLEncoder.encode(userBySelect.getUsername(),HTTP.UTF_8);
				} else {
					username =URLEncoder.encode("不存在 ,数据中没有的userid",HTTP.UTF_8);
				}

			} else {
				userid= null;
				username =URLEncoder.encode("不存在 ,错误的userid",HTTP.UTF_8);
			}

		}
        MonitoringParam.put("url", URLEncoder.encode(urlParam,HTTP.UTF_8));
        MonitoringParam.put("ip", ip);
        MonitoringParam.put("userid", userid);
        MonitoringParam.put("username", username);
        MonitoringParam.put("time", time);
        //调用manager 接口
        String result = null ;
        try{ 
        	 log.info("调用manager接口开始插入监控数据---"+((JSONObject) JSON.toJSON(MonitoringParam)).toJSONString());
        	 result = HttpClientUtil.post(INSERT_MONITORING_URL, MonitoringParam, HTTP.UTF_8);
        }catch(Exception e){
        	log.info("manager后台网络异常插入数据失败---"+((JSONObject) JSON.toJSON(MonitoringParam)).toJSONString());
        }
        if(result!=null){
        	JSONObject json = JSON.parseObject(result);
        	boolean value = json.getBooleanValue("result");
        	if(value){
        		log.info("调用接口监控数据插入成功---"+((JSONObject) JSON.toJSON(MonitoringParam)).toJSONString());
        	}else{
        		log.info("调用接口监控数据插入失败---"+((JSONObject) JSON.toJSON(MonitoringParam)).toJSONString());
        	}
        }
         
		return j.proceed(args);
	}
	/**
	 * 获取url参数地址
	 * @param request
	 * @param paramNames
	 * @return
	 */
	
	private String getParamAddress( HttpServletRequest request,Map<String, String> paramNames){
		String urlparam = null;
		if (paramNames.size() > 0) {
			// 获取参数
			String url = request.getRequestURL().toString() + "?";
			Set<String> paramSet = paramNames.keySet();
			for (String paraname : paramSet) {
				String paramKeyValue = paraname + "="
						+ paramNames.get(paraname) + "&";
				url += paramKeyValue;
			}
			urlparam = url.substring(0, url.lastIndexOf("&"));// url 参数
		}else{//没有参数时
			urlparam = request.getRequestURL().toString();
		}
		return urlparam ;
	}
	
}