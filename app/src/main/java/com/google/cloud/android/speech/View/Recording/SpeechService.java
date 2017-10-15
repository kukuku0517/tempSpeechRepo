package com.google.cloud.android.speech.View.Recording;

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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.RealmUtil;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

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
import io.grpc.internal.IoUtils;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;
import io.realm.Realm;

public class SpeechService extends Service {
    private Realm realm;
    private RecordRealm record;
    private VoiceRecorder mVoiceRecorder;

    public interface Listener {

        /**
         * Called when a new piece of text was recognized by the Speech API.
         *
         * @param text    The text.
         * @param isFinal {@code true} when the API finished processing audio.
         */
        void onSpeechRecognized(String text, boolean isFinal, long startMillis);

    }

    private final Listener mSpeechServiceListener =
            new Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal, final long sentenceStart) {
                    if (isFinal) {
                        Log.i(TAG, "dismiss");
                        if (mVoiceRecorder != null) {
                            mVoiceRecorder.dismiss();
                        }
                    }
                    //mText != null &&
                    if (!TextUtils.isEmpty(text)) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (isFinal) {
//                                    Log.i(TAG, "** final");
//                                    mText.setText(null);
//
//                                    realm.beginTransaction();
//                                    long recordStart = record.getStartMillis();
//                                    if (recordStart == -1) {
//                                        record.setStartMillis(sentenceStart);
//                                        recordStart = sentenceStart;
//                                    }
//
//                                    SentenceRealm sentence = new RealmUtil<SentenceRealm>().createObject(realm, SentenceRealm.class);
//                                    sentence.setStartMillis(sentenceStart - recordStart);
//                                    StringTokenizer st = new StringTokenizer(text);
//                                    while (st.hasMoreTokens()) {
//                                        String token = st.nextToken();
//                                        WordRealm word = new RealmUtil<WordRealm>().createObject(realm, WordRealm.class);
//                                        word.setWord(token);
//                                        sentence.getWordList().add(word);
//                                    }
//                                    record.getSentenceRealms().add(sentence);
//
//
//                                    realm.commitTransaction();
//
//                                    mRecyclerView.smoothScrollToPosition(0);
//                                } else {
//                                    mText.setText(text);
//                                }
//                            }
//                        });


                            if (isFinal) {
                                Log.i(TAG, "** final");

                                realm.beginTransaction();
                                long recordStart = record.getStartMillis();
                                if (recordStart == -1) {
                                    record.setStartMillis(sentenceStart);
                                    recordStart = sentenceStart;
                                }

                                SentenceRealm sentence = new RealmUtil<SentenceRealm>().createObject(realm, SentenceRealm.class);
                                sentence.setStartMillis(sentenceStart - recordStart);
                                StringTokenizer st = new StringTokenizer(text);
                                while (st.hasMoreTokens()) {
                                    String token = st.nextToken();
                                    WordRealm word = new RealmUtil<WordRealm>().createObject(realm, WordRealm.class);
                                    word.setWord(token);
                                    sentence.getWordList().add(word);
                                }
                                record.getSentenceRealms().add(sentence);

                                realm.commitTransaction();

//                                mRecyclerView.smoothScrollToPosition(0);
                            } else { //not final -> send temp text to record activity
//                                mText.setText(text);
                            }

                    }

                }
            };

    public void initRecorder(final String title, final ArrayList<String> tags) {

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                record = new RealmUtil<RecordRealm>().createObject(realm, RecordRealm.class);
                        record.setTitle(title);
                        record.setTagList(tags);
                    }
                });


        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.setTitle(record.getTitle());
        mVoiceRecorder.start();

    }

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart(long startMillis) {
//            showStatus(true);
            startRecognizing(mVoiceRecorder.getSampleRate(), startMillis);

        }

        @Override
        public void onVoice(byte[] data, int size) {
            recognize(data, size);

            Log.i(TAG,"still alive");

        }

        @Override
        public void onVoiceEnd() {

            finishRecognizing();

        }

    };


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

    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;

    private final SpeechBinder2 mBinder = new SpeechBinder2();
    private final ArrayList<SpeechService.Listener> mListeners = new ArrayList<>();
    private volatile AccessTokenTask mAccessTokenTask;
    private SpeechGrpc.SpeechStub mApi;
    private static Handler mHandler;

    private long startMillis;

    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver
            = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse response) {
            Log.i(TAG, "answer received");
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
                for (SpeechService.Listener listener : mListeners) {
                    listener.onSpeechRecognized(text, isFinal, startMillis);
                    Log.i(TAG, text);
                }
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

    private final StreamObserver<RecognizeResponse> mFileResponseObserver
            = new StreamObserver<RecognizeResponse>() {
        @Override
        public void onNext(RecognizeResponse response) {
            String text = null;
            if (response.getResultsCount() > 0) {
                final SpeechRecognitionResult result = response.getResults(0);
                if (result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                    text = alternative.getTranscript();
                }
            }
            if (text != null) {
                for (SpeechService.Listener listener : mListeners) {
                    listener.onSpeechRecognized(text, true, 0);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the API.", t);
        }

        @Override
        public void onCompleted() {
//            Log.i(TAG, "API completed.");
        }

    };

    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    public static SpeechService from(IBinder binder) {
        return ((SpeechBinder2) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        realm = Realm.getDefaultInstance();
        mHandler = new Handler();
        this.addListener(mSpeechServiceListener);
        fetchAccessToken();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG,"dead");
        this.removeListener(mSpeechServiceListener);
        mHandler.removeCallbacks(mFetchAccessTokenRunnable);
        mHandler = null;
        // Release the gRPC channel.
        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e);
                }
            }
            mApi = null;
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


    public void addListener(@NonNull SpeechService.Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@NonNull SpeechService.Listener listener) {
        mListeners.remove(listener);
    }

    public boolean hasListener() {
        return mListeners != null;
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


        this.startMillis = startMillis;

        // Configure the API
        mRequestObserver = mApi.streamingRecognize(mResponseObserver);

        Log.i(TAG, "1 : observer created");
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(getDefaultLanguageCode())
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(false)
                        .build())
                .build());
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
        Log.i(TAG, "buffer sent");
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
        Log.i(TAG, "2 : observer nullified");
    }

    public void stopRecording(){
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
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


//    public void recognizeInputStream(InputStream stream) {
//        try {
//            mApi.recognize(
//                    RecognizeRequest.newBuilder()
//                            .setConfig(RecognitionConfig.newBuilder()
//                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                                    .setLanguageCode(getDefaultLanguageCode())
//                                    .setSampleRateHertz(16000)
//                                    .build())
//                            .setAudio(RecognitionAudio.newBuilder()
//                                    .setContent(ByteString.readFrom(stream))
//                                    .build())
//                            .build(),
//                    mFileResponseObserver);
//        } catch (IOException e) {
//            Log.e(TAG, "Error loading the input", e);
//        }
//
//
//    }


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

    public void recognizeFileStream(final String title, final ArrayList<String> tags, String fileName) {
        try {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    record = new RealmUtil<RecordRealm>().createObject(realm, RecordRealm.class);
                    record.setTitle(title);
                    record.setTagList(tags);
                }
            });

            FileInputStream stream = new FileInputStream(new File(fileName));

            getValidSampleRates();

            byte[] data = IoUtils.toByteArray(new FileInputStream(new File(fileName)));

            mRequestObserver = mApi.streamingRecognize(mResponseObserver);
            createAudioRecord();
            getValidSampleRates();

            Log.i(TAG, "1 : observer created");
            mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                            .setConfig(RecognitionConfig.newBuilder()
                                    .setLanguageCode(getDefaultLanguageCode())
                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                    .setSampleRateHertz(16000)
                                    .build())
                            .setInterimResults(true)
                            .setSingleUtterance(false)
                            .build())
                    .build());

            while (true) {
                if (stream.read(mBuffer, 0, bufferSize) == -1) {
                    break;
                }

                mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(mBuffer))
                        .build());
            }
            int a = 0;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}


