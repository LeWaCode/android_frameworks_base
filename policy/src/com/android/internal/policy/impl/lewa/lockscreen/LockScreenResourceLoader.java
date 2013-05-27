package com.android.internal.policy.impl.lewa.lockscreen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.android.internal.policy.impl.lewa.view.Utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.MemoryFile;
import android.util.Log;

public class LockScreenResourceLoader{
    
    private static final String TAG = "LockScreenResourceLoader";
    //private static String lockscreenFile = "/system/media/lockscreen.zip";
    //private static String defaultTheme = "/system/media/default.lwt";

    private static ZipFile lockscreenZip = null;
    
    public LockScreenResourceLoader(){

		String lockscreenFileName = new StringBuilder().append(LockScreenConstants.DATA_SYSTEM_FACE).append("/")
            .append(LockScreenConstants.FACE_LOCKSCREEN).toString();
        File lockscreen = new File(lockscreenFileName);

		
		ZipFile defaultLwt = null;
		InputStream is_lockscreen = null;
		FileOutputStream fos_lockscreen = null;
		InputStream is_lockscreen_wallpaper = null;
		FileOutputStream fos_lockscreen_wallpaper = null;

        try{
			initFaceDir();
            if(lockscreen.exists()){
                lockscreenZip = new ZipFile(lockscreen);
				if(lockscreenZip == null){
					Log.e(TAG, "ERROR! lockscreen open failed.");
				}
            }else{
            	File defaultLwtFile = new File(LockScreenConstants.FACE_DEFAULT_THEME_FILE_FP);
            	defaultLwt = new ZipFile(defaultLwtFile);
				
				ZipEntry lockscreenZipEntry = defaultLwt.getEntry("lockscreen");
				if(lockscreenZipEntry != null){
					fos_lockscreen = new FileOutputStream(lockscreen);
					is_lockscreen = defaultLwt.getInputStream(lockscreenZipEntry);
					
					if(writeSourceToTarget(is_lockscreen,fos_lockscreen)){
						changeAccessPermission(lockscreenFileName);
						lockscreenZip = new ZipFile(lockscreen);
						if(lockscreenZip == null){
							Log.e(TAG, "ERROR! lockscreen open failed.");
						}
					}else{
						Log.e(TAG, "ERROR! writed lockscreen failed.");
					}
				}else{
					Log.e(TAG, "ERROR! read lockscreen failed.");
				}

				// if wallpaper dir don't exist, then create it
				
				
				// we search jpg file fist.
				String imgFileName = new StringBuilder(LockScreenConstants.WALLPAPER_DIR_FP).append(LockScreenConstants.WALLPAPER_FILE_NAME_JPG).toString();
				File lockscreenWallpaper = new File(imgFileName);
				ZipEntry lockscreenWallpaperEntry = defaultLwt.getEntry(LockScreenConstants.WALLPAPER_DIR_P+LockScreenConstants.WALLPAPER_FILE_NAME_JPG);
				if(lockscreenWallpaperEntry == null){
					// the jpg file don't exist search the png file.
					imgFileName = new StringBuilder(LockScreenConstants.WALLPAPER_DIR_FP).append(LockScreenConstants.WALLPAPER_FILE_NAME_PNG).toString();
					lockscreenWallpaper = new File(imgFileName);
					lockscreenWallpaperEntry = defaultLwt.getEntry(LockScreenConstants.WALLPAPER_DIR_P+LockScreenConstants.WALLPAPER_FILE_NAME_PNG);
				}
				if(lockscreenWallpaper != null && lockscreenWallpaperEntry != null){
					is_lockscreen_wallpaper = defaultLwt.getInputStream(lockscreenWallpaperEntry);
					fos_lockscreen_wallpaper = new FileOutputStream(lockscreenWallpaper);
				}

				
				if(is_lockscreen_wallpaper != null && fos_lockscreen_wallpaper != null){
					
					if(writeSourceToTarget(is_lockscreen_wallpaper, fos_lockscreen_wallpaper) == false){
						Log.e(TAG, "ERROR! lockscreen wallpaper writed failed.");
					}
					changeAccessPermission(imgFileName);
					
				}else{
					Log.e(TAG, "ERROR! can't get lockscreen Wallpaper or open file failed.");
				}
                
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
			try{
				if(fos_lockscreen != null){
					fos_lockscreen.close();
					fos_lockscreen = null;
				}
				if(is_lockscreen != null){
					is_lockscreen.close();
					is_lockscreen = null;
				}
				if(fos_lockscreen_wallpaper != null){
					fos_lockscreen_wallpaper.close();
					fos_lockscreen_wallpaper = null;
				}
				if(is_lockscreen_wallpaper != null){
					is_lockscreen_wallpaper.close();
					is_lockscreen_wallpaper = null;
				}
				
				if(defaultLwt != null){
					defaultLwt.close();
					defaultLwt = null;
				}
			}catch(Exception e1){
				 e1.printStackTrace();
			}
		}
    }

	private boolean writeSourceToTarget(InputStream source,FileOutputStream target){
        BufferedOutputStream bos = null;
        
        try {
            bos = new BufferedOutputStream(target);
            byte[] buffer = new byte[2048];
            int temp = -1;
            while((temp = source.read(buffer)) != -1){
                bos.write(buffer, 0, temp);
            }
            bos.flush();
            target.flush();
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
         	try {
                if(bos != null){
                    bos.close();
                    bos = null;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return false;
    }

    
    public static Options getOptions(){
        
        Options options = new Options();
        
        options.inJustDecodeBounds = false;
        options.inPurgeable=true;
        options.inInputShareable = true;
        options.inDither = false;
        
        return options;
    }
    
    public static Bitmap getBitmapFromZip(String imageName) {
        InputStream is = null;
        ZipEntry zipEntry = null;
		if(lockscreenZip == null){
			return null;
		}
        try {
            zipEntry = lockscreenZip.getEntry(new StringBuilder().append("face/").append(imageName).toString());
            if(zipEntry == null){
                return null;
            }
            is = lockscreenZip.getInputStream(zipEntry);
            
            return BitmapFactory.decodeStream(is, null, getOptions());
        } catch (Exception e) {
            // TODO: handle exception
        }finally{
            try {
                if(is != null){
                    is.close();
                    is = null;
                }
                if(zipEntry != null){   
                    zipEntry = null;
                }
                
            } catch (Exception e2) {
                // TODO: handle exception
            }
        }
        return null;
    }


    /**
    *
    */
    private static ZipEntry containLockScreen(){
        ZipEntry zipEntry = null;
        try{
            if(lockscreenZip != null){
                zipEntry = lockscreenZip.getEntry(LockScreenConstants.FACE_MAIN_XML);
                if(zipEntry != null){
                    return zipEntry;
                }
            }
        }catch(Exception e){
            Log.e(TAG,e+"");
            e.printStackTrace();
        }
        return null;
    }

	private static void changeAccessPermission(String fileName){
		StringBuilder sb = new StringBuilder("chmod 777 ");
		sb.append(fileName);
		Utils.runShellBat(sb.toString());
	}
	private static void initFaceDir(){
		File face = new File(LockScreenConstants.DATA_SYSTEM_FACE);
		if(!face.exists()){
            Log.i(TAG, "create face dir");
            face.mkdirs();
            changeAccessPermission(LockScreenConstants.DATA_SYSTEM_FACE);
        }
		File wallPaperDir = new File(LockScreenConstants.WALLPAPER_DIR_FP);
		if(!wallPaperDir.exists()){
			wallPaperDir.mkdir();
			changeAccessPermission(LockScreenConstants.WALLPAPER_DIR_FP);
		}
		
	}
    /**
    *ÐÞ¸Ä
    */
    public static Element getManifestRoot() {
        InputStream inputstream = null;
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
            DocumentBuilderFactory documentbuilderfactory = DocumentBuilderFactory.newInstance();
            documentbuilderfactory.setIgnoringComments(true);
            documentbuilderfactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder documentbuilder = documentbuilderfactory.newDocumentBuilder();

            //File advance = new File("/data/system/advance");
            
            //if(advance.exists()){
            //    Utils.runShellBat("rm -rvf /data/system/advance");
            //}

            ZipEntry main_xml = containLockScreen();

            
            
            File main_xml_file = new File("/data/system/face/main.xml");
            
            long mainFileLastModifyTime = main_xml_file.lastModified();
            long zipMainFileLastModifyTime = -1;
            
            if(main_xml != null){
                zipMainFileLastModifyTime = main_xml.getTime();
            }

            if(!main_xml_file.exists() || (mainFileLastModifyTime != zipMainFileLastModifyTime)){
                File face = new File("/data/system/face");
                if(!face.exists()){
                    Log.i(TAG, "create face dir");
                    face.mkdirs();
                    Utils.runShellBat("chmod 777 /data/system/face");
                }
                inputstream = getLockscreenFileStream("main.xml");
                
                if(inputstream == null){
                    return null;
                }

                fos = new FileOutputStream(main_xml_file);
                byte[] buffer = new byte[1024*4];
                bis = new BufferedInputStream(inputstream);
                int temp = -1;
                while ((temp = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, temp);
                }
                fos.flush();
                
            }

            Document document = documentbuilder.parse(main_xml_file);
            document.normalize();
            Element element = document.getDocumentElement();



            if(element.getNodeName().equals("Lockscreen")){
                return element;
            }
            
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,e+"");
            e.printStackTrace();
        }finally{
            try{
                if(bis != null){
                    bis.close();
                    bis = null;
                }
                if(fos != null){
                    fos.close();
                    fos = null;
                }
                if(inputstream != null){
                    inputstream.close();
                    inputstream = null;
                }
                
            }catch(IOException e){
                e.printStackTrace();
               }
        }
        
        return null;
    }

   
    public static InputStream getLockscreenFileStream(String fileName){

		if(lockscreenZip == null){
			return null;
		}
        try {
            ZipEntry zipentry = lockscreenZip.getEntry(new StringBuilder().append("face/").append(fileName).toString());
            if(zipentry != null){

                return lockscreenZip.getInputStream(zipentry);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
        
    }
   

}

