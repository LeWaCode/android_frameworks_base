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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

/**
 * The helper class which makes views support magnifier easily.
 *
 */
public class ViewMagnifier {

    private static final int TOLERANCE_TOUCH = 10;

    private View mHostView;
    private Context mContext;

    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mLayoutParams = null;
    private MagnifierView mMagnifierView = null;

    protected int mWidth;
    protected int mHeight;
    protected float mScale = 1.2f;

    protected int mScreenX;
    protected int mScreenY;

    protected boolean mShowing = false;

    private int mPositionX = 0;
    private int mPositionY = 0;
    private float mDrawingX = 0;
    private float mDrawingY = 0;

    public ViewMagnifier(View hostView) {
        this.mHostView = hostView;
        this.mContext = mHostView.getContext();

        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int displayWidth = mWindowManager.getDefaultDisplay().getWidth();
        int displayHeight = mWindowManager.getDefaultDisplay().getHeight();
        int x = Math.min(displayWidth, displayHeight);
        this.mWidth = Math.round(x * 0.4f);
        this.mHeight = mWidth;
        this.mMagnifierView = new MagnifierView(mContext);
    }

    /**
     * @return The width of magnifier.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Set the width of magnifier.
     * @param width
     */
    public void setWidth(int width) {
        this.mWidth = width;
    }

    /**
     * @return The height of magnifier.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Set the height of magnifier.
     * @param height
     */
    public void setHeight(int height) {
        this.mHeight = height;
    }

    /**
     * @return The scale of magnifier content.
     */
    public float getScale() {
        return mScale;
    }

    /**
     * Set the scale of magnifier content.
     * @param scale
     */
    public void setScale(float scale) {
        this.mScale = scale;
    }

    /**
     * @return Whether the magnifier is showing.
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Show magnifier at assigned position relative to left-top of screen.
     * @param screenX
     * @param screenY
     */
    public void show(int screenX, int screenY) {
        show(screenX, screenY, true);
    }

    /**
     * Show magnifier at assigned position relative to left-top of screen.
     * @param screenX
     * @param screenY
     * @param animate Whether enable animation or not.
     */
    public void show(int screenX, int screenY, boolean animate) {
        if (!mShowing) {
            this.mScreenX = screenX;
            this.mScreenY = screenY;
            // calculate the position and drawing translation of magnifier.
            calculatePosition(screenX, screenY);
            calculateDrawingPosition(screenX, screenY);
            // show
            showInternal(animate);
        }
    }

    /**
     * Move magnifier to assigned position relative to left-top of screen.
     * @param screenX
     * @param screenY
     */
    public void move(int screenX, int screenY) {
        if (mShowing) {
            if (mScreenX == screenX && mScreenY == screenY) {
                return;
            }
            this.mScreenX = screenX;
            this.mScreenY = screenY;
            // calculate the position and drawing translation of magnifier.
            calculatePosition(screenX, screenY);
            calculateDrawingPosition(screenX, screenY);
            // move
            moveInternal();
        }
    }

    /**
     * Hide magnifier.
     */
    public void hide() {
        if (mShowing) {
            try {
                mWindowManager.removeViewImmediate(mMagnifierView);
            } finally {
                // set showing flag whether hiding view is successful.
                mShowing = false;
            }
        }
    }

    /**
     * Destroy magnifier.
     */
    public void destroy() {
        Bitmap bitmap = mMagnifierView.mMagnifierBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    protected void showInternal(boolean animate) {
        // set parameters containing position and size.
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.token = mHostView.getWindowToken();
        lp.x = mPositionX;
        lp.y = mPositionY;
        lp.width = mWidth;
        lp.height = mHeight;
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.type = WindowManager.LayoutParams.TYPE_TOP_MOST;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lp.packageName = mContext.getPackageName();
        if (animate) {
            lp.windowAnimations = com.android.internal.R.style.zzz_text_magnifier_popup;
        }
        mLayoutParams = lp;
        mWindowManager.addView(mMagnifierView, lp);
        // set showing flag
        mShowing = true;
    }

    protected void moveInternal() {
        // reposition the magnifier.
        WindowManager.LayoutParams lp = mLayoutParams;
        lp.x = mPositionX;
        lp.y = mPositionY;
        mWindowManager.updateViewLayout(mMagnifierView, lp);
        // redraw the content of magnifier.
        mMagnifierView.invalidate();
    }

    protected void calculatePosition(int screenX, int screenY) {
        mPositionX = screenX - mWidth / 2;
        mPositionY = screenY - mHeight - TOLERANCE_TOUCH;
        mPositionY = Math.max(mPositionY, -mHeight / 3);
    }

    protected void calculateDrawingPosition(int screenX, int screenY) {
        int[] location = new int[2];
        mHostView.getRootView().getLocationOnScreen(location);
        mDrawingX = screenX - location[0] - mWidth / 2f;
        mDrawingY = screenY - location[1] - mHeight / 2f;
    }

    protected class MagnifierView extends View {
        private static final int BORDER_WIDTH = 4;

        private Bitmap mMagnifierBitmap;
        private Paint mOutlinePaint = new Paint();
        private RectF mOutlineRect = null;
        private Path mClipPath = new Path();

        private Bitmap mBitmap;
        private int mBitmapWidth;
        private int mBitmapHeight;
        private Canvas mBitmapCanvas;
        private Paint mBitmapPaint;

        private Paint mPaint;

        protected MagnifierView(Context context) {
            super(context);
            init();
        }

        private void init() {
            Drawable drawable = mContext.getResources().getDrawable(com.android.internal.R.drawable.zzz_text_magnifier);
            mMagnifierBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mMagnifierBitmap);
            drawable.setBounds(0, 0, mWidth, mHeight);
            drawable.draw(canvas);

            mOutlinePaint.setAntiAlias(true);
            mOutlinePaint.setColor(Color.DKGRAY);
            mOutlinePaint.setStrokeWidth(BORDER_WIDTH);
            mOutlinePaint.setStyle(Style.STROKE);
            mOutlineRect = new RectF(BORDER_WIDTH / 2, BORDER_WIDTH / 2, mWidth - BORDER_WIDTH, mHeight - BORDER_WIDTH);
            mClipPath.addOval(mOutlineRect, Path.Direction.CW);

            mBitmapWidth = Math.round(mWidth / mScale);
            mBitmapHeight = Math.round(mHeight / mScale);
            mBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
            mBitmapCanvas = new Canvas(mBitmap);
            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
            mBitmapPaint.setDither(true);
            mBitmapPaint.setFilterBitmap(true);
            mBitmapPaint.setColor(Color.BLACK);
            mBitmapPaint.setStyle(Style.FILL);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setFilterBitmap(true);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(mWidth, mHeight);
        }

        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            mBitmapCanvas.drawRect(new RectF(0, 0, mBitmapWidth, mBitmapHeight), mBitmapPaint);
            mBitmapCanvas.save();
            mBitmapCanvas.translate(-mDrawingX, -mDrawingY);
            mHostView.getRootView().draw(mBitmapCanvas);
            mBitmapCanvas.restore();

            canvas.save();
            canvas.clipPath(mClipPath);
            canvas.scale(mScale, mScale);
            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
            canvas.restore();

            canvas.drawBitmap(mMagnifierBitmap, 0, 0, null);
        }
    }

}
