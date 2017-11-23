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
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.cloud.android.speech.data.DTO.MediaTimeDTO;
import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.databinding.ActivityRecordBinding;
import com.google.cloud.android.speech.databinding.ItemRecordBinding;
import com.google.cloud.android.speech.event.PartialRealtimeTextEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.event.PartialStatusEvent;
import com.google.cloud.android.speech.event.PartialRecordEvent;
import com.google.cloud.android.speech.event.RecordEndEvent;
import com.google.cloud.android.speech.view.recordList.adapter.TagRealmAdapter;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;
import com.google.cloud.android.speech.view.customView.rvInteractions.ItemTouchHelperCallBack;
import com.google.cloud.android.speech.view.recording.adapter.RecordRealmAdapter;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.view.recording.handler.RecordHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;


public class RecordActivity extends AppCompatActivity implements MessageDialogFragment.Listener, RecordHandler {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";


    private SpeechService mSpeechService;

    // View references
    private TextView mStatus;
    private TextView mText;
    private RecordRealmAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Context context = this;
    private ImageButton recordBtn, stopBtn;
    private int dirId;
    boolean serviceBinded = false;
    private Realm realm;
    private RecordRealm record;

    private int recordId;

    String title;

    private ActivityRecordBinding activityRecordBinding;
    private ItemRecordBinding currentBinding;

    ArrayList<Integer> tags = new ArrayList<>();
    ObservableDTO<Integer> status = new ObservableDTO<>();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPartialEvent(PartialRealtimeTextEvent event) {
        currentText.setValue(event.getPartial());
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onPartialTimerEvent(PartialRecordEvent event) {
        timeDTO.setTotal(event.getSecond() * 1000);
        EventBus.getDefault().removeStickyEvent(event);
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onPartialStatusEvent(PartialStatusEvent event) {

        status.setValue(event.getStatus());
        if (event.getStatus() == PartialStatusEvent.END) {
            currentText.setValue("");
        }
        EventBus.getDefault().removeStickyEvent(event);
    }


    private RecyclerView tagRecyclerView;
    private TagRealmAdapter tagAdapter;
    private RecyclerView.LayoutManager tagLayout;
    private SeekBar seekBar;
    private ArrayList<TagRealm> tagTags = new ArrayList<>();
    ObservableDTO<Boolean> isPlaying = new ObservableDTO<>();
    ObservableDTO<String> currentText = new ObservableDTO<>();

    ObservableDTO<Boolean> isLoading = new ObservableDTO<>();
    MediaTimeDTO timeDTO = new MediaTimeDTO();
    RecordDTO recordDTO;
    RealmList<SentenceRealm> records = new RealmList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();
        EventBus.getDefault().register(this);

        activityRecordBinding = DataBindingUtil.setContentView(this, R.layout.activity_record);
        tagRecyclerView = activityRecordBinding.rvTags;
        seekBar = activityRecordBinding.sbNavigate;
        //data초기화
        isPlaying.setValue(false);
        isLoading.setValue(false);

        activityRecordBinding.setIsLoading(isLoading);
        activityRecordBinding.setHandler(this);
        activityRecordBinding.setTime(timeDTO);
        activityRecordBinding.setIsPlaying(isPlaying);
        activityRecordBinding.setSentenceString(currentText);
        activityRecordBinding.setPartialStatus(status);

        //view 초기화
        setSupportActionBar(activityRecordBinding.toolbar);
        getSupportActionBar().setTitle("");

        tagAdapter = new TagRealmAdapter(getBaseContext(), tagTags, new TagHandler() {
            @Override
            public void onClickTag(View v, TagRealm tag) {

            }
        });

        seekBar.setMax(1);
        tagAdapter.setHasStableIds(true);
        tagLayout = new LinearLayoutManager(getBaseContext(), LinearLayoutManager.HORIZONTAL, false);
        tagRecyclerView.setAdapter(tagAdapter);
        tagRecyclerView.setLayoutManager(tagLayout);


        mRecyclerView = activityRecordBinding.recyclerView;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new RecordRealmAdapter(null, recordId, true, true, context);
        mRecyclerView.setAdapter(mAdapter);
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallBack(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);


        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);

        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setMinimumHeight(40);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //data setting

        title = getIntent().getStringExtra("title");
        tags = Parcels.unwrap(getIntent().getParcelableExtra("tags"));
        dirId = getIntent().getIntExtra("dirId", 1);
        speaker = getIntent().getBooleanExtra("speaker", false);
        if (tags != null && tags.size() != 0) {
            for (int i : tags) {
                tagTags.add(realm.where(TagRealm.class).equalTo("id", i).findFirst());
            }
        }
        activityRecordBinding.tvTitle.setText(title);

        if (SpeechService.IS_RECORDING) {
            initService();
            isPlaying.setValue(true);
        }
        //set tag recyclerview


    }

    private boolean speaker = false;

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
                recordId = mSpeechService.createSpeechRecord(title, dirId, tagTags,speaker);
            }

            realm.beginTransaction();
            record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
            realm.commitTransaction();

            recordDTO = new RecordDTO(record);
            dirId = record.getDirectoryId();
            tags = new ArrayList<>();
            tagTags = new ArrayList<>();
            tagTags.addAll(record.getTagList());

            tagAdapter.updateData(tagTags);

            activityRecordBinding.setRecord(recordDTO);
            activityRecordBinding.tvTitle.setText(record.getTitle());
            mAdapter.updateData(record.getSentenceRealms());
//            mAdapter.notifyDataSetChanged();

            String[]
                    PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            if (checkPermissions(context, PERMISSIONS)) {
                if (!SpeechService.IS_RECORDING) {
                    mSpeechService.initSpeechRecognizing(title);
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

    @Override
    protected void onStop() {
        if (serviceBinded && mServiceConnection!=null) {
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


    @Override
    public void onClickStart(View v) {
        initService();
        isPlaying.setValue(true);
    }

    @Override
    public void onClickStop(View v) {
        isLoading.setValue(true);
        activityRecordBinding.executePendingBindings();
        stopVoiceRecorder();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEndEvent(RecordEndEvent event) {

        serviceBinded = false;
        unbindService(mServiceConnection);
        mSpeechService = null;
        isPlaying.setValue(false);

        onBackPressed();
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
        if (requestCode == 1 || requestCode == 2) {
            if (permissions.length == 3 && grantResults.length == 3
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                mSpeechService.initSpeechRecognizing(title);

            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
