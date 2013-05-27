package com.android.systemui.statusbar;

import com.android.systemui.R;

import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.provider.Telephony;
import android.util.Log;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.media.RingtoneManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class DataMonitorService extends Service{
    
    
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private Handler mHandler;
    private Runnable mRunnable;

    private long initData;
    private long usedData;
    private long todayuseddata;
    private long monthuseddata;

    private long wlaninitData;
    private long wlanusedData;
    private long wlantodaydata;
    private long wlanmonthdata;

    private int mYear;
    private int mMonth;
    private int mDay;

    public static final String PREFERENCE_NAME = "datamonitorfile";
    public static final String FIRST_USE = "first_use";
    public static final String MONTH_TOTAL = "month_total";
    public static final String MONTH_USED = "month_used";
    public static final String MONTH_LEFT = "month_left";
    public static final String TODAY_TOTAL = "today_total";
    public static final String TODAY_USED = "today_used";    
    public static final String REMIND_DATA = "remind_data";
    public static final String PRE_YEAR = "pre_year";
    public static final String PRE_MONTH = "pre_month";
    public static final String PRE_DAY = "pre_day";
    public static final String QUERY_CODE = "query_code";
    public static final String QUERY_NUMBER = "query_number";
    public static final String DEFAULT_QUERY_CODE = "default_query_code";
    public static final String DEFAULT_QUERY_NUMBER = "default_query_number";
    public static final String REMIND_ME = "reminder_me";
    public static final String START_DATE = "start_date";
    public static final String INSTALL_DATE = "install_date";
    public static final String BE_ACCOUNTED = "be_accounted";
    public static final String INSTALL_DATE_ACCOUNTED = "install_date_accounted";
    public static final String WLAN_TODAY = "wlan_today";
    public static final String WLAN_MONTH = "wlan_month";
    public static final String TIPS_SHOWN = "tips_shown";

    public static final int  NEGATIVE_MODE = 0;
    public static final int  POSITIVE_MODE  = 1;
	
    public static final long  NEGATIVE_INTERVAL_MILLIS = 60*1000;
    public static final long  POSITIVE_INTERVAL_MILLIS = 5*1000;
    private static final long GB_UNIT = 1024*1024*1024;
    private static final long MB_UNIT = 1024*1024;
    private static final long KB_UNIT = 1024;    
    private static final String TAG = "DataMonitorService";
    private static java.text.NumberFormat  formater;
    public static boolean mRemind = false;
    public static boolean mCloseMobileDataDialogOpened = false;
    
    static final boolean LOGC = false; 

     // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    private int mDataMonitorMode = NEGATIVE_MODE ;
    private boolean mNetworkIsConnected = false;

    private TelephonyManager mTelephonyMgr = null;


     public class LocalBinder extends Binder {
        DataMonitorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DataMonitorService.this;
        }
    }

    
    private void updateTodayLeftData(long todaydata, long leftdata, long monthused) {    
        long monthtotal = mSharedPreferences.getLong(MONTH_TOTAL, 0);
        String today_UNIT = "K";
        String left_UNIT = "K";
        String monthused_UNIT = "K";
        float today = Float.valueOf(todaydata+"")/KB_UNIT;
        float left = Float.valueOf(leftdata+"")/KB_UNIT;
        float month = Float.valueOf(monthused+"")/KB_UNIT;

        if (LOGC) {
            Log.i(TAG, "updateTodayLeftData today_str = " + today);
            Log.i(TAG, "updateTodayLeftData left_str = " + left);
            Log.i(TAG, "updateTodayLeftData month_str = " + month);
        }

        if (today >= KB_UNIT) {
            today = Float.valueOf(todaydata+"")/MB_UNIT;
            today_UNIT = "M";
        }
        if (today >= KB_UNIT) {
            today = Float.valueOf(todaydata+"")/GB_UNIT;
            today_UNIT = "G";
        }

        if (left >= KB_UNIT) {
            left = Float.valueOf(leftdata+"")/MB_UNIT;
            left_UNIT = "M";
        }
        if (left >= KB_UNIT) {
            left = Float.valueOf(leftdata+"")/GB_UNIT;
            left_UNIT = "G";
        }

        if (month >= KB_UNIT) {
            month = Float.valueOf(monthused+"")/MB_UNIT;
            monthused_UNIT = "M";
        }
        if (month >= KB_UNIT) {
            month = Float.valueOf(monthused+"")/GB_UNIT;
            monthused_UNIT = "G";
        }

        if(formater == null) {
            formater  =  java.text.DecimalFormat.getInstance();  
            formater.setMaximumFractionDigits(2);  
            formater.setMinimumFractionDigits(2);  
        }

        String today_str = formater.format(today);
        String left_str = formater.format(left);
        String month_str = formater.format(month);

        if (LOGC) {
            Log.i(TAG, "updateTodayLeftData today_str = " + today_str);
            Log.i(TAG, "updateTodayLeftData left_str = " + left_str);
            Log.i(TAG, "updateTodayLeftData month_str = " + month_str);
        }

        /*if (DataMonitorActivity.mHasFocus) {    
        DataMonitorActivity.updateTodayLeftData(month_str, monthused_UNIT, today_str, today_UNIT, left_str, left_UNIT, monthtotal, monthuseddata, todayuseddata);
        }*/
        if (DataActivity.mHasFocus) {    
            DataActivity.updateTodayLeftData(month_str, monthused_UNIT, today_str, today_UNIT, left_str, left_UNIT, monthtotal, monthuseddata, todayuseddata);
        }
        StatusBarService.updateTodayLeftData(mContext, month_str, monthused_UNIT, today_str, today_UNIT, left_str, left_UNIT, monthtotal, monthuseddata, todayuseddata);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

     public void setPositiveDataMonitorMode(boolean bPositiveMode) {
	    if (LOGC) {
            Log.i(TAG, "setPositiveDataMonitorMode =  " + bPositiveMode);
        }
    
	 if(bPositiveMode) {
		mHandler.removeCallbacks(mRunnable);
		mHandler.post(mRunnable);
	 	mDataMonitorMode = mDataMonitorMode + 1;
					
	 }
	 else {
	 	mDataMonitorMode = mDataMonitorMode - 1;
	 }
        return;
    }


    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mContext = this;

        mHandler = new Handler();
        mSharedPreferences = mContext.getSharedPreferences(PREFERENCE_NAME, Activity.MODE_PRIVATE);    
        updateDate();
        if (!mSharedPreferences.getBoolean(FIRST_USE, false)) {    
            initPreferenceFile();    
        } 
          
        mRunnable = new Runnable() {    
            @Override
            public void run() {
                // TODO Auto-generated method stub
                long nowdata = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
                usedData = nowdata - initData;    
                if (usedData < 0) {
                    usedData = 0;
                }
                long wlannowdata = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes() - nowdata;
                wlanusedData = wlannowdata - wlaninitData;    
                if (wlanusedData < 0) {
                    wlanusedData = 0;
                }
                if (LOGC) {
                    Log.i(TAG, "run initData = " + initData);
                    Log.i(TAG, "run nowdata = " + nowdata);
                    Log.i(TAG, "run usedData = " + usedData);
                }

                todayuseddata = mSharedPreferences.getLong(TODAY_USED, 0);
                monthuseddata = mSharedPreferences.getLong(MONTH_USED, 0);    
                wlantodaydata= mSharedPreferences.getLong(WLAN_TODAY, 0);
                wlanmonthdata= mSharedPreferences.getLong(WLAN_MONTH, 0);
                if (LOGC) {
                    Log.i(TAG, "run pre todayuseddata = " + todayuseddata);
                    Log.i(TAG, "run pre monthuseddata = " + monthuseddata);
                }

                todayuseddata += usedData;
                monthuseddata += usedData;

                wlantodaydata += wlanusedData;
                wlanmonthdata += wlanusedData;

                if (LOGC) {
                    Log.i(TAG, "run todayuseddata = " + todayuseddata);
                    Log.i(TAG, "run monthuseddata = " + monthuseddata);
                }    

                initData = nowdata;
                wlaninitData = wlannowdata;
                onDayChangedDeal();
                onDayUnchangedDeal();
                FirewallActivity.saveUidData(DataMonitorService.this, getInstalledApplications());
		  if(mDataMonitorMode >= POSITIVE_MODE && mNetworkIsConnected) {
                	mHandler.postDelayed(mRunnable, POSITIVE_INTERVAL_MILLIS);
		  }
		  else {
		  	mHandler.postDelayed(mRunnable,NEGATIVE_INTERVAL_MILLIS );
		  }
            }
        };
    
        IntentFilter filter = new IntentFilter();
        filter.addAction(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION);
        registerReceiver(mIntentReceiver, filter);
        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        registerReceiver(mIntentReceiver, filter);

	 mTelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	 mTelephonyMgr.listen(new PhoneStateListener() {
		@Override
		public void onDataConnectionStateChanged(int state) {
		 if (LOGC) {
    	        Log.i(TAG, "onDataConnectionStateChanged = " + state);
        	}   
			switch(state){
			     case TelephonyManager.DATA_DISCONNECTED:
				 	mNetworkIsConnected = false;
				break;
			     case TelephonyManager.DATA_CONNECTING:
				break;
			      case TelephonyManager.DATA_CONNECTED:
				   mNetworkIsConnected = true;
				   if(mDataMonitorMode >= POSITIVE_MODE) {
				   	mHandler.removeCallbacks(mRunnable);
					mHandler.post(mRunnable);
				   }
				break;
			}
		}
     },PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

     	 initUserData();
      	mHandler.post(mRunnable);
        
        onDayChangedDeal();
		
    }
    
    private List<ApplicationInfo> installed = null;
    private List<ApplicationInfo> getInstalledApplications(){
        if(installed == null)
            installed = getPackageManager().getInstalledApplications(0);
        return installed;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        FirewallActivity.saveUidData(this, getInstalledApplications());
        
        mHandler.removeCallbacks(mRunnable);
        mContext.unregisterReceiver(mIntentReceiver);
        onDayChangedDeal();
        onDayUnchangedDeal();
    
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);    
        
        //this will be delete
        //initPreferenceFile();
        //for test

    }


    private void initUserData() {
	 todayuseddata = mSharedPreferences.getLong(TODAY_USED, 0);
        monthuseddata = mSharedPreferences.getLong(MONTH_USED, 0);
        mRemind = false;
        mCloseMobileDataDialogOpened = false;

        wlantodaydata = mSharedPreferences.getLong(WLAN_TODAY, 0);
        wlanmonthdata = mSharedPreferences.getLong(WLAN_MONTH, 0);
                
        if (LOGC) {
            Log.i(TAG, "onStart todayuseddata = " + todayuseddata);
            Log.i(TAG, "onStart monthuseddata = " + monthuseddata);
        }
        
        initData = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
        wlaninitData = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes() - initData;
            
        if (LOGC) {
            Log.i(TAG, "onStart initData = " + initData);
        }    

    }
    
    private void initPreferenceFile() {    
        if (LOGC) {
            Log.i(TAG, "initPreferenceFile");
        } 

        updateDate();    
        SharedPreferences.Editor editor = mSharedPreferences.edit();    
        editor.putBoolean(FIRST_USE, true);
        editor.putLong(MONTH_TOTAL, (30 * MB_UNIT));    
        editor.putLong(MONTH_USED, 0);
        editor.putLong(MONTH_LEFT, 0); 
        editor.putLong(TODAY_TOTAL, 0);
        editor.putLong(TODAY_USED, 0);
        editor.putLong(WLAN_TODAY, 0);
        editor.putLong(WLAN_MONTH, 0);
        editor.putLong(REMIND_DATA, (5 * MB_UNIT));
        editor.putInt(PRE_YEAR, mYear);
        editor.putInt(PRE_MONTH, mMonth);
        editor.putInt(PRE_DAY, mDay);    
        //editor.putInt(QUERY_NUMBER, 10086);
        editor.putInt(DEFAULT_QUERY_NUMBER, 10086);    
        //editor.putString(QUERY_CODE, "CXLL");
        editor.putString(DEFAULT_QUERY_CODE, "CXLL");
        editor.putBoolean(REMIND_ME, false);
        editor.putInt(START_DATE, 1);
        editor.putInt(INSTALL_DATE, mDay);
        editor.putBoolean(BE_ACCOUNTED, false);
        editor.putBoolean(INSTALL_DATE_ACCOUNTED, false);
        editor.putBoolean(TIPS_SHOWN, false);
        editor.commit();
    }
    
    private void updateDate() {
        final Calendar calendar = Calendar.getInstance();
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH) + 1;
        mDay = calendar.get(Calendar.DAY_OF_MONTH);    

        if (LOGC) {
            Log.i(TAG, "updateDate mYear = " + mYear);
            Log.i(TAG, "updateDate mMonth = " + mMonth);
            Log.i(TAG, "updateDate mDay = " + mDay);
        }
    }
    
    private void onDayChangedDeal() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();    
        if (checkYearChanged()) {
            if (LOGC) {
                Log.i(TAG, "onDayChangedDeal Year Changed");
            }    
            mRemind = false;
            todayuseddata = 0;
            editor.putLong(TODAY_USED, 0);
            editor.putInt(PRE_YEAR, mYear);
            editor.putInt(PRE_MONTH, mMonth);
            editor.putInt(PRE_DAY, mDay);
            editor.putLong(WLAN_TODAY, 0);
            editor.putLong(WLAN_MONTH, 0);
            wlantodaydata = 0;
            wlanmonthdata = 0;
            if (checkAccountDay()) {
                if (LOGC) {
                    Log.i(TAG, "onDayChangedDeal Year Changed Account");
                }

                monthuseddata = 0;
                editor.putLong(MONTH_USED, 0);        
                editor.putLong(MONTH_LEFT, mSharedPreferences.getLong(MONTH_TOTAL, 0));
            }
        } else if (checkMonthChanged()) {
            if (LOGC) {
                Log.i(TAG, "onDayChangedDeal Month Changed");
            }

            mRemind = false;
            todayuseddata = 0;
            editor.putLong(TODAY_USED, 0);
            editor.putInt(PRE_MONTH, mMonth);
            editor.putInt(PRE_DAY, mDay);
            editor.putLong(WLAN_TODAY, 0);
            editor.putLong(WLAN_MONTH, 0);
            wlantodaydata = 0;
            wlanmonthdata = 0;
            if (checkAccountDay()) {
                if (LOGC) {
                    Log.i(TAG, "onDayChangedDeal Month Changed Account");
                }

                monthuseddata = 0;
                editor.putLong(MONTH_USED, 0);        
                editor.putLong(MONTH_LEFT, mSharedPreferences.getLong(MONTH_TOTAL, 0));
            }
        } else if (checkDayChanged()) {
            if (LOGC) {
                Log.i(TAG, "onDayChangedDeal Day Changed");
            }
            todayuseddata = 0;    
            mRemind = false;
            editor.putInt(PRE_DAY, mDay);
            editor.putLong(TODAY_USED, 0);
            editor.putLong(WLAN_TODAY, 0);
            wlantodaydata = 0;
            if (checkAccountDay()) {
                if (LOGC) {
                    Log.i(TAG, "onDayChangedDeal Day Changed Account");
                }

                monthuseddata = 0;
                editor.putLong(MONTH_USED, 0);
                editor.putLong(MONTH_LEFT, mSharedPreferences.getLong(MONTH_TOTAL, 0));
            }
        } else {

        }
        editor.commit();
    }
    
    private void onDayUnchangedDeal() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(TODAY_USED, todayuseddata);
        editor.putLong(MONTH_USED, monthuseddata);
        editor.putLong(WLAN_TODAY, wlantodaydata);
        editor.putLong(WLAN_MONTH, wlanmonthdata);
        long month_total = mSharedPreferences.getLong(MONTH_TOTAL, 0);
        long month_left = month_total - monthuseddata;
        if (LOGC) {
            Log.i(TAG, "onDayUnchangedDeal month_total = " + month_total);
            Log.i(TAG, "onDayUnchangedDeal monthuseddata = " + monthuseddata);
            Log.i(TAG, "onDayUnchangedDeal month_left = " + month_left);
        }
    
    
        if (month_total > 0 && month_left > 0) {
            editor.putLong(MONTH_LEFT, month_left);
        } else {
            month_left = 0;
            editor.putLong(MONTH_LEFT, 0);
        }    
        editor.commit();    
        updateTodayLeftData(todayuseddata, month_left, monthuseddata);    
    
        //remind me
        if (!mRemind) {    
            if (LOGC) {
                Log.i(TAG, "onDayUnchangedDeal need remind!");
            }
            long remind_data = mSharedPreferences.getLong(REMIND_DATA, 0);
            boolean remind_me = mSharedPreferences.getBoolean(REMIND_ME, false);

            if (LOGC) {
                Log.i(TAG, "onDayUnchangedDeal remind_data= " + remind_data);
                Log.i(TAG, "onDayUnchangedDeal month_left= " + month_left);
                Log.i(TAG, "onDayUnchangedDeal remind_me= " + remind_me);
            }

            if ((remind_data > 0) && remind_me) {
                if (month_left <= remind_data) {
                    if (LOGC) {
                        Log.i(TAG, "onDayUnchangedDeal remind_me notification");
                    }

                    mRemind = true;
                    NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    Notification notification = new Notification(R.drawable.monitor_remind, getString(R.string.remind_notify_title), System.currentTimeMillis());
                    notification.flags|=Notification.FLAG_AUTO_CANCEL;
                    notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DataMonitorService.class), PendingIntent.FLAG_ONE_SHOT);        
                    notification.setLatestEventInfo(this, getString(R.string.remind_notify_title), getString(R.string.remind_notify, remind_data/MB_UNIT), contentIntent);     
                    notificationManager.notify(R.string.reminder_me, notification); 
                }
            }           
        }
        
        if (month_left > 0) {
            mCloseMobileDataDialogOpened = false;
        } else {
            if (!mCloseMobileDataDialogOpened) {
                if (getDataState(mContext)) {
                    mCloseMobileDataDialogOpened = true;
                    showCloseMobileDataDialog();
                }                    
            }                
        }

    }

    private static boolean getDataState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            /* Make sure the state change propagates */
            Thread.sleep(100);
        } catch (java.lang.InterruptedException ie) {
        }
        return cm.getMobileDataEnabled();
    }

    private void disableMobileData(Context context) {
        boolean enabled = getDataState(context);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (enabled) {
            cm.setMobileDataEnabled(false);
        }
    }
    
    private void showCloseMobileDataDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle(R.string.data_reminder_title)
                                    .setMessage(R.string.data_reminder_message)
                                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            disableMobileData(mContext);
                                        }
                                    })
                                    .setNegativeButton(com.android.internal.R.string.no, null)
                                    .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        dialog.show();
    }
    
    private boolean checkYearChanged() {
        updateDate();
        int pre_year = mSharedPreferences.getInt(PRE_YEAR, -1);
        return (mYear == pre_year) ? false:true;
    }
    
    private boolean checkMonthChanged() {
        updateDate();    
        int pre_month = mSharedPreferences.getInt(PRE_MONTH, -1);    
        return (mMonth == pre_month) ? false:true;
    }
    
    private boolean checkDayChanged() {
        updateDate();
        int pre_day = mSharedPreferences.getInt(PRE_DAY, -1);
        return (mDay == pre_day) ? false:true;
    }
    
    private boolean checkAccountDay() {
        updateDate();
        int start_date = mSharedPreferences.getInt(START_DATE, 1);    
        boolean install_date_accounted = mSharedPreferences.getBoolean(INSTALL_DATE_ACCOUNTED, false);
        int install_date = mSharedPreferences.getInt(INSTALL_DATE, 1);
        int month_days = getMonthDays(mYear, mMonth);
        start_date = (start_date > month_days) ? month_days : start_date;
        
        if (!install_date_accounted) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(INSTALL_DATE_ACCOUNTED, true);
            if (install_date >= start_date) {
                editor.putBoolean(BE_ACCOUNTED, true);
            }
            editor.commit();
        }
        
        boolean accounted = mSharedPreferences.getBoolean(BE_ACCOUNTED, false);
        if (LOGC) {
            Log.i(TAG, "checkAccountDay accounted= " + accounted);
            Log.i(TAG, "checkAccountDay start_date= " + start_date);
            Log.i(TAG, "checkAccountDay install_date= " + install_date);
            Log.i(TAG, "checkAccountDay mDay= " + mDay);
        } 
        
        
        if (mDay <= start_date) {
            if (accounted) {
                accounted = false;
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(BE_ACCOUNTED, false);
                editor.commit();
            }
        }
        
        if (mDay >= start_date) {
            if (!accounted) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(BE_ACCOUNTED, true);
                editor.commit();
                return true;
            } else {
                return false;
            }    
        } 
        return false;
    }
    
    private Calendar setCalendarDate(Calendar calendar, int year, int month, int day) {
        int monthdays = getMonthDays(year, month);
        if (day > monthdays) {
            calendar.set(year, month, monthdays);
        } else {
            calendar.set(year, month, day);
        }
        return calendar;
    }
    
    private int getMonthDays(int year, int month) {
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                {
                    return 31;
                }
            case 4:
            case 6:
            case 9:
            case 11:
                {
                    return 30;
                }
            case 2:
                if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0)) {
                    return 29;
                } else {
                    return 28;
                }
            default:
                break;
        }
        return 0;
    }
    
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Telephony.Intents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                updateNetworkName(intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                        intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                        intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
            } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                installed = null;
            }
        }
    };
    
    private void updateNetworkName(boolean showSpn, String telephonySpn, boolean showPlmn, String telephonyPlmn) {
        if (LOGC) {
            Log.e("CarrierLabel", "zhangbo updateNetworkName showSpn=" + showSpn + " telephonySpn=" + telephonySpn
                + " showPlmn=" + showPlmn + " telephonyPlmn=" + telephonyPlmn);
        }
        String spn = getCarrierString(telephonyPlmn, telephonySpn);
        if (spn != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            String query_code = mSharedPreferences.getString(DataMonitorService.QUERY_CODE, "");
            int query_number = mSharedPreferences.getInt(DataMonitorService.QUERY_NUMBER, 0);
            if (LOGC) {
                Log.e("CarrierLabel", "zhangbo query_code = "+query_code+" query_number = "+query_number);
            }
            String china_mobile;
            String china_unicom;
            String china_telecom;
            try {
                china_mobile = getString(R.string.china_mobile);
                china_unicom = getString(R.string.china_unicom);
                china_telecom = getString(R.string.china_telecom);
            } catch (Resources.NotFoundException e) {
            // TODO: handle exception
                china_mobile = "CHINA MOBILE";
                china_unicom = "CHN-UNICOM";
                china_telecom = "China Telecom";
            } 
            if (spn.equalsIgnoreCase("CHINA MOBILE") || spn.equalsIgnoreCase("CMCC") || spn.equalsIgnoreCase("China Mobile CMCC") || spn.contains(china_mobile)) {
                if (LOGC) {  
                    Log.e("CarrierLabel", "zhangbo CMCC");
                }
                if (query_number == 0) {
                    editor.putInt(QUERY_NUMBER, 10086);
                }
                if (query_code.equals("")) {
                    editor.putString(QUERY_CODE, "CXLL");
                }
                editor.putInt(DEFAULT_QUERY_NUMBER, 10086);                     
                editor.putString(DEFAULT_QUERY_CODE, "CXLL"); 
            } else if (spn.equalsIgnoreCase("CHN-UNICOM") || spn.equalsIgnoreCase("China Unicom") || spn.equalsIgnoreCase("CHN-CUGSM") || spn.contains(china_unicom)) {
                if (LOGC) {
                    Log.e("CarrierLabel", "zhangbo China Unicom");
                }
                if (query_number == 0) {
                    editor.putInt(QUERY_NUMBER, 10010);
                }
                if (query_code.equals("")) {
                    editor.putString(QUERY_CODE, "CXLL");
                }
                editor.putInt(DEFAULT_QUERY_NUMBER, 10010);     
                editor.putString(DEFAULT_QUERY_CODE, "CXLL"); 
            } else if (spn.equalsIgnoreCase("China Telecom") || spn.contains(china_telecom)) {
                if (LOGC) {
                    Log.e("CarrierLabel", "zhangbo China Telecom");
                }
                if (query_number == 0) {
                    editor.putInt(QUERY_NUMBER, 10001);
                }
                if (query_code.equals("")) {
                    editor.putString(QUERY_CODE, "108");
                }
                editor.putInt(DEFAULT_QUERY_NUMBER, 10001);     
                editor.putString(DEFAULT_QUERY_CODE, "108"); 
            } else {

            }
            editor.commit();
        }
    }

    static String getCarrierString(String telephonyPlmn, String telephonySpn) {
         //modified by zhangbo for Carrier display when language switch
        if (telephonyPlmn != null && (telephonySpn == null || "".contentEquals(telephonySpn))) {
            return telephonyPlmn;
        } else if (telephonySpn != null && (telephonyPlmn == null || "".contentEquals(telephonyPlmn))) {
            return telephonySpn;
        } else if (telephonyPlmn != null && telephonySpn != null) {
            return telephonySpn;
        } else {
            return "";
        }
    }
}
