package com.android.systemui.statusbar;

import android.content.Context;
import android.content.SharedPreferences;

public class SharePreferenceHelper {
	
	public String TAG=SharePreferenceHelper.class.getSimpleName();

	private SharedPreferences sp;
	private SharedPreferences.Editor editor;

	private Context context;

	public SharePreferenceHelper(Context c, String name) {
		context = c;
		sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		editor = sp.edit();
	}

	public void putValue(String key, boolean value) {
		editor = sp.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public void putValue(String key, int value) {
		editor = sp.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public boolean getBooleanValue(String key) {
		return sp.getBoolean(key, false);
	}

	public int getIntValue(String key) {
		return sp.getInt(key, 0);
	}
}