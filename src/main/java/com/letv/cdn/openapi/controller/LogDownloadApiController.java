package com.letv.cdn.openapi.controller;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.rubyeye.xmemcached.MemcachedClient;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONObject;
import com.lecloud.commons.logging.annotation.Log;
import com.letv.cdn.openapi.cache.MemcacheManager;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.pojo.User;
import com.letv.cdn.openapi.pojo.UserDomain;
import com.letv.cdn.openapi.service.LogDownloadApiService;
import com.letv.cdn.openapi.service.UserDomainService;
import com.letv.cdn.openapi.service.UserService;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.utils.ErrorMsg;
import com.letv.cdn.openapi.utils.MD5Util;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapi.web.ResponseUtil;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 *  获取日志下载地址API
 * <b>Create Date</b> : 2014-10-24<br>
 * @author liuchangfu
 */
@Controller
@RequestMapping("/cdn/domain")
public class LogDownloadApiController {
	
	/**日志下载时 sgin 参数生成拼接的一个固定秘钥*/
	private final String LOG_DOWNLOAD_KEY = "cdn_openapi";
	/**domaintag 的生成前缀*/
	private final String DOMAINTAG_KEY = "102.";
	/**0.1版本日志下载uri前缀*/
	private final String DOWNLOAD_LOG_URL = Env.get("download_log_url");
	/**0.2 版本日志下载uri前缀*/
	private final String LOG_FILE_URL = Env.get("log_file_url");
	/**限制日志下载时间的天数 */
	private final String CONSTRAIN_TIME = Env.get("constraint_time");
	
	
	@Resource
	UserService userService;
	@Resource
	UserDomainService userDomainService ;
	@Resource
	LogDownloadApiService logdownloadApiService;
	
	private static final Logger log = LoggerFactory.getLogger(LogDownloadApiController.class);
	/**
	 * 获取日志下载地址接口
	 * @param request
	 * @param response
	 * @param userid
	 * @param domaintag
	 * @param day
	 * @param sign
	 * @return
	 * @throws Exception
	 * @2014, by liuchangfu
	 */
	@OpenapiAuth
	@RequestMapping(value = "/{useridurl}_{domaintag}/logdownloadurl", method = RequestMethod.GET)
	public ResponseJson getLogdownloadUrl(HttpServletRequest request,HttpServletResponse response,
								  @PathVariable() String useridurl, 
								  @PathVariable()String domaintag,
								  @RequestParam(value = "userid", required = true) String userid,
								  @RequestParam(value = "ver", required = true) String ver,
								  @RequestParam(value = "day", required = true) String day,
								  @RequestParam(value = "sign",required = true) String sign) throws Exception {
		log.info("{调用获取日志下载地址begin----userid=}"+userid);
		JSONObject jsonResult = new JSONObject();
		this.paramVidation(userid,domaintag,day,useridurl);
		String key = UUID.randomUUID().toString(); //随机key
		String keyValue =  MD5Util.getStringMD5String(userid+domaintag+day);//key 用参数拼接key的值
		MemcachedClient client = MemcacheManager.getMemcachedClient();
	    client.set(key, 60 * 60 * 24 * 7, keyValue );//设置下载失效时间/7天
		User user = userService.selectByUserid(Integer.parseInt(userid));
		String userkey = user.getUserkey();
		String sginParameter = getMD5key(userkey,domaintag,day);//签名认证
		//boolean isNewPath = logdownloadApiService.isLogdownloadNewPath(userid, day);
		boolean isNewPath = logdownloadApiService.isLogdownloadNewPathII(day);
		if(isNewPath){
			boolean exists = day.compareTo(LogDownloadApiService.REQUEST_AWAY_TIME) >=0 ? logdownloadApiService.existsFile(day, DOMAINTAG_KEY+domaintag):
			logdownloadApiService.existsFileWhitGet(day, DOMAINTAG_KEY+domaintag);
			if(exists){
				jsonResult.put("status",1);
				jsonResult.put("url", DOWNLOAD_LOG_URL+"?userid="+userid+"&domaintag="+domaintag+"&day="+day+"&key="+key+"&sign="+sginParameter);
				log.info("{调用获取日志下载地址end,返回url成功----userid=}"+userid);
				log.info("++++++++"+DOWNLOAD_LOG_URL+"?userid="+userid+"&domaintag="+domaintag+"&day="+day+"&key="+key+"&sign="+sginParameter);
			}else{
				jsonResult.put("status",0);
				jsonResult.put("url", "");
				log.info(jsonResult.toString());
			}
		}else{
			String filePath =logdownloadApiService.getFilePath(DOMAINTAG_KEY+domaintag, day);
			File file = new File(filePath);
			if(file.exists()){
				jsonResult.put("status",1);
				jsonResult.put("url", DOWNLOAD_LOG_URL+"?userid="+userid+"&domaintag="+domaintag+"&day="+day+"&key="+key+"&sign="+sginParameter);
				log.info("{调用获取日志下载地址end,返回url成功----userid=}"+userid);
				log.info("++++++++"+DOWNLOAD_LOG_URL+"?userid="+userid+"&domaintag="+domaintag+"&day="+day+"&key="+key+"&sign="+sginParameter);
			}else{
				jsonResult.put("status",0);
				jsonResult.put("url", "");
				log.info(jsonResult.toString());
			}
		}
		return ResponseJson.noCache(jsonResult, HttpStatus.OK);
	}
	
	/**
	 * 获取压缩日志下载地址
	 * <br>
	 * 2014年12月22日
	 * @author gao.jun
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @param userid 用户id
	 * @param domaintag 域名标识
	 * @param day 日志日期，格式yyyyMMdd
	 * @return
	 * @throws Exception
	 * @since 0.2
	 */
	@OpenapiAuth
	@RequestMapping(value = "/{userid}_{domaintag}/logdownloadurl", method = RequestMethod.GET, headers={"Lecloud-api-version=0.2"})
	public ResponseJson getLogdownloadUrl(HttpServletRequest request,HttpServletResponse response,
								  @PathVariable String userid, 
								  @PathVariable String domaintag,
								  @RequestParam(value = "day", required = true) String day,
						          @RequestHeader(value = "Accept",required = false) String accept) throws Exception {
		if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
		log.info("{调用获取日志下载地址begin----userid=}"+userid);
		JSONObject jsonResult = new JSONObject();
		this.paramVidation(userid,domaintag,day,userid);
		String key = UUID.randomUUID().toString(); //随机key
		String keyValue =  MD5Util.getStringMD5String(userid+domaintag+day);//key 用参数拼接key的值
		MemcachedClient client = MemcacheManager.getMemcachedClient();
	    client.set(key, 7*60*60*24,keyValue );//设置下载失效时间/24小时*7 7天
		User user = userService.selectByUserid(Integer.parseInt(userid));
		String userkey = user.getUserkey();
		String sginParameter = getMD5key(userkey,domaintag,day);//签名认证
		//boolean isNewPath = logdownloadApiService.isLogdownloadNewPath(userid, day);
		boolean isNewPath = logdownloadApiService.isLogdownloadNewPathII(day);
		//新路径日志
		if(isNewPath){
			boolean exists = day.compareTo(LogDownloadApiService.REQUEST_AWAY_TIME) >=0 ? logdownloadApiService.existsFile(day, DOMAINTAG_KEY+domaintag):
			logdownloadApiService.existsFileWhitGet(day, DOMAINTAG_KEY+domaintag);
			if(!exists){
				log.info("日志文件不存----{"+userid+"--"+domaintag+"--"+day+"}");
				log.info(jsonResult.toString());
				return ResponseJson.resourceNotFound(ErrorMsg.OPERATION_FAILURE,"该日志文件不存在");
			}
			jsonResult.put("status",1);
			//sign 为签名认证拼接上key(缓存失效时间key)
			String sign = sginParameter+key ;
			String url =  LOG_FILE_URL+userid+"_"+domaintag+"/logfile?day="+day+"&sign="+sign;
			jsonResult.put("url", url);
			log.info("{调用获取日志下载地址end,返回url成功----userid=}"+userid+"url= "+ url);
		}else{
			String filePath = logdownloadApiService.getFilePath(DOMAINTAG_KEY+domaintag, day);
			File file = new File(filePath);
			if(file.exists()){
				jsonResult.put("status",1);
				//sign 为签名认证拼接上key(缓存失效时间key)
				String sign = sginParameter+key ;
				String url =  LOG_FILE_URL+userid+"_"+domaintag+"/logfile?day="+day+"&sign="+sign;
				jsonResult.put("url", url);
				log.info("{调用获取日志下载地址end,返回url成功----userid=}"+userid+"url= "+ url);
			}else{
				log.info("日志文件不存----{"+userid+"--"+domaintag+"--"+day+"}");
				log.info(ResponseJson.resourceNotFound(ErrorMsg.OPERATION_FAILURE,"该日志文件不存在").toString());
				return ResponseJson.resourceNotFound(ErrorMsg.OPERATION_FAILURE,"该日志文件不存在");
			}
		}
		return ResponseJson.noCache(jsonResult, HttpStatus.OK);
	}
	
	/**
	 * 接口参数验证
	 * @throws ParameterException 
	 * @2014, by liuchangfu
	 */
	private void paramVidation(String userid,String domaintag,String day ,String useridurl) throws Exception {
		
		Assert.isTrue(!"".equals(day) && RegExpValidatorUtil.isDate(day, "yyyyMMdd"), "参数错误：日期(day)为空或不是一个有效的日期");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Long daytime = sdf.parse(day).getTime();
		//Long startTime = sdf.parse("20140720").getTime();
		
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, Integer.parseInt(CONSTRAIN_TIME));
		Date time = c.getTime();
		String format = sdf.format(time);
		//限制时间为32天
		Long ConstraintTime = sdf.parse(format).getTime();
		
		Assert.isTrue(!"".equals(domaintag), "参数错误：域名标识(domaintag)为空");
		Assert.isTrue(userDomainService.selectBydomaintag(DOMAINTAG_KEY+domaintag)!=null,"参数错误：域名标识(domaintag)错误");
		Assert.isTrue(!"".equals(userid)||RegExpValidatorUtil.IsNumber(userid),"参数错误：用户唯一标识码(userid)为空或不是一个有效的数字");
		Assert.isTrue(userid.equals(useridurl), "参数错误：userid不一致");
		
		 //防止用户按照规则输入其他用户userid 拼接参数，对用户传入的useid 和domaintag进行匹配
        User userByuserid = userService.selectByUserid(Integer.parseInt(userid));
        long id = userByuserid.getId();
        List<UserDomain> userDomainList = userDomainService.selectByUserid(id);
        List<String>tagList = new ArrayList<String>();
        if(userDomainList!=null){
        	for(UserDomain userDomain:userDomainList){
        		tagList.add(userDomain.getDomainTag());
        	}//判断数据库跟userid 关联tag字段 集合中是否包含 tag 参数
        	Assert.isTrue(tagList.contains(DOMAINTAG_KEY+domaintag),"参数错误：用户唯一标识码(userid或domaintag)错误");
        }else{
        	Assert.isTrue(false,"参数错误：用户唯一标识码(userid)错误");
        }
       
        //Assert.isTrue(daytime.compareTo(startTime)>=0,"日期参数错误：不支持查询20140720之前日志下载");
		Assert.isTrue(daytime.compareTo(ConstraintTime)>0,"日期参数错误,目前只支持近32天的日志下载");
		
		log.info("{调用获取日志下载地址参数校验成功----userid=}"+userid);
	}
	/**
	 * 日志下载方法
	 * @param rq
	 * @param resp
	 * @param userid
	 * @param domaintag
	 * @param day
	 * @param key
	 * @param sign
	 * @2014, by liuchangfu
	 * @throws IOException 
	 * @throws ParseException 
	 */
	@Log(project = "openapi", module = "downloadlog", function = "downlogfile")
	@RequestMapping("/downloadlog")
	public void downloadFile(HttpServletRequest rq, HttpServletResponse resp,
							 @RequestParam(value = "userid",required = true) String userid,
							 @RequestParam(value = "domaintag",required = true) String domaintag,
							 @RequestParam(value = "day",required = true) String day,
							 @RequestParam(value = "key",required = true) String key,
							 @RequestParam(value = "sign",required = true) String sign
							 ) throws IOException, ParseException{
		JSONObject jsonResult = new JSONObject();
		if(StringUtil.isEmpty(domaintag)||StringUtil.isEmpty(userid)||StringUtil.isEmpty(day)||StringUtil.isEmpty(key)||StringUtil.isEmpty(sign)||!RegExpValidatorUtil.IsNumber(userid)){
			try {
				resp.setStatus(400);
				jsonResult.put("msg", "访问错误,无效链接地址");
				ResponseUtil.sendJsonNoCache(resp, jsonResult.toString());
				return;
			} catch (IOException e) {
				log.info("参数不正常时, 返回信息IO流异常",e);
			}
		}
		User user = userService.selectByUserid(Integer.parseInt(userid));
		String userkey = null ;
		if(user!=null){
			 userkey = user.getUserkey();
		}
		try {
			String sginParameter = getMD5key(userkey,domaintag,day);
			if(!sginParameter.equals(sign)){
				resp.setStatus(400);
				jsonResult.put("msg", "访问错误,非法链接无法下载");
				ResponseUtil.sendJsonNoCache(resp, jsonResult.toString());
				return ;
			}
		} catch (Exception e2) {
			log.info("加密异常失败",e2);
			throw new IOException();
		}
		
		MemcachedClient client = MemcacheManager.getMemcachedClient();
		String timeKey=null;
		try {
			timeKey = (String)client.get(key);
			if(timeKey==null){
				resp.setStatus(400);
				jsonResult.put("msg", "无效地址,或该下载地址已失效");
				ResponseUtil.sendJsonNoCache(resp, jsonResult.toString());
				return ;
			}
		} catch (Exception e2) {
			log.info("{获取缓存错误----}"+e2);
			resp.setStatus(400);
			jsonResult.put("msg", "服务器异常,请联系管理员(mahongsheng@letv.com)或稍后再试。。。");
			ResponseUtil.sendJsonNoCache(resp, jsonResult.toString());
			return ;
		}
		//boolean isNewPath = logdownloadApiService.isLogdownloadNewPath(userid, day);
		boolean isNewPath = logdownloadApiService.isLogdownloadNewPathII(day);
		if(isNewPath){
			logdownloadApiService.downloadFileByNew(resp, DOMAINTAG_KEY+domaintag, day);
		}else{
			logdownloadApiService.downloadFileByOld(DOMAINTAG_KEY+domaintag, day, resp);
		}

	}
	
	/*
	 * ver 0.2 日志下载
	 * @param rq
	 * @param resp
	 * @param userid
	 * @param domaintag
	 * @param day
	 * @param key
	 * @param sign
	 * @throws IOException
	 */
	@Log(project = "openapi", module = "downloadlog", function = "downlogfile")
	@RequestMapping(value = "/{userid}_{domaintag}/logfile")
	public void logFile(HttpServletRequest rq,HttpServletResponse resp,
						@PathVariable(value = "userid") String userid,
						@PathVariable(value = "domaintag") String domaintag,
						@RequestParam(value = "day", required = false) String day,
						@RequestParam(value = "sign", required = false) String sign)throws IOException, ParseException {
		if (StringUtil.isEmpty(domaintag) || StringUtil.isEmpty(userid)|| StringUtil.isEmpty(day)|| StringUtil.isEmpty(sign)|| !RegExpValidatorUtil.IsNumber(userid)) {
			throw new IllegalArgumentException("访问错误,无效链接地址!!");
		}
		//截取设置缓存失效的key
		String key = sign.substring(32);
		//截取签名认证
		sign = sign.substring(0, 32);
		User user = userService.selectByUserid(Integer.parseInt(userid));
		String userkey = null;
		if (user != null) {
			userkey = user.getUserkey();
		}
		String sginParameter = null ;
		try {
			sginParameter = getMD5key(userkey, domaintag, day);
		} catch (Exception e2) {
			log.error("加密异常失败", e2);
			throw new IOException();
		}
		if (!sginParameter.equals(sign)) {
			throw new IllegalArgumentException("访问错误,非法链接无法下载!!");
		}

		MemcachedClient client = MemcacheManager.getMemcachedClient();
		String timeKey = null;
		try {
			timeKey = (String) client.get(key);
			
		} catch (Exception e2) {
			log.error("获取日志下载地址是否失效的缓存key时错误----",e2);
			throw new IOException();
		}
		if (timeKey == null) {
			throw new IllegalArgumentException("无效地址,或该下载地址已失效!!");
		}
//		boolean isNewPath = logdownloadApiService.isLogdownloadNewPath(userid, day);
		boolean isNewPath = logdownloadApiService.isLogdownloadNewPathII(day);
		if(isNewPath){
			logdownloadApiService.downloadFileByNew(resp, DOMAINTAG_KEY+domaintag, day);
		}else{
			logdownloadApiService.downloadFileByOld(DOMAINTAG_KEY+domaintag, day, resp);
		}
	}
	
	/**
	 * 日志下载时参数 sgin 的生成规则
	 * @param userkey
	 * @param domintag
	 * @param day
	 * @return
	 * @throws Exception
	 * @2014, by liuchangfu
	 */
	private String  getMD5key(String userkey ,String domintag,String day) throws Exception{
		StringBuffer bufKey =  new StringBuffer(LOG_DOWNLOAD_KEY);
		String key = bufKey.append(userkey).append(domintag).append(day).toString();
		return MD5Util.getStringMD5String(key);
	}
	
/**-------------------------------------------云视频日志下载接口-------------------------------------------------*/
	/**
	 * 云视频日志下载
	 * @method: LogDownloadApiController  getLogdownloadUrlByCloud
	 * @param request
	 * @param response
	 * @param userid
	 * @param day
	 * @param accept
	 * @return
	 * @throws Exception  ResponseJson
	 * @create date： 2015年6月18日
	 * @2015, by liuchangfu.
	 */
	@OpenapiAuth
	@RequestMapping(value = "/{userid}/logdownloadurl", method = RequestMethod.GET, headers={"Lecloud-api-version=0.2"})
	public ResponseJson getLogdownloadUrlByCloud(HttpServletRequest request,HttpServletResponse response,
								  @PathVariable String userid, 
								  @RequestParam(value = "day", required = true) String day,
						          @RequestHeader(value = "Accept",required = false) String accept) throws Exception {
		if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
		if (userid.contains("_")) {
		    return this.getLogdownloadUrl(request, response, userid.split("_")[0], userid.split("_")[1], day, accept);
		}
		log.info("{调用获取日志下载地址begin----userid=}"+userid);
		JSONObject jsonResult = new JSONObject();
		this.paramVidation(userid,day);
		String key = UUID.randomUUID().toString(); //随机key
		String keyValue =  MD5Util.getStringMD5String(userid.concat("2.203b".concat(userid)).concat(day));//key 用参数拼接key的值
		MemcachedClient client = MemcacheManager.getMemcachedClient();
	    client.set(key, 7*60*60*24,keyValue );//设置下载失效时间/24小时*7 7天
		User user = userService.selectByUserid(Integer.parseInt(userid));
		String userkey = user.getUserkey();
		String sginParameter = getMD5key(userkey,"2.203b".concat(userid),day);//签名认证
		boolean exists = day.compareTo(LogDownloadApiService.REQUEST_AWAY_TIME) >= 0 ? logdownloadApiService
				.existsFileCloud(day, userid)
				: logdownloadApiService.existsFileCloudWhitGet(day, userid);
		if (!exists) {
			log.info("日志文件不存----{" + userid + "--" + day + "}");
			log.info(jsonResult.toString());
			return ResponseJson.resourceNotFound(ErrorMsg.OPERATION_FAILURE,
					"该日志文件不存在");
		}
		jsonResult.put("status", 1);
		// sign 为签名认证拼接上key(缓存失效时间key)
		String sign = sginParameter + key;
		String url = LOG_FILE_URL + userid + "_" + "2.203b".concat(userid)
				+ "/downloadfile?day=" + day + "&sign=" + sign;
		jsonResult.put("url", url);
		log.info("{调用获取日志下载地址end,返回url成功----userid=}" + userid + "url= " + url);
		return ResponseJson.noCache(jsonResult, HttpStatus.OK);
	}
	
	@Log(project = "openapi", module = "downloadlog", function = "downlogfile")
	@RequestMapping(value = "/{userid}_{domaintag}/downloadfile")
	public void logdownloadFile(HttpServletRequest rq,HttpServletResponse resp,
						@PathVariable(value = "userid") String userid,
						@PathVariable(value = "domaintag") String domaintag,
						@RequestParam(value = "day", required = false) String day,
						@RequestParam(value = "sign", required = false) String sign)throws IOException, ParseException {
		if (StringUtil.isEmpty(domaintag) || StringUtil.isEmpty(userid)|| StringUtil.isEmpty(day)|| StringUtil.isEmpty(sign)|| !RegExpValidatorUtil.IsNumber(userid)) {
			throw new IllegalArgumentException("访问错误,无效链接地址!!");
		}
		//截取设置缓存失效的key
		String key = sign.substring(32);
		//截取签名认证
		sign = sign.substring(0, 32);
		User user = userService.selectByUserid(Integer.parseInt(userid));
		String userkey = null;
		if (user != null) {
			userkey = user.getUserkey();
		}
		String sginParameter = null ;
		try {
			sginParameter = getMD5key(userkey, domaintag, day);
		} catch (Exception e2) {
			log.error("加密异常失败", e2);
			throw new IOException();
		}
		if (!sginParameter.equals(sign)) {
			throw new IllegalArgumentException("访问错误,非法链接无法下载!!");
		}

		MemcachedClient client = MemcacheManager.getMemcachedClient();
		String timeKey = null;
		try {
			timeKey = (String) client.get(key);
		} catch (Exception e2) {
			log.error("获取日志下载地址是否失效的缓存key时错误----",e2);
			throw new IOException();
		}
		if (timeKey == null) {
			throw new IllegalArgumentException("无效地址,或该下载地址已失效!!");
		}
		logdownloadApiService.downloadFileByCloud(resp,userid, day);
	}
	
	/**
	 * 接口参数验证
	 * @throws ParameterException 
	 * @2014, by liuchangfu
	 */
	private void paramVidation(String userid,String day) throws Exception {
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHH");
		sd.setLenient(false);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setLenient(false);
		if(StringUtils.isBlank(day)){
			throw new IllegalArgumentException("时间参数为空");
		}
		try {
			if(day.length()< 8){
				throw new IllegalArgumentException("时间参数错误");
			}
			else if(day.length()>8){
				sd.parse(day);
			}else{
				sdf.parse(day);
			}
		} catch (Exception e) {
			log.info("参数时间格式错误");
			throw new IllegalArgumentException("时间格式错误,正确格式为yyyyMMddHH/yyyyMMdd");
		}
		//Assert.isTrue(!"".equals(day) && RegExpValidatorUtil.isDate(day, "yyyyMMdd"), "参数错误：日期(day)为空或不是一个有效的日期");
		Long daytime = sdf.parse(day.substring(0,8)).getTime();
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, Integer.parseInt(CONSTRAIN_TIME));
		Date time = c.getTime();
		String format = sdf.format(time);
		//限制时间为32天
		Long ConstraintTime = sdf.parse(format).getTime();
		Assert.isTrue(!"".equals(userid)||RegExpValidatorUtil.IsNumber(userid),"参数错误：用户唯一标识码(userid)为空或不是一个有效的数字");
        //Assert.isTrue(daytime.compareTo(startTime)>=0,"日期参数错误：不支持查询20140720之前日志下载");
		Assert.isTrue(daytime.compareTo(ConstraintTime)>0,"日期参数错误,目前只支持近32天的日志下载");
		log.info("{调用获取日志下载地址参数校验成功----userid=}"+userid);
	}
	
}
