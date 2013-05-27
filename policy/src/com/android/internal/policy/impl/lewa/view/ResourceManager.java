package com.android.internal.policy.impl.lewa.view;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import com.android.internal.policy.impl.lewa.lockscreen.LockScreenResourceLoader;
import com.android.internal.policy.impl.lewa.lockscreen.LockScreenConstants;
import com.android.internal.policy.impl.LewaLockScreen;



import android.content.res.Resources;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import android.provider.Settings;

import android.util.Log;

public class ResourceManager {

    private static final String TAG = "ResourceManager";
    private static HashMap<String,Bitmap> bmpCache = new HashMap<String,Bitmap>();
    private static HashMap<String,NinePatch> mNinePatches = new HashMap<String,NinePatch>();
    private static SoftReference<Bitmap> mMaskBitmap;
    private static Element rootElement;
        
    public static Bitmap getBitmapFromCache(String imageName){
        
        Bitmap bitmap = null;
        if(TextUtils.isEmpty(imageName)){
           return null;
        } else {
            
            bitmap = (Bitmap)bmpCache.get(imageName);
           
            if(bitmap != null){
                return bitmap;
            }else {
                Log.i(TAG,"decode new image : " + imageName);
                bitmap = LockScreenResourceLoader.getBitmapFromZip(imageName);
                
                if(bitmap != null){
                    bmpCache.put(imageName, bitmap);
                }else {
                    Log.i(TAG, new StringBuilder().append("fail to load image: ").append(imageName).toString());
                }

            }
            
            return bitmap;
        }
        
    }

    /**
     * 如果锁屏更换了，则清除缓存
    */
    @SuppressWarnings("rawtypes")
    public static void clearCache(){

        Log.e(TAG,"clear cache");
        if(bmpCache != null){
            for(Entry<String,Bitmap> entry : bmpCache.entrySet()){
                Bitmap bitmap = entry.getValue();
                
                if(bitmap != null && !bitmap.isRecycled()){
                    bitmap.recycle();
                    bitmap = null;
                }
                
            }
            bmpCache.clear();
            //bmpCache = null;
        }

        mNinePatches.clear();
        //mNinePatches = null;

   
   }

    public static Drawable getDrawable(Resources resources, String imageName){
    
        Bitmap bitmap = getBitmapFromCache(imageName);
        Object obj = null;
        if(bitmap == null){
            obj = null;
        } else {
        
            if(bitmap.getNinePatchChunk() != null){
                byte[] abyte0 = bitmap.getNinePatchChunk();
                obj = new NinePatchDrawable(resources, bitmap, abyte0, new Rect(), imageName);
            } else{
                obj = new BitmapDrawable(resources, bitmap);
            }
        }
        return ((Drawable) (obj));
    }

    public static Element getManifestRoot(){
       
        if(rootElement == null || LewaLockScreen.lockscreenChanged == LockScreenConstants.LOCKSCREEN_CHANGED){
            rootElement = LockScreenResourceLoader.getManifestRoot();
        }
        
        return rootElement;
    }

    public static Bitmap getMaskBufferBitmap(int width, int height){
        Bitmap bitmap = null;
        if(mMaskBitmap != null){
            bitmap = (Bitmap)mMaskBitmap.get();
        }else{
            bitmap = null;
        }
        if(bitmap == null || bitmap.getHeight() < height || bitmap.getWidth() < width){
            bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            mMaskBitmap = new SoftReference<Bitmap>(bitmap);
        }
        return bitmap;
    }

    public static NinePatch getNinePatch(String ninePathName){
    
        NinePatch ninepatch = (NinePatch)mNinePatches.get(ninePathName);
        if(ninepatch == null){
        
            Bitmap bitmap = getBitmapFromCache(ninePathName);
            if(bitmap != null && bitmap.getNinePatchChunk() != null){
            
                byte abyte0[] = bitmap.getNinePatchChunk();
                ninepatch = new NinePatch(bitmap, abyte0, null);
                mNinePatches.put(ninePathName, ninepatch);
            }
        }
        return ninepatch;
    }
}
