package com.altimeter.bdureau.bearconsole;
/**
 *   @description:
 *   @author: boris.dureau@neuf.fr
 **/
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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

    private CheckBox cbLiftOff, cbApogee, cbMainChute, cbLanded;
    private TextView txtCurrentAltitude,txtMaxAltitude,  txtMainAltitude, txtLandedAltitude, txtLiftOffAltitude;
    private TextView txtLandedTime,txtMaxSpeedTime, txtMaxAltitudeTime, txtLiftOffTime, txtMainChuteTime;
    ConsoleApplication myBT ;
    Thread rocketTelemetry;
    ChartView chartView;
    private FlightData myflight=null;
    XYPlot plot;
    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime =0;
    private double FEET_IN_METER = 1;

    int altitudeTime=0;
    int altitude =0;

    boolean telemetry = true;
    //Button startTelemetryButton, stopTelemetryButton;
    Button dismissButton;

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

                            //int altitudeTime = (int)(System.currentTimeMillis()-LiftOffTime);
                            int altitude = (int) (Integer.parseInt((String)msg.obj)* FEET_IN_METER);
                            myflight.AddToFlight(altitudeTime,altitude,"Telemetry" );

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
                    if (!cbLiftOff.isChecked())
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0 || LiftOffTime >0)
                        {
                            cbLiftOff.setEnabled(true);
                            cbLiftOff.setChecked(true);
                            cbLiftOff.setEnabled(false);
                            if (LiftOffTime==0)
                                LiftOffTime= System.currentTimeMillis();
                            txtLiftOffTime.setText("0 ms");
                        }

                    break;
                case 3:
                    // Value 3 apogee fired yes/no
                    if(!cbApogee.isChecked())
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0)
                        {
                            cbApogee.setEnabled(true);
                            cbApogee.setChecked(true);
                            cbApogee.setEnabled(false);
                            txtMaxAltitudeTime.setText((int)(System.currentTimeMillis()-LiftOffTime) + " ms");
                        }

                    break;
                case 4:
                    //Value 4 apogee altitude
                    txtMaxAltitude.setText((String)msg.obj);
                    break;
                case 5:
                    //value 5 main fired yes/no
                    if(!cbMainChute.isChecked())
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0)
                        {
                            txtMainChuteTime.setText((System.currentTimeMillis() - LiftOffTime) + " ms");
                            cbMainChute.setEnabled(true);
                            cbMainChute.setChecked(true);
                            cbMainChute.setEnabled(false);
                        }

                    break;
                case 6:
                    // value 6 main altitude
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                    {
                        if (cbMainChute.isChecked()) {
                            txtMainAltitude.setText((String) msg.obj);
                        }
                    }

                    break;
                case 7:
                    //have we landed
                    if(!cbLanded.isChecked())
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                        if(Integer.parseInt((String)msg.obj) > 0)
                        {
                            cbLanded.setEnabled(true);
                            cbLanded.setChecked(true);
                            cbLanded.setEnabled(false);
                            txtLandedAltitude.setText(txtCurrentAltitude.getText());
                            txtLandedTime.setText((System.currentTimeMillis()-LiftOffTime)+" ms");
                        }

                    break;
                case 8:
                    // Value 8 contain the sample time
                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                    {
                        if(Integer.parseInt((String)msg.obj) > 0) {
                            altitudeTime = Integer.parseInt((String)msg.obj);
                        }
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
        //textView = (TextView) findViewById(R.id.textView);
        cbLiftOff = (CheckBox)findViewById(R.id.checkBoxLiftoff);
        cbLiftOff.setEnabled(false);
        cbApogee = (CheckBox)findViewById(R.id.checkBoxApogee);
        cbApogee.setEnabled(false);
        cbMainChute = (CheckBox)findViewById(R.id.checkBoxMainchute);
        cbMainChute.setEnabled(false);
        cbLanded = (CheckBox)findViewById(R.id.checkBoxLanded);
        cbLanded.setEnabled(false);

        txtCurrentAltitude =(TextView)findViewById(R.id.textViewCurrentAltitude);
        txtMaxAltitude =(TextView)findViewById(R.id.textViewApogeeAltitude);
        //txtFlightTime = (TextView)findViewById(R.id.text);
        txtLandedTime = (TextView) findViewById(R.id.textViewLandedTime);
        //startTelemetryButton = (Button) findViewById(R.id.buttonStartTelemetry);
        //stopTelemetryButton = (Button) findViewById(R.id.buttonStopTelemetry);
        dismissButton = (Button) findViewById(R.id.butDismiss);
        txtMaxSpeedTime = (TextView) findViewById(R.id.textViewMaxSpeedTime);
        txtMaxAltitudeTime = (TextView) findViewById(R.id.textViewApogeeTime);
        txtLiftOffTime= (TextView) findViewById(R.id.textViewLiftoffTime);
        txtMainChuteTime= (TextView) findViewById(R.id.textViewMainChuteTime);
        txtMainAltitude = (TextView) findViewById(R.id.textViewMainChuteAltitude);
        txtLandedAltitude = (TextView) findViewById(R.id.textViewLandedAltitude);
        txtLiftOffAltitude = (TextView) findViewById(R.id.textViewLiftoffAltitude);
        myBT.setHandler(handler);
        //stopTelemetryButton.setEnabled(false);


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

        if(myBT.getAppConf().getUnitsValue().equals("Meters"))
        {
            FEET_IN_METER =1;
        }
        else
        {
            FEET_IN_METER = 3.28084;
        }
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


        chartView = (ChartView) findViewById(R.id.telemetryChartView);
        chartView.setChart(chart);
        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (telemetry) {
                    telemetry = false;
                    myBT.write("h;\n".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                finish();      //exit the activity
            }
        });
    }

    public void startTelemetry() {
        telemetry= true;
        //startTelemetryButton.setEnabled(false);
        //stopTelemetryButton.setEnabled(true);
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
    public void onClickStartTelemetry(View view) {

        telemetry= true;
        //startTelemetryButton.setEnabled(false);
        //stopTelemetryButton.setEnabled(true);
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
       myBT.write("h;\n".toString());

       myBT.setExit(true);

       myflight.ClearFlight();
       telemetry =false;
       //stopTelemetryButton.setEnabled(false);

       myBT.clearInput();
       myBT.flush();


       //startTelemetryButton.setEnabled(true);

   }


    @Override
    protected void onStop() {
        //msg("On stop");
        super.onStop();
        if (telemetry) {
            telemetry = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;\n".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage =myBT.ReadResult();
    }
}
