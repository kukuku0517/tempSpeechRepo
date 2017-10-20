package com.google.cloud.android.speech.View.Recording.Background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.NetworkStats;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
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
import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.Event.PartialEvent;
import com.google.cloud.android.speech.Event.PartialTimerEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.AudioUtil;
import com.google.cloud.android.speech.Util.RealmUtil;
import com.google.cloud.android.speech.View.RecordList.ListActivity;
import com.google.cloud.android.speech.Event.ProcessIdEvent;
import com.google.cloud.speech.v1.LongRunningRecognizeRequest;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
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
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;
import io.realm.Realm;

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
    private SpeechGrpc.SpeechStub mApi2;
    private static Handler mHandler;

    private long startOfCall = -1;
    private long startOfRecording = -1;

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart(long startMillis) {
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
            stopRecognizing(RECORD);
            notifyProcess();

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
//                onFileRecognized(text, isFinal, startOfCall, recordId);
                onSpeechRecognized(isFinal, response.getResults(0).getAlternatives(0),startOfCall);
            }
        }


        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "API completed.");
        }

    };
    private final StreamObserver<StreamingRecognizeResponse> mFileResponseObserver
            = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            Log.i(TAG, "answer received");
            String text = null;
            boolean isFinal = false;
        SpeechRecognitionAlternative alternative=null;
            if (response.getResultsCount() > 0) {
                final StreamingRecognitionResult result = response.getResults(0);
                isFinal = result.getIsFinal();
                if (result.getAlternativesCount() > 0) {
                    alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                Log.i(TAG + "file", text);
//                for (SpeechService.Listener listener : mListeners) {
//                    listener.onFileRecognized(text, isFinal, startMillis);
//                    Log.i(TAG, text);
//                }
                onFileRecognized(text, isFinal, startOfCall, fileId);
                if (isFinal)
                    Log.d(TAG, "timetime" + String.valueOf(response.getResults(0).getAlternatives(0).getWords(0).getStartTime().getSeconds()));
//                EventBus.getDefault().postSticky(new ProcessEvent(fileId,ProcessEvent.FILE));
            }

            if(isFinal){
                long endTime = alternative.getWordsList().get(alternative.getWordsCount()-1).getEndTime().getSeconds()*1000;
                Log.d(TAG,"fileEnd : "+endTime);
                Log.d(TAG,"fileEnd minus : "+Math.abs(endOfFileSecond-endTime));

            }




        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "API completed.");

                stopRecognizing(FILE);
                stopForeground();

        }

    };

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isFileRecognizing() {
        return fileId == -1 ? false : true;
    }

    public int getFileId() {
        return fileId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
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
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                record.setTitle(title);
                record.setTagList(tags);
                recordId = record.getId();
            }
        });
        isRecording = true;
        startOfRecording = System.currentTimeMillis();
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
                record = new RealmUtil<RecordRealm>().createObject(realm, RecordRealm.class);
                recordId = record.getId();
            }
        });
        return recordId;
    }

    public int createFileRecord() {
        realm.beginTransaction();
        fileRecord = new RealmUtil<RecordRealm>().createObject(realm, RecordRealm.class);
        fileId = fileRecord.getId();
        realm.commitTransaction();
        Log.d(TAG, "fileId in craete :" + fileId);
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
    public void startRecognizing(int sampleRate, long startMillis) {
        if (mApi == null) {
            Log.i(TAG, "API not ready. Ignoring the request.");
            return;
        }

        this.startOfCall = startMillis;

        // Configure the API
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
    LongRunningRecognizeRequest request ;



    long endOfFileSecond=-1;

    public void recognizeFileStream(final String title, final ArrayList<String> tags, String fileName) {
        startForeground();
        try {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    fileRecord.setTitle(title);
                    fileRecord.setTagList(tags);
                }
            });


            File file = new File(fileName);
            long fileLenth = file.length();
            Log.d(TAG,"file"+fileName+fileLenth);
            FileInputStream stream = new FileInputStream(file);

            long millSecond = AudioUtil.getAudioLength(getApplicationContext(),fileName);

            Log.d(TAG,"file"+millSecond);

            getValidSampleRates();
            fileStreamFinished = false;
            mFileRequestObserver = mApi.streamingRecognize(mFileResponseObserver);

            createAudioRecord();
            getValidSampleRates();
            mFileRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                            .setConfig(RecognitionConfig.newBuilder()
                                    .setLanguageCode(getDefaultLanguageCode())
                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                    .setSampleRateHertz(16000)
                                    .setEnableWordTimeOffsets(true)

                                    .build())
                            .setInterimResults(true)
                            .setSingleUtterance(false)
                            .build())
                    .build());
            Log.d("buffer", String.valueOf(bufferSize));

            long currentBuffer=0;

            while (true) {

                if (stream.read(mBuffer, 0, bufferSize) == -1) {
                    break;
                }
                currentBuffer+=bufferSize;
                if(AudioUtil.isHearingVoice(mBuffer, bufferSize)){
                    endOfFileSecond=millSecond*currentBuffer/fileLenth;
                    Log.d(TAG,"file"+endOfFileSecond);
                }
                mFileRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(mBuffer))
                        .build());
            }
            mFileRequestObserver.onCompleted();

            fileStreamFinished = true;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the {@code data}.
     */


    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
        // Call the streaming recognition API
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());

//        Log.i(TAG, "buffer sent");
    }

    public void onFileRecognized(final String text, final boolean isFinal, final long startOfCall, int id) {
        if (isFinal) {
            Log.i(TAG, "dismiss");
            if (mVoiceRecorder != null) {
                mVoiceRecorder.dismiss();
            }
        }
        if (!TextUtils.isEmpty(text)) {
            if (isFinal) {
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                RecordRealm record = realm.where(RecordRealm.class).equalTo("id", id).findFirst();
                if (record.getStartMillis() == -1) {
                    record.setStartMillis(startOfRecording);
                }

                SentenceRealm sentence = new RealmUtil<SentenceRealm>().createObject(realm, SentenceRealm.class);
                sentence.setStartMillis((int) (startOfCall - startOfRecording));
                StringTokenizer st = new StringTokenizer(text);
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    WordRealm word = new RealmUtil<WordRealm>().createObject(realm, WordRealm.class);
                    word.setWord(token);
                    sentence.getWordList().add(word);
                }
                record.getSentenceRealms().add(sentence);

                realm.commitTransaction();
            } else { //not final -> send temp text to record activity
//                                mText.setText(text);

                EventBus.getDefault().post(new PartialEvent(text));
            }

        }

    }

    private void onSpeechRecognized(boolean isFinal, SpeechRecognitionAlternative alternative,long startOfCall) {
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
                if (record.getStartMillis() == -1) {
                    record.setStartMillis(startOfRecording);
                }
                SentenceRealm sentence = new RealmUtil<SentenceRealm>().createObject(realm, SentenceRealm.class);
                long startOfSentence = startOfCall - startOfRecording;
                sentence.setStartMillis((int) (startOfSentence));
                for (WordInfo wordInfo : alternative.getWordsList()) {
                    WordRealm word = new RealmUtil<WordRealm>().createObject(realm, WordRealm.class);
                    word.setWord(wordInfo.getWord());
                    word.setStartMillis(wordInfo.getStartTime().getSeconds() * 1000 + startOfSentence);
                    sentence.getWordList().add(word);
                }
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
        realm.beginTransaction();

        int id = type == RECORD ? recordId : fileId;
        Log.d(TAG, "fileId in stop :" + fileId);
        RecordRealm record = realm.where(RecordRealm.class).equalTo("id", id).findFirst();
        record.setConverted(true);
        if (type == RECORD) {
            recordId = -1;
        } else {
            fileId = -1;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), "convert complete", Toast.LENGTH_SHORT).show();

            }
        });

        realm.commitTransaction();
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


