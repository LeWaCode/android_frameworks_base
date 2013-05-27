package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.graphics.Canvas;
import android.util.Log;

public abstract class AnimatedScreenElement extends ScreenElement {

    private static final String TAG = "AnimatedScreenElement";
    
    protected AnimatedElement mAni;
    
    public AnimatedScreenElement(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
        mAni = new AnimatedElement(element,screenContext);
    }

    public int getX(){
        return mAni.getX();
    }

    public int getY(){
        return mAni.getY();
    }

    public void init(){
        mAni.init();
    }

    public void finish(){
        super.finish();
        if(mAni != null){
            mAni.finish();
            mAni = null;
        }
        
    }

    public void tick(long time){
        if(!isVisible()){
            return;
        }
        mAni.tick(time);
        if(mName.isEmpty()){
            return;
        } else{
            
            Expression.put("actual_x", String.valueOf(mAni.getX()));
            Expression.put("actual_y", String.valueOf(mAni.getY()));
        }
    }

}
