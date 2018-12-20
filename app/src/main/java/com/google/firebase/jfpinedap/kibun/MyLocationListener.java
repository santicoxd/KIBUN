package com.google.firebase.jfpinedap.kibun;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class MyLocationListener implements LocationListener {

    private BaseStatsView activity;

    public MyLocationListener(BaseStatsView activity){
        this.activity = activity;
    }

    @Override
    public void onLocationChanged(Location loc) {
        activity.setLocation(loc);
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
