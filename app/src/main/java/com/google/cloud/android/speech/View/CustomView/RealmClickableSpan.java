package com.google.cloud.android.speech.View.CustomView;

import android.app.Activity;
import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;

import com.google.cloud.android.speech.Data.Realm.WordRealm;
import com.google.cloud.android.speech.View.RecordList.NewRecordDialog;

import io.realm.Realm;

/**
 * Created by USER on 2017-10-25.
 */

public abstract class RealmClickableSpan extends ClickableSpan {

    Realm realm;
    public RealmClickableSpan() {
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setUnderlineText(false);
    }

    public abstract void onClick(View widget);
    public abstract void onDoubleClick(View widget);


}
