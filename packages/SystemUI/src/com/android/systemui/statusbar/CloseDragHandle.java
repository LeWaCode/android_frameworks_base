/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

//added by zhangbo
import com.android.systemui.R;
import android.graphics.Rect;
import android.view.View;


public class CloseDragHandle extends LinearLayout {
    StatusBarService mService;

    public CloseDragHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Ensure that, if there is no target under us to receive the touch,
     * that we process it ourself.  This makes sure that onInterceptTouchEvent()
     * is always called for the entire gesture.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            mService.interceptTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        /*return mService.interceptTouchEvent(event)
                ? true : super.onInterceptTouchEvent(event);*/
        //modified by zhangbo
        float xf = event.getRawX();
        float yf = event.getRawY();
        int statusBarSize = mService.getStatusBarHeight();
        Rect closeOnRect = new Rect();
        boolean IsIncloseOn = false;
        View closeOn = this.findViewById(R.id.close_on);
        if (closeOn != null) {
            closeOn.getGlobalVisibleRect(closeOnRect);
            if (closeOnRect.contains((int) xf, ((int) yf - statusBarSize))) {
                IsIncloseOn = true;
            }
        }
        if (IsIncloseOn) {
            return mService.interceptTouchEvent(event)
                ? true : super.onInterceptTouchEvent(event);
        } else {
            return super.onInterceptTouchEvent(event);
        }
    }
}

