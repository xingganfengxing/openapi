package com.letv.cdn.openapi.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.letv.cdn.openapi.utils.DateUtil;

/**
 * User: lichao Date: 2010-11-20 Time: 16:27:22
 */
public class HttpClientUtil{
    
    public static final String UTF_8 = "UTF-8";
    
    public static final String GBK = "GBK";
    
    private static DefaultHttpClient httpclient;
    
    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);
    
    static {
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.connection.timeout", 180000);
        params.setParameter("http.socket.timeout", 180000);
        params.setParameter("http.protocol.element-charset", UTF_8);
        params.setParameter("http.protocol.content-charset", UTF_8);
        
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
        httpclient = new DefaultHttpClient(manager, params);
    }
    
    public static String get(String uri, String responseEncode) throws IOException {
    
        HttpGet httpGet = new HttpGet(uri);
        long start = DateUtil.now();
        log.info("GET请求 ,请求的URL: {}  cost: {}ms", new Object[] { uri, (DateUtil.now() - start)});
        HttpResponse httpResponse = httpclient.execute(httpGet);
        
        String charset = responseEncode == null ? EntityUtils.getContentCharSet(httpResponse.getEntity())
                : responseEncode;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("GET请求 ,返回的状态码：{} --- 返回的内容: {}", new Object[] { statusCode, s});
        httpResponse.getEntity().consumeContent();
        return s;
    }
    
    public static HttpResponse getResponse(String uri, String responseEncode) throws IOException {
    
        HttpGet httpGet = new HttpGet(uri);
        long start = DateUtil.now();
        log.info("GET请求 ,请求的URL:" + uri);
        HttpResponse httpResponse = httpclient.execute(httpGet);
        
        String charset = responseEncode == null ? EntityUtils.getContentCharSet(httpResponse.getEntity())
                : responseEncode;
        // String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        // httpResponse.getEntity().consumeContent();
        //log.info("{} cost: {}ms --> {}", new Object[]{uri, (DateUtil.now() - start), s});
        return httpResponse;
    }
    
    public static String getRedirectUrl(String uri, String responseEncode) throws IOException {
    
        HttpGet httpGet = new HttpGet(uri);
        httpGet.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        HttpResponse httpResponse = httpclient.execute(httpGet);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        httpResponse.getEntity().consumeContent();
        if (statusCode == 301 || statusCode == 302) {
            return httpResponse.getFirstHeader("Location").getValue();
        } else {
            return null;
        }
        
    }
    
    public static String get(String url, Map<String, String> params, String encode, String responseEncode)
            throws IOException {
    
        return get(addParams(url, params, encode), responseEncode);
    }
    
    public static String get(String url, Map<String, String> params, String responseEncode)
            throws IOException {
    
        return get(addParams(url, params), responseEncode);
    }
    
    public static String post(String uri, Map<String, String> m, String encoding) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(500000).build();
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(getParamList(m), encoding);
        log.info("POST请求,请求的URL: {} --- 请求的参数{}", new Object[]{uri, EntityUtils.toString(entity)});
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setConfig(requestConfig);
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(httpPost);
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        httpResponse.getEntity().consumeContent();
        if (statusCode == HttpStatus.SC_OK) {
            return s;
        } else {
            httpPost.abort();
            return null;
        }
    }
    
    public static Boolean getDelete(String uri, Map<String, String> m, String encoding) throws IOException {
        
        String url = addParams(uri, m, encoding);
        log.info("GET请求,请求的URL: {}", url);
        HttpGet httpGet = new HttpGet(url);
        HttpResponse httpResponse = httpclient.execute(httpGet);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        httpResponse.getEntity().consumeContent();
        if (statusCode == HttpStatus.SC_OK) {
            return true;
        } else {
            return false;
        }
    }
    
    public static String post(String uri, List<NameValuePair> m, String encoding) throws IOException {
    
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(m, encoding);
        log.info("POST请求,请求的URL: {} --- 请求的参数{}", new Object[]{uri, EntityUtils.toString(entity)});
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(httpPost);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        if (statusCode == HttpStatus.SC_OK) {
            httpResponse.getEntity().consumeContent();
            return s;
        } else {
            httpResponse.getEntity().consumeContent();
            httpPost.abort();
            return null;
        }
    }
    
    public static String postXml(String uri, Map<String, String> params, String xmlData, String encoding)
            throws IOException {
        StringEntity entity = new StringEntity(xmlData, HttpClientUtil.UTF_8);
        //InputStreamEntity entity = new InputStreamEntity(in, in.available());
        String url = addParams(uri, params, HttpClientUtil.UTF_8);
        log.info("POST请求,请求的URL: {} --- 请求的参数{}", new Object[]{url, EntityUtils.toString(entity)});
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "text/xml;charset=utf-8");
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(httpPost);
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        if (statusCode == HttpStatus.SC_OK) {
            httpResponse.getEntity().consumeContent();
            return s;
        } else if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN){
            httpResponse.getEntity().consumeContent();
            return "403";
        } else {
            httpResponse.getEntity().consumeContent();
            httpPost.abort();
            return null;
        }
    }
    
    public static HttpResponse postContent(String uri, Map<String, String> m, String encoding) throws IOException {
    
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(getParamList(m), encoding);
        log.info("POST请求,请求的URL: {} --- 请求的参数{}", new Object[]{uri, EntityUtils.toString(entity)});
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(entity);
        return httpclient.execute(httpPost);
    }
    
    public static List<NameValuePair> getParamList(Map<String, String> params) {
    
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            qparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return qparams;
    }
    
    public static String addParams(String baseURI, Map<String, String> params, String encode) {
        
        if (baseURI == null)
            return null;
        String paramStr = URLEncodedUtils.format(getParamList(params), encode);
        char sp = (baseURI.indexOf('?') != -1) ? '&' : '?';
        return baseURI + sp + paramStr;
    }
    
    public static String addParams(String baseURI, Map<String, String> params) {
        
        if (baseURI == null)
            return null;
        StringBuilder paramStr = new StringBuilder();
        for(Map.Entry<String, String> entry:params.entrySet()){
            if(paramStr.length() > 0){
                paramStr.append("&");
            }
            paramStr.append(entry.getKey()).append("=").append(entry.getValue());
        }
        char sp = (baseURI.indexOf('?') != -1) ? '&' : '?';
        return baseURI + sp + paramStr;
    }
    
    public static String getPostRequestString(HttpServletRequest request, String defaultCharset) throws IOException{
        
        if(request.getContentLength() == 0){
            return null;
        }
        
        int i = request.getContentLength();
        if (i < 0) {
            i = 4096;
        }
        String charset = request.getCharacterEncoding();
        if (charset == null) {
            charset = defaultCharset;
        }
        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }
        String queryString = request.getQueryString();
        Reader reader = new InputStreamReader(new ByteArrayInputStream(queryString.getBytes()), charset);
        CharArrayBuffer buffer = new CharArrayBuffer(i); 
        try {
            char[] tmp = new char[1024];
            int l;
            while((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
        } finally {
            reader.close();
        }
        return buffer.toString();
    }
    
    /**
     * 发送删除请求
     * <br>
     * 2014年12月19日
     * @author gao.jun
     * @param uri 
     * @param responseEncode
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public static String deleteDomain(String uri, String responseEncode) throws ParseException, IOException {
    	HttpDelete delete = new HttpDelete(uri);
    	long start = DateUtil.now();
        log.info("Delete请求 ,请求的URL: {}  cost: {}ms", new Object[] { uri, (DateUtil.now() - start)});
        HttpResponse httpResponse = httpclient.execute(delete);
        
        String charset = responseEncode == null ? EntityUtils.getContentCharSet(httpResponse.getEntity())
                : responseEncode;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("Delete请求 ,返回的状态码：{} --- 返回的内容: {}", new Object[] { statusCode, s});
        httpResponse.getEntity().consumeContent();
        return s;
    }
    
    
    /**
     * openapi ver-0.2 post (针对提交or更新预分发文件的请求方式)
     * @param url
     * @param data 请求的流内容
     * @param Authorization 验证参数 
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String postBodyWithOutParams(String url,  String data,String Authorization, String encoding)throws IOException {
        StringEntity entity = new StringEntity(data, HttpClientUtil.UTF_8);
        //InputStreamEntity entity = new InputStreamEntity(in, in.available());
        log.info("POST请求,请求的URL: {} --- 请求的参数{}", new Object[]{url, EntityUtils.toString(entity)});
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        httpPost.setHeader("Authorization",Authorization);
        httpPost.setHeader("Lecloud-api-version","0.2");
        httpPost.setHeader("Accept","application/json");
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(httpPost);
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        if (statusCode == HttpStatus.SC_OK) {
            httpResponse.getEntity().consumeContent();
            return s;
        } else if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN){
            httpResponse.getEntity().consumeContent();
            return "403";
        } else {
            httpResponse.getEntity().consumeContent();
            httpPost.abort();
            return s;
            //return null;
        }
    }
    
    public static String postBodyWithOutParams(String url,  String data, String encoding)throws IOException {
        StringEntity entity = new StringEntity(data, HttpClientUtil.UTF_8);
        //InputStreamEntity entity = new InputStreamEntity(in, in.available());
        log.info("POST请求,请求的URL: {} --- 请求的参数{}", new Object[]{url, EntityUtils.toString(entity)});
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
        httpPost.setHeader("Accept","application/json");
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(httpPost);
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        if (statusCode == HttpStatus.SC_OK) {
            httpResponse.getEntity().consumeContent();
            return s;
        } else if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN){
            httpResponse.getEntity().consumeContent();
            return "403";
        } else {
            httpResponse.getEntity().consumeContent();
            httpPost.abort();
            return s;
            //return null;
        }
    }
    
    
    /**
     * openapi ver-0.2 delete (针对删除预分发文件的请求方式)
     * @param url
     * @param data 请求的流内容
     * @param Authorization 验证参数 
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String deleteXmlWithOutParams(String url,  String data, String Authorization,String encoding)throws IOException {
        StringEntity entity = new StringEntity(data, HttpClientUtil.UTF_8);
        //InputStreamEntity entity = new InputStreamEntity(in, in.available());
        log.info("Delete请求,请求的URL: {} --- 请求的参数{}", new Object[]{url, EntityUtils.toString(entity)});
        MyHttpDelete delete = new MyHttpDelete(url); 
        delete.setHeader("Content-Type", "application/json;charset=utf-8");
        delete.setHeader("Authorization",Authorization);
        delete.setHeader("Lecloud-api-version","0.2");
        delete.setHeader("Accept","application/json");
        delete.setEntity(entity);

        delete.setEntity(entity);
        HttpResponse httpResponse = httpclient.execute(delete);
        String charset = encoding == null ? EntityUtils.getContentCharSet(httpResponse.getEntity()) : encoding;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("返回的状态码：{} --- 返回的内容： {}", new Object[] { statusCode, s});
        if (statusCode == HttpStatus.SC_OK) {
            httpResponse.getEntity().consumeContent();
            return s;
        } else if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN){
            httpResponse.getEntity().consumeContent();
            return "403";
        } else {
            httpResponse.getEntity().consumeContent();
            delete.abort();
            //return null;
            return s;
        }
    }
    /**openapi ver-0.2 get (针对查询预分发文件的请求方式)
     * 
     * @param uri
     * @param Authorization
     * @param responseEncode
     * @return
     * @throws IOException
     */
    public static String getWithoutParam(String uri, String Authorization, String responseEncode) throws IOException {
    	
        HttpGet httpGet = new HttpGet(uri);
        
        httpGet.setHeader("Content-Type", "application/json;charset=utf-8");
        httpGet.setHeader("Authorization",Authorization);
        httpGet.setHeader("Lecloud-api-version","0.2");
        httpGet.setHeader("Accept","application/json");
        
        long start = DateUtil.now();
        log.info("GET请求 ,请求的URL: {}  cost: {}ms", new Object[] { uri, (DateUtil.now() - start)});
        HttpResponse httpResponse = httpclient.execute(httpGet);
        
        String charset = responseEncode == null ? EntityUtils.getContentCharSet(httpResponse.getEntity())
                : responseEncode;
        String s = EntityUtils.toString(httpResponse.getEntity(), charset);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.info("GET请求 ,返回的状态码：{} --- 返回的内容: {}", new Object[] { statusCode, s});
        httpResponse.getEntity().consumeContent();
        return s;
    }
    
    /**
     * head请求，返回到head中对应的Map
     * <br>
     * 2015年4月9日
     * @author gao.jun
     * @param uri
     * @param headerParam
     * @param responseEncode
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static Map<String,String> head(String uri, Map<String,String> headerParam, String responseEncode) throws ClientProtocolException, IOException {
    	HttpHead httpHead = new HttpHead(uri);
    	// 设置请求头
    	if(headerParam != null) {
    		for(Map.Entry<String,String> entry : headerParam.entrySet()) {
    			httpHead.setHeader(entry.getKey(),entry.getValue());
    		}
    	}
        // HttpClient默认不允许相同地址的调整
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10000)//设置请求超时时间
        		.setMaxRedirects(3)//设置最大跳转次数为3，默认为50
        		.build();
        httpHead.setConfig(requestConfig);
        long start = DateUtil.now();
        HttpResponse httpResponse = httpclient.execute(httpHead);
        log.info("Head请求 ,请求的URL: {}  cost: {}ms", new Object[] { uri, (DateUtil.now() - start)});
        int status = httpResponse.getStatusLine().getStatusCode();
        log.info("Head请求 ,返回的状态码：{} ", new Object[] { status });
        /*if(status == HttpStatus.SC_MOVED_TEMPORARILY){
	    	int redirectCount = 0;
	    	// 处理302跳转,仅进行两次302跳转
	    	while(redirectCount < 2) {
	    		Header locationHead = httpResponse.getLastHeader("Location");
	    		if(locationHead == null) {
	    			throw new IOException("302跳转时未找到Location对应Header");
	    		}
	    		HttpClientUtils.closeQuietly(httpResponse);
	    		httpHead = new HttpHead(locationHead.getValue());
	    		start = DateUtil.now();
	    		httpResponse = httpclient.execute(httpHead);
	    		log.info("第 {} 次Head 302重定向请求 ,请求的URL: {}  cost: {}ms", new Object[] {redirectCount + 1, locationHead.getValue(), (DateUtil.now() - start)});
	    		status = httpResponse.getStatusLine().getStatusCode();
	    		log.info("Head 302重定向请求 ,返回的状态码：{} ", new Object[] { status });
	    		if(status == HttpStatus.SC_OK) {
	    			break;
	    		}else if(status != HttpStatus.SC_MOVED_TEMPORARILY){
	    			throw new IOException("Head请求未正常响应");
	    		}
	    		++redirectCount;
	    	}
	    	
	    }*/
        Map<String,String> result = new HashMap<String,String>();
        if(status == HttpStatus.SC_OK) {
	    	for(Header head : httpResponse.getAllHeaders()) {
	    		result.put(head.getName(), head.getValue());
	    	}
        }else {
        	throw new IOException("Head请求失败");
        }
        HttpClientUtils.closeQuietly(httpResponse);
    	return result;
    }
    
    /**
     * Head请求时不传head
     * <br>
     * 2015年4月9日
     * @author gao.jun
     * @param uri
     * @param responseEncode
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static Map<String,String> head(String uri, String responseEncode) throws ClientProtocolException, IOException {
    	return head(uri, null, responseEncode);
    }
    
    /**
     * head请求获取地址是否存在
     * @method: HttpClientUtil  headExsitUrl
     * @param uri
     * @param responseEncode
     * @return
     * @throws ClientProtocolException
     * @throws IOException  boolean
     * @create date： 2015年6月2日
     * @2015, by liuchangfu.
     */
    public static boolean headExsitUrl(String uri,String responseEncode) throws ClientProtocolException, IOException {
    	HttpHead httpHead = new HttpHead(uri);
        // HttpClient默认不允许相同地址的调整
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10000)//设置请求超时时间
        		.setMaxRedirects(1)//设置最大跳转次数为3，默认为50
        		.build();
        httpHead.setConfig(requestConfig);
        long start = DateUtil.now();
        HttpResponse httpResponse = httpclient.execute(httpHead);
        log.info("Head请求 ,请求的URL: {}  cost: {}ms", new Object[] { uri, (DateUtil.now() - start)});
        int status = httpResponse.getStatusLine().getStatusCode();
        log.info("Head请求 ,返回的状态码：{} ", new Object[] { status });
        if(status == HttpStatus.SC_OK) {
        	HttpClientUtils.closeQuietly(httpResponse);
	    	return true ;
        }else {
        	HttpClientUtils.closeQuietly(httpResponse);
        	return false;
        }
    }
    
    /**
     * 获取响应 contentLength
     * @method: HttpClientUtil  headContentLength
     * @param uri
     * @param responseEncode
     * @return
     * @throws ClientProtocolException
     * @throws IOException  long
     * @create date： 2015年6月2日
     * @2015, by liuchangfu.
     */
    
    public static long headContentLength(String uri, String responseEncode) throws ClientProtocolException, IOException {
    	HttpHead httpHead = new HttpHead(uri);
        // HttpClient默认不允许相同地址的调整
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10000)//设置请求超时时间
        		.setMaxRedirects(2)//设置最大跳转次数为3，默认为50
        		.build();
        httpHead.setConfig(requestConfig);
        long start = DateUtil.now();
        HttpResponse httpResponse = httpclient.execute(httpHead);
        log.info("Head请求 ,请求的URL: {}  cost: {}ms", new Object[] { uri, (DateUtil.now() - start)});
        int status = httpResponse.getStatusLine().getStatusCode();
        log.info("Head请求 ,返回的状态码：{} ", new Object[] { status });
        long length = 0L ;
        if(status == HttpStatus.SC_OK) {
	    	for(Header head : httpResponse.getAllHeaders()) {
	    		if(head.getName().equals("Content-Length")){
	    			length = Long.valueOf(head.getValue());
	    			break ;
	    		}
	    	}
        }else {
        	length = 0L ;
        	log.info("请求地址不存在,：{}",new Object []{uri});
        }
        HttpClientUtils.closeQuietly(httpResponse);
    	return length;
    }
}
