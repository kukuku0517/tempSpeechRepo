package com.google.cloud.android.speech.diarization.formular;

import static com.google.cloud.android.speech.diarization.FeatureExtract.logBase;

public class MFCC {

    private int numMelFilters = 26;// how much
    private int numCepstra;// number of mfcc coeffs
    private double lowerFilterFreq = 0.00;// FmelLow
    private double samplingRate;
    private double upperFilterFreq;
    private double fftFrameNow[];
    private double fftFramePrev[];
    private boolean hasPrev = false;
    int featureDimension;
    private static final float windowSize = 0.025f;
    private int samplePerFrame;
    private DCT dct;
    private FFT fft;

    private double magSpectrum[];

    public MFCC(int samplingRate, int numCepstra, int featureDimension) {
        this.featureDimension = featureDimension;
        this.samplePerFrame = (int) (samplingRate * windowSize);
        this.samplingRate = samplingRate;
        this.numCepstra = numCepstra;
        this.upperFilterFreq = samplingRate / 2.0;
        this.dct = new DCT(this.numCepstra, numMelFilters);

    }


    public double[] doMFCC(float[] framedSignal, int index) {
        fftFrameNow = magnitudeSpectrum(framedSignal);
                int len = fftFrameNow.length;
        fftAbs=new double[len];
        System.arraycopy(fftFrameNow, 0, fftAbs, 0, len);
        for (int i = 0; i < len; i++) {
            fftAbs[i] = fftAbs[i] / len;
            fftFrameNow[i] = Math.pow(fftFrameNow[i],2)/ (1024);
        }

        if (index > 0) {
            hasPrev = true;
        }
        double energy = 0;
        for (double d : fftFrameNow) {
            energy += d;
        }
        if (energy == 0) {
            energy = 0.00000000000001f;
        }

        double fbank[] = melFilter(fftFrameNow, fftBinIndices());
        for (int i = 0; i < fbank.length; i++) {
            if (fbank[i] == 0) {
                fbank[i] = 0.00000000000001;
            }
        }

        double f[] = nonLinearTransformation(fbank);

        double cepc[] = dct.performDCT(f);

        for (int i = 0; i < cepc.length; i++) {
            double lift = 1 + (22 / 2) * Math.sin(Math.PI * i / 22);
            cepc[i] = cepc[i] * lift;
        }
        cepc[0] = Math.log(energy);

        double feature[] = new double[featureDimension];
        System.arraycopy(cepc, 0, feature, 8, numCepstra);
        feature[3] = spectralCentroid(fftFrameNow);
        feature[4] = spectralSpread();
        feature[5] = spectralEntropy();
        if (hasPrev) {
            feature[6] = spectralFlux(fftFramePrev);
        } else {
            feature[6] = spectralFlux(fftAbs);
        }
        feature[7] = spectralRollOff(0.9f);
        fftFramePrev = fftAbs;
        return feature;
    }


    int frameLengthPowOfTwo=1024;

    private double[] magnitudeSpectrum(float frame[]) {
        //int len =1024;

        frameLengthPowOfTwo = frame.length;
        frameLengthPowOfTwo = Integer.highestOneBit(frameLengthPowOfTwo);
        magSpectrum = new double[frameLengthPowOfTwo/ 2 + 1];
        fft = new FFT(frameLengthPowOfTwo);
        fft.fft(frame);
        for (int k = 0; k < frameLengthPowOfTwo / 2 + 1; k++) {
//            magSpectrum[k] = (fft.re[k] * fft.re[k] + fft.im[k] * fft.im[k]) / (1024);
            magSpectrum[k] = Math.sqrt(fft.re[k] * fft.re[k] + fft.im[k] * fft.im[k]);

        }
        return magSpectrum;
    }


    private int[] fftBinIndices() {
        int samplePerFrame = frameLengthPowOfTwo;
        int cbin[] = new int[numMelFilters + 2];
        cbin[0] = (int) Math.round(lowerFilterFreq / samplingRate * samplePerFrame);// cbin0
        cbin[cbin.length - 1] = (samplePerFrame / 2);// cbin24
        for (int i = 1; i <= numMelFilters; i++) {// from cbin1 to cbin23
            double fc = centerFreq(i);// center freq for i th filter
            cbin[i] = (int) Math.round(fc / samplingRate * samplePerFrame);
        }
        return cbin;
    }

    /**
     * performs mel filter operation
     *
     * @param bin  magnitude spectrum (| |)^2 of fft
     * @param cbin mel filter coeffs
     * @return mel filtered coeffs--> filter bank coefficients.
     */
    private double[] melFilter(double bin[], int cbin[]) {
        double temp[] = new double[numMelFilters + 2];
        for (int k = 1; k <= numMelFilters; k++) {
            double num1 = 0.0, num2 = 0.0;
            for (int i = cbin[k - 1]; i <= cbin[k]; i++) {
                num1 += ((i - cbin[k - 1] + 1) / (cbin[k] - cbin[k - 1] + 1)) * bin[i];
            }

            for (int i = cbin[k] + 1; i <= cbin[k + 1]; i++) {
                num2 += (1 - ((i - cbin[k]) / (cbin[k + 1] - cbin[k] + 1))) * bin[i];
            }

            temp[k] = num1 + num2;
        }
        double fbank[] = new double[numMelFilters];
        for (int i = 0; i < numMelFilters; i++) {
            fbank[i] = temp[i + 1];
            // System.out.println(fbank[i]);
        }
        return fbank;
    }

    /**
     * performs nonlinear transformation
     *
     * @param fbank
     * @return f log of filter bac
     */
    private double[] nonLinearTransformation(double fbank[]) {
        double f[] = new double[fbank.length];
        final double FLOOR = -50;
        for (int i = 0; i < fbank.length; i++) {
            f[i] = Math.log(fbank[i]);
            // check if ln() returns a value less than the floor
            if (f[i] < FLOOR) {
//                f[i] = FLOOR;
            }
        }
        return f;
    }

    private double centerFreq(int i) {
        double melFLow, melFHigh;
        melFLow = freqToMel(lowerFilterFreq);
        melFHigh = freqToMel(upperFilterFreq);
        double temp = melFLow + ((melFHigh - melFLow) / (numMelFilters + 1)) * i;
        return inverseMel(temp);
    }

    private double inverseMel(double x) {
        double temp = Math.pow(10, x / 2595) - 1;
        return 700 * (temp);
    }

    protected double freqToMel(double freq) {
        return 2595 * log10(1 + freq / 700);
    }

    private double log10(double value) {
        return Math.log(value) / Math.log(10);
    }


    private final double EPS = 0.00000001;

    double[] weightedFrame;
    int len;
    double fftAbs[];
    double centroid;
    double fftSum;

    private double spectralCentroid(double[] fft) {
        this.fftAbs = fft;
        len = fft.length;

        weightedFrame = new double[len];

        double max = -1;

        for (int i = 0; i < len; i++) {
//            fftAbs[i]=Math.abs(fftAbs[i]);
            weightedFrame[i] = fftAbs[i] * (i + 1) * samplingRate / (2.0f * len);
            if (max < fftAbs[i]) {
                max = fftAbs[i];
            }
        }

        if (max == 0) {
            max += EPS;
        }
        for (int i = 0; i < len; i++) {
            weightedFrame[i] /= max;
            fftAbs[i] /= max;
        }

        double weightSum = 0;
        fftSum = 0;
        for (double d : weightedFrame) {
            weightSum += d + EPS;
        }
        for (double d : fftAbs) {
            fftSum += d + EPS;
        }

        centroid = weightSum / fftSum / samplingRate * 2.0f;

        return centroid;
    }

    private double spectralSpread() {
        double spread = 0;

        for (int i = 0; i < len; i++) {
            spread += (Math.pow((i + 1) - centroid, 2) * fftAbs[i]) / fftSum;
        }
        spread = Math.sqrt(spread) / (samplingRate / 2.0f);
        return spread;
    }

    private double spectralEntropy() {
        int numOfBlock = 10;

        float energy = 0;
        for (double f : fftAbs)
            energy += f * f;
        int len = fftAbs.length;
        int subLen = len / 10;

        double[] subSum = new double[numOfBlock];
        for (int i = 0; i < numOfBlock; i++) {
            subSum[i] = 0;
            for (int j = 0; j < subLen; j++) {
                subSum[i] += Math.pow(fftAbs[i * subLen + j], 2);
            }
            subSum[i] /= (energy + EPS);
        }
        float total = 0;
        for (double f : subSum) {
            total += -f * logBase(f + EPS, 2);
        }

        return total;
    }

    private double spectralFlux(double[] fftPrev) {
        double sum = 0;
        double prevSum = 0;
        int len = fftAbs.length;
        for (int i = 0; i < len; i++) {
            sum += fftAbs[i] + EPS;
            prevSum += fftPrev[i] + EPS;
        }
        double flux = 0;
        for (int i = 0; i < len; i++) {
            flux += Math.pow(((fftAbs[i] / sum) - (fftPrev[i] / prevSum)), 2);
        }
        return flux;

    }

    private double spectralRollOff(float threshold) {
        double total = 0;
        for (double d : fftAbs)
            total += d * d;
        int len = fftAbs.length;
        double thres = total * threshold;
        double cum = 0;
        float count = 0;
        for (double d : fftAbs) {
            cum += d * d;
            count++;
            if (cum > thres)
                break;
        }
        return count / len;
    }
}
