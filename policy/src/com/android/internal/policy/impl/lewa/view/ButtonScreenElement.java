package com.android.internal.policy.impl.lewa.view;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class ButtonScreenElement extends AnimatedScreenElement {

    private static final String TAG = "ButtonScreenElement";
    public static final String TAG_NAME = "Button";
    private ButtonActionListener mListener;
    private String mListenerName;
    
    private ElementGroup mNormalElements;
    private ElementGroup mPressedElements;
    
    private AnimatedScreenElement mParent;
    private boolean mPressed;
    
    private int mPreviousTapPositionX;
    private int mPreviousTapPositionY;
    private long mPreviousTapUpTime;
    private ArrayList<Trigger> mTriggers;
    
    private final String NORMAL = "Normal";
    private final String PRESSED = "Pressed";
    private final String TRIGGERS = "Triggers";
    private final String TRIGGER = "Trigger";
    
    public ButtonScreenElement(Element element,ScreenContext screenContext) throws DomParseException {
        super(element,screenContext);
       
        mTriggers = new ArrayList<Trigger>();
        mListenerName = element.getAttribute("listener");
        load(element);
        
    }

    public void load(Element element) throws DomParseException {

        if(element == null){
            Log.e(TAG, "node is null");
            throw new DomParseException("node is null");
        }
        /**
         * Normal指的是在正常状态下
         */
        Element normalElement = Utils.getChild(element, NORMAL);
        if(normalElement != null){
            mNormalElements = new ElementGroup(normalElement,screenContext);
        }
        
        Element pressedElement = Utils.getChild(element, PRESSED);
        if(pressedElement != null){
            mPressedElements = new ElementGroup(pressedElement,screenContext);
        }
        
        Element triggersElement = Utils.getChild(element, TRIGGERS);
        if(triggersElement == null){
            return;
        }
        NodeList nodeList = triggersElement.getChildNodes();
        
        int length = nodeList.getLength();
        
        for(int i=0;i<length;i++){
            if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE){
                Element triggerElement = (Element)nodeList.item(i);
                if(triggerElement.getNodeName().equals(TRIGGER)){
                    Trigger trigger = new Trigger(triggerElement);
                    mTriggers.add(trigger);
                }
            }
        }
    }
    
    private class Trigger {
   
        private ButtonAction mAction;
        private Property pro;
   
        public Trigger(Element element) throws DomParseException {
            mAction = ButtonAction.Invalid;
            load(element);
        }
        
        private void load(Element element) throws DomParseException {
        
            if(element == null){
                return;
            }
            
            String target = element.getAttribute("target");
            String property = element.getAttribute("property");
            String value = element.getAttribute("value");
            if(target.isEmpty()){
                throw new DomParseException("invalid trigger element");
            }
                
            if(property.equals("visibility")){
                pro = new VisibilityProperty(target, value);
            }
            if(pro == null){
                throw new DomParseException("invalid trigger element");
            }
                
            String action = element.getAttribute("action");
            if(action.equalsIgnoreCase("down")){
                mAction = ButtonAction.Down;
            } else if(action.equalsIgnoreCase("up")){
                mAction = ButtonAction.Up;
            } else if(action.equalsIgnoreCase("double")){
                mAction = ButtonAction.Double;
            } else if(action.equalsIgnoreCase("long")){
                mAction = ButtonAction.Long;
            } else{
                throw new DomParseException("invalid trigger action");
            }
        }

        public ButtonAction getAction(){
            return mAction;
        }

        public void perform(){
            pro.perform();
        }

        
    }
    
    private class VisibilityProperty extends Property {
    
        private boolean mIsShow;
        private boolean mIsToggle;
        
        protected VisibilityProperty(String target, String value){
            super(target);
            
            if(value.equalsIgnoreCase("toggle")){
                mIsToggle = true;
            }else if(value.equalsIgnoreCase("true")){
                mIsShow = true;
            }else{
                mIsShow = false;
            }
        }

        public void perform(){
        
            ScreenElement screenelement = getTarget();
            if(screenelement == null){
                return;
            }
            if(mIsToggle){
            
                boolean isShow = false;
                if(!screenelement.isVisible()){
                    Expression.putRealTimeVar("music_control", Expression.VISIBILITY, "true");
                    isShow = true;
                }else {
                    Expression.putRealTimeVar("music_control", Expression.VISIBILITY, "false");
                    isShow = false;
                }
                screenelement.show(isShow);
            } else{
                screenelement.show(mIsShow);
            }
        }
    }
    
    private abstract class Property {
    
        protected String mTarget;
        protected ScreenElement mTargetElement;

        protected Property(String target){
            mTarget = target;
        }

        protected ScreenElement getTarget(){
        
            if(mTarget != null && mTargetElement == null){
            
                ScreenElement screenelement = screenContext.mRoot;

                mTargetElement = screenelement.findElement(mTarget);

                if(mTargetElement == null){
                    Log.e(TAG, new StringBuilder().append("could not find trigger target, name: ").append(mTarget).toString());
                    mTarget = null;
                }
            }
            return mTargetElement;
        }

        public abstract void perform();
    }
    
    private static enum ButtonAction{
        Down,Up,Double,Long,Invalid
    }
    
    public static interface ButtonActionListener {
    
        public abstract boolean onButtonDoubleClick(String action);

        public abstract boolean onButtonDown(String action);

        public abstract boolean onButtonLongClick(String action);

        public abstract boolean onButtonUp(String action);
    }
    
    private ElementGroup getCurrentElementGroup(){
        if(mPressed && mPressedElements != null){
            return mPressedElements;
        }else{
            return mNormalElements;
        }
    }
    
    private void performAction(ButtonAction buttonaction){
    
        int size = mTriggers.size();
        for(int i=0;i<size;i++){
            Trigger trigger = mTriggers.get(i);
            if(trigger.getAction() == buttonaction){
                trigger.perform();
            }
        }
    }
    
    public void finish(){
    
        if(mNormalElements != null){
            mNormalElements.finish();
        }
        if(mPressedElements != null){
            mPressedElements.finish();
        } 
    }
    
    public void init(){
    
        if(mNormalElements != null){
            mNormalElements.init();
        }
        if(mPressedElements != null){
            mPressedElements.init();
        }
        if(mListener != null){
            return;
        }
        if(mListenerName.isEmpty()){
            return;
        }
        ScreenElement screenelement = screenContext.mRoot;
        ScreenElement btnListener = screenelement.findElement(mListenerName);
        try{
            mListener = (ButtonActionListener)btnListener;
        }catch(ClassCastException classcastexception){
            Log.e(TAG, new StringBuilder().append("button listener designated by the name is not actually a listener: ").append(mListenerName).toString());
        }
    }
    
    public void onTouch(MotionEvent motionevent){
        
        if(!isVisible()){
            return;
        }
        int x = (int)motionevent.getX();
        int y = (int)motionevent.getY();
        
        int action = motionevent.getActionMasked();
        switch(action){
            case MotionEvent.ACTION_DOWN:
            {
                if(!touched(x, y)){
                    return;
                }
                mPressed = true;
                if(mListener != null){
                    mListener.onButtonDown(mName);//mName is Button Name
                }
                performAction(ButtonAction.Down);
                
                long detlaTapUpTime = SystemClock.uptimeMillis() - mPreviousTapUpTime;
                // 双击超时阀值，仅在两次双击事件的间隔（第一次单击的UP事件和第二次单击的DOWN事件）小于此阀值，双击事件才能成立
                long doubleClickTimeOut = ViewConfiguration.getDoubleTapTimeout();
                if(detlaTapUpTime <= doubleClickTimeOut){
                    int detlaTapPositionX = x - mPreviousTapPositionX;
                    int detlaTapPositionY = y - mPreviousTapPositionY;
                    
                    int dist = detlaTapPositionX * detlaTapPositionX + detlaTapPositionY * detlaTapPositionY;
                    int i = ViewConfiguration.get(context).getScaledDoubleTapSlop();
                    int j = i * i;
                    
                    if(dist < j){
                        if(mListener != null){
                            mListener.onButtonDoubleClick(mName);
                        }
                        performAction(ButtonAction.Double);
                    }
                }
                mPreviousTapPositionX = x;
                mPreviousTapPositionY = y;
                break;
            }
            case MotionEvent.ACTION_MOVE:
            {
                mPressed = touched(x, y);
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                if(touched(x, y)){
                    if(mListener != null){
                        mListener.onButtonUp(mName);
                    }
                    
                    performAction(ButtonAction.Up);
                    mPreviousTapUpTime = SystemClock.uptimeMillis();
                }
                mPressed = false;
                break;
            }
        
        
        
        };
       
    }
    
    public void pause() {
   
        if(mNormalElements != null){
            mNormalElements.pause();
        }
            
        if(mPressedElements != null){
            mPressedElements.pause();
        } 
    }

    public void render(Canvas canvas){
        if(!isVisible()){
            return;
        }
        ElementGroup elementgroup = getCurrentElementGroup();
        
        if(elementgroup != null){
            elementgroup.render(canvas);
        } 
    }

    public void resume(){
    
        if(mNormalElements != null){
            mNormalElements.resume();
        }
            
        if(mPressedElements != null){
            mPressedElements.resume();
        } 
    }

    public void setListener(ButtonActionListener buttonactionlistener){
        mListener = buttonactionlistener;
    }

    public void setParent(AnimatedScreenElement animatedscreenelement){
        mParent = animatedscreenelement;
    }

   public void showCategory(String category, boolean flag){
    
        if(mNormalElements != null){
            mNormalElements.showCategory(category, flag);
        }
           
        if(mPressedElements != null){
            mPressedElements.showCategory(category, flag);
        } 
    }
    
    public void tick(long time){
    
        if(!isVisible()){
            return;
        }
        ElementGroup elementgroup = getCurrentElementGroup();
        if(elementgroup != null){
        
            elementgroup.tick(time);
        }
    }
    
    private boolean touched(int x, int y){

        int parentX = 0;
        int parentY = 0;
        
        if(mParent != null){
            parentX = mParent.getX();
            parentY = mParent.getY();
        }else {
            parentX = 0;
            parentY = 0;
        }
        
        int animX = mAni.getX();
        int animY = mAni.getY();
        
        int parent_anim_x = parentX + animX;
        if(x < parent_anim_x){
            return false;
        }
        
        int anim_width = mAni.getWidth();
        int parent_anim_x_width = parent_anim_x + anim_width;
        if(x > parent_anim_x_width){
            return false;
        }
        
        int parent_anim_y = parentY + animY;
        if(y < parent_anim_y){
            return false;
        }
        
        int anim_height = mAni.getHeight();
        int parent_anim_y_height = parent_anim_y + anim_height;
        if(y > parent_anim_y_height){
            return false;
        }
        return true;
        
    }
}
