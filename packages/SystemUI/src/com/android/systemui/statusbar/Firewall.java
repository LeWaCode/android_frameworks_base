package com.android.systemui.statusbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;
import java.io.InputStream;

import android.R.integer;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;
import android.app.ActivityManager;

import com.android.systemui.R;

public final class Firewall {

    public static final String FIREWALL_PREFS = "FireWallPrefs";
    public static final String WIFI_ALLOWED_UIDS = "WifiAllowedUids";
    public static final String WIFI_FORBIDDEN_UIDS = "WifiForbiddenUids";
    public static final String MOBILE_ALLOWED_UIDS = "MobileAllowedUids";
    public static final String MOBILE_FORBIDDEN_UIDS = "MobileForbiddenUids";

    public static final String MOBILE_ACCESS_MODE = "MobileAccessMode";
    public static final String WIFI_ACCESS_MODE = "WifiAccessMode";

    public static final String ENABLED = "Enabled";
    public static final String LOGENABLED = "LogEnabled";

    public static final int ANY = 65526;
    public static final int ACCESS_ALL_ALLOWED = 2;
    public static final int ACCESS_ALL_FORBIDDEN = 0;
    public static final int ACCESS_NORMAL = 1;

    private static String TAG = "Firewall";
    private static boolean DBG = false;
    
    public static boolean applyRules(Context context, SparseArray sparsearray, boolean showErrors) {
        saveRules(context, sparsearray);
        return applySavedRules(context, showErrors);
    }
    
    public static void saveRules(Context context, SparseArray sparsearray) {
        if (context == null || sparsearray == null) {
            log("saveRules error!");
            return;
        }
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        StringBuilder WifiAllowedUids = new StringBuilder();
        StringBuilder WifiForbiddenUids = new StringBuilder();
        StringBuilder MobileAllowedUids = new StringBuilder();
        StringBuilder MobileForbiddenUids = new StringBuilder();
        int k = getMobileAccessMode(context);
        int l = getWifiAccessMode(context);
        if (k == ACCESS_ALL_ALLOWED) {
            MobileAllowedUids.append(ANY);
        } else if (k == ACCESS_ALL_FORBIDDEN)
        {
            MobileForbiddenUids.append(ANY);
        }
        
        if (l == ACCESS_ALL_ALLOWED) {
            WifiAllowedUids.append(ANY);
        } else if (l == ACCESS_ALL_FORBIDDEN)
        {
            WifiForbiddenUids.append(ANY);
        }

        int i = 0;
        int j = sparsearray.size();
        do {
            while (i < j) {
                FirewallActivity.UidInfo uidinfo = (FirewallActivity.UidInfo)sparsearray.valueAt(i);
                int uid = uidinfo.mUid;
                if (uidinfo.mEnableWifi) {
                    if(WifiAllowedUids.length() != 0) {
                        WifiAllowedUids.append('|');
                    }
                    WifiAllowedUids.append(uid);
                } else {
                    if(WifiForbiddenUids.length() != 0) {
                        WifiForbiddenUids.append('|');
                    }
                    WifiForbiddenUids.append(uid);
                    
                    if (uidinfo.packageName.equalsIgnoreCase("com.tencent.qq")) {
                    killBackgroundProcesses(context, uidinfo.packageName);
                    }
                    /*if (uidinfo.mIsRunning) {
                        killBackgroundProcesses(context, uidinfo.packageName);
                    }*/
                }
                
                if (uidinfo.mEnableMobile) {
                    if(MobileAllowedUids.length() != 0) {
                        MobileAllowedUids.append('|');
                    }
                    MobileAllowedUids.append(uid);
                } else {
                    if(MobileForbiddenUids.length() != 0) {
                        MobileForbiddenUids.append('|');
                    }
                    MobileForbiddenUids.append(uid);
                    
                    if (uidinfo.packageName.equalsIgnoreCase("com.tencent.qq")) {
                        killBackgroundProcesses(context, uidinfo.packageName);
                    }
                    /*if (uidinfo.mIsRunning) {
                        killBackgroundProcesses(context, uidinfo.packageName);
                    }*/
                }
                i++;
            }
            android.content.SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(WIFI_ALLOWED_UIDS, WifiAllowedUids.toString());            
            editor.putString(WIFI_FORBIDDEN_UIDS, WifiForbiddenUids.toString());
            editor.putString(MOBILE_ALLOWED_UIDS, MobileAllowedUids.toString());
            editor.putString(MOBILE_FORBIDDEN_UIDS, MobileForbiddenUids.toString());
            editor.commit();
            return;
        } while (true);    
    }

    private static void killBackgroundProcesses(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager)(context.getSystemService(Context.ACTIVITY_SERVICE)); 
        activityManager.killBackgroundProcesses(packageName);      
    }
        
    public static boolean applySavedRules(Context context, boolean showErrors) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        String WifiAllowedUids = sharedpreferences.getString(WIFI_ALLOWED_UIDS, "");
        String WifiForbiddenUids = sharedpreferences.getString(WIFI_FORBIDDEN_UIDS, "");
        String MobileAllowedUids = sharedpreferences.getString(MOBILE_ALLOWED_UIDS, "");
        String MobileForbiddenUids = sharedpreferences.getString(MOBILE_FORBIDDEN_UIDS, "");
        int wifiAllowed[] = parseUidsString(WifiAllowedUids);
        int wifiForbidden[] = parseUidsString(WifiForbiddenUids);
        int mobileAllowed[] = parseUidsString(MobileAllowedUids);
        int mobileForbidden[] = parseUidsString(MobileForbiddenUids);
        if (!applyRulesImpl(context, wifiAllowed, mobileAllowed, true, showErrors)) {
            return false;
        } else {
            if (!applyRulesImpl(context, wifiForbidden, mobileForbidden, false, showErrors)) {
                return false;
            } else {
                return true;
            }
        }
    }

    private static boolean applyRulesImpl(Context context, int wifi[], int mobile[], boolean IsAllowed, boolean showErrors) {
        if (context == null) {
            return false;
        }
        assertBinaries(context, showErrors); 
        final String ITFS_WIFI[] = { "tiwlan+", "wlan+", "eth+" }; 
        final String ITFS_3G[] = { "rmnet+", "pdp+", "ppp+", "uwbr+", "wimax+", "vsnet+" }; 
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        boolean logenabled = sharedpreferences.getBoolean(LOGENABLED, false);
        boolean any_wifi;
        boolean any_3g;
        String targetRule;
        if(IsAllowed)
            targetRule = "RETURN";
        else
            targetRule = "droidwall-reject";  
        
        if(wifi.length <= 0 || wifi[0] != ANY) {
            any_wifi = false;
        } else {
            any_wifi = true;
        }

        if(mobile.length <= 0 || mobile[0] != ANY) {
            any_3g = false;    
        } else {
            any_3g = true;
        }

        final StringBuilder script = new StringBuilder();       
        try {
            int code; 
            script.append(scriptHeader(context)); 
            script.append("" + "$IPTABLES --version || exit 1\n" 
                    + "# Create the droidwall chains if necessary\n" 
                    + "$IPTABLES -L droidwall >/dev/null 2>/dev/null || $IPTABLES --new droidwall || exit 2\n" 
                    + "$IPTABLES -L droidwall-3g >/dev/null 2>/dev/null || $IPTABLES --new droidwall-3g || exit 3\n" 
                    + "$IPTABLES -L droidwall-wifi >/dev/null 2>/dev/null || $IPTABLES --new droidwall-wifi || exit 4\n" 
                    + "$IPTABLES -L droidwall-reject >/dev/null 2>/dev/null || $IPTABLES --new droidwall-reject || exit 5\n" 
                    + "# Add droidwall chain to OUTPUT chain if necessary\n" 
                    + "$IPTABLES -L OUTPUT | $GREP -q droidwall || $IPTABLES -A OUTPUT -j droidwall || exit 6\n" 
                    + "# Flush existing rules\n" 
                    + "$IPTABLES -F droidwall || exit 7\n" 
                    + "$IPTABLES -F droidwall-3g || exit 8\n" 
                    + "$IPTABLES -F droidwall-wifi || exit 9\n" 
                    + "$IPTABLES -F droidwall-reject || exit 10\n" + ""); 

            // Check if logging is enabled 
            if (logenabled) { 
                script.append("" 
                        + "# Create the log and reject rules (ignore errors on the LOG target just in case it is not available)\n" 
                        + "$IPTABLES -A droidwall-reject -j LOG --log-prefix \"[DROIDWALL] \" --log-uid\n" 
                        + "$IPTABLES -A droidwall-reject -j REJECT || exit 11\n" 
                        + ""); 
            } else { 
                script.append("" 
                        + "# Create the reject rule (log disabled)\n" 
                        + "$IPTABLES -A droidwall-reject -j REJECT || exit 11\n" 
                        + ""); 
            } 
            
            if (IsAllowed && logenabled) { 
                script.append("# Allow DNS lookups on white-list for a better logging (ignore errors)\n"); 
                script.append("$IPTABLES -A droidwall -p udp --dport 53 -j RETURN\n"); 
            }
            
            script.append("# Main rules (per interface)\n");
            for (int i = 0; i < ITFS_WIFI.length; i++) {
                script.append("$IPTABLES -A droidwall -o ").append(ITFS_WIFI[i]).append(" -j droidwall-wifi || exit\n");
            }
            for (int i = 0; i < ITFS_3G.length; i++) {
                script.append("$IPTABLES -A droidwall -o ").append(ITFS_3G[i]).append(" -j droidwall-3g || exit\n");
            }
            
            if (IsAllowed && !any_wifi) {
                int k = android.os.Process.getUidForName("dhcp");
                if(k != -1)
                {
                    script.append("# dhcp user\n");
                    script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner ").append(k).append(" -j RETURN || exit\n");
                }
                k = android.os.Process.getUidForName("wifi");
                if(k != -1)
                {
                    script.append("# wifi user\n");
                    script.append("$IPTABLES -A droidwall-wifi -m owner --uid-owner ").append(k).append(" -j RETURN || exit\n");
                }
            }
            
            if (any_3g) { 
                if (!IsAllowed) { 
                    /* block any application on this interface */ 
                    script.append("$IPTABLES -A droidwall-3g -j ") 
                            .append(targetRule).append(" || exit\n"); 
                } 
            } else { 
                /* release/block individual applications on this interface */ 
                for (final Integer uid : mobile) { 
                    if (uid >= 0) {
                        script.append( 
                        "$IPTABLES -A droidwall-3g -m owner --uid-owner ") 
                        .append(uid).append(" -j ").append(targetRule) 
                        .append(" || exit\n"); 
                    }
                } 
            } 
            
            if (any_wifi) { 
                if (!IsAllowed) { 
                    /* block any application on this interface */ 
                    script.append("$IPTABLES -A droidwall-wifi -j ") 
                    .append(targetRule).append(" || exit\n"); 
                } 
            } else { 
                /* release/block individual applications on this interface */ 
                for (final Integer uid : wifi) { 
                    if (uid >= 0) {
                        script.append( 
                        "$IPTABLES -A droidwall-wifi -m owner --uid-owner ") 
                        .append(uid).append(" -j ").append(targetRule) 
                        .append(" || exit\n"); 
                    }
                } 
            } 
            
            if (IsAllowed) {
                if (!any_3g) {
                    script.append("$IPTABLES -A droidwall-3g -j droidwall-reject || exit\n"); 
                }
                if (!any_wifi) {
                    script.append("$IPTABLES -A droidwall-wifi -j droidwall-reject || exit\n");
                }
            } else {
                if (any_3g) {
                    script.append("$IPTABLES -A droidwall-3g -j droidwall-reject || exit\n");
                }
                if (any_wifi) {
                    script.append("$IPTABLES -A droidwall-wifi -j droidwall-reject || exit\n");
                }
            }
            
            final StringBuilder res = new StringBuilder(); 
            code = runScriptAsRoot(context, script.toString(), res); 
            if (showErrors && code != 0) { 
                String msg = res.toString(); 
                Log.e("DroidWall", msg); 
                // Remove unnecessary help message from output 
                if (msg.indexOf("\nTry `iptables -h' or 'iptables --help' for more information.") != -1) { 
                    msg = msg.replace( 
                    "\nTry `iptables -h' or 'iptables --help' for more information.", ""); 
                } 
                alert(context, "Error applying iptables rules. Exit code: " + code 
                    + "\n\n" + msg.trim()); 
            } else { 
                return true; 
            }             
        } catch (Exception e) {
            // TODO: handle exception
            if (showErrors) {
                alert(context, "error refreshing iptables: " + e); 
            }
        }        
        return false;
    }

    public static int runScriptAsRoot(Context context, String script, StringBuilder res)
        throws IOException {
        return runScriptAsRoot(context, script, res, 40000L);
    }

    public static int runScriptAsRoot(Context context, String script, StringBuilder res, long timeout) {
        return runScript(context, script, res, timeout, true);
    }
    
    public static int runScript(Context context, String script, StringBuilder res, long timeout, boolean asroot) {     
        final File file = new File(context.getCacheDir(), "firewall.sh"); 
        final ScriptRunner runner = new ScriptRunner(context, file, script, res, asroot); 
        runner.start(); 
        try { 
            if (timeout > 0) { 
                runner.join(timeout); 
            } else { 
                runner.join(); 
            } 
            if (runner.isAlive()) { 
                // Timed-out 
                runner.interrupt(); 
                runner.join(150); 
                runner.destroy(); 
                runner.join(50); 
            } 
        } catch (InterruptedException ex) { 
        } 
        return runner.exitcode; 
    }
    
    private static String scriptHeader(Context context) {
        /*final String dir = context.getCacheDir().getAbsolutePath(); 
        final String myiptables = dir + "/iptables_armv5"; 
        return "" + "IPTABLES=iptables\n" + "BUSYBOX=busybox\n" + "GREP=grep\n" + "ECHO=echo\n" 
                + "# Try to find busybox\n"
                + "if busybox --help >/dev/null 2>/dev/null ; then\n" 
                + "BUSYBOX=busybox\n"                 
                + "elif /system/xbin/busybox --help >/dev/null 2>/dev/null ; then\n" 
                + " BUSYBOX=/system/xbin/busybox\n" 
                + "elif /system/bin/busybox --help >/dev/null 2>/dev/null ; then\n" 
                + " BUSYBOX=/system/bin/busybox\n"                 
                + "fi\n" 
                
                + "# Try to find grep\n" 
                + "if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n" 
                + " if $ECHO 1 | $BUSYBOX grep -q 1 >/dev/null 2>/dev/null ; then\n" 
                + "     GREP=\"$BUSYBOX grep\"\n" 
                + " fi\n" 
                
                + " # Grep is absolutely required\n" 
                + " if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n" 
                + "     $ECHO The grep command is required. DroidWall will not work.\n" 
                + "     exit 1\n" 
                + " fi\n" 
                + "fi\n" ; */
        String s = context.getDir("bin", 0).getAbsolutePath();
        String s1 = String.valueOf(s);
        String s2 = (new StringBuilder(s1)).append("/iptables_armv5").toString();
        return (new StringBuilder("IPTABLES=iptables\nBUSYBOX=busybox\nGREP=grep\nECHO=echo\n# Try to find busybox\nif "))
            .append(s).append("/busybox_g1 --help >/dev/null 2>/dev/null ; then\n")
            .append("\tBUSYBOX=").append(s).append("/busybox_g1\n")
            .append("\tGREP=\"$BUSYBOX grep\"\n").append("\tECHO=\"$BUSYBOX echo\"\n")
            .append("elif busybox --help >/dev/null 2>/dev/null ; then\n").append("\tBUSYBOX=busybox\n")
            .append("elif /system/xbin/busybox --help >/dev/null 2>/dev/null ; then\n").append("\tBUSYBOX=/system/xbin/busybox\n")
            .append("elif /system/bin/busybox --help >/dev/null 2>/dev/null ; then\n").append("\tBUSYBOX=/system/bin/busybox\n").append("fi\n")
            .append("# Try to find grep\n").append("if ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n")
            .append("\tif $ECHO 1 | $BUSYBOX grep -q 1 >/dev/null 2>/dev/null ; then\n").append("\t\tGREP=\"$BUSYBOX grep\"\n").append("\tfi\n")
            .append("\t# Grep is absolutely required\n").append("\tif ! $ECHO 1 | $GREP -q 1 >/dev/null 2>/dev/null ; then\n")
            .append("\t\t$ECHO The grep command is required. DroidWall will not work.\n").append("\t\texit 1\n").append("\tfi\n").append("fi\n")
            .append("# Try to find iptables\n").append("if ").append(s2).append(" --version >/dev/null 2>/dev/null ; then\n").append("\tIPTABLES=")
            .append(s2).append("\n").append("fi\n").toString();
        
    }

    public static boolean assertBinaries(Context ctx, boolean showErrors) { 
        boolean changed = false; 
        try { 
            // Check iptables_armv5 
            File file = new File(ctx.getDir("bin", 0), "iptables_armv5"); 
            if (!file.exists()) { 
                copyRawFile(ctx, R.raw.iptables_armv5, file, "755"); 
                changed = true; 
            } 
            // Check busybox 
            file = new File(ctx.getDir("bin", 0), "busybox_g1"); 
            if (!file.exists()) { 
                copyRawFile(ctx, R.raw.busybox_g1, file, "755"); 
                changed = true; 
            } 
            if (changed) { 
                //Toast.makeText(ctx, R.string.toast_bin_installed, 
                        //Toast.LENGTH_LONG).show(); 
            } 
        } catch (Exception e) { 
            if (showErrors) 
                alert(ctx, "Error installing binary files: " + e); 
            return false; 
        } 
        return true; 
    } 

    private static void copyRawFile(Context context, int i, File file, String s)
        throws IOException, InterruptedException
    {
        String s1 = file.getAbsolutePath();
        FileOutputStream fileoutputstream = new FileOutputStream(file);
        InputStream inputstream = context.getResources().openRawResource(i);
        byte abyte0[] = new byte[1024];
        do
        {
            int j = inputstream.read(abyte0);
            if(j <= 0)
            {
                fileoutputstream.close();
                inputstream.close();
                Runtime runtime = Runtime.getRuntime();
                String s2 = (new StringBuilder("chmod ")).append(s).append(" ").append(s1).toString();
                int k = runtime.exec(s2).waitFor();
                return;
            }
            fileoutputstream.write(abyte0, 0, j);
        } while(true);
    }
    
    public static void loadRules(Context context, SparseArray sparsearray) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        
        int wifiAllowed[] = parseUidsString(sharedpreferences.getString(WIFI_ALLOWED_UIDS, ""));
        int i = 0;
        do {
            int j = wifiAllowed.length;
            if(i >= j)
                break;
            FirewallActivity.UidInfo uidinfo = (FirewallActivity.UidInfo)sparsearray.get(wifiAllowed[i]);
            if(uidinfo != null) {
                uidinfo.mEnableWifi = true;
            }
            i++;
        } while(true);
        
        int wifiForbidden[] = parseUidsString(sharedpreferences.getString(WIFI_FORBIDDEN_UIDS, ""));
        i = 0;
        do {
            int j = wifiForbidden.length;
            if(i >= j)
                break;
            FirewallActivity.UidInfo uidinfo = (FirewallActivity.UidInfo)sparsearray.get(wifiForbidden[i]);
            if(uidinfo != null) {
                uidinfo.mEnableWifi = false;
            }
            i++;
        } while(true);
        
        int mobileAllowed[] = parseUidsString(sharedpreferences.getString(MOBILE_ALLOWED_UIDS, ""));
        i = 0;
        do {
            int j = mobileAllowed.length;
            if(i >= j)
                break;
            FirewallActivity.UidInfo uidinfo = (FirewallActivity.UidInfo)sparsearray.get(mobileAllowed[i]);
            if(uidinfo != null) {
                uidinfo.mEnableMobile = true;
            }
            i++;
        } while(true);
        
        int mobileForbidden[] = parseUidsString(sharedpreferences.getString(MOBILE_FORBIDDEN_UIDS, ""));
        i = 0;
        do {
            int j = mobileForbidden.length;
            if(i >= j)
                return;
            FirewallActivity.UidInfo uidinfo = (FirewallActivity.UidInfo)sparsearray.get(mobileForbidden[i]);
            if(uidinfo != null) {
                uidinfo.mEnableMobile = false;
            }
            i++;
        } while(true);
    }
    
    public static boolean purgeIptables(Context context, boolean showErrors) {
        StringBuilder res = new StringBuilder(); 
        try { 
            assertBinaries(context, showErrors); 
            int code = runScriptAsRoot(context, scriptHeader(context) 
                    + "$IPTABLES -F droidwall\n" 
                    + "$IPTABLES -F droidwall-reject\n" 
                    + "$IPTABLES -F droidwall-3g\n" 
                    + "$IPTABLES -F droidwall-wifi\n", res); 
            if (code == -1) { 
                if (showErrors) 
                    alert(context, "error purging iptables. exit code: " + code + "\n" + res); 
                return false; 
            } 
            return true; 
        } catch (Exception e) { 
            if (showErrors) 
                alert(context, "error purging iptables: " + e); 
            return false; 
        } 
    }
    
    private static int[] parseUidsString(String s) {
        int ai[] = new int[0];
        if(s.length() > 0)
        {
            StringTokenizer stringtokenizer = new StringTokenizer(s, "|");
            ai = new int[stringtokenizer.countTokens()];
            int i = 0;
            do
            {
                int j = ai.length;
                if(i >= j)
                    break;
                String s1 = stringtokenizer.nextToken();
                if(!s1.equals(""))
                    try
                    {
                        int k = Integer.parseInt(s1);
                        ai[i] = k;
                    }
                    catch(Exception exception)
                    {
                        ai[i] = -1;
                    }
                i++;
            } while(true);
        }
        return ai;
    }
    
    public Firewall() {
        
    }
    
    private static void alert(Context context, String s) {
        if(context == null) {
            return;
        } else {
            return;
        }
    }
    
    public static int getMobileAccessMode(Context context) {
        return context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE).getInt(MOBILE_ACCESS_MODE, ACCESS_ALL_ALLOWED);
    }

    public static int getWifiAccessMode(Context context) {
        return context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE).getInt(WIFI_ACCESS_MODE, ACCESS_ALL_ALLOWED);
    }
    
    public static void setMobileAccessMode(Context context, int accessMode) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        if(sharedpreferences.getInt(MOBILE_ACCESS_MODE, ACCESS_ALL_ALLOWED) == accessMode) {
            return;
        } else {
            android.content.SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(MOBILE_ACCESS_MODE, accessMode);
            editor.commit();
            return;
        }
    }

    public static void setWifiAccessMode(Context context, int accessMode) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        if(sharedpreferences.getInt(WIFI_ACCESS_MODE, ACCESS_ALL_ALLOWED) == accessMode) {
            return;
        } else {
            android.content.SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(WIFI_ACCESS_MODE, accessMode);
            editor.commit();
            return;
        }
    }
    
    public static void setEnabled(Context context, boolean isEnable) {
        SharedPreferences sharedpreferences = context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE);
        if(sharedpreferences.getBoolean(ENABLED, false) == isEnable) {
            return;
        } else {
            android.content.SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(ENABLED, isEnable);
            editor.commit();
            return;
        }
    }
    
    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(FIREWALL_PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false);
    }
        
    private static final class ScriptRunner extends Thread {
        private final boolean asroot;
        private Context context;
        private Process exec;
        public int exitcode;
        private final File file;
        private final StringBuilder res;
        private final String script;
        
        public ScriptRunner(Context context1, File file1, String s, StringBuilder stringbuilder, boolean flag) {
            exitcode = -1;
            context = context1;
            file = file1;
            script = s;
            res = stringbuilder;
            asroot = flag;
        }
        
        public void run() {
            try {
                file.createNewFile();
                final String abspath = file.getAbsolutePath(); 
                // make sure we have execution permission on the script file
                Runtime.getRuntime().exec("chmod 777 " + abspath).waitFor(); 
                // Write the script to be executed 
                final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
                if (new File("/system/bin/sh").exists()) { 
                    out.write("#!/system/bin/sh\n"); 
                } 
                out.write(script);
                if (!script.endsWith("\n")) 
                    out.write("\n"); 
                out.write("exit\n"); 
                out.flush(); 
                out.close(); 
                
                boolean flag1 = android.provider.Settings.Secure.putString(context.getContentResolver(), "firewall_script", abspath);                
                if (this.asroot) { 
                    // Create the "su" request to run the script 
                    exec = Runtime.getRuntime().exec("su0 -c " + abspath); 
                } else { 
                    // Create the "sh" request to run the script 
                    exec = Runtime.getRuntime().exec("sh " + abspath); 
                }                
                InputStreamReader r = new InputStreamReader(exec.getInputStream()); 
                final char buf[] = new char[1024]; 
                int read = 0; 
                // Consume the "stdout" 
                while ((read = r.read(buf)) != -1) { 
                    if (res != null) 
                        res.append(buf, 0, read); 
                } 
                // Consume the "stderr" 
                r = new InputStreamReader(exec.getErrorStream()); 
                read = 0; 
                while ((read = r.read(buf)) != -1) { 
                    if (res != null) 
                        res.append(buf, 0, read); 
                } 
                // get the process exit code 
                if (exec != null) 
                    this.exitcode = exec.waitFor();                 
            } catch (InterruptedException ex) { 
                if (res != null) 
                    res.append("\nOperation timed-out"); 
            } catch (Exception ex) { 
                if (res != null) 
                    res.append("\n" + ex); 
            } finally { 
                destroy(); 
            }                                                               
        }
        
        public void destroy() {            
            if (exec != null) {
                exec.destroy(); 
            }
            exec = null; 
        }        
    }
    
    
    private static void log(String msg) {
        if (DBG) {
            Log.e(TAG, "zhangbo " + msg);
        }        
    }    
}
