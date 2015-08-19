package com.letv.cdn.openapi.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: haoshihai
 * Date: 13-7-16
 * Time: 上午11:43
 * To change this template use File | Settings | File Templates.
 */
public class DateUtil {

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    static SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS");
    static Calendar calendar = null;
    static Map<String, Integer> map = new HashMap<String, Integer>();
    static {
        map.put("hour", Calendar.HOUR_OF_DAY);
        map.put("day", Calendar.DATE);
        map.put("week", Calendar.WEEK_OF_MONTH);
        map.put("month", Calendar.MONTH);
    }
    
    
    public static long now() {
        return System.currentTimeMillis();
    }
    public static String nowNice() {
        return dateFormat1.format(new Date());
    }

     public static String nowNice_1() {
        return dateFormat.format(new Date());
    }

    public static long getTime(String timeFormat) throws ParseException {
        return dateFormat.parse(timeFormat).getTime();
    }
      public static Date getDate(String timeFormat) throws ParseException {
        return dateFormat.parse(timeFormat);
    }
      
    /**
     * 前一周开始时间
     * @return
     */
    public static long getLastWeekStartTime() {
        getIntCalendar();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
        return calendar.getTimeInMillis();
    }
    
    /**
     * 前一天零点时间
     * @return
     */
    public static long getYesterStartTime() {
        getIntCalendar();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
        return calendar.getTimeInMillis();
    }
    
    /**
     * 当天零点时间
     * @return
     */
    public static long getYesterEndTime() {
        getIntCalendar();
        return calendar.getTimeInMillis();
    }
    
    /**
     * 前一天零点时间
     * @return
     */
    public static Date getYesterStartDate() {
        getIntCalendar();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
        return calendar.getTime();
    }
    
    /**
     * 明天零点时间
     * @return
     */
    public static Date getTomorrowStartDate() {
        getIntCalendar();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        return calendar.getTime();
    }
    
    /**
     * 当天零点时间
     * @return
     */
    public static Date getTodayStartDate() {
        getIntCalendar();
        return calendar.getTime();
    }

    /**
     * 当天零点时间
     * @return
     */
    public static Calendar getIntCalendar() {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public List<String> getTimeCategory(long startTime, long endTime, String cycle) {
        List<String> list = new ArrayList<String>();
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        while (calendar.getTimeInMillis() < endTime) {
            dateFormat.format(new Date(calendar.getTimeInMillis()));
            calendar.set(map.get(cycle), calendar.get(map.get(cycle) + 1));
            list.add(dateFormat.format(calendar.getTime()));
        }
        return list;
    }

    //取当前时间前一个小时整点值
    public static long startHourTime() {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    //当然时间小时整点值
    public static long endHourTime() {
        calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static void main(String args[]) {
        System.out.println("currHourTime " + DateUtil.endHourTime());
        System.out.println("lastHourTime " + DateUtil.startHourTime());
    }

    /**
     * 返回天单位日期的毫秒数
     *
     * @param day
     * @return
     * @throws java.text.ParseException
     * @author Chen Hao
     * @since 2013-07-24
     */
    public static Long getTimeByDayString(String day) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d = sdf.parse(day);
        return d.getTime();
    }

    /**
     * 为以天为单位的日期字符串增加一天
     *
     * @param day
     * @return
     * @throws java.text.ParseException
     * @author Chen Hao
     * @since 2013-07-24
     */
    public static String addOneDayByDayString(String day) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Long t = getTimeByDayString(day) + 24 * 60 * 60 * 1000;
        return sdf.format(new Date(t));
    }


    /**
     * yyyy-MM-dd HH:mm:ss 格式日期转换为毫秒数
     * @param startTime
     * @return
     * @throws java.text.ParseException
     */
    public static Long parseTime(String startTime) throws ParseException {
        if (startTime == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = sdf.parse(startTime);
        return d.getTime();
    }
    //根据格式化格式获取当前时间
    public static String getCurrentTime(String fomart){
       Date date = new Date();
       SimpleDateFormat sdf = new SimpleDateFormat(fomart);
      String time = sdf.format(date);
      return time;
       
    }
    
    /**
     * 将pattern1格式的String时间转换成pattern2格式的String时间
     * 例如：将yyyyMMddHHmm格式的时间转换成yyyy-MM-dd HH:mm格式的时间，时间为String类型
     *  String date = "201407021230";转换后：date = "2014-07-02 12:30";
     * @param date
     * @param pattern1
     * @param pattern2
     * @throws ParseException 
     */
    public static String changePattern(String date, String pattern1, String pattern2) throws ParseException{
    	DateFormat src = new SimpleDateFormat(pattern1);
    	DateFormat dest = new SimpleDateFormat(pattern2);
    	return dest.format(src.parse(date));
    }
    
    /**
     * 将包含小时的格式转换成日期格式
     * @param date
     * @return
     * @throws ParseException
     */
    public static String getDayPattern(String date) throws ParseException{
    	return changePattern(date, "yyyy-MM-dd HH", "yyyyMMdd");
    }
    
    /**
     * 转换成数据库中对应的时间格式
     * @param date
     * @return
     * @throws ParseException
     */
    public static String getDBPattern(String date) throws ParseException{
    	return changePattern(date, "yyyy-MM-dd HH", "yyyyMMddHH00");
    }
    
    /**
     * 
     * @param date "yyyyMMddHHmm"格式的时间
     * @param flag true表示返回下一天的开始时间，false表示今天的开始时间
     * @return
     * @throws ParseException
     */
    public static String getTheStartTimeOfNextDay(String date, boolean flag) throws ParseException{
    	DateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
    	Calendar c = Calendar.getInstance();
		c.setTime(df.parse(date));
		c.set(Calendar.HOUR_OF_DAY, 0);
		if( flag ){
			c.add(Calendar.DATE, 1);
		}
		return df.format(c.getTime());
    }
    
    /**
     * 返回下一天
     * @param date 日期格式须为yyyyMMdd
     * @return
     * @throws ParseException
     */
    public static String getTheNextDay(String date) throws ParseException{
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
    	Calendar c = Calendar.getInstance();
		c.setTime(df.parse(date));
		c.add(Calendar.DATE, 1);
		return df.format(c.getTime());
    }
    
    /**
     * 返回两个日期之间的日期list，包含开始和结束日期
     * @param startDate
     * @param endDate
     * @return
     * @throws ParseException
     */
    public static List<String> getDays(String startDate, String endDate) throws ParseException{
    	List<String> dayList = new ArrayList<String>();
    	startDate = DateUtil.changePattern(startDate, "yyyy-MM-dd", "yyyyMMdd");
		endDate = DateUtil.changePattern(endDate, "yyyy-MM-dd", "yyyyMMdd");
		endDate = DateUtil.getTheNextDay(endDate);
		while(!startDate.equals(endDate)){
			dayList.add(startDate);
			startDate = DateUtil.getTheNextDay(startDate);
		}
    	return dayList;
    }
    
}
