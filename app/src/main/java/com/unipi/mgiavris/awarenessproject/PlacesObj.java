package com.unipi.mgiavris.awarenessproject;

public class PlacesObj {

    public String placeName, placeAddress;

    public PlacesObj(){}

    public String getPlaceName() {
        return placeName;
    }

    public String getPlaceAddress() {
        return placeAddress;
    }

    public PlacesObj(String placeName, String placeAddress) {
        this.placeName = placeName;
        this.placeAddress = placeAddress;
    }
}