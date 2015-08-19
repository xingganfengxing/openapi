package com.letv.cdn.openapi.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.letv.cdn.openapi.dao.report.DomainMapper;
import com.letv.cdn.openapi.pojo.Domain;
import com.letv.cdn.openapi.pojo.DomainExample;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.web.HttpClientUtil;


/**
 * 
 * @author liuchangfu
 *
 */
@Service
public class LogDownloadApiService {

	private static final Logger log = LoggerFactory.getLogger(LogDownloadApiService.class);
	/**日志信息根路径*/
	private final String DOWNLOAD_LOG_PATH = Env.get("download_log_path");
	/** 日志新路径userid组 */
	private static final String NEW_PATH_LOGDOWNLOAD_USERIDS = Env.get("logdownload_new_path_userid");
	/** 日志下载新路径允许时间 */
	private static final String NEW_PATH_LOGDOWNLOAD_TIME = Env.get("logdownload_new_path_time");
	/** 日志下载swift路径**/
	//private static final String LOGDOWNLOAD_SWIFT_URL ="http://10.140.60.216:8080/v1/AUTH_e1fa51fa-28cf-4a10-ae68-08d6255bd590/testcontainer";
	private static final String LOGDOWNLOAD_SWIFT_URL =Env.get("logdownload_swift_url");
	/** 日志路径*/
	private static final String LOGDOWNLOAD_PREFIX ="/data/logmerge/all/";
	/** 日志文件请求方式 时间节点*/
	public static final String REQUEST_AWAY_TIME = "20150616" ;
	@Resource
	DomainMapper domainMapper;



	/**
	 * 判断下载日志是否走新路径
	 * 
	 * @method: LogDownloadService isLogdownloadNewPath
	 * @param time
	 * @param userid
	 * @return boolean
	 * @create date： 2015年3月2日
	 * @2015, by liuchangfu.
	 */
	public boolean isLogdownloadNewPath(String userid, String time) {
		
		// 获取配置的userid组
		List<String> useridList = getConfigParam(NEW_PATH_LOGDOWNLOAD_USERIDS);
		
		if (useridList.contains(userid)&& Integer.parseInt(time) >= Integer.parseInt(NEW_PATH_LOGDOWNLOAD_TIME)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 判断下载日志是否走新路径(所有用户)
	 * 
	 * @method: LogDownloadService isLogdownloadNewPath
	 * @param time
	 * @param userid
	 * @return boolean
	 * @create date： 2015年3月2日
	 * @2015, by liuchangfu.
	 */
	public boolean isLogdownloadNewPathII( String time) {
		if (Integer.parseInt(time) >= Integer.parseInt(NEW_PATH_LOGDOWNLOAD_TIME)) {
			return true;
		}
		return false;
	}

	/**
	 * 获取日志下载路径(判断用户类型)
	 * 
	 * @method: LogDownloadService getNewLogdownloadPath
	 * @param time
	 * @param userid
	 * @param domaintag
	 * @return String
	 * @create date： 2015年3月2日
	 * @2015, by liuchangfu.
	 */
	public String getNewLogdownloadPath(String time, String timeHour,String domaintag) {
		String logSuffix = timeHour.substring(0,8).compareTo("20150601") >=0 ? ".log.desen.gz" :".log.gz";
		String dayTime = time;
		StringBuffer rootDir = new StringBuffer(LOGDOWNLOAD_PREFIX);
		DomainExample domainExample = new DomainExample();
		domainExample.createCriteria().andTagEqualTo(domaintag);
		List<Domain> domainList = domainMapper.selectByExample(domainExample);
		String filPath = null;
		// 获取用户类型
		String userType = domainList.get(0).getType();
		if (Domain.TYPE_CDN.equals(userType)) {
			int begin = domaintag.indexOf(".") + 1;
			String tag = domaintag.substring(begin, domaintag.length());
			filPath = rootDir.append(dayTime).append("/").append("cdn_").append(tag).append("_").append(timeHour).append(logSuffix).toString();
		}
		if (Domain.TYPE_CLOUD.equals(userType)) {
			int begin = "2.203b".length();
			String userid = domaintag.substring(begin, domaintag.length());
			filPath = rootDir.append(dayTime).append("/").append("cloud_").append(userid).append("_").append(timeHour).append(logSuffix).toString();
		}
		log.info("filPath {}", filPath);
		return filPath;
	}
	/**
	 * 
	 * 获取日志下载路径(cdn用户类型)
	 * @method: LogDownloadApiService  getNewLogdownloadPathII
	 * @param time
	 * @param timeHour
	 * @param domaintag
	 * @return  String
	 * @create date： 2015年3月9日
	 * @2015, by liuchangfu.
	 */
	public String getNewLogdownloadPathII(String timeHour,String domaintag) {
		//日志格式更改 6月1号
		String logSuffix = timeHour.substring(0,8).compareTo("20150601") >=0 ? ".log.desen.gz" :".log.gz";
		StringBuffer rootDir = new StringBuffer(LOGDOWNLOAD_SWIFT_URL);
		String filPath = null;
		int begin = domaintag.indexOf(".") + 1;
		String tag = domaintag.substring(begin, domaintag.length());
		filPath = rootDir.append("/").append("cdn_").append(tag).append("_").append(timeHour).append(logSuffix).toString();
		log.info("filPath {}", filPath);
		return filPath;
	}
	/**
	 * 获取文件(根据domaintag ,day 获取文件地址)
	 * 
	 * @param business
	 * @param startTime
	 * @return
	 * @throws ParseException
	 * @2014, by liuchangfu
	 */

	public String getFilePath(String business, String startTime){
		String dayString = startTime;
		StringBuffer rootDir = new StringBuffer(DOWNLOAD_LOG_PATH);
		String filPath = rootDir.append(dayString).append("/").append(business).append(".log.").append(dayString).append(".zip").toString();
		log.info("filPath {}", filPath);
		return filPath;
	}

	/**
	 * 字符串按逗号分割list
	 * 
	 * @method: LogDownloadService getConfigParam
	 * @param param
	 * @return List<String>
	 * @create date： 2015年3月2日
	 * @2015, by liuchangfu.
	 */
	public List<String> getConfigParam(String param) {
		List<String> paramList = new ArrayList<String>();
		if (param.contains(",")) {
			String[] params = param.split(",");
			paramList = Arrays.asList(params);
		} else {
			paramList.add(param);
		}
		return paramList;
	}


	/**
	 * 日志下载新路径
	 * 
	 * @method: LogDownloadService downloadFileByNew
	 * @param resp
	 * @param tag
	 * @param time
	 * @throws IOException
	 * @throws ParseException
	 *             void
	 * @create date： 2015年3月2日
	 * @2015, by liuchangfu.
	 */
	public void downloadFileByNew(HttpServletResponse resp, String tag,String time) throws IOException, ParseException {
		OutputStream ots = resp.getOutputStream();
		BufferedOutputStream os = new BufferedOutputStream(ots);
		InputStream in = null;
		URL urlfile = null;
		HttpURLConnection httpUrl = null;
		int begin = tag.indexOf(".") + 1;
		String domaintag = tag.substring(begin, tag.length());
		String fileName = "log_" + time + "_" + domaintag + ".log.gz";
		List<String> fileNameDayList = this.getUrlDayList(time, tag);
		resp.reset();
		resp.resetBuffer();
		resp.setHeader("Content-Disposition", "attachment; filename="+ fileName);
		resp.setContentType("application/octet-stream; charset=utf-8");
		long fileLength = time.compareTo(REQUEST_AWAY_TIME) >=0 ? this.fileLengthByDay(fileNameDayList):
		this.fileLengthByDayWhitGet(time, tag);
		resp.addHeader("Content-Length", Long.toString(fileLength));
		try {
			for (String logUrl : fileNameDayList) {
				urlfile = new URL(logUrl);
				httpUrl = (HttpURLConnection) urlfile.openConnection();
				httpUrl.setDoOutput(true);
				httpUrl.setRequestProperty("Content-type", "application/x-java-serialized-object"); 
				httpUrl.setRequestProperty("Content-type", "application/octet-stream; charset=utf-8");
//				httpUrl.setConnectTimeout(120000);
				httpUrl.setDoInput(true);
				int code = httpUrl.getResponseCode();
				if (code == 404) {
					log.info("下载地址不存在url : {}", new Object[] { logUrl });
					continue ;
				}
				httpUrl.connect();
				in = httpUrl.getInputStream();
				BufferedInputStream bin = new BufferedInputStream(in);
 				log.info("url ,: {}, 下载文件大小 : {}",new Object[] { logUrl ,httpUrl.getContentLength() / 1024 +"KB" });
				byte[] b = new byte[1024];
		        int len;
		        while ((len = bin.read(b)) > 0){
	                os.write(b, 0, len);
		        }
				httpUrl.disconnect();
			}
			os.flush();
		} catch (Exception e) {
			log.info("网络异常 url： {} : message: {}");
			throw new IOException();
		}
		finally{
			if (os != null) {
				os.close();
			}
			if (in != null) {
				in.close();
				log.info("下载完成...");
			}
		}
	}
	/**
	 * 日志下载老路径
	 * 
	 * @method: LogDownloadApiService  downloadFileByOld
	 * @param tag
	 * @param day
	 * @param resp
	 * @throws ParseException  void
	 * @create date： 2015年3月3日
	 * @2015, by liuchangfu.
	 */
	public void downloadFileByOld(String tag ,String day ,HttpServletResponse resp) throws ParseException{
		OutputStream os = null ;
		BufferedInputStream fileInputStream = null ;
		try {
			os = resp.getOutputStream();
			String filePath = getFilePath(tag, day);
			File file = new File(filePath);
			if(!file.exists()){
				throw new IllegalArgumentException("该日期时间的日志不存在!!");
			}
		    fileInputStream = new BufferedInputStream(new FileInputStream(file));
			resp.reset();
			resp.setHeader("Content-Disposition", "attachment; filename="+ filePath);
			resp.setContentType("application/octet-stream; charset=utf-8");
			long fileLength = file.length();
			if (fileLength <= Integer.MAX_VALUE) {
				resp.setContentLength((int) fileLength);
			} else {
				resp.addHeader("Content-Length", Long.toString(fileLength));
			}
			int i;
			while ((i = fileInputStream.read()) != -1) {
				os.write(i);
			}
			os.flush();

		}catch(IOException e){
			log.error("{获取IO流失败----}"+e);
		}
		 finally {
			if (os != null) {
				try {
				    fileInputStream.close();
					os.close();
				} catch (IOException e1) {
					log.info(e1.toString());
				}
			}
		}
	}

	/**
	 * 获取某一天的每小时的日志绝对路径文件名集合
	 * 
	 * @method: LogDownloadService getFileNameDayList
	 * @param time
	 * @param tag
	 * @return List<String>
	 * @create date： 2015年3月2日
	 * @2015, by liuchangfu.
	 */
	public List<String> getUrlDayList(String time, String tag) {
		List<String> fileNameList = new ArrayList<String>();
		for (int j = 0; j < 24; j++) {
			String hour = String.valueOf(j);
			if (hour.length() < 2) {
				hour = "0" + hour;
			}
			String timeHour = time + hour;
			/** 日志下载格式路径切换 */
			String logSuffix = timeHour.substring(0,8).compareTo("20150601") >=0 ? ".log.desen.gz" :".log.gz";
			String filePath = this.getNewLogdownloadPathII(timeHour, tag);
			String lateFilePath  = filePath.replace(logSuffix,".late".concat(logSuffix));
			fileNameList.add(filePath);
			fileNameList.add(lateFilePath);
		}
		return fileNameList;
	}
	
	/**
	 * head请求获取文件大小
	 * 
	 * @method: LogDownloadService  fileLengthByDay
	 * @param time
	 * @param tag
	 * @return
	 * @throws IOException  long
	 * @create date： 2015年6月16日
	 * @2015, by liuchangfu.
	 */
	public long fileLengthByDay(List<String> urlDayList) throws IOException {
		//List<String> urlDayList = this.getUrlDayList(time, tag);
		long fileLength = 0L;
		for (String url : urlDayList) {
			try{
				long length = HttpClientUtil.headContentLength(url, HttpClientUtil.UTF_8);
				fileLength += length ;
			}catch(Exception e){
				log.info("获取文件大小网络异常{}" ,new Object[]{url});
				throw new RuntimeException();
			}
		}
		return fileLength;
	}
	/**
	 * head 判断文件是否存在
	 * @method: LogDownloadApiService  existsFile
	 * @param time
	 * @param tag
	 * @return
	 * @throws IOException  boolean
	 * @create date： 2015年6月16日
	 * @2015, by liuchangfu.
	 */
	public boolean existsFile(String time, String tag) throws IOException {
		List<String> urlDayList = this.getUrlDayList(time, tag);
		for (String url : urlDayList) {
			try{
				boolean headExsitUrl = HttpClientUtil.headExsitUrl(url, HttpClientUtil.UTF_8);
				if(headExsitUrl==true){
					return true ;
				}
			}catch(Exception e){
				log.info("获取文件是否存在网络异常{}{}" ,new Object[]{url});
				throw new RuntimeException();
			}
		}
		return false;
	}
	
	
	
	/**
	 * 获取文件大小
	 * @method: LogDownloadApiService  fileLengthByDay
	 * @param time
	 * @param tag
	 * @return
	 * @throws IOException  long
	 * @create date： 2015年6月10日
	 * @2015, by liuchangfu.
	 */
	
	public long fileLengthByDayWhitGet(String time, String tag) throws IOException{
		String logSuffix = time.compareTo("20150601") >=0 ? ".log.desen.gz" :".log.gz";
		String url = this.getFileLengthUrl(time, tag);
		long now = System.currentTimeMillis();
		Long lenth = 0L ;
		String string = HttpClientUtil.get(url, HttpClientUtil.UTF_8);
		if(StringUtils.isNotBlank(string)){
			JSONArray parseArray = JSONArray.parseArray(string);
			for(int i = 0 ; i < parseArray.size();i++ ){
				String name = parseArray.getJSONObject(i).getString("name");
				if(name.contains(logSuffix)||name.contains(".late".concat(logSuffix))){
					lenth +=parseArray.getJSONObject(i).getIntValue("bytes");
					log.info("日志name----:{}",name);
				}
			}
			log.info("日志文件大小 ： {} Kb",lenth);
			log.info("耗时----：{}",new Long[]{System.currentTimeMillis()-now});
		}else{
			log.info("请求获取日志文件大小接口时返回错误");
			throw new IOException();
		}
		return lenth ;
	}
	
	/**
	 * 获取文件是否存在
	 * @method: LogDownloadService  fileLengthByDay
	 * @param time
	 * @param tag
	 * @return  long
	 * @throws IOException 
	 * @create date： 2015年6月10日
	 * @2015, by liuchangfu.
	 */
	
	public boolean existsFileWhitGet(String time, String tag) throws IOException {
		String logSuffix = time.compareTo("20150601") >= 0 ? ".log.desen.gz": ".log.gz";
		String url = this.getFileLengthUrl(time, tag);
		long now = System.currentTimeMillis();
		Long lenth = 0L;
		String string = HttpClientUtil.get(url, HttpClientUtil.UTF_8);
		if (StringUtils.isNotBlank(string)) {
			JSONArray parseArray = JSONArray.parseArray(string);
			for (int i = 0; i < parseArray.size(); i++) {
				String name = parseArray.getJSONObject(i).getString("name");
				if (name.contains(logSuffix)|| name.contains(".late".concat(logSuffix))) {
					lenth += parseArray.getJSONObject(i).getIntValue("bytes");
					if (lenth > 0) {
						return true;
					}
				}
			}
			log.info("耗时----：{}",new Long[] { System.currentTimeMillis() - now });
		} else {
			log.info("请求获取日志文件大小接口时返回错误");
			throw new IOException();
		}
		return false;
	}
	
	/**
	 * 获取get请求的文件大小url
	 * 
	 * @method: LogDownloadService  getFileLengthUrl
	 * @param time
	 * @param domaintag
	 * @param isMapTag
	 * @return  String
	 * @create date： 2015年6月10日
	 * @2015, by liuchangfu.
	 */
	public String getFileLengthUrl(String time, String domaintag) {
		StringBuffer rootUrl = new StringBuffer(LOGDOWNLOAD_SWIFT_URL);
		int begin = domaintag.indexOf(".") + 1;
		String tag = null;
		tag = domaintag.substring(begin, domaintag.length());
		String url = rootUrl.append("_segments?prefix=").append("cdn_").append(tag).append("_").append(time).append("&format=json").toString();
		log.info("filPath {}", url);
		return url;
	}
	
	
/**-----------------------------------------------云视频日志下载ver0.2 --------------------------------------------------------------*/
	
	/**
	 * 云视频日志下载
	 * @method: LogDownloadApiService  downloadFileByCloud
	 * @param resp
	 * @param userid
	 * @param time
	 * @throws IOException
	 * @throws ParseException  void
	 * @create date： 2015年6月18日
	 * @2015, by liuchangfu.
	 */
	public void downloadFileByCloud(HttpServletResponse resp, String userid,String time) throws IOException, ParseException {
		OutputStream ots = resp.getOutputStream();
		BufferedOutputStream os = new BufferedOutputStream(ots);
		InputStream in = null;
		URL urlfile = null;
		HttpURLConnection httpUrl = null;
		String fileName = "log_" + time + "_" + userid + ".log.gz";
		List<String> fileNameDayList = this.getUrlDayListCloud(time, userid);
		resp.reset();
		resp.resetBuffer();
		resp.setHeader("Content-Disposition", "attachment; filename="+ fileName);
		resp.setContentType("application/octet-stream; charset=utf-8");
		long fileLength = time.substring(0,8).compareTo(REQUEST_AWAY_TIME) >=0 ? this.fileLengthByDay(fileNameDayList):
		this.fileLengthCloudWhitGet(time, userid);
		resp.addHeader("Content-Length", Long.toString(fileLength));
		try {
			for (String logUrl : fileNameDayList) {
				urlfile = new URL(logUrl);
				httpUrl = (HttpURLConnection) urlfile.openConnection();
				httpUrl.setDoOutput(true);
				httpUrl.setRequestProperty("Content-type", "application/x-java-serialized-object"); 
				httpUrl.setRequestProperty("Content-type", "application/octet-stream; charset=utf-8");
//				httpUrl.setConnectTimeout(120000);
				httpUrl.setDoInput(true);
				int code = httpUrl.getResponseCode();
				if (code == 404) {
					log.info("下载地址不存在url : {}", new Object[] { logUrl });
					continue ;
				}
				httpUrl.connect();
				in = httpUrl.getInputStream();
				BufferedInputStream bin = new BufferedInputStream(in);
 				log.info("url ,: {}, 下载文件大小 : {}",new Object[] { logUrl ,httpUrl.getContentLength() / 1024 +"KB" });
				byte[] b = new byte[1024];
		        int len;
		        while ((len = bin.read(b)) > 0){
	                os.write(b, 0, len);
		        }
				httpUrl.disconnect();
			}
			os.flush();
		} catch (Exception e) {
			log.info("网络异常 url： {} : message: {}");
			throw new IOException();
		}
		finally{
			if (os != null) {
				os.close();
			}
			if (in != null) {
				in.close();
				log.info("下载完成...");
			}
		}
	}
	
	/**
	 * head请求获取文件是否存在
	 * @method: LogDownloadApiService  existsFileCloud
	 * @param day
	 * @param userid
	 * @return  boolean
	 * @create date： 2015年6月18日
	 * @2015, by liuchangfu.
	 */
	public boolean existsFileCloud(String day, String userid) {
		List<String> urlDayList = this.getUrlDayListCloud(day, userid);
		for (String url : urlDayList) {
			try{
				boolean headExsitUrl = HttpClientUtil.headExsitUrl(url, HttpClientUtil.UTF_8);
				if(headExsitUrl==true){
					return true ;
				}
			}catch(Exception e){
				log.info("获取文件是否存在网络异常{}{}" ,new Object[]{url});
				throw new RuntimeException();
			}
		}
		return false;
	}
	/**
	 * get 获取文件是否存在
	 * @method: LogDownloadService  fileLengthByDay
	 * @param time
	 * @param tag
	 * @return  long
	 * @throws IOException 
	 * @create date： 2015年6月10日
	 * @2015, by liuchangfu.
	 */
	
	public boolean existsFileCloudWhitGet(String time, String userid) throws IOException {
		String ptime = time.substring(0,8);
		String logSuffix = ptime.compareTo("20150601") >= 0 ? ".log.desen.gz": ".log.gz";
		String url = LOGDOWNLOAD_SWIFT_URL.concat("_segments?prefix=").concat("cloud_").concat(userid).concat("_").concat(time).concat("&format=json");
		long now = System.currentTimeMillis();
		Long lenth = 0L;
		String string = HttpClientUtil.get(url, HttpClientUtil.UTF_8);
		if (StringUtils.isNotBlank(string)) {
			JSONArray parseArray = JSONArray.parseArray(string);
				for(int i = 0 ; i < parseArray.size();i++ ){
					String name = parseArray.getJSONObject(i).getString("name");
					if(name.contains(logSuffix)||name.contains(".late".concat(logSuffix))){
						lenth +=parseArray.getJSONObject(i).getIntValue("bytes");
						if (lenth > 0) {
							return true;
						}
					}
				}
			log.info("耗时----：{}",new Long[] { System.currentTimeMillis() - now });
		} else {
			log.info("请求获取日志文件大小接口时返回错误");
			throw new IOException();
		}
		return false;
	}
	/**
	 * get获取文件大小
	 * @method: LogDownloadApiService  fileLengthByDay
	 * @param time
	 * @param tag
	 * @return
	 * @throws IOException  long
	 * @create date： 2015年6月10日
	 * @2015, by liuchangfu.
	 */
	
	public long fileLengthCloudWhitGet(String time, String userid) throws IOException{
		String ptime = time.substring(0,8);
		String logSuffix = ptime.compareTo("20150601") >=0 ? ".log.desen.gz" :".log.gz";
		String url = LOGDOWNLOAD_SWIFT_URL.concat("_segments?prefix=").concat("cloud_").concat(userid).concat("_").concat(time).concat("&format=json");
		long now = System.currentTimeMillis();
		Long lenth = 0L ;
		String string = HttpClientUtil.get(url, HttpClientUtil.UTF_8);
		if(StringUtils.isNotBlank(string)){
			JSONArray parseArray = JSONArray.parseArray(string);
				for(int i = 0 ; i < parseArray.size();i++ ){
					String name = parseArray.getJSONObject(i).getString("name");
					if(name.contains(logSuffix)||name.contains(".late".concat(logSuffix))){
						lenth +=parseArray.getJSONObject(i).getIntValue("bytes");
						log.info("日志name----:{}",name);
					}
				}
			log.info("日志文件大小 ： {} Kb",lenth);
			log.info("耗时----：{}",new Long[]{System.currentTimeMillis()-now});
		}else{
			log.info("请求获取日志文件大小接口时返回错误");
			throw new IOException();
		}
		return lenth ;
	}
	
	
	/**
	 * 获取下载地址集合
	 * 
	 * @method: LogDownloadApiService getUrlDayListCloud
	 * @param day
	 * @param userid
	 * @return List<String>
	 * @create date： 2015年6月18日
	 * @2015, by liuchangfu.
	 */
	private List<String> getUrlDayListCloud(String day, String userid) {
		List<String> fileNameList = new ArrayList<String>();
		String logSuffix = day.substring(0, 8).compareTo("20150601") >= 0 ? ".log.desen.gz": ".log.gz";
		if (day.length() > 8) {
			String filePath = LOGDOWNLOAD_SWIFT_URL.concat("/cloud_").concat(userid).concat("_").concat(day).concat(logSuffix);
			String lateFilePath = filePath.replace(logSuffix,".late".concat(logSuffix));
			fileNameList.add(filePath);
			fileNameList.add(lateFilePath);
		} else {
			for (int j = 0; j < 24; j++) {
				String hour = String.valueOf(j);
				if (hour.length() < 2) {
					hour = "0" + hour;
				}
				String timeHour = day + hour;
				/** 日志下载格式路径切换 */
				String filePath = LOGDOWNLOAD_SWIFT_URL.concat("/cloud_").concat(userid).concat("_").concat(timeHour).concat(logSuffix);
				String lateFilePath = filePath.replace(logSuffix,".late".concat(logSuffix));
				fileNameList.add(filePath);
				fileNameList.add(lateFilePath);
			}
		}
		return fileNameList;
	}
	
}
