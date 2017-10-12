package com.google.cloud.android.speech.View.Recording.Adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.databinding.ItemResultBinding;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultViewHolder extends RecyclerView.ViewHolder {

    ItemResultBinding binding;

    ResultViewHolder(View view) {
        super(view);
        binding = DataBindingUtil.bind(view);

    }

}