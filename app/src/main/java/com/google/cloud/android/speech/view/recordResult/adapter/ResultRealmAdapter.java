package com.google.cloud.android.speech.view.recordResult.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.databinding.BindingAdapter;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.recordResult.CustomView.ItemTouchHelperAdpater;

import org.greenrobot.eventbus.EventBus;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm, ResultRealmViewHolder> implements ItemTouchHelperAdpater {

    //    private MyItemClickListener listener;
    Context context;
    Realm realm;
    int focus = 0;
    int recordId = -1;


    public ResultRealmAdapter(@Nullable OrderedRealmCollection<SentenceRealm> data, int recordId, boolean autoUpdate, boolean updateOnModification, Context context) {
        super(data, autoUpdate, updateOnModification);
        this.context = context;
        this.recordId = recordId;
        realm = Realm.getDefaultInstance();
    }


    @Override
    public ResultRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record_result, parent, false);
        return new ResultRealmViewHolder(v, context);
    }


    //    @BindingAdapter("dragFocusIndex")
//    public static void setFocus(LinearLayout v, boolean focus) {
//        if (focus) {
//            v.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
//        } else {
//            v.setBackgroundColor(context.getResources().getColor(R.color.default_background));
//        }
//    }
//
    @Override
    public void onBindViewHolder(ResultRealmViewHolder holder, final int position) {
        SentenceRealm sentenceRealm = getItem(position);
        holder.onBindView(sentenceRealm);
        holder.focus(focus == position);

        if (dragFocusIndex != position) {
            holder.binding.setDroppableState(0);
        } else if (!dragDroppable) {
            holder.binding.setDroppableState(1);
        } else {
            holder.binding.setDroppableState(2);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long time = getItem(position).getStartMillis();
                EventBus.getDefault().post(new SeekEvent(time));
            }
        });
    }

    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }

    public void focus(int index) {
        int temp = focus;
        focus = index;
        notifyItemChanged(temp);
        notifyItemChanged(focus);
    }

    public int getFocus() {
        return focus;
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