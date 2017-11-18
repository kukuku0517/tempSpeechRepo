package com.google.cloud.android.speech.data.realm;

import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by samsung on 2017-10-07.
 */

public class RecordRealm extends RealmObject implements PrimaryRealm {

    @PrimaryKey
    private int id;
    private int directoryId=-1;

    public int getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(int directoryId) {
        this.directoryId = directoryId;
    }

    private String title = "";
    private String audioPath;
    private String videoPath;
    private int duration = 0;
    private long startMillis = -1;
    private boolean converted = false;
    private RealmList<TagRealm> tagList = new RealmList<>();
    private RealmList<SentenceRealm> sentenceList = new RealmList<>();
    private RealmList<ClusterRealm> clusterMembers = new RealmList<>();
    private RealmList<IntegerRealm> cluster = new RealmList<>();
    private boolean isOrigin=true;
    private int duplicateId=-1;

    public int getDuplicateId() {
        return duplicateId;
    }

    public void setDuplicateId(int duplicateId) {
        this.duplicateId = duplicateId;
    }

    public boolean isOrigin() {
        return isOrigin;
    }

    public void setOrigin(boolean origin) {
        isOrigin = origin;
    }

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

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public void setCluster(RealmList<IntegerRealm> cluster) {
        this.cluster = cluster;
    }

    public void setCluster(int[] cluster) {
        for (int i : cluster) {
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

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
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

    public ClusterRealm getByClusterNo(int clusterNo) {
        for (int i = 0; i < clusterMembers.size(); i++) {
            if (clusterMembers.get(i).getClusterNo() == clusterNo) {
                return clusterMembers.get(i);
            }
        }
        return null;
    }


    public void addTagList(TagRealm tag) {
        for (TagRealm t : tagList) {
            if (t.getId() == tag.getId()) return;
        }
        tagList.add(tag);
    }

    public void cascadeDelete() {

        cluster.deleteAllFromRealm();

        for (SentenceRealm sentence : sentenceList) {
            sentence.getWordList().deleteAllFromRealm();
        }
        sentenceList.deleteAllFromRealm();
        this.deleteFromRealm();
    }

}
