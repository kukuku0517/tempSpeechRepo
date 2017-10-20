package com.google.cloud.android.speech.Util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * Created by USER on 2017-10-20.
 */

public class AudioUtil {

    private static final int AMPLITUDE_THRESHOLD = 1500;

    public static long getAudioLength(Context context, String fileName){
        Uri uri = Uri.parse(fileName);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context,uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long millSecond = Integer.parseInt(durationStr);
        return millSecond;
    }
    public static  boolean isHearingVoice(byte[] buffer, int size) {
        for (int i = 0; i < size - 1; i += 2) {
            // The buffer has LINEAR16 in little endian.
            int s = buffer[i + 1];
            if (s < 0) s *= -1;
            s <<= 8;
            s += Math.abs(buffer[i]);
            if (s > AMPLITUDE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
}
