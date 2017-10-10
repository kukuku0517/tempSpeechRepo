package com.google.cloud.android.speech.RecordingResult;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.RealmData.RecordRealm;
import com.google.cloud.android.speech.RealmData.SentenceRealm;
import com.google.cloud.android.speech.Recording.ResultRealmAdapter;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class RecordResultActivity extends AppCompatActivity {
    private final static String TAG = "Speech";
    private Realm realm;
    private RecyclerView mRecyclerView;
    private ResultRealmAdapter mAdpater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_result);
        int itemId = getIntent().getIntExtra("id", 1);

        realm = Realm.getDefaultInstance();
        RealmList<SentenceRealm> sentenceResults= realm.where(RecordRealm.class).equalTo("id", itemId).findFirst().getSentenceRealms();

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_record_result);
        mAdpater = new ResultRealmAdapter(sentenceResults,true,true);
        mRecyclerView.setAdapter(mAdpater);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));


    }
}
