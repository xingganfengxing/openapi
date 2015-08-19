package com.letv.cdn.openapi.pojo.contentterms;
/**
 * 分发or更新文件参数封装类
 * @author liuchangfu
 *
 */
//TODO rename
public class Content {
	
	private String src ; //文件资源路径
	private String key ; //回调时的唯一标示key
	private String md5;  //文件MD5值
	private String oldkey ; 
	
	private long version;
	
	private Byte status;
	
	public Content() {
		super();
	}
	public Content(String src, String key, String md5,String oldkey) {
		super();
		this.src = src;
		this.key = key;
		this.md5 = md5;
		this.oldkey = oldkey ;
	}
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	public String getOldkey() {
		return oldkey;
	}
	public void setOldkey(String oldkey) {
		this.oldkey = oldkey;
	}
	@Override
	public String toString() {
		return "ContentApi [src=" + src + ", key=" + key + ", md5=" + md5+ ", oldkey=" + oldkey + "]";
	}
	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}
	public Byte getStatus() {
		return status;
	}
	public void setStatus(Byte status) {
		this.status = status;
	}
	
	
}
