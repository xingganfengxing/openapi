package com.letv.cdn.openapi.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.letv.cdn.openapi.common.StringUtil;
import com.letv.cdn.openapi.dao.domain.CoopDomainMapper;
import com.letv.cdn.openapi.dao.openapi.PreDistTaskMapper;
import com.letv.cdn.openapi.pojo.CoopDomainExample;
import com.letv.cdn.openapi.pojo.PreDistTaskExample;
import com.letv.cdn.openapi.pojo.contentterms.Content;
import com.letv.cdn.openapi.utils.Env;
import com.letv.cdn.openapi.web.HttpClientUtil;

/**
 * 临时的向cdn提交任务的service。136098为ucloud的id
 * <br>
 * 2015年1月8日
 * @author gao.jun
 *
 */
@Service
public class TempFileSubmitService {
	
	private static Logger log = LoggerFactory.getLogger(TempFileSubmitService.class);
	
	@Resource
	PreDistTaskMapper taskMapper;
	
	@Resource
	CoopDomainMapper cdMapper;
	
	public void tempSubmit() {
    	Map<String, List<Content>> map;
    	int count = 0;
		try {
			map = getContent();
			for(Map.Entry<String, List<Content>> entry : map.entrySet()) {
				log.info("domain: " + entry.getKey());
				List<Content> contents = entry.getValue();
				for(int i = 0,size = contents.size(); i < size; ++i) {
					Content c = contents.get(i);
					log.info(i + ": key: " + c.getKey() + " : src: " + c.getSrc());
				}
				count += contents.size();
				HttpClientUtil.postXml(Env.get("submit_file_uri"),subParams(entry.getKey()),
						subxml(contents, 136098), HttpClientUtil.UTF_8);
	    	}
			log.info("count: " + count);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    private Map<String, String> subParams(String coopDomain) {
    	Map<String, String> params = new HashMap<String, String>();
		params.put("prop", "9");
		params.put("bussid", "136098");
		params.put("priority", "50");
		params.put("gslbdomain", coopDomain);
		params.put("cip", "10.58.132.91");
		params.put("user", "acloud");
		return params;
    }
    
    private Map<String,List<Content>> getContent() throws IOException {
    	Map<String,List<Content>> map = new HashMap<String,List<Content>>();
    	File file = new File(ContentService.class.getClassLoader().getResource("task_trying.trying").getPath());
    	FileReader fr = null;
    	BufferedReader br = null;
		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
	    	String line = br.readLine(); 
	    	while(StringUtils.isNotEmpty(line)) {
	    		if(line.startsWith("#")) {
	    			line = br.readLine(); 
	    			continue;
	    		}
	    		String[] strArr = line.split("	");
	    		if(strArr.length == 2) {
	    			String outkey = "136098_" + strArr[0];
	    			
	    			PreDistTaskExample example = new PreDistTaskExample();
	    			example.createCriteria().andUseridEqualTo(136098).andOutkeyEqualTo(outkey);
	    			String domaintag = taskMapper.selectByExample(example).get(0).getDomaintag();
	    			if(StringUtils.isNotEmpty(domaintag)) {
	    				CoopDomainExample domainExample = new CoopDomainExample();
	    				domainExample.createCriteria().andUsertagEqualTo(domaintag);
	    				
	    				String domain = cdMapper.selectByExample(domainExample).get(0).getDomain();
	    				
	    				if(StringUtils.isNotEmpty(domain)) {
	    					
	    					List<Content> curList = map.get(domain);
	    					if(curList == null) {
	    						curList = new ArrayList<Content>();
	    					}
	    					curList.add(new Content(strArr[1], outkey, null, null));
	    					
	    					map.put(domain, curList);
	    				}
	    			}
	    		}
	    		
	    		line = br.readLine(); 
	    	}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			fr.close();
			fr = null;
		}
    	
    	return map;
    }
    
    private String subxml(List<Content> contentApiList, Integer userid) {

		// 生成xml
		Document document = DocumentHelper.createDocument();
		// 设置xml的字符编码
		document.setXMLEncoding("UTF-8");
		Element ccscElement = document.addElement("ccsc");
		for (Content contentApi : contentApiList) {
			// 后台存储用户的任务标识,为避免不同用户的key标识重复,将userid+key作为lecloud系统对此次任务的标识
			Element item_idElement = ccscElement.addElement("item_id");
			item_idElement.addAttribute("value",userid + "_" + contentApi.getKey());
			Element source_pathElement = item_idElement.addElement("source_path");
			source_pathElement.addText(contentApi.getSrc());
			if (StringUtil.notEmpty(contentApi.getMd5())) {
				Element md5Element = item_idElement.addElement("md5");
				md5Element.addText(contentApi.getMd5());
			}
		}
		String xmlData = document.asXML();
		return xmlData;
	}
}
