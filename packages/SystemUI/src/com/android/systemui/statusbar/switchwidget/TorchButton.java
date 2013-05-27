package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.location.LocationManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TorchButton extends ObserveButton
{
    public TorchButton() {
        super();
        mType = BUTTON_TORCH;
        mObservedUris.add(Settings.System.getUriFor(Settings.System.TORCH_STATE));
        mLabel=R.string.title_toggle_flashlight;
    }

    @Override
    protected void updateState() {
        boolean enabled = Settings.System.getInt(
                sContext.getContentResolver(), Settings.System.TORCH_STATE, 0) == 1;
        if(enabled) {
            mIcon = R.drawable.stat_torch_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_torch_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
        boolean bright = Settings.System.getInt(sContext.getContentResolver(),Settings.System.EXPANDED_FLASH_MODE, 0) == 1;
        Intent i = new Intent("net.cactii.flash2.TOGGLE_FLASHLIGHT");
        i.putExtra("bright", bright);
        sContext.sendBroadcast(i);
    }

    @Override
    protected boolean onLongClick() {
        try {
            startActivity("net.cactii.flash2", "net.cactii.flash2.MainActivity");
        } catch (Exception e) {
            Toast.makeText(sContext, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
