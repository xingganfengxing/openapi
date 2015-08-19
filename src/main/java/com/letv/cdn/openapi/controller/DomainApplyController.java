package com.letv.cdn.openapi.controller;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.letv.cdn.openapi.pojo.DomainApply;
import com.letv.cdn.openapi.service.DomainApplyService;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * 域名申请controller<br/>
 * 2014-12-11
 * @author gao.jun
 *
 */
@Controller
@RequestMapping("/cdn")
public class DomainApplyController {
	
	@Resource
	DomainApplyService domainApplyService;
	
	@OpenapiAuth
    @RequestMapping(value = "/domainapply", method = RequestMethod.POST)
	public ResponseJson add(@RequestParam String userid,
			@RequestParam String domaintag,
			@RequestParam String domain,
			@RequestParam String source,
			@RequestParam Short serviceType,
			@RequestParam String contacts,
			@RequestParam String contactsPhone,
			@RequestParam String contactsEmail,
			@RequestParam(required = false) String remark){
		checkApplyParam(userid, domaintag, domain, source, contacts,
				contactsPhone, contactsEmail);
		
        /*DomainApply domainApply = new DomainApply(Integer.valueOf(userid), domaintag, domain, source, serviceType, 
        		contacts, contactsPhone, contactsEmail, remark);*/
		DomainApply domainApply = domainApplyService.insert(Integer.valueOf(userid), domaintag, domain, source, serviceType, 
        		contacts, contactsPhone, contactsEmail, remark);
        if(domainApply != null) {
        	return ResponseJson.createdNoCache(domainApply);
        } else {
        	return ResponseJson.internalServerError("添加域名加速申请失败");
        }
	}
	
	@OpenapiAuth
    @RequestMapping(value = "/domainapply/{id}", method = RequestMethod.PUT)
	public ResponseJson update(@PathVariable Integer id,
			@RequestParam String userid,
			@RequestParam String domaintag,
			@RequestParam String domain,
			@RequestParam String source,
			@RequestParam Integer serviceType,
			@RequestParam String contacts,
			@RequestParam String contactsPhone,
			@RequestParam String contactsEmail,
			@RequestParam(required = false) String remark) throws IllegalAccessException, InvocationTargetException{
		
		DomainApply domainApply = domainApplyService.selectById(id);
		Assert.notNull(domainApply, "The domain apply isn't found!");
		Short serviceStatus = domainApply.getServiceStatus();
		Assert.isTrue(DomainApply.AUDIT_APPROVING.equals(serviceStatus), "The domain apply has been audited or been canceled!");
		
		checkApplyParam(userid, domaintag, domain, source, contacts,
				contactsPhone, contactsEmail);
		
        DomainApply newDomainApply = new DomainApply(Integer.valueOf(userid), domaintag, domain, source, Short.valueOf(serviceType.toString()), 
        		contacts, contactsPhone, contactsEmail, remark);
        
        BeanUtils.copyProperties(domainApply, newDomainApply);
        
        if(domainApplyService.update(domainApply)) {
        	return ResponseJson.okWithNoCache(domainApply);
        } else {
        	return ResponseJson.internalServerError("修改域名加速申请失败");
        }
	}
	
	@OpenapiAuth
    @RequestMapping(value = "/domainapply/{id}", method = RequestMethod.DELETE)
	public ResponseJson cancel(@PathVariable Integer id) {
		DomainApply domainApply = domainApplyService.selectById(id);
		Assert.notNull(domainApply, "The domain apply isn't found!");
		if(domainApplyService.delete(id)) {
			return ResponseJson.okWithNoCache(domainApply);
		}else {
			return ResponseJson.internalServerError("取消域名加速申请失败");
		}
	}
	
	@OpenapiAuth
    @RequestMapping(value = "/domainapply/{id}", method = RequestMethod.GET)
	public ResponseJson getById(@PathVariable Integer id) {
		DomainApply domainApply = domainApplyService.selectById(id);
		Assert.notNull(domainApply, "The domain apply isn't found!");
		return ResponseJson.okWithNoCache(domainApply);
	}
	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public void test(@RequestParam() Integer id){
		domainApplyService.selectById(id);
		ResponseJson.okWithNoCache("{\"rows\":[],\"total\":0}");
	}
	
	@OpenapiAuth
    @RequestMapping(value = "/domainapplys", method = RequestMethod.GET)
//	@ResponseBody
	public ResponseJson pagedQuery(@RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer userid,
            @RequestParam(required = false) String domaintag,
			@RequestParam(required = false) String domain,
			@RequestParam(required = false) String source,
			@RequestParam(required = false) Short serviceStatus,
			@RequestParam int page, @RequestParam int rows) throws ParseException {
		
		Assert.isTrue(RegExpValidatorUtil.isDate(startTime,"yyyy-MM-dd HH:mm:ss"),"The parameter startTime isn't valid.");
		Assert.isTrue(RegExpValidatorUtil.isDate(endTime,"yyyy-MM-dd HH:mm:ss"),"The parameter endTime isn't valid.");
		Assert.isTrue(page >= 1, "The parameter page must larger or equal 1.");
		Assert.isTrue(rows >= 1, "The parameter rows must larger or equal 1.");
		
		return ResponseJson.okWithNoCache(domainApplyService.pagedQuery(startTime, endTime, userid, domaintag, 
				domain, source, serviceStatus, page, rows));
	}
    
	@OpenapiAuth
    @RequestMapping(value = "/domainapplys", method = RequestMethod.PUT)
    public ResponseJson audit(@RequestParam String ids,@RequestParam Short serviceStatus){ 
    	Assert.isTrue(!StringUtils.isEmpty(ids),"The parameter ids is empty");
    	Assert.isTrue(DomainApply.AUDIT_ALLOWED.equals(serviceStatus) || 
    			DomainApply.AUDIT_DENY.equals(serviceStatus),"The parameter serviceStatus isn't valid.");
    	 domainApplyService.audit(ids, serviceStatus);
    	 return ResponseJson.okWithNoCache("审核成功");
    }
	
	private void checkApplyParam(String userid, String domaintag,
			String domain, String source, String contacts,
			String contactsPhone, String contactsEmail) {
		Assert.isTrue(userid.length() >= 6, "The parameter userid must be greater than or equal to 6 characters.");
		Assert.isTrue(domaintag.length() <= 128, "The parameter domaintag must be less than 128 characters.");
        Assert.isTrue(domain.length() <= 128, "The parameter domain must be less than or equal to 128 characters.");
        Assert.isTrue(source.length() <= 128, "The parameter source must be less than or equal to 128 characters.");
        Assert.isTrue(contacts.length() <= 45, "The parameter contacts must be less than or equal to 45 characters.");
        Assert.isTrue(contactsPhone.length() <= 45, "The parameter contactsPhone must be less than or equal to 45 characters.");
//        Assert.isTrue(RegExpValidatorUtil.isEmail(contactsEmail), "The parameter contactsEmail isn't valid.");
        Assert.isTrue(contactsEmail.length() <= 45, "The parameter contactsEmail must be less than or equal to 45 characters.");
	}
}
