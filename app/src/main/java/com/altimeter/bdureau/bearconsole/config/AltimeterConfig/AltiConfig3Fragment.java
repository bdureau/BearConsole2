package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltiConfig3Fragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Third tab of the altimeter configuration: altimeter name and firmware version, units,
 *  beeping mode and frequency, altimeter identifier, telemetry port, Bluetooth name (recent
 *  ESP32 / TTGO firmwares) and servo options (AltiServo). The typed values are validated by
 *  validateInputs() before upload.
 * ============================================================================
 */

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.ConfigInputValidator;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AltiTabConfigActivity;
import com.github.florent37.viewtooltip.ViewTooltip;

public class AltiConfig3Fragment extends Fragment {
    private TextView altiName, txtAltimeterID;
    private Spinner dropdownUnits, dropdownBipMode, dropdownUseTelemetryPort;
    private EditText freq, altiID, bluetoothName;
    private Spinner dropdownServoStayOn, dropdownServoSwitch;
    private TextView txtServoStayOn, txtServoSwitch, txtBluetoothName;

    private AltiConfigData lAltiCfg = null;
    private boolean viewCreated = false;

    public AltiConfig3Fragment() {
        super();
    }

    public AltiConfig3Fragment(AltiConfigData cfg) {
        this.lAltiCfg = cfg;
    }

    public boolean isViewCreated() {
        return viewCreated;
    }

    /**
     * Checks every value typed in this tab before upload; invalid fields are flagged.
     * The Bluetooth name is only checked when the board supports it (field displayed).
     *
     * @return true if all the values can be sent to the altimeter
     */
    public boolean validateInputs() {
        boolean ok = true;
        ok &= ConfigInputValidator.checkInt(freq, ConfigInputValidator.BEEP_FREQUENCY_MIN, ConfigInputValidator.BEEP_FREQUENCY_MAX);
        ok &= ConfigInputValidator.checkInt(altiID, ConfigInputValidator.ALTI_ID_MIN, ConfigInputValidator.ALTI_ID_MAX);
        ok &= ConfigInputValidator.checkBluetoothName(bluetoothName);
        return ok;
    }

    public int getFreq() {
        return parseInteger(freq, 440);
    }

    public void setFreq(int frequency) {
        if (freq != null) freq.setText(String.valueOf(frequency));
    }

    public int getAltiID() {
        return parseInteger(altiID, 0);
    }

    public void setAltiID(int id) {
        if (altiID != null) altiID.setText(String.valueOf(id));
    }

    public int getDropdownUseTelemetryPort() {
        return dropdownUseTelemetryPort != null ? (int) dropdownUseTelemetryPort.getSelectedItemId() : 0;
    }

    public int getDropdownUnits() {
        return dropdownUnits != null ? (int) dropdownUnits.getSelectedItemId() : 0;
    }

    public int getDropdownBipMode() {
        return dropdownBipMode != null ? (int) dropdownBipMode.getSelectedItemId() : 0;
    }

    public int getServoStayOn() {
        return dropdownServoStayOn != null ? (int) dropdownServoStayOn.getSelectedItemId() : 0;
    }

    public int getServoSwitch() {
        return dropdownServoSwitch != null ? (int) dropdownServoSwitch.getSelectedItemId() : 0;
    }

    public String getBluetoothName() {
        return bluetoothName != null && bluetoothName.getText() != null
                ? bluetoothName.getText().toString().trim()
                : "";
    }

    private int parseInteger(EditText editText, int fallback) {
        if (editText == null || editText.getText() == null) return fallback;
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab3, container, false);

        if (lAltiCfg == null && getActivity() instanceof AltiTabConfigActivity) {
            lAltiCfg = ((AltiTabConfigActivity) getActivity()).getAltiConfigData();
        }

        dropdownBipMode = view.findViewById(R.id.spinnerBipMode);
        dropdownUseTelemetryPort = view.findViewById(R.id.spinnerUseTelemetryPort);
        dropdownUnits = view.findViewById(R.id.spinnerUnit);
        altiName = view.findViewById(R.id.txtAltiNameValue);
        freq = view.findViewById(R.id.editTxtBipFreq);
        altiID = view.findViewById(R.id.editTxtAltiIDValue);
        dropdownServoStayOn = view.findViewById(R.id.spinnerServoStayOn);
        txtServoStayOn = view.findViewById(R.id.txtServoStayOn);
        txtBluetoothName = view.findViewById(R.id.txtBluetoothName);
        bluetoothName = view.findViewById(R.id.editBluetoothName);
        dropdownServoSwitch = view.findViewById(R.id.spinnerServoSwitch);
        txtServoSwitch = view.findViewById(R.id.txtServoSwitch);
        txtAltimeterID = view.findViewById(R.id.txtAltimeterID);

        setupSpinners(view);
        setupTooltips(view);
        populateFields();

        viewCreated = true;
        return view;
    }

    private void setupSpinners(View view) {
        String[] bipOptions = {"Mode1", "Mode2", getString(R.string.config_off)};
        dropdownBipMode.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, bipOptions));

        String[] telemetryPortOptions = {"No", "Yes"};
        dropdownUseTelemetryPort.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, telemetryPortOptions));

        String[] unitOptions = {getString(R.string.unit_meter), getString(R.string.unit_feet)};
        dropdownUnits.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, unitOptions));

        String[] yesNoOptions = {getString(R.string.config_no), getString(R.string.config_yes)};
        ArrayAdapter<String> yesNoAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, yesNoOptions);
        dropdownServoStayOn.setAdapter(yesNoAdapter);
        dropdownServoSwitch.setAdapter(yesNoAdapter);
    }

    private void setupTooltips(View view) {
        view.findViewById(R.id.txtAltiUnit).setOnClickListener(v -> showTooltip(v, getString(R.string.txtAltiUnit_tooltip)));
        freq.setOnClickListener(v -> showTooltip(v, getString(R.string.editTxtBipFreq_tooltip)));
    }

    private void showTooltip(View v, String text) {
        if (v == null || getContext() == null) return;
        ViewTooltip.on(v).color(Color.BLACK).position(ViewTooltip.Position.TOP).text(text).show();
    }

    private void populateFields() {
        if (lAltiCfg == null) return;

        altiName.setText(String.format("%s ver: %d.%d",
                lAltiCfg.getAltimeterName(), lAltiCfg.getAltiMajorVersion(), lAltiCfg.getAltiMinorVersion()));
        dropdownBipMode.setSelection(lAltiCfg.getBeepingMode());
        dropdownUnits.setSelection(lAltiCfg.getUnits());
        freq.setText(String.valueOf(lAltiCfg.getBeepingFrequency()));
        dropdownUseTelemetryPort.setSelection(lAltiCfg.getUseTelemetryPort());

        String name = lAltiCfg.getAltimeterName();

        if ("AltiServo".equals(name)) {
            dropdownServoStayOn.setVisibility(View.VISIBLE);
            txtServoStayOn.setVisibility(View.VISIBLE);
            dropdownServoSwitch.setVisibility(View.VISIBLE);
            txtServoSwitch.setVisibility(View.VISIBLE);

            dropdownServoStayOn.setSelection(lAltiCfg.getServoStayOn());
            dropdownServoSwitch.setSelection(lAltiCfg.getServoSwitch());
        } else {
            dropdownServoStayOn.setVisibility(View.INVISIBLE);
            txtServoStayOn.setVisibility(View.INVISIBLE);
            dropdownServoSwitch.setVisibility(View.INVISIBLE);
            txtServoSwitch.setVisibility(View.INVISIBLE);
        }

        boolean supportsBluetoothName = (("AltiMultiESP32".equals(name) || "AltiMultiESP32_accel".equals(name)
                || "AltiMultiESP32_accel_375".equals(name) || "AltiMultiESP32_accel_345".equals(name))
                && lAltiCfg.getAltiMajorVersion() >= 2 && lAltiCfg.getAltiMinorVersion() > 0)
                || ("TTGOBearAltimeter".equals(name) && lAltiCfg.getAltiMajorVersion() >= 0 && lAltiCfg.getAltiMinorVersion() > 4);

        if (supportsBluetoothName) {
            txtBluetoothName.setVisibility(View.VISIBLE);
            bluetoothName.setVisibility(View.VISIBLE);
            bluetoothName.setText(lAltiCfg.getBluetoothName());
        } else {
            txtBluetoothName.setVisibility(View.INVISIBLE);
            bluetoothName.setVisibility(View.INVISIBLE);
        }

        boolean supportsAltiID = ("AltiMultiESP32".equals(name) || "AltiMultiESP32_accel".equals(name)
                || "AltiMultiESP32_accel_375".equals(name) || "AltiMultiESP32_accel_345".equals(name))
                && lAltiCfg.getAltiMajorVersion() <= 2 && lAltiCfg.getAltiMinorVersion() < 1;

        if (supportsAltiID) {
            txtAltimeterID.setVisibility(View.VISIBLE);
            altiID.setVisibility(View.VISIBLE);
            altiID.setText(String.valueOf(lAltiCfg.getAltiID()));
        } else {
            txtAltimeterID.setVisibility(View.INVISIBLE);
            altiID.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
        altiName = null;
        txtAltimeterID = null;
        dropdownUnits = null;
        dropdownBipMode = null;
        dropdownUseTelemetryPort = null;
        freq = null;
        altiID = null;
        bluetoothName = null;
        dropdownServoStayOn = null;
        dropdownServoSwitch = null;
        txtServoStayOn = null;
        txtServoSwitch = null;
        txtBluetoothName = null;
    }
}