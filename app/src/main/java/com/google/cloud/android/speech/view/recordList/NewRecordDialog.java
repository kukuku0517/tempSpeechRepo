package com.google.cloud.android.speech.view.recordList;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.util.FileUtil;

import java.io.File;

/**
 * Created by samsung on 2017-10-07.
 */

public class NewRecordDialog extends DialogFragment {
    public interface NewRecordDialogListener {
        public void onDialogPositiveClick(String title, String tag, int requestCode);

        public void onDialogNegativeClick();
    }

    // Use this instance of the interface to deliver action events
    NewRecordDialogListener mListener;
    private EditText mEditTextTitle;
    private EditText mEditTextTag;
    private int requestCode;

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mListener = (NewRecordDialogListener) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_new_record, null);
        mEditTextTitle = (EditText) view.findViewById(R.id.et_new_record_title);
        mEditTextTag = (EditText) view.findViewById(R.id.et_new_record_tag);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        builder.setView(view)
                // Add action buttons
                .setPositiveButton("시작하기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String title = mEditTextTitle.getText().toString();
                        String tag = mEditTextTag.getText().toString();


                        File file = new File(FileUtil.getFilename(title));
                        if (file.exists()) {
                            Toast.makeText(getContext(), "이미 존재하는 제목 입니다", Toast.LENGTH_SHORT).show();
                        } else if (title.equals("")) {
                            Toast.makeText(getContext(), "제목을 입력하세요", Toast.LENGTH_SHORT).show();
                        }else{
                            mListener.onDialogPositiveClick(title, tag, requestCode);
                            dialog.dismiss();
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
