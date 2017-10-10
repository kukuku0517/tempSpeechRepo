package com.google.cloud.android.speech.Recording;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultAdapter extends RecyclerView.Adapter<ResultViewHolder> {

    private final ArrayList<String> mResults = new ArrayList<>();

    ResultAdapter(ArrayList<String> results) {
        if (results != null) {
            mResults.addAll(results);
        }
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ResultViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ResultViewHolder holder, int position) {
        holder.text.setText(mResults.get(position));
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    void addResult(String result) {
        mResults.add(0, result);
        notifyItemInserted(0);
    }

    public ArrayList<String> getResults() {
        return mResults;
    }

}
