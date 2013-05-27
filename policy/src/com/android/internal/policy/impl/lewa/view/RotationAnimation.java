package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.util.Log;

public class RotationAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Rotation";
    public static final String TAG = "RotationAnimation";
    private int mCurrentAngle;
    
    
    public RotationAnimation(Element element) throws DomParseException {
        super(element, "Rotation");
    }

    @Override
    protected AnimationItem onCreateItem() {
        String[] attrs = new String[1];
        attrs[0] = "angle";//旋转角度angle属性
        return new BaseAnimation.AnimationItem(attrs);
    }

    @Override
    protected void onTick(AnimationItem prefixAI, AnimationItem suffixAI, float rate) {
        int prefixAIAngle = 0;
        
        if(prefixAI == null){
            prefixAIAngle = 0;
        }else {
            prefixAIAngle = prefixAI.get(0);
        }
        mCurrentAngle = (int) (prefixAIAngle + (suffixAI.get(0) - prefixAIAngle) * rate);
    }
    
    public final int getAngle(){
        return mCurrentAngle;
    }

}
