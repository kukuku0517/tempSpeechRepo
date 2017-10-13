package com.google.cloud.android.speech.Data.DTO;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.widget.TextView;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.Data.Realm.StringRealm;
import com.google.cloud.android.speech.Util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;

/**
 * Created by samsung on 2017-10-07.
 */

public class RecordDTO {
    private String title = "";
    private int duration = 0;
    private ArrayList<String> tagList = new ArrayList<>();
    private long startMillis = -1;


    public RecordDTO(RecordRealm recordRealm) {
        this.title = recordRealm.getTitle();
        this.duration = recordRealm.getDuration();
        this.startMillis = recordRealm.getStartMillis();
        for (StringRealm s : recordRealm.getTagList()) {
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
        recordRealm.setStartMillis(recordDTO.getStartMillis());
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

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    @BindingAdapter("android:text")
    public static void setText(TextView view, int value) {
        String timeString = DateUtil.durationToDate(value);
        view.setText(timeString);
    }

    @BindingAdapter("android:text")
    public static void setText(TextView view, long value) {
        String timeString = DateUtil.millisToDate(value);
        view.setText(timeString);
    }


}
