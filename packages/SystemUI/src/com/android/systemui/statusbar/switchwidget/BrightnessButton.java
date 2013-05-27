package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.os.IPowerManager;
import android.os.Power;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BrightnessButton extends ObserveButton
{
    /**
     * Minimum and maximum brightnesses. Don't go to 0 since that makes the
     * display unusable
     */
    private static final int MIN_BACKLIGHT = Power.BRIGHTNESS_DIM + 10;
    private static final int MAX_BACKLIGHT = Power.BRIGHTNESS_ON;
    // Auto-backlight level
    private static final int AUTO_BACKLIGHT = -1;
    // Mid-range brightness values + thresholds
    private static final int DEFAULT_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.4f);
    private static final int LOW_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.25f);
    private static final int MID_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.5f);
    private static final int HIGH_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.75f);

    // whether or not backlight is supported
    private static Boolean SUPPORTS_AUTO_BACKLIGHT=null;

    // CM modes of operation
    private static final int CM_MODE_AUTO_MIN_DEF_MAX = 0;
    private static final int CM_MODE_AUTO_MIN_LOW_MID_HIGH_MAX = 1;
    private static final int CM_MODE_AUTO_LOW_MAX = 2;
    private static final int CM_MODE_MIN_MAX = 3;

    public BrightnessButton() {
        super();
        mType = BUTTON_BRIGHTNESS;
        mObservedUris.add(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE));
        mObservedUris.add(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS));
        mLabel=R.string.title_toggle_brightness;
    }

    @Override
    protected void updateState() {
        if (isBrightnessSetToAutomatic()) {
            mIcon = R.drawable.stat_brightness_auto;
            mState = STATE_ENABLED;
        } else {
            switch (getBrightnessState()) {
                case STATE_ENABLED:
                    mIcon = R.drawable.stat_brightness_on;
                    mState = STATE_ENABLED;
                    break;
                case STATE_TURNING_ON:
                    mIcon = R.drawable.stat_brightness_mid;
                    mState = STATE_INTERMEDIATE;
                    break;
                case STATE_TURNING_OFF:
                    mIcon = R.drawable.stat_brightness_mid;
                    mState = STATE_INTERMEDIATE;
                    break;
                default:
                    mIcon = R.drawable.stat_brightness_off;
                    mState = STATE_DISABLED;
                    break;
            }
        }
    }

    @Override
    protected void toggleState() {
        try {
            IPowerManager ipm = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (ipm != null) {
                int brightness = getNextBrightnessValue();
                ContentResolver resolver = sContext.getContentResolver();
                if (brightness == AUTO_BACKLIGHT) {
                    Settings.System.putInt(resolver
                            , Settings.System.SCREEN_BRIGHTNESS_MODE
                            , Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                } else {
                    if (isAutomaticModeSupported()) {
                        Settings.System.putInt(resolver
                                , Settings.System.SCREEN_BRIGHTNESS_MODE
                                , Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    }
                    ipm.setBacklightBrightness(brightness);
                    Settings.System.putInt(resolver
                            , Settings.System.SCREEN_BRIGHTNESS, brightness);
                }
            }
            
        } catch (RemoteException e) {
            // Doesn't care if it fails or not
        }
    }
    
	private int getBrightnessValue() {
		int nowBrightnessValue = 10;
        try {
			nowBrightnessValue = android.provider.Settings.System.getInt(sContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return nowBrightnessValue - 30;
	}

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.DISPLAY_SETTINGS");
        return false;
    }

    private int getMinBacklight() {
        if (Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.LIGHT_SENSOR_CUSTOM, 0) != 0) {
            return Settings.System.getInt(sContext.getContentResolver()
                    , Settings.System.LIGHT_SCREEN_DIM, MIN_BACKLIGHT);
        } else {
            return MIN_BACKLIGHT;
        }
    }

    private int getNextBrightnessValue() {
        int brightness = Settings.System.getInt(
                sContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        int currentMode = getCurrentCMMode();

        if (isAutomaticModeSupported() && isBrightnessSetToAutomatic()) {
            if (currentMode == CM_MODE_AUTO_LOW_MAX) {
                return LOW_BACKLIGHT;
            } else {
                return getMinBacklight();
            }
        } else if (brightness < LOW_BACKLIGHT) {
            if (currentMode == CM_MODE_AUTO_LOW_MAX) {
                return LOW_BACKLIGHT;
            } else if (currentMode == CM_MODE_MIN_MAX) {
                return MAX_BACKLIGHT;
            } else {
                return DEFAULT_BACKLIGHT;
            }
        } else if (brightness < DEFAULT_BACKLIGHT) {
            if (currentMode == CM_MODE_AUTO_MIN_DEF_MAX) {
                return DEFAULT_BACKLIGHT;
            } else if (currentMode == CM_MODE_AUTO_LOW_MAX || currentMode == CM_MODE_MIN_MAX) {
                return MAX_BACKLIGHT;
            } else {
                return MID_BACKLIGHT;
            }
        } else if (brightness < MID_BACKLIGHT) {
            if (currentMode == CM_MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return MID_BACKLIGHT;
            } else {
                return MAX_BACKLIGHT;
            }
        } else if (brightness < HIGH_BACKLIGHT) {
            if (currentMode == CM_MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return HIGH_BACKLIGHT;
            } else {
                return MAX_BACKLIGHT;
            }
        } else if (brightness < MAX_BACKLIGHT) {
            return MAX_BACKLIGHT;
        } else if (isAutomaticModeSupported() && currentMode != CM_MODE_MIN_MAX) {
            return AUTO_BACKLIGHT;
        } else if (currentMode == CM_MODE_AUTO_LOW_MAX) {
            return LOW_BACKLIGHT;
        } else {
            return getMinBacklight();
        }
    }

    private int getBrightnessState() {
        int brightness = Settings.System.getInt(
                sContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,0);

        int currentMode = getCurrentCMMode();

        if (brightness < LOW_BACKLIGHT) {
            return SwitchButton.STATE_DISABLED;
        } else if (brightness < DEFAULT_BACKLIGHT) {
            return SwitchButton.STATE_DISABLED;
        } else if (brightness < MID_BACKLIGHT) {
            if (currentMode == CM_MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return SwitchButton.STATE_DISABLED;
            } else {
                return SwitchButton.STATE_TURNING_OFF;
            }
        } else if (brightness < HIGH_BACKLIGHT) {
            if (currentMode == CM_MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return SwitchButton.STATE_TURNING_OFF;
            } else {
                return SwitchButton.STATE_TURNING_ON;
            }
        } else if (brightness < MAX_BACKLIGHT) {
            return SwitchButton.STATE_TURNING_ON;
        } else {
            return SwitchButton.STATE_ENABLED;
        }
    }

    private boolean isAutomaticModeSupported() {
        if (SUPPORTS_AUTO_BACKLIGHT == null) {
            if (sContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available)) {
                SUPPORTS_AUTO_BACKLIGHT = true;
            } else {
                SUPPORTS_AUTO_BACKLIGHT = false;
            }
        }

        return SUPPORTS_AUTO_BACKLIGHT;
    }

    private boolean isBrightnessSetToAutomatic() {
        try {
            IPowerManager ipm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            if (ipm != null) {
                int brightnessMode = Settings.System.getInt(
                        sContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
                return brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            }
        } catch (Exception e) {
        }

        return false;
    }

    private int getCurrentCMMode() {
        return Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.EXPANDED_BRIGHTNESS_MODE, CM_MODE_AUTO_MIN_DEF_MAX);
    }
}
