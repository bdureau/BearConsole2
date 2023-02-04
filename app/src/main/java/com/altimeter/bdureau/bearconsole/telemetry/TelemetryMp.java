package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display real time telemetry providing
 * that you have a telemetry long range module. This activity display the telemetry
 * using the MPAndroidChart library.
 * @author: boris.dureau@neuf.fr
 **/

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class TelemetryMp extends AppCompatActivity {

    public String TAG = "TelemetryMp.class";
    private static ViewPager mViewPager;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    public LocationBroadCastReceiver receiver = null;
    SectionsStatusPageAdapter adapter;
    Tab0TelemetryFragment statusPage0 = null;
    Tab1TelemetryFragment statusPage1 = null;
    Tab2TelemetryFragment statusPage2 = null;
    Tab3TelemetryFragment statusPage3 = null;

    Marker marker, markerDest;
    Polyline polyline1 = null;
    public Double rocketLatitude = 48.8698;
    public Double rocketLongitude = 2.2190;
    LatLng dest = new LatLng(rocketLatitude, rocketLongitude);

    private ConsoleApplication myBT;
    Thread rocketTelemetry;

    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;
    private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;
    //ArrayList<Entry> yValues;
    int altitudeTime = 0;

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
                        if (statusPage0.isViewCreated())
                            if (statusPage0.cbLiftOff.isChecked() && !statusPage0.cbLanded.isChecked()) {
                                int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                                statusPage0.txtCurrentAltitude.setText(String.valueOf(altitude));
                                statusPage0.yValues.add(new Entry(altitudeTime, altitude));

                                //plot every seconde
                                if ((altitudeTime - lastPlotTime) > 1000) {
                                    lastPlotTime = altitudeTime;

                                    LineDataSet set1 = new LineDataSet(statusPage0.yValues, "Altitude/Time");

                                    set1.setDrawValues(false);
                                    set1.setDrawCircles(false);
                                    set1.setLabel(getResources().getString(R.string.altitude));

                                    statusPage0.dataSets.clear();
                                    statusPage0.dataSets.add(set1);

                                    statusPage0.data = new LineData(statusPage0.dataSets);
                                    statusPage0.mChart.clear();
                                    statusPage0.mChart.setData(statusPage0.data);
                                }
                                // Tell altitude every 5 secondes
                                if ((altitudeTime - lastSpeakTime) > 5000 && liftOffSaid) {
                                    if (myBT.getAppConf().getAltitude_event().equals("true")) {
                                        mTTS.speak(getResources().getString(R.string.altitude) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    lastSpeakTime = altitudeTime;
                                }
                            }
                    }
                    break;
                case 2:
                    // Value 2 lift off yes/no
                    if (statusPage0.isViewCreated())
                        if (!statusPage0.cbLiftOff.isChecked())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0 || LiftOffTime > 0) {
                                    statusPage0.cbLiftOff.setEnabled(true);
                                    statusPage0.cbLiftOff.setChecked(true);
                                    statusPage0.cbLiftOff.setEnabled(false);
                                    if (LiftOffTime == 0)
                                        LiftOffTime = System.currentTimeMillis();
                                    statusPage0.txtLiftOffTime.setText("0 ms");
                                    if (!liftOffSaid) {
                                        if (myBT.getAppConf().getLiftOff_event().equals("true")) {
                                            mTTS.speak(getResources().getString(R.string.lift_off), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        liftOffSaid = true;
                                    }
                                }

                    break;
                case 3:
                    // Value 3 apogee fired yes/no
                    if (statusPage0.isViewCreated())
                        if (!statusPage0.cbApogee.isChecked())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0) {
                                    statusPage0.cbApogee.setEnabled(true);
                                    statusPage0.cbApogee.setChecked(true);
                                    statusPage0.cbApogee.setEnabled(false);
                                    statusPage0.txtMaxAltitudeTime.setText((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                                }

                    break;
                case 4:
                    //Value 4 apogee altitude
                    if (statusPage0.isViewCreated())
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                            int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);

                            if (statusPage0.cbApogee.isChecked()) {
                                statusPage0.txtMaxAltitude.setText(String.valueOf(altitude));
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
                    if (statusPage0.isViewCreated())
                        if (!statusPage0.cbMainChute.isChecked())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0) {
                                    statusPage0.txtMainChuteTime.setText((System.currentTimeMillis() - LiftOffTime) + " ms");
                                    statusPage0.cbMainChute.setEnabled(true);
                                    statusPage0.cbMainChute.setChecked(true);
                                    statusPage0.cbMainChute.setEnabled(false);
                                }

                    break;
                case 6:
                    // value 6 main altitude
                    if (statusPage0.isViewCreated())
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                            if (statusPage0.cbMainChute.isChecked()) {
                                int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                                statusPage0.txtMainAltitude.setText(String.valueOf(altitude));

                                if (!mainSaid) {
                                    if (myBT.getAppConf().getMain_event().equals("true")) {
                                        mTTS.speak(getResources().getString(R.string.main_deployed) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    mainSaid = true;
                                }
                            }
                        }

                    break;
                case 7:
                    //have we landed
                    if (statusPage0.isViewCreated())
                        if (!statusPage0.cbLanded.isChecked())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0) {
                                    statusPage0.cbLanded.setEnabled(true);
                                    statusPage0.cbLanded.setChecked(true);
                                    statusPage0.cbLanded.setEnabled(false);
                                    statusPage0.txtLandedAltitude.setText(statusPage0.txtCurrentAltitude.getText());
                                    statusPage0.txtLandedTime.setText((System.currentTimeMillis() - LiftOffTime) + " ms");
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
                case 9:
                    // Value 9 contains the output 1 status
                    statusPage1.setOutput1Status((String) msg.obj);
                    break;
                case 10:
                    // Value 10 contains the output 2 status
                    statusPage1.setOutput2Status((String) msg.obj);
                    break;
                case 11:
                    // Value 11 contains the output 3 status
                    statusPage1.setOutput3Status((String) msg.obj);
                    break;
                case 12:
                    //Value 12 contains the output 4 status
                    statusPage1.setOutput4Status((String) msg.obj);
                    break;
                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        statusPage1.setVoltage(voltage + " Volts");
                    } else {
                        statusPage1.setVoltage("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the temperature
                    statusPage1.setTemperature((String) msg.obj + "°C");
                    break;

                case 15:
                    //Value 15 contains the EEprom usage
                    statusPage1.setEEpromUsage((String) msg.obj + " %");
                    break;

                case 16:
                    //Value 16 contains the number of flight
                    statusPage1.setNbrOfFlight((String) msg.obj);
                    break;
                /*case 18:
                    //Value 18 contains the latitude
                    Log.d("status", "latitude");
                    setLatitudeValue((String) msg.obj );
                    break;*/
                case 18:
                    //Value 18 contains the latitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String latitude = (String) msg.obj;
                        if (latitude.matches("\\d+(?:\\.\\d+)?")) {
                            double latitudeVal = Double.parseDouble(latitude) / 100000;
                            statusPage2.setLatitudeValue("" + latitudeVal);
                            setLatitudeValue(latitude);
                        }
                    }
                    break;
               /* case 19:
                    //Value 19 contains the longitude
                    setLongitudeValue((String) msg.obj );
                    break;*/
                case 19:
                    //Value 19 contains the longitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String longitude = (String) msg.obj;
                        if (longitude.matches("\\d+(?:\\.\\d+)?")) {
                            double longitudeVal = Double.parseDouble(longitude) / 100000;
                            statusPage2.setLongitudeValue("" + longitudeVal);
                            setLongitudeValue(longitude);
                        }
                    }
                    break;
                case 20:
                    //Value 20 contains the number of satellites
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String nbrOfSatellite = (String) msg.obj;
                        if (nbrOfSatellite.matches("\\d+(?:\\.\\d+)?")) {
                            int nbrOfSatelliteVal = Integer.parseInt(nbrOfSatellite);
                            statusPage2.setSatellitesVal("" + nbrOfSatelliteVal);
                        }
                    }
                    break;
                case 21:
                    //Value 21 contains the hdop
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String hdop = (String) msg.obj;
                        if (hdop.matches("\\d+(?:\\.\\d+)?")) {
                            int hdopVal = Integer.parseInt(hdop);
                            statusPage2.setHdopVal("" + hdopVal);
                        }
                    }
                    break;
                case 22:
                    //Value 22 contains the location age
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String locationAge = (String) msg.obj;
                        if (locationAge.matches("\\d+(?:\\.\\d+)?")) {
                            int locationAgeVal = Integer.parseInt(locationAge);
                            statusPage2.setLocationAgeValue("" + locationAgeVal);
                        }
                    }
                    break;
                case 23:
                    //Value 23 contains the GPS altitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSAltitude = (String) msg.obj;
                        if (GPSAltitude.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSAltitudeVal = Integer.parseInt(GPSAltitude);
                            statusPage2.setGPSAltitudeVal("" + GPSAltitudeVal);
                        }
                    }
                    break;
                case 24:
                    // Value 24 contains the GPS Speed
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSSpeed = (String) msg.obj;
                        if (GPSSpeed.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSSpeedVal = Integer.parseInt(GPSSpeed);
                            statusPage2.setGPSSpeedVal("" + GPSSpeedVal);
                        }
                    }
                    break;
                case 25:
                    // Value 25 contains the time for sat acquisition
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String TimeSat = (String) msg.obj;
                        if (TimeSat.matches("\\d+(?:\\.\\d+)?")) {
                            int TimeSatVal = Integer.parseInt(TimeSat);
                            statusPage2.setTimeSatValue(String.format("%.2f", (double) (TimeSatVal / 1000)) + " secs");
                        }
                    }
                    break;

            }
        }
    };

    private void setLatitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Double val = Double.parseDouble(value);
            Log.d("track", "latitude:" + value);
            if (val != 0) {
                rocketLatitude = Double.parseDouble(value) / 100000;
                myBT.getAppConf().setRocketLatitude("" + rocketLatitude);

            }
        }
    }

    private void setLongitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Double val = Double.parseDouble(value);
            Log.d("track", "longitude:" + value);
            if (val != 0) {
                rocketLongitude = Double.parseDouble(value) / 100000;
                myBT.getAppConf().setRocketLongitude("" + rocketLongitude);
                myBT.getAppConf().SaveConfig();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry_mp);

        receiver = new LocationBroadCastReceiver();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                startService();
            }
        } else {
            startService();
        }

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();


        mViewPager = (ViewPager) findViewById(R.id.container);

        setupViewPager(mViewPager);


        dismissButton = (Button) findViewById(R.id.butDismiss);

        myBT.setHandler(handler);

        // Read the application config
        myBT.getAppConf().ReadConfig();
        if (myBT.getAppConf().getRocketLatitude().matches("\\d+(?:\\.\\d+)?"))
            rocketLatitude = Double.parseDouble(myBT.getAppConf().getRocketLatitude());

        if (myBT.getAppConf().getRocketLongitude().matches("\\d+(?:\\.\\d+)?"))
            rocketLongitude = Double.parseDouble(myBT.getAppConf().getRocketLongitude());
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


        String myUnits = "";

        if (myBT.getAppConf().getUnits().equals("0")) {
            //Meters
            FEET_IN_METER = 1;
            myUnits = getResources().getString(R.string.Meters_fview);
        } else {
            //Feet
            FEET_IN_METER = 3.28084;
            myUnits = getResources().getString(R.string.Feet_fview);
        }
        //startTelemetry();

        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (myBT.getConnected() && !telemetry) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            telemetry = true;
            //rocketTelemetry.start();
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        if (!telemetry) break;
                        myBT.ReadResult(10000);
                    }
                }
            };
            rocketTelemetry = new Thread(r);
            rocketTelemetry.start();
        }
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

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        statusPage0 = new Tab0TelemetryFragment(myBT);
        statusPage1 = new Tab1TelemetryFragment(myBT);
        statusPage2 = new Tab2TelemetryFragment(myBT);
        statusPage3 = new Tab3TelemetryFragment(myBT);

        adapter.addFragment(statusPage0, "TAB0");
        adapter.addFragment(statusPage1, "TAB1");

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            adapter.addFragment(statusPage2, "TAB2");
            adapter.addFragment(statusPage3, "TAB3");
        }

        linearDots = findViewById(R.id.idAltiStatusLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewListener);

    }

    public void agregaIndicateDots(int pos, int nbr) {
        dotsSlide = new TextView[nbr];
        linearDots.removeAllViews();

        for (int i = 0; i < dotsSlide.length; i++) {
            dotsSlide[i] = new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if (dotsSlide.length > 0) {
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    public class SectionsStatusPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsStatusPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab0TelemetryFragment extends Fragment {
        private boolean ViewCreated = false;
        ConsoleApplication lBT;
        private CheckBox cbLiftOff, cbApogee, cbMainChute, cbLanded;
        private TextView txtCurrentAltitude, txtMaxAltitude, txtMainAltitude, txtLandedAltitude, txtLiftOffAltitude;
        private TextView txtLandedTime, txtMaxSpeedTime, txtMaxAltitudeTime, txtLiftOffTime, txtMainChuteTime;
        private LineChart mChart;

        LineData data;
        ArrayList<ILineDataSet> dataSets;
        ArrayList<Entry> yValues;

        public Tab0TelemetryFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public boolean isViewCreated() {
            return ViewCreated;
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

    public static class Tab1TelemetryFragment extends Fragment {
        private static final String TAG = "Tab1TelemetryFragment";
        private boolean ViewCreated = false;
        private TextView txtStatusAltiName, txtStatusAltiNameValue;
        private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
        private TextView txtViewAltitude, txtViewVoltage, txtViewLink, txtTemperature, txtEEpromUsage, txtNbrOfFlight;
        private TextView txtViewOutput3, txtViewOutput4, txtViewBatteryVoltage, txtViewEEprom, txtViewFlight;
        ConsoleApplication lBT;

        public Tab1TelemetryFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public void setOutput1Status(String value) {
            if (ViewCreated)
                this.txtViewOutput1Status.setText(outputStatus(value));
        }

        public void setOutput2Status(String value) {
            if (ViewCreated)
                this.txtViewOutput2Status.setText(outputStatus(value));
        }

        public void setOutput3Status(String value) {
            if (ViewCreated)
                this.txtViewOutput3Status.setText(outputStatus(value));
        }

        public void setOutput4Status(String value) {
            if (ViewCreated)
                this.txtViewOutput4Status.setText(outputStatus(value));
        }

        public void setAltitude(String value) {
            if (ViewCreated)
                this.txtViewAltitude.setText(value);
        }

        public void setVoltage(String value) {
            if (ViewCreated)
                this.txtViewVoltage.setText(value);
        }

        public void setTemperature(String value) {
            if (ViewCreated)
                this.txtTemperature.setText(value);
        }

        public void setEEpromUsage(String value) {
            if (ViewCreated)
                this.txtEEpromUsage.setText(value);
        }

        public void setNbrOfFlight(String value) {
            if (ViewCreated)
                this.txtNbrOfFlight.setText(value);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_telemetry_mp_tab1, container, false);

            txtStatusAltiName = (TextView) view.findViewById(R.id.txtStatusAltiName);
            txtStatusAltiNameValue = (TextView) view.findViewById(R.id.txtStatusAltiNameValue);
            txtViewOutput1Status = (TextView) view.findViewById(R.id.txtViewOutput1Status);
            txtViewOutput2Status = (TextView) view.findViewById(R.id.txtViewOutput2Status);
            txtViewOutput3Status = (TextView) view.findViewById(R.id.txtViewOutput3Status);
            txtViewOutput4Status = (TextView) view.findViewById(R.id.txtViewOutput4Status);
            txtViewAltitude = (TextView) view.findViewById(R.id.txtViewAltitude);
            txtViewVoltage = (TextView) view.findViewById(R.id.txtViewVoltage);
            txtViewLink = (TextView) view.findViewById(R.id.txtViewLink);
            txtViewOutput3 = (TextView) view.findViewById(R.id.txtViewOutput3);
            txtViewOutput4 = (TextView) view.findViewById(R.id.txtViewOutput4);

            txtViewBatteryVoltage = (TextView) view.findViewById(R.id.txtViewBatteryVoltage);
            txtTemperature = (TextView) view.findViewById(R.id.txtViewTemperature);
            txtEEpromUsage = (TextView) view.findViewById(R.id.txtViewEEpromUsage);
            txtNbrOfFlight = (TextView) view.findViewById(R.id.txtViewNbrOfFlight);
            txtViewEEprom = (TextView) view.findViewById(R.id.txtViewEEprom);
            txtViewFlight = (TextView) view.findViewById(R.id.txtViewFlight);

            txtStatusAltiNameValue.setText(lBT.getAltiConfigData().getAltimeterName());
            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                txtViewVoltage.setVisibility(View.VISIBLE);
                txtViewBatteryVoltage.setVisibility(View.VISIBLE);
            } else {
                txtViewVoltage.setVisibility(View.INVISIBLE);
                txtViewBatteryVoltage.setVisibility(View.INVISIBLE);
            }
            if (!lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")) {
                txtViewOutput3Status.setVisibility(View.VISIBLE);
                txtViewOutput3.setVisibility(View.VISIBLE);
            } else {
                txtViewOutput3Status.setVisibility(View.INVISIBLE);
                txtViewOutput3.setVisibility(View.INVISIBLE);
            }

            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
                txtViewOutput4Status.setVisibility(View.VISIBLE);
                txtViewOutput4.setVisibility(View.VISIBLE);
            } else {
                txtViewOutput4Status.setVisibility(View.INVISIBLE);
                txtViewOutput4.setVisibility(View.INVISIBLE);
            }
            //hide eeprom
            if (lBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")
                    || lBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
                txtViewEEprom.setVisibility(View.INVISIBLE);
                txtViewFlight.setVisibility(View.INVISIBLE);
                txtEEpromUsage.setVisibility(View.INVISIBLE);
                txtNbrOfFlight.setVisibility(View.INVISIBLE);
            } else {
                txtViewEEprom.setVisibility(View.VISIBLE);
                txtViewFlight.setVisibility(View.VISIBLE);
                txtEEpromUsage.setVisibility(View.VISIBLE);
                txtNbrOfFlight.setVisibility(View.VISIBLE);
            }

            txtViewLink.setText(lBT.getConnectionType());

            ViewCreated = true;
            return view;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            ViewCreated = false;
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }

        private String outputStatus(String msg) {
            String res = "";
            switch (msg) {
                case "0":
                    res = getResources().getString(R.string.no_continuity);
                    break;
                case "1":
                    res = this.getContext().getResources().getString(R.string.continuity);
                    break;
                case "-1":
                    res = getResources().getString(R.string.disabled);
                    break;
            }
            return res;
        }
    }

    public static class Tab2TelemetryFragment extends Fragment {
        private static final String TAG = "Tab2TelemetryFragment";
        private boolean ViewCreated = false;
        private TextView txtViewLatitude, txtViewLongitude, txtViewLatitudeValue, txtViewLongitudeValue;
        private TextView txtViewSatellitesVal, txtViewHdopVal, txtViewGPSAltitudeVal, txtViewGPSSpeedVal;
        private TextView txtViewLocationAgeValue, txtViewTimeSatValue;
        ConsoleApplication lBT;

        public Tab2TelemetryFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public void setLatitudeValue(String value) {
            if (ViewCreated)
                this.txtViewLatitudeValue.setText(value);
        }

        public void setLongitudeValue(String value) {
            if (ViewCreated)
                this.txtViewLongitudeValue.setText(value);
        }

        public void setSatellitesVal(String value) {
            if (ViewCreated)
                this.txtViewSatellitesVal.setText(value);
        }

        public void setHdopVal(String value) {
            if (ViewCreated)
                this.txtViewHdopVal.setText(value);
        }

        public void setGPSAltitudeVal(String value) {
            if (ViewCreated)
                this.txtViewGPSAltitudeVal.setText(value);
        }

        public void setGPSSpeedVal(String value) {
            if (ViewCreated)
                this.txtViewGPSSpeedVal.setText(value);
        }

        public void setLocationAgeValue(String value) {
            if (ViewCreated)
                this.txtViewLocationAgeValue.setText(value);
        }

        public void setTimeSatValue(String value) {
            if (ViewCreated)
                this.txtViewTimeSatValue.setText(value);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab2, container, false);

            txtViewLatitude = (TextView) view.findViewById(R.id.txtViewLatitude);
            txtViewLongitude = (TextView) view.findViewById(R.id.txtViewLongitude);
            txtViewLatitudeValue = (TextView) view.findViewById(R.id.txtViewLatitudeValue);
            txtViewLongitudeValue = (TextView) view.findViewById(R.id.txtViewLongitudeValue);
            txtViewSatellitesVal = (TextView) view.findViewById(R.id.txtViewSatellitesVal);
            txtViewHdopVal = (TextView) view.findViewById(R.id.txtViewHdopVal);
            txtViewGPSAltitudeVal = (TextView) view.findViewById(R.id.txtViewGPSAltitudeVal);
            txtViewGPSSpeedVal = (TextView) view.findViewById(R.id.txtViewGPSSpeedVal);
            txtViewLocationAgeValue = (TextView) view.findViewById(R.id.txtViewLocationAgeValue);
            txtViewTimeSatValue = (TextView) view.findViewById(R.id.txtViewTimeSatValue);

            ViewCreated = true;
            return view;
        }
    }

    public static class Tab3TelemetryFragment extends Fragment {
        private static final String TAG = "Tab3TelemetryFragment";
        private boolean ViewCreated = false;

        public GoogleMap lMap = null;
        ConsoleApplication lBT;
        Button butBack, butShareMap;

        public Tab3TelemetryFragment(ConsoleApplication bt) {
            lBT = bt;
        }

        public GoogleMap getlMap() {
            return lMap;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab3, container, false);

            butBack = (Button) view.findViewById(R.id.butBack);
            butShareMap = (Button) view.findViewById(R.id.butShareMap);
            SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapStatus);

            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if (lMap == null) {
                        lMap = googleMap;
                        lMap.setMapType(Integer.parseInt(lBT.getAppConf().getMapType()));
                    }
                }
            });

            butBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mViewPager.setCurrentItem(0);

                }
            });

            butShareMap.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    takeMapScreenshot();
                }
            });

            ViewCreated = true;
            return view;
        }
        private void takeMapScreenshot() {
            GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                Bitmap bitmap;

                @Override
                public void onSnapshotReady(Bitmap snapshot) {
                    // Callback is called from the main thread, so we can modify the ImageView safely.
                    bitmap = snapshot;
                    shareScreenshot(bitmap);
                }
            };
            lMap.snapshot(callback);
        }

        private void shareScreenshot(Bitmap bitmap) {
            try {
                // Save the screenshot to a file
                String filePath = MediaStore.Images.Media.insertImage(this.getContext().getContentResolver(),
                        bitmap, "Title", null);
                Uri fileUri = Uri.parse(filePath);
                // Share the screenshot
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/*");
                share.putExtra(Intent.EXTRA_STREAM, fileUri);
                startActivity(Intent.createChooser(share, "Share Map screenshot"));
            } catch (Exception e) {
                //Toast.makeText(this, "Error saving/sharing Map screenshot", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    public class LocationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("coordinate", intent.getAction());
            if (intent.getAction().equals("ACT_LOC")) {
                double latitude = intent.getDoubleExtra("latitude", 0f);
                double longitude = intent.getDoubleExtra("longitude", 0f);
                Log.d("coordinate", "latitude is:" + latitude + " longitude is: " + longitude);

                if (statusPage3.getlMap() != null) {
                    LatLng latLng = new LatLng(latitude, longitude);
                    if (marker != null) {
                        marker.setPosition(latLng);
                    } else {
                        BitmapDescriptor manIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_map);
                        marker = statusPage3.getlMap().addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng).icon(manIcon));
                    }

                    if (markerDest != null) {
                        markerDest.setPosition(dest);
                    } else {
                        BitmapDescriptor rocketIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_rocket_map);
                        markerDest = statusPage3.getlMap().addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(dest).icon(rocketIcon));
                    }
                    List<LatLng> coord;
                    coord = new ArrayList();

                    dest = new LatLng(rocketLatitude, rocketLongitude);
                    coord.add(0, dest);

                    coord.add(1, latLng);
                    if (polyline1 == null)
                        polyline1 = statusPage3.getlMap().addPolyline(new PolylineOptions().clickable(false));
                    //Get the line color from the config
                    polyline1.setColor(myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getMapColor())));
                    polyline1.setPoints(coord);
                    if (statusPage3.getlMap().getCameraPosition().zoom > 10)
                        statusPage3.getlMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, statusPage3.getlMap().getCameraPosition().zoom));
                    else
                        statusPage3.getlMap().animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            }
        }
    }

    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);

        Intent intent = new Intent(TelemetryMp.this, LocationService.class);
        startService(intent);
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

}
