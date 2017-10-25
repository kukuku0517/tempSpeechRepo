package com.google.cloud.android.speech.View.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;

import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.Event.SeekEvent;
import com.google.cloud.android.speech.R;

import org.greenrobot.eventbus.EventBus;

import io.realm.Realm;

/**
 * Created by USER on 2017-10-25.
 */

public class RealmSpannableTextView extends android.support.v7.widget.AppCompatTextView {

    private SpannableStringBuilder builder;
    private int sid;
    private SentenceRealm sentence;
    private int startIndex = 0;
    private final String TAG = "customView";
    private Realm realm;

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
        if (sid != 0) {
            setMovementMethod(CustomMovementMethod.getInstance());
            setLinksClickable(true);
            setText(builder);
        }
    }

    public void buildSentence(SentenceRealm sentenceRealm) {
        builder = new SpannableStringBuilder(sentenceRealm.getSentence());
        startIndex = 0;
        for (final WordRealm word : sentenceRealm.getWordList()) {
            RealmClickableSpan span = new RealmClickableSpan() {
                @Override
                public void onClick(View widget) {
                    realm.beginTransaction();
                    word.setHighlight(!word.isHighlight());
                    realm.commitTransaction();
                    EventBus.getDefault().post(new SeekEvent(word.getStartMillis()));
                }

                @Override
                public void onDoubleClick(View widget) {
                    AlternativeDialogFragment dialog = new AlternativeDialogFragment();
                    dialog.setWord(word);
                    dialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "NewRecordDialogFragment");
                }
            };

            builder.setSpan(span, startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (word.isHighlight()) {
//                float[] outerR = new float[]{12, 12, 12, 12, 0, 0, 0, 0};
//                RectF inset = new RectF(6, 6, 6, 6);
//                float[] innerR = new float[]{12, 12, 0, 0, 12, 12, 0, 0};
//                ShapeDrawable drawable = new ShapeDrawable(new RoundRectShape(outerR, inset, innerR));
//                drawable.setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);
//                builder.setSpan(drawable, startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                builder.setSpan(new ForegroundColorSpan(Color.WHITE), startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.primary)), startIndex, startIndex + word.getWord().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            startIndex += word.getWord().length() + 1;
        }

    }


}
