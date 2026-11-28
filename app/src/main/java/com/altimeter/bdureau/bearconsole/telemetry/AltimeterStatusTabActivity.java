package com.altimeter.bdureau.bearconsole.telemetry;

/*
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
 *    - AltimeterAccelFragment / RocketViewFragment: accelerometer values, the accelerometer
 *      calibration done by the firmware ("j;"), and the live 3D attitude of the rocket
 *      (Processing sketch of the gimbal console) that follows the accelerometers;
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
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
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
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.altimeter.bdureau.bearconsole.connection.TelemetryReader;
import com.altimeter.bdureau.bearconsole.connection.AccelCalibrationSession;
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

public class AltimeterStatusTabActivity extends BaseActivity
        implements AltimeterOutputFragment.OutputCommandSender,
        AltimeterAccelFragment.CalibrationListener {
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

    // Rocket position in decimal degrees (double: full GPS precision)
    private double rocketLatitude = 48.8698;
    private double rocketLongitude = 2.2190;

    private Button btnDismiss, btnRecording;
    private ConsoleApplication myBT;
    // Background reading loop and command queue (never blocks the UI thread)
    private TelemetryReader reader;
    private boolean recording = false;
    Intent locIntent = null;
    // Last accelerometer values in thousandths of m/s^2 (unit of the firmware),
    // [sensor][axis] (AltimeterAccelFragment.SENSOR_*)
    private final int[][] accelMilli = new int[2][3];
    // true once a complete X Y Z sample of the sensor has been received
    private final boolean[] accelReceived = new boolean[2];
    // true while the altimeter calibrates its accelerometers (telemetry stream stopped)
    private boolean calibrating = false;
    private AccelCalibrationSession calibration = null;
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
            // The telemetry frames may arrive before the pages are created (or after a
            // re-creation, before they are bound again), and some pages do not exist for
            // every altimeter: ignore the value instead of crashing on a null page.
            if (!(msg.obj instanceof String) || pageFor(msg.what) == null) {
                return;
            }
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
                    //Value 1 contains the current altitude, always sent in meters by the
                    //firmware: convert it when the application displays feet (same rule as
                    //the telemetry screen), so that the value matches its unit label
                    if (ConsoleApplication.isSignedNumber((String) msg.obj)) {
                        double altitudeMeters = Double.parseDouble((String) msg.obj);
                        String myUnits;
                        long altitude;
                        if (myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS) {
                            myUnits = getResources().getString(R.string.Meters_fview);
                            altitude = Math.round(altitudeMeters);
                        } else {
                            myUnits = getResources().getString(R.string.Feet_fview);
                            altitude = Math.round(altitudeMeters * METERS_TO_FEET);
                        }
                        statusPage1.setAltitude(altitude + " " + myUnits);
                    }
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
                        if (ConsoleApplication.isSignedNumber(latitude)) {
                            double latitudeVal = Double.parseDouble(latitude) / 100000.0;
                            rocketLatitude = latitudeVal;
                            statusPage2.setLatitudeValue("" + latitudeVal);
                        }
                    }
                    break;
                case 19:
                    //Value 19 contains the longitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String longitude = (String) msg.obj;
                        if (ConsoleApplication.isSignedNumber(longitude)) {
                            double longitudeVal = Double.parseDouble(longitude) / 100000.0;
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
                        if (ConsoleApplication.isSignedNumber(GPSAltitude)) {
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
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                    // Values 26..31 contain the accelerometer axes (thousandths of m/s^2)
                    onAccelField(msg.what, (String) msg.obj);
                    break;
            }
        }
    };

    /**
     * One accelerometer axis received in a telemetry frame (msg.what 26..31, thousandths of m/s^2:
     * the firmware sends 1000 x the acceleration in m/s^2, 9807 for 1 g).
     *  - ESP32 accelerometer boards: 26..28 = ADXL375 X Y Z, 29..31 = ADXL345 X Y Z;
     *  - TTGOBearAltimeter: 26..28 = ADXL345 X Y Z.
     * When the Z axis (last field of a sensor in the frame) arrives, the complete sample is
     * given to the 3D rocket view.
     */
    private void onAccelField(int what, String value) {
        if (!ConsoleApplication.isSignedNumber(value)) return;
        String name = myBT.getAltiConfigData().getAltimeterName();
        int sensor, axis;
        if (name.equals("AltiMultiESP32_accel") || name.equals("AltiMultiESP32_accel_345")
                || name.equals("AltiMultiESP32_accel_375")) {
            sensor = (what <= 28) ? AltimeterAccelFragment.SENSOR_375 : AltimeterAccelFragment.SENSOR_345;
            axis = (what - 26) % 3;
        } else if (name.equals("TTGOBearAltimeter") && what <= 28) {
            sensor = AltimeterAccelFragment.SENSOR_345;
            axis = what - 26;
        } else {
            return;
        }
        int milli = ConsoleApplication.parseIntSafe(value, 0);
        accelMilli[sensor][axis] = milli;
        statusPage1ter.setAccelValue(sensor, axis, milli);
        if (axis == 2) {
            accelReceived[sensor] = true;
            updateAttitude();
        }
    }

    /**
     * Gives the 3D view the vector (in g) of the low g ADXL345 (finer resolution) when it
     * sends values, otherwise the one of the ADXL375.
     */
    private void updateAttitude() {
        if (statusPage5 == null) return;
        int sensor = AltimeterAccelFragment.SENSOR_345;
        int[] v = accelMilli[sensor];
        boolean has345 = accelReceived[sensor]
                && (Math.abs(v[0]) + Math.abs(v[1]) + Math.abs(v[2])) > 1000;
        if (!has345) sensor = AltimeterAccelFragment.SENSOR_375;
        if (!accelReceived[sensor]) return;
        double[] g = new double[3];
        for (int a = 0; a < 3; a++) {
            // thousandths of m/s^2 -> g
            g[a] = accelMilli[sensor][a] / 1000.0 / ConsoleApplication.STANDARD_GRAVITY;
        }
        statusPage5.setAcceleration(g);
    }

    /**
     * Accelerometer calibration done by the firmware ("j;", see AccelCalibrationSession).
     * The telemetry reading loop is stopped first: during the calibration nothing else may
     * be sent (any character stops it) and the session reads the text sent by the firmware.
     * The session is queued on the I/O executor after the stop, so the two never read together.
     */
    @Override
    public void startAccelCalibration(AccelCalibrationSession.Listener ui) {
        if (calibration != null && calibration.isRunning()) return;
        calibrating = true;
        reader.stop(false);
        calibration = new AccelCalibrationSession(myBT, new AccelCalibrationSession.Listener() {
            @Override
            public void onCalibrationStarted(boolean anyOrder) {
                ui.onCalibrationStarted(anyOrder);
            }

            @Override
            public void onNextFace(int face, int number) {
                ui.onNextFace(face, number);
            }

            @Override
            public void onFaceDone(int face, int count) {
                ui.onFaceDone(face, count);
            }

            @Override
            public void onCalibrationFinished(AccelCalibrationSession.Result result, String details) {
                endAccelCalibration();
                ui.onCalibrationFinished(result, details);
            }
        });
        calibration.start();
    }

    @Override
    public void cancelAccelCalibration() {
        if (calibration != null) calibration.cancel();
    }

    /**
     * The calibration has ended: restart the stream (the values are now calibrated by the
     * firmware) and detect the rocket axis again.
     */
    private void endAccelCalibration() {
        if (!calibrating) return;
        calibrating = false;
        accelReceived[0] = false;
        accelReceived[1] = false;
        if (statusPage5 != null) statusPage5.resetAxis();
        if (!isFinishing() && !isDestroyed() && myBT.getConnected()) {
            reader.start("y1;");
        }
    }

    /** Meters to feet conversion factor used for the displayed altitude. */
    private static final double METERS_TO_FEET = 3.28084;

    /**
     * Page that displays a telemetry field (msg.what), or null if this page does not exist
     * (not created yet, or not used for the connected altimeter).
     */
    private Fragment pageFor(int what) {
        if (what == 1 || (what >= 9 && what <= 16)) return statusPage1;
        if (what >= 18 && what <= 25) return statusPage2;
        if (what >= 26 && what <= 31) return statusPage1ter;
        return null;
    }

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
        // A calibration in progress is stopped (one character is enough for the firmware)
        if (calibration != null) {
            calibration.release();
            calibration = null;
        }
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

        // Release the position: the GPS is stopped only when no other screen uses it
        if (locIntent != null) {
            LocationService.release(this);
            locIntent = null;
        }

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
        // Not during an accelerometer calibration: the stream is restarted when it ends
        if (myBT.getConnected() && !reader.isRunning() && !calibrating) {
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
            if (LocationService.ACTION_LOCATION.equals(intent.getAction())) {
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

        // Private broadcast of this application only (RECEIVER_NOT_EXPORTED): mandatory
        // flag since Android 14, and no other application can send fake positions
        LocationService.registerPositionReceiver(this, receiver);

        // The position is shared with the other screens (see LocationService.acquire()):
        // acquire it only once for this screen
        if (locIntent == null) {
            locIntent = new Intent(AltimeterStatusTabActivity.this, LocationService.class);
            LocationService.acquire(this);
        }
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
