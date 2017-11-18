package com.google.cloud.android.speech.event;

import java.util.ArrayList;

/**
 * Created by USER on 2017-10-17.
 */

public class FileEvent {
    private String title;
    private ArrayList<Integer> tags;
    private String filePath;
    private int requestCode;
    private int dirId;

    public int getDirId() {
        return dirId;
    }



    public int getRequestCode() {
        return requestCode;
    }

    public String getTitle() {
        return title;
    }

    public ArrayList<Integer> getTags() {
        return tags;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileEvent(String title, ArrayList<Integer> tags, String filePath, int requestCode, int dirId) {
        this.title = title;
        this.tags = tags;
        this.filePath = filePath;
        this.requestCode = requestCode;
        this.dirId = dirId;
    }
}
