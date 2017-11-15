package com.google.cloud.android.speech.view.recordList;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.databinding.DialogNewRecordBinding;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.recordList.adapter.TagRealmAdapter;
import com.google.cloud.android.speech.view.recordList.handler.DialogHandler;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by samsung on 2017-10-07.
 */

public class NewRecordDialog extends DialogFragment implements DialogHandler {
    DialogNewRecordBinding binding;

    @Override
    public void onClickTagAdd(View v) {
        String tagName = mEditTextTag.getText().toString();
        TagRealm tag = realm.where(TagRealm.class).equalTo("name", tagName).findFirst();

        if (tag == null) {
            realm.beginTransaction();
            tag = RealmUtil.createObject(realm, TagRealm.class);
            tag.setName(tagName);
            tag.setCount(1);
            Random rnd = new Random();
            tag.setColorCode(rnd.nextInt(100) + 100);
            realm.commitTransaction();
            originalTags.add(tag);
            originalAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(getContext(), "이미 존재하는 이름입니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClickCancel(View v) {
        dismiss();
    }

    @Override
    public void onClickStart(View v) {
        String title = mEditTextTitle.getText().toString();

        File file = new File(FileUtil.getFilename(title));
        if (file.exists()) {
            Toast.makeText(getContext(), "이미 존재하는 제목 입니다", Toast.LENGTH_SHORT).show();
        } else if (title.equals("")) {
            Toast.makeText(getContext(), "제목을 입력하세요", Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<Integer> tagIds = new ArrayList<Integer>();
            for (TagRealm t : addedTags) {
                tagIds.add(t.getId());
            }
            mListener.onDialogPositiveClick(title, tagIds, requestCode);
            dismiss();
        }

    }

    public interface NewRecordDialogListener {
        void onDialogPositiveClick(String title, ArrayList<Integer> tags, int requestCode);

        void onDialogNegativeClick();
    }

    // Use this instance of the interface to deliver action events
    NewRecordDialogListener mListener;
    private EditText mEditTextTitle;
    private EditText mEditTextTag;

    private RecyclerView originalRecyclerView;
    private RecyclerView addRecyclerView;

    private TagRealmAdapter originalAdapter;
    private RecyclerView.LayoutManager originalLayout;

    private TagRealmAdapter addAdapter;
    private RecyclerView.LayoutManager addLayout;

    private ArrayList<TagRealm> originalTags = new ArrayList<>();
    private ArrayList<TagRealm> addedTags = new ArrayList<>();

    private int requestCode;
    private Realm realm;

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_new_record, null, false);
        binding.setHandler(this);
        mListener = (NewRecordDialogListener) getActivity();
        realm = Realm.getDefaultInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = binding.getRoot();
        mEditTextTitle = binding.etNewRecordTitle;
        mEditTextTag = binding.etNewRecordTag;
        originalRecyclerView = binding.rvOriginTags;
        addRecyclerView = binding.rvAddTags;

        RealmResults<TagRealm> tags = realm.where(TagRealm.class).findAll();
        for (TagRealm tag : tags) {
            originalTags.add(tag);
        }

        originalAdapter = new TagRealmAdapter(getContext(), originalTags, new TagHandler() {
            @Override
            public void onClickTag(View v, TagRealm tag) {
                for (int i = 0; i < originalTags.size(); i++) {
                    if (originalTags.get(i).getId() == tag.getId()) {
                        originalTags.remove(i);
                        break;
                    }
                }
                addedTags.add(tag);
                addAdapter.notifyDataSetChanged();
                originalAdapter.notifyDataSetChanged();
            }
        });

        addAdapter = new TagRealmAdapter(getContext(), addedTags, new TagHandler() {
            @Override
            public void onClickTag(View v, TagRealm tag) {
                for (int i = 0; i < addedTags.size(); i++) {
                    if (addedTags.get(i).getId() == tag.getId()) {
                        addedTags.remove(i);
                        break;
                    }
                }
                originalTags.add(tag);
                addAdapter.notifyDataSetChanged();
                originalAdapter.notifyDataSetChanged();
            }
        });

        originalAdapter.setHasStableIds(true);
        originalLayout = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        originalRecyclerView.setAdapter(originalAdapter);
        originalRecyclerView.setLayoutManager(originalLayout);

        addAdapter.setHasStableIds(true);
        addLayout = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        addRecyclerView.setAdapter(addAdapter);
        addRecyclerView.setLayoutManager(addLayout);
        builder.setView(view);
        return builder.create();
    }


}
