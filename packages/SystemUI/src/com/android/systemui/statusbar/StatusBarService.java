/*
 * Copyright (C) 2010 The Android Open Source Project
 * Patched by Sven Dawitz; Copyright (C) 2011 CyanogenMod Project
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

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import android.text.format.Formatter;
import android.app.ActivityManagerNative;
import android.os.SystemProperties;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.CmSystem;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
//import android.widget.CheckBox;
import android.widget.LewaCheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.*;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.internal.statusbar.StatusBarNotification;
import com.android.systemui.R;
//changed by zhuyaopeng 12.05.25
import com.android.systemui.statusbar.switchwidget.SwitchButton;
import com.android.systemui.statusbar.switchwidget.SwitchWidget;

//add by zhangbo begin
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;

import android.util.Log;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.app.KeyguardManager;
import java.io.File;
import android.os.Build;
import org.apache.http.util.*;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.NetworkInfo;

//end 
import android.graphics.drawable.AnimationDrawable;

public class StatusBarService extends Service implements CommandQueue.Callbacks {

    static final String TAG = "StatusBarService";

    private static final String DATA_TYPE_TMOBILE_STYLE = "vnd.tmobile.cursor.item/style";
    private static final String DATA_TYPE_TMOBILE_THEME = "vnd.tmobile.cursor.item/theme";
    private static final String ACTION_TMOBILE_THEME_CHANGED = "com.tmobile.intent.action.THEME_CHANGED";
    static final boolean SPEW_ICONS = false;
    static final boolean SPEW = false;

    public static final String ACTION_STATUSBAR_START = "com.android.internal.policy.statusbar.START";

    // values changed onCreate if its a bottomBar
    static int EXPANDED_LEAVE_ALONE = -10000;
    static int EXPANDED_FULL_OPEN = -10001;
    static int EXPANDED_LEAVE_ALONE_EXT = -10002;//add by zhangbo 20111203

    private static final int MSG_ANIMATE = 1000;
    private static final int MSG_ANIMATE_REVEAL = 1001;

    byte[] mBuffer = new byte[1024];//add by zhangxianjia read meminfo

    static final long PAGE_SIZE = 4*1024;

    long SECONDARY_SERVER_MEM;

    StatusBarPolicy mIconPolicy;

    //added by zhuyaopeng 2012/06/28
    private int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
    private String inCallScreen="com.android.phone.InCallScreen";
    private FrameLayout inCallScreenLayer;
    private SharePreferenceHelper sp;
    private SharePreferenceHelper sp2;
    public static String POWER_MANAGER_LONG_NAME="spm_power_long_rules";
    public static String POWER_MANAGER_SLEEP_NAME="spm_power_sleep_rules";

    CommandQueue mCommandQueue;
    IStatusBarService mBarService;

    private Bundle metaData = null;
    private String trackingKey = "";
    public Boolean ifUpdate = true;

    /**
     * Shallow container for {@link #mStatusBarView} which is added to the
     * window manager impl as the actual status bar root view. This is done so
     * that the original status_bar layout can be reinflated into this container
     * on skin change.
     */
    FrameLayout mStatusBarContainer;

    int mIconSize;
    Display mDisplay;
    CmStatusBarView mStatusBarView;
    int mPixelFormat;
    H mHandler = new H();
    Object mQueueLock = new Object();

    // icons
    LinearLayout mIcons;
    IconMerger mNotificationIcons;
    LinearLayout mStatusIcons;

    // expanded notifications
    Dialog mExpandedDialog;
    ExpandedView mExpandedView;
    ExpandedView monitorExpandedView;
    WindowManager.LayoutParams mExpandedParams;
    ScrollView mScrollView;
    //changed by zhuyaopeng 12.05.25
    //ScrollView mBottomScrollView;
    LinearLayout mNotificationLinearLayout;
    //    LinearLayout mBottomNotificationLinearLayout;
    View mExpandedContents;

    // Begin, added by zhumeiquan for new req SW1 #5492, 20120524
    View mExpandedNotifications;
    View mUsbModeNotification;
    LewaCheckBox mUsbModeButton;
    private OnCheckedChangeListener mUsbModeButtonCheckListener;
    private OnClickListener mUsbNotifLister;
    // End

    // top bar
    TextView mNoNotificationsTitle;
    TextView mClearButton;
    //    TextView mCompactClearButton;
    //    ViewGroup mClearButtonParent;
    CmBatteryMiniIcon mCmBatteryMiniIcon;
    // drag bar
    CloseDragHandle mCloseView;
    // ongoing
    NotificationData mOngoing = new NotificationData();
    TextView mOngoingTitle;
    LinearLayout mOngoingItems;
    // latest
    NotificationData mLatest = new NotificationData();
    TextView mLatestTitle;
    LinearLayout mLatestItems;
    ItemTouchDispatcher mTouchDispatcher;
    // position
    int[] mPositionTmp = new int[2];
    boolean mExpanded;
    boolean mExpandedVisible;

    // the date view
    DateView mDateView;
    CarrierLabel mCarrier;

    // the tracker view
    TrackingView mTrackingView;
    WindowManager.LayoutParams mTrackingParams;
    int mTrackingPosition; // the position of the top of the tracking view.
    private boolean mPanelSlightlyVisible;

    //changed by zhuyaopeng 12.05.25
    // the power widget
    //    PowerWidget mPowerWidget;
    private SwitchWidget mSwitchWidget;

    private FrameLayout mNotificationsLayout;
    public TextView mTabSwitches;
    public TextView mTabNotifications;

    public final static int TAB_NOTIFICATIONS = 1;
    public final static int TAB_SWITCHES = 2;

    public int mTab = TAB_SWITCHES;

    private int mLastX;

    //Carrier label stuff
    /*    LinearLayout mCarrierLabelLayout;
    LinearLayout mCompactCarrierLayout;
    LinearLayout mPowerAndCarrier;*/

    // ticker
    private Ticker mTicker;
    private View mTickerView;
    private boolean mTicking;

    // Tracking finger for opening/closing.
    int mEdgeBorder; // corresponds to R.dimen.status_bar_edge_ignore
    boolean mTracking;
    VelocityTracker mVelocityTracker;

    static final int ANIM_FRAME_DURATION = (1000 / 45);//modified by zhangbo

    // for brightness control on status bar
    int mLinger = 0;

    boolean mAnimating;
    long mCurAnimationTime;
    float mDisplayHeight;
    float mAnimY;
    float mAnimVel;
    float mAnimAccel;
    long mAnimLastTime;
    boolean mAnimatingReveal = false;
    int mViewDelta;
    int[] mAbsPos = new int[2];

    //add by zhangxianjia
    RunningState mState;

    // for disabling the status bar
    int mDisabled = 0;

    // weather or not to show status bar on bottom
    boolean mBottomBar;
    boolean mButtonsLeft;
    boolean mDeadZone;
    boolean mHasSoftButtons;
    boolean mLewaBottomVirtualKey;
    boolean mShakeClear;
    Context mContext;

    //changed by zhuyaopeng 12.05.25
    // SwitchWidget style: single page or dual pages
    int mStyle;

    //add by zhaolei,for tips
    private boolean mTipsShown;
    private PopuViewForNotifications mPopuViewForNotifications = null;

    //added by zhuyaopeng    
    private CollapseExpandViewReceiver collapseViewReceiver;
    public static String ACTION_COLLAPSE_ViEW="collapseViewReceiver_action";

    //added by krshen
    private ShakeListener mShaker;

    // tracks changes to settings, so status bar is moved to top/bottom
    // as soon as cmparts setting is changed
    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_BOTTOM), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SOFT_BUTTONS_LEFT), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_DEAD_ZONE), false, this);
            //            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.STATUS_BAR_COMPACT_CARRIER), false, this);
            // Watching for settings for switchwidget
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SWITCH_WIDGET_STYLE), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SHAKE_CLEAR_NOTIFICATIONS), false, this);

            // No longer needed as we always show out switchwidget
            // resolver.registerContentObserver(
            // Settings.System.getUriFor(Settings.System.EXPANDED_VIEW_WIDGET), false, this);

            //Begin, Modify by zhangbo for request
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.VIRTUAL_KEY), true, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BOTTOM_VIRTUAL_KEY), true, this);
            // End
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            int defValue;

            defValue=(CmSystem.getDefaultBool(mContext, CmSystem.CM_DEFAULT_BOTTOM_STATUS_BAR) ? 1 : 0);
            mBottomBar = (Settings.System.getInt(resolver,
                    Settings.System.STATUS_BAR_BOTTOM, defValue) == 1);
            defValue=(CmSystem.getDefaultBool(mContext, CmSystem.CM_DEFAULT_SOFT_BUTTONS_LEFT) ? 1 : 0);
            mButtonsLeft = (Settings.System.getInt(resolver,
                    Settings.System.SOFT_BUTTONS_LEFT, defValue) == 1);
            defValue=(CmSystem.getDefaultBool(mContext, CmSystem.CM_DEFAULT_USE_DEAD_ZONE) ? 1 : 0);
            mDeadZone = (Settings.System.getInt(resolver,
                    Settings.System.STATUS_BAR_DEAD_ZONE, defValue) == 1);
            //begin,changed by zhuyaopeng 12.05.25
            mStyle = Settings.System.getInt(getContentResolver()
                    , Settings.System.SWITCH_WIDGET_STYLE, SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES);
            //end,changed by zhuyaopeng 12.05.25

            //Begin, Modify by zhangbo for request
            /*           mCompactCarrier = (Settings.System.getInt(resolver,
                    Settings.System.STATUS_BAR_COMPACT_CARRIER, 0) == 1);*/
            mSoftButtonEnable = (Settings.System.getInt(resolver,Settings.System.VIRTUAL_KEY, 0) == 1);
            mLewaBottomVirtualKey = (Settings.System.getInt(resolver,Settings.System.BOTTOM_VIRTUAL_KEY, 0) == 1);
            // End
            mShakeClear = Settings.System.getInt(getContentResolver()
                    , Settings.System.SHAKE_CLEAR_NOTIFICATIONS, 1) == 1;
            updateLayout();
            //            updateCarrierLabel();
            updateSoftButton();
            setupKeysView();
        }
    }    

    public int getmTab() {
        return mTab;
    }

    public void setmTab(int mTab) {
        this.mTab = mTab;
    }

    public int getmStyle() {
        return mStyle;
    }

    private class ExpandedDialog extends Dialog {
        ExpandedDialog(Context context) {
            super(context, com.android.internal.R.style.Theme_Light_NoTitleBar);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                if (!down) {
                    animateCollapse();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }


    @Override
    public void onCreate() {
        sp=new SharePreferenceHelper(this,POWER_MANAGER_LONG_NAME);
        sp2=new SharePreferenceHelper(this, POWER_MANAGER_SLEEP_NAME);    	
        //begin,changed by zhuyaopeng 12.05.25
        mLastX = 0;
        mStyle = Settings.System.getInt(getContentResolver(), Settings.System.SWITCH_WIDGET_STYLE, SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES);
        //end,changed by zhuyaopeng 12.05.25

        // First set up our views and stuff.
        mDisplay = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        SECONDARY_SERVER_MEM =Integer.valueOf(SystemProperties.get("ro.SECONDARY_SERVER_MEM"))*PAGE_SIZE;
        mState = RunningState.getInstance(this);
        mState.pause();
        formater = java.text.DecimalFormat.getInstance();  
        formater.setMaximumFractionDigits(2);  
        formater.setMinimumFractionDigits(2); 
        makeStatusBarView(this);

        // reset vars for bottom bar
        if (mBottomBar) {
            EXPANDED_LEAVE_ALONE *= -1;
            EXPANDED_FULL_OPEN *= -1;
        }

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // -- add for UA by luoyongxing
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // -- end
        registerReceiver(mBroadcastReceiver, filter);

        //added by zhuyaopeng 2012/07/10
        this.registerCollapseExpandViewReceiver();        

        try {
            IntentFilter tMoFilter = new IntentFilter(ACTION_TMOBILE_THEME_CHANGED);
            tMoFilter.addDataType(DATA_TYPE_TMOBILE_THEME);
            tMoFilter.addDataType(DATA_TYPE_TMOBILE_STYLE);
            registerReceiver(mBroadcastReceiver, tMoFilter);
        } catch (MalformedMimeTypeException e) {
            Slog.e(TAG, "Could not set T-Mo mime types", e);
        }

        // Put up the view
        FrameLayout container = new FrameLayout(StatusBarService.this);
        container.addView(mStatusBarView);
        mStatusBarContainer = container;
        addStatusBarView();    
        //notifyfirewall = new NotificationFirewall(StatusBarService.this);
        NotificationFirewall.getInstance(StatusBarService.this).applySavedRules();
        //notifyfirewall.addBlockPackage("com.lewa.store");
    }

    @Override
    public void onDestroy() {
        unbindService(mConnection);
        // we're never destroyed
    }


    /**
     * Nobody binds to us.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ComponentName getTopComponent(){
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
        return componentName;
    }

    public void listenTeleCalls(final CmStatusBarView view) {
        TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneListener = new PhoneStateListener() {

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                int newBackground=R.drawable.statusbar_background;
                switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    mPhoneState=TelephonyManager.CALL_STATE_IDLE;
                    newBackground=R.drawable.statusbar_background;
                    view.setBackgroundResource(newBackground);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "offhook");
                    mPhoneState=TelephonyManager.CALL_STATE_OFFHOOK;
                    ComponentName cn=getTopComponent();
                    if(!cn.getClassName().equals(inCallScreen)){
                        newBackground=R.drawable.statusbar_background_green;
                        view.setBackgroundResource(newBackground);
                    }					
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mPhoneState=TelephonyManager.CALL_STATE_RINGING;
                    newBackground=R.drawable.statusbar_background;
                    view.setBackgroundResource(newBackground);
                    break;
                default:
                    break;
                }
            }
        };
        telManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    //    private boolean mCompactCarrier = false;

    // ================================================================================
    // Constructing the view
    // ================================================================================
    private void makeStatusBarView(Context context) {
        Resources res = context.getResources();

        //        mTouchDispatcher = new ItemTouchDispatcher(this);

        mIconSize = res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_icon_size);


        //added by zhuyaopeng 2012/05/26
        ExpandedView expanded = (ExpandedView)View.inflate(context, R.layout.status_bar_expanded, null);
        expanded.mService = this;
        //end added

        //Check for compact carrier layout and apply if enabled
        //        mCompactCarrier = Settings.System.getInt(getContentResolver(),Settings.System.STATUS_BAR_COMPACT_CARRIER , 0) == 1;
        mKM = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        mLockscreenStyle = (Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF , 6));
        Settings.System.putInt(getContentResolver(), Settings.System.LOCK_HOME_IN_MEMORY,  1);
        CmStatusBarView sb = (CmStatusBarView)View.inflate(context, R.layout.status_bar, null);
        sb.mService = this;
        //        listenTeleCalls(sb);
        // figure out which pixel-format to use for the status bar.
        mPixelFormat = PixelFormat.TRANSLUCENT;
        Drawable bg = sb.getBackground();
        if (bg != null) {
            mPixelFormat = bg.getOpacity();
        }
        mStatusBarView = sb;
        mStatusIcons = (LinearLayout)sb.findViewById(R.id.statusIcons);
        mNotificationIcons = (IconMerger)sb.findViewById(R.id.notificationIcons);
        mIcons = (LinearLayout)sb.findViewById(R.id.icons);
        mTickerView = sb.findViewById(R.id.ticker);
        mDateView = (DateView)sb.findViewById(R.id.date);
        mCarrier = (CarrierLabel)sb.findViewById(R.id.carrier);
        mCmBatteryMiniIcon = (CmBatteryMiniIcon)sb.findViewById(R.id.CmBatteryMiniIcon);
        mExpandedDialog = new ExpandedDialog(context);

        //changed by zhuyaopeng
        mExpandedView = expanded;
        /*        monitorExpandedView=(ExpandedView)View.inflate(context, R.layout.status_bar_expanded_old, null);
        monitorExpandedView.setVisibility(View.GONE);
        monitorExpandedView.mService=this;*/
        //end changed
        mExpandedContents = expanded.findViewById(R.id.notificationLinearLayout);
        mOngoingTitle = (TextView)expanded.findViewById(R.id.ongoingTitle);
        mOngoingItems = (LinearLayout)expanded.findViewById(R.id.ongoingItems);
        mLatestTitle = (TextView)expanded.findViewById(R.id.latestTitle);
        mLatestItems = (LinearLayout)expanded.findViewById(R.id.latestItems);
        // Begin, added by zhumeiquan for new req SW1 #5492, 20120524   
        mUsbModeNotification = expanded.findViewById(R.id.usb_mode_notification);
        mUsbNotifLister = new USBNotiListener();
        mUsbModeNotification.setOnClickListener(mUsbNotifLister);            
        mUsbModeButton = (LewaCheckBox)expanded.findViewById(R.id.usb_mode_button);
        mUsbModeButtonCheckListener = new USBButtonListener();
        mUsbModeButton.setOnCheckedChangeListener(mUsbModeButtonCheckListener);
        // End        
        mNoNotificationsTitle = (TextView)expanded.findViewById(R.id.noNotificationsTitle);
        mClearButton = (TextView)expanded.findViewById(R.id.clear_all_button);
        mClearButton.setOnClickListener(mClearButtonListener);
        mScrollView = (ScrollView)expanded.findViewById(R.id.scroll);
        // mBottomScrollView = (ScrollView)expanded.findViewById(R.id.bottomScroll);
        mNotificationLinearLayout = (LinearLayout)expanded.findViewById(R.id.notificationLinearLayout);
        // mBottomNotificationLinearLayout = (LinearLayout)
        // expanded.findViewById(R.id.bottomNotificationLinearLayout);
        mNotificationsLayout = (FrameLayout)expanded.findViewById(R.id.notifications);

        mExpandedView.setVisibility(View.GONE);
        //        monitorExpandedView.setVisibility(View.GONE);
        mOngoingTitle.setVisibility(View.GONE);
        mLatestTitle.setVisibility(View.GONE);

        //added by zhuyaopeng 2012/05/26
        /* mSwitchWidget = (SwitchWidget)expanded.findViewById(R.id.switch_widget);
        mSwitchWidget.setupSettingsObserver(mHandler);
        mSwitchWidget.setGlobalButtonOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(Settings.System.getInt(getContentResolver(),
                        Settings.System.EXPANDED_HIDE_ONCHANGE, 0) == 1) {
                    animateCollapse();
                }
            }
        });
        mSwitchWidget.setGlobalButtonOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                animateCollapse();
                return true;
            }
        });
         */

        mTabNotifications = (TextView) expanded.findViewById(R.id.image_tab_notifications);
        mTabSwitches = (TextView) expanded.findViewById(R.id.image_tab_switches);
        // Set background transparency of tab textviews
        // mTabNotifications.setBackgroundColor(Color.argb(155, 0, 255, 0));
        // mTabSwitches.setBackgroundColor(Color.argb(155, 0, 255, 0));
        mTabSwitches.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTab != TAB_SWITCHES) {
                    mTab = TAB_SWITCHES;
                    onTabChange();
                }
            }
        });
        mTabNotifications.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTab != TAB_NOTIFICATIONS) {
                    mTab = TAB_NOTIFICATIONS;
                    onTabChange();
                }
            }
        });

        //end added by zhuyaopeng 2012/05/26

        mTicker = new MyTicker(context, sb);
        TickerView tickerView = (TickerView)sb.findViewById(R.id.tickerText);
        tickerView.mTicker = mTicker;
        mTrackingView = (TrackingView)View.inflate(context, R.layout.status_bar_tracking, null);
        mTrackingView.mService = this;
        mCloseView = (CloseDragHandle)mTrackingView.findViewById(R.id.close);
        mCloseOn = (ImageView)mTrackingView.findViewById(R.id.close_on);
        mCloseView.mService = this;
        mContext = context;
        mTrackingView.setVisibility(View.GONE);
        updateLayout();
        mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);
        // Begin , Added by zhumeiquan for statusbar transparent, 20120315        
        updateStatusBarBackground();
        // End
    }

    /**
     * 弃用
     */
    private void updateCarrierLabel() {
        boolean isLockScreen = mKM.inKeyguardRestrictedInputMode();
        mLockscreenStyle = (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_STYLE_PREF, 6));
        if (isLockScreen && mLockscreenStyle == 6 ) {
            mCarrier.setVisibility(View.VISIBLE);
        } else {
            mCarrier.setVisibility(View.GONE);
        }
        // Begin , Added by zhumeiquan for statusbar transparent, 20120315        
        updateStatusBarBackground();
        // End       
        if (isLockScreen) {
            mStatusBarView.mClock.setVisibility(View.GONE);
        } else {
            mStatusBarView.mClock.setVisibility(View.VISIBLE);
        }
    }

    private void updateLayout() {
        if (mTrackingView == null || mCloseView == null || mExpandedView == null || mSwitchWidget == null) {
            return;
        }

        if (SwitchWidget.DEBUG) {
            Log.d(TAG, "StatusBarService.updateLayout(): style = "
                    + (mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES
                    ? "[Dual pages]" : "[Single page]"));
        }

        // Remove all notification views
        mNotificationLinearLayout.removeAllViews();
        // Readd to correct scrollview depending on mBottomBar
        if (mBottomBar) {
            mScrollView.setVisibility(View.GONE);
        } else {
            // Begin, added by zhumeiquan for new req SW1 #5492, 20120524            
            mNotificationLinearLayout.addView(mUsbModeNotification);
            // End
            mNotificationLinearLayout.addView(mLatestItems);
            mNotificationLinearLayout.addView(mOngoingItems);
            mScrollView.setVisibility(View.VISIBLE);
        }

        //begin,changed by zhuyaopeng 12.05.25        
        LinearLayout llTabs = (LinearLayout) mExpandedView.findViewById(R.id.layout_tabs);
        RelativeLayout rlSwitches = (RelativeLayout) mExpandedView.findViewById(R.id.layout_switches);

        llTabs.removeAllViews();
        ViewGroup g = (ViewGroup) mSwitchWidget.getParent();
        if (g != null) {
            g.removeView(mSwitchWidget);
        }
        // ((ViewGroup) mSwitchWidget.getParent()).removeView(mSwitchWidget);
        if (mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES) {
            llTabs.addView(mTabNotifications);
            llTabs.addView(mTabSwitches);

            rlSwitches.addView(mSwitchWidget);
            mSwitchWidget.setMode(SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES);
            onTabChange();
        } else if (mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_SINGLE_PAGE) {
            llTabs.addView(mSwitchWidget);

            mSwitchWidget.setMode(SwitchWidget.SWITCH_WIDGET_STYLE_SINGLE_PAGE);

            mSwitchWidget.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.VISIBLE);
            // if (mBottomBar) {
            // mScrollView.setVisibility(View.GONE);
            // mBottomScrollView.setVisibility(View.VISIBLE);
            // } else {
            // mBottomScrollView.setVisibility(View.GONE);
            // mScrollView.setVisibility(View.VISIBLE);
            // }
        }
        //end,changed by zhuyaopeng 12.05.25
    }

    protected void addStatusBarView() {
        Resources res = getResources();
        final int height= res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);

        final View view = mStatusBarContainer;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
                WindowManager.LayoutParams.TYPE_STATUS_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING,
                PixelFormat.RGBA_8888);
        lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
        lp.setTitle("StatusBar");
        lp.windowAnimations = com.android.internal.R.style.Animation_StatusBar;

        WindowManagerImpl.getDefault().addView(view, lp);

        //begin,changed by zhuyaopeng 12.05.25
        // mSwitchWidget.setupWidget(mStyle);
        //end,changed by zhuyaopeng 12.05.25

        //mPowerWidget.setupWidget(); //move it to mStartTask
    }

    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        if (SPEW_ICONS) {
            Slog.d(TAG, "addIcon slot=" + slot + " index=" + index + " viewIndex=" + viewIndex
                    + " icon=" + icon);
        }
        StatusBarIconView view = new StatusBarIconView(this, slot);
        view.set(icon);
        mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(mIconSize, mIconSize));
    }

    public void updateIcon(String slot, int index, int viewIndex,
            StatusBarIcon old, StatusBarIcon icon) {
        if (SPEW_ICONS) {
            Slog.d(TAG, "updateIcon slot=" + slot + " index=" + index + " viewIndex=" + viewIndex
                    + " old=" + old + " icon=" + icon);
        }
        StatusBarIconView view = (StatusBarIconView)mStatusIcons.getChildAt(viewIndex);
        view.set(icon);
    }

    public void removeIcon(String slot, int index, int viewIndex) {
        if (SPEW_ICONS) {
            Slog.d(TAG, "removeIcon slot=" + slot + " index=" + index + " viewIndex=" + viewIndex);
        }
        mStatusIcons.removeViewAt(viewIndex);
    }

    public void addNotification(IBinder key, StatusBarNotification notification) {
        boolean shouldTick = true;
        if (notification.notification.fullScreenIntent != null) {
            shouldTick = false;
            Slog.d(TAG, "Notification has fullScreenIntent; sending fullScreenIntent");
            try {
                notification.notification.fullScreenIntent.send();
            } catch (PendingIntent.CanceledException e) {

            }
        }

        if (addNotificationViews(key, notification) != null) {          
            if (shouldTick) {
                try {
                    tick(notification);
                }catch (Exception ex) {
                    ex.printStackTrace();
                    this.recreateStatusBar();
                }
            }

            // Recalculate the position of the sliding windows and the titles.
            // Begin, added by zhumeiquan for new req SW1 #5492, 20120524            
            updateUsbNotification(notification, true); 
            // End
            setAreThereNotifications();
            updateExpandedViewPos(EXPANDED_LEAVE_ALONE);
        }
    }

    public void updateNotification(IBinder key, StatusBarNotification notification) {
        NotificationData oldList;
        int oldIndex = mOngoing.findEntry(key);
        if (oldIndex >= 0) {
            oldList = mOngoing;
        } else {
            oldIndex = mLatest.findEntry(key);
            if (oldIndex < 0) {
                Slog.w(TAG, "updateNotification for unknown key: " + key);
                return;
            }
            oldList = mLatest;
        }
        final NotificationData.Entry oldEntry = oldList.getEntryAt(oldIndex);
        final StatusBarNotification oldNotification = oldEntry.notification;
        final RemoteViews oldContentView = oldNotification.notification.contentView;

        final RemoteViews contentView = notification.notification.contentView;

        if (false) {
            Slog.d(TAG, "old notification: when=" + oldNotification.notification.when
                    + " ongoing=" + oldNotification.isOngoing()
                    + " expanded=" + oldEntry.expanded
                    + " contentView=" + oldContentView);
            Slog.d(TAG, "new notification: when=" + notification.notification.when
                    + " ongoing=" + oldNotification.isOngoing()
                    + " contentView=" + contentView);
        }

        // Can we just reapply the RemoteViews in place?  If when didn't change, the order
        // didn't change.
        if (notification.notification.when == oldNotification.notification.when
                && notification.isOngoing() == oldNotification.isOngoing()
                && oldEntry.expanded != null
                && contentView != null && oldContentView != null
                && contentView.getPackage() != null
                && oldContentView.getPackage() != null
                && oldContentView.getPackage().equals(contentView.getPackage())
                && oldContentView.getLayoutId() == contentView.getLayoutId()) {
            if (SPEW) Slog.d(TAG, "reusing notification");
            oldEntry.notification = notification;
            try {
                // Reapply the RemoteViews
                contentView.reapply(this, oldEntry.content);
                // update the contentIntent
                final PendingIntent contentIntent = notification.notification.contentIntent;
                if (contentIntent != null) {
                    oldEntry.content.setOnClickListener(new Launcher(contentIntent,
                            notification.pkg, notification.tag, notification.id));
                }
                // Update the icon.
                final StatusBarIcon ic = new StatusBarIcon(notification.pkg,
                        notification.notification.icon, notification.notification.iconLevel,
                        notification.notification.number);
                if (!oldEntry.icon.set(ic)) {
                    handleNotificationError(key, notification, "Couldn't update icon: " + ic);
                    return;
                }
            }
            catch (RuntimeException e) {
                // It failed to add cleanly.  Log, and remove the view from the panel.
                Slog.w(TAG, "Couldn't reapply views for package " + contentView.getPackage(), e);
                removeNotificationViews(key);
                addNotificationViews(key, notification);
            }
        } else {
            if (SPEW) Slog.d(TAG, "not reusing notification");
            removeNotificationViews(key);
            addNotificationViews(key, notification);
        }

        // Restart the ticker if it's still running
        if (notification.notification.tickerText != null
                && !TextUtils.equals(notification.notification.tickerText,
                        oldEntry.notification.notification.tickerText)) {
            try {
                tick(notification);
            }catch (Exception ex) {
                ex.printStackTrace();
                this.recreateStatusBar();
            }
        }

        // Recalculate the position of the sliding windows and the titles.
        // Begin, added by zhumeiquan for new req SW1 #5492, 20120524
        updateUsbNotification(notification, true);
        // End
        setAreThereNotifications();
        updateExpandedViewPos(EXPANDED_LEAVE_ALONE);
    }

    public void removeNotification(IBinder key) {
        if (SPEW) Slog.d(TAG, "removeNotification key=" + key);
        StatusBarNotification old = removeNotificationViews(key);

        if (old != null) {
            // Cancel the ticker if it's still running
            mTicker.removeEntry(old);

            // Recalculate the position of the sliding windows and the titles.            
            // Begin, added by zhumeiquan for new req SW1 #5492, 20120524
            updateUsbNotification(old, false);
            // End
            setAreThereNotifications();
            updateExpandedViewPos(EXPANDED_LEAVE_ALONE);
        }
    }

    private int chooseIconIndex(boolean isOngoing, int viewIndex) {
        final int latestSize = mLatest.size();
        if (isOngoing) {
            return latestSize + (mOngoing.size() - viewIndex);
        } else {
            return latestSize - viewIndex;
        }
    }

    View[] makeNotificationView(final StatusBarNotification notification, ViewGroup parent) {
        Notification n = notification.notification;
        RemoteViews remoteViews = n.contentView;
        if (remoteViews == null) {
            return null;
        }

        // create the row view
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LatestItemContainer row = (LatestItemContainer) inflater.inflate(R.layout.status_bar_latest_event, parent, false);

        // Begin, added by zhumeiquan for new req SW1 #5492, 20120524
        //modified by zhanghui for sovling two usb icon issue ,20120816
        if ("com.android.systemui".equals(notification.pkg) && 
                (n.mTagFlag == 2 || n.mTagFlag == 3)) {
            row.setVisibility(View.GONE);
        }
        // End

        if ((n.flags & Notification.FLAG_ONGOING_EVENT) == 0 && (n.flags & Notification.FLAG_NO_CLEAR) == 0) {
            row.setOnSwipeCallback(mTouchDispatcher, new Runnable() {
                //            	row.setOnSwipeCallback(new Runnable() {
                public void run() {
                    try {
                        mBarService.onNotificationClear(notification.pkg, notification.tag, notification.id);
                    } catch (RemoteException e) {
                        // Skip it, don't crash.
                    }
                }
            });
        }

        // bind the click event to the content area
        ViewGroup content = (ViewGroup)row.findViewById(R.id.content);
        content.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        content.setOnFocusChangeListener(mFocusChangeListener);
        PendingIntent contentIntent = n.contentIntent;
        if (contentIntent != null) {
            content.setOnClickListener(new Launcher(contentIntent, notification.pkg,
                    notification.tag, notification.id));
            content.setOnLongClickListener(new Launcher(contentIntent, notification.pkg,
                    notification.tag, notification.id));
        }

        View expanded = null;
        Exception exception = null;
        try {
            expanded = remoteViews.apply(this, content);
        }
        catch (RuntimeException e) {
            exception = e;
        }
        if (expanded == null) {
            String ident = notification.pkg + "/0x" + Integer.toHexString(notification.id);
            Slog.e(TAG, "couldn't inflate view for notification " + ident, exception);
            return null;
        } else {
            content.addView(expanded);
            row.setDrawingCacheEnabled(true);
        }

        return new View[] { row, content, expanded };
    }

    StatusBarIconView addNotificationViews(IBinder key, StatusBarNotification notification) {
        NotificationData list;
        ViewGroup parent;
        final boolean isOngoing = notification.isOngoing();
        if (isOngoing) {
            list = mOngoing;
            parent = mOngoingItems;
        } else {
            list = mLatest;
            parent = mLatestItems;
        }
        // Construct the expanded view.
        final View[] views = makeNotificationView(notification, parent);
        if (views == null) {
            handleNotificationError(key, notification, "Couldn't expand RemoteViews for: "
                    + notification);
            return null;
        }
        final View row = views[0];
        final View content = views[1];
        final View expanded = views[2];
        // Construct the icon.
        final StatusBarIconView iconView = new StatusBarIconView(this,
                notification.pkg + "/0x" + Integer.toHexString(notification.id));
        final StatusBarIcon ic = new StatusBarIcon(notification.pkg, notification.notification.icon,
                notification.notification.iconLevel, notification.notification.number);
        if (!iconView.set(ic)) {
            handleNotificationError(key, notification, "Coulding create icon: " + ic);
            return null;
        }
        // Add the expanded view.
        final int viewIndex = list.add(key, notification, row, content, expanded, iconView);
        // add by shenqi avoid IndexOutOfBoundsException
        if(viewIndex > parent.getChildCount()) {
            Log.d(TAG,"addNotificationViews is abornal view Index = "+viewIndex + "Child count =  " + parent.getChildCount());
            parent.addView(row);
        }
        else {
            parent.addView(row, viewIndex);
        }
        //end by shenqi
        // Add the icon.
        final int iconIndex = chooseIconIndex(isOngoing, viewIndex);
        mNotificationIcons.addView(iconView, iconIndex);
        return iconView;
    }

    StatusBarNotification removeNotificationViews(IBinder key) {
        NotificationData.Entry entry = mOngoing.remove(key);
        if (entry == null) {
            entry = mLatest.remove(key);
            if (entry == null) {
                Slog.w(TAG, "removeNotification for unknown key: " + key);
                return null;
            }
        }
        // Remove the expanded view.
        ((ViewGroup)entry.row.getParent()).removeView(entry.row);
        // Remove the icon.
        ((ViewGroup)entry.icon.getParent()).removeView(entry.icon);

        return entry.notification;
    }

    private void setAreThereNotifications() {
        boolean latest = mLatest.hasVisibleItems();



        if (latest) {
            if(mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES && 
                    mTab == TAB_SWITCHES) {
                return;
            }
            mClearButton.setVisibility(View.VISIBLE);
            mClearButton.setText(R.string.status_bar_clear_all_button);
            startShakeListener();
        } else {
            mClearButton.setVisibility(View.GONE);
            //    mClearButton.setText(R.string.status_bar_no_notifications_title);
            stopShakeListener();
        }
    }
    /**
     * State is one or more of the DISABLE constants from StatusBarManager.
     */
    public void disable(int state) {
        final int old = mDisabled;
        final int diff = state ^ old;
        mDisabled = state;

        Log.d(TAG,"disable,diff="+diff+",state="+state+",old="+old+",mDisabled="+mDisabled);

        // Begin , Added by zhumeiquan for statusbar transparent, 20120315   
        boolean isLockScreen = mKM.inKeyguardRestrictedInputMode();
        if (isLockScreen) {
            mStatusBarView.setBackgroundResource(R.drawable.statusbar_background_keyguard);
        } else if ((diff & StatusBarManager.DISABLE_BACKGROUND) != 0) {
            Log.d(TAG,"diff="+diff+",update background");
            updateStatusBarBackground();                
        }
        // End

        if ((diff & StatusBarManager.DISABLE_EXPAND) != 0) {
            if ((state & StatusBarManager.DISABLE_EXPAND) != 0) {
                if (SPEW) Slog.d(TAG, "DISABLE_EXPAND: yes");
                animateCollapse();
            }
        }
        if ((diff & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
            if ((state & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
                if (SPEW) Slog.d(TAG, "DISABLE_NOTIFICATION_ICONS: yes");
                if (mTicking) {
                    mTicker.halt();
                } else {
                    setNotificationIconVisibility(false, com.android.internal.R.anim.fade_out);
                }
            } else {
                if (SPEW) Slog.d(TAG, "DISABLE_NOTIFICATION_ICONS: no");
                if (!mExpandedVisible) {
                    setNotificationIconVisibility(true, com.android.internal.R.anim.fade_in);
                }
            }
        } else if ((diff & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
            if (mTicking && (state & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
                if (SPEW) Slog.d(TAG, "DISABLE_NOTIFICATION_TICKER: yes");
                mTicker.halt();
            }
        }
    }


    /*    //added by zhuyaopeng 2012/06/29
    private Handler timeHandler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if(msg.what==1){
				 ComponentName cn=getTopComponent();
		         if(!cn.getClassName().equals(inCallScreen)){
	            	resId=R.drawable.statusbar_background_green;
	            	mStatusBarView.setBackgroundResource(resId);
//	            	Log.e(TAG,"timeHandler,changed background");
		         }else{
		        	 resId=R.drawable.statusbar_background;
		             mStatusBarView.setBackgroundResource(resId);
//		             Log.e(TAG,"timeHandler,should not changed background");
		         }
			}
		}

    }; */       


    // Begin , Added by zhumeiquan for statusbar transparent, 20120315     
    private void updateStatusBarBackground() {
        Log.d(TAG, "updateStatusBarBackground");
        boolean isLockScreen = mKM.inKeyguardRestrictedInputMode();    
        int resId = R.drawable.statusbar_background;

        if (isLockScreen) {
            resId = R.drawable.statusbar_background_keyguard;
            Log.d(TAG,"isLockScreen,mDisabled="+mDisabled);
        }else if (mExpandedVisible) {
            resId = R.drawable.statusbar_background;
            Log.d(TAG,"mExpandedVisible,mDisabled="+mDisabled);
        }else if ((mDisabled & StatusBarManager.DISABLE_BACKGROUND) != 0) {
            resId = R.drawable.statusbar_background_transparent;
            Log.d(TAG,"mDisabled,mDisabled="+mDisabled+",statusbarManager value="+StatusBarManager.DISABLE_BACKGROUND);//262144
        }else{
            resId = R.drawable.statusbar_background;
            Log.d(TAG,"else done,mDisabled="+mDisabled);
        } 
        mStatusBarView.setBackgroundResource(resId);
    }
    // End


    /*    
    //added by zhuyaopeng 2012/06/29
    private Handler timeHandler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if(msg.what==1){
				 ComponentName cn=getTopComponent();
		         if(!cn.getClassName().equals(inCallScreen)){
//	            	resId=R.drawable.statusbar_background_green;
//	            	mStatusBarView.setBackgroundResource(resId);
		        	onStatusBarVirtualLayerAttached();
	            	Log.e(TAG,"timeHandler,changed background");
		         }else{
//		        	 resId=R.drawable.statusbar_background;
//		             mStatusBarView.setBackgroundResource(resId);
		        	 onStatusBarVirtualLayerDetached();
		             Log.e(TAG,"timeHandler,should not changed background");
		         }
			}
		}

    };      


    // Begin , Added by zhumeiquan for statusbar transparent, 20120315     
    private void updateStatusBarBackground() {
    	Log.d(TAG, "updateStatusBarBackground");
        boolean isLockScreen = mKM.inKeyguardRestrictedInputMode();    
        int resId = R.drawable.statusbar_background;

        if(mPhoneState!=TelephonyManager.CALL_STATE_OFFHOOK){
        	Log.d(TAG,"not offhook");
        	onStatusBarVirtualLayerDetached();
        	if (isLockScreen) {
                resId = R.drawable.statusbar_background_keyguard;//white
                Log.d(TAG,"isLockScreen");
            } else if (mExpandedVisible) {
                resId = R.drawable.statusbar_background;
                Log.d(TAG,"mExpandedVisible");
            } else if ((mDisabled & StatusBarManager.DISABLE_BACKGROUND) != 0) {//gray
                resId = R.drawable.statusbar_background_transparent;
                Log.d(TAG,"mDisabled");
            }else{
            	resId = R.drawable.statusbar_background;
            	Log.d(TAG,"else done");
            } 
        }else{
//        	Log.e(TAG,"update background,is offhook");
            ComponentName cn=getTopComponent();
            if(!cn.getClassName().equals(inCallScreen)){
//            	resId=R.drawable.statusbar_background_green;
//            	Log.e(TAG,"is offhook,changed background");
            	onStatusBarVirtualLayerAttached();
            }else{
//            	Log.e(TAG,"is inCallScreen");
            	new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(500);
						} catch (Exception e) {
							e.printStackTrace();
						}
						Message message = Message.obtain();
						message.what =1;
						timeHandler.sendMessage(message);
					}
				}).start();          	
            }       	
        }
        mStatusBarView.setBackgroundResource(resId);
    }
    // End
     */    



    /*    
    //begin added by zhuyaopeng 2012/07/07 
    protected void onStatusBarVirtualLayerAttached(){
    	LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	Resources res = getResources();
        final int height= res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);  
		inCallScreenLayer=(FrameLayout) inflater.inflate(R.layout.status_bar_virtual_layer,null);
		int resId=R.drawable.statusbar_background_green;
		inCallScreenLayer.setBackgroundResource(resId);
		int pixelFormat = PixelFormat.RGBA_8888;
		WindowManager.LayoutParams lp;
		lp = new WindowManager.LayoutParams(
	                ViewGroup.LayoutParams.MATCH_PARENT,
	                height,
	                WindowManager.LayoutParams.TYPE_STATUS_BAR,
	                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING,
	                pixelFormat);
	     lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
	     lp.setTitle("StatusBar layer");
	     lp.windowAnimations = com.android.internal.R.style.Animation_StatusBar;
//	     WindowManagerImpl.getDefault().addView(inCallScreenLayer, lp);
	     mStatusBarContainer.addView(inCallScreenLayer,lp);
	     Log.e(TAG,"onStatusBarVirtualLayerAttached");
    }

    protected void onStatusBarVirtualLayerDetached(){
    	if(null!=inCallScreenLayer){
//        	WindowManagerImpl.getDefault().removeView(inCallScreenLayer);
    		mStatusBarContainer.removeView(inCallScreenLayer);
//    		mStatusBarContainer.removeViewAt(1);
//    		mStatusBarContainer.invalidate();
//    		mStatusBarContainer.startLayoutAnimation();
//    		mStatusBarContainer.requestLayout();
//    		mStatusBarContainer.updateViewLayout(mStatusBarContainer, mStatusBarContainer.getLayoutParams());
    		try {
//    			updateStatusBarBackground();
    			recreateStatusBar();
			} catch (Exception e) {
				e.printStackTrace();
			}
        	Log.e(TAG,"onStatusBarVirtualLayerDetached");
    	}
    }    
    //end added
     */



    /**
     * All changes to the status bar and notifications funnel through here and are batched.
     */
    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
            case MSG_ANIMATE:
                doAnimation();
                break;
            case MSG_ANIMATE_REVEAL:
                doRevealAnimation();
                break;
            }
        }
    }

    View.OnFocusChangeListener mFocusChangeListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            // Because 'v' is a ViewGroup, all its children will be (un)selected
            // too, which allows marqueeing to work.
            v.setSelected(hasFocus);
        }
    };

    private void makeExpandedVisible() {
        if (SPEW) Slog.d(TAG, "Make expanded visible: expanded visible=" + mExpandedVisible);
        if (mExpandedVisible) {
            return;
        }
        //add by zhangxianjia 20130117
        if(mSharedPreferences.getBoolean("firstsetupswitchbtn", true)) {
            mSwitchWidget.setupWidget();
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean("firstsetupswitchbtn", false);
            editor.commit();
        }

        //add by zhangxianjia, 20120828, bug9433
        if(mSwitchWidget != null && mSwitchWidget.mHasSetup == 0) {
            mSwitchWidget.setupWidget();
        }

        mExpandedVisible = true;
        if(mBound) {
            mService.setPositiveDataMonitorMode(true);
        }
        visibilityChanged(true);

        //begin,changed by zhuyaopeng 12.05.25
        //      mPowerWidget.updateWidget();
        mSwitchWidget.updateWidget();
        //end,changed by zhuyaopeng 12.05.25

        updateExpandedViewPos(EXPANDED_LEAVE_ALONE_EXT);
        mExpandedParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mExpandedParams.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        mExpandedDialog.getWindow().setAttributes(mExpandedParams);
        mExpandedView.requestFocus(View.FOCUS_FORWARD);
        mTrackingView.setVisibility(View.VISIBLE);
        mExpandedView.setVisibility(View.VISIBLE);        

        /*        if (SwitchWidget.DEBUG) {
             Log.d(TAG, "makeExpandedVisible");
         }
        setStatusBarTransparency(false);*/


        if (!mTicking) {
            setDateViewVisibility(true, com.android.internal.R.anim.fade_in);
            setNotificationIconVisibility(false, com.android.internal.R.anim.fade_out);
            setCarrierViewVisibility(true, com.android.internal.R.anim.fade_in);            
        }


        // Begin, added by zhumeiquan for statusbar transparent, 20120314
        updateStatusBarBackground();
        // End

        //Begin, Add by zhangbo begin for change language
        setAreThereNotifications();
        text1.setText(R.string.monthly_data_flow);
        text2.setText(R.string.today_flow_ext);
        text3.setText(R.string.left_flow_ext);

        updateSystemPerformanceButton();

        long todayuseddata = mSharedPreferences.getLong(DataMonitorService.TODAY_USED, 0);
        long monthleftdata = mSharedPreferences.getLong(DataMonitorService.MONTH_LEFT, 0);    
        long monthused = mSharedPreferences.getLong(DataMonitorService.MONTH_USED, 0);        
        updateTodayLeftData(todayuseddata, monthleftdata, monthused);
        //End
    }

    private LewaPopupTipView mMemoryClearTip;
    private LewaPopupTipView mShortcutTip;

    private void showTips() {
        //        if (mDataMonitorButton == null || mPowerWidget == null || mSharedPreferences == null) {
        if (mDataMonitorButton == null || mSwitchWidget == null || mSharedPreferences == null) {
            return ;
        }
        mTipsShown = true;

        //add tips
        mMemoryClearTip = new LewaPopupTipView(this);
        String slidingString = getString(R.string.tip_status_memoryclear);
        mMemoryClearTip.show(LewaPopupTipView.STYLE_BELOW, slidingString, 
                getResources().getDimensionPixelSize(R.dimen.popWindow_clearmemory_width),
                getResources().getDimensionPixelSize(R.dimen.popWindow_clearmemory_y));

        //modify by zhaolei,for shortcut tips
        /*mShortcutTip = new LewaPopupTipView(this);
        slidingString = getString(R.string.tip_status_shortcut);
        mShortcutTip.show(LewaPopupTipView.STYLE_ABOVE, slidingString, 
                getResources().getDimensionPixelSize(R.dimen.popWindow_shortcut_width),
                getResources().getDimensionPixelSize(R.dimen.popWindow_shortcut_y));*/
    }

    public void animateExpand() {
        if (SPEW) Slog.d(TAG, "Animate expand: expanded=" + mExpanded);
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            return ;
        }
        if (mExpanded) {
            return;
        }

        prepareTracking(0, true);
        performFling(0, 2000.0f, true);

        /*
         * if (SwitchWidget.DEBUG) {
         *     Log.d(TAG, "animateExpand");
         * }
         * setStatusBarTransparency(false);
         */
    }

    public void animateCollapse() {
        Log.d(TAG,"animateCollapse");
        Log.d(TAG,"mDisabled="+mDisabled);//262144
        if(mShortcutTip != null || mMemoryClearTip != null) {
            clearTips();
            return;
        }
        if(mPopuViewForNotifications != null) {
            mPopuViewForNotifications.onfinish();
            if(!mPopuViewForNotifications.mNeedClose) {
                mPopuViewForNotifications = null;
                return;
            }
            mPopuViewForNotifications = null;
        } 

        if (SPEW) {
            Slog.d(TAG, "animateCollapse(): mExpanded=" + mExpanded
                    + " mExpandedVisible=" + mExpandedVisible
                    + " mExpanded=" + mExpanded
                    + " mAnimating=" + mAnimating
                    + " mAnimY=" + mAnimY
                    + " mAnimVel=" + mAnimVel);
        }

        if (!mExpandedVisible) {
            return;
        }

        int y;
        if (mAnimating) {
            y = (int)mAnimY;
        } else {
            if(mBottomBar)
                y = 0;
            else
                y = mDisplay.getHeight()-1;
        }
        // Let the fling think that we're open so it goes in the right direction
        // and doesn't try to re-open the windowshade.
        mExpanded = true;
        prepareTracking(y, false);
        performFling(y, -2000.0f, true);

        /*
         * if (SwitchWidget.DEBUG) {
         *     Log.d(TAG, "animateCollapse");
         * }
         * setStatusBarTransparency(true);
         */
    }

    private void registerCollapseExpandViewReceiver() {
        if (null == collapseViewReceiver) {
            collapseViewReceiver = new CollapseExpandViewReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_COLLAPSE_ViEW);
            intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
            intentFilter.addAction(SwitchButton.QUERY_POWER_STATUS_RULE_ACTION);
            intentFilter.addAction(SwitchButton.QUERY_POWER_STATUS_SLEEP_MODE_RULE_ACTION);
            registerReceiver(collapseViewReceiver, intentFilter);
        }
    }

    class CollapseExpandViewReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                animateCollapse();//close expanded view	
	
                if(intent.getAction().compareTo(Intent.ACTION_LOCALE_CHANGED)==0){
                    //					android.os.Process.killProcess(android.os.Process.myPid());
                    recreateStatusBar();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void performExpand() {
        Log.e(TAG,"performExpand");
        if (SPEW) Slog.d(TAG, "performExpand: mExpanded=" + mExpanded);
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            return ;
        }
        if (mExpanded) {
            return;
        }

        mSoftbuttonVisible = false;
        mKeysView.setKeyViewVisiblity(View.GONE);
        mExpanded = true;
        mStatusBarView.updateQuickNaImage();
        makeExpandedVisible();
        updateExpandedViewPos(EXPANDED_FULL_OPEN);
        //added by krshen
        if (mTab == TAB_NOTIFICATIONS) startShakeListener();

        if (false) postStartTracing();

        if (SwitchWidget.DEBUG) {
            Log.d(TAG, "performExpand");
        }
        // setStatusBarTransparency(false);
    }

    void performCollapse() {
        Log.d(TAG,"performCollapse");
        if (SPEW) Slog.d(TAG, "performCollapse: mExpanded=" + mExpanded
                + " mExpandedVisible=" + mExpandedVisible
                + " mTicking=" + mTicking);

        if (!mExpandedVisible) {
            return;
        }
        mExpandedVisible = false;
        visibilityChanged(false);
        mExpandedParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mExpandedParams.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        mExpandedDialog.getWindow().setAttributes(mExpandedParams);
        mTrackingView.setVisibility(View.GONE);
        mExpandedView.setVisibility(View.GONE);

        //added by zhuyaopeng
        //        monitorExpandedView.setVisibility(View.GONE);

        if ((mDisabled & StatusBarManager.DISABLE_NOTIFICATION_ICONS) == 0) {
            setNotificationIconVisibility(true, com.android.internal.R.anim.fade_in);
        }
        if (mDateView.getVisibility() == View.VISIBLE) {
            setDateViewVisibility(false, com.android.internal.R.anim.fade_out);
        }

        if (mCarrier.getVisibility() == View.VISIBLE) {
            setCarrierViewVisibility(false, com.android.internal.R.anim.fade_out);
        }
        // Begin, Added by zhumeiquan for Statusbar transparent, 20120313       
        updateStatusBarBackground();        
        // End
        //added by krshen
        stopShakeListener();
        if (!mExpanded) {
            return;
        }
        mExpanded = false;
        if(mBound) {
            mService.setPositiveDataMonitorMode(false);
        }
        mStatusBarView.updateQuickNaImage();
    }

    void doAnimation() {
        if (mAnimating) {
            if (SPEW) Slog.d(TAG, "doAnimation");
            if (SPEW) Slog.d(TAG, "doAnimation before mAnimY=" + mAnimY);
            incrementAnim();
            if (SPEW) Slog.d(TAG, "doAnimation after  mAnimY=" + mAnimY);
            if ((!mBottomBar && mAnimY >= mDisplay.getHeight()-1) || (mBottomBar && mAnimY <= 0)) {
                if (SPEW) Slog.d(TAG, "Animation completed to expanded state.");
                mAnimating = false;
                updateExpandedViewPos(EXPANDED_FULL_OPEN);
                performExpand();
            }
            else if ((!mBottomBar && mAnimY < mStatusBarView.getHeight())
                    || (mBottomBar && mAnimY > (mDisplay.getHeight()-mStatusBarView.getHeight()))) {
                if (SPEW) Slog.d(TAG, "Animation completed to collapsed state.");
                mAnimating = false;
                if(mBottomBar)
                    updateExpandedViewPos(mDisplay.getHeight());
                else
                    updateExpandedViewPos(0);
                performCollapse();
            }
            else {
                updateExpandedViewPos((int)mAnimY);
                mCurAnimationTime += ANIM_FRAME_DURATION;
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), mCurAnimationTime);
            }
        }
    }

    void stopTracking() {
        mTracking = false;
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    void incrementAnim() {
        long now = SystemClock.uptimeMillis();
        float t = ((float)(now - mAnimLastTime)) / 1000;            // ms -> s
        final float y = mAnimY;
        final float v = mAnimVel;                                   // px/s
        final float a = mAnimAccel;                                 // px/s/s
        if (mBottomBar) {
            mAnimY = y - (v * t) - (0.5f * a * t * t);                          // px
        } else {
            mAnimY = y + (v * t) + (0.5f * a * t * t);                          // px
        }
        mAnimVel = v + (a * t);                                       // px/s
        mAnimLastTime = now;                                        // ms
    }

    void doRevealAnimation() {
        int h = mCloseOn.getHeight() + mStatusBarView.getHeight();

        if (mBottomBar) {
            h = mDisplay.getHeight() - mStatusBarView.getHeight();
        }

        if (mAnimatingReveal && mAnimating &&
                ((mBottomBar && mAnimY > h) || (!mBottomBar && mAnimY < h))) {
            incrementAnim();
            if ((mBottomBar && mAnimY <= h) || (!mBottomBar && mAnimY >= h)) {
                mAnimY = h;
                updateExpandedViewPos((int)mAnimY);
            } else {
                updateExpandedViewPos((int)mAnimY);
                mCurAnimationTime += ANIM_FRAME_DURATION;
                mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_REVEAL),
                        mCurAnimationTime);
            }
        }
    }

    void prepareTracking(int y, boolean opening) {
        mTracking = true;
        mVelocityTracker = VelocityTracker.obtain();
        if (opening) {
            mAnimAccel = 2000.0f;
            mAnimVel = 200;
            mAnimY = mBottomBar ? mDisplay.getHeight() : (mStatusBarView.getHeight() + mCloseOn.getHeight());
            updateExpandedViewPos((int)mAnimY);
            mAnimating = true;
            mAnimatingReveal = true;
            mHandler.removeMessages(MSG_ANIMATE);
            mHandler.removeMessages(MSG_ANIMATE_REVEAL);
            long now = SystemClock.uptimeMillis();
            mAnimLastTime = now;
            mCurAnimationTime = now + ANIM_FRAME_DURATION;
            mAnimating = true;

            boolean latest = mLatest.hasVisibleItems(); // Intelligent determine  add by shenqi
            boolean ongoing = mOngoing.hasVisibleItems();
            //Log.d(TAG, "StatusBarService.updateLayout" + "latest = "+latest + " ongoing = " + ongoing);
            if((latest ||ongoing)  && mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES && mTab == TAB_SWITCHES) {
                mTab = TAB_NOTIFICATIONS;
                onTabChange();
                /*		    mTabSwitches.setTextColor(0xFF7F7F7F);
	        mTabNotifications.setTextColor(0xFF009EE7);*/
            }
            else if ((!latest && !ongoing) && mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES && mTab == TAB_NOTIFICATIONS) {
                mTab = TAB_SWITCHES;
                onTabChange();
                /*		     mTabSwitches.setTextColor(0xFF009EE7);
	            mTabNotifications.setTextColor(0xFF7F7F7F);*/
            }
            mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE_REVEAL),
                    mCurAnimationTime);
        } else {
            // it's open, close it?
            if (mAnimating) {
                mAnimating = false;
                mHandler.removeMessages(MSG_ANIMATE);
            }
            updateExpandedViewPos(y + mViewDelta);
        }
    }

    void performFling(int y, float vel, boolean always) {
        mAnimatingReveal = false;
        mDisplayHeight = mDisplay.getHeight();

        mAnimY = y;
        mAnimVel = vel;

        //Slog.d(TAG, "starting with mAnimY=" + mAnimY + " mAnimVel=" + mAnimVel);

        if (mExpanded) {
            if (!always &&
                    ((mBottomBar && (vel < -200.0f || (y < 25 && vel < 200.0f))) ||
                            (!mBottomBar && (vel >  200.0f || (y > (mDisplayHeight-25) && vel > -200.0f))))) {
                // We are expanded, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the expanded position.
                mAnimAccel = 2000.0f;
                if (vel < 0) {
                    mAnimVel *= -1;
                }
            } else {
                // We are expanded and are now going to animate away.
                mAnimAccel = -2000.0f;
                if (vel > 0) {
                    mAnimVel *= -1;
                }
            }
        } else {
            if (always
                    || ( mBottomBar && (vel < -200.0f || (y < (mDisplayHeight/2) && vel <  200.0f)))
                    || (!mBottomBar && (vel >  200.0f || (y > (mDisplayHeight/2) && vel > -200.0f)))) {
                // We are collapsed, and they moved enough to allow us to
                // expand.  Animate in the notifications.
                mAnimAccel = 2000.0f;
                if (vel < 0) {
                    mAnimVel *= -1;
                }
            } else {
                // We are collapsed, but they didn't move sufficiently to cause
                // us to retract.  Animate back to the collapsed position.
                mAnimAccel = -2000.0f;
                if (vel > 0) {
                    mAnimVel *= -1;
                }
            }
        }
        long now = SystemClock.uptimeMillis();
        mAnimLastTime = now;
        mCurAnimationTime = now + ANIM_FRAME_DURATION;
        mAnimating = true;
        mHandler.removeMessages(MSG_ANIMATE);
        mHandler.removeMessages(MSG_ANIMATE_REVEAL);
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE), mCurAnimationTime);
        stopTracking();
    }

    //Begin, Added by zhangbo
    boolean mTouchDown = false;
    boolean mTouchUp = false;
    boolean mSoftButtonEnable = true;
    private float firstRawY;
    public static boolean mSoftbuttonVisible = false;
    private ImageView mCloseOn;

    private void updateSoftButton() {
        if(!mSoftButtonEnable) {        
            mSoftbuttonVisible = false;
            if (mKeysView != null) {
                mKeysView.setKeyViewVisiblity(View.GONE);
            }
        }        
    }
    //End
    public void clearTips() {
        if(mMemoryClearTip != null && mMemoryClearTip.isShowing()) {
            mMemoryClearTip.dismiss();
            mMemoryClearTip = null;
        }
        if(mShortcutTip != null && mShortcutTip.isShowing()) {
            mShortcutTip.dismiss();
            mShortcutTip = null;
        }
    }

    boolean interceptTouchEvent(MotionEvent event) {
        if (SPEW) {
            Slog.d(TAG, "Touch: rawY=" + event.getRawY() + " event=" + event + " mDisabled="
                    + mDisabled);
        }
        //modify by zhaolei,120327,for statusbat touch and launcher tips
        Intent intent = new Intent("com.android.systemui.statusbar.TOUCH");
        sendBroadcast(intent);
        //end

        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
            return false;
        }

        if (!mTrackingView.mIsAttachedToWindow) {
            return false;
        }

        final int statusBarSize = mStatusBarView.getHeight();
        final int hitSize = statusBarSize * 2;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //begin,changed by zhuyaopeng 12.05.25
            int x = (int)event.getRawX();
            if (mLastX == 0) {
                mLastX = x;
                if (SwitchWidget.DEBUG) {
                    Log.d(TAG, "ACTION_DOWN; mLastX = " + Integer.toString(mLastX));
                }
            }
            //end,changed by zhuyaopeng 12.05.25

            //add by zhangbo begin
            firstRawY = event.getRawY();
            mTouchUp = false;
            if (firstRawY < statusBarSize + 10) {
                if (!mExpandedVisible) {
                    mTouchDown = true;
                }
            }
            //add by zhangbo end
            final int y = (int)event.getRawY();
            //            mLinger = 0;//zhuyaopeng
            if (!mExpanded) {
                mViewDelta = mBottomBar ? mDisplay.getHeight() - y : statusBarSize - y;
            } else {
                mTrackingView.getLocationOnScreen(mAbsPos);
                mViewDelta = mAbsPos[1] + (mBottomBar ? 0 : mTrackingView.getHeight()) - y;
            }
            if ((!mBottomBar && ((!mExpanded && y < hitSize) || ( mExpanded && y > (mDisplay.getHeight()-hitSize)))) 
                    || (mBottomBar && (( mExpanded && y < hitSize) || (!mExpanded && y > (mDisplay.getHeight()-hitSize))))) {

                // We drop events at the edge of the screen to make the windowshade come
                // down by accident less, especially when pushing open a device with a keyboard
                // that rotates (like g1 and droid)

                //zhuyaopeng
                //                int x = (int)event.getRawX();

                final int edgeBorder = mEdgeBorder;
                int edgeLeft = mButtonsLeft ? mStatusBarView.getSoftButtonsWidth() : 0;
                int edgeRight = mButtonsLeft ? 0 : mStatusBarView.getSoftButtonsWidth();

                final int w = mDisplay.getWidth();
                final int deadLeft = w / 2 - w / 4;  // left side of the dead zone
                final int deadRight = w / 2 + w / 4; // right side of the dead zone

                boolean expandedHit = (mExpanded && (x >= edgeBorder && x < w - edgeBorder));
                boolean collapsedHit = (!mExpanded && (x >= edgeBorder + edgeLeft && x < w - edgeBorder - edgeRight)
                        && (!mDeadZone || mDeadZone && (x < deadLeft || x > deadRight)));

                if (expandedHit || collapsedHit) {
                    prepareTracking(y, !mExpanded);// opening if we're not already fully visible
                    mVelocityTracker.addMovement(event);
                }
            }
        } else if (mTracking) {
            mVelocityTracker.addMovement(event);
            int minY = statusBarSize + mCloseOn.getHeight();
            if (mBottomBar) {
                minY = mDisplay.getHeight() - statusBarSize - mCloseView.getHeight();
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                //add by zhangbo begin
                mTouchUp = false;
                if (event.getRawY() - firstRawY > 10.0) {
                    mTouchDown = false;
                    makeExpandedVisible();
                }
                //add by zhangbo end
                int y = (int)event.getRawY();
                if ((!mBottomBar && mAnimatingReveal && y < minY) ||
                        (mBottomBar && mAnimatingReveal && y > minY)) {
                    //changed by zhuyaopeng
                    /*try {
                        if (Settings.System.getInt(mContext.getContentResolver() , Settings.System.STATUS_BAR_BRIGHTNESS_TOGGLE) == 1) {
                            //Credit for code goes to daryelv github : https://github.com/daryelv/android_frameworks_base
                            // See if finger is moving left/right an adequate amount
                            mVelocityTracker.computeCurrentVelocity(1000);
                            float yVel = mVelocityTracker.getYVelocity();
                            if (yVel < 0) {
                                yVel = -yVel;
                            }

                            if (yVel < 50.0f) {
                                if (mLinger > 50) {
                                    // Check that Auto-Brightness not enabled
                                    Context context = mStatusBarView.getContext();
                                    boolean auto_brightness = false;
                                    int brightness_mode = 0;
                                    try {
                                        brightness_mode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
                                    } catch (SettingNotFoundException e){
                                        auto_brightness = false;
                                    }
                                    auto_brightness = (brightness_mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                                    if (auto_brightness) {
                                        // do nothing - Don't manually set brightness from statusbar
                                    } else {
                                        // set brightness according to x position on statusbar
                                        float x = (float)event.getRawX();
                                        float screen_width = (float)(context.getResources().getDisplayMetrics().widthPixels);
                                        // Brightness set from the 90% of pixels in the middle of screen, can't always get to the edges
                                        int new_brightness = (int)(((x - (screen_width * 0.05f)) / (screen_width * 0.9f)) * (float)android.os.Power.BRIGHTNESS_ON );
                                        // don't let screen go completely dim or past 100% bright
                                        if (new_brightness < 10) {
                                            new_brightness = 10;
                                        }

                                        if (new_brightness > android.os.Power.BRIGHTNESS_ON ) {
                                            new_brightness = android.os.Power.BRIGHTNESS_ON;
                                        }

                                        // Set the brightness
                                        try {
                                            IPowerManager.Stub.asInterface(ServiceManager.getService("power")).setBacklightBrightness(new_brightness);
                                            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, new_brightness);
                                        } catch (Exception e){
                                            Slog.w(TAG, "Setting Brightness failed: " + e);
                                        }
                                    }
                                }else {
                                    mLinger++;
                                }
                            } else {
                                mLinger = 0;
                            }
                        }
                    } catch (SettingNotFoundException e) {

                    }*/
                } else {
                    mAnimatingReveal = false;
                    updateExpandedViewPos(y + (mBottomBar ? -mViewDelta : mViewDelta));
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                //add by zhangbo begin
                if (mSoftButtonEnable) {
                    mTouchDown = false;
                    if (event.getRawY() < statusBarSize + 10) {
                        if (!mExpandedVisible) {
                            mTouchUp = true;
                            if (mSoftbuttonVisible == false) {
                                mSoftbuttonVisible = true;
                                mKeysView.setKeyViewVisiblity(View.VISIBLE);
                            } else {
                                mSoftbuttonVisible = false;
                                mKeysView.setKeyViewVisiblity(View.GONE);
                            }
                            return false;
                        }
                    }
                } else {
                    mTouchDown = false;
                    if (event.getRawY() < statusBarSize + 10) {
                        if(!mExpandedVisible) {
                            mTouchUp = true;
                        }
                    }
                }
                //add by zhangbo end
                mVelocityTracker.computeCurrentVelocity(1000);
                float yVel = mVelocityTracker.getYVelocity();
                boolean negative = yVel < 0;

                float xVel = mVelocityTracker.getXVelocity();
                if (xVel < 0) {
                    xVel = -xVel;
                }

                if (xVel > 150.0f) {
                    xVel = 150.0f; // limit how much we care about the x axis
                }

                float vel = (float)Math.hypot(yVel, xVel);
                if (negative) {
                    vel = -vel;
                }
                performFling((int)event.getRawY(), vel, false);       
            }
        } 
        return false;
    }

    private class Launcher implements View.OnClickListener, View.OnLongClickListener {
        private PendingIntent mIntent;
        private String mPkg;
        private String mTag;
        private int mId;

        Launcher(PendingIntent intent, String pkg, String tag, int id) {
            mIntent = intent;
            mPkg = pkg;
            mTag = tag;
            mId = id;
        }

        public void onClick(View v) {
            try {
                // The intent we are sending is for the application, which
                // won't have permission to immediately start an activity after
                // the user switches to home.  We know it is safe to do at this
                // point, so make sure new activity switches are now allowed.
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {

            }

            if (mIntent != null) {
                int[] pos = new int[2];
                v.getLocationOnScreen(pos);
                Intent overlay = new Intent();
                overlay.setSourceBounds(
                        new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
                try {
                    mIntent.send(StatusBarService.this, 0, overlay);
                } catch (PendingIntent.CanceledException e) {
                    // the stack trace isn't very helpful here.  Just log the exception message.
                    Slog.w(TAG, "Sending contentIntent failed: " + e);
                }
            }

            try {
                mBarService.onNotificationClick(mPkg, mTag, mId);
            } catch (RemoteException ex) {
                // system process is dead if we're here.
            }

            // close the shade if it was open
            animateCollapse();
        }

        @Override
        public boolean onLongClick(View v) {
            // TODO Auto-generated method stub
            mPopuViewForNotifications = new PopuViewForNotifications(mContext,StatusBarService.this, mPkg,mTag,mId);
            return true;
        }
    }

    private void tick(StatusBarNotification n) {
        // Show the ticker if one is requested. Also don't do this
        // until status bar window is attached to the window manager,
        // because...  well, what's the point otherwise?  And trying to
        // run a ticker without being attached will crash!
        if (n.notification.tickerText != null && mStatusBarView.getWindowToken() != null) {
            if (0 == (mDisabled & (StatusBarManager.DISABLE_NOTIFICATION_ICONS
                    | StatusBarManager.DISABLE_NOTIFICATION_TICKER))) {
                if(!mHasSoftButtons || mStatusBarView.getSoftButtonsWidth() == 0)
                    mTicker.addEntry(n);
            }
        }
    }

    /**
     * Cancel this notification and tell the StatusBarManagerService / NotificationManagerService
     * about the failure.
     *
     * WARNING: this will call back into us.  Don't hold any locks.
     */
    void handleNotificationError(IBinder key, StatusBarNotification n, String message) {
        removeNotification(key);
        try {
            mBarService.onNotificationError(n.pkg, n.tag, n.id, n.uid, n.initialPid, message);
        } catch (RemoteException ex) {
            // The end is nigh.
        }
    }

    private class MyTicker extends Ticker {
        MyTicker(Context context, CmStatusBarView sb) {
            super(context, sb);
        }

        @Override
        void tickerStarting() {
            if (SPEW) Slog.d(TAG, "tickerStarting");
            mTicking = true;
            mIcons.setVisibility(View.GONE);
            mTickerView.setVisibility(View.VISIBLE);
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.push_up_in, null));
            mIcons.startAnimation(loadAnim(com.android.internal.R.anim.push_up_out, null));
            if (mExpandedVisible) {
                setDateViewVisibility(false, com.android.internal.R.anim.push_up_out);
                setCarrierViewVisibility(false, com.android.internal.R.anim.push_up_out);
            }
        }

        @Override
        void tickerDone() {
            if (SPEW) Slog.d(TAG, "tickerDone");
            mTicking = false;
            mIcons.setVisibility(View.VISIBLE);
            mTickerView.setVisibility(View.GONE);
            mIcons.startAnimation(loadAnim(com.android.internal.R.anim.push_down_in, null));
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.push_down_out, null));
            if (mExpandedVisible) {
                setDateViewVisibility(true, com.android.internal.R.anim.push_down_in);
                setNotificationIconVisibility(false, com.android.internal.R.anim.fade_out);
                setCarrierViewVisibility(true, com.android.internal.R.anim.push_down_in);              
            }
        }

        void tickerHalting() {
            if (SPEW) Slog.d(TAG, "tickerHalting");
            mTicking = false;
            mIcons.setVisibility(View.VISIBLE);
            mTickerView.setVisibility(View.GONE);
            mIcons.startAnimation(loadAnim(com.android.internal.R.anim.fade_in, null));
            mTickerView.startAnimation(loadAnim(com.android.internal.R.anim.fade_out, null));
            if (mExpandedVisible) {
                setDateViewVisibility(true, com.android.internal.R.anim.fade_in);
                setNotificationIconVisibility(false, com.android.internal.R.anim.fade_out);
                setCarrierViewVisibility(true, com.android.internal.R.anim.fade_in);
            }
        }
    }

    private Animation loadAnim(int id, Animation.AnimationListener listener) {
        Animation anim = AnimationUtils.loadAnimation(StatusBarService.this, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    }

    public String viewInfo(View v) {
        return "(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom()
                + " " + v.getWidth() + "x" + v.getHeight() + ")";
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump StatusBar from from pid="
                    + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());
            return;
        }

        synchronized (mQueueLock) {
            pw.println("Current Status Bar state:");
            pw.println("  mExpanded=" + mExpanded
                    + ", mExpandedVisible=" + mExpandedVisible);
            pw.println("  mTicking=" + mTicking);
            pw.println("  mTracking=" + mTracking);
            pw.println("  mAnimating=" + mAnimating
                    + ", mAnimY=" + mAnimY + ", mAnimVel=" + mAnimVel
                    + ", mAnimAccel=" + mAnimAccel);
            pw.println("  mCurAnimationTime=" + mCurAnimationTime
                    + " mAnimLastTime=" + mAnimLastTime);
            pw.println("  mDisplayHeight=" + mDisplayHeight
                    + " mAnimatingReveal=" + mAnimatingReveal
                    + " mViewDelta=" + mViewDelta);
            pw.println("  mDisplayHeight=" + mDisplayHeight);
            pw.println("  mExpandedParams: " + mExpandedParams);
            pw.println("  mExpandedView: " + viewInfo(mExpandedView));
            pw.println("  mExpandedDialog: " + mExpandedDialog);
            pw.println("  mTrackingParams: " + mTrackingParams);
            pw.println("  mTrackingView: " + viewInfo(mTrackingView));
            //pw.println("  mOngoingTitle: " + viewInfo(mOngoingTitle));
            pw.println("  mOngoingItems: " + viewInfo(mOngoingItems));
            //pw.println("  mLatestTitle: " + viewInfo(mLatestTitle));
            pw.println("  mLatestItems: " + viewInfo(mLatestItems));
            //pw.println("  mNoNotificationsTitle: " + viewInfo(mNoNotificationsTitle));
            pw.println("  mCloseView: " + viewInfo(mCloseView));
            pw.println("  mTickerView: " + viewInfo(mTickerView));
            pw.println("  mScrollView: " + viewInfo(mScrollView)
                    + " scroll " + mScrollView.getScrollX() + "," + mScrollView.getScrollY());
            //pw.println("  mBottomScrollView: " + viewInfo(mBottomScrollView)
            // + " scroll " + mBottomScrollView.getScrollX() + "," + mBottomScrollView.getScrollY());
            pw.println("mNotificationLinearLayout: " + viewInfo(mNotificationLinearLayout));
            //pw.println("mBottomNotificationLinearLayout: " + viewInfo(mBottomNotificationLinearLayout));
        }

        if (true) {
            // must happen on ui thread
            mHandler.post(new Runnable() {
                public void run() {
                    Slog.d(TAG, "mStatusIcons:");
                    mStatusIcons.debug();
                }
            });
        }

    }

    void onBarViewAttached() {
        WindowManager.LayoutParams lp;
        int pixelFormat;
        Drawable bg;

        /// ---------- Tracking View --------------
        pixelFormat = PixelFormat.TRANSLUCENT;
        bg = mTrackingView.getBackground();
        if (bg != null) {
            pixelFormat = bg.getOpacity();
        }

        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                pixelFormat);
        //        lp.token = mStatusBarView.getWindowToken();
        lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
        lp.setTitle("TrackingView");
        lp.y = mTrackingPosition;
        mTrackingParams = lp;

        WindowManagerImpl.getDefault().addView(mTrackingView, lp);
    }

    void onBarViewDetached() {
        WindowManagerImpl.getDefault().removeView(mTrackingView);
    }

    void onTrackingViewAttached() {
        WindowManager.LayoutParams lp;
        int pixelFormat;
        Drawable bg;

        /// ---------- Expanded View --------------
        pixelFormat = PixelFormat.TRANSLUCENT;

        final int disph = mDisplay.getHeight();
        lp = mExpandedDialog.getWindow().getAttributes();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = getExpandedHeight();
        lp.x = 0;
        mTrackingPosition = lp.y = (mBottomBar ? disph : -disph); // sufficiently large positive
        lp.type = WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL;
        lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_DITHER
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.format = pixelFormat;
        lp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
        lp.setTitle("StatusBarExpanded");
        mExpandedDialog.getWindow().setAttributes(lp);
        mExpandedDialog.getWindow().setFormat(pixelFormat);
        mExpandedParams = lp;

        mExpandedDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        //changed by zhuyaopeng
        mExpandedDialog.setContentView(mExpandedView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        /*        mExpandedDialog.setContentView(monitorExpandedView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                           ViewGroup.LayoutParams.MATCH_PARENT));*/
        //end changed
        mExpandedDialog.getWindow().setBackgroundDrawable(null);
        mExpandedDialog.show();
        //changed by zhuyaopeng
        //        FrameLayout hack = (FrameLayout)mExpandedView.getParent();
    }

    void onTrackingViewDetached() {
    }

    void setDateViewVisibility(boolean visible, int anim) {
        /*    	 // woody
        if (visible || (!visible && mDateView.getVisibility() != View.VISIBLE)) {
            return;
        }*/

        mDateView.setUpdates(visible);
        mDateView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    void setCarrierViewVisibility(boolean visible, int anim) {
        mCarrier.setVisibility(visible ? View.VISIBLE : View.GONE);
        mCarrier.startAnimation(loadAnim(anim, null));
    }

    void setNotificationIconVisibility(boolean visible, int anim) {
        int old = mNotificationIcons.getVisibility();
        int v = visible ? View.VISIBLE : View.INVISIBLE;
        if (old != v) {
            mNotificationIcons.setVisibility(v);
            if (v == View.VISIBLE)
            {
                mNotificationIcons.startAnimation(loadAnim(anim, null));
            }
        }
    }

    void updateExpandedViewPos(int expandedPosition) {
        if (SPEW) {
            Slog.d(TAG, "updateExpandedViewPos before expandedPosition=" + expandedPosition
                    + " mTrackingParams.y="
                    + ((mTrackingParams == null) ? "???" : mTrackingParams.y)
                    + " mTrackingPosition=" + mTrackingPosition);
        }
        //add by zhangbo begin
        if(mTouchDown == true) {
            return ;
        }
        //add by zhangbo end

        int h = mBottomBar ? 0 : mStatusBarView.getHeight();
        int disph = mDisplay.getHeight();

        // If the expanded view is not visible, make sure they're still off screen.
        // Maybe the view was resized.
        if (!mExpandedVisible) {
            if (mTrackingView != null) {
                mTrackingPosition = mBottomBar ? disph : -disph;
                if (mTrackingParams != null) {
                    mTrackingParams.y = mTrackingPosition;
                    WindowManagerImpl.getDefault().updateViewLayout(mTrackingView, mTrackingParams);
                }
            }
            if (mExpandedParams != null) {
                mExpandedParams.y = mBottomBar ? disph : -disph;
                mExpandedDialog.getWindow().setAttributes(mExpandedParams);
            }
            return;
        }

        // tracking view...
        int pos;
        if (expandedPosition == EXPANDED_FULL_OPEN) {
            pos = h;
        } else if (expandedPosition == EXPANDED_LEAVE_ALONE) {
            pos = mTrackingPosition;
        } else if (expandedPosition == EXPANDED_LEAVE_ALONE_EXT) {
            pos = mTrackingPosition + mCloseOn.getHeight() + h;
        } else {
            if ((mBottomBar && expandedPosition >= 0) || (!mBottomBar && expandedPosition <= disph)) {
                pos = expandedPosition;
            } else {
                pos = disph;
            }
            pos -= mBottomBar ? mCloseView.getHeight() : disph - h;
        }
        if (mBottomBar && pos < 0) {
            pos = 0;
        }

        mTrackingPosition = mTrackingParams.y = pos;
        mTrackingParams.height = disph - h;
        WindowManagerImpl.getDefault().updateViewLayout(mTrackingView, mTrackingParams);

        if(pos > 0 || pos == EXPANDED_LEAVE_ALONE) {
            mTipsShown = mSharedPreferences.getBoolean(DataMonitorService.TIPS_SHOWN, false);

            if (!mTipsShown) {
                if (mShortcutTip == null || mMemoryClearTip == null) {
                    showTips();
                    SharedPreferences sp = getSharedPreferences(
                            DataMonitorService.PREFERENCE_NAME,
                            mContext.MODE_WORLD_READABLE);
                    SharedPreferences.Editor editor = sp.edit();

                    editor.putBoolean(DataMonitorService.TIPS_SHOWN, true);
                    editor.commit();
                }                
            }
        }
        if (mExpandedParams != null) {
            //changed by zhuyaopeng
            mCloseView.getLocationInWindow(mPositionTmp);
            final int closePos = mPositionTmp[1];

            mExpandedContents.getLocationInWindow(mPositionTmp);
            final int contentsBottom = mPositionTmp[1] + mExpandedContents.getHeight();

            if (expandedPosition != EXPANDED_LEAVE_ALONE) {
                if(mBottomBar) {
                    mExpandedParams.y = pos + mCloseView.getHeight();
                } else {
                    /*    mExpandedParams.y = pos + mTrackingView.getHeight()
                        - (mTrackingParams.height-closePos) - contentsBottom;*/
                    mExpandedParams.y = pos + mTrackingView.getHeight()
                            - (mTrackingParams.height-closePos) - disph;
                }
                int max = mBottomBar ? mDisplay.getHeight() : h;
                if (mExpandedParams.y > max) {
                    mExpandedParams.y = max;
                }
                int min = mBottomBar ? mCloseView.getHeight() : mTrackingPosition;
                if (mExpandedParams.y < min) {
                    mExpandedParams.y = min;
                    if (mBottomBar) {
                        mTrackingParams.y = 0;
                    }
                }

                boolean visible = mBottomBar ? mTrackingPosition < mDisplay.getHeight()
                        : (mTrackingPosition + mTrackingView.getHeight()) > h;
                        if (!visible) {
                            // if the contents aren't visible, move the expanded view way off screen
                            // because the window itself extends below the content view.
                            mExpandedParams.y = mBottomBar ? disph : -disph;
                        }
                        mExpandedDialog.getWindow().setAttributes(mExpandedParams);

                        if (SPEW) Slog.d(TAG, "updateExpandedViewPos visibilityChanged(" + visible + ")");
                        visibilityChanged(visible);
            }
        }

        if (SPEW) {
            Slog.d(TAG, "updateExpandedViewPos after  expandedPosition=" + expandedPosition
                    + " mTrackingParams.y=" + mTrackingParams.y
                    + " mTrackingView.getHeight=" + mTrackingView.getHeight()
                    + " mTrackingPosition=" + mTrackingPosition
                    + " mExpandedParams.y=" + mExpandedParams.y
                    + " mExpandedParams.height=" + mExpandedParams.height);
        }
    }

    int getExpandedHeight() {
        return mDisplay.getHeight() - mStatusBarView.getHeight() - mCloseView.getHeight();
    }

    void updateExpandedHeight() {
        if (mExpandedView != null) {
            mExpandedParams.height = getExpandedHeight();
            mExpandedDialog.getWindow().setAttributes(mExpandedParams);
        }
    }

    /**
     * The LEDs are turned o)ff when the notification panel is shown, even just a little bit.
     * This was added last-minute and is inconsistent with the way the rest of the notifications
     * are handled, because the notification isn't really cancelled.  The lights are just
     * turned off.  If any other notifications happen, the lights will turn back on.  Steve says
     * this is what he wants. (see bug 1131461)
     */
    void visibilityChanged(boolean visible) {
        if (mPanelSlightlyVisible != visible) {
            mPanelSlightlyVisible = visible;
            try {
                mBarService.onPanelRevealed();
            } catch (RemoteException ex) {
                // Won't fail unless the world has ended.
            }
        }
    }

    /**
     * not use this func
     * @param net
     */
    void performDisableActions(int net) {
        int old = mDisabled;
        int diff = net ^ old;
        mDisabled = net;

        // act accordingly
        if ((diff & StatusBarManager.DISABLE_EXPAND) != 0) {
            if ((net & StatusBarManager.DISABLE_EXPAND) != 0) {
                Slog.d(TAG, "DISABLE_EXPAND: yes");
                animateCollapse();
            }
        }
        if ((diff & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
            if ((net & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
                Slog.d(TAG, "DISABLE_NOTIFICATION_ICONS: yes");
                if (mTicking) {
                    mNotificationIcons.setVisibility(View.INVISIBLE);
                    mTicker.halt();
                } else {
                    setNotificationIconVisibility(false, com.android.internal.R.anim.fade_out);
                }
            } else {
                Slog.d(TAG, "DISABLE_NOTIFICATION_ICONS: no");
                if (!mExpandedVisible) {
                    setNotificationIconVisibility(true, com.android.internal.R.anim.fade_in);
                }
            }
        } else if ((diff & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
            Slog.d(TAG, "DISABLE_NOTIFICATION_TICKER: "
                    + (((net & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0)
                            ? "yes" : "no"));
            if (mTicking && (net & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
                mTicker.halt();
            }
        }
    }

    private View.OnClickListener mClearButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                mBarService.onClearAllNotifications();
            } catch (RemoteException ex) {
                // system process is dead if we're here.
            } 
            animateCollapse(); 
        }
    };

    void clearNotifications() {
        try {
            mBarService.onClearAllNotifications();
            stopShakeListener();
        } catch (RemoteException ex) {
            // system process is dead if we're here.
        } 
    }

    private void startShakeListener(){
        if(mShakeClear && mExpanded && mLatest.hasVisibleItems()) {
            if(mShaker == null) {
                mShaker = new ShakeListener(this);
            } else {
                mShaker.resume();
            }
        }
    }

    private void stopShakeListener(){
        if(mShaker != null) {
            mShaker.pause();
            mShaker = null;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)
                    || Intent.ACTION_SCREEN_OFF.equals(action)) {
                animateCollapse();
                updateCarrierLabel();//1205
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
                updateResources();
            } else if (ACTION_TMOBILE_THEME_CHANGED.equals(action)) {
                // Normally it will restart on its own, but sometimes it doesn't.  Other times it's slow. 
                // This will help it restart reliably and faster.
                PendingIntent restartIntent = PendingIntent.getService(mContext, 0, new Intent(mContext, StatusBarService.class), 0);
                AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, restartIntent);
                android.os.Process.killProcess(android.os.Process.myPid());
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                updateCarrierLabel();//1205
            }
        }
    };

    private static void copyNotifications(ArrayList<Pair<IBinder, StatusBarNotification>> dest,
            NotificationData source) {
        int N = source.size();
        for (int i = 0; i < N; i++) {
            NotificationData.Entry entry = source.getEntryAt(i);
            dest.add(Pair.create(entry.key, entry.notification));
        }
    }
    // add by luoyongxing for lewa theme.
    // re-load the notification icons after theme changed.
    private void resetNotificationIcon(){
        int size;
        NotificationData.Entry entry;
        size = mOngoing.size();

        for(int i = 0; i < size; i++){
            entry = mOngoing.getEntryAt(i);
            if(entry.icon != null){
                entry.icon.reset();
            }
        }
        size = mLatest.size();
        for(int i = 0; i < size; i++){
            entry = mLatest.getEntryAt(i);
            if(entry.icon != null){
                entry.icon.reset();
            }
        }
    }
    // add by luoyongxing for lewa theme.
    // re-load the status bar icons after theme changed.
    public void resetStatusBar(){
        /*
		int nIcons = mStatusIcons.getChildCount();
 		for (int i = 0; i < nIcons; i++) {
            StatusBarIconView iconView = (StatusBarIconView)mStatusIcons.getChildAt(i);
           	iconView.reset();
        }

		resetNotificationIcon();
		// reset resources for memery indicator in statusbar.
		updateSystemPerformanceButton();*/
        recreateStatusBar();
    }

    /**
     * Re create Status bar,now is using...
     */
    private void recreateStatusBar() {  
        addedKeysView=false;
        mStatusBarContainer.removeAllViews();

        // extract icons from the soon-to-be recreated viewgroup.
        int nIcons = mStatusIcons.getChildCount();
        ArrayList<StatusBarIcon> icons = new ArrayList<StatusBarIcon>(nIcons);
        ArrayList<String> iconSlots = new ArrayList<String>(nIcons);
        for (int i = 0; i < nIcons; i++) {
            StatusBarIconView iconView = (StatusBarIconView)mStatusIcons.getChildAt(i);
            icons.add(iconView.getStatusBarIcon());
            iconSlots.add(iconView.getStatusBarSlot());
        }

        // extract notifications.
        int nNotifs = mOngoing.size() + mLatest.size();
        ArrayList<Pair<IBinder, StatusBarNotification>> notifications =
                new ArrayList<Pair<IBinder, StatusBarNotification>>(nNotifs);
        copyNotifications(notifications, mOngoing);
        copyNotifications(notifications, mLatest);
        mOngoing.clear();
        mLatest.clear();

        //add by zhangxianjia, 20120827, for change theme systemui out of memory
        if(mExpandedView != null) {
            mExpandedView.removeAllViews();
        }
        makeStatusBarView(this);

        // recreate StatusBarIconViews.
        for (int i = 0; i < nIcons; i++) {
            StatusBarIcon icon = icons.get(i);
            String slot = iconSlots.get(i);
            addIcon(slot, i, i, icon);
        }


        mStatusBarContainer.addView(mStatusBarView);
        updateExpandedViewPos(EXPANDED_LEAVE_ALONE);

        mInitMemory = readAvailMem();

        ExpandedView expanded = (ExpandedView)View.inflate(mContext,
                R.layout.status_bar_expanded, null);
        expanded.mTouchDispatcher = mTouchDispatcher;
        expanded.mService = StatusBarService.this;
        mExpandedDialog = new ExpandedDialog(mContext);
        mExpandedView = expanded;
        mOngoingItems = (LinearLayout)expanded.findViewById(R.id.ongoingItems);
        mLatestItems = (LinearLayout)expanded.findViewById(R.id.latestItems);
        mClearButton = (TextView)expanded.findViewById(R.id.clear_all_button);
        mClearButton.setOnClickListener(mClearButtonListener);
        mScrollView = (ScrollView)expanded.findViewById(R.id.scroll);
        mNotificationLinearLayout = (LinearLayout)expanded.findViewById(R.id.notificationLinearLayout);
        // Begin, added by zhumeiquan for new req SW1 #5492, 20120524            
        mUsbModeNotification = expanded.findViewById(R.id.usb_mode_notification);
        mUsbNotifLister = new USBNotiListener();
        mUsbModeNotification.setOnClickListener(mUsbNotifLister);            
        mUsbModeButton = (LewaCheckBox)expanded.findViewById(R.id.usb_mode_button);
        mUsbModeButtonCheckListener = new USBButtonListener();
        mUsbModeButton.setOnCheckedChangeListener(mUsbModeButtonCheckListener);
        // End

        mExpandedView.setVisibility(View.GONE);

        //begin,changed by zhuyaopeng 12.05.25
        mSwitchWidget = (SwitchWidget)expanded.findViewById(R.id.switch_widget);
        mSwitchWidget.setupSettingsObserver(mHandler);
        mSwitchWidget.setGlobalButtonOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(Settings.System.getInt(getContentResolver(),
                        Settings.System.EXPANDED_HIDE_ONCHANGE, 0) == 1) {
                    animateCollapse();
                }
            }
        });
        mSwitchWidget.setGlobalButtonOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                animateCollapse();
                return true;
            }
        });

        mTabNotifications = (TextView) expanded.findViewById(R.id.image_tab_notifications);
        mTabSwitches = (TextView) expanded.findViewById(R.id.image_tab_switches);
        if(mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES){
            Resources res = mContext.getResources();
            int textColor = res.getColor(R.color.switchtabtxtcolor_on);
            if(mTab==TAB_SWITCHES){
                mTabSwitches.setTextColor(textColor);
            }else if(mTab==TAB_NOTIFICATIONS){
                mTabNotifications.setTextColor(textColor);
            }
        }
        // Set background transparency of tab textviews
        // mTabNotifications.setBackgroundColor(Color.argb(155, 0, 255, 0));
        // mTabSwitches.setBackgroundColor(Color.argb(155, 0, 255, 0));
        mTabSwitches.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTab != TAB_SWITCHES) {
                    mTab = TAB_SWITCHES;
                    onTabChange();
                    /* mTabSwitches.setTextColor(0xFF009EE7);
                      mTabNotifications.setTextColor(0xFF7F7F7F);*/
                }
            }
        });
        mTabNotifications.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTab != TAB_NOTIFICATIONS) {
                    mTab = TAB_NOTIFICATIONS;
                    onTabChange();
                    /* mTabSwitches.setTextColor(0xFF7F7F7F);
                      mTabNotifications.setTextColor(0xFF009EE7);*/
                }
            }
        });
        //end,changed by zhuyaopeng 12.05.25
        // recreate notifications.
        for (int i = 0; i < nNotifs; i++) {
            Pair<IBinder, StatusBarNotification> notifData = notifications.get(i);
            addNotificationViews(notifData.first, notifData.second);
        }

        setAreThereNotifications();
        mDateView.setVisibility(View.INVISIBLE);
        updateCarrierLabel();
        makeMonitorAndSystemView(mContext);  

        mSwitchWidget.setupWidget();

        // Lastly, call to the icon policy to install/update all the icons.
        mIconPolicy = new StatusBarPolicy(StatusBarService.this);

        mKeysView= new KeysView(mContext);

        // set up settings observer
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        // load config to determine if we want statusbar buttons
        mHasSoftButtons = CmSystem.getDefaultBool(mContext, CmSystem.CM_HAS_SOFT_BUTTONS);

        (new applyFirewallRules()).start();

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() { 
            @Override
            public void run() {
                adjustInitMemory();
            }
        };
        handler.postDelayed(runnable, 60000);
    }

    /**
     * Reload some of our resources when the configuration changes.
     *
     * We don't reload everything when the configuration changes -- we probably
     * should, but getting that smooth is tough.  Someday we'll fix that.  In the
     * meantime, just update the things that we know change.
     */
    void updateResources() {
        Resources res = getResources();
        mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);
        if (false) Slog.v(TAG, "updateResources");
    }

    // tracing
    void postStartTracing() {
        mHandler.postDelayed(mStartTracing, 3000);
    }

    void vibrate() {
        android.os.Vibrator vib = (android.os.Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(250);
    }

    Runnable mStartTracing = new Runnable() {
        public void run() {
            vibrate();
            SystemClock.sleep(250);
            Slog.d(TAG, "startTracing");
            android.os.Debug.startMethodTracing("/data/statusbar-traces/trace");
            mHandler.postDelayed(mStopTracing, 10000);
        }
    };

    Runnable mStopTracing = new Runnable() {
        public void run() {
            android.os.Debug.stopMethodTracing();
            Slog.d(TAG, "stopTracing");
            vibrate();
        }
    };

    // Begin, added by zhumeiquan for new req SW1 #5492, 20120524
    //modified by zhanghui for sovling two usb icon issue ,20120816
    private void updateUsbNotification(StatusBarNotification notification, boolean isVisible) {
        if ("com.android.systemui".equals(notification.pkg) && 
                (notification.notification.mTagFlag == 2 || notification.notification.mTagFlag == 3)) {
            if (!isVisible) {
                mUsbModeNotification.setVisibility(View.GONE);
            } else {
                mUsbModeNotification.setVisibility(View.VISIBLE);
                mUsbModeButton.setEnabled(true);
                mUsbModeButton.setOnCheckedChangeListener(null);
                mUsbModeButton.setChecked(notification.notification.mTagFlag == 2 ? true : false);
                mUsbModeButton.setOnCheckedChangeListener(mUsbModeButtonCheckListener);
            }
        }
    }

    private class USBButtonListener implements OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton compoundbutton, boolean isChecked) {
            mUsbModeButton.setEnabled(false);
            Intent intent = new Intent();
            intent.setClass(mContext, com.android.systemui.usb.UsbModeSelection.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent = intent.setAction(isChecked ? "UsbModeSelection.action.MOUNT_STORAGE" 
                    : "UsbModeSelection.action.CHARGE_ONLY");
            startActivity(intent);
        }
    }

    private class USBNotiListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            if (mUsbModeButton.isEnabled()) {
                mUsbModeButton.setChecked(!mUsbModeButton.isChecked());
            }
        }
    }
    // End

    //add by zhangbo begin
    private ImageButton mDataMonitorButton;

    private boolean mSystemPerformanceButtonClicked = false;
    private long mAvailMemory = 0;
    private long mInitMemory = 0;

    private ImageButton mSystemPerformanceButton;
    private ProgressBar mSystemWaitBar;
    private TextView mSystemSuggest;
    private TextView mSystemCount; 
    private ImageView mSystemStar1;
    private ImageView mSystemStar2;
    private ImageView mSystemStar3;
    private ImageView mSystemStar4;
    private ImageView mSystemStar5; 
    private static SharedPreferences mSharedPreferences;
    private static TextView main_month_used;
    private static TextView main_today;
    private static TextView main_left;
    private static ProgressBar main_progressbar;

    /*    private static ImageView indi1;
    private static ImageView indi2;
    private static ImageView indi3; 
    private static ImageView indi4; */

    private TextView text1; 
    private TextView text2; 
    private static TextView text3; 

    private View monitor_layout;
    private static final long GB_UNIT = 1024*1024*1024;
    private static final long MB_UNIT = 1024*1024;
    private static final long KB_UNIT = 1024;

    public static boolean mHasFocus = false;

    static final boolean LOGC = false; 

    //soft button
    static KeysView mKeysView;
    ImageView mHomeButton;
    ImageView mMenuButton;
    ImageView mBackButton;
    public static final int KEYCODE_VIRTUAL_HOME_LONG = KeyEvent.getMaxKeyCode() + 1;
    public static final int KEYCODE_VIRTUAL_BACK_LONG = KeyEvent.getMaxKeyCode() + 2;
    private KeyguardManager mKM;
    private int mLockscreenStyle;//1205
    private static java.text.NumberFormat  formater;
    public static HashMap mHashMap;
    private boolean mDataMonitorButtonClicked = false;

    public void updateTodayLeftData(long todaydata, long leftdata, long monthused){    
        long monthtotal = mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL , 0); 
        String today_UNIT = "K";
        String left_UNIT = "K";
        String monthused_UNIT = "K";
        float today = Float.valueOf(todaydata+"") / KB_UNIT;
        float left = Float.valueOf(leftdata+"") / KB_UNIT;
        float month = Float.valueOf(monthused+"") / KB_UNIT;

        if (today >= KB_UNIT) {
            today = Float.valueOf(todaydata+"") / MB_UNIT;
            today_UNIT = "M";
        }

        if (today >= KB_UNIT) {
            today = Float.valueOf(todaydata+"") / GB_UNIT;
            today_UNIT = "G";
        }

        if (left >= KB_UNIT) {
            left = Float.valueOf(leftdata+"") / MB_UNIT;
            left_UNIT = "M";
        }

        if (left >= KB_UNIT) {
            left = Float.valueOf(leftdata+"") / GB_UNIT;
            left_UNIT = "G";
        }

        if (month >= KB_UNIT) {
            month = Float.valueOf(monthused+"") / MB_UNIT;
            monthused_UNIT = "M";
        }
        if (month >= KB_UNIT) {
            month = Float.valueOf(monthused+"") / GB_UNIT;
            monthused_UNIT = "G";
        }

        String today_str = formater.format(today);
        String left_str = formater.format(left);
        String month_str = formater.format(month);       
        updateTodayLeftData(mContext, month_str, monthused_UNIT, today_str, today_UNIT, left_str, left_UNIT, monthtotal, monthused, todaydata);    
    }

    private static String delPoint(String s){
        return s.split("\\.")[0];
    }
 
    public static String humanReadableByteCount(long bytes, boolean si, boolean detail) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + "B";
        int exp = (int)(Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".substring(exp-1, exp);
        double num = bytes / Math.pow(unit, exp);
        return detail || exp >= 3 ? String.format("%.2f%s", (float)num, pre) : String.format("%d%s", Math.round(num), pre);
    }

    public static void updateTodayLeftData(Context context, String monthuseddata, String monthused_UNIT, String todaydata, String today_UNIT, String leftdata, String left_UNIT,
            long total, long monthused, long todayused){     

        if (main_today == null || main_left == null || main_month_used == null || main_progressbar == null) {
            return;
        }

        long reminddata = mSharedPreferences.getLong(DataMonitorService.REMIND_DATA, 0); 
        long left = mSharedPreferences.getLong(DataMonitorService.MONTH_LEFT, 0); 

        if (left <= reminddata) {
            main_left.setTextColor(Color.RED);
        } else {
            //            main_left.setTextColor(0xffbdbebd);
            int textColor = context.getResources().getColor(R.color.titlebar_textcolor);
            main_left.setTextColor(textColor);//
        }

        long exceeddata = monthused - total;
        if (exceeddata > 0) {
            text3.setText(R.string.exceed_flow_ext);
            main_left.setText(humanReadableByteCount(exceeddata, false, true)); 
        }  else {
            text3.setText(R.string.left_flow_ext);
            main_left.setText(humanReadableByteCount(Math.abs(exceeddata), false, true)); 
        }

        main_today.setText(delPoint(todaydata) + today_UNIT);  
        main_month_used.setText(humanReadableByteCount(total, false, false));

        if (total != 0) {         
            int monthused_rat = (int)((monthused * 100) / total);
            int todayused_rat = (int)((todayused * 100) / total);                     
            int progress = monthused_rat - todayused_rat;
            int secondprogress = monthused_rat;                
            main_progressbar.setProgress(progress);
            main_progressbar.setSecondaryProgress(secondprogress);
        } else {
            main_progressbar.setProgress(0);
            main_progressbar.setSecondaryProgress(0);
        }
    }

    private void makeMonitorAndSystemView(Context context) {
        mSharedPreferences = context.getSharedPreferences(DataMonitorService.PREFERENCE_NAME, Activity.MODE_PRIVATE);
        addDataMonitorView(context);
    }


    /*    public static void updateIndi(int curr) {
        switch (curr) {
        case 0:
            indi1.setImageResource(R.drawable.pagemark);
            indi2.setImageResource(R.drawable.pagepass);
            indi3.setImageResource(R.drawable.pagepass);
            indi4.setImageResource(R.drawable.pagepass);
            break;
        case 1:
            indi1.setImageResource(R.drawable.pagepass);
            indi2.setImageResource(R.drawable.pagemark);
            indi3.setImageResource(R.drawable.pagepass);
            indi4.setImageResource(R.drawable.pagepass);
            break;
        case 2:
            indi1.setImageResource(R.drawable.pagepass);
            indi2.setImageResource(R.drawable.pagepass);
            indi3.setImageResource(R.drawable.pagemark); 
            indi4.setImageResource(R.drawable.pagepass);
            break;
        case 3:
            indi1.setImageResource(R.drawable.pagepass);
            indi2.setImageResource(R.drawable.pagepass);
            indi3.setImageResource(R.drawable.pagepass);  
            indi4.setImageResource(R.drawable.pagemark);
            break;     
        default:
            break;
        }
    }*/

    private void addDataMonitorView(final Context context) {  
        //        ExpandedView mExpandedView =(ExpandedView)View.inflate(context, R.layout.status_bar_expanded_old,null);
        main_progressbar = (ProgressBar)mExpandedView.findViewById(R.id.main_progressbar);
        main_today = (TextView)mExpandedView.findViewById(R.id.main_today); 
        main_left = (TextView)mExpandedView.findViewById(R.id.main_left);
        main_month_used = (TextView)mExpandedView.findViewById(R.id.main_used);

        /*        indi1 = (ImageView)mTrackingView.findViewById(R.id.indi1);
        indi2 = (ImageView)mTrackingView.findViewById(R.id.indi2);
        indi3 = (ImageView)mTrackingView.findViewById(R.id.indi3);
        indi4 = (ImageView)mTrackingView.findViewById(R.id.indi4);*/

        text1 = (TextView)mExpandedView.findViewById(R.id.text1);
        text2 = (TextView)mExpandedView.findViewById(R.id.text2);
        text3 = (TextView)mExpandedView.findViewById(R.id.text3);

        monitor_layout = mExpandedView.findViewById(R.id.monitor_layout);
        monitor_layout.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {

                animateCollapse();//close expanded view                
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(StatusBarService.this, DatamonitorMainActivity.class);
                StatusBarService.this.startActivity(intent);               
            }
        });

        mDataMonitorButton = (ImageButton)mExpandedView.findViewById(R.id.data_monitor_button); 
        mDataMonitorButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mHashMap = new HashMap();
                SharedPreferences sharedpreferences = context
                        .getSharedPreferences(WhiteListActivity.WHITELIST_PREFS, Context.MODE_PRIVATE);
                String s = sharedpreferences.getString(WhiteListActivity.WHITE_LIST, "");
                Log.e("statusbar", "white list : " + s);
                String whitelist[] = WhiteListActivity.parseUidsString(s);   //change the whitelist to String by zhangxianjia
                int i = 0;
                do {
                    int j = whitelist.length;
                    if(i >= j) {
                        break;
                    }
                    mHashMap.put(i, whitelist[i]);
                    i++;
                } while(true);

                if (!mDataMonitorButtonClicked) {
                    mDataMonitorButtonClicked = true;             
                } else {                    
                    return;             
                }
 
                //mAvailMemory = readAvailMem();
                ActivityManager activityManager = (ActivityManager)(mContext.getSystemService(ACTIVITY_SERVICE)); 
                List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
                for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
                    int j = 0;
                    int k = 0;
                    // added by maowenjiang 2012-08-14
                    // get pkg list of a running app precess and corresponding to the white list
                    String[] runningPkgList = runningAppProcessInfo.pkgList;
                    for(j = 0; j < mHashMap.size(); j++) {
                            // modified by maowenjiang 2012-08-14
                            // fixed ttplayer or other apps in white list are invalid 
                            for(k = 0; k < runningPkgList.length; k ++) {
                                if(mHashMap.get(j).toString().equalsIgnoreCase(runningPkgList[k])) {
                                    break;
                                }
                            }
                            if(k != runningPkgList.length) {
                                break;
                            }
                    }
                    if(j != mHashMap.size() || k != runningPkgList.length) {
                        continue;
                    } else if (runningAppProcessInfo.processName.equalsIgnoreCase("android.process.acore")
                            || runningAppProcessInfo.uid <= 1000) {

                    } else {
                        for (String packageName : runningAppProcessInfo.pkgList) {
                            if (packageName.equalsIgnoreCase("android")
                                    || packageName.equalsIgnoreCase("system")
                                    || packageName.equalsIgnoreCase("system_process")
                                    || packageName.equalsIgnoreCase("system_server")
                                    || packageName.equalsIgnoreCase("android.process.media")

                                    || packageName.equalsIgnoreCase("com.android.systemui")
                                    || packageName.equalsIgnoreCase("com.android.phone")
                                    || packageName.equalsIgnoreCase("com.android.wallpaper")
                                    || packageName.equalsIgnoreCase("com.android.settings")
                                    || packageName.equalsIgnoreCase("com.android.deskclock")
                                    || packageName.equalsIgnoreCase("com.android.server")
                                    || packageName.equalsIgnoreCase("com.android.email")
                                    || packageName.equalsIgnoreCase("com.android.bluetooth")
                                    || packageName.equalsIgnoreCase("com.android.inputmethod.latin")

                                    // lewa application list
                                    || packageName.equalsIgnoreCase("com.lewa.spm")
                                    || packageName.equalsIgnoreCase("com.lewa.intercept")
                                    || packageName.equalsIgnoreCase("com.lewa.labi")
                                    || packageName.equalsIgnoreCase("com.lewa.launcher")
                                    || packageName.equalsIgnoreCase("com.lewatek.swapper")

                                    // thirdparty inputmethod list
                                    || packageName.equalsIgnoreCase("com.iflytek.inputmethod")
                                    || packageName.equalsIgnoreCase("com.sohu.inputmethod.sogou")
                                    || packageName.equalsIgnoreCase("com.baidu.input")
                                    || packageName.equalsIgnoreCase("com.tencent.qqpinyin")
                                    || packageName.equalsIgnoreCase("com.cootek.smartinput")

                                    // android provider list
                                    || packageName.equalsIgnoreCase("com.android.providers.calendar")
                                    || packageName.equalsIgnoreCase("com.android.providers.media")
                                    || packageName.equalsIgnoreCase("com.android.providers.downloads")
                                    || packageName.equalsIgnoreCase("com.android.providers.drm")
                                    || packageName.equalsIgnoreCase("com.android.providers.contacts")
                                    || packageName.equalsIgnoreCase("com.android.providers.telephony")

                                    // others
                                    || packageName.equalsIgnoreCase("com.noshufou.android.su")
                                    || packageName.equalsIgnoreCase("com.motorola.usb")
                                    || packageName.equalsIgnoreCase("org.adwfreak.launcher")
                                    || packageName.equalsIgnoreCase("com.android.internal.service.wallpaper.ImageWallpaper")) {
                            } else {
                                activityManager.forceStopPackage(packageName);
                                activityManager.killBackgroundProcesses(packageName);
                                /**begin added by zhuyaopeng**/
                                String specPkg="com.when.android.calendar365";
                                if(packageName.equals(specPkg)){
                                    Intent intent = new Intent("com.when.action.REGISTER_ALARMS");
                                    mContext.sendBroadcast(intent);
                                    Log.d(TAG,"is 365");
                                }
                            }
                        }
                    }
                }

                final Handler handler = new Handler();
                Runnable runnable = new Runnable() {                    
                    @Override
                    public void run() {
                        mDataMonitorButtonClicked = false;
                        long availMemory = readAvailMem();
                        long releaseMemory = availMemory - mAvailMemory;

                        if (releaseMemory < 0 || releaseMemory > getTotalMemory()) {
                            releaseMemory = 0;
                        }  

                        final long releaseM = releaseMemory;
                        final long availM = availMemory;

                        if (releaseM != 0) {
                            if (mExpandedVisible) {
                                Log.e(TAG,"mExpandedVisible=="+mExpandedVisible);
                                mDataMonitorButton.setImageResource(R.drawable.memoryclear_anim);
                                final AnimationDrawable animationDrawable = (AnimationDrawable) mDataMonitorButton    
                                        .getDrawable();
                                animationDrawable.start();

                                int duration = 0;
                                for(int i = 0;i < animationDrawable.getNumberOfFrames();i++){
                                    duration += animationDrawable.getDuration(i); 
                                } 


                                Handler handler = new Handler(); 
                                handler.postDelayed(new Runnable() {

                                    public void run() {               
                                        updateSystemPerformanceButton();
                                        Toast toast = Toast.makeText(mContext, getString(R.string.release_left_memory, releaseM, availM), Toast.LENGTH_SHORT);
                                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                        toast.show();
                                    }

                                }, duration);                                
                            }
                            handler.removeCallbacks(this);
                        } else {
                            if (mExpandedVisible) {
                                Toast toast = Toast.makeText(mContext, R.string.release_memory_ext, Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                                toast.show();
                            }
                            handler.removeCallbacks(this);
                        }
                    }
                };
                handler.postDelayed(runnable, 600);
            }
        });

        mDataMonitorButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                animateCollapse();
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(StatusBarService.this, WhiteListActivity.class);
                StatusBarService.this.startActivity(intent);
                return true;
            }
        });
    }

    private void updateSystemPerformanceButton() {
        long count = calculateSystemCount();
        if (count < 20) {
            //            mDataMonitorButton.setImageResource(R.drawable.systemp1);
            mDataMonitorButton.setImageResource(R.drawable.saoba_clear_red);
        } else if (count < 40) {
            //            mDataMonitorButton.setImageResource(R.drawable.systemp2);
            mDataMonitorButton.setImageResource(R.drawable.saoba_clear_normal);
        } else {
            //            mDataMonitorButton.setImageResource(R.drawable.systemp5);
            mDataMonitorButton.setImageResource(R.drawable.saoba_clear_normal);
        }
    }

    private void adjustInitMemory() {
        ActivityManager activityManager = (ActivityManager)(mContext.getSystemService(ACTIVITY_SERVICE)); 
        if (mHashMap == null) {
            return ;
        }
        for (RunningAppProcessInfo runningAppProcessInfo : activityManager.getRunningAppProcesses()) {
            if (mHashMap.get(runningAppProcessInfo.uid) != null) {

            } else if (runningAppProcessInfo.processName.equalsIgnoreCase("android.process.acore")
                    || runningAppProcessInfo.processName.equalsIgnoreCase("com.lewa.store")
                    || runningAppProcessInfo.processName.equalsIgnoreCase("com.lewa.pond")
                    || runningAppProcessInfo.processName.equalsIgnoreCase("com.lewa.push")) {                    
            } else {
                for (String packageName : runningAppProcessInfo.pkgList) {
                    if (packageName.equalsIgnoreCase("com.lewa.launcher")
                            || packageName.equalsIgnoreCase("com.when.android.calendar365")
                            || packageName.equalsIgnoreCase("com.android.systemui")) {

                    } else {
                        activityManager.killBackgroundProcesses(packageName);
                    }
                }
            }
        }

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() { 
            @Override
            public void run() {
                mInitMemory = readAvailMem();
            }
        };
        handler.postDelayed(runnable, 1000);

    }

    private long calculateSystemCount() {
        long availMemory = readAvailMem();
        mAvailMemory = availMemory;
        long totalMemory = getTotalMemory();
        long count = 0;

        if (totalMemory == 0) {
            count = 99;
        } else {
            count = (availMemory*100)/(totalMemory);
        }

        if (count >= 100) {
            count = 99;
        }
        return count;
    }
    //add by zhangxianjia, the total meminfo calculate
    private long extractMemValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (buffer[index] >= '0' && buffer[index] <= '9') {
                int start = index;
                index++;
                while (index < buffer.length && buffer[index] >= '0'
                        && buffer[index] <= '9') {
                    index++;
                }
                String str = new String(buffer, 0, start, index-start);
                return ((long)Integer.parseInt(str)) * 1024;
            }
            index++;
        }
        return 0;
    }

    private boolean matchText(byte[] buffer, int index, String text) {
        int N = text.length();
        if ((index+N) >= buffer.length) {
            return false;
        }
        for (int i=0; i<N; i++) {
            if (buffer[index+i] != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private long readAvailMem() {
        try {
            long memFree = 0;
            long memCached = 0;
            FileInputStream is = new FileInputStream("/proc/meminfo");
            int len = is.read(mBuffer);
            is.close();
            final int BUFLEN = mBuffer.length;
            for (int i=0; i<len && (memFree == 0 || memCached == 0); i++) {
                if (matchText(mBuffer, i, "MemFree")) {
                    i += 7;
                    memFree = extractMemValue(mBuffer, i);
                } else if (matchText(mBuffer, i, "Cached")) {
                    i += 6;
                    memCached = extractMemValue(mBuffer, i);
                }
                while (i < BUFLEN && mBuffer[i] != '\n') {
                    i++;
                }
            }
            long mLastBackgroundProcessMemory = 0;
            if(mState != null) {
                mState.updateNow();
                mState.waitForData();
                mLastBackgroundProcessMemory = mState.mBackgroundProcessMemory;
            }

            return (memFree + memCached - SECONDARY_SERVER_MEM + mLastBackgroundProcessMemory)/(1024*1024);
        } catch (java.io.FileNotFoundException e) {
        } catch (Exception e) {
        }
        return 0;
    }

    //end
    private long getAvailMemory() {
        Long avail = null;
        BufferedReader reader = null;

        try {
            // Grab a reader to /proc/meminfo
            reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")), 1000);

            // This is memTotal which we don't need
            String line = reader.readLine();

            // This is memFree which we need
            line = reader.readLine();
            String[] free = line.split(":");
            // Have to remove the kb on the end
            String [] memFree = free[1].trim().split(" ");

            // This is Buffers which we don't need
            line = reader.readLine();

            // This is Cached which we need
            line = reader.readLine();
            String[] cached = line.split(":");
            // Have to remove the kb on the end
            String[] memCached = cached[1].trim().split(" ");

            avail = Long.parseLong(memFree[0]) + Long.parseLong(memCached[0]);
            avail = avail / 1024;
        } catch(Exception e) {
            e.printStackTrace();
            // We don't want to return null so default to 0
            avail = Long.parseLong("0");
        } finally {
            // Make sure the reader is closed no matter what
            try { 
                reader.close(); 
            } catch(Exception e) {

            }
            reader = null;
        }
        return avail;
    }
    // add by zhangxianjia, total meminfo caculate
    int mLastNumBackgroundProcesses = -1;
    int mLastNumForegroundProcesses = -1;
    int mLastNumServiceProcesses = -1;
    long mLastBackgroundProcessMemory = -1;
    long mLastForegroundProcessMemory = -1;
    long mLastServiceProcessMemory = -1;
    long mLastAvailMemory = -1;
    long availMem = -1;
    //end
    private long getTotalMemory() {

        //add by zhangxianjia, caculate total meminfo
        availMem = readAvailMem()*1024*1024;
        Log.e("statusbar service meminfo",String.valueOf(availMem));
        long totalMem = -1;
        mState.updateNow();
        synchronized (mState.mLock) {
            if (mLastNumBackgroundProcesses != mState.mNumBackgroundProcesses
                    || mLastBackgroundProcessMemory != mState.mBackgroundProcessMemory
                    || mLastAvailMemory != availMem) {
                mLastNumBackgroundProcesses = mState.mNumBackgroundProcesses;
                mLastBackgroundProcessMemory = mState.mBackgroundProcessMemory;
                mLastAvailMemory = availMem;
            }
            if (mLastNumForegroundProcesses != mState.mNumForegroundProcesses
                    || mLastForegroundProcessMemory != mState.mForegroundProcessMemory
                    || mLastNumServiceProcesses != mState.mNumServiceProcesses
                    || mLastServiceProcessMemory != mState.mServiceProcessMemory) {
                mLastNumForegroundProcesses = mState.mNumForegroundProcesses;
                mLastForegroundProcessMemory = mState.mForegroundProcessMemory;
                mLastNumServiceProcesses = mState.mNumServiceProcesses;
                mLastServiceProcessMemory = mState.mServiceProcessMemory;
            }

            totalMem = availMem
                    + mLastForegroundProcessMemory + mLastServiceProcessMemory;

            Log.e("statusbar service meminfo",String.valueOf(totalMem));

        }
        //end

        /*   Long total = null;
        BufferedReader reader = null;

        try {
           // Grab a reader to /proc/meminfo
           reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")), 1000);

           // Grab the first line which contains mem total
           String line = reader.readLine();

           // Split line on the colon, we need info to the right of the colon
           String[] info = line.split(":");

           // We have to remove the kb on the end
           String[] memTotal = info[1].trim().split(" ");

           // Convert kb into mb
           total = Long.parseLong(memTotal[0]);
           total = total / 1024;
        }
        catch(Exception e) {
           e.printStackTrace();
           // We don't want to return null so default to 0
           total = Long.parseLong("0");
        }
        finally {
           // Make sure the reader is closed no matter what
           try { reader.close(); }
           catch(Exception e) {}
           reader = null;
        }*/
        return totalMem/(1024*1024);
    }

    private DataMonitorService mService;
    private  boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DataMonitorService.LocalBinder binder = (DataMonitorService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        mInitMemory = readAvailMem();

        bindService(new Intent(this, DataMonitorService.class),mConnection,Context.BIND_AUTO_CREATE);
        mStartHandler.post(mStartTasks);
    }

    private Handler mStartHandler = new Handler();

    private Runnable mStartTasks = new Runnable() {
        @Override
        public void run() {
            ExpandedView expanded = (ExpandedView)View.inflate(mContext,
                    R.layout.status_bar_expanded, null);
            expanded.mTouchDispatcher = mTouchDispatcher;
            expanded.mService = StatusBarService.this;
            mExpandedDialog = new ExpandedDialog(mContext);
            mExpandedView = expanded;
            mOngoingItems = (LinearLayout)expanded.findViewById(R.id.ongoingItems);
            mLatestItems = (LinearLayout)expanded.findViewById(R.id.latestItems);
            mClearButton = (TextView)expanded.findViewById(R.id.clear_all_button);
            mClearButton.setOnClickListener(mClearButtonListener);
            mScrollView = (ScrollView)expanded.findViewById(R.id.scroll);
            mNotificationLinearLayout = (LinearLayout)expanded.findViewById(R.id.notificationLinearLayout);
            // Begin, added by zhumeiquan for new req SW1 #5492, 20120524            
            mUsbModeNotification = expanded.findViewById(R.id.usb_mode_notification);
            mUsbNotifLister = new USBNotiListener();
            mUsbModeNotification.setOnClickListener(mUsbNotifLister);            
            mUsbModeButton = (LewaCheckBox)expanded.findViewById(R.id.usb_mode_button);
            mUsbModeButtonCheckListener = new USBButtonListener();
            mUsbModeButton.setOnCheckedChangeListener(mUsbModeButtonCheckListener);
            // End

            mExpandedView.setVisibility(View.GONE);

            //begin,changed by zhuyaopeng 12.05.25
            mSwitchWidget = (SwitchWidget)expanded.findViewById(R.id.switch_widget);
            mSwitchWidget.setupSettingsObserver(mHandler);
            mSwitchWidget.setGlobalButtonOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(Settings.System.getInt(getContentResolver(),
                            Settings.System.EXPANDED_HIDE_ONCHANGE, 0) == 1) {
                        animateCollapse();
                    }
                }
            });
            mSwitchWidget.setGlobalButtonOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    animateCollapse();
                    return true;
                }
            });

            mTabNotifications = (TextView) expanded.findViewById(R.id.image_tab_notifications);
            mTabSwitches = (TextView) expanded.findViewById(R.id.image_tab_switches);
            if(mStyle == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES){
                Resources res = mContext.getResources();
                int textColor = res.getColor(R.color.switchtabtxtcolor_on);
                if(mTab==TAB_SWITCHES){
                    mTabSwitches.setTextColor(textColor);
                }else if(mTab==TAB_NOTIFICATIONS){
                    mTabNotifications.setTextColor(textColor);
                }
            }
            // Set background transparency of tab textviews
            // mTabNotifications.setBackgroundColor(Color.argb(155, 0, 255, 0));
            // mTabSwitches.setBackgroundColor(Color.argb(155, 0, 255, 0));
            mTabSwitches.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mTab != TAB_SWITCHES) {
                        mTab = TAB_SWITCHES;
                        onTabChange();
                        /* mTabSwitches.setTextColor(0xFF009EE7);
                        mTabNotifications.setTextColor(0xFF7F7F7F);*/
                    }
                }
            });
            mTabNotifications.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mTab != TAB_NOTIFICATIONS) {
                        mTab = TAB_NOTIFICATIONS;
                        onTabChange();
                        /* mTabSwitches.setTextColor(0xFF7F7F7F);
                        mTabNotifications.setTextColor(0xFF009EE7);*/
                    }
                }
            });
            //end,changed by zhuyaopeng 12.05.25

            /*            mPowerWidget = (PowerWidget)mTrackingView.findViewById(R.id.exp_power_stat);
            mExpandedContents = mNotificationLinearLayout;
            mPowerWidget.setupSettingsObserver(mHandler);

            mPowerWidget.setGlobalButtonOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if(Settings.System.getInt(getContentResolver(),
                        Settings.System.EXPANDED_HIDE_ONCHANGE, 0) == 1) {
                            animateCollapse();
                    }
                }
            });

            mPowerWidget.setGlobalButtonOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    animateCollapse();
                    return true;
                }
            });*/


            setAreThereNotifications();
            mDateView.setVisibility(View.INVISIBLE);
            updateCarrierLabel();
            makeMonitorAndSystemView(mContext);

            // Connect in to the status bar manager service
            StatusBarIconList iconList = new StatusBarIconList();
            ArrayList<IBinder> notificationKeys = new ArrayList<IBinder>();
            ArrayList<StatusBarNotification> notifications = new ArrayList<StatusBarNotification>();
            mCommandQueue = new CommandQueue(StatusBarService.this, iconList);

            mBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
            try {
                mBarService.registerStatusBar(mCommandQueue, iconList, notificationKeys, notifications);
            } catch (RemoteException ex) {
                // If the system process isn't there we're doomed anyway.
            }

            // Set up the initial icon state
            int N = iconList.size();
            int viewIndex = 0;
            for (int i = 0; i < N; i++) {
                StatusBarIcon icon = iconList.getIcon(i);
                if (icon != null) {
                    addIcon(iconList.getSlot(i), i, viewIndex, icon);
                    viewIndex++;
                }
            }
            // Set up the initial notification state
            N = notificationKeys.size();
            if (N == notifications.size()) {
                for (int i = 0; i < N; i++) {
                    addNotification(notificationKeys.get(i), notifications.get(i));
                }
            } else {
                Slog.e(TAG, "Notification list length mismatch: keys=" + N
                        + " notifications=" + notifications.size());
            }

            //            mPowerWidget.setupWidget();
            mSwitchWidget.setupWidget();
            //            mSwitchWidget.setupWidget(mStyle);

            // Lastly, call to the icon policy to install/update all the icons.
            mIconPolicy = new StatusBarPolicy(StatusBarService.this);

            mKeysView= new KeysView(mContext);

            // set up settings observer
            SettingsObserver settingsObserver = new SettingsObserver(mHandler);
            settingsObserver.observe();
            // load config to determine if we want statusbar buttons
            mHasSoftButtons = CmSystem.getDefaultBool(mContext, CmSystem.CM_HAS_SOFT_BUTTONS);

            (new applyFirewallRules()).start();

            final Handler handler = new Handler();
            Runnable runnable = new Runnable() { 
                @Override
                public void run() {
                    adjustInitMemory();
                }
            };
            handler.postDelayed(runnable, 60000);
        }

    };

    //begin,changed by zhuyaopeng 12.05.25
    public void onTabChange() {
        Resources res = mContext.getResources();
        int onTextColor = res.getColor(R.color.switchtabtxtcolor_on);
        int offTextColor = res.getColor(R.color.switchtabtxtcolor_off);
        if (mTab == TAB_NOTIFICATIONS) {
            mSwitchWidget.setVisibility(View.GONE);
            mScrollView.setVisibility(View.VISIBLE);
            // if (mBottomBar) {
            // mScrollView.setVisibility(View.GONE);
            // mBottomScrollView.setVisibility(View.VISIBLE);
            // } else {
            // mBottomScrollView.setVisibility(View.GONE);
            // mScrollView.setVisibility(View.VISIBLE);
            // }
            setAreThereNotifications();
            /*            mTabNotifications.setBackgroundResource(R.drawable.switch_tab_on);
            mTabSwitches.setBackgroundResource(R.drawable.switch_tab_off);*/
            mTabNotifications.setBackgroundResource(R.drawable.stat_tab_on);
            mTabSwitches.setBackgroundResource(R.drawable.stat_tab_off);
           
            mTabSwitches.setTextColor(offTextColor);
            mTabNotifications.setTextColor(onTextColor);
            startShakeListener();
        } else {
            mSwitchWidget.setVisibility(View.VISIBLE);
            // mBottomScrollView.setVisibility(View.GONE);
            mScrollView.setVisibility(View.GONE);
            mClearButton.setVisibility(View.GONE);
            /*            mTabNotifications.setBackgroundResource(R.drawable.switch_tab_off);
            mTabSwitches.setBackgroundResource(R.drawable.switch_tab_on);*/
            mTabNotifications.setBackgroundResource(R.drawable.stat_tab_off);
            mTabSwitches.setBackgroundResource(R.drawable.stat_tab_on);

            mTabSwitches.setTextColor(onTextColor);
            mTabNotifications.setTextColor(offTextColor);
            stopShakeListener();
        }
    }
    //end,changed by zhuyaopeng 12.05.25


    private WindowManager mWindowmanager;    
    private WindowManager.LayoutParams mLayoutparams;
    private boolean addedKeysView = false;

    /**
     * Virtual Keys
     */
    private void setupKeysView() {
        mSoftbuttonVisible = false;
        if (mKeysView != null) {
            mKeysView.setKeyViewVisiblity(View.GONE);
        }

        if (mWindowmanager == null) {
            mWindowmanager = (WindowManager)getSystemService("window");
        }

        if (mLayoutparams == null) {
            mLayoutparams = new WindowManager.LayoutParams();
        }

        if (addedKeysView && null!=mKeysView) {
            mWindowmanager.removeViewImmediate(mKeysView);
        }
        mLayoutparams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutparams.width = WindowManager.LayoutParams.FILL_PARENT;
        mLayoutparams.flags = 40;
        mLayoutparams.format = PixelFormat.TRANSPARENT;
        mLayoutparams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mLayoutparams.gravity = mLewaBottomVirtualKey ? Gravity.BOTTOM : Gravity.TOP;
        mWindowmanager.addView(mKeysView, mLayoutparams);
        addedKeysView = true;
        mHomeButton = (ImageView)mKeysView.findViewById(R.id.home);
        mMenuButton = (ImageView)mKeysView.findViewById(R.id.menu);
        mBackButton = (ImageView)mKeysView.findViewById(R.id.back);
        mHomeButton.setOnClickListener(
                new ImageButton.OnClickListener() {
                    public void onClick(View v) {                      
                        Intent setIntent = new Intent(Intent.ACTION_MAIN);
                        setIntent.addCategory(Intent.CATEGORY_HOME);
                        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        StatusBarService.this.startActivity(setIntent);
                    }
                }
                );
        mHomeButton.setOnLongClickListener(
                new ImageButton.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        simulateKeypress(KEYCODE_VIRTUAL_HOME_LONG);
                        return true;
                    }
                }
                );
        mMenuButton.setOnClickListener(
                new ImageButton.OnClickListener() {
                    public void onClick(View v) {
                        if(LOGC) Slog.i(TAG, "Menu clicked");
                        simulateKeypress(KeyEvent.KEYCODE_MENU);
                    }
                }
                );
        mBackButton.setOnClickListener(
                new ImageButton.OnClickListener() {
                    public void onClick(View v) {
                        if(LOGC) Slog.i(TAG, "Back clicked");
                        simulateKeypress(KeyEvent.KEYCODE_BACK);
                    }
                }
                );
        mBackButton.setOnLongClickListener(
                new ImageButton.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        simulateKeypress(KEYCODE_VIRTUAL_BACK_LONG);
                        return true;
                    }
                }
                );
    }

    int getStatusBarHeight() {
        int statusBarSize = 0;
        if (mStatusBarView != null) {
            statusBarSize = mStatusBarView.getHeight();
        }
        return statusBarSize;
    }

    private class applyFirewallRules extends Thread {
        public void run() {
            final boolean enabled = Firewall.isEnabled(mContext);
            if(enabled) {
                Firewall.applySavedRules(mContext, false);
                return;
            }
        }
    }

    private void simulateKeypress(final int keyCode) {
        new Thread(new KeyEventInjector( keyCode )).start();
    }

    private class KeyEventInjector implements Runnable {
        private int keyCode;

        KeyEventInjector(final int keyCode) {
            this.keyCode = keyCode;
        }

        public void run() {
            try {
                if(! (IWindowManager.Stub
                        .asInterface(ServiceManager.getService("window")))
                        .injectKeyEvent(
                                new KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true) ) {
                    Slog.w(TAG, "Key down event not injected");
                    return;
                }
                if(! (IWindowManager.Stub
                        .asInterface(ServiceManager.getService("window")))
                        .injectKeyEvent(
                                new KeyEvent(KeyEvent.ACTION_UP, keyCode), true) ) {
                    Slog.w(TAG, "Key up event not injected");
                }
            } catch(RemoteException ex) {
                Slog.w(TAG, "Error injecting key event", ex);
            }
        }
    }
}
