package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.view.recordResult.RecordResultActivity;
import com.google.cloud.android.speech.databinding.ItemRecordListBinding;

import java.util.Random;

import io.realm.RealmObject;

/**
 * Created by USER on 2017-10-16.
 */

public class ListRealmViewHolder extends ListViewHolder {
    ItemRecordListBinding binding;
    RecordRealm recordRealm;
    Context context;

    public void setData(RecordRealm recordRealm) {
        this.recordRealm = recordRealm;
    }


    public ListRealmViewHolder(View view, Context context) {
        super(view);
        this.context = context;
        binding = DataBindingUtil.bind(itemView);
        //TODO change in bindviewholder, according to tag specific color
//        "hsl(" + 360 * Math.random() + ',' +
//                (25 + 70 * Math.random()) + '%,' +
//                (85 + 10 * Math.random()) + '%)';

        Random rnd = new Random();
        binding.tvTag.setBackgroundColor(Color.HSVToColor(new float[]{rnd.nextInt(100) + 100, (float) (rnd.nextFloat() * 0.5 + 0.3), (float) (rnd.nextFloat() * 0.5 + 0.3)}));
    }

    public void onItemClick(View view) {
        Intent intent = new Intent(context, RecordResultActivity.class);
        intent.putExtra("id", recordRealm.getId());
        context.startActivity(intent);
    }

    public void onItemMenuClick(View view) {
        Toast.makeText(context, "menu click", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void bindView(RealmObject data) {
        RecordDTO recordDTO = new RecordDTO((RecordRealm) data);
        setData((RecordRealm)data);
        binding.setRecord(recordDTO);
        binding.setItem(this);
    }
}