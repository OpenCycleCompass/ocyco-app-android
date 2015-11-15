package de.mytfg.jufo.ibis.util;

import android.app.Dialog;
import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import de.mytfg.jufo.ibis.R;

public class TransparentLoadingOverlay extends Dialog {

    public TransparentLoadingOverlay(Context context) {
        super(context, R.style.TransparentLoadingLayout);
        setTitle(null);
        setCancelable(false);
        setOnCancelListener(null);
        // create layout
        setContentView(R.layout.transparent_loading_layout);
    }

    @Override
    public void show() {
        super.show();
        // animate ImageView
        ImageView iv_loading = (ImageView) findViewById(R.id.iv_loading);
        Animation loading_animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotation);
        iv_loading.setAnimation(loading_animation);
        iv_loading.startAnimation(loading_animation);

    }
}

