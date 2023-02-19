package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

import android.graphics.Color;
import android.os.Bundle;
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

public class AltimeterConfig2Fragment extends Fragment {
    private static final String TAG = "AltimeterConfig2Fragment";
    private AltiConfigData lAltiCfg=null;
    private String[] itemsBaudRate;
    private String[] itemsAltimeterResolution;
    private String[] itemsEEpromSize;
    private String[] itemsBeepOnOff;
    private String[] itemsTelemetryType;
    private String[] itemsSupersonicDelayOnOff;
    private String[] itemsBatteryType;

    private Spinner dropdownBeepOnOff;
    private Spinner dropdownBaudRate;
    private EditText ApogeeMeasures;
    private Spinner dropdownAltimeterResolution, dropdownEEpromSize;
    private Spinner dropdownTelemetryType;
    private Spinner dropdownSupersonicDelay;
    private Spinner dropdownBatteryType;
    private EditText EndRecordAltitude;
    private EditText LiftOffAltitude, RecordingTimeout;
    private boolean ViewCreated = false;
    private TextView txtViewTelemetryType, txtViewEEpromSize;

    public AltimeterConfig2Fragment (AltiConfigData cfg) {
        lAltiCfg=cfg;
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }

    public int getApogeeMeasures() {
        int ret;
        try {
            ret = Integer.parseInt(this.ApogeeMeasures.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setApogeeMeasures(int apogeeMeasures) {
        ApogeeMeasures.setText(String.valueOf(apogeeMeasures));
    }

    public int getAltimeterResolution() {
        return (int) this.dropdownAltimeterResolution.getSelectedItemId();
    }

    public void setAltimeterResolution(int AltimeterResolution) {
        this.dropdownAltimeterResolution.setSelection(AltimeterResolution);
    }

    public int getEEpromSize() {
        int ret;
        try {
            ret = Integer.parseInt(itemsEEpromSize[(int) dropdownEEpromSize.getSelectedItemId()]);
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setEEpromSize(int EEpromSize) {
        this.dropdownEEpromSize.setSelection(lAltiCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
    }

    public long getBaudRate() {
        int ret;
        try {
            ret = Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]);
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setBaudRate(long BaudRate) {
        this.dropdownBaudRate.setSelection(lAltiCfg.arrayIndex(itemsBaudRate, String.valueOf(BaudRate)));
    }

    public int getBeepOnOff() {
        return (int) this.dropdownBeepOnOff.getSelectedItemId();
    }

    public void setBeepOnOff(int BeepOnOff) {
        dropdownBeepOnOff.setSelection(BeepOnOff);
    }

    public int getRecordTempOnOff() {
        return (int) this.dropdownTelemetryType.getSelectedItemId();
    }

    public void setRecordTempOnOff(int RecordTempOnOff) {
        this.dropdownTelemetryType.setSelection(RecordTempOnOff);
    }

    public int getSupersonicDelayOnOff() {
        return (int) this.dropdownSupersonicDelay.getSelectedItemId();
    }

    public void setSupersonicDelayOnOff(int SupersonicDelayOnOff) {
        this.dropdownSupersonicDelay.setSelection(SupersonicDelayOnOff);
    }

    public int getEndRecordAltitude() {
        int ret;
        try {
            ret = Integer.parseInt(this.EndRecordAltitude.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setEndRecordAltitude(int EndRecordAltitude) {
        this.EndRecordAltitude.setText(String.valueOf(EndRecordAltitude));
    }

    public int getLiftOffAltitude() {
        int ret;
        try {
            ret = Integer.parseInt(this.LiftOffAltitude.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setLiftOffAltitude(int LiftOffAltitude) {
        this.LiftOffAltitude.setText(String.valueOf(LiftOffAltitude));
    }

    public int getBatteryType() {
        return (int) this.dropdownBatteryType.getSelectedItemId();
    }

    public void setBatteryType(int BatteryType) {
        dropdownBatteryType.setSelection(BatteryType);
    }

    public int getRecordingTimeout() {
        int ret;
        try {
            ret = Integer.parseInt(this.RecordingTimeout.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }
    public void setRecordingTimeout(int RecordingTimeout) {
        this.RecordingTimeout.setText(String.valueOf(RecordingTimeout));
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab2, container, false);

        //baud rate
        dropdownBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);
        itemsBaudRate = new String[]{"300",
                "1200",
                "2400",
                "4800",
                "9600",
                "14400",
                "19200",
                "28800",
                "38400",
                "57600",
                "115200",
                "230400"};
        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        // Tool tip
        view.findViewById(R.id.txtViewBaudRate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the altimeter baud rate. Be carefull you might not be able to communicate
                        .text(getResources().getString(R.string.txtViewBaudRate_tooltip))
                        .show();
            }
        });
        // nbr of measures to determine apogee
        ApogeeMeasures = (EditText) view.findViewById(R.id.editTxtApogeeMeasures);
        // Tool tip
        ApogeeMeasures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Number of measures to determine the apogee
                        .text(getResources().getString(R.string.editTxtApogeeMeasures_tooltip))
                        .show();
            }
        });
        // altimeter resolution
        dropdownAltimeterResolution = (Spinner) view.findViewById(R.id.spinnerAltimeterResolution);
        itemsAltimeterResolution = new String[]{"ULTRALOWPOWER", "STANDARD", "HIGHRES", "ULTRAHIGHRES"};
        ArrayAdapter<String> adapterAltimeterResolution = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsAltimeterResolution);
        dropdownAltimeterResolution.setAdapter(adapterAltimeterResolution);

        // Tool tip
        view.findViewById(R.id.txtViewAltimeterResolution).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the altimeter resolution. The faster you rocket goes the lower it has to be
                        .text(getResources().getString(R.string.txtViewAltimeterResolution_tooltip))
                        .show();
            }
        });
        //Altimeter external eeprom size
        txtViewEEpromSize = (TextView) view.findViewById(R.id.txtViewEEpromSize);
        dropdownEEpromSize = (Spinner) view.findViewById(R.id.spinnerEEpromSize);
        itemsEEpromSize = new String[]{"32", "64", "128", "256", "512", "1024"};
        ArrayAdapter<String> adapterEEpromSize = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
        dropdownEEpromSize.setAdapter(adapterEEpromSize);

        if (lAltiCfg != null) {
            if (lAltiCfg.getAltimeterName().equals("AltiServo") ||
                    lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                dropdownEEpromSize.setVisibility(View.INVISIBLE);
                txtViewEEpromSize.setVisibility(View.INVISIBLE);
            } else {
                dropdownEEpromSize.setVisibility(View.VISIBLE);
                txtViewEEpromSize.setVisibility(View.VISIBLE);
            }
        }
        // Tool tip
        view.findViewById(R.id.txtViewEEpromSize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose the altimeter eeprom size depending on which eeprom is used
                        .text(getResources().getString(R.string.txtViewEEpromSize_tooltip))
                        .show();
            }
        });
        // Switch beeping on or off
        // 0 is On and 1 is Off
        dropdownBeepOnOff = (Spinner) view.findViewById(R.id.spinnerBeepOnOff);
        //On
        //Off
        itemsBeepOnOff = new String[]{getResources().getString(R.string.config_on),
                getResources().getString(R.string.config_off)};
        ArrayAdapter<String> adapterBeepOnOff = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsBeepOnOff);
        dropdownBeepOnOff.setAdapter(adapterBeepOnOff);

        // Telemetry type
        txtViewTelemetryType = (TextView) view.findViewById(R.id.txtViewTelemetryType);
        dropdownTelemetryType = (Spinner) view.findViewById(R.id.spinnerTelemetryType);
        //Fast,Average, Slow, VerySlow
        itemsTelemetryType = new String[]{"Fast", "Average", "Slow", "VerySlow"};
        ArrayAdapter<String> adapterTelemetryType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsTelemetryType);
        dropdownTelemetryType.setAdapter(adapterTelemetryType);

        if (lAltiCfg != null) {
            if (lAltiCfg.getAltimeterName().equals("AltiServo") ||
                    lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                dropdownTelemetryType.setVisibility(View.INVISIBLE);
                txtViewTelemetryType.setVisibility(View.INVISIBLE);
            } else {
                dropdownTelemetryType.setVisibility(View.VISIBLE);
                txtViewTelemetryType.setVisibility(View.VISIBLE);
            }
        }

        // Tool tip
        txtViewTelemetryType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose if you want to record the temperature
                        .text("Choose the type of telemetry module you use")
                        .show();
            }
        });
        // supersonic delay on/off
        dropdownSupersonicDelay = (Spinner) view.findViewById(R.id.spinnerSupersonicDelay);
        //Off
        //On
        itemsSupersonicDelayOnOff = new String[]{getResources().getString(R.string.config_off), getResources().getString(R.string.config_on)};
        ArrayAdapter<String> adapterSupersonicDelayOnOff = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsSupersonicDelayOnOff);
        dropdownSupersonicDelay.setAdapter(adapterSupersonicDelayOnOff);

        // nbr of meters to stop recording altitude
        EndRecordAltitude = (EditText) view.findViewById(R.id.editTxtEndRecordAltitude);
        // Tool tip
        EndRecordAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //At which altitude do you consider that we have landed
                        .text(getResources().getString(R.string.EndRecordAltitude_tooltip))
                        .show();
            }
        });
        // nbr of meters to consider that we have a lift off
        LiftOffAltitude = (EditText) view.findViewById(R.id.editTxtLiftOffAltitude);

        // Tool tip
        LiftOffAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //At which altitude do you consider that we have lift off
                        .text(getResources().getString(R.string.LiftOffAltitude_tooltip))
                        .show();
            }
        });
        dropdownBatteryType = (Spinner) view.findViewById(R.id.spinnerBatteryType);
        //"Unknown",
        itemsBatteryType = new String[]{getResources().getString(R.string.config_unknown),
                "2S (7.4 Volts)", "9 Volts", "3S (11.1 Volts)"};
        ArrayAdapter<String> adapterBatteryType = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, itemsBatteryType);
        dropdownBatteryType.setAdapter(adapterBatteryType);

        // Tool tip
        view.findViewById(R.id.txtViewBatteryType).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter battery type used to make sure that we do not discharge it too much
                        .text(getResources().getString(R.string.txtViewBatteryType_tooltip))
                        .show();
            }
        });

        //Max recording time in seconds
        RecordingTimeout =(EditText) view.findViewById(R.id.editTxtRecordingTimeOut);
        if (lAltiCfg != null) {
            dropdownBaudRate.setSelection(lAltiCfg.arrayIndex(itemsBaudRate, String.valueOf(lAltiCfg.getConnectionSpeed())));
            ApogeeMeasures.setText(String.valueOf(lAltiCfg.getNbrOfMeasuresForApogee()));
            dropdownAltimeterResolution.setSelection(lAltiCfg.getAltimeterResolution());
            dropdownEEpromSize.setSelection(lAltiCfg.arrayIndex(itemsEEpromSize, String.valueOf(lAltiCfg.getEepromSize())));
            dropdownBeepOnOff.setSelection(lAltiCfg.getBeepOnOff());
            dropdownTelemetryType.setSelection(lAltiCfg.getTelemetryType());
            dropdownSupersonicDelay.setSelection(lAltiCfg.getSupersonicDelay());
            EndRecordAltitude.setText(String.valueOf(lAltiCfg.getEndRecordAltitude()));
            LiftOffAltitude.setText(String.valueOf(lAltiCfg.getLiftOffAltitude()));
            dropdownBatteryType.setSelection(lAltiCfg.getBatteryType());
            RecordingTimeout.setText(String.valueOf(lAltiCfg.getRecordingTimeout()));
        }
        ViewCreated = true;
        return view;
    }
}
