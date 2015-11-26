package com.iamkaan.whatstheweather.util;

import android.content.Context;
import android.location.Location;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.iamkaan.whatstheweather.listener.WeatherInfoFetchListener;
import com.iamkaan.whatstheweather.util.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * includes method for fetching current weather for given location
 */
public class WeatherHelper {

    private static final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String URL_SETTINGS = "&format=json";

    private static final String JSON_QUERY = "query";
    private static final String JSON_RESULTS = "results";
    private static final String JSON_CHANNEL = "channel";
    private static final String JSON_ITEM = "item";
    private static final String JSON_CONDITION = "condition";
    private static final String JSON_FORECAST = "forecast";
    private static final String JSON_TEMP = "temp";
    private static final String JSON_HIGH = "high";
    private static final String JSON_LOW = "low";
    private static final String JSON_TEXT = "text";

    public static void getWeatherInfo(Context context, Location location, final WeatherInfoFetchListener listener) {
        Volley.newRequestQueue(context).add(
                new StringRequest(BASE_URL + getQuery(location) + URL_SETTINGS,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    listener.onFetch(resolveJson(response));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    listener.onError(e);
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                error.printStackTrace();
                                listener.onError(error);
                            }
                        }
                )
        );
    }

    private static Weather resolveJson(String jsonString) throws JSONException {
        JSONObject itemAll = new JSONObject(jsonString);
        JSONObject query = itemAll.getJSONObject(JSON_QUERY);
        JSONObject results = query.getJSONObject(JSON_RESULTS);
        JSONObject channel = results.getJSONObject(JSON_CHANNEL);
        JSONObject item = channel.getJSONObject(JSON_ITEM);
        JSONObject condition = item.getJSONObject(JSON_CONDITION);
        JSONArray forecast = item.getJSONArray(JSON_FORECAST);
        JSONObject todaysForecast = forecast.getJSONObject(0);

        return new Weather(
                condition.getString(JSON_TEMP),
                todaysForecast.getString(JSON_HIGH),
                todaysForecast.getString(JSON_LOW),
                todaysForecast.getString(JSON_TEXT)
        );
    }

    private static String getQuery(Location location) {
        return ("select item from weather.forecast where woeid in " +
                "(select woeid from geo.placefinder where text=\"" +
                location.getLatitude() +
                "," +
                location.getLongitude() +
                "\" and gflags=\"R\")" +
                //setting units as celsius
                " and u=\"c\"")
                .replace(" ", "%20");
    }
}
