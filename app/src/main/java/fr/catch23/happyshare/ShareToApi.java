package fr.catch23.happyshare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class ShareToApi extends Activity {
    private Handler mHandler;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        final Context context = this.getApplicationContext();
        mHandler = new Handler();
        final  Activity activity = this;

        if ((Intent.ACTION_SEND.equals(action)) && type != null){
            final APIManager apimger = new APIManager(context, mHandler);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        apimger.postToApi(intent);
                    } catch (final ShareException e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {Toast.makeText(context, e.getUser_message(), Toast.LENGTH_LONG).show();}
                        });
                    }
                    mHandler.post(new Runnable() {
                          @Override
                          public void run() {activity.finish(); }
                      });
                }
            };
            thread.start();
        }
    }
}

