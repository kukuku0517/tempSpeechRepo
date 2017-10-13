package com.google.cloud.android.speech.View.Recording.Adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.cloud.android.speech.databinding.ItemRecordBinding;

/**
 * Created by USER on 2017-10-13.
 */


public class RecordRealmViewHolder extends RecyclerView.ViewHolder{

    ItemRecordBinding binding;

    public RecordRealmViewHolder(View view) {
        super(view);
        binding= DataBindingUtil.bind(view);
    }

}