package fr.catch23.happyshare

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Notify{
    companion object {
        private var notificationID = 123
        fun uploadingNotification(context: Context): NotificationCompat.Builder {
            var builder = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                    .setSmallIcon(R.drawable.ic_notif_happyshare)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notify_uploading))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setProgress(0, 0, true)
            with(NotificationManagerCompat.from(context)) {
                notify(notificationID, builder.build())
            }
            return builder
        }

        fun uploadedNotification(context: Context, builder: NotificationCompat.Builder, image_url: String) {
            NotificationManagerCompat.from(context).apply {
                var notificationIntent = Intent(Intent.ACTION_VIEW, Uri.parse(image_url));
                var contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                builder.setProgress(0, 0, false)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.notify_upload_success))
                        .setOngoing(false)
                        .setContentIntent(contentIntent)
                notify(notificationID, builder.build())
            }
        }

        fun errorNotificationOpenSettings(context: Context, builder: NotificationCompat.Builder) {
            NotificationManagerCompat.from(context).apply {
                var notificationIntent = Intent(context, HappyShareSettingsActivity::class.java);
                var contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                builder.setProgress(0, 0, false)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.notify_settings_empty))
                        .setOngoing(false)
                        .setContentIntent(contentIntent)
                notify(notificationID, builder.build())
            }
        }

        fun errorNotification(context: Context, builder: NotificationCompat.Builder, error_msg: String) {
            NotificationManagerCompat.from(context).apply {
                builder.setProgress(0, 0, false)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notify_upload_error) + "\nError: " + error_msg))
                        .setContentText(context.getString(R.string.notify_upload_error))
                        .setOngoing(false)
                notify(notificationID, builder.build())
            }
        }
    }
}