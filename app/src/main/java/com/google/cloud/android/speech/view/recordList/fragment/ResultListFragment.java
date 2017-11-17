package com.google.cloud.android.speech.view.recordList.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.view.recordList.adapter.ListRealmAdapter;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.google.cloud.android.speech.view.recordList.handler.ListHandler;
import com.google.cloud.android.speech.event.ProcessIdEvent;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.databinding.FragmentResultListBinding;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmResults;

public class ResultListFragment extends Fragment implements ListHandler {

    RecyclerView recyclerView;
    ListRealmAdapter adapter;
    Realm realm;
    FragmentResultListBinding binding;
    private SpeechService mSpeechService;

    public ResultListFragment() {
        // Required empty public constructor
    }

    private int mPageNumber;

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


    public static ResultListFragment create(int pageNumber) {
        ResultListFragment fragment = new ResultListFragment();
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mSpeechService != null) {
            getActivity().unbindService(mServiceConnection);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt("page");
        realm = ((ListActivity) getActivity()).realm;
        if (realm == null) {
            realm.init(getContext());
            ((ListActivity) getActivity()).realm = Realm.getDefaultInstance();
            realm = ((ListActivity) getActivity()).realm;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(getActivity(), SpeechService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mServiceConnection, getActivity().BIND_AUTO_CREATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_result_list, container, false);
        binding = DataBindingUtil.bind(view);
        binding.setHandler(this);



        RealmResults<RecordRealm> result = realm.where(RecordRealm.class).equalTo("converted", true).equalTo("isOrigin",true).findAll();

        Log.d("lifecycle", "resultlist create view");
//        for (final RecordRealm record : result) {
//            if (record.getDuration() == 0) {
//                String filePath = recRord.getAudioPath();
//                Uri mUri = Uri.fromFile(new File(filePath));
//                //TODO uncomment this
//                try {
//                    MediaPlayer mediaPlayer = new MediaPlayer();
//                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                    mediaPlayer.setDataSource(getActivity(), mUri);
//                    mediaPlayer.prepareAsync();
//                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                        @Override
//                        public void onPrepared(MediaPlayer mp) {
//                            realm.beginTransaction();
//                            record.setDuration(mp.getDuration());
//                            realm.commitTransaction();
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }

//        recyclerView = (RecyclerView) view.findViewById(R.id.rv_record);
        recyclerView = binding.rvRecord;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ListRealmAdapter(result, true, true, getActivity());
        recyclerView.setAdapter(adapter);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessIdEvent(ProcessIdEvent event) {

        Log.d("lifecycle", "list process event");
        if (event.isRecording()) {
            binding.btnRecord.setEnabled(false);
//            ((ProcessListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_process_list)).setRecordItem(event.getRecordId());
        } else {
            binding.btnRecord.setEnabled(true);
        }
        if (event.isFiling()) {
            binding.btnFile.setEnabled(false);
//            ((ProcessListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_process_list)).setFileItem(event.getFileId());
        } else {
            binding.btnFile.setEnabled(true);
        }
    }

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_FILE_AUDIO_PERMISSION = 2;
    private static final int REQUEST_FILE_VIDEO_PERMISSION = 3;


    @Override
    public void onClickFabRecord(View view) {
        ((ListActivity) getActivity()).openDialog(REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onClickFabFile(View view) {
        ((ListActivity) getActivity()).openDialog(REQUEST_FILE_AUDIO_PERMISSION);
    }

    @Override
    public void onClickFabVideo(View view) {
        ((ListActivity) getActivity()).openDialog(REQUEST_FILE_VIDEO_PERMISSION);
    }

}
