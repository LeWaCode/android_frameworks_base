package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

public class PopuViewFrame extends LinearLayout{

	PopuViewForNotifications mPopuViewForNotifications;
	
	public PopuViewFrame(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public PopuViewFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// TODO Auto-generated method stub
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (getKeyDispatcherState() == null) {
                return super.dispatchKeyEvent(event);
            }

            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0) {
                getKeyDispatcherState().startTracking(event, this);
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP
                    && getKeyDispatcherState().isTracking(event) && !event.isCanceled()) {
            	mPopuViewForNotifications.onfinish();
            	return true;
            }
            return super.dispatchKeyEvent(event);
        } else {
            return super.dispatchKeyEvent(event);
        }
	}
	
	public void setPopuViewForNotifications(PopuViewForNotifications pfn) {
		mPopuViewForNotifications = pfn;
	}

}
