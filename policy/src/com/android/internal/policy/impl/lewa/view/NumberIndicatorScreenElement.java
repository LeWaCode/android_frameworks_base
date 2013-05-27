package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class NumberIndicatorScreenElement extends AnimatedScreenElement {

    private static final String TAG = "NumberIndicatorScreenElement";
    public static final String TAG_NAME = "NumberIndicator";
    private ImagesInOne mImages;
    /**
     * 每个数字的宽度
     */
    private int mNumberWidth;
    private Paint mPaint;
    private boolean mShowZero;
    
    public NumberIndicatorScreenElement(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
        mPaint = new Paint();
        load(element);
    }
    
    public void load(Element element) throws DomParseException {

        if(element == null){
            Log.e(TAG, "node is null");
            throw new DomParseException("node is null");
        } else {
            /*mNumExpression = Expression.build(element.getAttribute("number"));
            mNumberWidth = Utils.getAttrAsIntThrows(element, "numberWidth");
            mShowZero = Boolean.parseBoolean(element.getAttribute("showZero"));
            ResourceManager resourcemanager = mContext.mResourceManager;
            String bitmapName = mAni.getBitmap();
            android.graphics.Bitmap bitmap = resourcemanager.getBitmap(bitmapName);
            mImages = new ImagesInOne(bitmap, mNumberWidth);*/
        }
    }

    public void render(Canvas canvas){
    
        if(!isVisible()){
            return;
        }
        int alpha = mAni.getAlpha();
        if(alpha <= 0){
            return;
        }
        int num = 0;
        if(num < 0){
            return;
        }
        if(num == 0 && !mShowZero){
            return;
        }
        String numStr = String.valueOf(num);
        int animX = mAni.getX();
        
        mPaint.setAlpha(alpha);
        
        int length = numStr.length();
        int animY = mAni.getY();
        
        for(int i=0;i<length;i++){
            
            int c = numStr.charAt(i) - 48;
            mImages.draw(canvas, animX, animY, c, mPaint);
            animX += mNumberWidth;
        }
        
    }

}
