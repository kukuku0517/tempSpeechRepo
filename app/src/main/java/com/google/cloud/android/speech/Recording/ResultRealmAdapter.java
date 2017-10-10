package com.google.cloud.android.speech.Recording;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.RealmData.RecordRealm;
import com.google.cloud.android.speech.RealmData.SentenceRealm;
import com.google.cloud.android.speech.RealmData.WordRealm;
import com.google.cloud.android.speech.RecordList.RealmString;
import com.google.cloud.android.speech.RecordList.RecordDTO;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import static android.R.attr.type;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm,ResultRealmViewHolder> {

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
    public void onBindViewHolder(ResultRealmViewHolder holder, int position) {
        SentenceRealm sentenceRealm =getItem(position);
        StringBuilder s = new StringBuilder();
        for(WordRealm w:sentenceRealm.getWordList()){
            s.append(w.getWord());
        }
        holder.binding.setSentence(s.toString());
    }
    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }
}