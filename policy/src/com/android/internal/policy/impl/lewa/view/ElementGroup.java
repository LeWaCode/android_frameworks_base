package com.android.internal.policy.impl.lewa.view;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;

public class ElementGroup extends AnimatedScreenElement {

    private static final String TAG = "ElementGroup";
    protected ArrayList<ScreenElement> mElements;
    
    public ElementGroup(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
        mElements = new ArrayList<ScreenElement>();
        load(element);
    }

    public void load(Element element) throws DomParseException {
        
        if(element == null){
        
            Log.e("LockScreen_ElementGroup", "node is null");
            throw new DomParseException("node is null");
        }
        
        ScreenElementFactory screenelementfactory = screenContext.mFactory;
        
        NodeList nodelist = element.getChildNodes();
        int length = nodelist.getLength();
        
        for(int i=0;i<length;i++){
            Node item = nodelist.item(i);
            if(item.getNodeType() == Node.ELEMENT_NODE){
                Element childElement = (Element) item;
                
                ScreenElement screenElement = screenelementfactory.createInstance(childElement, screenContext);
                if(screenElement != null){
                    mElements.add(screenElement);
                }else{
                    Log.e(TAG,new StringBuilder().append("unrecognized element: ").append(childElement.getNodeName()).toString());
                }
            }
        }
    }
    
    public ScreenElement findElement(String name) {
    
        ScreenElement screenelement = super.findElement(name);

        if(screenelement != null){
            return screenelement;
        }

        int size = mElements.size();
        for(int i=0;i<size;i++){
            screenelement = (ScreenElement)mElements.get(i).findElement(name);
            if(screenelement != null){
                return screenelement;
            }
        
        }
        return null;
    }

    public void finish(){
        super.finish();
        
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).finish();
        }
        mElements.clear();
    }

    public void init(){
        super.init();

        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).init();
        }
    }

    public void onTouch(MotionEvent motionevent) {
    
        if(!isVisible()){
            return;
        }
        super.onTouch(motionevent);
        int size = mElements.size();
        for(int i=0;i<size;i++){

            ((ScreenElement)mElements.get(i)).onTouch(motionevent);
        }
    }

    public void pause(){
    
        super.pause();

        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).pause();
        }
        
    }

    public void render(Canvas canvas){
        
        if(!isVisible()){
            return;
        }
        if(mAni.getAlpha() <= 0){
            return;
        }
        
        int x = getX();
        int y = getY();
        
        int count = canvas.save();
        canvas.translate(x, y);
        
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).render(canvas);
        }
        canvas.restoreToCount(count);
    }

    public void resume(){
    
        super.resume();
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).resume();
        }
    }

    public void showCategory(String category, boolean isShow){
    
        super.showCategory(category, isShow);
        
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).showCategory(category, isShow);
            
        }
    }

    public void tick(long time){
    
        if(!isVisible())
            return;
        super.tick(time);
        
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ((ScreenElement)mElements.get(i)).tick(time);
        }
    }
    
    
}
