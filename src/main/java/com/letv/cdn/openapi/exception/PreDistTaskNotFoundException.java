package com.letv.cdn.openapi.exception;

/**
 * 未找到预分发任务
 * <br>
 * 2015年3月30日
 * @author gao.jun
 *
 */
public class PreDistTaskNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 2028047476866032492L;

	public PreDistTaskNotFoundException() {
		super();
	}

	public PreDistTaskNotFoundException(String message) {
		super(message);
	}

	public PreDistTaskNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public PreDistTaskNotFoundException(Throwable cause) {
		super(cause);
	}

}
