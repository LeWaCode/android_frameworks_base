package com.android.systemui.statusbar.powerwidget;

import com.android.systemui.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.android.internal.app.ShutdownThread;
import android.os.PowerManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;



public class RebootButton extends PowerButton {
    public RebootButton() { mType = BUTTON_REBOOT; }

    @Override
    protected void updateState() {
        mIcon = R.drawable.stat_reboot;
        mState = STATE_DISABLED;
    }

    @Override
    protected void toggleState() {
		
        final Context context = mView.getContext();

        final AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle(R.string.reboot)
                                    .setMessage(R.string.reboot_confirm)
                                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent reboot = new Intent(Intent.ACTION_REBOOT);  
                                            reboot.putExtra("nowait", 1);  
                                            reboot.putExtra("interval", 1);  
                                            reboot.putExtra("window", 0); 
                                            context.sendBroadcast(reboot);
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
