package com.google.cloud.android.speech.diarization.main;

import android.util.Log;


import com.google.cloud.android.speech.diarization.FeatureExtract;
import com.google.cloud.android.speech.diarization.formular.PreProcess;
import com.google.cloud.android.speech.diarization.FeatureVector;
import com.google.cloud.android.speech.util.AudioUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SpeechDiary {

    private static final int SAMPLING_RATE = 44100; // (int) fc.getRate();
    private static final int SAMPLE_PER_FRAME = 1024; // 512,23.22ms
    private static final int FEATURE_DIMENSION = 39;
    private FeatureExtract featureExtract;
    private PreProcess prp;
    private int id;

    public FeatureVector extractFeatureFromFile(float[] arrAmp) {
        prp = new PreProcess(arrAmp, SAMPLING_RATE);
        if (prp.framedSignal[0].length >= SAMPLE_PER_FRAME && prp.framedSignal.length > 1) {
            featureExtract = new FeatureExtract(prp.framedSignal, SAMPLING_RATE, SAMPLE_PER_FRAME);
            featureExtract.makeMfccFeatureVector();
            featureExtract.setSilence(arrAmp);
            return featureExtract.getFeatureVector();
        } else {
            return null;
        }
    }

    public SpeechDiary(int id) {
        this.id = id;
    }
}
