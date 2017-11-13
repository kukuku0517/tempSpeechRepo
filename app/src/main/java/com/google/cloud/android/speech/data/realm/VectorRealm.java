package com.google.cloud.android.speech.data.realm;

import com.google.cloud.android.speech.data.realm.primitive.DoubleRealm;
import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by USER on 2017-11-13.
 */

public class VectorRealm extends RealmObject{

    RealmList<DoubleRealm> featureVector = new RealmList<>();

    public RealmList<DoubleRealm> getFeatureVector() {
        return featureVector;
    }

    public void setFeatureVector(RealmList<DoubleRealm> featureVector) {
        this.featureVector = featureVector;
    }

    public void setFeatureVector(double[] featureVector) {
        for (double i : featureVector) {
            DoubleRealm iRealm = new DoubleRealm();
            iRealm.set(i);
            this.featureVector.add(iRealm);
        }
    }

}
