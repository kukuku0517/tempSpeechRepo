package com.google.cloud.android.speech.View.RecordResult.Adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.databinding.ItemRecordedResultBinding;


/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmViewHolder extends RecyclerView.ViewHolder{

    ItemRecordedResultBinding binding;

    public ResultRealmViewHolder(View view) {
        super(view);
        binding= DataBindingUtil.bind(view);
    }

    void onClickView(View view){

    }
}