package com.google.cloud.android.speech.RecordList;

import android.databinding.BindingAdapter;
import android.widget.TextView;

import com.google.cloud.android.speech.RealmData.RecordRealm;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by samsung on 2017-10-07.
 */

public class RecordDTO {
    String title="";
    int duration=0;
    ArrayList<String> tagList=new ArrayList<>();

    public RecordDTO(String title, int duration, ArrayList<String> tagList) {
        this.title = title;
        this.duration = duration;
        this.tagList = tagList;
    }

    public RecordDTO(RecordRealm recordRealm) {
        this.title = recordRealm.getTitle();
        this.duration = recordRealm.getDuration();
        for(RealmString s:recordRealm.getTagList()){
            this.tagList.add(s.getString());
        }

    }

    public static RecordRealm toRealm(RecordDTO recordDTO) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RecordRealm recordRealm = realm.createObject(RecordRealm.class);
        recordRealm.setTitle(recordDTO.getTitle());
        recordRealm.setDuration(recordDTO.getDuration());
        recordRealm.setTagList(recordDTO.getTagList());
        realm.commitTransaction();
        return recordRealm;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public ArrayList<String> getTagList() {
        return tagList;
    }

    public void setTagList(ArrayList<String> tagList) {
        this.tagList = tagList;
    }

    @BindingAdapter("android:text")
    public static void setText(TextView view, int value) {
        view.setText(Integer.toString(value));
    }


}
