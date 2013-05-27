package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.util.Log;

public class ScaleAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Scale";
    public static final String TAG = "ScaleAnimation";
    private int mCurrentH;
    private int mCurrentW;
    private int mMaxH;
    private int mMaxW;
    
    
    public ScaleAnimation(Element element) throws DomParseException {
        super(element, "Scale");
        
        int size = mAnimationItems.size();
        for(int i=0;i<size;i++){
            AnimationItem animationItem = mAnimationItems.get(i);
            int w = animationItem.get(0);
            
            if(w > mMaxW){
                mMaxW = w;
            }
            
            int h = animationItem.get(1);
            if(h > mMaxH){
                mMaxH = h;
            }
        }
        
    }

    @Override
    protected AnimationItem onCreateItem() {
        String[] attrs = new String[2];
        attrs[0] = "w";
        attrs[1] = "h";
        return new BaseAnimation.AnimationItem(attrs);
    }

    @Override
    protected void onTick(AnimationItem prefixAI, AnimationItem suffixAI, float rate) {
        int prefixAIW = 0;
        int prefixAIH = 0;
        
        if(prefixAI == null){
            prefixAIW = 0;
        }else {
            prefixAIW = prefixAI.get(0);
        }
        
        mCurrentW = (int) (prefixAIW + (suffixAI.get(0) - prefixAIW) * rate);
        
        if(prefixAI == null){
            prefixAIH = 0;
        }else {
            prefixAIH = prefixAI.get(1);
        }
        
        mCurrentH = (int) (prefixAIH + (suffixAI.get(1) - prefixAIH) * rate);
        
    }

    public final int getHeight(){
        return mCurrentH;
    }

    public final int getMaxHeight(){
        return mMaxH;
    }

    public final int getMaxWidth(){
        return mMaxW;
    }

    public final int getWidth(){
        return mCurrentW;
    }
}
