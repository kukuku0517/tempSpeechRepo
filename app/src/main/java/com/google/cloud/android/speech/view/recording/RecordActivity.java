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

package com.google.cloud.android.speech.view.recording;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.cloud.android.speech.event.PartialEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.view.recording.adapter.RecordRealmAdapter;
import com.google.cloud.android.speech.view.background.SpeechService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import io.realm.Realm;


public class RecordActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";


    private SpeechService mSpeechService;

    // View references
    private TextView mStatus;
    private TextView mText;
    private RecordRealmAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Context context = this;
    private ImageButton recordBtn, stopBtn;

    boolean serviceBinded = false;
    private Realm realm;
    private RecordRealm record;

    private int recordId;

    String title;


    ArrayList<String> tags = new ArrayList<>();


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPartialEvent(PartialEvent event) {
        mText.setText(event.getPartial());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        EventBus.getDefault().register(this);
        title = getIntent().getStringExtra("title");
        tags = getIntent().getStringArrayListExtra("tags");

        realm = Realm.getDefaultInstance();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("");

        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);
        recordBtn = (ImageButton) findViewById(R.id.record);
        stopBtn = (ImageButton) findViewById(R.id.stop);

        if (SpeechService.IS_RECORDING) {
            initService();
        }

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initService();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVoiceRecorder();
                serviceBinded = false;
                unbindService(mServiceConnection);
                stopService(new Intent(context, SpeechService.class));
                mSpeechService = null;
                onBackPressed();
                //TODO save Duration or get Duration first loading from directory
            }
        });
    }

    private void initService() {
        Intent intent = new Intent(context, SpeechService.class);
        startService(intent);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            serviceBinded = true;
            if (SpeechService.IS_RECORDING) {
                recordId = mSpeechService.getRecordId();
            } else {
                recordId = mSpeechService.createSpeechRecord();
            }

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
                }
            });

            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new RecordRealmAdapter(record.getSentenceRealms(), true, true, context);
            mRecyclerView.setAdapter(mAdapter);

            String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            if (checkPermissions(context, PERMISSIONS)) {
                if (!SpeechService.IS_RECORDING) {
                    mSpeechService.initSpeechRecognizing(title, tags);
                }
            } else if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                    Manifest.permission.RECORD_AUDIO) || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showPermissionMessageDialog();
            } else {
                ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, 1);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBinded = false;
        }

    };

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
        if (requestCode == 1 || requestCode == 2) {
            if (permissions.length == 3 && grantResults.length == 3
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                mSpeechService.initSpeechRecognizing(title, tags);

            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStop() {
        if (serviceBinded) {
            unbindService(mServiceConnection);
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void stopVoiceRecorder() {
        mSpeechService.stopSpeechRecognizing();
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }



    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

}
