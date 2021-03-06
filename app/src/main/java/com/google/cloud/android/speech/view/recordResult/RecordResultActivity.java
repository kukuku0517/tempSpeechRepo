package com.google.cloud.android.speech.view.recordResult;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.cloud.android.speech.data.DTO.MediaTimeDTO;
import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.FeatureRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.data.realm.VectorRealm;
import com.google.cloud.android.speech.databinding.DialogAlertBinding;
import com.google.cloud.android.speech.diarization.KMeansCluster;
import com.google.cloud.android.speech.event.ResultMenuClickEvent;
import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.util.LogUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.customView.CenterLinearLayoutManager;
import com.google.cloud.android.speech.view.recordList.adapter.TagRealmAdapter;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;
import com.google.cloud.android.speech.view.customView.rvInteractions.ItemTouchHelperCallBack;
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
import io.realm.RealmChangeListener;
import io.realm.RealmList;

public class RecordResultActivity extends AppCompatActivity implements ResultHandler, SurfaceHolder.Callback {
    private Realm realm;
    private ActivityResultBinding binding;
    private MediaPlayer mediaPlayer; //TODO release
    private RecyclerView mRecyclerView;

    private SeekBar seekbar;
    private RecyclerView tagRecyclerView;
    private TagRealmAdapter tagAdapter;
    private RecyclerView.LayoutManager tagLayout;
    private SurfaceView surfaceView;

    private SurfaceHolder surfaceHolder;
    private ResultRealmAdapter mAdapter;

    private String filePath;
    private boolean hasVideo = false;
    private ObservableDTO<Boolean> isPlaying = new ObservableDTO<>();
    private ObservableDTO<Boolean> diary = new ObservableDTO<>();
    private ObservableDTO<Boolean> loop = new ObservableDTO<>();
    private ObservableDTO<String> progress = new ObservableDTO<>();
    private ObservableDTO<Integer> hasDiary = new ObservableDTO<>();

    private MediaThread mediaThread = new MediaThread();
    private MediaTimeDTO timeDTO = new MediaTimeDTO();

    private int originRecordId;
    private int duplicateRecordId = -1;

    private RecordRealm record;
    private ArrayList<TagRealm> tagTags = new ArrayList<>();
    private int[] results;

    class MediaThread extends Thread {
        @Override
        public void run() {
            while (isPlaying.getValue()) {
                int current = mediaPlayer.getCurrentPosition();
                seekbar.setProgress(current);
            }
        }

    }

    class ClusterAsync extends AsyncTask<Integer, Float, Integer> {

        @Override
        protected Integer doInBackground(final Integer... params) {

            Log.d("loadingloading", String.valueOf(2));
            Realm realm = Realm.getDefaultInstance();
            FeatureRealm feature = realm.where(FeatureRealm.class).equalTo("id", originRecordId).findFirst();

            Log.d("loadingloading", String.valueOf(3));
            KMeansCluster cluster = new KMeansCluster(3, dimension, feature.getFeatureVectors(), feature.getSilence());
            cluster.setListener(new SpeakerDiaryClickListener() {
                @Override
                public void onSpeakerDiaryComplete(int process) {
                    publishProgress(process / (float) params[0]);
                }
            });

            Log.d("loadingloading", String.valueOf(4));
            try {
                results = cluster.iterRun(params[0]);
                realm.beginTransaction();
                if (duplicateRecordId != -1) {
                    RecordRealm dupToDelete = realm.where(RecordRealm.class).equalTo("id", duplicateRecordId).findFirst();
                    dupToDelete.cascadeDelete();
                }
                duplicateRecordId = RealmUtil.duplicateRecord(realm, originRecordId);
                realm.commitTransaction();
                Log.d("loadingloading", String.valueOf(5));
                cluster.applyClusterToRealm(3, results, duplicateRecordId, 0.01f);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return duplicateRecordId;
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
            progress.setValue(String.format("Loading... %.0f%%", values[0] * 100));

        }

        @Override
        protected void onPostExecute(Integer aVoid) {
            diary.setValue(false);
            Toast.makeText(getBaseContext(), R.string.speaker_complete, Toast.LENGTH_SHORT).show();
            Realm realm = Realm.getDefaultInstance();
            swapRecord(aVoid);
            hasDiary.setValue(duplicateRecordId);
            realm.beginTransaction();
            record.setCluster(results);
            realm.commitTransaction();
            binding.tlList.getTabAt(1).select();
        }
    }


    private void swapRecord(int recordId) {
        Realm realm = Realm.getDefaultInstance();
        record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
        mAdapter.updateData(record.getSentenceRealms(), recordId);
    }

    /**
     * only if video path exist.
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Uri mUri = Uri.fromFile(new File(filePath));
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDisplay(surfaceHolder);
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
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get data
        realm = Realm.getDefaultInstance();
        originRecordId = getIntent().getIntExtra("id", 1);

        record = realm.where(RecordRealm.class).equalTo("id", originRecordId).findFirst();
        if (record.getDuplicateId() != -1) {
            duplicateRecordId = record.getDuplicateId();
        }

        FeatureRealm fv = realm.where(FeatureRealm.class).equalTo("id", originRecordId).findFirst();
        if (fv != null) {
//            Log.d("fv", String.valueOf(fv.getFeatureVectors().size()));
//
//            double[][] f = new double[fv.getFeatureVectors().size()][];
//            for(int j=0;j<fv.getFeatureVectors().size();j++){
//                double[] ff = new double[fv.getFeatureVectors().get(j).getFeatureVector().size()];
//                for(int i=0;i<ff.length;i++){
//                    ff[i]=fv.getFeatureVectors().get(j).getFeatureVector().get(i).get();
//                }
//                f[j]=ff;
//            }
//            LogUtil.writeToFileAsWhole(f,"fvresults");
        }


        //init values from data
        RealmList<SentenceRealm> sentenceResults = record.getSentenceRealms();
        timeDTO.setTotal(record.getDuration());
        timeDTO.setNow(0);

        //init values
        loop.setValue(true);
        isPlaying.setValue(false);
        diary.setValue(false);
        progress.setValue("Loading...0%");
        hasDiary.setValue(duplicateRecordId);

        //init databinding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("");
        binding.toolbar.setTitleTextColor(getResources().getColor(R.color.naver_green));
        tagRecyclerView = binding.rvTags;
        seekbar = binding.sbNavigate;

        if (record.getVideoPath() != null) {
            hasVideo = true;
            filePath = record.getVideoPath();
            surfaceView = binding.svVideo;
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);
        } else {
            filePath = record.getAudioPath();
        }

        binding.setHandler(this);
        binding.setTime(timeDTO);
        binding.setIsPlaying(isPlaying);
        binding.setLoop(loop);
        binding.setDiary(diary);
        binding.setProgress(progress);
        binding.setRecord(new RecordDTO(record));

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFabClick(v);
            }
        });
        binding.rvRecordResult.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        if (recyclerView.getVerticalScrollbarPosition()<0||recyclerView.getVerticalScrollbarPosition()>recyclerView.getScrollBarSize()||binding.fab.getVisibility() == View.GONE)
                            binding.fab.show();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        if (binding.fab.getVisibility() == View.VISIBLE)
                            binding.fab.hide();
                        break;

                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
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
        mAdapter = new ResultRealmAdapter(sentenceResults, originRecordId, true, true, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new CenterLinearLayoutManager(this));
        mRecyclerView.setItemAnimator(null);
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallBack(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);
        //set mediaplayer
        if (!hasVideo) {
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
        }
        //set seekbar
        seekbar.setPadding(0, 0, 0, 0);
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


        binding.tlList.getTabAt(0).setIcon(R.drawable.speech_single);
        binding.tlList.getTabAt(0).getIcon().setColorFilter(ContextCompat.getColor(this, R.color.gray), PorterDuff.Mode.SRC_ATOP);

        binding.tlList.getTabAt(1).setIcon(R.drawable.speech_conversation);
        binding.tlList.getTabAt(1).getIcon().setColorFilter(ContextCompat.getColor(this, R.color.gray), PorterDuff.Mode.SRC_ATOP);
        binding.tlList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        onClickViewOrigin(null);
                        mRecyclerView.setAnimation(AnimationUtils.loadAnimation(getBaseContext(), android.R.anim.fade_in));

                        break;
                    case 1:

                        onClickViewDiary(null);
                        mRecyclerView.setAnimation(AnimationUtils.loadAnimation(getBaseContext(), android.R.anim.fade_in));

                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        realm.addChangeListener(new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm realm) {
                RecordRealm origin = realm.where(RecordRealm.class).equalTo("id", originRecordId).findFirst();
                RecordRealm dup = realm.where(RecordRealm.class).equalTo("id", origin.getDuplicateId()).findFirst();
                if (dup != null && duplicateRecordId != dup.getId()) {
                    duplicateRecordId = dup.getId();
                    binding.tlList.getTabAt(1).select();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEndEvent(ResultMenuClickEvent event) {
        mAdapter.setEditMode();
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

    @Override
    public void onClickDiary(View v) {
        RecordRealm temp = realm.where(RecordRealm.class).equalTo("id",originRecordId).findFirst();

        if (temp.isSpeaker()){

            final DialogAlertBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_alert, null, false);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(binding.getRoot());

            binding.setTitle("화자 인식");
            binding.setCategory("화자 인식을 시작합니다.\n (조금 걸릴 수도 있습니다)");
            final Dialog dialog = builder.create();
            dialog.show();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.findViewById(R.id.rl_confirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    diary.setValue(true);
                    binding.executePendingBindings();
                    dialog.dismiss();
                    new ClusterAsync().execute(10);
                }
            });
            dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }else{
            Toast.makeText(this, "화자 인식 데이터가 없습니다", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onClickDelete(View v) {


        final DialogAlertBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_alert, null, false);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(binding.getRoot());

        binding.setTitle("삭제");
        binding.setCategory("정말 삭제하시겠습니까?");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.findViewById(R.id.rl_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        String filePath = record.getAudioPath();
                        record.cascadeDelete();
                        onBackPressed();

//                FileUtil.deleteFile(getBaseContext(), filePath);
                    }


                });
            }
        });
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


    }

    @Override
    public void onClickTitle(View v) {

    }

    @Override
    public void onClickViewOrigin(View v) {
        swapRecord(originRecordId);
    }

    @Override
    public void onClickViewDiary(View v) {
        if (duplicateRecordId == -1) {
            onClickDiary(v);
            binding.tlList.getTabAt(0).select();
        } else {
            swapRecord(duplicateRecordId);
        }
    }

    @Override
    public void onFabClick(View v) {
        mAdapter.setEditMode();
        if (mAdapter.isEditMode()) {
            ((FloatingActionButton) v).setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close_black_24dp));
        } else {
            ((FloatingActionButton) v).setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_edit_black_24dp));
        }
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


