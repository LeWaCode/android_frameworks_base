package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings;

import android.util.Log;
import com.android.internal.telephony.Phone;

import java.util.List;
import java.util.ArrayList;

public class NetworkModeButton extends ReceiverButton
{
    // retrieved from Phone.apk
    public static final String ACTION_NETWORK_MODE_CHANGED
            = "com.android.internal.telephony.NETWORK_MODE_CHANGED";
    public static final String ACTION_REQUEST_NETWORK_MODE
            = "com.android.internal.telephony.REQUEST_NETWORK_MODE";
    public static final String ACTION_MODIFY_NETWORK_MODE
            = "com.android.internal.telephony.MODIFY_NETWORK_MODE";
    public static final String EXTRA_NETWORK_MODE = "networkMode";

    private static final int NO_NETWORK_MODE_YET = -99;
    private static final int NETWORK_MODE_UNKNOWN = -100;

    private static final int CM_MODE_3G2G = 0;
    private static final int CM_MODE_3GONLY = 1;
    private static final int CM_MODE_BOTH = 2;

    private static int NETWORK_MODE = NO_NETWORK_MODE_YET;
    private static int INTENDED_NETWORK_MODE = NO_NETWORK_MODE_YET;
    private static int CURRENT_INTERNAL_STATE = STATE_INTERMEDIATE;


    protected final List<Uri> mObservedUris;

    public NetworkModeButton() {
        super();
        mType = BUTTON_NETWORKMODE;
        mFilter.addAction(ACTION_NETWORK_MODE_CHANGED);
        mObservedUris = new ArrayList<Uri>();
        mObservedUris.add(Settings.Secure.getUriFor(Settings.Secure.PREFERRED_NETWORK_MODE));
        mLabel=R.string.title_toggle_networkmode;
    }

    @Override
    protected void onChangeUri(Uri uri) {
        update();
    }

    @Override
    protected List<Uri> getObservedUris() {
        return mObservedUris;
    }

    @Override
    protected void updateState() {
         Context context = mView.getContext();
        NETWORK_MODE = get2G3G(context);
        mState = networkModeToState(context);
        
        switch (mState) {
        case STATE_DISABLED:
            mIcon = R.drawable.stat_2g3g_off;
            break;
        case STATE_ENABLED:
            if (NETWORK_MODE == Phone.NT_MODE_GSM_ONLY) {//modified by zhangbo
                //mIcon = R.drawable.stat_3g_on;//modified zhangbo
                mIcon = R.drawable.stat_2g3g_off;
            } else {
                mIcon = R.drawable.stat_2g3g_on;
            }
            break;
        case STATE_INTERMEDIATE:
            // In the transitional state, the bottom green bar
            // shows the tri-state (on, off, transitioning), but
            // the top dark-gray-or-bright-white logo shows the
            // user's intent. This is much easier to see in
            // sunlight.
            if (CURRENT_INTERNAL_STATE == STATE_TURNING_ON) {
                if (NETWORK_MODE == Phone.NT_MODE_GSM_ONLY) {//modified by zhangbo
                    //mIcon = R.drawable.stat_3g_on;//modified zhangbo
                    mIcon = R.drawable.stat_2g3g_off;
                } else {
                    mIcon = R.drawable.stat_2g3g_on;
                }
            } else {
                mIcon = R.drawable.stat_2g3g_off;
            }
            break;
        }
    }

    @Override
    protected void toggleState() {
        Context context = mView.getContext();
        int currentMode = getCurrentCMMode(context);

        Intent intent = new Intent(ACTION_MODIFY_NETWORK_MODE);
        
        switch (NETWORK_MODE) {
        case Phone.NT_MODE_WCDMA_PREF:
        case Phone.NT_MODE_GSM_UMTS:
            intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
            CURRENT_INTERNAL_STATE = STATE_TURNING_OFF;
            INTENDED_NETWORK_MODE = Phone.NT_MODE_GSM_ONLY;
            break;
        case Phone.NT_MODE_WCDMA_ONLY:
            if(currentMode == CM_MODE_3GONLY) {
                intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
                CURRENT_INTERNAL_STATE = STATE_TURNING_OFF;
                INTENDED_NETWORK_MODE = Phone.NT_MODE_GSM_ONLY;
            } else {
                intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_WCDMA_PREF);
                CURRENT_INTERNAL_STATE = STATE_TURNING_ON;
                INTENDED_NETWORK_MODE = Phone.NT_MODE_WCDMA_PREF;
            }
            break;
        case Phone.NT_MODE_GSM_ONLY:
            if(currentMode == CM_MODE_3GONLY || currentMode == CM_MODE_BOTH) {
                intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_WCDMA_ONLY);
                CURRENT_INTERNAL_STATE = STATE_TURNING_ON;
                INTENDED_NETWORK_MODE = Phone.NT_MODE_WCDMA_ONLY;
            } else {
                intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_WCDMA_PREF);
                CURRENT_INTERNAL_STATE = STATE_TURNING_ON;
                INTENDED_NETWORK_MODE = Phone.NT_MODE_WCDMA_PREF;
            }
            break;
            
        case Phone.NT_MODE_CDMA:
        case Phone.NT_MODE_EVDO_NO_CDMA:
            intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_CDMA_NO_EVDO);
            CURRENT_INTERNAL_STATE = STATE_TURNING_OFF;
            INTENDED_NETWORK_MODE = Phone.NT_MODE_CDMA_NO_EVDO;
            break;
            
        case Phone.NT_MODE_CDMA_NO_EVDO:
            intent.putExtra(EXTRA_NETWORK_MODE, Phone.NT_MODE_CDMA);
            CURRENT_INTERNAL_STATE = STATE_TURNING_OFF;
            INTENDED_NETWORK_MODE = Phone.NT_MODE_CDMA;
            break;
 
        }

        NETWORK_MODE = NETWORK_MODE_UNKNOWN;
        context.sendBroadcast(intent);
    }

    @Override
    protected boolean onLongClick() {
        startActivity("com.android.phone", "com.android.phone.Settings");
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
         if (intent.getExtras() != null) {
            NETWORK_MODE = intent.getExtras().getInt(EXTRA_NETWORK_MODE);
            //Update to actual state
            INTENDED_NETWORK_MODE = NETWORK_MODE;
        }

        //need to clear intermediate states
        CURRENT_INTERNAL_STATE = STATE_ENABLED;

        int widgetState = networkModeToState(context);
        CURRENT_INTERNAL_STATE = widgetState;
     
        update();
    }


     private static int get2G3G(Context context) {
        int state = 99;
        try {
            state = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.PREFERRED_NETWORK_MODE);
        } catch (SettingNotFoundException e) {
        }
        return state;
    }

    private int networkModeToState(Context context) {
        if (CURRENT_INTERNAL_STATE == STATE_TURNING_ON ||
                CURRENT_INTERNAL_STATE == STATE_TURNING_OFF) {
            return STATE_INTERMEDIATE;
        }            
        
        switch(NETWORK_MODE) {
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_CDMA:            //modify by zhaolei,for 3g
            case Phone.NT_MODE_EVDO_NO_CDMA:    //modify by zhaolei,for 3g only
                return STATE_ENABLED;
            case Phone.NT_MODE_GSM_ONLY:
                return STATE_DISABLED;
//            case Phone.NT_MODE_CDMA:
            case Phone.NT_MODE_CDMA_NO_EVDO:
//            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_GLOBAL:
                // need to check wtf is going on
                Log.d(TAG, "Unexpected network mode (" + NETWORK_MODE + ")");
                return STATE_DISABLED;
        }
        return STATE_INTERMEDIATE;
    }

    private int getCurrentCMMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANDED_NETWORK_MODE,
                CM_MODE_3G2G);
    }
}
