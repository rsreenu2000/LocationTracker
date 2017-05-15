/* https://github.com/windy1/google-places-api-java
 * Create jar using: mvn clean package
 */

package com.example.sreenirayanki.myapplication.backend;

import java.util.List;
import java.util.logging.Logger;

import se.walkercrou.places.GooglePlaces;
import se.walkercrou.places.Place;

public class PlacesHelper {

    private static final Logger Log = Logger.getLogger("PlacesHelper");

    public String getPlaceNameOrAddress(String latitude, String longitude) {
        GooglePlaces google = new GooglePlaces("AIzaSyDtZFDtX3tVLGIVkm_ymuImX6OCubsihJY");

        google.setDebugModeEnabled(true);

        List<Place> places = google.getNearbyPlaces(Double.valueOf(latitude), Double.valueOf(longitude), 100, 10);
        if (places != null && places.size() > 0) {
            Place place = places.get(0);
            String name = place.getName();
            String vicinity = place.getVicinity();
            String address = place.getAddress();

            String ref = place.getReferenceId();
            Place details = google.getPlace(ref);

            Log.info("name: " + name);
            Log.info("vicinity: " + vicinity);
            Log.info("address: " + address);
        }

        return null;
    }

    public static void main(String[] args) {
        PlacesHelper helper = new PlacesHelper();
        helper.getPlaceNameOrAddress("41.879172", "-87.635915");
    }
}
