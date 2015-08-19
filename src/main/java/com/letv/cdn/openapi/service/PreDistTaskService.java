package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.dao.openapi.PreDistParamMapper;
import com.letv.cdn.openapi.dao.openapi.PreDistTaskMapper;
import com.letv.cdn.openapi.pojo.PreDistParam;
import com.letv.cdn.openapi.pojo.PreDistParamExample;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.pojo.PreDistTaskExample.Criteria;
import com.letv.cdn.openapi.pojo.contentterms.Content;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.utils.CDNHelper;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.web.HttpClientUtil;
import com.letv.cdn.openapiauth.utils.LetvApiHelper;

/**
 * <br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年11月20日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
@Service
public class PreDistTaskService {

    private static final Logger log = LoggerFactory.getLogger(PreDistTaskService.class);
    
    /** 提交预分发文件的接口 */
	private static final String SUBMIT_FILE_URI = Env.get("submit_file_uri");
	
    @Resource
    PreDistTaskMapper pdtm;
    
    @Resource
	PreDistParamMapper preDistParamMapper;
    
    @Resource
    DomainExtensionService domainExtService;
    
    /**
     * 插入<br/>
     * <b>Method</b>: PreDistTaskService#insert <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param userid
     * @param domaintag
     * @param src
     * @param outkey
     * @param md5
     * @return boolean
     */
    public boolean insert(Integer userid, String domaintag, String src,String outkey, String md5,String type) {
		PreDistTask pdt = new PreDistTask();
		pdt.setCreateTime(System.currentTimeMillis());
		pdt.setDomaintag(domaintag);
		pdt.setMd5(md5);
		pdt.setOutkey(userid + "_" + outkey);
		pdt.setSrc(src);
		pdt.setStatus(PreDistTask.STATUS_ING);
		pdt.setUserid(userid);
		pdt.setTaskType(type);
		// 从request中获取appkey属性
		String appkey = ApiHelper.getAppkey();
		pdt.setAppkey(appkey);

		return this.pdtm.insert(pdt) == 1;
    }

    /**
     * 回调<br/>
     * <b>Method</b>: PreDistTaskService#callback <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param outkey
     * @param callbackCode cdn回调的状态
     * @param taskStatus 任务的状态
     * @param size
     * @param storePath
     * @param md5
     * @param callbacked 回调客户的状态，如果为0标识未回调或未回调成功
     * @return Byte
     */
    public Byte callback(PreDistTask pdt, String callbackCode, Byte taskStatus, Long size,String storePath, String md5, byte callbacked) {
		long currTime = System.currentTimeMillis();
//		PreDistTask pdt = selectByOutkey(outkey);
		if (pdt == null) {
			return null;
		}
		if (pdt.getCallbackCode() != null
				&& pdt.getCallbackCode().equals(
						PreDistTask.CALLBACK_CODE_SUCCESS)) {
			log.info("任务已成功，不再接收回调");
			return PreDistTask.STATUS_SUCCESS;
		}
		pdt.setCallbackTime(currTime);
		pdt.setCallbackCode(callbackCode);
		pdt.setSize(size);
		pdt.setStorePath(storePath);
		if (StringUtils.hasLength(md5)) {
			pdt.setMd5(md5);
		}
		if(taskStatus != null) {
			pdt.setStatus(taskStatus);
		}
		if(callbacked != 0) {
			pdt.setClientCallbacked(callbacked);
			pdt.setClientCallbackTime(new Date());
		}
		if (this.pdtm.updateByPrimaryKeySelective(pdt) == 0) {
			return null;
		}
		return pdt.getStatus();
    }

    public static void main(String[] args) throws ParseException {
		// Mon, 16 Jul 2007 22:23:00 GMT
		// EEE, dd MMM yyyy HH:mm:ss GMT
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String s = sdf.format(new Date());
		// TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		// Date date = new Date();
		// System.out.println(date);
		System.out.println(s);

		Date d = sdf.parse("Tue, 25 Nov 2014 06:43:39 GMT");
		System.out.println(d.toString());
    }

    /**
     * 状态置为删除<br/>
     * <b>Method</b>: PreDistTaskService#deleteTask <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param userid
     * @param src
     * @param key
     * @param md5
     * @return boolean
     */
    public boolean deleteTask(Integer userid, String src, String key, String md5) {
		PreDistTask pdt = this.selectByOutkeyMd5Src(userid, key, md5, src);
		pdt.setStatus(PreDistTask.STATUS_DELETED);
		pdt.setDeleteTime(System.currentTimeMillis());
		return this.pdtm.updateByPrimaryKey(pdt) > 0;
    }

    /**
     * 根据outkey查询<br/>
     * <b>Method</b>: PreDistTaskService#selectByOutkey <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param userid
     * @param key
     * @return PreDistTask
     */
    public PreDistTask selectByOutkey(Integer userid, String key) {
		if (!StringUtils.hasLength(key)) {
			return null;
		}
		return this.selectByOutkey(userid + "_" + key);
    }

    /**
     * 根据outkey查询<br/>
     * <b>Method</b>: PreDistTaskService#selectByOutkey <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param outkey
     * @return PreDistTask
     */
    public PreDistTask selectByOutkey(String outkey) {
		if (!StringUtils.hasLength(outkey)) {
			return null;
		}
		PreDistTaskExample pdte = new PreDistTaskExample();
		pdte.createCriteria().andOutkeyEqualTo(outkey);
		List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
		if (pdtList.size() > 0) {
			return pdtList.get(0);
		} else {
			return null;
		}
    }

    /**
     * 根据md5查询<br/>
     * <b>Method</b>: PreDistTaskService#selectByMd5 <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param userid
     * @param md5
     * @return PreDistTask
     */
    public PreDistTask selectByMd5(Integer userid, String md5) {
		if (!StringUtils.hasLength(md5)) {
			return null;
		}
		PreDistTaskExample pdte = new PreDistTaskExample();
		pdte.createCriteria().andUseridEqualTo(userid).andMd5EqualTo(md5);
		List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
		if (pdtList.size() > 0) {
			return pdtList.get(0);
		} else {
			return null;
		}
    }

    /**
     * 按outkye、md5、src的顺序查询，直到查到为止<br/>
     * <b>Method</b>: PreDistTaskService#selectByOutkeyMd5Src <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param userid
     * @param key
     * @param md5
     * @param src
     * @return PreDistTask
     */
    public PreDistTask selectByOutkeyMd5Src(Integer userid, String key,String md5, String src) {
		PreDistTask pdt = this.selectByOutkey(userid, key);
		if (pdt == null && StringUtils.hasLength(md5)) {
			pdt = this.selectByMd5(userid, md5);
		}
		if (pdt == null) {
			pdt = this.selectBySrc(src);
		}
		return pdt;
    }

    /**
     * 按src查询<br/>
     * <b>Method</b>: PreDistTaskService#selectBySrc <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param src
     * @return PreDistTask
     */
    public PreDistTask selectBySrc(String src) {
		if (!StringUtils.hasLength(src)) {
			return null;
		}
		PreDistTaskExample pdte = new PreDistTaskExample();
		pdte.createCriteria().andSrcEqualTo(src);
		List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
		if (pdtList.size() > 0) {
			return pdtList.get(0);
		} else {
			return null;
		}
    }

    /**
     * 物理删除数据<br/>
     * <b>Method</b>: PreDistTaskService#deleteDate <br/>
     * <b>Create Date</b> : 2014年11月21日
     * 
     * @author Chen Hao
     * @param userid
     * @param key
     * @return boolean
     */
    public boolean deleteDate(Integer userid, String key) {
		PreDistTask pdt = this.selectByOutkey(userid, key);
		return this.pdtm.deleteByPrimaryKey(pdt.getId()) > 0;
    }

    /**
     * 提交分发文件时 cdn接口 返回重复或失败 删除默认记录(优先key)
     * 
     * @author liuchangfu
     * @param userid
     * @param key
     * @param src
     */
    public void deleteRepeat(Integer userid, String key, String src) {

		PreDistTask distTask = this.selectByOutkey(userid, key);
		if (distTask != null) {
			this.pdtm.deleteByPrimaryKey(distTask.getId());
		} else { // 按src删除 增加条件status = 2
			if (StringUtils.hasLength(src)) {
				PreDistTaskExample pdte = new PreDistTaskExample();
				pdte.createCriteria().andSrcEqualTo(src).andStatusEqualTo(PreDistTask.STATUS_ING);
				List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
				if (pdtList.size() > 0) {
					for (PreDistTask pds : pdtList) {
						this.pdtm.deleteByPrimaryKey(pds.getId());
					}
				}
			}
		}
    }

    /**
     * 删除分发文件接口时调用 按src查询 删除时的查询只针对status=1并且useid为当前用户
     * 
     * @author liuchangfu
     * @param src
     * @return List<PreDistTask>
     */
    public List<PreDistTask> selectBySrcOnDel(String src, Integer userid) {
		if (!StringUtils.hasLength(src)) {
			return null;
		}
		PreDistTaskExample pdte = new PreDistTaskExample();
		pdte.createCriteria().andSrcEqualTo(src).andStatusEqualTo(PreDistTask.STATUS_SUCCESS).andUseridEqualTo(userid);
		List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
		return pdtList ;
    }

    /**
     * 根据key和src进行查找(优先key)<br>
     * 只删除分发成功的任务 update by gao.jun
     * @author liuchangfu
     * @param userid
     * @param key
     * @param md5
     * @param src
     * @return List<PreDistTask>
     */
    public List<PreDistTask> selectByOutkeySrc(Integer userid, String key,String src) {
//		PreDistTask pdt = this.selectByOutkey(userid, key);
		PreDistTaskExample pdte = new PreDistTaskExample();
		pdte.createCriteria().andStatusEqualTo(PreDistTask.STATUS_SUCCESS).andOutkeyEqualTo(userid + "_" + key);
		List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
		List<PreDistTask> preDistTaskList = new ArrayList<PreDistTask>();
		if (pdtList == null || pdtList.isEmpty()) {
			List<PreDistTask> list = this.selectBySrcOnDel(src, userid);
			preDistTaskList.addAll(list);
		} else {
			preDistTaskList.add(pdtList.get(0));
		}
		return preDistTaskList;
    }

    /**
     * 状态置为删除<br/>
     * 
     * @author liuchangfu
     * @param userid
     * @param src
     * @param key
     * @param md5
     * 
     */
    public void delTask(Integer userid, String key, String src) {
		List<PreDistTask> pdtList = selectByOutkeySrc(userid, key, src);
		if (pdtList.size() > 0) {
			for (PreDistTask pdt : pdtList) {
				pdt.setStatus(PreDistTask.STATUS_DELETED);
				pdt.setDeleteTime(System.currentTimeMillis());
				this.pdtm.updateByPrimaryKey(pdt);
			}
		}
    }
    
    /**
     * 
     * 按条件查询记录条数
     * @method: PreDistTaskService  selectTotalSize
     * @param src
     * @param status
     * @param tasktag
     * @param tagList
     * @param userid
     * @return  int
     * @create date： 2015年1月20日
     * @2015, by liuchangfu.
     */
    public int selectTotalSize(String src, Integer status, String tasktag,List<String> tagList, Integer userid) {
		PreDistTaskExample pdte = new PreDistTaskExample();
		Criteria criteria = pdte.createCriteria();
		if (StringUtil.notEmpty(tasktag)) {
			criteria.andTasktagEqualTo(tasktag);
		}
		if (StringUtil.notEmpty(src)) {
			criteria.andSrcEqualTo(src.trim());
		}
		if (status != null) {
			int statusCode = status;
			criteria.andStatusEqualTo((byte) statusCode);
		}
		criteria.andUseridEqualTo(userid).andDomaintagIn(tagList);
		return this.pdtm.countByExample(pdte);
    }
    
    
    /**
     * 多参数分页查询
     * 
     * @param src
     * @param status
     * @param page
     * @param rows
     * @param tasktag
     * @param tagList
     * @param userid
     * @return
     */
    public List<PreDistTask> selectBySrcAndstatus(String src, Integer status,Integer page, Integer rows, String tasktag, List<String> tagList,Integer userid) {
		PreDistTaskExample pdte = new PreDistTaskExample();
		Criteria criteria = pdte.createCriteria();
		if (StringUtil.notEmpty(tasktag)) {
			criteria.andTasktagEqualTo(tasktag);
		}
		if (StringUtil.notEmpty(src)) {
			criteria.andSrcEqualTo(src.trim());
		}
		if (status != null) {
			int statusCode = status;
			criteria.andStatusEqualTo((byte) statusCode);
		}
		criteria.andUseridEqualTo(userid).andDomaintagIn(tagList);
		pdte.setOrderByClause("create_time");
		if (page != null && rows != null) {
			int begin = (page - 1) * rows;
			int end = rows;
			// pdte.setLimitValue1(begin);
			// pdte.setLimitValue2(end);
			pdte.setBegin(begin);
			pdte.setTotalSize(end);
		}
		return this.pdtm.selectBySrcAndStatusPagination(pdte);
    }

    /**
     * 更改提交任务状态为重复提交
     * 
     * @param userid
     * @param key
     * @param src
     */
    public void updateRepeat(Integer userid, String key, String src,String tasktag) {
		PreDistTask distTask = this.selectByOutkey(userid, key);
		if (distTask != null) {
			distTask.setStatus(PreDistTask.TASK_REPEAT);
			this.pdtm.updateByPrimaryKeySelective(distTask);
		} else { // 按src 更新
			if (StringUtils.hasLength(src)) {
				PreDistTaskExample pdte = new PreDistTaskExample();
				pdte.createCriteria().andSrcEqualTo(src).andTasktagEqualTo(tasktag);// TODO 本次任务tag
				List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
				if (pdtList.size() > 0) {
					for (PreDistTask pds : pdtList) {
						pds.setStatus(PreDistTask.TASK_REPEAT);
						this.pdtm.updateByPrimaryKeySelective(distTask);
					}
				}
			}
		}
    }

    /**
     * 更新提交任务状态为提交失败
     * 
     * @param userid
     * @param key
     * @param src
     */
    public void updateSubFail(Integer userid, String key, String src,String tasktag) {
		PreDistTask distTask = this.selectByOutkey(userid, key);
		if (distTask != null) {
			distTask.setStatus(PreDistTask.SUB_FAIL);
			this.pdtm.updateByPrimaryKeySelective(distTask);
		} else { // 按src 更新
			if (StringUtils.hasLength(src)) {
				PreDistTaskExample pdte = new PreDistTaskExample();
				pdte.createCriteria().andSrcEqualTo(src).andTasktagEqualTo(tasktag);
				List<PreDistTask> pdtList = this.pdtm.selectByExample(pdte);
				if (pdtList.size() > 0) {
					for (PreDistTask pds : pdtList) {
						pds.setStatus(PreDistTask.SUB_FAIL);
						this.pdtm.updateByPrimaryKeySelective(distTask);
					}
				}
			}
		}

    }

    /**
     * 插入默认数据 ver 0.3
     * 
     * @param userid
     * @param domaintag
     * @param src
     * @param outkey
     * @param md5
     * @param tasktag
     * @param presubmit 是否为异步提交
     * @return
     */
    public boolean insertWithTasktag(Integer userid, String domaintag,String src, String outkey, String md5, String tasktag,
    		String type, boolean presubmit) {
    	return insertWithTasktag(userid, domaintag, src, outkey, md5, tasktag, type, presubmit ? PreDistTask.STATUS_PRE_SUBMIT : PreDistTask.STATUS_ING, presubmit);
	}
    
    public boolean insertWithTasktag(Integer userid, String domaintag,String src, String outkey, String md5, String tasktag,
    		String type, byte status, boolean presubmit) {
		PreDistTask pdt = new PreDistTask();
		pdt.setCreateTime(System.currentTimeMillis());
		pdt.setDomaintag(domaintag);
		pdt.setMd5(md5);
		pdt.setOutkey(userid + "_" + outkey);
		pdt.setSrc(src);
		pdt.setStatus(status);
		pdt.setUserid(userid);
		pdt.setTasktag(tasktag);
		pdt.setTaskType(type);
		// 从request中获取appkey属性
		String appkey = ApiHelper.getAppkey();
		pdt.setAppkey(appkey);
		return this.pdtm.insert(pdt) == 1;
	}
    
    /**
     * 设置任务的状态
     * <br>
     * 2015年4月9日
     * @author gao.jun
     * @param pdt 任务对象，id必须存在
     * @param status
     * @return
     */
    public boolean setTaskStatus(PreDistTask pdt, byte status) {
    	pdt.setStatus(status);
    	return this.pdtm.updateByPrimaryKey(pdt) == 1 ? true : false;
	}
    
    /**
     * 批量提交PreDistTask集合中的任务
     * @param tasks
     * @param domain 访问域名
     * @param address 请求IP
     * @return
     * @throws IOException
     */
    public String submitTask(List<PreDistTask> tasks, String domain, String address, String setHost) throws IOException {
    	String result = null;
    	if(!tasks.isEmpty()) {
    		PreDistTask task = tasks.get(0);
    		String userid = task.getUserid().toString();
    		Map<String, String> params = this.subParams(userid, address, domain, tasks.get(0).getAppkey());
    		String xmlData = generateSubmitXml(tasks, task.getDomaintag(), domain, setHost);
    		try {
    			//[ { "user": "acloud", "itemid": "137587_acloudtest1416309910832", "source": "http:\/\/dlsw.baidu.com\/sw-search-sp\/soft\/cc\/13478\/npp.6.6.9.Installer.1410249599.exe", "filemd5": "", "domain": "v.letvcloud.com", "strorepath": "105\/44\/54\/acloud\/137587\/sw-search-sp\/soft\/cc\/13478\/npp.6.6.9.Installer.1410249599.exe", "staus": 1 } ]
    			result = HttpClientUtil.postXml(SUBMIT_FILE_URI, params, xmlData, HttpClientUtil.UTF_8);
    			// 服务器内部异常
    			if (result == null) {
    				log.error("服务器内部异常");
    			}
    			// 调用接口的IP不在白名单内
    			if ("403".equals(result)) {
    				log.info("调用接口的IP不在白名单内");
    			}
    		} catch (IOException e) {
    			log.error("提交任务失败", e);
    			throw e;
    		}
    	}
		return result;
	}
    
    /**
     * 生成PreDistTask集合对应的提交消息体
     * @param tasks
     * @return
     * @since v0.3
     */
	public String generateSubmitXml(List<PreDistTask> tasks, String domaintag, String domain, String setHost) {
		if(tasks == null || tasks.isEmpty()) {
			return null;
		}
		List<Content> contents = new ArrayList<Content>();
		for(PreDistTask task : tasks) {
			String filterDomain = domainExtService.getFilterDomain(domaintag);
			String newSrc = task.getSrc();
			if(org.apache.commons.lang.StringUtils.isNotBlank(filterDomain)) {
				// 转换src，转换为filterDomain
				String src = task.getSrc();
				String oldSrcDomain = org.apache.commons.lang.StringUtils.substringBetween(src, "http://", "/");
				newSrc = "http://".concat(filterDomain).concat(org.apache.commons.lang.StringUtils.substringAfter(src, oldSrcDomain))
				        .concat("?lersrc=").concat(LetvApiHelper.encodeBase64(oldSrcDomain))
						.concat("&cuhost=").concat(LetvApiHelper.encodeBase64(domain));
				if(org.apache.commons.lang.StringUtils.isNotBlank(setHost)) {
					newSrc.concat("&sthost=").concat(LetvApiHelper.encodeBase64(setHost));
				}
			}
			Content c = new Content(newSrc, org.apache.commons.lang.StringUtils.substringAfter(task.getOutkey(), "_"), task.getMd5(), null);
			if(task.getVersion() != null) {
				c.setVersion(task.getVersion());
			}
			contents.add(c);
		}
		return CDNHelper.generateSubmitXml(contents, tasks.get(0).getUserid().toString());
	}
	
	/**
	 * 生成提交任务的请求参数，从httpRequest中获取appkey
	 * @param userid 用户id
	 * @param cip 请求ip
	 * @param domain 访问域名
	 * @return
	 */
	public Map<String, String> subParams(String userid, String cip, String domain) {
		return subParams(userid, cip, domain, ApiHelper.getAppkey());
	}
	
	/**
	 * 生成提交任务的请求参数
	 * <br>
	 * 2015年4月29日
	 * @author gao.jun
	 * @param userid 用户id
	 * @param cip 请求ip
	 * @param domain 访问域名
	 * @param appkey
	 * @return
	 */
	public Map<String, String> subParams(String userid, String cip, String domain, String appkey) {
		return this.subParams(userid, cip, domain, appkey, null);
	}
	
	public Map<String, String> subParams(String userid, String cip, String domain, String appkey, Integer priority) {
	    // 调用文件分发系统的接口
	    // prop: 0正常，9全网分发
	    // bussid：用户ID,userid
	    // priority：分发优先级0--100
	    // gslbdomain:用户访问CDN的域名
	    // cip：用户提交分发任务的IP
	    // user：acloud,固定给第三方分发通用名
	    PreDistParamExample example = new PreDistParamExample();
	    PreDistParamExample.Criteria c = example.createCriteria();
	    c.andAppkeyEqualTo(appkey);
	    List<PreDistParam> paramList = preDistParamMapper.selectByExample(example);
	    // 默认分发比例9
	    String prop = "9";
	    if(!paramList.isEmpty()) {
	        PreDistParam param = paramList.get(0);
	        prop = param.getProp();
	        priority = param.getPriority();
	    }
	    // 默认分发优先级50
        if (priority == null) {
            priority = 50;
        }
	    Map<String, String> params = new HashMap<String, String>();
	    params.put("prop", prop);
	    params.put("bussid", userid);
	    params.put("priority", String.valueOf(priority));
	    params.put("gslbdomain", domain);
	    params.put("cip", cip);
	    params.put("user", "acloud");
	    return params;
	}
	
	/**
	 * 根据src判断任务是否重复
	 * <br>
	 * 2015年4月29日
	 * @author gao.jun
	 * @param src
	 * @return 当存在分发中或分发失败重试中的任务，表示重复；当有超过2条“待提交”的任务，表示重复
	 */
	public boolean checkSrcRepeated(String src) {
		PreDistTaskExample pdte = new PreDistTaskExample();
    	pdte.createCriteria().andSrcEqualTo(src).andStatusIn(Arrays.asList(PreDistTask.STATUS_ING,PreDistTask.STATUS_FAILURE_RETRY));
    	if(pdtm.countByExample(pdte) >= 1) {
    		return true;
    	}
    	pdte.clear();
    	pdte.createCriteria().andSrcEqualTo(src).andStatusEqualTo(PreDistTask.STATUS_PRE_SUBMIT);
    	if(pdtm.countByExample(pdte) >= 2) {
    		return true;
    	}
    	return  false;
	}
	
	/**
	 * 根据outkey更新storePath
	 * @author gao.jun
	 * @param outKey
	 * @param storePath
	 */
	public void updateStorePathByOutkey(String outKey, String storePath) {
		// 更新storePath
		if(storePath != null) {
			PreDistTaskExample pdte = new PreDistTaskExample();
			pdte.createCriteria().andOutkeyEqualTo(outKey);
			PreDistTask rec = new PreDistTask();
			rec.setStorePath(storePath.replace("\\/", "/"));
			pdtm.updateByExampleSelective(rec, pdte);
		}
	}
	
	/**
	 * 根据tasktag和outkey查询任务
	 * @author gao.jun
	 * @param tasktag
	 * @param outkeys
	 * @return
	 */
	public List<PreDistTask> selectByTasktagAndkeys(String tasktag, String[] outkeys) {
		PreDistTaskExample pdte = new PreDistTaskExample();
		PreDistTaskExample.Criteria c = pdte.createCriteria();
		if(org.apache.commons.lang.StringUtils.isNotBlank(tasktag)) {
			c.andTasktagEqualTo(tasktag);
		}
		if(outkeys != null && outkeys.length != 0) {
			c.andOutkeyIn(Arrays.asList(outkeys));
		}
		return pdtm.selectByExample(pdte);
	}
	
	/**
	 * 生成进度查询信息
	 * @author gao.jun
	 * @param tasktag
	 * @param outkeys
	 * @return
	 * @throws IOException
	 * @since v0.3
	 */
	public JSONObject generateProgress(String tasktag, String[] outkeys) throws IOException {
		List<PreDistTask> tasks = selectByTasktagAndkeys(tasktag, outkeys);
		DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US);
    	format.setTimeZone(TimeZone.getTimeZone("GMT"));
		JSONArray jsonArray = new JSONArray();
		for(PreDistTask task : tasks) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("src", task.getSrc());
			// 按GMT时间进行格式化
			jsonObj.put("ptime", format.format(new Date(task.getCreateTime())));
			jsonObj.put("status", task.getStatus());
			jsonObj.put("key", org.apache.commons.lang.StringUtils.substringAfter(task.getOutkey(), "_"));
			if(task.getStatus() == 2 || task.getStatus() == 3) {
				int progress = CDNHelper.getDistPercent(task.getStorePath());
				if(progress >= 100){
					jsonObj.put("progress", "99%");
				}else {
					jsonObj.put("progress", String.valueOf(progress).concat("%"));
				}
			}else if(task.getStatus() == 1){
				jsonObj.put("progress", "100%");
			}else {
				jsonObj.put("progress", "0%");
			}
			jsonArray.add(jsonObj);
		}
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("result", jsonArray);
		return jsonObj;
	}
}
