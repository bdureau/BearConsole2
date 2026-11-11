package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltiConfig1Fragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  First tab of the altimeter configuration: function of each deployment output (main, drogue,
 *  timer, altitude, lift off...), delay of each output and main deployment altitude. Outputs 3
 *  and 4 are only shown for the boards that have them. The typed values are validated by
 *  validateInputs() before upload.
 * ============================================================================
 */

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

public class AltiConfig1Fragment extends Fragment {
    private AltiConfigData lAltiCfg;

    private Spinner dropdownOut1, dropdownOut2, dropdownOut3, dropdownOut4;
    private EditText outDelay1, outDelay2, outDelay3, outDelay4;
    private EditText mainAltitude;
    private TextView txtOut1, txtOut2, txtOut3, txtOut4;
    private TextView txtViewDelay1, txtViewDelay2, txtViewDelay3, txtViewDelay4;

    private boolean viewCreated = false;

    public AltiConfig1Fragment() {
        super();
    }

    public AltiConfig1Fragment(AltiConfigData cfg) {
        this.lAltiCfg = cfg;
    }

    /**
     * Checks every value typed in this tab before upload; invalid fields are flagged.
     * Hidden fields (outputs not available on the connected board) are not checked.
     *
     * @return true if all the values can be sent to the altimeter
     */
    public boolean validateInputs() {
        boolean ok = true;
        ok &= ConfigInputValidator.checkInt(outDelay1, ConfigInputValidator.OUTPUT_DELAY_MIN, ConfigInputValidator.OUTPUT_DELAY_MAX);
        ok &= ConfigInputValidator.checkInt(outDelay2, ConfigInputValidator.OUTPUT_DELAY_MIN, ConfigInputValidator.OUTPUT_DELAY_MAX);
        ok &= ConfigInputValidator.checkInt(outDelay3, ConfigInputValidator.OUTPUT_DELAY_MIN, ConfigInputValidator.OUTPUT_DELAY_MAX);
        ok &= ConfigInputValidator.checkInt(outDelay4, ConfigInputValidator.OUTPUT_DELAY_MIN, ConfigInputValidator.OUTPUT_DELAY_MAX);
        ok &= ConfigInputValidator.checkInt(mainAltitude, ConfigInputValidator.MAIN_ALTITUDE_MIN, ConfigInputValidator.MAIN_ALTITUDE_MAX);
        return ok;
    }

    public boolean isViewCreated() {
        return viewCreated;
    }

    public int getDropdownOut1() {
        return dropdownOut1 != null ? (int) dropdownOut1.getSelectedItemId() : 0;
    }

    public int getDropdownOut2() {
        return dropdownOut2 != null ? (int) dropdownOut2.getSelectedItemId() : 1;
    }

    public int getDropdownOut3() {
        return dropdownOut3 != null ? (int) dropdownOut3.getSelectedItemId() : 2;
    }

    public int getDropdownOut4() {
        if (lAltiCfg != null && ("AltiMultiSTM32".equals(lAltiCfg.getAltimeterName())
                || "AltiGPS".equals(lAltiCfg.getAltimeterName())
                || "AltiServo".equals(lAltiCfg.getAltimeterName()))) {
            return dropdownOut4 != null ? (int) dropdownOut4.getSelectedItemId() : -1;
        }
        return -1;
    }

    public int getOutDelay1() {
        return parseInteger(outDelay1, 0);
    }

    public int getOutDelay2() {
        return parseInteger(outDelay2, 0);
    }

    public int getOutDelay3() {
        return parseInteger(outDelay3, 0);
    }

    public int getOutDelay4() {
        if (lAltiCfg != null && ("AltiMultiSTM32".equals(lAltiCfg.getAltimeterName())
                || "AltiGPS".equals(lAltiCfg.getAltimeterName())
                || "AltiServo".equals(lAltiCfg.getAltimeterName()))) {
            return parseInteger(outDelay4, 0);
        }
        return -1;
    }

    public int getMainAltitude() {
        return parseInteger(mainAltitude, 100);
    }

    private int parseInteger(EditText editText, int defaultValue) {
        if (editText == null || editText.getText() == null) return defaultValue;
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab1, container, false);

        if (lAltiCfg == null && getActivity() instanceof AltiTabConfigActivity) {
            lAltiCfg = ((AltiTabConfigActivity) getActivity()).getAltiConfigData();
        }

        dropdownOut1 = view.findViewById(R.id.spinnerOut1);
        dropdownOut2 = view.findViewById(R.id.spinnerOut2);
        dropdownOut3 = view.findViewById(R.id.spinnerOut3);
        dropdownOut4 = view.findViewById(R.id.spinnerOut4);

        outDelay1 = view.findViewById(R.id.editTxtDelay1);
        outDelay2 = view.findViewById(R.id.editTxtDelay2);
        outDelay3 = view.findViewById(R.id.editTxtDelay3);
        outDelay4 = view.findViewById(R.id.editTxtDelay4);

        mainAltitude = view.findViewById(R.id.editTxtMainAltitude);

        txtOut1 = view.findViewById(R.id.txtOut1);
        txtOut2 = view.findViewById(R.id.txtOut2);
        txtOut3 = view.findViewById(R.id.txtOut3);
        txtOut4 = view.findViewById(R.id.txtOut4);

        txtViewDelay1 = view.findViewById(R.id.txtViewDelay1);
        txtViewDelay2 = view.findViewById(R.id.txtViewDelay2);
        txtViewDelay3 = view.findViewById(R.id.txtViewDelay3);
        txtViewDelay4 = view.findViewById(R.id.txtViewDelay4);

        // Shared adapter for all 4 output selectors
        String[] outputOptions = new String[]{
                getString(R.string.main_config),
                getString(R.string.drogue_config),
                getString(R.string.timer_config),
                getString(R.string.disabled_config),
                getString(R.string.landing_config),
                getString(R.string.liftoff_config),
                getString(R.string.altitude_config)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, outputOptions);

        dropdownOut1.setAdapter(adapter);
        dropdownOut2.setAdapter(adapter);
        dropdownOut3.setAdapter(adapter);
        dropdownOut4.setAdapter(adapter);

        dropdownOut1.setOnItemSelectedListener(createOutputListener(txtViewDelay1, R.string.altitude1, R.string.delay1));
        dropdownOut2.setOnItemSelectedListener(createOutputListener(txtViewDelay2, R.string.altitude2, R.string.delay2));
        dropdownOut3.setOnItemSelectedListener(createOutputListener(txtViewDelay3, R.string.altitude3, R.string.delay3));
        dropdownOut4.setOnItemSelectedListener(createOutputListener(txtViewDelay4, R.string.altitude4, R.string.delay4));

        setupTooltips();
        populateFields();

        viewCreated = true;
        return view;
    }

    private AdapterView.OnItemSelectedListener createOutputListener(TextView delayLabel, int altResId, int delayResId) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (delayLabel == null) return;
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals(getString(R.string.altitude_config))) {
                    delayLabel.setText(getString(altResId));
                } else {
                    delayLabel.setText(getString(delayResId));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private void setupTooltips() {
        if (mainAltitude != null) {
            mainAltitude.setOnClickListener(v -> showTooltip(v, getString(R.string.main_altitude_tooltip)));
        }
        if (outDelay1 != null) {
            outDelay1.setOnClickListener(v -> showTooltip(v, getString(R.string.OutDelay1_tooltip)));
        }
        if (outDelay2 != null) {
            outDelay2.setOnClickListener(v -> showTooltip(v, getString(R.string.OutDelay2_tooltip)));
        }
        if (txtOut1 != null) {
            txtOut1.setOnClickListener(v -> showTooltip(v, getString(R.string.txtOut1_tooltip)));
        }
        if (txtOut2 != null) {
            txtOut2.setOnClickListener(v -> showTooltip(v, getString(R.string.txtOut2_tooltip)));
        }
    }

    private void showTooltip(View v, String text) {
        if (v == null || getContext() == null) return;
        ViewTooltip.on(v).color(Color.BLACK).position(ViewTooltip.Position.TOP).text(text).show();
    }

    private void populateFields() {
        if (lAltiCfg == null) return;

        dropdownOut1.setSelection(lAltiCfg.getOutput1());
        dropdownOut2.setSelection(lAltiCfg.getOutput2());
        outDelay1.setText(String.valueOf(lAltiCfg.getOutput1Delay()));
        outDelay2.setText(String.valueOf(lAltiCfg.getOutput2Delay()));
        mainAltitude.setText(String.valueOf(lAltiCfg.getMainAltitude()));

        String altiName = lAltiCfg.getAltimeterName();

        if (!"AltiDuo".equals(altiName)) {
            dropdownOut3.setSelection(lAltiCfg.getOutput3());
            dropdownOut3.setVisibility(View.VISIBLE);
            txtOut3.setVisibility(View.VISIBLE);
            outDelay3.setText(String.valueOf(lAltiCfg.getOutput3Delay()));
            outDelay3.setVisibility(View.VISIBLE);
            txtViewDelay3.setVisibility(View.VISIBLE);

            txtOut3.setOnClickListener(v -> showTooltip(v, getString(R.string.txtOut3_tooltip)));
            outDelay3.setOnClickListener(v -> showTooltip(v, getString(R.string.OutDelay3_tooltip)));
        } else {
            dropdownOut3.setVisibility(View.INVISIBLE);
            txtOut3.setVisibility(View.INVISIBLE);
            outDelay3.setVisibility(View.INVISIBLE);
            txtViewDelay3.setVisibility(View.INVISIBLE);
        }

        if ("AltiMultiSTM32".equals(altiName) || "AltiGPS".equals(altiName) || "AltiServo".equals(altiName)) {
            dropdownOut4.setSelection(lAltiCfg.getOutput4());
            dropdownOut4.setVisibility(View.VISIBLE);
            txtOut4.setVisibility(View.VISIBLE);
            outDelay4.setText(String.valueOf(lAltiCfg.getOutput4Delay()));
            outDelay4.setVisibility(View.VISIBLE);
            txtViewDelay4.setVisibility(View.VISIBLE);

            txtOut4.setOnClickListener(v -> showTooltip(v, getString(R.string.txtOut4_tooltip)));
            outDelay4.setOnClickListener(v -> showTooltip(v, getString(R.string.OutDelay4_tooltip)));
        } else {
            dropdownOut4.setVisibility(View.INVISIBLE);
            txtOut4.setVisibility(View.INVISIBLE);
            outDelay4.setVisibility(View.INVISIBLE);
            txtViewDelay4.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
        dropdownOut1 = null;
        dropdownOut2 = null;
        dropdownOut3 = null;
        dropdownOut4 = null;
        outDelay1 = null;
        outDelay2 = null;
        outDelay3 = null;
        outDelay4 = null;
        mainAltitude = null;
        txtOut1 = null;
        txtOut2 = null;
        txtOut3 = null;
        txtOut4 = null;
        txtViewDelay1 = null;
        txtViewDelay2 = null;
        txtViewDelay3 = null;
        txtViewDelay4 = null;
    }
}