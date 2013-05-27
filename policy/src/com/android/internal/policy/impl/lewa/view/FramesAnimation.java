package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.util.Log;

public class FramesAnimation extends TranslateAnimation {

    public static final String TAG = "FramesAnimation";
    private String mCurrentBitmap;
    
    public FramesAnimation(Element element) throws DomParseException {
        super(element,"Frame");
    }

    @Override
    protected AnimationItem onCreateItem() {
        String[] attrs = new String[2];
        attrs[0] = "x";
        attrs[1] = "y";
        return new Frame(attrs);
    }

    @Override
    protected void onTick(AnimationItem prefixAI, AnimationItem suffixAI, float rate) {
        // TODO Auto-generated method stub
        mCurrentX = suffixAI.get(0);
        mCurrentY = suffixAI.get(1);
        mCurrentBitmap = ((Frame)suffixAI).mSrc;
    }

    public final String getSrc(){
        return mCurrentBitmap;
    }
    
    public class Frame extends BaseAnimation.AnimationItem {
    
        public static final String TAG_NAME = "Frame";
        public String mSrc;
        
        public Frame(String attrs[]){
        
            super(attrs);
        }
        
        public BaseAnimation.AnimationItem load(Element element) throws DomParseException {
        
            mSrc = element.getAttribute("src");
            return super.load(element);
        }

    }
}
