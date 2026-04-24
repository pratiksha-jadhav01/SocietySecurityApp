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
 * Utility class to handle local notifications for the Notice Board.
 * Creates notification channels and displays alerts for new society notices.
 */
public class NotificationHelper {

    // Channel ID — must be unique per channel type
    private static final String CHANNEL_ID = "notice_channel";
    private static final String CHANNEL_NAME = "Notice Board";

    /**
     * Creates a notification channel for Android 8.0 (API 26) and above.
     * This should be called once (e.g., in onCreate of the main activity or Application class).
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
     * @param context Application context.
     * @param title Title of the notice.
     * @param message Content of the notice.
     * @param openActivity The activity to open when the notification is tapped.
     */
    public static void showNoticeNotification(Context context, String title, String message, Class<?> openActivity) {

        // Create intent to open OwnerNoticeActivity on tap
        Intent intent = new Intent(context, openActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // PendingIntent — required for notification tap action
        // FLAG_IMMUTABLE is mandatory for Android 12+ (API 31+)
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        // Build notification using a default icon (update to R.drawable.ic_notifications if available)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) 
                .setContentTitle(title != null ? title : "New Notice")
                .setContentText(message != null ? message : "Check the notice board")
                // BigTextStyle allows displaying the full message in expanded view
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message != null ? message : ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true) // Dismisses the notification when tapped
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        // Check POST_NOTIFICATIONS permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Permission not granted, skip notification
            }
        }

        // Use current time as ID to ensure each notice gets a unique notification entry
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
