package com.letv.cdn.openapi.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.letv.cdn.openapi.dao.openapi.DomainExtensionMapper;
import com.letv.cdn.openapi.pojo.DomainExtension;
import com.letv.cdn.openapi.pojo.DomainExtensionExample;

@Service
public class DomainExtensionService {
	
	private static final Logger log = LoggerFactory.getLogger(DomainExtensionService.class);
	
	@Resource
    DomainExtensionMapper domainExtensionMapper;
	
	private Map<String, String> filterDomain = new HashMap<String, String>();
	
	@PostConstruct
	public void init() {
		Map<String, String> newMap = new HashMap<String, String>();
		for(DomainExtension ext : domainExtensionMapper.selectByExample(new DomainExtensionExample())) {
			newMap.put(ext.getDomaintag(), ext.getFilterDomain());
		}
		filterDomain = newMap;
		log.info("Cached domain extension finish...");
	}
	
	@PreDestroy
	public void destory() {
		filterDomain = null;
	}
	
	public String getFilterDomain(String domaintag) {
		return filterDomain.get(domaintag);
	}
}
