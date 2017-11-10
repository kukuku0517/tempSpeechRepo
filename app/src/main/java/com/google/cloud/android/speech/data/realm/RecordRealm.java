package com.google.cloud.android.speech.data.realm;

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

    private RealmList<StringRealm> tagList = new RealmList<>();
    private RealmList<SentenceRealm> sentenceList = new RealmList<>();


    private RealmList<ClusterRealm> clusterMembers = new RealmList<>();

    public RealmList<ClusterRealm> getClusterMembers() {
        return clusterMembers;
    }

    public void setClusterMembers(RealmList<ClusterRealm> clusterMembers) {
        this.clusterMembers = clusterMembers;
    }


    private long startMillis = -1;
    private boolean converted = false;

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

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public RealmList<SentenceRealm> getSentenceRealms() {
        return sentenceList;
    }

    public void setSentenceRealms(RealmList<SentenceRealm> sentenceRealms) {
        this.sentenceList = sentenceRealms;
    }

    public RecordRealm() {
    }

    public RecordRealm(String title, int duration, ArrayList<String> tagList) {
        this.title = title;
        this.duration = duration;
        setTagList(tagList);
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

    public RealmList<StringRealm> getTagList() {

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
    public void setTagList(ArrayList<String> tagList) {
        for (String s : tagList) {
            this.tagList.add(new StringRealm(s));
        }
    }

    public void cascadeDelete() {
        for (SentenceRealm sentence : sentenceList) {
            sentence.getWordList().deleteAllFromRealm();
        }
        sentenceList.deleteAllFromRealm();
        this.deleteFromRealm();
    }

}
