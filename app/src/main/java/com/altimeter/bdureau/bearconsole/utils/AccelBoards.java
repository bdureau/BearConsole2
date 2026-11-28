package com.altimeter.bdureau.bearconsole.utils;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AccelBoards
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  What the application knows about the accelerometer of each altimeter model, in one place
 *  (status screen, telemetry decoding, flight decoding, flight accelerometer tab):
 *    - AltiMultiESP32_accel / _345 / _375 (RocketFlightLogger): ADXL375 (high g) and ADXL345
 *      (low g); live values in fields 18..23 of the telemetry frame; values sent in
 *      thousandths of m/s^2 (Adafruit sensor events x 1000); calibration by the firmware
 *      ("j;", firmware 2.5 and later);
 *    - TTGOBearAltimeter: ADXL345 only, fields 18..20, thousandths of m/s^2; calibration by
 *      the firmware ("j;", same protocol as RocketFlightLogger, from the 0.6 firmware with
 *      the calibration);
 *    - TTGOMiniBearAltimeter: ADXL345 in the recorded flights only (no live values);
 *    - UltimateAltimeter: QMI8658 IMU, fields 18..20 of the telemetry frame. Its library
 *      (SensorLib) gives the acceleration in g, so the firmware sends thousandths of g
 *      (milli-g), live and in the recorded flights, unlike the other boards. Calibration by
 *      the firmware ("j;", same protocol, from the 0.7 firmware with the calibration); the
 *      board has no buzzer, it shows the progress on its screen.
 *  Every value is converted to m/s^2 (curves) or thousandths of m/s^2 (live values) here.
 * ============================================================================
 */

import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.altimeter.bdureau.bearconsole.R;

public final class AccelBoards {

    /** Standard gravity (m/s^2). */
    public static final double STANDARD_GRAVITY = 9.80665;

    private AccelBoards() {
    }

    /** true for the ESP32 boards with ADXL375 + ADXL345 (RocketFlightLogger). */
    public static boolean isEsp32Accel(String model) {
        return "AltiMultiESP32_accel".equals(model)
                || "AltiMultiESP32_accel_345".equals(model)
                || "AltiMultiESP32_accel_375".equals(model);
    }

    /**
     * true when the altimeter sends live accelerometer values in its telemetry frames:
     * the status screen then shows the values and the 3D rocket.
     */
    public static boolean hasLiveAccel(String model) {
        return isEsp32Accel(model)
                || "TTGOBearAltimeter".equals(model)
                || "UltimateAltimeter".equals(model);
    }

    /** true when the recorded flights contain accelerometer curves. */
    public static boolean hasFlightAccel(String model) {
        return hasLiveAccel(model) || "TTGOMiniBearAltimeter".equals(model);
    }

    /** true when the board has a high g ADXL375 in addition to its low g sensor. */
    public static boolean hasHighG(String model) {
        return isEsp32Accel(model);
    }

    /** Name of the low g sensor (rows of the accelerometer tabs). */
    public static String lowGSensorName(String model) {
        return "UltimateAltimeter".equals(model) ? "QMI8658" : "ADXL345";
    }

    /**
     * true when the firmware calibrates its accelerometers ("j;" command). An older firmware
     * of these boards does not answer: AccelCalibrationSession reports NOT_SUPPORTED.
     */
    public static boolean supportsFirmwareCalibration(String model) {
        return isEsp32Accel(model)
                || "TTGOBearAltimeter".equals(model)
                || "UltimateAltimeter".equals(model);
    }

    /** true when the firmware sends thousandths of g instead of thousandths of m/s^2. */
    public static boolean sendsMilliG(String model) {
        return "UltimateAltimeter".equals(model);
    }

    /**
     * Live value of the telemetry frame -> thousandths of m/s^2 (unit of the status screen).
     */
    public static int toMilliMs2(String model, int raw) {
        return sendsMilliG(model) ? (int) Math.round(raw * STANDARD_GRAVITY) : raw;
    }

    /**
     * Adapts the accelerometer rows (layout fragment_telemetry_mp_tab2, shared by the status
     * screen and the flight accelerometer tab) to the board: the ADXL375 rows are removed
     * when the board has no high g sensor (the low g rows then move up under the title),
     * and the low g rows are named after the sensor ("QMI8658 X" on the Ultimate).
     */
    public static void configureAccelRows(View root, String model) {
        if (!hasHighG(model)) {
            int[] highG = {R.id.txtViewAccel375x, R.id.txtViewAccel375xValue,
                    R.id.txtViewAccel375y, R.id.txtViewAccel375yValue,
                    R.id.txtViewAccel375z, R.id.txtViewAccel375zValue};
            for (int id : highG) {
                View v = root.findViewById(id);
                if (v != null) v.setVisibility(View.GONE);
            }
            // The first low g row was placed under the last ADXL375 row
            for (int id : new int[]{R.id.txtViewAccel345x, R.id.txtViewAccel345xValue}) {
                View v = root.findViewById(id);
                if (v != null && v.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    lp.addRule(RelativeLayout.BELOW, R.id.txtViewTitle);
                    v.setLayoutParams(lp);
                }
            }
        }
        if (!"ADXL345".equals(lowGSensorName(model))) {
            String name = lowGSensorName(model);
            setText(root, R.id.txtViewAccel345x, name + " X");
            setText(root, R.id.txtViewAccel345y, name + " Y");
            setText(root, R.id.txtViewAccel345z, name + " Z");
        }
    }

    private static void setText(View root, int id, String text) {
        View v = root.findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(text);
    }

    /** Value of a recorded flight -> m/s^2 (unit of the flight curves). */
    public static float toMs2(String model, int raw) {
        double ms2 = raw / 1000.0;
        return (float) (sendsMilliG(model) ? ms2 * STANDARD_GRAVITY : ms2);
    }
}
