package com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltimeterAccelFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Status screen tab of the accelerometer boards: live values (m/s^2) of the ADXL375 (high g)
 *  and ADXL345 (low g) accelerometers on the three axes (ESP32 accelerometer boards), or of
 *  the single low g sensor of the TTGOBearAltimeter (ADXL345) and of the UltimateAltimeter
 *  (QMI8658), see AccelBoards; and the accelerometer calibration done by the firmware.
 *  The calibration is done by the firmware (command "j;": RocketFlightLogger 2.5, TTGO Bear
 *  0.6, UltimateAltimeter 0.7, see AccelCalibrationSession): the board is put on its 6 faces; on each face it must stay
 *  still until the short beep, then it is turned. This fragment shows the progress reported
 *  by the firmware (face to put up when the order is imposed, faces already measured), a Stop
 *  button, and the result (calibration values or reason of the failure).
 *  The session itself (commands, reading) is owned by the host activity (CalibrationListener)
 *  because it must stop and restart the telemetry stream of the screen.
 * ============================================================================
 */

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.connection.AccelCalibrationSession;
import com.altimeter.bdureau.bearconsole.utils.AccelBoards;

import java.util.Locale;

public class AltimeterAccelFragment extends Fragment implements AccelCalibrationSession.Listener {

    /** Implemented by the host activity: runs the calibration session. */
    public interface CalibrationListener {
        /** Stops the telemetry stream and starts the calibration ("j;"). */
        void startAccelCalibration(AccelCalibrationSession.Listener ui);

        /** Asks the firmware to stop the calibration. */
        void cancelAccelCalibration();
    }

    /** Sensor indexes used by setAccelValue(). */
    public static final int SENSOR_375 = 0;
    public static final int SENSOR_345 = 1;

    /** Localized names of the faces, same order as AccelCalibrationSession.FIRMWARE_FACE_NAMES. */
    private static final int[] FACE_NAMES = {R.string.accel_face_x_up, R.string.accel_face_x_down,
            R.string.accel_face_y_up, R.string.accel_face_y_down,
            R.string.accel_face_z_up, R.string.accel_face_z_down};

    private boolean ViewCreated = false;
    // [sensor][axis] value fields
    private final TextView[][] valueViews = new TextView[2][3];
    ConsoleApplication lBT;

    // Progress dialog of the calibration
    private AlertDialog progressDialog;
    private TextView txtInstruction, txtFaces;
    private ProgressBar progressFaces;
    private final boolean[] facesDone = new boolean[6];

    /** Required by the framework to re-create the fragment (configuration change, process restore). */
    public AltimeterAccelFragment() {
        super();
    }

    public AltimeterAccelFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // After a re-creation by the framework the dependencies were not given by a
        // constructor: the application object is always available from the context
        if (lBT == null) {
            lBT = (ConsoleApplication) context.getApplicationContext();
        }
    }

    /**
     * Displays the value of one axis.
     *
     * @param sensor   SENSOR_375 or SENSOR_345
     * @param axis     0 = X, 1 = Y, 2 = Z
     * @param milliMs2 value sent by the altimeter, in thousandths of m/s^2
     */
    public void setAccelValue(int sensor, int axis, int milliMs2) {
        if (ViewCreated && valueViews[sensor][axis] != null)
            valueViews[sensor][axis].setText(String.format(Locale.US, "%.2f m/s²", milliMs2 / 1000.0));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_status_accel, container, false);
        valueViews[SENSOR_375][0] = view.findViewById(R.id.txtViewAccel375xValue);
        valueViews[SENSOR_375][1] = view.findViewById(R.id.txtViewAccel375yValue);
        valueViews[SENSOR_375][2] = view.findViewById(R.id.txtViewAccel375zValue);
        valueViews[SENSOR_345][0] = view.findViewById(R.id.txtViewAccel345xValue);
        valueViews[SENSOR_345][1] = view.findViewById(R.id.txtViewAccel345yValue);
        valueViews[SENSOR_345][2] = view.findViewById(R.id.txtViewAccel345zValue);

        Button butCalibrate = view.findViewById(R.id.butCalibrateAccel);
        butCalibrate.setOnClickListener(v -> confirmStart());

        // Only the sensors of this board; the calibration only for the firmwares that do it
        String model = lBT.getAltiConfigData().getAltimeterName();
        AccelBoards.configureAccelRows(view, model);
        view.findViewById(R.id.layoutAccelCalibration).setVisibility(
                AccelBoards.supportsFirmwareCalibration(model) ? View.VISIBLE : View.GONE);

        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        ViewCreated = false;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    // --------------------------------------------------------------------------------
    // Calibration dialogs
    // --------------------------------------------------------------------------------

    /** Explains the procedure before sending the command. */
    private void confirmStart() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.accel_calibration_title)
                .setMessage(R.string.accel_calibration_help)
                .setPositiveButton(R.string.accel_calibration_start, (d, w) -> start())
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    private void start() {
        if (!(getActivity() instanceof CalibrationListener)) return;
        for (int i = 0; i < 6; i++) facesDone[i] = false;

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_accel_calibration, null);
        txtInstruction = content.findViewById(R.id.txtCalibInstruction);
        txtFaces = content.findViewById(R.id.txtCalibFaces);
        progressFaces = content.findViewById(R.id.progressCalibFaces);
        progressFaces.setProgress(0);
        refreshFaces();

        // Not cancelable by a touch outside: the firmware must be stopped explicitly
        progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.accel_calibration_title)
                .setView(content)
                .setCancelable(false)
                .setNegativeButton(R.string.accel_calibration_stop, null)
                .create();
        progressDialog.show();
        // Keep the dialog open after Stop: it closes when the firmware confirms
        progressDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            txtInstruction.setText(R.string.accel_calibration_stopping);
            v.setEnabled(false);
            if (getActivity() instanceof CalibrationListener) {
                ((CalibrationListener) getActivity()).cancelAccelCalibration();
            }
        });

        ((CalibrationListener) getActivity()).startAccelCalibration(this);
    }

    private void refreshFaces() {
        if (txtFaces == null || !isAdded()) return;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < 6; i++) {
            if (facesDone[i]) count++;
            sb.append(facesDone[i] ? "✔ " : "○ ").append(getString(FACE_NAMES[i]));
            if (i < 5) sb.append('\n');
        }
        txtFaces.setText(sb.toString());
        progressFaces.setProgress(count);
    }

    @Override
    public void onCalibrationStarted(boolean anyOrder) {
        if (txtInstruction == null || !isAdded()) return;
        txtInstruction.setText(anyOrder ? R.string.accel_calibration_any_order
                : R.string.accel_calibration_fixed_order);
    }

    @Override
    public void onNextFace(int face, int number) {
        if (txtInstruction == null || !isAdded()) return;
        txtInstruction.setText(getString(R.string.accel_calibration_next_face, number,
                getString(FACE_NAMES[face])));
    }

    @Override
    public void onFaceDone(int face, int count) {
        if (face < 0 || face >= 6) return;
        facesDone[face] = true;
        refreshFaces();
    }

    @Override
    public void onCalibrationFinished(AccelCalibrationSession.Result result, String details) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        txtInstruction = null;
        txtFaces = null;
        if (!isAdded()) return;
        int msg;
        switch (result) {
            case SAVED: msg = R.string.accel_calibration_saved_msg; break;
            case STOPPED: msg = R.string.accel_calibration_stopped_msg; break;
            case NOT_SUPPORTED: msg = R.string.accel_calibration_not_supported; break;
            case NO_ANSWER: msg = R.string.accel_calibration_no_answer; break;
            case FAILED:
            default: msg = R.string.accel_calibration_failed_msg; break;
        }
        String text = getString(msg);
        if (details != null && !details.isEmpty()) {
            // Values or reason, as printed by the firmware
            text = text + "\n\n" + details;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.accel_calibration_title)
                .setMessage(text)
                .setPositiveButton(R.string.fv_ok, null)
                .show();
    }
}
