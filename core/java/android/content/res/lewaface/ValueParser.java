package android.content.res.lewaface;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;
import android.content.res.Resources;

public class ValueParser {
    public static final boolean DEBUG = true;
	public static final String TAG = "LWFACE/VP";
	private HashMap<String, String> mVariables;
	private InputStream mIs;
	private Resources mRes;
	private HashMap<Integer, Long> mResMap;
	private String mPackageName;
    
	
	public ValueParser(InputStream is, Resources res, String packageName, HashMap<Integer, Long> rMap){
		if(is == null || res == null || rMap == null){
			Log.e(TAG, "ERROR! invalid pramam.");
			return;
		}
		mResMap = rMap;
		mRes = res;
		mVariables = new HashMap<String, String>();
		mIs = is;
		mPackageName = packageName;
	}
	
	public void parseValues()
	{
		XmlPullParser parser = Xml.newPullParser();  
		try {
			parser.setInput(mIs, "UTF-8");
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}  
		int event;
		try {
			event = parser.getEventType();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}//产生第一个事件  
		
		ValueItem item = null;
		String fullVariableName = null;
		String VariableValue = null;
		if(mVariables == null){
			return;
		}
        while(event!=XmlPullParser.END_DOCUMENT){  
            switch(event){
            
            case XmlPullParser.TEXT:
            	
            	if(item != null){
            		if(parser.getText() == null){
            			Log.e(TAG, "ERROR! item " + item.name + " value = null");
            			continue;
            		}
            		item.value = parser.getText();
            	}else if(fullVariableName != null){
            		VariableValue = parser.getText();
            	}
            	break;
            case XmlPullParser.START_DOCUMENT://判断当前事件是否是文档开始事件  
                
                break;  
            case XmlPullParser.START_TAG://判断当前事件是否是标签元素开始事件  
            	if(parser.getName().equals("item")){
            		
            		item = new ValueItem();
            		item.name = parser.getName();
            		for(int i = 0; i <  parser.getAttributeCount(); i++){
            	
                		if(parser.getAttributeName(i).equals("name")){
                			item.valueName = parser.getAttributeValue(i);
                		}else if(parser.getAttributeName(i).equals("type")){
                			item.type = parser.getAttributeValue(i);
                		}
                		
                	}
            		if(item.type != null){
            			StringBuilder sb = new StringBuilder(item.type);
            			sb.append("/");
            			sb.append(item.valueName);
            			item.fullValueName = sb.toString();
            		}else{
            			item.fullValueName = item.valueName;
            		}
            	}else if(parser.getName().equals("variable")){
            		String type = null;
            		String valueName = null;
            		for(int i = 0; i <  parser.getAttributeCount(); i++){
                    	
                		if(parser.getAttributeName(i).equals("name")){
                			valueName = parser.getAttributeValue(i);
                		}else if(parser.getAttributeName(i).equals("type")){
                			type = parser.getAttributeValue(i);
                		}
                		
                	}
            		if(type != null){
            			StringBuilder sb = new StringBuilder(type);
            			sb.append("/");
            			sb.append(valueName);
            			fullVariableName = sb.toString();
            		}else{
            			fullVariableName = valueName;
            		}
            	}else{
            		item = new ValueItem();
            		item.name = parser.getName();
            		for(int i = 0; i <  parser.getAttributeCount(); i++){
            	
                		if(parser.getAttributeName(i).equals("name")){
                			item.valueName = parser.getAttributeValue(i);
                		}else if(parser.getAttributeName(i).equals("type")){
                			item.type = parser.getAttributeValue(i);
                		}
                		
                	}
            		if(item.valueName != null){
            			StringBuilder sb = new StringBuilder(item.name);
            			sb.append("/");
            			sb.append(item.valueName);
            			item.fullValueName = sb.toString();
            		}else{
            			item.fullValueName = item.valueName;
            		}
            	}
            	
                break;  
            case XmlPullParser.END_TAG://判断当前事件是否是标签元素结束事件
            	if(fullVariableName != null && VariableValue != null){
            		mVariables.put(fullVariableName, VariableValue);
            		fullVariableName = null;
            		VariableValue =null;
            	}
            	if(item != null){
            		if(item.value != null && item.value.indexOf('@') >= 0){// use the variable
            			String temp = item.value.substring(1, item.value.length());
            			String realValue = mVariables.get(temp);
            			if(realValue != null){
                			item.value = realValue;
                		}
            		}

					String valueSub;
					if(item.value == null){
						Log.e(TAG, "ERROR! value of " + item.fullValueName + " is null!");
						item = null;
						continue;
					}
        			if(item.value.codePointAt(0) == '#'){
        				 valueSub = item.value.substring(1);
        			}else{
        				valueSub = item.value;
        			}
            		try{
        				long value = Long.valueOf(valueSub, 16);
						value = value | LewaTheme.LEWA_COLOR_MARK; // mark the color value is valid.
						int id;
						id = mRes.getIdentifier(item.fullValueName, null, mPackageName);
						if(id != 0){
							mResMap.put(id, value);
                            if(DEBUG)Log.i(TAG, "get resid from name:"+item.fullValueName+", id=" + Integer.toHexString(id));
						}else{
							Log.e(TAG, "Can't get resid for " + item.fullValueName + " at " + mPackageName);
						}
        			}catch(NumberFormatException e){
        				e.printStackTrace();
						item = null;
        				continue;
        			}
					
					
            		//Log.i("XML", item.toString());
                	item = null;
            	}
            	
            	
                break;  
            }  
            try {
				event = parser.next();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}//进入下一个元素并触发相应事件  
        }//end while  
	}
	
	public class ValueItem{
		String name;
		String type;
		String valueName;
		String value;
		String fullValueName;
		
		public ValueItem(){
		
		}
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("item name[");
			sb.append(name);
			sb.append("] type[");
			sb.append(type);
			sb.append("] valueName[");
			sb.append(valueName);
			sb.append("] fullValueName[");
			sb.append(fullValueName);
			sb.append("] value[");
			sb.append(value);
			return sb.toString();
		}
	}
    
}
