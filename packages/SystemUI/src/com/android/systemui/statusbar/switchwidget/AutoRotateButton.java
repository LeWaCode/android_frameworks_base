package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AutoRotateButton extends ObserveButton
{
    public AutoRotateButton() {
        super();
        mType = BUTTON_AUTOROTATE;
        mObservedUris.add(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION));
        mLabel=R.string.title_toggle_autorotate;
    }

    @Override
    protected void updateState() {
        if (getOrientationState() == 1) {
            mIcon = R.drawable.stat_orientation_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_orientation_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
        if(getOrientationState() == 0) {
            Settings.System.putInt(
                    sContext.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1);
        } else {
            Settings.System.putInt(
                    sContext.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 0);
        }
    }

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.DISPLAY_SETTINGS");
        return false;
    }

    private int getOrientationState() {
        return Settings.System.getInt(
                sContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0);
    }
}
