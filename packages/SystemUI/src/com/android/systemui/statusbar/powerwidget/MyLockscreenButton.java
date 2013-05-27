package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;




public class MyLockscreenButton extends PowerButton {
    public MyLockscreenButton() { mType = BUTTON_LOCK_SCREEN; }

    @Override
    protected void updateState() {
        mIcon = R.drawable.stat_lockscreen;
        mState = STATE_DISABLED;
    }

    @Override
    protected void toggleState() {
	Context context = mView.getContext();
	LockScreenUtil.getInstance(context).lockScreen();
    }

    @Override
    protected boolean handleLongClick() {
        
        return false;
    }
}
