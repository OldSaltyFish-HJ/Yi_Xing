package com.example.new_map;

import android.content.Context;
import android.location.LocationManager;

public class IsGpsWork {
    //判断GPS是否开启
    public static boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        // 判断GPS模块是否开启
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
