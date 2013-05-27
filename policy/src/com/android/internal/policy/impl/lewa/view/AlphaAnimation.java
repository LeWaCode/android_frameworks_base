package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.util.Log;

public class AlphaAnimation extends BaseAnimation {

    public static final String INNER_TAG_NAME = "Alpha";
    public static final String TAG = "AlphaAnimation";
    private int mCurrentAlpha;
    private int a = 0;
    
    public AlphaAnimation(Element element) throws DomParseException{
        super(element, INNER_TAG_NAME);
    }
    
    @Override
    protected AnimationItem onCreateItem() {
        String[] attrs = new String[1];
        attrs[0] = "a";//Õ∏√˜∂»a Ù–‘
        return new BaseAnimation.AnimationItem(attrs);
    }
    
    @Override
    protected void onTick(AnimationItem prefixAI, AnimationItem suffixAI, float rate) {
        int prefixAIAlpha = 0;
        
        if(prefixAI == null){
            prefixAIAlpha = 255;
        }else {
            prefixAIAlpha = prefixAI.get(a);
        }
        
        mCurrentAlpha = (int) (prefixAIAlpha + (suffixAI.get(a) - prefixAIAlpha) * rate);
    }
    
    public final int getAlpha(){
        return mCurrentAlpha;
    }
}
