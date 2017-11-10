package com.google.cloud.android.speech.view.recordResult;

import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import com.google.cloud.android.speech.data.DTO.MediaTimeDTO;
import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.ClusterDataRealm;
import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.view.customView.CenterLinearLayoutManager;
import com.google.cloud.android.speech.view.recordResult.handler.ResultHandler;
import com.google.cloud.android.speech.view.recordResult.adapter.ResultRealmAdapter;
import com.google.cloud.android.speech.databinding.ActivityResultBinding;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmList;

public class RecordResultActivity extends AppCompatActivity implements ResultHandler {
    private final static String TAG = "Speech";
    private Realm realm;
    private RecyclerView mRecyclerView;
    private ResultRealmAdapter mAdapter;
    private String filePath;
    private MediaPlayer mediaPlayer; //TODO release
    private SeekBar seekbar;
    private ObservableDTO<Boolean> isPlaying = new ObservableDTO<>();
    private ObservableDTO<Boolean> loop = new ObservableDTO<>();
    private ActivityResultBinding binding;
    private int pos;
    private MediaTimeDTO timeDTO = new MediaTimeDTO();
    private ClusterDataRealm cluster;
    private int[] clusterArray;
    private RecordRealm record;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.result, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        String filePath = record.getFilePath();
                        record.cascadeDelete();
                        FileUtil.deleteFile(getBaseContext(), filePath);
                    }


                });
                onBackPressed();
                break;
        }
        return true;
    }

    MediaThread mediaThread = new MediaThread();

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
        int itemId = getIntent().getIntExtra("id", 1);
        isPlaying.setValue(false);
        loop.setValue(true);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(getResources().getColor(R.color.naver_green));
        binding.setHandler(this);
        binding.setTime(timeDTO);
        binding.setIsPlaying(isPlaying);
        binding.setLoop(loop);


        seekbar = binding.sbNavigate;


        realm = Realm.getDefaultInstance();
        record = realm.where(RecordRealm.class).equalTo("id", itemId).findFirst();
        cluster = realm.where(ClusterDataRealm.class).equalTo("id", record.getId()).findFirst();
        clusterArray = cluster.getClustersArray();

        binding.setRecord(new RecordDTO(record));

        getSupportActionBar().setTitle("");
        filePath = record.getFilePath();
        RealmList<SentenceRealm> sentenceResults = record.getSentenceRealms();
        timeDTO.setTotal(record.getDuration());
        timeDTO.setNow(0);

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


        mRecyclerView = (RecyclerView) findViewById(R.id.rv_record_result);
        mAdapter = new ResultRealmAdapter(sentenceResults, true, true, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new CenterLinearLayoutManager(this));
        mRecyclerView.setItemAnimator(null);

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
                } else {//                    int index = (int) (progressObs/0.01/1000);
//                    if(index<clusterArray.length){
//                        if(temp!=clusterArray[index] && clusterArray[index]!=0){
//                            Log.d("changedIndex",progressObs+":"+temp+">"+clusterArray[index]);
//                            temp=clusterArray[index];
//                        }
//                        switch(clusterArray[index]){
//                            case 0:
//                                binding.toolbar.setBackgroundColor(Color.WHITE);
//                                break;
//                            case 1:
//                                binding.toolbar.setBackgroundColor(Color.BLUE);
//                                break;
//                            case 2:
//                                binding.toolbar.setBackgroundColor(Color.RED);
//                                break;
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

}
