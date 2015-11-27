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
import android.widget.Button;
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

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends FragmentActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, WeatherInfoFetchListener, View.OnClickListener {

    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 0;

    private static final String STATE_USER_LAT = "user_lat";
    private static final String STATE_USER_LNG = "user_lng";
    private static final String STATE_PIN_LAT = "pin_lat";
    private static final String STATE_PIN_LNG = "pin_lng";
    private static final String STATE_WEATHER = "weather";

    @Bind(R.id.icon)
    ImageView icon;

    @Bind(R.id.progress_message)
    TextView progressMessage;
    @Bind(R.id.day_high_low)
    TextView weatherHighLow;
    @Bind(R.id.current_temp)
    TextView currentTemp;
    @Bind(R.id.day_text)
    TextView weatherText;
    @Bind(R.id.location)
    TextView location;

    @Bind(R.id.whats_the_weather)
    Button whatsTheWeather;
    @Bind(R.id.error_button)
    Button errorButton;

    @Bind(R.id.progress_bar)
    View progressBar;
    @Bind(R.id.progress)
    View progress;
    @Bind(R.id.root)
    View rootView;
    @Bind(R.id.info)
    View info;
    @Bind(R.id.pin)
    View pin;

    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    Location userLocation;
    Location pinLocation;
    GoogleMap map;

    Weather weather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        whatsTheWeather.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(STATE_USER_LAT, userLocation.getLatitude());
        outState.putDouble(STATE_USER_LNG, userLocation.getLongitude());
        outState.putDouble(STATE_PIN_LAT, pinLocation.getLatitude());
        outState.putDouble(STATE_PIN_LNG, pinLocation.getLongitude());
        outState.putSerializable(STATE_WEATHER, weather);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userLocation = new Location("iamkaan");
        pinLocation = new Location("iamkaan");
        userLocation.setLatitude(savedInstanceState.getDouble(STATE_USER_LAT));
        userLocation.setLongitude(savedInstanceState.getDouble(STATE_USER_LNG));
        pinLocation.setLatitude(savedInstanceState.getDouble(STATE_PIN_LAT));
        pinLocation.setLongitude(savedInstanceState.getDouble(STATE_PIN_LNG));
        weather = (Weather) savedInstanceState.getSerializable(STATE_WEATHER);
        if (weather != null) {
            setWeatherInfoUI();
        } else {
            WeatherHelper.getWeatherInfo(getApplicationContext(),
                    userLocation.getLatitude(), userLocation.getLongitude(), this);
        }
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
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                askForPermission();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation();
                } else {
                    askForPermission();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        showProgressMessage(getString(R.string.progress_message_fetch_weather));

        pinLocation.setLatitude(map.getCameraPosition().target.latitude);
        pinLocation.setLongitude(map.getCameraPosition().target.longitude);

        WeatherHelper.getWeatherInfo(getApplicationContext(),
                pinLocation.getLatitude(),
                pinLocation.getLongitude(), this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (userLocation == null ||
                (location.distanceTo(userLocation) > 25 && location.distanceTo(pinLocation) < 250)) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            //means we're getting the location for the first time, so we focus on the location
            if (userLocation == null) {
                map.animateCamera(CameraUpdateFactory
                        .newLatLngZoom(new LatLng(latitude, longitude), 15f));
            }

            userLocation = new Location(location);
            pinLocation = new Location(location);

            showProgressMessage(getString(R.string.progress_message_fetch_weather));

            WeatherHelper.getWeatherInfo(getApplicationContext(),
                    location.getLatitude(), location.getLongitude(), this);
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
        weather = result;
        setWeatherInfoUI();
    }

    private void setWeatherInfoUI() {
        currentTemp.setText(getString(R.string.x_celsius_degree, weather.temp));
        weatherHighLow.setText(getString(R.string.x_celsius_degree_low_high,
                weather.dayHigh, weather.dayLow));
        weatherText.setText(weather.dayText);
        location.setText(weather.location);

        Picasso.with(getApplication())
                .load(weather.iconURL)
                .fit()
                .centerInside()
                .into(icon);

        ValueAnimator colorAnimation = ValueAnimator
                .ofObject(new ArgbEvaluator(),
                        ((ColorDrawable) rootView.getBackground()).getColor(),
                        WeatherHelper.getWeatherColor(weather.temp));
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                rootView.setBackgroundColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();

        showInfo();
    }

    @Override
    public void onError(Exception exception) {
        showErrorMessage(getString(R.string.weather_fetch_error_title),
                getString(R.string.try_again_button), this);
    }

    /**
     * shows the error card with a message that requests permission to use user location
     */
    private void askForPermission() {
        showErrorMessage(
                getString(R.string.location_access_request),
                getString(R.string.location_access_allow),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
                    }
                });
    }

    /**
     * enables my location feature of Google Maps and connects to Google LocationServices API
     */
    private void enableLocation() {
        if (map != null) {
            map.setMyLocationEnabled(true);
        }
        googleApiClient.connect();
    }

    /**
     * hides progress card and shows info card
     */
    private void showInfo() {
        progress.setVisibility(View.GONE);
        info.setVisibility(View.VISIBLE);
        pin.setVisibility(View.VISIBLE);
    }

    /**
     * hides info card and shows progress card after setting progress message
     *
     * @param message message to show next to circular progress bar
     */
    private void showProgressMessage(String message) {
        progressMessage.setText(message);

        info.setVisibility(View.GONE);
        errorButton.setVisibility(View.GONE);

        progress.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * hides info card and shows progress card after setting error message
     * and adds listener to the errorButton
     *
     * @param message       error message to show user
     * @param buttonTitle   title of the button
     * @param clickListener listener for button clicks
     */
    private void showErrorMessage(String message,
                                  String buttonTitle,
                                  View.OnClickListener clickListener) {
        progressMessage.setText(message);
        errorButton.setText(buttonTitle);

        info.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        errorButton.setVisibility(View.VISIBLE);

        errorButton.setOnClickListener(clickListener);
    }
}