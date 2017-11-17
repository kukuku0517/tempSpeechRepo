package com.google.cloud.android.speech.view.background;

import android.os.AsyncTask;
import android.widget.Toast;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.FeatureRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.diarization.KMeansCluster;
import com.google.cloud.android.speech.util.LogUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.recordResult.handler.SpeakerDiaryClickListener;

import java.io.IOException;

import io.realm.Realm;

/**
 * Created by USER on 2017-11-17.
 */

public class ClusterAsync extends AsyncTask<Integer, Float, Integer> {

    private int originRecordId;
    private int duplicateRecordId;
    private static final int DIMENSION = 21;
    private ClusterAsyncCallback callback;

    public ClusterAsync(int originRecordId,  ClusterAsyncCallback callback) {
        this.originRecordId = originRecordId;
        this.callback = callback;
    }

    @Override
    protected Integer doInBackground(final Integer... params) {
        int[] results;
        Realm realm = Realm.getDefaultInstance();
        RecordRealm origin = realm.where(RecordRealm.class).equalTo("id", originRecordId).findFirst();
        duplicateRecordId= origin.getDuplicateId();
        FeatureRealm feature = realm.where(FeatureRealm.class).equalTo("id", originRecordId).findFirst();

        int[] sil = new int[feature.getSilence().size()];
        for (int i = 0; i < feature.getSilence().size(); i++) {
            sil[i] = feature.getSilence().get(i).get();
        }

        KMeansCluster cluster = new KMeansCluster(3, DIMENSION, feature.getFeatureVectors(), feature.getSilence());
        cluster.setListener(new SpeakerDiaryClickListener() {
            @Override
            public void onSpeakerDiaryComplete(int process) {
                publishProgress(process / (float) params[0]);
            }
        });

        try {
            results = cluster.iterRun(params[0]);
            realm.beginTransaction();
            if (duplicateRecordId != -1) {
                RecordRealm dupToDelete = realm.where(RecordRealm.class).equalTo("id", duplicateRecordId).findFirst();
                dupToDelete.cascadeDelete();
            }
            duplicateRecordId = RealmUtil.duplicateRecord(realm, originRecordId);
            realm.commitTransaction();
            cluster.applyClusterToRealm(3, results, duplicateRecordId, 0.01f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return duplicateRecordId;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        super.onProgressUpdate(values);
        callback.onProgress(values[0]);
//        progress.setValue(String.format("Loading... %.0f%%", values[0] * 100));

    }

    @Override
    protected void onPostExecute(Integer dupId) {
//        diary.setValue(false);
//        Toast.makeText(getBaseContext(), R.string.speaker_complete, Toast.LENGTH_SHORT).show();
//        Realm realm = Realm.getDefaultInstance();
//        swapRecord(aVoid);
//        hasDiary.setValue(duplicateRecordId);
//        realm.beginTransaction();
//        record.setCluster(results);
//        realm.commitTransaction();
        callback.onPostExecute(dupId);
    }
}