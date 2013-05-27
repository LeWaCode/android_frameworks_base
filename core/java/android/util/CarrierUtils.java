/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.util;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.PrintWriter;

import com.android.internal.util.XmlUtils;

/**
 * A class containing utility methods related to time zones.
 */
public class CarrierUtils {
    private static final String TAG = "CarrierUtils";

    public static CharSequence CarrierMobilization(String carrierString) {
    if (carrierString == null) {
            return null;
    }

     Resources r = Resources.getSystem();
     XmlResourceParser parser = r.getXml(com.android.internal.R.xml.mobilization);

      try {

        XmlUtils.beginDocument(parser, "mobilization");

        while (true) {
            XmlUtils.nextElement(parser);
            String element = parser.getName();
            if (element == null || !(element.equals("mobilization"))) {
                    break;
	    }

	    String code = parser.getAttributeValue(null, "id");

            String carrierStringtemp = carrierString.replaceAll("\n", "").replaceAll(" ", "");
            String codetemp = code.replaceAll("\n", "").replaceAll(" ", "");
             if (carrierStringtemp.equalsIgnoreCase(codetemp)/*carrierString.equalsIgnoreCase(code)*/) {
                if (parser.next() == XmlPullParser.TEXT) {
                     return parser.getText();
                }
              }
         }
       } catch (XmlPullParserException e) {
            Log.e(TAG, "Got exception while getting preferred  Carrier.", e);
       } catch (IOException e) {
            Log.e(TAG, "Got exception while getting preferred  Carrier.", e);
       } finally {
            parser.close();
       }
       return carrierString;
    }
}
