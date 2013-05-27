package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AirplaneButton extends ObserveButton
{
	
	private String TAG=AirplaneButton.class.getSimpleName();
	
    public AirplaneButton() {
        super();
        mType = BUTTON_AIRPLANE;
        mObservedUris.add(Settings.System.getUriFor(Settings.System.AIRPLANE_MODE_ON));
        mLabel=R.string.title_toggle_airplane;
    }

    @Override
    protected void updateState() {
        if (getState()) {
            mIcon = R.drawable.stat_airplane_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_airplane_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
        boolean state = getState();
        Settings.System.putInt(sContext.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, state ? 0 : 1);

        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", !state);
        sContext.sendBroadcast(intent);
        Log.e(TAG, "toggleState()");
    }

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.AIRPLANE_MODE_SETTINGS");
        return false;
    }

    private boolean getState() {
        return Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.AIRPLANE_MODE_ON,0) == 1;
    }
}
