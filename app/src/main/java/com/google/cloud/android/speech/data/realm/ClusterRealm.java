package com.google.cloud.android.speech.data.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by USER on 2017-11-10.
 */

public class ClusterRealm extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    int id;

    int clusterNo=0;

    public int getClusterNo() {
        return clusterNo;
    }

    public void setClusterNo(int clusterNo) {
        this.clusterNo = clusterNo;
    }

    String name;
    String photoPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
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
