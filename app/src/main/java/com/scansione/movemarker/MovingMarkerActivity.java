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
import com.google.android.gms.maps.model.BitmapDescriptor;
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
public class MovingMarkerActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    protected LatLng source;// = new LatLng(22.642388, 88.440017);
    protected LatLng end;// = new LatLng(22.585975, 88.340454);
    private ProgressDialog progressDialog;
    private static final String LOG_TAG = "MovingMarkerActivity";
    protected GoogleApiClient mGoogleApiClient;
    private List<LatLng> latLngs = new ArrayList<LatLng>();
    private GoogleMap mGoogleMap;
    private final Handler mHandler = new Handler();
    private AutoCompleteTextView actv_source, actv_destination;
    private PlaceAutocompleteAdapter mAdapter;
    private ImageView iv_navigate;
    private BottomSheetBehavior<View> mBottomSheetBehavior2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moving_marker);
        actv_source = (AutoCompleteTextView) findViewById(R.id.actv_source);
        actv_destination = (AutoCompleteTextView) findViewById(R.id.actv_destination);
        iv_navigate = (ImageView) findViewById(R.id.iv_navigate);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Track Me");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        MapsInitializer.initialize(this);
        mGoogleApiClient.connect();

        mAdapter = new PlaceAutocompleteAdapter(this,
                mGoogleApiClient, null, null);
        actv_source.setAdapter(mAdapter);
        actv_destination.setAdapter(mAdapter);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        /*
        * Sets the start and destination points based on the values selected
        * from the autocomplete text views.
        * */

        actv_source.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final AutocompletePrediction item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.getPlaceId());

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        source = place.getLatLng();
                    }
                });

            }
        });
        actv_destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final AutocompletePrediction item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.getPlaceId());
            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(LOG_TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);
                        end = place.getLatLng();
                    }
                });

            }
        });
        iv_navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateRootAndStartNavigate();
            }
        });
    }

    private void generateRootAndStartNavigate() {
        if (source == null || end == null) {
            if (source == null) {
                if (actv_source.getText().length() > 0) {
                    actv_source.setError("Choose location from dropdown.");
                } else {
                    Toast.makeText(this, "Please choose a starting point.", Toast.LENGTH_SHORT).show();
                }
            }
            if (end == null) {
                if (actv_source.getText().length() > 0) {
                    actv_source.setError("Choose location from dropdown.");
                } else {
                    Toast.makeText(this, "Please choose a destination.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            progressDialog = ProgressDialog.show(this, "Please wait.",
                    "Fetching route information.", true);
            CameraUpdate center = CameraUpdateFactory.newLatLng(source);
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(17);
            mGoogleMap.moveCamera(center);
            mGoogleMap.animateCamera(zoom);
            new GetDirectionOnMap(mGoogleMap, source, end) {
                @Override
                protected void onPostExecute(List<LatLng> latLngs) {
                    super.onPostExecute(latLngs);
                    progressDialog.dismiss();
                    MovingMarkerActivity.this.latLngs = new ArrayList<LatLng>(latLngs);
                    animator.startAnimation();
                }
            }.execute();

            hideKeyboard();
        }
    }

    private Animator animator = new Animator();


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


    public class Animator implements Runnable {

        private static final int ANIMATE_SPEEED = 2000;
        private static final int ANIMATE_SPEEED_TURN = 500;

        private final Interpolator interpolator = new LinearInterpolator();
        int currentIndex = 0;
        float tilt = 0;

        long start = SystemClock.uptimeMillis();

        LatLng endLatLng = null;
        LatLng beginLatLng = null;
        private Marker trackingMarker;
        private float previousBearing;

        public void reset() {
            //resetMarkers();
            start = SystemClock.uptimeMillis();
            currentIndex = 0;
            endLatLng = getEndLatLng();
            beginLatLng = getBeginLatLng();

        }

        public void stop() {
            trackingMarker.remove();
            mHandler.removeCallbacks(animator);

        }

        public void initialize() {
            reset();
            // We first need to put the camera in the correct position for the first run (we need 2 markers for this).....
            LatLng markerPos = latLngs.get(0);
            LatLng secondPos = latLngs.get(1);

            setupCameraPositionForMovement(markerPos, secondPos);

        }

        private void setupCameraPositionForMovement(LatLng markerPos,
                                                    LatLng secondPos) {

            previousBearing = bearingBetweenLatLngs(markerPos, secondPos);

            int height = 70;
            int width = 40;
            BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.car_marker, null);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            trackingMarker = mGoogleMap.addMarker(new MarkerOptions().position(markerPos)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                    .title("Car")
                    .snippet("Yo"));
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
            if (latLngs.size() > 1) {
                animator.initialize();
            }
        }


        @Override
        public void run() {

            long elapsed = SystemClock.uptimeMillis() - start;
            double t = interpolator.getInterpolation((float) elapsed / ANIMATE_SPEEED);


            double lat = t * endLatLng.latitude + (1 - t) * beginLatLng.latitude;
            double lng = t * endLatLng.longitude + (1 - t) * beginLatLng.longitude;
            LatLng newPosition = new LatLng(lat, lng);

            LatLng begin = getBeginLatLng();
            LatLng end = getEndLatLng();

            float bearingL = bearingBetweenLatLngs(begin, end);
            float rot = (float) (t * bearingL + (1 - t) * previousBearing);
            previousBearing = bearingL;
            trackingMarker.setRotation(-rot > 180 ? rot / 2 : rot);
            trackingMarker.setPosition(newPosition);

            if (t < 1) {
                mHandler.postDelayed(this, 16);
            } else {

                System.out.println("Move to next marker.... current = " + currentIndex + " and size = " + latLngs.size());
                if (currentIndex < latLngs.size() - 2) {

                    currentIndex++;

                    endLatLng = getEndLatLng();
                    beginLatLng = getBeginLatLng();


                    start = SystemClock.uptimeMillis();


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

                    start = SystemClock.uptimeMillis();
                    mHandler.postDelayed(animator, 16);

                } else {
                    currentIndex++;
                    //    stopAnimation();
                }

            }
        }


        private LatLng getEndLatLng() {
            return latLngs.get(currentIndex + 1);
        }

        private LatLng getBeginLatLng() {
            return latLngs.get(currentIndex);
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