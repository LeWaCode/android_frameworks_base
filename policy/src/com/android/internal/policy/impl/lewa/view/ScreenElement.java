package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public abstract class ScreenElement {

    private static final String TAG = "ScreenElement";
    
    protected Align mAlign;
    protected AlignV mAlignV;
    protected String mCategory;
    protected String mName;
    protected ScreenContext screenContext;
    protected Context context;
    
    private boolean mShow = true;
    
    private String showStr;
    
    protected Expression expression;
    
    protected Element element;
    
    public ScreenElement(Element element,ScreenContext screenContext){
        this.screenContext = screenContext;
        this.context = screenContext.mContext;
        if(element == null){
            return;
        }
        this.element = element;
        expression = new Expression();
        
        mCategory = element.getAttribute("category");
        mName = element.getAttribute("name");
        
        showStr = element.getAttribute("visibility");
        
        if(!showStr.isEmpty()){
            if(showStr.equalsIgnoreCase("false")){
                mShow = false;
            } else if(showStr.equalsIgnoreCase("true")){
                mShow = true;
            } else{
                expression.putStr("visibility", showStr);
                showStr = "unknow";
            }
        }
        mAlign = Align.LEFT;

        String alignStr = element.getAttribute("align");
        if(!alignStr.equalsIgnoreCase("right")){
            if(alignStr.equalsIgnoreCase("left")){
                mAlign = Align.LEFT;
            } else if(alignStr.equalsIgnoreCase("center")){
                mAlign = Align.CENTER;
            }
        }else{
            mAlign = Align.RIGHT;
        }

        mAlignV = AlignV.TOP;
        String alignVStr = element.getAttribute("alignV");
        if(alignVStr.equalsIgnoreCase("bottom")){
            mAlignV = AlignV.BOTTOM;
        }else if(alignVStr.equalsIgnoreCase("center")){
            mAlignV = AlignV.CENTER;
        }else{
            mAlignV = AlignV.TOP;
        }

    }
    
    protected static enum AlignV{
        TOP,CENTER,BOTTOM
    }
    
    public ScreenElement findElement(String name){
        if(mName.equals(name)){
            return this;
        }else{
            return null;
        } 
    }

    public void finish(){
        element = null;
        expression = null;
        context = null;
        screenContext = null;
    }
    
    public String getName(){
        return mName;
    }
    
    public void init(){
    
    }

    protected int getLeft(int animX, int bmpWidth){
        if(bmpWidth <= 0){
            return animX;
        }else{
            switch (mAlign) {
            case LEFT:
                return animX;
            case CENTER:
                return animX - bmpWidth/2;
            case RIGHT:
                return animX - bmpWidth;
            default:
                break;
            }
        }
        return animX;
    }
    
    protected int getTop(int animY, int bmpHeight){
       if(bmpHeight <= 0){
            return animY;
        }else{
            switch (mAlignV) {
            case TOP:
                return animY;
            case CENTER:
                return animY - bmpHeight/2;
            case BOTTOM:
                return animY - bmpHeight;
            default:
                break;
            }
        }
        return animY;
    }
    
    public boolean isVisible(){
       
        if(!showStr.isEmpty()){
           
            if(showStr.equals("unknow")){
                
                String visibilityExp = expression.getStr("visibility");
                if(visibilityExp != null){
                    if(visibilityExp.equalsIgnoreCase("false")){
                        return false;
                    }else if(visibilityExp.equalsIgnoreCase("true")){
                        return true;
                    }else{ 
                        /**not(#music_control.visibility)
                         * 如果走这里，那么就意味着此部件的visibility属性为"取非"表达式
                         * 取非表达式支持布尔值，也支持int类型数值
                         * 可能的表达式形式：not(#music_control.visibility)、not(true)、not(1)
                         */
                        
                        String name = null;
                        String value = null;
                        String subStr = null;
                        int position = visibilityExp.indexOf("#");
                        int position_last_quot = visibilityExp.lastIndexOf("'");
                        if(position != -1){ //如果不等于-1，那么说明此表达示中有变量表达式
                            int position_point = visibilityExp.indexOf(".");
                            name = visibilityExp.substring(position+1,position_point);
                            value = visibilityExp.substring(position_point+1,position_last_quot);
                            
                            String visible = Expression.getRealTimeVar(name, value, "false");
                            if(visible.equals("true")){
                                return false;
                            }else {
                                return true;
                            }
                            
                        }else{
                            int position_first_quot = visibilityExp.indexOf("'");
                            subStr = visibilityExp.substring(position_first_quot, position_last_quot);
                            try {
                                int number = Integer.valueOf(subStr);
                                if(number >= 1 ){
                                    return false;
                                }else{
                                    return true;
                                }
                            } catch (Exception e) {
                                return true;
                            }
                            
                        }
                    } 
                }else {
                    return true;
                }
                
            }else {
                return mShow;
            }
            
            
        }
        
        if(!mCategory.isEmpty()){
            if(mCategory.equals(Expression.get("battery_category", ""))){
                return true;
            }else {
                return false;
            }
        }
        return mShow;
    }

    public void onTouch(MotionEvent motionevent){
    
    }

    public void pause(){
    
    }

    public abstract void render(Canvas canvas);

    public void resume(){
    
    }

    public void setShouldUpdate(boolean shouldUpdate){
    
        screenContext.mShouldUpdate = shouldUpdate;
    }

    public void setView(View view){
        screenContext.mView = view;
    }

    public boolean shouldUpdate(){
        return screenContext.mShouldUpdate;
    }

    public void show(boolean isShow){
        mShow = isShow;
    }

   public void showCategory(String category, boolean isShow){
    
        if(mCategory.equals(category)){
            show(isShow);
        }
        
    }

    public abstract void tick(long time);
}
