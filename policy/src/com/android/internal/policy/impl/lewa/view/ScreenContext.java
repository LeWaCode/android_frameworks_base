package com.android.internal.policy.impl.lewa.view;

import android.content.Context;
import android.view.View;

public class ScreenContext {

    public final Context mContext;
    public final ScreenElementFactory mFactory;
    public ScreenElement mRoot;
    public boolean mShouldUpdate;
    public View mView;
    
    public ScreenContext(Context context){
        this(context, new ScreenElementFactory());
    }

    public ScreenContext(Context context, ScreenElementFactory screenelementfactory){
        mContext = context;
        mFactory = screenelementfactory;
    }

}
