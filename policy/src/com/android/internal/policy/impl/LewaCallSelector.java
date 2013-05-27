package com.android.internal.policy.impl;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.Scroller;
import android.widget.ImageView;
import android.view.View;

import com.android.internal.R;

public class LewaCallSelector extends FrameLayout implements AnimationListener{
    
    private OnCallDialTriggerListener mOnDialTriggerListener;
    private boolean mLenseMode=true;
    private int mGrabbedState = NOTHING_GRABBED;
    public static final int NOTHING_GRABBED = 0;
    public static final int LEFT_HANDLE_GRABBED = 1;
    public static final int MID_HANDLE_GRABBED = 2;
    public static final int RIGHT_HANDLE_GRABBED = 3;
    
	private static String TAG = "LewaCallSelector";
	private static boolean DBG = false;
	private static int unlockpositon = 692;//include systembar height
	
	private int mLeft;
	private int mTop;
	private int mRight;
	private int mBottom;
	
	private float mLastY;
	private int mDeltaY;
	private int mHeight = 108;
	private Scroller mScroller;
	private Context mContext;
	private TranslateAnimation mTranslateAnimation;
	
	private float firstRawY;
	private boolean firstTouch = true;
	private ImageView miss_call_bg = null;

	private float mDensity;

	public LewaCallSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mScroller = new Scroller(context);
		mDensity = context.getResources().getDisplayMetrics().density;
		if(mDensity == 1)
		{
			unlockpositon = 408;
		}
		// TODO Auto-generated constructor stub
	}
	
	public LewaCallSelector(Context context) {
		super(context);
		mContext = context;
		mScroller = new Scroller(context);
		mDensity = context.getResources().getDisplayMetrics().density;
		if(mDensity == 1)
		{
			unlockpositon = 408;
		}
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		final int action = event.getAction();
        final float currentY = event.getRawY();
        if (miss_call_bg == null) {
            miss_call_bg = (ImageView)this.findViewById(R.id.miss_call_bg);
        }
        if(event.getRawY() - firstRawY < 0){
	        return true;
	    }
        switch (action) {     
		case MotionEvent.ACTION_DOWN:
			log("ACTION_DOWN");
			LewaLockScreen_zb.setUnlocktipsText(R.string.lock_screen_drag_here);
            LewaLockScreen_zb.setUnlocktipsVisible(VISIBLE);
            LewaLockScreen_zb.setUnlocklineVisible(VISIBLE); 
			if(firstTouch){
				firstRawY = event.getRawY();
	            firstTouch = false;
	            mLeft = getLeft();
	            mTop = getTop();
	            mRight = getRight();
	            mBottom = getBottom();   
	            mHeight = getHeight();
	            log("firstRawY "+firstRawY);
	            log("getHeight "+getHeight());
	            log(" getLeft "+getLeft()+" getTop "+getTop()+" getRight "+getRight()+" getBottom "+getBottom());
	        }
            mLastY = currentY - getTop();
            if (mLenseMode){
                setGrabbedState(MID_HANDLE_GRABBED);
            }
            miss_call_bg.setBackgroundResource(R.drawable.lock_noti_bg_hi);
            break; 
		case MotionEvent.ACTION_MOVE:
			log("ACTION_MOVE");
			if (mLenseMode){
                setGrabbedState(MID_HANDLE_GRABBED);
            }
			log("event.getRawY() = "+event.getRawY());
			layout(0, (int)(currentY - mLastY), getRight(), (int)(currentY - mLastY + mHeight));
			break;
		case MotionEvent.ACTION_UP:
			log("ACTION_UP");
			log("event.getRawY() = "+event.getRawY());
			//LewaLockScreen.setUnlocktipsVisible(GONE);
            LewaLockScreen_zb.setUnlocklineVisible(GONE);
			miss_call_bg.setBackgroundResource(R.drawable.lock_noti_bg);
			
			if (LewaLockScreen_zb.mPluggedIn) {
                if (LewaLockScreen_zb.mBatteryLevel < 100) {
                    LewaLockScreen_zb.unlock_tips.setText(getContext().getString(R.string.lockscreen_plugged_in, LewaLockScreen_zb.mBatteryLevel));
                    LewaLockScreen_zb.unlock_tips.setVisibility(View.VISIBLE);
                } else {
                    LewaLockScreen_zb.unlock_tips.setText(getContext().getString(R.string.lockscreen_charged));
                    LewaLockScreen_zb.unlock_tips.setVisibility(View.VISIBLE);
                }
            } else {
                LewaLockScreen_zb.setUnlocktipsVisible(GONE);
            }
			
			if (upInUnlockPosition(event.getRawY())) {
			    if(mLenseMode)
                    dispatchTriggerEvent(OnCallDialTriggerListener.LEFT_HANDLE);
                else
                    dispatchTriggerEvent(OnCallDialTriggerListener.MID_HANDLE);
			    
			    Intent intent = new Intent(Intent.ACTION_VIEW);
	            intent.setType("vnd.android.cursor.dir/calls");
	            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            mContext.startActivity(intent);
				
			} else {	
			    layout(mLeft, mTop, mRight, mBottom);
    		    //float DeltaY = event.getRawY() - firstRawY;
			    float DeltaY = getTop() - mTop;
    			mTranslateAnimation = new TranslateAnimation(0, 0, DeltaY, 0);
    			mTranslateAnimation.setAnimationListener(this);
    			//mTranslateAnimation.setFillAfter(true);
    			mTranslateAnimation.setDuration(800);
    			startAnimation(mTranslateAnimation);	
			}
			setGrabbedState(NOTHING_GRABBED);
			break;
		default:
			break;
		}        
		return true;
	}
	
	private boolean upInUnlockPosition(float RawY) {
		log("RawY = "+RawY);		
		if (RawY > unlockpositon) {
			log("upInUnlockPosition true!!!");
			return true;
		} else {
			log("upInUnlockPosition false");		
			return false;
		}		
	}
	
	private void log(String msg) {
		if (DBG) {
			Log.i(TAG, "zhangbo " + msg);
		}		
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationStart(Animation animation) {
		// TODO Auto-generated method stub
		//layout(mLeft, mTop, mRight, mBottom);
	}	
	
	public void setLenseSquare(boolean newState){
        mLenseMode=false;
        if(newState){
            mLenseMode=true;
        }
    }
    
    public void setOnCallDialTriggerListener(OnCallDialTriggerListener l) {
        mOnDialTriggerListener = l;
    }
    
    private void setGrabbedState(int newState) {
        if (true/*newState != mGrabbedState*/) {
            mGrabbedState = newState;
            if (mOnDialTriggerListener != null) {
                mOnDialTriggerListener.onCallGrabbedStateChange(this, mGrabbedState);
            }
        }
    }
    
    private void dispatchTriggerEvent(int whichHandle) {
        //vibrate();
        if (mOnDialTriggerListener != null) {
            mOnDialTriggerListener.onCallDialTrigger(this, whichHandle);
        }
    }
    
	public interface OnCallDialTriggerListener {
        /**
         * The dial was triggered because the user grabbed the left handle,
         * and rotated the dial clockwise.
         */
        public static final int LEFT_HANDLE = 1;

        /**
         * The dial was triggered because the user grabbed the middle handle,
         * and moved the dial down.
         */
        public static final int MID_HANDLE = 2;

        /**
         * The dial was triggered because the user grabbed the right handle,
         * and rotated the dial counterclockwise.
         */
        public static final int RIGHT_HANDLE = 3;

        /**
         * Called when the dial is triggered.
         *
         * @param v The view that was triggered
         * @param whichHandle  Which "dial handle" the user grabbed,
         *        either {@link #LEFT_HANDLE}, {@link #RIGHT_HANDLE}.
         */
        void onCallDialTrigger(View v, int whichHandle);

        /**
         * Called when the "grabbed state" changes (i.e. when
         * the user either grabs or releases one of the handles.)
         *
         * @param v the view that was triggered
         * @param grabbedState the new state: either {@link #NOTHING_GRABBED},
         * {@link #LEFT_HANDLE_GRABBED}, or {@link #RIGHT_HANDLE_GRABBED}.
         */
        void onCallGrabbedStateChange(View v, int grabbedState);
    }
}

