package com.altimeter.bdureau.bearconsole.config.AppConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.AppConfigData;

public class AppConfig3Fragment extends Fragment {
    private Spinner spMapColor, spMapType;
    private CheckBox cbAllowManualRecording, cbUseOpenMap;
    private boolean ViewCreated = false;
    private ConsoleApplication BT;
    private AppConfigData appConfigData;

    public AppConfig3Fragment(ConsoleApplication lBT,
                              AppConfigData cfgData) {
        BT = lBT;
        appConfigData = cfgData;
    }

    public int getMapColor() {
        return (int) this.spMapColor.getSelectedItemId();
    }

    public void setMapColor(int value) {
        this.spMapColor.setSelection(value);
    }

    public int getMapType() {
        return (int) this.spMapType.getSelectedItemId();
    }

    public void setMapType(int value) {
        this.spMapType.setSelection(value);
    }

    public boolean getAllowManualRecording() {
        return cbAllowManualRecording.isChecked();
    }

    public void setAllowManualRecording(boolean value) {
        cbAllowManualRecording.setChecked(value);
    }

    public boolean getUseOpenMap() { return cbUseOpenMap.isChecked();}

    public void setUseOpenMap(boolean value) {
        cbUseOpenMap.setChecked(value);
        if (cbUseOpenMap.isChecked())
            spMapType.setEnabled(false);
        else
            spMapType.setEnabled(true);
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_app_config_tab3, container, false);
        // map color
        spMapColor = (Spinner) view.findViewById(R.id.spinnerMapColor);
        ArrayAdapter<String> adapterMapColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spMapColor.setAdapter(adapterMapColor);

        // map type
        spMapType = (Spinner) view.findViewById(R.id.spinnerMapType);
        ArrayAdapter<String> adapterMapType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsMap());
        spMapType.setAdapter(adapterMapType);

        spMapColor.setSelection(BT.getAppConf().getMapColor());
        spMapType.setSelection(BT.getAppConf().getMapType());

        // allow manual recording
        cbAllowManualRecording = (CheckBox) view.findViewById(R.id.checkBoxAllowManualRecording);
        cbAllowManualRecording.setChecked(BT.getAppConf().getManualRecording());
        //use OpenMap or GoogleMap
        cbUseOpenMap = (CheckBox) view.findViewById(R.id.checkBoxUseOpenMap);
        cbUseOpenMap.setChecked(BT.getAppConf().getUseOpenMap());
        // Check if we enable Map type or not which is only available for GoogleMap
        if (cbUseOpenMap.isChecked())
            spMapType.setEnabled(false);
        else
            spMapType.setEnabled(true);
        cbUseOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbUseOpenMap.isChecked())
                    spMapType.setEnabled(false);
                else
                    spMapType.setEnabled(true);
            }
        });

        ViewCreated = true;
        return view;
    }
}