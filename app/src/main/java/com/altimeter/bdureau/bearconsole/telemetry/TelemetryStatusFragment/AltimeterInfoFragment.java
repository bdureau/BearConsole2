package com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment;
/**
 * @description: This will display altimeter information such as current altitude, battery voltage etc...
 * @author: boris.dureau@neuf.fr
 **/

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

public class AltimeterInfoFragment extends Fragment {
    private static final String TAG = "Tab1TelemetryFragment";
    private boolean ViewCreated = false;
    private TextView txtStatusAltiName, txtStatusAltiNameValue;
    private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
    private TextView txtViewAltitude, txtViewVoltage, txtViewLink, txtTemperature, txtEEpromUsage, txtNbrOfFlight;
    private TextView txtViewOutput3, txtViewOutput4, txtViewBatteryVoltage, txtViewEEprom, txtViewFlight;
    ConsoleApplication lBT;

    public AltimeterInfoFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    public void setOutput1Status(String value) {
        if (ViewCreated)
            this.txtViewOutput1Status.setText(outputStatus(value));
    }

    public void setOutput2Status(String value) {
        if (ViewCreated)
            this.txtViewOutput2Status.setText(outputStatus(value));
    }

    public void setOutput3Status(String value) {
        if (ViewCreated)
            this.txtViewOutput3Status.setText(outputStatus(value));
    }

    public void setOutput4Status(String value) {
        if (ViewCreated)
            this.txtViewOutput4Status.setText(outputStatus(value));
    }

    public void setAltitude(String value) {
        if (ViewCreated)
            this.txtViewAltitude.setText(value);
    }

    public void setVoltage(String value) {
        if (ViewCreated)
            this.txtViewVoltage.setText(value);
    }

    public void setTemperature(String value) {
        if (ViewCreated)
            this.txtTemperature.setText(value);
    }

    public void setEEpromUsage(String value) {
        if (ViewCreated)
            this.txtEEpromUsage.setText(value);
    }

    public void setNbrOfFlight(String value) {
        if (ViewCreated)
            this.txtNbrOfFlight.setText(value);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_telemetry_mp_tab1, container, false);

        txtStatusAltiName = (TextView) view.findViewById(R.id.txtStatusAltiName);
        txtStatusAltiNameValue = (TextView) view.findViewById(R.id.txtStatusAltiNameValue);
        txtViewOutput1Status = (TextView) view.findViewById(R.id.txtViewOutput1Status);
        txtViewOutput2Status = (TextView) view.findViewById(R.id.txtViewOutput2Status);
        txtViewOutput3Status = (TextView) view.findViewById(R.id.txtViewOutput3Status);
        txtViewOutput4Status = (TextView) view.findViewById(R.id.txtViewOutput4Status);
        txtViewAltitude = (TextView) view.findViewById(R.id.txtViewAltitude);
        txtViewVoltage = (TextView) view.findViewById(R.id.txtViewVoltage);
        txtViewLink = (TextView) view.findViewById(R.id.txtViewLink);
        txtViewOutput3 = (TextView) view.findViewById(R.id.txtViewOutput3);
        txtViewOutput4 = (TextView) view.findViewById(R.id.txtViewOutput4);

        txtViewBatteryVoltage = (TextView) view.findViewById(R.id.txtViewBatteryVoltage);
        txtTemperature = (TextView) view.findViewById(R.id.txtViewTemperature);
        txtEEpromUsage = (TextView) view.findViewById(R.id.txtViewEEpromUsage);
        txtNbrOfFlight = (TextView) view.findViewById(R.id.txtViewNbrOfFlight);
        txtViewEEprom = (TextView) view.findViewById(R.id.txtViewEEprom);
        txtViewFlight = (TextView) view.findViewById(R.id.txtViewFlight);

        txtStatusAltiNameValue.setText(lBT.getAltiConfigData().getAltimeterName());
        if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
            txtViewVoltage.setVisibility(View.VISIBLE);
            txtViewBatteryVoltage.setVisibility(View.VISIBLE);
        } else {
            txtViewVoltage.setVisibility(View.INVISIBLE);
            txtViewBatteryVoltage.setVisibility(View.INVISIBLE);
        }
        if (!lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")) {
            txtViewOutput3Status.setVisibility(View.VISIBLE);
            txtViewOutput3.setVisibility(View.VISIBLE);
        } else {
            txtViewOutput3Status.setVisibility(View.INVISIBLE);
            txtViewOutput3.setVisibility(View.INVISIBLE);
        }

        if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            txtViewOutput4Status.setVisibility(View.VISIBLE);
            txtViewOutput4.setVisibility(View.VISIBLE);
        } else {
            txtViewOutput4Status.setVisibility(View.INVISIBLE);
            txtViewOutput4.setVisibility(View.INVISIBLE);
        }
        //hide eeprom
        if (lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")
                || lBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            txtViewEEprom.setVisibility(View.INVISIBLE);
            txtViewFlight.setVisibility(View.INVISIBLE);
            txtEEpromUsage.setVisibility(View.INVISIBLE);
            txtNbrOfFlight.setVisibility(View.INVISIBLE);
        } else {
            txtViewEEprom.setVisibility(View.VISIBLE);
            txtViewFlight.setVisibility(View.VISIBLE);
            txtEEpromUsage.setVisibility(View.VISIBLE);
            txtNbrOfFlight.setVisibility(View.VISIBLE);
        }

        txtViewLink.setText(lBT.getConnectionType());

        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ViewCreated = false;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    private String outputStatus(String msg) {
        String res = "";
        switch (msg) {
            case "0":
                res = getResources().getString(R.string.no_continuity);
                break;
            case "1":
                res = this.getContext().getResources().getString(R.string.continuity);
                break;
            case "-1":
                res = getResources().getString(R.string.disabled);
                break;
        }
        return res;
    }
}