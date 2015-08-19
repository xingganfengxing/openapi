package com.letv.cdn.openapi.service;

import org.springframework.stereotype.Service;

import com.lecloud.commons.logging.annotation.Log;

/**
 * <br>
 * <b>Project</b> : openapi<br>
 * <b>Create Date</b> : 2014年12月17日<br>
 * <b>Company</b> : 乐视云计算<br>
 * <b>Copyright @ 2014 letv – Confidential and Proprietary</b><br>
 * 
 * @author Chen Hao
 */
@Service
public class LogTestService{
    
    @Log
    public void ltservice() throws InterruptedException {
        Thread.sleep(1234);
    }
}

	