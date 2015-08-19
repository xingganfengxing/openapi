/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : SignAspect.java 
 * Date Created: 2014年10月20日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月20日 上午9:15:33 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.exception.NoRightException;
import com.letv.cdn.openapi.exception.NoRightException.Type;
import com.letv.cdn.openapi.pojo.User;
import com.letv.cdn.openapi.service.UserService;
import com.letv.cdn.openapi.utils.AspectUtil;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.LetvApiHelper;

/**
 * TODO:校验MD5码
 * 
 * @author chenyuxin
 * @createDate 2014年10月20日
 */
@Aspect
@Component
public class SignAspect{

    private static final Logger log = LoggerFactory.getLogger(SignAspect.class);
    
    @Resource
    UserService userService;
    
    /**
     * 验证MD5校验码是否一致
     * 
     * @method: SignAspect checkSign
     * @param j
     * @return
     * @throws Throwable
     *         Object
     * @createDate： 2014年10月20日
     * @2014, by chenyuxin.
     */
    @Around("execution(* *.*(..)) && @annotation(com.letv.cdn.openapi.annotation.Sign)")
    public Object checkSign(ProceedingJoinPoint j) throws Throwable {
    
        // 获得传入的参数
        Object[] args = j.getArgs();
        // 方法对象
        Method m = AspectUtil.getMethod(j);
        Map<String, String> paramNames = new HashMap<String, String>();
        // 方法参数注解
        Annotation[][] parmAnns = m.getParameterAnnotations();
        
        // 将参数名排序后，将key和对应value拼接起来，再加上user对应的密钥，生成MD5校验码
        for (int i = 0; i < parmAnns.length; i++) {
            for (Annotation annotation : parmAnns[i]) {
                if (annotation instanceof RequestParam) {
                    // 获取方法的参数名称
                    String paramName = ((RequestParam) annotation).value();
                    if (args[i] != null) {
                        paramNames.put(paramName, args[i].toString());
                    }
                }
            }
        }
        
        String sign = (String) paramNames.get("sign");
        String userid = (String) paramNames.get("userid");
        
        // userid不能为空
        Assert.isTrue(StringUtil.notEmpty(userid) && RegExpValidatorUtil.IsNumber(userid),"查询参数错误：用户唯一标识码(userid)为空或不是一个有效的数字");
        
        // 验证uesrid是否存在
        User user = userService.selectByUserid(Integer.parseInt(userid));
        Assert.isTrue(user != null, "查询参数错误：用户唯一标识码(userid)不存在");
        
        Assert.isTrue(StringUtil.notEmpty(sign), "查询参数错误：MD5校验码(sign)不能为空");
        Assert.hasLength((String) paramNames.get("ver"), "查询参数错误：版本号(ver)不能为空");
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        // 用户ip校验
        // TODO 获取用户访问的白名单
        // 获取对应用户允许访问接口的ip
        List<String> whiteList = this.allowIps(userid);
        boolean visitRight = true;//whiteList.contains(request.getRemoteAddr());
        log.info("ip:" + LetvApiHelper.getClientIpAddr(request));
        if (!visitRight) {
            throw new NoRightException(Type.IP_NOT_ALLOW);
        }
        String requestParamString = this.getSortMapString2(paramNames);
        paramNames.remove("sign");
        // 生成MD5校验码
        String md5Sign = LetvApiHelper.sign(paramNames, user.getUserkey());
        // 判断MD5校验码是否一致
        if (!sign.equals(md5Sign)) {
            @SuppressWarnings("unchecked")
            Map<String, String[]> paraMap = request.getParameterMap();
            log.info("sign不匹配：传入的参数--{}, 收到的参数--{}", new Object[]{ getSortMapString(paraMap), requestParamString});
            throw new NoRightException(Type.SECRET_KEY_WRONG);
        }
        return j.proceed(args);
        
    }
    
    /**
     * 按特定方式获取Map的String
     * 
     * 将Map的key按升序排序，将key和value拼接起来，按之前的顺序将所有字符串拼接起来
     * @method: SignAspect  getSortMapString
     * @param m
     * @return  String
     * @createDate： 2014年11月6日
     * @2014, by chenyuxin.
     */
    private String getSortMapString(Map<String, String[]> m){
        String[] names = m.keySet().toArray(new String[m.size()]);
        Arrays.sort(names);

        StringBuilder buf = new StringBuilder();
        for (String name : names) {
            buf.append(name);
            String[] values = m.get(name);
            for(int i = 0; i < values.length; i++){
                buf.append(values[i]);
                if(i != values.length -1){
                    buf.append(name);
                }
            }
        }
        return buf.toString();
    }
    
    private String getSortMapString2(Map<String, String> m){
        String[] names = m.keySet().toArray(new String[m.size()]);
        Arrays.sort(names);

        StringBuilder buf = new StringBuilder();
        for (String name : names) {
            buf.append(name);
            buf.append(m.get(name));
        }
        return buf.toString();
    }
    
    private List<String> allowIps(String userid) {
    
        List<String> ipList = new ArrayList<String>();
        // TODO 获取白名单方法
        ipList.add("127.0.0.1");
        return ipList;
    }
    
}
