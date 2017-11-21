package com.google.cloud.android.speech.view.recordResult.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.customView.AlternativeDialogFragment;
import com.google.cloud.android.speech.view.customView.rvInteractions.ItemTouchHelperAdpater;
import com.google.cloud.android.speech.view.recordResult.handler.SpannableItemClickListener;

import org.greenrobot.eventbus.EventBus;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm, ResultRealmViewHolder> implements ItemTouchHelperAdpater {

    //    private MyItemClickListener listener;
    private Context context;
    private Realm realm;
    private int focus = 0;
    private int recordId = -1;
    private int lastPosition = -1;
    private boolean editMode = false;


    public void setEditMode() {
        this.editMode = !editMode;
        notifyDataSetChanged();
    }
    public boolean isEditMode() {return editMode;
    }


    public ResultRealmAdapter(@Nullable OrderedRealmCollection<SentenceRealm> data, int recordId, boolean autoUpdate, boolean updateOnModification, Context context) {
        super(data, autoUpdate, updateOnModification);
        this.context = context;
        this.recordId = recordId;
        realm = Realm.getDefaultInstance();
    }

    public void updateData(@Nullable OrderedRealmCollection<SentenceRealm> data, int recordId) {
        super.updateData(data);
        this.recordId = recordId;
    }

    @Override
    public ResultRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record_result, parent, false);
        return new ResultRealmViewHolder(v, context);
    }

    @Override
    public void onBindViewHolder(ResultRealmViewHolder holder, final int position) {
        final SentenceRealm sentenceRealm = getItem(position);
        holder.onBindView(sentenceRealm);
        holder.focus(focus == position);

        if (editMode) {
            holder.binding.setDroppableState(3);

        } else {
            if (dragFocusIndex != position) {
                holder.binding.setDroppableState(0);
            } else if (!dragDroppable) {
                holder.binding.setDroppableState(1);
            } else {
                holder.binding.setDroppableState(2);
            }
        }


        holder.binding.spannable.setListener(new SpannableItemClickListener() {
            @Override
            public void onClickItem(int id, int pos) {
                if (editMode) {
                    realm.beginTransaction();
                    WordRealm word = realm.where(WordRealm.class).equalTo("id", id).findFirst();
                    realm.commitTransaction();

                    AlternativeDialogFragment dialog = new AlternativeDialogFragment();
                    dialog.setWord(word);
                    dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "NewRecordDialogFragment");
                } else {
                    realm.beginTransaction();
                    WordRealm word = realm.where(WordRealm.class).equalTo("id", id).findFirst();
                    realm.commitTransaction();
                    EventBus.getDefault().post(new SeekEvent(word.getStartMillis()));
                }

            }

            @Override
            public void onLongClickItem(int id, int pos) {
                if (editMode) {
                    realm.beginTransaction();
                    RealmUtil.splitSentence(realm, recordId, position, getItem(position).getId(), id, getItem(position).getCluster());
                    realm.commitTransaction();
                } else {
                    realm.beginTransaction();
                    WordRealm word = realm.where(WordRealm.class).equalTo("id", id).findFirst();
                    word.setHighlight(!word.isHighlight());
                    realm.commitTransaction();
                }

            }


        }, getItem(position).getId());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long time = getItem(position).getStartMillis();
                EventBus.getDefault().post(new SeekEvent(time));
            }
        });
//        setAnimation(holder.itemView, position);

    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
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