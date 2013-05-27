/*
 * Copyright (C) 2011 Ndoo Internet Technology
 *
 * @version: 1.0
 * @since: 2011/07/21
 * @update: 2011/07/21
 *
 * @description: Implementation of the class SwitchWidget
 *
 * @author: Woody Guo <guozhenjiang@ndoo.net>
 */

package com.android.systemui.statusbar.switchwidget;

import android.view.MotionEvent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wimax.WimaxHelper;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.HashMap;

public class SwitchWidget extends FrameLayout {
    public static final boolean DEBUG = false;
    private static final String TAG = "SwitchWidget";

    public static final int SWITCH_WIDGET_STYLE_SINGLE_PAGE = 1;
    public static final int SWITCH_WIDGET_STYLE_DUAL_PAGES = 2;

    private static final int BUTTONS_PER_LINE = 4;
    private static final int BUTTONS_PER_LINE_H = 6;
//    private static final int BUTTONS_PER_LINE_TINY = 6;
    private static final int BUTTONS_PER_LINE_TINY = 4;
    private static final int BUTTONS_PER_LINE_TINY_H = 12;

    private static final FrameLayout.LayoutParams WIDGET_LAYOUT_PARAMS
            = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, // width = match_parent
            ViewGroup.LayoutParams.WRAP_CONTENT  // height = wrap_content
            );

    private static final LinearLayout.LayoutParams LINE_LAYOUT_PARAMS
            = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, // width = match_parent
            ViewGroup.LayoutParams.WRAP_CONTENT, // height = wrap_content
            1.0f                                 // weight = 1
            );

    private static final LinearLayout.LayoutParams BUTTON_LAYOUT_PARAMS
            = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, // width = wrap_content
            ViewGroup.LayoutParams.WRAP_CONTENT,  // height = match_parent
            1.0f                                // weight = 1
            );

    private LayoutInflater mInflater;
    private WidgetBroadcastReceiver mBroadcastReceiver = null;
    private WidgetSettingsObserver mObserver = null;
    private int mMode;
    private int mButtonsPerLine;

    private HorizontalScrollView mScrollView;
    private float mX, mCurX;
    private int mDeltaX, mDeltaXxx;
    
    //add by zhangxianjia
    public int mHasSetup = 0;
    
    //added by zhuyaopeng 2012/07/09
    private String BUILD_MODEL_S5830="GT-S5830";    
    private String BUILD_MODEL_G13="Wildfire S";
    private String BUILD_MODEL_U8800="U8800";
    private String BUILD_MODEL_U880="U880";
    private String BUILD_MODEL_A60="Lenovo A60";
    
    private String tempBigSwitches="";

    public SwitchWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mMode = SWITCH_WIDGET_STYLE_DUAL_PAGES;  // Big icons mode
        mX = mCurX = 0;
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Switches icons mode between Big and Tiny
     */
    public void switchMode() {
/*
 *         if (mMode == SWITCH_WIDGET_STYLE_DUAL_PAGES) mMode = SWITCH_WIDGET_STYLE_SINGLE_PAGE;
 *         else mMode = SWITCH_WIDGET_STYLE_DUAL_PAGES;
 * 
 *         updateButtonLayoutWidth();
 *         setupWidget();
 *         updateWidget();
 */

        Settings.System.putInt(getContext().getContentResolver(), Settings.System.SWITCH_WIDGET_STYLE
                , (mMode == SWITCH_WIDGET_STYLE_DUAL_PAGES
                ? SWITCH_WIDGET_STYLE_SINGLE_PAGE : SWITCH_WIDGET_STYLE_DUAL_PAGES));
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Set icons mode
     */
    public void setMode(final int mode) {
        if (mMode != mode) {
            mMode = mode;

            updateButtonLayoutWidth();
            setupWidget();
            updateWidget();
        }
    }

    public void setupWidget(final int mode) {
        mMode = mode;
	 updateButtonLayoutWidth();
        setupWidget();
	 updateWidget();
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Loads buttons from a string; register broadcast receiver and observer
     */
    public void setupWidget() {
        // Remove all views from the layout
        removeAllViews();
        //add by zhangxianjia for promote the expandview speed
        mHasSetup = 1;

        Context context = getContext();
        SwitchButton.sContext = context;
        
        // setOnLongClickListener(null);
        // Unregister our content receiver
        if(mBroadcastReceiver != null) {
            context.unregisterReceiver(mBroadcastReceiver);
        }
        // Unobserve our content
        if(mObserver != null) {
            mObserver.unobserve();
        }

        // Clear the button instances
        SwitchButton.unloadAllButtons();

        if (mMode == SWITCH_WIDGET_STYLE_DUAL_PAGES) {
            loadModeBig();
        } else{
            loadModeTiny();
        }

        if(mBroadcastReceiver == null) {
            mBroadcastReceiver = new WidgetBroadcastReceiver();
        }

        IntentFilter filter = SwitchButton.getAllBroadcastIntentFilters();
        filter.addAction(Settings.SETTINGS_CHANGED);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        context.registerReceiver(mBroadcastReceiver, filter);

        // setOnLongClickListener(mLongClickListener);

        if(mObserver != null) {
            mObserver.observe();
        }
    }

    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            switchMode();
            return true;
        }
    };

	public static String doStr(String str){
		String[] btns=str.split("\\|");
		String newButtons="";
		for(int i=0;i<btns.length;i++){
			if(i<11){
				newButtons+=btns[i]+"|";
			}else{
				break;
			}			
		}
		return newButtons;
	}
	
	public int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
	
	private String doSwitches(String buttons){
//		Log.d(TAG,"before="+buttons);
        if(!BUILD_MODEL_S5830.equalsIgnoreCase(Build.MODEL) && !BUILD_MODEL_G13.equalsIgnoreCase(Build.MODEL)){
        	buttons=buttons.replace(SwitchButton.BUTTON_TORCH+SwitchButton.BUTTON_DELIMITER,"");
        	if(BUILD_MODEL_U880.equalsIgnoreCase(Build.MODEL) || BUILD_MODEL_A60.equalsIgnoreCase(Build.MODEL)){//these two mobiles not have night mode
        		buttons+=SwitchButton.BUTTON_NETWORKMODE + SwitchButton.BUTTON_DELIMITER;
        	}else{
        		buttons+=SwitchButton.BUTTON_BRIGHTNESS + SwitchButton.BUTTON_DELIMITER;
        	}           
//          Log.d(TAG,"after="+buttons);
        }
        return buttons;
	}
    
    private void loadModeBig() {
        Context context = getContext();
        String buttons = Settings.System.getString(context.getContentResolver(), Settings.System.SWITCH_WIDGET_BUTTONS);
//        Log.e(TAG,"loadModeBig(),buttons=="+buttons);
		if(buttons!=null){
	        buttons=doStr(buttons);
	        buttons+=SwitchButton.BUTTON_SWITCH_WIDGET_STYLE;
//	        Log.e(TAG,"loadModeBig(),buttons!=null,final buttons=="+buttons);
	        tempBigSwitches=buttons;
                if(buttons.contains(SwitchButton.BUTTON_POWER_MANAGER)) {
                    buttons = null;
                }
		}
        
        if (buttons == null) {
            buttons = SwitchButton.BUTTONS_ALL;
            buttons=doStr(buttons);
            buttons=doSwitches(buttons);
            buttons+=SwitchButton.BUTTON_SWITCH_WIDGET_STYLE;
            if(BUILD_MODEL_U880.equalsIgnoreCase(Build.MODEL) || BUILD_MODEL_A60.equalsIgnoreCase(Build.MODEL)){
            	Log.d(TAG,"is u880,final buttons="+buttons);
            }
        }
        SwitchButton.TINY_MODE = false;
        updateButtonLayoutWidth();

        // Create a LinearLayout to hold child LinearLayouts which
        // each holds mButtonsPerLine buttons in a single line
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        ll.setPadding(0, dip2px(context,4.0f), 0, 0);
        String[] button = buttons.split("\\|");
        int count = button.length;
        int lines = (count / mButtonsPerLine) + ((count % mButtonsPerLine != 0) ? 1:0 );
        
//        Log.i(TAG,"buttons=="+buttons+",button count=="+count+",lines=="+lines);

        for (int i = 0; i < lines - 1; ++i) {
            // Inflate our button
            View lineView = mInflater.inflate(R.layout.switch_line, null, false);
            lineView.setPadding(dip2px(getContext(), 4.0f), 0, dip2px(getContext(), 4.0f), 0);
            LinearLayout lll = (LinearLayout) lineView.findViewById(R.id.ll_switch_line);
            for (int j = 0; j < mButtonsPerLine; ++j) {
                View bt = mInflater.inflate(R.layout.switch_icon, null, false);
                bt.setPadding(dip2px(getContext(),3.0f), dip2px(getContext(), 12.0f), dip2px(getContext(), 3.0f), dip2px(getContext(), 12.0f));
                View v = bt.findViewById(R.id.v_switch_icon);
                SwitchButton.loadButton(button[i*mButtonsPerLine+j], v);
                lll.addView(bt, BUTTON_LAYOUT_PARAMS);
            }

            ll.addView(lineView, LINE_LAYOUT_PARAMS);
        }

        // Add the buttons in the last line
        View lineView2 = mInflater.inflate(R.layout.switch_line, null, false);
        lineView2.setPadding(dip2px(getContext(), 4.0f), 0, dip2px(getContext(), 4.0f), 0);
        LinearLayout llll = (LinearLayout) lineView2.findViewById(R.id.ll_switch_line);
        int j = (lines-1)*mButtonsPerLine;
        for (; j < count; ++j) {
            View bt = mInflater.inflate(R.layout.switch_icon, null, false);
            bt.setPadding(dip2px(getContext(),3.0f), dip2px(getContext(), 12.0f), dip2px(getContext(), 3.0f), dip2px(getContext(), 12.0f));
            View v = bt.findViewById(R.id.v_switch_icon);
            SwitchButton.loadButton(button[j], v);
            llll.addView(bt, BUTTON_LAYOUT_PARAMS);
        }
        ll.addView(lineView2, LINE_LAYOUT_PARAMS);

        addView(ll, WIDGET_LAYOUT_PARAMS);
        
        updateWidget();
    }
    
    private String removeTorch(String buttons){
    	if(!BUILD_MODEL_S5830.equalsIgnoreCase(Build.MODEL) && !BUILD_MODEL_G13.equalsIgnoreCase(Build.MODEL)){
    		buttons=buttons.replace(SwitchButton.BUTTON_TORCH+SwitchButton.BUTTON_DELIMITER,"");
    	}
    	return buttons;
    }

    private void loadModeTiny() {
        Context context = getContext();
        String buttons = Settings.System.getString(context.getContentResolver(), Settings.System.SWITCH_WIDGET_BUTTONS_TINY);
//        Log.e(TAG,"loadModeTiny,buttons=="+buttons);
        if(buttons!=null){
	        buttons=doStr(buttons);
	        buttons+=SwitchButton.BUTTON_SWITCH_WIDGET_STYLE;
//	        Log.e(TAG,"loadModeTiny,buttons!=null,=="+buttons);
	        if(!tempBigSwitches.equals("") && !tempBigSwitches.equals(buttons)){
	        	Log.e(TAG,"diff,buttons="+buttons);
	        }
	        buttons=removeTorch(buttons);
		}
        
        if (buttons == null) {
            buttons = SwitchButton.BUTTONS_ALL;
            buttons=doStr(buttons);
            doSwitches(buttons);
            buttons+=SwitchButton.BUTTON_SWITCH_WIDGET_STYLE;
            
            buttons=removeTorch(buttons);
        }

        SwitchButton.TINY_MODE = true;
        updateButtonLayoutWidth();

        // create a linearlayout to hold our buttons
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        int buttonCount = 0;
        for(String button : buttons.split("\\|")) {
            if (DEBUG) Log.d(TAG, "Setting up button: " + button);
            View buttonView = mInflater.inflate(R.layout.switch_icon, null, false);
            View v = buttonView.findViewById(R.id.v_switch_icon);

            if (SwitchButton.loadButton(button, v)) {
                ll.addView(buttonView, BUTTON_LAYOUT_PARAMS);
                buttonCount++;
            }
        }

        // Determines if a horizontal scroll view is needed
        // based on a threshold of button counts
        if(buttonCount > mButtonsPerLine) {
            // mScrollView = new PageHorizontalScrollView(context);
            mScrollView = new HorizontalScrollView(context);
            mScrollView.setClickable(true);

            mScrollView.setOnTouchListener(new OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Need to find out why this event is not triggered
                            // Probably due to false returned from HorizontalScrollView.onTouchEvent
                            // and onInterceptTouchEvent?
                            if (DEBUG) {
                                Log.d(TAG, "HorizontalScrollView.onTouchListener() ACTION_DOWN on X: " + Float.toString(event.getX()));
                            }
                            mX = event.getX();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // Does not need to track this movement if ACTION_DOWN is triggered
                            if (mX == 0) mX = event.getX();
                            if (DEBUG) {
                                Log.d(TAG, "HorizontalScrollView.onTouchListener() ACTION_MOVE on X: " + Float.toString(event.getX()));
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            mCurX = event.getX();
                            final int deltaX = (int) (mCurX - mX);
                            if (DEBUG) {
                                Log.d(TAG, "HorizontalScrollView.onTouchListener() ACTION_UP on X: " + Float.toString(mCurX) + " Delta: " + Integer.toString(deltaX));
                            }
                            final int absDeltaX = deltaX < 0 ? -deltaX : deltaX;
                            if (absDeltaX > mDeltaX) {
				// forbidden fullscroll 
                               // if (absDeltaX > mDeltaXxx) {
                               //     mScrollView.fullScroll(deltaX > 0 ? FOCUS_LEFT : FOCUS_RIGHT);
                                //} else {
                                    mScrollView.pageScroll(deltaX > 0 ? FOCUS_LEFT : FOCUS_RIGHT);
                                //}
                                computeScroll();
                                mX = 0;
                            }
                            break;
                    }
                    return true;
                }
            });
            // Makes the fading edge the size of a button
            // (makes it more noticible that we can scroll)
            mScrollView.setFadingEdgeLength(context
                    .getResources().getDisplayMetrics().widthPixels / mButtonsPerLine);
            mScrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            mScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            // Sets the padding on the linear layout to the size of
            // our scrollbar, so we don't have them overlap
//            ll.setPadding(ll.getPaddingLeft(), ll.getPaddingTop(), ll.getPaddingRight(), mScrollView.getVerticalScrollbarWidth());
            ll.setPadding(ll.getPaddingLeft(),ll.getPaddingTop(), ll.getPaddingRight(),0);
//            Log.e(TAG,"mScrollView height=="+mScrollView.getVerticalScrollbarWidth());//10
            mScrollView.addView(ll, WIDGET_LAYOUT_PARAMS);
            mScrollView.setHorizontalScrollBarEnabled(false);
            mScrollView.setHorizontalFadingEdgeEnabled(false);
            addView(mScrollView, WIDGET_LAYOUT_PARAMS);
        } else {
            // not needed, just add the linear layout
            addView(ll, WIDGET_LAYOUT_PARAMS);
        }
        
        updateWidget();
    }

    public void updateWidget() {
    	Log.i(TAG,"update widget!~");
        SwitchButton.updateAllButtons();
    }

    public void setupSettingsObserver(Handler handler) {
        if(mObserver == null) {
            mObserver = new WidgetSettingsObserver(handler);
        }
    }

    public void setGlobalButtonOnClickListener(View.OnClickListener listener) {
        SwitchButton.setGlobalOnClickListener(listener);
    }

    public void setGlobalButtonOnLongClickListener(View.OnLongClickListener listener) {
        SwitchButton.setGlobalOnLongClickListener(listener);
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Sets the number of buttons showing in a line, 
     * and gets the width of a single switch button
     */
    private void updateButtonLayoutWidth() {
        Display display = ((WindowManager)
                getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        final int w = display.getWidth();
        final int h = display.getHeight();
        final int r = display.getRotation();

        // final android.util.DisplayMetrics
                // metrics = getContext().getResources().getDisplayMetrics();
        // final int w = metrics.widthPixels;
        // final int h = metrics.heightPixels;
        mButtonsPerLine = mMode == SWITCH_WIDGET_STYLE_SINGLE_PAGE ? BUTTONS_PER_LINE_TINY : BUTTONS_PER_LINE;

        if (r == Surface.ROTATION_90 || r == Surface.ROTATION_270) {
            if (w > h) {
                mButtonsPerLine = mMode == SWITCH_WIDGET_STYLE_SINGLE_PAGE
                    ? BUTTONS_PER_LINE_TINY_H : BUTTONS_PER_LINE_H;
            }
        }

        BUTTON_LAYOUT_PARAMS.width = w / mButtonsPerLine;
        if (!SwitchButton.TINY_MODE) {
            BUTTON_LAYOUT_PARAMS.height = BUTTON_LAYOUT_PARAMS.width;
            Log.e(TAG,"dual mode,height=="+BUTTON_LAYOUT_PARAMS.height);
        }
	 else {
	 	if (w > h) {
			BUTTON_LAYOUT_PARAMS.height = BUTTON_LAYOUT_PARAMS.width * 2 - dip2px(getContext(),10.0f);
	 	}
		else {
			BUTTON_LAYOUT_PARAMS.height = BUTTON_LAYOUT_PARAMS.width - dip2px(getContext(),20.0f);
		}
	 }

        mDeltaX = w / 12 - 1;
        mDeltaXxx = w / 3 - 1;
        
        Log.d(TAG,"single page,Button heigth=="+Integer.toString(BUTTON_LAYOUT_PARAMS.height));//120px
        
        if (DEBUG) {
            Log.d(TAG, "Screen size: " + Integer.toString(w) + "*" + Integer.toString(h));
            Log.d(TAG, "Buttons per Line: " + Integer.toString(mButtonsPerLine)
                    + " Button width: " + Integer.toString(BUTTON_LAYOUT_PARAMS.width));
            Log.d(TAG, "Page scroll pixels: " + Integer.toString(mDeltaX)
                    + " Home/End scroll pixels: " + Integer.toString(mDeltaXxx));
        }
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Determines whether or not to show this widget
     */
    private void updateVisibility() {
        // now check if we need to display the widget still
        /*
         * boolean displayPowerWidget = Settings.System.getInt(getContext().getContentResolver(),
         *            Settings.System.EXPANDED_VIEW_WIDGET, 1) == 1;
         * if(!displayPowerWidget) {
         *     setVisibility(View.GONE);
         * } else {
         *     setVisibility(View.VISIBLE);
         * }
         */
         setVisibility(View.VISIBLE);
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: BroadcastReceiver to layout switch buttons
     */
    private class WidgetBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                if (DEBUG) {
                    Log.d(TAG, "Action received: ACTION_BOOT_COMPLETED");
                }
                setupWidget();
		  updateWidget();
                updateVisibility();
            } else if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                // re-calc button width in case of orientation changed
                if (DEBUG) {
                    Log.d(TAG, "Action received: ACTION_CONFIGURATION_CHANGED");
                }
                updateButtonLayoutWidth();
                setupWidget();
                updateWidget();
            } else {
                SwitchButton.handleOnReceive(context, intent);
            }
        }
    };

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Class to watch for settings changes
     */
    private class WidgetSettingsObserver extends ContentObserver {
        public WidgetSettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = getContext().getContentResolver();

            // watch for haptic feedback
            // resolver.registerContentObserver(
                    // Settings.System.getUriFor(Settings.System.EXPANDED_HAPTIC_FEEDBACK),
                    // false, this);

            // watch for changes in color
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.EXPANDED_VIEW_WIDGET_COLOR),
                    false, this);

            // watch for switch buttons
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SWITCH_WIDGET_BUTTONS), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SWITCH_WIDGET_BUTTONS_TINY), false, this);

            // watch for switch-button specific stuff that has been loaded
            for(Uri uri : SwitchButton.getAllObservedUris()) {
                resolver.registerContentObserver(uri, false, this);
            }
        }

        public void unobserve() {
            ContentResolver resolver = getContext().getContentResolver();

            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChangeUri(Uri uri, boolean selfChange) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.SWITCH_WIDGET_BUTTONS))) {
                // Always shows all buttons in dual pages mode
                if (mMode == SWITCH_WIDGET_STYLE_DUAL_PAGES) {
                    setupWidget();
                    updateWidget();
//                    Log.d(TAG,"onchangeUri,dual pages,update Widget");
                }
//                Log.d(TAG,"onchangeUri,dual pages");
            } else if (uri.equals(
                    Settings.System.getUriFor(Settings.System.SWITCH_WIDGET_BUTTONS_TINY))) {
                if (mMode == SWITCH_WIDGET_STYLE_SINGLE_PAGE) {
                    setupWidget();
                    updateWidget();
//                    Log.d(TAG,"onchangeUri,single pages,update Widget");
                }
//                Log.d(TAG,"onchangeUri,single pages");
            } else {
                SwitchButton.handleOnChangeUri(uri);
//                Log.d(TAG,"handleOnChangeUri");
            }
        }
    }
}
