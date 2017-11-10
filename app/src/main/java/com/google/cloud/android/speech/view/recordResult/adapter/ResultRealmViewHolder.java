package com.google.cloud.android.speech.view.recordResult.adapter;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.util.DateUtil;
import com.google.cloud.android.speech.databinding.ItemRecordResultBinding;
import com.google.cloud.android.speech.view.recordResult.handler.ClusterItemClickListener;

import io.realm.Realm;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmViewHolder extends RecyclerView.ViewHolder implements ClusterItemClickListener {

    ItemRecordResultBinding binding;
    static Context context;
    SentenceRealm sentenceRealm;

    public ResultRealmViewHolder(View view, Context context) {
        super(view);
        binding = DataBindingUtil.bind(view);
        this.context = context;
    }

    void onBindView(SentenceRealm sentenceRealm) {
        StringBuilder s = new StringBuilder();

        binding.setSentenceString(s.toString());
        binding.setSentenceTime(DateUtil.durationToDate((int) sentenceRealm.getStartMillis()));
        binding.setSentence(sentenceRealm);
        binding.setHandler(this);
        this.sentenceRealm = sentenceRealm;

    }

    void focus(boolean index) {
        binding.setFocus(index);
    }

    @BindingAdapter("focus")
    public static void setFocus(LinearLayout v, boolean focus) {
        if (focus)
            v.setBackgroundColor(context.getResources().getColor(R.color.naver_green));
        else
            v.setBackgroundColor(Color.WHITE);
    }

    @BindingAdapter("clusterNumber")
    public static void setSentence(TextView v, String value) {
        v.setText(value);
    }

    @BindingAdapter("clusterColor")
    public static void setClusterColor(ImageView v, int value) {
        switch (value) {
            case 0:
                v.setColorFilter(Color.WHITE);
                break;
            case 1:
                v.setColorFilter(Color.CYAN);
                break;
            case 2:
                v.setColorFilter(Color.GREEN);
                break;
        }
    }

    Realm realm;

    @Override
    public void onClickCluster(View v) {
        Toast.makeText(context, String.valueOf(sentenceRealm.getCluster().getClusterNo()), Toast.LENGTH_SHORT).show();
        realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        sentenceRealm.getCluster().setClusterNo(3);
        realm.commitTransaction();
    }
}
