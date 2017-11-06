package com.google.cloud.android.speech.view.recordResult;

import android.databinding.DataBindingUtil;
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

import com.google.cloud.android.speech.data.DTO.MediaTimeDTO;
import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.view.recordResult.handler.ResultHandler;
import com.google.cloud.android.speech.view.recordResult.adapter.ResultRealmAdapter;
import com.google.cloud.android.speech.databinding.ActivityResultBinding;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

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

    ObservableDTO<Boolean> isPlaying = new ObservableDTO<>();

    private ActivityResultBinding binding;
    int pos;

    MediaTimeDTO timeDTO = new MediaTimeDTO();

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

    class MediaThread extends Thread {
        @Override
        public void run() {
            while (isPlaying.getValue()) {
                int current = mediaPlayer.getCurrentPosition();
                seekbar.setProgress(current);
                timeDTO.setNow(current);
            }
        }
    }

    @Override
    public void onClickStart(View v) {
        // MediaPlayer 객체 초기화 , 재생
        isPlaying.setValue(true); // 씨크바 쓰레드 반복 하도록
        mediaPlayer.setLooping(false); // true:무한반복
        mediaPlayer.start(); // 노래 재생 시작
        int a = mediaPlayer.getDuration(); // 노래의 재생시간(miliSecond)
        seekbar.setMax(a);// 씨크바의 최대 범위를 노래의 재생시간으로 설정
        new MediaThread().start(); // 씨크바 그려줄 쓰레드 시작


    }

    @Override
    public void onClickStop(View v) {
        pos = mediaPlayer.getCurrentPosition();
        mediaPlayer.pause(); // 일시중지
        isPlaying.setValue(false); // 쓰레드 정지
    }

    @Override
    public void onClickRestart(View v) {
        pos = mediaPlayer.getCurrentPosition();
        mediaPlayer.start();
        isPlaying.setValue(true);// 쓰레드 정지
    }

    private RecordRealm record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        int itemId = getIntent().getIntExtra("id", 1);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(getResources().getColor(R.color.primary));
        binding.setHandler(this);
        binding.setTime(timeDTO);
        binding.setIsPlaying(isPlaying);
        seekbar = binding.sbNavigate;
        isPlaying.setValue(false);


        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getMax() == progress) {

                    mediaPlayer.stop();
                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isPlaying.setValue(false);
                mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                isPlaying = true;
                isPlaying.setValue(true);
                int time = seekBar.getProgress();
                mediaPlayer.seekTo(time);
//                mediaPlayer.start();
//                new MediaThread().start();

            }
        });
        realm = Realm.getDefaultInstance();
        record = realm.where(RecordRealm.class).equalTo("id", itemId).findFirst();
        getSupportActionBar().setTitle(record.getTitle());

        filePath = record.getFilePath();
        RealmList<SentenceRealm> sentenceResults = record.getSentenceRealms();


        timeDTO.setTotal(record.getDuration());
        timeDTO.setNow(0);


        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        File mFile = new File(filePath);
        long fLength = mFile.length();
        char[] buffer = new char[(int) (fLength - 44)];





        try {
            br.read(buffer, 0, (int) (fLength - 44));
            ShortBuffer sbuf = ByteBuffer.wrap(new String(buffer).getBytes(StandardCharsets.UTF_8)).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            short[] audioShorts = new short[sbuf.capacity()];
            sbuf.get(audioShorts);
            for(short s:audioShorts){

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Uri mUri = Uri.fromFile(new File(filePath));
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(this, mUri);

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    int total = mp.getDuration();
//                    isPlaying.setValue(true);
                    seekbar.setMax(total);

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


        mRecyclerView = (RecyclerView) findViewById(R.id.rv_record_result);
        mAdapter = new ResultRealmAdapter(sentenceResults, true, true, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));


//        mAdapter.setOnItemClickListener(new MyItemClickListener() {
//            @Override
//            public void onClick(View view, int position) {
//                long time = record.getSentenceRealms().get(position).getStartMillis();
//                mediaPlayer.seekTo((int) time);
//            }
//        });


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPartialEvent(SeekEvent event) {
        mediaPlayer.seekTo((int) event.getMillis());
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

}
