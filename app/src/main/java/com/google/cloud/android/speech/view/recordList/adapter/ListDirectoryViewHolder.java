package com.google.cloud.android.speech.view.recordList.adapter;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.databinding.DialogRenameDirBinding;
import com.google.cloud.android.speech.databinding.ItemDirectoryListBinding;
import com.google.cloud.android.speech.event.DirEvent;

import org.greenrobot.eventbus.EventBus;

import io.realm.Realm;
import io.realm.RealmObject;

/**
 * Created by USER on 2017-11-18.
 */

public class ListDirectoryViewHolder extends ListViewHolder implements PopupMenu.OnMenuItemClickListener {
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
        binding.setFocus(droppable);
    }

    public ListDirectoryViewHolder(View view, Context context) {
        super(view);
        this.context = context;
        binding = DataBindingUtil.bind(itemView);
        binding.setHandler(this);


    }

    public void onItemClick(View v){
        EventBus.getDefault().postSticky(new DirEvent(directoryRealm.getId()));
    }

    public void onMenuClick(View v) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.getMenuInflater().inflate(R.menu.dir_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rename:
                final DialogRenameDirBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_rename_dir, null, false);
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(binding.getRoot());

                binding.setTitle("폴더 이름 변경");
                binding.setCategory1("이름");
                final Dialog dialog = builder.create();
                dialog.show();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                dialog.findViewById(R.id.rl_confirm).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String s = binding.etRename.getText().toString();
                        if (s.length() == 0) {
                            Toast.makeText(context,"이름을 입력해주세요",Toast.LENGTH_SHORT).show();

                        } else {
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            directoryRealm.setName(binding.etRename.getText().toString());
                            realm.commitTransaction();
                            dialog.dismiss();
                        }
                    }
                });
                dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                return true;
            case R.id.action_delete:
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                directoryRealm.deleteFromRealm();
                realm.commitTransaction();
                return true;
        }


        return false;
    }
}
