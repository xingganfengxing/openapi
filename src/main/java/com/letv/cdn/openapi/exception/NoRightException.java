package com.letv.cdn.openapi.exception;

/**
 * 当客户端无权访问接口时抛出此异常<br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年10月24日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
public class NoRightException extends RuntimeException{

    private static final long serialVersionUID = -7208315676887130463L;

    private Type type;
    

    public NoRightException(Type type) {
        super(type.getMessage());
        this.type = type;
    }

    public NoRightException(Type type, Throwable cause) {
        super(type.getMessage(), cause);
        this.type = type;
    }
    
    public Type getType() {
        return this.type;
    }
    
    /**
     * 表示无权限的类型<br>
     * <b>Project</b> : openapi<br>
     * <b>Create Date</b> : 2014年10月24日<br>
     * <b>Company</b> : 乐视云计算<br>
     * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
     * 
     * @author Chen Hao
     */
    public static enum Type {
        /** 秘钥错误 */
        SECRET_KEY_WRONG("Your secret key is wrong."),
        /** 客户端ip不在允许范围内 */
        IP_NOT_ALLOW("Your ip is not allowed.");
        
        private String msg;
        
        Type(String msg){
            this.msg = msg;
        }
        
        String getMessage() {
            return this.msg;
        }
    }
}

	