package com.android.systemui.statusbar;

import com.android.systemui.R;

import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DataActivity extends Activity{

    private Context mContext;
    private static SharedPreferences mSharedPreferences;
    private static TextView month_used_data;
    private static TextView today_data;
    private static TextView left_data;
    private static TextView text3; 
    private static TextView month_total;
    private static TextView today_save;
    private static TextView month_save;

    private static ProgressBar progressbar;
    public static final int MAX_TEXT_INPUT_LENGTH = 6;	
    private static final long GB_UNIT = 1024*1024*1024;
    private static final long MB_UNIT = 1024*1024;
    private static final long KB_UNIT = 1024;	
    private static final String TAG = "DataActivity";
    private static java.text.NumberFormat  formater;
    public static boolean mHasFocus = false;	
    static final boolean LOGC = false; 

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)
            updateWLANData();
        //mHasFocus = hasFocus;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dataactivity);
        mContext = this;		
        mSharedPreferences = mContext.getSharedPreferences(DataMonitorService.PREFERENCE_NAME, Activity.MODE_PRIVATE);
        month_used_data = (TextView)findViewById(R.id.month_used_data);
        today_data = (TextView)findViewById(R.id.today_data); 
        left_data = (TextView)findViewById(R.id.left_data);
        text3 = (TextView)findViewById(R.id.text3);
        //month_total = (TextView)findViewById(R.id.month_total);
        today_save = (TextView)findViewById(R.id.today_save);
        month_save = (TextView)findViewById(R.id.month_save);
        progressbar = (ProgressBar)findViewById(R.id.progressbar1);   
        formater  =  java.text.DecimalFormat.getInstance();  
        formater.setMaximumFractionDigits(2);  
        formater.setMinimumFractionDigits(2); 
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        long todayuseddata = mSharedPreferences.getLong(DataMonitorService.TODAY_USED, 0);
        long monthleftdata = mSharedPreferences.getLong(DataMonitorService.MONTH_LEFT, 0);	 
        long monthused = mSharedPreferences.getLong(DataMonitorService.MONTH_USED, 0);
        updateTodayLeftData(todayuseddata, monthleftdata, monthused); 
        long monthtotal = mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 0); 
        updateMonthTotal(monthtotal);
        updateWLANData();
        mHasFocus = true;
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mHasFocus = false;
    }

    private void updateWLANData() {
        long wlantoday = mSharedPreferences.getLong(DataMonitorService.WLAN_TODAY, 0); 
        long wlanmonth = mSharedPreferences.getLong(DataMonitorService.WLAN_MONTH, 0); 
        String today_UNIT = "K";
        String month_UNIT = "K";

        float today = Float.parseFloat(String.valueOf(wlantoday))/KB_UNIT; 
        if (today < 0) {
            today = 0;
        }
        if (today >= KB_UNIT) {
            today = Float.parseFloat(String.valueOf(wlantoday))/MB_UNIT;
            today_UNIT = "M";
        }
        if (today >= KB_UNIT) {
            today = Float.parseFloat(String.valueOf(wlantoday))/GB_UNIT;
            today_UNIT = "G";
        }
        
        float month = Float.parseFloat(String.valueOf(wlanmonth))/KB_UNIT;
        if (month < 0) {
            month = 0;
        }
        if (month >= KB_UNIT) {
            month = Float.parseFloat(String.valueOf(wlanmonth))/MB_UNIT;
            month_UNIT = "M";
        }
        if (month >= KB_UNIT) {
            month = Float.parseFloat(String.valueOf(wlanmonth))/GB_UNIT;
            month_UNIT = "G";
        }

        String today_str = formater.format(today);
        String month_str = formater.format(month);        
        today_save.setText(today_str + today_UNIT);
        month_save.setText(month_str + month_UNIT);
    }
    
    public static void updateMonthTotal(long monthtotal) {	
        /*String total_UNIT = "K";
        float month = Float.valueOf(monthtotal+"")/KB_UNIT;
        if (month >= KB_UNIT) {
            month = Float.valueOf(monthtotal+"")/MB_UNIT;
            total_UNIT = "M";
        }
        if (month >= KB_UNIT) {
            month = Float.valueOf(monthtotal+"")/GB_UNIT;
            total_UNIT = "G";
        }
        String month_str = formater.format(month);        
        month_total.setText(month_str + total_UNIT);*/
    }
    
    public static void updateTodayLeftData(long todaydata, long leftdata, long monthused) {	
        long monthtotal = mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 0); 
        String today_UNIT = "K";
        String left_UNIT = "K";
        String monthused_UNIT = "K";
        float today = Float.valueOf(todaydata+"")/KB_UNIT;
        float left = Float.valueOf(leftdata+"")/KB_UNIT;
        float month = Float.valueOf(monthused+"")/KB_UNIT;
        updateMonthTotal(monthtotal);
        if (LOGC) {
            Log.i(TAG, "updateTodayLeftData today_str = " + today);
            Log.i(TAG, "updateTodayLeftData left_str = " + left);
            Log.i(TAG, "updateTodayLeftData month_str = " + month);
        }

        if (today >= KB_UNIT) {
            today = Float.valueOf(todaydata+"")/MB_UNIT;
            today_UNIT = "M";
        }
        if (today >= KB_UNIT) {
            today = Float.valueOf(todaydata+"")/GB_UNIT;
            today_UNIT = "G";
        }
        
        if (left >= KB_UNIT) {
            left = Float.valueOf(leftdata+"")/MB_UNIT;
            left_UNIT = "M";
        }
        if (left >= KB_UNIT) {
            left = Float.valueOf(leftdata+"")/GB_UNIT;
            left_UNIT = "G";
        }

        if (month >= KB_UNIT) {
            month = Float.valueOf(monthused+"")/MB_UNIT;
            monthused_UNIT = "M";
        }
        if (month >= KB_UNIT) {
            month = Float.valueOf(monthused+"")/GB_UNIT;
            monthused_UNIT = "G";
        }
 
        String today_str = formater.format(today);
        String left_str = formater.format(left);
        String month_str = formater.format(month);
        if (LOGC) {
            Log.i(TAG, "updateTodayLeftData today_str = " + today_str);
            Log.i(TAG, "updateTodayLeftData left_str = " + left_str);
            Log.i(TAG, "updateTodayLeftData month_str = " + month_str);
        } 
        updateTodayLeftData(month_str, monthused_UNIT, today_str, today_UNIT, left_str, left_UNIT, monthtotal, monthused, todaydata);	
    }

    public static void updateTodayLeftData(String monthuseddata, String monthused_UNIT, String todaydata, String today_UNIT, String leftdata, String left_UNIT,
            long total, long monthused, long todayused) { 	
   
        long reminddata = mSharedPreferences.getLong(DataMonitorService.REMIND_DATA, 0); 
        long left = mSharedPreferences.getLong(DataMonitorService.MONTH_LEFT, 0); 
        if (LOGC) {
            Log.i(TAG, "updateTodayLeftData left_data = " + left_data);
            Log.i(TAG, "updateTodayLeftData text3 = " + text3);
            Log.i(TAG, "updateTodayLeftData today_data = " + today_data);
            Log.i(TAG, "updateTodayLeftData month_used_data = " + month_used_data);
            Log.i(TAG, "updateTodayLeftData progressbar = " + progressbar);
            
        } 
        if (left_data == null || text3 == null || today_data == null ||month_used_data == null)
        {
            Log.e(TAG, "zhangbo left_data == null || text3 == null ");
            return ;
        }
        if (left <= reminddata) {
            left_data.setTextColor(Color.RED);
        } else {
            left_data.setTextColor(0xff515151);
        }

        if (monthused > total) {
            long exceeddata = monthused - total;  		
            String exceed_UNIT = "K";
            float exceed = Float.valueOf(exceeddata+"")/KB_UNIT;
            if (exceed >= KB_UNIT) {
                exceed = Float.valueOf(exceeddata+"")/MB_UNIT;
                exceed_UNIT = "M";
            }
            if (exceed >= KB_UNIT) {
                exceed = Float.valueOf(exceeddata+"")/GB_UNIT;
                exceed_UNIT = "G";
            }
            
            String exceed_str = formater.format(exceed);      	
            text3.setText(R.string.exceed_flow_ext1);
            left_data.setText(exceed_str + exceed_UNIT); 
        } else {
            text3.setText(R.string.left_flow_ext1);
            left_data.setText(leftdata + left_UNIT); 
        }


        today_data.setText(todaydata + today_UNIT);
        //left_data.setText(leftdata + left_UNIT); 
        month_used_data.setText(monthuseddata + monthused_UNIT);

        if (total != 0) {
            /*int progress = (int)((monthused * 100) / total);
            int secondprogress = progress + (int)((todayused * 100) / total);*/

            int monthused_rat = (int)((monthused * 100) / total);
            int todayused_rat = (int)((todayused * 100) / total);      	       	
            int progress = monthused_rat - todayused_rat;
            int secondprogress = monthused_rat; 

            progressbar.setProgress(progress);
            progressbar.setSecondaryProgress(secondprogress);
        } else {
            progressbar.setProgress(0);
            progressbar.setSecondaryProgress(0);
        }
    }
}
