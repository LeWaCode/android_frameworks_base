package com.android.internal.policy.impl.lewa.lockscreen;



import com.android.internal.policy.impl.KeyguardScreenCallback;
import com.android.internal.policy.impl.lewa.view.Expression;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.telephony.TelephonyManager;
import android.graphics.Rect;

import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

public class LewaLockScreenView extends SurfaceView{
    private static final String TAG = "LewaLockScreenView";
    private static final boolean DEBUG = false;
	public static final int MSG_PHONE_STATE = 1;
	public static final int MSG_SCREEN_OFF = 2;
	public static final int MSG_SCREEN_ON = 3;
	public static final int MSG_CHECK_STATUS = 4;
    public static final int MSG_UNLOCK = 5;

	public static final int IGNORE_FLAG_WINDOW_FOCUS = 0x01;
	public static final int IGNORE_FLAG_CALLING = 0x02;
	public static final int IGNORE_FLAG_PARENT_VISIBLE = 0x04;

	private static boolean mViewLive = true;// if the lockscreenview is alive.
	
    private int mFrameRate;
    private KeyguardScreenCallback mKeyguardScreenCallback;
    private boolean mPaused;
    private long mPausedTime;
    private LockScreenRoot mRoot;
    private boolean mStarted;
    private boolean mStop;     
    private RenderThread mThread;
    
    private Context context;
    private PowerManager pm;
	private SurfaceHolder mSfHolder;
	private Bitmap mBgBmp;
	private final BroadcastReceiver mIntentReceiver;
	private boolean mViewVisibile;

	
    public LewaLockScreenView(Context context, KeyguardScreenCallback keyguardscreencallback, LockScreenRoot lockscreenroot){
        super(context);
        this.context = context;

        pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE); 
        
        mFrameRate = 30;
        mKeyguardScreenCallback = keyguardscreencallback;
        setFocusable(true);
        setFocusableInTouchMode(true);
        mFrameRate = lockscreenroot.getFrameRate();
        mRoot = lockscreenroot;
        mRoot.setView(this);
		mSfHolder = getHolder();
		// just like set setZOrderOnTop(true), but don't change the im layer.
		setWindowType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
		setBgBmp();
		mViewVisibile = false;
		
		mIntentReceiver = new PhoneStateChangeReceiver();
		IntentFilter intentfilter = new IntentFilter();
        //intentfilter.addAction("android.intent.action.SCREEN_OFF");
        //intentfilter.addAction("android.intent.action.SCREEN_ON");
		intentfilter.addAction("android.intent.action.PHONE_STATE");
        intentfilter.addAction("lewa.intent.action.UNLOCK");
		context.registerReceiver(mIntentReceiver, intentfilter);
		
		mViewLive = true;
			
		mSfHolder.addCallback(new Callback(){
       
        
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				Log.i("LWFACE", "surfaceCreated()");
				mViewVisibile = true;
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				Log.i("LWFACE", "surfaceDestroyed()");
				mViewVisibile = false;
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				// TODO Auto-generated method stub
				Log.i("LWFACE", "surfaceChanged()");
			}
        });
    }


    private class RenderThread extends Thread {
    
        private int mFrameTime;
        private long mLastTouchTime;
        private long mLastUpdateSystemTime;
        private long mPausedTime;
        
        private Object notify;

        public RenderThread(){
            super("LewaRender");
            notify = new Object();
            if(mFrameRate != 0){
                mFrameTime = 1000 / mFrameRate;
            } else{
                mFrameTime = 0x7fffffff;
            }
        }

        private void waiteForResume(){
            try {
                synchronized(notify){

                    notify.wait();
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onResume(){
            if(mPaused){
                return;
            }
            /*if(getVisibility() != 0){
                return;
            }*/
            synchronized(notify){
                notify.notifyAll();
            }
        }

		public void stopThread(){
            mStop = true;
            synchronized(notify){
                notify.notifyAll();
            }
        }

        public void onTouch(MotionEvent motionevent){
            
            if(SystemClock.elapsedRealtime() - mLastTouchTime >= 4000L){
                mKeyguardScreenCallback.pokeWakelock();
                mLastTouchTime = SystemClock.elapsedRealtime();
            }
            
            mRoot.onTouch(motionevent);
            mRoot.setShouldUpdate(true);
            
        }
		
        public void run(){
        

            mRoot.init();
            mPausedTime = 0L;
            
            mStarted = true;
            
            postInvalidate();
            
            
           while (true) {
                
                if(mStop){
					if(mRoot != null){
                    	mRoot.finish();
                    	mRoot = null;
					}
                    break;
                }else{
                	 if(context == null){
					 	continue;
                	 }
                     TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                     //int callState = tm.getCallState();
                     if(mPaused || getVisibility() != 0|| !pm.isScreenOn() || !hasWindowFocus()|| tm.getCallState() == TelephonyManager.CALL_STATE_RINGING){
                        
						if(!mPaused){
							mMsgHandler.sendEmptyMessage(MSG_CHECK_STATUS);
						}
                        mRoot.pause();
							
                        long realTime = SystemClock.elapsedRealtime();
                        
                        waiteForResume();
                        
						
						mMsgHandler.sendEmptyMessage(MSG_CHECK_STATUS);
									
                        long detlaTime = SystemClock.elapsedRealtime() - realTime;
                        
                        mPausedTime = detlaTime + mPausedTime; 
                        
                        mRoot.resume();
                    }
                }
                
                long updateTime = SystemClock.elapsedRealtime() - mLastUpdateSystemTime;
                
                if(updateTime > 33 || mRoot.shouldUpdate()){
                    //postInvalidate();
                    tick();
					draw();
                    mLastUpdateSystemTime = SystemClock.elapsedRealtime();
					if(mRoot != null){
                    	mRoot.setShouldUpdate(false);
					}
                }
                try {
                    Thread.currentThread().sleep(10L);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
            

        }

        
    }
	
    public void cleanUp(){
		mViewLive = false;
		
        setOnTouchListener(null);
		if(mThread != null){
			mThread.stopThread();
		}
        sleep(10L); // wait for render thread stop
        mThread = null;
        mKeyguardScreenCallback = null;
        pm = null;
        
		if(mBgBmp != null && !mBgBmp.isRecycled()){
			mBgBmp.recycle();
			mBgBmp = null;
		}
    }

    protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("android.intent.action.BATTERY_CHANGED");
        context.registerReceiver(informationReceiver, intentfilter);
        if(mThread == null){
            mThread = new RenderThread();
            mThread.start();
        }
        
    }

    protected void onDetachedFromWindow(){
        super.onDetachedFromWindow();
		if(context != null){
        	context.unregisterReceiver(informationReceiver);
			context.unregisterReceiver(mIntentReceiver);
			context = null;
		}
    }

    protected void onDraw(Canvas canvas){
		
        /*
        super.onDraw(canvas);
        if(!mStarted){
            return;
        }
        mRoot.tick(SystemClock.elapsedRealtime() - mPausedTime);

        
        mRoot.render(canvas);*/
        
    }
	
	private void tick(){
		if(mRoot != null){
    		mRoot.tick(SystemClock.elapsedRealtime() - mPausedTime);
		}
    }
	
	private void setBgBmp(){
		File bgFile;
		Bitmap bgBmp;
		String jgpFileName = "/data/system/face/wallpaper/lock_screen_wallpaper.jpg";
		String pngFileName = "/data/system/face/wallpaper/lock_screen_wallpaper.png";
		
		if(mBgBmp != null && !mBgBmp.isRecycled()){
			mBgBmp.recycle();
		}
        bgFile = new File(jgpFileName);
        if(bgFile.exists()){
        	bgBmp = BitmapFactory.decodeFile(jgpFileName);
        }else{
        	bgFile = new File(pngFileName);
			if(bgFile.exists()){
				bgBmp = BitmapFactory.decodeFile(pngFileName);
			}else{
				Log.e(TAG,"Can't open lock_screen_wallpaper.jpg & lock_screen_wallpaper.png");
				bgBmp = null;
			}
        }
        
  		mBgBmp = bgBmp;
  
	}
    protected void draw(){

		if(mBgBmp == null){
			Log.e(TAG, "lockscreen backgrand bitmap invalid.");
			return;
    	}
    	Canvas canvas = mSfHolder.lockCanvas();
    	if(canvas == null){
    		Log.w(TAG, "lockCanvas failed!");
    		return;
    	}
		int count = canvas.save();
		if(mBgBmp == null){
			mBgBmp = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvasTemp = new Canvas(mBgBmp);
			canvasTemp.drawColor(Color.GRAY);
		}
        if (DEBUG) Log.d(TAG, "canvas, width: " + canvas.getWidth() + "height: " + canvas.getHeight());
        canvas.drawBitmap(mBgBmp, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), new Paint());
		canvas.restoreToCount(count);
        mRoot.render(canvas);
        mSfHolder.unlockCanvasAndPost(canvas);
    }

    public void onPause(){
		
		
        mPaused = true;
    }

    public void onResume(){
		
        mPaused = false;
        if(mThread != null){
            mThread.onResume();
        }
    }
	
	public void hideSurfaceView(){
		if(mViewVisibile){
        	 setVisibility(View.GONE);
        }
	}
    public boolean onTouchEvent(MotionEvent motionevent){
        
        if(mThread != null){
            mThread.onTouch(motionevent);
        }
        return true;
    }

    /*public void setVisibility(int i){
        setVisibility(i);
        if(i != 0){
            return;
        }
        if(mThread != null){
            mThread.onResume();
        }
    }*/
    
    
    private BroadcastReceiver informationReceiver = new BroadcastReceiver(){
        
            private static final String TAG_NAME_NORMAL = "Normal";
            private static final String TAG_NAME_CHARGING = "Charging";
            private static final String TAG_NAME_LOWBATTERY = "BatteryLow";
            private static final String TAG_NAME_BATTERYFULL = "BatteryFull";
            
            private static final int BATTERY_NORMAL = 0;
            private static final int BATTERY_CHARGING = 1;
            private static final int BATTERY_LOW = 2;
            private static final int BATTERY_FULL = 3;
            
            public void onReceive(Context context, Intent intent){

        
                    String curCategory = TAG_NAME_NORMAL;
                    int battery_state = BATTERY_NORMAL;
                    
                    int battery_level = intent.getIntExtra("level", -1);
                    int status = intent.getIntExtra("status", -1);
                    int scale = intent.getIntExtra("scale", -1);

                    
                    
                      //TLog.i("BATT", "level:"+battery_level+", scale:"+ scale+", status:"+ status);

                    
                    if(status == BatteryManager.BATTERY_STATUS_CHARGING){
                        curCategory = TAG_NAME_CHARGING;
                        battery_state = BATTERY_CHARGING;
                    }else if(status == BatteryManager.BATTERY_STATUS_FULL){
                        curCategory = TAG_NAME_BATTERYFULL;
                        battery_state = BATTERY_FULL;
                    }else if(status == BatteryManager.BATTERY_STATUS_DISCHARGING){
                        curCategory = TAG_NAME_NORMAL;
                        battery_state = BATTERY_NORMAL;
                    }else{
                        if(battery_level<15){
                            curCategory = TAG_NAME_LOWBATTERY;
                            battery_state = BATTERY_LOW;
                        }
                    }
            
                    Expression.put("battery_level", String.valueOf(battery_level));
                    Expression.put("battery_state", String.valueOf(battery_state));
                    Expression.put("battery_category", curCategory);
                }
                
        };
    
   

      private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }
	private void changeVisibility( ){

		if(context == null){
			return;
		}
		TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		boolean callRinging = (tm.getCallState() == TelephonyManager.CALL_STATE_RINGING);
		boolean focus = hasWindowFocus();
		View parentView = (View)getParent();
		boolean parentVisible;

		if(parentView != null){
			parentVisible = parentView.getVisibility() == View.VISIBLE;
		}else{
			Log.w(TAG, "parent view == null.");
			parentVisible = true;
		}
		if(!callRinging && focus && parentVisible ){
			if(!mViewVisibile){
				Log.e(TAG, "+++> show the surfaceView");
				setVisibility(View.VISIBLE);
			}
		}else if(mViewVisibile){
			setVisibility(View.GONE);
		}
	}

	private void changeVisibility(int ignoreFlag){
		
		TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		boolean callRinging = (tm.getCallState() == TelephonyManager.CALL_STATE_RINGING);
		boolean focus = hasWindowFocus();
		View parentView = (View)getParent();
		boolean parentVisible;

		if(parentView != null){
			parentVisible = parentView.getVisibility() == View.VISIBLE;
		}else{
			Log.w(TAG, "parent view == null.");
			parentVisible = true;
		}
		if((IGNORE_FLAG_CALLING & ignoreFlag) == IGNORE_FLAG_CALLING){
			callRinging = true;
		}else if((IGNORE_FLAG_PARENT_VISIBLE & ignoreFlag) == IGNORE_FLAG_PARENT_VISIBLE){
			parentVisible = true;
		}else if((IGNORE_FLAG_WINDOW_FOCUS & ignoreFlag) == IGNORE_FLAG_WINDOW_FOCUS){
			focus = true;
		}
		
		if(!callRinging && focus && parentVisible ){
			if(!mViewVisibile){
				setVisibility(View.VISIBLE);
			}
		}else if(mViewVisibile){
			setVisibility(View.GONE);
		}
	}
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		// TODO Auto-generated method stub
		super.onVisibilityChanged(changedView, visibility);
		Log.i(TAG, "onVisibilityChanged:" + visibility + " view="+changedView);
		if(changedView.equals(getParent())){
			if(visibility == View.VISIBLE){
				onResume();
			}else{
				hideSurfaceView();
			}
		}else if(this.equals(changedView)){
			if(visibility == View.VISIBLE){
				onResume();
			}
		}
	}
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // TODO Auto-generated method stub
		super.onWindowFocusChanged(hasWindowFocus);
		Log.i(TAG, "===>onWindowFocusChanged:"+hasWindowFocus);
		if(hasWindowFocus)
		{
			/*TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

			boolean calling = (tm.getCallState() == TelephonyManager.CALL_STATE_RINGING);
		    // when incoming call, don't need to show the surfaceView.
			if(!mViewVisibile && !calling){
				changeVisibility(true);
            	setVisibility(View.VISIBLE); 
			}*/
			onResume();
		}else{
			hideSurfaceView();
		}
    }

	 private Handler mMsgHandler = new Handler(){

	@Override
	public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
		
		super.handleMessage(msg);
		if(mViewLive == false || context == null){
			Log.w(TAG, "lockscreenView already destroyed.");
			return;
		}
		
		if(msg.what == MSG_SCREEN_OFF){
			
		}else if(msg.what == MSG_SCREEN_ON){
			
		}else if(msg.what == MSG_PHONE_STATE){
			// when incoming call, hide the surfaceView.
			TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			boolean calling = (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE);
			if(!calling){
				onResume();
			}else{
				hideSurfaceView();
			}

		}else if(msg.what == MSG_CHECK_STATUS){
				changeVisibility( );
		}else if(msg.what == MSG_UNLOCK){
		    if(mRoot != null && mRoot.getUnlockerCallback() != null){
                mRoot.getUnlockerCallback().unlocked(null);      
            }else{
                Log.e(TAG, "ERROR! mRoot == null or mUnlockerCallback == null");
            }
        }
		
		
	}
	  
  };
	
	 private class PhoneStateChangeReceiver extends BroadcastReceiver{
    
        public void onReceive(Context context, Intent intent){
            
            String action = intent.getAction();

			if(context == null){
				Log.e(TAG, "error: context==null.");
				return;
			}

            if(action.equals("android.intent.action.SCREEN_ON")){

				mMsgHandler.sendEmptyMessage(MSG_SCREEN_ON);
            }else if(action.equals("android.intent.action.SCREEN_OFF")){
            	mMsgHandler.sendEmptyMessage(MSG_SCREEN_OFF);
            }else if(action.equals("android.intent.action.PHONE_STATE")){
            	mMsgHandler.sendEmptyMessage(MSG_PHONE_STATE);
            }else if(action.equals("lewa.intent.action.UNLOCK")){
                mMsgHandler.sendEmptyMessage(MSG_UNLOCK);
            }
        }
    }
}
