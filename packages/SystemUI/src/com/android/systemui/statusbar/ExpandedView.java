/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import com.android.systemui.statusbar.switchwidget.SwitchWidget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.util.Slog;

import android.widget.Scroller;
import android.view.ViewConfiguration;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.DecelerateInterpolator;
import android.view.VelocityTracker;
import android.util.Log;

public class ExpandedView extends LinearLayout {
	
	private String TAG=ExpandedView.class.getSimpleName();
	
    StatusBarService mService;
    ItemTouchDispatcher mTouchDispatcher;
    int mPrevHeight = -1;

    private float mLastMotionX;
    private float mLastMotionY;
    private int mTouchSlop;
    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    private final static int TOUCH_SWIPE_DOWN_GESTURE = 2;
    private final static int TOUCH_SWIPE_UP_GESTURE = 3;
    private final static int VELOCITY_UNITS = 1000;
    private final static int SNAP_VELOCITY = 1300; 
    private int mTouchState = TOUCH_STATE_REST;
    private Scroller mScroller = null;
    private VelocityTracker mVelocityTracker;
    
	private static final int SWIPE_MIN_DISTANCE = 100;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 50;
	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
        
    public ExpandedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context, new DecelerateInterpolator(1.5f));
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mVelocityTracker = VelocityTracker.obtain();
        
        gestureDetector=new GestureDetector(new StatusBarGestureDetector());
        gestureListener=new View.OnTouchListener() {			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
    }

    @Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
    	if(gestureDetector.onTouchEvent(ev)){
    		Log.d(TAG,"set touch action cancel");
    		ev.setAction(MotionEvent.ACTION_CANCEL);
    	}
		return super.dispatchTouchEvent(ev);
	}
    
	class StatusBarGestureDetector extends SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
					return false;
				}
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {					
					try {
						if(mService.getmTab()==StatusBarService.TAB_NOTIFICATIONS){
							if(mService.getmStyle()==SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES){
								mService.setmTab(StatusBarService.TAB_SWITCHES);
								mService.onTabChange();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {					
					try {
						if(mService.getmTab()==StatusBarService.TAB_SWITCHES){
							if(mService.getmStyle()==SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES){
								mService.setmTab(StatusBarService.TAB_NOTIFICATIONS);
								mService.onTabChange();
							}
						}	
					} catch (Exception e) {
						e.printStackTrace();
					}			
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	@Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    /** We want to shrink down to 0, and ignore the background. */
    @Override
    public int getSuggestedMinimumHeight() {
        return 0;
    }

    @Override
     protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
         super.onLayout(changed, left, top, right, bottom);
         int height = bottom - top;
         if (height != mPrevHeight) {
             //Slog.d(StatusBarService.TAG, "height changed old=" + mPrevHeight
             //     + " new=" + height);
             mPrevHeight = height;
             mService.updateExpandedViewPos(StatusBarService.EXPANDED_LEAVE_ALONE);
         }
     }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();
            if ((action == MotionEvent.ACTION_MOVE)
                        && (mTouchState != TOUCH_STATE_REST)) {
                return true;
            }
            final float x = ev.getX();
            final float y = ev.getY();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastMotionX = x;
                    mLastMotionY = y;
                    mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
                                    : TOUCH_STATE_SCROLLING;
                    break;                  
                case MotionEvent.ACTION_MOVE:
                    final int xDiff = (int) Math.abs(x - mLastMotionX);
                    final int yDiff = (int) Math.abs(y - mLastMotionY);
                    final int touchSlop = mTouchSlop;

                    mVelocityTracker.addMovement(ev);
                    mVelocityTracker.computeCurrentVelocity(VELOCITY_UNITS);
                    float yVelocity = mVelocityTracker.getYVelocity();
                    
                    boolean xMoved = xDiff > touchSlop;
                    boolean yMoved = yDiff > touchSlop;
                    if (xMoved || yMoved) {
                        if (xDiff < yDiff) {
                            if ((y - mLastMotionY) < 0) {
                                if (Math.abs(y - mLastMotionY) > (touchSlop * 2)) {
                                    if (yVelocity < -SNAP_VELOCITY) {
                                        mTouchState = TOUCH_SWIPE_UP_GESTURE;
                                    } else {
                                        mTouchState = TOUCH_STATE_REST;
                                    }
                                }
                            } else {
                                if (Math.abs(y - mLastMotionY) > (touchSlop * 2)) {
                                    if (yVelocity > SNAP_VELOCITY) {
                                        mTouchState = TOUCH_SWIPE_DOWN_GESTURE;
                                    } else {
                                        mTouchState = TOUCH_STATE_REST;
                                    }
                                }
                            }
                        } else {
                            mTouchState = TOUCH_STATE_REST;//TOUCH_STATE_SCROLLING;
                        }
                    }
                    break;              
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mTouchState = TOUCH_STATE_REST;
                    break;
                default :
                    break;
            }
            return mTouchState != TOUCH_STATE_REST;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:                  
                    break;               
                case MotionEvent.ACTION_MOVE:               
                    break;              
                case MotionEvent.ACTION_UP:
                    if (mTouchState == TOUCH_SWIPE_DOWN_GESTURE) {

                    } else if (mTouchState == TOUCH_SWIPE_UP_GESTURE) {
                        mService.animateCollapse();
                    }
                    mTouchState = TOUCH_STATE_REST;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mTouchState = TOUCH_STATE_REST;
                    break;
                default :
                    break;
            }
            return true;
        }
}
