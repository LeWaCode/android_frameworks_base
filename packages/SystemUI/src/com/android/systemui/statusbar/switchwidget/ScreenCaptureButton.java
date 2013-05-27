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

public class ScreenCaptureButton extends ObserveButton
{
    public ScreenCaptureButton() {
        super();
        mType = BUTTON_SCREEN_CAPTURE;
        mObservedUris.add(Settings.System.getUriFor(Settings.System.SCREEN_CAPTURE_MODE));
        mObservedUris.add(Settings.System.getUriFor(Settings.System.SCREEN_CAPTURE_STYLE));
    }

    @Override
    protected void updateState() {
        if (getState()) {
            mIcon = R.drawable.switch_screencapture_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.switch_screencapture_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
        boolean state = getState();
        int newstate = state ? 0 : 1;
        Settings.System.putInt(sContext.getContentResolver()
                , Settings.System.SCREEN_CAPTURE_STYLE, newstate);
        Settings.System.putInt(sContext.getContentResolver()
                , Settings.System.SCREEN_CAPTURE_MODE, newstate);

        if (1 == newstate) {
            sContext.sendBroadcast(new Intent("android.intent.action.SCREENSHOT"));
        } else {
            sContext.stopService(new Intent("com.woody.captureit.capture"));
        }
    }

    @Override
    protected boolean onLongClick() {
        startActivity("com.woody.captureit.settings");
        return false;
    }

    private boolean getState() {
        return Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.SCREEN_CAPTURE_MODE, 0) == 1
                && Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.SCREEN_CAPTURE_STYLE, 0) == 1;
    }
}
