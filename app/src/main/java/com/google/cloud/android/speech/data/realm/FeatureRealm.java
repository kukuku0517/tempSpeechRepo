package com.google.cloud.android.speech.data.realm;

import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;

import java.util.Vector;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by USER on 2017-11-13.
 */

public class FeatureRealm extends RealmObject implements PrimaryRealm {
    @PrimaryKey
    int id;

    RealmList<VectorRealm> featureVectors = new RealmList<>();
    RealmList<IntegerRealm> silence = new RealmList<>();

    public RealmList<IntegerRealm> getSilence() {
        return silence;
    }

    public void setSilence(RealmList<IntegerRealm> silence) {
        this.silence = silence;
    }

    public void setSilence(int[] silence) {
        for(int i:silence){
            IntegerRealm integerRealm = new IntegerRealm();
            integerRealm.set(i);
            this.silence.add(integerRealm);
        }
    }

    public RealmList<VectorRealm> getFeatureVectors() {
        return featureVectors;
    }

    public void setFeatureVectors(RealmList<VectorRealm> featureVectors) {
        this.featureVectors = featureVectors;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
}
