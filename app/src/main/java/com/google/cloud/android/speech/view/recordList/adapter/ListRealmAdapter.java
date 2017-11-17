package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.R;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by USER on 2017-10-16.
 */

public class ListRealmAdapter extends RecyclerView.Adapter<ListViewHolder> {

    final static int DIRECTORY = 0;
    final static int RECORD = 1;

    RealmList<RealmObject> data;
    Context context;
    Realm realm;

    public ListRealmAdapter(RealmList<RealmObject> data, Context context) {
        this.context = context;
        this.data=data;
        realm = Realm.getDefaultInstance();

    }

    public void updateData(RealmList<RealmObject> data){
        this.data=data;
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case DIRECTORY:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_directory_list, parent, false);
                return new ListDirectoryViewHolder(v, context);
            case RECORD:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record_list, parent, false);
                return new ListRealmViewHolder(v, context);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, int position) {

//        RecordDTO recordDTO = new RecordDTO(getItem(position));
//        holder.setData(getItem(position));
//
//        holder.binding.setRecord(recordDTO);
//        holder.binding.setItem(holder);

        holder.bindView(getItem(position));

    }

    @Override
    public int getItemViewType(int position) {
        RealmObject obj = getItem(position);
        if (obj instanceof DirectoryRealm) {
            return DIRECTORY;
        } else if (obj instanceof RecordRealm) {
            return RECORD;
        } else {
            return -1;
        }


    }

    private RealmObject getItem(int position){
        return data.get(position);
    }
    @Override
    public int getItemCount() {
        return data.size();
    }
}
