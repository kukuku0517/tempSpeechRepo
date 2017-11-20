package com.google.cloud.android.speech.view.customView.rvInteractions;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

/**
 * Created by USER on 2017-11-16.
 */

public class ItemTouchHelperCallBack extends ItemTouchHelper.Callback {

    private final ItemTouchHelperAdpater mAdapter;
    //    private RecyclerView.ViewHolder target;
    private int targetIndex;

    public ItemTouchHelperCallBack(ItemTouchHelperAdpater mAdapter) {
        this.mAdapter = mAdapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        int swipeFlags =0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }


    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        int from = viewHolder.getAdapterPosition();
        int to = target.getAdapterPosition();
        this.targetIndex = to;
        mAdapter.onItemMove(from, to);
        Log.d("position", from + ":" + to);
        return true;
    }


    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mAdapter.onItemDrop(viewHolder.getAdapterPosition(), targetIndex);
        Log.d("dragdrop", "drop");
    }


}
