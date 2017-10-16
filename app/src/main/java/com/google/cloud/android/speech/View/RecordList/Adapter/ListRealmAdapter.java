package com.google.cloud.android.speech.View.RecordList.Adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.Data.DTO.RecordDTO;
import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.View.RecordResult.Adapter.ResultRealmViewHolder;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by USER on 2017-10-16.
 */

public class ListRealmAdapter extends RealmRecyclerViewAdapter<RecordRealm,ListRealmViewHolder> {

    Context context;
    Realm realm;

    public ListRealmAdapter(@Nullable OrderedRealmCollection<RecordRealm> data, boolean autoUpdate,boolean updateOnModification,Context context) {
        super(data, autoUpdate, updateOnModification);
        this.context = context;
        realm = Realm.getDefaultInstance();
    }

    @Override
    public ListRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record_list, parent, false);
        return new ListRealmViewHolder(v,context);
    }

    @Override
    public void onBindViewHolder(ListRealmViewHolder holder, int position) {

        RecordDTO recordDTO = new RecordDTO(getItem(position));
        holder.setData(getItem(position));

        holder.binding.setRecord(recordDTO);
        holder.binding.setItem(holder);

    }
}
