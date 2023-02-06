package com.altimeter.bdureau.bearconsole.telemetry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

public class AltimeterOutputFragment extends Fragment {
    private static final String TAG = "Tab1StatusFragment";
    private boolean ViewCreated = false;

    private ConsoleApplication lBT;
    private Switch switchOutput1, switchOutput2, switchOutput3, switchOutput4;

    public AltimeterOutputFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    /*public void setOutput1Status(String value) {
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
    }*/

    public void resetSwitches() {
        //switch off output
        if (this.switchOutput1.isChecked()) {
            lBT.write("k1F;\n".toString());
            lBT.clearInput();
            lBT.flush();
        }
        if (this.switchOutput2.isChecked()) {
            lBT.write("k2F;\n".toString());
            lBT.clearInput();
            lBT.flush();
        }
        if (this.switchOutput3.isChecked()) {
            lBT.write("k3F;\n".toString());
            lBT.clearInput();
            lBT.flush();
        }
        if (this.switchOutput4.isChecked()) {
            lBT.write("k4F;\n".toString());
            lBT.clearInput();
            lBT.flush();
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_altimeter_status_tab1bis, container, false);


        switchOutput1 = (Switch) view.findViewById(R.id.switchOutput1);
        switchOutput2 = (Switch) view.findViewById(R.id.switchOutput2);
        switchOutput3 = (Switch) view.findViewById(R.id.switchOutput3);
        switchOutput4 = (Switch) view.findViewById(R.id.switchOutput4);
        if (!lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo"))
            switchOutput3.setVisibility(View.VISIBLE);
        else
            switchOutput3.setVisibility(View.INVISIBLE);
        if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||
                lBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS"))
            switchOutput4.setVisibility(View.VISIBLE);
        else
            switchOutput4.setVisibility(View.INVISIBLE);

        switchOutput1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput1.isChecked())
                    lBT.write("k1T;\n".toString());
                else
                    lBT.write("k1F;\n".toString());

                lBT.flush();
                lBT.clearInput();
            }
        });


        switchOutput1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput1.isChecked())
                    lBT.write("k1T;\n".toString());
                else
                    lBT.write("k1F;\n".toString());

                lBT.flush();
                lBT.clearInput();

            }
        });

        switchOutput2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput2.isChecked())
                    lBT.write("k2T;\n".toString());
                else
                    lBT.write("k2F;\n".toString());

                lBT.flush();
                lBT.clearInput();
            }
        });
        switchOutput2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput2.isChecked())
                    lBT.write("k2T;\n".toString());
                else
                    lBT.write("k2F;\n".toString());

                lBT.flush();
                lBT.clearInput();

            }
        });
        switchOutput3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput3.isChecked())
                    lBT.write("k3T;\n".toString());
                else
                    lBT.write("k3F;\n".toString());

                lBT.flush();
                lBT.clearInput();
            }
        });
        switchOutput3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput3.isChecked())
                    lBT.write("k3T;\n".toString());
                else
                    lBT.write("k3F;\n".toString());

                lBT.flush();
                lBT.clearInput();

            }
        });
        switchOutput4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput4.isChecked())
                    lBT.write("k4T;\n".toString());
                else
                    lBT.write("k4F;\n".toString());

                lBT.flush();
                lBT.clearInput();
            }
        });
        switchOutput4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput4.isChecked())
                    lBT.write("k4T;\n".toString());
                else
                    lBT.write("k4F;\n".toString());

                lBT.flush();
                lBT.clearInput();

            }
        });
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
