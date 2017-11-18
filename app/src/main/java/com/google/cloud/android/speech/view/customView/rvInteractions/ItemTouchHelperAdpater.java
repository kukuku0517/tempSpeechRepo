package com.google.cloud.android.speech.view.customView.rvInteractions;

/**
 * Created by USER on 2017-11-16.
 */

public interface ItemTouchHelperAdpater {
    boolean onItemDrop(int from, int to);
    boolean onItemMove(int from, int to);
    void onItemDismiss(int position);

}
