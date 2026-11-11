package com.altimeter.bdureau.bearconsole.Flight;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightData
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Data repository holding rocket flight profiles organized by flight name.
 *  Maintains JFreeChart/AFreeChart XYSeriesCollection structures for altitude,
 *  velocity, acceleration, voltages, GPS telemetry, and IMU sensor data.
 *  Optimized for O(1) hash lookups, memory leak prevention, and fast packet insertion.
 * ============================================================================
 */

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.altimeter.bdureau.bearconsole.R;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlightData {
    private static final String TAG = "FlightData";

    // Application context to prevent Activity context leaks
    private final Context context;
    private final String altimeterName;

    // Flight repository: flightName -> XYSeriesCollection
    private final Map<String, XYSeriesCollection> flightsMap;

    public FlightData(@NonNull Context current, @NonNull String name) {
        // Use ApplicationContext to prevent leaking UI components
        this.context = current.getApplicationContext();
        this.altimeterName = name;
        this.flightsMap = new HashMap<>();
    }

    /**
     * Returns the total number of flights stored.
     */
    public synchronized int getNbrOfFlight() {
        return flightsMap.size();
    }

    /**
     * Returns a sorted list of all recorded flight names.
     */
    @NonNull
    public synchronized List<String> getAllFlightNames2() {
        List<String> flightNames = new ArrayList<>(flightsMap.keySet());
        Collections.sort(flightNames);
        return flightNames;
    }

    /**
     * Clears all flight datasets from memory.
     */
    public synchronized void ClearFlight() {
        flightsMap.clear();
    }

    /**
     * Fast O(1) retrieval of flight data series.
     */
    @Nullable
    public synchronized XYSeriesCollection GetFlightData(@NonNull String flightName) {
        return flightsMap.get(flightName);
    }

    /**
     * Fast O(1) check for flight record presence.
     */
    public synchronized boolean FlightExist(@NonNull String flightName) {
        return flightsMap.containsKey(flightName);
    }

    /**
     * Adds an (X, Y) point to the primary series (Index 0: Altitude).
     */
    public void AddToFlight(float x, float y, @NonNull String flightName) {
        AddToFlight(x, y, flightName, 0);
    }

    /**
     * Inserts an (X, Y) telemetry point into the specified data series index.
     * Creates the flight structure automatically if not yet present.
     */
    public synchronized void AddToFlight(float x, float y, @NonNull String flightName, int serieIndex) {
        XYSeriesCollection flightData = flightsMap.get(flightName);

        if (flightData == null) {
            flightData = createFlight(flightName);
            flightsMap.put(flightName, flightData);
        }

        if (serieIndex >= 0 && serieIndex < flightData.getSeriesCount()) {
            flightData.getSeries(serieIndex).add(x, y);
        } else {
            Log.w(TAG, "Invalid series index: " + serieIndex + " for flight: " + flightName);
        }
    }

    /**
     * Manually registers a pre-built dataset for a flight.
     */
    public synchronized void AddFlightData(@NonNull XYSeriesCollection flightData, @NonNull String flightName) {
        flightsMap.put(flightName, flightData);
    }

    /**
     * Initializes the telemetry series schema according to the altimeter model.
     */
    @NonNull
    private XYSeriesCollection createFlight(final String name) {
        final XYSeriesCollection collection = new XYSeriesCollection();

        // 0: Altitude
        collection.addSeries(new XYSeries(context.getString(R.string.curve_altitude)));
        // 1: Temperature
        collection.addSeries(new XYSeries(context.getString(R.string.curve_temperature)));
        // 2: Pressure
        collection.addSeries(new XYSeries(context.getString(R.string.curve_pressure)));
        // 3: Speed (Computed)
        collection.addSeries(new XYSeries(context.getString(R.string.curve_speed)));
        // 4: Acceleration (Computed)
        collection.addSeries(new XYSeries(context.getString(R.string.curve_accel)));

        // Battery voltage channel
        if (altimeterName.equals("AltiGPS") || altimeterName.equals("AltiMultiSTM32") ||
                altimeterName.equals("AltiMultiESP32") || altimeterName.equals("AltiMultiESP32_accel") ||
                altimeterName.equals("AltiMultiESP32_accel_345") || altimeterName.equals("AltiMultiESP32_accel_375") ||
                altimeterName.equals("TTGOBearAltimeter") || altimeterName.equals("TTGOMiniBearAltimeter")) {
            collection.addSeries(new XYSeries("voltage"));
        }

        // GPS Telemetry channels
        if (altimeterName.equals("AltiGPS")) {
            collection.addSeries(new XYSeries(context.getString(R.string.curve_latitude)));
            collection.addSeries(new XYSeries(context.getString(R.string.curve_longitude)));
            collection.addSeries(new XYSeries("gps altitude"));
            collection.addSeries(new XYSeries("satelites"));
            collection.addSeries(new XYSeries("gps speed"));
            collection.addSeries(new XYSeries("sea altitude"));
        }

        // Fixed duplicate naming for UltimateAltimeter channels
        if (altimeterName.equals("UltimateAltimeter")) {
            collection.addSeries(new XYSeries("accelX"));
            collection.addSeries(new XYSeries("accelY"));
            collection.addSeries(new XYSeries("accelZ"));
        }

        if (altimeterName.equals("TTGOBearAltimeter") || altimeterName.equals("TTGOMiniBearAltimeter")) {
            collection.addSeries(new XYSeries("accel345x"));
            collection.addSeries(new XYSeries("accel345y"));
            collection.addSeries(new XYSeries("accel345z"));
        }

        // Dual accelerometer variants (ADXL375 + ADXL345)
        if (altimeterName.equals("AltiMultiESP32_accel") ||
                altimeterName.equals("AltiMultiESP32_accel_345") ||
                altimeterName.equals("AltiMultiESP32_accel_375")) {
            collection.addSeries(new XYSeries("accel375x"));
            collection.addSeries(new XYSeries("accel375y"));
            collection.addSeries(new XYSeries("accel375z"));
            collection.addSeries(new XYSeries("accel345x"));
            collection.addSeries(new XYSeries("accel345y"));
            collection.addSeries(new XYSeries("accel345z"));
        }

        return collection;
    }
}