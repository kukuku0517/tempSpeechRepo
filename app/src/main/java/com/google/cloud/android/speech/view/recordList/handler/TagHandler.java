package com.google.cloud.android.speech.view.recordList.handler;

import android.view.View;

import com.google.cloud.android.speech.data.realm.TagRealm;

/**
 * Created by USER on 2017-11-15.
 */

public interface TagHandler {
    void onClickTag(View v, TagRealm tag);

}
