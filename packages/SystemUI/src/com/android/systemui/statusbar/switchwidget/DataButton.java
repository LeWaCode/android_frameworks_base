package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.MemoryFile;
import android.provider.Settings;
import android.util.Log;
import android.net.ConnectivityManager;
import com.android.internal.telephony.TelephonyIntents;
import android.content.ContentResolver;
import android.location.LocationManager;
import android.database.Cursor;
import android.content.ContentQueryMap;
import java.util.Observable;
import java.util.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.net.Uri; //added by zhangbo

public class DataButton extends ReceiverButton{
	
	public static String TAG=DataButton.class.getSimpleName();
	
    public static boolean STATE_CHANGE_REQUEST = false;

    private static final List<Uri> OBSERVED_URIS = new ArrayList<Uri>();//added by zhangbo

    static {
        OBSERVED_URIS.add(Settings.Secure.getUriFor(Settings.Secure.MOBILE_DATA));
    }

    
    public DataButton() {
        super();
        mType = BUTTON_DATA;
        mFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mFilter.addAction(QUERY_POWER_STATUS_ACTION);
            //ADDED BY luokairong s
        Cursor settingsCursor = sContext.getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mContentQueryMap.addObserver(new SettingsObserver());
             //ADDED BY luokairong e
        mLabel=R.string.title_toggle_mobiledata;
    }
    

    @Override
	public void onReceive(Context context, Intent intent) {
	     Log.d(TAG,"DataButton,onReceive "+intent.getAction());
		if(intent.getAction().equals(QUERY_POWER_STATUS_ACTION)){	
			boolean enabled= getDataState();
           Log.d(TAG,"onReceive,data=="+enabled);
	        
			if(enabled) {
			     mIcon = R.drawable.stat_data_on;
		         mState = STATE_ENABLED;
			}else {
			     mIcon = R.drawable.stat_data_off;
		         mState = STATE_DISABLED;
			}			
			updateView();
		}
	}

	@Override
	protected IntentFilter getBroadcastIntentFilter() {
		// TODO Auto-generated method stub
		return mFilter;
	}


	@Override
    protected void updateState() {
        if (STATE_CHANGE_REQUEST) {
            mIcon = R.drawable.stat_data_on;
            mState = STATE_INTERMEDIATE;
        } else  if (getDataState()) {
            mIcon = R.drawable.stat_data_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_data_off;
            mState = STATE_DISABLED;
        }
    }
	
	
    @Override
    protected void toggleState() {
    	boolean enabled= getDataState();
        Log.d(TAG,"data=="+enabled);
        ConnectivityManager cm = (ConnectivityManager)sContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        cm.setMobileDataEnabled(!enabled);
		if(!enabled) {
		     
            //ADDED BY luokairong s
             Intent intent = new Intent(POWERSAVING_ACTION_NOTIFY_ON);
             intent.putExtra(POWERSAVING_DEV_TYPE, DEV_DATA);
             sContext.sendBroadcast(intent);
             //ADDED BY luokairong e

             mIcon = R.drawable.stat_data_on;
	         mState = STATE_ENABLED;
             
		}
		else {
		     mIcon = R.drawable.stat_data_off;
	         mState = STATE_DISABLED;
		}
		 updateView();
   }
    

    @Override
    protected boolean onLongClick() {
        startActivity("com.android.phone", "com.android.phone.Settings");
        return false;
    }

    private boolean getDataRomingEnabled() {
        return Settings.Secure.getInt(sContext.getContentResolver(),
                Settings.Secure.DATA_ROAMING,0) > 0;
    }

    private boolean getDataState() {
        ConnectivityManager cm = (ConnectivityManager)
                sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getMobileDataEnabled();
    }

      @Override
    protected List<Uri> getObservedUris() {//added by zhangbo
        return OBSERVED_URIS;
    }
    /*
     * public void networkModeChanged(int networkMode) {
     *     if (STATE_CHANGE_REQUEST) {
     *         ConnectivityManager cm = (ConnectivityManager)
     *                 sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
     *         cm.setMobileDataEnabled(true);
     *         STATE_CHANGE_REQUEST=false;
     *     }
     * }
     */

       private final class SettingsObserver implements Observer {
        
        public void update(Observable o, Object arg) {
           Log.i("lkr","SettingsObserver-------"+o);
           boolean enabled= getDataState();
           Log.d(TAG,"onReceive,data=="+enabled);
	        
			if(enabled) {
			     mIcon = R.drawable.stat_data_on;
		         mState = STATE_ENABLED;
			}else {
			     mIcon = R.drawable.stat_data_off;
		         mState = STATE_DISABLED;
			}			
			updateView();
        }
    }
    private ContentQueryMap mContentQueryMap=null;
}
