package com.sreeni.locationtracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Main2Activity extends Activity {

    private static final String TAG = Main2Activity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO: figure out a way to retrieve JSON payload
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        StringBuilder builder = new StringBuilder();
        if (extras == null) {
            Toast.makeText(this, "No Payload, something went wrong!", Toast.LENGTH_LONG).show();
            return;
        }

        for (String key : extras.keySet()) {
            if (key.equals("Name") || key.equals("Latitude") || key.equals("Longitude") || key.equals("Accuracy") ) {
                Object value = extras.get(key);
                if (value != null) {
                    Log.d(TAG, String.format("%s %s", key, value.toString()));
                    String line = key + ":" + value.toString();
                    builder.append(line).append("\n");
                }
            }
        }

        Toast.makeText(this, builder.toString(), Toast.LENGTH_LONG).show();
    }
}
