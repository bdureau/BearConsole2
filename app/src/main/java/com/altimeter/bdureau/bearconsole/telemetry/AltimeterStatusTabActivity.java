package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display real time altimeter status using the bluetooth module/or telemetry
 * This is a tabbed activity that will display several fragments for each tab of the telemetry
 * - AltimeterInfoFragment => display altimeter status
 * - AltimeterOutputFragment => will allow you to test your altimeter outputs
 * - GPSStatusFragment => will display information about the GPS when using the altiGPS
 * - GPSMapStatusFragment => will display the Map when using the altiGPS
 * This has the ability to take screen shot of each tab and share them using Whats app, email etc ...
 * @author: boris.dureau@neuf.fr
 **/

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.AltimeterAccelFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.AltimeterInfoFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.AltimeterOutputFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.GPSGoogleMapStatusFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.GPSOpenMapStatusFragment;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryStatusFragment.GPSStatusFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AltimeterStatusTabActivity extends AppCompatActivity {
    public String TAG = "AltimeterStatusTabActivity";
    private static ViewPager mViewPager; //static so that the map does not disapear

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    public LocationBroadCastReceiver receiver = null;
    SectionsStatusPageAdapter adapter;
    private AltimeterInfoFragment statusPage1 = null;
    private AltimeterAccelFragment statusPage1ter = null;
    private AltimeterOutputFragment statusPage1bis = null;
    private RocketViewFragment statusPage5 = null;
    private GPSStatusFragment statusPage2 = null;
    private GPSGoogleMapStatusFragment statusPage3 = null;
    private GPSOpenMapStatusFragment statusPage4 = null;

    private float rocketLatitude = 48.8698f;
    private float rocketLongitude = 2.2190f;

    private Button btnDismiss, btnRecording;
    private ConsoleApplication myBT;
    Thread altiStatus;
    private boolean status = true;
    private boolean recording = false;
    Intent locIntent = null;
    private double accel375[] = {0.0, 0.0, 0.0};
    private double accel345[] = {0.0, 0.0, 0.0};
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
                case 1:
                    String myUnits;
                    //Value 1 contains the current altitude
                    if (myBT.getAppConf().getUnits() == 0)
                        //Meters
                        myUnits = getResources().getString(R.string.Meters_fview);
                    else
                        //Feet
                        myUnits = getResources().getString(R.string.Feet_fview);
                    if (statusPage1 != null)
                        statusPage1.setAltitude((String) msg.obj + " " + myUnits);
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
                            rocketLatitude = latitudeVal;
                            statusPage2.setLatitudeValue("" + latitudeVal);
                        }
                    }
                    break;
                case 19:
                    //Value 19 contains the longitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String longitude = (String) msg.obj;
                        if (longitude.matches("\\d+(?:\\.\\d+)?")) {
                            Float longitudeVal = Float.parseFloat(longitude) / 100000;
                            rocketLongitude = longitudeVal;
                            statusPage2.setLongitudeValue("" + longitudeVal);
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
                            statusPage2.setTimeSatValue(String.format("%.2f",  ((double) TimeSatVal / (double) 1000)) + " secs");
                        }
                    }
                    break;
                case 26:
                    //Value 26 contains Accel375X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375") ) {
                        String accel375x = (String) msg.obj;
                        if (accel375x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375xVal = Integer.parseInt(accel375x);
                            statusPage1ter.setAccel375x(String.format("%.2f",  ((double)accel375xVal / (double)1000)));
                            accel375[0]= ((double)accel375xVal / (double)1000);
                        }
                    }
                    //for the TTGOBearAltimeter it is an ADXL345
                    if(myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
                        String accel345x = (String) msg.obj;
                        if (accel345x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345xVal = Integer.parseInt(accel345x);
                            statusPage1ter.setAccel345x(String.format("%.2f",  ((double)accel345xVal / (double)1000)));
                            accel345[0]= ((double)accel345xVal / (double)1000);
                        }
                    }
                    break;
                case 27:
                    //Value 27 contains Accel375Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375") ) {
                        String accel375y = (String) msg.obj;
                        if (accel375y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375yVal = Integer.parseInt(accel375y);
                            statusPage1ter.setAccel375y(String.format("%.2f", ((double) accel375yVal / (double) 1000)));
                            accel375[1]= ((double)accel375yVal / (double)1000);
                        }
                    }
                    //for the TTGOBearAltimeter it is an ADXL345
                    if(myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
                        String accel345y = (String) msg.obj;
                        if (accel345y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345yVal = Integer.parseInt(accel345y);
                            statusPage1ter.setAccel345y(String.format("%.2f",  ((double)accel345yVal / (double)1000)));
                            accel345[1]= ((double)accel345yVal / (double)1000);
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
                            int accel375zVal = Integer.parseInt(accel375z);
                            statusPage1ter.setAccel375z(String.format("%.2f",  ((double)accel375zVal / (double)1000)));
                            accel375[2]= ((double)accel375zVal / (double)1000);
                            statusPage5.setInputString(accel375);
                        }
                    }
                    //for the TTGOBearAltimeter it is an ADXL345
                    if(myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
                        String accel345z = (String) msg.obj;
                        if (accel345z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345zVal = Integer.parseInt(accel345z);
                            statusPage1ter.setAccel345z(String.format("%.2f",  ((double)accel345zVal / (double)1000)));
                            accel345[2]= ((double)accel345zVal / (double)1000);
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
                            int accel345xVal = Integer.parseInt(accel345x);
                            statusPage1ter.setAccel345x(String.format("%.2f", ((double)accel345xVal / (double)1000)));
                            accel345[0]= ((double)accel345xVal / (double)1000);
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
                            int accel345yVal = Integer.parseInt(accel345y);
                            statusPage1ter.setAccel345y(String.format("%.2f",  ((double)accel345yVal / (double)1000)));
                            accel345[1]= ((double)accel345yVal / (double)1000);
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
                            int accel345zVal = Integer.parseInt(accel345z);
                            statusPage1ter.setAccel345z(String.format("%.2f",  ((double)accel345zVal / (double)1000)));
                            accel345[2]= ((double)accel345zVal / (double)1000);
                        }
                    }
                    break;
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
        outState.putBoolean("RECORDING_KEY", recording);
        outState.putBoolean("STATUS_KEY", status);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState()");
        recording = savedInstanceState.getBoolean("RECORDING_KEY");
        status = savedInstanceState.getBoolean("STATUS_KEY");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        myBT = (ConsoleApplication) getApplication();
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
            verifyStoragePermission(AltimeterStatusTabActivity.this);
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

        setContentView(R.layout.activity_altimeter_status_tab);
        rocketLatitude = myBT.getAppConf().getRocketLatitude();

        rocketLongitude = myBT.getAppConf().getRocketLongitude();
        mViewPager = (ViewPager) findViewById(R.id.container);

        setupViewPager(mViewPager);

        myBT.setHandler(handler);
        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnRecording = (Button) findViewById(R.id.butRecording);
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS") && myBT.getAppConf().getManualRecording())
            btnRecording.setVisibility(View.VISIBLE);
        else
            btnRecording.setVisibility(View.INVISIBLE);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnDismiss()");
                if (recording) {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Confirm Exit Recording")
                            .setMessage("Recording in progress... Are you sure you want to exit?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "about to Stop recording !!!!");
                                    /*if (status) {
                                        status = false;
                                        myBT.write("h;\n".toString());
                                        myBT.setExit(true);
                                        myBT.clearInput();
                                        myBT.flush();
                                    }*/
                                    //turn off telemetry
                                    /*myBT.flush();
                                    myBT.clearInput();
                                    myBT.write("y0;\n".toString());*/

                                    //exit recording if running
                                    if (recording) {
                                        recording = false;
                                        myBT.flush();
                                        myBT.clearInput();
                                        myBT.write("w0;\n".toString());
                                        myBT.clearInput();
                                        myBT.flush();
                                        Log.d(TAG, "Stopped recording !!!!");
                                        msg("Stopped recording");
                                    }
                                    finish();      //exit the  activity
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    if (status) {
                        status = false;
                        myBT.write("h;\n".toString());
                        myBT.setExit(true);
                        myBT.clearInput();
                        myBT.flush();
                    }
                    //turn off telemetry
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y0;\n".toString());
                    finish();      //exit the  activity
                }
            }
        });

        btnRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (recording) {
                    recording = false;
                    myBT.write("w0;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                    msg("Stopped recording");
                } else {
                    recording = true;
                    myBT.write("w1;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                    msg("Started recording");
                }
            }
        });


        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!status) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        altiStatus = new Thread(r);
        altiStatus.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed()");
        //super.onBackPressed();
        if (recording) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit Recording")
                    .setMessage("Recording in progress... Are you sure you want to exit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //exit recording if running
                            if (recording) {
                                recording = false;
                                myBT.flush();
                                myBT.clearInput();
                                myBT.write("w0;\n".toString());
                                myBT.clearInput();
                                myBT.flush();
                                msg("Stopped recording");
                            }
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            //turn off telemetry
            myBT.flush();
            myBT.clearInput();
            myBT.write("y0;\n".toString());
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        //switch off output
        if (!myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
            statusPage1bis.resetSwitches();
        }

        if(locIntent != null)
            stopService(locIntent);

        if (receiver != null) {
         try {
             unregisterReceiver(receiver);
         }
         catch (IllegalArgumentException  e) {
             Log.d(TAG, "error");
         }
            receiver = null;
        }

        //turn off telemetry
        myBT.flush();
        myBT.clearInput();
        myBT.write("y0;\n".toString());

        //exit recording if running
        if (recording) {
            recording = false;
            myBT.flush();
            myBT.clearInput();
            myBT.write("w0;\n".toString());
            myBT.clearInput();
            myBT.flush();
            msg("Stopped recording");
        }

        //////////////////////////////
        if (status) {
            status = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
            //finish();
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
        //long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(timeOut);

        /////////////////////////////

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            status = true;

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        if (!status) break;
                        myBT.ReadResult(10000);
                    }
                }
            };
            altiStatus = new Thread(r);
            altiStatus.start();
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");

        /*if (recording) {
            recording = false;
            myBT.write("w0;\n".toString());
            myBT.clearInput();
            myBT.flush();
        }*/

        /*if (status) {
            status = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
            //finish();
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

        myMessage = myBT.ReadResult(10000);*/
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        statusPage1 = new AltimeterInfoFragment(myBT);
        if (!myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
            statusPage1bis = new AltimeterOutputFragment(myBT);
        }


        adapter.addFragment(statusPage1, "TAB1");
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel")||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345")||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
            statusPage1ter = new AltimeterAccelFragment(myBT);
            adapter.addFragment(statusPage1ter, "TAB1TER");
            statusPage5 = new RocketViewFragment();
            adapter.addFragment(statusPage5, "TAB5");
        }

        if(myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
            statusPage1ter = new AltimeterAccelFragment(myBT);
            adapter.addFragment(statusPage1ter, "TAB1TER");
        }
        if (!myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
            adapter.addFragment(statusPage1bis, "TAB1BIS");
        }
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            statusPage2 = new GPSStatusFragment(myBT);
            adapter.addFragment(statusPage2, "TAB2");

            if (!myBT.getAppConf().getUseOpenMap()) {
                statusPage3 = new GPSGoogleMapStatusFragment(myBT, viewPager);
                adapter.addFragment(statusPage3, "TAB3");
            } else {
                statusPage4 = new GPSOpenMapStatusFragment(myBT, viewPager);
                adapter.addFragment(statusPage4, "TAB4");
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

    public static void verifyStoragePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSION_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        //registerReceiver(receiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        locIntent = new Intent(AltimeterStatusTabActivity.this, LocationService.class);
        startService(locIntent);
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
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
            Intent i = new Intent(AltimeterStatusTabActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_status_alti");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(AltimeterStatusTabActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
