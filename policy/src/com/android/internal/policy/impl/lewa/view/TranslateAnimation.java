package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.util.Log;

public class TranslateAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Translate";
    public static final String TAG = "TranslateAnimation";
    protected int mCurrentX;
    protected int mCurrentY;
    private int x = 0;
    private int y = 1;
    
    public TranslateAnimation(Element element, String tagName) throws DomParseException {
        super(element, tagName);
    }
    
    public TranslateAnimation(Element element) throws DomParseException {
        super(element, "Translate");
    }

    @Override
    protected AnimationItem onCreateItem() {
        String[] attrs = new String[2];
        attrs[0] = "x";
        attrs[1] = "y";
        return new BaseAnimation.AnimationItem(attrs);
    }

    @Override
    protected void onTick(AnimationItem prefixAI, AnimationItem suffixAI, float rate) {
        /**
         * prefixAIX
         */
        int prefixAIX = 0;
        if(prefixAI == null){
            prefixAIX = 0;
        }else {
            prefixAIX = prefixAI.get(x);
        }
 
        mCurrentX = (int) (prefixAIX + (suffixAI.get(x) - prefixAIX) * rate);
        
        /**
         * prefixAIY
         */
        int prefixAIY = 0;
        if(prefixAI == null){
            prefixAIY = 0;
        }else {
            prefixAIY = prefixAI.get(y);
        }
        
        mCurrentY = (int) (prefixAIY + (suffixAI.get(y) - prefixAIY) * rate);
    }
    
    public final int getX(){
        return mCurrentX;
    }

    public final int getY(){
        return mCurrentY;
    }

}
