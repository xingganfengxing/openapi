/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : openapi 
 * Class Name  : ContentService.java 
 * Date Created: 2014年10月30日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月30日 下午5:43:55 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.dao.openapi.PreDistParamMapper;
import com.letv.cdn.openapi.exception.NoRightException;
import com.letv.cdn.openapi.exception.NoRightException.Type;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.PreDistParam;
import com.letv.cdn.openapi.pojo.PreDistParamExample;
import com.letv.cdn.openapi.pojo.PreDistTask;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * TODO:ContentService的service层
 * 
 * @author chenyuxin
 * @createDate 2014年10月30日
 */
@Service
public class ContentService{
    
    private static final Logger log  = LoggerFactory.getLogger(ContentService.class);
    
    /**查询分发文件的接口*/
    private static final String GET_LECLOUD_FILE_URI = Env.get("get_lecloud_file_key_uri");
    /**提交预分发文件的接口*/
    private static final String SUBMIT_FILE_URI = Env.get("submit_file_uri");
    /**删除分发文件的接口*/
    private static final String DELETE_FILE_URI = Env.get("delete_file_uri");
    
    private static final int SUB_STATUS_SUCCESS = 200;
    
//    private static final int SUB_STATUS_REPEAT = 400;
    
    @Resource
    DomainService domainService;
    
    @Resource
    PreDistTaskService pdts;
    
    @Resource
    PreDistParamMapper preDistParamMapper;
    
    /**
     * 提交预分发文件的方法
     * 
     * @method: ContentService  subFile
     * @param domaintag 域名标识
     * @param key 客户定义的此次任务的唯一标识
     * @param src 资源地址
     * @param md5 文件MD5校验码
     * @param userid 用户id
     * @param cip 用户的真实ip
     * @return
     * @throws IOException 
     * @throws DocumentException 
     * @throws Exception  boolean
     * @createDate： 2014年10月29日
     * @2014, by chenyuxin.
     */
    public boolean subFile(String domaintag, String key, String src, String md5, Integer userid, String cip,String type) throws IOException, DocumentException{
        /*
         * key——item_id 经过url解码后的src——source_path md5——md5 
         * <?xml version="1.0" encoding="UTF-8"?> 
         * <ccsc> 
         *     <item_id value="5374">
         *         <source_path>http://210.51.33.77:8001/FTP/DN/20100811/3/
         *     DragonNest_Patch_v21-22.exe</source_path>
         *         <md5>ae5800d3fa505e92e244023a02bd9735</md5> 
         *     </item_id>
         * </ccsc>
         */
        src = RegExpValidatorUtil.replaceBlank(src.trim());
        Assert.isTrue(src != null && src.length() > 7 && "http://".equalsIgnoreCase(src.substring(0, 7)), "文件源地址(src)不能为空且必须包含\"http://\"");
        if (!this.pdts.insert(userid, domaintag, src, key, md5,type)) {
            return false;
        }
        log.info("访问文件分发系统接口：" + SUBMIT_FILE_URI);
        
        // 如果不存在appkey对应的预分发任务参数，则返回失败-false
        Map<String, String> params = subParams(domaintag, userid.toString(), cip);
        String tmpResult = HttpClientUtil.postXml(SUBMIT_FILE_URI, params, subxml(key, src, md5, userid), HttpClientUtil.UTF_8);
        boolean rst = true;
        // 服务器内部异常
        if (tmpResult == null) {
            rst = false;
            throw new IOException();
        }
        // 调用接口的IP不在白名单内
        if ("403".equals(tmpResult)) {
            rst = false;
            throw new NoRightException(Type.IP_NOT_ALLOW);
        }
        // 正常返回,若xml节点result为SUCCESS，表示提交成功；为FAILURE，表示提交失败.
        //[ { "user": "acloud", "itemid": "137587_acloudtest1416309910832", "source": "http:\/\/dlsw.baidu.com\/sw-search-sp\/soft\/cc\/13478\/npp.6.6.9.Installer.1410249599.exe", "filemd5": "", "domain": "v.letvcloud.com", "strorepath": "105\/44\/54\/acloud\/137587\/sw-search-sp\/soft\/cc\/13478\/npp.6.6.9.Installer.1410249599.exe", "staus": 1 } ]
        try {
            JSONArray ja = (JSONArray) JSONObject.parse(tmpResult);
            log.info("提交预分发文件结果：{}", ja.toJSONString());
            JSONObject jo = ja.getJSONObject(0);
            int status = jo.getIntValue("status");
            if (status != ContentService.SUB_STATUS_SUCCESS) {
                rst = false;
                pdts.updateSubFail(userid, key, src, null);
            }
        } catch (Exception e) {
            log.error("提交预分发文件结果解析异常：" + tmpResult, e);
        }
//        Element rootElement = DocumentHelper.parseText(tmpResult).getRootElement();
//        tmpResult = rootElement.elementText("result");
//        if ("FAILURE".equals(tmpResult)) {
//            rst = false;
//        }
//        if (!rst) {
//            this.pdts.deleteDate(userid, key);
//        }
        return rst;
    }

    private Map<String, String> subParams(String domaintag, String userid, String cip) {
        // 调用文件分发系统的接口
        // prop: 0正常，9全网分发
        // bussid：用户ID,userid
        // priority：分发优先级0--100
        // gslbdomain:用户访问CDN的域名
        // cip：用户提交分发任务的IP
        // user：acloud,固定给第三方分发通用名
		
		String appkey = ApiHelper.getAppkey();
		PreDistParamExample example = new PreDistParamExample();
		PreDistParamExample.Criteria c = example.createCriteria();
		c.andAppkeyEqualTo(appkey);
		List<PreDistParam> paramList = preDistParamMapper.selectByExample(example);
		// 默认分发比例9
		String prop = "9";
		// 默认分发优先级50
		int priority = 50;
		if(!paramList.isEmpty()) {
			PreDistParam param = paramList.get(0);
			prop = param.getProp();
			priority = param.getPriority();
		}
		Map<String, String> params = new HashMap<String, String>();
		params.put("prop", prop);
		params.put("bussid", userid);
		params.put("priority", String.valueOf(priority));
		CoopDomain coopDomain = domainService.selectByUseridAndDomaintag(userid, domaintag);
		if (coopDomain == null) {
			throw new IllegalArgumentException("查询参数错误:用户标识(userid)和域名标识(domaintag)不匹配");
		}
		params.put("gslbdomain", coopDomain.getSubDomain());
		params.put("cip", cip);
		params.put("user", "acloud");
		return params;
    }

    private String subxml(String key, String src, String md5, Integer userid) {
    
        // 生成xml
        Document document = DocumentHelper.createDocument();
        // 设置xml的字符编码
        document.setXMLEncoding("UTF-8");
        Element ccscElement = document.addElement("ccsc");
        // 后台存储用户的任务标识,为避免不同用户的key标识重复,将userid+key作为lecloud系统对此次任务的标识
        Element item_idElement = ccscElement.addElement("item_id");
        item_idElement.addAttribute("value", userid + "_" + key);
        Element source_pathElement = item_idElement.addElement("source_path");
        source_pathElement.addText(src);
        if (StringUtil.notEmpty(md5)) {
            Element md5Element = item_idElement.addElement("md5");
            md5Element.addText(md5);
        }
        // 将xml对象转换成流
        // InputStream in = new
        // ByteArrayInputStream(document.asXML().getBytes("utf-8"));
        String xmlData = document.asXML();
        log.info("xml内容： --start--" + document.asXML() + "--End--");
        return xmlData;
    }
    
    /**
     * 删除分发文件的分发
     * 
     * @method: ContentService  deleteFile
     * @param src 经过url编码的文件源地址
     * @return
     * @throws IOException  boolean
     * @createDate： 2014年11月11日
     * @2014, by chenyuxin.
     */
    public boolean deleteFile(Integer userid, String domaintag, String src, String key, String md5) throws IOException{
        src = RegExpValidatorUtil.replaceBlank(src);
        PreDistTask pdt = this.pdts.selectByOutkeyMd5Src(userid, key, md5, src);
        String storePath = null;
        if (pdt == null) {
            storePath = this.getShortPathBySrc(userid.toString(), domaintag, src);
        } else {
            storePath = pdt.getStorePath();
        } 
        Map<String, String> map = new HashMap<String, String>();
        map.put("user", "acloud");
        boolean flag = false;
        if(StringUtils.hasLength(storePath)){
            map.put("file", storePath);
            // 删除物理文件
            flag = HttpClientUtil.getDelete(DELETE_FILE_URI, map, HttpClientUtil.UTF_8);
            if(flag) {
                Map<String, String> params = new HashMap<String, String>();
                String[] srcArr = src.substring(7).split("/", 2);
                CoopDomain cd = this.domainService.selectByUseridAndDomaintag(userid.toString(), domaintag);
                params.put("domain", cd.getSubDomain());
                params.put("uri", "/" + srcArr[1]);
                params.put("delete", "1");
                // 删除缓存中的记录
                HttpClientUtil.get(GET_LECLOUD_FILE_URI, params, HttpClientUtil.UTF_8);
                this.pdts.deleteTask(userid, src, key, md5);
            }
        }
        return flag;
    }
    
    /**
     * 通过资源地址获取letv对应的短地址
     * 
     * @method: ContentService  getShortPathBySrc
     * @param src
     * @return
     * @throws IOException  String
     * @createDate： 2014年10月29日
     * @2014, by chenyuxin.
     */
    public String getShortPathBySrc(String userid, String domaintag, String src) throws IOException {
        CoopDomain cd = this.domainService.selectByUseridAndDomaintag(userid, domaintag);
        // src必须包含"http://"
        Assert.isTrue(src != null && src.length() > 7 && "http://".equalsIgnoreCase(src.substring(0, 7)), "文件源地址(src)不能为空且必须包含\"http://\"");
        String[] srcArr = src.substring(7).split("/", 2);
        if (srcArr.length < 2 || "".equals(srcArr[1])) {
            throw new IllegalArgumentException("文件源地址(src)缺少要查询的资源路径");
        }
        // src按"/"分割，前一部分为用户的domian，后一部分为文件的路径
        // http://log.letvcdn.letv.com/ext/mapping?domain=video.cztv.com&uri=/video/rzx/201309/17/1379379018229.mp4
        Map<String, String> params = new HashMap<String, String>();
        if (cd == null) {
            params.put("domain", srcArr[0]);
        } else {
            params.put("domain", cd.getSubDomain());
        }
        params.put("uri", "/" + srcArr[1]);
        // 获取资源短地址
        String queryResult = HttpClientUtil.get(GET_LECLOUD_FILE_URI, params, HttpClientUtil.UTF_8);
        
        // 分发成功返回的结果：{ "status": 1, "result":"88\/25\/60\/cztvcom\/0\/video\/rzx\/201309\/17\/1379379018229.mp4","filename": "video\/rzx\/201309\/17\/1379379018229.mp4" }
        JSONObject jo = JSONObject.parseObject(queryResult);
        // 当分发成功后,status为1,result对应的是lecloud使用的key;失败或正在分发中，status为0,result为空字符串
        Integer status = jo.getInteger("status");
        if (status == 1) {
            String result = jo.getString("result").replace("\\", "");
            log.info("ShortPathBySrc-------------" + result);
            return result;
        }
        return null;
    }
    
    /**
     * 通过outkey获取用户的回调接口
     * 
     * @method: ContentService  getClientCallbackUri
     * @param outkey
     * @return  String
     * @createDate： 2014年10月30日
     * @2014, by chenyuxin.
     */
    public String getClientCallbackUri(String outkey){
    	String uri = null;
    	PreDistTask task = pdts.selectByOutkey(outkey);
    	if(task != null && task.getAppkey() != null) {
    		PreDistParamExample example = new PreDistParamExample();
    		PreDistParamExample.Criteria c = example.createCriteria();
    		c.andAppkeyEqualTo(task.getAppkey());
    		List<PreDistParam> params = preDistParamMapper.selectByExample(example);
    		if(params.size() == 1) {
    			uri = params.get(0).getCallback();
    		}
    	}
    	return uri;
    }
    
    public String getClientCallbackUriByAppkey(String appkey){
    	String uri = null;
    	if(org.apache.commons.lang.StringUtils.isNotBlank(appkey)) {
    		PreDistParamExample example = new PreDistParamExample();
    		PreDistParamExample.Criteria c = example.createCriteria();
    		c.andAppkeyEqualTo(appkey);
    		List<PreDistParam> params = preDistParamMapper.selectByExample(example);
    		if(params.size() == 1) {
    			uri = params.get(0).getCallback();
    		}
    	}
    	return uri;
    }
}
