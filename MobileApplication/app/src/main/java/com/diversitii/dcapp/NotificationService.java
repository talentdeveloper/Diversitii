package com.diversitii.dcapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Catch notifications received while the app is open.
 */
public class NotificationService extends FirebaseMessagingService {
    static final int NOTIF_ID = 4267; // Allow system to update existing notification, if any

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        /*
         * Note: only have 10 seconds to handle: if data should be processed by long-running
         * job, use Firebase Job Dispatcher
         */

        if (remoteMessage.getData().size() > 0) {
            // If you include custom data in a notification, handle it here
        }

        // Create notification
        if (remoteMessage.getNotification() != null) {
            // Define action
            Intent resultIntent = new Intent(this, RulesOffersActivity.class);
            // Clicking the notification opens a new activity, no need to create artificial back stack
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(resultPendingIntent)
                    .setContentTitle(remoteMessage.getNotification().getTitle()) // fine if no title given
                    .setContentText(remoteMessage.getNotification().getBody());
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIF_ID, mBuilder.build());
        }
    }
}
