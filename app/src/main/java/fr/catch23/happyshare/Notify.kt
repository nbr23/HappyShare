package fr.catch23.happyshare

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat

class Notify{
    companion object {
        private var notificationID = 123
        fun uploadingNotification(context: Context): NotificationCompat.Builder {
            var builder = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                    .setSmallIcon(android.support.coreui.R.drawable.notification_icon_background)
                    .setContentTitle("HappyShare")
                    .setContentText("Upload to in progress…")
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
                        .setContentText("Upload completed!")
                        .setOngoing(false)
                        .setContentIntent(contentIntent)
                notify(notificationID, builder.build())
            }
        }

        fun errorNotification(context: Context, builder: NotificationCompat.Builder) {
            NotificationManagerCompat.from(context).apply {
                builder.setProgress(0, 0, false)
                        .setContentText("Could not upload the picture, try again later")
                        .setOngoing(false)
                notify(notificationID, builder.build())
            }
        }
    }
}