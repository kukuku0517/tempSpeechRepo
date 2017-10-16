/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.speech.View.Recording;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.RealmUtil;
import com.google.cloud.android.speech.View.RecordList.NewRecordDialog;
import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.View.Recording.Adapter.RecordRealmAdapter;

import java.util.ArrayList;
import java.util.StringTokenizer;

import io.realm.Realm;


public class RecordActivity extends AppCompatActivity implements MessageDialogFragment.Listener, NewRecordDialog.NewRecordDialogListener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_FILE_AUDIO_PERMISSION = 2;


//    private SpeechService mSpeechService;
//    private VoiceRecorder mVoiceRecorder;
//
//

    private SpeechService mSpeechService;
    private VoiceRecorder mVoiceRecorder;

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;
    private RecordRealmAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Context context = this;
    private ImageButton recordBtn, stopBtn;

    boolean serviceBinded = false;
    boolean isRecording=false;
    String fileUri;
    private Realm realm;
    private RecordRealm record;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);

            recordId = mSpeechService.getRecordId();
//            mSpeechService.addListener(mSpeechServiceListener);
            mStatus.setVisibility(View.VISIBLE);
            serviceBinded = true;

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Log.d(TAG, "in service" + String.valueOf(recordId));
                    record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();

                }
            });

            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
//        final ArrayList<String> results = savedInstanceState == null ? null :
//                savedInstanceState.getStringArrayList(STATE_RESULTS);
//

            mAdapter = new RecordRealmAdapter(record.getSentenceRealms(), true, true, context);
            mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
//            mSpeechService = null;
            serviceBinded = false;
        }

    };

    private int recordId;

    private void initialize(final int requestCode) {


        Intent intent = new Intent(context, SpeechService.class);
        startService(intent);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

        if (checkPermissions(context, PERMISSIONS)) {

            NewRecordDialog dialog = new NewRecordDialog();
            dialog.setRequestCode(requestCode);
            dialog.show(getSupportFragmentManager(), "NewRecordDialogFragment");


        } else if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                Manifest.permission.RECORD_AUDIO) || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS,
                    requestCode);
        }
    }

    private boolean checkPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION || requestCode == REQUEST_FILE_AUDIO_PERMISSION) {
            if (permissions.length == 2 && grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                NewRecordDialog dialog = new NewRecordDialog();
                dialog.setRequestCode(requestCode);
                dialog.show(getSupportFragmentManager(), "NewRecordDialogFragment");

            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        fileUri = getIntent().getStringExtra("fileUri");

        realm=Realm.getDefaultInstance();
        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);
        recordBtn = (ImageButton) findViewById(R.id.record);
        stopBtn = (ImageButton) findViewById(R.id.stop);

//        bindService(new Intent(context, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

        if(SpeechService.isRecording){
            initialize(REQUEST_RECORD_AUDIO_PERMISSION);
        }

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initialize(REQUEST_RECORD_AUDIO_PERMISSION);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVoiceRecorder();
                serviceBinded = false;
                // Stop Cloud Speech API
//                mSpeechService.removeListener(mSpeechServiceListener);
                unbindService(mServiceConnection);
                stopService(new Intent(context, SpeechService.class));
                mSpeechService = null;

                onBackPressed();

                //TODO save Duration or get Duration first loading from directory


            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API
//        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
//
//        // Start listening to voices
////        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
////                == PackageManager.PERMISSION_GRANTED) {
////            startVoiceRecorder();
//        String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
//
//        if(checkPermissions(this,PERMISSIONS)){
//            startVoiceRecorder();
//        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                Manifest.permission.RECORD_AUDIO)) {
//            showPermissionMessageDialog();
//        } else {
//            ActivityCompat.requestPermissions(this, PERMISSIONS,
//                    REQUEST_RECORD_AUDIO_PERMISSION);
//        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
//        stopVoiceRecorder();

        // Stop Cloud Speech API
//        if (mSpeechService != null) {
//            if (mSpeechService.hasListener()) {
//                mSpeechService.removeListener(mSpeechServiceListener);
//            }
//        }
        if (serviceBinded) {
            unbindService(mServiceConnection);

        }
//        mSpeechService = null;

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        if (mAdapter != null) {
//            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
//        }
    }

    final String TAG = "Speech";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
                initialize(REQUEST_FILE_AUDIO_PERMISSION);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDialogPositiveClick(final String title, String tag, int requestCode) {
        StringTokenizer st = new StringTokenizer(tag);
        final ArrayList<String> tags = new ArrayList<>();
        while (st.hasMoreTokens()) {
            tags.add(st.nextToken());
        }

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && !isRecording) {
            mSpeechService.initRecorder(title, tags);
        } else if (requestCode == REQUEST_FILE_AUDIO_PERMISSION) {

//            mSpeechService.recognizeFileStream(FileUtil.getFilename(fileUri));
            mSpeechService.recognizeFileStream(title, tags, fileUri);
//            try {
//                FileInputStream fileInputStream = new FileInputStream(file);
//
//                mSpeechService.recognizeInputStream(fileInputStream);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
        }

    }

    @Override
    public void onDialogNegativeClick() {

    }


    private void stopVoiceRecorder() {
//        if (mVoiceRecorder != null) {
//            mVoiceRecorder.stop();
//            mVoiceRecorder = null;
//        }
        mSpeechService.stopRecording();
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

//    private final SpeechService.Listener mSpeechServiceListener =
//            new SpeechService.Listener() {
//                @Override
//                public void onSpeechRecognized(final String text, final boolean isFinal, final long sentenceStart) {
//                    if (isFinal) {
//                        Log.i(TAG, "dismiss");
//                        if (mVoiceRecorder != null) {
//
//                            mVoiceRecorder.dismiss();
//                        }
//                    }
//                    if (mText != null && !TextUtils.isEmpty(text)) {
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
//                    }
//                }
//            };


}
