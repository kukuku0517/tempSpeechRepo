package com.google.cloud.android.speech.View.Recording.Adapter;

/**
 * Created by USER on 2017-10-19.
 */

public class ProcessIdEvent {
    int recordId;
    int fileId;

    public ProcessIdEvent(int recordId, int fileId) {
        this.recordId = recordId;
        this.fileId = fileId;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public boolean isRecording(){
        return recordId==-1?false:true;
    }
    public boolean isFiling(){
        return fileId==-1?false:true;
    }
}
