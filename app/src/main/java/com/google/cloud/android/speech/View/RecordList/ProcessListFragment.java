package com.google.cloud.android.speech.View.RecordList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.Data.DTO.RecordDTO;
import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.FileUtil;
import com.google.cloud.android.speech.View.RecordList.Adapter.ListRealmAdapter;
import com.google.cloud.android.speech.View.RecordList.Adapter.ProcessEvent;
import com.google.cloud.android.speech.View.Recording.Adapter.RecordRealmAdapter;
import com.google.cloud.android.speech.View.Recording.SpeechService;
import com.google.cloud.android.speech.databinding.FragmentProcessListBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;


public class ProcessListFragment extends Fragment {

    String TAG = "Speech";

    public ProcessListFragment() {
        // Required empty public constructor
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG + "file", filePath);
                                mSpeechService.createFileRecord();
                                mSpeechService.recognizeFileStream(title, tags, filePath);

                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }

    };

    private int mPageNumber;
    private static ProcessListFragment instance;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        mPageNumber = getArguments().getInt("page");
        EventBus.getDefault().register(this);

        Log.i("event", "register");
        Log.i("event", "oncreate");


    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            Log.i("event", "register");
        }
        Log.i("event", "onresume");
    }

    SpeechService mSpeechService;

    private String title;
    private ArrayList<String> tags;
    private String filePath;

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
        this.mSpeechService = ((ListActivity) getActivity()).mSpeechService;
        this.mSpeechService.createFileRecord();
        this.mSpeechService.recognizeFileStream(title, tags, filePath);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessEvent(ProcessEvent event) {

        EventBus.getDefault().removeStickyEvent(event);
        realm.beginTransaction();
        if (event.getType()==ProcessEvent.FILE) {
            file.setRealm(realm.where(RecordRealm.class).equalTo("id", event.getId()).findFirst());
        }
        if (event.getType()==ProcessEvent.RECORD) {
            record.setRealm(realm.where(RecordRealm.class).equalTo("id", event.getId()).findFirst());
        }
        realm.commitTransaction();

    }

    private RecordDTO record = new RecordDTO();
    private RecordDTO file = new RecordDTO();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_process_list, container, false);
        View view = binding.getRoot();
        binding.includeRecord.setRecord(record);
        binding.includeFile.setRecord(file);
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
        Log.i("event", "unregister");
    }
}
