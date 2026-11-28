package com.altimeter.bdureau.bearconsole;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: LocationService
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Service providing the position of the phone (fused location provider, every 5 seconds)
 *  to the telemetry, status and tracking screens, so that the distance and bearing to the
 *  rocket can be displayed. Each new position is broadcast with the action ACTION_LOCATION
 *  ("ACT_LOC"). The broadcast is private to the application: it is sent with the package of
 *  the application and received by receivers registered with RECEIVER_NOT_EXPORTED (see
 *  registerPositionReceiver()), as required since Android 14 for runtime receivers.
 *
 *  Several screens can use the position at the same time (e.g. a tracking map opened from
 *  the status screen): they call acquire() / release() instead of startService() /
 *  stopService(). The service is stopped, and the GPS switched off, only when the last
 *  screen releases it.
 *
 *  The service runs in the foreground (location type, with a notification) so that the
 *  position keeps being updated while the phone is locked during the walk to the rocket:
 *  Android throttles the location of background services after a few minutes.
 * ============================================================================
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {
    private static final String TAG = "LocationService";

    /** Action of the broadcast carrying a new phone position (extras "latitude", "longitude"). */
    public static final String ACTION_LOCATION = "ACT_LOC";

    /** Interval between two positions of the phone, in milliseconds. */
    private static final long LOCATION_INTERVAL_MS = 5000;

    /** Notification channel and id of the foreground notification. */
    private static final String CHANNEL_ID = "rocket_tracking";
    private static final int NOTIFICATION_ID = 4711;

    // Number of screens currently using the position (acquire() / release(), UI thread only)
    private static int users = 0;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    // True once the location updates have been requested (onStartCommand can be called again)
    private boolean updatesRequested = false;
    // True once the service has been moved to the foreground
    private boolean inForeground = false;

    /**
     * Starts the service for one more screen. Every call must be balanced by one call to
     * release(), usually in onDestroy(). Must be called from the UI thread, while the screen
     * is visible and after the location permission has been granted.
     */
    @MainThread
    public static void acquire(@NonNull Context context) {
        users++;
        context.startService(new Intent(context, LocationService.class));
    }

    /**
     * Releases the service for one screen. The service (and the GPS) is stopped only when no
     * other screen uses it any more.
     */
    @MainThread
    public static void release(@NonNull Context context) {
        if (users > 0) users--;
        if (users == 0) {
            context.stopService(new Intent(context, LocationService.class));
        }
    }

    /**
     * Registers a receiver of the phone positions broadcast by this service.
     * The receiver is not exported (ContextCompat.RECEIVER_NOT_EXPORTED): Android 14 (API 34)
     * and later refuse a runtime receiver registered without RECEIVER_EXPORTED or
     * RECEIVER_NOT_EXPORTED for a non-system broadcast, and a private receiver also prevents
     * another application from injecting fake positions. ContextCompat applies the flag on
     * every Android version supported by the application.
     * The caller must unregister it with Context.unregisterReceiver().
     */
    public static void registerPositionReceiver(@NonNull Context context,
                                                @NonNull BroadcastReceiver receiver) {
        ContextCompat.registerReceiver(context, receiver, new IntentFilter(ACTION_LOCATION),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                // The result can be empty: never broadcast a position that does not exist
                if (location == null) return;

                // Explicit package: the broadcast is only delivered inside this application
                Intent intent = new Intent(ACTION_LOCATION);
                intent.setPackage(getPackageName());
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLongitude());
                Log.d(TAG, "latitude:" + location.getLatitude());
                sendBroadcast(intent);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasLocationPermission()) {
            // The screens request the permission before using the service: nothing to provide
            stopSelf();
            return START_NOT_STICKY;
        }
        startInForeground();
        requestLocation();
        // Not restarted by the system after a kill: the screens start it again when needed
        return START_NOT_STICKY;
    }

    /** Stops the location updates: the GPS is no longer used once the last screen is closed. */
    @Override
    public void onDestroy() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        updatesRequested = false;
        if (inForeground) {
            stopForeground(true);
            inForeground = false;
        }
        super.onDestroy();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Moves the service to the foreground with a "location" type, so that the position keeps
     * being updated while the phone is locked. The service is started with startService() by a
     * visible screen, which is what Android requires for a location foreground service. If
     * Android refuses (rare), the service keeps working as a normal background service, as
     * before.
     */
    private void startInForeground() {
        if (inForeground) return;
        try {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            inForeground = true;
        } catch (RuntimeException e) {
            // e.g. ForegroundServiceStartNotAllowedException or SecurityException
            Log.w(TAG, "Cannot start the location service in the foreground", e);
        }
    }

    /** Builds the notification shown while the position of the phone is used. */
    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        getString(R.string.location_channel_name), NotificationManager.IMPORTANCE_LOW);
                manager.createNotificationChannel(channel);
            }
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_location)
                .setContentTitle(getString(R.string.location_notification_title))
                .setContentText(getString(R.string.location_notification_text))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void requestLocation() {
        // Several screens may start the service: request the updates only once
        if (updatesRequested) return;

        // LocationRequest.Builder replaces the deprecated "new LocationRequest()" + setters
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .build();

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            updatesRequested = true;
        } catch (SecurityException e) {
            // Permission revoked between the check and the request
            Log.w(TAG, "Location permission not granted", e);
        }
    }
}
