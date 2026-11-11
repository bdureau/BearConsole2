package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightViewInfoFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Displays comprehensive post-flight summary analytics (flight duration, apogee,
 *  time to apogee, burn time, max velocity, drogue descent rate, main deployment
 *  altitude, landing speed, acceleration in Gs and GPS ground distance).
 *  The statistics derived from the altitude curve are computed by the nested
 *  FlightStatsCalculator class (pure Java, unit-testable) using least-squares fits
 *  over several samples rather than single noisy samples of the speed curve.
 *  Includes asynchronous background CSV flight export and ZIP sharing compatible
 *  with modern scoped storage.
 * ============================================================================
 */

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.LocationUtils;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FlightViewInfoFragment extends Fragment {
    private static final String TAG = "FlightViewInfoFragment";

    private FlightData myflight;
    private XYSeriesCollection allFlightData;
    private String[] units = null;
    private String flightName = null;
    private ConsoleApplication myBT;

    private TextView nbrOfSamplesValue, flightNbrValue;
    private TextView apogeeAltitudeValue, flightDurationValue, burnTimeValue, maxVelociyValue, maxAccelerationValue;
    private TextView timeToApogeeValue, mainAltitudeValue, maxDescentValue, landingSpeedValue, flightDistanceValue;
    private Button buttonExportToCsv, butExportAndShare;

    private int numberOfCurves = 0;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Default zero-argument constructor required for Fragment state restoration.
     */
    public FlightViewInfoFragment() {
        super();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // After a re-creation by the framework the dependencies were not given by a
        // constructor: the application object is always available from the context
        if (myBT == null) {
            myBT = (ConsoleApplication) context.getApplicationContext();
        }
    }

    /**
     * After a re-creation by the framework the flight data were not given by a constructor:
     * they are taken again from the host FlightViewTabActivity. Called from onCreateView(),
     * when the activity has finished its own onCreate().
     */
    private void restoreFromHost() {
        if (!(getActivity() instanceof FlightViewTabActivity)) return;
        FlightViewTabActivity host = (FlightViewTabActivity) getActivity();
        if (myflight == null) myflight = host.getFlightRepository();
        if (allFlightData == null) allFlightData = host.getAllFlightData();
        if (units == null) units = host.getUnits();
        if (flightName == null) flightName = host.getFlightName();
    }

    public FlightViewInfoFragment(FlightData data,
                                  XYSeriesCollection data2,
                                  ConsoleApplication bt,
                                  String[] pUnits,
                                  String pFlightName) {
        this.myflight = data;
        this.allFlightData = data2;
        this.units = pUnits;
        this.flightName = pFlightName;
        this.myBT = bt;
    }

    private void msg(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        restoreFromHost();
        View view = inflater.inflate(R.layout.fragment_flight_info, container, false);

        buttonExportToCsv = view.findViewById(R.id.butExportToCsv);
        butExportAndShare = view.findViewById(R.id.butExportAndShare);
        apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
        flightDurationValue = view.findViewById(R.id.flightDurationValue);
        burnTimeValue = view.findViewById(R.id.burnTimeValue);
        maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
        maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);
        timeToApogeeValue = view.findViewById(R.id.timeToApogeeValue);
        mainAltitudeValue = view.findViewById(R.id.mainAltitudeValue);
        maxDescentValue = view.findViewById(R.id.maxDescentValue);
        landingSpeedValue = view.findViewById(R.id.landingSpeedValue);
        nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
        flightNbrValue = view.findViewById(R.id.flightNbrValue);
        flightDistanceValue = view.findViewById(R.id.flightDistanceValue);

        if (myBT == null && getActivity() != null) {
            myBT = (ConsoleApplication) getActivity().getApplication();
        }

        if (myBT != null && myBT.getAltiConfigData() != null) {
            if (!myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                flightDistanceValue.setVisibility(View.INVISIBLE);
            }
            determineNumberOfCurves();
        }

        populateFlightStatistics();

        butExportAndShare.setOnClickListener(v -> exportAndShareZipAsync());
        buttonExportToCsv.setOnClickListener(v -> exportCsvAsync());

        return view;
    }

    private void determineNumberOfCurves() {
        String altiName = myBT.getAltiConfigData().getAltimeterName();
        if (altiName.equals("AltiGPS") || altiName.equals("AltiMultiESP32_accel")) {
            numberOfCurves = 8;
        } else if (altiName.equals("AltiMultiSTM32") || altiName.equals("AltiMultiESP32")) {
            numberOfCurves = 6;
        } else if (altiName.equals("AltiServo") || altiName.equals("AltiDuo") ||
                altiName.equals("AltiMultiV2") || altiName.equals("AltiMulti")) {
            numberOfCurves = 5;
        } else {
            numberOfCurves = allFlightData != null ? allFlightData.getSeriesCount() : 0;
        }
    }

    /**
     * Computes the flight statistics from the altitude curve and displays them.
     * Every value except the acceleration and the GPS distance comes from the nested
     * {@link FlightStatsCalculator}, which uses least-squares fits over several samples
     * instead of single noisy samples of the speed curve.
     */
    private void populateFlightStatistics() {
        if (myflight == null || flightName == null || myBT == null) {
            return;
        }

        XYSeriesCollection flightData = myflight.GetFlightData(flightName);
        if (flightData == null || flightData.getSeriesCount() == 0) {
            return;
        }

        XYSeries altitudeSeries = flightData.getSeries(0);
        int nbrData = altitudeSeries.getItemCount();

        boolean isMeters = (myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS);
        String unitsLabel = myBT.getAppConf().getUnitsValue();
        String speedUnit = isMeters ? "m/s" : "ft/s";
        String secsUnit = getString(R.string.unit_secs);

        flightNbrValue.setText(String.valueOf(flightName));
        nbrOfSamplesValue.setText(String.valueOf(nbrData));

        if (nbrData == 0) {
            return;
        }

        // Altitude curve as arrays for the calculator
        double[] timeMs = new double[nbrData];
        double[] altitude = new double[nbrData];
        for (int i = 0; i < nbrData; i++) {
            timeMs[i] = altitudeSeries.getX(i).doubleValue();
            altitude[i] = altitudeSeries.getY(i).doubleValue();
        }

        FlightStatsCalculator.FlightStats stats =
                FlightStatsCalculator.compute(timeMs, altitude, isMeters);

        Log.d(TAG, String.format(Locale.US,
                "liftoff=%d apogee=%d touchdown=%d (recorded=%b) ground=%.1f",
                stats.liftoffIndex, stats.apogeeIndex, stats.touchdownIndex,
                stats.touchdownRecorded, stats.groundAltitude));

        // Flight duration (liftoff to touchdown; "~" if the recording stopped before touchdown)
        String duration = formatDuration(stats.flightDurationSec);
        flightDurationValue.setText(stats.flightDurationEstimated ? "~" + duration : duration);

        // Apogee and time to apogee (from liftoff)
        apogeeAltitudeValue.setText(format("%.0f " + unitsLabel, stats.apogeeAltitude));
        timeToApogeeValue.setText(format("%.2f " + secsUnit, stats.timeToApogeeSec));

        // Max velocity and burn time (peak of the smoothed ascent speed)
        maxVelociyValue.setText(format("%.0f " + speedUnit, stats.maxVelocity));
        burnTimeValue.setText(format("%.2f " + secsUnit, stats.burnoutTimeSec));

        // Descent rate under drogue (or under the single parachute)
        maxDescentValue.setText(format("%.1f " + speedUnit, stats.maxDescentRate));

        // Main parachute deployment altitude (break in the descent curve)
        mainAltitudeValue.setText(format("%.0f " + unitsLabel, stats.mainDeployAltitude));
        if (!Double.isNaN(stats.mainDeployAltitude)) {
            Log.d(TAG, String.format(Locale.US, "Main deploy: t=%.2fs alt=%.1f main rate=%.2f",
                    stats.mainDeployTimeSec, stats.mainDeployAltitude, stats.mainDescentRate));
        }

        // Landing speed (regression over the last 30 m / 100 ft of the descent)
        landingSpeedValue.setText(format("%.1f " + speedUnit, stats.landingSpeed));

        // Maximum acceleration in G. The curve is in altitude units per s^2: in feet it must
        // be converted to meters (x 0.3048) before dividing by g.
        XYSeries accel = (allFlightData != null) ? allFlightData.getSeries(getString(R.string.curve_accel)) : null;
        if (accel != null && accel.getItemCount() > 0) {
            double maxAccelMs2 = isMeters ? accel.getMaxY() : accel.getMaxY() * 0.3048;
            maxAccelerationValue.setText(String.format(Locale.US, "%.1f G", maxAccelMs2 / 9.80665));
        } else {
            maxAccelerationValue.setText(R.string.not_available);
        }

        // GPS surface distance
        if (myBT.getAltiConfigData() != null
                && "AltiGPS".equals(myBT.getAltiConfigData().getAltimeterName())
                && flightData.getSeriesCount() > 7) {
            XYSeries latSeries = flightData.getSeries(6);
            XYSeries lonSeries = flightData.getSeries(7);
            int count = Math.min(latSeries.getItemCount(), lonSeries.getItemCount());
            List<GeoPoint> points = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                double lat = latSeries.getY(i).doubleValue();
                double lon = lonSeries.getY(i).doubleValue();
                // 0,0 means "no GPS fix". Negative values are valid (south / west hemispheres).
                boolean noFix = (lat == 0.0 && lon == 0.0);
                if (!noFix && Math.abs(lat) <= 90.0 && Math.abs(lon) <= 180.0) {
                    points.add(new GeoPoint(lat, lon));
                }
            }

            if (!points.isEmpty()) {
                GeoPoint[] coord = points.toArray(new GeoPoint[0]);
                flightDistanceValue.setText(String.format(Locale.US, "%.2f %s",
                        LocationUtils.distanceBetweenCoordinates(coord), unitsLabel));
            }
        }
    }

    /** Formats a value, or returns the localized "N/A" text if it could not be determined (NaN). */
    private String format(String pattern, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return getString(R.string.not_available);
        return String.format(Locale.US, pattern, value);
    }

    /** Formats a duration in seconds, or in minutes above one minute. */
    private String formatDuration(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds)) return getString(R.string.not_available);
        if (seconds < 60.0) return String.format(Locale.US, "%.2f %s", seconds, getString(R.string.unit_secs));
        return String.format(Locale.US, "%.2f %s", seconds / 60.0, getString(R.string.unit_minutes));
    }

    private File getFlightExportDirectory() {
        Context context = requireContext();
        File exportDir = new File(context.getExternalFilesDir(null), "BearConsoleFlights");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        return exportDir;
    }

    private void exportCsvAsync() {
        ioExecutor.execute(() -> {
            StringBuilder savedCurves = new StringBuilder();
            boolean allSaved = true;

            for (int j = 0; j < numberOfCurves; j++) {
                String fileName = writeCurveToCsv(j, allFlightData);
                if (fileName != null) {
                    savedCurves.append(fileName).append("\n");
                } else {
                    allSaved = false;
                }
            }

            final boolean success = allSaved;
            final String filesSummary = savedCurves.toString();

            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (success) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.save_curves_title))
                            .setMessage(getString(R.string.save_curve_msg) + "\n" + filesSummary)
                            .setCancelable(false)
                            .setPositiveButton(R.string.save_curve_ok, (dialog, id) -> dialog.dismiss())
                            .show();
                    msg(getString(R.string.curves_saved_msg));
                } else {
                    msg(getString(R.string.flight_export_failed));
                }
            });
        });
    }

    private void exportAndShareZipAsync() {
        ioExecutor.execute(() -> {
            File exportDir = getFlightExportDirectory();
            File zipFile = new File(exportDir, "flightData.zip");

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(bos)) {

                byte[] buffer = new byte[8192];
                for (int j = 0; j < numberOfCurves; j++) {
                    String fileName = writeCurveToCsv(j, allFlightData);
                    if (fileName == null) continue;

                    File csvFile = new File(exportDir, fileName);
                    if (!csvFile.exists()) continue;

                    try (FileInputStream fis = new FileInputStream(csvFile)) {
                        ZipEntry ze = new ZipEntry(fileName);
                        zos.putNextEntry(ze);
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }

                mainHandler.post(() -> {
                    if (isAdded()) {
                        shareFile(zipFile);
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Error generating flight ZIP archive", e);
                mainHandler.post(() -> {
                    if (isAdded()) msg(getString(R.string.flight_zip_failed));
                });
            }
        });
    }

    private String writeCurveToCsv(int curveIndex, XYSeriesCollection data) {
        if (data == null || curveIndex >= data.getSeriesCount()) {
            return null;
        }

        String valHeader;
        switch (curveIndex) {
            case 0: valHeader = getString(R.string.curve_altitude); break;
            case 1: valHeader = getString(R.string.curve_temperature); break;
            case 2: valHeader = getString(R.string.curve_pressure); break;
            case 3: valHeader = getString(R.string.curve_speed); break;
            case 4: valHeader = getString(R.string.curve_accel); break;
            case 5: valHeader = "voltage"; break;
            case 6: valHeader = getString(R.string.curve_latitude); break;
            case 7: valHeader = getString(R.string.curve_longitude); break;
            default: valHeader = "curve_" + curveIndex; break;
        }

        String unit = (units != null && curveIndex < units.length) ? units[curveIndex] : "";
        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_HH-mm-ss", Locale.US);
        String dateSuffix = sdf.format(System.currentTimeMillis());
        String curveKey = data.getSeries(curveIndex).getKey().toString();
        String fileName = flightName + "-" + curveKey + dateSuffix + ".csv";

        File file = new File(getFlightExportDirectory(), fileName);

        XYSeries series = data.getSeries(curveIndex);
        int count = series.getItemCount();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file), 16384)) {
            writer.write("time(ms)," + valHeader + " " + unit + "\n");
            for (int i = 0; i < count; i++) {
                writer.write(series.getX(i).doubleValue() + "," + series.getY(i).doubleValue() + "\n");
            }
            return fileName;
        } catch (IOException e) {
            Log.e(TAG, "Failed writing CSV: " + fileName, e);
            return null;
        }
    }

    private void shareFile(File file) {
        Context context = getContext();
        if (context == null) return;

        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.bearconsole_has_shared2));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_with5)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, getString(R.string.no_app_available), Toast.LENGTH_SHORT).show();
        }
    }

    public int searchX(XYSeries series, double searchVal) {
        int count = series.getItemCount();
        for (int i = 1; i < count; i++) {
            if (searchVal >= series.getY(i - 1).doubleValue() && searchVal <= series.getY(i).doubleValue()) {
                return i;
            }
        }
        return -1;
    }

    public int searchXBack(XYSeries series, double searchVal) {
        int count = series.getItemCount();
        for (int i = count - 1; i >= 1; i--) {
            if (searchVal <= series.getY(i - 1).doubleValue() && searchVal >= series.getY(i).doubleValue()) {
                return i;
            }
        }
        return -1;
    }

    public int searchY(XYSeries series, double searchVal) {
        int count = series.getItemCount();
        for (int i = 1; i < count; i++) {
            if (searchVal >= series.getX(i - 1).doubleValue() && searchVal <= series.getX(i).doubleValue()) {
                return i;
            }
        }
        return -1;
    }

    public int searchYBack(XYSeries series, double searchVal) {
        int count = series.getItemCount();
        for (int i = count - 1; i >= 1; i--) {
            if (searchVal >= series.getX(i - 1).doubleValue() && searchVal <= series.getX(i).doubleValue()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    // ---- FlightStatsCalculator (begin) ----
    /**
     * Computes the post-flight statistics of a rocket flight from its altitude curve.
     *
     * Pure Java (no Android dependency): it can be copied out and unit tested on a desktop JVM.
     *
     * Why everything is derived from the altitude curve:
     *  the barometric altitude is the only clean measurement. A speed obtained by differentiating
     *  it sample by sample is very noisy, and taking the maximum of a noisy signal always
     *  over-estimates it. So instead of picking single extreme samples, every value below is
     *  obtained from least-squares straight lines fitted over several samples:
     *
     *  - Ground reference: the altitudes are relative to the launch pad, so the ground is 0
     *    (or the pre-liftoff level if the recording starts below 0). The last recorded sample is
     *    NOT used, because the recording often stops a few meters above the ground.
     *  - Liftoff: start of the climb (last point within 3 m / 10 ft of the ground before the
     *    rocket is clearly flying). It is the origin of every duration.
     *  - Apogee altitude: maximum after a 3-point median filter (removes isolated spikes).
     *    Apogee instant: smoothed vertical speed crossing zero (the top of the curve is flat).
     *  - Max velocity / burnout: vertical speed from sliding linear regressions, peak searched
     *    between liftoff and apogee only. Burnout = time of max velocity.
     *  - Descent: two-segment linear regression on the descent (drogue then main). Gives the
     *    drogue descent rate, the main descent rate and the main deployment altitude at the
     *    intersection of the two lines. With a single parachute, one line is fitted instead.
     *  - Landing speed: linear regression over the last 30 m (100 ft) of the descent.
     *  - Flight duration: liftoff to touchdown. If the recording stops before touchdown, the
     *    remaining time is estimated from the landing speed (flagged as estimated).
     *
     * All rates are positive values, expressed in altitude units per second.
     */
    static final class FlightStatsCalculator {

        /** Result of the computation. Values that cannot be determined are Double.NaN. */
        public static final class FlightStats {
            public double groundAltitude = Double.NaN;
            public int liftoffIndex = -1;
            public int apogeeIndex = -1;
            public int touchdownIndex = -1;
            /** false if the recording stopped before the rocket reached the ground. */
            public boolean touchdownRecorded = false;

            public double flightDurationSec = Double.NaN;
            /** true if flightDurationSec includes an estimated part (recording stopped early). */
            public boolean flightDurationEstimated = false;
            public double apogeeAltitude = Double.NaN;
            public double timeToApogeeSec = Double.NaN;
            public double maxVelocity = Double.NaN;
            public double burnoutTimeSec = Double.NaN;
            /** Descent rate under drogue (or under the single parachute if there is no main). */
            public double maxDescentRate = Double.NaN;
            public double mainDescentRate = Double.NaN;
            public double mainDeployAltitude = Double.NaN;
            /** Main deployment time, counted from liftoff. */
            public double mainDeployTimeSec = Double.NaN;
            public double landingSpeed = Double.NaN;
        }

        // Thresholds expressed in meters; converted when the altitudes are in feet
        private static final double LIFTOFF_THRESHOLD_M = 3.0;
        private static final double GROUND_MARGIN_M = 3.0;
        private static final double LANDING_WINDOW_M = 30.0;
        private static final double METERS_TO_FEET = 3.28084;

        /** Number of first samples used to estimate the pre-liftoff level. */
        private static final int GROUND_SAMPLES = 5;
        /** Half width of the sliding window used to compute the reported max velocity. */
        private static final double SPEED_HALF_WINDOW_SEC = 0.25;
        /** Wider window used to locate the burnout and apogee instants (less sensitive to noise). */
        private static final double TIMING_HALF_WINDOW_SEC = 0.5;
        /** Time skipped after apogee (rounded top of the curve, rocket turning over). */
        private static final double POST_APOGEE_SKIP_SEC = 1.0;
        /** Minimum number of samples on each side of the main deployment breakpoint. */
        private static final int MIN_SEGMENT_SAMPLES = 8;
        /** The main descent rate must be at most this fraction of the drogue descent rate. */
        private static final double MAX_MAIN_TO_DROGUE_RATE_RATIO = 0.75;
        /** Minimum data required for the landing speed regression. */
        private static final int MIN_LANDING_SAMPLES = 5;
        private static final double MIN_LANDING_DURATION_SEC = 1.0;

        private final double[] t;   // seconds since the first sample
        private final double[] y;   // altitude
        private final int n;
        private final double liftoffThreshold;
        private final double groundMargin;
        private final double landingWindow;

        // Prefix sums of t, y, t^2, t*y, y^2 (index i = sum of samples [0, i))
        private final double[] sT, sY, sTT, sTY, sYY;


        private FlightStatsCalculator(double[] timeMs, double[] altitude, boolean isMeters) {
            n = Math.min(timeMs.length, altitude.length);
            t = new double[n];
            y = new double[n];
            for (int i = 0; i < n; i++) {
                t[i] = (timeMs[i] - timeMs[0]) / 1000.0;
                y[i] = altitude[i];
            }
            double scale = isMeters ? 1.0 : METERS_TO_FEET;
            liftoffThreshold = LIFTOFF_THRESHOLD_M * scale;
            groundMargin = GROUND_MARGIN_M * scale;
            landingWindow = LANDING_WINDOW_M * scale;

            sT = new double[n + 1];
            sY = new double[n + 1];
            sTT = new double[n + 1];
            sTY = new double[n + 1];
            sYY = new double[n + 1];
            for (int i = 0; i < n; i++) {
                sT[i + 1] = sT[i] + t[i];
                sY[i + 1] = sY[i] + y[i];
                sTT[i + 1] = sTT[i] + t[i] * t[i];
                sTY[i + 1] = sTY[i] + t[i] * y[i];
                sYY[i + 1] = sYY[i] + y[i] * y[i];
            }
        }

        /**
         * Computes the flight statistics.
         *
         * @param timeMs   sample times in milliseconds (increasing)
         * @param altitude altitudes relative to the launch pad, in meters or feet
         * @param isMeters true if the altitudes are in meters, false if in feet
         * @return the statistics (never null; undetermined values are NaN)
         */
        public static FlightStats compute(double[] timeMs, double[] altitude, boolean isMeters) {
            FlightStats s = new FlightStats();
            if (timeMs == null || altitude == null || Math.min(timeMs.length, altitude.length) < 3) {
                return s;
            }
            new FlightStatsCalculator(timeMs, altitude, isMeters).fill(s);
            return s;
        }

        private void fill(FlightStats s) {
            // Ground and liftoff
            s.groundAltitude = Math.min(0.0, medianOfFirstSamples(GROUND_SAMPLES));
            s.liftoffIndex = findLiftoffIndex(s.groundAltitude);
            double liftoffT = t[s.liftoffIndex];

            // Apogee altitude: maximum after a 3-point median filter (ignores isolated spikes)
            int highestIndex = s.liftoffIndex;
            double best = median3(s.liftoffIndex);
            for (int i = s.liftoffIndex + 1; i < n; i++) {
                double v = median3(i);
                if (v > best) {
                    best = v;
                    highestIndex = i;
                }
            }
            s.apogeeAltitude = best;

            // Burnout (max velocity) and apogee instant (speed crossing zero), both located
            // on the smoothed speed: the top of the curve is flat, so the highest sample alone
            // can be off by more than a second.
            s.apogeeIndex = computeAscent(s, liftoffT, highestIndex);
            s.timeToApogeeSec = t[s.apogeeIndex] - liftoffT;

            // Touchdown: first point back near the ground after apogee (median filtered so that
            // a single noisy sample does not end the flight early)
            s.touchdownIndex = n - 1;
            for (int i = s.apogeeIndex + 1; i < n; i++) {
                if (median3(i) <= s.groundAltitude + groundMargin) {
                    s.touchdownIndex = i;
                    s.touchdownRecorded = true;
                    break;
                }
            }

            int descentEnd = computeDescent(s, liftoffT);
            computeLandingSpeed(s, descentEnd);
            computeDuration(s, liftoffT);
        }

        /**
         * Max velocity, burnout time and apogee instant.
         *  - The burnout instant is the peak of the speed smoothed over +/-0.5 s, searched between
         *    liftoff and the highest sample. The reported max velocity is the +/-0.25 s speed at
         *    that instant (taking a maximum of the less smoothed speed would over-estimate it).
         *  - The apogee instant is the first point after burnout where the smoothed speed is <= 0.
         *
         * @return index of the apogee instant
         */
        private int computeAscent(FlightStats s, double liftoffT, int highestIndex) {
            if (highestIndex <= s.liftoffIndex) return highestIndex;

            double maxV = Double.NEGATIVE_INFINITY;
            int burnoutIdx = -1;
            for (int i = s.liftoffIndex; i <= highestIndex; i++) {
                double v = verticalSpeedAt(i, TIMING_HALF_WINDOW_SEC);
                if (!Double.isNaN(v) && v > maxV) {
                    maxV = v;
                    burnoutIdx = i;
                }
            }
            if (burnoutIdx < 0 || maxV <= 0) return highestIndex;

            // Refine: the heavily smoothed peak lags the real one, so take the peak of the less
            // smoothed speed, but only within +/-0.5 s of it (a bounded search keeps noise from
            // inflating the value or moving the instant far away).
            int refinedIdx = burnoutIdx;
            double refinedV = verticalSpeedAt(burnoutIdx, SPEED_HALF_WINDOW_SEC);
            for (int i = burnoutIdx; i >= s.liftoffIndex && t[burnoutIdx] - t[i] <= TIMING_HALF_WINDOW_SEC; i--) {
                double v = verticalSpeedAt(i, SPEED_HALF_WINDOW_SEC);
                if (v > refinedV) { refinedV = v; refinedIdx = i; }
            }
            for (int i = burnoutIdx + 1; i <= highestIndex && t[i] - t[burnoutIdx] <= TIMING_HALF_WINDOW_SEC; i++) {
                double v = verticalSpeedAt(i, SPEED_HALF_WINDOW_SEC);
                if (v > refinedV) { refinedV = v; refinedIdx = i; }
            }
            s.maxVelocity = Double.isNaN(refinedV) ? maxV : refinedV;
            s.burnoutTimeSec = t[refinedIdx] - liftoffT;

            // Apogee: smoothed speed crossing zero, searched up to 3 s past the highest sample
            for (int i = burnoutIdx; i < n && t[i] <= t[highestIndex] + 3.0; i++) {
                if (verticalSpeedAt(i, TIMING_HALF_WINDOW_SEC) <= 0) return i;
            }
            return highestIndex;
        }

        /**
         * Descent rates and main deployment, by two-segment linear regression.
         *
         * @return index of the last descent sample used (ground tail excluded)
         */
        private int computeDescent(FlightStats s, double liftoffT) {
            // Start: skip the rounded top of the curve
            int start = s.apogeeIndex;
            while (start < n - 1 && t[start] < t[s.apogeeIndex] + POST_APOGEE_SKIP_SEC) start++;

            // End: exclude the samples lying on the ground
            int end = s.touchdownIndex;
            while (end > start && median3(end) <= s.groundAltitude + groundMargin) end--;

            int count = end - start + 1;
            if (count < 3) return end;

            // One straight line over the whole descent (used if no main deployment is found)
            double[] single = fitLine(start, end + 1);
            double singleRate = (single != null && single[0] < 0) ? -single[0] : Double.NaN;

            if (count >= 2 * MIN_SEGMENT_SAMPLES) {
                double bestError = Double.MAX_VALUE;
                double[] bestDrogue = null, bestMain = null;
                int bestK = -1;
                // k = first sample of the main segment; it is shared by both segments
                for (int k = start + MIN_SEGMENT_SAMPLES; k <= end + 1 - MIN_SEGMENT_SAMPLES; k++) {
                    double[] drogue = fitLine(start, k + 1);
                    double[] main = fitLine(k, end + 1);
                    if (drogue == null || main == null) continue;
                    double error = drogue[2] + main[2];
                    if (error < bestError) {
                        bestError = error;
                        bestDrogue = drogue;
                        bestMain = main;
                        bestK = k;
                    }
                }

                if (bestK >= 0 && bestDrogue[0] < 0 && bestMain[0] < 0
                        && Math.abs(bestMain[0]) <= MAX_MAIN_TO_DROGUE_RATE_RATIO * Math.abs(bestDrogue[0])) {
                    // Deployment at the intersection of the two lines (robust to noise)
                    double tDeploy = (bestMain[1] - bestDrogue[1]) / (bestDrogue[0] - bestMain[0]);
                    if (!(tDeploy > t[start] && tDeploy < t[end])) tDeploy = t[bestK];
                    double altDeploy = bestDrogue[0] * tDeploy + bestDrogue[1];

                    if (altDeploy > s.groundAltitude + groundMargin && altDeploy < s.apogeeAltitude) {
                        s.maxDescentRate = -bestDrogue[0];
                        s.mainDescentRate = -bestMain[0];
                        s.mainDeployAltitude = altDeploy;
                        s.mainDeployTimeSec = tDeploy - liftoffT;
                        return end;
                    }
                }
            }

            // No distinct main deployment: single descent rate
            s.maxDescentRate = singleRate;
            return end;
        }

        /** Landing speed: linear regression over the last part of the descent. */
        private void computeLandingSpeed(FlightStats s, int descentEnd) {
            int from = -1;
            for (int i = s.apogeeIndex + 1; i <= descentEnd; i++) {
                if (y[i] <= s.groundAltitude + landingWindow) {
                    from = i;
                    break;
                }
            }
            if (from >= 0 && descentEnd - from + 1 >= MIN_LANDING_SAMPLES
                    && t[descentEnd] - t[from] >= MIN_LANDING_DURATION_SEC) {
                double[] line = fitLine(from, descentEnd + 1);
                if (line != null && line[0] < 0) {
                    s.landingSpeed = -line[0];
                    return;
                }
            }
            // Not enough data close to the ground: use the last descent rate found
            s.landingSpeed = !Double.isNaN(s.mainDescentRate) ? s.mainDescentRate : s.maxDescentRate;
        }

        /** Flight duration from liftoff to touchdown (estimated if the recording stopped early). */
        private void computeDuration(FlightStats s, double liftoffT) {
            double duration = t[s.touchdownIndex] - liftoffT;
            if (!s.touchdownRecorded && s.apogeeIndex < n - 1
                    && !Double.isNaN(s.landingSpeed) && s.landingSpeed > 0) {
                double remainingAlt = y[n - 1] - s.groundAltitude;
                if (remainingAlt > 0) {
                    duration += remainingAlt / s.landingSpeed;
                    s.flightDurationEstimated = true;
                }
            }
            s.flightDurationSec = Math.max(0.0, duration);
        }

        // ---------------------------------------------------------------------------------------
        // Helpers
        // ---------------------------------------------------------------------------------------

        private double medianOfFirstSamples(int count) {
            int c = Math.min(count, n);
            double[] v = new double[c];
            System.arraycopy(y, 0, v, 0, c);
            Arrays.sort(v);
            return (c % 2 == 1) ? v[c / 2] : (v[c / 2 - 1] + v[c / 2]) / 2.0;
        }

        /**
         * Liftoff = start of the climb: first find a point clearly in flight (10 m / 30 ft above
         * the ground, median filtered), then walk back until the altitude is back near the pad.
         * A noisy sample on the pad therefore cannot trigger an early liftoff.
         * If the recording starts in flight, liftoff is the first sample.
         */
        private int findLiftoffIndex(double ground) {
            double clearlyFlying = ground + 10.0 * (liftoffThreshold / LIFTOFF_THRESHOLD_M);
            int i = 0;
            while (i < n && median3(i) <= clearlyFlying) i++;
            if (i >= n) return 0;
            while (i > 0 && median3(i - 1) > ground + liftoffThreshold) i--;
            return (i > 0) ? i - 1 : 0;
        }

        /** Median of samples i-1, i, i+1 (raw value at both ends of the curve). */
        private double median3(int i) {
            if (i <= 0 || i >= n - 1) return y[i];
            double a = y[i - 1], b = y[i], c = y[i + 1];
            return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
        }

        /** Vertical speed at sample i: slope of a linear regression over +/- halfWindowSec. */
        private double verticalSpeedAt(int i, double halfWindowSec) {
            int lo = i, hi = i;
            while (lo > 0 && (t[i] - t[lo - 1] <= halfWindowSec || i - lo < 2)) lo--;
            while (hi < n - 1 && (t[hi + 1] - t[i] <= halfWindowSec || hi - i < 2)) hi++;
            double[] line = fitLine(lo, hi + 1);
            return line == null ? Double.NaN : line[0];
        }

        /**
         * Least-squares straight line y = a * t + b over samples [from, to).
         *
         * @return {a, b, sumOfSquaredErrors}, or null if the segment is degenerate
         */
        private double[] fitLine(int from, int to) {
            int count = to - from;
            if (count < 2) return null;

            double st = sT[to] - sT[from];
            double sy = sY[to] - sY[from];
            double stt = sTT[to] - sTT[from];
            double sty = sTY[to] - sTY[from];
            double syy = sYY[to] - sYY[from];

            double varT = stt - st * st / count;  // centered sum of t^2
            if (varT <= 1e-9) return null;
            double covTY = sty - st * sy / count; // centered sum of t*y
            double varY = syy - sy * sy / count;  // centered sum of y^2

            double a = covTY / varT;
            double b = (sy - a * st) / count;
            double sse = Math.max(0.0, varY - covTY * covTY / varT);
            return new double[]{a, b, sse};
        }
    }
    // ---- FlightStatsCalculator (end) ----
}