package com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltimeterFcFlightFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Live telemetry tab drawn with AFreeChart (Android 7): altitude curve during the flight and
 *  flight events (lift off, apogee, main deployment, landing) with their time and altitude.
 * ============================================================================
 */

import androidx.annotation.NonNull;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.Flight.FlightView.ChartView;
import com.altimeter.bdureau.bearconsole.R;

import org.afree.chart.AFreeChart;
import org.afree.chart.ChartFactory;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;


public class AltimeterFcFlightFragment extends Fragment {
    private boolean ViewCreated = false;
    ConsoleApplication lBT;
    private CheckBox cbLiftOff, cbApogee, cbMainChute, cbLanded;
    private TextView txtCurrentAltitude, txtMaxAltitude, txtMainAltitude, txtLandedAltitude, txtLiftOffAltitude;
    private TextView txtLandedTime, txtMaxSpeedTime, txtMaxAltitudeTime, txtLiftOffTime, txtMainChuteTime;

    private ChartView chartView;
    private AFreeChart mChart = null;
    private XYPlot plot;

    /** Required by the framework to re-create the fragment (configuration change, process restore). */
    public AltimeterFcFlightFragment() {
        super();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // After a re-creation by the framework the dependencies were not given by a
        // constructor: the application object is always available from the context
        if (lBT == null) {
            lBT = (ConsoleApplication) context.getApplicationContext();
        }
    }

    public AltimeterFcFlightFragment(ConsoleApplication bt) {
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


    public void setLiftOffChecked(boolean flag) {
        if(ViewCreated) {
            cbLiftOff.setEnabled(true);
            cbLiftOff.setChecked(flag);
            cbLiftOff.setEnabled(false);
        }
    }


    public void setCurrentAltitude (String altitude) {
        if(ViewCreated)
            this.txtCurrentAltitude.setText(altitude);
    }


    public void setLiftOffTime(String time){
        if(ViewCreated)
            this.txtLiftOffTime.setText(time);
    }


    public void setApogeeChecked(boolean flag) {
        if(ViewCreated) {
            this.cbApogee.setEnabled(true);
            this.cbApogee.setChecked(flag);
            this.cbApogee.setEnabled(false);
        }
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


    public void setMainChuteChecked(boolean flag ) {
        if(ViewCreated) {
            this.cbMainChute.setEnabled(true);
            this.cbMainChute.setChecked(flag);
            this.cbMainChute.setEnabled(false);
        }
    }
    public void setMainAltitude(String value) {
        if(ViewCreated)
            this.txtMainAltitude.setText(value);
    }

    public void setLandedChecked(boolean flag ) {
        if(ViewCreated) {
            this.cbLanded.setEnabled(true);
            this.cbLanded.setChecked(flag);
            this.cbLanded.setEnabled(false);
        }
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

    public void plotYvalues(XYSeriesCollection flightData) {
        plot.setDataset(0, flightData);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_telemetry_fc_tab0, container, false);

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

        int graphBackColor = lBT.getAppConf().ConvertColor(lBT.getAppConf().getGraphBackColor());

        int fontSize = lBT.getAppConf().ConvertFont(lBT.getAppConf().getFontSize());

        int axisColor = lBT.getAppConf().ConvertColor(lBT.getAppConf().getGraphColor());

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        String myUnits = "";

        Font font = new Font("Dialog", Typeface.NORMAL,fontSize);

        mChart = ChartFactory.createXYLineChart(
                getResources().getString(R.string.Altitude_time),
                getResources().getString(R.string.Time_fv),
                getResources().getString(R.string.Altitude) + " (" + myUnits + ")",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        mChart.getTitle().setFont(font);
        // set the background color for the chart...
        mChart.setBackgroundPaintType(new SolidColor(graphBackColor));

        // get a reference to the plot for further customisation...
        plot = mChart.getXYPlot();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        plot.setBackgroundPaintType(new SolidColor(graphBackColor));
        plot.setOutlinePaintType(new SolidColor(Color.YELLOW));
        plot.setDomainZeroBaselinePaintType(new SolidColor(Color.GREEN));
        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));

        final ValueAxis Xaxis = plot.getDomainAxis();
        Xaxis.setAutoRange(true);
        Xaxis.setAxisLinePaintType(new SolidColor(axisColor));

        final ValueAxis YAxis = plot.getRangeAxis();
        YAxis.setAxisLinePaintType(new SolidColor(axisColor));


        Xaxis.setTickLabelFont(font);
        Xaxis.setLabelFont(font);

        YAxis.setTickLabelFont(font);
        YAxis.setLabelFont(font);

        //Xaxis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));

        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        YAxis.setLabelPaintType(new SolidColor(labelColor));
        YAxis.setTickLabelPaintType(new SolidColor(nbrColor));
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);


        chartView = (ChartView) view.findViewById(R.id.telemetryChartView);
        chartView.setChart(mChart);


        ViewCreated = true;
        return view;
    }
}
