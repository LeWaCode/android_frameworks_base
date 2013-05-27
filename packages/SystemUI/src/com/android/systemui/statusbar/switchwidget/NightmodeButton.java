package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.statusbar.StatusBarService;

import java.util.List;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

public class NightmodeButton extends ObserveButton {
	
	String TAG=NightmodeButton.class.getSimpleName();
	
	public NightmodeButton() {
		super();
		mType = BUTTON_NIGHT_MODE;
		mObservedUris.add(Settings.System.getUriFor(Settings.System.NIGHT_MODES));
		mLabel=R.string.power_widgetbutton_nightmode;
	}

	@Override
	protected void updateState() {

		final Context context = mView.getContext();
		if (RenderFXServiceRunning(context)) {
			mIcon = R.drawable.stat_nightmode_on;
			mState = STATE_ENABLED;
		} else {
			mIcon = R.drawable.stat_nightmode_off;
			mState = STATE_DISABLED;
		}

	}

	@Override
	protected void toggleState() {
		final Context context = mView.getContext();

		Intent pendingIntent = new Intent(context, RenderFXService.class);

		if (RenderFXServiceRunning(context)) {
			Log.d(TAG,"stop service");
			context.stopService(pendingIntent);
		} else {
			Log.d(TAG,"start service");
			context.startService(pendingIntent);
		}
		update();
		Log.d(TAG,"toggleState");
        Intent intent=new Intent(StatusBarService.ACTION_COLLAPSE_ViEW);
        context.sendBroadcast(intent);
	}

	/*@Override
	protected boolean handleLongClick() {
		Intent intent = new Intent("android.settings.DISPLAY_SETTINGS");
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mView.getContext().startActivity(intent);
		return true;
	}*/
	
	@Override
	protected boolean onLongClick() {
        startActivity("android.settings.DISPLAY_SETTINGS");
        return false;
	}

	private boolean RenderFXServiceRunning(Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Activity.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> svcList = am.getRunningServices(100);

		if (!(svcList.size() > 0))
			return false;

		for (RunningServiceInfo serviceInfo : svcList) {
			if (serviceInfo.service.getClassName().endsWith(".RenderFXService"))
				return true;
		}
		return false;
	}
}
