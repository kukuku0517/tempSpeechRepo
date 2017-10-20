package com.google.cloud.android.speech.View.RecordResult.Adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.cloud.android.speech.Data.Realm.SentenceRealm;
import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.databinding.ItemRecordResultBinding;

import io.realm.Realm;

/**
 * Created by samsung on 2017-10-08.
 */

public class ResultRealmViewHolder extends RecyclerView.ViewHolder{

    ItemRecordResultBinding binding;

    public ResultRealmViewHolder(View view) {
        super(view);
        binding= DataBindingUtil.bind(view);


    }

    void onClickView(View view){

    }

    void onBindView(SentenceRealm sentenceRealm, Context context, final Realm realm){
        binding.llContainer.removeAllViews();
        for (final WordRealm w : sentenceRealm.getWordList()) {

            EditText editText = new EditText(context);
            editText.setHint(w.getWord());
            Log.d("words",w.getStartMillis()+" : "+w.getWord());
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

            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        realm.beginTransaction();
                        w.setWord(((EditText) v).getText().toString());
                        realm.commitTransaction();
                    }
                }
            });
//            editText.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                }
//
//                @Override
//                public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                }
//
//                @Override
//                public void afterTextChanged(Editable s) {
//                    realm.beginTransaction();
//                    w.setWord(s.toString());
//                    realm.commitTransaction();
//                }
//            });

            binding.llContainer.addView(editText);

        }
    }
}