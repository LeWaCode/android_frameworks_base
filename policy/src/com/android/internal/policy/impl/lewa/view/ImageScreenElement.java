package com.android.internal.policy.impl.lewa.view;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.provider.Settings;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;

public class ImageScreenElement extends AnimatedScreenElement {

    private static final String TAG = "ImageScreenElement";
    public static final String MASK_TAG_NAME = "Mask";
    public static final String TAG_NAME = "Image";
    private boolean mAntiAlias;
    private Bitmap mImageScreenBitmap;
    private Rect mDesRect;
    private Paint mMaskPaint;
    private Paint mPaint;
    private ArrayList<AnimatedElement> mMasks;
    
    
    private Bitmap mMaskBitmap;
    private Canvas mMaskCanvas;
    
    private String mBitmapName;

    private boolean firstTimeStart = false;
    private boolean isLewaIndexPNG = false;
    
    
    public ImageScreenElement(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
        mDesRect = new Rect();
        
        mPaint = new Paint();
        /**mAntiAlias = Boolean.parseBoolean(element.getAttribute("antiAlias"));
        mPaint.setFilterBitmap(mAntiAlias);*/
        mPaint.setFilterBitmap(true);
        mPaint.setAntiAlias(true);
        
        mMaskPaint = new Paint();
        PorterDuffXfermode porterduffxfermode = new PorterDuffXfermode(Mode.DST_IN);
        mMaskPaint.setXfermode(porterduffxfermode);

	isLewaIndexPNG = element.getAttribute("src").equals("lewa_index.png");
	if(isLewaIndexPNG){
		firstTimeStart = (Settings.System.getInt(context.getContentResolver(),
    							Settings.System.LOCKSCREEN_FIRST_TIME_UNLOCK_PROMPT, 0) == 0);
		if(firstTimeStart){
			Settings.System.putInt(context.getContentResolver(),
            	Settings.System.LOCKSCREEN_FIRST_TIME_UNLOCK_PROMPT,1);
		}
	}
		
		
        load(element);
    }
    
    public void load(Element element) throws DomParseException {
        if(element == null){
            Log.e(TAG, "node is null");
            throw new DomParseException("node is null");
        } else{
            loadMask(element);
        }
    }
    
    private void loadMask(Element element) throws DomParseException {
        if(mMasks == null){
            mMasks = new ArrayList<AnimatedElement>();
        }
        mMasks.clear();
        
        NodeList nodelist = element.getElementsByTagName("Mask");
        
        int length = nodelist.getLength();
        for(int i=0;i<length;i++){
            Element childElement = (Element) nodelist.item(i);
            AnimatedElement animatedElement = new AnimatedElement(childElement,screenContext);
            mMasks.add(animatedElement);
        }
        
    }
    
    private void renderWithMask(Canvas canvas, AnimatedElement animatedelement){
    
        canvas.save();
        
        float rotationAngle = animatedelement.getRotationAngle();
        
        float centerX = animatedelement.getCenterX();
        float centerY = animatedelement.getCenterY();
        
        canvas.rotate(rotationAngle, centerX, centerY);
        
        String bitmapName = animatedelement.getBitmapName();
        
        Bitmap bitmap = ResourceManager.getBitmapFromCache(bitmapName);
        int x = animatedelement.getX();
        int animX = 0;
        if(animatedelement.isAlignAbsolute()){
            animX = mAni.getX();
        }else{
            animX = 0;
        }
        
        float deltaX = x - animX;
        
        
        int y  = animatedelement.getY();
        int animY = 0;
        if(animatedelement.isAlignAbsolute()){
            animY = mAni.getY();
        }else{
            animY = 0;
        }
        
        float deltaY = y - animY;
        
        
        canvas.drawBitmap(bitmap, deltaX, deltaY, mMaskPaint);
        
        canvas.restore();
    }
    
    public void init(){
        super.init();
        if(mMasks == null){
            return;
        }
        int size = mMasks.size();
        for(int i=0;i<size;i++){
            mMasks.get(i).init();
        }
    }
    
    protected Bitmap getImageScreenBitmap(){
        String bitmapname = mAni.getBitmapName();
        if(mBitmapName != null && bitmapname.equals(mBitmapName)){
            return mImageScreenBitmap;
        }else {
            mImageScreenBitmap = ResourceManager.getBitmapFromCache(bitmapname);
            mBitmapName = bitmapname;
            return mImageScreenBitmap;
        }
        
    }

    public void finish(){
        if(mMasks != null){
            mMasks.clear();
            mMasks = null;
        }
    }
    
    public void setImageScreenBitmap(Bitmap bitmap){
        mImageScreenBitmap = bitmap;
    }
    
    public void tick(long time) {
        
        if(!isVisible()){
            return;
        }
        
        super.tick(time);
        
        if(mMasks == null){
            return;
        }
        int size = mMasks.size();
        for(int i=0;i<size;i++){
            ((AnimatedElement)mMasks.get(i)).tick(time);
        }
    }
    
    public void render(Canvas canvas){
		
		
	if(!firstTimeStart && isLewaIndexPNG){
            return;
        }
        
        if(!isVisible()){
            return;
        }
       
        int alpha = mAni.getAlpha();
        
        if(alpha <= 0){
            return;
        }
        
        Bitmap bitmap = getImageScreenBitmap();
       
        if(bitmap == null){
            return;
        }
        mPaint.setAlpha(alpha);
        
        int width = mAni.getWidth();
        if(width < 0){
            width = bitmap.getWidth();
        }
         
        int height = mAni.getHeight();
        if(height < 0){
            height = bitmap.getHeight();
        }
        
        if(mMasks.size() == 0){
           
            canvas.save();
            
            float rotationAngle = mAni.getRotationAngle();
            
            int x = mAni.getX();
            
            float centerX = mAni.getCenterX();
            
            float anim_X_CenterX = x + centerX;
            
            int y = mAni.getY();
            
            float centerY = mAni.getCenterY();
            
            float anim_Y_CenterY = y + centerY;
            
            canvas.rotate(rotationAngle, anim_X_CenterX, anim_Y_CenterY);
            
            int left = getLeft(x, width);
            int left_width = left + width;
            
            int top = getTop(y, height);
            int top_height = top + height;
            
            if(bitmap.getNinePatchChunk() != null){
                String bitmapName = mAni.getBitmapName();
                NinePatch ninepatch = ResourceManager.getNinePatch(bitmapName);
                if(ninepatch != null){
                    mDesRect.set(x, y, x+width, y+height);
                    ninepatch.draw(canvas, mDesRect, mPaint);
                }else{
                    Log.e(TAG, new StringBuilder().append("the image contains ninepatch chunk but couldn't get NinePatch object: ").append(mAni.getBitmapName()).toString());
                }
            }else if(mAni.getWidth() > 0 || mAni.getHeight() > 0){
                mDesRect.set(left, top, left_width, top_height);
                canvas.drawBitmap(bitmap, null, mDesRect, mPaint);
            }else {
                
                canvas.drawBitmap(bitmap, x, y, mPaint);
            }
            
            canvas.restore();
        }else {
            int animMaxWidth = mAni.getMaxWidth();
            if(animMaxWidth < 0){
                animMaxWidth = bitmap.getWidth();
            }
           
            int animMaxHeight = mAni.getMaxHeight();
            if(animMaxHeight < 0){
                animMaxHeight = bitmap.getHeight();
            }
            
            if(mMaskBitmap == null){
                mMaskBitmap = ResourceManager.getMaskBufferBitmap(animMaxWidth, animMaxHeight);
            }
            if(mMaskCanvas == null){
                mMaskCanvas = new Canvas(mMaskBitmap);
                
            }
            mMaskCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
            
            if(width > 0 || height > 0){
                mDesRect.set(0, 0, width, height);
                mMaskCanvas.drawBitmap(bitmap, null, mDesRect, mPaint);
            }else {
                mMaskCanvas.drawBitmap(bitmap, 0f, 0f, null);
            }
            
            int size = mMasks.size();
            for(int i=0;i<size;i++){
                renderWithMask(mMaskCanvas, mMasks.get(i));
            }
            canvas.save();
            
            float animRotationAngle = mAni.getRotationAngle();
            
            int animX = mAni.getX();
            float animCenterX = mAni.getCenterX();
            float anim_X_CenterX = animX + animCenterX;
            
            int animY = mAni.getY();
            float animCenterY = mAni.getCenterY();
            float anim_Y_CenterY = animY + animCenterY;
            
            canvas.rotate(animRotationAngle, anim_X_CenterX, anim_Y_CenterY);
       
            mPaint.setAlpha(alpha);
            canvas.drawBitmap(mMaskBitmap, getLeft(animX, width), getTop(animY, height), mPaint);
            canvas.restore();
            
        }
    }
    
}
