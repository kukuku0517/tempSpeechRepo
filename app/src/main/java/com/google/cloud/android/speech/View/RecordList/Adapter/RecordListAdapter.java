package com.google.cloud.android.speech.View.RecordList.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.Data.DTO.RecordDTO;

import io.realm.RealmResults;

/**
 * Created by samsung on 2017-10-06.
 */

public class RecordListAdapter extends RecyclerView.Adapter<RecordListViewHolder> {

    RealmResults<RecordRealm> realmResults;

    public void setRealmResults(RealmResults<RecordRealm> realmResults){
        this.realmResults = realmResults;
    }
    @Override
    public RecordListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record,parent,false);
        return new RecordListViewHolder(view,parent.getContext());
    }

    @Override
    public void onBindViewHolder(RecordListViewHolder holder, int position) {


        RecordDTO recordDTO = new RecordDTO(realmResults.get(position));
        holder.setData(realmResults.get(position));
        holder.binding.setRecord(recordDTO);
        holder.binding.setItem(holder);

    }

    @Override
    public int getItemCount() {
        return realmResults.size();
    }
}
