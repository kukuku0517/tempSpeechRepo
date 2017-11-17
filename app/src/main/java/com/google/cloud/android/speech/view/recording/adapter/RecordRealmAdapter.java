package com.google.cloud.android.speech.view.recording.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.util.DateUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.recordResult.CustomView.ItemTouchHelperAdpater;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by USER on 2017-10-13.
 */


public class RecordRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm, RecordRealmViewHolder> implements ItemTouchHelperAdpater {

    Context context;
    Realm realm;
    int recordId;

    public RecordRealmAdapter(@Nullable OrderedRealmCollection<SentenceRealm> data, int recordId, boolean autoUpdate, boolean updateOnModification, Context context) {
        super(data, autoUpdate, updateOnModification);
        this.context = context;
        this.recordId=recordId;
        realm = Realm.getDefaultInstance();
    }


    @Override
    public RecordRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordRealmViewHolder(v);
    }

    String TAG = "Speech";

    @Override
    public void onBindViewHolder(RecordRealmViewHolder holder, final int position) {

        SentenceRealm sentenceRealm = getItem(position);
        holder.binding.setSentence(sentenceRealm);
        Log.i(TAG, DateUtil.durationToTextFormat((int) sentenceRealm.getStartMillis()));


    }

    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }


    @Override
    public boolean onItemDrop(final int fromPosition, final int toPosition) {
        int temp = dragFocusIndex;
        dragFocusIndex = -1;
        dragDroppable = false;

        if (Math.abs(fromPosition - toPosition) == 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setMessage("병합 하시겠습니까?")

                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        // 확인 버튼 클릭시 설정
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            RealmUtil.mergeSentence(realm, recordId, getItem(fromPosition).getId(), getItem(toPosition).getId(), fromPosition, toPosition);
                            realm.commitTransaction();
                            notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        // 취소 버튼 클릭시 설정
                        public void onClick(DialogInterface dialog, int whichButton) {
                            notifyDataSetChanged();
                            dialog.cancel();
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();


        } else {
            notifyItemChanged(temp);
        }
        return true;
    }

    int dragFocusIndex = -1;
    boolean dragDroppable = false;

    @Override
    public boolean onItemMove(int from, int to) {
        int temp = dragFocusIndex;
        if (temp != to) {
            dragFocusIndex = to;
            dragDroppable = Math.abs(from - to) == 1;
        }

        notifyItemChanged(temp);
        notifyItemChanged(dragFocusIndex);

        return true;
    }

    @Override
    public void onItemDismiss(int position) {

    }
}