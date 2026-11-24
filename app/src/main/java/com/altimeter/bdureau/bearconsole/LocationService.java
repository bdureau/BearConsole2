package com.altimeter.bdureau.bearconsole;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: LocationService
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Background service providing the position of the phone (fused location provider, every
 *  5 seconds) to the tracking screens, so that the distance and bearing to the rocket can be
 *  displayed. Each new position is broadcast with the action "ACT_LOC".
 *  The location updates are requested only once, and removed when the service is stopped
 *  (onDestroy), so that the GPS of the phone is switched off when no screen uses it.
 * ============================================================================
 */

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService extends Service {
    private static final String TAG = "LocationService";

    /** Interval between two positions of the phone, in milliseconds. */
    private static final long LOCATION_INTERVAL_MS = 5000;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    // True once the location updates have been requested (onStartCommand can be called again)
    private boolean updatesRequested = false;

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

                Intent intent = new Intent("ACT_LOC");
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLongitude());
                Log.d(TAG, "latitude:" + location.getLatitude());
                sendBroadcast(intent);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        requestLocation();
        return super.onStartCommand(intent, flags, startId);
    }

    /** Stops the location updates: the GPS is no longer used once the tracking screen is closed. */
    @Override
    public void onDestroy() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        updatesRequested = false;
        super.onDestroy();
    }

    private void requestLocation() {
        // Several screens may start the service: request the updates only once
        if (updatesRequested) return;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // The tracking screens request the location permission before starting this
            // service: without it no position can be provided
            return;
        }

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
