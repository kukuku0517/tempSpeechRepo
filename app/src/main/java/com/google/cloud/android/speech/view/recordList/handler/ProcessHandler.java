package com.google.cloud.android.speech.view.recordList.handler;

import android.view.View;

/**
 * Created by USER on 2017-10-19.
 */

public interface ProcessHandler {
    void onClickStopRecord(View view);

    void onClickStopFile(View view);

    void onClickMode(View v, int id);

    void onClickSamplerate(View v, int rate);

    void onClickSpeaker(View v);

    void onClickStartRecognition(View view);

    void onClickTagAdd(View v);

    void onClickMakeDir(View view);

}
