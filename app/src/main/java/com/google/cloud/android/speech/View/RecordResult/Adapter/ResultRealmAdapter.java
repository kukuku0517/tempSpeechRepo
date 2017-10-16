package com.google.cloud.android.speech.View.RecordResult.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.Util.DateUtil;
import com.google.cloud.android.speech.View.RecordResult.MyItemClickListener;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm, ResultRealmViewHolder> {

    private MyItemClickListener listener;
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
        StringBuilder s = new StringBuilder();

        LinearLayout layout = holder.binding.llContainer;

        holder.binding.setSentenceString(s.toString());
        holder.binding.setSentenceTime(DateUtil.durationToDate((int) sentenceRealm.getStartMillis()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v, position);
            }
        });
        holder.onBindView(sentenceRealm,context,realm);
    }

    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }

    public void setOnItemClickListener(MyItemClickListener listener) {
        this.listener = listener;
    }
}