package com.android.internal.policy.impl.lewa.lockscreen;


import java.util.Calendar;

import org.w3c.dom.Element;

import com.android.internal.policy.impl.lewa.view.Expression;
import com.android.internal.policy.impl.lewa.view.ResourceManager;
import com.android.internal.policy.impl.lewa.view.ScreenContext;
import com.android.internal.policy.impl.lewa.view.ScreenElement;
import com.android.internal.policy.impl.lewa.view.DomParseException;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import android.util.Log;



public class LockScreenRoot extends ScreenElement implements UnlockerListener{

    private static final String TAG = "LockScreenRoot";
    
    private static final int DEFAULT_FRAME_RATE = 30;
    
    private static final String TAG_NAME_CHARGING = "Charging";
    private static final String TAG_NAME_LOWBATTERY = "BatteryLow";
    private static final String TAG_NAME_BATTERYFULL = "BatteryFull";
    
    protected Calendar mCalendar;
    private boolean mDisplayDesktop;
    private LockScreenElementGroup mElementGroup;
    private int mFrameRate;
    private final BroadcastReceiver mIntentReceiver;
    private SoundManager mSoundManager;
    private UnlockerCallback mUnlockerCallback;
    private Context context;
    private ScreenContext screenContext;

    private Element element;
    
    public LockScreenRoot(ScreenContext screenContext,UnlockerCallback unlockercallback) {
        super(null, screenContext);

        this.screenContext = screenContext;
        context = screenContext.mContext;
        
        mCalendar = Calendar.getInstance();
        mIntentReceiver = new InformationReceiver();
        mFrameRate = 30;
        screenContext.mRoot = this;
        mUnlockerCallback = unlockercallback;
        
        initDisplay();
        
        updateTime();

        mSoundManager = new SoundManager(context);
    }
    
    private void initDisplay(){
        
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        
        int width = display.getWidth();
        int height = display.getHeight();
        
        double screen_width = 0.0;
        if(width < height){
            screen_width = width;
        }else{
            screen_width = height;
        }
        Expression.put(Expression.SCREEN_WIDTH, String.valueOf(screen_width));
        
        double screen_height = 0.0;
        if(height > width){
            screen_height = height;
        }else{
            screen_height = width;
        }
        Expression.put(Expression.SCREEN_HEIGHT, String.valueOf(screen_height));
    }

    private void updateTime(){
    
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        
        Expression.put(Expression.AMPM, String.valueOf(mCalendar.get(Calendar.AM_PM)));
        Expression.put(Expression.HOUR12, String.valueOf(mCalendar.get(Calendar.HOUR)));
        Expression.put(Expression.HOUR24, String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY)));
        Expression.put(Expression.MINUTE, String.valueOf(mCalendar.get(Calendar.MINUTE)));
        Expression.put(Expression.YEAR, String.valueOf(mCalendar.get(Calendar.YEAR)));
        Expression.put(Expression.MONTH, String.valueOf(mCalendar.get(Calendar.MONTH)));
        Expression.put(Expression.DATE, String.valueOf(mCalendar.get(Calendar.DATE)));
        Expression.put(Expression.DAY_OF_WEEK, String.valueOf(mCalendar.get(Calendar.DAY_OF_WEEK)));
    }

    public ScreenElement findElement(String name){
    
        if(mElementGroup != null){
            return mElementGroup.findElement(name);
        }else{
            return null;
        }
    }

   public void finish(){
        super.finish();
        mSoundManager.release();
        mSoundManager = null;        

        mUnlockerCallback = null;
        
        if(mElementGroup != null){
            mElementGroup.finish();
            mElementGroup = null;
        }
        
        context.unregisterReceiver(mIntentReceiver);

        element = null;
        context = null;
        screenContext = null;
        
    }

    public ScreenContext getContext(){
        return screenContext;
    }

    public int getFrameRate(){
        return mFrameRate;
    }

    public void init(){
    
       if(mElementGroup != null){
            mElementGroup.init();
            mElementGroup.showCategory(TAG_NAME_BATTERYFULL, false);
            mElementGroup.showCategory(TAG_NAME_CHARGING, false);
            mElementGroup.showCategory(TAG_NAME_LOWBATTERY, false);
        }
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.intent.action.TIME_TICK");
        intentfilter.addAction("android.intent.action.TIME_SET");
        intentfilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        context.registerReceiver(mIntentReceiver, intentfilter);
        
        refreshAlarm();
    }
    
    private void refreshAlarm(){
        String next_alarm_time = android.provider.Settings.System.getString(context.getContentResolver(), "next_alarm_formatted");
        Expression.put("next_alarm_time", next_alarm_time);
    }

    public boolean isDisplayDesktop(){
        return mDisplayDesktop;
    }

    public boolean load(){
    
        element = ResourceManager.getManifestRoot();

        if(element == null){
            return false;
        }
        try
        {
            mFrameRate = Integer.parseInt(element.getAttribute("frameRate"));
            mDisplayDesktop = Boolean.parseBoolean(element.getAttribute("displayDesktop"));
           
        }catch(NumberFormatException numberformatexception){
            mFrameRate = 30;
        }
     
        try {
            mElementGroup = new LockScreenElementGroup(element, screenContext);
        } catch (DomParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    public void onTouch(MotionEvent motionevent){
    
        if(mElementGroup == null){
            mUnlockerCallback.unlocked(null);
        } else{
        
            int x = (int)motionevent.getX();
            int y = (int)motionevent.getY();
            

            Expression.putRealTimeVar(mName, Expression.TOUCH_X, String.valueOf(x));
            Expression.putRealTimeVar(mName, Expression.TOUCH_Y, String.valueOf(y));
            mElementGroup.onTouch(motionevent);
        }
    }

    public void pause(){
    
        if(mElementGroup != null){
            mElementGroup.pause();
        }
    }

    public void playSound(String sound){
        mSoundManager.playSound(sound, true);
    }


    public void resume(){
    
        if(mElementGroup != null){
            mElementGroup.resume();
        }
            
        refreshAlarm();
    }

    private class InformationReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent){
        
           String action = intent.getAction();
            
           if(action.equals("android.intent.action.TIME_TICK")
                    || action.equals("android.intent.action.TIME_SET")
                    || action.equals("android.intent.action.TIMEZONE_CHANGED")){
                updateTime();
            }
        }
    }
   public UnlockerCallback getUnlockerCallback(){
        return mUnlockerCallback;
   }

   public interface UnlockerCallback{
    
        public abstract void pokeWakelock();

        public abstract void unlocked(Intent intent);
    }
    
    @Override
    public void render(Canvas canvas) {
        if(mElementGroup != null){
            mElementGroup.render(canvas);
        }
    }

    @Override
    public void tick(long time) {
        //Expression.put("time", String.valueOf(time));
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        //Expression.put("second", String.valueOf(mCalendar.get(Calendar.SECOND)));
        if(mElementGroup != null){
            mElementGroup.tick(time);
        } 
    }

    @Override
    public void endUnlockMoving(UnlockerScreenElement unlockerscreenelement) {
        // TODO Auto-generated method stub
        if(mElementGroup != null){
            mElementGroup.endUnlockMoving(unlockerscreenelement);
        }
    }

    @Override
    public void startUnlockMoving(UnlockerScreenElement unlockerscreenelement) {
        // TODO Auto-generated method stub
        if(mElementGroup != null) {
            mElementGroup.startUnlockMoving(unlockerscreenelement);
        }
    }

}
