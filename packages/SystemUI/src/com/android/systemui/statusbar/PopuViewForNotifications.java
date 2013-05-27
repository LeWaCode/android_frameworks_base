package com.android.systemui.statusbar;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.os.ServiceManager;
import android.os.RemoteException;

import com.android.internal.statusbar.IStatusBarService;

import com.android.systemui.R;

public class PopuViewForNotifications implements OnClickListener{

	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mLayoutparams;
	private PopuViewFrame mTipViews;
	
	private Context mContext;
	private String mPackageName;
	private String mTag;
	private int mId;
	private StatusBarService mService;
	private IStatusBarService mBarService;
	private String mAppName;	

	public boolean mNeedClose = false;

	public PopuViewForNotifications(Context context,StatusBarService service, String pkg,String tag, int id) {

		mContext = context;
		mPackageName = pkg;
		mService = service;
		mTag = tag;
		mId = id;
		
		mWindowManager = (WindowManager) context
				.getSystemService(context.WINDOW_SERVICE);
		mLayoutparams = new WindowManager.LayoutParams();

		mBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));

		mLayoutparams.width = LayoutParams.FILL_PARENT;
		mLayoutparams.height = LayoutParams.WRAP_CONTENT;

		mLayoutparams.format = PixelFormat.TRANSPARENT;
		mLayoutparams.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
		
		mLayoutparams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
							 | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
		
		mLayoutparams.dimAmount = 0.5f;
		
		mTipViews = (PopuViewFrame)LayoutInflater.from(context).inflate(
				R.layout.popuviewframe, null);
		mTipViews.setPopuViewForNotifications(this);
		TextView appName = (TextView) mTipViews.findViewById(R.id.app_name);

		PackageManager pm = context.getPackageManager();
		
		try{
			ApplicationInfo appinfo = pm.getApplicationInfo(pkg, 0);
			mAppName = appinfo.loadLabel(pm).toString();
			if(mAppName == null) {
				mAppName = pkg;
			}
			appName.setText(mAppName);
		} catch (NameNotFoundException e) {
			// TODO: handle exception
		}

		View app_info = mTipViews.findViewById(R.id.app_info);
		View app_stop = mTipViews.findViewById(R.id.app_stop);

		app_info.setOnClickListener(this);
		app_stop.setOnClickListener(this);

		//mTipViews.setPadding(10, 0, 10, 0);

		mWindowManager.addView(mTipViews, mLayoutparams);
	}

	public void onfinish() {
		if (mWindowManager != null) {
			mWindowManager.removeView(mTipViews);
			mWindowManager = null;
			mTipViews = null;
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId() == R.id.app_info) {
			mNeedClose = true;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromParts(
					"package", mPackageName, null));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setClassName("com.android.settings",
					"com.android.settings.applications.InstalledAppDetails");
			mService.animateCollapse();
			mContext.startActivity(intent);
		} else if(v.getId() == R.id.app_stop) {
			NotificationFirewall.getInstance(mService).addBlockPackage(mPackageName);
			String str = mContext.getResources().getString(R.string.poputip_toast, mAppName);
			Toast.makeText(mContext, str, 500).show();
			mService.animateCollapse();
			try{
				mBarService.onNotificationClear(mPackageName, mTag, mId);
		  	} catch (RemoteException ex) {
                // system process is dead if we're here.
            		}
		}
		

	}

}
