package com.altimeter.bdureau.bearconsole.telemetry;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: RocketViewFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Status screen tab of the accelerometer boards: live 3D attitude of the rocket, drawn by
 *  the Processing sketch utils.Rocket (same renderer as the MotorGimbalConsole application)
 *  hosted in a child PFragment.
 *  The host activity gives each complete accelerometer sample (X, Y, Z in g) with
 *  setAcceleration(). This fragment turns it into the attitude of the rocket:
 *    - on the pad the accelerometer measures the 1 g reaction to gravity, i.e. the "up"
 *      vector expressed in the board axes;
 *    - the long axis of the rocket is the board axis that carries most of the gravity on
 *      the first valid sample (the board is mounted along the body), with its sign; the two
 *      other axes (b, c) are perpendicular to the body;
 *    - tilt = angle between "up" and the long axis; angle on b / c = tilt projected on each
 *      axis; the nose direction is given to the sketch, which tilts the rocket accordingly.
 *  The roll cannot be measured with an accelerometer. When the total acceleration is far
 *  from 1 g (rocket handled, moving) the attitude is flagged as not valid.
 *  After a calibration of the accelerometers (resetAxis()) the long axis is detected again.
 * ============================================================================
 */

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.utils.Rocket;

import processing.android.PFragment;

public class RocketViewFragment extends Fragment {
    private static final String TAG = "RocketViewFragment";

    /** Accelerations outside of this range (g) do not give a meaningful attitude. */
    private static final double MIN_VALID_G = 0.6;
    private static final double MAX_VALID_G = 1.4;
    private static final String[] AXIS_NAMES = {"X", "Y", "Z"};

    private Rocket myRocket;
    private PFragment fragment;
    private View view;
    private boolean ViewCreated = false;

    // Long axis of the rocket in the board frame (0 = X, 1 = Y, 2 = Z), -1 = not detected yet
    private int upAxis = -1;
    private int upSign = 1;

    /** Required by the framework to re-create the fragment (configuration change, process restore). */
    public RocketViewFragment() {
        super();
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_altimeter_status_part3, container, false);

        myRocket = new Rocket();
        myRocket.setAccelLabels(getString(R.string.attitude_tilt), getString(R.string.attitude_total_accel),
                getString(R.string.attitude_moving), getString(R.string.attitude_waiting));
        fragment = new PFragment(myRocket);
        getChildFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fragment != null) fragment.onDestroy();
        if (myRocket != null) myRocket.onDestroy();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        if (fragment != null) fragment.onStop();
        if (myRocket != null) myRocket.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (fragment != null) fragment.requestDraw();
        if (view != null) {
            view.refreshDrawableState();
            view.bringToFront();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fragment != null) {
            fragment.onResume();
            // Only way to redraw the sketch after leaving the tab (same as the gimbal console)
            getChildFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    /** The accelerometers have been calibrated: detect the long axis again. */
    public void resetAxis() {
        upAxis = -1;
    }

    /**
     * New accelerometer sample.
     *
     * @param g acceleration on the board X, Y and Z axes, in g
     */
    public void setAcceleration(double[] g) {
        if (myRocket == null || view == null) return;
        double m = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
        boolean valid = m > MIN_VALID_G && m < MAX_VALID_G;

        if (upAxis < 0) {
            if (!valid) {
                myRocket.setAccelAttitude(0, 0, 1, 0, 0, 0, (float) m, false, "?", "?");
                return;
            }
            upAxis = 0;
            for (int a = 1; a < 3; a++) {
                if (Math.abs(g[a]) > Math.abs(g[upAxis])) upAxis = a;
            }
            upSign = g[upAxis] >= 0 ? 1 : -1;
        }

        // Right-handed board frame (a = long axis, b, c perpendicular); when the long axis
        // points to -a, b is flipped too so that the frame stays right-handed
        int ib = (upAxis + 1) % 3, ic = (upAxis + 2) % 3;
        double ua = g[upAxis] * upSign, ub = g[ib] * upSign, uc = g[ic];
        float tilt = 0, angB = 0, angC = 0, dx = 0, dy = 0, dz = 1;
        if (valid) {
            ua /= m;
            ub /= m;
            uc /= m;
            // When the rocket leans towards +b, the +b axis points down: ub < 0
            dx = (float) -ub;
            dy = (float) -uc;
            dz = (float) ua;
            tilt = (float) Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, ua))));
            angB = (float) Math.toDegrees(Math.atan2(-ub, ua));
            angC = (float) Math.toDegrees(Math.atan2(-uc, ua));
        }
        myRocket.setAccelAttitude(dx, dy, dz, tilt, angB, angC, (float) m, valid,
                (upSign > 0 ? "+" : "-") + AXIS_NAMES[ib], "+" + AXIS_NAMES[ic]);
    }
}
