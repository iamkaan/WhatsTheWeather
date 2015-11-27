package com.iamkaan.whatstheweather.ui;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.iamkaan.whatstheweather.R;
import com.iamkaan.whatstheweather.listener.WeatherInfoFetchListener;
import com.iamkaan.whatstheweather.util.WeatherHelper;
import com.iamkaan.whatstheweather.util.model.Weather;
import com.squareup.picasso.Picasso;


public class MainActivity extends FragmentActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, WeatherInfoFetchListener {

    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 0;

    ImageView icon;
    TextView currentTemp;
    TextView weatherText;
    TextView weatherHighLow;
    View rootView;
    View info;

    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    Location userLocation;
    GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        info = findViewById(R.id.info);
        rootView = findViewById(R.id.root);
        icon = (ImageView) findViewById(R.id.icon);
        currentTemp = (TextView) findViewById(R.id.current_temp);
        weatherText = (TextView) findViewById(R.id.day_text);
        weatherHighLow = (TextView) findViewById(R.id.day_high_low);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map = googleMap;
            enableLocation();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                //TODO show explanation to user
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation();
                } else {
                    //TODO handle permission denied
                }
            }
        }
    }

    private void enableLocation() {
        map.setMyLocationEnabled(true);
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (userLocation == null || location.distanceTo(userLocation) > 25) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            //means we're getting the location for the first time, so we focus on the location
            if (userLocation == null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15f));
            }

            userLocation = location;

            WeatherHelper.getWeatherInfo(getApplicationContext(), location, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onFetch(Weather result) {
        currentTemp.setText(getString(R.string.x_celsius_degree, result.temp));
        weatherHighLow.setText(getString(R.string.x_celsius_degree_low_high, result.dayHigh, result.dayLow));
        weatherText.setText(result.dayText);

        ValueAnimator colorAnimation = ValueAnimator
                .ofObject(new ArgbEvaluator(),
                        ((ColorDrawable) rootView.getBackground()).getColor(),
                        WeatherHelper.getWeatherColor(result.temp));
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                rootView.setBackgroundColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();

        Picasso.with(getApplication())
                .load(result.iconURL)
                .error(R.mipmap.ic_launcher)
                .fit()
                .centerInside()
                .into(icon);

        info.setVisibility(View.VISIBLE);
    }

    @Override
    public void onError(Exception exception) {

    }
}