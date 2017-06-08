package com.scansione.movemarker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();
        mGoogleApiClient.connect();
        PlaceAutocompleteAdapter mAdapter = new PlaceAutocompleteAdapter(this,
                mGoogleApiClient, null, null);
        ((AutoCompleteTextView) findViewById(R.id.actv)).setAdapter(mAdapter);
    }
}
