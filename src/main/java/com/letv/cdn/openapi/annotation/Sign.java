/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : Sign.java 
 * Date Created: 2014年10月20日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月20日 上午9:10:19 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO:添加该注解，表示符合切面校验MD5码的方法
 * 
 * @author chenyuxin
 * @createDate 2014年10月20日
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sign {
    
}
