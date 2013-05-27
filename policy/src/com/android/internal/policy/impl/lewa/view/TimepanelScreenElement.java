package com.android.internal.policy.impl.lewa.view;

import java.util.Calendar;
import java.lang.ref.SoftReference;


import org.w3c.dom.Element;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import java.util.Calendar;

public class TimepanelScreenElement extends AnimatedScreenElement {

    private static final String TAG = "TimepanelScreenElement";
    private static final String M12 = "hh:mm";
    private static final String M24 = "kk:mm";
    public static final String TAG_NAME = "Time";
    private int mBmpHeight;
    private int mBmpWidth;
    private SoftReference<Bitmap> mBuffer;//四个时间数字和'：'组成的图片
    protected Calendar mCalendar;
    private String mFormat;
    private final Handler mHandler;
    //private final BroadcastReceiver mIntentReceiver;
    private Paint mPaint;
    
    private Canvas mCanvas;
    private int mTimeFormatLength;
    private int hourDrawed = -1;
    private int minDrawed = -1;
    
    public TimepanelScreenElement(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
        
        mFormat = M24;
        mPaint = new Paint();
        mHandler = new Handler();
        mCalendar = Calendar.getInstance();
        //mIntentReceiver = new TimeZoneChangeReceiver();
    }
    
    public void finish(){
       // context.unregisterReceiver(mIntentReceiver);
    }

    public void init(){
    
        super.init();
        
        setDateFormat();
        
        //IntentFilter intentfilter = new IntentFilter();
        //intentfilter.addAction("android.intent.action.TIME_TICK");
        //intentfilter.addAction("android.intent.action.TIME_SET");
        //intentfilter.addAction("android.intent.action.TIMEZONE_CHANGED");
       // context.registerReceiver(mIntentReceiver, intentfilter);
        
        updateTime();
    }

    private void setDateFormat(){
    
        if(DateFormat.is24HourFormat(context)){
            mFormat = M24;
        }else{
            mFormat = M12;
        }
        mTimeFormatLength = mFormat.length();
    }
    
    private Bitmap getDigitBmp(char c){
 
        String timeStr = null;
        if(c == ':'){
            timeStr = "dot";
        }else{
            timeStr = String.valueOf(c);
        }
        
        String timeBmp = null;
        String bitmap = mAni.getBitmapName();
        if(bitmap.isEmpty()){
            timeBmp = "time.png";
        }else {
            timeBmp = bitmap;
        }
        int position = timeBmp.indexOf('.');
        String timeBmpSub = timeBmp.substring(0,position);
        
        String bitmapName = new StringBuilder().append(timeBmpSub).append("_").append(timeStr).append(timeBmp.substring(position)).toString();
        
        return ResourceManager.getBitmapFromCache(bitmapName);
    }
    
    private void updateTime(){

        
        if(mBuffer == null){
        
            Bitmap bitmap_char = getDigitBmp('0');
            Bitmap bitmap_dot = getDigitBmp(':');
            if(bitmap_char == null){
                return;
            }
            if(bitmap_dot == null){
                return;
            }
            mBmpHeight = bitmap_char.getHeight();
            int bmp_width_4_chars = bitmap_char.getWidth() * 4;
            int bmp_width_dot = bitmap_dot.getWidth();
            int totalWidth = bmp_width_4_chars + bmp_width_dot;
            mBuffer = new SoftReference<Bitmap>(Bitmap.createBitmap(totalWidth, mBmpHeight, Config.ARGB_4444));
            

        }
        
        if(mCanvas == null){
            mCanvas = new Canvas((Bitmap)mBuffer.get());  
            
        }
        mCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
        
        mCalendar.setTimeInMillis(System.currentTimeMillis());
  
        CharSequence timeStr = DateFormat.format(mFormat, mCalendar);
        int left = 0;
        if(mTimeFormatLength > 0){
            for(int i=0;i<mTimeFormatLength;i++){
                char c = timeStr.charAt(i);
                Bitmap digitBitmap = getDigitBmp(c);
                mCanvas.drawBitmap(digitBitmap, left, 0f, null);
                left += digitBitmap.getWidth();
            //    digitBitmap.recycle();
            }
            mBmpWidth = left;
            screenContext.mShouldUpdate = true;
        }
       
    }
    
    public void render(Canvas canvas){

        int curHour;
        int curMinute;
        
        if(!isVisible()){
            return;
        }
        Calendar calendar = Calendar.getInstance();
        curHour = calendar.get(Calendar.HOUR_OF_DAY);
        curMinute = calendar.get(Calendar.MINUTE);
        if(curHour != hourDrawed || curMinute != minDrawed)
        {
            hourDrawed = curHour;
            minDrawed = curMinute;
            updateTime();
        }
        int alpha = mAni.getAlpha();
        if(alpha <= 0){
            return;
        }
        if(mBuffer == null){
            return;
        } else{
            mPaint.setAlpha(alpha);
            float left = getLeft(mAni.getX(), mBmpWidth);
            float top = getTop(mAni.getY(), mBmpHeight);
            canvas.drawBitmap((Bitmap)mBuffer.get(), left, top, mPaint);
        }
    }
    
    /*private class TimeZoneChangeReceiver extends BroadcastReceiver{
    
        public void onReceive(Context context, Intent intent){
            
            String action = intent.getAction();
            
            if(action.equals("android.intent.action.TIME_TICK")
                    || action.equals("android.intent.action.TIME_SET")
                    || action.equals("android.intent.action.TIMEZONE_CHANGED")){
                
                
                class UpdateTimeThread implements Runnable {
                    public void run(){
                        try{
                         //   updateTime();
                        } catch(Exception exception){
                            exception.printStackTrace();
                            Log.e(TAG, new StringBuilder().append("fail to updateTime: ").append(exception.toString()).toString());
                        }
                    }
                }

                mHandler.post(new UpdateTimeThread());
            }
            
        }

    }*/

}
