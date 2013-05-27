package android.content.res.lewaface;

import android.os.SystemProperties;
import java.util.ArrayList;
import android.util.Log;

public class LewaTheme implements Cloneable{
    private static final String TAG = "LewaTheme";
	private String mThemeName;
	private ArrayList<String> mResModelNames;
	public static final int MAX_MODEL = 100;
	public static final String LEWA_THEME_RES_PATH = "/data/system/face/";
	public static final String LEWA_THEME_VALUES_FILE = "values.xml";
	public static final String LEWA_THEME_PROPERTY_NAME = "persist.sys.LWTName";
	public static final String LEWA_THEME_PROPERTY_MODEL_NUM = "persist.sys.LWTModelNum";
    public static final int LEWA_THEME_PROPERTY_MODEL_PAGE_NUM = 3;
	public static final String LEWA_THEME_PROPERTY_MODEL_PAGE = "persist.sys.LWTModelNames_P";
    public static final int LEWA_THEME_LENGTH_PER_PAGE = 91;
    public static final int[] MAP_KEYS = {0, "android".hashCode(), "app".hashCode()};
    public static final int KEY_FOR_ANROID = 1;
    public static final int KEY_FOR_APP = 2;
    public static final boolean TRACE_DEBUG = false;
    
	public static final long LEWA_COLOR_MARK = 0x100000000L;
	
	private static final LewaTheme sBootTheme = new LewaTheme(true);
    private static final LewaTheme sSystemTheme = new LewaTheme();
	
	public LewaTheme()
	{

	}

	public LewaTheme(boolean loadSaved){
		if(loadSaved){
			load();
		}
	}
    
	public LewaTheme(String themeName, String modelNames, String modelNumStr){
		setLewaTheme(themeName, modelNames, modelNumStr);
	}
    // use themeName model Names str and model num to set this LewaTheme.
    // the modelNames this a string contain models names and split by ','.
	public void setLewaTheme(String themeName, String modelNames, String modelNumStr){
		if(themeName == null || themeName.length() == 0
			|| modelNames == null || modelNames.length() == 0 
			|| modelNumStr == null || modelNumStr.length() == 0){
			return;
		}
        mThemeName = themeName;
		int modelNum = Integer.valueOf(modelNumStr);
		if(modelNum <= 0 || modelNum > MAX_MODEL){
			return;
		}
		String[] modelNamesStr = modelNames.split(",");
		if(modelNum != modelNamesStr.length){
			return;
		}
		if(mResModelNames == null){
			mResModelNames = new ArrayList<String>();
		}
       for(int i = 0; i < modelNamesStr.length; i ++){
    	   mResModelNames.add(modelNamesStr[i]);
       }
		
	}
    // check if this is a valid LewaTheme.
	public boolean isValid(){
        if((mThemeName != null && mThemeName.length() > 0)
            && (mResModelNames != null && mResModelNames.size() > 0)){
            return true;
        }else{
            return false;
        }
	}
    // get Theme name
	public String getThemeName() {
		return mThemeName;
	}
    // set Theme Name
	public void setThemeName(String themeName) {
		this.mThemeName = themeName;
	}
	// get model names string, in this String, model name split by ','
	public String getResModelNamesStr(){
		StringBuilder sb;
		
		if(mResModelNames == null || mResModelNames.size() <= 0){
			return "";
		}
		sb = new StringBuilder();
		for(int i = 0; i < mResModelNames.size(); i++)
		{
			if(sb.length() > 0){
				sb.append(',');
			}
			sb.append(mResModelNames.get(i));
			
		}
		return sb.toString();
	}
    // get theme id, different theme has different id.
	public String getId(){
		if(mThemeName == null){
			return "(invalid)";
		}		
		return new StringBuilder(Integer.toHexString(mThemeName.hashCode())).append(Integer.toHexString(hashCode())).toString();
	}
    // get model name ArrayList.
	public ArrayList<String> getResModelNames() {
		return mResModelNames;
	}
    // set model names ArrayList
	public void setResModelNames(ArrayList<String> resModelNames) {
		this.mResModelNames = resModelNames;
	}
    // add a model to this LewaTheme
	public void addModelName(String modelName){
		if(modelName == null){
			return;
		}
		if(mResModelNames == null){
			mResModelNames = new ArrayList<String>();
		}
		if(!isSupportTheme(modelName)){
			mResModelNames.add(modelName);
		}
		
	}
    // check if this LewaTheme contain thsi model.
	public boolean isSupportTheme(String model)
	{
		for(int i = 0; i < mResModelNames.size(); i++)
		{
			if(mResModelNames.get(i).equals(model)){
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if(mThemeName == null){
			sb.append("null, ");
	
		}else{
		    sb.append(mThemeName);
        }
		if(isValid() == false){
			sb.append("invalid, ");	
		}
		if(mResModelNames != null){
			sb.append("modelnum=");
			sb.append(mResModelNames.size());
			sb.append(", models: ");
			for(int i = 0; i < mResModelNames.size(); i++){
				sb.append(mResModelNames.get(i));
				sb.append(" ");
			}

		}
		return sb.toString();
		
	}
    
	@Override
    public Object clone() {
        try {
			// TODO: just super clone?
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {

	 	if (object == this) {
            return true;
        }
		if(object == null){
			return false;
		}
        if (object instanceof LewaTheme) {
            LewaTheme o = (LewaTheme) object;
			String thisId = getId();
			
			if(!thisId.equals(o.getId())){
				return false;
			}
			return true;
        }
		return false;
    }
    
	@Override
	public int hashCode(){
	
		if( mThemeName == null){
			return 0;
		}
	 	int hashcode = mThemeName.hashCode();
        
	    if(mResModelNames != null){
    		for(int i = 0 ; i < mResModelNames.size(); i++){
    			hashcode += mResModelNames.get(i).hashCode()*(i+1);	
    		}
        }
		return hashcode;
	}
     // save theme name and modles name to SystemProperties.
    public void save(){

        if(isValid()){
            SystemProperties.set(LEWA_THEME_PROPERTY_NAME, mThemeName);
            SystemProperties.set(LEWA_THEME_PROPERTY_MODEL_NUM, String.valueOf(mResModelNames.size()));
            int size = mResModelNames.size();
   
            int index = 0;
            for(int page = 0; page < LEWA_THEME_PROPERTY_MODEL_PAGE_NUM; page++){
                String pageKey = new StringBuilder(LEWA_THEME_PROPERTY_MODEL_PAGE).append(page).toString();
                StringBuilder pageVal = new StringBuilder("");
                for(; index < size; index++){
                    String item = mResModelNames.get(index);
                    if((pageVal.length() + item.length() + 1) < LEWA_THEME_LENGTH_PER_PAGE){
                        pageVal.append(item);
                        if(index != (size - 1)){
                            pageVal.append(',');
                        }
                    }else{
                        break;
                    }
                }
               
                SystemProperties.set(pageKey, pageVal.toString()); 
            }
            if(index != size){
                Log.e(TAG, "save LewaTheme failded, to many models.");
            }
        }else{
    		if(mThemeName != null){
    			SystemProperties.set(LewaTheme.LEWA_THEME_PROPERTY_NAME, mThemeName);
    		}else{
    			SystemProperties.set(LewaTheme.LEWA_THEME_PROPERTY_NAME, "Default");
    		}
    		for(int page = 0; page < LEWA_THEME_PROPERTY_MODEL_PAGE_NUM; page++){
                String pageKey = new StringBuilder(LEWA_THEME_PROPERTY_MODEL_PAGE).append(page).toString();
                SystemProperties.set(pageKey, "");
            }
        }
    }
    // load theme name and modles name from SystemProperties.
    public void load(){
        mThemeName = SystemProperties.get(LEWA_THEME_PROPERTY_NAME);
		String modelNumStr = SystemProperties.get(LEWA_THEME_PROPERTY_MODEL_NUM);
        StringBuilder sb = new StringBuilder();
        for(int page = 0; page < LEWA_THEME_PROPERTY_MODEL_PAGE_NUM; page++){
            String pageKey = new StringBuilder(LEWA_THEME_PROPERTY_MODEL_PAGE).append(page).toString();
            String val = SystemProperties.get(pageKey);
            if(val != null){
                sb.append(val);
            }
        }
        if(TRACE_DEBUG)Log.i(TAG, "modelNames:"+sb.toString());
		setLewaTheme(mThemeName, sb.toString(), modelNumStr);
    }
    
	public static LewaTheme getBootTheme(){
		return sBootTheme;
	}

	public static LewaTheme getSystemTheme(){
		return sSystemTheme;
	}

    public final static int mTraceId[] = getTraceId();
    public static final String LEWA_THEME_TRACE_ID = "persist.sys.TraceIds";
    
    public static void log(int id, String msg){
        
        if(mTraceId != null){
            for(int i = 0; i < mTraceId.length; i++){            
                if(mTraceId[i] == id){
                    Log.i("traceId", new StringBuilder().append(id).append(':').append(msg).toString());
                }
            }
        
        }
        
    }

    public static void log(String msg){
        
        if(mTraceId != null && mTraceId.length > 0){
            Log.i("traceId", msg);
        }
        
    }

    
    public static int[] getTraceId(){
        String traceIdStr = SystemProperties.get(LEWA_THEME_TRACE_ID);
        if(traceIdStr == null || traceIdStr.length() <= 0){
            return null;
        }
        String[] traceIds = traceIdStr.split(",");
        if(traceIds.length > 0){
            int ids[] = new int[traceIds.length];
            for(int i = 0; i < traceIds.length; i++){
                if(traceIds[i] != null){
                    ids[i] = Integer.valueOf(traceIds[i]);
                    Log.i("traceId", "add traceId:"+ids[i]);
                }else{
                    ids[i] = 0;
                }
            }
            return ids;
        }
        return null;
    }
}
