package com.google.cloud.android.speech.View.RecordResult.Adapter;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.View.RecordResult.MyItemClickListener;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm,ResultRealmViewHolder> {

    private MyItemClickListener listener;

    public ResultRealmAdapter(@Nullable OrderedRealmCollection<SentenceRealm> data, boolean autoUpdate, boolean updateOnModification) {
        super(data, autoUpdate, updateOnModification);
        getItem(0);
    }

    @Override
    public ResultRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recorded_result, parent, false);
        return new ResultRealmViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ResultRealmViewHolder holder, final int position) {
        SentenceRealm sentenceRealm =getItem(position);
        StringBuilder s = new StringBuilder();
        for(WordRealm w:sentenceRealm.getWordList()){
            s.append(w.getWord());
        }
        holder.binding.setSentenceString(s.toString());
        holder.binding.setSentenceTime(String.valueOf(sentenceRealm.getStartMillis()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v,position);
            }
        });
    }
    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }

    public void setOnItemClickListener(MyItemClickListener listener){
        this.listener=listener;
    }
}