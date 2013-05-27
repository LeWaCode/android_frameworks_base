package com.android.internal.policy.impl;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.Scroller;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.internal.R;
import android.content.Intent;
import android.widget.ViewFlipper;

public class LewaRotarySelector extends FrameLayout{
    

    private OnDialTriggerListener mOnDialTriggerListener;
    private boolean mLenseMode=true;
    private int mGrabbedState = NOTHING_GRABBED;
    public static final int NOTHING_GRABBED = 0;
    public static final int LEFT_HANDLE_GRABBED = 1;
    public static final int MID_HANDLE_GRABBED = 2;
    public static final int RIGHT_HANDLE_GRABBED = 3;
    
    
	private static String TAG = "LewaRotarySelector";
	private static boolean DBG = false;
	private static int unlockpositon = -300;//include systembar height
	
	private float mLastX;
	private float mLastY;
	private int mDeltaX;
	private int mDeltaY;
	private Scroller mScroller;

	private float mDensity;
	//[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
	private Context context;
	private ViewFlipper view_flipper;
	private boolean isAlarmNotify;
	private boolean hasAlreadyNotify = false;
	private int alarmId = -1;
	//[End]
	
	public LewaRotarySelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		//mScroller = new Scroller(context);
		mScroller = new Scroller(getContext(), new DecelerateInterpolator(2f));
		// TODO Auto-generated constructor stub
		//[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
		this.context = context;
		//[End]
		mDensity = context.getResources().getDisplayMetrics().density;
		if(mDensity == 1)
		{
			unlockpositon = -160;
		}
	}
	
	public LewaRotarySelector(Context context) {
		super(context);
		//mScroller = new Scroller(context);
		mScroller = new Scroller(getContext(), new DecelerateInterpolator(2f));
		// TODO Auto-generated constructor stub
		//[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
		this.context = context;
		//[End]
		mDensity = context.getResources().getDisplayMetrics().density;
		if(mDensity == 1)
		{
			unlockpositon = -160;
		}
	}
	
	//[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
	public void setViewFlipper(ViewFlipper view_flipper){
		this.view_flipper = view_flipper;
	}

	public ViewFlipper getViewFlipper(){
		return view_flipper;
	}

	public void setIsAlarmNotify(boolean isAlarmNotify){
		this.isAlarmNotify = isAlarmNotify;
	}

	public boolean getIsAlarmNotify(){
		return isAlarmNotify;
	}

	public void setHasAlreadyNotify(boolean hasAlreadyNotify){
		this.hasAlreadyNotify = hasAlreadyNotify;
	}

	public boolean getHasAlreadyNotify(){
		return hasAlreadyNotify;
	}

	public void setAlarmId(int alarmId){
		this.alarmId = alarmId;
	}

	public int getAlarmId(){
		return alarmId;
	}
	
	private void dismiss() {
	    Intent intent = new Intent("com.lewa.action.AlarmLockScreen");
	    context.sendBroadcast(intent);
        context.stopService(new Intent("com.android.deskclock.ALARM_ALERT"));
        context.stopService(new Intent("com.lewa.action.AlarmNotifationService"));
    }

    private void snooze() {
        Intent cancelAlarm = new Intent("com.lewa.action.AlarmLockScreen");
        context.sendBroadcast(cancelAlarm);
        Intent intent = new Intent("com.lewa.action.AlarmNotifation");
        intent.putExtra("alarm_id",getAlarmId());
        context.sendBroadcast(intent);
        context.stopService(new Intent("com.lewa.action.AlarmNotifationService"));
    }
    //[End]
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		final int action = event.getAction();
		final float currentX = event.getX();
        final float currentY = event.getY();
        switch (action) {       
        case MotionEvent.ACTION_CANCEL:			
			break;
		case MotionEvent.ACTION_DOWN:
			log("ACTION_DOWN");
			LewaLockScreen_zb.setUnlocktipsText(R.string.lock_screen_drag_down);
			LewaLockScreen_zb.setUnlocktipsVisible(VISIBLE); 
                   //LewaLockScreen.setUnlocklineVisible(VISIBLE);
			mLastX = currentX;
			mLastY = currentY;
			log("mLastX = "+mLastX+" mLastY = "+ mLastY);
			if (mLenseMode){
                setGrabbedState(MID_HANDLE_GRABBED);
            }
			break;
		case MotionEvent.ACTION_MOVE:
			log("ACTION_MOVE");
			if (mLenseMode){
                setGrabbedState(MID_HANDLE_GRABBED);
            }
			//[Begin fulianwu, for lockscreen alarm,2011_10_19,modify]
			boolean isAlarmNotify = getIsAlarmNotify();

			if(isAlarmNotify){
				mDeltaX = (int)(mLastX - currentX);
				mLastX = currentX;
				
				ViewFlipper view_flipper = getViewFlipper();
				
				if (view_flipper != null && mDeltaX > 30 ){				
					view_flipper.setInAnimation(context, R.anim.push_left_in);
	            	view_flipper.setOutAnimation(context, R.anim.push_left_out);
	            	view_flipper.showPrevious();
					setIsAlarmNotify(false);
					setHasAlreadyNotify(false);

					snooze();
				}

				if(view_flipper != null && mDeltaX < -30) {	            	
					view_flipper.setInAnimation(context, R.anim.push_right_in);
	            	view_flipper.setOutAnimation(context, R.anim.push_right_out);
	            	view_flipper.showNext();
					setIsAlarmNotify(false);
					setHasAlreadyNotify(false);

					dismiss();
				}				
			}else{
			mDeltaY = (int)(mLastY - currentY);
			log("mLastY = "+mLastY+" currentY = "+currentY+" mDeltaY = "+ mDeltaY);
			//mLastX = currentX;
			mLastY = currentY;
			/*if (moveToUnlockPosition()) {
				log("move to unlock!!!");
				scrollTo(0, unlockpositon);
				break;
			}*/	
			int ScrollY = getScrollY();
			log("ScrollY = "+ScrollY);	
			if (mDeltaY < 0) {
				scrollBy(0, mDeltaY);				
			} 
			else {
				if (ScrollY + mDeltaY > 0) {
					scrollTo(0, 0);
				} else {
					scrollBy(0, mDeltaY);
				}				
			}
			}
			postInvalidate();
			break;
		case MotionEvent.ACTION_UP:
			log("ACTION_UP");
            //LewaLockScreen.setUnlocktipsVisible(GONE);
            LewaLockScreen_zb.setUnlocklineVisible(GONE);
            
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
            
			if (upInUnlockPosition()) {
				log("up in unlock!!!");
				if(mLenseMode)
                    dispatchTriggerEvent(OnDialTriggerListener.LEFT_HANDLE);
                else
                    dispatchTriggerEvent(OnDialTriggerListener.MID_HANDLE);
			} else {
				mScroller.startScroll(0, getScrollY(), 0, 0, Math.abs(getScrollY())*2);
				invalidate();
			}
			setGrabbedState(NOTHING_GRABBED);
			break;

		default:
			break;
		}        
		return true;
	}

	@Override
	public void computeScroll() {
		// TODO Auto-generated method stub
		if (mScroller.computeScrollOffset()) { 
            scrollTo(0, 0); 
            //postInvalidate();
        } 
	}

	public void Init() {
            scrollTo(0, 0); 
	}

      

	private boolean moveToUnlockPosition() {
		int ScrollY = getScrollY();
		log("ScrollY = "+ScrollY);		
		if (ScrollY + mDeltaY > unlockpositon) {
			log("moveToUnlockPosition false");
			return false;
		} else {
			log("moveToUnlockPosition true!!!");		
			return true;
		}		
	}
	
	private boolean upInUnlockPosition() {
		int ScrollY = getScrollY();
		log("ScrollY = "+getScrollY());		
		if (ScrollY > unlockpositon) {
			log("upInUnlockPosition false");
			return false;
		} else {
			log("upInUnlockPosition true!!!");		
			return true;
		}		
	}
	
	private void log(String msg) {
		if (DBG) {
			Log.i(TAG, "zhangbo " + msg);
		}		
	}	
	
    public void setLenseSquare(boolean newState){
        mLenseMode=false;
        if(newState){
            mLenseMode=true;
        }
    }
    
	public void setOnDialTriggerListener(OnDialTriggerListener l) {
        mOnDialTriggerListener = l;
    }
	
    private void setGrabbedState(int newState) {
        if (true/*newState != mGrabbedState*/) {
            mGrabbedState = newState;
            if (mOnDialTriggerListener != null) {
                mOnDialTriggerListener.onGrabbedStateChange(this, mGrabbedState);
            }
        }
    }
	
    private void dispatchTriggerEvent(int whichHandle) {
        //vibrate();
        if (mOnDialTriggerListener != null) {
            mOnDialTriggerListener.onDialTrigger(this, whichHandle);
        }
    }
    
	public interface OnDialTriggerListener {
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
	        void onDialTrigger(View v, int whichHandle);

	        /**
	         * Called when the "grabbed state" changes (i.e. when
	         * the user either grabs or releases one of the handles.)
	         *
	         * @param v the view that was triggered
	         * @param grabbedState the new state: either {@link #NOTHING_GRABBED},
	         * {@link #LEFT_HANDLE_GRABBED}, or {@link #RIGHT_HANDLE_GRABBED}.
	         */
	        void onGrabbedStateChange(View v, int grabbedState);
	    }
}
