package com.android.internal.policy.impl.lewa.view;



import android.text.TextUtils;
import android.util.Log;

public class Variable {

    private String mObjectName;
    private String mPropertyName;
    
    public Variable(String propertyName) {
    
        int pos = propertyName.indexOf('.');
        if(pos == -1){
            mObjectName = null;
            mPropertyName = propertyName;
        } else{
            mObjectName = propertyName.substring(0, pos);
            mPropertyName = propertyName.substring(pos+1);
        }
        if(!TextUtils.isEmpty(mPropertyName)){
            return;
        } else{
            Log.e("Variable", new StringBuilder().append("invalid variable name:").append(propertyName).toString());
        }
    }

    public String getObjName(){
        return mObjectName;
    }

    public String getPropertyName(){
        return mPropertyName;
    }

}
