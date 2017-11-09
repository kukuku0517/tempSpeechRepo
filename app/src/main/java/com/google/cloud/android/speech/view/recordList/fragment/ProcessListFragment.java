package com.google.cloud.android.speech.view.recordList.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.event.FileEvent;
import com.google.cloud.android.speech.view.recordList.handler.ProcessItemHandler;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.google.cloud.android.speech.view.recordList.handler.ProcessHandler;
import com.google.cloud.android.speech.event.ProcessIdEvent;
import com.google.cloud.android.speech.event.PartialTimerEvent;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.view.recording.RecordActivity;
import com.google.cloud.android.speech.databinding.FragmentProcessListBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import io.realm.Realm;


public class ProcessListFragment extends Fragment implements ProcessHandler, ProcessItemHandler {

    private static final String TAG = "Speech";
    private static ProcessListFragment instance;

    private RecordDTO record = new RecordDTO();
    private RecordDTO file = new RecordDTO();
    private int mPageNumber;
    private SpeechService mSpeechService;
    private String title;
    private ArrayList<String> tags;
    private String filePath;

    public ProcessListFragment() {
        // Required empty public constructor
    }


    public static ProcessListFragment create(int pageNumber) {
        if (instance == null) {
            instance = new ProcessListFragment();
        }
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        instance.setArguments(args);
        return instance;
    }

    FragmentProcessListBinding binding;
    Realm realm;

    private int recordId;
    private int fileId;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.notifyProcess();
            Log.d("lifecycle", "service con");


            //TODO enable after end


//            recordId = mSpeechService.getRecordId();
////            mSpeechService.addListener(mSpeechServiceListener);
//            mStatus.setVisibility(View.VISIBLE);
//            serviceBinded = true;

//            realm.executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    Log.d(TAG, "in service" + String.valueOf(recordId));
//                    record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
//
//                }
//            });
//
//            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
//            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
////        final ArrayList<String> results = savedInstanceState == null ? null :
////                savedInstanceState.getStringArrayList(STATE_RESULTS);
////
//
//            mAdapter = new RecordRealmAdapter(record.getSentenceRealms(), true, true, context);
//            mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            Log.d("lifecycle", "service discon");
            mSpeechService = null;
        }

    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = ((ListActivity) getActivity()).realm;
        mPageNumber = getArguments().getInt("page");
        EventBus.getDefault().register(this);

        Log.d("lifecycle", "process create");
    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d("lifecycle", "process resume");
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);

        }

        Intent intent = new Intent(getActivity(), SpeechService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        Log.d("lifecycle", "process oncreateview");
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_process_list, container, false);
        View view = binding.getRoot();
        //here data must be an instance of the class MarsDataProvider
        binding.includeRecord.setRecord(record);
        binding.includeFile.setRecord(file);
        binding.includeRecord.setHandler(this);
        binding.includeRecord.setItemHandler(this);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (mSpeechService != null) {
            getActivity().unbindService(mServiceConnection);

            Log.d("lifecycle", "list unbind longrunningRequestRetrofit in stop");
        }

        Log.d("lifecycle", "process stop");
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onMessageEvent(FileEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        title = event.getTitle();
        tags = event.getTags();
        filePath = event.getFilePath();
        Log.d(TAG + "file", filePath);
//
//        Intent intent = new Intent(getActivity(), SpeechService.class);
//        getActivity().startService(intent);
//        getActivity().bindService(intent, mServiceConnection, getActivity().BIND_AUTO_CREATE);

        mSpeechService.createFileRecord();
        mSpeechService.recognizeFileStream(title, tags, filePath);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
//    public void onProcessEvent(ProcessEvent event) {
//
//        realm.beginTransaction();
//        if (event.getType() == ProcessEvent.FILE) {
//            file.setRealm(realm.where(RecordRealm.class).equalTo("id", event.getId()).findFirst());
//        }
//        if (event.getType() == ProcessEvent.RECORD) {
//            record.setRealm(realm.where(RecordRealm.class).equalTo("id", event.getId()).findFirst());
//        }
//
//        realm.commitTransaction();
//        EventBus.getDefault().removeStickyEvent(event);
//
//    }

    public void setRecordItem(int recordId) {
        this.recordId = recordId;
        realm.beginTransaction();
        record.setRealm(realm.where(RecordRealm.class).equalTo("id", recordId).findFirst());
        realm.commitTransaction();

    }

    public void setFileItem(int fileId) {
        this.fileId = fileId;
        realm.beginTransaction();

        file.setRealm(realm.where(RecordRealm.class).equalTo("id", fileId).findFirst());


        realm.commitTransaction();
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onPartialTimerEvent(PartialTimerEvent event) {
//        Log.d(TAG,"eventbus timer end"+event.getSecond());
        record.setDuration(event.getSecond() * 1000);
        if (binding == null && record == null) {
            Log.d(TAG, "eventbus timer end" + event.getSecond());
        } else {
            Log.d(TAG, "eventbus timer end" + event.getSecond());
        }
        EventBus.getDefault().removeStickyEvent(event);
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessIdEvent(ProcessIdEvent event) {

        binding.includeRecord.setIsVisible(event.isRecording());
        binding.includeRecordEmpty.setIsVisible(!event.isRecording());
        binding.includeFile.setIsVisible(event.isFiling());
        binding.includeFileEmpty.setIsVisible(!event.isFiling());

        Log.d("lifecycle", "list process event");
        if (event.isRecording()) {
            setRecordItem(event.getRecordId());
        }
        if (event.isFiling()) {
            setFileItem(event.getFileId());
        }
    }


    @Override
    public void onClickStopRecord(View view) {
        if (mSpeechService == null) {
            this.mSpeechService = ((ListActivity) getActivity()).mSpeechService;
        }

        this.mSpeechService.stopRecording();
        Log.d(TAG, "stop");
//        binding.includeRecord
    }

    @Override
    public void onClickStopFile(View view) {

    }


    @Override
    public void onRecordItemClick(View view) {
        Intent intent = new Intent(getActivity(), RecordActivity.class);
        intent.putExtra("id", recordId);
        getActivity().startActivity(intent);
    }


}
