package fr.catch23.happyshare

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Toast

class ShareToApi : Activity() {
    private var mHandler: Handler? = null



    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(getString(R.string.notification_channel_id), name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent.action
        val type = intent.type
        val context = this.applicationContext
        mHandler = Handler()
        val activity = this

        createNotificationChannel(context)

        if (Intent.ACTION_SEND == action && type != null) {
            val apimger = APIManager(context, mHandler!!)


            val thread = object : Thread() {
                override fun run() {
                    try {
                        apimger.postToApi(intent)
                    } catch (e: ShareException) {
                        mHandler!!.post {
                            Toast.makeText(context, e.user_message, Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }
            thread.start()
        }
        activity.finish()
    }
}

