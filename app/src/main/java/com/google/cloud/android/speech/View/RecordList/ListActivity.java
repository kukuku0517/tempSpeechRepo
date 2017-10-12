package com.google.cloud.android.speech.View.RecordList;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.View.RecordList.Adapter.RecordAdapter;
import com.google.cloud.android.speech.View.Recording.MainActivity;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.databinding.ActivityListBinding;


import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class ListActivity extends AppCompatActivity implements ListHandler {

    RecyclerView recyclerView;
    RecordAdapter adapter;
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

        recyclerView = (RecyclerView) findViewById(R.id.rv_record);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setRealmResults(result);

    }

    @Override
    public void onClickFab(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
