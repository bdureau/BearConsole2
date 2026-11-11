package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltiConfig2Fragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Second tab of the altimeter configuration: number of measures confirming the apogee,
 *  sensor resolution, EEPROM size, beep on / off, end of recording altitude, telemetry type,
 *  supersonic delay, lift off altitude, battery type, recording timeout and serial speed.
 *  The typed values are validated by validateInputs() before upload.
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

public class AltiConfig2Fragment extends Fragment {
    private AltiConfigData lAltiCfg = null;

    private static final String[] ITEMS_BAUD_RATE = {
            "300", "1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200", "230400"
    };
    private static final String[] ITEMS_RESOLUTION = {"ULTRALOWPOWER", "STANDARD", "HIGHRES", "ULTRAHIGHRES"};
    private static final String[] ITEMS_EEPROM = {"32", "64", "128", "256", "512", "1024"};
    private static final String[] ITEMS_TELEMETRY = {"Fast", "Average", "Slow", "VerySlow"};

    private Spinner dropdownBeepOnOff, dropdownBaudRate, dropdownAltimeterResolution, dropdownEEpromSize;
    private Spinner dropdownTelemetryType, dropdownSupersonicDelay, dropdownBatteryType;
    private EditText apogeeMeasures, endRecordAltitude, liftOffAltitude, recordingTimeout;
    private TextView txtViewTelemetryType, txtViewEEpromSize, txtViewEndRecordAltitude, txtViewBatteryType, txtViewRecordingTimeOut;

    private boolean viewCreated = false;

    public AltiConfig2Fragment() {
        super();
    }

    public AltiConfig2Fragment(AltiConfigData cfg) {
        this.lAltiCfg = cfg;
    }

    /**
     * Checks every value typed in this tab before upload; invalid fields are flagged.
     *
     * @return true if all the values can be sent to the altimeter
     */
    public boolean validateInputs() {
        boolean ok = true;
        ok &= ConfigInputValidator.checkInt(apogeeMeasures, ConfigInputValidator.APOGEE_MEASURES_MIN, ConfigInputValidator.APOGEE_MEASURES_MAX);
        ok &= ConfigInputValidator.checkInt(endRecordAltitude, ConfigInputValidator.END_RECORD_ALTITUDE_MIN, ConfigInputValidator.END_RECORD_ALTITUDE_MAX);
        ok &= ConfigInputValidator.checkInt(liftOffAltitude, ConfigInputValidator.LIFTOFF_ALTITUDE_MIN, ConfigInputValidator.LIFTOFF_ALTITUDE_MAX);
        ok &= ConfigInputValidator.checkInt(recordingTimeout, ConfigInputValidator.RECORDING_TIMEOUT_MIN, ConfigInputValidator.RECORDING_TIMEOUT_MAX);
        return ok;
    }

    public boolean isViewCreated() {
        return viewCreated;
    }

    public int getApogeeMeasures() {
        return parseInteger(apogeeMeasures, 5);
    }

    public int getAltimeterResolution() {
        return dropdownAltimeterResolution != null ? (int) dropdownAltimeterResolution.getSelectedItemId() : 0;
    }

    public int getEEpromSize() {
        if (dropdownEEpromSize != null && dropdownEEpromSize.getSelectedItem() != null) {
            try {
                return Integer.parseInt(dropdownEEpromSize.getSelectedItem().toString());
            } catch (NumberFormatException ignored) {}
        }
        return 512;
    }

    public long getBaudRate() {
        if (dropdownBaudRate != null && dropdownBaudRate.getSelectedItem() != null) {
            try {
                return Long.parseLong(dropdownBaudRate.getSelectedItem().toString());
            } catch (NumberFormatException ignored) {}
        }
        return 38400;
    }

    public int getBeepOnOff() {
        return dropdownBeepOnOff != null ? (int) dropdownBeepOnOff.getSelectedItemId() : 0;
    }

    public int getRecordTempOnOff() {
        return dropdownTelemetryType != null ? (int) dropdownTelemetryType.getSelectedItemId() : 0;
    }

    public int getSupersonicDelayOnOff() {
        return dropdownSupersonicDelay != null ? (int) dropdownSupersonicDelay.getSelectedItemId() : 0;
    }

    public int getEndRecordAltitude() {
        return parseInteger(endRecordAltitude, 3);
    }

    public int getLiftOffAltitude() {
        return parseInteger(liftOffAltitude, 20);
    }

    public int getBatteryType() {
        return dropdownBatteryType != null ? (int) dropdownBatteryType.getSelectedItemId() : 0;
    }

    public int getRecordingTimeout() {
        return parseInteger(recordingTimeout, 120);
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
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab2, container, false);

        if (lAltiCfg == null && getActivity() instanceof AltiTabConfigActivity) {
            lAltiCfg = ((AltiTabConfigActivity) getActivity()).getAltiConfigData();
        }

        dropdownBaudRate = view.findViewById(R.id.spinnerBaudRate);
        apogeeMeasures = view.findViewById(R.id.editTxtApogeeMeasures);
        dropdownAltimeterResolution = view.findViewById(R.id.spinnerAltimeterResolution);
        txtViewEEpromSize = view.findViewById(R.id.txtViewEEpromSize);
        dropdownEEpromSize = view.findViewById(R.id.spinnerEEpromSize);
        dropdownBeepOnOff = view.findViewById(R.id.spinnerBeepOnOff);
        txtViewTelemetryType = view.findViewById(R.id.txtViewTelemetryType);
        dropdownTelemetryType = view.findViewById(R.id.spinnerTelemetryType);
        dropdownSupersonicDelay = view.findViewById(R.id.spinnerSupersonicDelay);
        endRecordAltitude = view.findViewById(R.id.editTxtEndRecordAltitude);
        txtViewEndRecordAltitude = view.findViewById(R.id.txtViewEndRecordAltitude);
        liftOffAltitude = view.findViewById(R.id.editTxtLiftOffAltitude);
        dropdownBatteryType = view.findViewById(R.id.spinnerBatteryType);
        txtViewBatteryType = view.findViewById(R.id.txtViewBatteryType);
        recordingTimeout = view.findViewById(R.id.editTxtRecordingTimeOut);
        txtViewRecordingTimeOut = view.findViewById(R.id.txtViewRecordingTimeOut);

        setupSpinners(view);
        setupTooltips(view);
        populateFields();

        viewCreated = true;
        return view;
    }

    private void setupSpinners(View view) {
        dropdownBaudRate.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ITEMS_BAUD_RATE));
        dropdownAltimeterResolution.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ITEMS_RESOLUTION));
        dropdownEEpromSize.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ITEMS_EEPROM));

        String[] beepOptions = {getString(R.string.config_on), getString(R.string.config_off)};
        dropdownBeepOnOff.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, beepOptions));

        dropdownTelemetryType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, ITEMS_TELEMETRY));

        String[] supersonicOptions = {getString(R.string.config_off), getString(R.string.config_on)};
        dropdownSupersonicDelay.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, supersonicOptions));

        String[] batteryOptions = {getString(R.string.config_unknown), "2S (7.4 Volts)", "9 Volts", "3S (11.1 Volts)"};
        dropdownBatteryType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, batteryOptions));
    }

    private void setupTooltips(View view) {
        view.findViewById(R.id.txtViewBaudRate).setOnClickListener(v -> showTooltip(v, getString(R.string.txtViewBaudRate_tooltip)));
        apogeeMeasures.setOnClickListener(v -> showTooltip(v, getString(R.string.editTxtApogeeMeasures_tooltip)));
        view.findViewById(R.id.txtViewAltimeterResolution).setOnClickListener(v -> showTooltip(v, getString(R.string.txtViewAltimeterResolution_tooltip)));
        txtViewEEpromSize.setOnClickListener(v -> showTooltip(v, getString(R.string.txtViewEEpromSize_tooltip)));
        txtViewTelemetryType.setOnClickListener(v -> showTooltip(v, "Choose the type of telemetry module you use"));
        endRecordAltitude.setOnClickListener(v -> showTooltip(v, getString(R.string.EndRecordAltitude_tooltip)));
        liftOffAltitude.setOnClickListener(v -> showTooltip(v, getString(R.string.LiftOffAltitude_tooltip)));
        view.findViewById(R.id.txtViewBatteryType).setOnClickListener(v -> showTooltip(v, getString(R.string.txtViewBatteryType_tooltip)));
    }

    private void showTooltip(View v, String text) {
        if (v == null || getContext() == null) return;
        ViewTooltip.on(v).color(Color.BLACK).position(ViewTooltip.Position.TOP).text(text).show();
    }

    private void populateFields() {
        if (lAltiCfg == null) return;

        setSpinnerToValue(dropdownBaudRate, ITEMS_BAUD_RATE, String.valueOf(lAltiCfg.getConnectionSpeed()));
        apogeeMeasures.setText(String.valueOf(lAltiCfg.getNbrOfMeasuresForApogee()));
        dropdownAltimeterResolution.setSelection(lAltiCfg.getAltimeterResolution());
        liftOffAltitude.setText(String.valueOf(lAltiCfg.getLiftOffAltitude()));
        dropdownBeepOnOff.setSelection(lAltiCfg.getBeepOnOff());
        dropdownTelemetryType.setSelection(lAltiCfg.getTelemetryType());
        dropdownSupersonicDelay.setSelection(lAltiCfg.getSupersonicDelay());

        String altiName = lAltiCfg.getAltimeterName();

        if ("AltiServo".equals(altiName) || "AltiDuo".equals(altiName)) {
            dropdownEEpromSize.setVisibility(View.INVISIBLE);
            txtViewEEpromSize.setVisibility(View.INVISIBLE);
            dropdownTelemetryType.setVisibility(View.INVISIBLE);
            txtViewTelemetryType.setVisibility(View.INVISIBLE);
            endRecordAltitude.setVisibility(View.INVISIBLE);
            txtViewEndRecordAltitude.setVisibility(View.INVISIBLE);
            recordingTimeout.setVisibility(View.INVISIBLE);
            txtViewRecordingTimeOut.setVisibility(View.INVISIBLE);
            dropdownBatteryType.setVisibility(View.INVISIBLE);
            txtViewBatteryType.setVisibility(View.INVISIBLE);
        } else {
            dropdownEEpromSize.setVisibility(View.VISIBLE);
            txtViewEEpromSize.setVisibility(View.VISIBLE);
            dropdownTelemetryType.setVisibility(View.VISIBLE);
            txtViewTelemetryType.setVisibility(View.VISIBLE);
            endRecordAltitude.setVisibility(View.VISIBLE);
            txtViewEndRecordAltitude.setVisibility(View.VISIBLE);
            recordingTimeout.setVisibility(View.VISIBLE);
            txtViewRecordingTimeOut.setVisibility(View.VISIBLE);

            setSpinnerToValue(dropdownEEpromSize, ITEMS_EEPROM, String.valueOf(lAltiCfg.getEepromSize()));
            endRecordAltitude.setText(String.valueOf(lAltiCfg.getEndRecordAltitude()));
            recordingTimeout.setText(String.valueOf(lAltiCfg.getRecordingTimeout()));

            if ("AltiMulti".equals(altiName) || "AltiMultiV2".equals(altiName)) {
                dropdownBatteryType.setVisibility(View.INVISIBLE);
                txtViewBatteryType.setVisibility(View.INVISIBLE);
            } else {
                dropdownBatteryType.setVisibility(View.VISIBLE);
                txtViewBatteryType.setVisibility(View.VISIBLE);
                dropdownBatteryType.setSelection(lAltiCfg.getBatteryType());
            }
        }
    }

    private void setSpinnerToValue(Spinner spinner, String[] array, String value) {
        if (spinner == null || array == null || value == null) return;
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
        dropdownBaudRate = null;
        apogeeMeasures = null;
        dropdownAltimeterResolution = null;
        txtViewEEpromSize = null;
        dropdownEEpromSize = null;
        dropdownBeepOnOff = null;
        txtViewTelemetryType = null;
        dropdownTelemetryType = null;
        dropdownSupersonicDelay = null;
        endRecordAltitude = null;
        txtViewEndRecordAltitude = null;
        liftOffAltitude = null;
        dropdownBatteryType = null;
        txtViewBatteryType = null;
        recordingTimeout = null;
        txtViewRecordingTimeOut = null;
    }
}