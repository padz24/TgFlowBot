package com.tgflowbot;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_logo);
        TextView title = findViewById(R.id.tv_title);
        TextView subtitle = findViewById(R.id.tv_subtitle);

        logo.setAlpha(0f);
        logo.setTranslationY(60f);
        title.setAlpha(0f);
        title.setTranslationY(40f);
        subtitle.setAlpha(0f);

        Animator logoFade = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        logoFade.setDuration(600);
        Animator logoMove = ObjectAnimator.ofFloat(logo, "translationY", 60f, 0f);
        logoMove.setDuration(600);

        Animator titleFade = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);
        titleFade.setDuration(500);
        Animator titleMove = ObjectAnimator.ofFloat(title, "translationY", 40f, 0f);
        titleMove.setDuration(500);

        Animator subtitleFade = ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 0.8f);
        subtitleFade.setDuration(400);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(logoFade, logoMove);
        set.play(titleFade).with(titleMove).after(200);
        set.play(subtitleFade).after(400);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();

        new Handler(getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, WorkflowListActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1800);
    }
}
