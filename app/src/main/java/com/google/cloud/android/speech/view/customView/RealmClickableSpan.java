package com.google.cloud.android.speech.view.customView;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

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
