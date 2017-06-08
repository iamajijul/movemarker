package com.scansione.movemarker;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ajijul on 17/5/16.
 */
public class GetDirectionOnMap extends AsyncTask<Void, Void, List<LatLng>> {
    GoogleMap googleMap;
    private LatLng origin, destination;

    public GetDirectionOnMap(GoogleMap googleMap, LatLng origin, LatLng destination) {
        this.googleMap = googleMap;
        this.origin = origin;
        this.destination = destination;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<LatLng> doInBackground(Void... params) {
        String stringUrl = "http://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude + "&destination=" +
                destination.latitude + "," + destination.longitude + "&sensor=false";
        StringBuilder response = new StringBuilder();
        List<LatLng> points = new ArrayList<LatLng>();
        try {
            URL url = new URL(stringUrl);
            HttpURLConnection httpconn = (HttpURLConnection) url
                    .openConnection();
            if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(httpconn.getInputStream()),
                        8192);
                String strLine = null;

                while ((strLine = input.readLine()) != null) {
                    response.append(strLine);
                }
                input.close();
            }

            String jsonOutput = response.toString();

            JSONObject jsonObject = new JSONObject(jsonOutput);

            // routesArray contains ALL routes
            JSONArray routesArray = jsonObject.getJSONArray("routes");
            // Grab the first route
            JSONObject route = routesArray.getJSONObject(0);
                /* to get driving duration on time

                JSONArray jLegs = route.getJSONArray("legs");
                JSONObject leg = jLegs.getJSONObject(0);
                JSONObject jObject = leg.getJSONObject("duration");
                Logger.showErrorLog(jObject.getString("text"));*/

            JSONObject poly = route.getJSONObject("overview_polyline");
            String polyline = poly.getString("points");
            points = decodePoly(polyline);

        } catch (Exception e) {

        }
        return points;
    }

    @Override
    protected void onPostExecute(List<LatLng> latLngs) {
        super.onPostExecute(latLngs);
        if (latLngs != null) {
            for (int i = 0; i < latLngs.size() - 1; i++) {
                LatLng src = latLngs.get(i);
                LatLng dest = latLngs.get(i + 1);
                try {
                    googleMap.addPolyline(new PolylineOptions()
                            .add(new LatLng(src.latitude, src.longitude),
                                    new LatLng(dest.latitude, dest.longitude))
                            .width(15).color(Color.GREEN).geodesic(true));
                } catch (NullPointerException e) {
                }

            }
        }
    }

    /*
   decode poly point to LatLng
    */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

}

