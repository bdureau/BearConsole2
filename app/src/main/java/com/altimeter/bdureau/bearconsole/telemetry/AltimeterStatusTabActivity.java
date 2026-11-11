package com.altimeter.bdureau.bearconsole.telemetry;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltimeterStatusTabActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Real-time ground status of the altimeter, through the Bluetooth module or the telemetry
 *  radio. The altimeter streams "telemetry" frames decoded by ConsoleApplication and forwarded
 *  to the handler of this activity. Tabs (depending on the board):
 *    - AltimeterInfoFragment: outputs continuity, altitude, battery, temperature, EEPROM;
 *    - AltimeterAccelFragment / RocketViewFragment: accelerometer values and 3D attitude;
 *    - AltimeterOutputFragment: pyro / servo output test, each firing confirmed by the user;
 *    - GPSStatusFragment and a map fragment (Google Maps or OpenStreetMap) for the AltiGPS.
 *  On the AltiGPS a manual recording can be started / stopped; leaving the screen while
 *  recording asks for a confirmation (Back button and Dismiss button).
 *  All serial I/O runs in the background through {@link TelemetryReader}.
 * ============================================================================
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
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

import com.altimeter.bdureau.bearconsole.BaseActivity;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.connection.TelemetryReader;
import com.altimeter.bdureau.bearconsole.utils.TabPagerAdapter;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AltimeterStatusTabActivity extends BaseActivity
        implements AltimeterOutputFragment.OutputCommandSender {
    public String TAG = "AltimeterStatusTabActivity";
    private ViewPager mViewPager;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    public LocationBroadCastReceiver receiver = null;
    private TabPagerAdapter adapter;
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
    // Background reading loop and command queue (never blocks the UI thread)
    private TelemetryReader reader;
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
    // Receives the telemetry fields decoded by ConsoleApplication (msg.what = field index).
    // Bound to the main looper; unregistered and purged in onDestroy() to avoid leaking the activity.
    private final Handler handler = new Handler(Looper.getMainLooper()) {
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
                            statusPage2.setTimeSatValue(String.format("%.2f", ((double) TimeSatVal / (double) 1000)) + " " + getString(R.string.unit_secs));
                        }
                    }
                    break;
                case 26:
                    //Value 26 contains Accel375X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel375x = (String) msg.obj;
                        if (accel375x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375xVal = ConsoleApplication.parseIntSafe(accel375x, 0);
                            statusPage1ter.setAccel375x(String.format("%.2f", ((double) accel375xVal / (double) 1000)));
                            accel375[0] = ((double) accel375xVal / (double) 1000);
                        }
                    }
                    //for the TTGOBearAltimeter it is an ADXL345
                    if (myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
                        String accel345x = (String) msg.obj;
                        if (accel345x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345xVal = ConsoleApplication.parseIntSafe(accel345x, 0);
                            statusPage1ter.setAccel345x(String.format("%.2f", ((double) accel345xVal / (double) 1000)));
                            accel345[0] = ((double) accel345xVal / (double) 1000);
                        }
                    }
                    break;
                case 27:
                    //Value 27 contains Accel375Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel375y = (String) msg.obj;
                        if (accel375y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375yVal = ConsoleApplication.parseIntSafe(accel375y, 0);
                            statusPage1ter.setAccel375y(String.format("%.2f", ((double) accel375yVal / (double) 1000)));
                            accel375[1] = ((double) accel375yVal / (double) 1000);
                        }
                    }
                    //for the TTGOBearAltimeter it is an ADXL345
                    if (myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
                        String accel345y = (String) msg.obj;
                        if (accel345y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345yVal = ConsoleApplication.parseIntSafe(accel345y, 0);
                            statusPage1ter.setAccel345y(String.format("%.2f", ((double) accel345yVal / (double) 1000)));
                            accel345[1] = ((double) accel345yVal / (double) 1000);
                        }
                    }
                    break;
                case 28:
                    //Value 28 contains Accel375Z
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel375z = (String) msg.obj;
                        if (accel375z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel375zVal = ConsoleApplication.parseIntSafe(accel375z, 0);
                            statusPage1ter.setAccel375z(String.format("%.2f", ((double) accel375zVal / (double) 1000)));
                            accel375[2] = ((double) accel375zVal / (double) 1000);
                            statusPage5.setInputString(accel375);
                        }
                    }
                    //for the TTGOBearAltimeter it is an ADXL345
                    if (myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
                        String accel345z = (String) msg.obj;
                        if (accel345z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345zVal = ConsoleApplication.parseIntSafe(accel345z, 0);
                            statusPage1ter.setAccel345z(String.format("%.2f", ((double) accel345zVal / (double) 1000)));
                            accel345[2] = ((double) accel345zVal / (double) 1000);
                        }
                    }
                    break;
                case 29:
                    //Value 29 contains Accel345X
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel345x = (String) msg.obj;
                        if (accel345x.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345xVal = ConsoleApplication.parseIntSafe(accel345x, 0);
                            statusPage1ter.setAccel345x(String.format("%.2f", ((double) accel345xVal / (double) 1000)));
                            accel345[0] = ((double) accel345xVal / (double) 1000);
                        }
                    }
                    break;
                case 30:
                    //Value 30 contains Accel345Y
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel345y = (String) msg.obj;
                        if (accel345y.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345yVal = ConsoleApplication.parseIntSafe(accel345y, 0);
                            statusPage1ter.setAccel345y(String.format("%.2f", ((double) accel345yVal / (double) 1000)));
                            accel345[1] = ((double) accel345yVal / (double) 1000);
                        }
                    }
                    break;
                case 31:
                    //Value 31 contains Accel345z
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                            myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                        String accel345z = (String) msg.obj;
                        if (accel345z.matches("\\d+(?:\\.\\d+)?")) {
                            int accel345zVal = ConsoleApplication.parseIntSafe(accel345z, 0);
                            statusPage1ter.setAccel345z(String.format("%.2f", ((double) accel345zVal / (double) 1000)));
                            accel345[2] = ((double) accel345zVal / (double) 1000);
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
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState()");
        recording = savedInstanceState.getBoolean("RECORDING_KEY");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setTitle(R.string.status_screen);

        myBT = (ConsoleApplication) getApplication();
        reader = new TelemetryReader(myBT);

        // Back button: same behavior as the Dismiss button (confirmation while recording)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestExit();
            }
        });

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

        btnDismiss.setOnClickListener(v -> requestExit());

        btnRecording.setOnClickListener(v -> {
            if (recording) {
                recording = false;
                reader.send("w0;\n");
                msg(getString(R.string.status_recording_stopped));
            } else {
                recording = true;
                reader.send("w1;\n");
                msg(getString(R.string.status_recording_started));
            }
        });

        // Enable the stream and start reading it. "y1;" was already sent by the main screen
        // on first opening, but it must be sent again when the activity is re-created
        // (the previous instance switched the stream off in its onDestroy()).
        reader.start("y1;");
    }

    /**
     * Leaves the screen. While a manual GPS recording is running the user must confirm,
     * because leaving stops the recording. The stream itself is switched off in onDestroy().
     */
    private void requestExit() {
        if (recording) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.status_confirm_exit_recording_title)
                    .setMessage(R.string.status_confirm_exit_recording_msg)
                    .setPositiveButton(R.string.Yes, (dialog, which) -> {
                        if (recording) {
                            recording = false;
                            reader.send("w0;\n");
                            msg(getString(R.string.status_recording_stopped));
                        }
                        finish();
                    })
                    .setNegativeButton(R.string.No, null)
                    .show();
        } else {
            finish();
        }
    }

    /**
     * Output test commands ("k1T;", "k1F;"...) issued by AltimeterOutputFragment. They go through
     * the same ordered background queue as every other command of this screen.
     */
    @Override
    public void sendOutputCommand(String command) {
        reader.send(command);
    }

    @Override
    public void onDestroy() {
        // Safety: switch off any output left on by the output test tab. Done before
        // super.onDestroy(), while the fragment and the command queue are still alive.
        if (statusPage1bis != null) {
            statusPage1bis.resetSwitches();
        }
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

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

        // Stop the reading loop, then (in background) stop the recording if needed, switch the
        // telemetry stream off and send the handshake
        if (recording) {
            recording = false;
            reader.stop(true, "w0;\n", "y0;\n", "h;\n");
        } else {
            reader.stop(true, "y0;\n", "h;\n");
        }
        reader.release();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (myBT.getConnected() && !reader.isRunning()) {
            reader.start("y1;");
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new TabPagerAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_SET_USER_VISIBLE_HINT, this::onTabRestored);
        statusPage1 = new AltimeterInfoFragment(myBT);
        if (!myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter") &&
                !myBT.getAltiConfigData().getAltimeterName().equals("TTGOMiniBearAltimeter") &&
                !myBT.getAltiConfigData().getAltimeterName().equals("UltimateAltimeter")
        ) {
            statusPage1bis = new AltimeterOutputFragment(myBT);
        }


        adapter.addFragment(statusPage1);
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375")) {
            statusPage1ter = new AltimeterAccelFragment(myBT);
            adapter.addFragment(statusPage1ter);
            statusPage5 = new RocketViewFragment();
            adapter.addFragment(statusPage5);
        }

        if (myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")) {
            statusPage1ter = new AltimeterAccelFragment(myBT);
            adapter.addFragment(statusPage1ter);
        }
        if (!myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter") &&
                !myBT.getAltiConfigData().getAltimeterName().equals("TTGOMiniBearAltimeter") &&
                !myBT.getAltiConfigData().getAltimeterName().equals("UltimateAltimeter")
        ) {
            adapter.addFragment(statusPage1bis);
        }
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            statusPage2 = new GPSStatusFragment(myBT);
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
        if (fragment instanceof AltimeterInfoFragment) statusPage1 = (AltimeterInfoFragment) fragment;
        else if (fragment instanceof AltimeterAccelFragment) statusPage1ter = (AltimeterAccelFragment) fragment;
        else if (fragment instanceof AltimeterOutputFragment) statusPage1bis = (AltimeterOutputFragment) fragment;
        else if (fragment instanceof RocketViewFragment) statusPage5 = (RocketViewFragment) fragment;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED);
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
