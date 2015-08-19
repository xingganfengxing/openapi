package com.letv.cdn.openapi.monitoring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  添加该注解，表示对该方法接口 进行日志监控
 * @author liuchangfu
 * <b>Create Date</b> : 2014-10-24<br>
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public  @interface Monitoring {
	
}
