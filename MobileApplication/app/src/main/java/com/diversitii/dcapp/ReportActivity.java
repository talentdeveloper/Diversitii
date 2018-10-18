package com.diversitii.dcapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.UnsupportedEncodingException;

public class ReportActivity extends MusicActivity {
    private static final String TAG = ReportActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Utils.isAndroidTV(this)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else{
            if (Utils.isPortrait(this)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        setContentView(R.layout.activity_report);

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (!Utils.isPortrait(this)) {
            getOffers();
        }
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

    /**
     * Gets offer text file from cloud storage, if it is newer than the stored file. User must be
     * logged in with Firebase. NOTE: this is a duplicate of the code in RulesOffersActivity, fix.
     */
    private void getOffers() {
        // Check age of offers file
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = shprefs.edit();
        final long savedUpdateTime = shprefs.getLong(Constants.SHPREFS_OFFER_UPDATE_MILLIS, 0);
        final StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(Constants.STORAGE_URL + Constants.OFFERS_FILE);
        storageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                final long updateTime = storageMetadata.getUpdatedTimeMillis();
                if (updateTime > savedUpdateTime) {
                    // Get new file
                    storageRef.getBytes(Constants.MAX_OFFERS_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            String offers = null;
                            try {
                                offers = new String(bytes, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                Log.e(TAG, "Error getting offers: " + e.toString());
                                // Offers error text displays by default
                            }
                            editor.putString(Constants.SHPREFS_OFFER, offers);
                            editor.putLong(Constants.SHPREFS_OFFER_UPDATE_MILLIS, updateTime);
                            editor.apply();
                            updateOffersUi(offers);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error getting offers: " + e.toString());
                            // There was an error, use the saved text if any
                            updateOffersUi(null);
                        }
                    });
                } else {
                    // Use stored file
                    updateOffersUi(null);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error checking offers: " + e.toString());
                // There was an error, use the saved text if any
                updateOffersUi(null);
            }
        });
    }

    /**
     * Displays the offer or saved offer if any, or error message on error.
     * NOTE: this is a duplicate of the code in RulesOffersActivity, fix.
     *
     * @param offersText the contents of the offers text file, if retrieved
     */
    private void updateOffersUi(String offersText) {
        TextView tv = findViewById(R.id.offersOffersText);
        if (offersText != null) {
            tv.setText(offersText);
        } else { // load saved text or error text if nothing saved
            tv.setText(getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE)
                    .getString(Constants.SHPREFS_OFFER, getString(R.string.error_offers)));
        }
        tv.setVisibility(View.VISIBLE);
        findViewById(R.id.offer_progress).setVisibility(View.INVISIBLE); // hide spinner
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
    }
}
