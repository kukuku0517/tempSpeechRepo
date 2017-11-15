package com.google.cloud.android.speech.view.recordResult;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.cloud.android.speech.data.DTO.MediaTimeDTO;
import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.ClusterDataRealm;
import com.google.cloud.android.speech.data.realm.FeatureRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.data.realm.VectorRealm;
import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;
import com.google.cloud.android.speech.diarization.KMeansCluster;
import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.util.LogUtil;
import com.google.cloud.android.speech.view.customView.CenterLinearLayoutManager;
import com.google.cloud.android.speech.view.recordList.adapter.TagRealmAdapter;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;
import com.google.cloud.android.speech.view.recordResult.handler.ResultHandler;
import com.google.cloud.android.speech.view.recordResult.adapter.ResultRealmAdapter;
import com.google.cloud.android.speech.databinding.ActivityResultBinding;
import com.google.cloud.android.speech.view.recordResult.handler.SpeakerDiaryClickListener;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class RecordResultActivity extends AppCompatActivity implements ResultHandler {
    private final static String TAG = "Speech";
    private Realm realm;
    private RecyclerView mRecyclerView;
    private ResultRealmAdapter mAdapter;
    private String filePath;
    private MediaPlayer mediaPlayer; //TODO release
    private SeekBar seekbar;
    private ObservableDTO<Boolean> isPlaying = new ObservableDTO<>();
    private ObservableDTO<Boolean> diary = new ObservableDTO<>();
    private ObservableDTO<Boolean> loop = new ObservableDTO<>();
    private ActivityResultBinding binding;
    private int pos;
    private MediaTimeDTO timeDTO = new MediaTimeDTO();
    private RecordRealm record;
    private int recordId;

    private RecyclerView tagRecyclerView;
    private TagRealmAdapter tagAdapter;
    private RecyclerView.LayoutManager tagLayout;
    private ArrayList<TagRealm> tagTags = new ArrayList<>();

    private int prev = -1;

    private MediaThread mediaThread = new MediaThread();




    class MediaThread extends Thread {
        @Override
        public void run() {
            while (isPlaying.getValue()) {
                int current = mediaPlayer.getCurrentPosition();
                seekbar.setProgress(current);
            }
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get data
        realm = Realm.getDefaultInstance();
        recordId = getIntent().getIntExtra("id", 1);
        record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();

        //init values from data
        RealmList<SentenceRealm> sentenceResults = record.getSentenceRealms();
        filePath = record.getFilePath();
        timeDTO.setTotal(record.getDuration());
        timeDTO.setNow(0);

        //init values
        loop.setValue(true);
        isPlaying.setValue(false);
        diary.setValue(false);

        //init databinding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("");
        binding.toolbar.setTitleTextColor(getResources().getColor(R.color.naver_green));
        tagRecyclerView = binding.rvTags;
        seekbar = binding.sbNavigate;

        binding.setHandler(this);
        binding.setTime(timeDTO);
        binding.setIsPlaying(isPlaying);
        binding.setLoop(loop);
        binding.setDiary(diary);
        binding.setRecord(new RecordDTO(record));

        //set tag recyclerview
        for (TagRealm tag : record.getTagList()) {
            tagTags.add(tag);
        }
        tagAdapter = new TagRealmAdapter(getBaseContext(), tagTags, new TagHandler() {
            @Override
            public void onClickTag(View v, TagRealm tag) {

            }
        });
        tagAdapter.setHasStableIds(true);
        tagLayout = new LinearLayoutManager(getBaseContext(), LinearLayoutManager.HORIZONTAL, false);
        tagRecyclerView.setAdapter(tagAdapter);
        tagRecyclerView.setLayoutManager(tagLayout);

        //set data recylcerview
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_record_result);
        mAdapter = new ResultRealmAdapter(sentenceResults, true, true, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new CenterLinearLayoutManager(this));
        mRecyclerView.setItemAnimator(null);

        //set mediaplayer
        Uri mUri = Uri.fromFile(new File(filePath));
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setLooping(true); // true:무한반복
            mediaPlayer.setDataSource(this, mUri);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    int total = mp.getDuration();
                    seekbar.setMax(total);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //set seekbar
        seekbar.setPadding(0,0,0,0);
        seekbar.setMinimumHeight(40);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getMax() == progress) {
                    if (!mediaPlayer.isLooping()) {
                        mediaPlayer.stop();
                        isPlaying.setValue(false);
                        seekBar.setProgress(0);
                    } else {
                        mediaPlayer.seekTo(0);
                        mAdapter.focus(0);
                        mRecyclerView.smoothScrollToPosition(0);
                    }
                } else {
//                    if (record.getCluster().size() != 0) {
//                        int[] clusterArray = new int[record.getCluster().size()];
//
//                        int count = 0;
//                        for (IntegerRealm i : record.getCluster()) {
//                            clusterArray[count] = i.get();
//                            count++;
//                        }
//
//                        int index = (int) (progress / 0.01 / 1000);
//
//                        if (index < clusterArray.length && clusterArray[index] != prev) {
//                            switch (clusterArray[index]) {
//                                case 0:
//                                    binding.toolbar.setBackgroundColor(Color.WHITE);
//                                    break;
//                                case 1:
//                                    binding.toolbar.setBackgroundColor(Color.BLUE);
//                                    break;
//                                case 2:
//                                    binding.toolbar.setBackgroundColor(Color.RED);
//                                    break;
//                            }
//                            prev = clusterArray[index];
//                        }
//                    }

                    timeDTO.setNow(progress);
                    RealmList<SentenceRealm> sentences = record.getSentenceRealms();
                    for (int i = 0; i < sentences.size(); i++) {
                        if (mAdapter.getFocus() != i && sentences.get(i).getStartMillis() <= progress && progress < sentences.get(i).getEndMillis()) {
                            mAdapter.focus(i);
                            mRecyclerView.stopScroll();
                            mRecyclerView.smoothScrollToPosition(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mediaPlayer.pause();
                mediaThread.interrupt();
                seekBar.getProgress();

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int time = seekBar.getProgress();
                mediaPlayer.seekTo(time);
                if (isPlaying.getValue()) {
                    mediaPlayer.start();
                    mediaThread = new MediaThread();
                    mediaThread.start();
                }
            }
        });


    }


    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPlaying.setValue(false); // 쓰레드 정지
        if (mediaPlayer != null) {
            mediaPlayer.release(); // 자원해제
        }

        EventBus.getDefault().unregister(this);
    }

    /**
     * EventBus
     **************************************************/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPartialEvent(SeekEvent event) {
        mediaPlayer.seekTo((int) event.getMillis());
    }

    /**
     * ResultHandler
     **************************************************/
    @Override
    public void onClickStart(View v) {
        // MediaPlayer 객체 초기화 , 재생
        isPlaying.setValue(true); // 씨크바 쓰레드 반복 하도록

        mediaPlayer.start(); // 노래 재생 시작
        int a = mediaPlayer.getDuration(); // 노래의 재생시간(miliSecond)
        seekbar.setMax(a);// 씨크바의 최대 범위를 노래의 재생시간으로 설정
        mediaThread = new MediaThread();
        mediaThread.start(); // 씨크바 그려줄 쓰레드 시작

    }

    @Override
    public void onClickStop(View v) {
        pos = mediaPlayer.getCurrentPosition();
        mediaPlayer.pause(); // 일시중지
        isPlaying.setValue(false); // 쓰레드 정지
    }

    @Override
    public void onClickBack(View v) {
        int i = mAdapter.getFocus();
        if (i != 0) {
            i--;
        }
        int progress = (int) record.getSentenceRealms().get(i).getStartMillis();
        mAdapter.focus(i);
        mRecyclerView.stopScroll();
        mRecyclerView.smoothScrollToPosition(i);
        timeDTO.setNow(progress);
        seekbar.setProgress(progress);
        mediaPlayer.seekTo(progress);

    }

    @Override
    public void onClickForward(View v) {
        int i = mAdapter.getFocus();
        int progress;
        if (i != record.getSentenceRealms().size() - 1) {
            i++;
            progress = (int) record.getSentenceRealms().get(i).getStartMillis();
            mAdapter.focus(i);
        } else {
            progress = (int) record.getSentenceRealms().get(i).getEndMillis();
        }

        mRecyclerView.stopScroll();
        mRecyclerView.smoothScrollToPosition(i);
        timeDTO.setNow(progress);
        seekbar.setProgress(progress);
        mediaPlayer.seekTo(progress);
    }


    @Override
    public void onClickLoop(View v) {
        loop.setValue(!loop.getValue());
        mediaPlayer.setLooping(loop.getValue());

    }

    int[] results;

    @Override
    public void onClickDiary(View v) {
        diary.setValue(true);
        binding.executePendingBindings();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getDefaultInstance();
                FeatureRealm feature = realm.where(FeatureRealm.class).equalTo("id", recordId).findFirst();

                int[] sil = new int[feature.getSilence().size()];
                for (int i = 0; i < feature.getSilence().size(); i++) {
                    sil[i] = feature.getSilence().get(i).get();
                }
                LogUtil.print(sil, "silence");

                KMeansCluster cluster = new KMeansCluster(3, dimension, feature.getFeatureVectors(), feature.getSilence());
//                KMeansCluster cluster = new KMeansCluster(3,dimension*2,getMidTermFeatureVectors(feature.getFeatureVectors()),feature.getSilence());
                cluster.setListener(new SpeakerDiaryClickListener() {
                    @Override
                    public void onSpeakerDiaryComplete() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                diary.setValue(false);
                                Toast.makeText(getBaseContext(), R.string.speaker_complete, Toast.LENGTH_SHORT).show();
                                Realm realm = Realm.getDefaultInstance();
                                realm.beginTransaction();
                                record.setCluster(results);
                                realm.commitTransaction();
                            }
                        });
                    }
                });
                try {
                    results = cluster.iterRun(20);
                    cluster.applyClusterToRealm(3, results, recordId, 0.01f);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }

    @Override
    public void onClickDelete(View v) {

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    String filePath = record.getFilePath();
                    record.cascadeDelete();
                    FileUtil.deleteFile(getBaseContext(), filePath);
                }


            });

            onBackPressed();

    }


    private static final float shortWindowSize = 0.025f;
    private static final float shortWindowStep = 0.010f;
    private static final float midWindowSize = 2.0f;
    private static final float midWindowStep = 0.2f;
    private static final int dimension = 21;

    public double[][] getMidTermFeatureVectors(RealmList<VectorRealm> shortTermFeatureVectors) {

        int windowRatio = (int) (midWindowSize / shortWindowStep);
        int stepRatio = (int) (midWindowStep / shortWindowStep);
        int len = shortTermFeatureVectors.size();
        int count = 0;
        int noOfFrames = (int) Math.ceil(len / stepRatio);
        double[][] mtFeature = new double[noOfFrames][dimension * 2];
        for (int i = 0; i < noOfFrames; i++) {
            Arrays.fill(mtFeature[i], 0.0d);
        }
        int start;
        int end;
        int sLen;

        double stFeatures[][] = new double[len][dimension];
        double stFeaturesSquare[][] = new double[len][dimension];

        double temp;
        for (int i = 0; i < len; i++) {
            VectorRealm vectorRealm = shortTermFeatureVectors.get(i);
            for (int j = 0; j < dimension; j++) {
                temp = vectorRealm.getFeatureVector().get(j).get();
                stFeatures[i][j] = temp;
                stFeaturesSquare[i][j] = temp * temp;
            }
        }

        for (int i = 0; i < noOfFrames; i++) {
            start = count;
            end = count + windowRatio;
            if (end > len - 1) end = len - 1;
            sLen = end - start;

            for (int j = start; j < end; j++) {
                for (int k = 0; k < dimension; k++) {
                    mtFeature[i][k] += stFeatures[j][k] / sLen;
                    mtFeature[i][k + dimension] += Math.pow(stFeatures[j][k], 2) / sLen;
                }
            }

            for (int k = 0; k < dimension; k++) {
                mtFeature[i][k + dimension] = Math.sqrt(mtFeature[i][k + dimension] - Math.pow(mtFeature[i][k], 2));
            }

            count += stepRatio;
        }

        return mtFeature;
    }

}


