package com.mominulcse7.bachao;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.snackbar.Snackbar;
import com.mominulcse7.bachao.models.UserModel;
import com.mominulcse7.bachao.services.MyLocationReceiver;
import com.mominulcse7.bachao.services.MyNetworkReceiver;
import com.mominulcse7.bachao.utils.ConstantKey;
import com.mominulcse7.bachao.utils.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 99;
    private Activity activity;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private MyNetworkReceiver mNetworkReceiver;
    private MyLocationReceiver mLocationReceiver;

    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;

    private Marker sourceMarker = null, destinationMarker = null;
    private Marker currentLocationMarker = null;
    private ArrayList<Marker> markerList = new ArrayList<>();

    private Marker originDirectionMarker = null, destinationDirectionMarker = null;
    private Polyline polyline = null;
    private Map<String, Marker> markerMap = new HashMap<>();

    private static Location lastLocation;
    private static float angle;

    private LatLng origin = null, destination = null;
    private boolean isLocated = false;
    private boolean isSetLocationOnMap = false;
    private boolean isDestination = true;
    private boolean isStoreRiderInfo = false;

    private UserModel userModel = null;
    private LinearLayout llAlert, llSafe, llHelp;
    private RelativeLayout rlAlert;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(getResources().getColor(R.color.white));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        setContentView(R.layout.activity_map);

        activity = this;
        mNetworkReceiver = MyNetworkReceiver.getInstance(this);
        mLocationReceiver = MyLocationReceiver.getInstance(this);

        userModel = new UserModel();
        userModel.setId("01");
        userModel.setName("Mominul");
        userModel.setUserType("0");
        userModel.setToken("");

        Places.initialize(this, ConstantKey.GOOGLE_PLACE_API);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        rlAlert = findViewById(R.id.rlAlert);
        llAlert = findViewById(R.id.llAlert);
        llSafe = findViewById(R.id.llSafe);
        llHelp = findViewById(R.id.llHelp);

        rlAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userModel.getUserType().equals("1")) {
                    userModel.setUserType("0");
                    llAlert.setVisibility(View.VISIBLE);
                    llSafe.setVisibility(View.GONE);
                } else {
                    userModel.setUserType("1");
                    llAlert.setVisibility(View.GONE);
                    llSafe.setVisibility(View.VISIBLE);
                }
                onLocationChanged(lastLocation);
            }
        });

        llHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(activity, IncomingCallActivity.class));
            }
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(0, 0, 10, 140);

        if (Utility.getInstance().haveNetwork(this) && Utility.getInstance().isEnabledLocation(this)) {
            requestPermissions(); //If above the android version Marshmallow then call the location permission
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.network_location_unavailable), Snackbar.LENGTH_LONG).show();
        }

        //-------------------------------------------| Custom Marker like Uber
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (origin != null && sourceMarker != null) {
                    Point PickupPoint = mMap.getProjection().toScreenLocation(origin);
                    sourceMarker.setAnchor(PickupPoint.x < dpToPx(activity, 200) ? 0.00f : 1.00f, PickupPoint.y < dpToPx(activity, 100) ? 0.20f : 1.20f);
                }
                if (destination != null && destinationMarker != null) {
                    Point PickupPoint = mMap.getProjection().toScreenLocation(destination);
                    destinationMarker.setAnchor(PickupPoint.x < dpToPx(activity, 200) ? 0.00f : 1.00f, PickupPoint.y < dpToPx(activity, 100) ? 0.20f : 1.20f);
                }
            }
        });
    }

    //===============================================| Custom Marker like Uber
    private Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    private int dpToPx(Context context, float dpValue) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dpValue * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    //====================================| Current Location Icon
    private void changeCurrentLocationIcon() {
        if (mapFragment != null) {
            ImageView btnMyLocation = (ImageView) ((View) mapFragment.getView()
                    .findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            btnMyLocation.setImageResource(R.drawable.selector_my_location);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) btnMyLocation.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            //layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
            btnMyLocation.setLayoutParams(layoutParams);
        }
    }

    //Explain why the app needs the request permissions
    //https://developers.google.com/maps/documentation/android-sdk/location
    public void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION); //if there is no permission allowed then, display permission request dialog
        } else {
            mMap.setMyLocationEnabled(true);
            changeCurrentLocationIcon();
            getDeviceLocation();
            startLocationUpdates();
        }
    }

    //====================================| Handle the permissions request response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //For allow button
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    changeCurrentLocationIcon();
                    getDeviceLocation();
                    startLocationUpdates();
                }
            } else {
                //For denied button
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                requestPermissions();
            }
        }
    }

    //===============================================| Trigger new location updates at interval
    public void startLocationUpdates() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        onLocationChanged(location);
                    }
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5 * 1000); //long UPDATE_INTERVAL = 10 * 1000;  /* 5 secs */
        mLocationRequest.setFastestInterval(2000); //long FASTEST_INTERVAL = 2000; /* 2 sec */

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    public void onLocationChanged(Location location) {
        if (location != null) {
            if (!isLocated) {
                // placeOrigin.setText(Utility.getInstance().getAddress(HomeActivity.this, new LatLng(location.getLatitude(), location.getLongitude())));
                Utility.getInstance().goToLocation(mMap, location.getLatitude(), location.getLongitude(), 16);
                isLocated = true;
            }

            //If change the current location OR do not change current location
//            if (RiderPrefManager.getInstance(HomeActivity.this).getIsOrigin()) {
//                if (RiderPrefManager.getInstance(HomeActivity.this).getIsUserLatLng()) {
//                    getUserLatLng(); //Get destination and origin location from user
//                } else {
//                    if (RiderPrefManager.getInstance(HomeActivity.this).getOriginLatLng() != null) {
//                        origin = RiderPrefManager.getInstance(HomeActivity.this).getOriginLatLng(); //Change current location data
//                    }
//                }
//            } else {
//                if (RiderPrefManager.getInstance(HomeActivity.this).getIsUserLatLng()) {
//                    getUserLatLng(); //Get destination and origin location from user
//                } else {
//                    origin = new LatLng(location.getLatitude(), location.getLongitude());
//                    RiderPrefManager.getInstance(HomeActivity.this).saveOriginLatLng(origin); //Update current location from GPS
//                }
//            }

//            if (onlineOffline.isChecked() && riderModel != null && origin != null && !RiderPrefManager.getInstance(this).getIsAccepted()) {
//                GeoFireDAO.getInstance().setLocation(ConstantKey.RIDER_POSITION_NODE, ConstantKey.RIDER_ORIGIN_NODE, Utility.getInstance().checkRiderVehicle(riderModel.getVehicleType()), riderModel.getAuthId(), new GeoLocation(origin.latitude, origin.longitude));
//                FirebaseDAO.getInstance(this).storeRiderOriginPositionRecord(Utility.getInstance().checkRiderVehicle(riderModel.getVehicleType()), riderModel.getAuthId()); //To remove expired data by checking cloud function
//            }

            //After confirming, Origin/current location save to riders > origin. Now user gets driver current position continuously
//            if (riderModel != null && origin != null) {
//                GeoFireDAO.getInstance().setLocation(ConstantKey.RIDER_NODE, ConstantKey.RIDER_ORIGIN_NODE, Utility.getInstance().checkRiderVehicle(riderModel.getVehicleType()), riderModel.getAuthId(), new GeoLocation(origin.latitude, origin.longitude));
//            }

            //It is used for vehicle moving icon
            if (userModel != null) {
                addMarker(location, Utility.getInstance().setUserMarker(activity, userModel.getUserType()));
            }
            if (lastLocation != null) {
                double bearing = angleFromCoordinate(lastLocation.getLatitude(), lastLocation.getLongitude(), location.getLatitude(), location.getLongitude());
                changeMarkerPosition(bearing);
            }
            lastLocation = location;
        }
    }

    //Get destination and origin location from user
//    private void getUserLatLng() {
//        if (RiderPrefManager.getInstance(HomeActivity.this).getUserLatLng().size() > 0) {
//            ArrayList<LatLng> arr = RiderPrefManager.getInstance(HomeActivity.this).getUserLatLng(); //Get user LatLng to show direction, Only current location portion
//            //Log.d(TAG, "Test User: "+Utility.getInstance().getAddress(this, arr.get(0))+" | "+Utility.getInstance().getAddress(this, arr.get(1)));
//            origin = arr.get(0);
//            destination = arr.get(1);
//
//            if (riderModel != null && origin != null && destination != null && !RiderPrefManager.getInstance(this).getIsDirection()) {
//                NotificationSeatBookingModel nsb = RiderPrefManager.getInstance(HomeActivity.this).getSeatBookingModel();
//                if (nsb != null && Integer.parseInt(nsb.getAvailableSeat()) > 0) {
//                    /*if (nsb.getRidingType().equals("REGULAR")) {
//                        getOnlyDirectionLine(origin, destination);
//                    } else {
//                        getDirectionLine(riderModel, origin, destination);
//                    }*/
//                    getDirectionLine(riderModel, origin, destination);
//                }
//            }
//
//            if (RiderPrefManager.getInstance(this).getIsDirection() && !RiderPrefManager.getInstance(this).getIsAccepted()) {
//                storeRiderOriginPositionRecord(); //This method runs when destination is fixed | Problem previous key data save
//            }
//        }
//    }
//
//    private void storeRiderOriginPositionRecord() {
//        ArrayList<String> keys = RiderPrefManager.getInstance(HomeActivity.this).getDirectionKeys();
//        if (keys != null) {
//            for (int i = 0; i < keys.size(); i++) {
//                FirebaseDAO.getInstance(this).storeRiderOriginPositionRecord(Utility.getInstance().checkRiderVehicle(riderModel.getVehicleType()), keys.get(i)); //To remove expired data by checking cloud function
//            }
//        }
//    }

    //===============================================| Get Device Location/LatLng
    public void getDeviceLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        isLocated = true;
                        origin = new LatLng(location.getLatitude(), location.getLongitude());
                        Utility.getInstance().moveToLocation(activity, mMap, new LatLng(origin.latitude, origin.longitude));
                    }
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    //===============================================| Display user search position who are the searched within 5 minutes
    private void getUserSearchPositionByGeoFire(String riderVehicle, Location location, final Drawable drawable) {
        removeMarker();
//        GeoFireDAO.getInstance().getLocationOneRadius(new GeoFireDAO.LocationByRadiusCallback() {
//            @Override
//            public void onCallback(String key, GeoLocation location) {
//                markerList.add(mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)).icon(getMarkerIconFromDrawable(drawable))));
//            }
//        }, ConstantKey.USER_SEARCH_NODE, ConstantKey.RIDER_ORIGIN_NODE, Utility.getInstance().checkRiderVehicle(riderVehicle), new GeoLocation(location.getLatitude(), location.getLongitude()));
//
    }

    private void removeMarker() {
        if (markerList != null) {
            for (Marker marker : markerList) {
                marker.remove();
            }
        }
    }

    //===============================================| Marker/Bike rotates to direction using onLocationChanged
    private void addMarker(Location location, Drawable drawable) {
        LatLng latLng = new LatLng((location.getLatitude()), (location.getLongitude()));
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        currentLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                .draggable(true)
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_motorbike))
                .icon(getMarkerIconFromDrawable(drawable))
                .rotation(location.getBearing())
                .flat(true)
                .anchor(0.5f, 0.5f)
                .alpha((float) 0.91));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
    }

    private void changeMarkerPosition(double position) {
        float direction = (float) position;
        //Log.e("LocationBearing", "" + direction);

        if (direction == 360.0) {
            currentLocationMarker.setRotation(angle); //default
        } else {
            currentLocationMarker.setRotation(direction);
            angle = direction;
        }
    }

    private double angleFromCoordinate(double lat1, double long1, double lat2, double long2) {
        double distanceLong = (long2 - long1);

        double y = Math.sin(distanceLong) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(distanceLong);

        double bearing = Math.atan2(x, y);
        bearing = Math.toDegrees(bearing);
        bearing = (bearing + 360) % 360;
        bearing = 360 - bearing;

        return bearing;
    }

    //===============================================| Add Marker and Move Map Camera
    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    //===============================================| Direction
//    public void getDirectionLine(final RiderModel riderModel, final LatLng start, final LatLng end) {
//        new DirectionAsyncTask(this, new DirectionAsyncTask.AsyncResponse() {
//            @Override
//            public void processFinish(DirectionModel model) {
//                if (model != null) {
//                    //Log.d(TAG, "DirectionAsyncTask: "+ model.getPointsList().size() +" | "+ model.getStartAddress()+" | "+model.getEndAddress()+" | "+model.getDistance()+" km | "+model.getDuration()+" mins");
//                    //Log.d(TAG, "DirectionAsyncTask: "+ "Start: "+model.getPointsList().get(0) + " End: "+model.getPointsList().get(model.getPointsList().size() - 1));
//                    clearMarkerDirection();
//
//                    PolylineOptions options = new PolylineOptions().width(10).color(Color.parseColor("#444444")).geodesic(true);
//                    final ArrayList<String> keys = new ArrayList<>();
//
//                    FirebaseDAO.getInstance(HomeActivity.this).storeRiderInfoById(riderModel, riderModel.getAuthId());
//                    keys.add(riderModel.getAuthId());
//
//                    for (int i = 0; i < model.getPointsList().size(); i++) {
//                        LatLng point = model.getPointsList().get(i);
//                        options.add(point);
//
//                        int count = (int) Math.round((model.getPointsList().size() / model.getDistance())) * (i + 1);
//                        if (count < model.getPointsList().size()) {
//                            //setMarker(model.getPointsList().get(count));
//                            LatLng location = model.getPointsList().get(count);
//                            //Log.d(TAG, "DirectionAsyncTask: "+ model.getPointsList().get(count));
//                            //storeRiderInfoByPushId(), setLocationByPushId(origin), setLocationByPushId(destination), storeRiderGetRequest()
//                            FirebaseDAO.getInstance(HomeActivity.this).storeRiderInfoByPushId(new FirebaseDAO.StoreRiderCallback() {
//                                @Override
//                                public void onCallback(String pushId) {
//                                    keys.add(pushId);
//                                    Log.e(TAG, "DirectionAsyncTask: " + pushId);
//                                    RiderPrefManager.getInstance(HomeActivity.this).saveDirectionKeys(keys);
//                                }
//                            }, riderModel, new GeoLocation(location.latitude, location.longitude), new GeoLocation(location.latitude, location.longitude)); //only one destination fixed: new GeoLocation(end.latitude, end.longitude)
//                        }
//
//                        //if (i == model.getPointsList().size()-1) {} //Last index
//                    }
//
//                    originDirectionMarker = mMap.addMarker(new MarkerOptions().position(start).title(model.getStartAddress()).icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_radio_button_checked_black_24dp))));
//                    destinationDirectionMarker = mMap.addMarker(new MarkerOptions().position(end).icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_radio_button_checked_black_24dp))));
//
//                    polyline = mMap.addPolyline(options);
//                    setCameraWithCoordinationBounds(model.getSouthwest(), model.getNortheast());
//
//                    //-----------------------------------| Custom Marker like Uber
//                    View markerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
//                    ((TextView) markerView.findViewById(R.id.address_text)).setText("" + model.getStartAddress());
//                    ((TextView) markerView.findViewById(R.id.duration_text)).setText("" + model.getDurationText());
//                    ((TextView) markerView.findViewById(R.id.distance_text)).setText("" + model.getDistanceText());
//                    sourceMarker = mMap.addMarker(new MarkerOptions().position(start).icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(HomeActivity.this, markerView))).anchor(0.00f, 0.20f));
//
//                    RiderPrefManager.getInstance(HomeActivity.this).saveIsDirection(true);
//                }
//            }
//        }).execute(start, end);
//    }
//
//    public void getOnlyDirectionLine(final LatLng start, final LatLng end) {
//        new DirectionAsyncTask(this, new DirectionAsyncTask.AsyncResponse() {
//            @Override
//            public void processFinish(DirectionModel model) {
//                if (model != null) {
//                    clearMarkerDirection();
//
//                    //Log.d(TAG, "getOnlyDirectionLine: "+start+end);
//
//                    PolylineOptions options = new PolylineOptions().width(10).color(Color.parseColor("#444444")).geodesic(true);
//                    for (int i = 0; i < model.getPointsList().size(); i++) {
//                        options.add(model.getPointsList().get(i));
//                    }
//
//                    originDirectionMarker = mMap.addMarker(new MarkerOptions().position(start).title(model.getStartAddress()).icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_radio_button_checked_black_24dp))));
//                    destinationDirectionMarker = mMap.addMarker(new MarkerOptions().position(end).icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_radio_button_checked_black_24dp))));
//
//                    polyline = mMap.addPolyline(options);
//                    setCameraWithCoordinationBounds(model.getSouthwest(), model.getNortheast());
//
//                    //-----------------------------------| Custom Marker like Uber
//                    View markerView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
//                    ((TextView) markerView.findViewById(R.id.address_text)).setText("" + model.getStartAddress());
//                    ((TextView) markerView.findViewById(R.id.duration_text)).setText("" + model.getDurationText());
//                    ((TextView) markerView.findViewById(R.id.distance_text)).setText("" + model.getDistanceText());
//                    sourceMarker = mMap.addMarker(new MarkerOptions().position(start).icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(HomeActivity.this, markerView))).anchor(0.00f, 0.20f));
//                }
//            }
//        }).execute(start, end);
//    }

    private void setCameraWithCoordinationBounds(LatLng southwest, LatLng northeast) {
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void setMarker(String pushId, LatLng latLng, String title) {
        if (mMap != null) {
            markerMap.put(pushId, mMap.addMarker(new MarkerOptions().position(latLng).title(title)
                    .icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_traffic_stop)))));
        }
    }

    private void clearMarkerDirection() {
        if (sourceMarker != null && originDirectionMarker != null && destinationDirectionMarker != null && polyline != null) {
            sourceMarker.remove();
            originDirectionMarker.remove();
            destinationDirectionMarker.remove();
            polyline.remove();
        }
    }

    //====================================================| Get BroadcastReceiver | Get Notification Data from MyFirebaseMessagingService
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = new Bundle();
//            if (intent.getExtras() != null) {
//                String bookingModel = intent.getExtras().getString(ConstantKey.SEAT_BOOKING_MODEL);
//                bundle.putString(ConstantKey.SEAT_BOOKING_MODEL, bookingModel);
//                openBottomSheetDialog(bundle);
//            }
        }
    };

    private void openBottomSheetDialog(Bundle bundle) {
//        MyBottomSheetDialog bottomSheet = new MyBottomSheetDialog();
//        bottomSheet.setArguments(bundle);
//        bottomSheet.setCancelable(false);
//        //FragmentTransaction transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
//        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)); //After Oreo version this code must be used
            registerReceiver(mLocationReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));  //After Oreo version this code must be used
            LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), new IntentFilter(ConstantKey.NOTIFICATION_BROADCAST_RECEIVER)); //After Oreo version this code must be used

            //if (SharedPrefManager.getInstance(getApplicationContext()).getIsUpdatedLocation()) { }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mFusedLocationClient != null && mLocationRequest != null && mLocationCallback != null) {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mNetworkReceiver); //After Oreo version this code must be used
            unregisterReceiver(mLocationReceiver); //After Oreo version this code must be used
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver); //After Oreo version this code must be used

            //SharedPrefManager.getInstance(getApplicationContext()).saveIsUpdatedLocation(false);
            if (mFusedLocationClient != null && mLocationCallback != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}