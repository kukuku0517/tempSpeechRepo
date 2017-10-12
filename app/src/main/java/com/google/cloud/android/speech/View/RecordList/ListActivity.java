package com.google.cloud.android.speech.View.RecordList;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Util.FileUtil;
import com.google.cloud.android.speech.View.RecordList.Adapter.RecordListAdapter;
import com.google.cloud.android.speech.View.Recording.RecordActivity;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.databinding.ActivityListBinding;


import java.io.File;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;

public class ListActivity extends AppCompatActivity implements ListHandler {

    RecyclerView recyclerView;
    RecordListAdapter adapter;
    Realm realm;
    ActivityListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
        binding.setHandler(this);
        realm.init(this);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(config);

        realm = Realm.getDefaultInstance();
        RealmResults<RecordRealm> result = realm.where(RecordRealm.class).findAll();
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        for (final RecordRealm record : result) {
            if (record.getDuration()==0) {
                String filePath = FileUtil.getFilename(record.getTitle());
                Uri mUri = Uri.fromFile(new File(filePath));
                try {
                    mediaPlayer.setDataSource(this, mUri);
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

        recyclerView = (RecyclerView) findViewById(R.id.rv_record);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordListAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setRealmResults(result);

    }

    @Override
    public void onClickFab(View view) {
        Intent intent = new Intent(this, RecordActivity.class);
        startActivity(intent);
    }
}
