package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.cache.MemcacheManager;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.controller.content.CachedFileController;
import com.letv.cdn.openapi.dao.openapi.PreDistTaskMapper;
import com.letv.cdn.openapi.dao.report.UserMapper;
import com.letv.cdn.openapi.exception.NoRightException;
import com.letv.cdn.openapi.exception.NoRightException.Type;
import com.letv.cdn.openapi.exception.OpenapiFailException;
import com.letv.cdn.openapi.exception.PreDistTaskNotFoundException;
import com.letv.cdn.openapi.pojo.ApiDomain;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.pojo.contentterms.Content;
import com.letv.cdn.openapi.service.CachedFileService.ProcessInfo;
import com.letv.cdn.openapi.utils.CDNHelper;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapiauth.utils.LetvApiHelper;

/**
 * TODO:ContentService ver 0.2 的service层
 * 
 * @author liuchangfu
 * @createDate 2014年12月2日
 */
@Service
public class ContentIIService {
	
	/**
	 * verion在缓存中的超时时间为一周
	 */
	private static final int SECONDS_ONE_WEEK = 604800;

	/**
	 * 设置version的超时时间为5分钟
	 */
	private static final int SET_VERSION_TIMEOUT = 300000;

	private static final Logger log = LoggerFactory.getLogger(ContentIIService.class);

	/** 提交预分发文件的接口 */
	private static final String SUBMIT_FILE_URI = Env.get("submit_file_uri");
	/** 请求接口消息体格式类型为json */
	public static final String CONTENT_TYPE_JSON = "application/json";
	/** 请求接口消息体格式类型为xml */
	public static final String CONTENT_TYPE_XML = "text/xml";
	/** 提交文件任务cdn成功返回的状态码 */
	public static final int SUB_STATUS_SUCCESS = 200;
	/** 提交文件任务cdn重复返回的状态码 */
	public static final int SUB_STATUS_REPEAT = 400;
	/** 预分发任务提交,删除,更新请求成功返回的状态码 */
	public static final int SUCCESS_STATUS = 1;
	/** 预分发任务已存在提交重复状态码 */
	public static final int REPEAT_STATUS = 2;
	/** 预分发任务删除成功,提交失败返回的状态码 */
	public static final int SUB_FAIL__STATUS = 3;
	/** 预分发任务key重复提交失败 */
	public static final int KEY_REPEAT = 5;
	/**
	 * 获取version超时或超过失败重试次数
	 */
	public static final int GET_VERSION_TIMEOUT = 4;
	/** 预分发任务提交,删除失败返回的状态码 */
	public static final int FAIL__STATUS = 0;

	@Resource
	DomainService domainService;

	@Resource
	PreDistTaskService pdts;

	@Resource
	UserMapper usermapper;

	@Resource
	CachedFileService cachedFileService;
	@Resource
	PreDistTaskMapper pdtm;

	@Resource
	ApiDomainService apiDomainService;
	
	@Resource
	DomainExtensionService domainExtService;
	
	 /**
     * 根据content中的内容提交预分发任务。
     * <br>
     * 2015年4月8日
     * @author gao.jun
     * @update liuchangfu
     * @param submitContents
     * @return
     * @throws IOException 
     */
    @Transactional(value = "transactionManagerOpenapi", readOnly = false)
    public JSONArray submitContent(List<Content> submitContents,Integer userid,String domaintag,String cip ,String tasktag) throws IOException {
    	JSONArray jsonArray = new JSONArray();
    	CoopDomain coopDomain = domainService.selectByUseridAndDomaintag(userid.toString(), domaintag);
		int status = FAIL__STATUS;
    	insertPreDistTasks(submitContents, userid, domaintag, tasktag);
    	for(Content content : submitContents) {
    		String filterDomain = domainExtService.getFilterDomain(domaintag);
    		if(org.apache.commons.lang.StringUtils.isNotBlank(filterDomain)) {
    			// 转换src，转换为filterDomain
				String src = content.getSrc();
				String oldSrcDomain = org.apache.commons.lang.StringUtils.substringBetween(src, "http://", "/");
				content.setSrc("http://".concat(filterDomain).concat(org.apache.commons.lang.StringUtils.substringAfter(src, oldSrcDomain))
				        .concat("?lersrc=").concat(LetvApiHelper.encodeBase64(oldSrcDomain))
				        .concat("&cuhost=").concat(LetvApiHelper.encodeBase64(coopDomain.getSubDomain())));
    		}
    	}
		log.info("访问文件分发系统接口：" + SUBMIT_FILE_URI);
		String tmpResult = HttpClientUtil.postXml(SUBMIT_FILE_URI,
				pdts.subParams(userid.toString(), cip, coopDomain.getSubDomain()),
				CDNHelper.generateSubmitXml(submitContents, userid.toString()), HttpClientUtil.UTF_8);
		// 服务器内部异常
		if (tmpResult == null) {
			log.error("服务器内部异常");
			throw new IOException();
		}
		// 调用接口的IP不在白名单内
		if ("403".equals(tmpResult)) {
			log.info("调用接口的IP不在白名单内");
			throw new NoRightException(Type.IP_NOT_ALLOW);
		}
		JSONArray ja = JSONObject.parseArray(tmpResult);
		log.info("提交预分发文件结果：{}", ja.toJSONString());
		String src = null;
		String key = null;
		for (int i = 0; i < ja.size(); i++) {
			JSONObject jo = ja.getJSONObject(i);
			int statusCode = jo.getIntValue("status");
			String resultKey = jo.getString("itemid");
			if (resultKey != null) {
				key = resultKey.split("_", 2)[1];
			}
			src = jo.getString("source");
			if (statusCode == SUB_STATUS_SUCCESS) {
				status = SUCCESS_STATUS;
			} else if (statusCode == SUB_STATUS_REPEAT) {
				status = REPEAT_STATUS;
				try {
					this.pdts.updateRepeat(userid, key, src, tasktag); // 更改表状态为重复
				} catch (Exception e1) {
					log.error("更改表状态为任务提交重复时数据库异常");
				}
			} else {
				try {
					this.pdts.updateSubFail(userid, key, src, tasktag); // 更改表状态为提交失败
				} catch (Exception e2) {
					log.error("更改表状态为提交失败时数据库异常");
				}
			}
			//获取返回信息
			JSONObject jobj = resultJsonByRefresh(src, key, status);
			jsonArray.add(jobj);
		}
    	return jsonArray;
    }
    
    /**
     * 提交任务，插入数据库
     * 2015年5月7日<br>
     * @author gao.jun
     * @param submitContents
     * @param userid
     * @param domaintag
     * @param tasktag
     * @return
     */
    @Transactional(value = "transactionManagerOpenapi", readOnly = false)
	public List<Content> insertPreDistTasks(List<Content> submitContents,
			Integer userid, String domaintag, String tasktag) {
		return insertPreDistTasks(submitContents, userid, domaintag, tasktag, false);
	}
    
    /**
     * 提交任务，插入数据库，用于异步提交
     * 2015年5月7日<br>
     * @author gao.jun
     * @param submitContents
     * @param userid
     * @param domaintag
     * @param tasktag
     * @return
     */
    @Transactional(value = "transactionManagerOpenapi", readOnly = false)
	public List<Content> insertPreDistTasks(List<Content> submitContents,
			Integer userid, String domaintag, String tasktag, boolean presubmit) {
		for (Content contentApi : submitContents) {
			if(contentApi.getStatus() != null && contentApi.getStatus() == 2) {
				continue;
			}
			if (this.pdts.insertWithTasktag(userid, domaintag,
					contentApi.getSrc(), contentApi.getKey(),
					contentApi.getMd5(), tasktag, PreDistTask.TASK_SUB_TYPE, presubmit)) {
				contentApi.setStatus((byte)1);
			}else {
				contentApi.setStatus((byte)0);
			}
		}
		return submitContents;
	}
    
	/**
	 * 删除分发文件
	 * 
	 * @param userid
	 * @param domaintag
	 * @param contentApi
	 * @return
	 * @throws IOException
	 */
	@Transactional(value = "transactionManagerOpenapi",readOnly = false)
	public JSONObject deleteFile(Integer userid, String domaintag,Content contentApi) throws IOException {
		JSONObject jsonObject = new JSONObject();
		CoopDomain coopDomain = domainService.selectByUseridAndDomaintag(userid.toString(), domaintag);
		if (coopDomain == null) {
			throw new IllegalArgumentException("参数不匹配(该用户未开通此功能)");
		}
		List<PreDistTask> pdtList = this.pdts.selectByOutkeySrc(userid,contentApi.getOldkey(), contentApi.getSrc());
		try {
			this.pdts.delTask(userid, contentApi.getOldkey(),contentApi.getSrc());// 更新库状态
		} catch (Exception e) {
			log.info(e.toString());
			throw new OpenapiFailException("删除预分发文件时更改对pre_dist_task数据状态----数据库异常");
		}
		List<String> deleteKeyList = new ArrayList<String>();
		if (pdtList.size() > 0) {
			for (PreDistTask pdt : pdtList) {
				String storePath = null;
				storePath = pdt.getStorePath();
				if (storePath == null) {
					storePath = CDNHelper.getShortPathBySrc(userid.toString(),domaintag, pdt.getSrc(), coopDomain.getSubDomain());
				}
				if (StringUtils.hasLength(storePath)) {
					boolean flag = CDNHelper.deleteCdnFile(pdt.getSrc(), storePath, coopDomain.getSubDomain(), domainExtService.getFilterDomain(domaintag));
					if (flag) {
						deleteKeyList.add(pdt.getOutkey().split("_", 2)[1]); // 分解outkey
					} else {
						throw new OpenapiFailException("删除预分发文件时cdn接口返回删除失败");
					}
				} else {
					throw new OpenapiFailException("删除预分发文件时短地址不存在");
				}
			}
		} else {
			throw new PreDistTaskNotFoundException("删除预分发文件未找到分发成功记录异常");
		}
		jsonObject = this.produceDelJsonObject(contentApi, deleteKeyList);
		return jsonObject;
	}

	/**
	 * 通过资源地址获取letv对应的短地址
	 * 
	 * @param userid
	 * @param domaintag
	 * @param src
	 * @return
	 * @throws IOException
	
	public String getShortPathBySrc(String userid, String domaintag, String src)throws IOException {
		CoopDomain cd = this.domainService.selectByUseridAndDomaintag(userid,domaintag);
		String[] srcArr = src.substring(7).split("/", 2);
		if (srcArr.length < 2 || "".equals(srcArr[1])) {
			throw new IllegalArgumentException("文件源地址(src)缺少要查询的资源路径");
		}
		// src按"/"分割，前一部分为用户的domian，后一部分为文件的路径
		// http://log.letvcdn.letv.com/ext/mapping?domain=video.cztv.com&uri=/video/rzx/201309/17/1379379018229.mp4
		Map<String, String> params = new HashMap<String, String>();
		if (cd == null) {
			params.put("domain", srcArr[0]);
		} else {
			params.put("domain", cd.getDomain());
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
	} */

	/**
	 * 解析刷新参数及校验
	 * @method: ContentIIService  parseJsonArrayBySub
	 * @param data
	 * @param auth
	 * @param baseUserid
	 * @param batchSize 每次提交最大值
	 * @return  List<Content>
	 * @create date： 2015年4月8日
	 * @2015, by liuchangfu.
	 */
	public List<Content> parseJsonArrayBySub(String data, String auth,String baseUserid, int batchSize) {
		List<Content> contentApiList = new ArrayList<Content>();
		JSONObject jsonObject = JSON.parseObject(data);
		if (StringUtil.isEmptyTrim(auth)) {
			throw new IllegalArgumentException("请求体参数错误!!");
		}
		String domaintag = this.parseJson(data, "domaintag");
		if (StringUtil.isEmptyTrim(domaintag)) {
			throw new IllegalArgumentException("json格式参数错误,domaintag参数不存在或为空串");
		}
		if (baseUserid == null) {
			throw new IllegalArgumentException("请求头的Authorization 格式设置有误!!");
		}
		String items = jsonObject.getString("items");
		if(StringUtil.isEmptyTrim(items)){
			throw new IllegalArgumentException("请求头参数格式有误!!");
		}
		JSONArray jsonArray = JSON.parseArray(items);
		for (int i = 0; i < jsonArray.size(); i++) {
			String key = null;
			String src = null;
			// String oldkey = null;
			key = jsonArray.getJSONObject(i).getString("key");
			if (StringUtil.isEmptyTrim(key)) {
				log.info("json数据key不存在时或空串时，自动生成一个随机key");
				key = UUID.randomUUID().toString().replace("-", "");
			} else {
				if (key.length() > 32) {
					throw new IllegalArgumentException("key字符个数不能大于32");
				}
				if(pdts.selectByOutkey(baseUserid.concat("_").concat(key)) != null) {
					Content contentApi = new Content(jsonArray.getJSONObject(i).getString("src"), key, null,null);
					// 设置状态为2，表示key重复
					contentApi.setStatus((byte)2);
					contentApiList.add(contentApi);
					continue;
				}
			}
			String srcInit = jsonArray.getJSONObject(i).getString("src");
			if (StringUtil.isEmptyTrim(srcInit)) {
				throw new IllegalArgumentException("src不能为空或是空串");
			} else {
				src = RegExpValidatorUtil.replaceBlank(srcInit.trim());
				if (src.length() < 7) {
					throw new IllegalArgumentException("json格式参数错误,src参数格式有误");
				}
				if (src.length() > 7&& !"http://".equalsIgnoreCase(src.substring(0, 7))) {
					throw new IllegalArgumentException("每条文件源地址(src)必须包含\"http://\"");
				}
				CoopDomain coopDomain = domainService.selectByUseridAndDomaintag(baseUserid, domaintag);
				if (coopDomain == null) {
					throw new IllegalArgumentException("参数错误:用户标识(userid)和域名标识(domaintag)不匹配");
				}
				String[] srcArr = src.substring(7).split("/", 2);
				if (srcArr.length < 2 || "".equals(srcArr[1])) {
					throw new IllegalArgumentException("文件源地址(src)缺少要查询的资源路径");
				}
				if (srcArr[0].equals(coopDomain.getDomain()) || srcArr[0].equals(coopDomain.getQueryhost())) {
					throw new IllegalArgumentException("每个文件的源地址(src)域名不能和预分发配置中的访问域名相同");
				}
			}
			String md5 = null;
			md5 = jsonArray.getJSONObject(i).getString("md5");
			if (StringUtil.isEmptyTrim(md5)) {
				log.info("传入的json格式串中没有md5----");// 没有md5属性时赋默认值null
			}
			// oldkey = jsonArray.getJSONObject(i).getString("oldkey");
			// oldkey = "".equals(oldkey) ? null : oldkey;
			// if(StringUtil.isEmptyTrim(src)&&StringUtil.isEmptyTrim(oldkey)){
			// throw new IllegalArgumentException("src和oldkey至少传一个");
			// }
			Content contentApi = new Content(src, key, md5,null);
			contentApiList.add(contentApi);
		}
		if (contentApiList.size() > batchSize) {
			throw new IllegalArgumentException("提交任务数不能超过".concat(String.valueOf(batchSize)));
		}
		return contentApiList;
	}

	/**
	 * 解析删除分发文件接口 json items 数组级校验
	 * 
	 * @param data
	 * @param label
	 * @return
	 */
	public List<Content> parseJsonArrayByDel(String data,String auth,String baseUserid) {
		if (StringUtil.isEmptyTrim(auth)) {
			throw new IllegalArgumentException("请求体参数错误!!");
		}
		if (baseUserid == null) {
			throw new IllegalArgumentException("请求头的Authorization 格式设置有误!!");
		}
		List<Content> contentApiList = new ArrayList<Content>();
		JSONObject jsonObject = JSON.parseObject(data);
		String domaintag = jsonObject.getString("domaintag");
		if(StringUtil.isEmptyTrim(domaintag)){
			throw new IllegalArgumentException("domiantag参数错误!!");
		}
		String items = jsonObject.getString("items");
		if(StringUtil.isEmptyTrim(items)){
			throw new IllegalArgumentException("请求头参数格式有误!!");
		}
		JSONArray jsonArray = JSON.parseArray(items);
		String key = null;
		String src = null;
		for (int i = 0; i < jsonArray.size(); i++) {
			key = jsonArray.getJSONObject(i).getString("key");
			src = jsonArray.getJSONObject(i).getString("src");
			if (StringUtil.isEmptyTrim(key) && StringUtil.isEmptyTrim(src)) {
				throw new IllegalArgumentException("传入参数有误，key或src必须传一个!");
			}
			key = "".equals(key) ? null : key;
			src = "".equals(src) ? null : src;
			Content contentApi = new Content(src, null, null, key);
			contentApiList.add(contentApi);
		}
		if (contentApiList.size() > 100) {
			throw new IllegalArgumentException("删除任务数不能超过100");
		}
		return contentApiList;
	}
	
	/**
	 * 刷新文件逻辑处理
	 * @method: ContentIIService  submitFile
	 * @param cip
	 * @param contentApiList
	 * @param userid
	 * @param domaintag
	 * @param tasktag
	 * @return
	 * @throws IOException  JSONObject
	 * @create date： 2015年4月8日
	 */
	public JSONObject submitFile(String cip, List<Content> contentApiList,Integer userid, String domaintag, 
			String tasktag) throws IOException {
		JSONArray jArray = new JSONArray();
		List<Content> submitContents = new ArrayList<Content>();
		List<Content> delContents = new ArrayList<Content>();
		Map<String, List<Content>> contentMap = deleteRepeatList(contentApiList);
    	//没有重复的文件列表
    	List<Content> noRepeatList = contentMap.get("noRepeatList");
		
		CoopDomain cd = this.domainService.selectByUseridAndDomaintag(userid.toString(),domaintag);
		for(Content content : noRepeatList) {
			// key重复
			if(keyExist(content.getKey(),userid)) {
				JSONObject obj = new JSONObject();
				// 设置状态outkey重复  status=5
				obj = resultJsonByRefresh(content.getSrc(), content.getKey(), KEY_REPEAT);
				jArray.add(obj);
				continue;
			}
			// 使用src查询
			List<PreDistTask> tasks = selectExistTasks(content.getSrc());
			if(!tasks.isEmpty()){
				PreDistTask task = tasks.get(0);
				if(PreDistTask.STATUS_SUCCESS == task.getStatus()) {
					delContents.add(content);
				}else if(PreDistTask.STATUS_ING == task.getStatus() || PreDistTask.STATUS_FAILURE_RETRY == task.getStatus()) {
					JSONObject obj = new JSONObject();
					// 设置状态任务重复 status=2
					obj = resultJsonByRefresh(content.getSrc(), content.getKey(), REPEAT_STATUS);
					jArray.add(obj);
				}
			}else {
				// 新增
			    if (cd.getDomain().endsWith("aipai.com")) {
			        String filterDomain = domainExtService.getFilterDomain(cd.getDomaintag());
			        Long version = CDNHelper.getVersion(content.getSrc(), cd.getDomain(), filterDomain, cd.getSetHost());
			        content.setVersion(version);
			    }
				submitContents.add(content);
			}
		}
		if(!delContents.isEmpty()){
			// 设置待删除任务的version
			List<Content> versionedContents = setContentVersion(delContents, cd, System.currentTimeMillis(), jArray, tasktag);
			// 删除设置version成功的任务
			List<Content> deletedContent = deleteExistTask(versionedContents, userid.toString(), domaintag, cd, jArray);
			// 将删除成功的任务加入重新提交的集合中
			submitContents.addAll(deletedContent);
		}
		if(!submitContents.isEmpty()){
			// 批量提交任务
			jArray.addAll(submitContent(submitContents,userid,domaintag,cip,tasktag));
		}
		if(!contentMap.get("repeatList").isEmpty()){
			//重复文件封装返回信息
			JSONObject jsonByRepeat = jsonBySubOnFail(contentMap.get("repeatList"), REPEAT_STATUS);
			JSONArray array = jsonByRepeat.getJSONArray("result");
			jArray.addAll(array);
		}
		JSONObject resultJson = new JSONObject();
		resultJson.put("tasktag", tasktag);
		resultJson.put("result", jArray);
		return resultJson;
	}
	
	/**
	 * 校验key是否存在
	 * <br>
	 * 2015年4月8日
	 * @author gao.jun
	 * @update liuchangfu
	 * @param key
	 * @return
	 */
	private boolean keyExist(String key,Integer userid) {
		String outkey =  String.valueOf(userid).concat("_").concat(key) ;
		PreDistTaskExample pdte = new PreDistTaskExample();
		pdte.createCriteria().andOutkeyEqualTo(outkey);
		List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
		if(pdtList.size()>0){
			return true;
		}
		return false;
	}
    
	/**
	 * 根据src查询已有的预分发任务，理论上仅一个任务
	 * <br>
	 * 2015年4月8日
	 * @author gao.jun
	 * @update liuchangfu
	 * @param key
	 * @param src
	 * @return
	 */
    public List<PreDistTask> selectExistTasks(String src) {
    	PreDistTaskExample pdte = new PreDistTaskExample();
    	pdte.createCriteria().andSrcEqualTo(src)
    	.andStatusIn(Arrays.asList(PreDistTask.STATUS_ING,PreDistTask.STATUS_SUCCESS,PreDistTask.STATUS_FAILURE_RETRY));
    	List<PreDistTask> tasks = pdtm.selectByExample(pdte);
    	return tasks;
    }
    
    /**
     * 删除任务，并清除预取缓存和回源缓存，返回删除成功的content
     * <br>
     * 2015年4月8日
     * @author gao.jun
     * @param task
     * @throws IOException 
     */
    private List<Content> deleteExistTask(List<Content> contentList, String userid, String domaintag, CoopDomain cd, JSONArray resultjArray) {
    	List<Content> successContents = new ArrayList<Content>();
    	List<PreDistTask> failedTasks = new ArrayList<PreDistTask>();
    	for (Content content : contentList) {
    		List<PreDistTask> tasks = this.selectExistTasks(content.getSrc());
    		if(tasks.isEmpty()) {
    			successContents.add(content);
    			continue;
    		}
    		PreDistTask pdt  = tasks.get(0);
    		// 设置删除状态
    		if(!pdts.setTaskStatus(pdt, PreDistTask.STATUS_DELETED)) {
    			failedTasks.add(pdt);
    			continue;
    		}
			String storePath = null;
			storePath = pdt.getStorePath();
			try{
				if (storePath == null) {
					storePath = CDNHelper.getShortPathBySrc(userid, domaintag, pdt.getSrc(), cd.getSubDomain());
				}
				if (StringUtils.hasLength(storePath)) {
					boolean flag = CDNHelper.deleteCdnFile(pdt.getSrc(), storePath, cd.getSubDomain(), domainExtService.getFilterDomain(domaintag));
					/**
					 * 测试模拟 刷新删除失败
					flag= false ;
					 */
					if(!flag) {
						// 还原分发成功状态
						pdts.setTaskStatus(pdt, PreDistTask.STATUS_SUCCESS);
						failedTasks.add(pdt);
						continue;
					}
				} else {
					// 还原分发成功状态
					pdts.setTaskStatus(pdt, PreDistTask.STATUS_SUCCESS);
					failedTasks.add(pdt);
					continue;
				}
			}catch(IOException e){
				// 还原分发成功状态
				pdts.setTaskStatus(pdt, PreDistTask.STATUS_SUCCESS);
				failedTasks.add(pdt);
				continue;
			}
			successContents.add(content);
		}
		// 设置删除失败的Json数据
    	for(PreDistTask task : failedTasks) {
    		JSONObject obj = new JSONObject();
    		obj.put("src", task.getSrc());
    		obj.put("key", task.getOutkey());
    		obj.put("status", FAIL__STATUS);
    		resultjArray.add(obj);
    	}
    	return successContents;
    }
    
    /**
     * 根据src获取文件计算生成version，并设置到content中。返回设置verison成功的content。
     * <br>
     * 通过head请求获取文件大小和修改时间，将时间转换为毫秒，并和文件大小做加和后返回。
     * <br>
     * 2015年4月8日
     * @author gao.jun
     * @param src
     * @return
     */
    private List<Content> setContentVersion(List<Content> contents, CoopDomain cd, long startTime, JSONArray jArray, String tasktag) {
    	List<Content> versionContents = new ArrayList<Content>();
    	String filterDomain = domainExtService.getFilterDomain(cd.getDomaintag());
    	for(Content content : contents) {
    		// 如果获取version总共超过5分钟，则后续的任务全部失败
    		if(System.currentTimeMillis() - startTime > SET_VERSION_TIMEOUT) {
    			JSONObject obj = new JSONObject();
    			obj.put("src", content.getSrc());
    			obj.put("key", content.getKey());
        		obj.put("status", GET_VERSION_TIMEOUT);
        		jArray.add(obj);
    		}else {
    			Long version = CDNHelper.getVersion(content.getSrc(), cd.getDomain(), filterDomain, cd.getSetHost());
    			if(version != null) {
    				content.setVersion(version);
    				// 将重新提交任务的version保存到memcache，对应的key为outkey加速"_version"，超时间为一周
					MemcacheManager.saveToCache(cd.getUserid().concat("_").concat(content.getKey()).concat("_version"), SECONDS_ONE_WEEK, version);
					versionContents.add(content);
    			}else {
    				// 未获取version，则插入一条提交失败（状态为0）的记录
    				pdts.insertWithTasktag(Integer.valueOf(cd.getUserid()), cd.getDomaintag(), content.getSrc(), 
    						content.getKey(), content.getMd5(), tasktag, PreDistTask.TASK_SUB_TYPE, (byte)FAIL__STATUS, false);
    				
    				JSONObject obj = new JSONObject();
        			obj.put("src", content.getSrc());
        			obj.put("key", content.getKey());
            		obj.put("status", GET_VERSION_TIMEOUT);
            		jArray.add(obj);
    			}
    		}
    	}
    	return versionContents;
    }
    
    /**
	 * 去除提交分发元素重复问题
	 * 
	 * @method: ContentIIService  deleteRepeat
	 * @param contentApiList   key(noRepeatList)没有重复的集合 
	 * 						   key(repeatList)重复的集合 
	 * @return  List<Content>
	 * @create date： 2015年4月7日
	 * @2015, by liuchangfu.
	 */
	public Map<String ,List<Content>> deleteRepeatList(List<Content> contentApiList){
		List<Content> contentApiListNoRepeat = new ArrayList<Content>();
		List<Content> contentApiListRepeat = new ArrayList<Content>();
		Map<String ,List<Content>> mapList = new HashMap<String, List<Content>>();
		Set<String> setSrc = new TreeSet<String>();
		for(Content content:contentApiList){
			boolean isNotRepeat = setSrc.add(content.getSrc());
			if(isNotRepeat){
				contentApiListNoRepeat.add(content);
			}else{
				contentApiListRepeat.add(content);
			}
		}
		mapList.put("noRepeatList", contentApiListNoRepeat);
		mapList.put("repeatList", contentApiListRepeat);
		return mapList ;
	}
	
	/**单个文件刷新返回相关信息JSONObject
	 * @method: ContentIIService  resultJsonByRefresh
	 * @param src
	 * @param key
	 * @param status
	 * @return  JSONObject
	 * @create date： 2015年4月8日
	 * @2015, by liuchangfu.
	 */
	public JSONObject resultJsonByRefresh(String src ,String key ,int status){
		JSONObject jobj = new JSONObject();
		jobj.put("key", key);
		jobj.put("src", src);
		jobj.put("status", status);
		return jobj ;
	}
	/**
	 * 解析多文件接口消息体中 json单个属性值
	 * 
	 * @param data
	 * @param label
	 * @return
	 */
	public String parseJson(String data, String label) {
		JSONObject jsonObject = JSON.parseObject(data);
		String labelValue = jsonObject.getString(label);
		return labelValue;
	}
	
	/**
	 * 提交多条预分发任务时返回的json对象信息 list
	 * 
	 * @param contentApiList
	 * @return
	 */
	public JSONObject jsonBySubOnFail(List<Content> contentApiList, int status) {
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		for (Content contentApi : contentApiList) {
			JSONObject jo = new JSONObject();
			jo.put("key", contentApi.getKey());
			jo.put("src", contentApi.getSrc());
			jo.put("status", status);
			jsonArray.add(jo);
		}
		jsonObject.put("result", jsonArray);
		return jsonObject;
	}

	/**
	 * 提交多条预分发失败时返回的json对象信息
	 * 
	 * @param contentApiList
	 * @return
	 */
	public JSONObject jsonBySubOnFailII(List<Content> contentApiList,int status, String tasktag) {
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		for (Content contentApi : contentApiList) {
			JSONObject jo = new JSONObject();
			jo.put("key", contentApi.getKey());
			jo.put("src", contentApi.getSrc());
			jo.put("status", status);
			jsonArray.add(jo);
		}
		jsonObject.put("tasktag", tasktag);
		jsonObject.put("result", jsonArray);
		return jsonObject;
	}
	/**
	 * 删除任务时返回的json 信息 删除失败时keyList 为空
	 * 
	 * @param contentApi
	 * @return
	 */
	public JSONObject produceDelJsonObject(Content contentApi,List<String> keyList) {
		JSONObject jo = new JSONObject();
		String key = contentApi.getOldkey() == null ? "" : contentApi.getOldkey();
		String src = contentApi.getSrc() == null ? "" : contentApi.getSrc();
		jo.put("key", key);
		jo.put("src", src);
		jo.put("deletedKey", keyList);
		return jo;
	}
	/**
	 * 获取请求的contentType
	 * 
	 * @param contentType
	 * @param data
	 * @return
	 */
	public String getContentType(String contentType, String data) {
		if (contentType != null && contentType.contains(CONTENT_TYPE_JSON)) {
			if (!StringUtil.isJson(data)) {
				throw new IllegalArgumentException("请求的json格式数据错误!");
			}
			return "json";
		} else if (contentType != null
				&& contentType.contains(CONTENT_TYPE_XML)) {
			if (!StringUtil.isXML(data)) { // 处理xml格式
				throw new IllegalArgumentException("请求的xml格式参数错误!");
			}
			return "xml";
		} else {
			return null;
		}
	}
	
	/**
	 * 提交任务，用于异步的自动提交
	 * <br>
	 * 2015年4月28日
	 * @author gao.jun
	 * @since v0.3
	 */
	public void submitTask() {
		PreDistTaskExample example = new PreDistTaskExample();
		example.setLimitValue1(0);
		// 一次最多提交100个任务
		example.setLimitValue2(100);
		// 按创建时间排序
		example.setOrderByClause("create_time");
		// 查询待提交状态的任务
		example.createCriteria().andStatusEqualTo((byte)7);
		List<PreDistTask> feedbackTasks = pdtm.selectByExample(example);
		if(feedbackTasks != null && !feedbackTasks.isEmpty()) {
			// 域名缓存，减少查询数据库次数
			Map<String,CoopDomain> domainCache = new HashMap<String,CoopDomain>();
			// 待提交任务集合
			Map<String,List<PreDistTask>> submitTasks = new HashMap<String,List<PreDistTask>>();
			// 待更新状态的任务，key为状态码，value为outkey集合
			Map<Byte,List<String>> statusTasks = new HashMap<Byte,List<String>>();
			for(PreDistTask task : feedbackTasks) {
				// 缓存域名
				if(!domainCache.containsKey(task.getDomaintag())) {
					domainCache.put(task.getDomaintag(), domainService.selectByUseridAndDomaintag(task.getUserid().toString(), task.getDomaintag()));
				}
				if(org.apache.commons.lang.StringUtils.isNotBlank(task.getAppkey())) {
					// 根据域名进行任务分组
					if(!submitTasks.containsKey(task.getDomaintag())) {
						submitTasks.put(task.getDomaintag(), new ArrayList<PreDistTask>());
					}
					submitTasks.get(task.getDomaintag()).add(task);
				}
			}
			// 提交任务，并设置状态
			setSubmitStatus(domainCache, submitTasks, statusTasks);
			// 更新任务状态
			for(Map.Entry<Byte,List<String>> entry : statusTasks.entrySet()) {
				PreDistTaskExample pdte = new PreDistTaskExample();
				// 只更新状态还是待提交的任务，应对更新之前任务已经回调的情况
				pdte.createCriteria().andOutkeyIn(entry.getValue()).andStatusEqualTo(PreDistTask.STATUS_PRE_SUBMIT);
				PreDistTask rec = new PreDistTask();
				rec.setStatus(entry.getKey());
				pdtm.updateByExampleSelective(rec, pdte);
			}
		}
	}
	
	/**
	 * 调用cdn接口提交任务，并设置任务status
	 * <br>
	 * 2015年4月28日
	 * @author gao.jun
	 * @param domainCache
	 * @param submitTasks
	 * @param statusTasks
	 * @since v0.3
	 */
	private void setSubmitStatus(Map<String, CoopDomain> domainCache,
			Map<String, List<PreDistTask>> submitTasks,
			Map<Byte,List<String>> statusTasks) {
		String address = null;
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			log.error("获取主机IP失败", e1);
			return;
		}
		for(Map.Entry<String,List<PreDistTask>> entry : submitTasks.entrySet()) {
			String domaintag = entry.getKey();
			List<PreDistTask> tasks = entry.getValue();
			CoopDomain coopDomain = domainCache.get(domaintag);
			if(!tasks.isEmpty()) {
				try {
					String result = pdts.submitTask(tasks, coopDomain.getSubDomain(), address, coopDomain.getSetHost());
					// 需要再次提交的任务
					List<String> reSubmitTaskKeys = new ArrayList<String>();
					if(org.apache.commons.lang.StringUtils.isNotBlank(result)) {
						JSONArray jsonArray = JSONObject.parseArray(result);
						for (int i = 0,length = jsonArray.size(); i < length; i++) {
							JSONObject jsonObj = jsonArray.getJSONObject(i);
							// cdn返回状态
							int statusCode = jsonObj.getIntValue("status");
							String resultKey = jsonObj.getString("itemid");
							// 重复
							if (statusCode == SUB_STATUS_REPEAT) {
								String src = jsonObj.getString("source").replace("\\/", "/");
								String storePath = CDNHelper.getShortPathBySrc(resultKey.split("_")[0],domaintag, 
										src, coopDomain.getSubDomain());
								// 短地址不存在，则为重复
								if(storePath == null) {
									addStatusTaskKey(statusTasks, PreDistTask.STATUS_SRC_REPEAT, resultKey);
								}else {
									// 删除CDN文件成功，加入重新提交集合
									if(CDNHelper.deleteCdnFile(src, storePath, coopDomain.getSubDomain(), domainExtService.getFilterDomain(domaintag))) {
										reSubmitTaskKeys.add(resultKey);
									}else {// 删除失败，则任务失败
										addStatusTaskKey(statusTasks, PreDistTask.STATUS_FAILURE, resultKey);
									}
								}
							} else if(statusCode == SUB_STATUS_SUCCESS){ // 成功
								String storePath = jsonObj.getString("strorepath");
								// 更新storepath
								pdts.updateStorePathByOutkey(resultKey, storePath);
								addStatusTaskKey(statusTasks, PreDistTask.STATUS_ING, resultKey);
							}else { // 403，失败
								addStatusTaskKey(statusTasks, PreDistTask.STATUS_FAILURE, resultKey);
							}
						}
					}
					if(!reSubmitTaskKeys.isEmpty()) {
						List<PreDistTask> reSubmitTasks = new ArrayList<PreDistTask>();
						String filterDomain = domainExtService.getFilterDomain(domaintag);
						for(String outkey : reSubmitTaskKeys) {
							for(PreDistTask task : tasks) {
								if(outkey.equals(task.getOutkey())) {
									// 获取version
									Long version = CDNHelper.getVersion(task.getSrc(), coopDomain.getDomain(), filterDomain, coopDomain.getSetHost());
									if(version != null) {
										task.setVersion(version);
										reSubmitTasks.add(task);
										// 将重新提交任务的version保存到memcache，对应的key为outkey加"_version"
										MemcacheManager.saveToCache(task.getOutkey().concat("_version"), SECONDS_ONE_WEEK, version);
									}else {
										log.error("提交任务时，获取version失败，src：{}，domain：{}", task.getSrc(), coopDomain.getDomain());
										// 加入失败
										addStatusTaskKey(statusTasks, PreDistTask.STATUS_FAILURE, task.getOutkey());
									}
									break;
								}
							}
						}
						String reSubmitResult = pdts.submitTask(reSubmitTasks, coopDomain.getSubDomain(), address, coopDomain.getSetHost());
						if(org.apache.commons.lang.StringUtils.isNotBlank(reSubmitResult)) {
							JSONArray jsonArray = JSONObject.parseArray(reSubmitResult);
							for (int i = 0,length = jsonArray.size(); i < length; i++) {
								JSONObject jsonObj = jsonArray.getJSONObject(i);
								int statusCode = jsonObj.getIntValue("status");
								String resultKey = jsonObj.getString("itemid");
								if(statusCode == SUB_STATUS_SUCCESS){ // 成功
									String storePath = jsonObj.getString("strorepath");
									// 更新storepath
									pdts.updateStorePathByOutkey(resultKey, storePath);
									addStatusTaskKey(statusTasks, PreDistTask.STATUS_ING, resultKey);
								}else { // 失败
									addStatusTaskKey(statusTasks, PreDistTask.STATUS_FAILURE, resultKey);
								}
							}
						}
					}
				} catch (Exception e) {
					log.error("提交任务时，设置任务状态失败", e);
				}
			}
		}
	}
	
	/**
	 * 将key加入对应状态的集合
	 * @param statusTasks
	 * @param status
	 * @param outkey
	 */
	private void addStatusTaskKey(Map<Byte,List<String>> statusTasks, Byte status, String outkey) {
		if(!statusTasks.containsKey(status)) {
			statusTasks.put(status, new ArrayList<String>());
		}
		statusTasks.get(status).add(outkey);
	}
	
	/**
	 * 删除分发文件（处理成功或失败的返回信息）
	 * 
	 * @param userid
	 * @param contentApiList
	 * @param domaintag
	 * @return
	 * @throws IOException
	 */
	public Map<String,Object> delFileAPi(Integer userid, List<Content> contentApiList,String domaintag) throws IOException {
		Map<String,Object> resultMap = new HashMap<String,Object>();
		JSONArray jsonArray = new JSONArray();
		List<String> delSrcs = new ArrayList<String>();
		List<String> delKeys = new ArrayList<String>();
		//List<Content> notFoundContents = new ArrayList<Content>();
		for (Content contentApi : contentApiList) {
			if(org.apache.commons.lang.StringUtils.isNotBlank(contentApi.getSrc())) {
				delSrcs.add(contentApi.getSrc());
			}else {
				delKeys.add(contentApi.getOldkey());
			}
			try {
				JSONObject jsonResult = this.deleteFile(userid,domaintag, contentApi); // 正常时返回
				jsonArray.add(jsonResult);
			} catch (OpenapiFailException e) {
				log.info(e.getMessage());
				JSONObject jsonByFail = this.produceDelJsonObject(contentApi, new ArrayList<String>()); // 失败时返回
				jsonArray.add(jsonByFail);
			} catch (PreDistTaskNotFoundException e) {
				log.info("未找到要删除的预分发任务，key：{}，src：{}", contentApi.getKey(), contentApi.getSrc());
				JSONObject jsonByFail = this.produceDelJsonObject(contentApi, new ArrayList<String>()); // 失败时返回
				jsonArray.add(jsonByFail);
				//notFoundContents.add(contentApi);
			}
		}
		// 对于删除失败的文件，进行回源缓存文件的刷新  by gao.jun
		JSONObject linksJsonObj = null;
		ApiDomain apiDomain = apiDomainService.selectByUseridAndDomaintag(userid.toString(), domaintag);
		if(apiDomain != null && (!delSrcs.isEmpty() || !delKeys.isEmpty())) {
			// 通过key获取src
			if(!delKeys.isEmpty()) {
				for(String key : delKeys) {
					PreDistTask taksk = pdts.selectByOutkey(userid, key);
					if(taksk != null) {
						String src = taksk.getSrc();
						if(!delSrcs.contains(src)){
							delSrcs.add(src);
						}
					}
				}
			}
			// 将文件路径中的域名转化为访问域名
			List<String> domainSrcs = new ArrayList<String>();
			Pattern p = Pattern.compile("http://[^/]*/(.*)");
			Matcher m = null;
			for(String src : delSrcs) {
				m = p.matcher(src);
				if(m.matches()) {
					domainSrcs.add("http://".concat(apiDomain.getDomain()).concat("/").concat(m.group(1)));
				}
				m.reset();
			}
			// 调用内容刷新，清除缓存
			if(!domainSrcs.isEmpty()) {
				JSONObject obj = new JSONObject();
				obj.put("domaintag", domaintag);
				obj.put("srcs", domainSrcs.toArray(new String[]{}));
				linksJsonObj = cachedFileService.processCachedFile(obj.toJSONString(), new ProcessInfo(CachedFileController.DELETE_CACHE_FILE_URI, 
						"CDN file refresh interface", "refresh or delete file in preloadedfile"));
			}
		}
		resultMap.put("deleteContents", jsonArray);
		if(linksJsonObj != null) {
			resultMap.put("links", linksJsonObj.get("links"));
		}
//		// 未查询到的分发任务
//		if(!notFoundContents.isEmpty()) {
//			resultMap.put("notFoundContents", notFoundContents);
//		}
		return resultMap;
	}
	
	/**
	 * 通过content集合删除任务，并通过status标识处理状态
	 * @author gao.jun
	 * @since v0.3
	 */
	public JSONObject delFileByContents(Integer userid, List<Content> contentApiList,String domaintag) {
		Set<String> srcs = new HashSet<String>();
		Pattern p = Pattern.compile("http://[^/]*/(.*)");
		Matcher m = null;
		ApiDomain apiDomain = apiDomainService.selectByUseridAndDomaintag(userid.toString(), domaintag);
		JSONArray jsonArray = new JSONArray();
		for (Content contentApi : contentApiList) {
			String src = contentApi.getSrc();
			if(org.apache.commons.lang.StringUtils.isBlank(contentApi.getSrc()) 
					&& org.apache.commons.lang.StringUtils.isNotBlank(contentApi.getOldkey())) {
				PreDistTask taksk = pdts.selectByOutkey(userid, contentApi.getOldkey());
				if(taksk != null) {
					src = taksk.getSrc();
				}
			}
			if(org.apache.commons.lang.StringUtils.isNotBlank(src)) {
				m = p.matcher(src);
				if(m.matches()) {
					srcs.add("http://".concat(apiDomain.getDomain()).concat("/").concat(m.group(1)));
				}
				m.reset();
			}
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("src", src);
			jsonObj.put("key", contentApi.getOldkey());
			try {
				// 正常时返回
				this.deleteFile(userid,domaintag, contentApi); 
				jsonObj.put("status", 1);
			} catch (PreDistTaskNotFoundException e) {
				log.error("未找到要删除的任务，src：{}，oldkey：{}", src, contentApi.getOldkey());
				jsonObj.put("status", 2);
			} catch (Exception e) {
				log.error("删除任务失败，src：{}，oldkey：{}", src, contentApi.getOldkey());
				jsonObj.put("status", 0);
			}
			jsonArray.add(jsonObj);
		}
		JSONObject linksJsonObj = null;
		// 调用内容刷新，清除缓存
		if(!srcs.isEmpty()) {
			JSONObject obj = new JSONObject();
			obj.put("domaintag", domaintag);
			obj.put("srcs", srcs.toArray(new String[]{}));
			linksJsonObj = cachedFileService.processCachedFile(obj.toJSONString(), new ProcessInfo(CachedFileController.DELETE_CACHE_FILE_URI, 
					"CDN file refresh interface", "refresh or delete file in preloadedfile"));
		}
		JSONObject jsonObj = new JSONObject();
		if(linksJsonObj != null) {
			jsonObj.put("links", linksJsonObj.get("links"));
		}
		jsonObj.put("result", jsonArray);
		return jsonObj;
	}
}
