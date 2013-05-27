package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.location.LocationManager;
import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GpsButton extends ObserveButton
{
    public GpsButton() {
        super();
        mType = BUTTON_GPS;
        mObservedUris.add(Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED));
        mLabel=R.string.title_toggle_gps;
    }

    @Override
    protected void updateState() {
        if(getGpsState()) {
            mIcon = R.drawable.stat_gps_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.stat_gps_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
        ContentResolver resolver = sContext.getContentResolver();
        boolean enabled = getGpsState();
        Settings.Secure.setLocationProviderEnabled(
            resolver, LocationManager.GPS_PROVIDER, !enabled);
        //ADDED BY luokairong s
        if(!enabled){
             
             Intent intent = new Intent(POWERSAVING_ACTION_NOTIFY_ON);
             intent.putExtra(POWERSAVING_DEV_TYPE, DEV_GPS);
             sContext.sendBroadcast(intent);
             
        }
        //ADDED BY luokairong e
        
    }

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.LOCATION_SOURCE_SETTINGS");
        return false;
    }

    private boolean getGpsState() {
        ContentResolver resolver = sContext.getContentResolver();
        return Settings.Secure.isLocationProviderEnabled(
                resolver, LocationManager.GPS_PROVIDER);
    }
}
