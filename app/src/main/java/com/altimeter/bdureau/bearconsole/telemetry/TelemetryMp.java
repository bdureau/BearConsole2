package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display real time telemetry providing
 * that you have a telemetry long range module. This activity display the telemetry
 * using the MPAndroidChart library.
 * @author: boris.dureau@neuf.fr
 **/

import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;

import android.graphics.Typeface;

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

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class TelemetryMp extends AppCompatActivity {

    private CheckBox cbLiftOff, cbApogee, cbMainChute, cbLanded;
    private TextView txtCurrentAltitude, txtMaxAltitude, txtMainAltitude, txtLandedAltitude, txtLiftOffAltitude;
    private TextView txtLandedTime, txtMaxSpeedTime, txtMaxAltitudeTime, txtLiftOffTime, txtMainChuteTime;
    ConsoleApplication myBT;
    Thread rocketTelemetry;

    private LineChart mChart;

    LineData data;
    ArrayList<ILineDataSet> dataSets;
    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;
    private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;
    ArrayList<Entry> yValues;
    int altitudeTime = 0;
    //int altitude = 0;

    boolean telemetry = true;
    boolean liftOffSaid = false;
    boolean apogeeSaid = false;
    boolean landedSaid = false;
    boolean mainSaid = false;

    Button dismissButton;

    private TextToSpeech mTTS;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current altitude
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (cbLiftOff.isChecked() && !cbLanded.isChecked()) {
                            int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                            txtCurrentAltitude.setText(String.valueOf(altitude) );
                            yValues.add(new Entry(altitudeTime, altitude));

                            //plot every seconde
                            if ((altitudeTime - lastPlotTime) > 1000) {
                                lastPlotTime = altitudeTime;

                                LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");

                                set1.setDrawValues(false);
                                set1.setDrawCircles(false);
                                set1.setLabel(getResources().getString(R.string.altitude));

                                dataSets.clear();
                                dataSets.add(set1);

                                data = new LineData(dataSets);
                                mChart.clear();
                                mChart.setData(data);
                            }
                            // Tell altitude every 5 secondes
                            if ((altitudeTime - lastSpeakTime) > 5000 && liftOffSaid) {
                                if (myBT.getAppConf().getAltitude_event().equals("true")) {
                                       /* if (Locale.getDefault().getLanguage() == "en")
                                            mTTS.speak("altitude " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue() , TextToSpeech.QUEUE_FLUSH, null);
                                        else if (Locale.getDefault().getLanguage() == "fr")
                                            mTTS.speak("altitude " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                        else
                                            mTTS.speak("altitude " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);*/
                                    mTTS.speak(getResources().getString(R.string.altitude) + " " + String.valueOf(altitude) +  " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                }
                                lastSpeakTime = altitudeTime;
                            }
                        }
                    }
                    break;
                case 2:
                    // Value 2 lift off yes/no
                    if (!cbLiftOff.isChecked())
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                            if (Integer.parseInt((String) msg.obj) > 0 || LiftOffTime > 0) {
                                cbLiftOff.setEnabled(true);
                                cbLiftOff.setChecked(true);
                                cbLiftOff.setEnabled(false);
                                if (LiftOffTime == 0)
                                    LiftOffTime = System.currentTimeMillis();
                                txtLiftOffTime.setText("0 ms");
                                if (!liftOffSaid) {
                                    if (myBT.getAppConf().getLiftOff_event().equals("true")) {

                                        mTTS.speak(getResources().getString(R.string.lift_off) , TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    liftOffSaid = true;
                                }
                            }

                    break;
                case 3:
                    // Value 3 apogee fired yes/no
                    if (!cbApogee.isChecked())
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                            if (Integer.parseInt((String) msg.obj) > 0) {
                                cbApogee.setEnabled(true);
                                cbApogee.setChecked(true);
                                cbApogee.setEnabled(false);
                                txtMaxAltitudeTime.setText((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                            }

                    break;
                case 4:
                    //Value 4 apogee altitude
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);

                        if (cbApogee.isChecked()) {
                            txtMaxAltitude.setText(String.valueOf(altitude));
                            if (!apogeeSaid) {
                                //first check if say it is enabled
                                if (myBT.getAppConf().getApogee_altitude().equals("true")) {
                                    mTTS.speak(getResources().getString(R.string.telemetry_apogee) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                }
                                apogeeSaid = true;
                            }
                        }
                    }
                    break;
                case 5:
                    //value 5 main fired yes/no
                    if (!cbMainChute.isChecked())
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                            if (Integer.parseInt((String) msg.obj) > 0) {
                                txtMainChuteTime.setText((System.currentTimeMillis() - LiftOffTime) + " ms");
                                cbMainChute.setEnabled(true);
                                cbMainChute.setChecked(true);
                                cbMainChute.setEnabled(false);
                            }

                    break;
                case 6:
                    // value 6 main altitude
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (cbMainChute.isChecked()) {
                            int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                            txtMainAltitude.setText(String.valueOf(altitude) );

                            if (!mainSaid) {
                                if (myBT.getAppConf().getMain_event().equals("true")) {

                                    mTTS.speak(getResources().getString(R.string.main_deployed) +" " + String.valueOf(altitude) + " " +myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                }
                                mainSaid = true;
                            }
                        }
                    }

                    break;
                case 7:
                    //have we landed
                    if (!cbLanded.isChecked())
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                            if (Integer.parseInt((String) msg.obj) > 0) {
                                cbLanded.setEnabled(true);
                                cbLanded.setChecked(true);
                                cbLanded.setEnabled(false);
                                txtLandedAltitude.setText(txtCurrentAltitude.getText());
                                txtLandedTime.setText((System.currentTimeMillis() - LiftOffTime) + " ms");
                                if (!landedSaid) {
                                    if (myBT.getAppConf().getLanding_event().equals("true")) {

                                        mTTS.speak(getResources().getString(R.string.rocket_has_landed), TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    landedSaid = true;
                                }
                            }

                    break;
                case 8:
                    // Value 8 contain the sample time
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (Integer.parseInt((String) msg.obj) > 0) {
                            altitudeTime = Integer.parseInt((String) msg.obj);
                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry_mp);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();


        cbLiftOff = (CheckBox) findViewById(R.id.checkBoxLiftoff);
        cbLiftOff.setEnabled(false);
        cbApogee = (CheckBox) findViewById(R.id.checkBoxApogee);
        cbApogee.setEnabled(false);
        cbMainChute = (CheckBox) findViewById(R.id.checkBoxMainchute);
        cbMainChute.setEnabled(false);
        cbLanded = (CheckBox) findViewById(R.id.checkBoxLanded);
        cbLanded.setEnabled(false);

        txtCurrentAltitude = (TextView) findViewById(R.id.textViewCurrentAltitude);
        txtMaxAltitude = (TextView) findViewById(R.id.textViewApogeeAltitude);

        txtLandedTime = (TextView) findViewById(R.id.textViewLandedTime);

        dismissButton = (Button) findViewById(R.id.butDismiss);
        txtMaxSpeedTime = (TextView) findViewById(R.id.textViewMaxSpeedTime);
        txtMaxAltitudeTime = (TextView) findViewById(R.id.textViewApogeeTime);
        txtLiftOffTime = (TextView) findViewById(R.id.textViewLiftoffTime);
        txtMainChuteTime = (TextView) findViewById(R.id.textViewMainChuteTime);
        txtMainAltitude = (TextView) findViewById(R.id.textViewMainChuteAltitude);
        txtLandedAltitude = (TextView) findViewById(R.id.textViewLandedAltitude);
        txtLiftOffAltitude = (TextView) findViewById(R.id.textViewLiftoffAltitude);
        myBT.setHandler(handler);

        // Read the application config
        myBT.getAppConf().ReadConfig();

        //init text to speech
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;

                    if (Locale.getDefault().getLanguage() == "en")
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if (Locale.getDefault().getLanguage() == "fr")
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else if (Locale.getDefault().getLanguage() == "tr")
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage() == "nl")
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage() == "es")
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage() == "it")
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage() == "hu")
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage() == "ru")
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);

                    if (!myBT.getAppConf().getTelemetryVoice().equals("")) {
                        Log.d("Voice", myBT.getAppConf().getTelemetryVoice());
                        String[] itemsVoices;
                        String items = "";
                        int i = 0;
                        try {
                            for (Voice tmpVoice : mTTS.getVoices()) {

                                if (tmpVoice.getName().startsWith(Locale.getDefault().getLanguage())) {
                                    Log.d("Voice", tmpVoice.getName());
                                    if (myBT.getAppConf().getTelemetryVoice().equals(i + "")) {
                                        mTTS.setVoice(tmpVoice);
                                        Log.d("Voice", "Found voice");
                                        break;
                                    }
                                    i++;
                                }
                            }
                        } catch (Exception e) {

                        }


                    }

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {

                    }
                } else {
                    Log.e("TTS", "Init failed");
                }
            }
        });
        mTTS.setPitch(1.0f);
        mTTS.setSpeechRate(1.0f);

        int graphBackColor;//= Color.WHITE;
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

        int axisColor;//=Color.BLACK;
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        String myUnits = "";
        /*if (myBT.getAppConf().getUnits().equals("0"))
            //Meters
            myUnits = getResources().getString(R.string.Meters_fview);
        else
            //Feet
            myUnits = getResources().getString(R.string.Feet_fview);*/
        if (myBT.getAppConf().getUnits().equals("0")) {
        //if (myBT.getAppConf().getUnitsValue().equals(getResources().getString(R.string.Meters_fview))) {
            FEET_IN_METER = 1;
            myUnits = getResources().getString(R.string.Meters_fview);
        } else {
            FEET_IN_METER = 3.28084;
            myUnits = getResources().getString(R.string.Feet_fview);
        }
        //font

        yValues = new ArrayList<>();
        yValues.add(new Entry(0, 0));
        //yValues.add(new Entry(1,0));
        //altitude
        LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.altitude));
        mChart = (LineChart) findViewById(R.id.telemetryChartView);

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
        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (telemetry) {
                    telemetry = false;
                    myBT.write("h;".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                //turn off telemetry
                myBT.flush();
                myBT.clearInput();
                myBT.write("y0;".toString());

                finish();      //exit the activity
            }
        });
    }

    public void startTelemetry() {
        telemetry = true;

        lastPlotTime = 0;
        myBT.initFlightData();

        LiftOffTime = 0;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!telemetry) break;
                    myBT.ReadResult(100000);
                }
            }
        };

        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();

    }

    public void onClickStartTelemetry(View view) {

        telemetry = true;

        lastPlotTime = 0;
        myBT.initFlightData();


        LiftOffTime = 0;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!telemetry) break;
                    myBT.ReadResult(100000);
                }
            }
        };

        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();


    }

    public void onClickStopTelemetry(View view) {
        myBT.write("h;".toString());

        myBT.setExit(true);

        telemetry = false;

        myBT.clearInput();
        myBT.flush();

    }


    @Override
    protected void onStop() {
        //msg("On stop");
        super.onStop();
        if (telemetry) {
            telemetry = false;
            myBT.write("h;".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(100000);
    }
}
