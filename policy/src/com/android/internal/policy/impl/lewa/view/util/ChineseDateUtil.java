package com.android.internal.policy.impl.lewa.view.util;

import java.util.Calendar;
import android.content.Context;
import com.android.internal.R;


public class ChineseDateUtil {

    private int gregorianYear;
    private int gregorianMonth;
    private int gregorianDate;
    private int chineseYear;
    private int chineseMonth;// 负数表示闰月
    private int chineseDate;

    private static final char[] daysInGregorianMonth = { 31, 28, 31, 30, 31,
            30, 31, 31, 30, 31, 30, 31 };

    /**
     * 农历月份大小压缩表，两个字节表示一年。两个字节共十六个二进制位数 前四个位数表示闰月月份，后十二个位数表示十二个农历月份的大小。
     */
    private static final char[] chineseMonths = { 0x00, 0x04, 0xad, 0x08, 0x5a,
            0x01, 0xd5, 0x54, 0xb4, 0x09, 0x64, 0x05, 0x59, 0x45, 0x95, 0x0a,
            0xa6, 0x04, 0x55, 0x24, 0xad, 0x08, 0x5a, 0x62, 0xda, 0x04, 0xb4,
            0x05, 0xb4, 0x55, 0x52, 0x0d, 0x94, 0x0a, 0x4a, 0x2a, 0x56, 0x02,
            0x6d, 0x71, 0x6d, 0x01, 0xda, 0x02, 0xd2, 0x52, 0xa9, 0x05, 0x49,
            0x0d, 0x2a, 0x45, 0x2b, 0x09, 0x56, 0x01, 0xb5, 0x20, 0x6d, 0x01,
            0x59, 0x69, 0xd4, 0x0a, 0xa8, 0x05, 0xa9, 0x56, 0xa5, 0x04, 0x2b,
            0x09, 0x9e, 0x38, 0xb6, 0x08, 0xec, 0x74, 0x6c, 0x05, 0xd4, 0x0a,
            0xe4, 0x6a, 0x52, 0x05, 0x95, 0x0a, 0x5a, 0x42, 0x5b, 0x04, 0xb6,
            0x04, 0xb4, 0x22, 0x6a, 0x05, 0x52, 0x75, 0xc9, 0x0a, 0x52, 0x05,
            0x35, 0x55, 0x4d, 0x0a, 0x5a, 0x02, 0x5d, 0x31, 0xb5, 0x02, 0x6a,
            0x8a, 0x68, 0x05, 0xa9, 0x0a, 0x8a, 0x6a, 0x2a, 0x05, 0x2d, 0x09,
            0xaa, 0x48, 0x5a, 0x01, 0xb5, 0x09, 0xb0, 0x39, 0x64, 0x05, 0x25,
            0x75, 0x95, 0x0a, 0x96, 0x04, 0x4d, 0x54, 0xad, 0x04, 0xda, 0x04,
            0xd4, 0x44, 0xb4, 0x05, 0x54, 0x85, 0x52, 0x0d, 0x92, 0x0a, 0x56,
            0x6a, 0x56, 0x02, 0x6d, 0x02, 0x6a, 0x41, 0xda, 0x02, 0xb2, 0xa1,
            0xa9, 0x05, 0x49, 0x0d, 0x0a, 0x6d, 0x2a, 0x09, 0x56, 0x01, 0xad,
            0x50, 0x6d, 0x01, 0xd9, 0x02, 0xd1, 0x3a, 0xa8, 0x05, 0x29, 0x85,
            0xa5, 0x0c, 0x2a, 0x09, 0x96, 0x54, 0xb6, 0x08, 0x6c, 0x09, 0x64,
            0x45, 0xd4, 0x0a, 0xa4, 0x05, 0x51, 0x25, 0x95, 0x0a, 0x2a, 0x72,
            0x5b, 0x04, 0xb6, 0x04, 0xac, 0x52, 0x6a, 0x05, 0xd2, 0x0a, 0xa2,
            0x4a, 0x4a, 0x05, 0x55, 0x94, 0x2d, 0x0a, 0x5a, 0x02, 0x75, 0x61,
            0xb5, 0x02, 0x6a, 0x03, 0x61, 0x45, 0xa9, 0x0a, 0x4a, 0x05, 0x25,
            0x25, 0x2d, 0x09, 0x9a, 0x68, 0xda, 0x08, 0xb4, 0x09, 0xa8, 0x59,
            0x54, 0x03, 0xa5, 0x0a, 0x91, 0x3a, 0x96, 0x04, 0xad, 0xb0, 0xad,
            0x04, 0xda, 0x04, 0xf4, 0x62, 0xb4, 0x05, 0x54, 0x0b, 0x44, 0x5d,
            0x52, 0x0a, 0x95, 0x04, 0x55, 0x22, 0x6d, 0x02, 0x5a, 0x71, 0xda,
            0x02, 0xaa, 0x05, 0xb2, 0x55, 0x49, 0x0b, 0x4a, 0x0a, 0x2d, 0x39,
            0x36, 0x01, 0x6d, 0x80, 0x6d, 0x01, 0xd9, 0x02, 0xe9, 0x6a, 0xa8,
            0x05, 0x29, 0x0b, 0x9a, 0x4c, 0xaa, 0x08, 0xb6, 0x08, 0xb4, 0x38,
            0x6c, 0x09, 0x54, 0x75, 0xd4, 0x0a, 0xa4, 0x05, 0x45, 0x55, 0x95,
            0x0a, 0x9a, 0x04, 0x55, 0x44, 0xb5, 0x04, 0x6a, 0x82, 0x6a, 0x05,
            0xd2, 0x0a, 0x92, 0x6a, 0x4a, 0x05, 0x55, 0x0a, 0x2a, 0x4a, 0x5a,
            0x02, 0xb5, 0x02, 0xb2, 0x31, 0x69, 0x03, 0x31, 0x73, 0xa9, 0x0a,
            0x4a, 0x05, 0x2d, 0x55, 0x2d, 0x09, 0x5a, 0x01, 0xd5, 0x48, 0xb4,
            0x09, 0x68, 0x89, 0x54, 0x0b, 0xa4, 0x0a, 0xa5, 0x6a, 0x95, 0x04,
            0xad, 0x08, 0x6a, 0x44, 0xda, 0x04, 0x74, 0x05, 0xb0, 0x25, 0x54,
            0x03 };

    /**
     * 初始日，公历农历对应日期： // 公历1901年1月1日，对应农历4598年11月11日
     */
    private static int baseYear = 1901;
    private static int baseMonth = 1;
    private static int baseDate = 1;
    private static int baseIndex = 0;
    private static int baseChineseYear = 4598 - 1;
    private static int baseChineseMonth = 11;
    private static int baseChineseDate = 11;

    /**
    * 中国农历中的"闰"字
    */
    private static String chinese_char_run = "\\u95f0";

    /**
    * 中国农历中的"月"字
    */
    private static String chinese_char_month = "\\u6708";

    private static String[] chineseMonthNames = null;

    private static String[] chineseDateNames = null;

    private Context context;


    public ChineseDateUtil(Calendar calendar,Context context) {
        
        if(chineseMonthNames == null){
            chineseMonthNames = context.getResources().getStringArray(
                com.android.internal.R.array.chineseMonthNames);
        }
        if(chineseDateNames == null){
            chineseDateNames = context.getResources().getStringArray(
                com.android.internal.R.array.chineseDateNames);
        }

        this.context = context;
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        setGregorian(year, month, day);
        
        computeChineseFields();
        
    }

    public void setGregorian(int y, int m, int d) {
        gregorianYear = y;
        gregorianMonth = m;
        gregorianDate = d;
        chineseYear = 0;
        chineseMonth = 0;
        chineseDate = 0;
    }

    public boolean isGregorianLeapYear(int year) {
        boolean isLeap = false;
        if (year % 4 == 0){
            isLeap = true;
        }
        
        if (year % 100 == 0){
            isLeap = false;
        }
            
        if (year % 400 == 0){
            isLeap = true;
        }
            
        return isLeap;
    }

    public int daysInGregorianMonth(int y, int m) {
        int d = daysInGregorianMonth[m - 1];
        if (m == 2 && isGregorianLeapYear(y)){
            d++;// 公历闰年二月多一天
        }
            
        return d;
    }

    public int dayOfYear(int y, int m, int d) {
        int c = 0;
        for (int i = 1; i < m; i++) {
            c = c + daysInGregorianMonth(y, i);
        }
        c = c + d;
        return c;
    }

    public int computeChineseFields() {
        if (gregorianYear < 1901 || gregorianYear > 2100){
            return 1;
        }
            
        int startYear = baseYear;
        int startMonth = baseMonth;
        int startDate = baseDate;
        chineseYear = baseChineseYear;
        chineseMonth = baseChineseMonth;
        chineseDate = baseChineseDate;
        // 第二个对应日，用以提高计算效率
        // 公历2000年1月1日，对应农历4697年11月25日
        if (gregorianYear >= 2000) {
            startYear = baseYear + 99;
            startMonth = 1;
            startDate = 1;
            chineseYear = baseChineseYear + 99;
            chineseMonth = 11;
            chineseDate = 25;
        }
        int daysDiff = 0;
        for (int i = startYear; i < gregorianYear; i++) {
            daysDiff += 365;
            if (isGregorianLeapYear(i)){
                daysDiff += 1;// leapyear
            }     
        }
        for (int i = startMonth; i < gregorianMonth; i++) {
            daysDiff += daysInGregorianMonth(gregorianYear, i);
        }
        daysDiff += gregorianDate - startDate;

        chineseDate += daysDiff;
        int lastDate = daysInChineseMonth(chineseYear, chineseMonth);
        int nextMonth = nextChineseMonth(chineseYear, chineseMonth);
        while (chineseDate > lastDate) {
            if (Math.abs(nextMonth) < Math.abs(chineseMonth)){
                chineseYear++;
            }
                
            chineseMonth = nextMonth;
            chineseDate -= lastDate;
            lastDate = daysInChineseMonth(chineseYear, chineseMonth);
            nextMonth = nextChineseMonth(chineseYear, chineseMonth);
        }
        return 0;
    }

    private static int[] bigLeapMonthYears = {
            // 大闰月的闰年年份
            6, 14, 19, 25, 33, 36, 38, 41, 44, 52, 55, 79, 117, 136, 147, 150,
            155, 158, 185, 193 };

    public int daysInChineseMonth(int y, int m) {
        // 注意：闰月m<0
        int index = y - baseChineseYear + baseIndex;
        int v = 0;
        int l = 0;
        int d = 30;
        if (1 <= m && m <= 8) {
            v = chineseMonths[2 * index];
            l = m - 1;
            if (((v >> l) & 0x01) == 1){
                d = 29;
            }
                
        } else if (9 <= m && m <= 12) {
            v = chineseMonths[2 * index + 1];
            l = m - 9;
            if (((v >> l) & 0x01) == 1){
                d = 29;
            }
                
        } else {
            v = chineseMonths[2 * index + 1];
            v = (v >> 4) & 0x0F;
            if (v != Math.abs(m)) {
                d = 0;
            } else {
                d = 29;
                for (int i = 0; i < bigLeapMonthYears.length; i++) {
                    if (bigLeapMonthYears[i] == index) {
                        d = 30;
                        break;
                    }
                }
            }
        }
        return d;
    }

    public int nextChineseMonth(int y, int m) {
        int n = Math.abs(m) + 1;
        if (m > 0) {
            int index = y - baseChineseYear + baseIndex;
            int v = chineseMonths[2 * index + 1];
            v = (v >> 4) & 0x0F;
            if (v == m){
                 n = -m;
            }
               
        }
        if (n == 13){
            n = 1;
        }
            
        return n;
    }

    /**
     * 获得中国农历月：10月
     */
    public String getChineseMonth() {
        String ret = "";

        if (chineseMonth > 0) {
            ret = new StringBuilder().append(chineseMonthNames[chineseMonth - 1]).append(unicodeTozhCN(chinese_char_month)).toString();
        } else if (chineseMonth < 0) {
            ret = new StringBuilder().append(unicodeTozhCN(chinese_char_run)).append(chineseMonthNames[-chineseMonth - 1]).append(unicodeTozhCN(chinese_char_month)).toString();
        }

        return ret;
    }

    /**
    *将Unicode编码转换为中文字符
    */
    private String unicodeTozhCN(String unicode){ 
        StringBuffer zhCN= new StringBuffer();
        //不是"\\u"，而是 "\\\\u" 
        String[] hex = unicode.split("\\\\u");
        //注意要从 1 开始，而不是从0开始。第一个是空
        for(int i=1;i<hex.length;i++){
            //将16进制数转换为 10进制的数据
            int data = Integer.parseInt(hex[i],16);
            //强制转换为char类型就是中文字符 
            zhCN.append((char)data);  
        }
        return zhCN.toString(); 
    } 

    /**
     * 获得中国农历日：30日
     */
    public String getChineseDate() {
        String ret = "";

        if (chineseMonth > 0) {
            ret = chineseDateNames[chineseDate - 1];
        } else if (chineseMonth < 0) {
            ret = chineseDateNames[chineseDate - 1];
        }

        return ret;
    }

    public String getChineseDay(){
        return new StringBuilder().append(getChineseMonth()).append(getChineseDate()).toString();
    }
    
    /**public static void main(String[] arg) {
        Calendar calendar = Calendar.getInstance();
        ChineseDateUtil c = new ChineseDateUtil(calendar);

        System.out.println(c.getChineseDay());

    }*/
}
