package com.diversitii.dcapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class SlidingActivity extends MusicActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean mIsPortrait = Utils.isPortrait(this);
        boolean mIsAndroidTV = Utils.isAndroidTV(this);
        if (mIsAndroidTV) {
            //Toast.makeText(this, "This is Android TV", Toast.LENGTH_LONG).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            //setContentView(R.layout.sliding_panel);
        } else {
            if (Utils.isPortrait(this)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                //Toast.makeText(this,"This is Tablet",Toast.LENGTH_LONG).show();
            }
            //setContentView(R.layout.sliding_panel);
        }

    }
}
