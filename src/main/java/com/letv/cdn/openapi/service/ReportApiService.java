package com.letv.cdn.openapi.service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * TODO: report 调用 Api接口ReportApiService
 * @author liuchangfu
 * @createDate 2015年1月4日
 */
@Service
public class ReportApiService {
	
	
	
	/**
	 * 根据逗号分隔获取tag集合
	 * @param tag
	 * @return
	 */
	public List<String> getTagList(String tag) {
		List<String> tagList = new ArrayList<String>();
		if (tag.contains(",")) {
			String[] tags = tag.split(",");
			tagList = Arrays.asList(tags);
		}else{
			String[] tags = new String[]{tag};
			tagList = Arrays.asList(tags);
		}
		
		return tagList;
	}

}
