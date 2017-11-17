package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.view.View;
import android.widget.Toast;

import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.databinding.ItemDirectoryListBinding;
import com.google.cloud.android.speech.databinding.ItemRecordListBinding;
import com.google.cloud.android.speech.view.recordResult.RecordResultActivity;

import java.util.Random;

import io.realm.RealmObject;

/**
 * Created by USER on 2017-11-18.
 */

public class ListDirectoryViewHolder extends ListViewHolder {
    ItemDirectoryListBinding binding;
    DirectoryRealm directoryRealm;
    Context context;

    public ListDirectoryViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bindView(RealmObject data) {
        directoryRealm = (DirectoryRealm) data;
        binding.setData(directoryRealm);
    }

    public ListDirectoryViewHolder(View view, Context context) {
        super(view);
        this.context = context;
        binding = DataBindingUtil.bind(itemView);

    }

    public void onItemClick(View view) {
        Toast.makeText(context, "wow", Toast.LENGTH_SHORT).show();
    }

    public void onItemMenuClick(View view) {
        Toast.makeText(context, "menu click", Toast.LENGTH_SHORT).show();
    }
}
