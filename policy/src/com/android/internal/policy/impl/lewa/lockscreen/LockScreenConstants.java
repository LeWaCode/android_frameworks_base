package com.android.internal.policy.impl.lewa.lockscreen;


public class LockScreenConstants {
    public static final String DATA_SYSTEM_FACE = "/data/system/face";
    
    public static final String WALLPAPER_FILE_NAME_JPG = "lock_screen_wallpaper.jpg";
    
    public static final String WALLPAPER_FILE_NAME_PNG = "lock_screen_wallpaper.png";
    
    public static final String WALLPAPER_DIR_FP = "/data/system/face/wallpaper/";
    
    public static final String WALLPAPER_DIR_P = "wallpaper/";
    
    public static final String FACE_DEFAULT_THEME_FILE_FP =  "/system/media/default.lwt";
    
    public static final String FACE_LOCKSCREEN = "lockscreen";

    public static final String FACE_MAIN_XML = "face/main.xml";

    /**
    * 标识当前锁屏是否已经更改，如果更改则必须清除缓存，重新decode锁屏图片
    */
    public static final int LOCKSCREEN_CHANGED = 1;

    public static final int LOCKSCREEN_UNCHANGED = 0; 
}
