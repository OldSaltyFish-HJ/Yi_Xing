package com.example.newmap2;

import com.amap.api.maps.model.LatLng;

public class MyLatLng {
    public static final int ABLE = 1;
    public static final int UNABLE = 0;
    private LatLng latLng;
    private int state;

    public MyLatLng(LatLng latLng, int state) {
        this.latLng = latLng;
        this.state = state;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
