package com.google.cloud.android.speech.View.Recording.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.R;

import java.util.ArrayList;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultAdapter extends RecyclerView.Adapter<ResultViewHolder> {

    private final ArrayList<String> mResults = new ArrayList<>();

    public ResultAdapter(ArrayList<String> results) {
        if (results != null) {
            mResults.addAll(results);
        }
    }

    @Override
    public ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ResultViewHolder holder, int position) {
        holder.binding.setResult(mResults.get(position));
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
