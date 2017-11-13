package com.google.cloud.android.speech.data.realm;

import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by USER on 2017-11-09.
 */

public class ClusterDataRealm extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    int id;

    RealmList<IntegerRealm> clusters = new RealmList<>();

    @Override
    public int getId() {
        return id;
    }


    @Override
    public void setId(int id) {
        this.id = id;

    }


    public RealmList<IntegerRealm> getClusters() {
        return clusters;
    }

    public int[] getClustersArray() {
        int [] array = new int[clusters.size()];
        for(int i=0;i<clusters.size();i++){
            array[i]=clusters.get(i).get();
        }
        return array;
    }

    public void setClusters(RealmList<IntegerRealm> clusters) {
        this.clusters = clusters;
    }

    public void setClusters(int[] clusters) {
        this.clusters.clear();
        for (int i : clusters) {
            IntegerRealm intt = new IntegerRealm();
            intt.set(i);
            this.clusters.add(intt);
        }
    }


}
