package com.google.cloud.android.speech.view.recording.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.R;

import java.util.ArrayList;

/**
 * Created by samsung on 2017-10-08.
 */

public class RecordAdapter extends RecyclerView.Adapter<RecordViewHolder> {

    private final ArrayList<String> mResults = new ArrayList<>();

    public RecordAdapter(ArrayList<String> results) {
        if (results != null) {
            mResults.addAll(results);
        }
    }

    @Override
    public RecordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position) {
//        holder.binding.setResult(mResults.get(position));
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    public void addResult(String result) {
        mResults.add(0, result);
        notifyItemInserted(0);
    }

    public ArrayList<String> getResults() {
        return mResults;
    }

}
