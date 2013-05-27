package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarService;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.KeyEvent;
import android.graphics.PixelFormat;
import android.view.WindowManager;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class SwitchStyleButton extends ObserveButton{
	
    public SwitchStyleButton() {
        super();
        mType = BUTTON_SWITCH_WIDGET_STYLE;
        mObservedUris.add(Settings.System.getUriFor(Settings.System.SWITCH_WIDGET_STYLE));
        mLabel=R.string.title_toggle_switchwidgetstyle;
    }

    @Override
    protected void updateState() {
        if (getState() == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES) {
            mIcon = R.drawable.switch_widget_more;
            mState = STATE_ENABLED;
        } else {
            mIcon = R.drawable.switch_widget_more;
            mState = STATE_DISABLED;
        }
    }

    @Override
    protected void toggleState() {
       /* int newState;
        if (getState() == SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES) {
            newState = SwitchWidget.SWITCH_WIDGET_STYLE_SINGLE_PAGE;
        } else {
            newState = SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES;
        }
        Settings.System.putInt(sContext.getContentResolver(), Settings.System.SWITCH_WIDGET_STYLE, newState);*/
    	Intent intent=new Intent(StatusBarService.ACTION_COLLAPSE_ViEW);
    	sContext.sendBroadcast(intent);
    	
    	startActivity("com.android.settings.switchwidgetsettings");
        Log.e(TAG,"toggleState");
    }

    @Override
    protected boolean onLongClick() {
/*
 *         SwitchWidgetOptionsView options
 *                 = (SwitchWidgetOptionsView) View.inflate(
 *                 sContext, R.layout.switch_buttons_order, null);
 * 
 *         Dialog dialog = new OptionsDialog(sContext);
 * 
 *         WindowManager.LayoutParams lp;
 *         int pixelFormat = PixelFormat.RGBX_8888;
 * 
 *         lp = dialog.getWindow().getAttributes();
 *         lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
 *         lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
 *         lp.x = 0;
 *         lp.y = 0;
 *         // lp.y = (mBottomBar ? disph : -disph); // sufficiently large positive
 *         lp.type = WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL;
 *         lp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
 *                 | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
 *                 | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
 *                 | WindowManager.LayoutParams.FLAG_DITHER
 *                 | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
 *         lp.format = pixelFormat;
 *         lp.gravity = Gravity.TOP | Gravity.FILL;
 *         lp.setTitle("StatusBarExpanded_SwitchWidgetOptions");
 *         dialog.getWindow().setAttributes(lp);
 *         dialog.getWindow().setFormat(pixelFormat);
 * 
 *         dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
 *         dialog.setContentView(options,
 *                 new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
 *                 , ViewGroup.LayoutParams.MATCH_PARENT));
 *         dialog.getWindow().setBackgroundDrawable(null);
 *         dialog.show();
 */
        // SwitchWidgetOptionsView.show(sContext);
        startActivity("com.android.settings.switchwidgetsettings");
        return false;
    }

/*
 *     private class OptionsDialog extends Dialog {
 *         OptionsDialog(Context context) {
 *             super(context, com.android.internal.R.style.Theme_Light_NoTitleBar);
 *         }
 * 
 *         @Override
 *         public boolean dispatchKeyEvent(KeyEvent event) {
 *             boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
 *             switch (event.getKeyCode()) {
 *             case KeyEvent.KEYCODE_BACK:
 *                 if (!down) {
 *                     // animateCollapse();
 *                     dismiss();
 *                 }
 *                 return true;
 *             }
 *             return super.dispatchKeyEvent(event);
 *         }
 *     }
 */

    private int getState() {
        return Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.SWITCH_WIDGET_STYLE, SwitchWidget.SWITCH_WIDGET_STYLE_DUAL_PAGES);
    }
}
