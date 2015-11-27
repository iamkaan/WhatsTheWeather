package com.iamkaan.whatstheweather.util.model;

import java.io.Serializable;

/**
 * model class for weather object
 */
public class Weather implements Serializable {

    public String location;
    public String temp;
    public String iconURL;
    public String dayHigh;
    public String dayLow;
    public String dayText;

    public Weather(String temp, String iconURL, String dayHigh, String dayLow, String dayText) {
        this.temp = temp;
        this.iconURL = iconURL;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.dayText = dayText;
        location = "";
    }
}
