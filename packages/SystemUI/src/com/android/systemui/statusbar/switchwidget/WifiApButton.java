package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;

public class WifiApButton extends ReceiverButton
{
    private static final class WifiApStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wm = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                return wifiApStateToFiveState(wm.getWifiApState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {
            final WifiManager wm = (WifiManager)
                    context.getSystemService(Context.WIFI_SERVICE);
            if (wm == null) {
                return;
            }

            // Actually request the WifiAp change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    // Disable tethering if enabling WifiAp
                    int wifiState = wm.getWifiState();
                    if (desiredState && ((wifiState == WifiManager.WIFI_STATE_ENABLING)
                            || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
                        wm.setWifiEnabled(false);
                    }

                    wm.setWifiApEnabled(null, desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, -1);
            int widgetState = wifiApStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WifiManager's state values into our Wifi/WifiAP/Bluetooth-common
         * state values.
         */
        private static int wifiApStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_AP_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_AP_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_AP_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_AP_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    public WifiApButton() {
        super();
        mStateTracker = new WifiApStateTracker();
        mType = BUTTON_WIFI_AP;
        mFilter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
    }

    @Override
    protected void updateState() {
        mState = mStateTracker.getTriState(sContext);
        switch (mState) {
            case STATE_DISABLED:
                mIcon = R.drawable.switch_wifi_ap_off;
                break;
            case STATE_ENABLED:
                mIcon = R.drawable.switch_wifi_ap_on;
                break;
            case STATE_INTERMEDIATE:
                mIcon = R.drawable.switch_wifi_ap_off;
                break;
        }
    }

    @Override
    protected void toggleState() {
        mStateTracker.toggleState(sContext);
    }

    @Override
    protected boolean onLongClick() {
        startActivity("com.android.settings", "com.android.settings.TetherSettings");
        return false;
    }
}
