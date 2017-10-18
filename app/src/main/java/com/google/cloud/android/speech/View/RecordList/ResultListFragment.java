package com.google.cloud.android.speech.View.RecordList;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.FileUtil;
import com.google.cloud.android.speech.View.RecordList.Adapter.ListRealmAdapter;

import java.io.File;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class ResultListFragment extends Fragment {

    RecyclerView recyclerView;
    ListRealmAdapter adapter;
    Realm realm;

    public ResultListFragment() {
        // Required empty public constructor
    }

    private int mPageNumber;

    public static ResultListFragment create(int pageNumber) {
        ResultListFragment fragment = new ResultListFragment();
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt("page");

        realm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_result_list, container, false);

        RealmResults<RecordRealm> result = realm.where(RecordRealm.class).equalTo("converted", true).findAll();

        for (final RecordRealm record : result) {
            if (record.getDuration() == 0) {
                String filePath = FileUtil.getFilename(record.getTitle());
                Uri mUri = Uri.fromFile(new File(filePath));
                //TODO uncomment this
                try {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setDataSource(getActivity(), mUri);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            realm.beginTransaction();
                            record.setDuration(mp.getDuration());
                            realm.commitTransaction();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        recyclerView = (RecyclerView) view.findViewById(R.id.rv_record);
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


}
