package com.letv.cdn.openapi.utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.exception.OpenapiFailException;
import com.letv.cdn.openapi.pojo.contentterms.Content;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapiauth.utils.LetvApiHelper;

/**
 * CDN调用帮助类
 * @author gao.jun
 *
 */
public class CDNHelper {
	
	private static final Logger log = LoggerFactory.getLogger(CDNHelper.class);
	
	/**
	 * 删除分发文件的接口
	 */
	private static final String DELETE_FILE_URI = Env.get("delete_file_uri");

	/**
	 * 查询分发文件的接口
	 */
	private static final String GET_LECLOUD_FILE_URI = Env.get("get_lecloud_file_key_uri");
	
	/**
	 * 进度查询URL
	 */
	private static final String CDN_DIST_PROGRESS = Env.get("cdn_distProgress");
	
	/**
	 * 通过head请求，生成文件的版本号
	 * @param src
	 * @param domain 加速域名
	 * @param filterDomain 过滤域名
	 * @return
	 */
	public static Long getVersion(String src, String domain, String filterDomain, String setHost) {
		boolean needHostHeader = false;
		
		// 走gfs，转换src，转换为filterDomain，不需要设置host头
		if(StringUtils.isNotBlank(filterDomain)) {
			String oldSrcDomain = StringUtils.substringBetween(src, "http://", "/");
			src = "http://".concat(filterDomain).concat(StringUtils.substringAfter(src, oldSrcDomain))
			        .concat("?lersrc=").concat(LetvApiHelper.encodeBase64(oldSrcDomain))
			        .concat("&cuhost=").concat(LetvApiHelper.encodeBase64(domain));
			// setHost有值，则增加sthost参数
			if(StringUtils.isNotBlank(setHost)) {
				src.concat("&sthost=").concat(LetvApiHelper.encodeBase64(setHost));
			}
		}else if(StringUtils.isNotBlank(setHost)) {// 不走gfs并且设置了sethost，则设置host头
			needHostHeader = true;
		}
		// 获取header信息
		Map<String,String> headers = null;
		if(needHostHeader) {
		    Map<String,String> head = new HashMap<String,String>();
		    head.put("Host", setHost);
			try {
				headers = HttpClientUtil.head(src, head, HttpClientUtil.UTF_8);
			}catch (IOException e) {
				log.info("第一次获取ucloud域名下文件的Header信息失败，Src：{}", src);
			}
			if(headers == null) {
				try {
					headers = HttpClientUtil.head(src, head, HttpClientUtil.UTF_8);
				}catch (IOException e) {
					log.info("第二次获取ucloud域名下文件的Header信息失败，Src：{}", src);
				}
			}
		}else {
			try {
				headers = HttpClientUtil.head(src, null, HttpClientUtil.UTF_8);
			}catch (IOException e) {
				log.info("第一次获取文件的Header信息失败，Src：{}", src);
			}
			if(headers == null) {
				try {
					headers = HttpClientUtil.head(src, null, HttpClientUtil.UTF_8);
				}catch (IOException e) {
					log.info("第二次获取文件的Header信息失败，Src：{}", src);
				}
			}
		}
		
		// 计算version
		Long version = null;
		if(headers != null) {
			version = 0L;
			String contentLength = headers.get("Content-Length");
			if(contentLength != null) {
				version += Long.valueOf(contentLength);
			}
			String lastModified = headers.get("Last-Modified");
			if(lastModified != null) {
				DateFormat format=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				try {
					version += format.parse(lastModified).getTime();
				} catch (ParseException e) {
					log.info("解析Header中的GMT时间失败，Time：{}", lastModified);
				}
			}
		}
		
		return version;
	}

	/**
	 * 删除CDN中的缓存文件
	 * @param src
	 * @param storePath
	 * @param domain 访问域名
	 * @return
	 * @throws IOException
	 */
	public static boolean deleteCdnFile(String src, String storePath, String domain, String filterDomain) throws IOException {
		boolean flag = false;
		boolean delPurgeSuccess = true;
		if(StringUtils.isNotBlank(filterDomain)) {
			String result = null;
			String url = "http://".concat(filterDomain).concat("/purge/?url=").concat(filterDomain).concat(StringUtils.substringAfter(src, StringUtils.substringBetween(src, "http://", "/")));
			try {
				result = HttpClientUtil.get(url, HttpClientUtil.UTF_8);
			} catch (Exception e) {
				log.error("删除gfs文件失败，url:{}", url, e);
				return flag;
			}
			if(StringUtils.isNotBlank(result)) {
				JSONObject jsonObj = JSONObject.parseObject(result);
				if("error".equals(jsonObj.get("return")) && "NOT VAILD URL".equals(jsonObj.get("msg"))) {
					delPurgeSuccess = false;
				}
			}else {
				delPurgeSuccess = false;
			}
		}
		if(delPurgeSuccess) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("user", "acloud");
			map.put("file", storePath);
			// realdel=0，只删DB，不删物理文件
			map.put("realdel", "0");
			// 删除物理文件
			flag = HttpClientUtil.getDelete(DELETE_FILE_URI, map,HttpClientUtil.UTF_8);
			if (flag) {
				Map<String, String> params = new HashMap<String, String>();
				String[] srcArr = src.substring(7).split("/", 2);
				params.put("domain", domain);
				params.put("uri", "/" + srcArr[1]);
				params.put("delete", "1");
				// 删除缓存中的记录
				HttpClientUtil.get(GET_LECLOUD_FILE_URI, params,HttpClientUtil.UTF_8);
			} else {
				throw new OpenapiFailException("删除预分发文件时cdn接口返回删除失败");
			}
		}
		
		return flag;
	}
	
	/**
	 * 生成文件提交对应xml消息体
	 * @param contents
	 * @param userid
	 * @return
	 */
	public static String generateSubmitXml(List<Content> contents, String userid) {
		// 生成xml
		Document document = DocumentHelper.createDocument();
		// 设置xml的字符编码
		document.setXMLEncoding("UTF-8");
		Element ccscElement = document.addElement("ccsc");
		for (Content contentApi : contents) {
			// 后台存储用户的任务标识,为避免不同用户的key标识重复,将userid+key作为lecloud系统对此次任务的标识
			Element item_idElement = ccscElement.addElement("item_id");
			item_idElement.addAttribute("value",userid + "_" + contentApi.getKey());
			Element source_pathElement = item_idElement.addElement("source_path");
			source_pathElement.addText(contentApi.getSrc());
			if (StringUtil.notEmpty(contentApi.getMd5())) {
				Element md5Element = item_idElement.addElement("md5");
				md5Element.addText(contentApi.getMd5());
			}
			if (contentApi.getVersion()!= 0) {
				Element versionElement = item_idElement.addElement("version");
				versionElement.addText(String.valueOf(contentApi.getVersion()));
			}
		}
		String xmlData = document.asXML();
		log.info("xml内容： --start--" + document.asXML() + "--End--");
		return xmlData;
	}
	
	/**
	 * 根据src获取短地址
	 * @param userid
	 * @param domaintag
	 * @param src
	 * @param domain 访问域名
	 * @return
	 * @throws IOException
	 */
	public static String getShortPathBySrc(String userid, String domaintag, String src, String domain)throws IOException {
		String[] srcArr = src.substring(7).split("/", 2);
		if (srcArr.length < 2 || "".equals(srcArr[1])) {
			throw new IllegalArgumentException("文件源地址(src)缺少要查询的资源路径");
		}
		// src按"/"分割，前一部分为用户的domian，后一部分为文件的路径
		// http://log.letvcdn.letv.com/ext/mapping?domain=video.cztv.com&uri=/video/rzx/201309/17/1379379018229.mp4
		Map<String, String> params = new HashMap<String, String>();
		if (domain == null) {
			params.put("domain", srcArr[0]);
		} else {
			params.put("domain", domain);
		}
		params.put("uri", "/" + srcArr[1]);
		// 获取资源短地址
		String queryResult = HttpClientUtil.get(GET_LECLOUD_FILE_URI, params,HttpClientUtil.UTF_8);
		// 分发成功返回的结果：{ "status": 1,
		// "result":"88\/25\/60\/cztvcom\/0\/video\/rzx\/201309\/17\/1379379018229.mp4","filename":
		// "video\/rzx\/201309\/17\/1379379018229.mp4" }
		JSONObject jo = JSONObject.parseObject(queryResult);
		// 当分发成功后,status为1,result对应的是lecloud使用的key;失败或正在分发中，status为0,result为空字符串
		Integer status = jo.getInteger("status");
		if (status == 1) {
			String result = jo.getString("result").replace("\\", "");
			log.info("ShortPathBySrc-------------" + result);
			return result;
		}
		log.info("查询短地址为空----cdn");
		return null;
	}
	
	/**
	 * 依据短地址查询CDN分发进度<br>
	 * @author gao.jun
	 * @param storePath
	 * @return 百分比
	 * @throws IOException
	 */
	public static int getDistPercent(String storePath) throws IOException {
		int progress = 0;
		if(StringUtils.isBlank(storePath)) {
			return progress;
		}
    	String progressStr;
		try {
			progressStr	= HttpClientUtil.get(CDN_DIST_PROGRESS.concat(storePath), HttpClientUtil.UTF_8);
		} catch (IOException e) {
			throw e;
		}
		if(StringUtils.isNotBlank(progressStr)) {
			progress = JSONObject.parseObject(progressStr).getIntValue("percent");
		}
    	return progress;
    }
	
	/**
	 * 获取文件已分发数量
	 * 2015年6月13日<br>
	 * @author gao.jun
	 * @param storePath
	 * @return
	 * @throws IOException
	 */
	public static Integer getDistFileCopyNum(String storePath) throws IOException {
		Integer progress = null;
		if(StringUtils.isBlank(storePath)) {
			return progress;
		}
    	String progressStr;
		try {
			progressStr	= HttpClientUtil.get(CDN_DIST_PROGRESS.concat(storePath), HttpClientUtil.UTF_8);
		} catch (IOException e) {
			throw e;
		}
		if(StringUtils.isNotBlank(progressStr)) {
			progress = JSONObject.parseObject(progressStr).getInteger("filecopy");
		}
    	return progress;
    }
	
}
