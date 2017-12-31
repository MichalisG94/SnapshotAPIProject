package com.unipi.mgiavris.awarenessproject;

public class WeatherObj {

    public float temp, feels, dew;
    public int humidity;
    public String condition;

    public WeatherObj(){}

    public float getTemp() {
        return temp;
    }

    public float getFeels() {
        return feels;
    }

    public float getDew() {
        return dew;
    }

    public int getHumidity() {
        return humidity;
    }

    public String getCondition() {
        return condition;
    }

    public WeatherObj(float temp, float feels, float dew, int humidity, String condition) {
        this.temp = temp;
        this.feels = feels;
        this.dew = dew;
        this.humidity = humidity;
        this.condition = condition;
    }
}