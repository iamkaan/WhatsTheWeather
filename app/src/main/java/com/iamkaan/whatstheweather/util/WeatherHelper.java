package com.iamkaan.whatstheweather.util;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.iamkaan.whatstheweather.listener.WeatherInfoFetchListener;
import com.iamkaan.whatstheweather.util.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * includes methods for fetching current and daily weather for requested location
 */
public class WeatherHelper {

    private static final String TAG = "whatstheweather";

    private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String URL_SETTINGS = "&format=json";

    private static final String BASE_ICON_URL = "http://l.yimg.com/a/i/us/we/52/";
    private static final String ICON_URL_EXTENSION = ".gif";

    private static final String JSON_QUERY = "query";
    private static final String JSON_RESULTS = "results";
    private static final String JSON_CHANNEL = "channel";
    private static final String JSON_ITEM = "item";
    private static final String JSON_CONDITION = "condition";
    private static final String JSON_FORECAST = "forecast";
    private static final String JSON_TEMP = "temp";
    private static final String JSON_CODE = "code";
    private static final String JSON_HIGH = "high";
    private static final String JSON_LOW = "low";
    private static final String JSON_TEXT = "text";

    private static final int TEMP_CRAZY_HIGH = 40;
    private static final int TEMP_HIGH = 25;
    private static final int TEMP_NORMAL = 17;
    private static final int TEMP_LOW = 5;

    private static final int COLOR_CRAZY_HIGH = Color.rgb(255, 152, 0);
    private static final int COLOR_HIGH = Color.rgb(255, 193, 7);
    private static final int COLOR_NORMAL = Color.rgb(255, 235, 59);
    private static final int COLOR_LOW = Color.rgb(93, 169, 244);
    private static final int COLOR_CRAZY_LOW = Color.rgb(68, 138, 255);

    /**
     * fetches and parses the weather information for requested location by using Volley
     *
     * @param context  required by Volley
     * @param lat      latitude of the requested location
     * @param lng      longitude of the requested location
     * @param listener listener to track result
     */
    public static void getWeatherInfo(final Context context,
                                      final double lat,
                                      final double lng,
                                      final WeatherInfoFetchListener listener) {
        Volley.newRequestQueue(context).add(
                new StringRequest(BASE_URL + getQuery(lat, lng) + URL_SETTINGS,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    Weather result = resolveJson(response);
                                    try {
                                        Geocoder gcd = new Geocoder(context, Locale.getDefault());
                                        List<Address> addresses = gcd.getFromLocation(lat, lng, 1);
                                        if (addresses.size() > 0) {
                                            result.location = addresses.get(0).getLocality();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    listener.onFetch(result);
                                } catch (JSONException e) {
                                    Log.e(TAG, BASE_URL + getQuery(lat, lng) + URL_SETTINGS);
                                    e.printStackTrace();
                                    listener.onError(e);
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e(TAG, BASE_URL + getQuery(lat, lng) + URL_SETTINGS);
                                error.printStackTrace();
                                listener.onError(error);
                            }
                        }
                )
        );
    }

    /**
     * resolves the expected weather info json
     *
     * @param jsonString string to create json object
     * @return weather object
     * @throws JSONException if json has unexpected structure
     */
    private static Weather resolveJson(String jsonString) throws JSONException {
        String dayHigh = "";
        String dayLow = "";
        String dayText = "";
        String currentTemp = "";
        String iconURL = "";

        JSONObject item;

        try {
            JSONObject itemAll = new JSONObject(jsonString);
            JSONObject query = itemAll.getJSONObject(JSON_QUERY);
            JSONObject results = query.getJSONObject(JSON_RESULTS);
            JSONObject channel = results.getJSONObject(JSON_CHANNEL);
            item = channel.getJSONObject(JSON_ITEM);
        } catch (JSONException e) {
            e.printStackTrace();
            throw e;
        }

        try {
            JSONArray forecast = item.getJSONArray(JSON_FORECAST);
            JSONObject todaysForecast = forecast.getJSONObject(0);

            dayHigh = todaysForecast.getString(JSON_HIGH);
            dayLow = todaysForecast.getString(JSON_LOW);
            dayText = todaysForecast.getString(JSON_TEXT);
        } catch (JSONException ignored) {
        }

        try {
            JSONObject condition = item.getJSONObject(JSON_CONDITION);
            currentTemp = condition.getString(JSON_TEMP);
            iconURL = getIconURL(condition.getString(JSON_CODE));
        } catch (JSONException ignored) {
        }

        if (iconURL.isEmpty()) {
            throw new JSONException("Couldn't resolve weather json!");
        }

        return new Weather(currentTemp, iconURL, dayHigh, dayLow, dayText);
    }

    /**
     * creates a query for Yahoo Weather API
     *
     * @param lat latitude of requested location
     * @param lng longitude of requested location
     * @return query string
     */
    private static String getQuery(double lat, double lng) {
        return ("select item from weather.forecast where woeid in " +
                "(select woeid from geo.placefinder where text=\"" +
                lat +
                "," +
                lng +
                "\" and gflags=\"R\")" +
                //setting units as celsius
                " and u=\"c\"")
                .replace(" ", "%20");
    }

    /**
     * adds related weather condition code to base icon URLs
     *
     * @param code weather condition code returned by Yahoo
     * @return icon url provided by Yahoo
     */
    private static String getIconURL(String code) {
        return BASE_ICON_URL + code + ICON_URL_EXTENSION;
    }

    /**
     * warm: orange
     * cold: blue
     *
     * @param temp temperature in string format and celsius unit
     * @return color int based on the temperature
     */
    public static int getWeatherColor(String temp) {
        int temperature = Integer.parseInt(temp);
        if (temperature > TEMP_CRAZY_HIGH) {
            return COLOR_CRAZY_HIGH;
        } else if (temperature > TEMP_HIGH) {
            return COLOR_HIGH;
        } else if (temperature > TEMP_NORMAL) {
            return COLOR_NORMAL;
        } else if (temperature > TEMP_LOW) {
            return COLOR_LOW;
        } else {
            return COLOR_CRAZY_LOW;
        }
    }
}
