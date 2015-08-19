/*
 * Copyright  2014. letv.com All Rights Reserved. 
 * Application : common 
 * Class Name  : RegExpValidatorUtil.java 
 * Date Created: 2014年10月16日 
 * Author      : chenyuxin 
 * 
 * Revision History 
 * 2014年10月16日 下午6:49:44 Amend By chenyuxin 
 */
package com.letv.cdn.openapi.utils;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * TODO:add description of class here
 * 
 * @author chenyuxin
 * @createDate 2014年10月16日
 */

public class RegExpValidatorUtil{
    /**
     * 验证邮箱
     * 
     * @param 待验证的字符串
     * @return 如果是符合的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean isEmail(String str) {
    
        String regex = "^([\\w-\\.]+)@((";
        return match(regex, str);
    }
    
    /**
     * 验证IP地址
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean isIP(String str) {
    
        String num = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";
        String regex = "^" + num + "\\." + num + "\\." + num + "\\." + num + "$";
        return match(regex, str);
    }
    
    /**
     * 验证网址Url
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsUrl(String str) {
    
        String regex = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
        return match(regex, str);
    }
    
    /**
     * 验证电话号码
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsTelephone(String str) {
    
        String regex = "^(";
        return match(regex, str);
    }
    
    /**
     * 验证输入密码条件(字符与数据同时出现)
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsPassword(String str) {
    
        String regex = "[A-Za-z]+[0-9]";
        return match(regex, str);
    }
    
    /**
     * 验证输入密码长度 (6-18位)
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsPasswLength(String str) {
    
        String regex = "^\\d{6,18}$";
        return match(regex, str);
    }
    
    /**
     * 验证输入邮政编号
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsPostalcode(String str) {
    
        String regex = "^\\d{6}$";
        return match(regex, str);
    }
    
    /**
     * 验证输入手机号码
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsHandset(String str) {
    
        String regex = "^[1]+[3,5]+\\d{9}$";
        return match(regex, str);
    }
    
    /**
     * 验证输入身份证号
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsIDcard(String str) {
    
        String regex = "(^\\d{18}$)|(^\\d{15}$)";
        return match(regex, str);
    }
    
    /**
     * 验证输入两位小数
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsDecimal(String str) {
    
        String regex = "^[0-9]+(.[0-9]{2})?$";
        return match(regex, str);
    }
    
    /**
     * 验证输入一年的12个月
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsMonth(String str) {
    
        String regex = "^(0?[[1-9]|1[0-2])$";
        return match(regex, str);
    }
    
    /**
     * 验证输入一个月的31天
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsDay(String str) {
    
        String regex = "^((0?[1-9])|((1|2)[0-9])|30|31)$";
        return match(regex, str);
    }
    
    /**
     * 验证日期格式 格式为"yyyy-MM-dd"
     * 
     * @method: RegExpValidatorUtil  isDate
     * @param str 待验证的字符串
     * @return  boolean
     * @createDate： 2014年10月22日
     * @2014, by chenyuxin.
     */
    public static boolean isDate(String str) {
        return isDate(str, "yyyy-MM-dd");
    }
    
    /**
     * 验证日期时间
     * 
     * @param str 待验证的字符串
     * @param pattern 验证的格式  可验证一下几种格式："yyyy-MM-dd"、"yyyy-MM-dd HH:mm:ss"、"yyyyMMdd"、"dd/MM/yyyy"
     * @return 如果是符合网址格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean isDate(String str, String pattern) {
        
        // yyyy-MM-dd
        String regex_yyyy_MM_dd = "^(?:(?!0000)[0-9]{4}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)-02-29)$";
        // yyyy-MM-dd HH:mm:ss
        String regex_yyyy_MM_dd_HH_mm_ss = "^(?:(?!0000)[0-9]{4}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)-02-29)\\s+([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$";
        // yyyyMMdd
        String regex_yyyyMMdd = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})(((0[13578]|1[02])(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)(0[1-9]|[12][0-9]|30))|(02(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))0229)";
        // dd/MM/yyyy
        String regex_dd_MM_yyyy = "(((0[1-9]|[12][0-9]|3[01])/((0[13578]|1[02]))|((0[1-9]|[12][0-9]|30)/(0[469]|11))|(0[1-9]|[1][0-9]|2[0-8])/(02))/([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3}))|(29/02/(([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00)))";
        
        String regex;
        if("yyyy-MM-dd".equals(pattern)){
            regex = regex_yyyy_MM_dd;
        }else if("yyyy-MM-dd HH:mm:ss".equals(pattern)){
            regex = regex_yyyy_MM_dd_HH_mm_ss;
        }else if("yyyyMMdd".equals(pattern)){
            regex = regex_yyyyMMdd;
        }else if("dd/MM/yyyy".equals(pattern)){
            regex = regex_dd_MM_yyyy;
        }else{
            regex = regex_yyyy_MM_dd;
        }
        return match(regex, str);
    }
    
    /**
     * 验证数字输入
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsNumber(String str) {
    
        String regex = "^[0-9]*$";
        return match(regex, str);
    }
    
    /**
     * 验证非零的正整数
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsIntNumber(String str) {
    
        String regex = "^\\+?[1-9][0-9]*$";
        return match(regex, str);
    }
    
    /**
     * 验证大写字母
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsUpChar(String str) {
    
        String regex = "^[A-Z]+$";
        return match(regex, str);
    }
    
    /**
     * 验证小写字母
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsLowChar(String str) {
    
        String regex = "^[a-z]+$";
        return match(regex, str);
    }
    
    /**
     * 验证验证输入字母
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsLetter(String str) {
    
        String regex = "^[A-Za-z]+$";
        return match(regex, str);
    }
    
    /**
     * 验证验证输入汉字
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsChinese(String str) {
    
        String regex = "^[\u4e00-\u9fa5],{0,}$";
        return match(regex, str);
    }
    
    /**
     * 验证验证输入字符串
     * 
     * @param 待验证的字符串
     * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
     */
    public static boolean IsLength(String str) {
    
        String regex = "^.{8,}$";
        return match(regex, str);
    }
    
    /**
     * @param regex
     *        正则表达式字符串
     * @param str
     *        要匹配的字符串
     * @return 如果str 符合 regex的正则表达式格式,返回true, 否则返回 false;
     */
    private static boolean match(String regex, String str) {
    
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }
    /**
     * 验证日期是否符合yyyyMMdd 的日期
     * @param time
     * @return
     */
	public static boolean isFromartDate(String time) {
		if (time != null) {
			String tmp = null;
			try {
				Format f = new SimpleDateFormat("yyyyMMdd");
				Date d = (Date) f.parseObject(time);
				tmp = f.format(d);

			} catch (ParseException e) {
				e.printStackTrace();
			}
			return tmp.equals(time);
		}else{
			return false ;
		}
	}
	/**
	 * API sign生成规则
	 * 需要传入一个调用接口传入所有参数的map ，和 secretkey(userkey)
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public static String getSignParam(Map<String,String> map,String secretkey) throws Exception{
		Set<String> keySet = map.keySet();
		List<String> list = new ArrayList<String>();
		String sign = "";
		for(String param:keySet){
			list.add(param);
		}
		Collections.sort(list);
		for(String paramKey:list){
			sign+=paramKey+map.get(paramKey);
		}
		String signMD5 = MD5Util.getStringMD5String(sign+secretkey);
		return signMD5 ;
	}
	
	/**
	 * 校验domain是否有效。
	 * <br>
	 * 域名中不能包含空格、逗号
	 * 2014年12月30日
	 * @author gao.jun
	 * @param domain
	 * @return 校验有效则返回true，反之返回false
	 */
	public static boolean validateDomain(String domain) {
		if(domain.contains(" ") || domain.contains(",")) {
			return false;
		}
		return true;
	}
	
    // 3. 检查字符串重复出现的词
    //
    // private void btnWord_Click(object sender, EventArgs e)
    // {
    // System.Text.RegularExpressions.MatchCollection matches =
    // System.Text.RegularExpressions.Regex.Matches(label1.Text,
    //
    // @"\b(?<word>\w+)\s+(\k<word>)\b",
    // System.Text.RegularExpressions.RegexOptions.Compiled |
    // System.Text.RegularExpressions.RegexOptions.IgnoreCase);
    // if (matches.Count != 0)
    // {
    // foreach (System.Text.RegularExpressions.Match match in matches)
    // {
    // string word = match.Groups["word"].Value;
    // MessageBox.Show(word.ToString(),"英文单词");
    // }
    // }
    // else { MessageBox.Show("没有重复的单词"); }
    //
    //
    // }
    //
    // 4. 替换字符串
    //
    // private void button1_Click(object sender, EventArgs e)
    // {
    //
    // string strResult =
    // System.Text.RegularExpressions.Regex.Replace(textBox1.Text,
    // @"[A-Za-z]\*?", textBox2.Text);
    // MessageBox.Show("替换前字符:" + "\n" + textBox1.Text + "\n" + "替换的字符:" + "\n"
    // + textBox2.Text + "\n" +
    // "替换后的字符:" + "\n" + strResult,"替换");
    // }
    //
    // 5. 拆分字符串
    //
    // private void button1_Click(object sender, EventArgs e)
    // {
    // //实例: 甲025-8343243乙0755-2228382丙029-32983298389289328932893289丁
    // foreach (string s in
    // System.Text.RegularExpressions.Regex.Split(textBox1.Text,@"\d{3,4}-\d*"))
    // {
    // textBox2.Text+=s; //依次输出 "甲乙丙丁"
    // }
    // }
    /**
     * 去除所有空白字符
     * <b>Method</b>: RegExpValidatorUtil#replaceBlank <br/>
     * <b>Create Date</b> : 2014年11月24日
     * @author Chen Hao
     * @param str
     * @return  String
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }    
}
