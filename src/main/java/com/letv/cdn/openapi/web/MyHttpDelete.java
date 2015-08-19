package com.letv.cdn.openapi.web;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
/**
 * 自定义MyHttpDelete类 (用于delete 请求方式发送body 内容 )
 * @author liuchangfu
 *
 */
public class MyHttpDelete extends HttpEntityEnclosingRequestBase {

	public static final String METHOD_NAME = "DELETE";
    public String getMethod() {
        return METHOD_NAME;
    }
    public MyHttpDelete(final String uri) {
        super();
        setURI(URI.create(uri));
    }
    public MyHttpDelete(final URI uri) {
        super();
        setURI(uri);
    }
    public MyHttpDelete() {
        super();
    }
	
}
