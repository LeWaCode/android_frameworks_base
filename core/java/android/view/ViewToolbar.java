/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package android.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * The base class is used to support toolbar popped up form views.
 *
 */
public abstract class ViewToolbar {

    private static final int TOLERANCE_TOUCH = 15;
    private static final int TOOLBAR_ITEM_PADDING_LEFT_AND_RIGHT = 5;
    private static final int TOOLBAR_ITEM_PADDING_BOTTOM = 3;

    protected View mHostView;
    protected Context mContext;

    protected WindowManager mWindowManager;
    protected WindowManager.LayoutParams mLayoutParams = null;
    protected LayoutInflater mLayoutInflater;
    protected ViewGroup mToolbarGroup;
    protected View mToolbarView;
    protected ImageView mToolbarPositionArrowView;

    protected boolean mShowing = false;

    private Drawable mLeftDrawable;
    private int mCenterDrawableResId;
    private Drawable mRightDrawable;
    private Drawable mSingleDrawable;
    private Drawable mArrowAboveDrawable;
    private Drawable mArrowBelowDrawable;

    private int mStatusBarHeight;
    private int mToolbarPositionArrowWidth;
    private int mToolbarPositionArrowHeight;

    private int mPositionX, mPositionY;

    protected int mToleranceTouch;
    protected int mToolbarItemPaddingLeftAndRight;
    protected int mToolbarItemPaddingBottom;

    public ViewToolbar(View hostView) {
        this.mHostView = hostView;
        this.mContext = mHostView.getContext();

        // initial resources
        Resources resources = mHostView.getResources();
        mLeftDrawable = resources.getDrawable(com.android.internal.R.drawable.zzz_text_toolbar_left);
        mCenterDrawableResId = com.android.internal.R.drawable.zzz_text_toolbar_center;
        mRightDrawable = resources.getDrawable(com.android.internal.R.drawable.zzz_text_toolbar_right);
        mSingleDrawable = resources.getDrawable(com.android.internal.R.drawable.zzz_text_toolbar_single);
        mArrowAboveDrawable = resources.getDrawable(com.android.internal.R.drawable.zzz_text_toolbar_position_arrow_above);
        mArrowBelowDrawable = resources.getDrawable(com.android.internal.R.drawable.zzz_text_toolbar_position_arrow_below);
        // initial window manager
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        calculateTolerance();
        mStatusBarHeight = resources.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
        // initial tool bar and it's items.
        mLayoutInflater = LayoutInflater.from(mContext);
        mToolbarView = mLayoutInflater.inflate(com.android.internal.R.layout.zzz_text_toolbar, null);
        mToolbarGroup = (ViewGroup) mToolbarView.findViewById(com.android.internal.R.id.zzz_toolbar_group);
        mToolbarGroup.setPadding(2, 2, 2, 2);
        mToolbarPositionArrowView = (ImageView) mToolbarView.findViewById(com.android.internal.R.id.zzz_toolbar_position_arrow);
       
        // calculate initial size of tool bar.
        mToolbarView.measure(0, 0);
        mToolbarPositionArrowWidth = mToolbarPositionArrowView.getMeasuredWidth();
        mToolbarPositionArrowHeight = mToolbarPositionArrowView.getMeasuredHeight();
    }

    /**
     * @return Whether the toolbar is showing.
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Show toolbar at assigned position relative to left-top of screen.
     * @param screenX
     * @param screenY
     * @param selected
     */
    public void show(int screenX, int screenY, boolean selected) {
        if (!mShowing) {
            showInternal(screenX, screenY, 0, selected);
        }
    }

    /**
     * Move toolbar to assigned position relative to left-top of screen.
     * @param screenX
     * @param screenY
     */
    public void move(int screenX, int screenY, boolean selected) {
        if (mShowing) {
            moveInternal(screenX, screenY, 0, selected);
        }
    }

    /**
     * Hide the toolbar.
     */
    public void hide() {
        if (mShowing) {
            try {
                mToolbarGroup.setPadding(2, 2, 2, 2);
                mToolbarPositionArrowView.setPadding(0, 0, 0, 0);
                mWindowManager.removeViewImmediate(mToolbarView);
            } finally {
                // set showing flag whether hiding view is successful.
                mShowing = false;
            }
        }
    }

    /**
     * Update items of toolbar.
     */
    protected abstract void updateToolbarItems();

    protected void showInternal(int screenX, int screenY, int cursorLineHeight, boolean selected) {
        // update tool bar.
        update();
        if (mToolbarGroup.getChildCount() < 1) {
            hide();
            return;
        }
        prepare(screenX, screenY, cursorLineHeight, selected);
        // reposition the toolbar.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.token = mHostView.getWindowToken();
        lp.x = mPositionX;
        lp.y = mPositionY;
        lp.width = LayoutParams.WRAP_CONTENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        } else {
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED;
        }
        lp.packageName = mContext.getPackageName();
        mLayoutParams = lp;
        mWindowManager.addView(mToolbarView, lp);
        // set showing flag
        mShowing = true;
    }

    protected void moveInternal(int screenX, int screenY, int cursorLineHeight, boolean selected) {
        if (mToolbarGroup.getChildCount() < 1) {
            hide();
            return;
        }
        prepare(screenX, screenY, cursorLineHeight, selected);
        // reposition the toolbar.
        WindowManager.LayoutParams lp = mLayoutParams;
        lp.x = mPositionX;
        lp.y = mPositionY;
        mWindowManager.updateViewLayout(mToolbarView, lp);
    }

    private void prepare(int screenX, int screenY, int cursorLineHeight, boolean selected) {
        // calculate the size of tool bar.
        mToolbarView.measure(0, 0);
        // calculate the position of tool bar.
        boolean aboveCursor = calculatePosition(screenX, screenY, cursorLineHeight, selected);
        // set position of arrow representing the trigger point. 
        int paddingLeft = screenX - mPositionX - mToolbarPositionArrowWidth / 2;
        paddingLeft = Math.max(10, paddingLeft);
        paddingLeft = Math.min(mToolbarGroup.getMeasuredWidth() - mToolbarPositionArrowWidth - 10, paddingLeft);
        if (aboveCursor) {
            mToolbarPositionArrowView.setImageDrawable(mArrowBelowDrawable);
            mToolbarGroup.setPadding(2, 2, 2, 2);
            mToolbarPositionArrowView.setPadding(paddingLeft, mToolbarGroup.getMeasuredHeight() - 9, 0, 0);
        } else {
            mToolbarPositionArrowView.setImageDrawable(mArrowAboveDrawable);
            mToolbarGroup.setPadding(2, mToolbarPositionArrowHeight - 6, 2, 2);
            mToolbarPositionArrowView.setPadding(paddingLeft, 0, 0, 0);
        }
    }

    private void update() {
        updateToolbarItems();
        // set drawable of items.
        int childCount = mToolbarGroup.getChildCount();
        if (childCount >= 2) {
            for (int i = 0; i < childCount; i++) {
                View view = mToolbarGroup.getChildAt(i);
                if (i == 0) {
                    view.setBackgroundDrawable(mLeftDrawable);
                    view.setPadding(mToolbarItemPaddingLeftAndRight * 2, 0, mToolbarItemPaddingLeftAndRight, mToolbarItemPaddingBottom);
                } else if (i == childCount - 1) {
                    view.setBackgroundDrawable(mRightDrawable);
                    view.setPadding(mToolbarItemPaddingLeftAndRight, 0, mToolbarItemPaddingLeftAndRight * 2, mToolbarItemPaddingBottom);
                } else {
                    view.setBackgroundResource(mCenterDrawableResId);
                    view.setPadding(mToolbarItemPaddingLeftAndRight, 0, mToolbarItemPaddingLeftAndRight, mToolbarItemPaddingBottom);
                }
            }
        } else if (childCount == 1) {
            View view = mToolbarGroup.getChildAt(0);
            view.setBackgroundDrawable(mSingleDrawable);
            view.setPadding(mToolbarItemPaddingLeftAndRight * 2, 0, mToolbarItemPaddingLeftAndRight * 2, mToolbarItemPaddingBottom);
        }
    }

    private boolean calculatePosition(int screenX, int screenY, int cursorLineHeight, boolean selected) {
        boolean aboveCursor = true;
        // calculate x
        int px = screenX - mHostView.getRootView().getScrollX();
        int half = mToolbarGroup.getMeasuredWidth() / 2;
        int displayWidth = mWindowManager.getDefaultDisplay().getWidth();
        if (px + half < displayWidth) {
            mPositionX = px - half;
        } else {
            mPositionX = displayWidth - mToolbarGroup.getMeasuredWidth();
        }
        mPositionX = Math.max(0, mPositionX);
        // calculate y
        int py = screenY - mHostView.getRootView().getScrollY();
        int th = mToolbarGroup.getMeasuredHeight() + mToolbarPositionArrowHeight;
        int lh = cursorLineHeight / 2;
        if (py - th - lh < mStatusBarHeight) {
            mPositionY = py + lh + (selected ? mToleranceTouch : 0) + 2;
            aboveCursor = false;
        } else {
            mPositionY = py - th - lh - (selected ? mToleranceTouch : 0) + 6;
            aboveCursor = true;
        }
        return aboveCursor;
    }

    private void calculateTolerance() {
        DisplayMetrics dm = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(dm);
        float ratio = 1.0f * dm.densityDpi / DisplayMetrics.DENSITY_MEDIUM;
        mToleranceTouch = Math.round(TOLERANCE_TOUCH * ratio);
        mToolbarItemPaddingLeftAndRight = Math.round(TOOLBAR_ITEM_PADDING_LEFT_AND_RIGHT * ratio);
        mToolbarItemPaddingBottom = Math.round(TOOLBAR_ITEM_PADDING_BOTTOM * ratio);
    }

}
