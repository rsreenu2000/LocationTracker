/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld

   https://cloud.google.com/solutions/mobile/firebase-app-engine-android-studio
*/

package com.example.sreenirayanki.myapplication.backend;

import com.google.maps.GaeRequestHandler;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.*;

public class MyServlet extends HttpServlet {

    private static final Logger Log = Logger.getLogger("com.example.sreenirayanki.myapplication.backend.MyServlet");

    // Refer to getPlaceNameOrAddress() for details
    private static final String API_KEY = "AIzaSyCKmhU_LguZaRUXCKZ-hZzvJBnC1gdZhao";

    // Refer to notifyUser() for details
    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "AIzaSyCKmhU_LguZaRUXCKZ-hZzvJBnC1gdZhao";
    private static final String REGISTRATION_ID = "f0qpZICgnyA:APA91bEhgfVqkfiVzQXl_4FX8-Mrxq0xTYAMELAiPkCp_gPiBtkXRDiPtMxTiS13ayJikCvVmeiJFiK4RA7Ud0DkK6LEfxOZbAPL6wfOLcDp6Jpa_mT2syCbV6fJ-gC8MoBXfU5yqV-R";

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String latitude = req.getParameter("latitude");
        String longitude = req.getParameter("longitude");
        String accuracy = req.getParameter("accuracy");

        Log.info("Inside POST method:accuracy " + accuracy);

        resp.setContentType("text/plain");

        if (latitude != null && longitude != null && accuracy != null) {
            //TODO: Will GCP start a new instance for every POST request or
            //TODO: do I need to span a new thread for processing every request?
            String name = getPlaceNameOrAddress(latitude, longitude);
            notifyUser(latitude, longitude, accuracy, name);
            resp.getWriter().println(name);
        } else {
            resp.getWriter().println("Invalid input");
        }

    }

    private String getPlaceNameOrAddress(String latitude, String longitude) {

        // Google Maps Geocoding API documentation
        // https://developers.google.com/maps/documentation/geocoding/start
        // How to Enable "Google Maps Geocoding API" in Google Cloud Console for a given project
        // https://console.cloud.google.com/apis/dashboard?project=locationtracker-a13eb
        // Get API Key (Server key)
        // https://console.cloud.google.com/apis/credentials?project=locationtracker-a13eb

        try {
            GeoApiContext context = new GeoApiContext(new GaeRequestHandler()).setApiKey(API_KEY);
            LatLng latLng = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
            GeocodingResult[] results = GeocodingApi.reverseGeocode(context, latLng).await();
            String locality = null;
            String sublocality = null;
            String neighborhood = null;
            for (AddressComponent component : results[0].addressComponents) {
                for (AddressComponentType type : component.types) {
                    switch (type.toString()) {
                        case "locality":
                            locality = component.shortName;
                            break;
                        case "sublocality":
                            sublocality = component.shortName;
                            break;
                        case "neighborhood":
                            neighborhood = component.shortName;
                            break;
                        default:
                            break;
                    }
                }
            }

            if (neighborhood != null) {
                return neighborhood;
            } else if (sublocality != null) {
                return sublocality;
            } else if (locality != null) {
                return locality;
            } else {
                return results[0].formattedAddress;
            }
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Error!!!";
    }

    private void notifyUser(String latitude, String longitude, String accuracy, String name) {

        // http://docs.aws.amazon.com/sns/latest/dg/mobile-push-gcm.html
        // Step 1: Create a Google API Project and Enable the GCM Service
        // Step 2: Obtain the Server API Key
        // Step 3: Obtain a Registration ID from GCM
        // Step 4: Send a Push Notification Message to a Mobile Endpoint using GCM

//        {
//            "to" : "APA91bHun4MxP5egoKMwt2KZFBaFUH-1RYqx...",
//                "notification" : {
//                    "title" : "Standford",
//                    "click_action" : "NOTIFICATION_ACTIVITIY"
//                },
//                "data" : {
//                    "Name" : "Stanford",
//                    "Latitude" : "37.427440",
//                    "Longitude" : "-122.169118",
//                    "Accuracy" : "18"
//                }
//        }

        final String payload = "{" +
                "\"to\" : \"" + REGISTRATION_ID + "\", " +
                "\"notification\" : " + "{ " +
                "\"title\" : \"" + name + "\", " + "\"click_action\" : \"" + "NOTIFICATION_ACTIVITY" + "\" " +
                "}, " +
                "\"data\" : " + "{ " +
                "\"Name\" : \"" + name + "\", " +
                "\"Latitude\" : \"" + latitude + "\", " +
                "\"Longitude\" : \"" + longitude + "\", " +
                "\"Accuracy\" : \"" + accuracy + "\" " +
                "} " +
                "}";

        try {
            // Set up the request
            URL object = new URL(FCM_URL);
            HttpURLConnection conn = (HttpURLConnection) object.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "key="+SERVER_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            // Execute HTTP Post
            OutputStream outputStream = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write(payload);
            //Log.info("payload: " + payload);
            writer.flush();
            writer.close();
            outputStream.close();
            conn.connect();

            // Read response
            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.info("POST response: " + response.toString());
            } else {
                Log.info("POST error: " + responseCode + " " + response.toString());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
