package com.android.internal.policy.impl.lewa.view;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

public abstract class BaseAnimation {

    private static final String TAG = "BaseAnimation";
    /**
     *mAnimationItems数组列表中存放的都是一些动画的子节点，如Scale、Translate
     */
    protected ArrayList<AnimationItem> mAnimationItems;
    /**
     * 整个动画的持续时间
     */
    private long mTimeRange;
    private long mStartTime;
    
    protected Element element;
    
    
    /**
     * tagName is "Alpha"、"Scale"、"Translate"
     * @param element
     * @param tagName
     * @throws DomParseException
     */
    public BaseAnimation(Element element,String tagName) throws DomParseException{
        mAnimationItems = new ArrayList<AnimationItem>();
        this.element = element;
        load(element,tagName);
    }
    
    private void load(Element element,String tagName) throws DomParseException{
        
        mAnimationItems.clear();
        NodeList nodeList = element.getElementsByTagName(tagName);
        
        int length = nodeList.getLength();
        for(int i=0;i<length;i++){
            Element childElement = (Element) nodeList.item(i);
            AnimationItem animationItem = onCreateItem().load(childElement);
            mAnimationItems.add(animationItem);
        }
        
        boolean hasItems = false;
        
        if(mAnimationItems.size()>0){
            hasItems = true;
        }
        
        Utils.asserts(hasItems, "BaseAnimation: empty items");
        
        int size = mAnimationItems.size()-1;

        mTimeRange = (long)(mAnimationItems.get(size)).mTime;
    }
    
    public void init(){
        mStartTime = 0L;
    }
    
    protected abstract AnimationItem onCreateItem();
    
    protected abstract void onTick(AnimationItem prefixAI, AnimationItem suffixAI, float rate);
    
    public final void tick(long time){
        if(mStartTime == 0L){
            mStartTime = time;
        }
        long currentTime = (time - mStartTime) % mTimeRange;
        
        AnimationItem prefixAI = null;
        
        int size = mAnimationItems.size();
        for(int i=0;i<size;i++){
            AnimationItem suffixAI = mAnimationItems.get(i);
            long suffixItemTime = suffixAI.mTime;
            if(currentTime <= suffixItemTime){
                /**
                 * 相邻动画之间的时间差
                 */
                long deltaTwoAnimationsTime = 0;
                /**
                 * prefixAITime一个动画的时间
                 */
                long prefixAITime = 0;
                float rate = 0;
                if(i == 0){
                    deltaTwoAnimationsTime = suffixItemTime;
                }else {
                    /**
                     * 前一个动画的索引
                     */
                    int k = i-1;
                    prefixAI = mAnimationItems.get(k);
                    /**
                     * 相邻两个关键帧动画之间的时间差
                     */
                    deltaTwoAnimationsTime = suffixAI.mTime - prefixAI.mTime;
                    
                    prefixAITime = prefixAI.mTime;
                }
                
                if(deltaTwoAnimationsTime == 0){
                    rate = 1f;
                }else {
                    rate = (float)(currentTime - prefixAITime) /(float)(suffixAI.mTime - prefixAITime);
                }
                onTick(prefixAI, suffixAI, rate);
                break;
            }
        }
        
    }
    
    /**
     *BaseAnimation 是指的ScaleAnimation、TranslateAnimation、FramesAnimation等
     *AnimationItem 是指的上面那些动画的子节点:Scale、Translate、Frame等
     */
    public class AnimationItem{
        
        /**
         * 动画组成子部件（Scale、Translate等）的属性集合
         */
        private String[] mAttrs;
        
        /**
         * 不同的动画子部件都会有时间这一共同属性
         */
        public long mTime;
        
        private Expression expression;
        
        
        public AnimationItem(String[] attrs){
            mAttrs = attrs;
            expression = new Expression();
        }
        
        /**
         * 根据传入的索引获得相应索引位置属性的值,如：<Alpha a="0" time="0"/>,translate = 0,代表取a="0"处的值
         * @param translate 此AnimationItem第几个属性
         * @return
         */
        public int get(int index){
            int value = 0;

            if(index<0){
                Log.e(TAG, new StringBuilder().append("fail to get number in AnimationItem:").append(index).toString());
                value = 0;
            }else{
                int length = mAttrs.length;
                if(index<length && mAttrs != null){
                    
                   value = expression.getDou(mAttrs[index], 0d).intValue();
                } 
            }
            return value;
        }
        
        /**
         * 此Element为Position、Size等
         */
        public AnimationItem load(Element element) throws DomParseException{
            
            try {
                mTime = Long.valueOf(element.getAttribute("time"));
            } catch (NumberFormatException e) {
                Log.e(TAG, "fail to get time attribute");
                throw new DomParseException("fail to get time attribute");
            }
            
            if(mAttrs != null){
                int length = mAttrs.length;
                for(int i = 0; i < length; i++){
                    String expValue = element.getAttribute(mAttrs[i]);
                    if(!expValue.trim().equals("")){
                        expression.putDou(mAttrs[i], expValue);
                    }
                }

            }
            
            return this;
        }
    }
    
    
    
}
