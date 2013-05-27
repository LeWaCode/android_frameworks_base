package com.android.internal.policy.impl.lewa.lockscreen;

import org.w3c.dom.Element;

import com.android.internal.policy.impl.lewa.view.ElementGroup;
import com.android.internal.policy.impl.lewa.view.ScreenContext;
import com.android.internal.policy.impl.lewa.view.ScreenElement;
import com.android.internal.policy.impl.lewa.view.DomParseException;


public class LockScreenElementGroup extends ElementGroup implements UnlockerListener{

    public LockScreenElementGroup(Element element, ScreenContext screenContext) throws DomParseException{
        super(element, screenContext);
    }

    public void endUnlockMoving(UnlockerScreenElement unlockerscreenelement){
    
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ScreenElement screenElement = mElements.get(i);
            if(screenElement instanceof UnlockerListener){
                ((UnlockerListener) screenElement).endUnlockMoving(unlockerscreenelement);
            }
        }
        
    }
    
    public void startUnlockMoving(UnlockerScreenElement unlockerscreenelement) {
   
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ScreenElement screenElement = mElements.get(i);
            if(screenElement instanceof UnlockerListener){
                ((UnlockerListener) screenElement).startUnlockMoving(unlockerscreenelement);
            }
        }
    }
}
