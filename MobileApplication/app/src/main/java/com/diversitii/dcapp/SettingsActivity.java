package com.diversitii.dcapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.diversitii.dcapp.database.TopicsDao;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends MusicActivity {
    private static final String TAG = SettingsActivity.class.getName();

    private MediaPlayer mMediaPlayer = null; // For sound effects only
    private String mTopicEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.isPortrait(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setContentView(R.layout.activity_settings);

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Sound effects seek bar
        SeekBar seekBar = findViewById(R.id.seekBar);
        // Load saved setting
        seekBar.setProgress(getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE).getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 10);
        // Collect changes
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int mProgress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                mProgress = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSoundEffectVolume(mProgress * 10);
            }
        });

        // Topic search box (note for multiple topics with the same name, only 1 wil be deleted)
        AutoCompleteTextView textView = findViewById(R.id.topicBoxSearchText);
        textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                mTopicEntry = ((AppCompatTextView) view).getText().toString();
            }
        });
        textView.setAdapter(new AutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line));
    }

    /**
     * Sets the global sound effect volume to the given percent.
     *
     * @param volPercent the percent from 0 - 100 of max volume
     */
    private void saveSoundEffectVolume(int volPercent) {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shprefs.edit();
        editor.putInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, volPercent);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        toggleNotifications(getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE)
                .getBoolean(Constants.SHPREFS_NOTIFICATIONS_OK, Constants.DEFAULT_NOTIFICATIONS_OK));
    }

    /**
     * Changes ON/OFF text on notifications button.
     *
     * @param isOn are notifications enabled
     */
    private void toggleNotifications(boolean isOn) {
        if (isOn) {
            ((TextView) findViewById(R.id.notificationsOffOn)).setText(getString((R.string.off)));
        } else {
            ((TextView) findViewById(R.id.notificationsOffOn)).setText(getString((R.string.on)));
        }
    }

    public void onClick(View v) {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shprefs.edit();
        switch (v.getId()) {
            case R.id.playMusic:
                editor.putBoolean(Constants.SHPREFS_MUSIC_ON, true);
                editor.apply();
                if (mMusicService != null) {
                    mMusicService.playMusic(true);
                } else {
                    doBindService();
                    Intent intent = new Intent(this, MusicService.class);
                    startService(intent);
                }
                break;
            case R.id.pauseMusic:
                editor.putBoolean(Constants.SHPREFS_MUSIC_ON, false);
                editor.apply();
                if (mMusicService != null) {
                    mMusicService.pauseMusic(true);
                }
                break;
            case R.id.stopMusic:
                editor.putBoolean(Constants.SHPREFS_MUSIC_ON, false);
                editor.apply();
                if (mMusicService != null) {
                    mMusicService.stopMusic(true);
                    doUnbindService();
                }
                break;
            case R.id.deleteBtn:
                if (new TopicsDao().deleteTopic(this, mTopicEntry)) {
                    Toast.makeText(this, getString(R.string.topic_deleted), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.error_topic_delete), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ll_notifications:
                boolean isOn = !(shprefs.getBoolean(Constants.SHPREFS_NOTIFICATIONS_OK, Constants.DEFAULT_NOTIFICATIONS_OK));
                if (isOn) {
                    FirebaseMessaging.getInstance().subscribeToTopic(Constants.NOTIFICATIONS_TOPIC);
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.NOTIFICATIONS_TOPIC);
                }
                editor.putBoolean(Constants.SHPREFS_NOTIFICATIONS_OK, isOn);
                editor.apply();
                toggleNotifications(isOn);
                break;
            case R.id.playBtn:
                // Sound effect
                float vol = shprefs.getInt(Constants.SHPREFS_SOUND_EFFECTS_VOL, 100) / 100f;
                if (mMediaPlayer != null) {
                    mMediaPlayer.reset();
                }
                mMediaPlayer = MediaPlayer.create(this, R.raw.general_whoosh01);
                mMediaPlayer.setVolume(vol, vol);
                mMediaPlayer.start();

                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;
            case R.id.websiteBtn:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_website))));
                break;
            case R.id.scorecardBtn:
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;
            case R.id.gameRulesBtn:
                startActivity(new Intent(getApplicationContext(), RulesOffersActivity.class));
                break;
            case R.id.previousBtn:
                startActivity(new Intent(getApplicationContext(), PacksActivity.class));
                break;
            case R.id.nextBtn:
                startActivity(new Intent(getApplicationContext(), ReportActivity.class));
                break;
            case R.id.settingsBtn:
                if (Utils.isPortrait(this)) {
                    startActivity(new Intent(getApplicationContext(), GameSettingsActivity.class));
                } else {
                    showSettingsDialog();
                }
                break;
            default:
                Log.e(TAG, "Unhandled button press");
                break;
        }
    }

    private void showSettingsDialog() {
        final SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = shprefs.edit();
        final Dialog dialog = new Dialog(this);

        // Fill screen
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        if (window != null) {
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;

            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(true);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_game_settings);

            // Load saved values
            int savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_SCORES, 0);
            ((TextView) dialog.findViewById(R.id.tv_game_score)).setText(String.valueOf(Constants.SCORES_VALUES[savedIndex]));
            savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_POINTS, 0);
            ((TextView) dialog.findViewById(R.id.tv_points_value)).setText(String.valueOf(Constants.POINTS_VALUES[savedIndex]));
            savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_LIVES, 0);
            ((TextView) dialog.findViewById(R.id.tv_game_lives)).setText(String.valueOf(Constants.LIVES_VALUES[savedIndex]));
            savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_TIMER, 0);
            ((TextView) dialog.findViewById(R.id.tv_timer)).setText(String.valueOf(
                    (Constants.TIMER_VALUES[savedIndex]) != 0 ?
                            Constants.TIMER_VALUES[savedIndex] :
                            getString(R.string.default_timer)));

            // Buttons
            final Activity thiss = this;
            dialog.findViewById(R.id.btn_game_score).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!Utils.isInGame(shprefs)) {
                        int i = shprefs.getInt(Constants.SHPREFS_SETTINGS_SCORES, 0);
                        i = (i + 1) < Constants.SCORES_VALUES.length ? (i + 1) : 0; // Increment array index
                        editor.putInt(Constants.SHPREFS_SETTINGS_SCORES, i);
                        editor.apply();
                        String newVal = Constants.SCORES_VALUES[i] + "";
                        ((TextView) dialog.findViewById(R.id.tv_game_score)).setText(newVal);
                    } else {
                        Toast.makeText(thiss, getString(R.string.error_game_in_progress), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            dialog.findViewById(R.id.btn_points_value).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!Utils.isInGame(shprefs)) {
                        int i = shprefs.getInt(Constants.SHPREFS_SETTINGS_POINTS, 0);
                        i = (i + 1) < Constants.POINTS_VALUES.length ? (i + 1) : 0; // Increment array index
                        editor.putInt(Constants.SHPREFS_SETTINGS_POINTS, i);
                        editor.apply();
                        String newVal = Constants.POINTS_VALUES[i] + "";
                        ((TextView) dialog.findViewById(R.id.tv_points_value)).setText(newVal);
                    } else {
                        Toast.makeText(thiss, getString(R.string.error_game_in_progress), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            dialog.findViewById(R.id.btn_game_lives).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!Utils.isInGame(shprefs)) {
                        int i = shprefs.getInt(Constants.SHPREFS_SETTINGS_LIVES, 0);
                        i = (i + 1) < Constants.LIVES_VALUES.length ? (i + 1) : 0; // Increment array index
                        editor.putInt(Constants.SHPREFS_SETTINGS_LIVES, i);
                        int lives = Constants.LIVES_VALUES[i];
                        editor.putInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, lives);
                        editor.putInt(Constants.SHPREFS_LIVES_PLAYER1, lives);
                        editor.putInt(Constants.SHPREFS_LIVES_PLAYER2, lives);
                        editor.putInt(Constants.SHPREFS_LIVES_PLAYER3, lives);
                        editor.putInt(Constants.SHPREFS_LIVES_PLAYER4, lives);
                        editor.apply();
                        String newVal = Constants.LIVES_VALUES[i] + "";
                        ((TextView) dialog.findViewById(R.id.tv_game_lives)).setText(newVal);
                    } else {
                        Toast.makeText(thiss, getString(R.string.error_game_in_progress), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            dialog.findViewById(R.id.btn_timer).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int i = shprefs.getInt(Constants.SHPREFS_SETTINGS_TIMER, 0);
                    i = (i + 1) < Constants.TIMER_VALUES.length ? (i + 1) : 0; // Increment array index
                    editor.putInt(Constants.SHPREFS_SETTINGS_TIMER, i);
                    editor.apply();
                    String newVal = (Constants.TIMER_VALUES[i]) != 0 ? Constants.TIMER_VALUES[i] + "" : getString(R.string.default_timer);
                    ((TextView) dialog.findViewById(R.id.tv_timer)).setText(newVal);
                }
            });
            dialog.findViewById(R.id.btn_game_rules).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(getApplicationContext(), RulesOffersActivity.class));
                    finish();
                }
            });
            dialog.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            dialog.show();
            dialog.getWindow().setAttributes(lp);
        } else {
            Log.e(TAG, "Null dialog window");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) { // clean up sound
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
    }
}
