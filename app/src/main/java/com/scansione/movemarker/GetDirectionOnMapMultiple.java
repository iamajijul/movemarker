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
public class GetDirectionOnMapMultiple extends AsyncTask<Void, Void, List<String>> {
    GoogleMap googleMap;
    private LatLng origin, destination;

    public GetDirectionOnMapMultiple(GoogleMap googleMap, LatLng origin, LatLng destination) {
        this.googleMap = googleMap;
        this.origin = origin;
        this.destination = destination;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<String> doInBackground(Void... params) {
        List<String> plyLines = new ArrayList<>();
        String stringUrl = "http://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude + "&destination=" +
                destination.latitude + "," + destination.longitude + "&sensor=false&alternatives=true";
        StringBuilder response = new StringBuilder();
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
            for (int i = 0; i < routesArray.length(); i++) {
                // Grab the first route
                JSONObject route = routesArray.getJSONObject(i);
                Log.e("$$", "Route Number " + i);
                /* to get driving duration on time

                JSONArray jLegs = route.getJSONArray("legs");
                JSONObject leg = jLegs.getJSONObject(0);
                JSONObject jObject = leg.getJSONObject("duration");
                Logger.showErrorLog(jObject.getString("text"));*/

                JSONObject poly = route.getJSONObject("overview_polyline");
                String polyline = poly.getString("points");
                plyLines.add(polyline);
            }


        } catch (Exception e) {

        }
        return plyLines;
    }

    @Override
    protected void onPostExecute(List<String> polylines) {
        super.onPostExecute(polylines);
        Log.e("$$", "polylines Number " + polylines.size());
        if (polylines != null) {
            for (int i = polylines.size() - 1; i >= 0; i--) {
                List<LatLng> latLngs;
                latLngs = decodePoly(polylines.get(i));
                Log.e("$$", "latLngs Number " + latLngs.size());
                int color = Color.GRAY;
                if (i == 0)
                    color = Color.BLUE;
                if (latLngs == null)
                    continue;
                for (int j = 0; j < latLngs.size() - 1; j++) {
                    LatLng src = latLngs.get(j);
                    LatLng dest = latLngs.get(j + 1);
                    if (i == 0 && j % 10 <= 5)
                        color = Color.BLUE;
                    else if (i == 0)
                        color = Color.GREEN;
                    try {
                        googleMap.addPolyline(new PolylineOptions()
                                .add(new LatLng(src.latitude, src.longitude),
                                        new LatLng(dest.latitude, dest.longitude))
                                .width(10).color(color).geodesic(true));
                    } catch (NullPointerException e) {
                    }
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
