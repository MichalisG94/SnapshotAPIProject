package com.unipi.mgiavris.awarenessproject;

import com.google.android.gms.maps.model.LatLng;

public class UserDataObj {

    public String headphones, userActivity, userID, timestamp;
    public WeatherObj weather;
    public LatLng latLng;
    public PlacesObj places;

    public UserDataObj() {}

    public String getUserID() {
        return userID;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHeadphones() {
        return headphones;
    }

    public String getUserActivity() {
        return userActivity;
    }

    public WeatherObj getWeather() {
        return weather;
    }

    public PlacesObj getPlaces() {
        return places;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    //ADD PLACES & BEACONS //TODO

    public UserDataObj(String userID, String timestamp, String headphones, String userActivity, WeatherObj weather, PlacesObj places, LatLng latLng) {
        this.userID = userID;
        this.timestamp = timestamp;
        this.headphones = headphones;
        this.userActivity = userActivity;
        this.weather = weather;
        this.places = places;
        this.latLng = latLng;
    }
}
