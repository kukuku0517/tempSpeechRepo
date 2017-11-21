package com.google.cloud.android.speech.view.recordResult.handler;

import android.view.View;

/**
 * Created by USER on 2017-10-12.
 */

public interface ResultHandler {
    void onClickStart(View v);
    void onClickStop(View v) ;
    void onClickBack(View v);
    void onClickForward(View v);
    void onClickLoop(View v);
    void onClickDiary(View v);
    void onClickDelete(View v);
    void onClickTitle(View v);
    void onClickViewOrigin(View v);
    void onClickViewDiary(View v);
    void onFabClick(View v);
}
