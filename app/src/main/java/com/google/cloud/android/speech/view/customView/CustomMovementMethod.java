package com.google.cloud.android.speech.view.customView;

import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Created by USER on 2017-10-25.
 */

public class CustomMovementMethod extends ScrollingMovementMethod {

    int clicks = 0;
    String TAG = "customView";
    final int DOUBLE_PRESS_INTERVAL = 250;


    private static CustomMovementMethod instance ;

    public static CustomMovementMethod getInstance() {
        if(instance==null){
            instance=new CustomMovementMethod();
        }

        return instance;
    }

    @Override
    public boolean onTouchEvent(final TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

//            int startSpan = off - context.getResources().getDimension(R.dimen.extra_space_start);
//            int endSpan = off + context.getResources().getDimension(R.dimen.extra_space_end);

            int startSpan = off;
            int endSpan = off;

            final RealmClickableSpan[] link = buffer.getSpans(startSpan, endSpan, RealmClickableSpan.class);

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    clicks++;
                    if (clicks == 1) {
                        Handler myHandler = new Handler() {
                            public void handleMessage(Message m) {
                                if (clicks >= 2) { // TODO distance
                                    Log.d(TAG, String.valueOf(clicks));
                                    link[0].onDoubleClick(widget);
                                    clicks=0;
                                } else {
                                    Log.d(TAG, String.valueOf(clicks));
                                    link[0].onClick(widget);
                                    clicks=0;
                                }
                            }
                        };
                        Message m = new Message();

                        myHandler.sendMessageDelayed(m, DOUBLE_PRESS_INTERVAL);
                    }


                } else if (action == MotionEvent.ACTION_DOWN) {

                    Selection.setSelection(buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]));
                }
                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }


        return super.onTouchEvent(widget, buffer, event);
    }

}
