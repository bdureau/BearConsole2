package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.github.florent37.viewtooltip.ViewTooltip;

public class AltimeterConfig3Fragment extends Fragment {
    private static final String TAG = "AltimeterConfig3Fragment";
    private TextView altiName;
    private Spinner dropdownUnits;
    private Spinner dropdownBipMode;
    private EditText Freq;
    private EditText altiID;
    private Spinner dropdownUseTelemetryPort;
    private Spinner dropdownServoStayOn;
    private TextView txtServoStayOn;
    private Spinner dropdownServoSwitch;
    private TextView txtServoSwitch;
    private AltiConfigData lAltiCfg=null;

    public AltimeterConfig3Fragment(AltiConfigData cfg) {
        lAltiCfg = cfg;
    }
    private boolean ViewCreated = false;

    public boolean isViewCreated() {
        return ViewCreated;
    }

    public int getFreq() {
        int ret;
        try {
            ret = Integer.parseInt(this.Freq.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;

    }

    public void setFreq(int freq) {
        Freq.setText(freq);
    }

    public void setAltiName(String altiName) {
        this.altiName.setText(altiName);
    }

    public String getAltiName() {
        return (String) this.altiName.getText();
    }

    public void setAltiID(int ID) {
        this.altiID.setText(ID);
    }

    public int getAltiID() {

        int ret;
        try {
            ret = Integer.parseInt(this.altiID.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getDropdownUseTelemetryPort() {
        return (int) this.dropdownUseTelemetryPort.getSelectedItemId();
    }

    public void setDropdownUseTelemetryPort(int UseTelemetryPort) {
        this.dropdownUseTelemetryPort.setSelection(UseTelemetryPort);
    }

    public int getDropdownUnits() {
        return (int) this.dropdownUnits.getSelectedItemId();
    }

    public void setDropdownUnits(int Units) {
        this.dropdownUnits.setSelection(Units);
    }

    public int getDropdownBipMode() {
        return (int) this.dropdownBipMode.getSelectedItemId();
    }

    public void setDropdownBipMode(int BipMode) {
        this.dropdownBipMode.setSelection(BipMode);
    }

    public int getServoStayOn() {
        return (int) this.dropdownServoStayOn.getSelectedItemId();
    }

    public void setServoStayOn(int ServoStayOn) {
        this.dropdownServoStayOn.setSelection(ServoStayOn);
    }
    public int getServoSwitch() {
        return (int) this.dropdownServoSwitch.getSelectedItemId();
    }

    public void setServoSwitch(int ServoSwitch) {
        this.dropdownServoSwitch.setSelection(ServoSwitch);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab3, container, false);
        //Beep mode
        dropdownBipMode = (Spinner) view.findViewById(R.id.spinnerBipMode);
        //"Mode1", "Mode2", "Off"
        String[] items = new String[]{"Mode1", "Mode2",
                getResources().getString(R.string.config_off)};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items);
        dropdownBipMode.setAdapter(adapter);

        Log.d("UseTelemetryPort","before UseTelemetryPort" );
        //UseTelemetryPort
        dropdownUseTelemetryPort = (Spinner) view.findViewById(R.id.spinnerUseTelemetryPort);
        //"No", "Yes"
        String[] itemsUseTelemetryPort = new String[]{"No",
                "Yes"};
        ArrayAdapter<String> adapterUseTelemetryPort = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsUseTelemetryPort);
        dropdownUseTelemetryPort.setAdapter(adapterUseTelemetryPort);

        Log.d("UseTelemetryPort","after UseTelemetryPort" );
        //units
        dropdownUnits = (Spinner) view.findViewById(R.id.spinnerUnit);
        //"Meters", "Feet"
        String[] items2 = new String[]{getResources().getString(R.string.unit_meter),
                getResources().getString(R.string.unit_feet)};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items2);
        dropdownUnits.setAdapter(adapter2);
        Log.d("UseTelemetryPort","after dropdownUnits" );
        // Tool tip
        view.findViewById(R.id.txtAltiUnit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the altitude units you are familiar with
                        .text(getResources().getString(R.string.txtAltiUnit_tooltip))
                        .show();
            }
        });
        //Altimeter name
        altiName = (TextView) view.findViewById(R.id.txtAltiNameValue);
        //here you can set the beep frequency
        Freq = (EditText) view.findViewById(R.id.editTxtBipFreq);

        // Tool tip
        Freq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the beeping frequency in Hz
                        .text(getResources().getString(R.string.editTxtBipFreq_tooltip))
                        .show();
            }
        });

        altiID = (EditText) view.findViewById(R.id.editTxtAltiIDValue);
        dropdownServoStayOn= (Spinner) view.findViewById(R.id.spinnerServoStayOn);
        //"No", "Yes"
        String[] items3 = new String[]{getResources().getString(R.string.config_no),
                getResources().getString(R.string.config_yes)};
        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownServoStayOn.setAdapter(adapter3);

        txtServoStayOn = (TextView) view.findViewById(R.id.txtServoStayOn);
        if(lAltiCfg.getAltimeterName().equals("AltiServo")) {
            dropdownServoStayOn.setVisibility(View.VISIBLE);
            txtServoStayOn.setVisibility(View.VISIBLE);
        } else {
            dropdownServoStayOn.setVisibility(View.INVISIBLE);
            txtServoStayOn.setVisibility(View.INVISIBLE);
        }

        //ServoSwitch
        dropdownServoSwitch = (Spinner) view.findViewById(R.id.spinnerServoSwitch);
        dropdownServoSwitch.setAdapter(adapter3);

        txtServoSwitch = (TextView) view.findViewById(R.id.txtServoSwitch);

        if(lAltiCfg.getAltimeterName().equals("AltiServo")) {
            dropdownServoSwitch.setVisibility(View.VISIBLE);
            txtServoSwitch.setVisibility(View.VISIBLE);
        } else {
            dropdownServoSwitch.setVisibility(View.INVISIBLE);
            txtServoSwitch.setVisibility(View.INVISIBLE);
        }

        if (lAltiCfg != null) {
            altiName.setText(lAltiCfg.getAltimeterName() + " ver: " +
                    lAltiCfg.getAltiMajorVersion() + "." + lAltiCfg.getAltiMinorVersion());

            dropdownBipMode.setSelection(lAltiCfg.getBeepingMode());
            dropdownUnits.setSelection(lAltiCfg.getUnits());
            Freq.setText(String.valueOf(lAltiCfg.getBeepingFrequency()));
            altiID.setText(String.valueOf(lAltiCfg.getAltiID()));
            if (lAltiCfg.getAltimeterName().equals("AltiServo")) {
                dropdownServoStayOn.setSelection(lAltiCfg.getServoStayOn());
                dropdownServoSwitch.setSelection(lAltiCfg.getServoSwitch());
            }
            //Log.d("UseTelemetryPort","before  getting config" );
            dropdownUseTelemetryPort.setSelection(lAltiCfg.getUseTelemetryPort());
            //Log.d("UseTelemetryPort","after getting config" );
        }
        ViewCreated = true;
        return view;
    }

}
