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

public class AppConfig1Fragment extends Fragment {
    private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
    private Spinner spConnectionType, spGraphicsLibType;
    private CheckBox cbAllowMainDrogue, cbFullUSBSupport;
    private ConsoleApplication BT;
    private boolean ViewCreated = false;
    private AppConfigData appConfigData;

    public AppConfig1Fragment(ConsoleApplication lBT,
                              AppConfigData cfgData) {
        BT = lBT;
        appConfigData = cfgData;
    }

    public int getAppLanguage() {
        return (int) this.spAppLanguage.getSelectedItemId();
    }

    public void setAppLanguage(int value) {
        this.spAppLanguage.setSelection(value);
    }

    public int getGraphColor() {
        return (int) this.spGraphColor.getSelectedItemId();
    }

    public void setGraphColor(int value) {
        this.spGraphColor.setSelection(value);
    }

    public int getAppUnit() {
        return (int) this.spAppUnit.getSelectedItemId();
    }

    public void setAppUnit(int value) {
        this.spAppUnit.setSelection(value);
    }

    public int getGraphBackColor() {
        return (int) this.spGraphBackColor.getSelectedItemId();
    }

    public void setGraphBackColor(int value) {
        this.spGraphBackColor.setSelection(value);
    }


    public int getFontSize() {
        return (int) this.spFontSize.getSelectedItemId();
    }

    public void setFontSize(int value) {
        this.spFontSize.setSelection(value);
    }

    public int getBaudRate() {
        return (int) this.spBaudRate.getSelectedItemId();
    }

    public void setBaudRate(int value) {
        this.spBaudRate.setSelection(value);
    }

    public int getConnectionType() {
        return (int) this.spConnectionType.getSelectedItemId();
    }

    public void setConnectionType(int value) {
        this.spConnectionType.setSelection(value);
    }

    public int getGraphicsLibType() {
        return (int) this.spGraphicsLibType.getSelectedItemId();
    }

    public void setGraphicsLibType(int value) {
        this.spGraphicsLibType.setSelection(value);
    }

    public boolean getAllowMainDrogue() {
        return cbAllowMainDrogue.isChecked();
    }

    public void setAllowMainDrogue(boolean value) {
        cbAllowMainDrogue.setChecked(value);
    }

    public boolean getFullUSBSupport() {
        return cbFullUSBSupport.isChecked();
    }



    public void setFullUSBSupport(boolean value) {
        cbFullUSBSupport.setChecked(value);
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.activity_app_config_part1, container, false);
        //Language
        spAppLanguage = (Spinner) view.findViewById(R.id.spinnerLanguage);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
        spAppLanguage.setAdapter(adapter);
        spAppLanguage.setEnabled(false); //disable it for the moment because it is causing troubles

        // graph color
        spGraphColor = (Spinner) view.findViewById(R.id.spinnerGraphColor);
        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spGraphColor.setAdapter(adapterColor);

        // graph back color
        spGraphBackColor = (Spinner) view.findViewById(R.id.spinnerGraphBackColor);
        ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spGraphBackColor.setAdapter(adapterGraphColor);

        //units
        spAppUnit = (Spinner) view.findViewById(R.id.spinnerUnits);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
        spAppUnit.setAdapter(adapter2);

        //font size
        spFontSize = (Spinner) view.findViewById(R.id.spinnerFontSize);

        ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
        spFontSize.setAdapter(adapterFontSize);

        //Baud Rate
        spBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
        spBaudRate.setAdapter(adapterBaudRate);

        //connection type
        spConnectionType = (Spinner) view.findViewById(R.id.spinnerConnectionType);

        ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
        spConnectionType.setAdapter(adapterConnectionType);

        //Graphics lib type
        spGraphicsLibType = (Spinner) view.findViewById(R.id.spinnerGraphicLibType);
        ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
        spGraphicsLibType.setAdapter(adapterGraphicsLibType);

        //Allow multiple main and drogue
        cbAllowMainDrogue = (CheckBox) view.findViewById(R.id.checkBoxAllowMultipleMainDrogue);

        //Allow only telemetry via USB
        cbFullUSBSupport = (CheckBox) view.findViewById(R.id.checkBoxFullUSBSupport);

        spAppLanguage.setSelection(BT.getAppConf().getApplicationLanguage());
        spAppUnit.setSelection(BT.getAppConf().getUnits());
        spGraphColor.setSelection(BT.getAppConf().getGraphColor());
        spGraphBackColor.setSelection(BT.getAppConf().getGraphBackColor());

        spFontSize.setSelection((BT.getAppConf().getFontSize()) - 8);
        spBaudRate.setSelection(BT.getAppConf().getBaudRate());
        spConnectionType.setSelection(BT.getAppConf().getConnectionType());
        spGraphicsLibType.setSelection(BT.getAppConf().getGraphicsLibType());
        if (BT.getAppConf().getAllowMultipleDrogueMain()) {
            cbAllowMainDrogue.setChecked(true);
        } else {
            cbAllowMainDrogue.setChecked(false);
        }
        if (BT.getAppConf().getFullUSBSupport()) {
            cbFullUSBSupport.setChecked(true);
        } else {
            cbFullUSBSupport.setChecked(false);
        }
        ViewCreated = true;
        return view;
    }

}