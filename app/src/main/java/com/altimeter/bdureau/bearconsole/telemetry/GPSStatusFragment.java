package com.altimeter.bdureau.bearconsole.telemetry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

public class GPSStatusFragment extends Fragment {
    private static final String TAG = "Tab2StatusFragment";
    private boolean ViewCreated = false;
    private TextView txtViewLatitude, txtViewLongitude, txtViewLatitudeValue, txtViewLongitudeValue;
    private TextView txtViewTelLatitudeValue, txtViewTelLongitudeValue;
    private TextView txtViewSatellitesVal, txtViewHdopVal, txtViewGPSAltitudeVal, txtViewGPSSpeedVal;
    private TextView txtViewLocationAgeValue, txtViewTimeSatValue;
    private ConsoleApplication lBT;

    public GPSStatusFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    public void setLatitudeValue(String value) {
        if (ViewCreated)
            this.txtViewLatitudeValue.setText(value);
    }

    public void setLongitudeValue(String value) {
        if (ViewCreated)
            this.txtViewLongitudeValue.setText(value);
    }

    public void setSatellitesVal(String value) {
        if (ViewCreated)
            this.txtViewSatellitesVal.setText(value);
    }

    public void setHdopVal(String value) {
        if (ViewCreated)
            this.txtViewHdopVal.setText(value);
    }

    public void setGPSAltitudeVal(String value) {
        if (ViewCreated)
            this.txtViewGPSAltitudeVal.setText(value);
    }

    public void setGPSSpeedVal(String value) {
        if (ViewCreated)
            this.txtViewGPSSpeedVal.setText(value);
    }

    public void setLocationAgeValue(String value) {
        if (ViewCreated)
            this.txtViewLocationAgeValue.setText(value);
    }

    public void setTimeSatValue(String value) {
        if (ViewCreated)
            this.txtViewTimeSatValue.setText(value);
    }

    public void setTelLatitudeValue(String value) {
        if (ViewCreated)
            this.txtViewTelLatitudeValue.setText(value);
    }

    public void setTelLongitudeValue(String value) {
        if (ViewCreated)
            this.txtViewTelLongitudeValue.setText(value);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_altimeter_status_tab2, container, false);

        // GPS altimeter data
        txtViewLatitude = (TextView) view.findViewById(R.id.txtViewLatitude);
        txtViewLongitude = (TextView) view.findViewById(R.id.txtViewLongitude);
        txtViewLatitudeValue = (TextView) view.findViewById(R.id.txtViewLatitudeValue);
        txtViewLongitudeValue = (TextView) view.findViewById(R.id.txtViewLongitudeValue);
        txtViewSatellitesVal = (TextView) view.findViewById(R.id.txtViewSatellitesVal);
        txtViewHdopVal = (TextView) view.findViewById(R.id.txtViewHdopVal);
        txtViewGPSAltitudeVal = (TextView) view.findViewById(R.id.txtViewGPSAltitudeVal);
        txtViewGPSSpeedVal = (TextView) view.findViewById(R.id.txtViewGPSSpeedVal);
        txtViewLocationAgeValue = (TextView) view.findViewById(R.id.txtViewLocationAgeValue);
        txtViewTimeSatValue = (TextView) view.findViewById(R.id.txtViewTimeSatValue);

        // GPS tel data
        txtViewTelLatitudeValue = (TextView) view.findViewById(R.id.txtViewTelLatitudeValue);
        txtViewTelLongitudeValue = (TextView) view.findViewById(R.id.txtViewTelLongitudeValue);

        ViewCreated = true;
        return view;
    }
}
