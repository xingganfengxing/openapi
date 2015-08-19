package com.letv.cdn.openapi.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.letv.cdn.openapi.dao.report.UserDomainMapper;
import com.letv.cdn.openapi.pojo.UserDomain;
import com.letv.cdn.openapi.pojo.UserDomainExample;
import com.letv.cdn.openapi.pojo.UserDomainExample.Criteria;

@Service
public class UserDomainService {
	
	 @Resource
	 UserDomainMapper userDomainMapper;
	 /**
     * 根据domaintag 获取UserDomain对象
     * @param domaintag
     * @return
     * @2014, by liuchangfu
     */
    public UserDomain selectBydomaintag(String domaintag){
    	  UserDomainExample ude = new UserDomainExample();
    	  Criteria criteria = ude.createCriteria();
    	  criteria.andDomainTagEqualTo(domaintag);
    	  List<UserDomain> userDomainlist = userDomainMapper.selectByExample(ude);
    	  if (userDomainlist.size() > 0) {
              return userDomainlist.get(0);
          } else {
              return null;
          }
    }
    /**
     * 根据userid  获取UserDomain对象
     * @param id
     * @return
     * @2014, by liuchangfu
     */
    public List<UserDomain> selectByUserid(Long id){
    	UserDomainExample ude = new UserDomainExample();
    	Criteria criteria = ude.createCriteria();
    	criteria.andUserIdEqualTo(id);
    	List<UserDomain> userDomainlist = userDomainMapper.selectByExample(ude);
    	if (userDomainlist.size() > 0) {
            return userDomainlist ;
        } else {
            return null;
        }
    	
    }
}
