package com.altimeter.bdureau.bearconsole.telemetry;
/**
 *   @description: This will display real time telemetry providing
 *   that you have a telemetry long range module. This activity display the telemetry
 *   using the AFreeChart library. If you are using Android 8 or greater you should
 *   use the MPAndroidChart otherwise it will crash your application
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import java.io.IOException;
import java.util.Locale;

import android.graphics.Typeface;

import org.afree.chart.ChartFactory;
import org.afree.chart.AFreeChart;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;

import org.afree.data.xy.XYSeriesCollection;

import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;


import android.graphics.Color;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.ChartView;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.R;

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
    private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;

    int altitudeTime=0;
    int altitude =0;

    boolean telemetry = true;
    boolean liftOffSaid = false;
    boolean apogeeSaid = false;
    boolean landedSaid = false;
    boolean mainSaid = false;
    //Button startTelemetryButton, stopTelemetryButton;
    Button dismissButton;

    private TextToSpeech mTTS;

    Handler handler = new Handler () {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current altitude

                    if(((String)msg.obj).matches("\\d+(?:\\.\\d+)?"))
                    {
                        if (cbLiftOff.isChecked()&& !cbLanded.isChecked()) {

                            //int altitudeTime = (int)(System.currentTimeMillis()-LiftOffTime);
                            int altitude = (int) (Integer.parseInt((String)msg.obj)* FEET_IN_METER);
                            //txtCurrentAltitude.setText(String.valueOf((String)msg.obj));
                            txtCurrentAltitude.setText(String.valueOf(altitude) );
                            myflight.AddToFlight(altitudeTime,altitude,"Telemetry" );

                            //plot every seconde
                            if ((altitudeTime - lastPlotTime )>1000) {
                                lastPlotTime = altitudeTime;
                                XYSeriesCollection flightData;
                                flightData = myflight.GetFlightData("Telemetry");
                                plot.setDataset(0, flightData);
                                //mTTS.speak("rocket has lift off", TextToSpeech.QUEUE_FLUSH,null);
                            }
                            // Tell altitude every 5 secondes
                            if ((altitudeTime - lastSpeakTime )>5000 && liftOffSaid) {
                               /* if(Locale.getDefault().getLanguage()== "en")
                                    mTTS.speak("altitude " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                else if(Locale.getDefault().getLanguage()== "fr")
                                    mTTS.speak("altitude " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                else
                                    mTTS.speak("altitude " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);*/
                                mTTS.speak(getResources().getString(R.string.altitude) + " " + String.valueOf(altitude) +  " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                lastSpeakTime = altitudeTime;
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
                            if(! liftOffSaid ){
                               /* if(Locale.getDefault().getLanguage()== "en")
                                    mTTS.speak("lift off", TextToSpeech.QUEUE_FLUSH,null);
                                else if(Locale.getDefault().getLanguage()== "fr")
                                    mTTS.speak("décollage", TextToSpeech.QUEUE_FLUSH,null);
                                else
                                    mTTS.speak("lift off", TextToSpeech.QUEUE_FLUSH,null);*/
                                mTTS.speak(getResources().getString(R.string.lift_off) , TextToSpeech.QUEUE_FLUSH, null);
                                liftOffSaid =true;
                            }
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
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        //txtMaxAltitude.setText((String) msg.obj);
                        int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                        txtMaxAltitude.setText(String.valueOf(altitude) );
                        if (!apogeeSaid) {
                           /* if (Locale.getDefault().getLanguage() == "en")
                                mTTS.speak("apogee " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                            else if (Locale.getDefault().getLanguage() == "fr")
                                mTTS.speak("apogée à " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                            else
                                mTTS.speak("apogee " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);*/
                            mTTS.speak(getResources().getString(R.string.telemetry_apogee) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                            apogeeSaid = true;
                        }
                    }
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
                            //txtMainAltitude.setText((String) msg.obj);
                            int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                            txtMainAltitude.setText(String.valueOf(altitude) );
                            if(! mainSaid ) {
                                //if(Locale.getDefault().getLanguage()== "en")
                                    mTTS.speak(getResources().getString(R.string.main_deployed) +" " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                /*else if(Locale.getDefault().getLanguage()== "fr")
                                    mTTS.speak("déploiement du parachute principal à " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                else
                                    mTTS.speak("main chute has deployed at " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
*/
                                mainSaid = true;
                            }
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
                            if(! landedSaid ) {
                               /* if(Locale.getDefault().getLanguage()== "en")
                                    mTTS.speak("rocket has landed", TextToSpeech.QUEUE_FLUSH, null);
                                else if(Locale.getDefault().getLanguage()== "fr")
                                    mTTS.speak("la fusée a attérie", TextToSpeech.QUEUE_FLUSH, null);
                                else
                                    mTTS.speak("rocket has landed", TextToSpeech.QUEUE_FLUSH, null);*/
                                mTTS.speak(getResources().getString(R.string.rocket_has_landed), TextToSpeech.QUEUE_FLUSH, null);
                                landedSaid = true;
                            }
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

        //init text to speech
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status ==TextToSpeech.SUCCESS ){
                    int result=0;
                    if(Locale.getDefault().getLanguage()== "en")
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if(Locale.getDefault().getLanguage()== "fr")
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);


                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("TTS", "Language not supported");
                    }else {

                    }
                } else {
                    Log.e("TTS","Init failed");
                }
            }
        });
        mTTS.setPitch(1.0f);
        mTTS.setSpeechRate(1.0f);


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

        txtLandedTime = (TextView) findViewById(R.id.textViewLandedTime);

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
        if (myBT.getAppConf().getUnits().equals("0")) {
            //Meters
            myUnits = getResources().getString(R.string.Meters_fview);
            FEET_IN_METER =1;
        }
        else {
            //Feet
            myUnits = getResources().getString(R.string.Feet_fview);
            FEET_IN_METER = 3.28084;
        }
       /* if(myBT.getAppConf().getUnitsValue().equals(getResources().getString(R.string.Meters_fview)))
        {
            FEET_IN_METER =1;
        }
        else
        {
            FEET_IN_METER = 3.28084;
        }*/
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
                //turn off telemetry
                myBT.flush();
                myBT.clearInput();
                myBT.write("y0;\n".toString());

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
                    myBT.ReadResult(100000);
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
                    myBT.ReadResult(100000);
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

        myMessage =myBT.ReadResult(100000);
    }
}
