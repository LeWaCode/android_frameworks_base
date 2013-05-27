package com.android.systemui.statusbar;

import com.android.systemui.R;

import java.util.ArrayList;
import com.lewa.os.ui.ViewPagerIndicatorActivity;
import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;


public class DatamonitorMainActivity extends ViewPagerIndicatorActivity {
    /** Called when the activity is first created. */
      private DataMonitorService mService;
     private  boolean mBound = false;
     private Handler mHandler;
     private Runnable mRunnable;
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
    public void onCreate(Bundle savedInstanceState) {
        
        
        ArrayList<StartParameter> aClasses = new ArrayList<StartParameter>();
        aClasses.add(new StartParameter(FirewallActivity.class, null,
                R.string.firewall));    
        aClasses.add(new StartParameter(DataActivity.class, null,
                R.string.data_activity));
            aClasses.add(new StartParameter(MonitorSetupActivity.class, null,
                R.string.monitor_setup));
            
        setupFlingParm(aClasses, R.layout.datamonitormain, R.id.indicator_outer,
                R.id.pager_outer);
        setDisplayScreen(1);
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
	
	bindService(new Intent(this, DataMonitorService.class),mConnection,Context.BIND_AUTO_CREATE);

	 mHandler = new Handler();
	 mRunnable = new Runnable() {    
            @Override
            public void run() {
            		if(mBound) {
          			mService.setPositiveDataMonitorMode(true);
            		}
            }
 	};
        
    }

    @Override
    protected void onStart() {
    	 super.onStart();
	 
    }

    @Override
    protected void onPause() {
        super.onPause();
	if(mBound) {
	 	mService.setPositiveDataMonitorMode(false);
	 }
    }

    @Override
    protected void onResume() {
        super.onResume();
	 if(mBound) {
	 	mService.setPositiveDataMonitorMode(true);
	 }
	 else {
	 	mHandler.postDelayed(mRunnable, 100);
	 }
    }


    @Override
    protected void onStop() {
        super.onStop();
        FirewallActivity firewallActivity = (FirewallActivity)super.getItemActivity(0);
        if (firewallActivity != null) {
            firewallActivity.Pause();
        }

	
    }

    @Override
     protected void onDestroy() {
     	 unbindService(mConnection);
	 super.onDestroy();
     }
}
