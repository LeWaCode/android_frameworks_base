package com.android.systemui.statusbar.switchwidget;

import android.os.SystemClock;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.content.Context;
import android.content.Intent;

import com.android.systemui.R;

public class ScreenOffButton extends StatelessButton
{
    public ScreenOffButton() {
        super();
        mType = BUTTON_SCREEN_OFF;
        mIcon = R.drawable.switch_screen_off;
    }

    @Override
    protected void onClick() {
        turnScreenOff();
    }

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.DISPLAY_SETTINGS");
        return true;
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Turns screen off
     */
    private void turnScreenOff() {
        final PowerManager pm = (PowerManager)
                sContext.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
    }
}
