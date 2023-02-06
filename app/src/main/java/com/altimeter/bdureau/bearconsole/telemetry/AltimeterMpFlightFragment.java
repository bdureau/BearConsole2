package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display altimeter realtime flight graph
 * @author: boris.dureau@neuf.fr
 **/
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class AltimeterMpFlightFragment extends Fragment {
    private boolean ViewCreated = false;
    ConsoleApplication lBT;
    private CheckBox cbLiftOff, cbApogee, cbMainChute, cbLanded;
    private TextView txtCurrentAltitude, txtMaxAltitude, txtMainAltitude, txtLandedAltitude, txtLiftOffAltitude;
    private TextView txtLandedTime, txtMaxSpeedTime, txtMaxAltitudeTime, txtLiftOffTime, txtMainChuteTime;
    private LineChart mChart;

    LineData data;
    ArrayList<ILineDataSet> dataSets;
    ArrayList<Entry> yValues;

    public AltimeterMpFlightFragment(ConsoleApplication bt) {
        lBT = bt;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    public boolean isLiftOffChecked() {
        if(ViewCreated)
            return this.cbLiftOff.isChecked();
        else
            return false;
    }

    public boolean isLandedChecked() {
        if(ViewCreated)
            return this.cbLanded.isChecked();
        else
            return false;
    }

    public boolean isApogeeChecked() {
        if(ViewCreated)
            return this.cbApogee.isChecked();
        else
            return false;
    }

    public boolean isMainChuteChecked() {
        if(ViewCreated)
            return this.cbMainChute.isChecked();
        else
            return false;
    }

    public void setLiftOffEnabled(boolean flag) {
        if(ViewCreated)
            cbLiftOff.setEnabled(flag);
    }

    public void setLiftOffChecked(boolean flag) {
        if(ViewCreated)
            cbLiftOff.setChecked(flag);
    }


    public void setCurrentAltitude (String altitude) {
        if(ViewCreated)
            this.txtCurrentAltitude.setText(altitude);
    }


    public void setLiftOffTime(String time){
        if(ViewCreated)
            this.txtLiftOffTime.setText(time);
    }

    public void setApogeeEnable(boolean flag) {
        if(ViewCreated)
            this.cbApogee.setEnabled(flag);
    }

    public void setApogeeChecked(boolean flag) {
        if(ViewCreated)
            this.cbApogee.setChecked(flag);
    }

    public void setMaxAltitudeTime( String value) {
        if(ViewCreated)
            this.txtMaxAltitudeTime.setText(value);
    }

    public void setMaxAltitude(String value) {
        if(ViewCreated)
            this.txtMaxAltitude.setText(value);
    }

    public void setMainChuteTime(String value) {
        if(ViewCreated)
            this.txtMainChuteTime.setText(value);
    }

    public void setMainChuteEnabled(boolean flag ) {
        if(ViewCreated)
            this.cbMainChute.setEnabled(flag);
    }

    public void setMainChuteChecked(boolean flag ) {
        if(ViewCreated)
            this.cbMainChute.setChecked(flag);
    }
    public void setMainAltitude(String value) {
        if(ViewCreated)
            this.txtMainAltitude.setText(value);
    }

    public void setLandedEnabled(boolean flag ) {
        if(ViewCreated)
            this.cbLanded.setEnabled(flag);
    }
    public void setLandedChecked(boolean flag ) {
        if(ViewCreated)
            this.cbLanded.setChecked(flag);
    }

    public void setLandedAltitude(String value) {
        if(ViewCreated)
            this.txtLandedAltitude.setText(value);
    }
    public String getLandedAltitude() {
        if(ViewCreated)
            return this.txtCurrentAltitude.getText()+"";
        else
            return "";
    }
    public void setLandedTime(String value) {
        if(ViewCreated)
            this.txtLandedTime.setText(value);
    }

    public void plotYvalues(ArrayList<Entry> yValues) {
        LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");

        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setLabel(getResources().getString(R.string.altitude));

        this.dataSets.clear();
        this.dataSets.add(set1);

        this.data = new LineData(this.dataSets);
        this.mChart.clear();
        this.mChart.setData(this.data);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_telemetry_mp_tab0, container, false);

        cbLiftOff = (CheckBox) view.findViewById(R.id.checkBoxLiftoff);
        cbLiftOff.setEnabled(false);
        cbApogee = (CheckBox) view.findViewById(R.id.checkBoxApogee);
        cbApogee.setEnabled(false);
        cbMainChute = (CheckBox) view.findViewById(R.id.checkBoxMainchute);
        cbMainChute.setEnabled(false);
        cbLanded = (CheckBox) view.findViewById(R.id.checkBoxLanded);
        cbLanded.setEnabled(false);

        txtCurrentAltitude = (TextView) view.findViewById(R.id.textViewCurrentAltitude);
        txtMaxAltitude = (TextView) view.findViewById(R.id.textViewApogeeAltitude);

        txtLandedTime = (TextView) view.findViewById(R.id.textViewLandedTime);

        txtMaxSpeedTime = (TextView) view.findViewById(R.id.textViewMaxSpeedTime);
        txtMaxAltitudeTime = (TextView) view.findViewById(R.id.textViewApogeeTime);
        txtLiftOffTime = (TextView) view.findViewById(R.id.textViewLiftoffTime);
        txtMainChuteTime = (TextView) view.findViewById(R.id.textViewMainChuteTime);
        txtMainAltitude = (TextView) view.findViewById(R.id.textViewMainChuteAltitude);
        txtLandedAltitude = (TextView) view.findViewById(R.id.textViewLandedAltitude);
        txtLiftOffAltitude = (TextView) view.findViewById(R.id.textViewLiftoffAltitude);

        // Read the application config
        lBT.getAppConf().ReadConfig();

        int graphBackColor;//= Color.WHITE;
        graphBackColor = lBT.getAppConf().ConvertColor(Integer.parseInt(lBT.getAppConf().getGraphBackColor()));

        int fontSize;
        fontSize = lBT.getAppConf().ConvertFont(Integer.parseInt(lBT.getAppConf().getFontSize()));

        int axisColor;//=Color.BLACK;
        axisColor = lBT.getAppConf().ConvertColor(Integer.parseInt(lBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        String myUnits = "";

        yValues = new ArrayList<>();
        yValues.add(new Entry(0, 0));
        //yValues.add(new Entry(1,0));
        //altitude
        LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.altitude));
        mChart = (LineChart) view.findViewById(R.id.telemetryChartView);

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setScaleMinima(0, 0);
        dataSets = new ArrayList<>();
        dataSets.add(set1);

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        Description desc = new Description();
        desc.setText(getResources().getString(R.string.tel_telemetry));
        mChart.setDescription(desc);

        ViewCreated = true;
        return view;
    }
}
