package com.android.internal.policy.impl.lewa.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;


public class ImagesInOne {

    private Bitmap mBitmap;
    private int mCount;
    private int mOneWidth;
    private Rect mDst;
    private Rect mSrc;
    
    public ImagesInOne(Bitmap bitmap, int width){
    
        mSrc = new Rect();
        mDst = new Rect();
        mBitmap = bitmap;
        mOneWidth = width;
        int bmpWidth = mBitmap.getWidth();
        mCount = bmpWidth / mOneWidth;
        if(bmpWidth % mOneWidth == 0){
            return;
        }else {
            throw new IllegalArgumentException("invalid width");
        }
    }

    public void draw(Canvas canvas, int width, int height, int index, Paint paint){
    
        int l = mCount - 1;
        if(index > l){
            throw new IllegalArgumentException("invalid index");
        } else {
            int totalWidth = mOneWidth + width;
            int totalHeight = mBitmap.getHeight() + height;
            mDst.set(width, height, totalWidth, totalHeight);
            int allWidth = mOneWidth * index;
            int widths = mOneWidth + allWidth;
            int bmpHeight = mBitmap.getHeight();
            mSrc.set(allWidth, 0, widths, bmpHeight);
            canvas.drawBitmap(mBitmap, mSrc, mDst, paint);
            return;
        }
    }
}
