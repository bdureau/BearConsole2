package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightViewInfoAccelFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Displays maximum G-force acceleration metrics along X, Y, and Z axes
 *  for both high-G (ADXL375) and low-G (ADXL345) accelerometer sensors.
 *  Hardened against null series references, View memory leaks, and Android
 *  fragment recreation crashes.
 * ============================================================================
 */

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.Locale;

public class FlightViewInfoAccelFragment extends Fragment {
    private static final String TAG = "FlightViewInfoAccelFragment";
    private static final double STANDARD_GRAVITY = 9.80665;

    private boolean viewCreated = false;
    private XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;

    private TextView txtViewAccel375x, txtViewAccel375y, txtViewAccel375z;
    private TextView txtViewAccel345x, txtViewAccel345y, txtViewAccel345z;

    /**
     * Default constructor required by the Android framework for state restoration.
     */
    public FlightViewInfoAccelFragment() {
        super();
    }

    /**
     * Parameterized constructor used by tab adapters.
     */
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
        if (allFlightData == null) allFlightData = host.getAllFlightData();
    }

    public FlightViewInfoAccelFragment(FlightData data,
                                       XYSeriesCollection data2,
                                       ConsoleApplication bt,
                                       String[] pUnits,
                                       String pFlightName) {
        this.allFlightData = data2;
        this.myBT = bt;
    }

    public void setAccel375x(String value) {
        if (viewCreated && txtViewAccel375x != null) txtViewAccel375x.setText(value);
    }

    public void setAccel375y(String value) {
        if (viewCreated && txtViewAccel375y != null) txtViewAccel375y.setText(value);
    }

    public void setAccel375z(String value) {
        if (viewCreated && txtViewAccel375z != null) txtViewAccel375z.setText(value);
    }

    public void setAccel345x(String value) {
        if (viewCreated && txtViewAccel345x != null) txtViewAccel345x.setText(value);
    }

    public void setAccel345y(String value) {
        if (viewCreated && txtViewAccel345y != null) txtViewAccel345y.setText(value);
    }

    public void setAccel345z(String value) {
        if (viewCreated && txtViewAccel345z != null) txtViewAccel345z.setText(value);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        restoreFromHost();
        View view = inflater.inflate(R.layout.fragment_telemetry_mp_tab2, container, false);

        txtViewAccel375x = view.findViewById(R.id.txtViewAccel375xValue);
        txtViewAccel375y = view.findViewById(R.id.txtViewAccel375yValue);
        txtViewAccel375z = view.findViewById(R.id.txtViewAccel375zValue);
        txtViewAccel345x = view.findViewById(R.id.txtViewAccel345xValue);
        txtViewAccel345y = view.findViewById(R.id.txtViewAccel345yValue);
        txtViewAccel345z = view.findViewById(R.id.txtViewAccel345zValue);

        populateAccelerationMetrics();

        viewCreated = true;
        return view;
    }

    /**
     * Extracts peak acceleration values across sensor channels and converts m/s² into Gs.
     */
    private void populateAccelerationMetrics() {
        if (allFlightData == null) {
            setFallbackText();
            return;
        }

        bindSeriesValue(txtViewAccel375x, "accel375x");
        bindSeriesValue(txtViewAccel375y, "accel375y");
        bindSeriesValue(txtViewAccel375z, "accel375z");

        bindSeriesValue(txtViewAccel345x, "accel345x");
        bindSeriesValue(txtViewAccel345y, "accel345y");
        bindSeriesValue(txtViewAccel345z, "accel345z");
    }

    /**
     * Safely reads a series by name, converts max value to Gs, and updates target TextView.
     */
    private void bindSeriesValue(TextView targetView, String seriesKey) {
        if (targetView == null) return;

        try {
            XYSeries series = allFlightData.getSeries(seriesKey);
            if (series != null && series.getItemCount() > 0) {
                double maxG = series.getMaxY() / STANDARD_GRAVITY;
                targetView.setText(String.format(Locale.US, "%.2f G", maxG));
            } else {
                targetView.setText(R.string.not_available);
            }
        } catch (Exception ignored) {
            targetView.setText(R.string.not_available);
        }
    }

    private void setFallbackText() {
        if (txtViewAccel375x != null) txtViewAccel375x.setText(R.string.not_available);
        if (txtViewAccel375y != null) txtViewAccel375y.setText(R.string.not_available);
        if (txtViewAccel375z != null) txtViewAccel375z.setText(R.string.not_available);
        if (txtViewAccel345x != null) txtViewAccel345x.setText(R.string.not_available);
        if (txtViewAccel345y != null) txtViewAccel345y.setText(R.string.not_available);
        if (txtViewAccel345z != null) txtViewAccel345z.setText(R.string.not_available);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
        // Release View references to prevent Activity/View memory leaks
        txtViewAccel375x = null;
        txtViewAccel375y = null;
        txtViewAccel375z = null;
        txtViewAccel345x = null;
        txtViewAccel345y = null;
        txtViewAccel345z = null;
    }

    public boolean isViewCreated() {
        return viewCreated;
    }
}