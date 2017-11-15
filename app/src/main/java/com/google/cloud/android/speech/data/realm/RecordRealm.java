package com.google.cloud.android.speech.data.realm;

import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;
import com.google.cloud.android.speech.data.realm.primitive.StringRealm;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by samsung on 2017-10-07.
 */

public class RecordRealm extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    private int id;
    private String title = "";
    private String filePath;

    private int duration = 0;
    private long startMillis = -1;
    private boolean converted = false;

    private RealmList<TagRealm> tagList = new RealmList<>();
    private RealmList<SentenceRealm> sentenceList = new RealmList<>();
    private RealmList<ClusterRealm> clusterMembers = new RealmList<>();
    private RealmList<IntegerRealm> cluster = new RealmList<>();

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public RealmList<IntegerRealm> getCluster() {
        return cluster;
    }

    public void setCluster(RealmList<IntegerRealm> cluster) {
        this.cluster = cluster;
    }


    public void setCluster(int[] cluster) {
        for(int i:cluster){
            IntegerRealm integerRealm = new IntegerRealm();
            integerRealm.set(i);
            this.cluster.add(integerRealm);
        }
    }

    public RealmList<ClusterRealm> getClusterMembers() {
        return clusterMembers;
    }

    public void setClusterMembers(RealmList<ClusterRealm> clusterMembers) {
        this.clusterMembers = clusterMembers;
    }


    public boolean isConverted() {
        return converted;

    }

    public void setConverted(boolean converted) {
        this.converted = converted;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public boolean hasCluster(int i) {
        for (ClusterRealm c : clusterMembers) {
            if (c.getClusterNo() == i) return true;
        }
        return false;

    }

    public RealmList<SentenceRealm> getSentenceRealms() {
        return sentenceList;
    }

    public void setSentenceRealms(RealmList<SentenceRealm> sentenceRealms) {
        this.sentenceList = sentenceRealms;
    }

    public RecordRealm() {
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public RealmList<TagRealm> getTagList() {

        return tagList;
    }

    public ClusterRealm getByClusterNo(int clusterNo){
        for(int i=0;i<clusterMembers.size();i++){
            if(clusterMembers.get(i).getClusterNo()==clusterNo){
                return clusterMembers.get(i);
            }
        }
        return null;
    }


    public void addTagList(TagRealm tag){
        for(TagRealm t:tagList){
            if(t.getId()==tag.getId())return;
        }
        tagList.add(tag);
    }

    public void cascadeDelete() {
        for (SentenceRealm sentence : sentenceList) {
            sentence.getWordList().deleteAllFromRealm();
        }
        sentenceList.deleteAllFromRealm();
        this.deleteFromRealm();
    }

}
