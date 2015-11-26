package com.iamkaan.whatstheweather.util.model;

/**
 * model class for weather object
 */
public class Weather {

    public String temp;
    public String dayHigh;
    public String dayLow;
    public String dayText;

    public Weather(String temp, String dayHigh, String dayLow, String dayText) {
        this.temp = temp;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.dayText = dayText;
    }
}
