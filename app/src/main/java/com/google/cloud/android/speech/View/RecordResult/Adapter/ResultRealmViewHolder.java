package com.google.cloud.android.speech.View.RecordResult.Adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.DateUtil;
import com.google.cloud.android.speech.View.CustomView.RealmSpannableTextView;
import com.google.cloud.android.speech.databinding.ItemRecordResultBinding;

import io.realm.Realm;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmViewHolder extends RecyclerView.ViewHolder {

    ItemRecordResultBinding binding;

    public ResultRealmViewHolder(View view) {
        super(view);
        binding = DataBindingUtil.bind(view);


    }

    void onClickView(View view) {

    }

    final String TAG = "customView";
    int startIndex = 0;


    void onBindView(SentenceRealm sentenceRealm, Context context, final Realm realm) {
        StringBuilder s = new StringBuilder();

        binding.setSentenceString(s.toString());
        binding.setSentenceTime(DateUtil.durationToDate((int) sentenceRealm.getStartMillis()));
        binding.setSentence(sentenceRealm);

    }

    private class RealmSpan extends ClickableSpan {

        WordRealm word;

        public RealmSpan(WordRealm word) {
            this.word = word;
        }

        @Override
        public void onClick(View widget) {
            Log.i(TAG, word.getWord());
        }

    }
}