package com.android.internal.policy.impl;

import com.android.internal.policy.impl.lewa.lockscreen.LewaLockScreenView;
import com.android.internal.policy.impl.lewa.lockscreen.LockScreenElementFactory;
import com.android.internal.policy.impl.lewa.lockscreen.LockScreenResourceLoader;
import com.android.internal.policy.impl.lewa.lockscreen.LockScreenRoot.UnlockerCallback;
import com.android.internal.policy.impl.lewa.lockscreen.UnlockerListener;
import com.android.internal.policy.impl.lewa.lockscreen.LockScreenRoot;
import com.android.internal.policy.impl.lewa.lockscreen.UnlockerScreenElement;
import com.android.internal.policy.impl.lewa.lockscreen.LockScreenConstants;

import com.android.internal.policy.impl.lewa.view.Expression;
import com.android.internal.policy.impl.lewa.view.ResourceManager;
import com.android.internal.policy.impl.lewa.view.ScreenContext;


import com.android.internal.widget.LockPatternUtils;

import android.database.sqlite.SqliteWrapper;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Settings;

import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.EncodedStringValue;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.BatteryManager;
import android.net.Uri;

import android.view.WindowManager;

import android.telephony.TelephonyManager;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.text.TextUtils;
import android.util.Log;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.PowerManager;

public class LewaLockScreen extends FrameLayout implements KeyguardScreen, 
        KeyguardUpdateMonitor.InfoCallback, KeyguardUpdateMonitor.SimStateCallback, LockScreenRoot.UnlockerCallback, UnlockerListener{

    private static final boolean DBG = false;
    private static final String TAG = "LewaLockScreen";
    private boolean isPaused;
    private boolean mBackDown;
    private KeyguardScreenCallback mCallback;
    private LockPatternUtils mLockPatternUtils;
    private ScreenContext screenContext;
    private LewaLockScreenView mLockscreenView;
    private boolean mMenuDown;
    private boolean mBackUnlockCamera;
    private LockScreenRoot mRoot;
    private KeyguardUpdateMonitor mUpdateMonitor;
    
    private Context mContext;

    public static int lockscreenChanged = 0;

    LewaLockScreen(Context context, Configuration configuration, LockPatternUtils lockpatternutils, KeyguardUpdateMonitor keyguardupdatemonitor, KeyguardScreenCallback keyguardscreencallback){
        
        super(context);
        
        isPaused = false;
        mLockPatternUtils = lockpatternutils;
        mUpdateMonitor = keyguardupdatemonitor;
        mCallback = keyguardscreencallback;
        
        setFocusable(true);
        setFocusableInTouchMode(true);
        mUpdateMonitor.registerInfoCallback(this);
        mUpdateMonitor.registerSimStateCallback(this);
        
        mContext = context;

        final ContentResolver resolver = mContext.getContentResolver();
        lockscreenChanged = (Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CHANGED, LockScreenConstants.LOCKSCREEN_UNCHANGED));
        if(lockscreenChanged == LockScreenConstants.LOCKSCREEN_CHANGED){
            /**
            *清除缓存
            */
            ResourceManager.clearCache();
            /**
            *更新数据
            */
            Settings.System.putInt(resolver,
                                   Settings.System.LOCKSCREEN_CHANGED, LockScreenConstants.LOCKSCREEN_UNCHANGED);
        }

        resolver.registerContentObserver(Calls.CONTENT_URI, true, mMissedCallContentObserver);
        resolver.registerContentObserver(Uri.parse("content://sms"), true, mNewSmsContentObserver);
        resolver.registerContentObserver(Uri.parse("content://mms"), true, mNewSmsContentObserver);
        resolver.registerContentObserver(Uri.parse("content://mms-sms/pdu_yl"), true, mNewSmsContentObserver);
        SettingObserver mSettingObserver = new SettingObserver(new Handler());
        mSettingObserver.observe();
        
        LockScreenResourceLoader lockscreenresourceloader = new LockScreenResourceLoader();
        LockScreenElementFactory lockscreenelementfactory = new LockScreenElementFactory(this, this);
        screenContext = new ScreenContext(mContext,lockscreenelementfactory);
       
        mRoot = new LockScreenRoot(screenContext, this);
        mRoot.load();
        
        LayoutParams layoutparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        

        
        mLockscreenView = new LewaLockScreenView(mContext, keyguardscreencallback, mRoot);
        addView(mLockscreenView, layoutparams);
		
        
        
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
    
    private static final String[] IM_STATUS_PROJECTION = new String[] {
        "_id", "person", "address","read", "body", "date" };

    private static final String NEW_INCOMING_IM_CONSTRAINT = "read = 0 and seen = 0";
    
    public static int mUnreadSmsCount = 0;
    public static int mUnreadMmsCount = 0;
    public static int mUnreadIMCount = 0;
    public static int mUnreadMsgCount = 0;
    public static String mUnreadSmsNum = null;
    //public static String mUnreadMmsNum = null;// only use mUnreadSmsNum
    //public static String mUnreadIMNum = null;

    public static String getFrom(Context context, Uri uri) {
        if(context == null){
            Log.e(TAG,"getFrom context == " + context);
        }
        if(uri == null){
            Log.e(TAG,"getFrom uri == " + uri);
        }
        String msgId = uri.getLastPathSegment();
        Uri.Builder builder = Mms.CONTENT_URI.buildUpon();
        if(builder == null){
            Log.e(TAG,"getFrom builder == " + builder);
        }
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
       // mUnreadMmsNum = null;
        if(context == null){
            Log.e(TAG,"getUnreadSmsCount context == " + context);
        }
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
            Smscursor.close();
        }
        
         Uri IMUri = Uri.parse("content://mms-sms/pdu_yl");
        Cursor IMcursor = SqliteWrapper.query(context, context.getContentResolver(), IMUri,
                            IM_STATUS_PROJECTION, NEW_INCOMING_IM_CONSTRAINT,
                            null, "date desc");
        if(IMcursor != null)
        {// IM have secode priority.
            mUnreadIMCount = IMcursor.getCount();
            if(mUnreadIMCount == 1 && mUnreadSmsNum == null)
            {
                if(IMcursor.moveToFirst())
                {
                    int index = IMcursor.getColumnIndex("address");
                    mUnreadSmsNum = IMcursor.getString(index);
                }
            }
            IMcursor.close();
        }
        
        Cursor Mmscursor = SqliteWrapper.query(context, context.getContentResolver(), Mms.CONTENT_URI,
                            MMS_STATUS_PROJECTION, NEW_INCOMING_MM_CONSTRAINT,
                            null, Mms.DATE + " desc");
        if(Mmscursor != null)
        {
            mUnreadMmsCount = Mmscursor.getCount();
            if(mUnreadMmsCount == 1 && mUnreadSmsNum == null)
            {
                if(Mmscursor.moveToFirst())
                {
                    long msgId = Mmscursor.getColumnIndex(Mms._ID);
                    Uri msgUri = Mms.CONTENT_URI.buildUpon().appendPath(
                            Long.toString(msgId)).build();
                    mUnreadSmsNum = getFrom(context, msgUri);
                }
            }
            Mmscursor.close();
        }
    
       
        
        mUnreadMsgCount = mUnreadSmsCount + mUnreadMmsCount + mUnreadIMCount;
        return mUnreadMsgCount;

    }

    private int getMissedCallCount(Context context) {
        int missedCallCount = 0;
        if(context == null){
            Log.e(TAG,"getMissedCallCount context == " + context);
        }
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
            callCursor.close();
        }
        

        return missedCallCount;
    }

    private MissedCallContentObserver mMissedCallContentObserver = new MissedCallContentObserver(mContext, new Handler());
    
    class MissedCallContentObserver extends ContentObserver{

        public MissedCallContentObserver(Context context, Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean deliverSelfNotifications() {
            // TODO Auto-generated method stub
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub

            if(mContext == null)
            {
                Log.e(TAG,"ERROR!! MissedCallContentObserver context == null");
                return;
            }
            Expression.put("call_missed_count", String.valueOf(getMissedCallCount(mContext)));    
        }
        
    }
    class SettingObserver extends ContentObserver{

        public SettingObserver(Handler handler) {
            super(handler);
        }
        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SHAKE_CLEAR_NOTIFICATIONS), false, this);
            onChange(true);
        }
        @Override
        public void onChange(boolean selfChange) {
            if(mContext != null) {
                mBackUnlockCamera = Settings.System.getInt(mContext.getContentResolver(), Settings.System.BACK_BTN_UNLOCK_CAMERA, 0) == 1;
            }
        }
    }
    
    private NewSmsContentObserver mNewSmsContentObserver = new NewSmsContentObserver(mContext, new Handler());
    
    class NewSmsContentObserver extends ContentObserver{

        public NewSmsContentObserver(Context context, Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }
        
        @Override
        public boolean deliverSelfNotifications() {
            // TODO Auto-generated method stub
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub
            Log.i(TAG, "===>.onChange:"+selfChange);

            if(mContext == null)
            {
                Log.e(TAG,"ERROR!! NewSmsContentObserver context == null");
                return;
            }
            Expression.put("sms_unread_count", String.valueOf(getUnreadSmsCount(mContext)));
        }
        
    }

    public void onResume(){
        if(mContext == null)
        {
            Log.e(TAG,"ERROR!! NewSmsContentObserver onResume context == null");
            return;
        }
        Expression.put("call_missed_count", String.valueOf(getMissedCallCount(mContext)));
        Expression.put("sms_unread_count", String.valueOf(getUnreadSmsCount(mContext)));
        Expression.put("state", String.valueOf(0));
        
        mLockscreenView.onResume();
        isPaused = false;
    }

    public void onPause(){
        if(mLockscreenView!=null){
        	mLockscreenView.onPause();
        }
        isPaused = true;
    }
    
    public void cleanUp(){

		if(mLockscreenView!=null){
        	mLockscreenView.cleanUp();
			removeView(mLockscreenView);
			mLockscreenView = null;
        
	        mUpdateMonitor.removeCallback(this);
	        mContext.getContentResolver().unregisterContentObserver(mMissedCallContentObserver);
	        mContext.getContentResolver().unregisterContentObserver(mNewSmsContentObserver);
	        mLockPatternUtils = null;
	        mCallback = null;
	        mUpdateMonitor = null;

	        mRoot = null;
	        mContext = null;
	        screenContext = null;
		}else{
			Log.e(TAG, "+++> system want to cleanUp, but was already cleanUp");
		}
        
       
    }
    
    public boolean dispatchKeyEvent(KeyEvent keyevent){
        
        int keyCode = keyevent.getKeyCode();
        if(keyevent.getAction() != KeyEvent.KEYCODE_UNKNOWN){
            
        }else {
            if(keyCode == KeyEvent.KEYCODE_BACK){
                mBackDown = true;
                if(mBackUnlockCamera && keyevent.isLongPress()){
                    Intent intent = new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    final Context context = getContext();
                    if(context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0){
                        unlocked(intent);
                    }
                }
            }else {
                mBackDown = false;
            }
        }
        return super.dispatchKeyEvent(keyevent);
    }
    
    public boolean isDisplayDesktop(){
        return mRoot.isDisplayDesktop();
    }
    
    public boolean needsInput(){
        
        return false;
    }
    
    protected void onAttachedToWindow(){
        super.onAttachedToWindow();
    }
    
    protected void onConfigurationChanged(Configuration configuration){
        
        super.onConfigurationChanged(configuration);
    }

    protected void onDetachedFromWindow(){
        super.onDetachedFromWindow();
    }
    
    public void onPhoneStateChanged(String s){
        //for save power in lockscreen interface if in calling
        if(s.equals(TelephonyManager.EXTRA_STATE_RINGING) || s.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
            onPause();
        }else{
            onResume();
        }
        
    }
    
    public void onRefreshBatteryInfo(boolean showingBatteryInfo, boolean pluggedIn, int batteryLevel){
        

    }
    
    public void onRefreshCarrierInfo(CharSequence charsequence, CharSequence charsequence1){
    
    }
    
    public void onRingerModeChanged(int i){
    
    }

    public void onSimStateChanged(com.android.internal.telephony.IccCard.State state){
    
    }

    public void onTimeChanged(){
    
    }

    public void onMusicChanged(){
    
    }

    /**public void pokeWakelock(int i){
        mCallback.pokeWakelock(i);
    }*/

    public void pokeWakelock(){
        mCallback.pokeWakelock();
    }

    public void unlocked(Intent intent){
        if(intent != null){

            mCallback.setPendingIntent(intent);
        }else{
            Log.e(TAG," intent is null");
        }
        mCallback.goToUnlockScreen();
        
    }
    
    @Override
    public void endUnlockMoving(UnlockerScreenElement unlockerscreenelement) {
        if(mRoot != null){
            mRoot.endUnlockMoving(unlockerscreenelement);
        }
    }

    @Override
    public void startUnlockMoving(UnlockerScreenElement unlockerscreenelement) {
        if(mRoot != null){
            mRoot.startUnlockMoving(unlockerscreenelement);
        }
    }
}
