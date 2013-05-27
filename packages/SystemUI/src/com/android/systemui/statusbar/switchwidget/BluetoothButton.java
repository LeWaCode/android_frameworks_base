package com.android.systemui.statusbar.switchwidget;

import java.util.Map;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.bluetooth.BluetoothAdapter;

public class BluetoothButton extends ReceiverButton
{
    private static final class BluetoothStateTracker extends StateTracker {

        @Override
        public int getActualState(Context context) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                return STATE_UNKNOWN;
            }

            return bluetoothStateToFiveState(mBluetoothAdapter.getState());
        }

        @Override
        protected void requestStateChange(Context context,
                final boolean desiredState) {
            // Actually request the Bluetooth change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if(mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                    } else {
                        mBluetoothAdapter.enable();
                    }
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        }

        /**
         * Converts BluetoothAdapter's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    return STATE_DISABLED;
                case BluetoothAdapter.STATE_ON:
                    return STATE_ENABLED;
                case BluetoothAdapter.STATE_TURNING_ON:
                    return STATE_TURNING_ON;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    public BluetoothButton() {
        super();
        mType = BUTTON_BLUETOOTH;
        mStateTracker = new BluetoothStateTracker();
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mLabel=R.string.title_toggle_bluetooth;
    }

    @Override
    protected void updateState() {
        mState = mStateTracker.getTriState(sContext);
        switch (mState) {
            case STATE_DISABLED:
                mIcon = R.drawable.stat_bluetooth_off;
                break;
            case STATE_ENABLED:
                mIcon = R.drawable. stat_bluetooth_on;
                break;
            case STATE_INTERMEDIATE:
                mIcon = R.drawable.switch_bluetooth_inter;
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
    
	public boolean getBluetoothState() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		return bluetoothStateToFiveState(mBluetoothAdapter.getState());
	}

	private boolean bluetoothStateToFiveState(int bluetoothState) {
		boolean flag = false;
		switch (bluetoothState) {
		case BluetoothAdapter.STATE_OFF:
			flag = false;
			break;
		case BluetoothAdapter.STATE_ON:
			flag = true;
			break;
		case BluetoothAdapter.STATE_TURNING_ON:
			flag = true;
			break;
		case BluetoothAdapter.STATE_TURNING_OFF:
			flag = false;
			break;
		default:
			break;
		}
		return flag;
	}

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.BLUETOOTH_SETTINGS");
        return false;
    }
}
