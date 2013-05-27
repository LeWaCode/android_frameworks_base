/** Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.Build;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.os.Bundle;

public class ErrorReportActivity extends Activity {

	private static final String TAG = "ErrorReportActivity";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Intent intent = getIntent();
		PackageInfo homePackageInfo = null;
		ApplicationInfo UcBrowserPackageInfo = null;
		if (Intent.ACTION_APP_ERROR.equals(intent.getAction())) {

			ApplicationErrorReport report = intent
					.getParcelableExtra(Intent.EXTRA_BUG_REPORT);
			try {
				homePackageInfo = getPackageManager().getPackageInfo(
						getPackageName(), 0);
				UcBrowserPackageInfo = getPackageManager().getApplicationInfo(
						"com.uc.browser", 0);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				String path = "http://www.lewaos.com/feedback";
				Log.e("phone info", "?LewaVersion="
						+ homePackageInfo.versionName + ",PhoneModel="
						+ Build.MODEL + ",SDK=" + Build.VERSION.SDK);
				path = path + "?LewaVersion=" + homePackageInfo.versionName
						+ "&PhoneModel=" + Build.MODEL + "&feedback=";
				switch(report.type) {
					case ApplicationErrorReport.TYPE_CRASH:
						path = path + report.crashInfo.stackTrace;
						break;
					case ApplicationErrorReport.TYPE_ANR:
						path = path + report.anrInfo.cause;
						break;
					case ApplicationErrorReport.TYPE_BATTERY:
						path = path + report.batteryInfo.usageDetails;
						break;
					case ApplicationErrorReport.TYPE_RUNNING_SERVICE:
						path = path + report.runningServiceInfo.serviceDetails;
						break;
					default:
						break;
				}
				
				if (UcBrowserPackageInfo != null) {				
					Uri uri = Uri.parse(path);			
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
					browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					browserIntent.setPackage("com.uc.browser");
					startActivity(browserIntent);
				}
				else {
					Uri uri = Uri.parse(path.replace("\n",""));
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
					browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(browserIntent);				
				}
				/*else {
					// send bug info through email
					// Setup the recipient in a String array
					// String[] mailto = { "fc@lewatek.com" };
					Uri uri = Uri.parse("mailto:fc@lewatek.com"); 
					// Create a new Intent to send messages
					Intent sendIntent = new Intent(Intent.ACTION_SENDTO, uri);
					// Write the body of theEmail
					// String emailBody = "You're password is: ";
					// Add attributes to the intent
					// sendIntent.setType("text/plain"); // use this line for
					// testing
					// in the emulator
					// sendIntent.setType("text/plain");
					// sendIntent.setType("message/rfc822"); // use this line
					// for
					// testing
					// on the real phone
					// sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report"
							+ "_" + homePackageInfo.versionName);
					sendIntent.putExtra(Intent.EXTRA_TEXT,
							report.crashInfo.stackTrace);
					// startActivity(Intent.createChooser(sendIntent, "chooser"));
					sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					sendIntent.setClassName("com.android.email",
							"com.android.email.activity.MessageCompose");
					startActivity(sendIntent);
					// send bug info to web
				}*/

			}
		}
		finish();
	}
}
