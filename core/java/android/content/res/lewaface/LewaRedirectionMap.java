package android.content.res.lewaface;

import java.util.HashMap;
import java.util.ArrayList;
import android.util.Log;
import android.os.Parcel;
import android.os.Parcelable;


public class LewaRedirectionMap implements Parcelable{

    public static final int LOCALE_DIR_NAME_LIST_MAX_SIZE = 64;
    public static final boolean DEBUG = false;
	public static final Parcelable.Creator<LewaRedirectionMap> CREATOR
            = new Parcelable.Creator<LewaRedirectionMap>() {
        public LewaRedirectionMap createFromParcel(Parcel in) {
            return new LewaRedirectionMap(in);
        }

        public LewaRedirectionMap[] newArray(int size) {
            return new LewaRedirectionMap[size];
        }
    };

	public static final String TAG = "LEWA/FACE";
	private String mPackageName;
	private HashMap<Integer, Long> mRedirectionMap;
    private ArrayList<String> mLocaleDirNames;
    private int mRedirectedCookie; // > 0 and 0 is a invalid value.

	 public LewaRedirectionMap() {
        
        mRedirectionMap = new HashMap<Integer, Long>();
        mLocaleDirNames = new ArrayList<String>();
        mRedirectedCookie = 0;
    }

	public LewaRedirectionMap(String packageName) {
        
        mRedirectionMap = new HashMap<Integer, Long>();
        mLocaleDirNames = new ArrayList<String>();
		mPackageName = packageName;
        mRedirectedCookie = 0;
    } 

    private LewaRedirectionMap(Parcel in) {
		
        readFromParcel(in);
    }

	public String getPackageName() {
		
		return mPackageName;
	}

	public void setPackageName(String packageName) {
		
		this.mPackageName = packageName;
	}
	
	public long lookupMap(int id){
        if(mRedirectionMap == null){
            if(DEBUG)Log.w(TAG, "error! mRedirectionMap == null, at "+mPackageName);
            return 0;
        }
		Long ret = mRedirectionMap.get(id);
		if(ret != null){
			return ret;
		}
		return 0;
	}
    public void setSetLocaleDirNames(ArrayList<String> dirNames){
        mLocaleDirNames = dirNames;
    }
	// dir like: res/drawable-zh-hdpi/dial_num_delete.png
	public boolean needRedirect(long attr, String fileName){
 		
        synchronized(mLocaleDirNames){
            String localeDir;
 		    int start, end;
     		start = fileName.indexOf('/');
     		end = fileName.lastIndexOf('/');
     		localeDir = fileName.substring(start+1, end);
     		if(localeDir == null){
                Log.e(TAG, "ERROR! needRedirect() can't get locale dir name @"+ fileName );
    			return false;
     		}
            if(mLocaleDirNames == null){
                Log.e(TAG, "ERROR! needRedirect() mLocaleDirNames = null");
                return false;
            }
            int size = mLocaleDirNames.size();
     		for(int i = 0; i < size; i++){
     			if((attr & (1<<i))==(1<<i)){
                    
     				if(localeDir.equals(mLocaleDirNames.get(i))){
     					return true;
     				}
     			}
     		}
    		Log.w(TAG, "can't find " + localeDir + " in attr:" + Long.toHexString(attr));
     		return false;
        }
 		
 	}
    
    
    @Override
    public int describeContents() {
    
        return 0;
    }
	
	public void clear(){
		if(mRedirectionMap != null){
			mRedirectionMap.clear();
			mRedirectionMap = null;
		}
        if(mLocaleDirNames != null){
            mLocaleDirNames.clear();
            mLocaleDirNames = null;
        }
	}
    public void readFromParcel(Parcel source) {
		
		int entrySize;
		
		entrySize = source.readInt(); 
		if(entrySize == 0){
			return;
		}
		mPackageName = source.readString();
		if(mRedirectionMap == null){
			mRedirectionMap = new HashMap<Integer, Long>();
		}
		int key;
		long value;
		for(int i = 0; i < entrySize; i++){
			key = source.readInt(); 
			value = source.readLong();
			mRedirectionMap.put(key, value);
		}

        int size = source.readInt();
        if(mLocaleDirNames == null){
            mLocaleDirNames = new ArrayList<String>(size);
        }
        for(int i = 0; i < size; i++){
            mLocaleDirNames.add(source.readString());
        }
        mLocaleDirNames.trimToSize();
        
    }
    
    @Override
     public void writeToParcel(Parcel dest, int flags) {
    	
		if(mPackageName == null || mPackageName.length() <=0 
            || mRedirectionMap == null || mRedirectionMap.size() <=0
            || mLocaleDirNames == null || mLocaleDirNames.size() <= 0){
			dest.writeInt(0);
		}else{
			dest.writeInt(mRedirectionMap.size());
			dest.writeString(mPackageName);	
			for(HashMap.Entry<Integer, Long> entry: mRedirectionMap.entrySet()){
				dest.writeInt(entry.getKey());
				dest.writeLong(entry.getValue());
			}
            int size = mLocaleDirNames.size();
            dest.writeInt(size);
            for(int i = 0; i < size; i++){
                dest.writeString(mLocaleDirNames.get(i));
            }
		}
		
       
    }
   
    public void setRedirectedCookie(int cookie){
        mRedirectedCookie = cookie;
    }

    public int getRedirectedCookie( ){
        return mRedirectedCookie;
    }
   	public HashMap<Integer, Long> getRedirectionMap(){
		return mRedirectionMap;
   	}

    public ArrayList<String> getLocaleDirNames(){
        return mLocaleDirNames;
    }
   

}
