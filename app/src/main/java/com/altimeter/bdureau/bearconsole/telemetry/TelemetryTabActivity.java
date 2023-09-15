package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display real time telemetry providing
 * that you have a telemetry long range module.
 * This is a tabbed activity that will display several fragments for each tab of the telemetry
 * - AltimeterMpFlightFragment => This activity display the telemetry graph using the MPAndroidChart library
 * and will do talking telemetry.
 * - AltimeterInfoFragment => display altimeter status
 * - GPSStatusFragment => will display information about the GPS when using the altiGPS
 * - GPSMapStatusFragment => will display the Map when using the altiGPS
 * @author: boris.dureau@neuf.fr
 **/

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Intent;


import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.AltimeterFcFlightFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.AltimeterInfoFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.AltimeterMpFlightFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.GPSGoogleMapStatusFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.GPSOpenMapStatusFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.GPSStatusFragment;
import com.github.mikephil.charting.data.Entry;

public class TelemetryTabActivity extends AppCompatActivity {

    public String TAG = "TelemetryMp.class";
    private ViewPager mViewPager;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    public LocationBroadCastReceiver receiver = null;
    private SectionsStatusPageAdapter adapter;
    private AltimeterMpFlightFragment statusPage0 = null;
    private AltimeterFcFlightFragment statusPage0bis = null;
    private AltimeterInfoFragment statusPage1 = null;
    private GPSStatusFragment statusPage2 = null;
    private GPSGoogleMapStatusFragment statusPage3 = null;
    private GPSOpenMapStatusFragment statusPage4 = null;

    public float rocketLatitude = 48.8698f;
    public float rocketLongitude = 2.2190f;
    private FlightData myflight = null; //used with afreeChart

    private ArrayList<Entry> yValues;

    private ConsoleApplication myBT;
    Thread rocketTelemetry;

    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;
    private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;

    private int altitudeTime = 0;

    private boolean telemetry = true;
    private boolean liftOffSaid = false;
    private boolean apogeeSaid = false;
    private boolean landedSaid = false;
    private boolean mainSaid = false;

    private Button dismissButton;

    private TextToSpeech mTTS = null;

    Intent locIntent = null;
    //Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSION_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current altitude
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                        //use afree chart
                        if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            if (statusPage0bis.isViewCreated())
                                if (statusPage0bis.isLiftOffChecked() && !statusPage0bis.isLandedChecked()) {
                                    statusPage0bis.setCurrentAltitude(altitude + "");
                                    myflight.AddToFlight(altitudeTime, altitude, "Telemetry");

                                    //plot every seconde
                                    if ((altitudeTime - lastPlotTime) > 1000) {
                                        lastPlotTime = altitudeTime;
                                        statusPage0bis.plotYvalues(myflight.GetFlightData("Telemetry"));
                                    }
                                }

                        } else
                        //use MpChart
                        {
                            if (statusPage0.isViewCreated())
                                if (statusPage0.isLiftOffChecked() && !statusPage0.isLandedChecked()) {
                                    statusPage0.setCurrentAltitude(altitude + "");
                                    yValues.add(new Entry(altitudeTime, altitude));

                                    //plot every seconde
                                    if ((altitudeTime - lastPlotTime) > 1000) {
                                        lastPlotTime = altitudeTime;
                                        statusPage0.plotYvalues(yValues);
                                    }
                                }
                        }
                        // Tell altitude every 5 secondes
                        if ((altitudeTime - lastSpeakTime) > 5000 && liftOffSaid) {
                            if (myBT.getAppConf().getAltitude_event()) {
                                mTTS.speak(getResources().getString(R.string.altitude) + " " + String.valueOf(altitude) + " "
                                        + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                            }
                            lastSpeakTime = altitudeTime;
                        }
                    }
                    break;
                case 2:
                    // Value 2 lift off yes/no
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (statusPage0bis.isViewCreated())
                            if (!statusPage0bis.isLiftOffChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0 || LiftOffTime > 0) {
                                        //statusPage0bis.setLiftOffEnabled(true);
                                        statusPage0bis.setLiftOffChecked(true);
                                        //statusPage0bis.setLiftOffEnabled(false);
                                        if (LiftOffTime == 0)
                                            LiftOffTime = System.currentTimeMillis();
                                        statusPage0bis.setLiftOffTime("0 ms");
                                        if (!liftOffSaid) {
                                            if (myBT.getAppConf().getLiftOff_event()) {
                                                mTTS.speak(getResources().getString(R.string.lift_off), TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            liftOffSaid = true;
                                        }
                                    }
                    } else {
                        if (statusPage0.isViewCreated())
                            if (!statusPage0.isLiftOffChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0 || LiftOffTime > 0) {
                                        //statusPage0.setLiftOffEnabled(true);
                                        statusPage0.setLiftOffChecked(true);
                                        //statusPage0.setLiftOffEnabled(false);
                                        if (LiftOffTime == 0)
                                            LiftOffTime = System.currentTimeMillis();
                                        statusPage0.setLiftOffTime("0 ms");
                                        if (!liftOffSaid) {
                                            if (myBT.getAppConf().getLiftOff_event()) {
                                                mTTS.speak(getResources().getString(R.string.lift_off), TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            liftOffSaid = true;
                                        }
                                    }
                    }
                    break;
                case 3:
                    // Value 3 apogee fired yes/no
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (statusPage0bis.isViewCreated())
                            if (!statusPage0bis.isApogeeChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        //statusPage0bis.setApogeeEnable(true);
                                        statusPage0bis.setApogeeChecked(true);
                                        //statusPage0bis.setApogeeEnable(false);
                                        statusPage0bis.setMaxAltitudeTime((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                                    }
                    } else {
                        if (statusPage0.isViewCreated())
                            if (!statusPage0.isApogeeChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        //statusPage0.setApogeeEnable(true);
                                        statusPage0.setApogeeChecked(true);
                                        //statusPage0.setApogeeEnable(false);
                                        statusPage0.setMaxAltitudeTime((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                                    }
                    }

                    break;
                case 4:
                    //Value 4 apogee altitude
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (statusPage0bis.isViewCreated())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                                int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);

                                if (statusPage0bis.isApogeeChecked()) {
                                    statusPage0bis.setMaxAltitude(altitude + "");
                                    if (!apogeeSaid) {
                                        //first check if say it is enabled
                                        if (myBT.getAppConf().getApogee_altitude()) {
                                            mTTS.speak(getResources().getString(R.string.telemetry_apogee) + " " + String.valueOf(altitude) + " "
                                                    + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        apogeeSaid = true;
                                    }
                                }
                            }
                    } else {
                        if (statusPage0.isViewCreated())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                                int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);

                                if (statusPage0.isApogeeChecked()) {
                                    statusPage0.setMaxAltitude(altitude + "");
                                    if (!apogeeSaid) {
                                        //first check if say it is enabled
                                        if (myBT.getAppConf().getApogee_altitude()) {
                                            mTTS.speak(getResources().getString(R.string.telemetry_apogee) + " " + String.valueOf(altitude) + " "
                                                    + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        apogeeSaid = true;
                                    }
                                }
                            }
                    }
                    break;
                case 5:
                    //value 5 main fired yes/no
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (statusPage0bis.isViewCreated())
                            if (!statusPage0bis.isMainChuteChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        statusPage0bis.setMainChuteTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                        //statusPage0bis.setMainChuteEnabled(true);
                                        statusPage0bis.setMainChuteChecked(true);
                                        //statusPage0bis.setMainChuteEnabled(false);
                                    }
                    }else {
                        if (statusPage0.isViewCreated())
                            if (!statusPage0.isMainChuteChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        statusPage0.setMainChuteTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                        //statusPage0.setMainChuteEnabled(true);
                                        statusPage0.setMainChuteChecked(true);
                                        //statusPage0.setMainChuteEnabled(false);
                                    }
                    }
                    break;
                case 6:
                    // value 6 main altitude
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (statusPage0bis.isViewCreated())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                                if (statusPage0bis.isMainChuteChecked()) {
                                    int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                                    statusPage0bis.setMainAltitude(String.valueOf(altitude));
                                    if (!mainSaid) {
                                        if (myBT.getAppConf().getMain_event()) {
                                            mTTS.speak(getResources().getString(R.string.main_deployed) + " " + String.valueOf(altitude) + " "
                                                    + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        mainSaid = true;
                                    }
                                }
                            }
                    } else {
                        if (statusPage0.isViewCreated())
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                                if (statusPage0.isMainChuteChecked()) {
                                    int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                                    statusPage0.setMainAltitude(String.valueOf(altitude));
                                    if (!mainSaid) {
                                        if (myBT.getAppConf().getMain_event()) {
                                            mTTS.speak(getResources().getString(R.string.main_deployed) + " " + String.valueOf(altitude) + " "
                                                    + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        mainSaid = true;
                                    }
                                }
                            }
                    }
                    break;
                case 7:
                    //have we landed
                    if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (statusPage0bis.isViewCreated())
                            if (!statusPage0bis.isLandedChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        //statusPage0bis.setLandedEnabled(true);
                                        statusPage0bis.setLandedChecked(true);
                                        //statusPage0bis.setLandedEnabled(false);
                                        statusPage0bis.setLandedAltitude(statusPage0bis.getLandedAltitude());
                                        statusPage0bis.setLandedTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                        if (!landedSaid) {
                                            if (myBT.getAppConf().getLanding_event()) {
                                                mTTS.speak(getResources().getString(R.string.rocket_has_landed), TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            landedSaid = true;
                                        }
                                    }
                    }else {
                        if (statusPage0.isViewCreated())
                            if (!statusPage0.isLandedChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        //statusPage0.setLandedEnabled(true);
                                        statusPage0.setLandedChecked(true);
                                        //statusPage0.setLandedEnabled(false);
                                        statusPage0.setLandedAltitude(statusPage0.getLandedAltitude());
                                        statusPage0.setLandedTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                        if (!landedSaid) {
                                            if (myBT.getAppConf().getLanding_event()) {
                                                mTTS.speak(getResources().getString(R.string.rocket_has_landed), TextToSpeech.QUEUE_FLUSH, null);
                                            }
                                            landedSaid = true;
                                        }
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
                case 18:
                    //Value 18 contains the latitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String latitude = (String) msg.obj;
                        if (latitude.matches("\\d+(?:\\.\\d+)?")) {
                            float latitudeVal = Float.parseFloat(latitude) / 100000;
                            statusPage2.setLatitudeValue("" + latitudeVal);
                            setLatitudeValue(latitude);
                            rocketLatitude = latitudeVal;
                        }
                    }
                    break;
                case 19:
                    //Value 19 contains the longitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String longitude = (String) msg.obj;
                        if (longitude.matches("\\d+(?:\\.\\d+)?")) {
                            float longitudeVal = Float.parseFloat(longitude) / 100000;
                            statusPage2.setLongitudeValue("" + longitudeVal);
                            setLongitudeValue(longitude);
                            rocketLongitude = longitudeVal;
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
                case 26:
                    //Value 26 contains Accel375X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")) {
                        String accel375x = (String) msg.obj;
                        if (accel375x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375xVal = Integer.parseInt(accel375x);
                        }
                    }
                    break;
                case 27:
                    //Value 27 contains Accel375Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")) {
                        String accel375y = (String) msg.obj;
                        if (accel375y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375yVal = Integer.parseInt(accel375y);
                        }
                    }
                    break;
                case 28:
                    //Value 28 contains Accel375Z
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")) {
                        String accel375z = (String) msg.obj;
                        if (accel375z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375zVal = Integer.parseInt(accel375z);
                        }
                    }
                    break;
                case 29:
                    //Value 29 contains Accel345X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")) {
                        String accel345x = (String) msg.obj;
                        if (accel345x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345xVal = Integer.parseInt(accel345x);
                        }
                    }
                    break;
                case 30:
                    //Value 30 contains Accel345Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")) {
                        String accel345y = (String) msg.obj;
                        if (accel345y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345yVal = Integer.parseInt(accel345y);
                        }
                    }
                    break;
                case 31:
                    //Value 31 contains Accel375z
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")) {
                        String accel375z = (String) msg.obj;
                        if (accel375z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375zVal = Integer.parseInt(accel375z);
                        }
                    }
                    break;
            }
        }
    };

    private void setLatitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Float val = Float.parseFloat(value);
            Log.d("track", "latitude:" + value);
            if (val != 0) {
                rocketLatitude = Float.parseFloat(value) / 100000;
                myBT.getAppConf().setRocketLatitude(rocketLatitude);
            }
        }
    }

    private void setLongitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Double val = Double.parseDouble(value);
            Log.d("track", "longitude:" + value);
            if (val != 0) {
                rocketLongitude = Float.parseFloat(value) / 100000;
                myBT.getAppConf().setRocketLongitude(rocketLongitude);
                myBT.getAppConf().SaveConfig();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry_tab);
        yValues = new ArrayList<>();

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

        if(Build.VERSION.SDK_INT>=23) {
            verifyStoragePermission(TelemetryTabActivity.this);
        }
        LocationManager lm = (LocationManager) this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.Cancel, null)
                    .show();
        }
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        dismissButton = (Button) findViewById(R.id.butDismiss);

        myBT.setHandler(handler);

        // Read the application config
        myBT.getAppConf().ReadConfig();
        rocketLatitude = myBT.getAppConf().getRocketLatitude();

        rocketLongitude = myBT.getAppConf().getRocketLongitude();
        //init text to speech
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;

                    if (Locale.getDefault().getLanguage().equals("en"))
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if (Locale.getDefault().getLanguage().equals("fr"))
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else if (Locale.getDefault().getLanguage().equals("tr"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("nl"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("es"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("it"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("hu"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("ru"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);

                    Log.d("Voice", myBT.getAppConf().getTelemetryVoice() + "");
                    //String[] itemsVoices;
                    //String items = "";
                    int i = 0;
                    try {
                        for (Voice tmpVoice : mTTS.getVoices()) {

                            if (tmpVoice.getName().startsWith(Locale.getDefault().getLanguage())) {
                                Log.d("Voice", tmpVoice.getName());
                                if (myBT.getAppConf().getTelemetryVoice() == i) {
                                    mTTS.setVoice(tmpVoice);
                                    Log.d("Voice", "Found voice");
                                    break;
                                }
                                i++;
                            }
                        }
                    } catch (Exception e) {

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
        if (myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS) {
            //Meters
            FEET_IN_METER = 1;
            myUnits = getResources().getString(R.string.Meters_fview);
        } else {
            //Feet
            FEET_IN_METER = 3.28084;
            myUnits = getResources().getString(R.string.Feet_fview);
        }

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

        if(locIntent != null)
            stopService(locIntent);

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        try {
            mTTS.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                        myBT.ReadResult(2000);
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
        //long timeOut = 10000;
        //long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(100000);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
            statusPage0bis = new AltimeterFcFlightFragment(myBT);
            adapter.addFragment(statusPage0bis, "TAB0");
        } else {
            statusPage0 = new AltimeterMpFlightFragment(myBT);
            adapter.addFragment(statusPage0, "TAB0");
        }

        statusPage1 = new AltimeterInfoFragment(myBT);
        statusPage2 = new GPSStatusFragment(myBT);


        adapter.addFragment(statusPage1, "TAB1");

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            adapter.addFragment(statusPage2, "TAB2");
            if (!myBT.getAppConf().getUseOpenMap()) {
                statusPage3 = new GPSGoogleMapStatusFragment(myBT, viewPager);
                adapter.addFragment(statusPage3, "TAB3");
            } else {
                statusPage4 = new GPSOpenMapStatusFragment(myBT, viewPager);
                adapter.addFragment(statusPage4, "TAB3");
            }
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

    public class LocationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("coordinate", intent.getAction());
            if (intent.getAction().equals("ACT_LOC")) {
                double latitude = intent.getDoubleExtra("latitude", 0f);
                double longitude = intent.getDoubleExtra("longitude", 0f);
                Log.d("coordinate", "latitude is:" + latitude + " longitude is: " + longitude);

                if (statusPage2 != null) {
                    statusPage2.setTelLatitudeValue(latitude + "");
                    statusPage2.setTelLongitudeValue(longitude + "");
                }

                if (!myBT.getAppConf().getUseOpenMap()) {
                    if (statusPage3 != null)
                        if (statusPage3.getlMap() != null) {
                            statusPage3.updateLocation(latitude, longitude, rocketLatitude, rocketLongitude);
                        }
                } else {
                    if (statusPage4 != null)
                        statusPage4.updateLocation(latitude, longitude, rocketLatitude, rocketLongitude);
                }
            }
        }
    }

    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);

        locIntent = new Intent(TelemetryTabActivity.this, LocationService.class);
        startService(locIntent);
    }

    public void startTelemetry() {
        telemetry = true;

        lastPlotTime = 0;
        myBT.initFlightData();

        myflight = myBT.getFlightData(); //this is used by the afreeChart
        LiftOffTime = 0;
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

    public static void verifyStoragePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSION_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(TelemetryTabActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry_alti");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(TelemetryTabActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
