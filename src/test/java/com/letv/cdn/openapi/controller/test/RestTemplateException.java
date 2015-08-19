package com.letv.cdn.openapi.controller.test;

import java.io.IOException;
import java.util.Map;

public class RestTemplateException extends IOException{
	
	private static final long serialVersionUID = 519763181224437812L;
	
	public Map<String,Object> properties;
	
	public RestTemplateException(){};

	public RestTemplateException(Map<String,Object> properties) {
		super();
		this.properties = properties;
	}
}
