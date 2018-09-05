package com.unipi.mgiavris.awarenessproject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class AwarenessActivity extends AppCompatActivity implements OnMapReadyCallback, DatePickerDialog.OnDateSetListener{

    private static final String TAG = "AwarenessActivity";
    private static final int REQ_MULTIPLE_PERMISSION = 100;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private MarkerOptions markerOptions = new MarkerOptions();
    private ArrayList<UserDataObj> userDatas = new ArrayList<>();
    public String selectedDate;
    private GoogleMap googleMap;
    private boolean weatherIcons = false, activityIcons = false, speedIcons = false, locationIcon = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_awareness);
        Log.d(TAG, "onCreate() activity");


        if(!checkPermission()) {
            Log.d(TAG, "if(!checkPermission())");
            askPermission();
        } else {
            Log.d(TAG, "else()");
            startService(new Intent(this, DataCollectionService.class));
        }

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() activity");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_MULTIPLE_PERMISSION:

                if(grantResults.length > 0) {
                    boolean locationPermission = (grantResults[1] == PackageManager.PERMISSION_GRANTED);
                    boolean phoneStatePermission = (grantResults[0] == PackageManager.PERMISSION_GRANTED);

                    if(locationPermission && phoneStatePermission) {
                        //Start the DataCollectionService Service when the app is launched
                        startService(new Intent(this, DataCollectionService.class));
                    } else {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AwarenessActivity.this);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permissions needed");
                        alertBuilder.setMessage("Permissions are required in order to run the app.");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();
                    }
                    break;
                }
        }
    }

    // Check if the permission is granted.
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission() - AwarenessActivity");
        if(Build.VERSION.SDK_INT >= 23) {
            return ((ContextCompat.checkSelfPermission(AwarenessActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(AwarenessActivity.this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED));
        } else {
            return true;
        }
    }

    // Ask for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.READ_PHONE_STATE}, REQ_MULTIPLE_PERMISSION);
    }

    //Generate unique identifier
    public static String getUID(Context context, String method) {
        Log.d(TAG, "getUID() -- Called by --||-- " + method);
        String uid;
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            uid = tManager.getDeviceId();
            return uid;
        } else {
            Log.d(TAG, "Permission Denied, could not get deviceID");
            return null;
        }
    }

    @Override
    public void onMapReady(GoogleMap mGoogleMap) {
        Log.d(TAG, "onMapReady()");
        googleMap = mGoogleMap;
        pickDate();
        //getUserData(googleMap);
    }

    private void pickDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, AwarenessActivity.this, year, month, day);
        //datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.getDatePicker().setMaxDate(c.getTimeInMillis()); // Αποτρέπει την επιλογή μελλοντικής ημερομηνίας
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {
        Log.d(TAG, "onDateSet - " + selectedDay + "/" + selectedMonth + "/" + selectedYear);
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth, selectedDay);
        selectedDate = DateFormat.format("dd-MM-yyyy", cal).toString();
        getUserData();
    }

    // Add every user's LatLng point to a list
    private void getUserData() {
        Log.d(TAG, "getUserData");
        userDatas.clear();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String key, timestamp, headphones, activity;
                LatLng latLng;
                WeatherObj weatherObj;
                PlacesObj placesObj;
                double speed;
                String currUserID = getUID(AwarenessActivity.this, "createMultMarkers()");
                for(DataSnapshot data1 : dataSnapshot.getChildren()) {
                    //Log.d(TAG, "DATA1 " + data1);
                    key = data1.getKey();
                    if(key.equals(currUserID)) {
                        for (DataSnapshot data2 : data1.getChildren()) {
                            //Log.d(TAG, "data2 " + data2);
                            timestamp = data2.getKey().substring(0, 10); //Επιλέγεται μονο η ημερομηνια και οχι η ώρα
                            if (timestamp.equals(selectedDate)) { //Προσθέτουμε μόνο τα στοιχεία που ταιριάζουν με την επιλεγμένη ημέρα
                                if (data2.child("headphones").exists()) {
                                    headphones = data2.child("headphones").getValue().toString();
                                } else headphones = null;

                                if (data2.child("activity").exists() && !data2.child("activity").getValue().equals("null")) {
                                    activity = data2.child("activity").getValue().toString();
                                } else activity = null;

                                if (data2.child("location").exists() && !data2.child("location").getValue().equals("null")) {
                                    latLng = new LatLng((double) data2.child("location").child("latitude").getValue(), (double) data2.child("location").child("longitude").getValue());
                                } else latLng = null;

                                if (data2.child("location").child("speed").exists() && !data2.child("location").getValue().equals("null")) {
                                    speed = Math.round(3.6 * (Double.parseDouble(data2.child("location").child("speed").getValue().toString())));
                                } else speed = -1; //-1 means that speed doesn't exist

                                if (data2.child("weather").exists() && !data2.child("weather").getValue().equals("null")) {
                                    weatherObj = data2.child("weather").getValue(WeatherObj.class);
                                } else weatherObj = null;

                                if (data2.child("places").exists() && !data2.child("places").getValue().equals("null")) {
                                    placesObj = data2.child("places").getValue(PlacesObj.class);
                                } else placesObj = null;
                                UserDataObj userDataObj = new UserDataObj(key, timestamp, headphones, activity, weatherObj, placesObj, latLng, speed);
                                userDatas.add(userDataObj);
                            }
                        }
                    }
                }
                createMultMarkers();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Create a marker for every point in the list
    private void createMultMarkers() {
        googleMap.clear(); // Clear previous markers
        Log.d(TAG, "createMultMarkers() - LIST SIZE = " + userDatas.size());
        BitmapDescriptor icon; // = BitmapDescriptorFactory.fromResource(R.drawable.unknown);
        for(int i=0; i< userDatas.size(); i++) {
            LatLng point = userDatas.get(i).latLng; //OXI GETWEATHER , GETPLACES ALLA weather sketo apo to userdatas
            if(point != null && userDatas.get(i).userActivity != null && userDatas.get(i).getWeather() != null && userDatas.get(i).getPlaces() != null) { //Θα πρεπει να γινεται ελεγχος οτι το gps ειναι ενεργοποιημενο για να γινεται εγγραφη στην βαση, προς το παρον μενει ετσι //TODO
                Log.d(TAG, "userDatas.get(i).userActivity ====> " + userDatas.get(i).userActivity);
                markerOptions.position(point);
                markerOptions.title(userDatas.get(i).places.placeName);
                markerOptions.snippet(userDatas.get(i).places.placeAddress);
                //Ανάλογα με την επιλογή απο το μενού επιλέγονται τα κατάλληλα εικονίδια
                if(locationIcon)
                    markerOptions.icon(getRandomIconColor());
                if(weatherIcons)
                    markerOptions.icon(getWeatherIcon(userDatas.get(i).getWeather().condition));
                if(activityIcons)
                    markerOptions.icon(getActivityIcon(userDatas.get(i).userActivity));
                if(speedIcons)
                    markerOptions.icon(getSpeedIcon(userDatas.get(i).speed));
                googleMap.addMarker(markerOptions);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 12));
            }
        }
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(AwarenessActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(AwarenessActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(AwarenessActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    private BitmapDescriptor getWeatherIcon(String weatherCondition) {
        BitmapDescriptor icon;
        switch ( weatherCondition ) {
            case "Clear" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.clearday);
                break;
            }
            case "Cloudy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.cloudy);
                break;
            }
            case "Foggy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.fog);
                break;
            }
            case "Hazy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.haze);
                break;
            }
            case "Icy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.icy);
                break;
            }
            case "Rainy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.rainy);
                break;
            }
            case "Snowy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.snowy);
                break;
            }
            case "Stormy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.stormy);
                break;
            }
            case "Windy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.windy);
                break;
            }
            case "Clear, Cloudy" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.clear_cloudy);
                break;
            }
            default: {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.unknown);
                break;
            }
        }
        return icon;
    }

    private BitmapDescriptor getActivityIcon(String userActivity) {
        BitmapDescriptor icon;
        switch ( userActivity ) {
            case "In Vehicle" : {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.invehicle);
                break;
            }
            case "On Bicycle": {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.bicycle);
                break;
            }
            case "On Foot": {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.onfoot);
                break;
            }
            case "Running": {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.running);
                break;
            }
            case "Still": {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.still);
                break;
            }
            case "Tilting": {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.tilting);
                break;
            }
            case "Walking": {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.walking);
                break;
            }
            default: {
                icon = BitmapDescriptorFactory.fromResource(R.drawable.unknown);
                break;
            }
        }
        return icon;
    }

    private BitmapDescriptor getSpeedIcon(double userSpeed) {
        BitmapDescriptor icon;
        if((0 <= userSpeed) && (userSpeed <= 10) )
            icon = BitmapDescriptorFactory.fromResource(R.drawable.white_circle);
        else if(userSpeed <= 50)
            icon = BitmapDescriptorFactory.fromResource(R.drawable.green_circle);
        else if(userSpeed <= 100)
            icon = BitmapDescriptorFactory.fromResource(R.drawable.orange_circle);
        else if(userSpeed > 100)
            icon = BitmapDescriptorFactory.fromResource(R.drawable.red_circle);
        else
            icon = BitmapDescriptorFactory.fromResource(R.drawable.warning);
        return icon;
    }

    // Generate a random hue color
    private BitmapDescriptor getRandomIconColor() {
        Random rand = new Random();
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        int randomColor = Color.rgb(r, g, b);

        float[] hsv = new float[3];
        Color.colorToHSV(randomColor, hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.calendar_icon, menu);
        inflater.inflate(R.menu.visualisation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.DatePicker: {
                pickDate();
                return true;
            }
        }
        switch ( item.getItemId() ) {
            case R.id.userLocation: {
                activityIcons = false;
                locationIcon = true;
                weatherIcons = false;
                speedIcons = false;
                getUserData();
                return true;
            }
        }
        switch ( item.getItemId() ) {
            case R.id.userSpeed: {
                activityIcons = false;
                locationIcon = false;
                weatherIcons = false;
                speedIcons = true;
                getUserData();
                return true;
            }
        }
        switch ( item.getItemId() ) {
            case R.id.userActivity: {
                activityIcons = true;
                locationIcon = false;
                weatherIcons = false;
                speedIcons = false;
                getUserData();
                return true;
            }
        }
        switch ( item.getItemId() ) {
            case R.id.weather: {
                activityIcons = false;
                locationIcon = false;
                weatherIcons = true;
                speedIcons = false;
                getUserData();
                return true;
            }
        }
        switch ( item.getItemId() ) {
            case R.id.exit: {
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}

    //int CurrentSpeed = (int)Math.round(3.6 * (location.getSpeed())); convert speed from m/s to km/h //TODO