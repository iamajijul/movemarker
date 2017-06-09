package com.scansione.movemarker;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ajijul
 */
public class TrackMe extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {

    protected LatLng source;// = new LatLng(22.642388, 88.440017);
    protected LatLng end;// = new LatLng(22.585975, 88.340454);
    private static final String LOG_TAG = "MovingMarkerActivity";
    protected GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private final Handler mHandler = new Handler();
    private PlaceAutocompleteAdapter mAdapter;
    private BottomSheetBehavior<View> mBottomSheetBehavior2;
    private Animator animator = new Animator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moving_marker);
        findViewById(R.id.ll).setVisibility(View.GONE);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Track Me");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        MapsInitializer.initialize(this);
        mGoogleApiClient.connect();
        mAdapter = new PlaceAutocompleteAdapter(this,
                mGoogleApiClient, null, null);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("$$", "*****onLocationChanged***********");

        end = source;
        source = new LatLng(location.getLatitude(), location.getLongitude());
        if (source != null && end != null)
            animator.setupCameraPositionForMovement(source, end);
        if (end != null
                && distance(source.latitude, source.longitude, end.latitude, end.longitude) > .007) {
            Log.e("$$", "*****Distance More than one ,mile***********");
            animator.startAnimation();
        }

    }

    /**
     * calculates the distance between two locations in MILES
     */
    private double distance(double lat1, double lng1, double lat2, double lng2) {

        double earthRadius = 3958.75; // in miles, change to 6371 for kilometer output

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = earthRadius * c;

        return dist; // output distance, in MILES
    }

    public class Animator implements Runnable {

        private static final int ANIMATE_SPEEED = 2000;
        private static final int ANIMATE_SPEEED_TURN = 500;

        private final Interpolator interpolator = new LinearInterpolator();
        float tilt = 0;

        long start = SystemClock.uptimeMillis();

        LatLng endLatLng = null;
        LatLng beginLatLng = null;
        private Marker trackingMarker;
        private float previousBearing;

        public void reset() {
            //resetMarkers();
            start = SystemClock.uptimeMillis();
            endLatLng = getEndLatLng();
            beginLatLng = getBeginLatLng();

        }

        public void stop() {
            trackingMarker.remove();
            mHandler.removeCallbacks(animator);

        }

        public void initialize() {
            reset();
            new Handler().post(animator);

        }

        private void setupCameraPositionForMovement(LatLng markerPos,
                                                    LatLng secondPos) {
            Log.e("$$", source + "*****setupCameraPositionForMovement***********" + end);
            if (trackingMarker != null)
                return;
            previousBearing = bearingBetweenLatLngs(markerPos, secondPos);

            int height = 70;
            int width = 40;
            BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.car_marker, null);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            trackingMarker = mGoogleMap.addMarker(new MarkerOptions().position(markerPos)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                    .title("Car")
                    .snippet("Yooo"));
            trackingMarker.setRotation(-previousBearing > 180 ? previousBearing / 2 : previousBearing);
            CameraPosition cameraPosition =
                    new CameraPosition.Builder()
                            .target(markerPos)
                            // .bearing(previousBearing)
                            .tilt(tilt)
                            .zoom(mGoogleMap.getCameraPosition().zoom >= 17 ? mGoogleMap.getCameraPosition().zoom : 17)
                            .build();

            mGoogleMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(cameraPosition),
                    ANIMATE_SPEEED_TURN,
                    new GoogleMap.CancelableCallback() {

                        @Override
                        public void onFinish() {
                            System.out.println("finished camera");
                            animator.reset();
                            Handler handler = new Handler();
                            handler.post(animator);
                        }

                        @Override
                        public void onCancel() {
                            System.out.println("cancelling camera");
                        }
                    }
            );
        }

        public void stopAnimation() {
            animator.stop();
        }

        public void startAnimation() {
            animator.initialize();

        }


        @Override
        public void run() {
            long elapsed = SystemClock.uptimeMillis() - start;
            double t = interpolator.getInterpolation((float) elapsed / ANIMATE_SPEEED);


            double lat = t * endLatLng.latitude + (1 - t) * beginLatLng.latitude;
            double lng = t * endLatLng.longitude + (1 - t) * beginLatLng.longitude;
            LatLng newPosition = new LatLng(lat, lng);


            float bearingL = bearingBetweenLatLngs(beginLatLng, endLatLng);
            float rot = (float) (t * bearingL + (1 - t) * previousBearing);
            previousBearing = bearingL;
            trackingMarker.setRotation(-rot > 180 ? rot / 2 : rot);
            trackingMarker.setPosition(newPosition);

            if (t < 1) {
                Log.e("$$", source + "*******t < 1*********" + end);

                mHandler.postDelayed(this, 16);
            } else {
                Log.e("$$", source + "*******t < 1 else {*********" + end);
                CameraPosition cameraPosition =
                        new CameraPosition.Builder()
                                .target(end) // changed this...
                                //  .bearing(bearingL)  //Open to see bearing on map but you have
                                // to stop rotation of marker
                                .tilt(tilt)
                                .zoom(mGoogleMap.getCameraPosition().zoom)
                                .build();


                mGoogleMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(cameraPosition),
                        ANIMATE_SPEEED_TURN,
                        null
                );

                // mHandler.postDelayed(this, 16);


            }
        }


        private LatLng getEndLatLng() {
            return end;
        }

        private LatLng getBeginLatLng() {
            return source;
        }

    }


    private Location convertLatLngToLocation(LatLng latLng) {
        Location loc = new Location("someLoc");
        loc.setLatitude(latLng.latitude);
        loc.setLongitude(latLng.longitude);
        return loc;
    }

    private float bearingBetweenLatLngs(LatLng begin, LatLng end) {
        Location beginL = convertLatLngToLocation(begin);
        Location endL = convertLatLngToLocation(end);

        return beginL.bearingTo(endL);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.v(LOG_TAG, connectionResult.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("$$", "onConnected");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    public void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.currentLocation) {

        }
        return true;
    }
}