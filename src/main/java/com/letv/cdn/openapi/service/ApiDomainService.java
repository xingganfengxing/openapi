package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.dao.domain.CoopDomainMapper;
import com.letv.cdn.openapi.dao.openapi.ApiDomainMapper;
import com.letv.cdn.openapi.pojo.ApiDomain;
import com.letv.cdn.openapi.pojo.ApiDomainExample;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.CoopDomainExample;
import com.letv.cdn.openapi.utils.DateUtil;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * api Domain对应的service
 * <br>
 * 2015年3月9日
 * @author gao.jun
 *
 */
@Service
public class ApiDomainService {
	
	private static final Logger log = LoggerFactory.getLogger(ApiDomainService.class);
	 
	private static Map<Integer,String> setHostUserDomains = new HashMap<Integer,String>();

	public static final Short ENABLED = 1;

	public static final Short DISABLED = 0;
	
	static {
		setHostUserDomains.put(136098, ".*src.ucloud.com.cn");
		setHostUserDomains.put(256567, ".*.qiniudns.com");
	}
	
	@Resource
    ApiDomainMapper apiDomainMapper;
	
	@Resource
    CoopDomainMapper coopDomainMapper;
	
	@Resource
	DomainService domainService;
	
	/**
	 * api Domain中的分页查询
	 * <br>
	 * 2015年3月9日
	 * @author gao.jun
	 * @param startTime
	 * @param endTime
	 * @param userid
	 * @param domaintag
	 * @param domain
	 * @param source
	 * @param enabled
	 * @param page
	 * @param rows
	 * @return
	 * @throws ParseException
	 */
	public String pagedQuery(String startTime, String endTime, Integer userid,
			String domaintag, String domain, String source,
			Short enabled, int page, int rows) throws ParseException {
    	ApiDomainExample example = new ApiDomainExample();
		ApiDomainExample.Criteria c = example.createCriteria();
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
		if(enabled != null) {
			c.andEnabledEqualTo(enabled);
		}
		Integer total = apiDomainMapper.countByExample(example);
		example.setLimitValue1((page - 1) * rows);
		example.setLimitValue1(page * rows);
		List<ApiDomain> applys = apiDomainMapper.selectByExample(example);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("total", total);
		jsonObj.put("rows", applys);
		return jsonObj.toJSONString();
    }
	
	/**
     * 保存api中的domain，同时保存cdn中的域名以及report中的域名信息
     * @param domain api中的domain对象
     * @return 是否保存成功
     */
    public boolean insert(ApiDomain domain) {
    	domain.setCreateTime(new Date(System.currentTimeMillis()));
    	CoopDomain coopDomain = new CoopDomain();
    	coopDomain.setAction(domain.getServiceType());
    	coopDomain.setDomain(domain.getDomain());
    	coopDomain.setRemark(domain.getRemark());
    	coopDomain.setSource(domain.getSource());
    	coopDomain.setUserid(domain.getUserid().toString());
    	coopDomain.setUsertag(domain.getDomaintag());
    	coopDomain.setOntest((short) 0);
    	// 设置setHost为domain
    	if(setHostUserDomains.containsKey(domain.getUserid()) && Pattern.compile(setHostUserDomains.get(domain.getUserid())).matcher(domain.getSource()).matches()) {
    		domain.setSetHost(domain.getDomain());
    		coopDomain.setSetHost(domain.getDomain());
    	}
    	// 添加api Domain、cdn domain和report domain
    	return this.apiDomainMapper.insert(domain) == 1 && this.insertCoopDomain(coopDomain) && this.addUserDomain(coopDomain);
    }
    
    
    /**
     * 根据userid和domaintag查询指定的域名记录。
     * 查询api中的domain信息，不直接查询CDN中的域名。
     * <b>Method</b>: DomainService#selectByUseridAndDomaintag <br/>
     * <b>Create Date</b> : 2014年11月3日
     * @author Chen Hao
     * @param userid
     * @param domaintag
     * @return  CoopDomain
     */
    public ApiDomain selectByUseridAndDomaintag(String userid, String domaintag) {
        ApiDomainExample de = new ApiDomainExample();
        ApiDomainExample.Criteria c = de.createCriteria();
        c.andUseridEqualTo(Integer.valueOf(userid));
        c.andDomaintagEqualTo(domaintag);
        List<ApiDomain> list = this.apiDomainMapper.selectByExample(de);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * 修改api中的加速域名信息，同时修改cdn中的域名
     * <b>Method</b>: DomainService#update <br/>
     * <b>Create Date</b> : 2014年10月30日
     * @author Chen Hao
     * @param domain
     * @return  boolean
     */
    public boolean update(ApiDomain domain) {
    	// 更新API Domain
    	boolean domainUpdated = this.apiDomainMapper.updateByPrimaryKey(domain) == 1;
    	
    	// 更新CDN Domain
    	boolean coopDomainUpdated = true;
    	CoopDomainExample coopDomainExample = new CoopDomainExample();
    	CoopDomainExample.Criteria c = coopDomainExample.createCriteria(); 
    	c.andUsertagEqualTo(domain.getDomaintag());
    	List<CoopDomain> coopDomains = coopDomainMapper.selectByExample(coopDomainExample);
    	if(coopDomains != null && coopDomains.size() == 1) {
    		CoopDomain coopDomain = coopDomains.get(0);
    		if(domain.getEnabled() == 1) {
    			if ("137587".equals(domain.getUserid()) || "136098".equals(domain.getUserid()) || "256567".equals(domain.getUserid())) {
    				coopDomain.setAction(CoopDomain.ACTION_ENABLE_SMALL_FILE_MOVE);
    			} else {
    				coopDomain.setAction(CoopDomain.ACTION_ENABLE_BIG_FILE);
    			}
    		}else {
    			coopDomain.setAction((short)0);
    		}
    		coopDomain.setDomain(domain.getDomain());
    		coopDomain.setSource(domain.getSource());
        	coopDomain.setRemark(domain.getRemark());
        	CoopDomainExample cdExample = new CoopDomainExample();
        	cdExample.createCriteria().andUsertagEqualTo(domain.getDomaintag());
        	coopDomainUpdated = coopDomainMapper.updateByExampleSelective(coopDomain, cdExample) == 1;
        	
    	}
        return domainUpdated && coopDomainUpdated;
    }
    
    /**
     * 启用或禁用域名
     * <br>
     * 2015年3月9日
     * @author gao.jun
     * @param domaintag
     * @param flag
     * @return
     */
    public boolean enableOrDisable(String domaintag, Short flag) {
    	// 更新API Domain
    	ApiDomainExample apiDomainExample = new ApiDomainExample();
    	apiDomainExample.createCriteria().andDomaintagEqualTo(domaintag);
    	ApiDomain apiDomain = new ApiDomain();
    	apiDomain.setEnabled(flag);
    	boolean domainUpdated = this.apiDomainMapper.updateByExampleSelective(apiDomain, apiDomainExample) == 1 ? true : false;
    	
    	// 更新cdn Domain
    	boolean coopDomainUpdated = true;
    	CoopDomainExample coopDomainExample = new CoopDomainExample();
    	CoopDomainExample.Criteria c = coopDomainExample.createCriteria(); 
    	c.andUsertagEqualTo(domaintag);
    	List<CoopDomain> coopDomains = coopDomainMapper.selectByExample(coopDomainExample);
    	if(coopDomains != null && coopDomains.size() == 1) {
    		CoopDomain coopDomain = coopDomains.get(0);
    		coopDomainUpdated = domainService.enableOrDisableDomain(coopDomain, flag);
    	}
    	
    	return domainUpdated && coopDomainUpdated;
    }
    
    /**
     * 根据domaintag删除加速域名
     * <br>
     * 2014年12月19日
     * @author gao.jun
     * @param domaintag
     * @return 正常删除返回true，反之为false
     * @throws IOException 
     */
    public boolean deleteByDomaintag(String domaintag) throws IOException {
    	ApiDomainExample domainExample = new ApiDomainExample();
    	domainExample.createCriteria().andDomaintagEqualTo(domaintag);
    	
    	CoopDomainExample example = new CoopDomainExample();
    	CoopDomainExample.Criteria c = example.createCriteria();
    	c.andUsertagEqualTo(domaintag);
    	
    	return  apiDomainMapper.deleteByExample(domainExample) == 1
    			&& coopDomainMapper.deleteByExample(example) ==  1 && deleteUserDomain(domaintag);
    }
    
    /**
     * 调用manager接口，删除域名信息
     * <br>
     * 2014年12月19日
     * @author gao.jun
     * @param domaintag
     * @return 正常删除返回true，反之为false
     * @throws IOException 
     */
    private boolean deleteUserDomain(String domaintag) throws IOException {
    	String result = null;
    	try {
    		result = HttpClientUtil.deleteDomain(Env.get("manager_del_domain").concat("/cdn_").concat(domaintag), HttpClientUtil.UTF_8);
		}catch (IOException e) {
			log.error(e.getMessage(), e);
			throw e;
		}
    	return result != null;
    }
    
    /**
     * 
     * <b>Method</b>: DomainService#insertCoopDomain <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param cd
     * @return  boolean
     */
    private boolean insertCoopDomain(CoopDomain cd) {
        return this.coopDomainMapper.insert(cd) == 1;
    }
    
    /**
     * 
     * <b>Method</b>: DomainService#addUserDomain <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param cd
     * @return  boolean
     */
    private boolean addUserDomain(CoopDomain cd) {
        String userid = cd.getUserid();
        String domain = cd.getUsertag();
        String tag = "102." + cd.getUsertag();
        Map<String, String> args = new HashMap<String, String>();
        args.put("userid", userid);
        args.put("domain", domain);
        args.put("tag", tag);
        args.put("type", "cdn");
        args.put("chargeType", "general");
        String rst = null;
        try {
            rst = HttpClientUtil.post(Env.get("manager_add_domain"), args, HttpClientUtil.UTF_8);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return rst != null;
    }
    
    /**
     * 校验域名标识和访问是否重复<br>
     * 2015年5月18日<br>
     * @author gao.jun
     * @param domaintag
     * @param domain
     * @return 如果重复，则返回false，反之为true
     */
    public boolean uniqueCheck(String domaintag, String domain) {
    	boolean valid = true;
    	ApiDomainExample example = new ApiDomainExample();
    	example.createCriteria().andDomainEqualTo(domain);
    	example.or(example.createCriteria().andDomaintagEqualTo(domaintag));
    	valid = apiDomainMapper.countByExample(example) >= 1 ? false : true;
    	return valid;
    }
}
