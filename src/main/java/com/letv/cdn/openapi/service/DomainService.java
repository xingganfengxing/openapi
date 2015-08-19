package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.letv.cdn.openapi.dao.domain.CoopDomainMapper;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.CoopDomainExample;
import com.letv.cdn.openapi.pojo.CoopDomainExample.Criteria;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * <br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年10月24日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
@Service
public class DomainService {
    
    private static final Logger log = LoggerFactory.getLogger(DomainService.class);
    
    @Resource
    CoopDomainMapper cdm;
    
    public List<CoopDomain> selectAll() {
        return this.cdm.selectByExample(new CoopDomainExample());
    }
    
    /**
     * 根据userid和domaintag查询指定的域名记录
     * <b>Method</b>: DomainService#selectByUseridAndDomaintag <br/>
     * <b>Create Date</b> : 2014年11月3日
     * @author Chen Hao
     * @param userid
     * @param domaintag
     * @return  CoopDomain
     */
    public CoopDomain selectByUseridAndDomaintag(String userid, String domaintag) {
        CoopDomainExample cde = new CoopDomainExample();
        Criteria c = cde.createCriteria();
        c.andUseridEqualTo(userid);
        c.andUsertagEqualTo(domaintag);
        List<CoopDomain> list = this.cdm.selectByExample(cde);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * 根据userid获取该用户所有域名信息
     * @method: CoopDomainService  selectByUserid
     * @param userid
     * @return  List<CoopDomain>
     * @createDate： 2014年10月14日
     * @2014, by Chen Hao.
     */
    public List<CoopDomain> selectByUserid(String userid) {
        CoopDomainExample cde = new CoopDomainExample();
        Criteria c = cde.createCriteria();
        c.andUseridEqualTo(userid);
        List<CoopDomain> list = this.cdm.selectByExample(cde);
        return list;
    }

    
    /**
     * 
     * <b>Method</b>: DomainService#insert <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param cd
     * @return  boolean
     */
    public boolean insert(CoopDomain cd) {
        return this.insertCoopDomain(cd) && this.addUserDomain(cd);
    }
    
    /**
     * 修改加速域名配置
     * <b>Method</b>: DomainService#update <br/>
     * <b>Create Date</b> : 2014年10月30日
     * @author Chen Hao
     * @param cd
     * @return  boolean
     */
    public boolean update(CoopDomain cd) {
        return this.cdm.updateByPrimaryKey(cd) == 1;
    }
    
    /**
     * 根据domaintag更新域名配置信息
     * <br>
     * 2014年12月22日
     * @author gao.jun
     * @param cd 待更新的域名信息
     * @return
     */
    public boolean updateByDomaintag(CoopDomain cd) {
    	CoopDomainExample cdExample = new CoopDomainExample();
    	cdExample.createCriteria().andUsertagEqualTo(cd.getUsertag());
    	return cdm.updateByExampleSelective(cd, cdExample) == 1;
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
        return this.cdm.insert(cd) == 1;
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
     * 
     * <b>Method</b>: DomainService#enable <br/>
     * <b>Create Date</b> : 2014年11月6日
     * @author Chen Hao
     * @throws IOException  void
     */
    public void enable() throws IOException {
        HttpClientUtil.get(Env.get("enable_domain_url"), HttpClientUtil.UTF_8);
    }
    
    /**
     * 禁用域名配置信息，并刷新配置信息<br>
     * 2014-12-19
     * @author gao.jun
     * @param cd 域名配置信息
     * @throws IOException
     */
    public boolean disableCdnDomain(CoopDomain cd){
    	cd.setAction((short)0);
    	return cdm.updateByPrimaryKeySelective(cd) == 1;
    }
    
    /**
     * 启用域名配置，ucloud用户设置为可拖拽小文件，其余用于为大文件预分发<br>
     * 2014-12-19
     * @author gao.jun
     * @param cd 域名配置信息
     * @throws IOException
     */
    public boolean enableCdnDomain(CoopDomain cd) {
    	// 对ucloud用户做特殊处理
    	if ("137587".equals(cd.getUserid()) || "136098".equals(cd.getUserid()) || "256567".equals(cd.getUserid())) {
            cd.setAction(CoopDomain.ACTION_ENABLE_SMALL_FILE_MOVE);
        } else {
            cd.setAction(CoopDomain.ACTION_ENABLE_BIG_FILE);
        }
    	return cdm.updateByPrimaryKeySelective(cd) == 1;
    }
    
    /**
     * 启用或禁用域名配置
     * <br>
     * 2014年12月29日
     * @author gao.jun
     * @param cd 域名信息
     * @param flag 启用为1，禁用为0
     * @return
     * @throws IOException
     */
    public boolean enableOrDisableDomain(CoopDomain cd, short flag) {
    	if(flag == 1) {
    		return enableCdnDomain(cd);
    	}else {
    		return disableCdnDomain(cd);
    	}
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
    	CoopDomainExample example = new CoopDomainExample();
    	CoopDomainExample.Criteria c = example.createCriteria();
    	c.andUsertagEqualTo(domaintag);
    	// 暂时不删除repot中的域名信息 2015-1-4
    	return cdm.deleteByExample(example) ==  1/* && deleteUserDomain(domaintag)*/;
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
    @SuppressWarnings("unused")
    private boolean deleteUserDomain(String domaintag) throws IOException {
    	String result = null;
    	try {
    		result = HttpClientUtil.deleteDomain(Env.get("manager_del_domain").concat("/cdn_").concat(domaintag), HttpClientUtil.UTF_8);
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			throw e;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw e;
		}
    	return result != null;
    }
}
