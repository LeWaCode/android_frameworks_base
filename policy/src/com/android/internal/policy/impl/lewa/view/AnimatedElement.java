package com.android.internal.policy.impl.lewa.view;

import java.util.ArrayList;

import org.w3c.dom.Element;

import android.util.Log;

public class AnimatedElement {
    
    private static final String TAG = "AnimatedElement";
    
    private ArrayList<BaseAnimation> mAnimations;
    protected String mSrc;
    private boolean mAlignAbsolute;
    
    private AlphaAnimation mAlphas;
    private TranslateAnimation mTranslates;
    private RotationAnimation mRotations;
    private ScaleAnimation mScales;
    private FramesAnimation mFrames;
    
    private String mAlpha;
    protected String mAngle;
    protected String mBaseX;
    protected String mBaseY;
    protected String mCenterX;
    protected String mCenterY;
    protected String mSrcId;
    protected String mWidth;
    protected String mHeight;
    protected String mSrcExpression;

    private Expression expression;
    
    private Element element;
    
    public AnimatedElement(Element element,ScreenContext screenContext) throws DomParseException {
        mAnimations = new ArrayList<BaseAnimation>();
        this.element = element;
        expression = new Expression();
        load(element);
    }
    
    public void load(Element element) throws DomParseException {

        if(element == null){
            Log.e(TAG, "node is null");
            throw new DomParseException("node is null");
        }
        
        mBaseX = element.getAttribute("x");
        if(!mBaseX.isEmpty()){
            expression.putDou("x", mBaseX);
        }

        mBaseY = element.getAttribute("y");
       
        if(!mBaseY.isEmpty()){
            expression.putDou("y", mBaseY);
        }
    
        mWidth = element.getAttribute("w");
        if(!mWidth.isEmpty()){
            expression.putDou("w", mWidth);
        }
    
        mHeight = element.getAttribute("h");
        if(!mHeight.isEmpty()){
            expression.putDou("h", mHeight);
        }
    
        mAngle = element.getAttribute("angle");
        if(!mAngle.isEmpty()){
            expression.putDou("angle", mAngle);
        }
    
        mCenterX = element.getAttribute("centerX");
        if(!mCenterX.isEmpty()){
            expression.putDou("centerX", mCenterX);
        }
    
        mCenterY = element.getAttribute("centerY");
        if(!mCenterY.isEmpty()){
            expression.putDou("centerY", mCenterY);
        }
        
        mAlpha = element.getAttribute("alpha");
        if(!mAlpha.isEmpty()){
            expression.putDou("alpha", mAlpha);
        }
        
        mSrcId = element.getAttribute("srcid");
        if(!mSrcId.isEmpty()){
            expression.putDou("srcId", mSrcId);
        }
    
        mSrc = element.getAttribute("src");
        
        if(element.getAttribute("align").equalsIgnoreCase("absolute")){
            mAlignAbsolute = true;
        }
           
        loadFrameAnimations(element);
        loadTranslateAnimations(element);
        loadRotationAnimations(element);
        loadScaleAnimations(element);
        loadAlphaAnimations(element);
    }

    private void loadFrameAnimations(Element element) throws DomParseException {
        
        Element childElement = Utils.getChild(element, "FramesAnimation");
        if(childElement != null){
            mFrames = new FramesAnimation(childElement);
            mAnimations.add(mFrames);
        }
    }
    
    private void loadTranslateAnimations(Element element) throws DomParseException {
        
        Element childElement = Utils.getChild(element, "TranslateAnimation");
        if(childElement != null){
            mTranslates = new TranslateAnimation(childElement);
            mAnimations.add(mTranslates);
        } 
    }

    private void loadRotationAnimations(Element element)throws DomParseException {
    
        Element childElement = Utils.getChild(element, "RotationAnimation");
        if(childElement != null){
            mRotations = new RotationAnimation(childElement);
            mAnimations.add(mRotations);
        } 
    }
    
    private void loadScaleAnimations(Element element) throws DomParseException {
    
        Element childElement = Utils.getChild(element, "ScaleAnimation");
        if(childElement != null){
            mScales = new ScaleAnimation(childElement);
            mAnimations.add(mScales);
        } 
    }
    
    private void loadAlphaAnimations(Element element)throws DomParseException {
    
        Element childElement = Utils.getChild(element, "AlphaAnimation");
        if(childElement != null){
            mAlphas = new AlphaAnimation(childElement);
            mAnimations.add(mAlphas);
        } 
    }
    
    public int getX(){

        int mX = 0;
        if (!mBaseX.isEmpty() && mBaseX.indexOf("move_x") != -1) {
            int position = mBaseX.indexOf(".move_x");
            String subStr = mBaseX.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            mX = expression.getDou("x", 0d).intValue() + Integer.valueOf(Expression.getRealTimeVar(name, "move_x", "0"));
        }else if(mBaseX.indexOf("#") != -1){
            mX = Expression.caculateInt(mBaseX);
        }else {
            mX = expression.getDou("x", 0d).intValue();
        }

        if (mFrames != null) {
            mX += mFrames.getX();
        }

        if (mTranslates != null) {
            mX += mTranslates.getX();
        }

        return mX;

    }
    
    public int getY(){
        int mY = 0;
        
        if(!mBaseY.isEmpty() && mBaseY.indexOf("move_y") != -1){
            int position = mBaseY.indexOf(".move_y");
            String subStr = mBaseY.substring(0, position);
            String name = subStr.substring(subStr.lastIndexOf("#") + 1, position);
            mY = expression.getDou("y", 0d).intValue() + Integer.valueOf(Expression.getRealTimeVar(name, "move_y", "0"));
        }else if(mBaseY.indexOf("#") != -1){
            mY = Expression.caculateInt(mBaseY);
        }else {
            mY = expression.getDou("y", 0d).intValue();
        }
        if(mFrames != null){
            mY += mFrames.getY();
        }
        if(mTranslates != null){
            mY += mTranslates.getY();
        }
        
        return mY;
    }
    
    public int getAlpha(){
        
        int alpha_exp = 255;
        int alphas = 255;
        int alpha = 255;
         if(!mAlpha.isEmpty()){
             
             expression.putDou("alpha", mAlpha);
             
             alpha_exp = expression.getDou("alpha", 0d).intValue();
         }else if(mAlpha.indexOf("#") != -1){
            alpha = Expression.caculateInt(mAlpha);
         } else{
             alpha_exp = 255;
         }
        
        if(mAlphas != null){
            alphas = mAlphas.getAlpha();
        }else{
            alphas = 255;
        }
        
        alpha = (int)((alpha_exp * alphas) / 255F);    
        
        return alpha;
    }
    
    public int getMaxWidth() {
        int maxWidth = -1;
        if(mScales != null){
            maxWidth = mScales.getMaxWidth();
        }else {
            if(mWidth != null){
            }
        }
        return maxWidth;
    }
    
    public int getWidth(){
        int width = -1;
        if(!mWidth.isEmpty()){
            width = expression.getDou("w", 0d).intValue();
        }else if(mWidth.indexOf("#") != -1){
            width = Expression.caculateInt(mWidth);
        }else if(mScales != null){
            width = mScales.getWidth();
        }
        return width;
    }
    
    public int getMaxHeight() {
        int maxHeight = -1;
        if(mScales != null){
            maxHeight = mScales.getMaxHeight();
        }else {
            if(mHeight != null){
            }
        }
        return maxHeight;
    }
    
    public int getHeight(){
        int height = -1;
        if(!mHeight.isEmpty()){
            height = expression.getDou("h", 0d).intValue();
        }else if(mHeight.indexOf("#") != -1){
            height = Expression.caculateInt(mHeight);
        }else if(mScales != null){
            height = mScales.getHeight();
        }
        return height;
    }
    
    public float getRotationAngle() {
        
        float angleEx = 0;
        float angleRo = 0;
        
        if(!mAngle.isEmpty()){
            
            expression.putDou("angle", mAngle);
            
            angleEx = expression.getDou("angle", 0d).intValue();
            
        } else{
            angleEx = 0;
        }
        if(mRotations != null){
            angleRo = mRotations.getAngle();
        }else{
            angleRo = 0; 
        }
        return angleEx + angleRo;
    }

    public float getCenterX() {
        float centerX = 0;
        if(mCenterX != null){
            centerX = expression.getDou("centerX", 0d).intValue();
        }
        return centerX;
    }

    public float getCenterY() {
        float centerY = 0;
        if(mCenterY != null){
            centerY = expression.getDou("centerY", 0d).intValue();
        }
        return centerY;
    }
    
    public String getBitmapName(){
        String bitmapName = mSrc;
        if(mFrames == null){
            if(!mSrcId.isEmpty()){
                int srcid = expression.getDou("srcId", 0d).intValue();
                int pos = mSrc.indexOf('.');
                bitmapName = new StringBuilder().append(mSrc.substring(0, pos)).append("_").append(srcid).append(mSrc.substring(pos)).toString();
                return bitmapName;
            }
            return bitmapName;
        }else {
            return mFrames.getSrc();
        }
        
    }
    
    public boolean isAlignAbsolute() {
        // TODO Auto-generated method stub
        return mAlignAbsolute;
    }
    
    public void init(){
        int size = mAnimations.size();
        for(int i=0;i<size;i++){
            mAnimations.get(i).init();
        }
    }

    public void finish(){
        if(mAnimations != null){
            mAnimations.clear();
            mAnimations = null;
        }
    }
    
    public void tick(long time){
        int size = mAnimations.size();
        for(int i=0;i<size;i++){
            ((BaseAnimation)mAnimations.get(i)).tick(time);
        }
    } 

}
