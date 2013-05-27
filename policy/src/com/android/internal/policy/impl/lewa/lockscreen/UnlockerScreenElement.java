package com.android.internal.policy.impl.lewa.lockscreen;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.android.internal.policy.impl.lewa.lockscreen.LockScreenRoot.UnlockerCallback;
import com.android.internal.policy.impl.lewa.view.ElementGroup;
import com.android.internal.policy.impl.lewa.view.Expression;
import com.android.internal.policy.impl.lewa.view.ScreenContext;
import com.android.internal.policy.impl.lewa.view.ScreenElement;
import com.android.internal.policy.impl.lewa.view.DomParseException;
import com.android.internal.policy.impl.lewa.view.Utils;
import com.android.internal.policy.impl.lewa.view.Utils.Point;
import com.android.internal.policy.impl.LewaLockScreen;

import android.provider.Settings;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.net.Uri;
import android.view.HapticFeedbackConstants;
/**
 * Unlocker这个部件
 * @author abc
 *
 */
public class UnlockerScreenElement extends ScreenElement implements UnlockerListener{
    
    private static final String TAG = "UnlockerScreenElement";

    private static final boolean DEBUG = false;
    private static final int DEFAULT_DRAG_TOLERANCE = 150;
    
    public static final String TAG_NAME = "Unlocker";
    private int mAlpha;
    private EndPoint mCurrentEndPoint;
    private ArrayList<EndPoint> mEndPoints;
    private boolean mMoving;
    private boolean mPressed;
    private StartPoint mStartPoint;
    private int mTouchOffsetX;
    private int mTouchOffsetY;
    private final UnlockerCallback mUnlockerCallback;
    private final UnlockerListener mUnlockerListener;
    private boolean mUnlockingHide;
    
    private final String STATE_NORMAL = "0";
    private final String STATE_PRESSED = "1";
    private final String STATE_REACHED = "2";
    private final String STATE_UNLOCKED = "3";
    
    private AudioManager mAudioManager;
    private boolean hapticOn = false;// add by luoyongxing @2012/07/11
    private BounceAnimationController mBounceAnimationController;
    
    
    public UnlockerScreenElement(Element element, ScreenContext screenContext, UnlockerCallback unlockercallback, UnlockerListener unlockerlistener) throws DomParseException{
        super(element, screenContext);
        
        mEndPoints = new ArrayList<EndPoint>();
        mUnlockerCallback = unlockercallback;
        mUnlockerListener = unlockerlistener;
        mBounceAnimationController = new BounceAnimationController();
        
        mAudioManager = (AudioManager)screenContext.mContext.getSystemService(Context.AUDIO_SERVICE); 
        hapticOn = (Settings.System.getInt(screenContext.mContext.getContentResolver() , Settings.System.HAPTIC_FEEDBACK_ENABLED,0)==1);
        load(element);
       
    }

    public void load(Element element) throws DomParseException {

        if(!element.getNodeName().equalsIgnoreCase("Unlocker")){
            Log.e(TAG, "Element node name is not Unlocker");
            throw new AssertionError();
        } else{
            
           mName = element.getAttribute("name");
           
            String alpha = element.getAttribute("alpha");
        
            if(!alpha.isEmpty()){
                expression.putDou("alpha", alpha);
                mAlpha = expression.getDou("alpha", 0d).intValue();
            }else {
                mAlpha = 255;
            }
            mBounceAnimationController.load(element);
            loadStartPoint(element);
            loadEndPoint(element);
        }
    }

    private class Position{
    
      public static final String TAG_NAME = "Position";
      private String mBaseX;
      private String mBaseY;
      private int x;
      private int y;

      public Position(Element element, String baseX, String baseY) throws DomParseException {
        this.mBaseX = baseX;
        this.mBaseY = baseY;
        load(element);
      }

      public int getX(){
        if (mBaseX == null){
            return x; 
        }
        return (int) (x + Double.valueOf(mBaseX));
      }

      public int getY(){
          if (mBaseY == null){
              return y; 
          }
          return (int) (y + Double.valueOf(mBaseY));
      }
      
      public void load(Element element) throws DomParseException {
    
          if (element == null){
            return;
          }
          
          Utils.asserts(element.getNodeName().equalsIgnoreCase(TAG_NAME), "wrong node tag");
          
          this.x = Utils.getAttrAsInt(element, "x", 0);
          this.y = Utils.getAttrAsInt(element, "y", 0);
        }
    }

    /**
     * EndPoint(解锁点可以有一到多个)
     * @author abc
     *
     */
    private class EndPoint extends UnlockPoint{
    
        
        public static final String TAG_NAME = "EndPoint";
        /**
         * Position一般都是两个
         */
        private ArrayList<Position> mPath;
        
        /**
         * 此数组列表保存的是动态生成的两个Position点，一个是StartPoint点的当前位置点，另一个是EndPoint点。
         * 此数组被初始化的条件是，当前StartPoint点的当前位置达到了UnlockedState的状态
         */
        private ArrayList<Position> mUnlockedPath;
        
        private String mPathX;
        private String mPathY;
        public Task mTask;
        private int mTolerance;

        public EndPoint(Element element) throws DomParseException{
            super(element, TAG_NAME);
            mTolerance = 150;
            load(element);
        }
        
        private void load(Element element) throws DomParseException{
            loadTask(element);
            loadPath(element);
        }

        private void loadTask(Element element){
        
            Element intentElement = Utils.getChild(element, "Intent");
            if(intentElement == null){
                return;
            }
            
            mTask = new Task();
            
            mTask.action = intentElement.getAttribute("action");
            mTask.type = intentElement.getAttribute("type");
            mTask.category = intentElement.getAttribute("category");
            mTask.packageName = intentElement.getAttribute("package");
            mTask.className = intentElement.getAttribute("class");
            
            if(mTask.action.isEmpty()){
                mTask = null;
            }
        }
        
        private void loadPath(Element element) throws DomParseException {
    
            Element pathElement = Utils.getChild(element, "Path");
            if(pathElement == null){
                mPath = null;
                return;
            }
            mTolerance = Utils.getAttrAsInt(pathElement, "tolerance", DEFAULT_DRAG_TOLERANCE);
            mPath = new ArrayList<Position>();
            
            String pathX = pathElement.getAttribute("x");
            String pathY = pathElement.getAttribute("y");
            
            if(!pathX.isEmpty()){
                expression.putDou("x", pathX);
                mPathX = String.valueOf(expression.getDou("x", 0d));
            }else {
                mPathX = "0";
            }
            if(!pathY.isEmpty()){
                expression.putDou("y", pathY);
                mPathY = String.valueOf(expression.getDou("y", 0d));
            }else {
                mPathY = "0";
            }

            NodeList nodelist = pathElement.getElementsByTagName("Position");
            
            int length = nodelist.getLength();
            for(int i=0;i<length;i++){
                Element et = (Element) nodelist.item(i);
                Position position = new Position(et, mPathX, mPathY);
                mPath.add(position);
            }
            
        }
        
        /**
         * 
         * @param x 当前触摸点x值
         * @param y 当前触摸点y值
         * @return point 返回最近的触摸点
         */
        private Point getNearestPoint(int x, int y){
        
            Point point = null;
            
            if(mPath == null){
                double dx = x - mTouchOffsetX;
                double dy = y - mTouchOffsetY;
                point = new Point(dx, dy);
            } else{
            
                int size = mPath.size();
                if(size <= 1){
                    return null;
                }
                double d2 = 1.7976931348623157E+308D;
                int count = 1;
               
                int offSetX = x - mTouchOffsetX;
                int offSetY = y - mTouchOffsetY;
                int index = count - 1;
                Position position1 = mPath.get(index);
                Position position2 = mPath.get(count);
                
                
                
                Point point1 = new Point(position1.getX(), position1.getY());
                Point point2 = new Point(position2.getX(), position2.getY());
                
                Point point3 = new Point(offSetX, offSetY);
                
                Point point4 = Utils.pointProjectionOnSegment(point1, point2, point3, true);
                double dou = Utils.Dist(point4, point3, false);
                if(dou < d2){
                    d2 = dou;
                    point = point4;
                }
                
            }
            return point;
        }

        public int getTransformedDist(Point point, int x, int y){
        
            int k;
            if(mPath == null){
                k = 0x7ffffffe;
            }else if(point == null){
                k = 0x7fffffff;
            }else{
                double d = x - mTouchOffsetX;
                double d1 = y - mTouchOffsetY;
                Point point1 = new Point(d, d1);
                int l1 = (int)Utils.Dist(point, point1, true);
                if(l1 < mTolerance){
                    k = l1;
                }else {
                    k = 0x7fffffff;
                }
            }
            return k;
        }

        protected void onStateChange(State mState, State state){
            
            if(mState == State.Invalid){
                return;
            }
            boolean playSound = (Settings.System.getInt(context.getContentResolver(),
            							Settings.System.LOCKSCREEN_SOUND_SWITCH, 1) == 1);
			
            int ringerMode = mAudioManager.getRingerMode();
            boolean silentMode = (ringerMode != AudioManager.RINGER_MODE_NORMAL);
            if(silentMode || !playSound){
                return;
            }
            
            switch(state){
            case Reached:
                playSound(mReachedSound);
                break;
            case Unlocked:
                playSound(mUnlockedSound);
                break;
            default:
                return;
            }
        }

        
    }

    /**
     * 起始解锁点只有有一个
     * @author abc
     *
     */
    private class StartPoint extends UnlockPoint{
    
        public static final String TAG_NAME = "StartPoint";
        private int unlockedDist;
        private boolean endPointUnlocked;

        public StartPoint(Element element) throws DomParseException{
            super(element, TAG_NAME);
            
            String dist = element.getAttribute("dist");
            if(dist.isEmpty()){
                dist = "0";
            }
            try {
                unlockedDist = Integer.valueOf(dist);
            } catch (NumberFormatException e) {
                unlockedDist = 0;
            }
            
        }
        
        protected void onStateChange(State mState, State state){
            
            if(mState == State.Invalid){
                return;
            }
            boolean playSound = (Settings.System.getInt(context.getContentResolver(),
            							Settings.System.LOCKSCREEN_SOUND_SWITCH, 1) == 1);
			
            int ringerMode = mAudioManager.getRingerMode();
            boolean silentMode = (ringerMode != AudioManager.RINGER_MODE_NORMAL);
            if(silentMode || !playSound){
                return;
            }
            
            switch(state){
                case Normal:
                    playSound(mNormalSound);
                    break;
                case Pressed:
                    playSound(mPressedSound);
                    break;
                case Unlocked:
                    playSound(mUnlockedSound);
                    break;
                default:
                    break;
            }
        }
        
        public int getUnlockedDist(){
            return unlockedDist;
        }

        public void setEndPointUnlocked(boolean endPointUnlocked){
            this.endPointUnlocked = endPointUnlocked;
        }
        
        public boolean getEndPointUnlocked(){
            return endPointUnlocked;
        }
        
    }
    /**
     * 解锁点的统称
     * @author fulw
     *
     */
    private class UnlockPoint{
    
        private ElementGroup mCurrentStateElements;
        
        /**
         * 正常状态下播放的声音
         */
        protected String mNormalSound;
        /**
         * 按下状态下播放的声音
         */
        protected String mPressedSound;
        /**
         * 到达状态到播放的声音
         */
        protected String mReachedSound;
        /**
         * 解锁过程中播放的声音
         */
        protected String mUnlockedSound;
        
        protected ElementGroup mNormalStateElements;
        protected ElementGroup mPressedStateElements;
        /**
         * Reached与Unlocked是两个互斥的组件
         */
        protected ElementGroup mReachedStateElements;
        protected ElementGroup mUnlockedStateElements;
        
        protected boolean hasUnlockedState = false;
        
        private State mState;
        
        protected int mCurrentX;
        protected int mCurrentY;
        
        private int mWidth;
        private int mHeight;
        protected int mX;
        protected int mY;
        

        public UnlockPoint(Element element, String nodeName) throws DomParseException{
            mState = State.Invalid;
            
            load(element, nodeName);
            
            Expression.putRealTimeVar(mName, Expression.MOVE_X, "0");
            Expression.putRealTimeVar(mName, Expression.MOVE_Y, "0");
            Expression.putRealTimeVar(mName, Expression.MOVE_DIST, "0");
        }
        
        public void load(Element element, String nodeName) throws DomParseException {
    
            Utils.asserts(element.getNodeName().equalsIgnoreCase(nodeName), "wrong node name");
            
            mNormalSound = element.getAttribute("normalSound");
            mPressedSound = element.getAttribute("pressedSound");
            mReachedSound = element.getAttribute("reachedSound");
            mUnlockedSound = element.getAttribute("unlockedSound");
            
            
            expression.putDou("x", element.getAttribute("x"));
            mX =expression.getDou("x", 0d).intValue();
            
            expression.putDou("y", element.getAttribute("y"));
            mY =expression.getDou("y", 0d).intValue();
            
            expression.putDou("w", element.getAttribute("w"));
            mWidth =expression.getDou("w", 0d).intValue();
            
            expression.putDou("h", element.getAttribute("h"));
            mHeight =expression.getDou("h", 0d).intValue();
            
            Element normalState = Utils.getChild(element, "NormalState");
            if(normalState != null){
                mNormalStateElements = new ElementGroup(normalState, screenContext);
            }
            
            Element pressedState = Utils.getChild(element, "PressedState");
            if(pressedState != null){
                mPressedStateElements = new ElementGroup(pressedState, screenContext);
            }
            
            Element unlockedState = Utils.getChild(element, "UnlockedState");
            if(unlockedState != null){
                mUnlockedStateElements = new ElementGroup(unlockedState, screenContext);
                hasUnlockedState = true;
            }
            
            if(!hasUnlockedState){//与Unlocked互斥
                Element reachedState = Utils.getChild(element, "ReachedState");
                if(reachedState != null){
                    mReachedStateElements = new ElementGroup(reachedState, screenContext);
                }
            }
            
            setState(State.Normal);
        }
        
        public void setState(State state){
            if(mState == state){
                return;
            }
            mState = state;
            switch (state) {
            case Normal:
                if(mNormalStateElements != null && !mPressed){
                    mNormalStateElements.init();
                }
                mCurrentStateElements = mNormalStateElements;
                break;
            case Pressed:
                if(mPressedStateElements != null){
                    mCurrentStateElements = mPressedStateElements;
                }else {
                    mCurrentStateElements = mNormalStateElements;
                }
                break;
            case Reached:
                if(mReachedStateElements != null){
                    mCurrentStateElements = mReachedStateElements;
                }else if(mPressedStateElements != null){
                    mCurrentStateElements = mPressedStateElements;
                }else {
                    mCurrentStateElements = mNormalStateElements;
                }
                break;
            case Unlocked:
                if(mUnlockedStateElements != null){
                    mCurrentStateElements = mUnlockedStateElements;
                }else {
                    mCurrentStateElements = mNormalStateElements;
                }
                break;
            default:
                break;
            }
            onStateChange(mState,state);
        }
        
        public void init(){
            
            mCurrentX = mX;
            mCurrentY = mY;
            
            if(mNormalStateElements != null){
                mNormalStateElements.init();
            }
            if(mPressedStateElements != null){
                mPressedStateElements.init();
            }
            if(mReachedStateElements != null){
            
                mReachedStateElements.init();
            }
        }
        
        
        public ScreenElement findElement(String name){
        
            ScreenElement screenElement = null;
            
            if(mPressedStateElements != null){
                screenElement = mPressedStateElements.findElement(name);
                if(screenElement != null){
                    return screenElement;
                }
            }
            
            if(mNormalStateElements != null){
                screenElement = mNormalStateElements.findElement(name);
                if(screenElement != null){
                    return screenElement;
                }
            }
            
            if(mReachedStateElements != null){
                screenElement = mReachedStateElements.findElement(name);
                if(screenElement != null){
                    return screenElement;
                }
            }
            
            return null;
                
        }

        public int getCurrentX(){
            return mCurrentX;
        }

        public int getCurrentY(){
            return mCurrentY;
        }

        public State getState(){
            return mState;
        }

        public int getX(){
            return mX;
        }

        public int getY() {
            return mY;
        }
        
        public boolean getHasUnlockedState(){
            return hasUnlockedState;
        }

        public void moveTo(int x, int y){
            mCurrentX = x;
            mCurrentY = y;
        }

        protected void onStateChange(State mState, State state){
            
        }

        public void pause(){
        
            if(mNormalStateElements != null){
                mNormalStateElements.pause();
            }
            if(mPressedStateElements != null){
                mPressedStateElements.pause();
            }
            if(mReachedStateElements != null){
                mReachedStateElements.pause();
            }
        }

        public void render(Canvas canvas){
            
            int count = canvas.save();
            float delatX = getCurrentX() - getX();
            float delatY = getCurrentY() - getY();
            canvas.translate(delatX, delatY);
            
            if(mCurrentStateElements != null){
                mCurrentStateElements.render(canvas);
            }
            
            canvas.restoreToCount(count);
        }

        public void resume(){
        
            if(mNormalStateElements != null){
                mNormalStateElements.resume();
            }
            if(mPressedStateElements != null){
                mPressedStateElements.resume();
            }
            if(mReachedStateElements != null){
                mReachedStateElements.resume();
            }
        }

        public void showCategory(String category, boolean isShow){
        
            if(mPressedStateElements != null){
                mPressedStateElements.showCategory(category, isShow);
            }
            if(mNormalStateElements != null){
                mNormalStateElements.showCategory(category, isShow);
            }
            if(mReachedStateElements != null){
                mReachedStateElements.showCategory(category, isShow);
            } 
        }

        public void tick(long time){
            if(mCurrentStateElements != null){
                mCurrentStateElements.tick(time);
            }
        }

        /**
         * 判断触摸是否在Unlocker(StartPoint、EndPoint)区域
         * @param x 当前触摸点x值
         * @param y 当前触摸点y值
         * @return 是否触摸到了Unlocker区域，返回true表示触摸到，返回false表示未触摸到
         */
        public boolean touched(int x, int y){
        
            /**
             * mx 表示StartPoint、EndPoint的初始x值
             * my 表示StartPoint、EndPoint的初始y值
             * mw 表示StartPoint、EndPoint的初始w值
             * mh 表示StartPoint、EndPoint的初始h值
             */
            int mx = getX();//0
            int my = getY();//440
            int mw = Integer.valueOf(mWidth);//480
            int mh = Integer.valueOf(mHeight);//200
            
            /**
             * 如果当前触摸点x值小于初始值mx，则未触摸到
             */
            if(x < mx){
                return false;
            }
            
            /**
             * 初始值mx与Unlocker的宽度之和
             */
            int mx_1 = mx + mw;
            
            /**
             * 如果当前触摸点x值大于mx_1或者当前触摸点y值小于初始值my，则未触摸到
             */
            if(x > mx_1 || y < my){
                return false;
            }
            
            /**
             * 初始值my与Unlocker的高度之和
             */
            int my_1 = my + mh;
            
            /**
             * 如果当前触摸点y值大于my_1，则未触摸到
             */
            if(y > my_1){
                return false;
            }
            
            /**
             * 触摸到
             */
            return true;
            
        }

        
    }

    private static enum State{
        Normal,Pressed,Reached,Unlocked,Invalid;
    }

    private class Task{

        public String action;
        public String category;
        public String className;
        public String packageName;
        public String type;
    }


    private void cancelMoving(){
    
        int x = mStartPoint.getX();
        int y = mStartPoint.getY();
        mStartPoint.moveTo(x, y);
        mStartPoint.setState(State.Normal);
        
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).setState(State.Normal);
        }
        Expression.putRealTimeVar(mName, Expression.MOVE_X, "0");
        Expression.putRealTimeVar(mName, Expression.MOVE_Y, "0");
        Expression.putRealTimeVar(mName, Expression.MOVE_DIST, "0");
        Expression.putRealTimeVar(mName, Expression.STATE, STATE_NORMAL);
        
        mUnlockerListener.endUnlockMoving(this);
    }

    private boolean checkEndPoint(boolean doUnlock, Point point, EndPoint endpoint){
    
        boolean reachedEndPoint = false;
        int x = (int)point.x;
        int y = (int)point.y;
        if(endpoint.touched(x, y)){
        
            if(doUnlock){
                doUnlock(endpoint);
            }
            reachedEndPoint = true;
            if(endpoint.getState() != State.Reached){
            
                endpoint.setState(State.Reached);
                if(hapticOn && screenContext.mView != null){// add by luoyongxing @2012/07/11
                    screenContext.mView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
                int size = mEndPoints.size();
                for(int i=0;i<size;i++){
                    EndPoint ep = mEndPoints.get(i);
                    if(ep != endpoint){
                        ((EndPoint)mEndPoints.get(i)).setState(State.Pressed);
                    }
                }
                
            }
        } else{
            endpoint.setState(State.Pressed);
        }
        return reachedEndPoint;
    }


    private void doUnlock(EndPoint endpoint){
        
        Task task = endpoint.mTask;
        
        Intent intent = null;
        
        if(task != null){
            String action = task.action;
            intent = new Intent(action);
            if(!task.type.isEmpty()){
            
                String type = task.type;
                if("vnd.android-dir/mms-sms".equals(type)){
                    if(LewaLockScreen.mUnreadMsgCount == 1  && LewaLockScreen.mUnreadSmsNum != null) {
                        Uri uri = null;
                       
                    
                        uri = Uri.parse("smsto:" + LewaLockScreen.mUnreadSmsNum);
                        intent.setAction(Intent.ACTION_SENDTO);
                        intent.setData(uri);
                        intent.putExtra("LockScreen_MSG", "true");
                                              
                    } else {
                        intent.setType("vnd.android-dir/mms-sms");
                        intent.putExtra("LockScreen_MSG", "true");
                    }
                }else{
                     intent.setType(type);
                }
 
            }
            
            if(!task.category.isEmpty()){
                String category = task.category;
                intent.addCategory(category);
            }
            if(!task.packageName.isEmpty() && !task.className.isEmpty()){
            
                String packageName = task.packageName;
                String className = task.className;
                ComponentName componentname = new ComponentName(packageName, className);
                intent.setComponent(componentname);
            }
            intent.setFlags(0x34000000);
        }
        try{
            mUnlockerCallback.unlocked(intent);
        }catch(ActivityNotFoundException activitynotfoundexception){
            Log.e(TAG, activitynotfoundexception.toString());
            activitynotfoundexception.printStackTrace();
        }
    }

    private boolean isShowing(){
    
        if(isVisible() && !mUnlockingHide){
            if(mAlpha <= 0d){
                return false;
            }
            return true;
        }else {
            return false;
        }
    }
    
    private void loadStartPoint(Element element) throws DomParseException{
        
        Element childElement = Utils.getChild(element, "StartPoint");
        mStartPoint = new StartPoint(childElement);
        
        boolean flag = false;
        if(childElement != null){
            flag = true;
        }
        Utils.asserts(flag, "no StartPoint node");
    }

    private void loadEndPoint(Element element)throws DomParseException{
    
        mEndPoints.clear();
        
        NodeList nodelist = element.getElementsByTagName("EndPoint");
        
        int length = nodelist.getLength();
        for(int i=0;i<length;i++){
            Element childElement = (Element) nodelist.item(i);
            EndPoint endPoint = new EndPoint(childElement);
            mEndPoints.add(endPoint);
        }
        
        boolean flag = false;
        if(!mEndPoints.isEmpty()){
            flag = true;
        }
        Utils.asserts(flag, "no end point for unlocker!");
    }

    private void moveStartPoint(int x, int y){
    
        mStartPoint.moveTo(x, y);
        
       int currentX = mStartPoint.getCurrentX();
        int mX = Integer.valueOf(mStartPoint.mX);
        int move_x = currentX - mX;
        int currentY = mStartPoint.getCurrentY();
        int mY = Integer.valueOf(mStartPoint.mY);
        int move_y = currentY - mY;
   
        double move_dist = Math.sqrt((currentX - mX)*(currentX - mX) + (currentY - mY)*(currentY - mY));
        
        Expression.putRealTimeVar(mName, Expression.MOVE_X, String.valueOf(move_x));
        Expression.putRealTimeVar(mName, Expression.MOVE_Y, String.valueOf(move_y));
        Expression.putRealTimeVar(mName, Expression.MOVE_DIST, String.valueOf(move_dist));
    }

    private void playSound(String soundName){
    
        if(!TextUtils.isEmpty(soundName)){
            ((LockScreenRoot)screenContext.mRoot).playSound(soundName);
        }
    }

    public void endUnlockMoving(UnlockerScreenElement unlockerscreenelement){
    
        if(unlockerscreenelement != this){
            mUnlockingHide = false;
        } 
    }

    public void init(){
        mStartPoint.init();
        
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).init();
        }
    }

    

    public void onTouch(MotionEvent event){
        
        if(!isShowing()){
            return;
        }
        
        int currentX = (int)event.getX();
        int currentY = (int)event.getY();
        
        switch(event.getActionMasked()){
        
        case MotionEvent.ACTION_DOWN:
           
            if(!mStartPoint.touched(currentX, currentY)){
                return;
            }
            mMoving = true;
            
            int startPointX = mStartPoint.getX();
            mTouchOffsetX = currentX - startPointX;
            
            int startPointY = mStartPoint.getY();
            mTouchOffsetY = currentY - startPointY;
            
            /**
             * 设定StartPoint现在处于被按下状态
             */
            mStartPoint.setState(State.Pressed);
            /**
             * 设定EndPoints现在处于被按下状态
             */
            int size = mEndPoints.size();
            for(int i=0;i<size;i++){
                EndPoint ep = (EndPoint) mEndPoints.get(i);
                ep.setState(State.Pressed);
            }
            
            mPressed = true;
            
            mUnlockerListener.startUnlockMoving(this);
            
            Expression.putRealTimeVar(mName, Expression.STATE, STATE_PRESSED);
            
            
            mBounceAnimationController.init();
            
            break;

        case MotionEvent.ACTION_MOVE:
            
            if(!mMoving){
                return;
            }
            CheckTouchResult checktouchresult = null;
            
            if(mStartPoint.getHasUnlockedState()){
                checktouchresult = unlockedCheckTouch(currentX, currentY, false);
            }else {
                checktouchresult = checkTouch(currentX, currentY, false);//Move状态不解锁
            }
            
            
            if(checktouchresult == null){
                mCurrentEndPoint = null;
            }else {
                mCurrentEndPoint = checktouchresult.endPoint; 
            }
            break;

        case MotionEvent.ACTION_UP:
            
            if(!mMoving){
                return;
            }
            mPressed = false;
            mMoving = false;
            
            CheckTouchResult ctr = null;
            if(mStartPoint.getHasUnlockedState()){
                //ctr = unlockedCheckTouch(currentX, currentY, true);
                ctr = unlockedCheckTouch(currentX, currentY, false);
            }else {
                ctr = checkTouch(currentX, currentY, true);//Up状态解锁
            }
            
            if(!ctr.unlocked){
            
                EndPoint ep = ctr.endPoint;
                if(!mStartPoint.getHasUnlockedState() || !mStartPoint.getEndPointUnlocked()){
                    cancelMoving();
                }else {
                    mBounceAnimationController.startBounceAnimationMoving(ep);
                }
                
            }
            mCurrentEndPoint = ctr.endPoint;
            
            Expression.putRealTimeVar(mName, Expression.STATE, STATE_NORMAL);
            
            break;
        
        default:
            break;
        }
        
    }
    
    private void moveToUnlockedPoint(){
        
        mStartPoint.moveTo(192, 200);
        
        mStartPoint.setState(State.Normal);
        
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).setState(State.Normal);
        }
        
        
        Expression.putRealTimeVar(mName, Expression.MOVE_X, "0");
        Expression.putRealTimeVar(mName, Expression.MOVE_Y, "0");
        Expression.putRealTimeVar(mName, Expression.MOVE_DIST, "0");
        Expression.putRealTimeVar(mName, Expression.STATE, STATE_NORMAL);
        
        mUnlockerListener.endUnlockMoving(this);
    }
    
    private class CheckTouchResult{
        public EndPoint endPoint;
        public boolean unlocked;
    }
    
    /**
     * 
     * @param currentX 当前触摸点x值
     * @param currentY 当前触摸点y值
     * @param flag
     * @return
     */
    private CheckTouchResult checkTouch(int currentX, int currentY, boolean doUnlock){

        int maxInt = 0x7fffffff;
        Point point = null;
        boolean endPointReached = false;
        
        CheckTouchResult checktouchresult = new CheckTouchResult();
        
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            EndPoint endPoint = mEndPoints.get(i);

            Point nearestPoint = endPoint.getNearestPoint(currentX, currentY);
            int dist = endPoint.getTransformedDist(nearestPoint, currentX, currentY);
            if(dist < maxInt){
                maxInt = dist;
                point = nearestPoint;
                checktouchresult.endPoint = endPoint;
            }
        }
        if(maxInt >= 0x7fffffff){
            return checktouchresult;       
        }
        
        if(maxInt >= 0x7ffffffe){
            for(int i=0;i<size;i++){
                EndPoint endPoint = mEndPoints.get(i);
                endPointReached = checkEndPoint(doUnlock, point, endPoint);
                if(endPointReached) {
                    checktouchresult.endPoint = endPoint;
                    break;
                }
            }
        }
        else {
            if(point != null){
               // moveStartPoint((int)point.x, (int)point.y);

                endPointReached = checkEndPoint(doUnlock, point, checktouchresult.endPoint);

            }             
        } 
        
        if(point != null){
            moveStartPoint((int)point.x, (int)point.y);
        }
       
        State state = null;
        
        if(endPointReached){
            state = State.Reached;
        }else {
            state = State.Pressed;
        }
        mStartPoint.setState(state);
        
        if(endPointReached){
            Expression.putRealTimeVar(mName, Expression.STATE, STATE_REACHED);
        }else {
            Expression.putRealTimeVar(mName, Expression.STATE, STATE_PRESSED);
        }
        return checktouchresult;
       
    }
    
    /**
     * 检测是否达到能解锁的条件,如果当前坐标点到StartPoint的起始点的距离大于用户设置的解锁条件距离返回true,否则false
     * @param point 当前坐标点
     * @param unlockedDist 用户设置的达到解锁条件的距离
     * @return
     */
    private boolean checkIfUnlocked(Point point){
        
        int x = (int)point.x;
        int y = (int)point.y;
        int dealtX = x - mStartPoint.getX();
        int dealtY = y - mStartPoint.getY();
        
        int realDist = (int) Math.sqrt(dealtX*dealtX + dealtY*dealtY);
        
        if(realDist > mStartPoint.getUnlockedDist()){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 
     * @param x 当前触摸点x值
     * @param y 当前触摸点y值
     * @param flag
     * @return
     */
    private CheckTouchResult unlockedCheckTouch(int x, int y, boolean doUnlock){

        int maxInt = 0x7fffffff;
        Point point = null;
        
        CheckTouchResult checktouchresult = new CheckTouchResult();
        
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            EndPoint endPoint = mEndPoints.get(i);
            
            Point nearestPoint = endPoint.getNearestPoint(x, y);
            int dist = endPoint.getTransformedDist(nearestPoint, x, y);
            if(dist < maxInt){
                maxInt = dist;
                point = nearestPoint;
                checktouchresult.endPoint = endPoint;
            }
        }
        
        boolean endPointUnlocked = false;
        
        if(point != null){
            
            moveStartPoint((int)point.x, (int)point.y);
            
            endPointUnlocked = checkIfUnlocked(point);
            
            mStartPoint.setEndPointUnlocked(endPointUnlocked);
            
            if(doUnlock && endPointUnlocked){
                moveToUnlockedPoint();
            }
        }
        State state = null;
        
        if(endPointUnlocked && !mPressed){
            state = State.Unlocked;
        }else {
            state = State.Pressed;
        }
        mStartPoint.setState(state);
        
        
        if(endPointUnlocked){
            Expression.putRealTimeVar(mName, Expression.STATE, STATE_UNLOCKED);
        }else {
            Expression.putRealTimeVar(mName, Expression.STATE, STATE_PRESSED);
        }
        
        
        return checktouchresult;
       
    }
    
    public void pause(){
        
        cancelMoving();
        
        mStartPoint.pause();
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).pause();
        }
    }

    
    public void render(Canvas canvas){
        if(!isShowing()){
            return;
        }
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).render(canvas);
        }
        mStartPoint.render(canvas);
    }

    public void resume(){
        mStartPoint.resume();
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).resume();
        }
    }

   public void showCategory(String category, boolean flag){
        mStartPoint.showCategory(category, flag);
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).showCategory(category, flag);
        }
    }

    public void startUnlockMoving(UnlockerScreenElement unlockerscreenelement){
    
        if(unlockerscreenelement != this){
           mUnlockingHide = true;
        }
    }

    public void tick(long time){
    
        if(!isShowing()){
            return;
        }
        mBounceAnimationController.tick(time);
        mStartPoint.tick(time);
        int size = mEndPoints.size();
        for(int i=0;i<size;i++){
            ((EndPoint)mEndPoints.get(i)).tick(time);
        }
    }
    
    private class BounceAnimationController {

        private int mBounceAccelation;
        private int mBounceInitSpeed;
        
        private String mBounceAccelationExp;
        private String mBounceInitSpeedExp;
        
        private int mBounceStartPointIndex;
        private long mBounceStartTime;
        
        private EndPoint mEndPoint;
        
        private long mPreDistance;
        
        private int mStartX;
        private int mStartY;
        
        public BounceAnimationController(){
            mBounceStartTime = -1;
        }
        
        private Point getPoint(int px_1, int py_1, int px_2, int py_2, long distVar){

            Point point_1 = new Point(px_1, py_1);
            Point point_2 = new Point(px_2, py_2);
            double dist = Utils.Dist(point_1, point_2, true);
            Point point = null;
            if((double)distVar >= dist){
                point = null;
            } else{
                double distRate = (dist - (double)distVar) / dist;
                double distX = (point_2.x - point_1.x) * distRate;
                double distY = (point_2.y - point_1.y) * distRate;
                double px = point_1.x + distX;
                double py = point_1.y + distY;
                point = new Point(px, py);
            }
            return point;
        }

        private void startBounceAnimation(EndPoint endpoint){
        
            if(!mBounceInitSpeedExp.isEmpty()){
                mBounceInitSpeed = Integer.valueOf(mBounceInitSpeedExp);
            }
            if(!mBounceAccelationExp.isEmpty()){
                mBounceAccelation = Integer.valueOf(mBounceAccelationExp);
            }
           
            if(endpoint == null){
                return;
            }
            
            mStartX = mStartPoint.getCurrentX();
            mStartY = mStartPoint.getCurrentY();
            
            
            if(mStartPoint.getEndPointUnlocked()){
                
                try {
                    endpoint.mUnlockedPath = new ArrayList<Position>();
                    Position pBegin = new Position(null, String.valueOf(mStartX), String.valueOf(mStartY));
                    
                    Position pTarget = new Position(null, String.valueOf(endpoint.mX), String.valueOf(endpoint.mY));
                    
                    endpoint.mUnlockedPath.add(pTarget);
                    endpoint.mUnlockedPath.add(pBegin);
                    
                    
                } catch (DomParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
            
            if(endpoint.mPath == null && endpoint.mUnlockedPath == null){
                return;
            }
            
            mBounceStartTime = 0L;
            mEndPoint = endpoint;
            
            mBounceStartPointIndex = -1;
            
            ArrayList<Position> arrayList = null;
            
            if(mStartPoint.getEndPointUnlocked()){
                 arrayList = endpoint.mUnlockedPath;
            }else {
                arrayList = endpoint.mPath;
            }
            int size = arrayList.size();
            int temp = 1;
            if(temp >= size){
                return;
            }
            for(int i=0;i<size;i++){
                Position position_1 = arrayList.get(0);
                Position position_2 = arrayList.get(temp);
                double px_1 = position_1.getX();
                double py_1 = position_1.getY();
                Point point_1 = new Point(px_1, py_1);
                
                double px_2 = position_2.getX();
                double py_2 = position_2.getY();
                Point point_2 = new Point(px_2, py_2);
                
                Point point_3 = new Point(mStartX, mStartY);
                if(Utils.pointProjectionOnSegment(point_1, point_2, point_3, false) != null){
                    mBounceStartPointIndex = temp - 1;
                    return;
                }
                temp++;
            }
        }

        public void init(){
            mBounceStartTime = -1;
        }

        public void load(Element element){
            mBounceInitSpeedExp = element.getAttribute("bounceInitSpeed");
            mBounceAccelationExp = element.getAttribute("bounceAcceleration");
        }

        public void startBounceAnimationMoving(EndPoint endpoint){
            if(mBounceInitSpeedExp.isEmpty()){
                cancelMoving();
            } else{
                startBounceAnimation(endpoint);
            }
        }

        public void tick(long time){
        
            if(mBounceStartTime < 0L){
                return;
            }
            if(mBounceStartTime == 0L){
                mBounceStartTime = time;
                mPreDistance = 0L;
                return;
            }
            
            long deltTime = time - mBounceStartTime;
            long l5 = ((long)mBounceInitSpeed * deltTime) / 1000L;
            long l6 = ((long)mBounceAccelation * deltTime * deltTime) / 2000000L;
            long l7 = l5 + l6;
            long l9 = ((long)mBounceAccelation * deltTime) / 1000L;
            if(mBounceInitSpeed + l9 <= 0L){
                cancelMoving();
                mBounceStartTime = -1;
                return;
            }
            if(mEndPoint != null && (mEndPoint.mPath != null || mEndPoint.mUnlockedPath != null)){
                int spCX = mStartPoint.getCurrentX();
                int spCY = mStartPoint.getCurrentY();
                long l13 = mPreDistance;
                long l14 = l7 - l13;
                
                Point point = null;
                
                ArrayList<Position> arrayList = mEndPoint.mPath;
                if(arrayList == null){
                    arrayList = mEndPoint.mUnlockedPath;
                }
                int temp = 0;
                for(int i = mBounceStartPointIndex ; i >= 0;i--){
                    Position position = arrayList.get(i);
                    int px = position.getX();
                    int py = position.getY();
                    point = getPoint(px, py, spCX, spCY, l14);
                    if(point != null){
                        temp = i;
                        break;
                    }
                    if(mBounceStartPointIndex == 0){
                        if(mStartPoint.getState() == State.Unlocked){
                            try {
                                Thread.currentThread().sleep(100);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            doUnlock(mEndPoint);
                        }else {
                            cancelMoving();
                        }
                        
                        mBounceStartTime = -1;
                        continue;
                    }
                    Point point2 = new Point(px, py);
                    Point point4 = new Point(spCX, spCY);
                    
                    double dist = Utils.Dist(point2, point4, true);
                    l14 = (long)(l14 - dist);
                    spCX = position.getX();
                    spCY = position.getY();                   
                }
                mBounceStartPointIndex = temp;
                if(point != null){
                    moveStartPoint((int)point.x, (int)point.y);
                    
                    
                    
                }
               
            }else {
                int spX = mStartPoint.getX();
                int spY = mStartPoint.getY();
  
                Point point = getPoint(spX, spY, mStartX, mStartY, l7);
                if(point == null){
                    cancelMoving();
                    mBounceStartTime = -1;
                } else{
                    moveStartPoint((int)point.x, (int)point.y);
                }
            }

            screenContext.mShouldUpdate = true;
            mPreDistance = l7;
        }

        
    }
}
