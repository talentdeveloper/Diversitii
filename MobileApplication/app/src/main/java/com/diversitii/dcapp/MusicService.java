package com.diversitii.dcapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Background music service.
 */
public class MusicService extends Service implements MediaPlayer.OnErrorListener {
    private static final String TAG = MusicService.class.getName();

    private final IBinder mBinder = new ServiceBinder();
    private MediaPlayer mMediaPlayer;
    private int mPausedPosition;

    private final static int STATE_PLAYING = 0;
    private final static int STATE_PAUSED = 1;
    private final static int STATE_STOPPED = 2;
    private int mState = STATE_PLAYING;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        playMusic(false);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopMusic(false);
    }

    @Override
    public boolean onError(MediaPlayer mp, int i, int i1) {
        Log.e(TAG, "Background music error");
        stopMusic(false);
        return false;
    }

    class ServiceBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Plays music.
     *
     * @param isGlobal whether turning on music is app-wide
     */
    public void playMusic(boolean isGlobal) {
        if (isGlobal) {
            mState = STATE_PLAYING;
        }
        SharedPreferences shprefs = getSharedPreferences(Constants.SHPREFS, Context.MODE_PRIVATE);
        if (shprefs.getBoolean(Constants.SHPREFS_MUSIC_ON, true) && mState == STATE_PLAYING) {
            if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                mMediaPlayer.seekTo(mPausedPosition);
                mMediaPlayer.start();
            } else if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(this, R.raw.jazzyfrenchy);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();
                mMediaPlayer.setOnErrorListener(this);
            }
        }
    }

    /**
     * Pauses music.
     *
     * @param isGlobal whether pausing music is app-wide
     */
    public void pauseMusic(boolean isGlobal) {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPausedPosition = mMediaPlayer.getCurrentPosition();
        }
        if (isGlobal) {
            mState = STATE_PAUSED;
        }
    }

    /**
     * Stops music.
     *
     * @param isGlobal whether stopping music is app-wide
     */
    public void stopMusic(boolean isGlobal) {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (isGlobal) {
            mState = STATE_STOPPED;
        }
    }
}
