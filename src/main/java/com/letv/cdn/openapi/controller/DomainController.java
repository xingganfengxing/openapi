package com.letv.cdn.openapi.controller;

import java.io.IOException;
import java.text.ParseException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.letv.cdn.openapi.pojo.CoopDomain;
import com.letv.cdn.openapi.pojo.ApiDomain;
import com.letv.cdn.openapi.service.ApiDomainService;
import com.letv.cdn.openapi.service.DomainService;
import com.letv.cdn.openapi.service.UserService;
import com.letv.cdn.openapi.utils.LogSystemStormShardedJedisPool;
import com.letv.cdn.openapi.utils.RegExpValidatorUtil;
import com.letv.cdn.openapi.utils.ApiHelper;
import com.letv.cdn.openapi.utils.ErrorMsg;
import com.letv.cdn.openapi.web.ResponseJson;
import com.letv.cdn.openapiauth.annotation.OpenapiAuth;

/**
 * 加速域名相关接口<br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年10月24日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
@Controller
@RequestMapping("/cdn/domain")
public class DomainController {

    private static final Logger log = LoggerFactory.getLogger(DomainController.class);
    @Resource
    DomainService cds;
    @Resource
    UserService us;
    
    @Resource
	ApiDomainService apiDomainService;
    
    /**
     * 添加加速域名
     * <b>Method</b>: DomainController#add <br/>
     * <b>Create Date</b> : 2014年10月24日
     * @author Chen Hao
     * @param request
     * @param response
     * @param domaintag
     * @param domain
     * @param source
     * @param remark
     * @param userid
     * @param sign
     * @return
     * @throws IOException 
     */
    @OpenapiAuth
    @RequestMapping(method = RequestMethod.POST)
    public ResponseJson add(@RequestParam(value = "domaintag", required = true) String domaintag,
                            @RequestParam(value = "domain",    required = true) String domain,
                            @RequestParam(value = "source",    required = true) String source,
                            @RequestParam(value = "remark",    required = true) String remark,
                            @RequestParam(value = "userid",    required = true) String userid, 
                            @RequestParam(value = "ver",       required = true) String ver,
                            @RequestParam(value = "sign",      required = true) String sign) throws IOException {
        Assert.isTrue(domaintag.length() <= 128, "The parameter domaintag must be less than 128 characters.");
        Assert.isTrue(domain.length() <= 128, "The parameter domain must be less than or equal to 128 characters.");
        Assert.isTrue(source.length() <= 128, "The parameter source must be less than or equal to 128 characters.");
        Assert.isTrue(remark.length() <= 64, "The parameter remark must be less than or equal to 64 characters.");
        Assert.isTrue(userid.length() >= 6, "The parameter userid must be greater than or equal to 6 characters.");
        
        CoopDomain cd = new CoopDomain();
        if (userid.equals("137587") || userid.equals("136098") || userid.equals("256567")) {
            // 对ucloud用户做特殊处理
            cd.setAction(CoopDomain.ACTION_ENABLE_SMALL_FILE_MOVE);
        } else {
            cd.setAction(CoopDomain.ACTION_ENABLE_BIG_FILE);
        }
        cd.setOntest((short) 0);
        cd.setUsertag(domaintag);
        cd.setDomain(domain);
        cd.setRemark(remark);
        cd.setSource(source);
        cd.setUserid(userid);
        
        ApiDomain apiDomain = new ApiDomain(Integer.valueOf(userid), domaintag, domain, source, cd.getAction(), Short.valueOf("1"), remark);
        
        if (userid.equals("137587") || userid.equals("136098") || userid.equals("256567")) {
            // 对ucloud用户做特殊处理
        	apiDomain.setServiceType(CoopDomain.ACTION_ENABLE_SMALL_FILE_MOVE);
        } else {
        	apiDomain.setServiceType(CoopDomain.ACTION_ENABLE_BIG_FILE);
        }
        
        if (apiDomainService.insert(apiDomain)) {
            this.cds.enable();
            return ResponseJson.createdNoCache(cd);
        } else {
            return ResponseJson.internalServerError("添加域名失败");
        }
    }
    
    /**
     * @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}", method = RequestMethod.PUT)
    public ResponseJson update(HttpServletRequest req,
                               @RequestBody String body,
                               @PathVariable String userid, 
                               @PathVariable String domaintag,
                               @RequestParam(value = "domain", required = false) String domain,
                               @RequestParam(value = "source", required = false) String source,
                               @RequestParam(value = "remark", required = false) String remark,
                               @RequestParam(value = "userid", required = false) String puserid,
                               @RequestParam(value = "ver",    required = false) String ver,
                               @RequestParam(value = "sign",   required = false) String sign) {
    	
        Assert.notNull(userid, "userid is not present");
        Assert.notNull(ver, "ver is not present");
        Assert.notNull(sign, "sign is not present");
        
        CoopDomain cd = this.cds.selectByUseridAndDomaintag(userid, domaintag);
        if(cd == null) {
        	return ResponseJson.notFound();
        }
        log.info("domain:{}", domain);
        log.info("cd:{}", ((JSONObject)JSON.toJSON(cd)).toJSONString());
        if (domain != null) {
            cd.setDomain(domain);
        }
        if (source != null) {
            cd.setSource(source);
        }
        if (remark != null) {
            cd.setRemark(remark);
        }
        if (this.cds.updateByDomaintag(cd)) {
            return ResponseJson.okWithNoCache(cd);
        } else {
            return ResponseJson.internalServerError("修改域名失败");
        }
    }
    */
    
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}", method = RequestMethod.POST)
    public ResponseJson updatePost(HttpServletRequest req,
            @RequestBody String body,
            @PathVariable String userid, 
            @PathVariable String domaintag,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "userid", required = false) String puserid,
            @RequestParam(value = "ver",    required = false) String ver,
            @RequestParam(value = "sign",   required = false) String sign) throws IOException {
        Assert.notNull(userid, "userid is not present");
        Assert.notNull(ver, "ver is not present");
        Assert.notNull(sign, "sign is not present");
        
        CoopDomain cd = this.cds.selectByUseridAndDomaintag(userid, domaintag);
        if (cd  == null) {
            return ResponseJson.notFound();
        }
        if (domain != null) {
            cd.setDomain(domain);
        }
        if (source != null) {
            cd.setSource(source);
        }
        if (remark != null) {
            cd.setRemark(remark);
        }
        
        ApiDomain apiDomain = this.apiDomainService.selectByUseridAndDomaintag(userid, domaintag);
    	if(apiDomain == null) {
        	return ResponseJson.notFound();
        }
        log.info("domain:{}", apiDomain);
        log.info("cd:{}", ((JSONObject)JSON.toJSON(apiDomain)).toJSONString());
        if (apiDomain != null) {
            apiDomain.setDomain(domain);
        }
        if (source != null) {
            apiDomain.setSource(source);
        }
        if (remark != null) {
            apiDomain.setRemark(remark);
        }
        if (this.apiDomainService.update(apiDomain)) {
            this.cds.enable();
            return ResponseJson.okWithNoCache(cd);
        } else {
            return ResponseJson.internalServerError("修改域名失败");
        }
    }
    
    
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}", method = RequestMethod.GET)
    public ResponseJson query(@PathVariable String userid, 
                              @PathVariable String domaintag,
                              @RequestParam(value = "userid", required = true) String puserid,
                              @RequestParam(value = "ver",    required = true) String ver,
                              @RequestParam(value = "sign",   required = true) String sign) {
        CoopDomain cd = this.cds.selectByUseridAndDomaintag(userid, domaintag);
        if (cd == null) {
            return ResponseJson.notFound();
        }
        return ResponseJson.okWithNoCache(cd);
    }
    
    /**
     * 添加加速域名
     * 2014-12-19
     * @author gao.jun
     * @param body 请求消息体
     * @param auth 加密权限参数
     * @return 
     * @since 0.2
     * @throws IOException
     * 
     */
    @OpenapiAuth
    @RequestMapping(method = RequestMethod.POST, headers={"Lecloud-api-version=0.2"})
    public ResponseJson add(@RequestBody String body,
                            @RequestHeader("Authorization") String auth,
                            @RequestHeader(value = "Accept",required = false) String accept) throws IOException {
    	
    	if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
    	
    	String userid = ApiHelper.getUserid(auth);
    	JSONObject jsonObj = JSONObject.parseObject(body);
    	String domaintag = jsonObj.getString("domaintag");
    	String domain = jsonObj.getString("domain");
    	String source = jsonObj.getString("source");
    	// 分发类型，目前只能为9-大文件预分发
    	String type = jsonObj.getString("type");
    	String remark = jsonObj.getString("remark");
    	short enabled = (short)(jsonObj.getString("enabled") == null ? 1 : Short.valueOf(jsonObj.getString("enabled").toString()));
    	
    	Assert.notNull(domaintag, "The parameter domaintag is not present");
        Assert.isTrue(domaintag.length() <= 128, "The parameter domaintag must be less than 128 characters.");
        Assert.notNull(domain, "The parameter domain is not present");
        Assert.isTrue(domain.length() <= 128, "The parameter domain must be less than or equal to 128 characters.");
        Assert.isTrue(RegExpValidatorUtil.validateDomain(domain), "The parameter domain is invalid.");
        Assert.notNull(source, "The parameter source is not present");
        Assert.isTrue(source.length() <= 128, "The parameter source must be less than or equal to 128 characters.");
        Assert.isTrue(RegExpValidatorUtil.validateDomain(source), "The parameter source is invalid.");
        Assert.isTrue(remark == null || remark.length() <= 64, "The parameter remark must be less than or equal to 64 characters.");
        Assert.isTrue(CoopDomain.ACTION_ENABLE_BIG_FILE.toString().equals(type), "The parameter type must be 9.");
        Assert.isTrue(userid.length() >= 6, "The parameter userid must be greater than or equal to 6 characters.");
        
        Assert.isTrue(apiDomainService.uniqueCheck(domaintag, domain),"The parameter domaintag or the parameter domain is already exist.");
        
        ApiDomain apiDomain = new ApiDomain(Integer.valueOf(userid), domaintag, domain, source, Short.valueOf(type), enabled, remark);
        
        if (userid.equals("137587") || userid.equals("136098") || userid.equals("256567")) {
            // 对ucloud用户做特殊处理
        	apiDomain.setServiceType(CoopDomain.ACTION_ENABLE_SMALL_FILE_MOVE);
        } else {
        	apiDomain.setServiceType(CoopDomain.ACTION_ENABLE_BIG_FILE);
        }
        
        if (this.apiDomainService.insert(apiDomain)) {
            // 往日志系统的storm集群的redis添加域名记录
            ShardedJedisPool pool = LogSystemStormShardedJedisPool.getLogSystemStormShardedJedisPool();
            ShardedJedis redis = pool.getResource();
            String key = LogSystemStormShardedJedisPool.KEY_DOMAINTAG_PREFIX.concat(apiDomain.getDomaintag());
            redis.set(key, apiDomain.getDomain());
            pool.returnResource(redis);
            
            this.cds.enable();
            return ResponseJson.createdNoCache(apiDomain);
        } else {
        	return ResponseJson.internalServerError(ErrorMsg.OPERATION_FAILURE);
        }
    }
    
    /**
     * 修改域名
     * 2014-12-19
     * @author gao.jun
     * @param userid 用户id
     * @param domaintag 域名标识
     * @param body 请求消息体
     * @param auth 加密权限参数
     * @return
     * @throws IOException 
     * @since 0.2
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}", method = RequestMethod.PUT, headers={"Lecloud-api-version=0.2"})
    public ResponseJson update(@PathVariable String userid, 
            @PathVariable String domaintag,
            @RequestBody String body,
            @RequestHeader("Authorization") String auth,
            @RequestHeader(value = "Accept",required = false) String accept) throws IOException {
    	
    	if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
    	JSONObject jsonObj = JSONObject.parseObject(body);
    	String domain = jsonObj.getString("domain");
    	String source = jsonObj.getString("source");
    	String remark = jsonObj.getString("remark");
    	
    	Assert.isTrue(domain == null || domain.length() <= 128, "The parameter domain must be less than or equal to 128 characters.");
        Assert.isTrue(domain == null || RegExpValidatorUtil.validateDomain(domain), "The parameter domain is invalid.");
        Assert.isTrue(source == null || source.length() <= 128, "The parameter source must be less than or equal to 128 characters.");
        Assert.isTrue(source == null || RegExpValidatorUtil.validateDomain(source), "The parameter source is invalid.");
        Assert.isTrue(remark == null || remark.length() <= 64, "The parameter remark must be less than or equal to 64 characters.");
    	
    	ApiDomain apiDomain = this.apiDomainService.selectByUseridAndDomaintag(userid, domaintag);
    	if(apiDomain == null) {
        	return ResponseJson.notFound();
        }
        log.info("domain:{}", apiDomain);
        log.info("cd:{}", ((JSONObject)JSON.toJSON(apiDomain)).toJSONString());
        if (apiDomain != null) {
            apiDomain.setDomain(domain);
        }
        if (source != null) {
            apiDomain.setSource(source);
        }
        if (remark != null) {
            apiDomain.setRemark(remark);
        }
        if (this.apiDomainService.update(apiDomain)) {
        	this.cds.enable();
            return ResponseJson.okWithNoCache(apiDomain);
        } else {
        	return ResponseJson.internalServerError(ErrorMsg.OPERATION_FAILURE);
        }
    	
    }
    
    /**
     * 获取域名配置
     * @author gao.jun
     * @param userid 用户id
     * @param domaintag 域名标识
     * @return
     * @since 0.2
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}", method = RequestMethod.GET, headers={"Lecloud-api-version=0.2"})
    public ResponseJson query(@PathVariable String userid, 
                              @PathVariable String domaintag,
                              @RequestHeader(value = "Accept",required = false) String accept) {
    	if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
    	ApiDomain domain = this.apiDomainService.selectByUseridAndDomaintag(userid,
				domaintag);
		if (domain == null) {
			return ResponseJson.notFound();
		}
		return ResponseJson.okWithNoCache(domain);
    }
    
    /**
     * 禁用域名配置
     * @author gao.jun
     * @param userid 用户id
     * @param domaintag 域名标识
     * @return
     * @since 0.2
     * @throws IOException 
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}/flag", method = RequestMethod.PUT, headers={"Lecloud-api-version=0.2"})
    public ResponseJson disableOrEnable(@PathVariable String userid, 
                              @PathVariable String domaintag,
                              @RequestBody String body,
                              @RequestHeader(value = "Accept",required = false) String accept) throws IOException {
    	if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
    	
    	JSONObject paramObj = JSONObject.parseObject(body);
    	Short flag = paramObj.getShort("flag");
    	Assert.isTrue(CoopDomain.FLAG_DISABLE.equals(flag)
    			|| CoopDomain.FLAG_ENABLE.equals(flag), "The parameter domaintag must be 1 or 0.");
    	
    	ApiDomain domain = this.apiDomainService.selectByUseridAndDomaintag(userid,domaintag);
		if (domain == null) {
			return ResponseJson.notFound();
		}
        if(apiDomainService.enableOrDisable(domaintag, flag)) {
        	this.cds.enable();
        	JSONObject jsonObj = new JSONObject();
        	jsonObj.put("flag", flag);
        	return ResponseJson.okWithNoCache(jsonObj);
        } else {
            return ResponseJson.internalServerError(ErrorMsg.OPERATION_FAILURE);
        }
    }
    
    /**
     * 根据域名标识删除加速域名<br>
     * 只有禁用状态的域名配置可以删除，企图删除一个启用状态的域名时会返回403
     * <br>
     * 2014年12月19日
     * @author gao.jun
     * @param userid 用户id
     * @param domaintag 域名标识
     * @return
     * @throws IOException
     */
    @OpenapiAuth
    @RequestMapping(value = "/{userid}_{domaintag}", method = RequestMethod.DELETE, headers={"Lecloud-api-version=0.2"})
    public ResponseJson delete(@PathVariable String userid, 
                              @PathVariable String domaintag,
                              @RequestHeader(value = "Accept",required = false) String accept) throws IOException {
        if(!ApiHelper.checkAcceptHeader(accept)) {
        	return ResponseJson.notAcceptable();
        }
        ApiDomain domain = this.apiDomainService.selectByUseridAndDomaintag(userid,
				domaintag);
        if (domain == null) {
            return ResponseJson.notFound();
        }else if(!CoopDomain.ACTION_DISABLE.equals(domain.getEnabled())) {
        	log.error("域名[".concat(domaintag).concat("]为启用状态，无法删除"));
        	return ResponseJson.badRequest(ErrorMsg.DOMAIN_IS_ENABLED);
        }
        if(apiDomainService.deleteByDomaintag(domaintag)) {
        	this.cds.enable();
        	JSONObject jsonObj = new JSONObject();
        	jsonObj.put("msg", "ok");
        	return ResponseJson.okWithNoCache(jsonObj);
        } else {
            return ResponseJson.internalServerError(ErrorMsg.OPERATION_FAILURE);
        }
    }
    
    /**
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
	@OpenapiAuth
    @RequestMapping(value = "/cdn/domain",method = RequestMethod.GET, headers={"Lecloud-api-version=0.2"})
	public ResponseJson pagedQuery(@RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Integer userid,
            @RequestParam(required = false) String domaintag,
			@RequestParam(required = false) String domain,
			@RequestParam(required = false) String source,
			@RequestParam(required = false) short enabled,
			@RequestParam int page, @RequestParam int rows) throws ParseException {
		
		Assert.isTrue(RegExpValidatorUtil.isDate(startTime,"yyyy-MM-dd hh:mm:ss"),"The parameter startTime isn't valid.");
		Assert.isTrue(RegExpValidatorUtil.isDate(endTime,"yyyy-MM-dd hh:mm:ss"),"The parameter endTime isn't valid.");
		Assert.isTrue(page >= 1, "The parameter page must larger or equal 1.");
		Assert.isTrue(rows >= 1, "The parameter rows must larger or equal 1.");
		
		return ResponseJson.okWithNoCache(apiDomainService.pagedQuery(startTime, endTime, userid, domaintag, 
				domain, source, enabled, page, rows));
	}
}
