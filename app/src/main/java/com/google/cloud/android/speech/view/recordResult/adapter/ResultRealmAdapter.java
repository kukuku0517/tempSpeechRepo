package com.google.cloud.android.speech.view.recordResult.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.SentenceRealm;

import org.greenrobot.eventbus.EventBus;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm, ResultRealmViewHolder> {

//    private MyItemClickListener listener;
    Context context;
    Realm realm;

    public ResultRealmAdapter(@Nullable OrderedRealmCollection<SentenceRealm> data, boolean autoUpdate, boolean updateOnModification, Context context) {
        super(data, autoUpdate, updateOnModification);
        this.context = context;
        realm = Realm.getDefaultInstance();
    }


    @Override
    public ResultRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record_result, parent, false);
        return new ResultRealmViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ResultRealmViewHolder holder, final int position) {
        SentenceRealm sentenceRealm = getItem(position);


        holder.onBindView(sentenceRealm,context,realm);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                listener.onClick(v, position);
                long time =getItem(position).getStartMillis();
                EventBus.getDefault().post(new SeekEvent(time));
            }
        });
    }

    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }

//    public void setOnItemClickListener(MyItemClickListener listener) {
//        this.listener = listener;
//    }
}