package com.android.systemui.statusbar.switchwidget;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.WindowManager;


import com.android.internal.app.ShutdownThread;
import com.android.systemui.R;

public class ShutdownButton extends StatelessButton
{
    public ShutdownButton() {
        super();
        mType = BUTTON_SHUTDOWN;
        mIcon = R.drawable.stat_poweroff;
        mLabel=R.string.title_toggle_shutdown;
    }

    @Override
    protected void onClick() {
        // shutdown(true);
        final Context context = mView.getContext(); 
        final AlertDialog dialog = new AlertDialog.Builder(sContext)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(com.android.internal.R.string.power_off)
                .setMessage(com.android.internal.R.string.shutdown_confirm)
                .setPositiveButton(com.android.internal.R.string.yes
                        , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        /*PowerManager pm = (PowerManager)
                                sContext.getSystemService(Context.POWER_SERVICE);
                        if (null != pm) {
                        	  //changed by zhuyaopeng
	                          pm.shutdown();
                        }*/
                     Intent shutdown = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);  
                                    shutdown.putExtra(Intent.EXTRA_KEY_CONFIRM, false);  
                                    shutdown.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
                                    context.startActivity(shutdown);
                    }
                })
                .setNegativeButton(com.android.internal.R.string.no, null)
                .create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        if (!sContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }

        dialog.show();
    }

    @Override
    protected boolean onLongClick() {
        // shutdown(false);
        PowerManager pm = (PowerManager) sContext.getSystemService(Context.POWER_SERVICE);
        if (null != pm) {
//            pm.shutdown();
        }
        return true;
    }

    /**
     * @author: Woody Guo <guozhenjiang@ndoo.net>
     * @description: Start the shutdown thread to turn off the device
     * @param confirm determines whether to ask for user's confirmation of the shutdown request
     */
    private void shutdown(final boolean confirm) {
/*
 *         Runnable runnable = new Runnable() {
 *             public void run() {
 *                 synchronized (this) {
 *                     ShutdownThread.shutdown(sContext, false);
 *                 }
 *             }
 *         };
 *         // ShutdownThread must run on a looper capable of displaying the UI
 *         mHandler.post(runnable);
 * 
 *         synchronized (runnable) {
 *             while (true) {
 *                 try {
 *                     runnable.wait();
 *                 } catch (InterruptedException e) {
 *                 }
 *             }
 *         }
 */
    }
}
