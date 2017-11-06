package com.google.cloud.android.speech.view.customView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.util.RealmUtil;

import io.realm.Realm;

/**
 * Created by USER on 2017-10-25.
 */

public class AlternativeDialogFragment extends AppCompatDialogFragment {

    private EditText mEditTextAlt;
    WordRealm word;

    public void setWord(WordRealm word) {
        this.word = word;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_alternative, null);
        mEditTextAlt = (EditText) view.findViewById(R.id.et_alternative);
        mEditTextAlt.setHint(word.getWord());


        builder.setView(view)
                // Add action buttons
                .setPositiveButton("시작하기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final String alt = mEditTextAlt.getText().toString();
                        if (alt.length() > 0) {
                            Realm realm = Realm.getDefaultInstance();
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    SentenceRealm sentenceRealm = realm.where(SentenceRealm.class).equalTo("id", word.getSentenceId()).findFirst();
                                    RealmUtil.updateWordRealm(word, sentenceRealm, alt);
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "내용을 입력하세요", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}
