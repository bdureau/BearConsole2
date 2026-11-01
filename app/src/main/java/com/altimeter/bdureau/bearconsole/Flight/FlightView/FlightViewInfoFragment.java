package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightViewInfoFragment
 *
 * @description:
 *  Displays comprehensive post-flight summary analytics (apogee, burn time,
 *  max velocity, acceleration in Gs, landing speed, and GPS ground distance).
 *  Includes asynchronous background CSV flight export and ZIP sharing compatible
 *  with modern scoped storage.
 *
 * @author: boris.dureau@neuf.fr
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

    private void populateFlightStatistics() {
        if (myflight == null || allFlightData == null || flightName == null) {
            return;
        }

        XYSeriesCollection flightData = myflight.GetFlightData(flightName);
        if (flightData == null || flightData.getSeriesCount() == 0) {
            return;
        }

        XYSeries altitudeSeries = flightData.getSeries(0);
        int nbrData = altitudeSeries.getItemCount();

        XYSeries speed = allFlightData.getSeries(getString(R.string.curve_speed));
        XYSeries accel = allFlightData.getSeries(getString(R.string.curve_accel));

        double feetInMeter = (myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS) ? 1.0 : 3.28084;
        String unitsLabel = myBT.getAppConf().getUnitsValue();

        flightNbrValue.setText(String.valueOf(flightName));
        nbrOfSamplesValue.setText(String.valueOf(nbrData));

        // Total flight duration
        double flightDuration = altitudeSeries.getMaxX() / 1000.0;
        if (flightDuration < 60.0) {
            flightDurationValue.setText(String.format(Locale.US, "%.2f secs", flightDuration));
        } else {
            flightDurationValue.setText(String.format(Locale.US, "%.2f minutes", flightDuration / 60.0));
        }

        // Apogee and time to apogee
        double apogeeAltitude = altitudeSeries.getMaxY();
        apogeeAltitudeValue.setText(String.format(Locale.US, "%.0f %s", apogeeAltitude, unitsLabel));

        int apogeePos = searchX(altitudeSeries, apogeeAltitude);
        double apogeeTime = 0.0;
        if (apogeePos != -1) {
            apogeeTime = altitudeSeries.getX(apogeePos).doubleValue();
            timeToApogeeValue.setText(String.format(Locale.US, "%.2f secs", apogeeTime / 1000.0));
        }

        // Maximum velocity
        if (speed != null) {
            double maxSpeed = speed.getMaxY();
            maxVelociyValue.setText(String.format(Locale.US, "%d %s/secs", (long) maxSpeed, unitsLabel));

            // Burn time derived from peak velocity timing
            int maxSpeedPos = searchX(speed, maxSpeed);
            if (maxSpeedPos != -1) {
                double burnTime = speed.getX(maxSpeedPos).doubleValue();
                burnTimeValue.setText(String.format(Locale.US, "%.2f secs", burnTime / 1000.0));
            } else {
                burnTimeValue.setText("N/A");
            }

            // Landing velocity evaluation
            int timeBeforeLanding = searchXBack(altitudeSeries, 30);
            if (timeBeforeLanding != -1) {
                int posLanding = searchY(speed, altitudeSeries.getX(timeBeforeLanding).doubleValue());
                if (posLanding != -1) {
                    double landingSpeed = speed.getY(posLanding).doubleValue();
                    landingSpeedValue.setText(String.format(Locale.US, "%d %s/secs", (long) landingSpeed, unitsLabel));
                } else {
                    landingSpeedValue.setText("N/A");
                }
            } else {
                landingSpeedValue.setText("N/A");
            }

            // Descent speed after apogee
            int descentStartPos = searchY(speed, apogeeTime + 100);
            if (descentStartPos != -1) {
                double maxDescentSpeed = 0.0;
                for (int i = descentStartPos; i < speed.getItemCount(); i++) {
                    double y = speed.getY(i).doubleValue();
                    if (y > maxDescentSpeed) {
                        maxDescentSpeed = y;
                    }
                }
                maxDescentValue.setText(String.format(Locale.US, "%d %s/secs", (long) maxDescentSpeed, unitsLabel));
            } else {
                maxDescentValue.setText("N/A");
            }
        }

        // Maximum acceleration
        if (accel != null) {
            double maxAccel = (accel.getMaxY() * feetInMeter) / 9.80665;
            maxAccelerationValue.setText(String.format(Locale.US, "%d G", (long) maxAccel));
        }

        mainAltitudeValue.setText(String.format(" %s", unitsLabel));

        // GPS surface distance
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS") && flightData.getSeriesCount() > 7) {
            XYSeries latSeries = flightData.getSeries(6);
            XYSeries lonSeries = flightData.getSeries(7);
            int count = latSeries.getItemCount();
            List<GeoPoint> points = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                double lat = latSeries.getY(i).doubleValue();
                double lon = lonSeries.getY(i).doubleValue();
                if (lat > 0.0 && lon > 0.0) {
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
                    msg("Failed saving some flight curves");
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
                    if (isAdded()) msg("Failed creating ZIP archive");
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
}