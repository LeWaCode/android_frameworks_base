package com.android.systemui.statusbar;

import com.android.systemui.R;

import java.util.Calendar;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.regex.Pattern;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MonitorSetupActivity extends PreferenceActivity implements OnPreferenceChangeListener, 
        OnPreferenceClickListener {    

    public static final int MAX_TEXT_INPUT_LENGTH = 6; 
    private static final long MB_UNIT = 1024 * 1024;
    private static final long KB_UNIT = 1024;
    private static final String TAG = "MonitorSetupActivity";
    
    private static SharedPreferences mSharedPreferences;
    private Context mContext;
    private Editor mEditor;
    
    PreferenceScreen monthly_data;
    CheckBoxPreference remind;
    ListPreference start_date;
    Preference sms_query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.monitorsetup);
        
        addPreferencesFromResource(R.xml.monitorset_preference);

        mContext = this;        
        mSharedPreferences = mContext.getSharedPreferences(DataMonitorService.PREFERENCE_NAME, Activity.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        
        monthly_data = (PreferenceScreen) findPreference("monthlydata_key");
        monthly_data.setOnPreferenceClickListener(this);
        
        remind = (CheckBoxPreference) findPreference("remindme_key");
        remind.setSummary(getString(R.string.reminder_data, 
                mSharedPreferences.getLong(DataMonitorService.REMIND_DATA, 5 * MB_UNIT ) / MB_UNIT));
        remind.setOnPreferenceChangeListener(this);
        
        start_date = (ListPreference) findPreference("startdate_key");
        start_date.setSummary(getString(R.string.start_from, mSharedPreferences.getInt(DataMonitorService.START_DATE, 1)));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int entries = 0;
        int entryValue = 0;
        switch(month){
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                entries = R.array.startdata1;
                entryValue = R.array.startdata1;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                entries = R.array.startdata2;
                entryValue = R.array.startdata2;
                break;
            case 2:
                if((year%400 == 0) || (year % 100 != 0 && year%4 == 0)) {
                    entries = R.array.startdata4;
                    entryValue = R.array.startdata4;
                } else {
                    entries = R.array.startdata3;
                    entryValue = R.array.startdata3;
                }
                break;
            default:
                entries = R.array.startdata1;
                entryValue = R.array.startdata1;
        }
        start_date.setEntries(entries);
        start_date.setEntryValues(entryValue);  
        
        start_date.setOnPreferenceChangeListener(this);
        
        sms_query = findPreference("smsquery_key");
        sms_query.setSummary(mSharedPreferences.getInt(DataMonitorService.QUERY_NUMBER, 0) + "");
        sms_query.setOnPreferenceClickListener(this);
        
        Preference clear_history = findPreference("clearhistory_key");
        clear_history.setOnPreferenceClickListener(this);

        EditTextPreference verify_data = (EditTextPreference) findPreference("verifydata_key");
        verify_data.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preference.getKey().equals("monthlydata_key")) {
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.monthly_data_input, null);
            final EditText edit = (EditText)layout.findViewById(android.R.id.edit);
            String monthly = String.valueOf(mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 30 * MB_UNIT)/MB_UNIT);
            edit.setText(monthly);
            edit.setSelection(0, monthly.length());
            AlertDialog dialog = new AlertDialog.Builder(this).setView(layout)
            .setTitle(R.string.please_input_monthly_data)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = edit.getText().toString();
                    long month_total = 0;
                    long monthused = mSharedPreferences.getLong(DataMonitorService.MONTH_USED, 0);
                    long month_left = month_total - monthused;
                    long today_used = mSharedPreferences.getLong(DataMonitorService.TODAY_USED, 0);
                    long total_data = 0;
                    
                    if(!value.trim().equals("")){
                        total_data = Long.valueOf(value);
                        month_total = MB_UNIT * total_data;
                        month_left = month_total - monthused;
                        today_used = mSharedPreferences.getLong(DataMonitorService.TODAY_USED, 0);        
                                        
                        if (month_total < 0) {
                            month_total = 0;
                        }
                        if (month_left < 0) {                    
                            month_left = 0;
                        }
                    }
                    
                    DataActivity.updateTodayLeftData(today_used, 0, monthused);  
                    mEditor.putLong(DataMonitorService.MONTH_TOTAL, month_total);                      
                    mEditor.putLong(DataMonitorService.MONTH_LEFT, month_left / MB_UNIT);
                    mEditor.commit();
                    preference.setSummary(StatusBarService.humanReadableByteCount(month_total, false, false));
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
            dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        } else if (preference.getKey().equals("smsquery_key")) {
            LinearLayout inputLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.sms_query_setting_dialog, null);        
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);                
            builder.setView(inputLayout);
            builder.setTitle(R.string.sms_query);
            
            final EditText sms_input_code = (EditText)inputLayout.findViewById(R.id.sms_input_code);
            final EditText sms_input_number = (EditText)inputLayout.findViewById(R.id.sms_input_number);
            sms_input_code.setText(mSharedPreferences.getString(DataMonitorService.QUERY_CODE, ""));
            sms_input_code.setSelection(mSharedPreferences.getString(DataMonitorService.QUERY_CODE, "").length());
            
            sms_input_number.setText(mSharedPreferences.getInt(DataMonitorService.QUERY_NUMBER, 0)+"");    
            sms_input_number.setSelection((mSharedPreferences.getInt(DataMonitorService.QUERY_NUMBER, 0)+"").length());
            
            sms_input_number.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_TEXT_INPUT_LENGTH)});
            
            Button button1 = (Button)inputLayout.findViewById(R.id.button1);
            Button button2 = (Button)inputLayout.findViewById(R.id.button2);
            Button button3 = (Button)inputLayout.findViewById(R.id.button3);
            Button button4 = (Button)inputLayout.findViewById(R.id.button4);
            
            final AlertDialog alertDialog = builder.show();
            
            button1.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"+mSharedPreferences.getInt(DataMonitorService.QUERY_NUMBER, 0)));
                    intent.putExtra("sms_body", mSharedPreferences.getString(DataMonitorService.QUERY_CODE, ""));
                    startActivity(intent);    
                    alertDialog.dismiss();
                }
            });
            
            button2.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    String defaul_query_code = mSharedPreferences.getString(DataMonitorService.DEFAULT_QUERY_CODE, "");
                    int defaul_query_number = mSharedPreferences.getInt(DataMonitorService.DEFAULT_QUERY_NUMBER, 0);                    
                    sms_input_code.setText(defaul_query_code);
                    sms_input_number.setText(defaul_query_number+"");  
                    preference.setSummary(defaul_query_number+"");
                    mEditor.putString(DataMonitorService.QUERY_CODE, defaul_query_code);
                    mEditor.putInt(DataMonitorService.QUERY_NUMBER, defaul_query_number);    
                    mEditor.commit();
                    alertDialog.dismiss();
                }
            });
            
            button3.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    String sms_code_str = sms_input_code.getText().toString();
                    if (sms_code_str != null && !sms_code_str.trim().equals("")) {
                        mEditor.putString(DataMonitorService.QUERY_CODE, sms_code_str);
                        mEditor.commit();
                    } else {

                    }
                    String sms_number_str = sms_input_number.getText().toString();
                    if (sms_number_str != null && !sms_number_str.trim().equals("")) {
                        int number = Integer.valueOf(sms_number_str);
                        preference.setSummary(sms_number_str);
                        mEditor.putInt(DataMonitorService.QUERY_NUMBER, number);
                        mEditor.commit();
                        
                    } else {

                    }
                    alertDialog.dismiss();
                }
            });
            
            button4.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    alertDialog.dismiss();
                }
            });
        } else if(preference.getKey().equals("clearhistory_key")) {
            LinearLayout inputLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.clear_history_dialog, null);        
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);                
            builder.setView(inputLayout);
            builder.setTitle(R.string.clear_history);
            
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {                    
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub    
                    long month_total = mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 0);                        
                    DataActivity.updateTodayLeftData(0, month_total, 0);

                    mEditor.putLong(DataMonitorService.WLAN_TODAY, 0);
                    mEditor.putLong(DataMonitorService.WLAN_MONTH, 0);
                    mEditor.putLong(DataMonitorService.TODAY_USED, 0);
                    mEditor.putLong(DataMonitorService.MONTH_USED, 0);
                    mEditor.putLong(DataMonitorService.MONTH_LEFT, 
                            mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 0));
                    mEditor.commit();
                    FirewallActivity.clearUidDataUsage(mContext);
		      NotificationFirewall.getInstance(mContext).removeAllBlockPackage();
                }
            });                
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {                    
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub                        
                }
            });
            builder.show();
        } 
        return false;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        
        if(preference.getKey().equals("remindme_key")) {
            if(!((CheckBoxPreference)preference).isChecked()) {
                ((CheckBoxPreference)preference).setChecked(true);
                DataMonitorService.mRemind = false;
                
                LinearLayout inputLayout = (LinearLayout)getLayoutInflater().
                        inflate(R.layout.reminder_month_input_dialog, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);                
                builder.setView(inputLayout);
                builder.setTitle(R.string.reminder_me);
                
                final EditText editText = (EditText)inputLayout.findViewById(R.id.reminder_input_data);
                editText.setText(mSharedPreferences.getLong(DataMonitorService.REMIND_DATA, 0) / MB_UNIT +"");
                editText.setSelection((mSharedPreferences.
                        getLong(DataMonitorService.REMIND_DATA, 0) / MB_UNIT +"").length());
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_TEXT_INPUT_LENGTH)});
                
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub                       
                        String remind_data_str = editText.getText().toString();
                        if(remind_data_str != null && !remind_data_str.trim().equals("")){
                            DataMonitorService.mRemind = false;
                            long remind_data = MB_UNIT * Long.valueOf(remind_data_str);    
                            preference.setSummary(getString(R.string.reminder_data, remind_data / MB_UNIT));
                            mEditor.putLong(DataMonitorService.REMIND_DATA, remind_data);
                            mEditor.commit();
                        } else {
                            preference.setSummary(getString(R.string.reminder_data, 0));
                            mEditor.putLong(DataMonitorService.REMIND_DATA, 0);
                            mEditor.commit();
                        }
                    }
                });
                builder.show();
            } else {
                ((CheckBoxPreference)preference).setChecked(false);
            }
            mEditor.putBoolean(DataMonitorService.REMIND_ME, ((CheckBoxPreference)preference).isChecked());
            mEditor.commit();
            
        } else if(preference.getKey().equals("startdate_key")) {
            mEditor.putInt(DataMonitorService.START_DATE, Integer.valueOf((String)newValue));
            ((ListPreference)preference).setValue((String)newValue);
            mEditor.commit();
            preference.setSummary(getString(R.string.start_from, newValue)); 
            
        } else if(preference.getKey().equals("verifydata_key")) {
            String value = (String)newValue;
            if(newValue != null && !value.trim().equals("")){
                DataMonitorService.mRemind = false;
                
                String month_used_str_deal = "";
                String[] months = value.split(",");
                for(int i = 0;i < months.length; i++){
                    month_used_str_deal += months[i];
                }

                //verify month used data
                double month_used = MB_UNIT * Double.valueOf(month_used_str_deal);    
                long month_uesd_data = Math.round(month_used);                            
                long month_total = mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 0);    
                long month_left = month_total - month_uesd_data;
                long today_used = mSharedPreferences.getLong(DataMonitorService.TODAY_USED, 0);        
                if (month_uesd_data < today_used) {
                    mEditor.putLong(DataMonitorService.TODAY_USED, month_uesd_data);
                } 
                if (month_total > 0 && month_left > 0) {
                    DataActivity.updateTodayLeftData(today_used, month_left, month_uesd_data);
                    mEditor.putLong(DataMonitorService.MONTH_LEFT, month_left);
                    mEditor.putLong(DataMonitorService.MONTH_USED, month_uesd_data);
                } else {  
                    DataActivity.updateTodayLeftData(today_used, 0, month_uesd_data);
                    mEditor.putLong(DataMonitorService.MONTH_LEFT, 0);
                    mEditor.putLong(DataMonitorService.MONTH_USED, month_uesd_data);
                }    
                mEditor.commit();
            }
        }
        return false;
    }
    
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        long month_total = mSharedPreferences.getLong(DataMonitorService.MONTH_TOTAL, 30 * MB_UNIT);
        monthly_data.setSummary(StatusBarService.humanReadableByteCount(month_total, false, false));
        remind.setChecked(mSharedPreferences.getBoolean(DataMonitorService.REMIND_ME, false));
        remind.setSummary(getString(R.string.reminder_data, mSharedPreferences.getLong(DataMonitorService.REMIND_DATA, 5 * MB_UNIT)/MB_UNIT));       
        sms_query.setSummary(mSharedPreferences.getInt(DataMonitorService.QUERY_NUMBER, 0)+"");        
        start_date.setSummary(getString(R.string.start_from, mSharedPreferences.getInt(DataMonitorService.START_DATE, 1) + ""));
    }
}
