package com.android.internal.policy.impl.lewa.view;

import java.util.HashMap;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

import android.util.Log;

public class Expression {

    private static final String TAG = "Expression";
    
    /**
     * =========================================================================
     * 组部件实时动态变量
     */
    public static final String ACTUAL_X = "actual_x";
    public static final String ACTUAL_Y = "actual_y";
    
    public static final String MOVE_X = "move_x";//解锁时在x方向移动距离
    public static final String MOVE_Y = "move_y";//解锁时在y方向移动距离
    
    public static final String TOUCH_X = "touch_x";//当前触摸点 x
    public static final String TOUCH_Y = "touch_y";//当前触摸点 y
    
    public static final String MOVE_DIST = "move_dist";//解锁时移动距离
    
    public static final String STATE = "state";//正常:0  按下:1   到达解锁位置:2
    
    public static final String MUSIC_STATE = "music_state";
    public static final int MUSIC_STATE_PLAY = 1;
    public static final int MUSIC_STATE_STOP = 0;
    
    /**
     * =========================================================================
     */
    
    public static final String AMPM = "ampm";//上下午 // 0 am, 1 pm
    public static final String HOUR12 = "hour12";//12小时制
    public static final String HOUR24 = "hour24";//24小时制
    public static final String MINUTE = "minute";//分钟
    public static final String MILLISECOND = "msec";//秒
    public static final String DATE = "date";//日
    public static final String MONTH = "month";//月  //0-11
    public static final String YEAR = "year";//年
    public static final String DAY_OF_WEEK = "day_of_week";//星期  // 1-7 星期日到星期六
    public static final String GLOBAL = "global";

    public static final String BATTERY_LEVEL = "battery_level";//电池电量 0-100
    public static final String BATTERY_STATE = "battery_state";//电池状态： 正常:0 充电:1 电量低:2 已充满:3
    
    public static final int BATTERY_STATE_CHARGING = 1;
    public static final int BATTERY_STATE_FULL = 3;
    public static final int BATTERY_STATE_LOW = 2;
    public static final int BATTERY_STATE_UNPLUGGED = 0;
    
    public static final String CALL_MISSED_COUNT = "call_missed_count";//未接电话
    public static final String SMS_UNREAD_COUNT = "sms_unread_count";//未读短信

    public static final String NEXT_ALARM_TIME = "next_alarm_time";
    
    public static final String SCREEN_HEIGHT = "screen_height";//屏幕高度
    public static final String SCREEN_WIDTH = "screen_width";//屏幕宽度
    
    public static final String SECOND = "second";
    

    public static final String TEXT_WIDTH = "text_width";
    public static final String TIME = "time"; //当前时间，long

    public static final int UNLOCKER_STATE_NORMAL = 0;
    public static final int UNLOCKER_STATE_PRESSED = 1;
    public static final int UNLOCKER_STATE_REACHED = 2;
    
    public static final String VISIBILITY = "visibility";
    public static final int VISIBILITY_FALSE = 0;
    public static final int VISIBILITY_TRUE = 1;
    
    /**
     * '|'指定的特殊字符，用于values中指定键的值不存在时(即为null)，用此特殊字符暂时替换，
     * 然后在进行replaceAll时，对原始表达式不产生影响
     */
    private static final String SPECIAL_CHAR = "0";
    
    private String pattern = "^-?\\d+$";//整数
    
    /**
     * values是存放以上声明的各种变量的具体值的，如当前未接电话为3
     */
    private static HashMap<String, String> values = new HashMap<String, String>();
    
    private HashMap<String, String> objStrs = new HashMap<String,String>();
    private HashMap<String, Double> objDous = new HashMap<String,Double>();
    
    private static HashMap<String, HashMap<String, String>> realTimeVars = new HashMap<String, HashMap<String,String>>();
    private static HashMap<String, String> realTimeVar;
    
    
    private static Evaluator evaluator = new Evaluator();
    
    /**
     * 将各种变量值保存到values数据结构中
     * @param variable 变量名称，如：sms_unread_count
     * @param value 变量值，如：2 （2条未读短信）
     */
    public static void put(String variable,String value){
        if(variable == null || variable.trim().equals("")){
            Log.e(TAG, "variable is invalid, variable == " + variable);
            return;
        }
        
        values.put(variable, value);
    }
    
    /**
     * 根据变量名称获得变量值，如果为null，则返回默认值
     * @param variable
     * @param defaultValue
     * @return
     */
    public static String get(String variable,String defaultValue){
        String value = values.get(variable);
        if(value == null){
            value = defaultValue;
        }
        return value;
    }
    
    /**
     * 
     * @param mName "phone_unlocker"
     * @param attrName "state"
     * @param expValue "1"
     */
   public static void putRealTimeVar(String mName,String attrName,String expValue){
       realTimeVar = realTimeVars.get(mName);
       if(realTimeVar == null){
           realTimeVar = new HashMap<String, String>();
           realTimeVars.put(mName, realTimeVar);
       }
       realTimeVar.put(attrName, expValue);
    }
    
    /**
     * 
     * @param mName "phone_unlocker"
     * @param attrName "state"
     * @param expValue "1"
     */
    public static String getRealTimeVar(String mName,String attrName,String defaultValue){
        realTimeVar = realTimeVars.get(mName);
        if(realTimeVar == null){
            return defaultValue;
        }
        String realVar = realTimeVar.get(attrName);
        
        if(realVar == null){
            return defaultValue;
        }
        return realVar;
    }
    
    /**
     * 给各个xml表达式中的变量赋值,并且含有表达式
     * @param attrName 部件中属性的名称
     * @param expValue 部件中属性的值
     */
    public void putDou(String attrName, String expValue){
        Double dou = 0d;
        if(expValue.matches(pattern)){
            dou = Double.valueOf(expValue);
        }else {
            
            try {
                dou = Double.valueOf(evaluator.evaluate(transform(expValue)));
                
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e(TAG, "NumberFormatException result == " + dou);
                dou = 0d;
            } catch (EvaluationException e) {
                e.printStackTrace();
                Log.e(TAG, "EvaluationException expValue == " + expValue);
            }
        }
        
        objDous.put(attrName, dou);
        
    }
    
    private static String getVar(String variable){
        return getVar(variable, "0");
    }
    
    /**
     * 根据表达式属性中的变量名获得其对应的值
     * @param expName
     * @param defaultValue
     * @return
     */
    private static String getVar(String variable,String defaultValue){
        if(variable == null || variable.trim().equals("")){
            Log.e(TAG, "variable is invalid, variable == " + variable + " ,return defaultValue == " + defaultValue);
            return defaultValue;
        }
        if(values.get(variable) == null){
            
            return SPECIAL_CHAR;
        }
      
        return values.get(variable);
    }
    
    /**
     * 表达式的转换
     * @param expValue
     * @return
     */
    private static String transform(String expValue){
        
        if(expValue == null || expValue.trim().equals("")){
            Log.e(TAG, "expValue is invalid, expValue == " + expValue);
            return null;
        }
        
        if(expValue.indexOf("#ampm") != -1){
            expValue = expValue.replaceAll("#ampm",getVar(AMPM));
        }
        if(expValue.indexOf("#hour12") != -1){
            expValue = expValue.replaceAll("#hour12",getVar(HOUR12));
        }
        if(expValue.indexOf("#hour24") != -1){
            expValue = expValue.replaceAll("#hour24",getVar(HOUR24));
        }
        if(expValue.indexOf("#minute") != -1){
            expValue = expValue.replaceAll("#minute",getVar(MINUTE));
        }
        if(expValue.indexOf("#msec") != -1){
            expValue = expValue.replaceAll("#msec",getVar(MILLISECOND));
        }
        if(expValue.indexOf("#date") != -1){
            expValue = expValue.replaceAll("#date",getVar(DATE));
        }
        if(expValue.indexOf("#month") != -1){
            expValue = expValue.replaceAll("#month",getVar(MONTH));
        }
        if(expValue.indexOf("#year") != -1){
            expValue = expValue.replaceAll("#year",getVar(YEAR));
        }
        if(expValue.indexOf("#day_of_week") != -1){
            expValue = expValue.replaceAll("#day_of_week",getVar(DAY_OF_WEEK));
        }
        if(expValue.indexOf("#battery_level") != -1){
            expValue = expValue.replaceAll("#battery_level",getVar(BATTERY_LEVEL));
        }
        if(expValue.indexOf("#battery_state") != -1){
            expValue = expValue.replaceAll("#battery_state",getVar(BATTERY_STATE));
        }
        if(expValue.indexOf("#call_missed_count") != -1){
            expValue = expValue.replaceAll("#call_missed_count",getVar(CALL_MISSED_COUNT));
        }
        if(expValue.indexOf("#sms_unread_count") != -1){
            expValue = expValue.replaceAll("#sms_unread_count",getVar(SMS_UNREAD_COUNT));
        }
        if(expValue.indexOf("#screen_height") != -1){
            expValue = expValue.replaceAll("#screen_height",getVar(SCREEN_HEIGHT));
        }
        if(expValue.indexOf("#screen_width") != -1){
            expValue = expValue.replaceAll("#screen_width",getVar(SCREEN_WIDTH));
        }
        if(expValue.indexOf("#text_width") != -1){
            expValue = expValue.replaceAll("#text_width",getVar(TEXT_WIDTH));
        }
        if(expValue.indexOf("#time") != -1){
            expValue = expValue.replaceAll("#time",getVar(TIME));
        }
        
        if(expValue.indexOf(".move_y") != -1){
            int position = expValue.indexOf(".move_y");
            String subStr = expValue.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            expValue = expValue.replaceAll(new StringBuilder().append("#").append(name).append(".move_y").toString(),getRealTimeVar(name, MOVE_Y, "0"));
        }
        if(expValue.indexOf(".move_x") != -1){
            int position = expValue.indexOf(".move_x");
            String subStr = expValue.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            expValue = expValue.replaceAll(new StringBuilder().append("#").append(name).append(".move_x").toString(),getRealTimeVar(name, MOVE_X, "0"));
        }
        if(expValue.indexOf(".state") != -1){
            int position = expValue.indexOf(".state");
            String subStr = expValue.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            expValue = expValue.replaceAll(new StringBuilder().append("#").append(name).append(".state").toString(),getRealTimeVar(name, STATE, "0"));
        }
        if(expValue.indexOf(".music_state") != -1){
            int position = expValue.indexOf(".music_state");
            String subStr = expValue.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            expValue = expValue.replaceAll(new StringBuilder().append("#").append(name).append(".music_state").toString(),getRealTimeVar(name, MUSIC_STATE, "0"));
        }
        if(expValue.indexOf(".move_dist") != -1){
            int position = expValue.indexOf(".move_dist");
            String subStr = expValue.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            expValue = expValue.replaceAll(new StringBuilder().append("#").append(name).append(".move_dist").toString(),getRealTimeVar(name, MOVE_DIST, "0"));
        }
        if(expValue.indexOf(".visibility") != -1){
            int position = expValue.indexOf(".visibility");
            String subStr = expValue.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            expValue = expValue.replaceAll(new StringBuilder().append("#").append(name).append(".visibility").toString(),getRealTimeVar(name, VISIBILITY, "0"));
        }
        return expValue;
       
    }
    
    /**
     * 根据部件的属性名获得其对应的值.计算并返回结果
     * @param attrName 属性名
     * @param defaultValue 返回默认值
     * @return
     */
    public Double getDou(String attrName,Double defaultValue){
        if(attrName == null || attrName.trim().equals("")){
            Log.e(TAG, "attrName is invalid, attrName == " + attrName + " ,return defaultValue == " + defaultValue);
            return defaultValue;
        }
        Double result = objDous.get(attrName);
        if(result == null){
            result = defaultValue;
        }
        return result;
    }
    
    /**
     * 给各个xml表达式中的变量赋值,用于值为String类型,并且含有表达式
     * @param elementName 部件的名称
     * @param attrName 部件中属性的名称
     * @param expValue 部件中属性的值
     */
    public void putStr(String attrName, String expValue){
        objStrs.put(attrName, expValue);
    }
    
    public String getStr(String attrName){
        
        return objStrs.get(attrName);
    }

	static public int caculateInt(String exp)
	{
		try{
			Double d;
			d = Double.valueOf(evaluator.evaluate(transform(exp)));
			return d.intValue(); 
		} catch (EvaluationException e) {
                e.printStackTrace();
                Log.e(TAG, "EvaluationException expValue == " + exp);
				return 0;
        }
	}
    
}
