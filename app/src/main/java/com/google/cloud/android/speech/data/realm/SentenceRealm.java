package com.google.cloud.android.speech.data.realm;

import android.databinding.BindingAdapter;
import android.widget.TextView;

import com.google.cloud.android.speech.util.DateUtil;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by samsung on 2017-10-07.
 */

public class SentenceRealm extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    private int id;
    private RealmList<WordRealm> wordList;
    private long startMillis;
    private long endMillis;
    private String sentence;
    ClusterRealm cluster;

    public   ClusterRealm getCluster() {
        return cluster;
    }

    public void setCluster( ClusterRealm cluster) {
        this.cluster = cluster;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public void setSentence() {
        StringBuilder builder = new StringBuilder();
        for (WordRealm wordRealm : wordList) {
            builder.append(wordRealm.getWord() + " ");
        }
        this.sentence = builder.toString();

    }

    public RealmList<WordRealm> getWordList() {
        return wordList;
    }

    public void setWordList(RealmList<WordRealm> wordList) {
        this.wordList = wordList;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }


    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
//
//    @BindingAdapter("android:text")
//    public static void setText(TextView v, int value) {
//        v.setText(DateUtil.durationToTextFormat(value));
//    }


}
