package com.letv.cdn.openapi.service;

import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.dao.openapi.DomainApplyMapper;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.ApiDomain;
import com.letv.cdn.openapi.pojo.DomainApply;
import com.letv.cdn.openapi.pojo.DomainApplyExample;
import com.letv.cdn.openapi.utils.DateUtil;

/**
 * 域名申请service<br/>
 * 2014-12-11
 * @author gao.jun
 */
@Service
public class DomainApplyService {
	
	@Resource
	DomainApplyMapper domainApplyMapper;
	
	@Resource
	ApiDomainService apiDomainService;
	
	@Resource
	DomainService domainService;
	
	/**
	 * 新增域名申请，设置服务状态为待审核，创建时间为当前系统时间
	 * @param domainApply 已设置了基础信息的域名申请对象
	 */
	public boolean insert(DomainApply domainApply) {
		domainApply.setServiceStatus(DomainApply.AUDIT_APPROVING);
		domainApply.setCreateTime(new Date(Calendar.getInstance().getTimeInMillis()));
		return domainApplyMapper.insert(domainApply) == 1;
	}
	
	public DomainApply insert(Integer userid, String domaintag, String domain,
			String source, Short serviceType, String contacts,
			String contactsPhone, String contactsEmail, String remark) {
		
		DomainApply domainApply = new DomainApply(Integer.valueOf(userid), domaintag, domain, source, serviceType, 
        		contacts, contactsPhone, contactsEmail, remark);
		if(insert(domainApply)) {
			return domainApply;
		}
		return null;
	}
	
	public boolean update(DomainApply domainApply) {
		return domainApplyMapper.updateByPrimaryKey(domainApply) == 1;
	}
	
	public boolean delete(Integer id) {
		return domainApplyMapper.deleteByPrimaryKey(id) == 1;
	}
	
	public DomainApply selectById(Integer id) {
		return domainApplyMapper.selectByPrimaryKey(id);
	}
	
	public JSONObject pagedQuery(String startTime, String endTime, Integer userid,
			String domaintag, String domain, String source,
			Short serviceStatus, int page, int rows) throws ParseException {
		
		DomainApplyExample example = new DomainApplyExample();
		DomainApplyExample.Criteria c = example.createCriteria();
		if(!StringUtils.isEmpty(startTime) && !StringUtils.isEmpty(startTime)) {
			c.andCreateTimeBetween(DateUtil.getDate(startTime), DateUtil.getDate(endTime));
		}
		if(userid != null) {
			c.andUseridEqualTo(userid);
		}
		if(!StringUtils.isEmpty(domaintag)) {
			c.andDomaintagEqualTo(domaintag);
		}
		if(!StringUtils.isEmpty(domain)) {
			c.andDomainEqualTo(domain);
		}
		if(!StringUtils.isEmpty(source)) {
			c.andSourceEqualTo(source);
		}
		if(serviceStatus != null) {
			c.andServiceStatusEqualTo(serviceStatus);
		}
		Integer total = domainApplyMapper.countByExample(example);
		example.setLimitValue1((page - 1) * rows);
		example.setLimitValue2(page * rows);
		List<DomainApply> applys = domainApplyMapper.selectByExample(example);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("total", total);
		jsonObj.put("rows", applys);
		return jsonObj;
	}
	
	public void audit(String ids,Short serviceStatus) {
		
		// 更新审核状态
		DomainApply updatedApply = new DomainApply();
		updatedApply.setServiceStatus(serviceStatus);
		DomainApplyExample example = new DomainApplyExample();
		DomainApplyExample.Criteria c = example.createCriteria();
		List<Integer> idList = new ArrayList<Integer>();
		for(String id : ids.split(",")) {
			idList.add(Integer.parseInt(id));
		}
		c.andIdIn(idList);
		domainApplyMapper.updateByExampleSelective(updatedApply, example);
		
		// 如果是审核通过，保存API中的domain，并且新增cdn中的domain和report中的user_domain
		if(DomainApply.AUDIT_ALLOWED.equals(serviceStatus)) {
			for(Integer id : idList) {
				
				DomainApply apply = domainApplyMapper.selectByPrimaryKey(id);
				
				ApiDomain domain = new ApiDomain(apply.getUserid(), apply.getDomaintag(), apply.getDomain(), 
						apply.getSource(), apply.getServiceType(), ApiDomainService.ENABLED, apply.getRemark());
				apiDomainService.insert(domain);
				
				CoopDomain cd = new CoopDomain();
				cd.setUserid(apply.getUserid().toString());
				cd.setUsertag(apply.getDomaintag());
				cd.setDomain(apply.getDomain());
				cd.setSource(apply.getSource());
				cd.setAction(apply.getServiceStatus());
				cd.setRemark(apply.getRemark());
				domainService.insert(cd);
			}
		}
	}
}
