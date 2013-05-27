package com.android.systemui.statusbar;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.systemui.R;

//liuaho fix vibrate when press  VIRTUAL_KEY home key
import android.provider.Settings;

public class KeysView extends FrameLayout implements OnTouchListener {

    private static final long KEY_VIEWS_DURATION = 5000;
    private static final int TOAST_DURATION = 500;

    public View mButtonsView;
    public Context mContext;

    private CommandShell mCommandShell = null;

    private Vibrator feedback;
    private Handler objHandler = new Handler();
    private final int resIds[] = { R.id.home ,R.id.menu, R.id.back};

    public KeysView(Context context) {
        super(context);
        mContext = context;
        init(context);
        // TODO Auto-generated constructor stub
        /*
        try {
        mCommandShell = new CommandShell();
        init(context);
        } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        }*/
    }
    
    public KeysView(Context context, AttributeSet attrs) {
        // TODO Auto-generated constructor stub
        super(context, attrs);
        mContext = context;        
    }
    
    public KeysView(Context context, AttributeSet attrs, int defStyle) {
        // TODO Auto-generated constructor stub
        super(context, attrs, defStyle);
        mContext = context;      
    }

    private void init(Context context) {

        FrameLayout.LayoutParams layoutparams = new FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.FILL_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutparams);
        View root = View.inflate(context, R.layout.keyspanel, null);

        mButtonsView = root.findViewById(R.id.panel);

        int resid = 0;
        View btn = null;
        for(int i=0; i<resIds.length; i++) {
            resid = resIds[i];
            btn = mButtonsView.findViewById(resid);
            btn.setId(resid);
            btn.setOnTouchListener(this);
        }         
        mButtonsView.setVisibility(View.GONE);
        feedback = (Vibrator)context.getSystemService("vibrator");
        addView(root);
        //for test
         
    }
    
    public void setKeyViewVisiblity(int visible){
        /*
        if(mCommandShell == null || mCommandShell.mDataOut == null)
        return;
        */
        mButtonsView.setVisibility(visible);
        if(visible == View.VISIBLE) {
            objHandler.postDelayed(mTasks, KEY_VIEWS_DURATION);
        }
    }

    private void doKeyEvent(int value) {
        if (mCommandShell != null) {
            if (!mCommandShell.doKeysEvent(value))
                Toast.makeText(mContext, "adb shell is error", TOAST_DURATION).show();
        } else {
            Toast.makeText(mContext, "adb shell is error", TOAST_DURATION).show();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //v.setPressed(true);
                //liuaho fix vibrate when press  VIRTUAL_KEY home key
                final boolean vibarteOn = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 1;

                if ((v.getId() == R.id.home) && vibarteOn) {
                    feedback.vibrate(30L);
                }
                objHandler.removeCallbacks(mTasks);
                /*
                switch (v.getId()) {
                case R.id.menu:
                doKeyEvent(82);
                break;
                case R.id.home:
                doKeyEvent(3);
                break;
                case R.id.back:
                doKeyEvent(4);
                break;
                default:
                break;
                }
                return true;
                */
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //v.setPressed(false);
                objHandler.postDelayed(mTasks, KEY_VIEWS_DURATION);
                break;
        }
        return false;
    }
    
    private Runnable mTasks = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            StatusBarService.mSoftbuttonVisible = false;
            mButtonsView.setVisibility(View.GONE);    
        }
    };
    
    public class CommandShell {

        private static final String SHELL_KEYEVENT = "input keyevent ";

        public OutputStream mDataOut;

        CommandShell() throws Exception {
            Runtime.getRuntime().exec("sh");
            Process process = Runtime.getRuntime().exec("su0");
            mDataOut = process.getOutputStream();
        }

        public boolean doKeysEvent(int value) {

            String str = new StringBuilder(SHELL_KEYEVENT).append(value).toString();
            byte abyte[] = null;
            try {
                abyte = (new StringBuilder(str)).append("\n").toString().getBytes("ASCII");
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            try {
                mDataOut.write(abyte);
                return true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }

    }
}
