package com.google.cloud.android.speech.view.background;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.cloud.android.speech.event.PartialStatusEvent;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.view.interfaces.VoiceRecorderCallBack;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by samsung on 2017-10-15.
 */

public class VoiceRecorder {


    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 22050, 44100};
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BPP = 16;
    private static final int AMPLITUDE_THRESHOLD = 1500;
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;
    private static String TITLE = "";
    private FileOutputStream os;
    private static String TAG = "kjh";
    private int recorderSampleRate = 0;
    private int bufferSize = 0;

    private final VoiceRecorderCallBack mCallback;

    private AudioRecord mAudioRecord;

    private Thread mThread;

    private byte[] mBuffer;

    private final Object mLock = new Object();

    /**
     * The timestamp of the last time that voice is heard.
     */
    private long mLastVoiceHeardMillis = Long.MAX_VALUE;

    /**
     * The timestamp when the current voice is started.
     */
    private long mVoiceStartedMillis;
    private boolean isRecording = false;

    public VoiceRecorder(@NonNull VoiceRecorderCallBack callback, int sampleRate) {
        mCallback = callback;
        this.recorderSampleRate = sampleRate;
    }

    public void setTitle(String title) {
        TITLE = title;
    }

    /**
     * Starts recording audio.
     * <p>
     * <p>The caller is responsible for calling {@link #stop()} later.</p>
     */
    public void start() {
        // Stop recording if it is currently ongoing.
        stop();

        try {
            os = new FileOutputStream(FileUtil.getTempFilename());
            isRecording = true;
            Log.d(TAG, "open filestream");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Try to create a new recording session.
        mAudioRecord = createAudioRecord();

        if (mAudioRecord == null) {
            throw new RuntimeException("Cannot instantiate VoiceRecorder");
        }
        // Start recording.
        mAudioRecord.startRecording();
        // Start processing the captured audio.
        mThread = new Thread(new ProcessVoice());
        mThread.start();
    }

    /**
     * Stops recording audio.
     */

    public void stop() {

        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }


    }


    /**
     * Dismisses the currently ongoing utterance.
     */
    public void dismiss() {
        if (mLastVoiceHeardMillis != Long.MAX_VALUE) {

            mCallback.onVoiceEnd();
            mLastVoiceHeardMillis = Long.MAX_VALUE;
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    public int getSampleRate() {
        if (mAudioRecord != null) {
            return mAudioRecord.getSampleRate();
        }
        return 0;
    }

    /**
     * Creates a new {@link AudioRecord}.
     *
     * @return A newly created {@link AudioRecord}, or null if it cannot be created (missing
     * permissions?).
     */


    private AudioRecord createAudioRecord() {

        final int sizeInBytes = AudioRecord.getMinBufferSize(recorderSampleRate, CHANNEL, ENCODING);
        bufferSize = sizeInBytes;
        final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                recorderSampleRate, CHANNEL, ENCODING, sizeInBytes);

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            mBuffer = new byte[sizeInBytes];
            return audioRecord;
        } else {
            audioRecord.release();
            return null;
        }


    }

    /**
     * Continuously processes the captured audio and notifies {@link #mCallback} of corresponding
     * events.
     */

    double countSize = 0;
    double sendSize = 0;

    int runsize = 0;

    private class ProcessVoice implements Runnable {

        @Override
        public void run() {
            while (true) {
                synchronized (mLock) {
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d("isRecording","interrupt break;");

                        if (mAudioRecord != null) {
                            mAudioRecord.stop();
                            mAudioRecord.release();
                            mAudioRecord = null;
                        }
                        mBuffer = null;
                        if (isRecording) {
                            isRecording = false;
                            Log.d("isRecording","falseeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
                            try {
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            FileUtil.copyWaveFile(FileUtil.getTempFilename(), FileUtil.getFilename(TITLE), recorderSampleRate, RECORDER_BPP, bufferSize, 1);
                            deleteTempFile();
                            dismiss();
                            mCallback.onConvertEnd();
                        }
                        break;
                    }
                    if(isRecording==false) {
                        Log.d("isRecording", "false break;");
                        break;
                    }else{
                        Log.d("isRecording","true");}

                    int size = 0;
                    if (mAudioRecord != null) {
                        size = mAudioRecord.read(mBuffer, 0, bufferSize);
                        try {
                            os.write(mBuffer);
                            countSize += mBuffer.length;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    final long now = System.currentTimeMillis();

                    if (isHearingVoice(mBuffer, size)) {
                        EventBus.getDefault().postSticky(new PartialStatusEvent(PartialStatusEvent.VOICE));
                        if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                            mVoiceStartedMillis = now;
                            try {
                                mCallback.onVoiceStart(mVoiceStartedMillis);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        mCallback.onVoice(mBuffer, size, true);
                        sendSize += mBuffer.length;
                        mLastVoiceHeardMillis = now;
                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) { //인식중 + 최대시간 초과
                            end();
                        }
                    } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                        EventBus.getDefault().postSticky(new PartialStatusEvent(PartialStatusEvent.SILENCE));
                        mCallback.onVoice(mBuffer, size, true);
                        sendSize += mBuffer.length;
                        if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) { //인식정지 시간초과
                            end();
                        }
                    } else {
                        mCallback.onVoice(mBuffer, size, false);
                        sendSize += mBuffer.length;
                    }
                }
            }
        }

        private void end() {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }

        private boolean isHearingVoice(byte[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }

    }

//    private String getFilename() {
//        String filepath = Environment.getExternalStorageDirectory().getPath();
//
//
//        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
//
//        if (!file.exists()) {
//            file.mkdirs();
//        }
//        String fileName = (TITLE==null|TITLE.equals(""))?String.valueOf(System.currentTimeMillis()):TITLE;
//        return (file.getAbsolutePath() + "/" + fileName +
//                AUDIO_RECORDER_FILE_EXT_WAV);
//    }
//
//    private String getTempFilename() {
//        String filepath = Environment.getExternalStorageDirectory().getPath();
//
//
//        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
//
//        if (!file.exists()) {
//            file.mkdirs();
//        }
//
//        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);
//
//        if (tempFile.exists())
//            tempFile.delete();
//
//        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
//    }

    private void deleteTempFile() {
        File file = new File(FileUtil.getTempFilename());
        file.delete();
    }

}
