package com.unipi.mgiavris.awarenessproject;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResult;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.awareness.snapshot.WeatherResult;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.unipi.mgiavris.awarenessproject.AwarenessActivity.getUID;

public class DataCollectionService extends Service {
    // ------ ON/OFF SERVICE WHEN GPS IS ENABLED/DISABLED ------ //TODO
    private static final String TAG = "DataCollectionService";
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private GoogleApiClient mGoogleApiClient;
    private String weatherCondition = null;
    private String currentActivity = null;
    //private boolean hasWeather = false, hasCondition = false;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() " + SystemClock.elapsedRealtime());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() " + SystemClock.elapsedRealtime());
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // If GPS is enabled collect data
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildApiClient();
            String timestamp = getTimestamp();
            headphoneState(timestamp);
            weatherConditions(timestamp);
            myLocation(timestamp);
            myCurrentActivity(timestamp);
            places(timestamp);
            /*while(!hasWeather && !hasCondition) { //error fix it
                Log.d(TAG, "-!-!-!-! inside while");
                if(weatherCondition.equals("Clear, Cloudy")) {
                    pushNotification(R.drawable.bicycle, "EISAI AKINHTOS");
                }
            }*/
        }
        stopSelf();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() " + SystemClock.elapsedRealtime());
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.set(
                alarm.RTC_WAKEUP,
                System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR,
                PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0)
        );

        /*alarm.setRepeating(alarm.RTC_WAKEUP,
                System.currentTimeMillis() + (1000 * 60),
                AlarmManager.INTERVAL_HALF_HOUR,
                PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0)
        );*/
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        Intent intent = new Intent(getApplicationContext(), DataCollectionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(
                alarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR,
                PendingIntent.getService(this, 0, new Intent(this, DataCollectionService.class), 0)
        );
        super.onTaskRemoved(rootIntent);
    }

    //Check headphones State
    private void headphoneState(final String timestamp) {
        Awareness.SnapshotApi.getHeadphoneState(mGoogleApiClient)
                .setResultCallback(new ResultCallback<HeadphoneStateResult>() {
                    @Override
                    public void onResult(@NonNull HeadphoneStateResult headphoneStateResult) {
                        if (headphoneStateResult.getStatus().isSuccess()) {
                            Log.d(TAG, "headphoneState - SUCCESS");
                            HeadphoneState headphoneState = headphoneStateResult.getHeadphoneState();
                            int state = headphoneState.getState();

                            if (state == HeadphoneState.PLUGGED_IN) {
                                databaseReference.child(getUID(getApplicationContext(), "headphoneState()")).child(timestamp).child("headphones").setValue(retrieveHeadphoneString(state));
                            } else if (state == HeadphoneState.UNPLUGGED) {
                                databaseReference.child(getUID(getApplicationContext(), "headphoneState()")).child(timestamp).child("headphones").setValue(retrieveHeadphoneString(state));
                            }
                        } else {
                            Log.d(TAG, "headphoneState - FAILURE");
                            databaseReference.child(getUID(getApplicationContext(), "headphoneState()")).child(timestamp).child("headphones").setValue("null");
                        }
                    }
                });
    }

    //Get Weather Conditions
    private void weatherConditions(final String timestamp) {
        if(checkPermission()) {
            Awareness.SnapshotApi.getWeather(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<WeatherResult>() {
                        @Override
                        public void onResult(@NonNull WeatherResult weatherResult) {
                            if (weatherResult.getStatus().isSuccess()) {
                                Log.d(TAG, "weatherConditions - SUCCESS");
                                Weather weather = weatherResult.getWeather();
                                int[] conditions = weather.getConditions();
                                StringBuilder newString = new StringBuilder();
                                if (conditions.length > 0) {
                                    for (int i = 0; i < conditions.length; i++) {
                                        if (i > 0) {
                                            newString.append(", ");
                                        }
                                        newString.append(retrieveConditionString(conditions[i]));

                                    }
                                }
                                WeatherObj weatherObj = new WeatherObj(
                                        Math.round(weather.getTemperature(Weather.CELSIUS)),
                                        Math.round(weather.getFeelsLikeTemperature(Weather.CELSIUS)),
                                        Math.round(weather.getDewPoint(Weather.CELSIUS)),
                                        weather.getHumidity(),
                                        newString.toString()
                                );
                                databaseReference.child(getUID(getApplicationContext(), "weatherConditions()")).child(timestamp).child("weather").setValue(weatherObj); //.child("Weather")
                                //hasWeather = true;
                                //weatherCondition = weatherObj.getCondition();
                            } else {
                                //Handle Failure
                                Log.d(TAG, "weatherConditions - FAILURE");
                                databaseReference.child(getUID(getApplicationContext(), "weatherConditions()")).child(timestamp).child("weather").setValue("null"); //.child("Weather")

                            }
                        }
                    });
        }
    }

    //Get user's current location
    private void myLocation(final String timestamp) {
        if (checkPermission()) { //Εχει εξασφαλιστει οτι η αδεια έχει δωθεί απο την onCreate αλλα ζηταει οπωσδήποτε να γινει ελεγχος και εδω
            Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<LocationResult>() {
                        @Override
                        public void onResult(@NonNull LocationResult locationResult) {
                            if (locationResult.getStatus().isSuccess()) {
                                Log.d(TAG, "myLocation - SUCCESS");
                                Location location = locationResult.getLocation();
                                Log.d(TAG, "Location Lat/Long: " + location.getLatitude() + " / " + location.getLongitude());
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                databaseReference.child(getUID(getApplicationContext(), "myLocation()")).child(timestamp).child("location").setValue(location);//child("location").setValue(latLng);
                            } else {
                                //Handle Failure
                                Log.d(TAG, "myLocation - FAILURE");
                                databaseReference.child(getUID(getApplicationContext(), "myLocation()")).child(timestamp).child("location").setValue("null");//child("location").setValue(latLng);
                            }
                        }
                    });
        }
    }

    //Get user's current activity
    private void myCurrentActivity(final String timestamp) {
        if (checkPermission()) {
            Awareness.SnapshotApi.getDetectedActivity(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DetectedActivityResult>() {
                        @Override
                        public void onResult(@NonNull DetectedActivityResult detectedActivityResult) {
                            if (detectedActivityResult.getStatus().isSuccess()) {
                                Log.d(TAG, "myCurrentActivity - SUCCESS");
                                ActivityRecognitionResult activityRecognitionResult = detectedActivityResult.getActivityRecognitionResult();
                                databaseReference.child(getUID(getApplicationContext(), "myCurrentActivity")).child(timestamp).child("activity").setValue(getActivityString(activityRecognitionResult.getMostProbableActivity().getType()));
                                Log.d(TAG, "Most Propable Activity : " + getActivityString(activityRecognitionResult.getMostProbableActivity().getType()));
                                //hasCondition = true;
                                //currentActivity = getActivityString(activityRecognitionResult.getMostProbableActivity().getType());
                            } else {
                                Log.d(TAG, "myCurrentActivity - FAILURE");
                                databaseReference.child(getUID(getApplicationContext(), "myCurrentActivity")).child(timestamp).child("activity").setValue("null");
                            }
                        }
                    });
        }
    }

    private void places(final String timestamp) {
        if(checkPermission()) {
            //final Place[] place = {null};
            Awareness.SnapshotApi.getPlaces(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<PlacesResult>() {
                        @Override
                        public void onResult(@NonNull PlacesResult placesResult) {
                            if (placesResult.getStatus().isSuccess()) {
                                Log.d(TAG, "places - SUCCESS");
                                List<PlaceLikelihood> placeLikelihood = placesResult.getPlaceLikelihoods();
                                //StringBuilder places = new StringBuilder();
                                String placesName = null, placesAddress = null;
                                if (placeLikelihood != null) {
                                    for (PlaceLikelihood place : placeLikelihood) {
                                        Log.i(TAG, "Place --- : " + place);

                                        placesName = place.getPlace().getName().toString();/*place.getPlace().getPlaceTypes().toString() + */
                                        placesAddress = place.getPlace().getAddress().toString();
                                    }
                                    PlacesObj places = new PlacesObj(placesName, placesAddress);
                                    databaseReference.child(getUID(getApplicationContext(), "places()")).child(timestamp).child("places").setValue(places);
                                }
                            } else {
                                Log.d(TAG, "places - FAILURE");
                                databaseReference.child(getUID(getApplicationContext(), "places()")).child(timestamp).child("places").setValue("null");

                            }
                        }
                    });
            //databaseReference.child(getUID(getApplicationContext())).child(timestamp).child("places").setValue(place[0]); // OVERFLOW ginotan des to genika
        }

    }

    private void pushNotification(int notification_icon, String contentText) {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "awarenessID";

        if(android.os.Build.VERSION.SDK_INT == 26) {
            CharSequence channelName = "Awareness Channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(notification_icon)
                .setContentTitle("Προσοχή!")
                .setContentText("Ο δρόμος μπορεί να είναι επικίνδυνος!");

        mNotificationManager.notify(100, mBuilder.build());
    }

    //Build GoogleApiClient
    private void buildApiClient() {
        Log.d(TAG, "buildApiClient()");
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect();
    }

    // Check if the permission is granted.
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        if(Build.VERSION.SDK_INT >= 23) {
            return ((ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED));
        } else {
            return true;
        }
    }

    private String getTimestamp() {
        Log.d(TAG, "getTimestamp()");
        Calendar calendar = Calendar.getInstance();
        return (new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(calendar.getTime()));
        /*SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+2:00")); //orizetai os zonh oras ayth ths elladas
       return sdf.format(new Date()); //datetime */
    }

    private String retrieveHeadphoneString(int state) {
        Log.d(TAG, "retrieveHeadphoneString()");
        switch (state) {
            case HeadphoneState.PLUGGED_IN:
                return "Plugged In";
            case HeadphoneState.UNPLUGGED:
                return "Unplugged";
            default:
                return "Unknown";
        }
    }

    private String retrieveConditionString(int condition) {
        Log.d(TAG, "retrieveConditionString");
        switch (condition) {
            case Weather.CONDITION_CLEAR:
                return "Clear";
            case Weather.CONDITION_CLOUDY:
                return "Cloudy";
            case Weather.CONDITION_FOGGY:
                return "Foggy";
            case Weather.CONDITION_HAZY:
                return "Hazy";
            case Weather.CONDITION_ICY:
                return "Icy";
            case Weather.CONDITION_RAINY:
                return "Rainy";
            case Weather.CONDITION_SNOWY:
                return "Snowy";
            case Weather.CONDITION_STORMY:
                return "Stormy";
            case Weather.CONDITION_WINDY:
                return "Windy";
            default:
                return "Unknown";
        }
    }

    private String getActivityString(int activity) {
        Log.d(TAG, "getActivityString");
        switch (activity) {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.WALKING:
                return "Walking";
            default:
                return "Unknown Activity";
        }
    }
}
// https://stackoverflow.com/questions/20673620/how-to-trigger-broadcast-receiver-when-gps-is-turn-on-off //todo
//ΟΤΑΝ ΘΑ ΕΝΕΡΓΟΠΟΙΕΙΤΑΙ ΤΟ GPS ΘΑ ΤΡΕΧΕΙ ΤΟ SERVICE //TODO