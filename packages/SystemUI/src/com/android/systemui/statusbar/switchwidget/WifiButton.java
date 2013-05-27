package com.android.systemui.statusbar.switchwidget;

import java.util.Map;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;

public class WifiButton extends ReceiverButton
{
    private static final class WifiStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wm = (WifiManager)
                    context.getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                return wifiStateToFiveState(wm.getWifiState());
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

            // Actually request the Wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    // Disable tethering if enabling Wifi
                    int wifiApState = wm.getWifiApState();
                    if (desiredState && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING)
                            || (wifiApState == wm.WIFI_AP_STATE_ENABLED))) {
                        wm.setWifiApEnabled(null, false);
                    }

                    wm.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            int widgetState = wifiStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WifiManager's state values into our Wifi/Bluetooth-common
         * state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    public WifiButton() {
        super();
        mStateTracker = new WifiStateTracker();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mType = BUTTON_WIFI;
        mLabel=R.string.title_toggle_wifi;
    }

    @Override
    protected void updateState() {
        mState = mStateTracker.getTriState(sContext);
        switch (mState) {
            case STATE_DISABLED:
                mIcon = R.drawable.stat_wifi_off;
                break;
            case STATE_ENABLED:
                mIcon = R.drawable.stat_wifi_on;
                break;
            case STATE_INTERMEDIATE:
                mIcon = R.drawable.switch_wifi_inter;
                break;
        }
    }

    @Override
    protected void toggleState() {
        mStateTracker.toggleState(sContext);
        new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(DELAY_RUN_TIME);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
    }
    
	public boolean getWifiState() {
		WifiManager wm = (WifiManager) sContext.getSystemService(Context.WIFI_SERVICE);
		boolean flag = false;
		switch (wm.getWifiState()) {
		case WifiManager.WIFI_STATE_DISABLED:
			flag = false;
			break;
		case WifiManager.WIFI_STATE_DISABLING:
			flag = false;
			break;
		case WifiManager.WIFI_STATE_ENABLED:
			flag = true;
			break;
		case WifiManager.WIFI_STATE_ENABLING:
			flag = true;
			break;
		case WifiManager.WIFI_STATE_UNKNOWN:
			break;
		default:
			break;
		}
		return flag;
	}

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.WIFI_SETTINGS");
        return false;
    }
}
