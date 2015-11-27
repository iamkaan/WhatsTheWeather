package com.iamkaan.whatstheweather.listener;

import com.iamkaan.whatstheweather.util.model.Weather;

/**
 * listener class for results of fetching weather info
 */
public interface WeatherInfoFetchListener {
    void onFetch(Weather result);
    void onError(Exception exception);
}
