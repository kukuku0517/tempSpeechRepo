package com.google.cloud.android.speech.View.Recording.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.Util.DateUtil;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by USER on 2017-10-13.
 */


public class RecordRealmAdapter extends RealmRecyclerViewAdapter<SentenceRealm, RecordRealmViewHolder> {

    Context context;
    Realm realm;

    public RecordRealmAdapter(@Nullable OrderedRealmCollection<SentenceRealm> data, boolean autoUpdate, boolean updateOnModification, Context context) {
        super(data, autoUpdate, updateOnModification);
        this.context = context;
        realm = Realm.getDefaultInstance();
    }


    @Override
    public RecordRealmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordRealmViewHolder(v);
    }

    String TAG = "Speech";

    @Override
    public void onBindViewHolder(RecordRealmViewHolder holder, final int position) {


        SentenceRealm sentenceRealm = getItem(position);
        holder.binding.setSentence(sentenceRealm);
        Log.i(TAG, DateUtil.durationToDate((int) sentenceRealm.getStartMillis()));
        StringBuilder s = new StringBuilder();

        LinearLayout layout = holder.binding.llContainer;
        for (final WordRealm w : sentenceRealm.getWordList()) {
            s.append("\n" + w.getWord());
            EditText editText = new EditText(context);
            editText.setHint(w.getWord());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 5, 5, 5);
            editText.setLayoutParams(params);

            if (w.isHighlight()) {
                editText.setBackgroundColor(Color.RED);
            } else {
                editText.setBackgroundColor(Color.WHITE);
            }
            editText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    realm.beginTransaction();
                    w.setHighlight(!w.isHighlight());
                    realm.commitTransaction();
                    return true;
                }
            });

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    realm.beginTransaction();
                    w.setWord(s.toString());
                    realm.commitTransaction();
                }
            });
            layout.addView(editText);

        }


    }

    @Override
    public long getItemId(int index) {
        return getItem(index).getId();
    }

}