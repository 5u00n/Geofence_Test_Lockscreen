package com.su00n.geofencingtest;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class LocationService extends Service {

    public static double latitude=0.0,longitude=0.0,altitude=0.0;
    private static final String TAG = "Geofence  SERVICE";


    Context context;
    Intent intent;

    PendingIntent pendingIntent;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                latitude = locationResult.getLastLocation().getLatitude();
                longitude = locationResult.getLastLocation().getLongitude();
                altitude = locationResult.getLastLocation().getAltitude();


                NotificationHelper notificationHelper = new NotificationHelper(context);

                GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

                if (geofencingEvent.hasError()) {
                    Log.d(TAG, "onReceive: Error receiving geofence event...");
                    return;
                }

                List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
                for (Geofence geofence : geofenceList) {
                    Log.d(TAG, "onReceive: " + geofence.getRequestId());
                }
                //Location location = geofencingEvent.getTriggeringLocation();
                int transitionType = geofencingEvent.getGeofenceTransition();
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                switch (transitionType) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        //  Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show();
                        notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_ENTER Service ", "From Service", MapsActivity.class);

                        // Vibrate for 500 milliseconds
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                        break;
                    case Geofence.GEOFENCE_TRANSITION_DWELL:
                        // Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                        notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_DWELL Service", "From Service", MapsActivity.class);
                        long[] pattern = {0, 100, 1000};

                        // The '0' here means to repeat indefinitely
                        // '0' is actually the index at which the pattern keeps repeating from (the start)
                        // To repeat the pattern from any other point, you could increase the index, e.g. '1'
                        v.vibrate(pattern, 0);
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        //Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show();
                        notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_EXIT Service", "From Service", MapsActivity.class);
                        break;
                }
               //Log.d("Location Update", "lat - " + latitude + " : " + "long - " + longitude + " : alt - " + altitude);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startLocationService() {

        context=getApplicationContext();

        String chanelId = "location_notification_service";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity
                    (this, 0, intent, PendingIntent.FLAG_MUTABLE);
        }
        else
        {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }



        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), chanelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(Notification.PRIORITY_MAX);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(chanelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(chanelId, "Location Service ", NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setDescription("This chanel id used by location Service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(Constraints.LOCATION_SERVICE_ID,builder.build());
    }


    private void stopLocationService(){
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            this.intent=intent;
            String action= intent.getAction();
        if(action!=null){
            if(action.equals(Constraints.ACTION_START_LOCATION_SERVICE)){
                startLocationService();
            }
            else if(action.equals(Constraints.ACTION_STOP_LOCATION_SERVICE)){
                stopLocationService();
            }
        }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}