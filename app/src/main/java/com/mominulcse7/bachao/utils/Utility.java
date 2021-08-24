package com.mominulcse7.bachao.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.mominulcse7.bachao.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Utility {

    private static final String TAG = "Utility";
    private static Utility mInstance;

    public static synchronized Utility getInstance() {
        if (mInstance == null) {
            mInstance = new Utility();
        }
        return mInstance;
    }

    //====================================================| Round a double to 2 decimal
    public double roundTwoDecimal(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    //===============================================| Get Address
    //https://stackoverflow.com/questions/9409195/how-to-get-complete-address-from-latitude-and-longitude
    public String getAddress(Context context, LatLng latLng) {
        String address = null, finalAddress = null;
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                String addr = addressList.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addressList.get(0).getLocality();
                String state = addressList.get(0).getAdminArea();
                String country = addressList.get(0).getCountryName();
                String postalCode = addressList.get(0).getPostalCode();
                String knownName = addressList.get(0).getFeatureName(); // Only if available else return NULL

                address = addressList.get(0).getAddressLine(0);
                try {
                    finalAddress = address.replace(", Bangladesh", "");
                } catch (Exception e) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return finalAddress;
    }

    //====================================================| Set Marker Icon using Vehicle
    public Drawable setUserMarker(Context context, String str) {
        Drawable drawable = null;
        if (str.equals("0")) {
            drawable = context.getResources().getDrawable(R.drawable.ic_active_green);
        } else if (str.equals("1")) {
            drawable = context.getResources().getDrawable(R.drawable.ic_active_red);
        } else
            drawable = context.getResources().getDrawable(R.drawable.ic_active_green);
        return drawable;
    }

    //===============================================| Move Map Camera
    public void goToLocation(GoogleMap mMap, double latitude, double longitude, int zoom) {
        LatLng latLng = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
    }

    public void moveToLocation(Context context, GoogleMap mMap, LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        mMap.animateCamera(CameraUpdateFactory.zoomIn()); // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16), 2000, null); // Zoom out to zoom level 10, animating with a duration of 2 seconds.
    }

    //====================================================| Close/hide the Android Soft Keyboard
    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //===============================================| Check Internet Connection
    public boolean haveNetwork(final Activity activity) {
        boolean have_Wifi = false;
        boolean have_MobileData = false;

        ConnectivityManager manager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] infos = manager.getAllNetworkInfo();
        for (NetworkInfo info : infos) {
            if (info.getTypeName().equalsIgnoreCase("WIFI")) {
                if (info.isConnected()) {
                    have_Wifi = true;
                }
            }
            if (info.getTypeName().equalsIgnoreCase("MOBILE")) {
                if (info.isConnected()) {
                    have_MobileData = true;
                }
            }
        }
        return have_Wifi | have_MobileData;
    }

    //===============================================| Check Location Connection
    public boolean isEnabledLocation(final Activity activity) {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

        /*if(!gpsEnabled && !networkEnabled) {
            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(myIntent);
        }*/
        return (!gpsEnabled && !networkEnabled) ? false : true;
    }

    //===============================================| Encryption and Decryption
    public String encode(String input) {
        try {
            byte[] data = input.getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String decode(String base64) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            return new String(data, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //===============================================| Random alphanumeric
    ////UUID.randomUUID().toString()
    public String getRandomString(int length) {
        final String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJLMNOPQRSTUVWXYZ1234567890!@#$%^&*()_+";
        StringBuilder result = new StringBuilder();
        while (length > 0) {
            Random rand = new Random();
            result.append(characters.charAt(rand.nextInt(characters.length())));
            length--;
        }
        return result.toString();
    }

    public String getRandomNumber() {
        return String.valueOf(new Random().nextInt(999999) + 1);
    }

}
