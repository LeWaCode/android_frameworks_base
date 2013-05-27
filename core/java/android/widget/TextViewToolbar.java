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

package android.widget;

import android.graphics.Color;
import android.text.Layout;
import android.view.Gravity;
import android.view.ViewToolbar;
import android.view.View.OnClickListener;

abstract class TextViewToolbar extends ViewToolbar {

    protected static final int ID_PASTE = android.R.id.paste;

    protected static final int ID_PASTE_STR = com.android.internal.R.string.paste;

    protected TextView mItemPaste;

    private int mScreenX;
    private int mScreenY;
    private int mLineHeight;

    protected TextView mTextView;

    TextViewToolbar(TextView hostView) {
        super(hostView);
        this.mTextView = hostView;        
    }

    protected void initToolbarItem() {
        // init past view
        mItemPaste = initToolbarItem(ID_PASTE, ID_PASTE_STR);
    }

    void show() {
        if (!mShowing) {
            calculateScreenPosition();
            int start = mTextView.getSelectionStart();
            int end = mTextView.getSelectionEnd();
            showInternal(mScreenX, mScreenY, mLineHeight, start != end);
        }
    }

    void move() {
        if (mShowing) {
            calculateScreenPosition();
            int start = mTextView.getSelectionStart();
            int end = mTextView.getSelectionEnd();
            moveInternal(mScreenX, mScreenY, mLineHeight, start != end);
        }
    }

    private void calculateScreenPosition() {
        int[] location = new int[2];
        mTextView.getLocationOnScreen(location);
        int start = mTextView.getSelectionStart();
        int end = mTextView.getSelectionEnd();
        Layout layout = mTextView.getLayout();
        if (layout == null ) {
        	mTextView.assumeLayout();
        	layout = mTextView.getLayout();
        }
        int line = layout.getLineForOffset(start);
        int top = layout.getLineTop(line);
        int bottom = layout.getLineBottom(line);
        mLineHeight = bottom - top;
        mScreenY = top + mLineHeight / 2 + location[1] + mTextView.getTotalPaddingTop() - mTextView.getScrollY();
        if (start == end) {
            mScreenX = Math.round(layout.getPrimaryHorizontal(start)) + location[0] + mTextView.getTotalPaddingLeft() - mTextView.getScrollX();
        } else {
            int left = Math.round(layout.getPrimaryHorizontal(start));
            int right;
            int lineEnd = layout.getLineForOffset(end);
            if (line == lineEnd) {
                right = Math.round(layout.getPrimaryHorizontal(end));
            } else {
                right = Math.round(layout.getLineRight(line));
            }
            mScreenX = (left + right) / 2 + location[0] + mTextView.getTotalPaddingLeft() - mTextView.getScrollX();
        }
        mScreenY = Math.max(location[1], mScreenY);
    }

    protected TextView initToolbarItem(int id, int textResId) {
        TextView textView = new TextView(mContext);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Color.WHITE);
        textView.setId(id);
        textView.setPadding(mToolbarItemPaddingLeftAndRight, 0, mToolbarItemPaddingLeftAndRight, 0);
        textView.setText(textResId);
        textView.setOnClickListener(getOnClickListener());
        return textView;
    }

    protected abstract OnClickListener getOnClickListener();

}