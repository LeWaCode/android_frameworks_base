/*
 * Copyright (C) 2011 Ndoo Internet Technology
 *
 * @version: 1.0
 * @since: 2011/07/21
 * @update: 2011/07/21
 *
 * @description: 
 *      SwitchButton: base class of all of switch buttons
 *      StatelessButton: base class of switch buttons without a state
 *
 * @author: Woody Guo <guozhenjiang@ndoo.net>
 */

package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Woody Guo <guozhenjiang@ndoo.net>
 * @description: A subclass must implement the methods updateState and toggleState
 */
public abstract class SwitchButton {
    public static final String TAG = "SwitchWidget.SwitchButton";

    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_TURNING_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_INTERMEDIATE = 5;
    public static final int STATE_UNKNOWN = 6;

    // TODO: Alarm, Privacy
    public static final String BUTTON_SCREEN_OFF = "turnScreenOff";
    public static final String BUTTON_LOCK_NOW = "lockDevice";
    public static final String BUTTON_SHUTDOWN = "shutdownDevice";
    public static final String BUTTON_REBOOT = "rebootDevice";

    public static final String BUTTON_AIRPLANE = "toggleAireplane";
    public static final String BUTTON_AUTOROTATE = "toggleAutoRotate";
    public static final String BUTTON_BLUETOOTH = "toggleBluetooth";
    public static final String BUTTON_GPS = "toggleGps";
    public static final String BUTTON_TORCH = "toggleTorch";
    public static final String BUTTON_DATA = "toggleData";
    public static final String BUTTON_SYNC = "toggleSync";
    public static final String BUTTON_WIFI = "toggleWifi";
    public static final String BUTTON_WIFI_AP = "toggleWifiAp";
    public static final String BUTTON_SOUND = "toggleSound";
    public static final String BUTTON_BRIGHTNESS = "toggleBrightness";
    public static final String BUTTON_NETWORKMODE = "toggleNetworkMode";
    public static final String BUTTON_SWITCH_WIDGET_STYLE = "toggleSwitchWidgetStyle";
    public static final String BUTTON_SCREEN_CAPTURE = "toggleScreenCapture";
    public static final String BUTTON_NIGHT_MODE = "nightmode";
    public static final String BUTTON_POWER_MANAGER = "powermanager";

    public static final String BUTTON_UNKNOWN = "unknown";

    //ADDED BY luokairong s
    public static final String POWERSAVING_ACTION_NOTIFY_ON="powersaving_action_notify_on";
    public static final String POWERSAVING_DEV_TYPE="dev_type";
    
    public static final int DEV_AIRPLANE = 1;
    public static final int DEV_BLUETOOTH =2;
    public static final int DEV_GPS = 3;
    public static final int DEV_DATA = 4;
    public static final int DEV_SYNC = 5;
    public static final int DEV_WIFI = 6;

    //ADDED BY luokairong e

    public static final String BUTTON_DELIMITER = "|";
    public static String BUTTONS_ALL =
            BUTTON_LOCK_NOW + BUTTON_DELIMITER
            + BUTTON_REBOOT + BUTTON_DELIMITER
            //+BUTTON_POWER_MANAGER+BUTTON_DELIMITER
            +BUTTON_NIGHT_MODE+BUTTON_DELIMITER
            + BUTTON_TORCH + BUTTON_DELIMITER
            + BUTTON_AIRPLANE + BUTTON_DELIMITER
            + BUTTON_AUTOROTATE + BUTTON_DELIMITER
            + BUTTON_BLUETOOTH + BUTTON_DELIMITER
            + BUTTON_GPS + BUTTON_DELIMITER
            + BUTTON_DATA + BUTTON_DELIMITER
            + BUTTON_WIFI + BUTTON_DELIMITER
            + BUTTON_SOUND + BUTTON_DELIMITER
            + BUTTON_BRIGHTNESS + BUTTON_DELIMITER
            + BUTTON_SHUTDOWN + BUTTON_DELIMITER
            + BUTTON_NETWORKMODE + BUTTON_DELIMITER

            + BUTTON_SWITCH_WIDGET_STYLE;
            ;
    
    public String QUERY_POWER_STATUS_ACTION="com.lewa.spm_notification_toast_action";
    public String QUERY_POWER_STATUS_ACTION_KEY="spm_notification_toast_message";
    public static String QUERY_POWER_STATUS_RULE_ACTION="com.lewa.spm_notification_long_mode_rule";
    public static String QUERY_POWER_STATUS_RULE_ACTION_KEY="longRule";
    public static String QUERY_POWER_STATUS_SLEEP_MODE_RULE_ACTION="com.lewa.spm_notification_sleep_mode_rule";
    public static String QUERY_POWER_STATUS_SLEEP_MODE_RULE_ACTION_KEY="sleepRule";
    public int DELAY_RUN_TIME=5000;
    public static String NOTICE_POWER_MANAGER_MSG="status_bar_notice_power_msg";
    
    public String NOTICE_POWER_MSG_SLEEP_KEY="sleep_diff_key";
    public int NOTICE_POWER_MSG_SLEEP_KEY_VALUE=100;

    // A list of all of our buttons and their corresponding classes
    private static final HashMap<String, Class<? extends SwitchButton>>
            BUTTONS = new HashMap<String, Class<? extends SwitchButton>>();
    static {    	
        BUTTONS.put(BUTTON_LOCK_NOW, LockButton.class);
        BUTTONS.put(BUTTON_NIGHT_MODE,NightmodeButton.class);
        BUTTONS.put(BUTTON_SHUTDOWN, ShutdownButton.class);
        BUTTONS.put(BUTTON_REBOOT, RebootButton.class);
        BUTTONS.put(BUTTON_AIRPLANE, AirplaneButton.class);
        BUTTONS.put(BUTTON_AUTOROTATE, AutoRotateButton.class);
        BUTTONS.put(BUTTON_BLUETOOTH, BluetoothButton.class);
        BUTTONS.put(BUTTON_GPS, GpsButton.class);
        BUTTONS.put(BUTTON_TORCH, TorchButton.class);
        BUTTONS.put(BUTTON_DATA, DataButton.class);
        BUTTONS.put(BUTTON_WIFI, WifiButton.class);
        BUTTONS.put(BUTTON_SOUND, SoundButton.class);
        BUTTONS.put(BUTTON_BRIGHTNESS, BrightnessButton.class);
        BUTTONS.put(BUTTON_NETWORKMODE, NetworkModeButton.class);
        //BUTTONS.put(BUTTON_POWER_MANAGER,PowerManagerButton.class);
        BUTTONS.put(BUTTON_SWITCH_WIDGET_STYLE, SwitchStyleButton.class);
    }

    public static boolean TINY_MODE = false;

    // A list of currently loaded buttons
    private static final HashMap<String, SwitchButton>
            BUTTONS_LOADED = new HashMap<String, SwitchButton>();

    private static int sIconTextureWidth = -1;

    public static Context sContext = null;

    protected int mIcon;
    protected int mLabel;
    protected int mState;
//    protected ImageView mView;
    protected TextView mView;

    protected String mType = BUTTON_UNKNOWN;

    // A static onClickListener that can be set to register a callback
    // when ANY button is clicked
    private static View.OnClickListener GLOBAL_ON_CLICK_LISTENER = null;

    // A static onLongClicklistener that can be set to register a callback
    // when ANY button is long clicked
    private static View.OnLongClickListener GLOBAL_ON_LONG_CLICK_LISTENER = null;

    // Thread to update UI views
    private Handler mViewUpdateHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(mView != null) {
                updateImageView();
            }
        }
    };

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Starts an activity with a given intent action
     * @param action is what the activity intent to do
     */
    public void startActivity(final String action) {
        Intent intent = new Intent(action);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sContext.startActivity(intent);
    }
    
    /**
     * @author ypzhu richfuns@gmail.com
     * @param context
     * @param packageName
     */
    public void directTo(Context context,String packageName){
	    PackageManager packageManager = context.getPackageManager();
	    Intent intent=new Intent(); 
	    try { 
	      intent =packageManager.getLaunchIntentForPackage(packageName); 
	    } catch (Exception e) { 
	    } 
	    context.startActivity(intent); 
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Starts a specific component
     * @param pkgName is the name of the package containing the specific component
     * @param className is the class name of the specific component
     */
    public void startActivity(final String pkgName, final String className) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(pkgName, className);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sContext.startActivity(intent);
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Gets the state of a button
     */
    protected abstract void updateState();

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Button is clicked
     */
    protected abstract void toggleState();

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Does nothing by default when a button is long clicked
     */
    protected boolean onLongClick() {
        return false;
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Refresh button state
     */
    protected void update() {
        updateState();
        updateView();
    }

    protected void onReceive(Context context, Intent intent) {
        // do nothing as a standard, override this if the button needs to respond
        // to broadcast events from the StatusBarService broadcast receiver
    }

    protected void onChangeUri(Uri uri) {
        // do nothing as a standard, override this if the button needs to respond
        // to a changed setting
    }

    protected IntentFilter getBroadcastIntentFilter() {
        return null;
    }

    protected List<Uri> getObservedUris() {
        return null;
    }
    
    protected boolean getPowerState() {
        return Settings.System.getInt(sContext.getContentResolver(), Settings.System.POWERMANAGER_MODE_ON,0) == 1;
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Configure a switch button
     * @param view is the imageview to be configured
     */
/*    protected void setupButton(View view) {
        if (view != null) {
            mView = (ImageView) view;
            mView.setTag(mType);
            mView.setOnClickListener(mClickListener);
            mView.setOnLongClickListener(mLongClickListener);
            mView.setPressed(false);
            mView.clearFocus();
            mView.setScaleType(ImageView.ScaleType.CENTER);
            // mView.setScaleType(TINY_MODE
                    // ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER);

            if (null == sContext) {
                sContext = mView.getContext();

 *                 final Resources resources = sContext.getResources();
 *                 final float density = resources.getDisplayMetrics().density;
 *                 final float blurPx = 5 * density;
 * 
 *                 final int iconWidth = (int)
 *                         resources.getDimension(android.R.dimen.app_icon_size);
 *                 sIconTextureWidth = iconWidth + (int)(blurPx*2);
 
            }
        } else if (null != mView) {
            mView.setTag(null);
            mView.setOnClickListener(null);
            mView.setOnLongClickListener(null);
        }
    }*/
    
    /**
     * @author: Woody Guo <woody.guo@gmail.com> 
     * @description: Configure a switch button 
     * @param view is the imageview to be configured 
     */ 
    protected void setupButton(View view) { 
        if (view != null) { 
            mView = (TextView) view; 
            mView.setTag(mType); 
            mView.setTag(R.id.tag_key_trigger_global_listener, false); 
            mView.setOnClickListener(mClickListener); 
            Log.d(TAG,"register button click event");
            mView.setOnLongClickListener(mLongClickListener); 
            mView.setPressed(false); 
            mView.clearFocus(); 
            // mView.setScaleType(ImageView.ScaleType.CENTER); 
            // mView.setScaleType(TINY_MODE 
            // ? ImageView.ScaleType.FIT_CENTER :ImageView.ScaleType.CENTER); 
            if (null == sContext) { 
                sContext = mView.getContext(); 
            } 
            if (-1 == sIconTextureWidth) { 
                final Resources resources = sContext.getResources(); 
//                final int iconWidth = (int)resources.getDimension(android.R.dimen.app_icon_size);//48
                final float density = resources.getDisplayMetrics().density; 
                int iconWidth =0;
                if(density==1.5){
                	iconWidth = (int)resources.getDimension(R.dimen.app_hdpi_icon_size);
                }else if(density==1){
                	iconWidth = (int)resources.getDimension(R.dimen.app_mdpi_icon_size);
                }else{
                	iconWidth = (int)resources.getDimension(android.R.dimen.app_icon_size);//48
                }
                final float blurPx = 0* density; 
                sIconTextureWidth = iconWidth + (int)(blurPx*2); 
                
//                Log.d(TAG,"density="+density+",blurPx="+blurPx);
//                06-01 20:39:46.300: D/SwitchWidget.SwitchButton(356): density=1.5,blurPx=7.5
                //72+15
//                Log.d(TAG, "view=="+view.toString()+",iconWidth="+iconWidth+",sIconTextureWidth="+sIconTextureWidth);
            }
            if (SwitchWidget.DEBUG) { 
                Log.d(TAG, "Icon texture width: " +Integer.toString(sIconTextureWidth) 
                        // + "; text view witdh: " +Integer.toString(mView.getWidth()) 
                        // + "; icon witdh: " + Integer.toString(iconWidth) 
                        ); 
            } 
//            Log.e(TAG,"setupButton,mview="+mView.toString());
        } else if (null != mView) { 
//        	Log.e(TAG,"view===="+view+",");
            mView.setTag(null); 
            mView.setTag(R.id.tag_key_trigger_global_listener, null); 
            mView.setOnClickListener(null); 
            mView.setOnLongClickListener(null); 
            mView.setText(null); 
            for (Drawable d : mView.getCompoundDrawables()) { 
                if (null != d) { 
                    d.setCallback(null); 
                } 
            } 
            mView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0); 
//            Log.e(TAG,"else setupButton,mview="+mView.toString());
        } 
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Sends an empty message to the UI update thread
     */
    protected void updateView() {
        mViewUpdateHandler.sendEmptyMessage(0);
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Set icon for the imageview
     */
/*    private void updateImageView() {
        // if (SwitchWidget.DEBUG) {
            // Log.d(TAG, "Update icon for button: " + mType);
        // }
        // Drawable icon = sContext.getResources().getDrawable(mIcon);
        // icon.setBounds(0, 0, sIconTextureWidth, sIconTextureWidth);
        // ((TextView) mView).setCompoundDrawables(null, icon, null, null);
        mView.setImageResource(mIcon);
    }*/
    
    /**
    
     * @author: Woody Guo <woody.guo@gmail.com>
 
     * @description: Set icon for the imageview
 
     */
 
    private void updateImageView() { 
        // if (SwitchWidget.DEBUG) {
 
            // Log.d(TAG, "Update icon for button: " + mType); 
        // } 
        if (!TINY_MODE) { 
            // Do not display label in tiny mode 
        	if(mLabel!=0){
                mView.setText(mLabel); 
        	}else{
//        		Log.e(TAG,"mLabel=="+mLabel);
        		mView.setText("");
        	}
        }else{
        	//Log.d(TAG,"tiny mode");
        	if(mLabel!=0){
                mView.setText(mLabel); 
        	}else{
        		mView.setText("");
        	}
        } 
        for (Drawable d : mView.getCompoundDrawables()) { 
            if (null != d) { 
                d.setCallback(null); 
            } 
        } 
        Drawable icon = sContext.getResources().getDrawable(mIcon); 
        icon.setBounds(0, 0, sIconTextureWidth, sIconTextureWidth); 
//        Log.d(TAG,"text=="+mView.getText()+",width="+sIconTextureWidth);
        mView.setCompoundDrawables(null, icon, null, null); 
        // mView.setCompoundDrawablesWithIntrinsicBounds(0, mIcon, 0, 0); 
        // mView.setImageResource(mIcon); 
//        Log.e(TAG,"updateImageView");
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
	    
            String type = (String)v.getTag();
            
            Log.d(TAG,"btn type="+type);

            for (Map.Entry<String, SwitchButton> entry : BUTTONS_LOADED.entrySet()) {
                if(entry.getKey().equals(type)) {
                    Log.d(TAG,"btn type=="+type+"entry key=="+entry.getKey()+",entry value=="+entry.getValue());
                    entry.getValue().toggleState();
                    break;
                }
            }

            // call our static listener if it's set
            if(GLOBAL_ON_CLICK_LISTENER != null) {
                GLOBAL_ON_CLICK_LISTENER.onClick(v);
            }
        }
    };

    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            boolean result = false;
            String type = (String)v.getTag();
            for (Map.Entry<String, SwitchButton> entry : BUTTONS_LOADED.entrySet()) {
                if (entry.getKey().endsWith(type)) {
                    result = entry.getValue().onLongClick();
                    break;
                }
            }

            if (!result && GLOBAL_ON_LONG_CLICK_LISTENER != null) {
                GLOBAL_ON_LONG_CLICK_LISTENER.onLongClick(v);
            }
            return true;
        }
    };

    public static boolean loadButton(String key, View view) {
        // first make sure we have a valid button
        if(BUTTONS.containsKey(key) && view != null) {
            synchronized (BUTTONS_LOADED) {
                if(BUTTONS_LOADED.containsKey(key)) {
                    // setup the button again
                    BUTTONS_LOADED.get(key).setupButton(view);
                } else {
                    try {
                        // we need to instantiate a new button and add it
                        SwitchButton sb = BUTTONS.get(key).newInstance();
                        // set it up
                        sb.setupButton(view);
                        // save it
                        BUTTONS_LOADED.put(key, sb);
                    } catch(Exception e) {
                        Log.e(TAG, "Error loading button: " + key, e);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static void unloadButton(String key) {
        synchronized (BUTTONS_LOADED) {
            if(BUTTONS_LOADED.containsKey(key)) {
                BUTTONS_LOADED.get(key).setupButton(null);
                BUTTONS_LOADED.remove(key);
            }
        }
    }

    public static void unloadAllButtons() {    	
        synchronized (BUTTONS_LOADED) {
            for (SwitchButton sb : BUTTONS_LOADED.values()) {
//            	Log.d(TAG,"uploadAllButtons:mLabel="+sb.mLabel);
                sb.setupButton(null);
            }

            BUTTONS_LOADED.clear();
        }
    }

    public static void updateAllButtons() {
        synchronized (BUTTONS_LOADED) {
            for(SwitchButton sb : BUTTONS_LOADED.values()) {
                sb.update();
            }
        }
    }

    // glue for broadcast receivers
    public static IntentFilter getAllBroadcastIntentFilters() {
        IntentFilter filter = new IntentFilter();

        synchronized(BUTTONS_LOADED) {
            for(SwitchButton button : BUTTONS_LOADED.values()) {
                IntentFilter tmp = button.getBroadcastIntentFilter();
                if (null == tmp) {
                    continue;
                }

                int num = tmp.countActions();
                for(int i = 0; i < num; i++) {
                    String action = tmp.getAction(i);
                    if(!filter.hasAction(action)) {
                        filter.addAction(action);
                    }
                }
            }
        }

        return filter;
    }

    // glue for content observation
    public static List<Uri> getAllObservedUris() {
        List<Uri> uris = new ArrayList<Uri>();

        synchronized(BUTTONS_LOADED) {
            for(SwitchButton button : BUTTONS_LOADED.values()) {
                List<Uri> tmp = button.getObservedUris();
                if (null == tmp) {
                    continue;
                }

                for(Uri uri : tmp) {
                    if(!uris.contains(uri)) {
                        uris.add(uri);
                    }
                }
            }
        }

        return uris;
    }

    public static void handleOnReceive(Context context, Intent intent) {
        String action = intent.getAction();

        synchronized(BUTTONS_LOADED) {
            for(SwitchButton button : BUTTONS_LOADED.values()) {
                IntentFilter iff = button.getBroadcastIntentFilter();
                if (null == iff) {
                    continue;
                }
                if(iff.hasAction(action)) {
                    button.onReceive(context, intent);
                }
            }
        }
    }

    public static void handleOnChangeUri(Uri uri) {
        synchronized(BUTTONS_LOADED) {
            for(SwitchButton button : BUTTONS_LOADED.values()) {
                List<Uri> uris = button.getObservedUris();
                if (null == uris) {
                    continue;
                }
                if(uris.contains(uri)) {
                    button.onChangeUri(uri);
                }
            }
        }
    }

    public static void setGlobalOnClickListener(View.OnClickListener listener) {
        GLOBAL_ON_CLICK_LISTENER = listener;
    }

    public static void setGlobalOnLongClickListener(View.OnLongClickListener listener) {
        GLOBAL_ON_LONG_CLICK_LISTENER = listener;
    }

    protected static SwitchButton getLoadedButton(String key) {
        synchronized(BUTTONS_LOADED) {
            if(BUTTONS_LOADED.containsKey(key)) {
                return BUTTONS_LOADED.get(key);
            } else {
                return null;
            }
        }
    }
}

/**
 * @author: Woody Guo <guozhenjiang@ndoo.net>
 * @description: A subclass must provide a constructor to set mIcon and mType,
 * and implement the method onClick
 */
abstract class StatelessButton extends SwitchButton {
    protected abstract void onClick();

    StatelessButton() {
        super();
        mState = STATE_ENABLED;
    }

    @Override
    protected void updateState() { }

    @Override
    protected void toggleState() {
        onClick();
    }
}

/**
 * @author: Woody Guo <guozhenjiang@ndoo.net>
 * @description: Base class of statefull buttons which observe system settings
 */
abstract class ObserveButton extends SwitchButton {
    protected final List<Uri> mObservedUris;

    ObserveButton() {
        super();
        mObservedUris = new ArrayList<Uri>();
    }

    @Override
    protected void onChangeUri(Uri uri) {
        update();
    }

    @Override
    protected List<Uri> getObservedUris() {
        return mObservedUris;
    }
}

/**
 * @author: Woody Guo <guozhenjiang@ndoo.net>
 * @description: Base class of statefull buttons which receive broadcasts
 */
abstract class ReceiverButton extends SwitchButton {
    protected StateTracker mStateTracker;
    protected IntentFilter mFilter;

    ReceiverButton() {
        super();
        mFilter = new IntentFilter();
        mStateTracker = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != mStateTracker) {
            mStateTracker.onActualStateChange(context, intent);
        }
        update();
    }

    @Override
    protected IntentFilter getBroadcastIntentFilter() {
        return mFilter;
    }
}
/**
 * The state machine for Wifi and Bluetooth toggling, tracking reality versus
 * the user's intent. This is necessary because reality moves relatively slowly
 * (turning on &amp; off radio drivers), compared to user's expectations.
 */
abstract class StateTracker {
    // Is the state in the process of changing?
    private boolean mInTransition = false;

    private Boolean mActualState = null; // initially not set

    private Boolean mIntendedState = null; // initially not set

    // Did a toggle request arrive while a state update was
    // already in-flight? If so, the mIntendedState needs to be
    // requested when the other one is done, unless we happened to
    // arrive at that state already.
    private boolean mDeferredStateChangeRequestNeeded = false;

    /**
     * User pressed a button to change the state. Something should immediately
     * appear to the user afterwards, even if we effectively do nothing. Their
     * press must be heard.
     */
    public final void toggleState(Context context) {
        int currentState = getTriState(context);
        boolean newState = false;
        switch (currentState) {
            case SwitchButton.STATE_ENABLED:
                newState = false;
                break;
            case SwitchButton.STATE_DISABLED:
                newState = true;
                break;
            case SwitchButton.STATE_INTERMEDIATE:
                if (mIntendedState != null) {
                    newState = !mIntendedState;
                }
                break;
        }
        mIntendedState = newState;
        if (mInTransition) {
            // We don't send off a transition request if we're
            // already transitioning. Makes our state tracking
            // easier, and is probably nicer on lower levels.
            // (even though they should be able to take it...)
            mDeferredStateChangeRequestNeeded = true;
        } else {
            mInTransition = true;
            requestStateChange(context, newState);
        }
    }

    /**
     * Update internal state from a broadcast state change.
     */
    public abstract void onActualStateChange(Context context, Intent intent);

    /**
     * Sets the value that we're now in. To be called from onActualStateChange.
     * 
     * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
     *            STATE_TURNING_OFF, STATE_UNKNOWN
     */
    protected final void setCurrentState(Context context, int newState) {
        final boolean wasInTransition = mInTransition;
        switch (newState) {
            case SwitchButton.STATE_DISABLED:
                mInTransition = false;
                mActualState = false;
                break;
            case SwitchButton.STATE_ENABLED:
                mInTransition = false;
                mActualState = true;
                break;
            case SwitchButton.STATE_TURNING_ON:
                mInTransition = true;
                mActualState = false;
                break;
            case SwitchButton.STATE_TURNING_OFF:
                mInTransition = true;
                mActualState = true;
                break;
        }

        if (wasInTransition && !mInTransition) {
            if (mDeferredStateChangeRequestNeeded) {
                Log.v("StateTracker", "processing deferred state change");
                if (mActualState != null && mIntendedState != null
                        && mIntendedState.equals(mActualState)) {
                    Log.v("StateTracker", "... but intended state matches, so no changes.");
                } else if (mIntendedState != null) {
                    mInTransition = true;
                    requestStateChange(context, mIntendedState);
                }
                mDeferredStateChangeRequestNeeded = false;
            }
        }
    }

    /**
     * If we're in a transition mode, this returns true if we're transitioning
     * towards being enabled.
     */
    public final boolean isTurningOn() {
        return mIntendedState != null && mIntendedState;
    }

    /**
     * Returns simplified 3-state value from underlying 5-state.
     * 
     * @param context
     * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
     */
    public final int getTriState(Context context) {
        /*
         * if (mInTransition) { // If we know we just got a toggle request
         * recently // (which set mInTransition), don't even ask the //
         * underlying interface for its state. We know we're // changing. This
         * avoids blocking the UI thread // during UI refresh post-toggle if the
         * underlying // service state accessor has coarse locking on its //
         * state (to be fixed separately). return
         * SwitchButton.STATE_INTERMEDIATE; }
         */
        switch (getActualState(context)) {
            case SwitchButton.STATE_DISABLED:
                return SwitchButton.STATE_DISABLED;
            case SwitchButton.STATE_ENABLED:
                return SwitchButton.STATE_ENABLED;
            default:
                return SwitchButton.STATE_INTERMEDIATE;
        }
    }

    /**
     * Gets underlying actual state.
     * 
     * @param context
     * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
     *         or or STATE_UNKNOWN.
     */
    public abstract int getActualState(Context context);

    /**
     * Actually make the desired change to the underlying radio API.
     */
    protected abstract void requestStateChange(Context context, boolean desiredState);
}
