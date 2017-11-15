package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.databinding.ItemTagBinding;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;

import java.util.Random;

/**
 * Created by USER on 2017-11-15.
 */

public class TagRealmViewHolder extends RecyclerView.ViewHolder {
    private Context context;
    ItemTagBinding binding;
    private static TagRealm tagRealm;
    private TagHandler tagHandler;

    public void setData(TagRealm tagRealm, TagHandler tagHandler) {
        this.tagRealm = tagRealm;
        this.tagHandler = tagHandler;
    }

    public TagRealmViewHolder(View view, Context context) {
        super(view);
        this.context = context;
        binding = DataBindingUtil.bind(itemView);
        binding.setHandler(tagHandler);
    }

    @BindingAdapter("tag_color")
    public static void setTagColor(TextView v, int colorCode) {
        Random rnd = new Random();
        GradientDrawable tvBackground = (GradientDrawable) v.getBackground();
        tvBackground.setColorFilter(Color.HSVToColor(new float[]{colorCode, 0.5f, 0.5f}), PorterDuff.Mode.SRC_ATOP);
    }


}