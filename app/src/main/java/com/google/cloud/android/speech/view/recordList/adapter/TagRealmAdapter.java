package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;
import com.google.instrumentation.stats.Tag;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by USER on 2017-11-15.
 */

public class TagRealmAdapter extends RecyclerView.Adapter<TagRealmViewHolder> {

    private Context context;
    private Realm realm;
    private TagHandler tagHandler;
    private ArrayList<TagRealm> tags;

    public void updateData(ArrayList<TagRealm> tags){
        this.tags=tags;
    }
    public TagRealmAdapter(Context context, ArrayList<TagRealm> tags, TagHandler tagHandler) {
        this.context = context;
        this.tags=tags;
        this.tagHandler = tagHandler;
        realm = Realm.getDefaultInstance();
    }

    @Override
    public TagRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag, parent, false);
        return new TagRealmViewHolder(v, context);
    }

    @Override
    public void onBindViewHolder(TagRealmViewHolder holder, int position) {
        TagRealm tag = tags.get(position);
        holder.binding.setTag(tag);
        holder.binding.setHandler(tagHandler);
        holder.setData(tag, tagHandler);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }


}
