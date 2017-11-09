package com.google.cloud.android.speech.view.background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.android.speech.data.realm.ClusterRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.diarization.FeatureVector;
import com.google.cloud.android.speech.diarization.KMeansCluster;
import com.google.cloud.android.speech.diarization.main.SpeechDiary;
import com.google.cloud.android.speech.event.PartialEvent;
import com.google.cloud.android.speech.event.PartialTimerEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.retrofit.GoogleClientRetrofit;
import com.google.cloud.android.speech.retrofit.LongRunning.Alternatives;
import com.google.cloud.android.speech.retrofit.LongRunning.LongrunningResponse;
import com.google.cloud.android.speech.retrofit.LongRunning.Results;
import com.google.cloud.android.speech.retrofit.LongRunning.Words;
import com.google.cloud.android.speech.util.AudioUtil;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.util.LogUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.interfaces.MediaCodecCallBack;
import com.google.cloud.android.speech.view.interfaces.StreamObserverRetrofit;
import com.google.cloud.android.speech.view.interfaces.VoiceRecorderCallBack;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.google.cloud.android.speech.event.ProcessIdEvent;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.cloud.speech.v1.WordInfo;
import com.google.protobuf.ByteString;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
//import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;
import io.realm.Realm;
import io.realm.RealmList;

public class SpeechService extends Service {

    private Realm realm;
    private RecordRealm record;
    private RecordRealm fileRecord;
    private VoiceRecorder mVoiceRecorder;
    private int recordId = -1;
    private int fileId = -1;
    private int timerCount = 0;
    private Timer timer = new Timer();

    private static final int RECORD = 0;
    private static final int FILE = 1;
    private static final String TAG = "SpeechService";
    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    /**
     * We reuse an access token if its expiration time is longer than this.
     */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    /**
     * We refresh the current access token before it expires.
     */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute
    public static boolean isRecording = false;
    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;

    private final SpeechBinder2 mBinder = new SpeechBinder2();
    private volatile AccessTokenTask mAccessTokenTask;
    private SpeechGrpc.SpeechStub mApi;
    private static Handler mHandler;

    private long startOfCall = -1;
    private long startOfRecording = -1;

    int count = 0;

    private final VoiceRecorderCallBack mVoiceCallback = new VoiceRecorderCallBack() {
        @Override
        public void onVoiceStart(long startMillis) throws IOException {
            startRecognizing(mVoiceRecorder.getSampleRate(), startMillis);
        }

        @Override
        public void onVoice(byte[] data, int size) {
            recognize(data, size);

        }

        @Override
        public void onVoiceEnd() {
            finishRecognizing();
        }

        @Override
        public void onConvertEnd() {
            Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {

                    stopRecognizing(RECORD);
                    notifyProcess();

                } // This is your code
            };
            mainHandler.post(myRunnable);


        }
    };

    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;
    private StreamObserver<StreamingRecognizeRequest> mFileRequestObserver;

    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver
            = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            String text = null;
            boolean isFinal = false;

            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();

                }
            }

            if (text != null) {
                onSpeechRecognized(isFinal, response.getResults(0).getAlternatives(0), startOfCall);
            }
        }


        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted()

        {

            Log.i(TAG, "API completedasdfafasdfasdfasdfasdf.");
        }

    };

    public boolean isRecording() {
        return isRecording;
    }

    public int getRecordId() {
        return recordId;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class SpeechBinder2 extends Binder {

        SpeechService getService() {
            return SpeechService.this;
        }

    }

    public void startForeground() {

        Intent notificationIntent = new Intent(this, ListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);
    }

    public static SpeechService from(IBinder binder) {
        return ((SpeechBinder2) binder).getService();
    }

    public void initRecorder(final String title, final ArrayList<String> tags) {
        startForeground();

        startOfRecording = System.currentTimeMillis();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                record.setTitle(title);
                record.setTagList(tags);
                record.setFilePath(FileUtil.getFilename(title));
                record.setStartMillis(startOfRecording);
                recordId = record.getId();
            }
        });
        isRecording = true;
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.setTitle(record.getTitle());
        mVoiceRecorder.start();

        timer = new Timer();
        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                timerCount++;
                EventBus.getDefault().postSticky(new PartialTimerEvent(timerCount));
                Log.d(TAG, "eventbus timer start :" + timerCount);
            }
        };
        timer.schedule(timertask, 1000, 1000);    // 1초 후에
    }

    public int createRecord() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                record = new RealmUtil().createObject(realm, RecordRealm.class);
                recordId = record.getId();

            }
        });
        return recordId;
    }

    public int createFileRecord() {
        realm.beginTransaction();
        fileRecord = new RealmUtil().createObject(realm, RecordRealm.class);
        fileId = fileRecord.getId();
        fileRecord.setStartMillis(System.currentTimeMillis());
        realm.commitTransaction();
        return fileId;
    }

    public void notifyProcess() {
        Log.d(TAG, "process on");
        EventBus.getDefault().postSticky(new ProcessIdEvent(recordId, fileId));
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */

    public void startRecognizing(int sampleRate, long startMillis) throws IOException {
        if (mApi == null) {
            Log.i(TAG, "API not ready. Ignoring the request.");
            return;
        }

        this.startOfCall = startMillis;

        mRequestObserver = mApi.streamingRecognize(mResponseObserver);
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(getDefaultLanguageCode())
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .setEnableWordTimeOffsets(true)

                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(false)
                        .build())
                .build());
    }


    long endOfFileSecond = -1;


    public static float[] floatMe(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    public static short[] shortMe(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }

    private boolean checkEmptyBytes(byte[] bytes) {
        for (byte by : bytes) {
            if (by != 0) {
                return true;
            }
        }
        return false;
    }

    ArrayList<double[][]> feature = new ArrayList<>();
    ArrayList<int[]> silence = new ArrayList<>();

    private boolean clusterInRange(long startWord, long endWord, long clusterTime) {
        if (startWord >= clusterTime) return true;
        if (endWord > clusterTime) return true;
        return false;
    }

    private boolean clusterInRange(WordRealm now, WordRealm next, long clusterTime) {
        if(next!=null){
            
        }else{
            if (now.getStartMillis() >= clusterTime) return true;
            if (now.getEndMillis() > clusterTime) return true;
        }


        return false;
    }

    private void applyCluster(int k, int[] results, final int fileId) {

        Realm realm = Realm.getDefaultInstance();

        final RecordRealm[] fileRecord = {realm.where(RecordRealm.class).equalTo("id", fileId).findFirst()};
        String path = fileRecord[0].getFilePath();

        long millSecond = AudioUtil.getAudioLength(getApplicationContext(), path);
        Log.d("kcluster millis", String.valueOf(millSecond));
        Log.d("kcluster result", String.valueOf(results.length));

        final float UNIT = 0.3f;
        float framePerUnit = (results.length * 1000 * UNIT) / millSecond;
        int noOfCluster = (int) Math.ceil(results.length / framePerUnit);
        Log.d("kcluster fpu", String.valueOf(framePerUnit));

        Log.d("kcluster noc", String.valueOf(noOfCluster));


        final int[] clusters = new int[noOfCluster];
        for (int i = 0; i < noOfCluster; i++) {

            int[] count = new int[k];
            int total = 0;

            for (int j = 0; j < framePerUnit && (i * framePerUnit + j) < results.length; j++) {
                int result = results[(int) (i * framePerUnit + j)];
                count[result]++;
                total += result;
            }
            if (total == 0) {
                clusters[i] = 0;
            } else {
                int maxIndex = -1;
                int max = -1;
                for (int j = 1; j < count.length; j++) {
                    if (count[j] > max) {
                        max = count[j];
                        maxIndex = j;
                    }
                }
                clusters[i] = maxIndex;

            }
        }

        LogUtil.print(clusters, "kcluster");


        if (fileRecord[0] != null) {

            int sentenceIndex = 0;
            int clusterIndex = 0;
            int err = 0;
            while (true) {
                //clusterIndex는 startSentence~endSentence(UNIT보정)사이
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();
                RealmList<SentenceRealm> sentences = record.getSentenceRealms();
                SentenceRealm sentence = sentences.get(sentenceIndex);
                long startSentence = sentence.getStartMillis() / 1000;
                long endSentence = sentence.getEndMillis() / 1000;
                clusterIndex = clusterIndex<startSentence/UNIT? (int) (startSentence / UNIT) :clusterIndex;
//                int clusterIndex = (int) (startSentence / UNIT);

                //한 문장을 다 확인 할때까지
                while (clusterIndex < (endSentence) / UNIT) {
                    int clusterNumber = clusters[clusterIndex];

                    if (clusterNumber == 0) { //현재 cluster가 silence : pass
                        clusterIndex++;
                        if (sentenceIndex == 0) {
                            err += UNIT;
                        }
                    } else if (sentence.getCluster() == 0) { // 현재 문장 cluster 미지정 : 현재 clusterNumber등록후 pass
                        realm.beginTransaction();
                        sentence.setCluster(clusterNumber);
                        realm.commitTransaction();
                        clusterIndex++;
                    } else if (sentence.getCluster() == clusterNumber) { //현재 cluster와 문장 cluster가 동일 : pas
                        clusterIndex++;
                    } else { //기존, 현재 cluster가 다를경우 문장 분리

                        RealmList<WordRealm> words = sentence.getWordList();
                        for (int i = 1; i < words.size(); i++) {
                            long startWord = words.get(i).getStartMillis() + err * 1000;
                            long endWord = words.get(i).getEndMillis() + err * 1000;

                            long clusterStart = (long) (clusterIndex * UNIT) * 1000;

                            if (clusterInRange(words.get(i),words.get(i+1)!=null?words.get(i+1):null, clusterStart)) {
                                realm.beginTransaction();
                                SentenceRealm origin = sentence;
                                SentenceRealm add = RealmUtil.createObject(realm, SentenceRealm.class);

                                add.setEndMillis(origin.getEndMillis());
                                origin.setEndMillis(startWord);
                                add.setStartMillis(startWord);

                                add.setCluster(clusterNumber);
                                RealmList<WordRealm> originWords = new RealmList<>();
                                RealmList<WordRealm> addWords = new RealmList<>();

                                for (int j = 0; j < words.size(); j++) {
                                    if (j < i) {
                                        originWords.add(words.get(j));
                                    } else {
                                        addWords.add(words.get(j));
                                    }
                                }

                                origin.setWordList(originWords);
                                add.setWordList(addWords);

                                origin.setSentence();
                                add.setSentence();


                                sentences.add(sentenceIndex + 1, add);
                                realm.commitTransaction();
                                break;

                            }
                        }
                        //문장을 추가했으므로 바로 다음 문장으로
//                        sentenceIndex++;
                        break;
                    }

                }
                //한문장이 끝나면 다음 문장으로
                sentenceIndex++;
                if (sentenceIndex == sentences.size()) break;

            }
        }


    }

    private MediaCodecCallBack mediaCodecCallBack = new MediaCodecCallBack() {
        @Override
        public void onReset() {
//            mFileRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//                    .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
//                            .setConfig(RecognitionConfig.newBuilder()
//                                    .setLanguageCode(getDefaultLanguageCode())
//                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                                    .setSampleRateHertz(fileSampleRate)
//                                    .setEnableWordTimeOffsets(true)
//
//                                    .build())
//                            .setInterimResults(true)
//                            .setSingleUtterance(false)
//                            .build())
//                    .build());
        }

        @Override
        public void onBufferRead(byte[] buffer) {

//                if (stream.read(mBuffer, 0, bufferSize) == -1) {
//                    break;
//                }
//TODO                currentBuffer+=bufferSize;

//                if(AudioUtil.isHearingVoice(buffer, bufferSize)){
//                    endOfFileSecond=millSecond*currentBuffer/fileLenth;
//                    Log.d(TAG,"file"+endOfFileSecond);
//                }
//            mFileRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//                    .setAudioContent(ByteString.copyFrom(buffer))
//                    .build());

            if (buffer.length > 0) {
                if (checkEmptyBytes(buffer)) {
                    float[] pcmFloat = floatMe(shortMe(buffer));
                    SpeechDiary speechDiary = new SpeechDiary();
                    FeatureVector fv = speechDiary.extractFeatureFromFile(pcmFloat);
                    if (fv != null) {
                        feature.add(fv.getFeatureVector());
                        silence.add(fv.getSilence());
                    }
                }
            }
        }

        @Override
        public void onCompleted() {
//            Realm realm = Realm.getDefaultInstance();
//            realm.executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    try {
//                        int[] results = new KMeansCluster(3, 13, feature, silence).iterRun(10);
//                        applyCluster(3, results, fileId, realm);
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            });

//            mFileRequestObserver.onCompleted();
            longrunningObservable.setSpeakerDiary(false);

            Log.d("observv","speaker");

            fileStreamFinished = true;

        }
    };


    int fileSampleRate = 16000;

    private StreamObserverRetrofit<LongrunningResponse> longrunningObserver
            = new StreamObserverRetrofit<LongrunningResponse>() {
        @Override
        public void onNext(LongrunningResponse response) {


        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete(LongrunningResponse response) {
            String text = null;

            Words[] words = response.getResults()[0].getAlternatives()[0].getWords();
            for(int i=0;i<words.length;i++){
                long start = words[i].getStartTime().getSeconds()*1000+words[i].getStartTime().getNanos()/1000000;
                long end= words[i].getEndTime().getSeconds()*1000+words[i].getEndTime().getNanos()/1000000;
                Log.d("call",words[i].getWord().toString()+":"+i+":"+start+"~"+end);
            }

            Alternatives alternative = null;

            if (response.getResults().length > 0) {
                final Results result = response.getResults()[0];
                if (result.getAlternatives().length > 0) {
                    alternative = result.getAlternatives()[0];
                    text = alternative.getTranscript();
                }
            }

            if (text != null) {
                onFileRecognized(true, alternative, startOfCall);
            }





        }
    };


    LongRunningObservable longrunningObservable = new LongRunningObservable();


    public void recognizeFileStream(final String title, final ArrayList<String> tags, final String fileName) {


        startForeground();

        longrunningObservable.setSpeakerDiary(true);
        longrunningObservable.setSpeechRecognize(true);
        longrunningObservable.setInit(true);

        longrunningObservable.addObserver(new LongRunningObserver() {
            @Override
            public void end() {
                longrunningObservable.setInit(false);
                Log.d("observv","end");
                int[] results = new int[0];
                try {
                    results = new KMeansCluster(3, 13, feature, silence).iterRun(20);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                applyCluster(3, results, fileId);
//                mFileRequestObserver.onCompleted();
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "API completed.");
                        stopRecognizing(FILE);
                        stopForeground();
                    } // This is your code
                };
                mainHandler.post(myRunnable);
            }
        });

        try {
            feature.clear();
            silence.clear();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    fileRecord.setTitle(title);
                    fileRecord.setTagList(tags);
                    fileRecord.setFilePath(fileName);
                }
            });

            File file = new File(fileName);
            fileSampleRate = AudioUtil.getSampleRate(file);


            getValidSampleRates();
            fileStreamFinished = false;

            createAudioRecord();
            getValidSampleRates();


            AudioStreamer audioStreamer = new AudioStreamer(mediaCodecCallBack);
            audioStreamer.setUrlString(fileName);

            GoogleClientRetrofit googleClientRetrofit = new GoogleClientRetrofit();
            googleClientRetrofit.longrunningRequestRetrofit(file.getName(), fileSampleRate, AudioUtil.wrap(file), longrunningObserver);

//            mFileRequestObserver = mApi.streamingRecognize(mFileResponseObserver);
//            mFileRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//                    .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
//                            .setConfig(RecognitionConfig.newBuilder()
//                                    .setLanguageCode(getDefaultLanguageCode())
//                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                                    .setSampleRateHertz(fileSampleRate)
//                                    .setEnableWordTimeOffsets(true)
//                                    .build())
//                            .setInterimResults(true)
//                            .setSingleUtterance(false)
//                            .build())
//                    .build());

            audioStreamer.play();

//

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
        Log.d("bufferCount recog", String.valueOf(count++));
        // Call the streaming recognition API
//        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//                .setAudioContent(ByteString.copyFrom(data, 0, size))
//                .build());
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());


//        Log.i(TAG, "buffer sent");
    }

    private void onFileRecognized(boolean isFinal, Alternatives alternative, long startOfCall) {

        String text = alternative.getTranscript();

        if (!TextUtils.isEmpty(text)) {
            if (isFinal) {

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();

                SentenceRealm sentence = new RealmUtil().createObject(realm, SentenceRealm.class);
                sentence.setSentence(text);
                long startOfSentence = startOfCall - startOfRecording;
                sentence.setStartMillis((int) (startOfSentence));
                Words[] words = alternative.getWords();
                for (int i = 0; i < words.length; i++) {
                    WordRealm word = new RealmUtil().createObject(realm, WordRealm.class);
                    word.setWord(words[i].getWord());
                    word.setSentenceId(sentence.getId());
                    long startSecond = words[i].getStartTime().getSeconds() * 1000 + startOfSentence;
                    startSecond += words[i].getStartTime().getNanos() / 1000000;
                    word.setStartMillis(startSecond);

                    long endSecond = words[i].getEndTime().getSeconds() * 1000 + startOfSentence;
                    endSecond += words[i].getEndTime().getNanos() / 1000000;
                    word.setEndMillis(endSecond);

                    sentence.getWordList().add(word);

                    if (i == words.length - 1) {
                        sentence.setEndMillis(endSecond);
                    }

                }


                record.getSentenceRealms().add(sentence);
                realm.commitTransaction();
                longrunningObservable.setSpeechRecognize(false);
                Log.d("observv","speech");

            } else {
                EventBus.getDefault().post(new PartialEvent(text));
            }

        }

    }


    private void onSpeechRecognized(boolean isFinal, SpeechRecognitionAlternative alternative, long startOfCall) {
        if (isFinal) {
            if (mVoiceRecorder != null) {
                mVoiceRecorder.dismiss();
            }
        }
        String text = alternative.getTranscript();

        if (!TextUtils.isEmpty(text)) {
            if (isFinal) {

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();

                SentenceRealm sentence = new RealmUtil().createObject(realm, SentenceRealm.class);
                sentence.setSentence(text);
                long startOfSentence = startOfCall - startOfRecording;
                sentence.setStartMillis((int) (startOfSentence));
                List<WordInfo> words = alternative.getWordsList();
//                for (WordInfo wordInfo : alternative.getWordsList()) {
//                for (int i = 0; i < words.size(); i++) {
//                    WordRealm word = new RealmUtil().createObject(realm, WordRealm.class);
//                    word.setWord(words.get(i).getWord());
//                    word.setSentenceId(sentence.getId());
//                    long second = words.get(i).getStartTime().getSeconds() * 1000 + startOfSentence;
//                    word.setStartMillis(second);
//                    sentence.getWordList().add(word);
//
//                    if (i == words.size() - 1) {
//                        sentence.setEndMillis(second);
//                    }
//                }

                TreeMap<Long, Integer> wordTimeRange = new TreeMap<>();

                for (int i = 0; i < words.size(); i++) {
                    WordRealm word = new RealmUtil().createObject(realm, WordRealm.class);
                    word.setWord(words.get(i).getWord());
                    word.setSentenceId(sentence.getId());
                    long second = words.get(i).getStartTime().getSeconds() * 1000 + startOfSentence;
                    word.setStartMillis(second);
                    sentence.getWordList().add(word);

//                    if (i == words.size() - 1) {
//                        sentence.setEndMillis(second);
//                    }

                    if (wordTimeRange.containsKey(second)) {
                        wordTimeRange.put(second, wordTimeRange.get(second) + 1);
                    } else {
                        wordTimeRange.put(second, 1);
                    }
                }


                int count = 0;

                for (Map.Entry<Long, Integer> entry : wordTimeRange.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        sentence.getWordList().get(count).setStartMillis(entry.getKey() + i * 1000 / entry.getValue());
                        count++;
                    }
                }

                sentence.setEndMillis(sentence.getWordList().get(count - 1).getStartMillis());

                record.getSentenceRealms().add(sentence);
                realm.commitTransaction();

            } else {
                EventBus.getDefault().post(new PartialEvent(text));
            }

        }

    }

    /**
     * Finishes recognizing speech audio.
     */
    public void finishRecognizing() {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
//        Log.i(TAG, "2 : observer nullified");
    }

    public void stopRecording() {
        if (mVoiceRecorder != null) {
            isRecording = false;
            mVoiceRecorder.stop();
            timer.cancel();
            timerCount = 0;
            startOfRecording = -1;
            mVoiceRecorder = null;
        }
    }


    public void stopRecognizing(final int type) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                int id = type == RECORD ? recordId : fileId;
                Log.d(TAG, "fileId in stop :" + fileId);
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", id).findFirst();
                record.setConverted(true);
                int[] results = new int[0];
                try {
                    results = new KMeansCluster(3, 13, feature, silence).iterRun(10);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ClusterRealm cluster = realm.createObject(ClusterRealm.class,fileId);
//                cluster.setId(fileId);
                cluster.setClusters(results);

                Log.d("AudioStream", String.valueOf(feature.size()));

//                applyCluster(3, results, fileId);

            }

        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {


                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "convert complete", Toast.LENGTH_SHORT).show();

                    }
                });


                if (type == RECORD) {
                    recordId = -1;
                } else {
                    fileId = -1;


                }
            }
        });


//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(Realm realm) {
//                int id = type==RECORD?recordId:fileId;
//                Log.d(TAG,"fileId in stop :"+fileId);
//                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", id).findFirst();
//                record.setConverted(true);
//                if(type==RECORD){
//                    recordId=-1;
//                }else{
//                    fileId=-1;
//                }
//                Handler handler = new Handler(Looper.getMainLooper());
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getBaseContext(), "convert complete", Toast.LENGTH_SHORT).show();
//
//                    }
//                });
//
//            }
//        });


        stopForeground();
    }

    private void stopForeground() {
        if (recordId == -1 && fileId == -1) {
            stopForeground(true);
        }
    }

//    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        notifyProcess();
        return super.onStartCommand(intent, flags, startId);
    }

    //Realm, accessToken, foreground service 초기화
    @Override
    public void onCreate() {
        super.onCreate();
        realm.init(getBaseContext());
        realm = Realm.getDefaultInstance();
        mHandler = new Handler();

        BucketAsync b = new BucketAsync();
//        b.execute();


//        Toast.makeText(getBaseContext(), System.getenv("GOOGLE_APPLICATION_CREDENTIALS").toString(), Toast.LENGTH_SHORT).show();
        fetchAccessToken();

    }

    class BucketAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

//            FirebaseStorage storage = FirebaseStorage.getInstance();
//            StorageReference storageRef = storage.getReference();
            GoogleCredentials credentials = null;
            final InputStream stream = getResources().openRawResource(R.raw.credential);

            try {
                credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(SCOPE);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            Storage storage = StorageOptions.getDefaultInstance().getService();
//
//            // The name for the new bucket
//            String bucketName = "speech_diary";  // "my-new-bucket";
//
//            // Creates the new bucket
//            Bucket bucket = storage.create(BucketInfo.newBuilder(bucketName)
//                    // See here for possible values: http://g.co/cloud/storage/docs/storage-classes
//                    .setStorageClass(StorageClass.COLDLINE)
//                    // Possible values: http://g.co/cloud/storage/docs/bucket-locations#location-mr
//                    .setLocation("asia")
//                    .build());
//
//            System.out.printf("Bucket %s created.%n", bucket.getName());


//
//            // Upload a local file to a new file to be created in your bucket.
////            InputStream uploadContent =null;
////            BlobId blobId = BlobId.of("speechdiary", "정환희진2.wav");
////            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
////            Blob zebraBlob = storage.create(blobInfo,uploadContent);
//
//            // Download a file from your bucket.
//            Blob giraffeBlob = storage.get("speechdiary", "정환희진2.wav", null);


            return null;
        }
    }

    //accessToken 제거
    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mFetchAccessTokenRunnable);
        mHandler = null;
        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mApi = null;
        }
    }

    /**
     * Recognize all data from the specified {@link InputStream}.
     *
     * @param stream The audio data.
     */

    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 22050, 44100};
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int recorderSampleRate = 44100;
    private int bufferSize = 0;

    private byte[] mBuffer;

    //buffer size 구하기위한 임시 함수
    private AudioRecord createAudioRecord() {
        for (int sampleRate : SAMPLE_RATE_CANDIDATES) {
            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);

            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            recorderSampleRate = sampleRate;
            bufferSize = sizeInBytes;
            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate, CHANNEL, ENCODING, sizeInBytes);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mBuffer = new byte[sizeInBytes];
                return audioRecord;
            } else {
                audioRecord.release();
            }
        }
        return null;
    }

    public int getValidSampleRates() {
        int sampleRate = 8000;
        for (int rate : new int[]{8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                sampleRate = rate;
            }
        }
        Log.i(TAG, String.valueOf(sampleRate));
        return sampleRate;
    }

    private boolean fileStreamFinished = false;


    private void fetchAccessToken() {
        if (mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    private String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        return language.toString();
    }

    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {
            final SharedPreferences prefs =
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String tokenValue = prefs.getString(PREF_ACCESS_TOKEN_VALUE, null);
            long expirationTime = prefs.getLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME, -1);

            // Check if the current token is still valid for a while
            if (tokenValue != null && expirationTime > 0) {
                if (expirationTime
                        > System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(expirationTime));
                }
            }

            // ***** WARNING *****
            // In this sample, we load the credential from a JSON file stored in a raw resource
            // folder of this client app. You should never do this in your app. Instead, store
            // the file in your server and obtain an access token from there.
            // *******************
            final InputStream stream = getResources().openRawResource(R.raw.credential);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                        .createScoped(SCOPE);
                final AccessToken token = credentials.refreshAccessToken();
                prefs.edit()
                        .putString(PREF_ACCESS_TOKEN_VALUE, token.getTokenValue())
                        .putLong(PREF_ACCESS_TOKEN_EXPIRATION_TIME,
                                token.getExpirationTime().getTime())
                        .apply();
                return token;
            } catch (IOException e) {
                Log.e(TAG, "Failed to obtain access token.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            mAccessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new GoogleCredentialsInterceptor(new GoogleCredentials(accessToken)
                            .createScoped(SCOPE)))
                    .build();
            mApi = SpeechGrpc.newStub(channel);

            // Schedule access token refresh before it expires
            if (mHandler != null) {
                mHandler.postDelayed(mFetchAccessTokenRunnable,
                        Math.max(accessToken.getExpirationTime().getTime()
                                - System.currentTimeMillis()
                                - ACCESS_TOKEN_FETCH_MARGIN, ACCESS_TOKEN_EXPIRATION_TOLERANCE));
            }
        }
    }

    /**
     * Authenticates the gRPC channel using the specified {@link GoogleCredentials}.
     */
    private static class GoogleCredentialsInterceptor implements ClientInterceptor {

        private final Credentials mCredentials;

        private Metadata mCached;

        private Map<String, List<String>> mLastMetadata;

        GoogleCredentialsInterceptor(Credentials credentials) {
            mCredentials = credentials;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                final MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
                final Channel next) {
            return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {
                @Override
                protected void checkedStart(ClientCall.Listener<RespT> responseListener, Metadata headers)
                        throws StatusException {
                    Metadata cachedSaved;
                    URI uri = serviceUri(next, method);
                    synchronized (this) {
                        Map<String, List<String>> latestMetadata = getRequestMetadata(uri);
                        if (mLastMetadata == null || mLastMetadata != latestMetadata) {
                            mLastMetadata = latestMetadata;
                            mCached = toHeaders(mLastMetadata);
                        }
                        cachedSaved = mCached;
                    }
                    headers.merge(cachedSaved);
                    delegate().start(responseListener, headers);
                }
            };
        }

        /**
         * Generate a JWT-specific service URI. The URI is simply an identifier with enough
         * information for a service to know that the JWT was intended for it. The URI will
         * commonly be verified with a simple string equality check.
         */
        private URI serviceUri(Channel channel, MethodDescriptor<?, ?> method)
                throws StatusException {
            String authority = channel.authority();
            if (authority == null) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Channel has no authority")
                        .asException();
            }
            // Always use HTTPS, by definition.
            final String scheme = "https";
            final int defaultPort = 443;
            String path = "/" + MethodDescriptor.extractFullServiceName(method.getFullMethodName());
            URI uri;
            try {
                uri = new URI(scheme, authority, path, null, null);
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI for auth")
                        .withCause(e).asException();
            }
            // The default port must not be present. Alternative ports should be present.
            if (uri.getPort() == defaultPort) {
                uri = removePort(uri);
            }
            return uri;
        }

        private URI removePort(URI uri) throws StatusException {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1 /* port */,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI after removing port")
                        .withCause(e).asException();
            }
        }

        private Map<String, List<String>> getRequestMetadata(URI uri) throws StatusException {
            try {
                return mCredentials.getRequestMetadata(uri);
            } catch (IOException e) {
                throw Status.UNAUTHENTICATED.withCause(e).asException();
            }
        }

        private static Metadata toHeaders(Map<String, List<String>> metadata) {
            Metadata headers = new Metadata();
            if (metadata != null) {
                for (String key : metadata.keySet()) {
                    Metadata.Key<String> headerKey = Metadata.Key.of(
                            key, Metadata.ASCII_STRING_MARSHALLER);
                    for (String value : metadata.get(key)) {
                        headers.put(headerKey, value);
                    }
                }
            }
            return headers;
        }

    }


}


