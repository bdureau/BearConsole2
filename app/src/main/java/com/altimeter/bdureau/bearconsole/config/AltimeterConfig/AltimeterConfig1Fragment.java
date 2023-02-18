package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.github.florent37.viewtooltip.ViewTooltip;

public class AltimeterConfig1Fragment extends Fragment {
    private static final String TAG = "AltimeterConfig1Fragment";
    private AltiConfigData lAltiCfg;

    private Spinner dropdownOut1, dropdownOut2, dropdownOut3, dropdownOut4;
    private EditText OutDelay1, OutDelay2, OutDelay3, OutDelay4;
    private EditText MainAltitude;
    private TextView txtOut1, txtOut2, txtOut3, txtViewDelay3, txtOut4, txtViewDelay4, txtViewDelay2, txtViewDelay1;

    private boolean ViewCreated = false;

    public AltimeterConfig1Fragment(AltiConfigData cfg) {
        lAltiCfg = cfg;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    public int getDropdownOut1() {
        return (int) this.dropdownOut1.getSelectedItemId();
    }

    public void setDropdownOut1(int Out1) {
        this.dropdownOut1.setSelection(Out1);
    }

    public int getDropdownOut2() {
        return (int) this.dropdownOut2.getSelectedItemId();
    }

    public void setDropdownOut2(int Out2) {
        this.dropdownOut2.setSelection(Out2);
    }

    public int getDropdownOut3() {
        return (int) this.dropdownOut3.getSelectedItemId();
    }

    public void setDropdownOut3(int Out3) {
        this.dropdownOut3.setSelection(Out3);
    }

    public int getDropdownOut4() {
        if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo"))
            return (int) this.dropdownOut4.getSelectedItemId();
        else
            return -1;
    }

    public void setDropdownOut4(int Out4) {
        if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
            this.dropdownOut4.setSelection(Out4);
            this.dropdownOut4.setVisibility(View.VISIBLE);
            txtOut4.setVisibility(View.VISIBLE);
        } else {
            this.dropdownOut4.setVisibility(View.INVISIBLE);
            txtOut4.setVisibility(View.INVISIBLE);
        }

    }

    public void setOutDelay1(int OutDelay1) {
        this.OutDelay1.setText(String.valueOf(OutDelay1));
    }

    public int getOutDelay1() {
        int ret;
        try {
            ret = Integer.parseInt(OutDelay1.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setOutDelay2(int OutDelay2) {
        this.OutDelay2.setText(String.valueOf(OutDelay2));
    }

    public int getOutDelay2() {
        int ret;
        try {
            ret = Integer.parseInt(OutDelay2.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setOutDelay3(int OutDelay3) {
        this.OutDelay3.setText(String.valueOf(OutDelay3));
    }

    public int getOutDelay3() {
        int ret;
        try {
            ret = Integer.parseInt(OutDelay3.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setOutDelay4(int OutDelay4) {
        if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
            this.OutDelay4.setText(String.valueOf(OutDelay4));
            this.OutDelay4.setVisibility(View.VISIBLE);
            txtViewDelay4.setVisibility(View.VISIBLE);
        } else {
            this.OutDelay4.setVisibility(View.INVISIBLE);
            txtViewDelay4.setVisibility(View.INVISIBLE);
        }

    }

    public int getOutDelay4() {
        if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
            int ret;
            try {
                ret = Integer.parseInt(OutDelay4.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        } else
            return -1;
    }

    public void setMainAltitude(int MainAltitude) {
        this.MainAltitude.setText(String.valueOf(MainAltitude));
    }

    public int getMainAltitude() {
        int ret;
        try {
            ret = Integer.parseInt(MainAltitude.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab1, container, false);

        //Output 1
        dropdownOut1 = (Spinner) view.findViewById(R.id.spinnerOut1);
        //"Main", "Drogue", "Timer", "Disabled", "Landing", "Liftoff","Altitude"
        String[] items3 = new String[]{getResources().getString(R.string.main_config),
                getResources().getString(R.string.drogue_config),
                getResources().getString(R.string.timer_config),
                getResources().getString(R.string.disabled_config),
                getResources().getString(R.string.landing_config),
                getResources().getString(R.string.liftoff_config),
                getResources().getString(R.string.altitude_config)
        };
        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items3);

        dropdownOut1.setAdapter(adapter3);
        dropdownOut1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                //Altitude
                if (selectedItem.equals(getResources().getString(R.string.altitude_config))) {
                    // Altitude1
                    txtViewDelay1.setText(getResources().getString(R.string.altitude1));
                } else {
                    //Delay1
                    txtViewDelay1.setText(getResources().getString(R.string.delay1));
                }
            } // to close the onItemSelected

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Output 2
        dropdownOut2 = (Spinner) view.findViewById(R.id.spinnerOut2);

        ArrayAdapter<String> adapter4 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownOut2.setAdapter(adapter4);
        dropdownOut2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                //Altitude
                if (selectedItem.equals(getResources().getString(R.string.altitude_config))) {
                    // Altitude2
                    txtViewDelay2.setText(getResources().getString(R.string.altitude2));
                } else {
                    //Delay2
                    txtViewDelay2.setText(getResources().getString(R.string.delay2));
                }
            } // to close the onItemSelected

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //Output 3
        dropdownOut3 = (Spinner) view.findViewById(R.id.spinnerOut3);

        ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownOut3.setAdapter(adapter5);
        dropdownOut3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                //Altitude
                if (selectedItem.equals(getResources().getString(R.string.altitude_config))) {
                    // Altitude3
                    txtViewDelay3.setText(getResources().getString(R.string.altitude3));
                } else {
                    //Delay2
                    txtViewDelay3.setText(getResources().getString(R.string.delay3));
                }
            } // to close the onItemSelected

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //Output 4
        dropdownOut4 = (Spinner) view.findViewById(R.id.spinnerOut4);

        ArrayAdapter<String> adapter6 = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownOut4.setAdapter(adapter6);
        dropdownOut4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                //Altitude
                if (selectedItem.equals(getResources().getString(R.string.altitude_config))) {
                    // Altitude4
                    txtViewDelay4.setText(getResources().getString(R.string.altitude4));
                } else {
                    //Delay2
                    txtViewDelay4.setText(getResources().getString(R.string.delay4));
                }
            } // to close the onItemSelected

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        OutDelay1 = (EditText) view.findViewById(R.id.editTxtDelay1);
        OutDelay2 = (EditText) view.findViewById(R.id.editTxtDelay2);
        OutDelay3 = (EditText) view.findViewById(R.id.editTxtDelay3);
        OutDelay4 = (EditText) view.findViewById(R.id.editTxtDelay4);

        MainAltitude = (EditText) view.findViewById(R.id.editTxtMainAltitude);
        // Tool tip
        MainAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter the altitude for the main chute
                        .text(getResources().getString(R.string.main_altitude_tooltip))
                        .show();
            }
        });
        // Tool tip
        OutDelay1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter the firing delay in ms for the 1st output
                        .text(getResources().getString(R.string.OutDelay1_tooltip))
                        .show();
            }
        });

        // Tool tip
        OutDelay2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Enter the firing delay in ms for the 2nd output
                        .text(getResources().getString(R.string.OutDelay2_tooltip))
                        .show();
            }
        });

        txtOut1 = (TextView) view.findViewById(R.id.txtOut1);
        txtOut2 = (TextView) view.findViewById(R.id.txtOut2);
        txtOut3 = (TextView) view.findViewById(R.id.txtOut3);
        txtViewDelay1 = (TextView) view.findViewById(R.id.txtViewDelay1);
        txtViewDelay2 = (TextView) view.findViewById(R.id.txtViewDelay2);
        txtViewDelay3 = (TextView) view.findViewById(R.id.txtViewDelay3);
        txtOut4 = (TextView) view.findViewById(R.id.txtOut4);
        txtViewDelay4 = (TextView) view.findViewById(R.id.txtViewDelay4);

        // Tool tip
        txtOut1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose what you want to do with the 1st output
                        .text(getResources().getString(R.string.txtOut1_tooltip))
                        .show();
            }
        });
        // Tool tip
        txtOut2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewTooltip
                        .on(v)
                        .color(Color.BLACK)
                        .position(ViewTooltip.Position.TOP)
                        //Choose what you want to do with the 2nd output
                        .text(getResources().getString(R.string.txtOut2_tooltip))
                        .show();
            }
        });

        Log.d("AltiConfig", "Before Attempting to set config");
        if (lAltiCfg != null) {
            Log.d("AltiConfig", "Attempting to set config");
            dropdownOut1.setSelection(lAltiCfg.getOutput1());
            dropdownOut2.setSelection(lAltiCfg.getOutput2());
            if (!lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                dropdownOut3.setSelection(lAltiCfg.getOutput3());
                dropdownOut3.setVisibility(View.VISIBLE);
                txtOut3.setVisibility(View.VISIBLE);

                // Tool tip
                txtOut3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewTooltip
                                .on(v)
                                .color(Color.BLACK)
                                .position(ViewTooltip.Position.TOP)
                                //Choose what you want to do with the 3rd output
                                .text(getResources().getString(R.string.txtOut3_tooltip))
                                .show();
                    }
                });
            } else {
                dropdownOut3.setVisibility(View.INVISIBLE);
                txtOut3.setVisibility(View.INVISIBLE);
            }
            if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                dropdownOut4.setSelection(lAltiCfg.getOutput4());
                dropdownOut4.setVisibility(View.VISIBLE);
                txtOut4.setVisibility(View.VISIBLE);
                // Tool tip
                txtOut4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewTooltip
                                .on(v)
                                .color(Color.BLACK)
                                .position(ViewTooltip.Position.TOP)
                                //Choose what you want to do with the 4th output
                                .text(getResources().getString(R.string.txtOut4_tooltip))
                                .show();
                    }
                });
            } else {
                dropdownOut4.setVisibility(View.INVISIBLE);
                txtOut4.setVisibility(View.INVISIBLE);
            }
            OutDelay1.setText(String.valueOf(lAltiCfg.getOutput1Delay()));
            OutDelay2.setText(String.valueOf(lAltiCfg.getOutput2Delay()));
            if (!lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                OutDelay3.setText(String.valueOf(lAltiCfg.getOutput3Delay()));
                // Tool tip
                OutDelay3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewTooltip
                                .on(v)
                                .color(Color.BLACK)
                                .position(ViewTooltip.Position.TOP)
                                //Enter the firing delay in ms for the 3rd output
                                .text(getResources().getString(R.string.OutDelay3_tooltip))
                                .show();
                    }
                });
            } else {
                OutDelay3.setVisibility(View.INVISIBLE);
                txtViewDelay3.setVisibility(View.INVISIBLE);
            }
            if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                OutDelay4.setText(String.valueOf(lAltiCfg.getOutput4Delay()));
                OutDelay4.setVisibility(View.VISIBLE);
                txtViewDelay4.setVisibility(View.VISIBLE);
                //Tooltip
                OutDelay4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewTooltip
                                .on(v)
                                .color(Color.BLACK)
                                .position(ViewTooltip.Position.TOP)
                                //Enter the firing delay in ms for the 4th output
                                .text(getResources().getString(R.string.OutDelay4_tooltip))
                                .show();
                    }
                });
            } else {
                OutDelay4.setVisibility(View.INVISIBLE);
                txtViewDelay4.setVisibility(View.INVISIBLE);
            }

            MainAltitude.setText(String.valueOf(lAltiCfg.getMainAltitude()));
        }
        ViewCreated = true;
        return view;
    }

}
