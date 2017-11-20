package com.google.cloud.android.speech.data.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by USER on 2017-11-15.
 */

public class TagRealm extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    int id;

    String name;
    int colorCode;
    int count=0;
    RealmList<RecordRealm> records = new RealmList<>();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColorCode() {
        return colorCode;
    }

    public void setColorCode(int colorCode) {
        this.colorCode = colorCode;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public RealmList<RecordRealm> getRecords() {
        return records;
    }

    public void setRecords(RealmList<RecordRealm> records) {
        this.records = records;
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
