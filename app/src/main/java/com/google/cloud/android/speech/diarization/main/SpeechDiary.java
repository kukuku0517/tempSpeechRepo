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

    private int sampleRate;
    private static final int FEATURE_DIMENSION = 21;
    private FeatureExtract featureExtract;
    private PreProcess prp;

    public FeatureVector extractFeatureFromFile(float[] arrAmp) {
        prp = new PreProcess(arrAmp, sampleRate);

        featureExtract = new FeatureExtract(prp.framedSignal, sampleRate, FEATURE_DIMENSION);
        featureExtract.makeMfccFeatureVector();
//            featureExtract.setSilence();
        featureExtract.setSilence(arrAmp);
        return featureExtract.getFeatureVector();

    }

    public SpeechDiary(int id, int sampleRate) {
        this.sampleRate = sampleRate;
    }
}
