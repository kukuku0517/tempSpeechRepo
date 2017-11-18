package com.google.cloud.android.speech.view.recordList.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.data.realm.PrimaryRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.customView.rvInteractions.ItemTouchHelperAdpater;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by USER on 2017-10-16.
 */

public class ListRealmAdapter extends RecyclerView.Adapter<ListViewHolder> implements ItemTouchHelperAdpater {

    final static int DIRECTORY = 0;
    final static int RECORD = 1;

    RealmList<RealmObject> data;
    Context context;
    Realm realm;

    public ListRealmAdapter(RealmList<RealmObject> data, Context context) {
        this.context = context;
        this.data = data;
        realm = Realm.getDefaultInstance();

    }

    public void updateData(RealmList<RealmObject> data) {
        this.data = data;
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case DIRECTORY:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_directory_list, parent, false);
                return new ListDirectoryViewHolder(v, context);
            case RECORD:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record_list, parent, false);
                return new ListRealmViewHolder(v, context);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, int position) {

        if (dragFocusIndex != position) {
            holder.setDroppable(0);
            Log.d("direct", String.valueOf(0));
        } else if (!dragDroppable) {
            holder.setDroppable(1);
            Log.d("direct", String.valueOf(1));
        } else {
            holder.setDroppable(2);
            Log.d("direct", String.valueOf(2));
        }


        holder.bindView(getItem(position));

    }

    @Override
    public int getItemViewType(int position) {
        RealmObject obj = getItem(position);
        if (obj instanceof DirectoryRealm) {
            return DIRECTORY;
        } else if (obj instanceof RecordRealm) {
            return RECORD;
        } else {
            return -1;
        }


    }

    private RealmObject getItem(int position) {
        return data.get(position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public boolean onItemDrop(final int from, final int to) {
        if(to==-1)return false;

        int fromType = (getItem(from) instanceof DirectoryRealm) ? DIRECTORY : RECORD;
        int toType = (getItem(to) instanceof DirectoryRealm) ? DIRECTORY : RECORD;


        int temp = dragFocusIndex;
        dragFocusIndex = -1;
        dragDroppable = false;

        if (fromType==RECORD&&toType==DIRECTORY) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(String.format("'%s' 폴더로 이동합니다",((DirectoryRealm)data.get(to)).getName()))
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        // 확인 버튼 클릭시 설정
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            int fromId = ( (PrimaryRealm)data.get(from)).getId();
                            int toId = ( (PrimaryRealm)data.get(to)).getId();
                            RealmUtil.recordToDir(realm,fromId,toId);
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
        } else if(fromType==DIRECTORY&&toType==DIRECTORY){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(String.format("'%s' 폴더로 이동합니다",((DirectoryRealm)data.get(to)).getName()))
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        // 확인 버튼 클릭시 설정
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            int fromId = ((PrimaryRealm)data.get(from)).getId();
                            int toId = ( (PrimaryRealm)data.get(to)).getId();
                            RealmUtil.dirTodir(realm,fromId,toId);
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
        }else{
            notifyItemChanged(temp);
        }

        return true;
    }


    private int dragFocusIndex = -1;
    private boolean dragDroppable = false;


    @Override
    public boolean onItemMove(int from, int to) {
        int toType = (getItem(to) instanceof DirectoryRealm) ? DIRECTORY : RECORD;

        int prev = dragFocusIndex;
        dragFocusIndex=to;

        if (toType == DIRECTORY) {
            dragDroppable = true;
        } else {
            dragDroppable = false;
        }

        notifyItemChanged(prev);
        notifyItemChanged(to);


        return false;
    }

    @Override
    public void onItemDismiss(int position) {

    }
}
