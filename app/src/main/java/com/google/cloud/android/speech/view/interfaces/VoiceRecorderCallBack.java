package com.google.cloud.android.speech.view.interfaces;

import android.media.AudioFormat;

/**
 * Created by USER on 2017-11-06.
 */
public interface VoiceRecorderCallBack {

    /**
     * Called when the recorder starts hearing voice.
     */
    void onVoiceStart(long millis);

    /**
     * Called when the recorder is hearing voice.
     *
     * @param data The audio data in {@link AudioFormat#ENCODING_PCM_16BIT}.
     * @param size The size of the actual data in {@code data}.
     */
    void onVoice(byte[] data, int size);

    /**
     * Called when the recorder stops hearing voice.
     */
    void onVoiceEnd();

    void onConvertEnd();

}