package com.letv.cdn.openapi.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.letv.cdn.openapi.dao.report.UserMapper;
import com.letv.cdn.openapi.pojo.User;
import com.letv.cdn.openapi.pojo.UserExample;

@Service
public class UserService{
    
    @Resource
    UserMapper um;
    
    public List<User> selectAll() {
        return this.um.selectByExample(new UserExample());
    }

    /**
     * 根据userid查询user对象
     * <b>Method</b>: UserService#selectByUserid <br/>
     * <b>Create Date</b> : 2014年11月7日
     * @author Chen Hao
     * @param userid
     * @return  User
     */
    public User selectByUserid(Integer userid) {
        UserExample ue = new UserExample();
        ue.createCriteria().andUseridEqualTo(userid);
        List<User> userList =  this.um.selectByExample(ue);
        if (userList.size() > 0) {
            return userList.get(0);
        } else {
            return null;
        }
    }
    
    
    
}
