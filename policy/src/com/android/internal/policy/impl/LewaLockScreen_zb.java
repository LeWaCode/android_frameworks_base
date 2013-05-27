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

package com.android.internal.policy.impl;
 
import com.android.internal.R;
import com.android.internal.telephony.IccCard;
import com.android.internal.widget.DigitalClock;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.RotarySelector;
import com.android.internal.widget.SlidingTab;
import com.android.internal.widget.SlidingTab.OnTriggerListener;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.media.AudioManager;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;


import android.provider.Settings;

import android.widget.ViewFlipper;
import android.widget.RelativeLayout;
import android.widget.ImageView;



import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;

import android.text.format.DateFormat;
import android.text.format.Time;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.database.ContentObserver;
import android.os.Handler;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.BitmapFactory.Options;
import android.database.sqlite.SqliteWrapper;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms.Addr;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.EncodedStringValue;
import android.text.TextUtils;


/**
 * The screen within {@link LockPatternKeyguardView} that shows general
 * information about the device depending on its state, and how to get
 * past it, as applicable.
 */
class LewaLockScreen_zb extends LinearLayout implements KeyguardScreen, KeyguardUpdateMonitor.InfoCallback,
        KeyguardUpdateMonitor.SimStateCallback, SlidingTab.OnTriggerListener, LewaRotarySelector.OnDialTriggerListener,
        LewaCallSelector.OnCallDialTriggerListener, LewaSmsSelector.OnSmsDialTriggerListener,
        OnGesturePerformedListener{

    private static final boolean DBG = false;
    private static final String TAG = "LewaLockScreen";
    private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    
    private LewaRotarySelector mLewaRotarySelector;//add LewaRotarySelector by zhangbo
    private LewaCallSelector mLewaCallSelector;//add LewaRotarySelector by zhangbo
    private LewaSmsSelector mLewaSmsSelector;//add LewaRotarySelector by zhangbo
    private static View unlock_line;
    public static TextView unlock_tips;
    private TextView mNewMsg;
    private TextView mMissCall;    
    private TextView mTime;
    private TextView mDate;
    private String mDateFormatString;
    
    private static ImageView unlock_bg;
    private static AnimationDrawable mbgAnimationDrawable;
    private static ImageView unlock_progress_bg;
    private static AnimationDrawable mprogressbgAnimationDrawable;
    private static View time_date;
    private static Handler mHandler;
    private static Runnable mprogressbgEndRunnable;
    private static Runnable mstartprogressbgRunnable;
    

    private Status mStatus = Status.Normal;

    private LockPatternUtils mLockPatternUtils;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private KeyguardScreenCallback mCallback;

    private TextView mCarrier;
    private SlidingTab mTabSelector;
    private SlidingTab mSelector2;
    private RotarySelector mRotarySelector;
    private DigitalClock mClock;
    //private TextView mDate;
    //private TextView mTime;
    private TextView mAmPm;
    private TextView mStatus1;
    private TextView mStatus2;
    private TextView mScreenLocked;
    private TextView mEmergencyCallText;
    private Button mEmergencyCallButton;
    private ImageButton mPlayIcon;
    private ImageButton mPauseIcon;
    private ImageButton mRewindIcon;
    private ImageButton mForwardIcon;
    private ImageButton mAlbumArt;
    private AudioManager am = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
    private boolean mWasMusicActive = am.isMusicActive();
    private boolean mIsMusicActive = false;
    private GestureLibrary mLibrary;

    private TextView mCustomMsg;
    private TextView mNowPlaying;

    // current configuration state of keyboard and display
    private int mKeyboardHidden;
    private int mCreationOrientation;

    // are we showing battery information?
    private boolean mShowingBatteryInfo = false;

    // last known plugged in state
    public static boolean mPluggedIn = false;

    // last known battery level
    public static int mBatteryLevel = 100;

    private String mNextAlarm = null;
    private Drawable mAlarmIcon = null;
    private String mCharging = null;
    private Drawable mChargingIcon = null;

    private boolean mSilentMode;
    private AudioManager mAudioManager;
    //private String mDateFormatString;
    private boolean mEnableMenuKeyInLockScreen;

    private boolean mTrackballUnlockScreen = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.TRACKBALL_UNLOCK_SCREEN, 0) == 1);

    private boolean mMenuUnlockScreen = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.MENU_UNLOCK_SCREEN, 0) == 1);

    private boolean mLockAlwaysBattery = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ALWAYS_BATTERY, 0) == 1);

    private boolean mLockMusicControls = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 1) == 1);

    private boolean mNowPlayingToggle = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_NOW_PLAYING, 1) == 1);

    private boolean mAlbumArtToggle = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ALBUM_ART, 1) == 1);

    private int mLockMusicHeadset = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_MUSIC_CONTROLS_HEADSET, 0));

    private boolean useLockMusicHeadsetWired = ((mLockMusicHeadset == 1) || (mLockMusicHeadset == 3));
    private boolean useLockMusicHeadsetBT = ((mLockMusicHeadset == 2) || (mLockMusicHeadset == 3));

    private boolean mLockAlwaysMusic = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ALWAYS_MUSIC_CONTROLS, 0) == 1);

    private boolean mCustomAppToggle = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_CUSTOM_APP_TOGGLE, 0) == 1);

    private String mCustomAppActivity = (Settings.System.getString(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_CUSTOM_APP_ACTIVITY));

    private int mLockscreenStyle = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_STYLE_PREF, 3));

    private int mCustomIconStyle = Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_CUSTOM_ICON_STYLE, 1);

    private boolean mRotaryUnlockDown = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ROTARY_UNLOCK_DOWN, 0) == 1);

    private boolean mRotaryHideArrows = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ROTARY_HIDE_ARROWS, 0) == 1);

    private boolean mUseRotaryLockscreen = (mLockscreenStyle == 2);

    private boolean mUseRotaryRevLockscreen = (mLockscreenStyle == 3);

    private boolean mUseLenseSquareLockscreen = (mLockscreenStyle == 4);
    private boolean mLensePortrait = false;

    private double mGestureSensitivity;
    private boolean mGestureTrail;
    private boolean mGestureActive;
    private boolean mHideUnlockTab;
    private int mGestureColor;

    private Bitmap mCustomAppIcon;
    private String mCustomAppName;
    /**
     * The status of this lock screen.
     */
    enum Status {
        /**
         * Normal case (sim card present, it's not locked)
         */
        Normal(true),

        /**
         * The sim card is 'network locked'.
         */
        NetworkLocked(true),

        /**
         * The sim card is missing.
         */
        SimMissing(false),

        /**
         * The sim card is missing, and this is the device isn't provisioned, so we don't let
         * them get past the screen.
         */
        SimMissingLocked(false),

        /**
         * The sim card is PUK locked, meaning they've entered the wrong sim unlock code too many
         * times.
         */
        SimPukLocked(false),

        /**
         * The sim card is locked.
         */
        SimLocked(true);

        private final boolean mShowStatusLines;

        Status(boolean mShowStatusLines) {
            this.mShowStatusLines = mShowStatusLines;
        }

        /**
         * @return Whether the status lines (battery level and / or next alarm) are shown while
         *         in this state.  Mostly dictated by whether this is room for them.
         */
        public boolean showStatusLines() {
            return mShowStatusLines;
        }
    }

    /**
     * In general, we enable unlocking the insecure key guard with the menu key. However, there are
     * some cases where we wish to disable it, notably when the menu button placement or technology
     * is prone to false positives.
     *
     * @return true if the menu key should be enabled
     */
    private boolean shouldEnableMenuKey() {
        final Resources res = getResources(); 
        final boolean configDisabled = res.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
        final boolean isMonkey = SystemProperties.getBoolean("ro.monkey", false);
        final boolean fileOverride = (new File(ENABLE_MENU_KEY_FILE)).exists();
        return !configDisabled || isMonkey || fileOverride;
    }

    /**
     * @param context Used to setup the view.
     * @param configuration The current configuration. Used to use when selecting layout, etc.
     * @param lockPatternUtils Used to know the state of the lock pattern settings.
     * @param updateMonitor Used to register for updates on various keyguard related
     *    state, and query the initial state at setup.
     * @param callback Used to communicate back to the host keyguard view.
     */
    LewaLockScreen_zb(Context context, Configuration configuration, LockPatternUtils lockPatternUtils,
            KeyguardUpdateMonitor updateMonitor,
            KeyguardScreenCallback callback) {
        super(context);

      //[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
        
        this.context = context;
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.deskclock.ALARM_ALERT");
        intentFilter.addAction("com.lewa.action.AlarmLockScreen");
        context.registerReceiver(alarmNotifyReceiver, intentFilter);
        
        //[End]
        
        mLockPatternUtils = lockPatternUtils;
        mUpdateMonitor = updateMonitor;
        mCallback = callback;

        mEnableMenuKeyInLockScreen = shouldEnableMenuKey();

        mCreationOrientation = configuration.orientation;

        mKeyboardHidden = configuration.hardKeyboardHidden;

        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.v(TAG, "***** CREATING LOCK SCREEN", new RuntimeException());
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + " res orient=" + context.getResources().getConfiguration().orientation);
        }
 
        log("LewaLockScreen create!!!");
        final LayoutInflater inflater = LayoutInflater.from(context);
        if (DBG) Log.v(TAG, "Creation orientation = " + mCreationOrientation);
        if (mCreationOrientation != Configuration.ORIENTATION_LANDSCAPE) {
            inflater.inflate(R.layout.keyguard_screen_tab_unlock_1, this, true);
        } else {
            inflater.inflate(R.layout.keyguard_screen_tab_unlock_1, this, true);
            //inflater.inflate(R.layout.keyguard_screen_tab_unlock_land, this, true);
        }
        ViewGroup lockWallpaper = (ViewGroup) findViewById(R.id.root);
        //setBackground(mContext,lockWallpaper);
        
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        mUpdateMonitor.registerInfoCallback(this);
        mUpdateMonitor.registerSimStateCallback(this);
        
        mGestureActive = (Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCKSCREEN_GESTURES_ENABLED, 0) == 1);
        mGestureTrail = (Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCKSCREEN_GESTURES_TRAIL, 0) == 1);
        mGestureColor = Settings.System.getInt(context.getContentResolver(),
                Settings.System.LOCKSCREEN_GESTURES_COLOR, 0xFFFFFF00);
        boolean prefHideUnlockTab = (Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCKSCREEN_GESTURES_DISABLE_UNLOCK, 0) == 1);
        if (!mGestureActive) {
            mGestureTrail = false;
        }
        
        //add by zhangbo
        mLewaRotarySelector = (LewaRotarySelector) findViewById(R.id.lewa_rotary_selector);
        mLewaRotarySelector.setOnDialTriggerListener(this);
        mLewaCallSelector = (LewaCallSelector) findViewById(R.id.miss_call_layout);
        mLewaCallSelector.setOnCallDialTriggerListener(this);
        mLewaSmsSelector = (LewaSmsSelector) findViewById(R.id.new_msg_layout);
        mLewaSmsSelector.setOnSmsDialTriggerListener(this);
        mNewMsg = (TextView) findViewById(R.id.new_msg);
        mMissCall = (TextView) findViewById(R.id.miss_call);
        mTime = (TextView) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);
        mDateFormatString = context.getString(R.string.full_wday_month_day_no_year);  
        unlock_line = findViewById(R.id.unlock_line);
        unlock_tips = (TextView)findViewById(R.id.unlock_tips);
        //unlock_tips.setText(R.string.lock_screen_drag_down);
        //unlock_tips.setVisibility(View.GONE); 
        
        context.getContentResolver().registerContentObserver(Calls.CONTENT_URI, true, mMissedCallContentObserver);
        context.getContentResolver().registerContentObserver(Uri.parse("content://sms"), true, mNewSmsContentObserver);
        context.getContentResolver().registerContentObserver(Uri.parse("content://mms"), true, mNewSmsContentObserver);
        
        mHandler = new Handler();
        mprogressbgEndRunnable = new Runnable() {
            
            @Override
            public void run() {
                // TODO Auto-generated method stub
                onprogressbgAnimationDrawableEnd();
                
            }
        };
        
        mstartprogressbgRunnable = new Runnable() {
            
            @Override
            public void run() {
                // TODO Auto-generated method stub
                startprogressbgAnimationDrawable();
                
            }
        };
        
        time_date = findViewById(R.id.time_date);
        
        unlock_bg = (ImageView)findViewById(R.id.unlock_bg);
        //mbgAnimationDrawable = (AnimationDrawable)getResources().getDrawable(R.anim.lockscreen_charger);
        //unlock_bg.setImageDrawable(mbgAnimationDrawable);
              /*
        mbgAnimationDrawable = new AnimationDrawable();
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_1), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_3), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_5), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_7), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_9), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_11), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_13), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_15), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_17), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_19), 500);
        mbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_breath_21), 500);
        mbgAnimationDrawable.setOneShot(true);
        mbgAnimationDrawable.setVisible(true, true);
            */
        
        unlock_progress_bg = (ImageView)findViewById(R.id.unlock_progress_bg);
        //mprogressbgAnimationDrawable = (AnimationDrawable)getResources().getDrawable(R.anim.lockscreen_charger_progress);
        mprogressbgAnimationDrawable = new AnimationDrawable();
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_1), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_2), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_3), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_4), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_5), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_6), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_7), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_8), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_9), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_10), 100);
        mprogressbgAnimationDrawable.addFrame(bitmap2drawable(R.drawable.charging_progress_11), 100);
            mprogressbgAnimationDrawable.setOneShot(false);
        //mprogressbgAnimationDrawable.setOneShot(true);
        mprogressbgAnimationDrawable.setVisible(true, true);
        unlock_progress_bg.setImageDrawable(mprogressbgAnimationDrawable);
        
        mPluggedIn = mUpdateMonitor.isDevicePluggedIn();
        mBatteryLevel = mUpdateMonitor.getBatteryLevel();

        
        mPlayIcon = (ImageButton) findViewById(R.id.musicControlPlay);
        mPauseIcon = (ImageButton) findViewById(R.id.musicControlPause);
        mRewindIcon = (ImageButton) findViewById(R.id.musicControlPrevious);
        mForwardIcon = (ImageButton) findViewById(R.id.musicControlNext);
        mAlbumArt = (ImageButton) findViewById(R.id.albumArt);
        mNowPlaying = (TextView) findViewById(R.id.musicNowPlaying);
        mNowPlaying.setSelected(true); // set focus to TextView to allow scrolling
        mNowPlaying.setTextColor(0xffffffff);
        
        mPlayIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                refreshMusicStatus();
                if (!am.isMusicActive()) {
                    mPauseIcon.setVisibility(View.VISIBLE);
                    mPlayIcon.setVisibility(View.GONE);
                    mRewindIcon.setVisibility(View.VISIBLE);
                    mForwardIcon.setVisibility(View.VISIBLE);
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                }
            }
        });

        mPauseIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                refreshMusicStatus();
                if (am.isMusicActive()) {
                    mPlayIcon.setVisibility(View.VISIBLE);
                    mPauseIcon.setVisibility(View.GONE);
                    mRewindIcon.setVisibility(View.GONE);
                    mForwardIcon.setVisibility(View.GONE);
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                }
            }
        });

        mRewindIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        });

        mForwardIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        });

        mAlbumArt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent musicIntent = new Intent(Intent.ACTION_VIEW);
                musicIntent.setClassName("com.android.music","com.android.music.MediaPlaybackActivity");
                musicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(musicIntent);
                mCallback.goToUnlockScreen();
            }
        });
        
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mSilentMode = isSilentMode();
        
        //end by zhangbo
        
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.setGestureVisible(mGestureTrail);
        gestures.setGestureColor(mGestureColor);
        boolean GestureCanUnlock = false;
        if (gestures != null) {
            if (mGestureActive) {
                File mStoreFile = new File(Environment.getDataDirectory(),
                        "/misc/lockscreen_gestures");
                mGestureSensitivity = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.LOCKSCREEN_GESTURES_SENSITIVITY, 3);
                mLibrary = GestureLibraries.fromFile(mStoreFile);
                if (mLibrary.load()) {
                    gestures.addOnGesturePerformedListener(this);
                    for (String name : mLibrary.getGestureEntries()) {
                        String[] payload = name.split("___", 2);
                        if ("UNLOCK".equals(payload[1])) {
                            GestureCanUnlock = true;
                            break;
                        }
                    }
                }
            }
        }
        
    }

    static void setBackground(Context bcontext, ViewGroup layout){
        String mLockBack = Settings.System.getString(bcontext.getContentResolver(), Settings.System.LOCKSCREEN_BACKGROUND);
        if (mLockBack!=null){
            if (!mLockBack.isEmpty()){
                try {
                    layout.setBackgroundColor(Integer.parseInt(mLockBack));
                }catch(NumberFormatException e){
                }
            }else{
                String lockWallpaper = "";
                try {
                    lockWallpaper = bcontext.createPackageContext("com.cyanogenmod.cmparts", 0).getFilesDir()+"/lockwallpaper";
                } catch (NameNotFoundException e1) {
                }
                if (!lockWallpaper.isEmpty()){
                    Bitmap lockb = BitmapFactory.decodeFile(lockWallpaper);
                    layout.setBackgroundDrawable(new BitmapDrawable(lockb));
                }
            }
        }
    }

    private void updateRightTabResources() {}
    private void toastMessage(final TextView textView, final String text, final int color, final int iconResourceId) {}
    
    private boolean isSilentMode() {
        return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
    }
    
    private void resetStatusInfo(KeyguardUpdateMonitor updateMonitor) {
        /*mShowingBatteryInfo = updateMonitor.shouldShowBatteryInfo();
        mPluggedIn = updateMonitor.isDevicePluggedIn();
        mBatteryLevel = updateMonitor.getBatteryLevel();*/
        
        mIsMusicActive = am.isMusicActive();

        //mStatus = getCurrentStatus(updateMonitor.getSimState());
        //updateLayout(mStatus);

        //refreshBatteryStringAndIcon();
        //refreshAlarmDisplay();
        refreshMusicStatus();
        refreshPlayingTitle();

        //mDateFormatString = getContext().getString(R.string.full_wday_month_day_no_year);
        refreshTimeAndDateDisplay();
        //updateStatusLines();
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER && mTrackballUnlockScreen)
                || (keyCode == KeyEvent.KEYCODE_MENU && mMenuUnlockScreen)
                /*|| (keyCode == KeyEvent.KEYCODE_MENU && mEnableMenuKeyInLockScreen)*/) {
            log("mMenuUnlockScreen = "+mMenuUnlockScreen+" mEnableMenuKeyInLockScreen = "+mEnableMenuKeyInLockScreen);
            mCallback.goToUnlockScreen();
        }
        return false;
    }

    /** {@inheritDoc} */
    public void onTrigger(View v, int whichHandle) {
        if (whichHandle == SlidingTab.OnTriggerListener.LEFT_HANDLE) {
            mCallback.goToUnlockScreen();
        } else if (whichHandle == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
            toggleSilentMode();
            updateRightTabResources();

            String message = mSilentMode ?
                    getContext().getString(R.string.global_action_silent_mode_on_status) :
                    getContext().getString(R.string.global_action_silent_mode_off_status);

            final int toastIcon = mSilentMode
                ? R.drawable.ic_lock_ringer_off
                : R.drawable.ic_lock_ringer_on;

            final int toastColor = mSilentMode
                ? getContext().getResources().getColor(R.color.keyguard_text_color_soundoff)
                : getContext().getResources().getColor(R.color.keyguard_text_color_soundon);
            toastMessage(mScreenLocked, message, toastColor, toastIcon);
            mCallback.pokeWakelock();
        }
    }

    /** {@inheritDoc} */
    public void onDialTrigger(View v, int whichHandle) {
        boolean mUnlockTrigger=false;
        boolean mCustomAppTrigger=false;

        if(whichHandle == RotarySelector.OnDialTriggerListener.LEFT_HANDLE){
            if(mRotaryUnlockDown)
                mCustomAppTrigger=true;
            else
                mUnlockTrigger=true;
        }
        if(whichHandle == RotarySelector.OnDialTriggerListener.MID_HANDLE){
            if(mRotaryUnlockDown)
                mUnlockTrigger=true;
            else
                mCustomAppTrigger=true;
        }

        if (mUnlockTrigger) {
            mCallback.goToUnlockScreen();
        } else if (mCustomAppTrigger) {
            if (mCustomAppActivity != null) {
                try {
                    Intent i = Intent.parseUri(mCustomAppActivity, 0);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    mContext.startActivity(i);
                    mCallback.goToUnlockScreen();
                } catch (URISyntaxException e) {
                } catch (ActivityNotFoundException e) {
                }
            }
        } else if (whichHandle == RotarySelector.OnDialTriggerListener.RIGHT_HANDLE) {
            // toggle silent mode
            toggleSilentMode();
            updateRightTabResources();

            String message = mSilentMode ? getContext().getString(
                    R.string.global_action_silent_mode_on_status) : getContext().getString(
                    R.string.global_action_silent_mode_off_status);

            final int toastIcon = mSilentMode ? R.drawable.ic_lock_ringer_off
                    : R.drawable.ic_lock_ringer_on;
            final int toastColor = mSilentMode ? getContext().getResources().getColor(
                    R.color.keyguard_text_color_soundoff) : getContext().getResources().getColor(
                    R.color.keyguard_text_color_soundon);
            toastMessage(mScreenLocked, message, toastColor, toastIcon);
            mCallback.pokeWakelock();
        }
    }

    /** {@inheritDoc} */
    public void onGrabbedStateChange(View v, int grabbedState) {
        /*if (grabbedState == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
            mSilentMode = isSilentMode();
            mTabSelector.setRightHintText(mSilentMode ? R.string.lockscreen_sound_on_label
                    : R.string.lockscreen_sound_off_label);
        }*/
        // Don't poke the wake lock when returning to a state where the handle is
        // not grabbed since that can happen when the system (instead of the user)
        // cancels the grab.
        if (grabbedState != SlidingTab.OnTriggerListener.NO_HANDLE) {
            mCallback.pokeWakelock();
        }
    }
 
    /** {@inheritDoc} */
    public void onCallDialTrigger(View v, int whichHandle) {
        boolean mUnlockTrigger=false;
        boolean mCustomAppTrigger=false;

        if(whichHandle == RotarySelector.OnDialTriggerListener.LEFT_HANDLE){
            if(mRotaryUnlockDown)
                mCustomAppTrigger=true;
            else
                mUnlockTrigger=true;
        }
        if(whichHandle == RotarySelector.OnDialTriggerListener.MID_HANDLE){
            if(mRotaryUnlockDown)
                mUnlockTrigger=true;
            else
                mCustomAppTrigger=true;
        }

        if (mUnlockTrigger) {
            mCallback.goToUnlockScreen();
        } else if (mCustomAppTrigger) {
            if (mCustomAppActivity != null) {
                try {
                    Intent i = Intent.parseUri(mCustomAppActivity, 0);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    mContext.startActivity(i);
                    mCallback.goToUnlockScreen();
                } catch (URISyntaxException e) {
                } catch (ActivityNotFoundException e) {
                }
            }
        } else if (whichHandle == RotarySelector.OnDialTriggerListener.RIGHT_HANDLE) {
            // toggle silent mode
            toggleSilentMode();
            updateRightTabResources();

            String message = mSilentMode ? getContext().getString(
                    R.string.global_action_silent_mode_on_status) : getContext().getString(
                    R.string.global_action_silent_mode_off_status);

            final int toastIcon = mSilentMode ? R.drawable.ic_lock_ringer_off
                    : R.drawable.ic_lock_ringer_on;
            final int toastColor = mSilentMode ? getContext().getResources().getColor(
                    R.color.keyguard_text_color_soundoff) : getContext().getResources().getColor(
                    R.color.keyguard_text_color_soundon);
            toastMessage(mScreenLocked, message, toastColor, toastIcon);
            mCallback.pokeWakelock();
        }
    }

    /** {@inheritDoc} */
    public void onCallGrabbedStateChange(View v, int grabbedState) {
        /*if (grabbedState == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
            mSilentMode = isSilentMode();
            mTabSelector.setRightHintText(mSilentMode ? R.string.lockscreen_sound_on_label
                    : R.string.lockscreen_sound_off_label);
        }*/
        // Don't poke the wake lock when returning to a state where the handle is
        // not grabbed since that can happen when the system (instead of the user)
        // cancels the grab.
        if (grabbedState != SlidingTab.OnTriggerListener.NO_HANDLE) {
            mCallback.pokeWakelock();
        }
    }

    /** {@inheritDoc} */
    public void onSmsDialTrigger(View v, int whichHandle) {
        boolean mUnlockTrigger=false;
        boolean mCustomAppTrigger=false;

        if(whichHandle == RotarySelector.OnDialTriggerListener.LEFT_HANDLE){
            if(mRotaryUnlockDown)
                mCustomAppTrigger=true;
            else
                mUnlockTrigger=true;
        }
        if(whichHandle == RotarySelector.OnDialTriggerListener.MID_HANDLE){
            if(mRotaryUnlockDown)
                mUnlockTrigger=true;
            else
                mCustomAppTrigger=true;
        }

        if (mUnlockTrigger) {
            mCallback.goToUnlockScreen();
        } else if (mCustomAppTrigger) {
            if (mCustomAppActivity != null) {
                try {
                    Intent i = Intent.parseUri(mCustomAppActivity, 0);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    mContext.startActivity(i);
                    mCallback.goToUnlockScreen();
                } catch (URISyntaxException e) {
                } catch (ActivityNotFoundException e) {
                }
            }
        } else if (whichHandle == RotarySelector.OnDialTriggerListener.RIGHT_HANDLE) {
            // toggle silent mode
            toggleSilentMode();
            updateRightTabResources();

            String message = mSilentMode ? getContext().getString(
                    R.string.global_action_silent_mode_on_status) : getContext().getString(
                    R.string.global_action_silent_mode_off_status);

            final int toastIcon = mSilentMode ? R.drawable.ic_lock_ringer_off
                    : R.drawable.ic_lock_ringer_on;
            final int toastColor = mSilentMode ? getContext().getResources().getColor(
                    R.color.keyguard_text_color_soundoff) : getContext().getResources().getColor(
                    R.color.keyguard_text_color_soundon);
            toastMessage(mScreenLocked, message, toastColor, toastIcon);
            mCallback.pokeWakelock();
        }
    }

    /** {@inheritDoc} */
    public void onSmsGrabbedStateChange(View v, int grabbedState) {
        /*if (grabbedState == SlidingTab.OnTriggerListener.RIGHT_HANDLE) {
            mSilentMode = isSilentMode();
            mTabSelector.setRightHintText(mSilentMode ? R.string.lockscreen_sound_on_label
                    : R.string.lockscreen_sound_off_label);
        }*/
        // Don't poke the wake lock when returning to a state where the handle is
        // not grabbed since that can happen when the system (instead of the user)
        // cancels the grab.
        if (grabbedState != SlidingTab.OnTriggerListener.NO_HANDLE) {
            mCallback.pokeWakelock();
        }
    }
     
    private Runnable mPendingR1;
    private Runnable mPendingR2;

    private void refreshAlarmDisplay() {
        /*mNextAlarm = mLockPatternUtils.getNextAlarm();
        if (mNextAlarm != null) {
            mAlarmIcon = getContext().getResources().getDrawable(R.drawable.ic_lock_idle_alarm);
        }
        updateStatusLines();*/
    }

    /** {@inheritDoc} */
    public void onRefreshBatteryInfo(boolean showBatteryInfo, boolean pluggedIn,
            int batteryLevel) {
        /*if (DBG) Log.d(TAG, "onRefreshBatteryInfo(" + showBatteryInfo + ", " + pluggedIn + ")");
        mShowingBatteryInfo = showBatteryInfo;
        mPluggedIn = pluggedIn;
        mBatteryLevel = batteryLevel;

        refreshBatteryStringAndIcon();
        updateStatusLines();*/
        boolean oldPluggedState = mPluggedIn;
        mPluggedIn = pluggedIn;
        mBatteryLevel = batteryLevel;
        if (mPluggedIn /*&& mBatteryLevel < 100*/) {
            setprogressbgbylevel(batteryLevel);
            if (!oldPluggedState) {
                startprogressbgAnimationDrawable();               
                if (mBatteryLevel < 100) {
                    unlock_tips.setText(getContext().getString(R.string.lockscreen_plugged_in, mBatteryLevel));
                    unlock_tips.setVisibility(View.VISIBLE);
                } else {
                    stopprogressbgAnimationDrawable();  
                    //stopAllChargeAnimationDrawable();
                    unlock_tips.setText(getContext().getString(R.string.lockscreen_charged));
                    unlock_tips.setVisibility(View.VISIBLE);
                }
            } else {

            }
        } else {
            //if pluggedIn, display finished.
            stopprogressbgAnimationDrawable();  
           // stopAllChargeAnimationDrawable();
            //setUnlocktipsVisible(View.GONE);
            unlock_tips.setVisibility(View.GONE);
        }
    }

    private void refreshBatteryStringAndIcon() {
        if (!mShowingBatteryInfo && !mLockAlwaysBattery || mLensePortrait) {
            mCharging = null;
            return;
        }

        if (mPluggedIn) {
            mChargingIcon =
                getContext().getResources().getDrawable(R.drawable.ic_lock_idle_charging);
            if (mUpdateMonitor.isDeviceCharged()) {
                mCharging = getContext().getString(R.string.lockscreen_charged);
            } else {
                mCharging = getContext().getString(R.string.lockscreen_plugged_in, mBatteryLevel);
            }
        } else {
            if (mBatteryLevel <= 20) {
                mChargingIcon =
                    getContext().getResources().getDrawable(R.drawable.ic_lock_idle_low_battery);
                mCharging = getContext().getString(R.string.lockscreen_low_battery, mBatteryLevel);
            } else {
                mChargingIcon =
                    getContext().getResources().getDrawable(R.drawable.ic_lock_idle_discharging);
                mCharging = getContext().getString(R.string.lockscreen_discharging, mBatteryLevel);
            }
        }
    }

    private void refreshMusicStatus() {
        if ((mWasMusicActive || mIsMusicActive || mLockAlwaysMusic
            || (mAudioManager.isWiredHeadsetOn() && useLockMusicHeadsetWired)
            || (mAudioManager.isBluetoothA2dpOn() && useLockMusicHeadsetBT)) && (mLockMusicControls)) {
            if (am.isMusicActive()) {
                mPauseIcon.setVisibility(View.VISIBLE);
                mPlayIcon.setVisibility(View.GONE);
                mRewindIcon.setVisibility(View.VISIBLE);
                mForwardIcon.setVisibility(View.VISIBLE);
            } else {
                mPlayIcon.setVisibility(View.VISIBLE);
                mPauseIcon.setVisibility(View.GONE);
                mRewindIcon.setVisibility(View.GONE);
                mForwardIcon.setVisibility(View.GONE);
            }
        } else {
            mPlayIcon.setVisibility(View.GONE);
            mPauseIcon.setVisibility(View.GONE);
            mRewindIcon.setVisibility(View.GONE);
            mForwardIcon.setVisibility(View.GONE);
        }
    }
    private void refreshPlayingTitle() {
        String nowPlaying = KeyguardViewMediator.NowPlaying();
        mNowPlaying.setText(nowPlaying);
        mNowPlaying.setVisibility(View.GONE);
        mAlbumArt.setVisibility(View.GONE);

        if (am.isMusicActive() && !nowPlaying.equals("") && mLockMusicControls
                && mCreationOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mNowPlayingToggle)
                mNowPlaying.setVisibility(View.VISIBLE);
            // Set album art
            Uri uri = getArtworkUri(getContext(), KeyguardViewMediator.SongId(),
                    KeyguardViewMediator.AlbumId());
            if (uri != null && mAlbumArtToggle) {
                //mAlbumArt.setImageURI(uri);
                //mAlbumArt.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sendMediaButtonEvent(int code) {
        long eventtime = SystemClock.uptimeMillis();

        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, code, 0);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        getContext().sendOrderedBroadcast(downIntent, null);

        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, code, 0);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        getContext().sendOrderedBroadcast(upIntent, null);
    }

    /** {@inheritDoc} */
    public void onTimeChanged() {
        refreshTimeAndDateDisplay();
        
        //[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
        if((alarm_notify != null) && (alarm_notify.getVisibility() == View.VISIBLE)){
            if(alarm_time != null && alarm_am_pm != null){
                setAlarmTime();
            }
        }
        //[End]
    }

    /** {@inheritDoc} */
    public void onMusicChanged() {
        refreshPlayingTitle();
    }

    private void refreshTimeAndDateDisplay() {
        /*mRotarySelector.invalidate();
        mDate.setText(DateFormat.format(mDateFormatString, new Date()));*/
        Date now = new Date();
        String timeString = DateFormat.getTimeFormat(mContext).format(now).toString();
        String dateString = DateFormat.format(mDateFormatString, now).toString();
        mTime.setText(timeString);
        mDate.setText(dateString);
    }

    private void updateStatusLines() {
        if (!mStatus.showStatusLines()
                || (mCharging == null && mNextAlarm == null) || mLensePortrait) {
            mStatus1.setVisibility(View.INVISIBLE);
            mStatus2.setVisibility(View.INVISIBLE);
        } else if (mCharging != null && mNextAlarm == null) {
            // charging only
            mStatus1.setVisibility(View.VISIBLE);
            mStatus2.setVisibility(View.INVISIBLE);

            mStatus1.setText(mCharging);
            mStatus1.setCompoundDrawablesWithIntrinsicBounds(mChargingIcon, null, null, null);
        } else if (mNextAlarm != null && mCharging == null) {
            // next alarm only
            mStatus1.setVisibility(View.VISIBLE);
            mStatus2.setVisibility(View.INVISIBLE);

            mStatus1.setText(mNextAlarm);
            mStatus1.setCompoundDrawablesWithIntrinsicBounds(mAlarmIcon, null, null, null);
        } else if (mCharging != null && mNextAlarm != null) {
            // both charging and next alarm
            mStatus1.setVisibility(View.VISIBLE);
            mStatus2.setVisibility(View.VISIBLE);

            mStatus1.setText(mCharging);
            mStatus1.setCompoundDrawablesWithIntrinsicBounds(mChargingIcon, null, null, null);
            mStatus2.setText(mNextAlarm);
            mStatus2.setCompoundDrawablesWithIntrinsicBounds(mAlarmIcon, null, null, null);
        }
    }

    /** {@inheritDoc} */
    public void onRefreshCarrierInfo(CharSequence plmn, CharSequence spn) {
        if (DBG) Log.d(TAG, "onRefreshCarrierInfo(" + plmn + ", " + spn + ")");
        //updateLayout(mStatus);
    }

    /**
     * Determine the current status of the lock screen given the sim state and other stuff.
     */
    private Status getCurrentStatus(IccCard.State simState) {
        boolean missingAndNotProvisioned = (!mUpdateMonitor.isDeviceProvisioned()
                && simState == IccCard.State.ABSENT);
        if (missingAndNotProvisioned) {
            return Status.SimMissingLocked;
        }

        switch (simState) {
            case ABSENT:
                return Status.SimMissing;
            case NETWORK_LOCKED:
                return Status.SimMissingLocked;
            case NOT_READY:
                return Status.SimMissing;
            case PIN_REQUIRED:
                return Status.SimLocked;
            case PUK_REQUIRED:
                return Status.SimPukLocked;
            case READY:
                return Status.Normal;
            case UNKNOWN:
                return Status.SimMissing;
        }
        return Status.SimMissing;
    }

    /**
     * Update the layout to match the current status.
     */
    private void updateLayout(Status status) {
        // The emergency call button no longer appears onupdateRightTabResources this screen.
        if (DBG) Log.d(TAG, "updateLayout: status=" + status);

        mCustomAppToggle = (Settings.System.getInt(mContext.getContentResolver(),
                                Settings.System.LOCKSCREEN_CUSTOM_APP_TOGGLE, 0) == 1);
        mRotarySelector.enableCustomAppDimple(mCustomAppToggle);

        mEmergencyCallButton.setVisibility(View.GONE); // in almost all cases

        switch (status) {
            case Normal:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                mUpdateMonitor.getTelephonySpn()));

                // Empty now, but used for sliding tab feedback
                mScreenLocked.setText("");

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                if (mUseRotaryLockscreen || mUseRotaryRevLockscreen || mUseLenseSquareLockscreen) {
                    mRotarySelector.setVisibility(View.VISIBLE);
                    mRotarySelector.setRevamped(mUseRotaryRevLockscreen);
                    mRotarySelector.setLenseSquare(mUseLenseSquareLockscreen);
                    mTabSelector.setVisibility(View.GONE);
                    if (mSelector2 != null) {
                        mSelector2.setVisibility(View.GONE);
                    }
                } else {
                    mRotarySelector.setVisibility(View.GONE);
                    mTabSelector.setVisibility(View.VISIBLE);
                    if (mSelector2 != null) {
                        if (mCustomAppToggle) {
                            mSelector2.setVisibility(View.VISIBLE);
                        } else {
                            mSelector2.setVisibility(View.GONE);
                        }
                    }
                }
                mEmergencyCallText.setVisibility(View.GONE);
                break;
            case NetworkLocked:
                // The carrier string shows both sim card status (i.e. No Sim Card) and
                // carrier's name and/or "Emergency Calls Only" status
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_network_locked_message)));
                mScreenLocked.setText(R.string.lockscreen_instructions_when_pattern_disabled);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                if (mUseRotaryLockscreen || mUseRotaryRevLockscreen || mUseLenseSquareLockscreen) {
                    mRotarySelector.setVisibility(View.VISIBLE);
                    mRotarySelector.setRevamped(mUseRotaryRevLockscreen);
                    mRotarySelector.setLenseSquare(mUseLenseSquareLockscreen);
                    mTabSelector.setVisibility(View.GONE);
                    if (mSelector2 != null) {
                        mSelector2.setVisibility(View.GONE);
                    }
                } else {
                    mRotarySelector.setVisibility(View.GONE);
                    mTabSelector.setVisibility(View.VISIBLE);
                    if (mSelector2 != null) {
                        if (mCustomAppToggle) {
                            mSelector2.setVisibility(View.VISIBLE);
                        } else {
                            mSelector2.setVisibility(View.GONE);
                        }
                    }
                }
                mEmergencyCallText.setVisibility(View.GONE);
                break;
            case SimMissing:
                // text
                mCarrier.setText(R.string.lockscreen_missing_sim_message_short);
                mScreenLocked.setText(R.string.lockscreen_missing_sim_instructions);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                if (mUseRotaryLockscreen || mUseRotaryRevLockscreen || mUseLenseSquareLockscreen) {
                    mRotarySelector.setVisibility(View.VISIBLE);
                    mRotarySelector.setRevamped(mUseRotaryRevLockscreen);
                    mRotarySelector.setLenseSquare(mUseLenseSquareLockscreen);
                    mTabSelector.setVisibility(View.GONE);
                    if (mSelector2 != null) {
                        mSelector2.setVisibility(View.GONE);
                    }
                } else {
                    mRotarySelector.setVisibility(View.GONE);
                    mTabSelector.setVisibility(View.VISIBLE);
                    if (mSelector2 != null) {
                        if (mCustomAppToggle) {
                            mSelector2.setVisibility(View.VISIBLE);
                        } else {
                            mSelector2.setVisibility(View.GONE);
                        }
                    }
                }
                mEmergencyCallText.setVisibility(View.VISIBLE);
                // do not need to show the e-call button; user may unlock
                break;
            case SimMissingLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_missing_sim_message_short)));
                mScreenLocked.setText(R.string.lockscreen_missing_sim_instructions);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mRotarySelector.setVisibility(View.GONE);
                mTabSelector.setVisibility(View.GONE); // cannot unlock
                if (mSelector2 != null) {
                    mSelector2.setVisibility(View.GONE);
                }
                mEmergencyCallText.setVisibility(View.VISIBLE);
                mEmergencyCallButton.setVisibility(View.VISIBLE);
                break;
            case SimLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_locked_message)));

                // layout
                mScreenLocked.setVisibility(View.INVISIBLE);
                if (mUseRotaryLockscreen || mUseRotaryRevLockscreen || mUseLenseSquareLockscreen) {
                    mRotarySelector.setVisibility(View.VISIBLE);
                    mRotarySelector.setRevamped(mUseRotaryRevLockscreen);
                    mRotarySelector.setLenseSquare(mUseLenseSquareLockscreen);
                    mTabSelector.setVisibility(View.GONE);
                    if (mSelector2 != null) {
                        mSelector2.setVisibility(View.GONE);
                    }
                } else {
                    mRotarySelector.setVisibility(View.GONE);
                    mTabSelector.setVisibility(View.VISIBLE);
                    if (mSelector2 != null) {
                        if (mCustomAppToggle) {
                            mSelector2.setVisibility(View.VISIBLE);
                        } else {
                            mSelector2.setVisibility(View.GONE);
                        }
                    }
                }
                mEmergencyCallText.setVisibility(View.GONE);
                break;
            case SimPukLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_puk_locked_message)));
                mScreenLocked.setText(R.string.lockscreen_sim_puk_locked_instructions);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mRotarySelector.setVisibility(View.GONE);
                mTabSelector.setVisibility(View.GONE); // cannot unlock
                if (mSelector2 != null) {
                     mSelector2.setVisibility(View.GONE);
                }
                mEmergencyCallText.setVisibility(View.VISIBLE);
                mEmergencyCallButton.setVisibility(View.VISIBLE);
                break;
        }
        if (mHideUnlockTab) {
            mRotarySelector.setVisibility(View.GONE);
            mTabSelector.setVisibility(View.GONE);
        }
    }

    static CharSequence getCarrierString(CharSequence telephonyPlmn, CharSequence telephonySpn) {
        if (telephonyPlmn != null && (telephonySpn == null || "".contentEquals(telephonySpn))) {
            return telephonyPlmn;
        } else if (telephonySpn != null && (telephonyPlmn == null || "".contentEquals(telephonyPlmn))) {
            return telephonySpn;
        } else if (telephonyPlmn != null && telephonySpn != null) {
            return telephonyPlmn + "|" + telephonySpn;
        } else {
            return "";
        }
    }

    public void onSimStateChanged(IccCard.State simState) {
        if (DBG) Log.d(TAG, "onSimStateChanged(" + simState + ")");
        mStatus = getCurrentStatus(simState);
        //updateLayout(mStatus);
        //updateStatusLines();
    }

    void updateConfiguration() {
        Configuration newConfig = getResources().getConfiguration();
        if (newConfig.orientation != mCreationOrientation) {
            mCallback.recreateMe(newConfig);
        } else if (newConfig.hardKeyboardHidden != mKeyboardHidden) {
            mKeyboardHidden = newConfig.hardKeyboardHidden;
            final boolean isKeyboardOpen = mKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            if (mUpdateMonitor.isKeyguardBypassEnabled() && isKeyboardOpen) {
                mCallback.goToUnlockScreen();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.v(TAG, "***** LOCK ATTACHED TO WINDOW");
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + ", new config=" + getResources().getConfiguration());
        }
        updateConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (LockPatternKeyguardView.DEBUG_CONFIGURATION) {
            Log.w(TAG, "***** LOCK CONFIG CHANGING", new RuntimeException());
            Log.v(TAG, "Cur orient=" + mCreationOrientation
                    + ", new config=" + newConfig);
        }
        updateConfiguration();
    }

    /** {@inheritDoc} */
    public boolean needsInput() {
        return false;
    }

    /** {@inheritDoc} */
    public void onPause() {
        //stopAllChargeAnimationDrawable();
        android.util.Log.e("Fulianwu","***********onPause*************");
        stopprogressbgAnimationDrawable();
    }

    /** {@inheritDoc} */
    public void onResume() {
        log("LewaLockScreen resume!!!");
        android.util.Log.e("Fulianwu","***********onResume*************");
        /*Date now = new Date();
        String timeString = DateFormat.getTimeFormat(mContext).format(now).toString();
        String dateString = DateFormat.format(mDateFormatString, now).toString();
        mTime.setText(timeString);
        mDate.setText(dateString);*/
        mLewaRotarySelector.Init();
        mIsMusicActive = am.isMusicActive();
        refreshMusicStatus();
        refreshPlayingTitle();
        
        refreshTimeAndDateDisplay();
        refreshSmsDisplay();
        refreshCallDisplay();
        
        mPluggedIn = mUpdateMonitor.isDevicePluggedIn();
        mBatteryLevel = mUpdateMonitor.getBatteryLevel();
        
        if (mPluggedIn /*&& mBatteryLevel < 100*/) {
            setprogressbgbylevel(mBatteryLevel);         
            startprogressbgAnimationDrawable();           
            if (mBatteryLevel < 100) {
                unlock_tips.setText(getContext().getString(R.string.lockscreen_plugged_in, mBatteryLevel));
                unlock_tips.setVisibility(View.VISIBLE);
            } else {
                //stopAllChargeAnimationDrawable();
                stopprogressbgAnimationDrawable();  
                unlock_tips.setText(getContext().getString(R.string.lockscreen_charged));
                unlock_tips.setVisibility(View.VISIBLE);
            }
          
        } else {
            //if pluggedIn, display finished.
            //stopAllChargeAnimationDrawable();
            stopprogressbgAnimationDrawable();  
            //setUnlocktipsVisible(View.GONE);
            unlock_tips.setVisibility(View.GONE);
        }
        
        /*int smscount = getUnreadSmsCount(mContext);
        int callcount = getMissedCallCount(mContext);
        log("smscount = "+smscount+" callcount = "+callcount); 
        if (smscount == 0) {
            mLewaSmsSelector.setVisibility(View.GONE);
        } else {
            if (smscount > 1) {
                mNewMsg.setText(mContext.getString(R.string.lock_screen_new_msgs, smscount));
            } else {
                mNewMsg.setText(mContext.getString(R.string.lock_screen_new_msg, smscount));
            }
            mLewaSmsSelector.setVisibility(View.VISIBLE);
        }
        
        if (callcount == 0) {
            mLewaCallSelector.setVisibility(View.GONE);
        } else {
            if (callcount > 1) {
                mMissCall.setText(mContext.getString(R.string.lock_screen_miss_calls, callcount));
            } else {
                mMissCall.setText(mContext.getString(R.string.lock_screen_miss_call, callcount));
            }
            mLewaCallSelector.setVisibility(View.VISIBLE);
        }*/
        //resetStatusInfo(mUpdateMonitor);
        //mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallButton);
    }

    /** {@inheritDoc} */
    public void cleanUp() {
        android.util.Log.e("Fulianwu","***********cleanUp*************");
        mUpdateMonitor.removeCallback(this); // this must be first
        mLockPatternUtils = null;
        mUpdateMonitor = null;
        mCallback = null;

        context.getContentResolver().unregisterContentObserver(mMissedCallContentObserver);
        context.getContentResolver().unregisterContentObserver(mNewSmsContentObserver);
          
        //[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
        context.unregisterReceiver(alarmNotifyReceiver);
        //[End]
    }

    /** {@inheritDoc} */
    public void onRingerModeChanged(int state) {
        boolean silent = AudioManager.RINGER_MODE_NORMAL != state;
        /*if (silent != mSilentMode) {
            mSilentMode = silent;
            updateRightTabResources();
        }*/
    }

    public void onPhoneStateChanged(String newState) {
        //mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallButton);
    }

    private void toggleSilentMode() {
            // tri state silent<->vibrate<->ring if silent mode is enabled, otherwise toggle silent mode
           /* final boolean mVolumeControlSilent = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.VOLUME_CONTROL_SILENT, 0) != 0;
            mSilentMode = mVolumeControlSilent
                ? ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || !mSilentMode)
                : !mSilentMode;
            if (mSilentMode) {
                final boolean vibe = mVolumeControlSilent
                ? (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE)
                : (Settings.System.getInt(
                    getContext().getContentResolver(),
                    Settings.System.VIBRATE_IN_SILENT, 1) == 1);

                mAudioManager.setRingerMode(vibe
                    ? AudioManager.RINGER_MODE_VIBRATE
                    : AudioManager.RINGER_MODE_SILENT);
            } else {
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }*/
    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
        if (predictions.size() > 0 && predictions.get(0).score > mGestureSensitivity) {
            String[] payload = predictions.get(0).name.split("___", 3);
            String uri = payload[1];
            if (uri != null) {
                if ("UNLOCK".equals(uri)) {
                    mCallback.goToUnlockScreen();
                } else if ("SOUND".equals(uri)) {
                    toggleSilentMode();
                    mCallback.pokeWakelock();
                } else if ("FLASHLIGHT".equals(uri)) {
                    mContext.sendBroadcast(new Intent("net.cactii.flash2.TOGGLE_FLASHLIGHT"));
                    mCallback.pokeWakelock();
                } else
                    try {
                        Intent i = Intent.parseUri(uri, 0);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        mContext.startActivity(i);
                        // Run in background if requested
                        if (payload.length > 2) {
                            // Define vibrator
                            Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate pattern when gesture is correct
                            long[] pattern = {
                                0, 200
                            };
                            v.vibrate(pattern, -1);

                            mCallback.pokeWakelock();
                        } else {
                            mCallback.goToUnlockScreen();
                        }
                    } catch (URISyntaxException e) {
                    } catch (ActivityNotFoundException e) {
                    }
            }
        } else {
            mCallback.pokeWakelock(); // reset timeout - give them another chance to gesture
        }
    }

    // shameless kang of music widgets
    public static Uri getArtworkUri(Context context, long song_id, long album_id) {

        if (album_id < 0) {
            // This is something that is not in the database, so get the album art directly
            // from the file.
            if (song_id >= 0) {
                return getArtworkUriFromFile(context, song_id, -1);
            }
            return null;
        }

        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                return uri;
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                // maybe it never existed to begin with.
                return getArtworkUriFromFile(context, song_id, album_id);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
        return null;
    }

    private static Uri getArtworkUriFromFile(Context context, long songid, long albumid) {

        if (albumid < 0 && songid < 0) {
            return null;
        }

        try {
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    return uri;
                }
            } else {
                Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    return uri;
                }
            }
        } catch (FileNotFoundException ex) {
            //
        }
        return null;
    }
    
    private void log(String msg) {
        if (DBG) {
            Log.i(TAG, "zhangbo " + msg);
        }       
    }
    
    private int getMissedCallCount(Context context) {
        int missedCallCount = 0;

        Cursor callCursor = context.getContentResolver().query(Calls.CONTENT_URI, new String[] { Calls.NUMBER, Calls.TYPE, Calls.NEW }, null, null, Calls.DEFAULT_SORT_ORDER);

        if (callCursor != null) {
            while (callCursor.moveToNext()) {
                int type = callCursor.getInt(callCursor.getColumnIndex(Calls.TYPE));
                switch (type) {
                case Calls.MISSED_TYPE:
                    if (callCursor.getInt(callCursor.getColumnIndex(Calls.NEW)) == 1) {
                        missedCallCount++;
                    }
                    break;
                case Calls.INCOMING_TYPE:
                case Calls.OUTGOING_TYPE:
                    break;
                }
            } 
        }
        callCursor.close();

        return missedCallCount;
    }

    private static final String[] SMS_STATUS_PROJECTION = new String[] {
        Sms.THREAD_ID, Sms.DATE, Sms.ADDRESS, Sms.SUBJECT, Sms.BODY };

    private static final String NEW_INCOMING_SM_CONSTRAINT =
            "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
            + " AND " + Sms.SEEN + " = 0)";

    private static final String[] MMS_STATUS_PROJECTION = new String[] {
        Mms.THREAD_ID, Mms.DATE, Mms._ID, Mms.SUBJECT, Mms.SUBJECT_CHARSET };
        
    private static final String NEW_INCOMING_MM_CONSTRAINT =
            "(" + Mms.MESSAGE_BOX + "=" + Mms.MESSAGE_BOX_INBOX
            + " AND " + Mms.SEEN + "=0"
            + " AND (" + Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND
            + " OR " + Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + "))";
    

    public static int mUnreadSmsCount = 0;
    public static int mUnreadMmsCount = 0;
    public static int mUnreadMsgCount = 0;
    public static String mUnreadSmsNum = null;
    public static String mUnreadMmsNum = null;

    public static String getFrom(Context context, Uri uri) {
        String msgId = uri.getLastPathSegment();
        Uri.Builder builder = Mms.CONTENT_URI.buildUpon();

        builder.appendPath(msgId).appendPath("addr");

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            builder.build(), new String[] {Addr.ADDRESS, Addr.CHARSET},
                            Addr.TYPE + "=" + PduHeaders.FROM, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String from = cursor.getString(0);

                    if (!TextUtils.isEmpty(from)) {
                        byte[] bytes = PduPersister.getBytes(from);
                        int charset = cursor.getInt(1);
                        return new EncodedStringValue(charset, bytes)
                                .getString();
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }
    
    private int getUnreadSmsCount(Context context) {
        mUnreadMsgCount = 0;
        mUnreadSmsCount = 0;
        mUnreadMmsCount = 0;
        mUnreadSmsNum = null;
        mUnreadMmsNum = null;

        /*Cursor smsCursor = context.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);

        if (smsCursor != null) {
            while (smsCursor.moveToNext()) {
                int type = smsCursor.getInt(smsCursor.getColumnIndex("read"));
                if (type == 0) {
                    unreadSmsCount++;
                }
            }
        } 
        smsCursor.close();

        return unreadSmsCount;*/

        Cursor Smscursor = SqliteWrapper.query(context, context.getContentResolver(), Sms.CONTENT_URI,
                            SMS_STATUS_PROJECTION, NEW_INCOMING_SM_CONSTRAINT,
                            null, Sms.DATE + " desc");      
        if(Smscursor != null)
        {
            mUnreadSmsCount = Smscursor.getCount();
            if(mUnreadSmsCount == 1)
            {
                if(Smscursor.moveToFirst())
                {
                    int recipientId = Smscursor.getColumnIndex(Sms.ADDRESS);
                    mUnreadSmsNum = Smscursor.getString(recipientId);
                }
            }
        }
        Smscursor.close();

        Cursor Mmscursor = SqliteWrapper.query(context, context.getContentResolver(), Mms.CONTENT_URI,
                            MMS_STATUS_PROJECTION, NEW_INCOMING_MM_CONSTRAINT,
                            null, Mms.DATE + " desc");
        if(Mmscursor != null)
        {
            mUnreadMmsCount = Mmscursor.getCount();
            if(mUnreadMmsCount == 1)
            {
                if(Mmscursor.moveToFirst())
                {
                    long msgId = Mmscursor.getColumnIndex(Mms._ID);
                    Uri msgUri = Mms.CONTENT_URI.buildUpon().appendPath(
                            Long.toString(msgId)).build();
                    mUnreadMmsNum = getFrom(context, msgUri);
                }
            }
        }
        Mmscursor.close();

        mUnreadMsgCount = mUnreadSmsCount + mUnreadMmsCount;
        return mUnreadMsgCount;

    }
    
    private void refreshSmsDisplay() {
        int smscount = getUnreadSmsCount(mContext);
        log("refreshSmsDisplay smscount = "+smscount); 
        if (smscount == 0) {
            mLewaSmsSelector.setVisibility(View.GONE);
        } else {
            if (smscount > 1) {
                mNewMsg.setText(mContext.getString(R.string.lock_screen_new_msgs, smscount));
            } else {
                mNewMsg.setText(mContext.getString(R.string.lock_screen_new_msg, smscount));
            }
            mLewaSmsSelector.setVisibility(View.VISIBLE);
        }
    }
    
    private void refreshCallDisplay() {
        int callcount = getMissedCallCount(mContext);
        log("refreshCallDisplay callcount = "+callcount);         
        if (callcount == 0) {
            mLewaCallSelector.setVisibility(View.GONE);
        } else {
            if (callcount > 1) {
                mMissCall.setText(mContext.getString(R.string.lock_screen_miss_calls, callcount));
            } else {
                mMissCall.setText(mContext.getString(R.string.lock_screen_miss_call, callcount));
            }
            mLewaCallSelector.setVisibility(View.VISIBLE);
        }
    }
    
    private MissedCallContentObserver mMissedCallContentObserver = new MissedCallContentObserver(mContext, new Handler());
    
    class MissedCallContentObserver extends ContentObserver{

        @Override
        public boolean deliverSelfNotifications() {
            // TODO Auto-generated method stub
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub
            log("MissedCallContentObserver change!!!"); 
            refreshCallDisplay();
        }

        public MissedCallContentObserver(Context context, Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }
        
    }
    
    private NewSmsContentObserver mNewSmsContentObserver = new NewSmsContentObserver(mContext, new Handler());
    
    class NewSmsContentObserver extends ContentObserver{

        @Override
        public boolean deliverSelfNotifications() {
            // TODO Auto-generated method stub
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub
            log("NewSmsContentObserver change!!!"); 
            refreshSmsDisplay();
        }

        public NewSmsContentObserver(Context context, Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }
        
    }
    
    public static void setUnlocklineVisible(int visible) {
        unlock_line.setVisibility(visible);
    }
    
    public static void setUnlocktipsText(int resid) {
        unlock_tips.setText(resid);
    }
     
    public static void setUnlocktipsVisible(int visible) {
        unlock_tips.setVisibility(visible);
    }
    
    public static void setUnlockprogressbgVisible(int visible) {
        unlock_progress_bg.setVisibility(visible);
    }
    
    public static void startAnimationDrawable() {
        unlock_bg.setImageDrawable(mbgAnimationDrawable);
        mbgAnimationDrawable.stop();
        mbgAnimationDrawable.start();
    }
    
    public static void stopAnimationDrawable() {
        unlock_bg.setImageDrawable(null);
        mbgAnimationDrawable.stop();
    }
    
    public static void startprogressbgAnimationDrawable() {
        //
        //mHandler.removeCallbacks(mstartprogressbgRunnable);
        setUnlockprogressbgVisible(View.VISIBLE);
        setDateTimeBG(R.drawable.charging_bg);
        mprogressbgAnimationDrawable.stop();
        mprogressbgAnimationDrawable.start();
        //mHandler.postDelayed(mprogressbgEndRunnable, mprogressbgAnimationDrawable.getDuration(1)*mprogressbgAnimationDrawable.getNumberOfFrames());
        
    }
    
    public static void stopprogressbgAnimationDrawable() {
        mprogressbgAnimationDrawable.stop();
        setUnlockprogressbgVisible(View.GONE);
        setDateTimeBG(R.drawable.unlock_datetime_bg);
    }
    
    public static void setDateTimeBG(int resid) {
        time_date.setBackgroundResource(resid);
    }
    
    public static void onprogressbgAnimationDrawableEnd() {
        mHandler.removeCallbacks(mprogressbgEndRunnable);
        startAnimationDrawable();
        mHandler.postDelayed(mstartprogressbgRunnable, mbgAnimationDrawable.getDuration(1)*mbgAnimationDrawable.getNumberOfFrames());
    }
    
    public static void stopAllChargeAnimationDrawable() {
        stopAnimationDrawable();
        stopprogressbgAnimationDrawable();
        mHandler.removeCallbacks(mstartprogressbgRunnable);
        mHandler.removeCallbacks(mprogressbgEndRunnable);
    }
    
    private void setprogressbgbylevel(int level) {
        if (level < 5) {
            unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg);          
        } else if (level < 16) {
            unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_1); 
        } else if (level < 26) {
            unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_2); 
        } else if (level < 39) {
            unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_3); 
        } else if (level < 52) {
            unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_4); 
        } else if (level < 65) {
            unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_5); 
       } else if (level < 78) {
           unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_6); 
       } else if (level < 91) {
           unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_7); 
       } else {
           unlock_progress_bg.setBackgroundResource(R.drawable.charging_bg_8);  
       }
    }

    private Drawable bitmap2drawable(int id){
        return new BitmapDrawable(getResources(), BitmapFactory.decodeStream(getResources().openRawResource(id),null,getBitmapOptions()));
    }
    
    private Options getBitmapOptions(){
        BitmapFactory.Options options=new BitmapFactory.Options(); 
        options.inJustDecodeBounds = false;
        options.inPurgeable=true;
        options.inInputShareable = true;
        options.inDither = false;
        options.inSampleSize = 2;
        
        return options;
    }
    
    //[Begin fulianwu, for lockscreen alarm,2011_10_19,add]
    
    private BroadcastReceiver alarmNotifyReceiver = new BroadcastReceiver(){
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("com.android.deskclock.ALARM_ALERT")){

                int lockscreenDisableOnSecurityValue = Settings.System.getInt(context.getContentResolver(), 
                    Settings.System.LOCKSCREEN_DISABLE_ON_SECURITY, 0);
                if(lockscreenDisableOnSecurityValue == 1){
                    return;
                }
                
                KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                
                if(km.inKeyguardRestrictedInputMode()){
                    int alarm_id = intent.getIntExtra("alarm_id", -1);
                    
                    if(alarm_id != -1){
                        mLewaRotarySelector.setAlarmId(alarm_id);
                    }
                    alarmHandler.sendEmptyMessage(ALARM_NOTIFATION_RECIVER);
                }
            }else if(action.equals("com.lewa.action.AlarmLockScreen")){
                NotificationManager nm = getNotificationManager(context);
                nm.cancel(mLewaRotarySelector.getAlarmId());
            }
            
            
        }
    };

    public Handler alarmHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
            case ALARM_NOTIFATION_RECIVER:
            {
        
                Intent alarmIntent = new Intent();
                alarmIntent.setAction("com.lewa.action.AlarmLockScreen");
               
                PendingIntent pending = PendingIntent.getBroadcast(context, mLewaRotarySelector.getAlarmId(), alarmIntent, 0);
                Notification n = new Notification(R.drawable.stat_notify_alarm,
                        "", 30);
                n.setLatestEventInfo(context, "",
                        "",
                        pending);
                n.flags |= Notification.FLAG_SHOW_LIGHTS
                        | Notification.FLAG_ONGOING_EVENT;
                n.defaults |= Notification.DEFAULT_LIGHTS;
                NotificationManager nm = getNotificationManager(context);
                nm.notify(mLewaRotarySelector.getAlarmId(), n);
                
                view_flipper = (ViewFlipper)LewaLockScreen_zb.this.findViewById(R.id.view_flipper);
                alarm_notify = (RelativeLayout)LewaLockScreen_zb.this.findViewById(R.id.alarm_notify);
                
                if(view_flipper != null && alarm_notify != null){
                    view_flipper.setDisplayedChild(1);
                    alarm_notify.setVisibility(View.VISIBLE);

                    alarm_time = (TextView)LewaLockScreen_zb.this.findViewById(R.id.alarm_time);
                    alarm_am_pm = (TextView)LewaLockScreen_zb.this.findViewById(R.id.am_pm);
                    alarm = (ImageView)LewaLockScreen_zb.this.findViewById(R.id.alarm);
                    Animation animation = AnimationUtils.loadAnimation(context, R.anim.lock_screen_alarm_shake);
                    alarm.startAnimation(animation);
                    
                    setAlarmTime();
                    
                     mLewaRotarySelector.setIsAlarmNotify(true);
                     mLewaRotarySelector.setViewFlipper(view_flipper);                    
                    
                }else{
                    Log.e(TAG, "Alarm Notify error");
                }
                 break;
            }
            default:
                break;
            }
        }
        
    };

    private String time_is_12_24() {

        return Settings.System.getString(context.getContentResolver(),
                Settings.System.TIME_12_24);
    }

    private String time_format(Date now,boolean time_is_12){
        SimpleDateFormat sdf = null;
        if(time_is_12){
            sdf = new SimpleDateFormat("h:mm");
        }else{
            sdf = new SimpleDateFormat("H:mm");
        }
        return sdf.format(now);
    }

    private void setAlarmTime(){
        Date now = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        String time_is_12_24 = time_is_12_24();
        if("24".equals(time_is_12_24)){
            alarm_am_pm.setText("");
            alarm_time.setText(time_format(now,false));
        }else{
            if(hour>=12){
                alarm_am_pm.setText(context.getString(R.string.lock_screen_pm));
            }else{
                alarm_am_pm.setText(context.getString(R.string.lock_screen_am));
            }
            alarm_time.setText(time_format(now,true));
        }
    }
    
    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    private ViewFlipper view_flipper;
    private RelativeLayout alarm_notify;
    private boolean IsAlarmNotify = false;
    private Context context;
    private TextView alarm_time;
    private TextView alarm_am_pm;
    private ImageView alarm;
    public static final int ALARM_NOTIFATION_RECIVER = 0;
    
    //[End]
}


