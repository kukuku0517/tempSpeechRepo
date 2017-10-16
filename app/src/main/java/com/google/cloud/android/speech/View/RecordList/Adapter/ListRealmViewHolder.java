package com.google.cloud.android.speech.View.RecordList.Adapter;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.cloud.android.speech.Data.Realm.RecordRealm;
import com.google.cloud.android.speech.View.RecordResult.RecordResultActivity;
import com.google.cloud.android.speech.databinding.ItemRecordBinding;
import com.google.cloud.android.speech.databinding.ItemRecordListBinding;

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
    }

    public void onItemClick(View view){
        Intent intent = new Intent(context, RecordResultActivity.class);
        intent.putExtra("id",recordRealm.getId());
        context.startActivity(intent);
    }
}