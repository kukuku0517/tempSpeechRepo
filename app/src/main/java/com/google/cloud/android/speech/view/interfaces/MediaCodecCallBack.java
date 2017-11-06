package com.google.cloud.android.speech.view.interfaces;

/**
 * Created by USER on 2017-11-06.
 */

public interface MediaCodecCallBack {
    void onReset();
    void onBufferRead(byte[] buffer);
    void onCompleted();
}
