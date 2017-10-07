package com.altimeter.bdureau.bearconsole;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import java.io.IOException;
import android.content.Intent;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.afree.chart.ChartFactory;
import org.afree.chart.AFreeChart;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;

import org.afree.data.category.DefaultCategoryDataset;
import org.afree.data.xy.XYSeriesCollection;

import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;


import android.graphics.Color;

public class Telemetry extends AppCompatActivity {
    TextView textView;
    private CheckBox cbLiftOff, cbApogee, cbMainChute, cbLanded;
    private TextView txtCurrentAltitude,txtMaxAltitude, txtFlightTime;
    ConsoleApplication myBT ;
    Thread rocketTelemetry;
    ChartView chartView;
    private FlightData myflight=null;
    XYPlot plot;
    private long LiftOffTime = 0;
    private int lastPlotTime =0;

    boolean telemetry = true;
    Button startTelemetryButton, stopTelemetryButton;
    //private MyHandler mHandler;
    Handler handler = new Handler () {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current altitude
                    txtCurrentAltitude.setText(String.valueOf((String)msg.obj));
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                    {
                        if (cbLiftOff.isChecked()&& !cbLanded.isChecked()) {
                            //myflight= myBT.getFlightData();
                            int altitudeTime = (int)(System.currentTimeMillis()-LiftOffTime);
                            myflight.AddToFlight(altitudeTime,Integer.parseInt((String)msg.obj),"Telemetry" );
                            //myBT.setFlightData(myflight);
                            //plot every seconde
                            if ((altitudeTime - lastPlotTime )>1000) {
                                lastPlotTime = altitudeTime;
                                XYSeriesCollection flightData;
                                flightData = myflight.GetFlightData("Telemetry");
                                plot.setDataset(0, flightData);

                            }
                        }
                    }
                    break;
                case 2:
                    // Value 2 lift off yes/no
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0 || LiftOffTime >0)
                        {
                            cbLiftOff.setChecked(true);
                            if (LiftOffTime==0)
                                LiftOffTime= System.currentTimeMillis();
                        }
                        else {
                            cbLiftOff.setChecked(false);
                        }
                    break;
                case 3:
                    // Value 3 apogee fired yes/no
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0)
                        {
                            cbApogee.setChecked(true);
                        }
                        else {
                            cbApogee.setChecked(false);
                        }
                    break;
                case 4:
                    //Value 4 apogee altitude
                    txtMaxAltitude.setText(String.valueOf(String.valueOf((String)msg.obj)));
                    break;
                case 5:
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0)
                        {
                            cbMainChute.setChecked(true);
                        }
                        else
                        {
                            cbMainChute.setChecked(false);
                        }
                    break;
                case 6:
                    break;
                case 7:
                    //have we landed
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0)
                        {
                            cbLanded.setChecked(true);
                            txtFlightTime.setText((System.currentTimeMillis()-LiftOffTime)+"ms");
                        }
                        else
                        {
                            cbLanded.setChecked(false);
                        }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        textView = (TextView) findViewById(R.id.textView);
        cbLiftOff = (CheckBox)findViewById(R.id.checkBoxLiftoff);
        cbApogee = (CheckBox)findViewById(R.id.checkBoxApogee);
        cbMainChute = (CheckBox)findViewById(R.id.checkBoxMainchute);
        cbLanded = (CheckBox)findViewById(R.id.checkBoxLanded);
        txtCurrentAltitude =(TextView)findViewById(R.id.textViewCurrentAltitude);
        txtMaxAltitude =(TextView)findViewById(R.id.textViewMaxAltitude);
        txtFlightTime = (TextView)findViewById(R.id.textViewFlightDuration);

        startTelemetryButton = (Button) findViewById(R.id.buttonStartTelemetry);
        stopTelemetryButton = (Button) findViewById(R.id.buttonStopTelemetry);

        myBT.setHandler(handler);
        stopTelemetryButton.setEnabled(false);


        // Read the application config
        myBT.getAppConf().ReadConfig();
        int graphBackColor;//= Color.WHITE;
        graphBackColor =myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

        int axisColor;//=Color.BLACK;
        axisColor=myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor=Color.BLACK;

        int nbrColor=Color.BLACK;
        String myUnits="";
        if (myBT.getAppConf().getUnits().equals("0"))
            //Meters
            myUnits=getResources().getString(R.string.Meters_fview);
        else
            //Feet
            myUnits = getResources().getString(R.string.Feet_fview);

        //font
        Font font = new Font("Dialog", Typeface.NORMAL,fontSize);

        AFreeChart chart = ChartFactory.createXYLineChart(
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

        chart.getTitle().setFont(font);
        // set the background color for the chart...


        chart.setBackgroundPaintType(new SolidColor(graphBackColor));

        // get a reference to the plot for further customisation...
        plot = chart.getXYPlot();

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


        //plot.setDataset(0, flightData);
        chartView = (ChartView) findViewById(R.id.telemetryChartView);
        chartView.setChart(chart);
    }

    public void onClickStartTelemetry(View view) {

        telemetry= true;
        startTelemetryButton.setEnabled(false);
        stopTelemetryButton.setEnabled(true);
        lastPlotTime=0;
        myBT.initFlightData();

        myflight= myBT.getFlightData();
        LiftOffTime =0;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true){
                    if(!telemetry) break;
                    myBT.ReadResult();
                }
            }
        };

        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();


    }
   public void onClickStopTelemetry(View view) {
       myBT.setExit(true);

       myflight.ClearFlight();
       telemetry =false;
       stopTelemetryButton.setEnabled(false);

       myBT.clearInput();
       myBT.flush();


       startTelemetryButton.setEnabled(true);

   }
}
