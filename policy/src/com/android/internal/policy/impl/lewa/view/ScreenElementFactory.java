package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;


public class ScreenElementFactory {

    public ScreenElementFactory(){}

    public ScreenElement createInstance(Element element, ScreenContext screencontext)throws DomParseException{
    
        String tagName = element.getTagName();
        ScreenElement screenElement = null;
        if(tagName.equalsIgnoreCase("Image")){
            screenElement = new ImageScreenElement(element, screencontext);
        } else if(tagName.equalsIgnoreCase("Time")){
            screenElement = new TimepanelScreenElement(element, screencontext);
        }else if(tagName.equalsIgnoreCase("NumberIndicator")){
            screenElement = new NumberIndicatorScreenElement(element, screencontext);
        }else if(tagName.equalsIgnoreCase("Text")){
            screenElement = new TextScreenElement(element, screencontext);
        }else if(tagName.equalsIgnoreCase("DateTime")){
            screenElement = new DateTimeScreenElement(element, screencontext);
        }else if(tagName.equalsIgnoreCase("Button")){
            screenElement = new ButtonScreenElement(element, screencontext);
        }else if(tagName.equalsIgnoreCase("MusicControl")){
            screenElement = new MusicControlScreenElement(element, screencontext);
        }else {
            screenElement = null;
        }
        return screenElement;
    }
}
