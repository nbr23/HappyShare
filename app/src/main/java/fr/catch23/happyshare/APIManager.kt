package fr.catch23.happyshare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Base64
import android.widget.Toast

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

class APIManager(private val context: Context, private val mHandler: Handler) {
    private val API_ENDPOINT: String
    private val API_RESULT_URL: String
    private val API_FROM_FIELD: String
    private val CHANNEL_ID: String
    private var media_uri: Uri? = null
    private var notificationID = 123

    init {
        this.API_ENDPOINT = context.getString(R.string.api_endpoint)
        this.API_RESULT_URL = context.getString(R.string.api_result_url)
        this.API_FROM_FIELD = context.getString(R.string.api_from_field)
        this.CHANNEL_ID = context.getString(R.string.notification_channel_id)
    }

    private fun displayUserMessage(message: String) {
        mHandler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    @Throws(IOException::class)
    private fun convertMediaToB64(): String {
        val inputstream = context.contentResolver.openInputStream(media_uri!!)
        var inputData = readBytes(inputstream!!)
        val b64 = Base64.encodeToString(inputData, Base64.DEFAULT)
        inputstream.close()
        return b64.replace("\n".toRegex(), "")
    }

    @Throws(JSONException::class)
    private fun getJsonQueryString(from: String, name: String, data: String): String {
        val json_param = JSONObject()
        json_param.put("from", URLEncoder.encode(from))
        json_param.put("name", "")
        json_param.put("blob", data)
        return json_param.toString()
    }

    private fun uploadingNotification(context: Context) : NotificationCompat.Builder{
        var builder = NotificationCompat.Builder(context, CHANNEL_ID)
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

    private fun uploadedNotification(builder: NotificationCompat.Builder, image_url: String) {
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

    private fun errorNotification(builder: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).apply {
            builder.setProgress(0, 0, false)
                    .setContentText("Could not upload the picture, try again later")
                    .setOngoing(false)
            notify(notificationID, builder.build())
        }
    }

    @Throws(ShareException::class)
    fun postToApi(intent: Intent) {
        val image_id: String
        var b64: String? = null
        val json_query: String
        val url: URL
        var urlConnection: HttpURLConnection? = null

        var builder = uploadingNotification(context)

        this.media_uri = intent.extras!!.getParcelable<Uri>(Intent.EXTRA_STREAM)

        try {
            b64 = convertMediaToB64()
        } catch (e: IOException) {
            errorNotification(builder)
            throw ShareException("Error while converting media to base64.")
        }

        try {
            json_query = getJsonQueryString(API_FROM_FIELD, "", b64)
        } catch (e: JSONException) {
            errorNotification(builder)
            throw ShareException("Abort: POST Request JSON malformed.")
        }

        try {
            url = URL(API_ENDPOINT)
        } catch (e: MalformedURLException) {
            errorNotification(builder)
            throw ShareException("Abort: Malformed URL.")
        }

        try {
            println(json_query)
            urlConnection = buildHttpPOSTConnection(url, json_query)
        } catch (e: IOException) {
            urlConnection?.disconnect()
            errorNotification(builder)
            throw ShareException("Abort: IOException.")
        }

        try {
            image_id = parseResponseJSON(urlConnection, "id")
        } catch (e: IOException) {
            errorNotification(builder)
            var error = getHTTPError(urlConnection)
            try {
                error = getJSONKey(error, "errmsg")
            } catch (e1: JSONException) {
            }
            throw ShareException("Abort: $error")
        } catch (e: JSONException) {
            errorNotification(builder)
            throw ShareException("Abort: Response JSON parse failed.")
        } finally {
            urlConnection.disconnect()
        }

        mHandler.post {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("HappyShare", API_RESULT_URL + image_id)
            uploadedNotification(builder, API_RESULT_URL + image_id)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Content shared, result copied to clipboard", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(JSONException::class)
    private fun getJSONKey(json_str: String, key: String): String {
        val ret_obj = JSONObject(json_str)
        return ret_obj.getString(key)
    }

    @Throws(IOException::class, JSONException::class)
    private fun parseResponseJSON(urlConnection: HttpURLConnection, key: String): String {
        val `in` = BufferedInputStream(urlConnection.inputStream)
        return getJSONKey(readString(`in`), key)
    }

    private fun getHTTPError(urlConnection: HttpURLConnection): String {
        try {
            val errorstream = urlConnection.errorStream ?: throw IOException()
            return readString(errorstream)
        } catch (e: IOException) {
            return "Error reading HTTP error message."
        }

    }

    @Throws(IOException::class)
    private fun buildHttpPOSTConnection(url: URL, query: String): HttpURLConnection {
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "POST"
        urlConnection.readTimeout = 120000
        urlConnection.connectTimeout = 120000
        urlConnection.doInput = true
        urlConnection.doOutput = true

        val os = urlConnection.outputStream
        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
        writer.write(query)
        writer.flush()
        writer.close()
        os.close()

        return urlConnection
    }

    @Throws(IOException::class)
    private fun readString(inputStream: InputStream): String {
        val sb = StringBuilder()
        var b = inputStream.read()
        while (b != -1)
        {
            sb.append(b.toChar())
            b = inputStream.read()
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    private fun readBytes(inputStream: InputStream): ByteArray {
        val byteArray = ByteArrayOutputStream()
        val bs = 2048
        val buf = ByteArray(bs)
        var size = inputStream.read(buf)

        while (size != -1)
        {
            byteArray.write(buf, 0, size)
            size = inputStream.read(buf)
        }

        return byteArray.toByteArray()
    }

}
