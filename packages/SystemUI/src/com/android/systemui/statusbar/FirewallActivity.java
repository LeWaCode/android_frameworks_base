package com.android.systemui.statusbar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.R.bool;
import android.R.integer;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.net.TrafficStats;
import android.widget.LewaCheckBox;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.android.systemui.R;

public class FirewallActivity extends Activity {
    
    private View mFireWall;
    private android.view.View.OnClickListener mFirewallListener;
    private ImageView mMobileAccess;
    private android.view.View.OnClickListener mMobileAccessListener;
    private ImageView mWifiAccess;
    private android.view.View.OnClickListener mWifiAccessListener;
    private LewaCheckBox mFireWallCheckBox;
    private final int mMobileImages[];
    private final int mWifiImages[];
    private ListView mListView;
    private View mLoading;
    private UidInfoAdapter mUidInfoAdapter;
    static boolean mSupportUidNetwork;
    private Comparator mCurrentComparator;
    private SparseArray mUidInfos;
    private PackageManager mPM;
    private boolean mNetworkMonitorEnable;
    private ProgressDialog mLoadingAppsListDialog;
    private Comparator mTrafficComparator;
    private Comparator mNameComparator;
    private Context mContext;
    private String upload;
    private String download;
    private String nodata;
    private java.text.NumberFormat  formater;
    private static final long GB_UNIT = 1024*1024*1024;
    private static final long MB_UNIT = 1024*1024;
    private static final long KB_UNIT = 1024;
    private static final String TAG = "FirewallActivity";
    static final boolean LOGC = false; 
    
    //new added by zhuyaopeng 2012/05/21
    private static String SP_FLOW_NAME="flow_name";
    private static String SP_mTxBytes ="Tx_";
    private static String SP_mRxBytes="Rx_";
    private static String SP_TxBytes ="T_";
    private static String SP_RxBytes="R_";
    private static String SP_mMobileTxBytes="mMobileTxBytes";
    private static String SP_mMobileRxBytes="mMobileRxBytes";
    private static String KEY_LAST_TIME="last";
    private static SharedPreferences sp;
    private SharedPreferences.Editor editor;
       
    public FirewallActivity(){
        int a1[] = {R.drawable.mobile_off, R.drawable.mobile, R.drawable.mobile_on};
        mMobileImages = a1;
        int a2[] = {R.drawable.wifi_off, R.drawable.wifi, R.drawable.wifi_on};
        mWifiImages = a2;
        mFirewallListener = new FirewallListener();
        mMobileAccessListener = new MobileAccessListener();
        mWifiAccessListener = new WifiAccessListener();
        mUidInfos = new SparseArray();

        mNameComparator = new NameComparator();
        mTrafficComparator = new TrafficComparator();
        mCurrentComparator = mTrafficComparator;
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.firewall);
        mPM = getPackageManager();
        mFireWall = findViewById(R.id.firewall);
        mFireWall.setOnClickListener(mFirewallListener);
        mFireWallCheckBox = (LewaCheckBox)findViewById(R.id.firewall_checkbox);
        //mFireWallCheckBox.setOnClickListener(mFirewallListener);
        mMobileAccess = (ImageView)findViewById(R.id.mobile_access);
        mMobileAccess.setOnClickListener(mMobileAccessListener);
        mWifiAccess = (ImageView)findViewById(R.id.wifi_access);
        mWifiAccess.setOnClickListener(mWifiAccessListener);
        mListView = (ListView)findViewById(R.id.list);
        mUidInfoAdapter = new UidInfoAdapter(this);
        mListView.setAdapter(mUidInfoAdapter);
        mLoading = findViewById(R.id.loading);
        mLoading.setVisibility(View.GONE);
        
        mLoadingAppsListDialog = new ProgressDialog(this);
        mLoadingAppsListDialog.setMessage(getString(R.string.loading_apps_list));
        mLoadingAppsListDialog.setCancelable(false);
        mContext = this;
        //begin,new added by zhuyaopeng 2012/05/21
        sp=mContext.getSharedPreferences(SP_FLOW_NAME, Context.MODE_PRIVATE);
        editor=sp.edit();

        upload = mContext.getString(R.string.upload);
        download = mContext.getString(R.string.download);
        nodata = mContext.getString(R.string.nodata);

        formater  =  java.text.DecimalFormat.getInstance();  
        formater.setMaximumFractionDigits(2);  
        formater.setMinimumFractionDigits(2);

        Firewall.assertBinaries(this, true); 

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_REBOOT);
        filter.addAction(Intent.ACTION_SHUTDOWN);    
        registerReceiver(mBroadcastReceiver, filter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            log("onReceive");
            Firewall.saveRules(mContext, mUidInfos);
        }
    };
    
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    	Log.d(TAG,"onpause");
        //refreshTraffic();
        //applyOrSaveRules();
    }    
    
    @Override
	protected void onStop() {
		super.onStop();
	}
    
	public void Pause() {
        log("Pause");
        applyOrSaveRules();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        log("onDestroy");
        //applyOrSaveRules();
        unregisterReceiver(mBroadcastReceiver);
    }
    
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        boolean isEnabled = Firewall.isEnabled(this);
        Firewall.setEnabled(mContext, isEnabled);
        mFireWallCheckBox.setChecked(isEnabled);
        //mFireWallCheckBox.setEnabled(isEnabled);
        mMobileAccess.setEnabled(isEnabled);
        mWifiAccess.setEnabled(isEnabled);
        mUidInfoAdapter.notifyDataSetChanged();  
        if (isEnabled) {
            int i = Firewall.getMobileAccessMode(mContext);
            mMobileAccess.setImageResource(mMobileImages[i]);
            int j = Firewall.getWifiAccessMode(mContext);
            mWifiAccess.setImageResource(mWifiImages[j]);                                       
        } else {
            mMobileAccess.setImageResource(mMobileImages[1]);
            mWifiAccess.setImageResource(mWifiImages[1]);            
        }
        log("onResume isEnabled = "+isEnabled);     
        refreshTraffic();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    private String format(long data) {
        String UNIT = "K";
        if (data <= 0)
        {
            return nodata;
        }
        float fdata = Float.valueOf(data+"")/KB_UNIT;
        if (fdata >= KB_UNIT) {
            fdata = Float.valueOf(data+"")/MB_UNIT;
            UNIT = "M";
        }
        if (fdata >= KB_UNIT) {
            fdata = Float.valueOf(data+"")/GB_UNIT;
            UNIT = "G";
        }
        if (fdata < 0.01) {
            return nodata;
        } 
        String fdata_str = formater.format(fdata);
        return (fdata_str + UNIT);
    }
        
    private void applyOrSaveRules() {
        (new applyOrSaveRulesThread()).start();
    }
    
    private class applyOrSaveRulesThread extends Thread {

        public void run()
        {
            final boolean enabled = Firewall.isEnabled(mContext);
            if(enabled) {
                Firewall.applyRules(mContext, mUidInfos, true);
                return;
            } else {
                Firewall.saveRules(mContext, mUidInfos);
                Firewall.purgeIptables(mContext, true);
                return;
            }
        }
    }
    
    public static List getRunningApplications(Context context, List list) {
        ArrayList arraylist = new ArrayList();
        ActivityManager activityManager = (ActivityManager)(context.getSystemService(ACTIVITY_SERVICE));        
        List listRunning = activityManager.getRunningAppProcesses();
        if(listRunning != null) { 
            HashMap hashmap = new HashMap();
            Iterator iterator = listRunning.iterator();
            do {
                if(!iterator.hasNext())
                    break;
                android.app.ActivityManager.RunningAppProcessInfo runningappprocessinfo 
                        = (android.app.ActivityManager.RunningAppProcessInfo)iterator.next();
                if(runningappprocessinfo != null && runningappprocessinfo.pkgList != null) {
                    int i = runningappprocessinfo.pkgList.length;
                    int j = 0;
                    while(j < i) {
                        String s = runningappprocessinfo.pkgList[j];
                        hashmap.put(s, runningappprocessinfo);
                        j++;
                    }
                }
            } while(true);
            Iterator iterator1 = list.iterator();
            while(iterator1.hasNext()) {
                ApplicationInfo applicationinfo = (ApplicationInfo)iterator1.next();
                if(hashmap.get(applicationinfo.packageName) != null)
                    arraylist.add(applicationinfo);
            }
        }
        return arraylist;
    }
    
    public static void resolveLabelIcon(Context context, UidInfo uidinfo, PackageManager packagemanager) {
        if(uidinfo.mUid == 1000) {
            uidinfo.mLabel = context.getString(R.string.android_system);
            uidinfo.mIcon = packagemanager.getDefaultActivityIcon();
            return;
        }
        Iterator iterator = uidinfo.mApplications.iterator();
        if(iterator.hasNext()) {
            ApplicationInfo applicationinfo = (ApplicationInfo)iterator.next();
            uidinfo.mLabel = applicationinfo.loadLabel(packagemanager).toString();
            uidinfo.mIcon = applicationinfo.loadIcon(packagemanager);
        }
        
        if(uidinfo.mLabel == null) {
            uidinfo.mLabel = String.valueOf(uidinfo.mUid);
        }
        
        if(uidinfo.mIcon != null) {
            return;
        } else {
            if (uidinfo.mIsSystemApp) {
                uidinfo.mIcon = packagemanager.getDefaultActivityIcon();
            } else {
                uidinfo.mIcon = context.getResources().getDrawable(R.drawable.def_app_icon);
            }
            //uidinfo.mIcon = packagemanager.getDefaultActivityIcon();
            return;
        }
    }
    
    public  void buildUidInfoList(Context context, SparseArray sparsearray, PackageManager packagemanager) {
        List listInstalled = packagemanager.getInstalledApplications(0);
        List listRunning = getRunningApplications(context, listInstalled);
        Iterator iterator = listInstalled.iterator();
        do {
            if(!iterator.hasNext())
                break;
            ApplicationInfo applicationinfo = (ApplicationInfo)iterator.next();
            int i = applicationinfo.uid;
            UidInfo uidinfo = (UidInfo)sparsearray.get(i);
            if(uidinfo == null) {
                if(packagemanager.checkPermission("android.permission.INTERNET", applicationinfo.packageName) 
                        != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
            }
            if(uidinfo == null) {
                uidinfo = new UidInfo(applicationinfo.uid);
                uidinfo.packageName = applicationinfo.packageName;
                uidinfo.setFlowValue();
                sparsearray.put(applicationinfo.uid, uidinfo);
            }
            uidinfo.addApplication(applicationinfo);
            resolveLabelIcon(context, uidinfo, packagemanager);
        } while(true);
        Iterator iteratorRunning = listRunning.iterator();
        do {
            if(!iteratorRunning.hasNext())
                break;
            int i = ((ApplicationInfo)iteratorRunning.next()).uid;
            UidInfo uidinfo = (UidInfo)sparsearray.get(i);
            if(uidinfo != null)
                uidinfo.mIsRunning = true;
        } while(true);
        Firewall.loadRules(context, sparsearray);
    }
    
    private void refreshTraffic() {
        new RefreshTrafficTask().execute();
    }
    
    public void putValue(String key,long value){
    	try {
    		editor=sp.edit();
    		editor.putLong(key, value);
    		editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	public long getLongValue(String key) {
		return sp.getLong(key, 0);
	}
    
    private class RefreshTrafficTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected void onPreExecute() {
            mUidInfos.clear();
            mLoading.setVisibility(View.VISIBLE);
        }
        
        @Override
        protected Void doInBackground(Object... params) {
            SparseArray sparsearray = mUidInfos;
            PackageManager packagemanager = mPM;
            buildUidInfoList(FirewallActivity.this, sparsearray, packagemanager);
            boolean flag = false;
            if(android.provider.Settings.Secure.getInt(getContentResolver(), "enable_monitor_traffic", 1) == 1) {
                flag = true;
            }
            mNetworkMonitorEnable = flag;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(mNetworkMonitorEnable) {
                int i = 0;
                do {
                    int j = mUidInfos.size();
                    if(i >= j) {
                        break;
                    }
                    int k = mUidInfos.keyAt(i);
                    UidInfo uidinfo = (UidInfo)mUidInfos.get(k);
                    if(uidinfo != null) {
                        String pkg = uidinfo.getPackageName();
                        long rx = getLongValue(SP_mRxBytes+pkg);
                        long tx = getLongValue(SP_mTxBytes+pkg);
                        rx += getLongValue(SP_RxBytes+pkg);
                        tx += getLongValue(SP_TxBytes+pkg);
                        uidinfo.mTxBytes[0] = tx;
                        uidinfo.mRxBytes[0] = rx;
                    }
                    i++;
                } while(true); 
            } else {
                int i = 0;
                do {
                    int j = mUidInfos.size();
                    if(i >= j) {
                        break;
                    }
                    int k = mUidInfos.keyAt(i);
                    UidInfo uidinfo = (UidInfo)mUidInfos.get(k);
                    long rx = TrafficStats.getUidRxBytes(k);
                    long tx = TrafficStats.getUidTxBytes(k);
                    if(uidinfo != null) {
                        uidinfo.mTxBytes[0] = tx;
                        uidinfo.mRxBytes[0] = rx;
                    }
                    i++;
                } while(true); 
            }
            mLoading.setVisibility(View.GONE);
            UidInfoAdapter uidinfoadapter = mUidInfoAdapter;
            SparseArray sparsearray = mUidInfos;
            uidinfoadapter.refresh(sparsearray);
        }
    }
    
    
    private class FirewallListener implements android.view.View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            boolean isEnabled = Firewall.isEnabled(mContext);
            log("FirewallListener isEnabled = "+isEnabled);
            if (isEnabled) {
                mMobileAccess.setImageResource(mMobileImages[Firewall.ACCESS_NORMAL]);
                mWifiAccess.setImageResource(mWifiImages[Firewall.ACCESS_NORMAL]);
            } else {
                int i = Firewall.getMobileAccessMode(mContext);
                mMobileAccess.setImageResource(mMobileImages[i]);
                int j = Firewall.getWifiAccessMode(mContext);
                mWifiAccess.setImageResource(mWifiImages[j]);
            }
            Firewall.setEnabled(mContext, !isEnabled);
            mFireWallCheckBox.setChecked(!isEnabled);
            //mFireWallCheckBox.setEnabled(!isEnabled);
            mMobileAccess.setEnabled(!isEnabled);
            mWifiAccess.setEnabled(!isEnabled);
            mUidInfoAdapter.notifyDataSetChanged();
        }        
    }
    
    private class MobileAccessListener implements android.view.View.OnClickListener {    
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int i = Firewall.getMobileAccessMode(mContext);
            int j = (i == Firewall.ACCESS_NORMAL || i == Firewall.ACCESS_ALL_FORBIDDEN) 
                    ? Firewall.ACCESS_ALL_ALLOWED : Firewall.ACCESS_ALL_FORBIDDEN;
            mMobileAccess.setImageResource(mMobileImages[j]);
            mUidInfoAdapter.enableMobile(j);
            Firewall.setMobileAccessMode(mContext, j);
        }        
    }
    
    private class WifiAccessListener implements android.view.View.OnClickListener {    
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int i = Firewall.getWifiAccessMode(mContext);
            int j = (i == Firewall.ACCESS_NORMAL || i == Firewall.ACCESS_ALL_FORBIDDEN) 
                    ? Firewall.ACCESS_ALL_ALLOWED : Firewall.ACCESS_ALL_FORBIDDEN;
            mWifiAccess.setImageResource(mWifiImages[j]);
            mUidInfoAdapter.enableWifi(j);
            Firewall.setWifiAccessMode(mContext, j);
        }        
    }
    
    class UidInfoAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mActiveUids.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return getUidInfoItem(position);
        }

        public UidInfo getUidInfoItem(int i) {
            UidInfo uidinfo = null;
            if (i < 0) {
                    return uidinfo;
            }
            int j = getCount();
            if (i < j) {
                    uidinfo = (UidInfo)mActiveUids.get(i);
            } else {
                    uidinfo = null;
            }
            return uidinfo;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            if(convertView == null) {
                    convertView = createView(position, parent);
            }
            bindView(position, convertView);
            return convertView;
        }

        private List mActiveUids;
        private List mAllUids;
        private Context mContext;
        private boolean mDisplaySystemApp;
        private List mNonSystemUids;
        private android.widget.CompoundButton.OnCheckedChangeListener mMobileListener;
        private android.widget.CompoundButton.OnCheckedChangeListener mWifiListener;
            
        class MobileListener
                implements android.widget.CompoundButton.OnCheckedChangeListener {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                // TODO Auto-generated method stub
                ((UidInfo)buttonView.getTag()).mEnableMobile = isChecked;
                int i = Firewall.getMobileAccessMode(mContext);
                log("MobileListener isChecked = "+isChecked+" i = "+i);
                if(!isChecked && i == Firewall.ACCESS_ALL_ALLOWED || isChecked && i == Firewall.ACCESS_ALL_FORBIDDEN) {
                    Firewall.setMobileAccessMode(mContext, Firewall.ACCESS_NORMAL);
                    mMobileAccess.setImageResource(mMobileImages[Firewall.ACCESS_NORMAL]);
                    return;
                }
                if(isChecked && mUidInfoAdapter.getMobileCheckedMode() == Firewall.ACCESS_ALL_ALLOWED) {
                    log("MobileListener getMobileCheckedMode() == 2");
                    Firewall.setMobileAccessMode(mContext, Firewall.ACCESS_ALL_ALLOWED);
                    mMobileAccess.setImageResource(mMobileImages[Firewall.ACCESS_ALL_ALLOWED]);
                    return;
                }
                if (!isChecked && mUidInfoAdapter.getMobileCheckedMode() == Firewall.ACCESS_ALL_FORBIDDEN) {
                    log("MobileListener getMobileCheckedMode() == 0");
                    Firewall.setMobileAccessMode(mContext, Firewall.ACCESS_ALL_FORBIDDEN);
                    mMobileAccess.setImageResource(mMobileImages[Firewall.ACCESS_ALL_FORBIDDEN]);
                    return;                
                }
            }

        }

        class WifiListener
                implements android.widget.CompoundButton.OnCheckedChangeListener {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                // TODO Auto-generated method stub
                ((UidInfo)buttonView.getTag()).mEnableWifi = isChecked;
                int i = Firewall.getWifiAccessMode(mContext);
                log("WifiListener isChecked = "+isChecked+" i = "+i);
                if(!isChecked && i == Firewall.ACCESS_ALL_ALLOWED|| isChecked && i == Firewall.ACCESS_ALL_FORBIDDEN) {
                    Firewall.setWifiAccessMode(mContext, Firewall.ACCESS_NORMAL);
                    mWifiAccess.setImageResource(mWifiImages[Firewall.ACCESS_NORMAL]);
                    return;
                }
                if(isChecked && mUidInfoAdapter.getWifiCheckedMode() == Firewall.ACCESS_ALL_ALLOWED) {
                    log("WifiListener getWifiCheckedMode() == 2");
                    Firewall.setWifiAccessMode(mContext, Firewall.ACCESS_ALL_ALLOWED);
                    mWifiAccess.setImageResource(mWifiImages[Firewall.ACCESS_ALL_ALLOWED]);
                    return;
                }
                if (!isChecked && mUidInfoAdapter.getWifiCheckedMode() == Firewall.ACCESS_ALL_FORBIDDEN) {
                    log("WifiListener getWifiCheckedMode() == 0");
                    Firewall.setWifiAccessMode(mContext, Firewall.ACCESS_ALL_FORBIDDEN);
                    mWifiAccess.setImageResource(mWifiImages[Firewall.ACCESS_ALL_FORBIDDEN]);
                    return;                    
                }
            }

        }

        public UidInfoAdapter(Context context) {
            super();
            mMobileListener = new MobileListener();
            mWifiListener = new WifiListener();
            mContext = context;
            boolean flag = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("display_system_app", true);
            mDisplaySystemApp = flag;
            mAllUids = new ArrayList();
            mNonSystemUids = new ArrayList();
            mActiveUids = new ArrayList();
        }

        private View createView(int i, ViewGroup viewgroup) {
            View view = ((LayoutInflater)viewgroup.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.app_info, viewgroup, false);
            ListEntry listentry = new ListEntry(null);
            ImageView imageview = (ImageView)view.findViewById(R.id.icon);
            TextView textview = (TextView)view.findViewById(R.id.text);
            TextView textview1 = (TextView)view.findViewById(R.id.text1);
            TextView textview2 = (TextView)view.findViewById(R.id.text2);
            CheckBox checkbox1 = (CheckBox)view.findViewById(R.id.enableMobile);
            CheckBox checkbox2 = (CheckBox)view.findViewById(R.id.enableWifi);
            listentry.icon = imageview;
            listentry.text = textview;
            listentry.text1 = textview1;
            listentry.text2 = textview2;
            listentry.mobile = checkbox1;
            listentry.wifi = checkbox2;
            listentry.mobile.setOnCheckedChangeListener(mMobileListener);
            listentry.wifi.setOnCheckedChangeListener(mWifiListener);
            view.setTag(listentry);
            return view;            
        }

        private void bindView(int i, View view) {
            UidInfo uidinfo = getUidInfoItem(i);
            if(uidinfo == null){
                return;
            }
            ListEntry listentry = (ListEntry)view.getTag();
            if(mSupportUidNetwork) {
				long tx = uidinfo.mTxBytes[0];
                long rx = uidinfo.mRxBytes[0];                
                listentry.text1.setText(upload + format(tx));  
                listentry.text2.setText(download + format(rx));   
            } else {
                long tx = uidinfo.mTxBytes[0];
                long rx = uidinfo.mRxBytes[0];                
                listentry.text1.setText(upload + format(tx));  
                listentry.text2.setText(download + format(rx));  
            }
                           
            listentry.text.setText(uidinfo.getLabel());            
            listentry.icon.setImageDrawable(uidinfo.getIcon());            
            listentry.mobile.setTag(uidinfo);
            listentry.wifi.setTag(uidinfo);
            listentry.mobile.setChecked(uidinfo.mEnableMobile);
            listentry.wifi.setChecked(uidinfo.mEnableWifi);
            log("bindView uidinfo.mEnableMobile = "+uidinfo.mEnableMobile+" uidinfo.mEnableWifi = "+uidinfo.mEnableWifi);
            boolean flag = Firewall.isEnabled(mContext);
            listentry.wifi.setEnabled(flag);
            listentry.mobile.setEnabled(flag);          
        }

        private int getCheckedMode(int i) {
            int j;
            if(i == 0) {
                j = Firewall.ACCESS_ALL_FORBIDDEN;
            } else {
                int k = getCount();
                if(i == k)
                    j = Firewall.ACCESS_ALL_ALLOWED;
                else
                    j = Firewall.ACCESS_NORMAL;
            }
            return j;
        }

        private void setData() {
            if(mDisplaySystemApp) {
                mActiveUids = mAllUids;
            } else {
                mActiveUids = mNonSystemUids;
            }
            sort(mCurrentComparator);
        }

        public void sort(Comparator comparator) {
            Collections.sort(mActiveUids, comparator);
            mCurrentComparator = comparator;
            notifyDataSetChanged();
        }

        public void enableMobile(int i) {
            for(Iterator iterator = mActiveUids.iterator(); iterator.hasNext();) {
                UidInfo uidinfo = (UidInfo)iterator.next();
                if(i == Firewall.ACCESS_ALL_ALLOWED)
                    uidinfo.mEnableMobile = true;
                else
                    uidinfo.mEnableMobile = false;
            }
            notifyDataSetChanged();
        }

        public void enableWifi(int i) {
            for(Iterator iterator = mActiveUids.iterator(); iterator.hasNext();) {
                UidInfo uidinfo = (UidInfo)iterator.next();
                if(i == Firewall.ACCESS_ALL_ALLOWED)
                    uidinfo.mEnableWifi = true;
                else
                    uidinfo.mEnableWifi = false;
            }
            notifyDataSetChanged();
        }

        public int getMobileCheckedMode() {
            int i = 0;
            int j = 0;
            do {
                int k = getCount();
                if(j < k) {
                    if(((UidInfo)mActiveUids.get(j)).mEnableMobile) {
                        i++;
                    }
                    j++;
                } else {
                    return getCheckedMode(i);
                }
            } while(true);
        }

        public int getWifiCheckedMode() {
            int i = 0;
            int j = 0;
            do {
                int k = getCount();
                if(j < k) {
                    if(((UidInfo)mActiveUids.get(j)).mEnableWifi) {
                        i++;
                    }
                    j++;
                } else {
                    return getCheckedMode(i);
                }
            } while(true);
        }

        public void refresh(SparseArray sparsearray) {
            mAllUids.clear();
            mNonSystemUids.clear();
            int i = sparsearray.size();
            for(int j = 0; j < i; j++) {
                UidInfo uidinfo = (UidInfo)sparsearray.valueAt(j);
                mAllUids.add(uidinfo);
                if(!uidinfo.mIsSystemApp) {
                    mNonSystemUids.add(uidinfo);
                }
            }
            setData();
        }

        public void setDisplaySystemApp(boolean flag) {
            if(flag == mDisplaySystemApp) {
                return;
            } else {
                mDisplaySystemApp = flag;
                setData();
                return;
            }
        }
    }
    
    static class UidInfo {
    	
        List mApplications;
        boolean mEnableMobile;
        boolean mEnableWifi;
        Drawable mIcon;
        boolean mIsRunning;
        boolean mIsSystemApp;
        String mLabel;
        String packageName;
        long mMobileRxBytes[];
        long mMobileTxBytes[];
        SparseArray mPreferUidLabels;
        long mRxBytes[];
        long mTxBytes[];
        int mUid;
		
        public UidInfo(int i) {
            mApplications = new ArrayList();
            mPreferUidLabels = new SparseArray();
            mUid = i;
            mTxBytes = new long[1];
            mRxBytes = new long[1];
            mMobileTxBytes = new long[1];
            mMobileRxBytes = new long[1];            
/*          mTxBytes[0] = 0;
            mRxBytes[0] = 0;
            mMobileTxBytes[0] = 0;
            mMobileRxBytes[0] = 0;*/
            mEnableMobile = true;
            mEnableWifi = true;
        }
        
        //begin,added by zhuyaopeng 2012/05/21 
        public void setFlowValue(){
            mTxBytes[0]=sp.getLong(SP_mTxBytes+packageName,0);
            mRxBytes[0]=sp.getLong(SP_mRxBytes+packageName,0);
            mMobileTxBytes[0]=sp.getLong(SP_mMobileTxBytes+packageName,0);
            mMobileRxBytes[0]=sp.getLong(SP_mMobileRxBytes+packageName, 0);
//            Log.e(TAG,"mTxBytes[0]="+mTxBytes[0]+",name="+SP_mTxBytes+packageName);
//            Log.e(TAG,"mRxBytes[0]="+mRxBytes[0]+",name="+SP_mRxBytes+packageName);
//            Log.e(TAG,"mMobileTxBytes[0]="+mMobileTxBytes[0]);
//            Log.e(TAG,"mMobileRxBytes[0]="+mMobileRxBytes[0]);
        }        

        public void addApplication(ApplicationInfo applicationinfo) {
            mApplications.add(applicationinfo);
            if((applicationinfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                return;
            } else {
                mIsSystemApp = true;
                return;
            }
        }

        public Drawable getIcon() {
            return mIcon;
        }

        public String getLabel() {
            return mLabel;
        }
        
        public String getPackageName(){
        	return packageName;
        }
    }
    
    private static class ListEntry {

        private ImageView icon;
        private TextView text;
        private TextView text1;
        private TextView text2;
        private CheckBox mobile;
        private CheckBox wifi;

        private ListEntry() {
            
        }
        
        ListEntry(ListEntry listEntry) {
            this();
        }
        
    }
    
    class NameComparator implements Comparator {
        private final Collator sCollator;

        NameComparator() {
            super();     
            sCollator = Collator.getInstance();
        }

        public final int compare(UidInfo uidinfo, UidInfo uidinfo1) {
            String s = uidinfo.getLabel();
            String s1 = uidinfo1.getLabel();
            return sCollator.compare(s, s1);
        }

        public int compare(Object obj, Object obj1) {
            UidInfo uidinfo = (UidInfo)obj;
            UidInfo uidinfo1 = (UidInfo)obj1;
            return compare(uidinfo, uidinfo1);
        }
    }
    
    class TrafficComparator implements Comparator {

        public final int compare(UidInfo uidinfo, UidInfo uidinfo1) {
            
            return (int)((uidinfo1.mTxBytes[0] + uidinfo1.mRxBytes[0]) - (uidinfo.mTxBytes[0] + uidinfo.mRxBytes[0]));
        }

        public  int compare(Object obj, Object obj1) {
            UidInfo uidinfo = (UidInfo)obj;
            UidInfo uidinfo1 = (UidInfo)obj1;
            return compare(uidinfo, uidinfo1);
        }    
    }

    private void log(String msg) {
        if (LOGC) {
            Log.e(TAG, "zhangbo " + msg);
        }        
    }
    public static class UidData{
        public UidData(String pkg, long tx, long rx){
            this.pkg = pkg;
            this.tx = tx;
            this.rx = rx;
        }
        public long tx;
        public long rx;
        public String pkg;
    }
    
    public static void saveUidData(Context context, List<ApplicationInfo> listRunning){
        SharedPreferences pref = context.getSharedPreferences(FirewallActivity.SP_FLOW_NAME, Context.MODE_PRIVATE);
        Editor edit = pref.edit();
        long boot = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        boolean rebooted = pref.getLong(KEY_LAST_TIME, 0) + 1000 <= boot;

        if(rebooted){
            ArrayList<UidData> uids = new ArrayList<UidData>();
            for(ApplicationInfo app : listRunning){
                String pkg = app.packageName;
                long tx = pref.getLong(SP_mTxBytes+pkg, 0);
                long rx = pref.getLong(SP_mRxBytes+pkg, 0);
                uids.add(new UidData(pkg, tx, rx));
            }
            uids.trimToSize();
            edit.clear();
            edit.commit();
            for(UidData uid : uids){
                long tx = uid.tx;
                long rx = uid.rx;
                String pkg = uid.pkg;
                if(tx > 0)
                    edit.putLong(SP_TxBytes+pkg, tx);
                if(rx > 0)
                    edit.putLong(SP_RxBytes+pkg, rx);
            }
            edit.putLong(KEY_LAST_TIME, boot);
        } else {
            for(ApplicationInfo app : listRunning){
                String pkg = app.packageName;
                int uid = app.uid;
                long tx = TrafficStats.getUidTxBytes(uid);
                long rx = TrafficStats.getUidRxBytes(uid);
                if(tx > 0)
                    edit.putLong(SP_mTxBytes+pkg, tx);
                if(rx > 0)
                    edit.putLong(SP_mRxBytes+pkg, rx);
            }
        }
        edit.commit();
    }
    public static void clearUidDataUsage(Context context) {
        context.getSharedPreferences(FirewallActivity.SP_FLOW_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }
}
