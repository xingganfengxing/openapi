
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.letv.cdn.openapiauth.utils.LetvApiHelper;

/**
 * TODO:add description of class here
 * 
 * @author chenyuxin
 * @createDate 2014年10月21日
 */

public class Test_MD5{

    static String SRC = "http://dlsw.baidu.com/sw-search-sp/soft/b2/15892/BaiduMus45704.exe";
    static String KEY = "acloudtest" + System.currentTimeMillis();
    static String DOMAINTAG = "video10iotekclass0com";
    //136098 7f769d6d82f2863b56e71177cb857b5c
    static String USERID = "136098";//134388
    static String SKEY = "7f769d6d82f2863b56e71177cb857b5c";
    
    
    public static void main(String[] args) throws Exception {
    	getBase64();
    }
    
    public static void getBase64() {
    	
    	String userid = "138866";
    	
    	String method = "DELETE";
    	
    	String uri = "/cdn/domain";
    	
        String appkey = "45a3e9d5a262f94b8521c6c90d915d23";
        
        String md5 = MD5.md5(userid + method + uri + appkey);
        
        String base64 = LetvApiHelper.encodeBase64(userid + ":" + md5);
        
        System.out.println("Basic " + base64);
    }
    
    public static void postDomain() {
    	Map<String, String> map = new LinkedHashMap<String, String>();
    	map.put("user", "138866");
        String sign = sign(map, "45a3e9d5a262f94b8521c6c90d915d23");
    	System.out.println(sign);
    }
    
    private static void getTraffic() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("granularity", "5min");//134388
        map.put("endday", "20141124");//134388
        map.put("startday", "20141124");//134388q
        map.put("userid", USERID);//134388
        map.put("ver", "0.1");
        map.put("sign", sign(map, "7f769d6d82f2863b56e71177cb857b5c"));
        String uri = "http://openapi.letvcloud.com/cdn/domain/" + USERID + "_" + DOMAINTAG+ "/traffic";
        getUrl(map, uri);
    }
    private static void getdomain() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("userid", "136098");//134388
        map.put("ver", "0.1");
        map.put("sign", sign(map, "7f769d6d82f2863b56e71177cb857b5c"));
        String uri = "http://openapi.letvcloud.com/cdn/domain/136098_wbdown0wn510com";
        getUrl(map, uri);
    }
    private static void delfile() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("src", SRC);
        map.put("domaintag", DOMAINTAG);
        map.put("key", KEY);
        map.put("userid", USERID);
        map.put("ver", "0.1");
        map.put("sign", sign(map, SKEY));
        String uri = "http://openapi.letvcloud.com/cdn/content/delfile";
        getUrl(map, uri);
    }
    
    private static void getPostDomain() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("domaintag", "letvcloudtest");
        map.put("domain", "v.letvcloud.com");
        map.put("source", "s.letvcloud.com");
        map.put("remark", "test");
        map.put("userid", "137587");
        map.put("ver", "0.1");
        String s = sign(map, "86a2b0487355b831fda63285fb06901e");
        System.out.println(s);
    }
    
    private static void getContentSubFileUrl() throws Exception{
        
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("domaintag", DOMAINTAG);
        map.put("src", SRC);
        /*File file = new File("C:\\Users\\chenyuxin\\Downloads\\apache-maven-3.2.3-bin.zip");
        String fileMD5 = MD5.md5(file);*/
        map.put("md5", "");
        map.put("key", KEY);
        map.put("userid", USERID);//134388
        map.put("ver", "0.1");
        map.put("sign", sign(map, SKEY));
        // http://localhost:8084/  http://openapi.letvcloud.com/
        String uri = "http://openapi.letvcloud.com/cdn/content/subfile";
        getUrl(map, uri);
    }
    
    private static void getContentProgress() throws IOException {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("src", SRC);
        map.put("key", KEY);
        map.put("userid", USERID);
        map.put("ver", "0.1");
        map.put("sign", sign(map, SKEY));
        String uri = "http://openapi.letvcloud.com/cdn/content/progress";
        getUrl(map, uri);
    }
    
    private static void getOpenApiUrl() throws Exception{
        //changba         102.10207       10      15c27aff929140ae3d16dc3b048ff135
        //bilibili.com    2.203b100172    32      15c27aff929104ae3d16dc3b408ff136
        //fenxiang1xia    102.miaopai     82      15c72aff818104ae3d16dc3b408ff136
        Map<String, String> map = new LinkedHashMap<String, String>();
        //map.put("domaintag", "102.miaopai");//域名
        map.put("startday", "20141101");//起始日期 格式：yyyyMMdd
        map.put("endday", "20141102");//结束日期
        map.put("granularity", "day");//数据粒度 day、5min
        map.put("userid", "134388");//用户id
        map.put("ver", "1.0");
        map.put("sign", sign(map, "042952a44457eeb7f6b9570ff436c3d6"));
        
        String dynamic = map.get("userid") + "_" + "102.10207";
        
        String uri = "http://openapi.letvcloud.com/cdn/domain/" + dynamic + "/traffic";
        //String uri = "http://openapi.letvcloud.com/traffic";bandwidth
        getUrl(map, uri);
    }
    
    private static void getReportUrl() throws Exception{
        Map<String, String> map = new LinkedHashMap<String, String>();
        //map.put("business", "102.10207");//域名
        //map.put("business", "2.203b100172");
        //map.put("business", "102.miaopai");
        map.put("startTime", "2014-10-25");//起始日期  格式：yyyy-MM-dd
        map.put("endTime", "2014-10-31");//结束日期
        map.put("dataType", "min");//数据粒度
        map.put("userid", "134388");//用户id
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String now = sdf.format(new Date());
        map.put("timeStamp", now);//时间戳
        
        //String sign = getStringMD5String(now + "15c27aff929140ae3d16dc3b048ff135");
        //String sign = getStringMD5String(now + "15c27aff929104ae3d16dc3b408ff136");
        String sign = getStringMD5String(now + "042952a44457eeb7f6b9570ff436c3d6");
        map.put("sign", sign);//MD5校验码
        
        //String uri = "http://openapi.letvcloud.com/bandwidthAPI";
        String uri = "http://openapi.letvcloud.com/trafficAPI";
        getUrl(map, uri);
    }
    
    private static void getUrl(Map<String, String> map, String uri){
        
        StringBuilder s = new StringBuilder();
        for(Map.Entry<String, String> entry:map.entrySet()){
            if(s.length() > 0){
                s.append("&");
            }
            s.append(entry.getKey()).append("=").append(entry.getValue());
        }
        System.out.println("md5-----------------" + map.get("md5"));
        System.out.println("sign----------------" + map.get("sign"));
        System.out.println( uri + "?" + s.toString() );
    }
    
    private static char md5Chars[] =
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f'};
    
    private static MessageDigest messagedigest;
    
    public static String getStringMD5String(String str) throws Exception {
        messagedigest = MessageDigest.getInstance("MD5");
        messagedigest.update(str.getBytes());
        return bufferToHex(messagedigest.digest());
    }
    
    private static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);
    }
    
    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }
    
    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = md5Chars[(bt & 0xf0) >> 4];
        char c1 = md5Chars[bt & 0xf];
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }

    private static String sign(Map<String, String> m, String key) {
        String[] names = m.keySet().toArray(new String[m.size()]);
        Arrays.sort(names);

        StringBuilder buf = new StringBuilder();
        for (String name : names) {
            buf.append(name);
            buf.append(m.get(name));
        }
        buf.append(key);

        return MD5.md5(buf.toString());
    }
    
    private static class MD5 {
        private static char md5Chars[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        public static String md5(String str) {
            MessageDigest md5 = getMD5Instance();
            md5.update(str.getBytes());
            byte[] digest = md5.digest();
            char[] chars = toHexChars(digest);
            return new String(chars);
        }

        public static String md5(File file) throws IOException{
            //File file = new File(filePath);
            FileInputStream in = new FileInputStream(file);
            FileChannel ch = in.getChannel();
            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = getMD5Instance();
            md5.update(byteBuffer);
            byte[] digest = md5.digest();
            char[] chars = toHexChars(digest);
            return new String(chars);
        }
        
        private static MessageDigest getMD5Instance() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ignored) {
                throw new RuntimeException(ignored);
            }
        }

        private static char[] toHexChars(byte[] digest) {
            char[] chars = new char[digest.length * 2];
            int i = 0;
            for (byte b : digest) {
                char c0 = md5Chars[(b & 0xf0) >> 4];
                chars[i++] = c0;
                char c1 = md5Chars[b & 0xf];
                chars[i++] = c1;
            }
            return chars;
        }
    }
    
}
