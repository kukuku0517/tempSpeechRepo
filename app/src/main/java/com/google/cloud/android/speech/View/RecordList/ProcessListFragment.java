package com.google.cloud.android.speech.View.RecordList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.FileUtil;
import com.google.cloud.android.speech.View.RecordList.Adapter.ListRealmAdapter;
import com.google.cloud.android.speech.View.Recording.Adapter.RecordRealmAdapter;
import com.google.cloud.android.speech.View.Recording.SpeechService;

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
                                Log.d(TAG+"file",filePath);
                                mSpeechService.recognizeFileStream(title, tags, filePath);

                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Log.d(TAG+"file",filePath);

        Intent intent = new Intent(getActivity(), SpeechService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mServiceConnection, getActivity().BIND_AUTO_CREATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_process_list, container, false);

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
