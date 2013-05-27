package android.phone;

import android.media.MediaRecorder;
import android.util.Log;
import android.os.Environment;
import android.content.Context;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.DataOutputStream;
import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.ref.WeakReference;
import android.os.SystemProperties;

/**
 * @author: Woody Guo <zjguo@lewatek.com>, zhangyawei <ywzhang@lewatek.com>
 */
public final class CallRecorder
{
    public static interface CRMessageReceiver
    {
        abstract public void onMessage(int message); 
    }
    
    private final static boolean DBG = true;
    private final static String TAG = "CallRecorder";

    public static final int FORMAT_WAV = 0;
    public static final int FORMAT_MPEG3 = 1;
    //public static final int FORMAT_3GP = 2;
    //public static final int FORMAT_MPEG4 = 3;
    
    public static final int MSG_SUCCESS = 0;
    public static final int MSG_UNKNOWN_ERROR = -1;
    public static final int MSG_NO_STORAGE_SPACE = -2;
    public static final int MSG_NO_EXTERNAL_STORAGE = -3;

    private static final String[] RECORD_SUFFIXES;
    private static final DateFormat SDF;

    private static AtomicBoolean sIsRecording;
    private static AtomicBoolean sManualControl;
    private static MediaRecorder sRecorder;
    //private static String sFolderName;
    private static boolean sUsingMediaRecorder;
    private static int sRecordingFormat;
    private static CRMessageReceiver sMsgReceiver;
    private static WeakReference<Context> sContext;
    

    static {
        RECORD_SUFFIXES = new String[] {"wav", "mp3", "3gp", "mp4"};
        SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
        sIsRecording = new AtomicBoolean(false);
        sManualControl = new AtomicBoolean(false);

        sRecordingFormat = FORMAT_MPEG3; 
        sUsingMediaRecorder = !checkSetDevicePermissions();
    }
    
    private static MediaRecorder.OnErrorListener sOnErrorListener = new MediaRecorder.OnErrorListener() {
        public void onError(MediaRecorder mr, int what, int extra) {
            Log.d(TAG, "message from MediaRecorder: " + what);
            sIsRecording.set(false);
            if(sMsgReceiver != null) {
                sMsgReceiver.onMessage(MSG_UNKNOWN_ERROR);
            }
        }
    };
    
    private static MediaRecorder.OnInfoListener sOnInfoListener = new MediaRecorder.OnInfoListener() {
        public void onInfo(MediaRecorder mr, int what, int extra) {
            Log.d(TAG, "message from MediaRecorder: " + what);
            sIsRecording.set(false);
            if(sMsgReceiver != null) {
                sMsgReceiver.onMessage(MSG_UNKNOWN_ERROR);
            }
        }
    };

    /*
     * register a message receiver
     */
    public static void registerMessageReceiver(Context ctx, CRMessageReceiver receiver) {
        Log.d(TAG, "register message receiver");
        sContext = new WeakReference<Context>(ctx);
        sMsgReceiver = receiver;
    }
    /*
     * boostup: volume gain for outgoing voice
     * boostdown: volume gain for incoming voice
     */
    public static void startRecording(final String filePath, int encoding_format, int boostup, int boostdown) {
        if (sIsRecording.get()) {
            if (DBG) Log.d(TAG, "recording in progress ...");
            return;
        }
    
        if (!isExternalStorageWritable()) {
            if(sMsgReceiver != null) {
                sMsgReceiver.onMessage(MSG_NO_EXTERNAL_STORAGE);
            }
            Log.e(TAG, "sd card not available");
            return;
        }

        sIsRecording.set(true);
        File containgFolder = new File(filePath).getParentFile();
        if(!containgFolder.exists()) {
            containgFolder.mkdirs();
        }

        if (sUsingMediaRecorder) {
            if (DBG) Log.d(TAG, "starting recording using MIC ...");
            if(sRecorder == null) {
                sRecorder = new MediaRecorder();
            }
            
            try {
                if("TD".equals(SystemProperties.get("ro.product.network"))) {
                    sRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                }
                else {
                    sRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                }
                sRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                sRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                int dot = filePath.lastIndexOf('.');
                String filePathNew = filePath;
                if(dot != -1) {
                    filePathNew = filePath.substring(0, dot);
                }
                filePathNew = filePathNew.concat(".amr");
                
                if (DBG) Log.d(TAG, "set output file to " + filePathNew);
                sRecorder.setOutputFile(filePathNew);
                sRecorder.setOnInfoListener(sOnInfoListener);
                sRecorder.setOnErrorListener(sOnErrorListener);
                sRecorder.prepare();
                sRecorder.start();
            } catch(Exception e) {
                sRecorder.reset();
                sRecorder = null;
                if(sMsgReceiver != null) {
                    sMsgReceiver.onMessage(MSG_UNKNOWN_ERROR);
                }
                sIsRecording.set(false);
                if (DBG) Log.d(TAG, "starting recording failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (DBG) Log.d(TAG, "starting recording using vocpcm ...");
            
            if (encoding_format > FORMAT_MPEG3) {
                encoding_format = FORMAT_MPEG3;
            }
            sRecordingFormat = encoding_format;
            
            startRecord(filePath, encoding_format, boostup, boostdown);
        }
    }
    
    /* return true if inner recording supported, otherwise return false */
    public static boolean isInnerRecordingSupported() { return !sUsingMediaRecorder; }
    
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } 
        else
            return false;
    }



    /*
     * start recording for phonecall voice talking
     * filePath: full path file name for the recorded audio to save, the containing folder will be created if necessary
     * encoding_format: audio encoding format of file, can be FORMAT_WAV or FORMAT_MPEG3,
     * if inner recording is not supported, amr format will be used
     */
    public static void startRecording(final String filePath, int encoding_format) {
        startRecording(filePath, encoding_format, 4, 4); // add bootup value of outgoing voice
    }

    /*
     * stop recording and store recorded audio into file
     * must be called one time at least to avoid possible memory leak
     */
    public static void stopRecording() {
        if (sIsRecording.get()) {
            sIsRecording.set(false);
            if (DBG) Log.d(TAG, "stopping recording ...");
            if (sUsingMediaRecorder) {
                if(sRecorder != null) {
                    try {
                        sRecorder.stop();
                        sRecorder.reset();
                        if(sMsgReceiver != null) {
                            Log.d(TAG, "send message to app");
                            if(sMsgReceiver != null) sMsgReceiver.onMessage(MSG_SUCCESS);
                        }
                    } catch(Exception e) {
                        if(sMsgReceiver != null) sMsgReceiver.onMessage(MSG_UNKNOWN_ERROR);
                        sRecorder.reset();
                        e.printStackTrace();
                    }
                    sRecorder = null;
                }
            } else {
                stopRecord();
            }
        } else {
            if (DBG) Log.d(TAG, "I'm not recording anything, why asked me to stop recording ...");
        }
    }

    public static boolean isRecording() {
        return sIsRecording.get();
    }

    public static boolean isManualControl() {
        return sManualControl.get();
    }

    public static void setManualControl() {
        sManualControl.compareAndSet(false, true);
    }

    public static void resetManualControl() {
        sManualControl.compareAndSet(true, false);
    }

    /*
     * public static void releaseRecorder() {
     *     if (!sIsRecording) {
     *         sRecorder.release();
     *     }
     * }
     */

    protected void finalize() throws Throwable {
        if (DBG) Log.d(TAG, "releasing recorder resources...");
        try {
            sRecorder.release();
        } finally {
            sRecorder = null;
            super.finalize();
        }
    }

    private static final boolean checkSetDevicePermissions() {
        java.lang.Process process = null;
        DataOutputStream os = null;
        File f1 = new File("/dev/voc_tx_record");
        try {
            if(f1.exists()) {    
                if(f1.canRead() && f1.canWrite()) return true;
            } else
                return false;

            // make device file readable and writable
            process = Runtime.getRuntime().exec("su0");
            os = new DataOutputStream(process.getOutputStream());
            os.flush();
            os.writeBytes("chmod 0666 /dev/voc_tx_record\n"); os.flush();
            os.writeBytes("chmod 0666 /dev/voc_rx_record\n"); os.flush();
            os.writeBytes("chmod 0666 /dev/voc_tx_playback\n"); os.flush();
            os.writeBytes("exit\n"); os.flush();
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "exception in checkSetDevicePermissions()");
            e.printStackTrace();
            return false;
        } finally {
            try {
                if(os != null) os.close();
                if(process != null) process.destroy();
            } catch (Exception e) { 
                Log.e(TAG, "exception when exiting checkSetDevicePermissions()");
                e.printStackTrace();
            }
        }

        // check readablity and writability again
        if (f1.exists() && f1.canRead() && f1.canWrite()) {
            return true;
        } else {
            return false;
        }

    }

    public static void onMessage(int message) {
        sIsRecording.set(false);
        Log.d(TAG, "message from runtime: " + message);
        if(sMsgReceiver != null) {
            try {
                sMsgReceiver.onMessage(message);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private static native int startRecord(String file, int encoding_mode, int bu, int bd);
    private static native void stopRecord();
    //private static native void answerCall(String file, String ofile, int bd);
}

