package com.android.systemui.statusbar.switchwidget;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.android.systemui.R;
import com.android.internal.app.ShutdownThread;

public class RebootButton extends StatelessButton
{
    private String mRebootReason;

    public RebootButton() {
        super();
        mType = BUTTON_REBOOT;
        mIcon = R.drawable.stat_reboot;
        mRebootReason = null;
        mLabel=R.string.title_toggle_reboot;
    }

    @Override
    protected void onClick() {
        // reboot(true);
        final AlertDialog dialog = new AlertDialog.Builder(sContext)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(com.android.internal.R.string.reboot_system)
                .setSingleChoiceItems(com.android.internal.R.array.shutdown_reboot_options
                        , 0, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < 0)
                            return;

                        String actions[] = sContext.getResources().getStringArray(
                                com.android.internal.R.array.shutdown_reboot_actions);

                        if (actions != null && which < actions.length)
                            mRebootReason = actions[which];
                    }
                })
                .setPositiveButton(com.android.internal.R.string.yes
                        , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PowerManager pm = (PowerManager)
                                sContext.getSystemService(Context.POWER_SERVICE);
                        if (null != pm) {
                            pm.reboot(mRebootReason);
                        }
                    }
                })
                .setNegativeButton(com.android.internal.R.string.no
                        , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.cancel();
                        }
                        return true;
                    }
                });

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        if (!sContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }

        dialog.show();
    }

    @Override
    protected boolean onLongClick() {
        // reboot(false);
/*        PowerManager pm = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
        if (null != pm) {
            pm.reboot(null);
        }*/
        return true;
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Start the shutdown thread to reboot the device
     * @param confirm determines whether or not to show the confirmation dialog
     */
    private void reboot(final boolean confirm) {
        Handler h = new Handler();
        h.post(new Runnable() {
            public void run() {
                synchronized (this) {
                    ShutdownThread.reboot(sContext, null, confirm);
                }
            }
        });
    }
}
