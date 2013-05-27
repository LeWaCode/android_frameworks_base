package com.android.internal.policy.impl.lewa.lockscreen;

import org.w3c.dom.Element;

import com.android.internal.policy.impl.lewa.view.ScreenContext;
import com.android.internal.policy.impl.lewa.view.ScreenElement;
import com.android.internal.policy.impl.lewa.view.ScreenElementFactory;
import com.android.internal.policy.impl.lewa.view.DomParseException;



public class LockScreenElementFactory extends ScreenElementFactory {

    private final LockScreenRoot.UnlockerCallback mUnlockerCallback;
    private final UnlockerListener mUnlockerListener;
    
    public LockScreenElementFactory(LockScreenRoot.UnlockerCallback unlockercallback, UnlockerListener unlockerlistener){
    
        mUnlockerCallback = unlockercallback;
        mUnlockerListener = unlockerlistener;
    }

    public ScreenElement createInstance(Element element, ScreenContext screencontext) throws DomParseException{
    
        ScreenElement screenElement = null;
        if(element.getTagName().equalsIgnoreCase("Unlocker")){
            screenElement = new UnlockerScreenElement(element, screencontext, mUnlockerCallback, mUnlockerListener);
        } else{
            screenElement = super.createInstance(element, screencontext);
        }
        return screenElement;
    }
}
