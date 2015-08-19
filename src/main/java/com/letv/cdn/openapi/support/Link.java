package com.letv.cdn.openapi.support;

/**
 * 链接对象，用于API调用后相关资源URL的封装
 * @author gao.jun
 *
 */
public class Link {
	
	public String href;
	
	public String rel;
	
	public String method;

	public Link(String href, String rel, String method) {
		super();
		this.href = href;
		this.rel = rel;
		this.method = method;
	}
}
