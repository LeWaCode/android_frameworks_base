package com.android.systemui.statusbar;

import java.util.ArrayList;
import java.util.List;

import com.android.systemui.R;

import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SelectDateActivity extends ListActivity{
	
	private Button mCancelButton = null;
	private SharedPreferences mSharedPreferences;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_start_date);
		mSharedPreferences = this.getSharedPreferences(DataMonitorService.PREFERENCE_NAME, Activity.MODE_PRIVATE);
		mCancelButton = (Button)findViewById(R.id.cancelListView);
		mCancelButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				SelectDateActivity.this.finish();
			}
		});
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,getData()));
		setSelection(mSharedPreferences.getInt(DataMonitorService.START_DATE, 1) - 1);
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(DataMonitorService.START_DATE, (position + 1));
		editor.commit();
		SelectDateActivity.this.finish();
	}

	private List<String> getData() {
		
		List<String> data = new ArrayList<String>();
		for (int i = 1; i < 32; i++) {
			data.add(i + "");			
		}
        return data;		
	}
}
