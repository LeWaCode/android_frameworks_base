package com.android.systemui.statusbar;

import com.android.systemui.R;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsSeekBar;
import android.widget.Scroller;
import android.widget.SeekBar;

public class MyScrollView extends ViewGroup{

	private static final int SNAP_VELOCITY = 600;
	private Scroller scroller;
	private View childview;
	private VelocityTracker velocitytracker;
	private float lastX;
	private float lastY;
	private int x;
	private int count;
	private int delayX;
	private int velocityX;
	private int childwidth;
	private int touchslot;
	private int childleft;
	private int childcount=getChildCount();
	private int currentscreen;
	private int defaultscreen=1;  
	private int touchstate;
	private int Touch_State_Scrolling=1;
	private int Touch_State_Rest=0;

	private SeekBar mSeekBar;
	private boolean mThumbPressed = false;

	private float mDensity;
	public MyScrollView(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		scroller=new Scroller(context);
		currentscreen=defaultscreen;
		touchslot=ViewConfiguration.get(context).getScaledWindowTouchSlop(); 
		mDensity = context.getResources().getDisplayMetrics().density;
	}

	public MyScrollView(Context context, AttributeSet attrs) {
		// TODO Auto-generated constructor stub
		this(context,attrs,0);		
	}

	public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
		// TODO Auto-generated constructor stub
		super(context, attrs, defStyle);
		scroller=new Scroller(context);
		currentscreen=defaultscreen;
		touchslot=ViewConfiguration.get(context).getScaledWindowTouchSlop();
		mDensity = context.getResources().getDisplayMetrics().density;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int width=MeasureSpec.getSize(widthMeasureSpec);
		

		for(int i=0;i<getChildCount();i++){
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		scrollTo(currentscreen*width, 0);
	}

	

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		
		//mSeekBar = (SeekBar)this.findViewById(R.id.seekbar);

		if(changed){
			
			childleft=0;
			for(int i=0;i<getChildCount();i++){
				childview=getChildAt(i);
				childwidth=childview.getMeasuredWidth();
				childview.layout(childleft, 0, childleft+childwidth, childview.getMeasuredHeight());
				childleft+=childwidth;
			}
		}
	}
	
	
	
	@Override
	public void computeScroll() {
		// TODO Auto-generated method stub

		if(scroller.computeScrollOffset()){
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
			postInvalidate();
			//changed by zhuyaopeng
//			StatusBarService.updateIndi(currentscreen);
		}
		
		 
	}
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub	

		final int action=ev.getAction();
		final float currentX=ev.getX();
		final float currentY=ev.getY();
		if(false)
            {      
                Log.e("zhangbo", "=== action= "+action);
                Log.e("zhangbo", "=== currentX= "+currentX + " currentY = "+currentY);
            }   
		
		switch(action){
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_DOWN:	
			{
				lastX=currentX;
				lastY=currentY;
				if(scroller.isFinished()){
					touchstate=Touch_State_Rest;
				}else{
					touchstate=Touch_State_Scrolling;
				} 
			}	
			break;
		case MotionEvent.ACTION_UP:
			touchstate=Touch_State_Rest;
			break;
		case MotionEvent.ACTION_MOVE:
			{
                        x=(int) Math.abs(lastX-currentX);
                        if(false)
                        {
                            Log.e("zhangbo", "=== "+lastX+" currentX"+currentX+" touchslot"+touchslot +" x"+x);
                        }
                        if(x>touchslot){
                            if(false)
                            {
                                Log.e("zhangbo", "=== "+action+" x>touchslot");
                            }
                            touchstate=Touch_State_Scrolling;
                        } 			
			}
			break;
		}
		if (touchstate == Touch_State_Rest) {
                if(false)
                {
                    Log.e("zhangbo", "=== "+action+" false");
                }
		    return false;
		} else {
                if(false)
                {      
                    Log.e("zhangbo", "=== "+action+" true");
                }
                return true;
		} 
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		if(velocitytracker==null){
			velocitytracker=VelocityTracker.obtain();
		}
		velocitytracker.addMovement(event);
		final int action=event.getAction();
        final float currentX=event.getX();
        final float currentY=event.getY();
        switch(action){
        case MotionEvent.ACTION_CANCEL:
        	touchstate=Touch_State_Rest;
        	break;
        case MotionEvent.ACTION_DOWN:
        	if(!(scroller.isFinished())){
        		scroller.abortAnimation();
        	}
        	lastX=currentX;
        	break;
        case MotionEvent.ACTION_MOVE:
        	delayX=(int) (lastX-currentX);
        	lastX=currentX;
        	scrollBy(delayX, 0);
        	break;
        case MotionEvent.ACTION_UP:
        	velocitytracker.computeCurrentVelocity(1000);
        	velocityX=(int) velocitytracker.getXVelocity();
        	if(velocityX>SNAP_VELOCITY && currentscreen>0){
        		snapToScreen(currentscreen-1);
        	}else if(velocityX<-SNAP_VELOCITY && currentscreen<getChildCount()-1){
        		snapToScreen(currentscreen+1);
        	}else{
        		snaptoDestination();
        	}
        	touchstate=Touch_State_Rest;
        	break;
        }
		return true;		
	}

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY
                    || y >= child.getBottom() - scrollY
                    || x < child.getLeft()
                    || x >= child.getRight());
        }
        return false;
    }

    private boolean inThumb(float x, float y) {
        if(false)
        {
            Log.e("inThumb", "=== currentscreen="+currentscreen);
        }
        if (currentscreen != 2) {
            if(false)
            {
                Log.e("inThumb", "=== currentscreen != 0");
            }
            return false;
        }
    	int modify = 22;
    	Rect rect = mSeekBar.getThumbBounds();

        int[] seekbarposition = new int[2];
       mSeekBar.getLocationInWindow(seekbarposition);
        if(false)
        {   
            Log.e("inThumb", "=== rect.left ="+rect.left + " rect.right ="+rect.right+" rect.top ="+rect.top+" rect.bottom ="+rect.bottom);

            Log.e("inThumb", "seekbarposition[0]= "+seekbarposition[0]);
            Log.e("inThumb", "seekbarposition[1]= "+seekbarposition[1]);

            Log.e("inThumb", "=== x="+x+" y="+y);
            Log.e("inThumb", "mDensity ="+mDensity);
        }
    	boolean ret = ((seekbarposition[0] + rect.left - modify) < x && x < (seekbarposition[0] + rect.right + modify) && ( rect.top - modify) < y && y < ( rect.bottom + modify));
	if(mDensity == 1)
	{
		modify = 18;
		ret = ((seekbarposition[0] + rect.left - modify) < x && x < (seekbarposition[0] + rect.right + modify) && ( rect.top - modify) < y && y < ( rect.bottom + modify));
	}
        if(false)
        {
            Log.e("inThumb", "=== ret="+ret);
        }
    	return ret;
    }
    
	private void snaptoDestination() {
		// TODO Auto-generated method stub
		final int width=getWidth();
		final int destscreen=(getScrollX()+width/2)/width;
		snapToScreen(destscreen);
		  
	}
	
	private void snapToScreen(int destscreen) {
		// TODO Auto-generated method stub

		destscreen=Math.max(0, Math.min(destscreen, getChildCount()-1));
		if(getScrollX()!=destscreen*getWidth()){
			delayX=destscreen*getWidth()-getScrollX();
			scroller.startScroll(getScrollX(), 0, delayX, 0, Math.abs(delayX)*2);
			currentscreen=destscreen;
			invalidate();
		}
	}
 
}
