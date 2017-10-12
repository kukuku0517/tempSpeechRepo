package com.google.cloud.android.speech.Util;

import android.os.Environment;

import java.io.File;

/**
 * Created by USER on 2017-10-11.
 */

public class FileUtil {
    private static String AUDIO_RECORDER_FOLDER = "Music";
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.pcm";

    public static String getFilename(String title) {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }
        String fileName = (title == null | title.equals("")) ? String.valueOf(System.currentTimeMillis()) : title;
        return (file.getAbsolutePath() + "/" + fileName +
                AUDIO_RECORDER_FILE_EXT_WAV);
    }


    public static  String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();


        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }
}
