package com.letv.cdn.openapi.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;

/**
 * 切面方法工具类
 * @author chenyuxin
 *
 */
public class AspectUtil {

	/**
     * 获得切面截取的方法对象
     * @param j
     * @return
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static Method getMethod(ProceedingJoinPoint j) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        // 方法签名对象
        Signature s = j.getSignature();
        // 方法声明的参数类型
        Class<?>[] paramClazz = getParamClass(s);
        // 方法所属类
        Class<?> clazz = s.getDeclaringType();
        // 方法对象
        return clazz.getMethod(s.getName(), paramClazz);
    }
    
    /**
     * 根据签名对象获得其声明的参数类型数组
     * @param s
     * @return
     * @throws ClassNotFoundException
     */
    private static Class<?>[] getParamClass(Signature s) throws ClassNotFoundException {
    	Class<?>[] paramClass = null;
    	Pattern p = Pattern.compile("(?<=\\()\\S+(?=\\))");
        Matcher m = p.matcher(s.toLongString());
        String[] classNames = null;
        if (m.find()) {
            classNames = m.group().split(",");
        }
        if(classNames != null){
	        paramClass = new Class<?>[classNames.length];
	        for (int i = 0; i < paramClass.length; i++) {
	            paramClass[i] = getClass(classNames[i]);
	        }
        }else{
        	paramClass = new Class<?>[]{};
        }
        return paramClass;
    }
    
    /**
     * 根据字符串转换为对应的类对象
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    private static Class<?> getClass(String className) throws ClassNotFoundException {
        Class<?> clazz = null;
        Class<?>[] clazzes = new Class<?>[] { byte.class, short.class, int.class, long.class, char.class, boolean.class, float.class, double.class };
        for (Class<?> c : clazzes) {
            if (className.equals(c.toString())) {
                clazz = c;
                break;
            }
        }
        if (className.indexOf("[]") > 0) {
            int s = className.indexOf("[]");
            int e = className.lastIndexOf("[]");
            clazz = Class.forName(className.replace("[]", ""));
            Object arr = Array.newInstance(clazz, new int[(e - s) / 2 + 1]);
            clazz = arr.getClass();
        }
        if (clazz == null) {
            clazz = Class.forName(className);
        }
        return clazz;
    }
}
