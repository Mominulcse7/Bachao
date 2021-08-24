package com.mominulcse7.bachao.services;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.mominulcse7.bachao.R;

public class MyNetworkReceiver extends BroadcastReceiver {

    /*
    Help Link:
    ===========
    https://stackoverflow.com/questions/12570621/efficient-approach-to-continuously-check-whether-internet-connection-is-availabl
    https://www.youtube.com/watch?v=C9Ai1xrthKo
    */

    private static final String TAG = "MyNetworkReceiver";
    private static MyNetworkReceiver mInstance;
    private Snackbar snackbar;

    //Singleton pattern
    public static synchronized MyNetworkReceiver getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MyNetworkReceiver(context);
        }
        return mInstance;
    }
    private MyNetworkReceiver(Context context) {
        this.snackbar = Snackbar.make(((Activity)context).findViewById(android.R.id.content), context.getString(R.string.network_unavailable), Snackbar.LENGTH_INDEFINITE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if(isConnected) {
                if (snackbar != null) {
                    snackbar.dismiss();
                }
                Log.d(TAG, "Internet connection is connected");
            } else {
                if (snackbar != null) {
                    snackbar.show();
                }
                Log.d(TAG, "Internet connection is not connected");
            }
        }
    }

}
