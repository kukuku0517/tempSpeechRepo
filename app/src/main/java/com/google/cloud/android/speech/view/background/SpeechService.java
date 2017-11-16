package com.google.cloud.android.speech.view.background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.cloud.android.speech.data.realm.ClusterDataRealm;
import com.google.cloud.android.speech.data.realm.ClusterRealm;
import com.google.cloud.android.speech.data.realm.FeatureRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.data.realm.VectorRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.diarization.FeatureVector;
import com.google.cloud.android.speech.diarization.KMeansCluster;
import com.google.cloud.android.speech.diarization.main.SpeechDiary;
import com.google.cloud.android.speech.event.PartialEvent;
import com.google.cloud.android.speech.event.PartialTimerEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.longRunning.recognizeObservable;
import com.google.cloud.android.speech.longRunning.RecognizeObserver;
import com.google.cloud.android.speech.retrofit.GoogleClientRetrofit;
import com.google.cloud.android.speech.longRunning.longRunningDTO.Alternatives;
import com.google.cloud.android.speech.longRunning.longRunningDTO.LongrunningResponse;
import com.google.cloud.android.speech.longRunning.longRunningDTO.Results;
import com.google.cloud.android.speech.longRunning.longRunningDTO.Words;
import com.google.cloud.android.speech.util.AudioUtil;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.interfaces.MediaCodecCallBack;
import com.google.cloud.android.speech.view.interfaces.StreamObserverRetrofit;
import com.google.cloud.android.speech.view.interfaces.VoiceRecorderCallBack;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.google.cloud.android.speech.event.ProcessIdEvent;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechContext;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
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

    private static final String TAG = "SpeechService";
    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute
    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    public static boolean IS_RECORDING = false;

    private Realm realm;
    private RecordRealm record;
    private RecordRealm fileRecord;
    private VoiceRecorder mVoiceRecorder;

    private int recordId = -1;
    private int fileId = -1;
    private long startOfCall = -1;
    private long startOfRecording = -1;

    private int timerCount = 0;
    private static final int RECORD = 0;
    private static final int FILE = 1;
    private Timer timer = new Timer();

    private final SpeechBinder mBinder = new SpeechBinder();
    private volatile AccessTokenTask mAccessTokenTask;
    private SpeechGrpc.SpeechStub mApi;
    private static Handler mHandler;

    private final VoiceRecorderCallBack mVoiceCallback = new VoiceRecorderCallBack() {
        @Override
        public void onVoiceStart(long startMillis) throws IOException {
            startSpeechRecognizing(mVoiceRecorder.getSampleRate(), startMillis);
        }

        @Override
        public void onVoice(byte[] data, int size) {
            recognizeSpeechStream(data, size);

        }

        @Override
        public void onVoiceEnd() {
            finishSpeechRecordingInterval();
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
                onSpeechResult(isFinal, response.getResults(0).getAlternatives(0), startOfCall);
            }
        }


        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {

        }

    };


//    HashMap<Integer, ArrayList<double[][]>> featureMap = new HashMap<>();
//    HashMap<Integer, ArrayList<int[]>> silenceMap= new HashMap<>();

    private void collectFeatureVectors(double[][] fv, int[] slnce, int id) {
//        feature.add(fv);
//        silence.add(slnce);
//        if(featureMap.containsKey(id)){
//            featureMap.get(id).add(fv);
//            silenceMap.get(id).add(slnce);
//        }else{
//            ArrayList<double[][]> fvList = new ArrayList<>();
//            ArrayList<int[]> slnceList= new ArrayList<>();
//            fvList.add(fv);
//            slnceList.add(slnce);
//            featureMap.put(id,fvList);
//            silenceMap.put(id,slnceList);
//        }
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        FeatureRealm featureRealm = realm.where(FeatureRealm.class).equalTo("id", id).findFirst();
        if (featureRealm == null) {
            featureRealm = realm.createObject(FeatureRealm.class, id);
        }
        for (double[] feature : fv) {
            VectorRealm vector = realm.createObject(VectorRealm.class);
            vector.setFeatureVector(feature);
            featureRealm.getFeatureVectors().add(vector);
        }

        featureRealm.setSilence(slnce);

        realm.commitTransaction();
    }


    private int bufferTotal = 0;
    private int sampleRate = 0;
    private Queue<byte[]> byteQueue = new LinkedList<>();

    private MediaCodecCallBack audioCodecCallback = new MediaCodecCallBack() {
        @Override
        public void onStart(int size, int rate) {

        }

        @Override
        public void onBufferRead(byte[] buffer, int rate) {
            if (buffer.length > 0) {
                if (checkEmptyBytes(buffer)) {
                    sampleRate = rate;
                    byteQueue.offer(buffer);
                    bufferTotal += buffer.length;

                    if (bufferTotal >= rate * 5) {
                        byte[] bufferBlock = new byte[bufferTotal];

                        int count = 0;
                        while (!byteQueue.isEmpty()) {
                            byte[] temp = byteQueue.poll();
                            System.arraycopy(temp, 0, bufferBlock, count, temp.length);
                            count += temp.length;
                        }

                        float[] pcmFloat = floatMe(shortMe(bufferBlock));
                        SpeechDiary speechDiary = new SpeechDiary(fileId, rate);
                        FeatureVector fv = speechDiary.extractFeatureFromFile(pcmFloat);
                        if (fv != null) {
                            collectFeatureVectors(fv.getFeatureVector(), fv.getSilence(), fileId);
                        }
                        bufferTotal = 0;
                    }
                }
            }
        }

        @Override
        public void onCompleted() {
            byte[] bufferBlock = new byte[bufferTotal];

            int count = 0;
            while (!byteQueue.isEmpty()) {
                byte[] temp = byteQueue.poll();
                System.arraycopy(temp, 0, bufferBlock, count, temp.length);
                count += temp.length;
            }

            float[] pcmFloat = floatMe(shortMe(bufferBlock));
            SpeechDiary speechDiary = new SpeechDiary(fileId, sampleRate);
            FeatureVector fv = speechDiary.extractFeatureFromFile(pcmFloat);
            if (fv != null) {
                collectFeatureVectors(fv.getFeatureVector(), fv.getSilence(), fileId);
            }
            bufferTotal = 0;

            fileObservable.setSpeakerDiary(false);
            Log.d("observv", "speaker");
        }
    };


    FileOutputStream os;
    int videoBufferSize = 0;
    private MediaCodecCallBack videoCodecCallBack = new MediaCodecCallBack() {
        @Override
        public void onStart(int size, int rate) {

        }

        @Override
        public void onBufferRead(byte[] buffer, int rate) throws IOException {

//            if (videoBufferSize == 0)
//                videoBufferSize = buffer.length;
            videoBufferSize += buffer.length;
            if (buffer.length > 0) {
                if (checkEmptyBytes(buffer)) {
                    if (os == null) {
                        os = new FileOutputStream(FileUtil.getTempFilename());
                    }
                    os.write(buffer);
                }
            }
        }

        @Override
        public void onCompleted() throws IOException {
            os.close();
            os=null;

            Realm realm = Realm.getDefaultInstance();
            RecordRealm fileRecord = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();
            FileUtil.copyWaveFile(FileUtil.getTempFilename(), FileUtil.getFilename(fileRecord.getTitle()), fileSampleRate, 16, videoBufferSize,2);

            File file = new File(FileUtil.getFilename(fileRecord.getTitle()));
            fileSampleRate = AudioUtil.getSampleRate(file);
            AudioStreamer audioStreamer = new AudioStreamer(audioCodecCallback);
            audioStreamer.setUrlString(file.getAbsolutePath());
            GoogleClientRetrofit googleClientRetrofit = new GoogleClientRetrofit();
            googleClientRetrofit.longrunningRequestRetrofit(file.getName(), fileSampleRate, AudioUtil.wrap(file), longrunningObserver);
            audioStreamer.play();
            videoBufferSize = 0;



        }
    };


    private int fileSampleRate;

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

            for (Results result : response.getResults()) {
                Alternatives alternative = result.getAlternatives()[0];
                onFileResult(true, alternative, startOfCall);
            }

            fileObservable.setRecognize(false);
        }
    };

    private recognizeObservable fileObservable = new recognizeObservable();
    private recognizeObservable speechObseravable = new recognizeObservable();

    private RecognizeObserver fileObserver = new RecognizeObserver() {

        @Override
        public void end() {
            fileObservable.setInit(false);

            Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "API completed.");
                    stopRecognizing(FILE);
                } // This is your code
            };
            mainHandler.post(myRunnable);

        }
    };
    private RecognizeObserver speechObserver = new RecognizeObserver() {

        @Override
        public void end() {
            speechObseravable.setInit(false);
            int[] results;
            try {
                Realm realm = Realm.getDefaultInstance();
                FeatureRealm feature = realm.where(FeatureRealm.class).equalTo("id", recordId).findFirst();
                results = new KMeansCluster(3, 21, feature.getFeatureVectors(), feature.getSilence()).iterRun(20);

                applyClusterToRealm(3, results, recordId);
            } catch (IOException e) {
                e.printStackTrace();
            }
//                featureMap.remove(fileId);
//                silenceMap.remove(fileId);


        }
    };
//    private ArrayList<double[][]> feature = new ArrayList<>();
//
//    private ArrayList<int[]> silence = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //Realm, accessToken, foreground service 초기화
    @Override
    public void onCreate() {
        super.onCreate();
        realm.init(getBaseContext());
        realm = Realm.getDefaultInstance();
        mHandler = new Handler();

        fetchAccessToken();

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

    public boolean isRecording() {
        return IS_RECORDING;
    }

    public int getRecordId() {
        return recordId;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class SpeechBinder extends Binder {

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
        return ((SpeechBinder) binder).getService();
    }

    public void initSpeechRecognizing(final String title, final ArrayList<Integer> tags) {
        startForeground();

        startOfRecording = System.currentTimeMillis();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                record.setTitle(title);
                for (int i : tags) {
                    TagRealm tagRealm = realm.where(TagRealm.class).equalTo("id", i).findFirst();
                    tagRealm.setCount(tagRealm.getCount() + 1);
                    record.addTagList(tagRealm);
                }
                record.setFilePath(FileUtil.getFilename(title));
                record.setStartMillis(startOfRecording);
                recordId = record.getId();
            }
        });
        IS_RECORDING = true;
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

    public int createSpeechRecord() {
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

    public void startSpeechRecognizing(int sampleRate, long startMillis) throws IOException {
        if (mApi == null) {
            Log.i(TAG, "API not ready. Ignoring the request.");
            return;
        }

        this.startOfCall = startMillis;


        SpeechContext context = SpeechContext.newBuilder().addPhrases("바이크").build();

        mRequestObserver = mApi.streamingRecognize(mResponseObserver);
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(getDefaultLanguageCode())
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .setEnableWordTimeOffsets(true)
                                .addSpeechContexts(context)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(false)
                        .build())
                .build());
    }

    private static float[] floatMe(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    private static short[] shortMe(byte[] bytes) {
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

    public void recognizeFileStream(final String title, final ArrayList<Integer> tags, final String fileName) {
        startForeground();
        fileObservable.setSpeakerDiary(true);
        fileObservable.setRecognize(true);
        fileObservable.setInit(true);
        fileObservable.addObserver(fileObserver);
        try {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    fileRecord.setTitle(title);
                    fileRecord.setFilePath(fileName);
                    for (int i : tags) {
                        fileRecord.addTagList(realm.where(TagRealm.class).equalTo("id", i).findFirst());
                    }
                }
            });

            File file = new File(fileName);
            fileSampleRate = AudioUtil.getSampleRate(file);
            AudioStreamer audioStreamer = new AudioStreamer(audioCodecCallback);
            audioStreamer.setUrlString(fileName);
            GoogleClientRetrofit googleClientRetrofit = new GoogleClientRetrofit();
            googleClientRetrofit.longrunningRequestRetrofit(file.getName(), fileSampleRate, AudioUtil.wrap(file), longrunningObserver);
            audioStreamer.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recognizeVideoStream(final String title, final ArrayList<Integer> tags, final String fileName) {
        startForeground();
        fileObservable.setSpeakerDiary(true);
        fileObservable.setRecognize(true);
        fileObservable.setInit(true);
        fileObservable.addObserver(fileObserver);
        try {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    fileRecord.setTitle(title);
                    fileRecord.setFilePath(fileName);
                    for (int i : tags) {
                        fileRecord.addTagList(realm.where(TagRealm.class).equalTo("id", i).findFirst());
                    }
                }
            });

            File file = new File(fileName);
            fileSampleRate = AudioUtil.getSampleRate(file);


            AudioStreamer audioStreamer = new AudioStreamer(videoCodecCallBack);
            audioStreamer.setUrlString(fileName);
            audioStreamer.play();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recognizeSpeechStream(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }

        // Call the streaming recognition API
//        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//                .setAudioContent(ByteString.copyFrom(data, 0, size))
//                .build());
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());

        //TODO
        if (checkEmptyBytes(data)) {
            float[] pcmFloat = floatMe(shortMe(data));
            SpeechDiary speechDiary = new SpeechDiary(recordId, fileSampleRate);
            FeatureVector fv = speechDiary.extractFeatureFromFile(pcmFloat);
            if (fv != null) {
                collectFeatureVectors(fv.getFeatureVector(), fv.getSilence(), recordId);
            }
        }

//        Log.i(TAG, "buffer sent");
    }

    private void onFileResult(boolean isFinal, Alternatives alternative, long startOfCall) {

        String text = alternative.getTranscript();

        if (!TextUtils.isEmpty(text)) {
            if (isFinal) {

                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();

                SentenceRealm sentence = new RealmUtil().createObject(realm, SentenceRealm.class);
                Words[] words = alternative.getWords();

                for (int i = 0; i < words.length; i++) {
                    WordRealm word = new RealmUtil().createObject(realm, WordRealm.class);
                    word.setWord(words[i].getWord());
                    word.setSentenceId(sentence.getId());
                    long startSecond = words[i].getStartTime().getSeconds() * 1000;
                    startSecond += words[i].getStartTime().getNanos() / 1000000;
                    word.setStartMillis(startSecond);

                    long endSecond = words[i].getEndTime().getSeconds() * 1000;
                    endSecond += words[i].getEndTime().getNanos() / 1000000;
                    word.setEndMillis(endSecond);

                    sentence.getWordList().add(word);

                    if (i == 0) {
                        sentence.setStartMillis(startSecond);
                    }
                    if (i == words.length - 1) {
                        sentence.setEndMillis(endSecond);
                    }
                }

                sentence.setSentence();
                record.getSentenceRealms().add(sentence);
                realm.commitTransaction();
                Log.d("observv", "speech");

            } else {
                EventBus.getDefault().post(new PartialEvent(text));
            }

        }

    }

    private void onSpeechResult(boolean isFinal, SpeechRecognitionAlternative alternative, long startOfCall) {
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

                long startOfSentence = startOfCall - startOfRecording;
                sentence.setStartMillis((int) (startOfSentence));
                List<WordInfo> words = alternative.getWordsList();
                TreeMap<Long, Integer> wordTimeRange = new TreeMap<>();

                for (int i = 0; i < words.size(); i++) {
                    WordRealm word = new RealmUtil().createObject(realm, WordRealm.class);
                    word.setWord(words.get(i).getWord());
                    word.setSentenceId(sentence.getId());
                    long second = words.get(i).getStartTime().getSeconds() * 1000 + startOfSentence;
                    word.setStartMillis(second);
                    sentence.getWordList().add(word);

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
                sentence.setSentence();
                record.getSentenceRealms().add(sentence);
                realm.commitTransaction();

            } else {
                EventBus.getDefault().post(new PartialEvent(text));
            }

        }

    }


    private int getClusterForWord(WordRealm word, int[] clusters, int err) {
        int UNIT = (int) (1000 * 0.01);
        int noOfCluster = 3;
//        err=0;
        int start = (int) ((word.getStartMillis() + err) / UNIT);
        int end = (int) ((word.getEndMillis() + err) / UNIT);
        if (end > clusters.length) end = clusters.length;
        int clusterCount[] = new int[noOfCluster];
        for (int i = 0; i < noOfCluster; i++) {
            clusterCount[i] = 0;
        }
        for (int i = start; i < end; i++) {
            clusterCount[clusters[i]]++;
        }
        int max = -1;
        int maxIndex = -1;
        for (int i = 0; i < noOfCluster; i++) {
            if (clusterCount[i] > max) {
                max = clusterCount[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private void applyClusterToRealm(int k, int[] results, final int fileId) {

        Realm realm = Realm.getDefaultInstance();
        final RecordRealm[] fileRecord = {realm.where(RecordRealm.class).equalTo("id", fileId).findFirst()};
        String path = fileRecord[0].getFilePath();
        final float UNIT = 0.01f;
        /**************************************************************/

        if (fileRecord[0] != null) {

            int sentenceIndex = 0;
            int err = 0;
            for (int i : results) {
                if (i == 0) err += UNIT * 1000;
                else break;
            }
            int clusterIndx;
            while (true) {
                //clusterIndex는 startSentence~endSentence(UNIT보정)사이
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", fileId).findFirst();
                RealmList<SentenceRealm> sentences = record.getSentenceRealms();
                SentenceRealm sentence = sentences.get(sentenceIndex);
                /***/

                for (int i = 0; i < sentence.getWordList().size(); i++) {
                    WordRealm word = sentence.getWordList().get(i);
                    clusterIndx = getClusterForWord(word, results, err);
                    realm.beginTransaction();
                    ClusterRealm cluster;
                    if (!record.hasCluster(clusterIndx)) {
                        cluster = RealmUtil.createObject(realm, ClusterRealm.class);
                        cluster.setClusterNo(clusterIndx);
                        record.getClusterMembers().add(cluster);
                    } else {
                        cluster = record.getByClusterNo(clusterIndx);
                    }

                    realm.commitTransaction();
                    if (clusterIndx == 0) {//현재 cluster가 silence : pass
                        continue;
                    } else if (sentence.getCluster() == null || sentence.getCluster().getClusterNo() == 0) { // 현재 문장 cluster 미지정 : 현재 clusterNumber등록후 pass
                        realm.beginTransaction();
                        sentence.setCluster(cluster);
                        realm.commitTransaction();
                        continue;
                    } else if (sentence.getCluster().getClusterNo() == clusterIndx) {//현재 cluster와 문장 cluster가 동일 : pas
                        continue;
                    } else { //기존, 현재 cluster가 다를경우 문장 분리

                        realm.beginTransaction();
                        SentenceRealm origin = sentence;
                        SentenceRealm add = RealmUtil.createObject(realm, SentenceRealm.class);

                        add.setEndMillis(origin.getEndMillis());
                        origin.setEndMillis(word.getStartMillis());
                        add.setStartMillis(word.getStartMillis());
                        add.setCluster(cluster);

                        RealmList<WordRealm> originWords = new RealmList<>();
                        RealmList<WordRealm> addWords = new RealmList<>();

                        RealmList<WordRealm> words = sentence.getWordList();
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

                /***/

                //한문장이 끝나면 다음 문장으로
                sentenceIndex++;
                if (sentenceIndex == sentences.size()) break;

            }
        }
        /**************************************************************/

        realm.beginTransaction();
        ClusterDataRealm cluster = realm.createObject(ClusterDataRealm.class, fileId);
        cluster.setClusters(results);

        realm.commitTransaction();
    }


    /**
     * Finishes recognizing speech audio.
     */
    public void finishSpeechRecordingInterval() {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
//        Log.i(TAG, "2 : observer nullified");
    }

    public void stopSpeechRecognizing() {
        if (mVoiceRecorder != null) {
            IS_RECORDING = false;
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

                stopForeground();
            }
        });

    }

    private void stopForeground() {
        if (recordId == -1 && fileId == -1) {
            stopForeground(true);
        }
    }


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


