package com.sreeni.locationtracker;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.CountDownLatch;

public class PubLocService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = PubLocService.class.getSimpleName();

    // CountDownLatch for waiting till MQTT operation is completed.
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private AWSIotMqttManager mqttManager;
    private AWSCredentials awsCredentials;
    private CognitoCachingCredentialsProvider credentialsProvider;

    GoogleApiClient mGoogleApiClient;

    public PubLocService() {
        super("PubLocService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Intent service started");

        connect(this);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Intent Service ended");

        //LAST STEP
        AlarmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void connect(final PubLocService pubLocService) {

        // MQTT Client
        mqttManager = new AWSIotMqttManager(LocationTrackerUtil.getClientID(),
                LocationTrackerUtil.CUSTOMER_SPECIFIC_ENDPOINT);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                LocationTrackerUtil.COGNITO_POOL_ID, // Identity Pool ID
                LocationTrackerUtil.MY_REGION // Region
        );

        // Use Cognito credentials provider for authentication with AWS IoT.
        awsCredentials = credentialsProvider.getCredentials();

        try {
            mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                    Log.d(TAG, "Status = " + String.valueOf(status));

                    if (status == AWSIotMqttClientStatus.Connected) {
                        // Start publishing now
                        publish(pubLocService);
                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(TAG, "Connection error: ", throwable);
                        }
                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(TAG, "Connection error: ", throwable);
                            throwable.printStackTrace();
                        }

                        countDownLatch.countDown();
                    }
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Connect exception: " + e.getMessage());
        }
    }

    private void publish(PubLocService pubLocService) {

        // Create an instance of GoogleAPIClient.
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(pubLocService)
                .addOnConnectionFailedListener(pubLocService)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private void disconnect() {
        mGoogleApiClient.disconnect();

        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Disconnect exception: ", e);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        // let's us assume that user has granted permission during app launch
        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                final String msg = "{\"latitude\": " + location.getLatitude() + "," +
                        "\"longitude\": " + location.getLongitude() + "," +
                        "\"accuracy\": " + location.getAccuracy() + "}";
                Log.d(TAG, "Location: " + msg);
                try {
                    mqttManager.publishString(msg, LocationTrackerUtil.topic, AWSIotMqttQos.QOS0);
                } catch (Exception e) {
                    Log.e(TAG, "Publish exception: ", e);
                }
            } else {
                Log.e(TAG, "Location retrieval failed");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission exception: ", e);
        } finally {
            disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient error: " + i);
        disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "GoogleApiClient error: " + result.getErrorMessage());
        disconnect();
    }

}
