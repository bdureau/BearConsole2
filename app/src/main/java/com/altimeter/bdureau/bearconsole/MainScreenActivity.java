package com.altimeter.bdureau.bearconsole;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: MainScreenActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Main dashboard activity for the altimeter console application.
 *  This activity provides a unified interface to:
 *    - Manage physical USB Serial and Bluetooth RFCOMM connections to rocket avionics.
 *    - Query, parse, and validate hardware firmware configuration in the background
 *      without stalling or freezing the Android UI thread.
 *    - Handle runtime broadcast events (USB device attached/detached/permission grants)
 *      complying with modern Android 14+ (API 34+) broadcast security requirements.
 *    - Route users to specialized operational screens: flight logs retrieval, real-time
 *      telemetry graphing, GPS map tracking, flash firmware, module settings, and diagnostic tests.
 *  Every blocking serial operation (USB / Bluetooth connection, configuration read, commands)
 *  runs on the shared I/O executor of the application; results are posted back to the UI thread.
 * ============================================================================
 */

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.altimeter.bdureau.bearconsole.Flash.FlashFirmware;
import com.altimeter.bdureau.bearconsole.Flight.FlightListActivity;
import com.altimeter.bdureau.bearconsole.Flight.FlightRecoveryActivity;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AltiTabConfigActivity;
import com.altimeter.bdureau.bearconsole.config.AppTabConfigActivity;
import com.altimeter.bdureau.bearconsole.config.ConfigModules.Config3DR;
import com.altimeter.bdureau.bearconsole.config.ConfigModules.ConfigBT;
import com.altimeter.bdureau.bearconsole.config.ConfigModules.ConfigLoraE220;
import com.altimeter.bdureau.bearconsole.config.ConfigModules.ConfigLoraE32;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.altimeter.bdureau.bearconsole.connection.SearchBluetooth;
import com.altimeter.bdureau.bearconsole.connection.TestConnection;
import com.altimeter.bdureau.bearconsole.telemetry.AltimeterStatusTabActivity;
import com.altimeter.bdureau.bearconsole.telemetry.RocketTrackGoogleMap;
import com.altimeter.bdureau.bearconsole.telemetry.RocketTrackOpenMap;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryTabActivity;
import com.felhr.usbserial.UsbSerialDevice;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class MainScreenActivity extends BaseActivity {
    private static final String TAG = "MainScreenActivity";

    // Bluetooth MAC address or device identifier
    private String address = null;

    // UI Dashboard Cards
    private CardView btnAltiSettings, btnReadFlights, btnConnectDisconnect, btnContinuityOnOff, btnReset;
    private CardView btnTelemetry, btnStatus, btnFlashFirmware, btnTrack, btnInfo;

    // UI Card Icons (used to toggle visual alpha when disabled)
    private ImageView image_settings, image_flights, image_telemetry, image_continuity, image_reset,
            image_status, image_track, image_flash, image_info, image_connect;

    // UI Card Labels
    private TextView text_settings, text_flights, text_telemetry, text_continuity, text_reset,
            text_status, text_track, text_flash, text_info, text_connect;

    // Android USB Host Service & Selected Target Device
    private UsbManager usbManager;
    private UsbDevice device;

    // Custom internal action for USB permission callback
    private static final String ACTION_USB_PERMISSION = "com.altimeter.bdureau.bearconsole.USB_PERMISSION";

    // Application state holder (handles active Bluetooth & USB communication pipelines)
    private ConsoleApplication myBT;
    private AltiConfigData AltiCfg = null;
    private FirmwareCompatibility firmCompat = null;

    // Google App Indexing client
    private GoogleApiClient client;

    // Shared, ordered background executor of the application for serial communication
    private ExecutorService ioExecutor;

    // UI Handler for posting results back to the Android Main Thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * BroadcastReceiver handling USB runtime lifecycle events and user authorization responses.
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "in broadcast - Action: " + action);

            if (action == null) {
                return;
            }

            // Case 1: User responded to the USB permission dialog
            if (ACTION_USB_PERMISSION.equals(action)) {
                Log.d(TAG, "ACTION_USB_PERMISSION received");

                // Safely extract the UsbDevice instance across Android versions
                UsbDevice usbDevice;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
                } else {
                    usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                }

                // Check whether the user granted permission
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                if (granted) {
                    Log.d(TAG, "USB Permission successfully granted by user");
                    if (usbDevice != null) {
                        device = usbDevice;
                    }

                    // Parse configured baud rate from application settings with fallback
                    int baudRate = 38400;
                    try {
                        baudRate = Integer.parseInt(myBT.getAppConf().getBaudRateValue());
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to parse baud rate, fallback to default: 38400", e);
                    }

                    // Open the USB serial connection in background: the connection check waits
                    // for the altimeter answer (several seconds), which must never block the UI
                    final UsbDevice target = device;
                    final int speed = baudRate;
                    ioExecutor.execute(() -> {
                        final boolean connected = target != null && myBT.connect(usbManager, target, speed);
                        mainHandler.post(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (connected) {
                                Log.d(TAG, "Connected to USB device with baud rate: " + speed);
                                myBT.setConnected(true);
                                myBT.setConnectionType("usb");
                                text_connect.setText(getResources().getString(R.string.disconnect));
                                btnFlashFirmware.setEnabled(false);

                                // Fetch altimeter configuration asynchronously without blocking UI
                                loadConfigurationAsync();
                            } else {
                                msg(getString(R.string.main_usb_cannot_connect));
                            }
                        });
                    });
                } else {
                    Log.w(TAG, "USB permission was denied by the user");
                    msg(getString(R.string.main_usb_permission_denied));
                }

            }
            // Case 2: A physical USB OTG cable / device was plugged in
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "USB device attached event received");
                msg(getString(R.string.main_usb_device_attached));

            }
            // Case 3: The USB cable / device was physically unplugged
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "USB device detached event received");
                if ("usb".equalsIgnoreCase(myBT.getConnectionType()) && myBT.getConnected()) {
                    // Closing the port also wakes up any thread blocked on the stream
                    myBT.Disconnect();
                    DisableUI();
                    setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
                    text_connect.setText(getResources().getString(R.string.connect));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Retrieve global application state and instantiate firmware version checker
        myBT = (ConsoleApplication) getApplication();
        ioExecutor = myBT.getIoExecutor();
        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);

        // Configure USB event filters
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        // Register receiver securely using ContextCompat (enforces RECEIVER_NOT_EXPORTED on API 34+)
        ContextCompat.registerReceiver(
                this,
                broadcastReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        // Bind CardView controls
        btnAltiSettings = findViewById(R.id.settings_card);
        btnReadFlights = findViewById(R.id.flights_card);
        btnTelemetry = findViewById(R.id.telemetry_card);
        btnStatus = findViewById(R.id.status_card);
        btnFlashFirmware = findViewById(R.id.flash_card);
        btnConnectDisconnect = findViewById(R.id.connect_card);
        btnContinuityOnOff = findViewById(R.id.continuity_card);
        btnReset = findViewById(R.id.reset_card);
        btnTrack = findViewById(R.id.track_card);
        btnInfo = findViewById(R.id.info_card);

        // Bind CardView icon images
        image_settings = findViewById(R.id.image_settings);
        image_flights = findViewById(R.id.image_flights);
        image_telemetry = findViewById(R.id.image_telemetry);
        image_continuity = findViewById(R.id.image_continuity);
        image_reset = findViewById(R.id.image_reset);
        image_status = findViewById(R.id.image_status);
        image_track = findViewById(R.id.image_status);
        image_flash = findViewById(R.id.image_flash);
        image_info = findViewById(R.id.image_info);
        image_connect = findViewById(R.id.image_connect);

        // Bind CardView text labels
        text_settings = findViewById(R.id.text_settings);
        text_flights = findViewById(R.id.text_flights);
        text_telemetry = findViewById(R.id.text_telemetry);
        text_continuity = findViewById(R.id.text_continuity);
        text_reset = findViewById(R.id.text_reset);
        text_status = findViewById(R.id.text_status);
        text_track = findViewById(R.id.text_track);
        text_flash = findViewById(R.id.text_flash);
        text_info = findViewById(R.id.text_info);
        text_connect = findViewById(R.id.text_connect);

        // Initialize UI state depending on existing active connection
        if (myBT.getConnected()) {
            loadConfigurationAsync();
            setEnabledCard(false, btnFlashFirmware, image_flash, text_flash);
        } else {
            DisableUI();
            text_connect.setText(getResources().getString(R.string.connect));
            setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
        }

        // Setup click listeners for all interactive dashboard cards
        btnAltiSettings.setOnClickListener(v -> {
            Intent i = new Intent(MainScreenActivity.this, AltiTabConfigActivity.class);
            startActivity(i);
        });

        btnInfo.setOnClickListener(v -> {
            Intent i = new Intent(MainScreenActivity.this, AboutActivity.class);
            startActivity(i);
        });

        btnFlashFirmware.setOnClickListener(v -> {
            myBT.getAppConf().ReadConfig();
            Intent i = new Intent(MainScreenActivity.this, FlashFirmware.class);
            startActivity(i);
        });

        btnReadFlights.setOnClickListener(v -> {
            Intent i = new Intent(MainScreenActivity.this, FlightListActivity.class);
            startActivity(i);
        });

        // Telemetry mode: activate live stream ('y1;') asynchronously before opening tab
        btnTelemetry.setOnClickListener(v -> {
            if (myBT.getConnected()) {
                ioExecutor.execute(() -> {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;");
                });
            }
            Intent i = new Intent(MainScreenActivity.this, TelemetryTabActivity.class);
            startActivity(i);
        });

        // Altimeter status mode: activate stream ('y1;') asynchronously before opening tab
        btnStatus.setOnClickListener(v -> {
            if (myBT.getConnected()) {
                ioExecutor.execute(() -> {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;");
                });
            }
            Intent i = new Intent(MainScreenActivity.this, AltimeterStatusTabActivity.class);
            startActivity(i);
        });

        // GPS Rocket Tracking: launch Google Maps or OpenStreetMap based on user preferences
        btnTrack.setOnClickListener(v -> {
            if (myBT.getConnected()) {
                ioExecutor.execute(() -> {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;");
                });
            }
            Intent i;
            if (myBT.getAppConf().getUseOpenMap()) {
                i = new Intent(MainScreenActivity.this, RocketTrackOpenMap.class);
            } else {
                i = new Intent(MainScreenActivity.this, RocketTrackGoogleMap.class);
            }
            startActivity(i);
        });

        // Primary Connect / Disconnect toggle handler
        btnConnectDisconnect.setOnClickListener(v -> {
            myBT.getAppConf().ReadConfig();
            if (myBT.getAppConf().getConnectionType() == 0)
                myBT.setConnectionType("bluetooth");
            else
                myBT.setConnectionType("usb");

            // If already connected, initiate disconnection sequence
            if (myBT.getConnected()) {
                Disconnect();
                DisableUI();
                setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
                text_connect.setText(getResources().getString(R.string.connect));
            } else {
                // Handling Bluetooth connection path
                if (myBT.getConnectionType().equals("bluetooth")) {
                    address = myBT.getAddress();
                    if (address != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainScreenActivity.this);
                        builder.setMessage(getString(R.string.do_you_want_to_connect_to_module) + myBT.getModuleName() + " ?")
                                .setTitle("")
                                .setCancelable(false)
                                .setPositiveButton(getResources().getString(R.string.Yes), (dialog, id) -> {
                                    dialog.cancel();
                                    new ConnectBT().execute();
                                })
                                .setNegativeButton(R.string.no, (dialog, id) -> {
                                    dialog.cancel();
                                    Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
                                    startActivity(i);
                                });
                        builder.create().show();
                    } else {
                        // No remembered address, open device discovery
                        Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
                        startActivity(i);
                    }
                } else {
                    // Handling USB connection path
                    myBT.setModuleName("USB");
                    HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                    if (!usbDevices.isEmpty()) {
                        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                            device = entry.getValue();
                            Log.d(TAG, String.format("USBDevice detected (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                                    device.getVendorId(), device.getProductId(),
                                    UsbSerialDevice.isSupported(device),
                                    device.getDeviceClass(), device.getDeviceSubclass(),
                                    device.getDeviceName()));

                            // Request USB permission if device contains a recognized serial chip
                            if (UsbSerialDevice.isSupported(device)) {
                                int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
                                Intent intent = new Intent(ACTION_USB_PERMISSION);
                                intent.setPackage(getPackageName()); // Explicit package for security

                                PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, flags);
                                Log.d(TAG, "Requesting permission for vendor ID: " + device.getVendorId());
                                usbManager.requestPermission(device, pi);
                                break;
                            }
                        }
                    }
                }
            }
        });

        btnReset.setOnClickListener(v -> {
            Intent i = new Intent(MainScreenActivity.this, ResetSettingsActivity.class);
            startActivity(i);
        });

        // Toggle deployment channel continuity beeper
        btnContinuityOnOff.setOnClickListener(v -> {
            if (myBT.getConnected()) {
                ioExecutor.execute(() -> {
                    myBT.write("c;");
                    myBT.flush();
                    myBT.clearInput();
                });
            }
            msg(getString(R.string.continuity_changed));
        });

        // Initialize Google App Indexing client
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * Executes the configuration retrieval flow in a background thread to prevent UI lockups.
     * Retries up to 4 times before declaring failure.
     */
    private void loadConfigurationAsync() {
        ioExecutor.execute(() -> {
            try {
                // Allow the newly opened serial port 1000ms to stabilize
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}

            boolean success = false;
            for (int attempt = 1; attempt <= 4 && !success; attempt++) {
                if (attempt > 1) {
                    Log.d(TAG, "Config attempt " + attempt);
                }
                success = readConfig();
            }

            final boolean configLoaded = success;
            // Dispatch UI modifications back to the main thread
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                applyConfigToUI(configLoaded);
            });
        });
    }

    /**
     * Inspects the retrieved altimeter configuration and selectively enables/disables
     * dashboard cards according to the capabilities of the specific hardware firmware.
     */
    private void applyConfigToUI(boolean success) {
        if (!success || myBT.getAltiConfigData() == null) {
            msg(getString(R.string.unsuported_firmware_msg));
            return;
        }

        String altiName = myBT.getAltiConfigData().getAltimeterName();
        Log.d(TAG, "Altimeter model confirmed: " + altiName);

        // Verify known altimeter families
        if (altiName.equals("AltiMultiSTM32") ||
                altiName.equals("AltiServo") ||
                altiName.equals("AltiMultiV2") ||
                altiName.equals("AltiGPS") ||
                altiName.equals("AltiDuo") ||
                altiName.equals("AltiMulti") ||
                altiName.equals("AltiMultiESP32") ||
                altiName.equals("AltiMultiESP32_accel") ||
                altiName.equals("AltiMultiESP32_accel_375") ||
                altiName.equals("AltiMultiESP32_accel_345") ||
                altiName.equals("TTGOBearAltimeter") ||
                altiName.equals("UltimateAltimeter") ||
                altiName.equals("TTGOMiniBearAltimeter")) {

            // Continuity beeper feature support check
            if (altiName.equals("AltiServo") ||
                    altiName.equals("TTGOBearAltimeter") ||
                    altiName.equals("UltimateAltimeter") ||
                    altiName.equals("TTGOMiniBearAltimeter")) {
                setEnabledCard(false, btnContinuityOnOff, image_continuity, text_continuity);
            } else {
                if (myBT.getAppConf().getConnectionType() == 0 ||
                        (myBT.getAppConf().getConnectionType() == 1 && myBT.getAppConf().getFullUSBSupport()))
                    setEnabledCard(true, btnContinuityOnOff, image_continuity, text_continuity);
                else
                    setEnabledCard(false, btnContinuityOnOff, image_continuity, text_continuity);
            }

            // Flash memory flight read support check
            if (altiName.equals("AltiServo") || altiName.equals("AltiDuo")) {
                setEnabledCard(false, btnReadFlights, image_flights, text_flights);
            } else {
                if (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT ||
                        (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB && myBT.getAppConf().getFullUSBSupport()))
                    setEnabledCard(true, btnReadFlights, image_flights, text_flights);
                else
                    setEnabledCard(false, btnReadFlights, image_flights, text_flights);
            }

            setEnabledCard(true, btnTelemetry, image_telemetry, text_telemetry);

            // GPS tracking map card visibility
            if (altiName.equals("AltiGPS")) {
                setEnabledCard(true, btnTrack, image_track, text_track);
            } else {
                setEnabledCard(false, btnTrack, image_track, text_track);
            }

            // Altimeter settings and reset controls
            if (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.BT ||
                    (myBT.getAppConf().getConnectionType() == GlobalConfig.ConnectionType.USB && myBT.getAppConf().getFullUSBSupport())) {
                setEnabledCard(true, btnAltiSettings, image_settings, text_settings);
                setEnabledCard(true, btnReset, image_reset, text_reset);
                setEnabledCard(true, btnStatus, image_status, text_status);
            } else {
                setEnabledCard(false, btnAltiSettings, image_settings, text_settings);
                setEnabledCard(false, btnReset, image_reset, text_reset);
                setEnabledCard(true, btnStatus, image_status, text_status);
            }

            // TTGO/Ultimate variants overrides
            if (altiName.equals("UltimateAltimeter") || altiName.equals("TTGOMiniBearAltimeter")) {
                setEnabledCard(false, btnAltiSettings, image_settings, text_settings);
                setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);
            }

            text_connect.setText(getResources().getString(R.string.disconnect));
            setEnabledCard(false, btnFlashFirmware, image_flash, text_flash);

            // Validate running firmware version against compatibility database
            if (!firmCompat.IsCompatible(altiName,
                    myBT.getAltiConfigData().getAltiMajorVersion() + "." +
                            myBT.getAltiConfigData().getAltiMinorVersion())) {
                msg(getString(R.string.flash_advice_msg));
            } else {
                msg(getResources().getString(R.string.MS_msg4));
            }
        } else {
            msg(getString(R.string.unsuported_firmware_msg) + ": " + altiName);
            myBT.Disconnect();
        }

        // Refresh options menu items state
        invalidateOptionsMenu();
    }

    /**
     * Disables functional cards when no communication link is active.
     */
    private void DisableUI() {
        setEnabledCard(false, btnAltiSettings, image_settings, text_settings);
        setEnabledCard(false, btnReadFlights, image_flights, text_flights);
        setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);
        setEnabledCard(false, btnContinuityOnOff, image_continuity, text_continuity);
        setEnabledCard(false, btnReset, image_reset, text_reset);
        setEnabledCard(false, btnStatus, image_status, text_status);
        invalidateOptionsMenu();
    }

    /**
     * Enables or disables a CardView and adjusts icon opacity for visual feedback.
     */
    private void setEnabledCard(boolean enable, CardView card, ImageView image, TextView text) {
        card.setEnabled(enable);
        image.setImageAlpha(enable ? 0xFF : 0x3F);
    }

    /**
     * Helper to show a long Toast notification.
     */
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    /**
     * Closes the active serial connection in the background.
     */
    private void Disconnect() {
        ioExecutor.execute(() -> myBT.Disconnect());
    }

    /**
     * Interrogates the altimeter over the serial stream using the protocol:
     * 1. Send "m0;" -> Halt main sensor sampling loop
     * 2. Send "b;"  -> Request EEPROM configuration dump
     * 3. Send "m1;" -> Resume main sensor loop
     *
     * Runs strictly on the background executor thread.
     */
    private boolean readConfig() {
        boolean success = false;
        if (!myBT.getConnected()) {
            Log.d(TAG, "readConfig: Connection not ready");
            return false;
        }

        Log.d(TAG, "Retrieving altimeter config...");
        myBT.setDataReady(false);
        myBT.flush();
        myBT.clearInput();
        myBT.write("m0;");
        Log.d(TAG, "Sent m0; command");

        waitForDataAvailable(3000);

        String myMessage = myBT.ReadResult(3000);
        Log.d(TAG, "Response to m0;: " + myMessage);

        if ("OK".equals(myMessage)) {
            // Request binary/text config data
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("b;");
            myBT.flush();

            waitForDataAvailable(3000);

            myMessage = myBT.ReadResult(3000);
            Log.d(TAG, "Response to b; attempt 1: " + myMessage);

            if ("start alticonfig end".equals(myMessage)) {
                try {
                    AltiCfg = myBT.getAltiConfigData();
                    success = true;
                } catch (Exception ignored) {}
            } else {
                // Secondary retry for slow microcontrollers
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("b;");
                myBT.flush();

                waitForDataAvailable(3000);

                myMessage = myBT.ReadResult(3000);
                if ("start alticonfig end".equals(myMessage)) {
                    try {
                        AltiCfg = myBT.getAltiConfigData();
                        success = true;
                    } catch (Exception ignored) {}
                }
            }

            // Restore normal altimeter sampling loop
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("m1;");

            waitForDataAvailable(3000);

            myMessage = myBT.ReadResult(3000);
            if ("OK".equals(myMessage)) {
                myBT.flush();
            }
        }
        return success;
    }

    /**
     * Bounded wait for the first bytes of an answer (delegates to ConsoleApplication).
     * Runs on the I/O executor only.
     */
    private void waitForDataAvailable(long timeoutMs) {
        myBT.waitForData(timeoutMs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        myBT.getAppConf().ReadConfig();

        // Control availability of Bluetooth search depending on connection type
        if (myBT.getAppConf().getConnectionType() == 1) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
        } else {
            menu.findItem(R.id.action_bluetooth).setEnabled(true);
        }

        // Toggle configuration menus depending on whether hardware is connected
        if (myBT.getConnected()) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(false);
            menu.findItem(R.id.action_modbt_settings).setEnabled(false);
            menu.findItem(R.id.action_modlora_settings).setEnabled(false);
            menu.findItem(R.id.action_modlorae32_settings).setEnabled(false);
            menu.findItem(R.id.action_test_connection).setEnabled(true);
            menu.findItem(R.id.action_recover_flight).setEnabled(true);
        } else {
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(true);
            menu.findItem(R.id.action_modbt_settings).setEnabled(true);
            menu.findItem(R.id.action_modlora_settings).setEnabled(true);
            menu.findItem(R.id.action_modlorae32_settings).setEnabled(true);
            menu.findItem(R.id.action_test_connection).setEnabled(false);
            menu.findItem(R.id.action_recover_flight).setEnabled(false);
        }
        return true;
    }

    /**
     * Displays a numeric input dialog allowing the user to select the sample recovery size.
     */
    private void showSamplesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.main_samples_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.main_samples_hint);

        builder.setView(input);
        builder.setPositiveButton(R.string.fv_ok, null);
        builder.setNegativeButton(R.string.Cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String value = input.getText().toString().trim();

                // Validation checks
                if (value.isEmpty()) {
                    input.setError(getString(R.string.main_samples_required));
                    return;
                }

                int samples;
                try {
                    samples = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    input.setError(getString(R.string.main_samples_numeric));
                    return;
                }

                if (samples <= 0) {
                    input.setError(getString(R.string.main_samples_too_small));
                    return;
                }

                if (samples >= 10000) {
                    input.setError(getString(R.string.main_samples_too_large));
                    return;
                }

                // Proceed to FlightRecoveryActivity with validated sample count
                Intent i = new Intent(MainScreenActivity.this, FlightRecoveryActivity.class);
                i.putExtra("samples", samples);
                startActivity(i);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(MainScreenActivity.this, AppTabConfigActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_recover_flight) {
            showSamplesDialog();
            return true;
        }
        if (id == R.id.action_help) {
            Intent i = new Intent(MainScreenActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_bluetooth) {
            Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            Intent i = new Intent(MainScreenActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_mod3dr_settings) {
            Intent i = new Intent(MainScreenActivity.this, Config3DR.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_modbt_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigBT.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_modlora_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigLoraE220.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_modlorae32_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigLoraE32.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_test_connection) {
            Intent i = new Intent(MainScreenActivity.this, TestConnection.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "MainScreen Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.altimeter.bdureau.bearconsole/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "MainScreen Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.altimeter.bdureau.bearconsole/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver to avoid Activity leak
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ignored) {}

        // The I/O executor belongs to the application: it keeps running for the other screens
    }

    /**
     * Asynchronous background task for establishing Bluetooth RFCOMM connections.
     */
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private AlertDialog alert;
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainScreenActivity.this);
            builder.setMessage(getResources().getString(R.string.MS_msg1) + "\n" + myBT.getModuleName())
                    .setTitle(getResources().getString(R.string.MS_msg2))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.main_screen_actity), (dialog, id) -> {
                        dialog.cancel();
                        myBT.setExit(true);
                        myBT.Disconnect();
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) {
            if (!myBT.getConnected()) {
                ConnectSuccess = myBT.connect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (ConnectSuccess) {
                myBT.setConnected(true);
                // Trigger configuration reading on the background thread
                loadConfigurationAsync();
            }
            // Safely dismiss dialog to avoid View not attached exceptions
            if (alert != null && alert.isShowing() && !isFinishing() && !isDestroyed()) {
                alert.dismiss();
            }
        }
    }

    /**
     * Helper mapping table to verify if the altimeter firmware version is compatible
     * with the current Android BearConsole release.
     */
    public class FirmwareCompatibility {
        public HashMap<String, String> hm;

        FirmwareCompatibility() {
            hm = new HashMap<>();
            Add("AltiMulti", "1.28,2.0,2.2,2.3");
            Add("AltiMultiV2", "1.28,2.0,2.2,2.3");
            Add("AltiMultiSTM32", "1.28,2.0,2.2,2.3");
            Add("AltiServo", "1.6");
            Add("AltiGPS", "1.7,1.8");
            Add("AltiDuo", "1.9");
            Add("AltiMultiESP32", "1.28,2.0,2.1,2.2,2.3");
            Add("AltiMultiESP32_accel", "2.0,2.1,2.2,2.3");
            Add("AltiMultiESP32_accel_375", "2.0,2.1,2.2,2.3");
            Add("AltiMultiESP32_accel_345", "2.0,2.1,2.2,2.3");
            Add("TTGOBearAltimeter", "0.4,0.5");
            Add("TTGOMiniBearAltimeter", "0.2,0.3");
            Add("UltimateAltimeter", "0.4,0.5");
        }

        public void Add(String altiName, String verList) {
            hm.put(altiName, verList);
        }

        public boolean IsCompatible(String altiName, String ver) {
            boolean compatible = false;
            String compatFirmwareList = "";

            Log.d(TAG, "Checking firmware compatibility for: " + altiName);
            Set set = hm.entrySet();
            Iterator i = set.iterator();
            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                if (me.getKey().equals(altiName)) {
                    compatFirmwareList = me.getValue().toString();
                    break;
                }
            }

            String[] firmwareVersion = compatFirmwareList.split(",");
            for (String s : firmwareVersion) {
                if (s.equals(ver)) {
                    compatible = true;
                    Log.d(TAG, "Firmware " + ver + " is compatible");
                    break;
                }
            }
            return compatible;
        }
    }
}