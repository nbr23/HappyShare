package fr.catch23.happyshare;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class APIManager {
    private String API_ENDPOINT;
    private String API_RESULT_URL;
    private String API_FROM_FIELD;

    private final Context context;
    private Uri media_uri = null;
    private Handler mHandler;

    public APIManager(final Context context, Handler mHandler)
    {
        this.context = context;
        this.mHandler = mHandler;
        this.API_ENDPOINT = context.getString(R.string.api_endpoint);
        this.API_RESULT_URL = context.getString(R.string.api_result_url);
        this.API_FROM_FIELD = context.getString(R.string.api_from_field);
    }

    private void displayUserMessage(final String message)
    {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String convertMediaToB64() throws IOException {
        byte[] inputData = new byte[0];
        InputStream is = context.getContentResolver().openInputStream(media_uri);
        inputData = readBytes(is);
        String b64 = Base64.encodeToString(inputData, Base64.DEFAULT);
        is.close();
        return b64.replaceAll("\n", "");
    }

    private String getJsonQueryString(String from, String name, String data) throws JSONException {
        JSONObject json_param = new JSONObject();
        json_param.put("from", URLEncoder.encode(from));
        json_param.put("name", "");
        json_param.put("blob", data);
        return json_param.toString();
    }

    public void postToApi(final Intent intent) throws ShareException
    {
        final String image_id;
        String b64 = null;
        String json_query;
        URL url;
        HttpURLConnection urlConnection = null;

        this.media_uri = (Uri) intent.getExtras().getParcelable(Intent.EXTRA_STREAM);

        try {
            b64 = convertMediaToB64();
        } catch (IOException e) {
            throw new ShareException("Error while converting media to base64.");
        }

        try {
            json_query = getJsonQueryString(API_FROM_FIELD, "", b64);
        } catch (JSONException e) {
            throw new ShareException("Abort: POST Request JSON malformed.");
        }

        try {
            url = new URL(API_ENDPOINT);
        } catch (MalformedURLException e) {
            throw new ShareException("Abort: Malformed URL.");
        }

        try {
            urlConnection = buildHttpPOSTConnection(url, json_query);
        } catch (IOException e) {
            if (urlConnection != null)
                urlConnection.disconnect();
            throw new ShareException("Abort: IOException.");
        }

        try {
            image_id = parseResponseJSON(urlConnection, "id");
        } catch (IOException e) {
            String error = getHTTPError(urlConnection);
            try {
                error = getJSONKey(error, "errmsg");
            } catch (JSONException e1) {
            }
            throw new ShareException("Abort: " + error);
        } catch (JSONException e) {
            throw new ShareException("Abort: Response JSON parse failed.");
        }
        finally {
            urlConnection.disconnect();
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ClipboardManager clipboard = (ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("HappyShare", API_RESULT_URL + image_id);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Content shared, result copied to clipboard", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getJSONKey(String json_str, String key) throws JSONException {
        JSONObject ret_obj = new JSONObject(json_str);
        return ret_obj.getString(key);
    }

    private String parseResponseJSON(HttpURLConnection urlConnection, String key) throws IOException, JSONException {
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        return getJSONKey(readString(in), key);
    }

    private String getHTTPError(HttpURLConnection urlConnection)
    {
        try {
            InputStream errorstream = urlConnection.getErrorStream();
            if (errorstream == null)
                throw new IOException();
            return readString(errorstream);
        } catch (IOException e) {
            return "Error reading HTTP error message.";
        }
    }

    private HttpURLConnection buildHttpPOSTConnection(URL url, String query) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setReadTimeout(120000);
        urlConnection.setConnectTimeout(120000);
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);

        OutputStream os = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(query);
        writer.flush();
        writer.close();
        os.close();

        return urlConnection;
    }

    private static String readString(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b = 0;
        while ((b = inputStream.read()) != -1)
            sb.append((char)b);

        return sb.toString();
    }

    private static byte[] readBytes(InputStream inputStream) throws IOException
    {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        int bs = 2048;
        byte[] buf = new byte[bs];
        int size = 0;

        while ((size = inputStream.read(buf)) != -1)
            byteArray.write(buf, 0, size);

        return byteArray.toByteArray();
    }

}
