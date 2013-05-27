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

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;

class TextViewHelper {

    // select paragraph of EditText at (x, y)
    public static void selectParagraph(TextView widget, float wx, float wy) {
        String text = widget.getText().toString();
        int offset =  getOffset(widget, wx, wy);
        int offset1 = offset;
        int offset2 = offset;
        if (offset < text.length()) {
            char c = text.charAt(offset);
            if (c == '\n') {
                offset1--;
            }
        }
        int index = text.lastIndexOf('\n', offset1);
        int start = index == -1 ? 0 : index + 1;
        index = text.indexOf('\n', offset2);
        int stop = index == -1 ? text.length() : index;
        Selection.setSelection((Spannable) widget.getText(), start, stop);
    }

    // return line number of view coordinator (x,y)
    public static int getLineNumber(TextView widget, float wy) {
        Layout layout = widget.getLayout();
        return layout.getLineForVertical(Math.round(getVertical(widget, wy)));
    }

    // return text offset of view coordinator (x,y)
    public static int getOffsetByLine(TextView widget, int line, float wx) {
        Layout layout = widget.getLayout();
        return layout.getOffsetForHorizontal(line, getHorizontal(widget, wx));
    }

    // return text offset of view coordinator (x,y)
    public static int getOffset(TextView widget, float wx, float wy) {
        int line = getLineNumber(widget, wy);
        return getOffsetByLine(widget, line, wx);
    }

    // return line text context of view coordinator (x,y)
    public static CharSequence getLineText(TextView widget, float wy) {
        int line = getLineNumber(widget, wy);
        Layout layout = widget.getLayout();
        int start = layout.getLineStart(line);
        int end = layout.getLineEnd(line);
        return layout.getText().subSequence(start, end);
    }

    private static float getHorizontal(TextView widget, float wx) {
        // Converts the absolute X,Y coordinates to the character offset for the
        // character whose position is closest to the specified
        // horizontal position.
        float x = wx - widget.getTotalPaddingLeft();
        // Clamp the position to inside of the view.
        if (x < 0) {
            x = 0;
        } else if (x >= (widget.getWidth() - widget.getTotalPaddingRight())) {
            x = widget.getWidth() - widget.getTotalPaddingRight() - 1;
        }
        //
        x += widget.getScrollX();
        return x;
    }

    private static float getVertical(TextView widget, float wy) {
        // Converts the absolute X,Y coordinates to the character offset for the
        // character whose position is closest to the specified
        // horizontal position.
        float y = wy - widget.getTotalPaddingTop();
        // Clamp the position to inside of the view.
        if (y < 0) {
            y = 0;
        } else if (y >= (widget.getHeight() - widget.getTotalPaddingBottom())) {
            y = widget.getHeight() - widget.getTotalPaddingBottom() - 1;
        }
        //
        y += widget.getScrollY();
        return y;
    }

}
