package com.google.cloud.android.speech.Event;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Created by USER on 2017-10-17.
 */

public class FileEvent {
    private String title;
    private ArrayList<String> tags;
    private String filePath;

    public String getTitle() {
        return title;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileEvent(String title, ArrayList<String>tags, String filePath){
        this.title=title;
        this.tags=tags;
        this.filePath = filePath;
    }

}
