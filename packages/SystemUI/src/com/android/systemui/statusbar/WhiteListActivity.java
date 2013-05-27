package com.android.systemui.statusbar;

import com.android.systemui.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.lang.String;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class WhiteListActivity extends ListActivity {

    private ListView mListView;
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
    private Button mOK;
    private Button mCancel;
    private String mSystemApp;
    private String mUserApp;
    private View mLoading;
    private TextView mNoApp;
    public int mSizeUserApp;

    public static final String WHITELIST_PREFS = "WhiteListPrefs";
    public static final String WHITE_LIST = "Whitelist";
    private static SharedPreferences mSharedpreferences;

    private static final String TAG = "WhiteListActivity";
    static final boolean LOGC = false;

    public WhiteListActivity(){

        mUidInfos = new SparseArray();        
        mNameComparator = new NameComparator();
        mTrafficComparator = new TrafficComparator();
        mCurrentComparator = mNameComparator;

    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whitelist);
        mPM = getPackageManager();

        //mListView = (ListView)findViewById(R.id.list);
        mUidInfoAdapter = new UidInfoAdapter(this);
        //mListView.setAdapter(mUidInfoAdapter);
        setListAdapter(mUidInfoAdapter);
        /*
        mOK = (Button)findViewById(R.id.ok);
        mCancel = (Button)findViewById(R.id.cancel);
        mOK.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                applyOrSaveRules();
                WhiteListActivity.this.finish();
            }
        });

        mCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            // TODO Auto-generated method stub
                WhiteListActivity.this.finish();
            }
        });*/
        mUserApp = getResources().getString(R.string.group_title_userapp);
        mSystemApp = getResources().getString(R.string.group_title_systemapp);

        mLoadingAppsListDialog = new ProgressDialog(this);
        mLoadingAppsListDialog.setMessage(getString(R.string.loading_apps_list));
        mLoadingAppsListDialog.setCancelable(false);
        mContext = this;
        mSharedpreferences = mContext.getSharedPreferences(WHITELIST_PREFS, Context.MODE_PRIVATE);

        mLoading = findViewById(R.id.loading);
        mLoading.setVisibility(View.GONE);
        mNoApp = (TextView)findViewById(R.id.noApp);
        mNoApp.setVisibility(View.GONE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id);
        CheckBox checkBox = (CheckBox)v.findViewById(R.id.checkbox);
        boolean checked = checkBox.isChecked();
        checkBox.setChecked(!checked);
        ((UidInfo)checkBox.getTag()).mEnable = !checked;
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        log("onPause");
        applyOrSaveRules();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        log("onDestroy");
        //applyOrSaveRules();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        refreshTraffic();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    private void applyOrSaveRules() {
        (new SaveRulesThread()).start();
    }

    private class SaveRulesThread extends Thread {

        public void run(){
            saveRules(mContext, mUidInfos);
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
        //do not use the app uid to record the whitelist by zhangxianjia
        //if(uidinfo.mUid == 1000) {
        //    uidinfo.mLabel = context.getString(R.string.android_system);
        //    uidinfo.mIcon = packagemanager.getDefaultActivityIcon();
        //    return;
        // }
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
        int next = 0;
        do {
            if(!iterator.hasNext())
                break;
            ApplicationInfo applicationinfo = (ApplicationInfo)iterator.next();
            int i = next;
            /**
             * modified by maowenjiang 2012-08-17, 
             * fixed default protected application does not display
             */
            if(filtrateProtectedApp(applicationinfo)) continue;
            UidInfo uidinfo = (UidInfo)sparsearray.get(i);
            /*if(uidinfo == null) {
                if(packagemanager.checkPermission("android.permission.INTERNET", applicationinfo.packageName) 
                        != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
            }*/
            uidinfo = new UidInfo(applicationinfo.uid);
            uidinfo.packageName = applicationinfo.packageName;
            sparsearray.put(next, uidinfo);
            uidinfo.addApplication(applicationinfo);
            resolveLabelIcon(context, uidinfo, packagemanager);
            next++;
        } while(true);
        Iterator iteratorRunning = listRunning.iterator();
        do {
            if(!iteratorRunning.hasNext())
                break;
            /**
             * modified by maowenjiang 2012-08-17, 
             * fixed default protected application does not display
             */
            ApplicationInfo applicationInfo = (ApplicationInfo) iteratorRunning.next();
            if(filtrateProtectedApp(applicationInfo)) continue;
            int i = applicationInfo.uid;
            UidInfo uidinfo = (UidInfo)sparsearray.get(i);
            if(uidinfo != null)
                uidinfo.mIsRunning = true;
        } while(true);
        loadRules(context, sparsearray);
    }
    
    /**
     * added by maowenjiang 2012-08-17 
     * filterate the protected apps
     * @param applicationinfo
     * @return
     */
    public boolean filtrateProtectedApp(ApplicationInfo applicationinfo) {
            if(applicationinfo.packageName.equalsIgnoreCase("android")
                    || applicationinfo.packageName.equalsIgnoreCase("system")
                    || applicationinfo.packageName.equalsIgnoreCase("system_process")
                    || applicationinfo.packageName.equalsIgnoreCase("system_server")
                    || applicationinfo.packageName.equalsIgnoreCase("android.process.media")

                    || applicationinfo.packageName.equalsIgnoreCase("com.android.systemui")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.phone")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.wallpaper")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.settings")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.deskclock")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.server")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.email")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.bluetooth")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.inputmethod.latin")

                    // lewa application list
                    || applicationinfo.packageName.equalsIgnoreCase("com.lewa.spm")
                    || applicationinfo.packageName.equalsIgnoreCase("com.lewa.intercept")
                    || applicationinfo.packageName.equalsIgnoreCase("com.lewa.labi")
                    || applicationinfo.packageName.equalsIgnoreCase("com.lewa.launcher")
                    || applicationinfo.packageName.equalsIgnoreCase("com.lewatek.swapper")

                    // thirdparty inputmethod list
                    || applicationinfo.packageName.equalsIgnoreCase("com.iflytek.inputmethod")
                    || applicationinfo.packageName.equalsIgnoreCase("com.sohu.inputmethod.sogou")
                    || applicationinfo.packageName.equalsIgnoreCase("com.baidu.input")
                    || applicationinfo.packageName.equalsIgnoreCase("com.tencent.qqpinyin")
                    || applicationinfo.packageName.equalsIgnoreCase("com.cootek.smartinput")

                    // android provider list
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.providers.calendar")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.providers.media")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.providers.downloads")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.providers.drm")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.providers.contacts")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.providers.telephony")

                    // others
                    || applicationinfo.packageName.equalsIgnoreCase("com.noshufou.android.su")
                    || applicationinfo.packageName.equalsIgnoreCase("com.motorola.usb")
                    || applicationinfo.packageName.equalsIgnoreCase("org.adwfreak.launcher")
                    || applicationinfo.packageName.equalsIgnoreCase("com.android.internal.service.wallpaper.ImageWallpaper")) {
                return true;
        }
        return false;
    }

    public static String[] parseUidsString(String s) {
        String ai[] = new String[0];
        if(s.length() > 0) {
            StringTokenizer stringtokenizer = new StringTokenizer(s, "|");
            ai = new String[stringtokenizer.countTokens()];
            int i = 0;
            do {
                int j = ai.length;
                if(i >= j)
                    break;
                String s1 = stringtokenizer.nextToken();
                if(!s1.equals(""))
                    try {
                        //int k = Integer.parseInt(s1);
                        ai[i] = s1;
                    } catch(Exception exception) {
                        ai[i] = "-1";
                    }
                i++;
            } while(true);
        }
        return ai;
    }

    private static void loadRules(Context context, SparseArray sparsearray) {
        String whitelist[] = parseUidsString(mSharedpreferences.getString(WHITE_LIST, ""));
        int i = 0;
        do {
            int j = whitelist.length;
            if(i >= j)
                break;
            for(int k=0; k<sparsearray.size(); k++) {
                UidInfo uidinfo = (UidInfo)sparsearray.get(k);
                if(uidinfo.packageName.equalsIgnoreCase(whitelist[i])) {
                    uidinfo.mEnable = true;
                }
            }
            i++;
        } while(true);
    }

    private static void saveRules(Context context, SparseArray sparsearray) {
        if (context == null || sparsearray == null) {
            log("saveRules error!");
            return;
        }
        if(StatusBarService.mHashMap != null) StatusBarService.mHashMap.clear();
        StringBuilder whitelistUids = new StringBuilder();
        int i = 0;
        int m = 0;
        int j = sparsearray.size();
        do {
            while (i < j) {
                UidInfo uidinfo = (UidInfo)sparsearray.valueAt(i);
                int uid = uidinfo.mUid;
                if (uidinfo.mEnable) {
                    if(whitelistUids.length() != 0) {
                        whitelistUids.append('|');
                    }
                    whitelistUids.append(uidinfo.packageName);
                    if(StatusBarService.mHashMap == null) StatusBarService.mHashMap = new HashMap();
                    StatusBarService.mHashMap.put(m, uidinfo.packageName);
                    log(StatusBarService.mHashMap.get(m) + "");
                    m++;
                } 
                i++;
            }
            android.content.SharedPreferences.Editor editor = mSharedpreferences.edit();
            editor.putString(WHITE_LIST, whitelistUids.toString());            
            editor.commit();
            return;
        } while (true);        
    }

    private void refreshTraffic() {
        new RefreshTrafficTask().execute();
    }

    private class RefreshTrafficTask extends AsyncTask<Object, Void, Void> {
        @Override        
        protected void onPreExecute() {
            //mLoadingAppsListDialog.show();
            mUidInfos.clear();
            mLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Object... arg0) {
            buildUidInfoList(WhiteListActivity.this, mUidInfos, mPM);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mLoading.setVisibility(View.GONE);
            mUidInfoAdapter.refresh(mUidInfos);
            //mLoadingAppsListDialog.dismiss();
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
            //if(convertView == null) {
            convertView = createView(position, parent);
            //}
            bindView(position, convertView);
            return convertView;
        }

        private List mActiveUids;
        private List mAllUids;
        private Context mContext;
        private boolean mDisplaySystemApp;
        private List mNonSystemUids;
        private OnCheckedChangeListener mCheckedListener;


        class CheckedChangeListener implements OnCheckedChangeListener {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                // TODO Auto-generated method stub
                ((UidInfo)buttonView.getTag()).mEnable = isChecked;
            }

        }

        public UidInfoAdapter(Context context) {
            super();
            //mMobileListener = new MobileListener();
            mCheckedListener = new CheckedChangeListener();
            mContext = context;
            mDisplaySystemApp = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("display_system_app", true);
            mDisplaySystemApp = true;
            mAllUids = new ArrayList();
            mNonSystemUids = new ArrayList();
            mActiveUids = new ArrayList();
        }

        private View createView(int i, ViewGroup viewgroup) {
            View view = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.whitelist_app_info, viewgroup, false);
            ListEntry listentry = new ListEntry(null);
            listentry.icon = (ImageView)view.findViewById(R.id.icon);
            listentry.text = (TextView)view.findViewById(R.id.app_lable);
            listentry.checkBox = (CheckBox)view.findViewById(R.id.checkbox);
            //listentry.checkBox.setOnCheckedChangeListener(mCheckedListener);
            view.setTag(listentry);
            return view;            
        }

        private void bindView(int i, View view) {

            if(i == 0) {
                TextView sectionTitle = (TextView)view.findViewById(R.id.header_name);
                sectionTitle.setVisibility(View.VISIBLE);
                String text = mUserApp + "     " + String.valueOf(mSizeUserApp);
                sectionTitle.setText(text);
            }
            log("mSizeUserApp = "+String.valueOf(mSizeUserApp));
            log("mAllUids.size() = "+String.valueOf(mAllUids.size()));
            if(mSizeUserApp == i){

                TextView sectionTitle = (TextView)view.findViewById(R.id.header_name);
                sectionTitle.setVisibility(View.VISIBLE);
                String text = mSystemApp + "     " + String.valueOf(mAllUids.size());
                sectionTitle.setText(text);
            }

            UidInfo uidinfo = getUidInfoItem(i);
            if(uidinfo == null)
                return;
            ListEntry listentry = (ListEntry)view.getTag();
            listentry.text.setText(uidinfo.getLabel());
            listentry.icon.setImageDrawable(uidinfo.getIcon());
            listentry.checkBox.setTag(uidinfo);
            listentry.checkBox.setChecked(uidinfo.mEnable);
            log("bindView uidinfo.mEnable = "+uidinfo.mEnable);
        }

        private void setData() {
            if(mDisplaySystemApp) {
                mSizeUserApp = mNonSystemUids.size();
                mActiveUids = mNonSystemUids;
            } //else {
            //   mActiveUids = mNonSystemUids;
            //}
            mActiveUids.addAll(mAllUids);
            int k = mActiveUids.size();
            if (k == 0) {
                mNoApp.setVisibility(View.VISIBLE);
            } else {
                mNoApp.setVisibility(View.GONE);
            }
            //sort(mCurrentComparator);
        }

        public void sort(Comparator comparator) {
            Collections.sort(mActiveUids, comparator);
            mCurrentComparator = comparator;
            notifyDataSetChanged();
        }

        public void refresh(SparseArray sparsearray) {
            mAllUids.clear();
            mNonSystemUids.clear();
            int i = sparsearray.size();    
            int iffb = mSharedpreferences.getInt("ifFirstBlood", -1);
            for(int j = 0; j < i; j++) {
                UidInfo uidinfo = (UidInfo)sparsearray.valueAt(j);

                if(!uidinfo.mIsSystemApp) {
                    mNonSystemUids.add(uidinfo);
                }else {
                    if(uidinfo.packageName.equalsIgnoreCase("com.lewa.spm") || 
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.face") ||
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.launcher") ||
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.push") ||
                            uidinfo.packageName.equalsIgnoreCase("com.when.android.calendar365") ||
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.pond") ||
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.labi") ||
                            uidinfo.packageName.equalsIgnoreCase("com.android.settings") ||
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.pim") ||
                            uidinfo.packageName.equalsIgnoreCase("com.gexin.rom")) {
                        if(iffb == -1) {
                            uidinfo.mEnable = true;
                            android.content.SharedPreferences.Editor editor = mSharedpreferences.edit();
                            editor.putInt("ifFirstBlood", 1);
                            editor.commit();
                            mAllUids.add(uidinfo);
                            continue;
                        }		       
                    }
                    if(uidinfo.packageName.equalsIgnoreCase("com.lewatek.swapper") || 
                            uidinfo.packageName.equalsIgnoreCase("android.tts") ||
                            uidinfo.packageName.equalsIgnoreCase("com.android.providers.downloads") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.providers.calendar") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.providers.contacts") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.provision") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.server.vpn") || 
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.feedback") || 
                            uidinfo.packageName.equalsIgnoreCase("com.lewa.providers.location") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.providers.userdictionary") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.defcontainer") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.providers.drm") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.providers.applications") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.server.vpn") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.htmlviewer") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.certinstaller") || 
                            uidinfo.packageName.equalsIgnoreCase("com.gexin.rom") || 
                            uidinfo.packageName.equalsIgnoreCase("com.google.android.dsf") || 
                            uidinfo.packageName.equalsIgnoreCase("com.android.quicksearchbox")) {
                        uidinfo.mEnable = true;
                        continue;
                    }
                    mAllUids.add(uidinfo);

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

        List mApplications;
        boolean mEnable;
        Drawable mIcon;
        boolean mIsRunning;
        boolean mIsSystemApp;
        String mLabel;
        String packageName;
        int mUid;

        public UidInfo(int i) {
            mApplications = new ArrayList();
            mUid = i;
            mEnable = false;
        }
    }

    private static class ListEntry {

        private ImageView icon;
        private TextView text;
        private CheckBox checkBox;

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

            return 0;
        }

        public  int compare(Object obj, Object obj1) {
            UidInfo uidinfo = (UidInfo)obj;
            UidInfo uidinfo1 = (UidInfo)obj1;
            return compare(uidinfo, uidinfo1);
        }    
    }

    private static void log(String msg) {
        if (LOGC) {
            Log.e(TAG, "zhangbo " + msg);
        }        
    }
}
