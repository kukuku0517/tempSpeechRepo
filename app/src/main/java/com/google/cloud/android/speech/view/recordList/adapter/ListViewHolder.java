package com.google.cloud.android.speech.view.recordList.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import io.realm.RealmObject;

/**
 * Created by USER on 2017-11-18.
 */

public abstract class ListViewHolder extends RecyclerView.ViewHolder{

    public ListViewHolder(View itemView) {
        super(itemView);
    }

    public abstract  void bindView(RealmObject data);
}
