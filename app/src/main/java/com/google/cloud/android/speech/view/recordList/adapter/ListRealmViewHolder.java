package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
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
        binding.setFocus(droppable);
        binding.setItem(this);
        if(recordRealm.getTagList().size()>0){
            binding.tvTag.setBackgroundColor(Color.HSVToColor(new float[]{recordRealm.getTagList().get(0).getColorCode(), 0.5f,0.5f}));
        }
    }
}