package com.mominulcse7.bachao.services;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.mominulcse7.bachao.R;

public class MyLocationReceiver extends BroadcastReceiver {

    /*
    Help Link:
    ===========
    https://stackoverflow.com/questions/47216479/android-check-if-user-turned-off-location
    */

    private static final String TAG = "MyLocationReceiver";
    private static MyLocationReceiver mInstance;
    private Snackbar snackbar;

    //Singleton pattern
    public static synchronized MyLocationReceiver getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MyLocationReceiver(context);
        }
        return mInstance;
    }
    private MyLocationReceiver(Context context) {
        this.snackbar = Snackbar.make(((Activity)context).findViewById(android.R.id.content), context.getString(R.string.location_unavailable), Snackbar.LENGTH_INDEFINITE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(gpsEnabled && networkEnabled) {
                if (snackbar != null) {
                    snackbar.dismiss();
                }
                Log.d(TAG, "GPS is enabled");
            } else {
                if (snackbar != null) {
                    snackbar.show();
                }
                Log.d(TAG, "GPS is disabled");
            }
        }

    }

}
