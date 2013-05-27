package com.android.systemui.usb;

import android.app.Activity;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.util.Log;
import android.provider.Settings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import com.android.systemui.util.SuCommander;
import android.os.Environment;
import java.io.File;

import com.android.systemui.R;

public class UsbModeSelection extends Activity {
    private StorageManager mStorageManager;
    private BroadcastReceiver mUsbStateReceiver;

    public UsbModeSelection() {
        mUsbStateReceiver = new USBStateReceiver();
    }

    private void switchUsbMassStorage(boolean flag) {
        if(flag != mStorageManager.isUsbMassStorageEnabled()) {
            (new SwitchThread(flag)).start();
        }
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        if (mStorageManager == null) {
            mStorageManager = (StorageManager)getSystemService("storage");
            if(mStorageManager == null) {
                Log.w("UsbModeSelection", "Failed to get StorageManager");
            }
        }

        String s = getIntent().getAction();
        if ("UsbModeSelection.action.MOUNT_STORAGE".equals(s)) {
            switchUsbMassStorage(true);            
        } else if ("UsbModeSelection.action.CHARGE_ONLY".equals(s)) {      
            switchUsbMassStorage(false);            
        }
        finish();
    }

    protected void onPause() {
        super.onPause();
        
        unregisterReceiver(mUsbStateReceiver);
        finish();
    }

    protected void onResume() {
        super.onResume();
        
        IntentFilter intentfilter = new IntentFilter("android.hardware.usb.action.USB_STATE");
        Intent intent = registerReceiver(mUsbStateReceiver, intentfilter);
    }
    
    private class USBStateReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.hardware.usb.action.USB_STATE") 
                && !intent.getExtras().getBoolean("connected")) {
                finish();
            }
        }
    }

    private class SwitchThread extends Thread {
        final boolean bOn;        

        SwitchThread(boolean flag) {
            super();
            bOn = flag;           
        }        

        public void run() {
            if (bOn) {
                //add by chenhengheng , 2012.03.15 , for swap ,for bug 3079,3956,3483
                int swapMethod = Settings.System.getInt(getContentResolver(), Settings.System.SWAPPER_HOWSWAP, -1);
                swapOff(swapMethod);
                // End
                mStorageManager.enableUsbMassStorage();
            } else {
                mStorageManager.disableUsbMassStorage();
            }
        }
    }

    //add by chenhengheng,2012.03.14,for swap,for bug 3079,3956,3483
    public final static int SYSTEMFILE_METHOD  = 3;
    public final static int SDCARDFILE_METHOD  = 1;
    public final static int PARTITION_METHOD   = 2;	
	
    private void swapOff(int method) {
        File swapFile = null;
        String partPath = "";
        String command  = "";
        Process p = null;
        SuCommander su;

        switch (method) {
        	case SDCARDFILE_METHOD:// fileModeOff
        	    partPath = Environment.getExternalStorageDirectory().getPath()+"/swap.img";
        	    swapFile = new File(partPath);
        	    command = "busybox swapoff " + partPath;
        		break;
        	case PARTITION_METHOD:// partitionSwapOff
        	    partPath = SystemProperties.get("ro.lewa.swapper.part_path");
        	    swapFile = new File(partPath);
        	    if (swapFile.exists()) {
        			command  = "busybox swapoff " + partPath;
        	    }
        		break;
        	default:
        		break;
        }

        if (!command.equals("")) {
            try {
                su = new SuCommander();
                while (!su.isReady()) {
                    Thread.sleep(100);
                }
                su.exec(command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (swapFile != null) {
           swapFile = null;
        }
    }
	//end by chenhengheng	
}
