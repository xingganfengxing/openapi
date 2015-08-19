package com.letv.cdn.openapi.exception;


/**
 * 当客户端访问接口失败时抛出异常(数据库异常,底层cdn接口发生异常返回失败信息...)<br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年10月24日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author liuchangfu
 */
public class OpenapiFailException extends RuntimeException {

	
	private static final long serialVersionUID = -4988993144292897656L;
	
	

	

	public OpenapiFailException() {
		super();
	}

	public OpenapiFailException(String message) {
		super(message);
	}
	

	public OpenapiFailException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenapiFailException(Throwable cause) {
		super(cause);
	}

}
