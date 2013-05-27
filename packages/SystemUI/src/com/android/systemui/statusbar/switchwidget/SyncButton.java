package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.provider.Settings;
import android.net.ConnectivityManager;
import android.content.SyncStatusObserver;

import android.view.View;

public class SyncButton extends SwitchButton
{
    public SyncButton() {
        super();
        mType = BUTTON_SYNC;
    }

    private SyncStatusObserver mSyncObserver = new SyncStatusObserver() {
            public void onStatusChanged(int which) {
                update();
            }
        };
    private Object mSyncObserverHandle = null;

    @Override
    protected void setupButton(View view) {
        super.setupButton(view);

        if (mView == null && mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        } else if(mView != null && mSyncObserverHandle == null) {
            mSyncObserverHandle = ContentResolver.addStatusChangeListener(
                    ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncObserver);
        }
    }

    @Override
    protected void updateState() {
        if (getSyncState()) {
            mIcon = R.drawable.switch_sync_on;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.switch_sync_off;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
        ConnectivityManager cm = (ConnectivityManager)
                sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean backgroundData = getBackgroundDataState();
        boolean sync = ContentResolver.getMasterSyncAutomatically();

        // four cases to handle:
        // setting toggled from off to on:
        // 1. background data was off, sync was off: turn on both
        if (!backgroundData && !sync) {
            cm.setBackgroundDataSetting(true);
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // 2. background data was off, sync was on: turn on background data
        if (!backgroundData && sync) {
            cm.setBackgroundDataSetting(true);
        }

        // 3. background data was on, sync was off: turn on sync
        if (backgroundData && !sync) {
            ContentResolver.setMasterSyncAutomatically(true);
        }

        // setting toggled from on to off:
        // 4. background data was on, sync was on: turn off sync
        if (backgroundData && sync) {
            ContentResolver.setMasterSyncAutomatically(false);
        }
    }

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.SYNC_SETTINGS");
        return false;
    }

    private boolean getBackgroundDataState() {
        ConnectivityManager cm = (ConnectivityManager)
                sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getBackgroundDataSetting();
    }

    private boolean getSyncState() {
        boolean backgroundData = getBackgroundDataState();
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        return backgroundData && sync;
    }
}
