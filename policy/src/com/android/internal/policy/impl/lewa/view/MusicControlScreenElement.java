package com.android.internal.policy.impl.lewa.view;

import org.w3c.dom.Element;

import com.android.internal.policy.impl.lewa.view.ButtonScreenElement.ButtonActionListener;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class MusicControlScreenElement extends ElementGroup implements ButtonActionListener{

    private static final String TAG = "MusicControlScreenElement";
    
    public static final String TAG_NAME = "MusicControl";
    
    private static final String BUTTON_MUSIC_ALBUM_COVER = "music_album_cover";
    private static final String BUTTON_MUSIC_DISPLAY = "music_display";
    private static final String BUTTON_MUSIC_NEXT = "music_next";
    private static final String BUTTON_MUSIC_PAUSE = "music_pause";
    private static final String BUTTON_MUSIC_PLAY = "music_play";
    private static final String BUTTON_MUSIC_PREV = "music_prev";
    
    private static final int CHECK_STREAM_MUSIC_DELAY = 1000;
    
    private static final int MUSIC_SHOW_NONE = 0;
    private static final int MUSIC_SHOW_PLAY = 2;
    private static final int MUSIC_SHOW_STOP = 1;
    
    private Bitmap mAlbumCoverBm;
    private String mAlbumName;
    private String mArtistName;
    private boolean mAutoShow;
    
    private ButtonScreenElement mButtonNext;
    private ButtonScreenElement mButtonPause;
    private ButtonScreenElement mButtonPlay;
    private ButtonScreenElement mButtonPrev;
    private ImageScreenElement mImageAlbumCover;
    private TextScreenElement mTextDisplay;
    
    
    private Bitmap mDefaultAlbumCoverBm;
    private Handler mHandler;
    
    private boolean mIsOnlineSongBlocking;
    private int mMusicStatus;
    
    //private BroadcastReceiver mPlayerStatusListener;
    private Runnable mCheckStreamMusicRunnable;
    private Runnable mMusicState;
    
    private final String MUSIC_PAUSE_STOP = "0";
    private final String MUSIC_PLAY = "1";
	private int mPressedKeyCode = -1;
    
    private final AudioManager audioManager;
    
    public MusicControlScreenElement(Element element,ScreenContext screenContext)
            throws DomParseException {
        super(element,screenContext);
        
        audioManager = (AudioManager) screenContext.mContext.getSystemService(Context.AUDIO_SERVICE);
        
        mHandler = new Handler();
        //mPlayerStatusListener = new PlayerStatusListener();
        mCheckStreamMusicRunnable = new CheckStreamMusicRunnable();
        
        int size = mElements.size();
        for(int i=0;i<size;i++){
            ScreenElement screenElement = mElements.get(i);
            String nodeName = screenElement.getName();
            if(nodeName.equals(BUTTON_MUSIC_PREV)){
                mButtonPrev = (ButtonScreenElement) screenElement;
            }else if(nodeName.equals(BUTTON_MUSIC_NEXT)){
                mButtonNext = (ButtonScreenElement) screenElement;
            }else if(nodeName.equals(BUTTON_MUSIC_PLAY)){
                mButtonPlay = (ButtonScreenElement) screenElement;
            }else if(nodeName.equals(BUTTON_MUSIC_PAUSE)){
                mButtonPause = (ButtonScreenElement) screenElement;
            }else if(nodeName.equals(BUTTON_MUSIC_DISPLAY)){
                mTextDisplay = (TextScreenElement) screenElement;
            }else if(nodeName.equals(BUTTON_MUSIC_ALBUM_COVER)){
                mImageAlbumCover = (ImageScreenElement) screenElement;
            }
        }
        
        if(mButtonPrev == null || mButtonNext == null || mButtonPlay == null || mButtonPause == null){
            throw new DomParseException("invalid music control");
        }

        setupButton(mButtonPrev);
        setupButton(mButtonNext);
        setupButton(mButtonPlay);
        setupButton(mButtonPause);
        
        mButtonPause.show(false);
        
        if(mImageAlbumCover != null){
            mDefaultAlbumCoverBm = BitmapFactory.decodeResource(screenContext.mContext.getResources(), 1);
        }
        mAutoShow = Boolean.parseBoolean(element.getAttribute("autoShow"));
    }
    
    private void setupButton(ButtonScreenElement buttonscreenelement){
        if(buttonscreenelement != null){
            buttonscreenelement.setListener(this);
            buttonscreenelement.setParent(this);
        }
    }
    
    private int getKeyCode(String btnName){
    
        byte keyCode = KeyEvent.KEYCODE_UNKNOWN;
        if(BUTTON_MUSIC_PREV.equals(btnName)){
            keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        }else if(BUTTON_MUSIC_NEXT.equals(btnName)){
            keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
        }else if(BUTTON_MUSIC_PLAY.equals(btnName) || BUTTON_MUSIC_PAUSE.equals(btnName)){
            keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        }else{
            keyCode = KeyEvent.KEYCODE_UNKNOWN;
        }
        return keyCode;
    }

    private void requestAlbum(){
        if(mImageAlbumCover != null){
            Intent intent = new Intent("lockscreen.action.SONG_METADATA_REQUEST");
            context.sendBroadcast(intent);
        }
    }

    private void requestAlbum(Intent intent){
        requestAlbum(intent, false);
    }

    private void requestAlbum(Intent intent, boolean flag){
    
        if(mImageAlbumCover == null){
            return;
        }
        String album = intent.getStringExtra("album");
        String artist = intent.getStringExtra("artist");
        if(!flag){
            if(Utils.equals(album, mAlbumName)){
                if(Utils.equals(artist, mArtistName) && mAlbumCoverBm != null){
                    return;
                }
            }
        }
        
        Uri uri = (Uri)intent.getParcelableExtra("album_uri");
        String album_path = intent.getStringExtra("album_path");
        mAlbumCoverBm = null;
        if(uri != null || album_path != null){
            requestAlbum();
        } else{
            mImageAlbumCover.setImageScreenBitmap(mDefaultAlbumCoverBm);
        }
    }

    private void setAlbumCover(Intent intent){
    
        if(mImageAlbumCover == null){
            return;
        }
        mAlbumName = intent.getStringExtra("album");
        mArtistName = intent.getStringExtra("artist");
        String tmp_album_path = intent.getStringExtra("tmp_album_path");
        if(tmp_album_path != null){
            mAlbumCoverBm = BitmapFactory.decodeFile(tmp_album_path);
        }
        Bitmap coverBitmap = null;
        if(mAlbumCoverBm != null){
            coverBitmap = mAlbumCoverBm;
        }else{
            coverBitmap = mDefaultAlbumCoverBm;
        }
        mImageAlbumCover.setImageScreenBitmap(coverBitmap);
        
        screenContext.mShouldUpdate = true;
       
    }

    

    private void updateMusic(){
    
        boolean isPauseing = false;
        boolean isPlaying = audioManager.isMusicActive();
        boolean playBtnShow = false;
        if(!isPlaying){
            playBtnShow = true;
        }else{
            playBtnShow = false;  
        }
        if(mIsOnlineSongBlocking){
            playBtnShow = false;
        }
        mButtonPlay.show(playBtnShow);
        
        if(!playBtnShow){
            isPauseing = true; 
        }
        mButtonPause.show(isPauseing);
        
        switch(mMusicStatus){
        case MUSIC_SHOW_PLAY:
            if(!isPlaying){
                mMusicStatus = 1;
                Expression.putRealTimeVar(mName, Expression.MUSIC_STATE, MUSIC_PAUSE_STOP);
            } 
            break;
        case MUSIC_SHOW_STOP:
            if(isPlaying){
                mMusicStatus = 2;
                Expression.putRealTimeVar(mName, Expression.MUSIC_STATE, MUSIC_PLAY);
            }
            break;
        default:
            return;

        }  
    }

    public void finish(){
        screenContext.mView.removeCallbacks(mCheckStreamMusicRunnable);
        screenContext.mView.removeCallbacks(mMusicState);
        //context.unregisterReceiver(mPlayerStatusListener);
    }

    public void init(){
        super.init();
        
        /*IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction("com.miui.player.metachanged");
        intentfilter.addAction("lockscreen.action.SONG_METADATA_UPDATED");
        intentfilter.addAction("com.miui.player.refreshprogress");
        intentfilter.addAction("com.miui.player.playstatechanged");
        
        context.registerReceiver(mPlayerStatusListener, intentfilter, null, mHandler);*/
        
        boolean isPlaying = audioManager.isMusicActive();
        if(isPlaying){
            mMusicStatus = MUSIC_SHOW_PLAY;
            Intent song_metadata_request = new Intent("lockscreen.action.SONG_METADATA_REQUEST");
            context.sendBroadcast(song_metadata_request);
            
            if(mAutoShow){
                show(true);
            }
        }
        
        
        String music_state = MUSIC_PAUSE_STOP;
        if(isPlaying){
            music_state = MUSIC_PLAY; 
        }
        
        Expression.putRealTimeVar(mName, Expression.MUSIC_STATE, music_state);
        
    }

    public boolean onButtonDoubleClick(String btnName){
        return false;
    }
    public boolean onButtonLongClick(String btnName){
        return false;
    }

    public boolean onButtonDown(String btnName){
    
        int keyCode = getKeyCode(btnName);
		
		mPressedKeyCode = keyCode;
		
        if(keyCode != -1){
            sendMediaButtonBroadcast(KeyEvent.ACTION_DOWN, keyCode);
        }
        return false;
    }
    
    public boolean onButtonUp(String btnName){
    
        int keyCode = getKeyCode(btnName);
        
        boolean flag = false;
        
        if(keyCode != -1 && keyCode == mPressedKeyCode){
 
            sendMediaButtonBroadcast(KeyEvent.ACTION_UP, keyCode);
			mPressedKeyCode = -1;
            View view = screenContext.mView;
            view.removeCallbacks(mMusicState);
                
            mMusicState = new MusicStateRunnable(btnName);
            
            view.post(mMusicState);
            flag = true;
        } else{
            flag = false;
        }
        return flag;
    }

    private void sendMediaButtonBroadcast(int action, int keyCode){

        long eventtime = SystemClock.uptimeMillis();

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent keyevent = new KeyEvent(eventtime, eventtime, action, keyCode, 0);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyevent);
        intent.putExtra("permission", "lewa.permission.ACTION_MEDIA_BUTTON");
        context.sendOrderedBroadcast(intent, "lewa.permission.ACTION_MEDIA_BUTTON");
    }
    
    public void pause(){
        screenContext.mView.removeCallbacks(mCheckStreamMusicRunnable);
    }

    public void resume(){
        screenContext.mView.removeCallbacks(mCheckStreamMusicRunnable);
        screenContext.mView.postDelayed(mCheckStreamMusicRunnable, 1000L);
    }

    public void show(boolean isShow){
        
        super.show(isShow);
        if(!isShow){
            mMusicStatus = 0;
            screenContext.mView.removeCallbacks(mCheckStreamMusicRunnable);
        } else{
            
            updateMusic();
            screenContext.mView.removeCallbacks(mCheckStreamMusicRunnable);
            screenContext.mView.postDelayed(mCheckStreamMusicRunnable, 1000L);
        }
        String visibility = "";
        if(isShow){
            visibility = "true";
        }else {
            visibility = "false";
        }
        Expression.put("visibility", visibility);
    }
    
    
    private class CheckStreamMusicRunnable implements Runnable{

        public void run(){
            
            updateMusic();
            
            screenContext.mView.postDelayed(this, CHECK_STREAM_MUSIC_DELAY);
        }
    }


    private class MusicStateRunnable implements Runnable{
    
        private String btnName;
        
        public MusicStateRunnable(String btnName){
            this.btnName = btnName;
        }
    
        public void run(){
        
            if(BUTTON_MUSIC_PLAY.equals(btnName)){
                mButtonPlay.show(false);
                mButtonPause.show(true);
                mMusicStatus = MUSIC_SHOW_STOP;
                Expression.putRealTimeVar(mName, Expression.MUSIC_STATE, MUSIC_PAUSE_STOP);
                
            }else if(BUTTON_MUSIC_PAUSE.equals(btnName)){
                mButtonPlay.show(true);
                mButtonPause.show(false);
                mMusicStatus = MUSIC_SHOW_PLAY;
                
                Expression.putRealTimeVar(mName, Expression.MUSIC_STATE, MUSIC_PLAY);
            }
            
            View view = screenContext.mView;
            view.removeCallbacks(mCheckStreamMusicRunnable);
            view.postDelayed(mCheckStreamMusicRunnable, 3000L);

        }
    }

}
