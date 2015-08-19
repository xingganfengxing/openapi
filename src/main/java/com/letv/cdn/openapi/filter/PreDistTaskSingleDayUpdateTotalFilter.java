package com.letv.cdn.openapi.filter;

import org.springframework.stereotype.Component;

import com.letv.cdn.openapi.pojo.CdnResourceLimitExample;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.utils.ErrorMsg;
import com.letv.cdn.openapiauth.pojo.Openapi;

/**
 * 进行单日更新的预分发任务数量的限制
 * <br>
 * 2015年3月10日
 * @author gao.jun
 *
 */
@Component("preDistTaskSingleDayUpdateTotalFilter")
public class PreDistTaskSingleDayUpdateTotalFilter extends PreDistTaskSingleDayTotalFilter {

	@Override
	protected void setLimitCriteria(String method,CdnResourceLimitExample.Criteria c) {
		// 更新的总数
		c.andOperationTypeEqualTo(Openapi.OPERATION_TYPE_UPDATE);
	}

	@Override
	protected void setTaskCriteria(String method,PreDistTaskExample.Criteria c) {
		// 刷新的任务
		c.andTaskTypeEqualTo(PreDistTask.TASK_REFRESH);
	}

	@Override
	protected ErrorMsg getErrorMsg(String method) {
		return ErrorMsg.TOO_MANY_TASK_OF_SINGLE_DAY_UPDATED;
	}
	
	@Override
	public Byte getOperationType() {
		return Openapi.OPERATION_TYPE_UPDATE;
	}
}
