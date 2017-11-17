package com.google.cloud.android.speech.view.recordResult.CustomView;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

/**
 * Created by USER on 2017-11-16.
 */

public class SentenceItemTouchHelperCallBack extends ItemTouchHelper.Callback {

    private final ItemTouchHelperAdpater mAdapter;
    private RecyclerView.ViewHolder target;

    public SentenceItemTouchHelperCallBack(ItemTouchHelperAdpater mAdapter) {
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
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        this.target = target;
        int from = viewHolder.getAdapterPosition();
        int to = target.getAdapterPosition();
        mAdapter.onItemMove(from, to);
        return true;
    }


    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mAdapter.onItemDrop(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        Log.d("dragdrop", "drop");
    }


}
