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

public class AltimeterAccelFragment extends Fragment {
    private static final String TAG = "Tab2StatusFragment";
    private boolean ViewCreated = false;
    private TextView txtViewAccel375x, txtViewAccel375y, txtViewAccel375z,
            txtViewAccel345x, txtViewAccel345y, txtViewAccel345z;
    ConsoleApplication lBT;

    public AltimeterAccelFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    public void setAccel375x(String value) {
        if (ViewCreated)
            this.txtViewAccel375x.setText(value);
    }

    public void setAccel375y(String value) {
        if (ViewCreated)
            this.txtViewAccel375y.setText(value);
    }

    public void setAccel375z(String value) {
        if (ViewCreated)
            this.txtViewAccel375z.setText(value);
    }

    public void setAccel345x(String value) {
        if (ViewCreated)
            this.txtViewAccel345x.setText(value);
    }

    public void setAccel345y(String value) {
        if (ViewCreated)
            this.txtViewAccel345y.setText(value);
    }

    public void setAccel345z(String value) {
        if (ViewCreated)
            this.txtViewAccel345z.setText(value);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_telemetry_mp_tab2, container, false);
        txtViewAccel375x = (TextView) view.findViewById(R.id.txtViewAccel375xValue);
        txtViewAccel375y = (TextView) view.findViewById(R.id.txtViewAccel375yValue);
        txtViewAccel375z = (TextView) view.findViewById(R.id.txtViewAccel375zValue);
        txtViewAccel345x = (TextView) view.findViewById(R.id.txtViewAccel345xValue);
        txtViewAccel345y = (TextView) view.findViewById(R.id.txtViewAccel345yValue);
        txtViewAccel345z = (TextView) view.findViewById(R.id.txtViewAccel345zValue);

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

}