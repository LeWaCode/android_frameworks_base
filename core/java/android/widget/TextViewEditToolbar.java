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

import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView.CursorController;

class TextViewEditToolbar extends TextViewToolbar {

    private static final int ID_SELECT_ALL = android.R.id.selectAll;
    private static final int ID_START_SELECTING_TEXT = android.R.id.startSelectingText;
    private static final int ID_CUT = android.R.id.cut;
    private static final int ID_COPY = android.R.id.copy;
    private static final int ID_SWITCH_INPUT_METHOD = android.R.id.switchInputMethod;

    private static final int ID_SELECT_ALL_STR = com.android.internal.R.string.selectAll;
    private static final int ID_START_SELECTING_TEXT_STR = com.android.internal.R.string.selectText;
    private static final int ID_CUT_STR = com.android.internal.R.string.cut;
    private static final int ID_COPY_STR = com.android.internal.R.string.copy;
    private static final int ID_SWITCH_INPUT_METHOD_STR = com.android.internal.R.string.inputMethod;

    private TextView mItemSelectAll;
    private TextView mItemStartSelect;
    private TextView mItemCut;
    private TextView mItemCopy;
    private TextView mItemInputMethod;

    private OnClickListener mOnClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (isShowing()) {
                if (mTextView instanceof ExtractEditText) {
                    mTextView.onTextContextMenuItem(v.getId());
                }
                onItemAction(v.getId());
                switch (v.getId()) {
                case ID_SELECT_ALL:
                case ID_START_SELECTING_TEXT:
                    hide();
                    show();
                    break;
                default:
                    hide();
                    break;
                }
            }
        }
    };

    TextViewEditToolbar(TextView hostView) {
        super(hostView);
        initToolbarItem();
    }

    protected void initToolbarItem() {
        super.initToolbarItem();
        mItemSelectAll = initToolbarItem(ID_SELECT_ALL, ID_SELECT_ALL_STR);
        mItemStartSelect = initToolbarItem(ID_START_SELECTING_TEXT, ID_START_SELECTING_TEXT_STR);
        mItemCopy = initToolbarItem(ID_COPY, ID_COPY_STR);
        mItemCut = initToolbarItem(ID_CUT, ID_CUT_STR);
        mItemInputMethod = initToolbarItem(ID_SWITCH_INPUT_METHOD, ID_SWITCH_INPUT_METHOD_STR);
    }

    protected OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    protected void updateToolbarItems() {
        mToolbarGroup.removeAllViews();
        // construct toolbar.
        if (mTextView.isInTextSelectionMode()) {
            if (mTextView.canCut()) {
                mToolbarGroup.addView(mItemCut);
            }
            if (mTextView.canCopy()) {
                mToolbarGroup.addView(mItemCopy);
            }
            if (mTextView.canPaste()) {
                mToolbarGroup.addView(mItemPaste);
            }
        } else {
            if (mTextView.canSelectText()) {
                if (!mTextView.hasPasswordTransformationMethod()) {
                    mToolbarGroup.addView(mItemStartSelect);
                }
                mToolbarGroup.addView(mItemSelectAll);
            }
            if (mTextView.canPaste()) {
                mToolbarGroup.addView(mItemPaste);
            }
            if (mTextView.isInputMethodTarget()) {
                mToolbarGroup.addView(mItemInputMethod);
            }
        }
    }

    private boolean onItemAction(int id) {
        CharSequence mText = mTextView.getText();
        CharSequence mTransformed = mTextView.getTransformed();

        int min = 0;
        int max = mText.length();
        if (mTextView.isFocused()) {
            final int selStart = mTextView.getSelectionStart();
            final int selEnd = mTextView.getSelectionEnd();
            min = Math.max(0, Math.min(selStart, selEnd));
            max = Math.max(0, Math.max(selStart, selEnd));
        }

        ClipboardManager clip = (ClipboardManager) mTextView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);

        CursorController selectionController = mTextView.getSelectionController();
        switch (id) {
        case ID_SELECT_ALL:
            Selection.setSelection((Spannable) mText, 0, mText.length());
            mTextView.startTextSelectionMode();
            if (selectionController != null) {
                selectionController.show();
            }
            return true;
        case ID_START_SELECTING_TEXT:
            mTextView.startTextSelectionMode();
            if (selectionController != null) {
                selectionController.show();
            }
            return true;
        case ID_CUT:
            int end = mTextView.getSelectionStart();
            clip.setText(mTransformed.subSequence(min, max));
            if (!(mTextView instanceof ExtractEditText)) {
                ((Editable) mText).delete(min, max);
            }
            mTextView.stopTextSelectionMode();
            if (mTextView instanceof ExtractEditText) {
                Selection.setSelection((Spannable) mTextView.getText(), end);
            }
            return true;
        case ID_COPY:
            clip.setText(mTransformed.subSequence(min, max));
            mTextView.stopTextSelectionMode();
            return true;
        case ID_PASTE:
            CharSequence paste = clip.getText();
            if (paste != null && paste.length() > 0) {
                Selection.setSelection((Spannable) mText, max);
                if (!(mTextView instanceof ExtractEditText)) {
                    ((Editable) mText).replace(min, max, paste);
                }
                mTextView.stopTextSelectionMode();
            }
            return true;
        case ID_SWITCH_INPUT_METHOD:
            if (!(mTextView instanceof ExtractEditText)) {
                InputMethodManager imm = InputMethodManager.peekInstance();
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
            return true;
        }
        return false;
    }

}
