package com.altimeter.bdureau.bearconsole.telemetry;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: TelemetryTabActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Real-time flight telemetry screen, used with a long range radio module (3DR, LoRa).
 *  The altimeter streams "telemetry" frames that ConsoleApplication decodes and forwards to
 *  the handler of this activity, one message per field. Tabs:
 *    - AltimeterMpFlightFragment (MPAndroidChart) or AltimeterFcFlightFragment (AFreeChart on
 *      Android 7): live altitude curve and flight events (lift off, apogee, main, landing),
 *      announced by voice when enabled in the settings;
 *    - AltimeterInfoFragment: altimeter status (outputs continuity, battery, EEPROM...);
 *    - GPSStatusFragment and a map fragment (Google Maps or OpenStreetMap) for the AltiGPS.
 *  The reading loop and every command sent to the altimeter run in the background through
 *  {@link TelemetryReader}; the UI thread never waits for the serial link.
 * ============================================================================
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;


import com.altimeter.bdureau.bearconsole.BaseActivity;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.connection.TelemetryReader;
import com.altimeter.bdureau.bearconsole.utils.TabPagerAdapter;
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

public class TelemetryTabActivity extends BaseActivity {

    public String TAG = "TelemetryMp.class";
    private ViewPager mViewPager;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    public LocationBroadCastReceiver receiver = null;
    private TabPagerAdapter adapter;
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
    // Background reading loop and command queue (never blocks the UI thread)
    private TelemetryReader reader;

    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;
    private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;

    private int altitudeTime = 0;

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
    // Receives the telemetry fields decoded by ConsoleApplication (msg.what = field index).
    // Bound to the main looper; unregistered and purged in onDestroy() to avoid leaking the activity.
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    // Value 1 contain the current altitude
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        int altitude = (int) (ConsoleApplication.parseIntSafe((String) msg.obj, 0) * FEET_IN_METER);
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
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0 || LiftOffTime > 0) {
                                        statusPage0bis.setLiftOffChecked(true);
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
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0 || LiftOffTime > 0) {
                                        statusPage0.setLiftOffChecked(true);
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
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                                        statusPage0bis.setApogeeChecked(true);
                                        statusPage0bis.setMaxAltitudeTime((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                                    }
                    } else {
                        if (statusPage0.isViewCreated())
                            if (!statusPage0.isApogeeChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                                        statusPage0.setApogeeChecked(true);
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
                                int altitude = (int) (ConsoleApplication.parseIntSafe((String) msg.obj, 0) * FEET_IN_METER);

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
                                int altitude = (int) (ConsoleApplication.parseIntSafe((String) msg.obj, 0) * FEET_IN_METER);

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
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                                        statusPage0bis.setMainChuteTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                        statusPage0bis.setMainChuteChecked(true);
                                    }
                    }else {
                        if (statusPage0.isViewCreated())
                            if (!statusPage0.isMainChuteChecked())
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                                        statusPage0.setMainChuteTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                        statusPage0.setMainChuteChecked(true);
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
                                    int altitude = (int) (ConsoleApplication.parseIntSafe((String) msg.obj, 0) * FEET_IN_METER);
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
                                    int altitude = (int) (ConsoleApplication.parseIntSafe((String) msg.obj, 0) * FEET_IN_METER);
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
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                                        statusPage0bis.setLandedChecked(true);
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
                                    if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                                        statusPage0.setLandedChecked(true);
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
                        if (ConsoleApplication.parseIntSafe((String) msg.obj, 0) > 0) {
                            altitudeTime = ConsoleApplication.parseIntSafe((String) msg.obj, 0);
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
                        statusPage1.setVoltage(voltage + " " + getString(R.string.unit_volts));
                    } else {
                        statusPage1.setVoltage(getString(R.string.not_available));
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
                            int nbrOfSatelliteVal = ConsoleApplication.parseIntSafe(nbrOfSatellite, 0);
                            statusPage2.setSatellitesVal("" + nbrOfSatelliteVal);
                        }
                    }
                    break;
                case 21:
                    //Value 21 contains the hdop
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String hdop = (String) msg.obj;
                        if (hdop.matches("\\d+(?:\\.\\d+)?")) {
                            int hdopVal = ConsoleApplication.parseIntSafe(hdop, 0);
                            statusPage2.setHdopVal("" + hdopVal);
                        }
                    }
                    break;
                case 22:
                    //Value 22 contains the location age
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String locationAge = (String) msg.obj;
                        if (locationAge.matches("\\d+(?:\\.\\d+)?")) {
                            int locationAgeVal = ConsoleApplication.parseIntSafe(locationAge, 0);
                            statusPage2.setLocationAgeValue("" + locationAgeVal);
                        }
                    }
                    break;
                case 23:
                    //Value 23 contains the GPS altitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSAltitude = (String) msg.obj;
                        if (GPSAltitude.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSAltitudeVal = ConsoleApplication.parseIntSafe(GPSAltitude, 0);
                            statusPage2.setGPSAltitudeVal("" + GPSAltitudeVal);
                        }
                    }
                    break;
                case 24:
                    // Value 24 contains the GPS Speed
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSSpeed = (String) msg.obj;
                        if (GPSSpeed.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSSpeedVal = ConsoleApplication.parseIntSafe(GPSSpeed, 0);
                            statusPage2.setGPSSpeedVal("" + GPSSpeedVal);
                        }
                    }
                    break;
                case 25:
                    // Value 25 contains the time for sat acquisition
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String TimeSat = (String) msg.obj;
                        if (TimeSat.matches("\\d+(?:\\.\\d+)?")) {
                            int TimeSatVal = ConsoleApplication.parseIntSafe(TimeSat, 0);
                            statusPage2.setTimeSatValue(String.format("%.2f", (double) (TimeSatVal / 1000)) + " " + getString(R.string.unit_secs));
                        }
                    }
                    break;
                case 26:
                    //Value 26 contains Accel375X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel375x = (String) msg.obj;
                        if (accel375x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375xVal = ConsoleApplication.parseIntSafe(accel375x, 0);
                        }
                    }
                    break;
                case 27:
                    //Value 27 contains Accel375Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel375y = (String) msg.obj;
                        if (accel375y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375yVal = ConsoleApplication.parseIntSafe(accel375y, 0);
                        }
                    }
                    break;
                case 28:
                    //Value 28 contains Accel375Z
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel375z = (String) msg.obj;
                        if (accel375z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375zVal = ConsoleApplication.parseIntSafe(accel375z, 0);
                        }
                    }
                    break;
                case 29:
                    //Value 29 contains Accel345X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel345x = (String) msg.obj;
                        if (accel345x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345xVal = ConsoleApplication.parseIntSafe(accel345x, 0);
                        }
                    }
                    break;
                case 30:
                    //Value 30 contains Accel345Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel345y = (String) msg.obj;
                        if (accel345y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345yVal = ConsoleApplication.parseIntSafe(accel345y, 0);
                        }
                    }
                    break;
                case 31:
                    //Value 31 contains Accel345z
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel345z = (String) msg.obj;
                        if (accel345z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345zVal = ConsoleApplication.parseIntSafe(accel345z, 0);
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
        setTitle(R.string.telemetry_screen);
        setContentView(R.layout.activity_telemetry_tab);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //prevent the phone screen to switch off when in telemetry
        yValues = new ArrayList<>();

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getCacheDir().getAbsolutePath(), "osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(osmConf.getOsmdroidBasePath().getAbsolutePath(), "tile");
        osmConf.setOsmdroidTileCache(tileCache);

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
        reader = new TelemetryReader(myBT);

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
    public void onDestroy() {
        super.onDestroy();

        // Stop receiving telemetry messages and drop the pending ones
        myBT.clearHandler(handler);
        handler.removeCallbacksAndMessages(null);

        if (locIntent != null)
            stopService(locIntent);

        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Location receiver was not registered");
            }
            receiver = null;
        }
        try {
            mTTS.shutdown();
        } catch (Exception e) {
            Log.w(TAG, "Text to speech shutdown failed", e);
        }

        // Stop the loop (if still running) and switch the telemetry stream off, in background
        reader.stop(false, "h;", "y0;");
        reader.release();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        // Coming back from another screen: re-enable the stream and restart the reading loop
        if (myBT.getConnected() && !reader.isRunning()) {
            reader.start("y1;");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The screen is no longer visible: pause the stream ("h;") without blocking the UI
        if (reader.isRunning()) {
            reader.stop(true, "h;");
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new TabPagerAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_SET_USER_VISIBLE_HINT, this::onTabRestored);
        if ((myBT.getAppConf().getGraphicsLibType() == 0) &
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
            statusPage0bis = new AltimeterFcFlightFragment(myBT);
            adapter.addFragment(statusPage0bis);
        } else {
            statusPage0 = new AltimeterMpFlightFragment(myBT);
            adapter.addFragment(statusPage0);
        }

        statusPage1 = new AltimeterInfoFragment(myBT);
        statusPage2 = new GPSStatusFragment(myBT);


        adapter.addFragment(statusPage1);

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            adapter.addFragment(statusPage2);
            if (!myBT.getAppConf().getUseOpenMap()) {
                statusPage3 = new GPSGoogleMapStatusFragment(myBT, viewPager);
                adapter.addFragment(statusPage3);
            } else {
                statusPage4 = new GPSOpenMapStatusFragment(myBT, viewPager);
                adapter.addFragment(statusPage4);
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

    /**
     * Called by the adapter when the ViewPager displays a fragment re-created by the system
     * (activity re-creation): the references used by the handler are re-bound to it.
     */
    private void onTabRestored(int position, Fragment fragment) {
        if (fragment instanceof AltimeterMpFlightFragment) statusPage0 = (AltimeterMpFlightFragment) fragment;
        else if (fragment instanceof AltimeterFcFlightFragment) statusPage0bis = (AltimeterFcFlightFragment) fragment;
        else if (fragment instanceof AltimeterInfoFragment) statusPage1 = (AltimeterInfoFragment) fragment;
        else if (fragment instanceof GPSStatusFragment) statusPage2 = (GPSStatusFragment) fragment;
        else if (fragment instanceof GPSGoogleMapStatusFragment) statusPage3 = (GPSGoogleMapStatusFragment) fragment;
        else if (fragment instanceof GPSOpenMapStatusFragment) statusPage4 = (GPSOpenMapStatusFragment) fragment;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        locIntent = new Intent(TelemetryTabActivity.this, LocationService.class);
        startService(locIntent);
    }

    /**
     * Resets the live flight data, enables the stream and starts the reading loop. "y1;" was
     * already sent by the main screen on first opening, but it must be sent again when the
     * activity is re-created (the previous instance switched the stream off in onDestroy()).
     */
    public void startTelemetry() {
        lastPlotTime = 0;
        myBT.initFlightData();

        myflight = myBT.getFlightData(); // used by the AFreeChart fragment
        LiftOffTime = 0;
        reader.start("y1;");
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
