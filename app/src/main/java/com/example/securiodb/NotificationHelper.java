package com.example.securiodb;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Helper class to manage notification channels and showing notifications for society notices.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID   = "notice_channel";
    private static final String CHANNEL_NAME = "Notice Board";
    private static final int NOTIF_ID_BASE   = 3000;

    /**
     * Creates a notification channel for notice board alerts. 
     * Required for Android 8.0 (Oreo) and above.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts for new society notices");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Displays a notification when a new notice is posted.
     * @param context The application context.
     * @param title The title of the notice.
     * @param message The body content of the notice.
     * @param openActivity The activity to open when the notification is tapped.
     */
    public static void showNoticeNotification(
            Context context,
            String title,
            String message,
            Class<?> openActivity) {

        // Create intent to open OwnerNoticeActivity on tap
        Intent intent = new Intent(context, openActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // PendingIntent — required for notification tap action
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            // FLAG_IMMUTABLE is required for Android 12+ (API 31+)
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon as fallback
                .setContentTitle(title != null ? title : "New Notice")
                .setContentText(message != null ? message : "Check the notice board")
                // BigTextStyle allows viewing long messages in the expanded notification
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message != null ? message : ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        // Check for POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Cannot post notification without permission
                return;
            }
        }

        // Use a timestamp-based ID to ensure each notice generates a unique notification
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
