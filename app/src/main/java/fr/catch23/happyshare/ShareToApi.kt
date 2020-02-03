package fr.catch23.happyshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast

class ShareToApi : Activity() {
    private var mHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent.action
        val type = intent.type
        val context = this.applicationContext
        mHandler = Handler()
        val activity = this

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

