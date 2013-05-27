package com.android.systemui.statusbar.switchwidget;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
// import android.app.KeyguardManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import android.provider.CmSystem;
import android.provider.Settings;

import android.os.SystemClock;
import android.os.PowerManager;
import android.os.ServiceManager;

import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarService;

public class LockButton extends StatelessButton {
	
	String TAG=LockButton.class.getSimpleName();
	
	private static final String DELAYED_KEYGUARD_ACTION = "com.android.internal.policy.impl.KeyguardViewMediator.DELAYED_KEYGUARD";
	
	private static Boolean LOCK_SCREEN_STATE = null;
	private KeyguardLock mLock = null;

	public LockButton() {
		super();
		mType = BUTTON_LOCK_NOW;
		mIcon = R.drawable.stat_lockscreen;
		mLabel = R.string.title_toggle_locknow;
	}

	@Override
	protected void onClick() {
//		lockNow();
	}

	@Override
	protected boolean onLongClick() {
		startActivity("com.android.settings", "com.android.settings.ChooseLockGeneric");
		return false;
	}
	
	@Override
	protected void updateState() {
		getState();
		if (LOCK_SCREEN_STATE == null) {
//			mIcon = R.drawable.stat_lock_screen_off;
			mIcon = R.drawable.stat_lockscreen;
			mState = STATE_INTERMEDIATE;
		} else if (LOCK_SCREEN_STATE) {
//			mIcon = R.drawable.stat_lock_screen_on;
			mIcon = R.drawable.stat_lockscreen;
			mState = STATE_ENABLED;
		} else {
//			mIcon = R.drawable.stat_lock_screen_off;
			mIcon = R.drawable.stat_lockscreen;
			mState = STATE_DISABLED;
		}
		Log.d(TAG, "updateState");
	}

	@Override
	protected void toggleState() {	
		
		try {
			Intent intent=new Intent(StatusBarService.ACTION_COLLAPSE_ViEW);
	    	sContext.sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//lockNow();
		turnScreenOff();
		update();
		Log.d(TAG,"toggleState");
	}

	private void turnScreenOff() {
        final PowerManager pm = (PowerManager)
                sContext.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
     }

/*	*//**
	 * @author: Woody Guo <guozhenjiang@ndoo.net>
	 * @description: Locks the device, i.e., show the lock screen
	 */
	private void lockNow() {
		if (true)
			Log.d("SwitchWidget.LockButton.lockNow", "sending broadcast");

		Intent intent = new Intent(DELAYED_KEYGUARD_ACTION);
		intent.putExtra("immediately", 1);
		//intent.putIntExtra("handlerMessage", NOTIFY_SCREEN_OFF);
		sContext.sendBroadcast(intent);
		// KeyguardManager km = (KeyguardManager)
		// sContext.getSystemService(Context.KEYGUARD_SERVICE);
		// if (null != km) {
		// km.newKeyguardLock(Context.KEYGUARD_SERVICE).reenableKeyguard();
		// }
	}
	
	private KeyguardLock getLock(Context context) {
		if (mLock == null) {
			Log.d(TAG,"getLock()");
			KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);
			mLock = keyguardManager.newKeyguardLock(Context.KEYGUARD_SERVICE);
		}
		return mLock;
	}

	private static boolean getState() {
		if (LOCK_SCREEN_STATE == null) {
			LOCK_SCREEN_STATE = true;
		}
		return LOCK_SCREEN_STATE;
	}
}
