package fr.catch23.happyshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

class APIManager(private val context: Context, private val mHandler: Handler) {
    private var media_uri: Uri? = null

    private fun displayUserMessage(message: String) {
        mHandler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    private fun compressImage(data: ByteArray): ByteArray {
        var image = BitmapFactory.decodeStream(data.inputStream())
        if (image.byteCount < 999999) {
            return data
        }

        val outputFile = File.createTempFile("happyshare", "jpg", context.cacheDir)
        val compressed_strm = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 95, outputFile.outputStream())

        val exif = ExifInterface(data.inputStream())
        if (exif.hasAttribute("Orientation")) {
            val newexif = ExifInterface(outputFile)
            newexif.setAttribute("Orientation", exif.getAttribute("Orientation"))
            newexif.saveAttributes()
        }
        return outputFile.readBytes()
    }

    @Throws(IOException::class)
    private fun convertMediaToB64(): String {
        val inputstream = context.contentResolver.openInputStream(media_uri!!)
        val data = inputstream?.readBytes()
        inputstream?.close()

        var image = compressImage(data!!)
        val b64 = Base64.encodeToString(image, Base64.DEFAULT)

        return b64.replace("\n".toRegex(), "")
    }

    @Throws(JSONException::class)
    private fun getJsonQueryString(from: String, name: String, data: String): String {
        val json_param = JSONObject()
        json_param.put("from", URLEncoder.encode(from))
        json_param.put("name", name)
        json_param.put("blob", data)
        return json_param.toString()
    }

    @Throws(ShareException::class)
    fun postToApi(intent: Intent) {
        val image_id: String
        var b64: String?
        val json_query: String
        val url: URL
        var urlConnection: HttpURLConnection? = null

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val API_FROM_FIELD = sharedPreferences.getString("api_username", "")
        var ROOT_URL = sharedPreferences.getString("api_root_url", "")

        var builder = Notify.uploadingNotification(context)

        if (API_FROM_FIELD.isNullOrEmpty() || ROOT_URL.isNullOrEmpty())
        {
            Notify.errorNotificationOpenSettings(context, builder)
            throw ShareException(context.getString(R.string.notify_settings_empty))
        }

        if (!ROOT_URL.endsWith('/')) {
            ROOT_URL = "$ROOT_URL/"
        }

        var API_ENDPOINT = "${ROOT_URL}api/images"

        this.media_uri = intent.extras!!.getParcelable<Uri>(Intent.EXTRA_STREAM)

        try {
            b64 = convertMediaToB64()
        } catch (e: Exception) {
            Notify.errorNotification(context, builder, "Error while converting media to base64.")
            throw ShareException("Error while converting media to base64.", e)
        }

        try {
            json_query = getJsonQueryString(API_FROM_FIELD, "", b64)
        } catch (e: JSONException) {
            Notify.errorNotification(context, builder, "POST Request JSON malformed.")
            throw ShareException("Abort: POST Request JSON malformed.", e)
        }

        try {
            url = URL(API_ENDPOINT)
        } catch (e: MalformedURLException) {
            Notify.errorNotification(context, builder, "Malformed URL.")
            throw ShareException("Abort: Malformed URL.", e)
        }

        try {
            urlConnection = buildHttpPOSTConnection(url, json_query)
        } catch (e: IOException) {
            urlConnection?.disconnect()
            Notify.errorNotification(context, builder, "IOException.")
            throw ShareException("Abort: IOException.", e)
        }

        try {
            image_id = parseResponseJSON(urlConnection, "id")
        } catch (e: IOException) {
            Notify.errorNotification(context, builder, "Response JSON parse failed.")
            var error = getHTTPError(urlConnection)
            try {
                error = getJSONKey(error, "errmsg")
            } catch (e1: JSONException) {
                Notify.errorNotification(context, builder, "Response JSON parse failed.")
            }
            throw ShareException("Abort: $error", e)
        } catch (e: JSONException) {
            Notify.errorNotification(context, builder, "Response JSON parse failed.")
            throw ShareException("Abort: Response JSON parse failed.", e)
        } finally {
            urlConnection.disconnect()
        }

        mHandler.post {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("HappyShare", ROOT_URL + image_id)
            Notify.uploadedNotification(context, builder, ROOT_URL + image_id)
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
        val response = BufferedInputStream(urlConnection.inputStream)
        return getJSONKey(readString(response), key)
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
