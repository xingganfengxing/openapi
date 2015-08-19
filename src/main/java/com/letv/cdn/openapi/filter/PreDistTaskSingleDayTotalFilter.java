package com.letv.cdn.openapi.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.dao.openapi.CdnResourceLimitMapper;
import com.letv.cdn.openapi.dao.openapi.PreDistTaskMapper;
import com.letv.cdn.openapi.pojo.CdnResourceLimit;
import com.letv.cdn.openapi.pojo.CdnResourceLimitExample;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.utils.ErrorMsg;
import com.letv.cdn.openapiauth.exception.NoRightException;
import com.letv.cdn.openapiauth.filter.ResourceFilter;
import com.letv.cdn.openapiauth.pojo.Openapi;

/**
 * 进行单日提交的预分发任务总量的限制
 * <br>
 * 2015年3月6日
 * @author gao.jun
 *
 */
@Component("preDistTaskSingleDayTotalFilter")
public class PreDistTaskSingleDayTotalFilter extends ResourceFilter {
	
	private static final Logger log = LoggerFactory.getLogger(PreDistTaskSingleDayTotalFilter.class);
	
	/**
	 * 默认的限制数量
	 */
	private final int DEFAULT_TOTAL_LIMIT = 50000;
	
	@Resource
	CdnResourceLimitMapper resourceLimitMapper;
	
	@Resource
	PreDistTaskMapper taskMapper;

	@Override
	public boolean filter(String appkey, Map<String, String> paramVals,
			HttpServletRequest request, String requstBody) {
		boolean allowed = true;
		
		// 查询单日预分发任务总量限制数量
		CdnResourceLimitExample example = new CdnResourceLimitExample();
		CdnResourceLimitExample.Criteria c = example.createCriteria();
		String domainTag = paramVals.get("domaintag");
		if(domainTag == null) {
			domainTag = paramVals.get("domaintag");
		}
		c.andLimitedObjEqualTo(CdnResourceLimit.LIMITED_OBJ_DOMAIN).andLimitedObjIdEqualTo(domainTag)
			.andResourceEqualTo(CdnResourceLimit.RESOURCE_TASK).andLimitedModeEqualTo(CdnResourceLimit.LIMITED_MODE_STORAGE)
			.andTimeCycleEqualTo(CdnResourceLimit.TIME_CYCLE_DAY);
		
		String method = null;
		try {
			method = getMethodParam(requstBody);
		} catch (IOException e) {
			log.error("分发任务资源控制时，获取method参数有误", e);
		}
		if(method == null) {
			return allowed;
		}
		setLimitCriteria(method,c);
		
		List<CdnResourceLimit> limits = resourceLimitMapper.selectByExample(example);
		int limitVal = DEFAULT_TOTAL_LIMIT;
		if(limits != null && !limits.isEmpty()) {
			limitVal = limits.get(0).getLimitedValue();
		}
		PreDistTaskExample taskExample = new PreDistTaskExample();
		PreDistTaskExample.Criteria taskCir = taskExample.createCriteria();
		taskCir.andDomaintagEqualTo(paramVals.get("domaintag"));
		// 设置当日的时间范围
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),0,0,0);
		long start = cal.getTimeInMillis();
		// 86400000为一天的毫秒数
		taskCir.andCreateTimeGreaterThanOrEqualTo(start).andCreateTimeLessThan(start + 86400000);
		// 查询分发中、分发成功、分发失败重试的任务
		taskCir.andStatusIn(Arrays.asList(PreDistTask.STATUS_ING,PreDistTask.STATUS_SUCCESS,PreDistTask.STATUS_FAILURE_RETRY));
		
		setTaskCriteria(method,taskCir);
		
		Integer taskTotal = taskMapper.countByExample(taskExample);
		if(taskTotal + getTaskNum(requstBody) > limitVal) {
			ErrorMsg errorMsg = getErrorMsg(method);
			throw new NoRightException(429, errorMsg.getCode(), errorMsg.getMsg());
		}
		return allowed;
	}

	protected void setLimitCriteria(String method,CdnResourceLimitExample.Criteria c) {
		if(PreDistTask.TASK_SUB_TYPE.equals(method)) {
			// 新增的资源限制
			c.andOperationTypeEqualTo(CdnResourceLimit.OPERATION_TYPE_INSERT);
		}else if(PreDistTask.TASK_REFRESH.equals(method)){
			// 刷新的资源限制
			c.andOperationTypeEqualTo(Openapi.OPERATION_TYPE_UPDATE);
		}
	}
	
	protected void setTaskCriteria(String method,PreDistTaskExample.Criteria c) {
		if(PreDistTask.TASK_SUB_TYPE.equals(method)) {
			// 新增的任务总数
			c.andTaskTypeEqualTo(PreDistTask.TASK_SUB_TYPE);
		}else if(PreDistTask.TASK_REFRESH.equals(method)){
			// 刷新的任务总数
			c.andTaskTypeEqualTo(PreDistTask.TASK_REFRESH);
		}
	}
	
	protected ErrorMsg getErrorMsg(String method) {
		if(PreDistTask.TASK_SUB_TYPE.equals(method)) {
			// 新增任务过多
			return ErrorMsg.TOO_MANY_TASK_OF_SINGLE_DAY_INSERT;
		}else {
			// 刷新任务过多
			return ErrorMsg.TOO_MANY_TASK_OF_SINGLE_DAY_UPDATED;
		}
	}

	@Override
	public String getResource() {
		return CdnResourceLimit.RESOURCE_TASK;
	}

	@Override
	public Byte getOperationType() {
		return Openapi.OPERATION_TYPE_INSERT;
	}
	
	protected String getMethodParam(String requstBody) throws IOException {
		String method = null;
		if(StringUtils.isNotBlank(requstBody)) {
			Object methodObj = JSONObject.parseObject(requstBody).get("method");
			if(methodObj != null) {
				method = methodObj.toString();
			}
		}
		return method;
	}
	
	/**
	 * 获取本次提交的任务数量
	 * <br>
	 * 2015年3月24日
	 * @author gao.jun
	 * @param requstBody
	 * @return
	 */
	private int getTaskNum(String requstBody) {
		int num = 0;
		if(StringUtils.isNotBlank(requstBody)) {
			Object itemsObj = JSONObject.parseObject(requstBody).get("items");
			if(itemsObj != null) {
				JSONArray itemArray = JSONArray.parseArray(itemsObj.toString());
				num = itemArray.size();
			}
		}
		return num;
	}
}
