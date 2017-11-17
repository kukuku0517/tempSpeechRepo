package com.google.cloud.android.speech.data.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by USER on 2017-11-18.
 */

public class DirectoryRealm extends RealmObject implements PrimaryRealm {
    @PrimaryKey
    int id;

    private RealmList<DirectoryRealm> directoryRealms = new RealmList<>();
    private int upperId = -1;
    private int depth;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private RealmList<RecordRealm> recordRealms = new RealmList<>();

    public RealmList<DirectoryRealm> getDirectoryRealms() {
        return directoryRealms;
    }

    public void setDirectoryRealms(RealmList<DirectoryRealm> directoryRealms) {
        this.directoryRealms = directoryRealms;
    }

    public int getUpperId() {
        return upperId;
    }

    public void setUpperId(int upperId) {
        this.upperId = upperId;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public RealmList<RecordRealm> getRecordRealms() {
        return recordRealms;
    }

    public void setRecordRealms(RealmList<RecordRealm> recordRealms) {
        this.recordRealms = recordRealms;
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
