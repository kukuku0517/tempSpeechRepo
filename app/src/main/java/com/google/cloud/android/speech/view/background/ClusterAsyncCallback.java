package com.google.cloud.android.speech.view.background;

/**
 * Created by USER on 2017-11-17.
 */

public interface ClusterAsyncCallback {
    void onProgress(float progress);

    void onPostExecute(int value);
}
