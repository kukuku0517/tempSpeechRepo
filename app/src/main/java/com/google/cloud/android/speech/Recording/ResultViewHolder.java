package com.google.cloud.android.speech.Recording;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.cloud.android.speech.R;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultViewHolder extends RecyclerView.ViewHolder {

    TextView text;

    ResultViewHolder(LayoutInflater inflater, ViewGroup parent) {
        super(inflater.inflate(R.layout.item_result, parent, false));
        text = (TextView) itemView.findViewById(R.id.text);
    }

}