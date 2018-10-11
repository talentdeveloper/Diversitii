package com.diversitii.dcapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Additional settings for game play.
 */
public class GameSettingsActivity extends MusicActivity {
    private static final String TAG = GameSettingsActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.isPortrait(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setContentView(R.layout.activity_game_settings);

        // Let device volume buttons control sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        loadSavedSettings();
    }

    private void loadSavedSettings() {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        // "Game score"
        int savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_SCORES, 0);
        ((TextView) findViewById(R.id.tv_game_score)).setText(String.valueOf(Constants.SCORES_VALUES[savedIndex]));
        // "Points value"
        savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_POINTS, 0);
        ((TextView) findViewById(R.id.tv_points_value)).setText(String.valueOf(Constants.POINTS_VALUES[savedIndex]));
        // Lives
        savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_LIVES, 0);
        ((TextView) findViewById(R.id.tv_game_lives)).setText(String.valueOf(Constants.LIVES_VALUES[savedIndex]));
        // Timer
        savedIndex = shprefs.getInt(Constants.SHPREFS_SETTINGS_TIMER, 0);
        ((TextView) findViewById(R.id.tv_timer)).setText(String.valueOf(
                (Constants.TIMER_VALUES[savedIndex]) != 0 ?
                        Constants.TIMER_VALUES[savedIndex] :
                        getString(R.string.default_timer)));
    }

    public void onClick(View v) {
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shprefs.edit();
        int i;
        String newVal;
        switch (v.getId()) {
            case R.id.btn_game_score:
                if (!Utils.isInGame(shprefs)) {
                    i = shprefs.getInt(Constants.SHPREFS_SETTINGS_SCORES, 0);
                    i = (i + 1) < Constants.SCORES_VALUES.length ? (i + 1) : 0; // Increment array index
                    editor.putInt(Constants.SHPREFS_SETTINGS_SCORES, i);
                    editor.apply();
                    newVal = Constants.SCORES_VALUES[i] + "";
                    ((TextView) findViewById(R.id.tv_game_score)).setText(newVal);
                } else {
                    Toast.makeText(this, getString(R.string.error_game_in_progress), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_points_value:
                if (!Utils.isInGame(shprefs)) {
                    i = shprefs.getInt(Constants.SHPREFS_SETTINGS_POINTS, 0);
                    i = (i + 1) < Constants.POINTS_VALUES.length ? (i + 1) : 0; // Increment array index
                    editor.putInt(Constants.SHPREFS_SETTINGS_POINTS, i);
                    editor.apply();
                    newVal = Constants.POINTS_VALUES[i] + "";
                    ((TextView) findViewById(R.id.tv_points_value)).setText(newVal);
                } else {
                    Toast.makeText(this, getString(R.string.error_game_in_progress), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_game_lives:
                if (!Utils.isInGame(shprefs)) {
                    i = shprefs.getInt(Constants.SHPREFS_SETTINGS_LIVES, 0);
                    i = (i + 1) < Constants.LIVES_VALUES.length ? (i + 1) : 0; // Increment array index
                    editor.putInt(Constants.SHPREFS_SETTINGS_LIVES, i);
                    int lives = Constants.LIVES_VALUES[i];
                    editor.putInt(Constants.SHPREFS_LIVES_SINGLE_PLAYER, lives);
                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER1, lives);
                    editor.putInt(Constants.SHPREFS_LIVES_PLAYER2, lives);
                    editor.apply();
                    newVal = Constants.LIVES_VALUES[i] + "";
                    ((TextView) findViewById(R.id.tv_game_lives)).setText(newVal);
                } else {
                    Toast.makeText(this, getString(R.string.error_game_in_progress), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_timer:
                i = shprefs.getInt(Constants.SHPREFS_SETTINGS_TIMER, 0);
                i = (i + 1) < Constants.TIMER_VALUES.length ? (i + 1) : 0; // Increment array index
                editor.putInt(Constants.SHPREFS_SETTINGS_TIMER, i);
                editor.apply();
                newVal = (Constants.TIMER_VALUES[i]) != 0 ? Constants.TIMER_VALUES[i] + "" : getString(R.string.default_timer);
                ((TextView) findViewById(R.id.tv_timer)).setText(newVal);
                break;
            case R.id.btn_game_rules:
                startActivity(new Intent(getApplicationContext(), RulesOffersActivity.class));
                finish();
                break;
            case R.id.nextBtn:
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
