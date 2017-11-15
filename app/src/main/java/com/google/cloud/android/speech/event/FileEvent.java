package com.google.cloud.android.speech.event;

import java.util.ArrayList;

/**
 * Created by USER on 2017-10-17.
 */

public class FileEvent {
    private String title;
    private ArrayList<Integer> tags;
    private String filePath;

    public String getTitle() {
        return title;
    }

    public ArrayList<Integer> getTags() {
        return tags;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileEvent(String title, ArrayList<Integer>tags, String filePath){
        this.title=title;
        this.tags=tags;
        this.filePath = filePath;
    }

}
