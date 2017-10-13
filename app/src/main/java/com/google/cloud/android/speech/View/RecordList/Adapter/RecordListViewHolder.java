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
 * Created by samsung on 2017-10-06.
 */

public class RecordListViewHolder extends RecyclerView.ViewHolder {
    ItemRecordListBinding binding;
    RecordRealm recordRealm;
    Context context;

    public void setData(RecordRealm recordRealm){
        this.recordRealm=recordRealm;
    }
    public RecordListViewHolder(View itemView, Context context) {
        super(itemView);
        this.context=context;
        binding = DataBindingUtil.bind(itemView);

    }

    public void onItemClick(View view){
        Intent intent = new Intent(context, RecordResultActivity.class);
        intent.putExtra("id",recordRealm.getId());
        context.startActivity(intent);
    }
}
