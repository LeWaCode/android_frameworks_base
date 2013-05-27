package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.android.internal.app.ShutdownThread;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;

public class PoweroffButton extends PowerButton {
    public PoweroffButton() { mType = BUTTON_POWER_OFF; }

    @Override
    protected void updateState() {
        mIcon = R.drawable.stat_poweroff;
        mState = STATE_DISABLED;
    }

    @Override
    protected void toggleState() {
        final Context context = mView.getContext(); 
        final AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle(com.android.internal.R.string.power_off)
                                    .setMessage(com.android.internal.R.string.shutdown_confirm)
                                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent shutdown = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);  
                                            shutdown.putExtra(Intent.EXTRA_KEY_CONFIRM, false);  
                                            shutdown.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
                                            context.startActivity(shutdown);
                                        }
                                    })
                                    .setNegativeButton(com.android.internal.R.string.no, null)
                                    .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        dialog.show();
                        
    }

    @Override
    protected boolean handleLongClick() {
        
        return false;
    }
}
