package com.sreeni.locationtracker;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmBroadcastReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = AlarmBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Starting Publish Location Service");
        Intent intentService = new Intent(context, PubLocService.class);
        startWakefulService(context, intentService);
    }
}
