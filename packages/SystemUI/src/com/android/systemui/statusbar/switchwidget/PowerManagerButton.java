package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PowerManagerButton extends ObserveButton
{
	
	private String TAG=PowerManagerButton.class.getSimpleName();
	private String POWER_ACTION="com.lewa.powermanager.action";
	IntentFilter mFilter;
    //added by luo kairong
    public static final String SPM_DEVS_SWITTCH_FINISH_ACTION = "spm_dev_switch_finish_action";
	
    public PowerManagerButton() {
        super();
        mType = BUTTON_POWER_MANAGER;
        mFilter = new IntentFilter();
        mFilter.addAction(QUERY_POWER_STATUS_ACTION);
        mFilter.addAction(SPM_DEVS_SWITTCH_FINISH_ACTION);
        mObservedUris.add(Settings.System.getUriFor(Settings.System.POWERMANAGER_MODE_ON));
        mLabel=R.string.title_toggle_powermanager;
    }
    
    @Override
   	protected IntentFilter getBroadcastIntentFilter() {
   		return mFilter;
   	}

   	@Override
   	protected void onReceive(Context context, Intent intent) {
       	String action=intent.getAction();
       	String value="";
       	if(action.equals(QUERY_POWER_STATUS_ACTION)){
       		//value=intent.getStringExtra(QUERY_POWER_STATUS_ACTION_KEY);
       	    //Toast.makeText(context,value,Toast.LENGTH_SHORT).show();
       	}
        //added by luo kairong
        else if(action.equals(SPM_DEVS_SWITTCH_FINISH_ACTION)){
       	    isClick=false;
       	    updateState();
            updateView();
        }
    }

    @Override
    protected void updateState() {
        //added by luo kairong
        if(isClick){
            return;
        }
            
    	if (getState()) {
            mIcon = R.drawable.stat_power_on;
            mState = STATE_ENABLED;
        }else {
            mIcon = R.drawable.stat_power_off;
            mState = STATE_DISABLED;
        }
    }
    
    boolean isClick=false;
    
    private Timer timer=new Timer();
    
    private Handler handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			if(msg.what==1){
				Log.d(TAG,"received msg");
	    		//boolean state = getState();
	            //Settings.System.putInt(sContext.getContentResolver(),Settings.System.POWERMANAGER_MODE_ON, state ? 0 : 1);
				isClick=true;
			    updateState();
			}
		}    	
    };

    @Override
    protected void toggleState() {   
    	if(!isClick){
    		boolean state = getState();
//            Settings.System.putInt(sContext.getContentResolver(),Settings.System.POWERMANAGER_MODE_ON, state ? 0 : 1);

            Intent intent = new Intent(POWER_ACTION);
            intent.putExtra("powerstate", !state ? 1:0);
            sContext.sendBroadcast(intent);
            
            mIcon=R.drawable.stat_power_inter;
            Drawable icon = sContext.getResources().getDrawable(mIcon); 
            int sIconTextureWidth=getIconTextureWidth();
            icon.setBounds(0, 0, sIconTextureWidth, sIconTextureWidth); 
            mView.setCompoundDrawables(null, icon, null, null); 
    		isClick=true;
            
            /*
            timer.schedule(new TimerTask() {
    			
    			@Override
    			public void run() {
    				Log.e(TAG,"task begin");
    				Message mesasge = new Message();  
                    mesasge.what = 1;  
                    handler.sendMessage(mesasge);  
    			}
    		}, DELAY_RUN_TIME); 
    		*/
    	}
    }

    @Override
    protected boolean onLongClick() {
    	directTo(sContext,"com.lewa.spm");
        return false;
    }

    private boolean getState() {
        return Settings.System.getInt(sContext.getContentResolver(), Settings.System.POWERMANAGER_MODE_ON,0) == 1;
    }
    
    
    private int getIconTextureWidth(){
	   final Resources resources = sContext.getResources(); 
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
       int sIconTextureWidth = iconWidth + (int)(blurPx*2); 
       return sIconTextureWidth;
    }

}
