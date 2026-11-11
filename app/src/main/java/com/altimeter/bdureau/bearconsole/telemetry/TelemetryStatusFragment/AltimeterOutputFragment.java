package com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltimeterOutputFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Ground test of the altimeter pyro / servo outputs (status screen). Each switch drives one
 *  output with the commands "k<n>T;" (output on) and "k<n>F;" (output off).
 *
 *  Safety rules implemented here:
 *    - Switching an output ON always asks for an explicit confirmation, because a connected
 *      igniter or ejection charge would fire immediately. The switch only shows ON once the
 *      user has confirmed.
 *    - Exactly one command is sent per change (a single listener; programmatic changes of
 *      the switches never send anything).
 *    - Switching OFF is immediate and needs no confirmation.
 *    - Every output still ON is switched off as soon as the screen is paused (Home button,
 *      screen off, another screen opened) and when the activity is destroyed.
 *  The state of each output is kept in a plain array, independent from the views, so that the
 *  outputs can be switched off even after the views have been destroyed.
 *
 *  Commands are sent through the host activity ({@link OutputCommandSender}) so that they go
 *  through its ordered background queue; without such a host they are written directly.
 * ============================================================================
 */

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

public class AltimeterOutputFragment extends Fragment {
    private static final String TAG = "AltimeterOutputFragment";

    /** Implemented by the host activity to send the output commands in its I/O queue. */
    public interface OutputCommandSender {
        void sendOutputCommand(String command);
    }

    private static final int NBR_OF_OUTPUTS = 4;

    private boolean ViewCreated = false;
    private ConsoleApplication lBT;
    private OutputCommandSender sender;

    private final Switch[] switches = new Switch[NBR_OF_OUTPUTS];
    // true = output currently ON on the altimeter (as far as the console knows)
    private final boolean[] outputOn = new boolean[NBR_OF_OUTPUTS];
    // true while a switch is changed by code: such changes must not send any command
    private boolean updatingSwitches = false;

    /** Required by the framework to re-create the fragment (configuration change, process restore). */
    public AltimeterOutputFragment() {
        super();
    }

    public AltimeterOutputFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (lBT == null) {
            lBT = (ConsoleApplication) context.getApplicationContext();
        }
        if (context instanceof OutputCommandSender) {
            sender = (OutputCommandSender) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        sender = null;
    }

    /** Switches off every output that is still ON. Safe to call at any time. */
    public void resetSwitches() {
        for (int i = 0; i < NBR_OF_OUTPUTS; i++) {
            if (outputOn[i]) {
                setOutput(i, false);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_status_tab1bis, container, false);

        switches[0] = view.findViewById(R.id.switchOutput1);
        switches[1] = view.findViewById(R.id.switchOutput2);
        switches[2] = view.findViewById(R.id.switchOutput3);
        switches[3] = view.findViewById(R.id.switchOutput4);

        String altiName = lBT.getAltiConfigData().getAltimeterName();
        // The AltiDuo has only two outputs
        switches[2].setVisibility("AltiDuo".equals(altiName) ? View.INVISIBLE : View.VISIBLE);
        // Only these boards have a fourth output
        boolean hasOutput4 = "AltiMultiSTM32".equals(altiName)
                || "AltiServo".equals(altiName)
                || "AltiGPS".equals(altiName);
        switches[3].setVisibility(hasOutput4 ? View.VISIBLE : View.INVISIBLE);

        for (int i = 0; i < NBR_OF_OUTPUTS; i++) {
            final int index = i;
            // Show the known state without sending anything
            setSwitchChecked(index, outputOn[index]);
            switches[index].setOnCheckedChangeListener(
                    (CompoundButton buttonView, boolean isChecked) -> onSwitchChanged(index, isChecked));
        }

        ViewCreated = true;
        return view;
    }

    /** Single entry point for a change made by the user on a switch. */
    private void onSwitchChanged(int index, boolean isChecked) {
        if (updatingSwitches) return;

        if (!isChecked) {
            // Switching off is always allowed immediately
            setOutput(index, false);
            return;
        }

        // Switching on: keep the switch OFF until the user confirms
        setSwitchChecked(index, false);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.output_fire_confirm_title)
                .setMessage(getString(R.string.output_fire_confirm_msg, index + 1))
                .setCancelable(true)
                .setPositiveButton(R.string.output_fire_confirm_yes, (dialog, which) -> setOutput(index, true))
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    /** Sends the command of one output and updates its switch. */
    private void setOutput(int index, boolean on) {
        outputOn[index] = on;
        sendCommand("k" + (index + 1) + (on ? "T" : "F") + ";\n");
        setSwitchChecked(index, on);
    }

    /** Changes a switch by code, without triggering a command. */
    private void setSwitchChecked(int index, boolean checked) {
        Switch sw = switches[index];
        if (sw == null) return;
        updatingSwitches = true;
        sw.setChecked(checked);
        updatingSwitches = false;
    }

    private void sendCommand(String command) {
        if (sender != null) {
            sender.sendOutputCommand(command);
        } else if (lBT != null && lBT.getConnected()) {
            lBT.write(command);
            lBT.flush();
        }
    }

    /** Safety: an output must never stay ON when the user is not looking at this screen. */
    @Override
    public void onPause() {
        super.onPause();
        resetSwitches();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ViewCreated = false;
        for (int i = 0; i < NBR_OF_OUTPUTS; i++) {
            switches[i] = null;
        }
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }
}
