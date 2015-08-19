package com.letv.cdn.openapi.utils;

/**
 * API信息代码与说明信息
 * <br>
 * 2014年12月22日
 * @author gao.jun
 *
 */
public enum ErrorMsg {
	
	OPERATION_FAILURE("100000","操作失败"),
	
	SECRET_KEY_WRONG("100001","您的secretkey错误"),
	
	IP_NOT_ALLOW("100002","您的ip没有访问权限"),
	
	DOMAIN_IS_ENABLED("101001","域名配置现为启用状态，只有禁用的域名方可删除"),
	
	TOO_MANY_TASK_OF_SINGLE_DAY_INSERT("102001","单日提交分发任务数过多"),
	
	TOO_MANY_TASK_OF_SINGLE_DAY_UPDATED("102002","单日刷新的分发任务数过多"),
	
	CACHED_FILE_PROGRESS_URL_NOT_FOUND("102003","内容刷新进度查询URL不存在或已失效");
	
	private String code;
	
	private String msg;
    
	private ErrorMsg(String code, String msg){
		this.code = code;
        this.msg = msg;
    }
    
    public String getMsg() {
        return this.msg;
    }
    
    public String getCode() {
    	return this.code;
    }
}
