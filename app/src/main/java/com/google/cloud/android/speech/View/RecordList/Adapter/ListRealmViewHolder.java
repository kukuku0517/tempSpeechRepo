package com.google.cloud.android.speech.View.RecordList.Adapter;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.View.RecordResult.RecordResultActivity;
import com.google.cloud.android.speech.databinding.ItemRecordBinding;
import com.google.cloud.android.speech.databinding.ItemRecordListBinding;

import java.util.Random;

/**
 * Created by USER on 2017-10-16.
 */

public class ListRealmViewHolder extends RecyclerView.ViewHolder{
    ItemRecordListBinding binding;
    RecordRealm recordRealm;
    Context context;

    public void setData(RecordRealm recordRealm){
        this.recordRealm=recordRealm;
    }


    public ListRealmViewHolder(View view,Context context) {
        super(view);
        this.context=context;
        binding = DataBindingUtil.bind(itemView);
        //TODO change in bindviewholder, according to tag specific color
//        "hsl(" + 360 * Math.random() + ',' +
//                (25 + 70 * Math.random()) + '%,' +
//                (85 + 10 * Math.random()) + '%)';

        Random rnd = new Random();
        binding.tvTag.setBackgroundColor(Color.HSVToColor(new float[]{rnd.nextInt(100)+100,  (float) (rnd.nextFloat()*0.5+0.3), (float) (rnd.nextFloat()*0.5+0.3)}));
    }

    public void onItemClick(View view){
        Intent intent = new Intent(context, RecordResultActivity.class);
        intent.putExtra("id",recordRealm.getId());
        context.startActivity(intent);
    }

    public void onItemMenuClick(View view){
        Toast.makeText(context,"menu click",Toast.LENGTH_SHORT).show();
    }
}