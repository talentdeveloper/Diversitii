package com.diversitii.dcapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity wrapper with background music service.
 */
public class MusicActivity extends AppCompatActivity {
    private boolean mIsServiceBound = false;

    MusicService mMusicService;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mMusicService = ((MusicService.ServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mMusicService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start background music
        doBindService();
        startService(new Intent(this, MusicService.class));
    }

    /**
     * Binds music service to Activity.
     */
    void doBindService() {
        bindService(new Intent(this, MusicService.class), mServiceConn, Context.BIND_AUTO_CREATE);
        mIsServiceBound = true;
    }

    /**
     * Unbinds music service from Activity.
     */
    void doUnbindService() {
        if (mIsServiceBound) {
            unbindService(mServiceConn);
            mIsServiceBound = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMusicService != null) {
            mMusicService.pauseMusic(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMusicService != null) {
            mMusicService.playMusic(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
}
