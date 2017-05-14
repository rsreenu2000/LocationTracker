package com.sreeni.locationtracker;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final String TAG = MainActivity.class.getSimpleName();

    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    Button btnDisconnect;
    Button btnPublish;
    Button btnViewMessages;

    AWSIotMqttManager mqttManager;
    String clientId;

    AWSCredentials awsCredentials;
    CognitoCachingCredentialsProvider credentialsProvider;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);

        btnPublish = (Button) findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(publishClick);
        btnPublish.setEnabled(false);

        btnViewMessages = (Button) findViewById(R.id.btnViewMessages);
        btnViewMessages.setOnClickListener(viewMessagesClick);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = LocationTrackerUtil.getClientID();
        tvClientId.setText(clientId);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, LocationTrackerUtil.CUSTOMER_SPECIFIC_ENDPOINT);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                LocationTrackerUtil.COGNITO_POOL_ID, // Identity Pool ID
                LocationTrackerUtil.MY_REGION // Region
        );

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {
                awsCredentials = credentialsProvider.getCredentials();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnConnect.setEnabled(true);
                    }
                });
            }
        }).start();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Check if BootCompleteReceiver has started the repeating alarm
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isTimerStarted = pref.getBoolean(LocationTrackerUtil.PREF_TIMER_STARTED, false);
        if (!isTimerStarted) {
            Log.d(TAG, "Set repeating alarm");
            LocationTrackerUtil.setupAlarm(this);
        }
    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    tvStatus.setText("Connected");
                                    btnPublish.setEnabled(true);

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(TAG, "Connection error.", throwable);
                                        throwable.printStackTrace();
                                    }
                                    tvStatus.setText("Disconnected");
                                    btnPublish.setEnabled(false);
                                } else {
                                    tvStatus.setText("Disconnected");
                                    btnPublish.setEnabled(false);
                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
        }
    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Disconnect error.", e);
            }
        }
    };

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Publish button clicked");
            btnPublish.setEnabled(false);
            mGoogleApiClient.connect();
        }
    };

    View.OnClickListener viewMessagesClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "View Messages Button clicked");
        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Check Permissions Now
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LocationTrackerUtil.REQUEST_LOCATION);
            } else {
                // permission has been granted, continue as usual
                publishInternal();
            }
        } else {
            // Legacy platforms doesn't do runtime permission check
            publishInternal();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "GoogleApiClient error (onConnectionSuspended): " + i);
        enablePublishButton();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "GoogleApiClient error (onConnectionFailed): " + result.getErrorMessage());
        enablePublishButton();

//        //TODO: need to understand this callback and handle it appropriately
//        if (mResolvingError) {
//            // Already attempting to resolve an error.
//            return;
//        } else if (result.hasResolution()) {
//            try {
//                mResolvingError = true;
//                result.startResolutionForResult(this, LocationTrackerUtil.REQUEST_RESOLVE_ERROR);
//            } catch (IntentSender.SendIntentException e) {
//                // There was an error with the resolution intent. Try again.
//                mGoogleApiClient.connect();
//            }
//        } else {
//            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
//            Log.e(TAG, result.getErrorMessage());
//            mResolvingError = true;
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LocationTrackerUtil.REQUEST_LOCATION) {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                publishInternal();
            } else {
                // Permission was denied or request was cancelled
                Log.d(TAG, "User has denied permissions!");
                enablePublishButton();
            }
        }
    }

    private void publishInternal() {
        // TODO: Permission check is complete; let's skip exception handling for now
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location != null) {
            final String msg = "{\"latitude\": " + location.getLatitude() + "," +
                    "\"longitude\": " + location.getLongitude() + "," +
                    "\"accuracy\": " + location.getAccuracy() + "}";

            Log.d(TAG, "Location: " + msg);

            try {
                mqttManager.publishString(msg, LocationTrackerUtil.topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(TAG, "Publish error.", e);
            } finally {
                enablePublishButton();
            }
        } else {
            Log.e(TAG, "Location retrieval failed");
            enablePublishButton();
        }
    }

    private void enablePublishButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPublish.setEnabled(true);
                mGoogleApiClient.disconnect();
            }
        });
    }
}
