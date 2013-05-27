package com.android.internal.policy.impl.lewa.lockscreen;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;

public class SoundManager implements OnLoadCompleteListener{

    private HashMap<Integer,Boolean> mPendingSoundMap;
    private ArrayList<Integer> mPlayingSoundMap;
    private SoundPool mSoundPool;
    private HashMap<String,Integer> mSoundPoolMap;
    
    public SoundManager(Context context){
    
        mSoundPoolMap = new HashMap<String,Integer>();
        mPendingSoundMap = new HashMap<Integer,Boolean>();
        mPlayingSoundMap = new ArrayList<Integer>();
        mSoundPool = new SoundPool(4, AudioManager.STREAM_SYSTEM, 100);
        mSoundPool.setOnLoadCompleteListener(this);
    }

    private void playSoundImp(int sampleId, boolean flag){
    
        if(flag && mPlayingSoundMap.size() != 0){
            int size = mPlayingSoundMap.size();
            for(int i=0;i<size;i++){
                mSoundPool.stop(mPlayingSoundMap.get(i));
            }
            mPlayingSoundMap.clear();
        }
        
        int success = mSoundPool.play(sampleId, 1F, 1F, 1, 0, 1F);
        mPlayingSoundMap.add(Integer.valueOf(success));
    }

    /**
     * Called when a sound has completed loading.
     *
     * @param soundPool SoundPool object from the load() method
     * @param soundPool the sample ID of the sound loaded.
     * @param status the status of the load operation (0 = success)
     */
    public void onLoadComplete(SoundPool soundpool, int sampleId, int status){
    
        Integer sample_Id = Integer.valueOf(sampleId);
        if(status == 0){
            boolean flag = ((Boolean)mPendingSoundMap.get(sample_Id)).booleanValue();
            playSoundImp(sample_Id, flag);
        }
        mPendingSoundMap.remove(sample_Id);
    }

    public void playSound(String soundName, boolean flag){
    
        if(mSoundPool == null){
            return;
        }
       
        Integer integer = (Integer)mSoundPoolMap.get(soundName);
        if(integer == null){
        
            InputStream is = null;
            FileOutputStream fos = null;
            BufferedInputStream bis = null;
            try {
                is = LockScreenResourceLoader.getLockscreenFileStream(soundName);
                String path = "/data/system/face/" + soundName;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024*10];
                bis = new BufferedInputStream(is);
                int temp = -1;
                while ((temp = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, temp);
                }
                fos.flush();
                
                int soundID = mSoundPool.load(path, 1);
                mSoundPoolMap.put(soundName, soundID);
                
                mPendingSoundMap.put(soundID, flag);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }finally{
                try {
                    if(fos != null){
                        fos.close();
                        fos = null;
                    }
                    if(bis != null){
                        bis.close();
                        bis = null;
                    }
                    if(is != null){
                        is.close();
                        is = null;
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
           
        } else{
            playSoundImp(integer, flag);
        }
    }

    public void release(){
        if(mSoundPool != null){
            mSoundPool.release();
            mSoundPool = null;
        }
        if(mPendingSoundMap != null){
            mPendingSoundMap.clear();
            mPendingSoundMap = null;
        }
        if(mPlayingSoundMap != null){
            mPlayingSoundMap.clear();
            mPlayingSoundMap = null;
        }
        if(mSoundPoolMap != null){
            mSoundPoolMap.clear();
            mSoundPoolMap = null;
        }
    }
}
