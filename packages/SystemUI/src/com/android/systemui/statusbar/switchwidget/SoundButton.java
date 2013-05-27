package com.android.systemui.statusbar.switchwidget;

import com.android.systemui.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.os.Vibrator;
import android.provider.Settings;

public class SoundButton extends ReceiverButton
{
    private AudioManager mAudioManager;

    public static final int RINGER_MODE_UNKNOWN = 0;
    public static final int RINGER_MODE_SILENT = 1;
    public static final int RINGER_MODE_VIBRATE_ONLY = 2;
    public static final int RINGER_MODE_SOUND_ONLY = 3;
    public static final int RINGER_MODE_SOUND_AND_VIBRATE = 4;

    public static final int CM_MODE_SOUNDVIB_VIB = 0;
    public static final int CM_MODE_SOUND_VIB = 1;
    public static final int CM_MODE_SOUND_SILENT = 2;
    public static final int CM_MODE_SOUNDVIB_VIB_SILENT = 3;
    public static final int CM_MODE_SOUND_VIB_SILENT = 4;
    public static final int CM_MODE_SOUNDVIB_SOUND_VIB_SILENT = 5;
    
    public static AudioManager AUDIO_MANAGER = null;
    public static Vibrator VIBRATOR = null;

    /*
     * <string-array name="entries_ring_widget">
     *     <item>Sound+Vibrate/Vibrate</item>
     *     <item>Sound/Vibrate</item>
     *     <item>Sound/Silent</item>
     *     <item>Sound+Vibrate/Vibrate/Silent</item>
     *     <item>Sound/Vibrate/Silent</item>
     *     <item>Sound+Vibrate/Sound/Vibrate/Silent</item>
     * </string-array>
     */

    public SoundButton() {
        super();
        mType = BUTTON_SOUND;
        mAudioManager = (AudioManager)sContext.getSystemService(Context.AUDIO_SERVICE);
        mFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        mFilter.addAction(AudioManager.VIBRATE_SETTING_CHANGED_ACTION);
        mLabel=R.string.title_toggle_sound;
    }

    @Override
    protected void updateState() {
    	initServices(mView.getContext());
        int ringMode = AUDIO_MANAGER.getRingerMode();
        boolean silentOrVibrateMode = ringMode != AudioManager.RINGER_MODE_NORMAL;
        if (silentOrVibrateMode)
        {
            mIcon = R.drawable.stat_ring_off_1;
            mState = STATE_DISABLED;
        }
        else
        {
            mIcon = R.drawable.stat_ring_on_1;
            mState = STATE_ENABLED;
        }
	
/*        switch (getSoundState()) {
            case RINGER_MODE_SOUND_AND_VIBRATE:
                mIcon = R.drawable.switch_sv_sound_vibrate;
                mState = STATE_ENABLED;
                break;
            case RINGER_MODE_SOUND_ONLY:
                mIcon = R.drawable.switch_sv_sound;
                mState = STATE_ENABLED;
                break;
            case RINGER_MODE_VIBRATE_ONLY:
                mIcon = R.drawable.switch_sv_vibrate;
                mState = STATE_DISABLED;
                break;
            case RINGER_MODE_SILENT:
                mIcon = R.drawable.switch_sv_silent;
                mState = STATE_DISABLED;
                break;
        }*/
    }
    
    private static void initServices(Context context) {
        if(AUDIO_MANAGER == null) {
            AUDIO_MANAGER = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        if(VIBRATOR == null) {
            VIBRATOR = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    @Override
    protected void toggleState() {

	 Context context = mView.getContext();
        int currentState = getSoundState(context);

        // services should be initialized in the last call, but we do this for completeness anyway
        initServices(context);

	 int ringMode = AUDIO_MANAGER.getRingerMode();
        boolean silentOrVibrateMode = ringMode != AudioManager.RINGER_MODE_NORMAL;
        if (!silentOrVibrateMode)
        {
            boolean vibeInSilent = (1 == Settings.System.getInt(context.getContentResolver(), Settings.System.VIBRATE_IN_SILENT, 1));                
            AUDIO_MANAGER.setRingerMode( vibeInSilent ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_SILENT);
        }
        else
        {
            AUDIO_MANAGER.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

	 // code from android4.0 ,forbidden
        /*switch (getSoundState()) {
            // order of check: soundvib sound vib silent
            case RINGER_MODE_SOUND_AND_VIBRATE:
                if (supports(RINGER_MODE_SOUND_ONLY)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ONLY_SILENT
                            , AudioManager.RINGER_MODE_NORMAL);
                } else if (supports(RINGER_MODE_VIBRATE_ONLY)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_VIBRATE);
                } else if (supports(RINGER_MODE_SILENT)) {
                    updateSettings(0, AudioManager.VIBRATE_SETTING_OFF
                            , AudioManager.RINGER_MODE_SILENT);
                } else { // Fall Back
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_VIBRATE);
                }
                break;

            case RINGER_MODE_SOUND_ONLY:
                if (supports(RINGER_MODE_VIBRATE_ONLY)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ONLY_SILENT
                            , AudioManager.RINGER_MODE_VIBRATE);
                } else if (supports(RINGER_MODE_SILENT)) {
                    updateSettings(0, AudioManager.VIBRATE_SETTING_OFF
                            , AudioManager.RINGER_MODE_SILENT);
                } else if (supports(RINGER_MODE_SOUND_AND_VIBRATE)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_NORMAL);
                } else { // Fall back
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_VIBRATE);
                }
                break;

            case RINGER_MODE_VIBRATE_ONLY:
                if (supports(RINGER_MODE_SILENT)) {
                    updateSettings(0, AudioManager.VIBRATE_SETTING_OFF
                            , AudioManager.RINGER_MODE_SILENT);
                } else if (supports(RINGER_MODE_SOUND_AND_VIBRATE)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_NORMAL);
                } else if (supports(RINGER_MODE_SOUND_ONLY)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ONLY_SILENT
                            , AudioManager.RINGER_MODE_NORMAL);
                } else { // Fall Back
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_NORMAL);
                }
                break;

            case RINGER_MODE_SILENT:
                if (supports(RINGER_MODE_SOUND_AND_VIBRATE)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_NORMAL);
                } else if (supports(RINGER_MODE_SOUND_ONLY)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ONLY_SILENT
                            , AudioManager.RINGER_MODE_NORMAL);
                } else if (supports(RINGER_MODE_VIBRATE_ONLY)) {
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ONLY_SILENT
                            , AudioManager.RINGER_MODE_VIBRATE);
                } else { // Fall Back
                    updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                            , AudioManager.RINGER_MODE_NORMAL);
                }
                break;
            default:
                updateSettings(1, AudioManager.VIBRATE_SETTING_ON
                        , AudioManager.RINGER_MODE_NORMAL);
        }*/
    }

    @Override
    protected boolean onLongClick() {
        startActivity("android.settings.SOUND_SETTINGS");
        return false;
    }

    private boolean supports(int ringerMode) {
        // Returns true if ringerMode is one of the modes
        // in the selected sound option for the widget
        int currentMode = getCurrentCMMode();

        switch (ringerMode) {
            case RINGER_MODE_SILENT:
                if (currentMode == CM_MODE_SOUND_SILENT
                        || currentMode == CM_MODE_SOUNDVIB_VIB_SILENT
                        || currentMode == CM_MODE_SOUND_VIB_SILENT
                        || currentMode == CM_MODE_SOUNDVIB_SOUND_VIB_SILENT)
                    return true;
                break;
            case RINGER_MODE_VIBRATE_ONLY:
                if (currentMode == CM_MODE_SOUND_VIB
                        || currentMode == CM_MODE_SOUNDVIB_VIB
                        || currentMode == CM_MODE_SOUNDVIB_VIB_SILENT
                        || currentMode == CM_MODE_SOUND_VIB_SILENT
                        || currentMode == CM_MODE_SOUNDVIB_SOUND_VIB_SILENT)
                    return true;
                break;
            case RINGER_MODE_SOUND_ONLY:
                if (currentMode == CM_MODE_SOUND_VIB
                        || currentMode == CM_MODE_SOUND_SILENT
                        || currentMode == CM_MODE_SOUND_VIB_SILENT
                        || currentMode == CM_MODE_SOUNDVIB_SOUND_VIB_SILENT)
                    return true;
                break;
            case RINGER_MODE_SOUND_AND_VIBRATE:
                if (currentMode == CM_MODE_SOUNDVIB_VIB
                        || currentMode == CM_MODE_SOUNDVIB_VIB_SILENT
                        || currentMode == CM_MODE_SOUNDVIB_SOUND_VIB_SILENT)
                    return true;
        }

        return false;
    }

     // code from android4.0 ,forbidden
    /*private int getSoundState() {
        int ringMode = mAudioManager.getRingerMode();
        int vibrateMode = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);

        if (ringMode == AudioManager.RINGER_MODE_NORMAL
                && vibrateMode == AudioManager.VIBRATE_SETTING_ON) {
            return RINGER_MODE_SOUND_AND_VIBRATE;
        } else if (ringMode == AudioManager.RINGER_MODE_NORMAL) {
            return RINGER_MODE_SOUND_ONLY;
        } else if (ringMode == AudioManager.RINGER_MODE_VIBRATE) {
            return RINGER_MODE_VIBRATE_ONLY;
        } else if (ringMode == AudioManager.RINGER_MODE_SILENT) {
            return RINGER_MODE_SILENT;
        } else {
            return RINGER_MODE_UNKNOWN;
        }
    }*/

      private static int getSoundState(Context context) {
        // ensure our services are initialized
        initServices(context);

        int ringMode = AUDIO_MANAGER.getRingerMode();
        
        if (ringMode == AudioManager.RINGER_MODE_NORMAL) {
            return RINGER_MODE_SOUND_ONLY;
        } else if (ringMode == AudioManager.RINGER_MODE_VIBRATE) {
            return RINGER_MODE_VIBRATE_ONLY;
        } else if (ringMode == AudioManager.RINGER_MODE_SILENT) {
            return RINGER_MODE_SILENT;
        } else {
            return RINGER_MODE_UNKNOWN;
        }
     
    }

    private int getCurrentCMMode() {
        return Settings.System.getInt(sContext.getContentResolver()
                , Settings.System.EXPANDED_RING_MODE, CM_MODE_SOUNDVIB_SOUND_VIB_SILENT);
    }

    private void updateSettings(final int vibrateInSilence
            , final int amVibrateSetting, final int amRingerMode) {
        Settings.System.putInt(sContext.getContentResolver()
                , Settings.System.VIBRATE_IN_SILENT, vibrateInSilence);

        mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, amVibrateSetting);
        mAudioManager.setRingerMode(amRingerMode);
    }
}
