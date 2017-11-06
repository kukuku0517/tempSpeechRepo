package com.google.cloud.android.speech.data.DTO;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.TextView;

import com.google.cloud.android.speech.BR;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.StringRealm;
import com.google.cloud.android.speech.util.DateUtil;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by samsung on 2017-10-07.
 */

public class RecordDTO extends BaseObservable {
    private String title = "";
    private int duration = 0;
    private ArrayList<String> tagList = new ArrayList<>();
    private long startMillis = -1;
    private String filePath;

    public RecordDTO() {

    }

    public RecordDTO(RecordRealm recordRealm) {
        setRealm(recordRealm);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setRealm(RecordRealm recordRealm) {
        setTitle(recordRealm.getTitle());
        this.duration = recordRealm.getDuration();
        setStartMillis(recordRealm.getStartMillis());
        setFilePath(recordRealm.getFilePath());
        ArrayList<String> temp = new ArrayList<>();
        for (StringRealm s : recordRealm.getTagList()) {
            temp.add(s.getString());
        }
        setTagList(temp);
    }

    public static RecordRealm toRealm(RecordDTO recordDTO) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        RecordRealm recordRealm = realm.createObject(RecordRealm.class);
        recordRealm.setTitle(recordDTO.getTitle());
        recordRealm.setDuration(recordDTO.getDuration());
        recordRealm.setTagList(recordDTO.getTagList());
        recordRealm.setStartMillis(recordDTO.getStartMillis());
        recordRealm.setFilePath(recordDTO.getFilePath());
        realm.commitTransaction();
        return recordRealm;
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    @Bindable
    public int getDuration() {
        return duration;
    }

    @Bindable
    public ArrayList<String> getTagList() {
        return tagList;
    }

    @Bindable
    public long getStartMillis() {
        return startMillis;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyPropertyChanged(BR.title);
    }

    public void setDuration(int duration) {
        this.duration = duration;
        notifyPropertyChanged(BR.duration);
    }

    public void setTagList(ArrayList<String> tagList) {
        this.tagList = tagList;
        notifyPropertyChanged(BR.tagList);
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
        notifyPropertyChanged(BR.startMillis);
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
