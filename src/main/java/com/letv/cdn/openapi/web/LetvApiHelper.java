package com.letv.cdn.openapi.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: lichao
 * Date: 12-10-31
 * Time: 下午7:16
 */
public class LetvApiHelper {

    private static final Logger log = LoggerFactory.getLogger(LetvApiHelper.class);

    public static void main(String[] args) throws IOException {
        String USER_UNIQUE = "0ba31bfb11";
        String SECRET_KEY = "f3ec2a75eca1d8946ea9ca09b9ead8dd";
        //String fmt = "json";
        String fmt = "xml";
        LetvApiHelper helper = new LetvApiHelper(USER_UNIQUE, SECRET_KEY, fmt);

        //生成API访问地址
//        String url = helper.makeUrl("video.upload.init", LetvApiHelper.newParamMap("video_name", "上传测试"));
//        String url = helper.makeUrl("video.upload.flash", LetvApiHelper.newParamMap("video_name", "上传测试", "js_callback", "fun_callback"));
//        String url = helper.makeUrl("video.update", LetvApiHelper.newParamMap("video_id", "2000451", "video_name", "更新测试"));
//        String url = helper.makeUrl("video.list", LetvApiHelper.newParamMap("'index'", "1", "size", "10", "status", "0"));
//        String url = helper.makeUrl("video.get", LetvApiHelper.newParamMap("video_id", "2000451"));
//        String url = helper.makeUrl("video.del", LetvApiHelper.newParamMap("video_id", "2000451"));
//        String url = helper.makeUrl("video.del.batch", LetvApiHelper.newParamMap("video_id_list", "2000185-2000188"));
//        String url = helper.makeUrl("data.video.hour", LetvApiHelper.newParamMap("date", "2012-10-15"));
//        String url = helper.makeUrl("data.video.date", LetvApiHelper.newParamMap("start_date", "2012-10-01", "start_date", "2012-10-15"));

        String url = helper.makeUrl("data.total.date", LetvApiHelper.newParamMap("start_date", "2012-10-01", "start_date", "2012-10-15"));
        System.out.println(url);


        String request = LetvApiHelper.request(url);
        System.out.println("request = " + request);
    }


    private static String API = "http://api.letvcloud.com/rest.php";
    private String userUnique;
    private String secretKey;
    private String fmt;

    private LetvApiHelper(String userUnique, String secretKey, String fmt) {
        this.userUnique = userUnique;
        this.secretKey = secretKey;
        this.fmt = fmt;
    }


    private static <T> Map<T, T> newParamMap(T... args) {
        if (args.length % 2 != 0) throw new IllegalArgumentException("需偶数个参数");
        Map<T, T> map = new HashMap<T, T>();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    public String makeUrl(String method, Map<String, String> param) {
        return makeUrl(method, param, fmt, userUnique, secretKey);
    }

//        public String makeUrl(String method, Map<String, String> param, String fmt) {
//            return makeUrl(method, param, fmt, userUnique, secretKey);
//        }

    public static String makeUrl(String method, Map<String, String> param, String fmt, String user, String key) {
        param.put("user_unique", user);
        param.put("timestamp", String.valueOf(System.currentTimeMillis()));
        param.put("format", fmt);
        param.put("api", method);
        param.put("ver", "2.0");
        param.put("sign", sign(param, key));
        return API + '?' + toRequestParam(param);
    }


    public static String sign(Map<String, String> m, String key) {
        String[] names = m.keySet().toArray(new String[m.size()]);
        Arrays.sort(names);

        StringBuilder buf = new StringBuilder();
        for (String name : names) {
            buf.append(name);
            buf.append(m.get(name));
        }
        buf.append(key);
        log.info("###### : {}", buf.toString());
        return MD5.md5(buf.toString());
    }

    private static String toRequestParam(Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            try {
                stringBuilder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException ignored) {
            }

        }
        return stringBuilder.toString();
    }

    public static String request(String api) throws IOException {
        URL url = new URL(api);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();

        ByteArrayOutputStream byteArrayBuffer = new ByteArrayOutputStream();
        copy(urlConnection.getInputStream(), byteArrayBuffer);
        return byteArrayBuffer.toString();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
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

    public static String getRemortIP(HttpServletRequest request) {
        
        if (request.getHeader("x-forwarded-for") == null) {
            return request.getRemoteAddr();
        }
        return request.getHeader("x-forwarded-for");
    }
    
    /**
     * 获取客户端真实IP
     * 如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，
     * 那么真正的用户端的真实IP则是取X-Forwarded-For中第一个非unknown的有效IP字符串。
     * @method: IpUtil  getClientIpAddr
     * @param request
     * @return  String
     * @createDate： 2014年11月10日
     * @2014, by chenyuxin.
     */
    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

