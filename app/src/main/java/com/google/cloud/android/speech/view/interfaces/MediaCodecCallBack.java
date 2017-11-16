package com.google.cloud.android.speech.view.interfaces;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by USER on 2017-11-06.
 */

public interface MediaCodecCallBack {
    void onStart(int bufferSize,int sampleRate);
    void onBufferRead(byte[] buffer,int sampleRate) throws IOException;
    void onCompleted() throws IOException;
}
