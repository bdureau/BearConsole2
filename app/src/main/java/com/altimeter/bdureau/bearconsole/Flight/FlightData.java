package com.altimeter.bdureau.bearconsole.Flight;

/*
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
import java.util.Comparator;
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
    // Attitude of the flights recorded with a gyroscope (UltimateAltimeter 0.8):
    // flightName -> 4 series "q0".."q3" (quaternion body -> world, Z up) against the time.
    // Kept apart from the curves: a quaternion component is not a curve to plot.
    private final Map<String, XYSeriesCollection> attitudeMap = new HashMap<>();

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
     * Returns the list of all recorded flight names, sorted by flight number
     * ("Flight 9" < "Flight 10" < "Flight 100"), see {@link #FLIGHT_NAME_ORDER}.
     */
    @NonNull
    public synchronized List<String> getAllFlightNames2() {
        List<String> flightNames = new ArrayList<>(flightsMap.keySet());
        flightNames.sort(FLIGHT_NAME_ORDER);
        return flightNames;
    }

    /**
     * Orders flight names by the number at their end, then alphabetically.
     * A plain alphabetical order puts "Flight 100" before "Flight 11" once there are more
     * than 99 flights; names without a trailing number are kept in alphabetical order.
     */
    public static final Comparator<String> FLIGHT_NAME_ORDER = (a, b) -> {
        long na = trailingNumber(a);
        long nb = trailingNumber(b);
        if (na >= 0 && nb >= 0 && na != nb) {
            return Long.compare(na, nb);
        }
        return a.compareTo(b);
    };

    /** Number at the end of a name ("Flight 012" -> 12), or -1 if there is none. */
    private static long trailingNumber(String name) {
        int end = name.length();
        int start = end;
        while (start > 0 && Character.isDigit(name.charAt(start - 1))) start--;
        if (start == end || end - start > 18) return -1;
        return Long.parseLong(name.substring(start, end));
    }

    /**
     * Removes a flight from the repository (e.g. a flight whose download was cancelled
     * and whose data are therefore incomplete).
     *
     * @return true if the flight existed
     */
    public synchronized boolean removeFlight(@NonNull String flightName) {
        attitudeMap.remove(flightName);
        return flightsMap.remove(flightName) != null;
    }

    /**
     * Clears all flight datasets from memory.
     */
    public synchronized void ClearFlight() {
        flightsMap.clear();
        attitudeMap.clear();
    }

    /**
     * Adds one attitude sample (quaternion w, x, y, z, body -> world) to a flight.
     */
    public synchronized void addAttitude(double timeMs, float q0, float q1, float q2, float q3,
                                         @NonNull String flightName) {
        XYSeriesCollection att = attitudeMap.get(flightName);
        if (att == null) {
            att = new XYSeriesCollection();
            att.addSeries(new XYSeries("q0"));
            att.addSeries(new XYSeries("q1"));
            att.addSeries(new XYSeries("q2"));
            att.addSeries(new XYSeries("q3"));
            attitudeMap.put(flightName, att);
        }
        att.getSeries(0).add(timeMs, q0);
        att.getSeries(1).add(timeMs, q1);
        att.getSeries(2).add(timeMs, q2);
        att.getSeries(3).add(timeMs, q3);
    }

    /** Attitude of the flight (4 series q0..q3), or null if it was not recorded. */
    @Nullable
    public synchronized XYSeriesCollection getAttitude(@NonNull String flightName) {
        return attitudeMap.get(flightName);
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
     * Same as {@link #AddToFlight(float, float, String, int)} with double precision values.
     * Used for the GPS coordinates, which need more than the 7 significant digits of a float.
     */
    public synchronized void AddToFlight(double x, double y, @NonNull String flightName, int serieIndex) {
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
     * Numerically computes vertical velocity (m/s or ft/s) and vertical acceleration (m/s² or ft/s²)
     * from altitude time-series using second-order centered finite differences.
     * Preserves physical velocity sign (positive ascent, negative descent) and eliminates
     * phase lag at peak velocity (burnout) and apogee.
     *
     * Shared by the flight list and the flight recovery screens, so that both compute the
     * curves the same way. The speed is stored in series 3 and the acceleration in series 4.
     *
     * @param flight Unique identifier name of the target flight dataset
     */
    public void computeVelocityAndAcceleration(@NonNull String flight) {
        XYSeriesCollection flightCollection = GetFlightData(flight);
        if (flightCollection == null) return;

        // Same context as createFlight(), so the series name always matches
        XYSeries altSeries = flightCollection.getSeries(context.getString(R.string.curve_altitude));
        if (altSeries == null) return;

        int nbrData = altSeries.getItemCount();
        if (nbrData < 2) return;

        double[] timeSec = new double[nbrData];
        double[] altVal = new double[nbrData];
        double[] speeds = new double[nbrData];

        // Cache time in seconds and altitude values
        for (int i = 0; i < nbrData; i++) {
            timeSec[i] = altSeries.getX(i).doubleValue() / 1000.0; // Convert ms to seconds
            altVal[i] = altSeries.getY(i).doubleValue();
        }

        // --- 1. VELOCITY COMPUTATION (Centered Differences) ---
        for (int i = 0; i < nbrData; i++) {
            double v = 0.0;
            if (i == 0) {
                // Forward difference for the initial sample boundary
                double dt = timeSec[1] - timeSec[0];
                if (dt > 0.0001) v = (altVal[1] - altVal[0]) / dt;
            } else if (i == nbrData - 1) {
                // Backward difference for the final sample boundary
                double dt = timeSec[nbrData - 1] - timeSec[nbrData - 2];
                if (dt > 0.0001) v = (altVal[nbrData - 1] - altVal[nbrData - 2]) / dt;
            } else {
                // Second-order central difference evaluated at the exact midpoint t[i]
                double dt = timeSec[i + 1] - timeSec[i - 1];
                if (dt > 0.0001) {
                    v = (altVal[i + 1] - altVal[i - 1]) / dt;
                } else {
                    v = speeds[i - 1]; // Safeguard against zero dt
                }
            }
            speeds[i] = v;

            // Index 3 corresponds to curve_speed
            AddToFlight((float) altSeries.getX(i).doubleValue(), (float) v, flight, 3);
        }

        // --- 2. VERTICAL ACCELERATION COMPUTATION (Derivative of Velocity) ---
        for (int i = 0; i < nbrData; i++) {
            double a = 0.0;
            if (i == 0) {
                // Forward difference at initial boundary
                double dt = timeSec[1] - timeSec[0];
                if (dt > 0.0001) a = (speeds[1] - speeds[0]) / dt;
            } else if (i == nbrData - 1) {
                // Backward difference at final boundary
                double dt = timeSec[nbrData - 1] - timeSec[nbrData - 2];
                if (dt > 0.0001) a = (speeds[nbrData - 1] - speeds[nbrData - 2]) / dt;
            } else {
                // Centered difference on the derived velocity profile
                double dt = timeSec[i + 1] - timeSec[i - 1];
                if (dt > 0.0001) {
                    a = (speeds[i + 1] - speeds[i - 1]) / dt;
                } else if (i > 0) {
                    a = 0.0;
                }
            }

            // Index 4 corresponds to curve_accel
            AddToFlight((float) altSeries.getX(i).doubleValue(), (float) a, flight, 4);
        }
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
            // Gyroscope rates (dps), firmware 0.8 and later
            collection.addSeries(new XYSeries("gyroX"));
            collection.addSeries(new XYSeries("gyroY"));
            collection.addSeries(new XYSeries("gyroZ"));
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