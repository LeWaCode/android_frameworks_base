package com.android.internal.policy.impl.lewa.view;


import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class TextScreenElement extends AnimatedScreenElement {

    private static final String TAG = "TextScreenElement";
    private static final int DEFAULT_SIZE = 18;
    private static final int PADDING = 50;
    public static final String TAG_NAME = "Text";
    private int mColor;
    
    protected Variable mFormatVar;
    private float mMarqueePos;
    private int mMarqueeSpeed;
    private Paint mPaint;
    private long mPreviousTime;
    private int mSize;
    
    /**
     * Text的显示内容与变量有关，如：format="正在充电 - %d%%" paras="#battery_level"
     */
    protected String mFormat;
    
    /**
     * Text的显示内容直接为常量，如：text="已充满"
     */
    private String mText;
    
    private String paras;
    
    /**
     * 用于保存上一次变量的值
     */
    private int mLastPara = -1;
    private String mLastText = "";
    
    
    public TextScreenElement(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
        
        mPaint = new Paint();
        mMarqueePos = 3.402823E+038F;
        load(element);
    }
    
    public void load(Element element) throws DomParseException {

        if(element == null){
            Log.e(TAG, "node is null");
            throw new DomParseException("node is null");
        }
        
        mText = element.getAttribute("text");
        paras = element.getAttribute("paras");
        
        mFormat = element.getAttribute("format");
       
        try {
            mColor = Color.parseColor(element.getAttribute("color"));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "The color is wrong, mColor == " + mColor);
            mColor = 0xFFFF0000;
        }
        
        mSize = Utils.getAttrAsInt(element, "size", DEFAULT_SIZE);
        mMarqueeSpeed = Utils.getAttrAsInt(element, "marqueeSpeed", 0);
        
        
        mPaint.setTextAlign(mAlign);
        mPaint.setColor(mColor);
        mPaint.setTextSize(mSize);
        mPaint.setAntiAlias(true);
        boolean bold = Boolean.parseBoolean(element.getAttribute("bold"));
        mPaint.setFakeBoldText(bold);
        mAni = new AnimatedElement(element,screenContext);
    }
    
    
    public void render(Canvas canvas){
        if(!isVisible()){
            return;
        }
        int alpha = mAni.getAlpha();
        if(alpha <= 0){
            return;
        }
        
        String text = getText();
        if(text == null){
            return;
        }
        
        mPaint.setAlpha(alpha);
        
        int width = mAni.getWidth();
        
        int x = mAni.getX();
        int y = mAni.getY();
        
        float angle = mAni.getRotationAngle();
        
        float centerX = mAni.getCenterX();
        float centerY = mAni.getCenterY();
        
        float textSize = mPaint.getTextSize();
        
        canvas.save();
        
        float x_centerX = x + centerX;
        float y_centerY = y + centerY;
        
        canvas.rotate(angle, x_centerX, y_centerY);
        
        if(width > 0){
            int left = getLeft(x, width);
            float f5 = y - 10;
            float total_width = left + width;
            float f7 = (float)y + textSize + 20F;
            canvas.clipRect(left, f5, total_width, f7);
        }
        if(mMarqueeSpeed == 0 || width == 0){
            float f9 = (float)y + textSize;
            canvas.drawText(text, x, f9, mPaint);
        } else{
        
            float marqueePos = 0f;
            if(mMarqueePos == 3.402823E+038F){
                marqueePos = 0f;
            }else{
                marqueePos = mMarqueePos;
            }
            float f12 = x + marqueePos;
            float f13 = (float)y + textSize;
            canvas.drawText(text, f12, f13, mPaint);
        }
        canvas.restore();
    }
    

    public void setText(String text){
        mText = text;
        mFormat = "";
    }
    
    protected String getText(){
        
        String text = "";
        String format = "";
        
        if(mText.trim().equals("")){
            if(!mFormat.trim().equals("")){
                int para = 0;
                if(!paras.trim().equals("")){
                    expression.putDou("paras", paras);
                    para = expression.getDou("paras", 0d).intValue();
                }
                if(mLastPara != para){
                    format = mFormat;
                    if(mFormat.indexOf("%d%%") != -1){
                        format = format.replaceAll("%d%%", para + "%");
                        
                    }
                    if(format.indexOf("%d") != -1){
                        format = format.replaceAll("%d", String.valueOf(para));
                        
                    }
                    mLastPara = para;
                    mLastText = format;
                }
                text = mLastText;
            }
        }else{
            text = mText;
        }
        return text;
    }
    

}
