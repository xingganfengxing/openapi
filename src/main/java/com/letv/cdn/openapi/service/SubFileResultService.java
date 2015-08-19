/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : SubFileResult.java 
 * Date Created: 2014年11月6日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年11月6日 下午5:38:50 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.letv.cdn.openapi.controller.ContentApiController;
import com.letv.cdn.openapi.dao.report.SubFileResultMapper;
import com.letv.cdn.openapi.pojo.SubFileResult;
import com.letv.cdn.openapi.pojo.SubFileResultExample;
import com.letv.cdn.openapi.pojo.SubFileResultExample.Criteria;

/**
 * TODO:提交预分发文件回调结果Service层
 * 
 * @author chenyuxin
 * @createDate 2014年11月6日
 */
@Service
public class SubFileResultService{
    
    @Resource
    SubFileResultMapper srm;
    
    /**
     * 新增提交分发文件结果记录
     * 
     * @method: SubFileResultService  insertSubFileResult
     * @param sr
     * @return  boolean
     * @createDate： 2014年11月6日
     * @2014, by chenyuxin.
     */
    public boolean insertSubFileResult(SubFileResult sfr){
        return srm.insert(sfr) == 1;
    }
    
    /**
     * 判断key能否查询到回调结果
     * 
     * @method: SubFileResultService  getResultByKey
     * @param key
     * @return  boolean
     * @createDate： 2014年11月7日
     * @2014, by chenyuxin.
     */
    public int getResultByKey(String key){
        SubFileResultExample example =  new SubFileResultExample();
        Criteria c = example.createCriteria();
        c.andOutkeyEqualTo(key);
        List<SubFileResult> list = srm.selectByExample(example);
        if(list.size() > 0){
            return ContentApiController.DISTRIBUTE_FAILURE;
        }
        return ContentApiController.DISTRIBUTE_ING;
    }
    
    /**
     * 判断MD5能否查询到回调结果
     * 
     * @method: SubFileResultService  getResultByMd5
     * @param md5
     * @return  boolean
     * @createDate： 2014年11月7日
     * @2014, by chenyuxin.
     */
    public int getResultByMd5(String md5){
        SubFileResultExample example =  new SubFileResultExample();
        Criteria c = example.createCriteria();
        c.andMd5EqualTo(md5);
        List<SubFileResult> list = srm.selectByExample(example);
        if(list.size() > 0){
            return ContentApiController.DISTRIBUTE_FAILURE;
        }
        return ContentApiController.DISTRIBUTE_ING;
    }
}
