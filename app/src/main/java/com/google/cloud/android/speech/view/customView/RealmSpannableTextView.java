package com.google.cloud.android.speech.view.customView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.event.SeekEvent;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.view.recordResult.handler.SpannableItemClickListener;

import org.greenrobot.eventbus.EventBus;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by USER on 2017-10-25.
 */

public class RealmSpannableTextView extends android.support.v7.widget.AppCompatTextView {

    private SpannableItemClickListener mListener;

    private SpannableStringBuilder builder;
    private int sid;
    private SentenceRealm sentence;
    private int startIndex = 0;
    private final String TAG = "customView";
    private Realm realm;

    public void setListener(SpannableItemClickListener mListener,int sid){
        this.mListener=mListener;
        this.sid=sid;

         realm.beginTransaction();
        sentence = realm.where(SentenceRealm.class).equalTo("id", sid).findFirst();
        buildSentence(sentence);
        realm.commitTransaction();

        setMovementMethod(CustomMovementMethod.getInstance());
        setLinksClickable(true);
        setText(builder);

        invalidate();
        requestLayout();
    }
    public RealmSpannableTextView(Context context) { // 생성자 종류별로 필요함
        this(context, null);

    }

    public RealmSpannableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public RealmSpannableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
        realm.beginTransaction();
        sentence = realm.where(SentenceRealm.class).equalTo("id", sid).findFirst();
        buildSentence(sentence);
        realm.commitTransaction();

        setMovementMethod(CustomMovementMethod.getInstance());
        setLinksClickable(true);
        setText(builder);

        invalidate();
        requestLayout();
    }

    private void init(Context context, AttributeSet attrs) {
        realm = Realm.getDefaultInstance();
        TypedArray types = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.RealmSpannableTextView, 0, 0);
        try {
            sid = types.getInteger(R.styleable.RealmSpannableTextView_sid, 0);
        } finally {
            types.recycle();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    public void buildSentence(SentenceRealm sentenceRealm) {
        builder = new SpannableStringBuilder(sentenceRealm.getSentence());
        startIndex = 0;
        final RealmList<WordRealm> words = sentenceRealm.getWordList();
        int size = words.size();
        for (int i=0;i<size;i++) {
            final WordRealm word = words.get(i);
            final int finalI = i;
            RealmClickableSpan span = new RealmClickableSpan() {
                @Override
                public void onClick(View widget) {

                    if(mListener!=null){
                      mListener.onClickItem(word.getId(), finalI);
                    }
                }

                @Override
                public void onDoubleClick(View widget) {
                    if(mListener!=null){
                        mListener.onLongClickItem(word.getId(), finalI);
                    }

                }
            };

            builder.setSpan(span, startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (word.isHighlight()) {
                builder.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.highlight)), startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
            startIndex += word.getWord().length() + 1;
        }
    }
}
