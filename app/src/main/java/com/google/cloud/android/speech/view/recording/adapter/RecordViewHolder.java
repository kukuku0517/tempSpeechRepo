package com.google.cloud.android.speech.view.recording.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.cloud.android.speech.databinding.ItemResultBinding;

/**
 * Created by samsung on 2017-10-08.
 */

public class RecordViewHolder extends RecyclerView.ViewHolder {

    ItemResultBinding binding;

    RecordViewHolder(View view) {
        super(view);
        binding = DataBindingUtil.bind(view);

    }

}