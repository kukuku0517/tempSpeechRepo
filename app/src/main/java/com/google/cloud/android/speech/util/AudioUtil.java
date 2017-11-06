package com.google.cloud.android.speech.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * Created by USER on 2017-10-20.
 */

public class AudioUtil {

    private static final int AMPLITUDE_THRESHOLD = 1500;

    public static long getAudioLength(Context context, String fileName) {
        Uri uri = Uri.parse(fileName);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long millSecond = Integer.parseInt(durationStr);
        return millSecond;
    }

    public static boolean isHearingVoice(byte[] buffer, int size) {
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



    public static int[] getSilenceFrames(float[] originalSignal, int sampleRate) {
        int samplePerFrame = sampleRate / 1000;
        int firstSamples = samplePerFrame * 200;
        firstSamples = originalSignal.length > firstSamples ? firstSamples : originalSignal.length;

        float[] voiced = new float[originalSignal.length];
        float sum = 0;
        double sd ;
        double m ;

        // 1. calculation of mean
        for (int i = 0; i < firstSamples; i++) {
            sum += originalSignal[i];
        }

        m = sum / firstSamples;// mean
        sum = 0;// reuse var for S.D.

        // 2. calculation of Standard Deviation
        for (int i = 0; i < firstSamples; i++) {
            sum += Math.pow((originalSignal[i] - m), 2);
        }
        sd = Math.sqrt(sum / firstSamples);

        for (int i = 0; i < originalSignal.length; i++) {
            // System.out.println("x-u/SD  ="+(Math.abs(originalSignal[i] -u ) /
            // sd));
            if ((Math.abs(originalSignal[i] - m) / sd) > 2) {
                voiced[i] = 1;
            } else {
                voiced[i] = 0;
            }
        }

        // 4. calculation of voiced and unvoiced signals
        // mark each frame to be voiced or unvoiced frame
        int frameCount = 0;
        int usefulFramesCount = 1;
        int count_voiced = 0;
        int count_unvoiced = 0;
        int voicedFrame[] = new int[originalSignal.length / samplePerFrame];
        int loopCount = originalSignal.length - (originalSignal.length % samplePerFrame);// skip
        // the
        // last
        for (int i = 0; i < loopCount; i += samplePerFrame) {
            count_voiced = 0;
            count_unvoiced = 0;
            for (int j = i; j < i + samplePerFrame; j++) {
                if (voiced[j] == 1) {
                    count_voiced++;
                } else {
                    count_unvoiced++;
                }
            }
            if (count_voiced > count_unvoiced) {
                usefulFramesCount++;
                voicedFrame[frameCount++] = 1;
            } else {
                voicedFrame[frameCount++] = 0;
            }
        }

//        // 5. silence removal
//        float[] silenceRemovedSignal = new float[usefulFramesCount * samplePerFrame];
//        int k = 0;
//        for (int i = 0; i < frameCount; i++) {
//            if (voicedFrame[i] == 1) {
//                for (int j = i * samplePerFrame; j < i * samplePerFrame + samplePerFrame; j++) {
//                    silenceRemovedSignal[k++] = originalSignal[j];
//                }
//            }
//        }
//        // end
//        return silenceRemovedSignal;

        return voicedFrame;
    }
}
