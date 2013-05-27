package com.android.internal.policy.impl.lewa.view;

import java.util.Calendar;

import org.w3c.dom.Element;

import com.android.internal.policy.impl.lewa.view.util.ChineseDateUtil;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.text.format.DateFormat;
import android.util.Log;

public class DateTimeScreenElement extends TextScreenElement {

    public static final String TAG_NAME = "DateTime";
    protected Calendar mCalendar;
   // private final DateChangeReceiver mDateChangeReceiver;

    
    public DateTimeScreenElement(Element element, ScreenContext screenContext) throws DomParseException {
        super(element, screenContext);
        mCalendar = Calendar.getInstance();
        //mDateChangeReceiver = new DateChangeReceiver();
    }

    protected String getText(){
        String format = mFormat;
		mCalendar.setTimeInMillis(System.currentTimeMillis());
        if(mFormat.contains("NNNN")){
            ChineseDateUtil cd = new ChineseDateUtil(mCalendar,context);
            format = mFormat.replace("NNNN", cd.getChineseDay());
        }
        return DateFormat.format(format, mCalendar).toString();
    }

    public void finish(){

        //context.unregisterReceiver(mDateChangeReceiver);
    }

    public void init(){
    
        super.init();

        //IntentFilter intentfilter = new IntentFilter();
        //intentfilter.addAction(Intent.ACTION_DATE_CHANGED);
        //context.registerReceiver(mDateChangeReceiver, intentfilter);
    }


    private class DateChangeReceiver extends BroadcastReceiver{
    
        public void onReceive(Context context, Intent intent){

            String action = intent.getAction();
            
            if(action.equals(Intent.ACTION_DATE_CHANGED)){

               mCalendar.setTimeInMillis(System.currentTimeMillis());
            }
            
        }

    }
}
