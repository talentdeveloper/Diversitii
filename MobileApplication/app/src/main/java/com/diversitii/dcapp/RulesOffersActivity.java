package com.diversitii.dcapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class RulesOffersActivity extends MusicActivity {
    private static final String TAG = RulesOffersActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.isPortrait(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setContentView(R.layout.activity_rules_offers);

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // If we got here from a notification, close the notification
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NotificationService.NOTIF_ID);

        Utils.getOffers(this, null, R.id.offersOffersText, R.id.offer_progress);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.previousBtn:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
            case R.id.sendEmailBtn:
                Utils.sendReport(this);
                break;
            case R.id.whiteMicroscope:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                finish();
                break;
            default:
                Log.e(TAG, "Unhandled button press");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
    }
}
