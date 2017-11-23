package com.google.cloud.android.speech.view.splash;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.PersistableBundle;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.databinding.ActivityRecordBinding;
import com.google.cloud.android.speech.databinding.ActivitySplashBinding;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.viksaa.sssplash.lib.activity.AwesomeSplash;
import com.viksaa.sssplash.lib.cnst.Flags;
import com.viksaa.sssplash.lib.model.ConfigSplash;

public class SplashActivity extends AppCompatActivity {

    ActivitySplashBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash);
        ImageView logo = binding.logo;
        final TextView sub = binding.subtitle;

        Animation animationLogo = AnimationUtils.loadAnimation(this,R.anim.fade_in_slow);
        animationLogo.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Animation animationSub =  AnimationUtils.loadAnimation(getBaseContext(),R.anim.fade_in_slow);
                animationSub.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(getBaseContext(), ListActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                        try {
                            t.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        t.start();

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                sub.startAnimation(animationSub);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        logo.startAnimation(animationLogo);

    }
}