package com.sreeni.locationtracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.amazonaws.regions.Regions;

import java.util.UUID;

public class LocationTrackerUtil {

    // shared preferences
    public static final String PREF_TIMER_STARTED = "timer_started";

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    public static final String CUSTOMER_SPECIFIC_ENDPOINT = "a26mf2vrdpqeom.iot.us-west-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with AWS IoT permissions.
    public static final String COGNITO_POOL_ID = "us-west-2:b3bdcaae-9845-45e5-ab2c-2d674a75281b";
    // Region of AWS IoT
    public static final Regions MY_REGION = Regions.US_WEST_2;
    public static final String topic = "location";

    // alarm constants
    private static final int ALARM_ID = 1;
    private static final int interval = 1 * 60 * 1000;

    // Google Location related
    public static final int REQUEST_LOCATION = 1;

    public static String getClientID() {
        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        return UUID.randomUUID().toString();
    }

    public static void setupAlarm(Context context) {
        // Need to start repeating alarm once (boot complete or app launch)
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(LocationTrackerUtil.PREF_TIMER_STARTED, true);
        editor.apply();

        // setup alarm parameters
        Intent alarmIntent = new Intent(context, AlarmBroadcastReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_ID,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // start alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval,
                interval,
                pendingIntent);
    }
}
