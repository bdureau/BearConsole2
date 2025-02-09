package com.altimeter.bdureau.bearconsole;
/**
 * @description: Main screen of the altimeter console. Here you will find all the buttons.
 * @author: boris.dureau@neuf.fr
 *
 **/

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.Flash.FlashFirmware;
import com.altimeter.bdureau.bearconsole.Flight.FlightListActivity;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AltiTabConfigActivity;
import com.altimeter.bdureau.bearconsole.config.AppTabConfigActivity;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
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
//import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainScreenActivity extends AppCompatActivity {
    public String TAG = "MainScreenActivity";
    String address = null;
    CardView btnAltiSettings, btnReadFlights, btnConnectDisconnect, btnContinuityOnOff, btnReset;
    CardView btnTelemetry, btnStatus, btnFlashFirmware, btnTrack, btnInfo;

    ImageView image_settings, image_flights, image_telemetry, image_continuity, image_reset,
            image_status, image_track, image_flash, image_info, image_connect;
    TextView text_settings, text_flights, text_telemetry, text_continuity, text_reset,
            text_status, text_track, text_flash, text_info, text_connect;

    private UsbManager usbManager;
    private UsbDevice device;
    private static final String ACTION_USB_PERMISSION = "com.altimeter.bdureau.bearconsole.USB_PERMISSION";

    ConsoleApplication myBT;
    private AltiConfigData AltiCfg = null;
    private FirmwareCompatibility firmCompat =null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            //msg("in broadcast");
            Log.d(TAG,"in broadcast");
            Log.d(TAG,"Action1:" + intent.getAction());
                    //msg("Action1:" + intent.getAction());
            String action = intent.getAction();
            if (action.equals(ACTION_USB_PERMISSION)) {
                Log.d(TAG, "ACTION_USB_PERMISSION");
                boolean granted = true;
                if(android.os.Build.VERSION.SDK_INT < 31)
                    granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);

                if (granted) {
                    Log.d(TAG, "Permission granted");
                    if (myBT.connect(usbManager, device, Integer.parseInt(/*myBT.getAppConf().getBaudRateValue()*/ "38400"))) {
                        Log.d(TAG, "baud:"+myBT.getAppConf().getBaudRateValue());
                        myBT.setConnected(true);
                        Log.d(TAG, "about to enableUI");
                        EnableUI();
                        btnFlashFirmware.setEnabled(false);
                        myBT.setConnectionType("usb");
                        text_connect.setText(getResources().getString(R.string.disconnect));
                    } else {
                        msg("Cannot connect");
                    }

                } else {
                    msg("PERM NOT GRANTED");
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                //usbManager = (UsbManager) getSystemService(getApplicationContext().USB_SERVICE);
                //myBT.setConnected(false);
                msg("I can connect via usb");
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (myBT.getConnectionType().equals("usb"))
                    if (myBT.getConnected()) {
                        myBT.Disconnect();
                        DisableUI();
                        // we are disconnected enable flash firmware
                        setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
                        text_connect.setText(getResources().getString(R.string.connect));
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(getApplicationContext().USB_SERVICE);

        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();

        //This will check if the firmware is compatible with the app and advice on flashing the firmware
        firmCompat = new FirmwareCompatibility();
        setContentView(R.layout.activity_main_screen);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(broadcastReceiver, filter, getApplicationContext().RECEIVER_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }

        //cards
        btnAltiSettings = (CardView) findViewById(R.id.settings_card);
        btnReadFlights = (CardView) findViewById(R.id.flights_card);
        btnTelemetry = (CardView) findViewById(R.id.telemetry_card);
        btnStatus = (CardView) findViewById(R.id.status_card);
        btnFlashFirmware = (CardView) findViewById(R.id.flash_card);
        btnConnectDisconnect = (CardView) findViewById(R.id.connect_card);
        btnContinuityOnOff = (CardView) findViewById(R.id.continuity_card);
        btnReset = (CardView) findViewById(R.id.reset_card);
        btnTrack = (CardView) findViewById(R.id.track_card);
        btnInfo = (CardView) findViewById(R.id.info_card);

        // images
        image_settings = (ImageView) findViewById(R.id.image_settings);
        image_flights = (ImageView) findViewById(R.id.image_flights);
        image_telemetry = (ImageView) findViewById(R.id.image_telemetry);
        image_continuity = (ImageView) findViewById(R.id.image_continuity);
        image_reset = (ImageView) findViewById(R.id.image_reset);
        image_status = (ImageView) findViewById(R.id.image_status);
        image_track = (ImageView) findViewById(R.id.image_status);
        image_flash = (ImageView) findViewById(R.id.image_flash);
        image_info = (ImageView) findViewById(R.id.image_info);
        image_connect = (ImageView) findViewById(R.id.image_connect);

        // text
        text_settings = (TextView) findViewById(R.id.text_settings);
        text_flights = (TextView) findViewById(R.id.text_flights);
        text_telemetry = (TextView) findViewById(R.id.text_telemetry);
        text_continuity = (TextView) findViewById(R.id.text_continuity);
        text_reset = (TextView) findViewById(R.id.text_reset);
        text_status = (TextView) findViewById(R.id.text_status);
        text_track = (TextView) findViewById(R.id.text_track);
        text_flash = (TextView) findViewById(R.id.text_flash);
        text_info = (TextView) findViewById(R.id.text_info);
        text_connect = (TextView) findViewById(R.id.text_connect);

        if (myBT.getConnected()) {
            EnableUI();
            //btnFlashFirmware.setEnabled(false);
            setEnabledCard(false, btnFlashFirmware, image_flash, text_flash);
        } else {
            DisableUI();
            text_connect.setText(getResources().getString(R.string.connect));
            setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
        }

        //commands to be sent to bluetooth
        btnAltiSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreenActivity.this, AltiTabConfigActivity.class);
                //Change the activity.
                startActivity(i);
            }
        });

        // about
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreenActivity.this, AboutActivity.class);
                //Change the activity.
                startActivity(i);
            }
        });
        //commands to be sent to flash the firmware
        btnFlashFirmware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                Intent i = new Intent(MainScreenActivity.this, FlashFirmware.class);
                //Change the activity.
                startActivity(i);
            }
        });

        btnReadFlights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreenActivity.this, FlightListActivity.class);
                startActivity(i);
            }
        });

        btnTelemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;".toString());
                }
                Intent i = new Intent(MainScreenActivity.this, TelemetryTabActivity.class);
                startActivity(i);
            }
        });

        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;".toString());
                }
                Intent i = new Intent(MainScreenActivity.this, AltimeterStatusTabActivity.class);
                startActivity(i);
            }
        });

        btnTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn on telemetry
                if (myBT.getConnected()) {
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y1;".toString());
                }
                Intent i = null;
                if (myBT.getAppConf().getUseOpenMap()) {
                    i = new Intent(MainScreenActivity.this, RocketTrackOpenMap.class);
                } else {
                    i = new Intent(MainScreenActivity.this, RocketTrackGoogleMap.class);
                }
                startActivity(i);
            }
        });

        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType()==0)
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {
                    Disconnect(); //close connection
                    DisableUI();
                    // we are disconnected enable flash firmware
                    setEnabledCard(true, btnFlashFirmware, image_flash, text_flash);
                    text_connect.setText(getResources().getString(R.string.connect));
                } else {
                    if (myBT.getConnectionType().equals("bluetooth")) {
                        address = myBT.getAddress();
                        AlertDialog.Builder builder = null;
                        AlertDialog alert;

                        builder = new AlertDialog.Builder(MainScreenActivity.this);
                        if (address != null) {
                            builder.setMessage(getString(R.string.do_you_want_to_connect_to_module) + myBT.getModuleName() + " ?")
                                    .setTitle("")
                                    .setCancelable(false)
                                    .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int id) {
                                            dialog.cancel();
                                            new ConnectBT().execute();
                                            if (myBT.getConnected()) {
                                                EnableUI();
                                                // cannot flash firmware if connected
                                                setEnabledCard(false, btnFlashFirmware, image_flash, text_flash);
                                                text_connect.setText(getResources().getString(R.string.disconnect));
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int id) {
                                            dialog.cancel();
                                            // choose the bluetooth device
                                            Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
                                            startActivity(i);
                                        }
                                    });
                            alert = builder.create();
                            alert.show();
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
                            startActivity(i);
                        }

                    } else {
                        myBT.setModuleName("USB");
                        //this is a USB connection
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (!usbDevices.isEmpty()) {

                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();
                                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                                        device.getVendorId(), device.getProductId(),
                                        UsbSerialDevice.isSupported(device),
                                        device.getDeviceClass(), device.getDeviceSubclass(),
                                        device.getDeviceName()));

                                if (UsbSerialDevice.isSupported(device)) {
                                    PendingIntent pi;
                                    int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;
                                    pi = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), flags);
                                    Log.d(TAG, "Requesting permission: vendor" + device.getVendorId());
                                    usbManager.requestPermission(device, pi);

                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreenActivity.this, ResetSettingsActivity.class);
                startActivity(i);
            }
        });

        btnContinuityOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myBT.getConnected()) {
                    // Send command to turn the continuity on or off
                    myBT.write("c;".toString());
                    myBT.flush();
                    myBT.clearInput();
                }
                msg(getString(R.string.continuity_changed));
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        /*IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }*/
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        //unregisterReceiver(broadcastReceiver);

    }

    private void DisableUI() {
        setEnabledCard(false, btnAltiSettings, image_settings, text_settings);

        setEnabledCard(false, btnReadFlights, image_flights, text_flights);

        setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);

        setEnabledCard(false, btnContinuityOnOff, image_continuity, text_continuity);

        setEnabledCard(false, btnReset, image_reset, text_reset);

        setEnabledCard(false, btnStatus, image_status, text_status);

        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();
    }

    private void setEnabledCard(boolean enable, CardView card, ImageView image, TextView text) {
        card.setEnabled(enable);
        image.setImageAlpha(enable? 0xFF : 0x3F);

    }

    private void EnableUI() {

        boolean success;
        //wait so that the port open
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        success = readConfig();
        //second attempt
        if (!success) {
            Log.d(TAG,"Config attempt 2");
            success = readConfig();
        }
        //third attempt
        if (!success) {
            Log.d(TAG,"Config attempt 3");
            success = readConfig();
        }
        //fourth and last
        if (!success) {
            Log.d(TAG,"Config attempt 4");
            success = readConfig();
        }

        /*
        supported firmware
        AltiMultiSTM32
        AltiServo
        AltiMultiV2
        AltiGPS
        AltiMulti
        AltiDuo
         */
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiV2") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMulti") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_375") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter") ||
                myBT.getAltiConfigData().getAltimeterName().equals("UltimateAltimeter") ||
                myBT.getAltiConfigData().getAltimeterName().equals("TTGOMiniBearAltimeter")
        ) {
            Log.d(TAG, "altimeter name: " + myBT.getAltiConfigData().getAltimeterName());
            if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                    myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter") ||
                    myBT.getAltiConfigData().getAltimeterName().equals("UltimateAltimeter") ||
                    myBT.getAltiConfigData().getAltimeterName().equals("TTGOMiniBearAltimeter")
            ) {
                setEnabledCard(false, btnContinuityOnOff, image_continuity, text_continuity);
            } else {
                //enable it for bT or USB only if full support
                if (myBT.getAppConf().getConnectionType()==0 ||
                        (myBT.getAppConf().getConnectionType()==1 && myBT.getAppConf().getFullUSBSupport()))
                    setEnabledCard(true, btnContinuityOnOff, image_continuity, text_continuity);
                else
                    setEnabledCard(false, btnContinuityOnOff, image_continuity, text_continuity);
            }

            if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                    myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")) {
                setEnabledCard(false, btnReadFlights, image_flights, text_flights);
            } else {
                //enable it for bT or USB only if full support
                if (myBT.getAppConf().getConnectionType()==GlobalConfig.ConnectionType.BT ||
                        (myBT.getAppConf().getConnectionType()==GlobalConfig.ConnectionType.USB && myBT.getAppConf().getFullUSBSupport()))
                    setEnabledCard(true, btnReadFlights, image_flights, text_flights);
                else
                    setEnabledCard(false, btnReadFlights, image_flights, text_flights);
            }
            setEnabledCard(true, btnTelemetry, image_telemetry, text_telemetry);
            if(myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                setEnabledCard(true, btnTrack, image_track, text_track);
            } else {
                setEnabledCard(false, btnTrack, image_track, text_track);
            }
            //enable it for bT or USB only if full support
            if (myBT.getAppConf().getConnectionType()== GlobalConfig.ConnectionType.BT ||
                    (myBT.getAppConf().getConnectionType()== GlobalConfig.ConnectionType.USB && myBT.getAppConf().getFullUSBSupport())) {
                setEnabledCard(true, btnAltiSettings, image_settings, text_settings);
                setEnabledCard(true, btnReset, image_reset, text_reset);
                setEnabledCard(true, btnStatus, image_status, text_status);
            } else {
                setEnabledCard(false, btnAltiSettings, image_settings, text_settings);
                setEnabledCard(false, btnReset, image_reset, text_reset);
                setEnabledCard(true, btnStatus, image_status, text_status);
            }

            if(myBT.getAltiConfigData().getAltimeterName().equals("TTGOBearAltimeter")){
                //setEnabledCard(false, btnAltiSettings, image_settings, text_settings);
            }
            if(myBT.getAltiConfigData().getAltimeterName().equals("UltimateAltimeter") ||
                    myBT.getAltiConfigData().getAltimeterName().equals("TTGOMiniBearAltimeter")){
                setEnabledCard(false, btnAltiSettings, image_settings, text_settings);
                setEnabledCard(false, btnTelemetry, image_telemetry, text_telemetry);
            }
            text_connect.setText(getResources().getString(R.string.disconnect));
            setEnabledCard(false, btnFlashFirmware, image_flash, text_flash);

            if(!firmCompat.IsCompatible(myBT.getAltiConfigData().getAltimeterName(),
                    myBT.getAltiConfigData().getAltiMajorVersion()+ "."+
                            myBT.getAltiConfigData().getAltiMinorVersion())) {
                msg(getString(R.string.flash_advice_msg));
            }
            else {
                msg(getResources().getString(R.string.MS_msg4));
            }
        } else {
            msg(getString(R.string.unsuported_firmware_msg) + ": " + myBT.getAltiConfigData().getAltimeterName());
            myBT.Disconnect();
        }
        // now enable or disable the menu entries by invalidating it
        invalidateOptionsMenu();
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void Disconnect() {
        myBT.Disconnect();
    }


    private boolean readConfig() {
        // ask for config
        boolean success = false;
        if (myBT.getConnected()) {

            Log.d(TAG, "Retreiving altimeter config...");
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d(TAG, "after m0");
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            Log.d(TAG, "before myMessage");
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            Log.d(TAG, myMessage);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("b;".toString());
                myBT.flush();

                //get the results
                //wait for the result to come back
                try {
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {

                }
                myMessage = myBT.ReadResult(3000);
                Log.d(TAG, myMessage);
                //reading the config
                if (myMessage.equals("start alticonfig end")) {
                    try {
                        AltiCfg = myBT.getAltiConfigData();
                        success = true;
                    } catch (Exception e) {
                        //  msg("pb ready data");
                    }
                } else {
                    // msg("data not ready");
                    //try again
                    myBT.setDataReady(false);
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("b;".toString());
                    myBT.flush();
                    //get the results
                    //wait for the result to come back
                    try {
                        while (myBT.getInputStream().available() <= 0) ;
                    } catch (IOException e) {

                    }
                    myMessage = myBT.ReadResult(3000);
                    //reading the config
                    if (myMessage.equals("start alticonfig end")) {
                        try {
                            AltiCfg = myBT.getAltiConfigData();
                            success = true;
                        } catch (Exception e) {
                            //  msg("pb ready data");
                        }
                    }
                }
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                //switch on the main loop before sending the config
                myBT.write("m1;".toString());

                //wait for the result to come back
                try {
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {

                }
                myMessage = myBT.ReadResult(3000);
                if (myMessage.equals("OK")) {
                    myBT.flush();
                }
            }
        } else {
            Log.d(TAG, "Not connected");
        }
        return success;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        myBT.getAppConf().ReadConfig();
       //only enable bluetooth module search if connection type is bluetooth
        if(myBT.getAppConf().getConnectionType()==1) {
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
        }
        else {
            menu.findItem(R.id.action_bluetooth).setEnabled(true);
        }

        //if we are connected then enable some menu options and if not disable them
        if(myBT.getConnected()){
            // We are connected so no need to choose the bluetooth
            menu.findItem(R.id.action_bluetooth).setEnabled(false);
            // We are connected so we do not want to configure the 3DR module
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(false);
            // same goes for the BT module
            menu.findItem(R.id.action_modbt_settings).setEnabled(false);
            // same goes for the lora module
            menu.findItem(R.id.action_modlora_settings).setEnabled(false);
            menu.findItem(R.id.action_modlorae32_settings).setEnabled(false);
            // Allow connection testing
            menu.findItem(R.id.action_test_connection).setEnabled(true);
        } else {
            // not connected so allow those
            menu.findItem(R.id.action_mod3dr_settings).setEnabled(true);
            menu.findItem(R.id.action_modbt_settings).setEnabled(true);
            menu.findItem(R.id.action_modlora_settings).setEnabled(true);
            menu.findItem(R.id.action_modlorae32_settings).setEnabled(true);
            //cannot do connection testing until we are connected
            menu.findItem(R.id.action_test_connection).setEnabled(false);
        }
        return true;

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //Open application configuration
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainScreenActivity.this, AppTabConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(MainScreenActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_bluetooth) {
            // choose the bluetooth device
            Intent i = new Intent(MainScreenActivity.this, SearchBluetooth.class);
            startActivity(i);
            return true;
        }
        //Open the about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(MainScreenActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        //Open the 3DR module config
        if (id == R.id.action_mod3dr_settings) {
            Intent i = new Intent(MainScreenActivity.this, Config3DR.class);
            startActivity(i);
            return true;
        }
        //Open the bluetooth module config
        if (id == R.id.action_modbt_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigBT.class);
            startActivity(i);
            return true;
        }
        //Open the lora module config
        if (id == R.id.action_modlora_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigLoraE220.class);
            startActivity(i);
            return true;
        }
        //Open the lora module config
        if (id == R.id.action_modlorae32_settings) {
            Intent i = new Intent(MainScreenActivity.this, ConfigLoraE32.class);
            startActivity(i);
            return true;
        }
        //Test current connection
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

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "MainScreen Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.altimeter.bdureau.bearconsole/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "MainScreen Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.altimeter.bdureau.bearconsole/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /* This is the Bluetooth connection sub class */
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;
        private AlertDialog alert;
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //"Connecting...", "Please wait!!!"
            builder = new AlertDialog.Builder(MainScreenActivity.this);
            //Connecting...
            builder.setMessage(getResources().getString(R.string.MS_msg1)+ "\n"+ myBT.getModuleName())
                    .setTitle(getResources().getString(R.string.MS_msg2))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.main_screen_actity), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            myBT.setExit(true);
                            myBT.Disconnect();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            if (!myBT.getConnected()) {
                if (myBT.connect())
                    ConnectSuccess = true;
                else
                    ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {

            } else {
                //Connected.
                myBT.setConnected(true);
                EnableUI();
            }
            alert.dismiss();
        }
    }

    public class FirmwareCompatibility {
        // Create a hash map
        public HashMap<String, String> hm;
        FirmwareCompatibility() {
            hm =null;
            hm = new HashMap();
            //init compatible versions
            Add("AltiMulti", "1.28,2.0");
            Add("AltiMultiV2", "1.28,2.0");
            Add("AltiMultiSTM32", "1.28,2.0");
            Add("AltiServo", "1.6");
            Add("AltiGPS", "1.7");
            Add("AltiDuo", "1.9");
            Add("AltiMultiESP32", "1.28,2.0,2.1");
            Add("AltiMultiESP32_accel", "2.0,2.1");
            Add("AltiMultiESP32_accel_375", "2.0,2.1");
            Add("AltiMultiESP32_accel_345", "2.0,2.1");
            Add("TTGOBearAltimeter", "0.4,0.5");
            Add("TTGOMiniBearAltimeter", "0.2,0.3");
            //Add("TTGOMiniBearAltimeter", "0.4");
            //Add("UltimateAltimeter", "0.2,0.3");
            Add("UltimateAltimeter", "0.4,0.5");

        }
        public void Add ( String altiName, String verList) {
            hm.put(altiName,verList);
            //Log.d(TAG, altiName + verList);
        }

        public boolean IsCompatible(String altiName,String ver) {
            boolean compatible = false;
            String compatFirmwareList="";

            Log.d(TAG, "Check compatibility");

            Set set = hm.entrySet();

            // Get an iterator
            Iterator i = set.iterator();
            while(i.hasNext()) {
                Map.Entry me = (Map.Entry)i.next();

                Log.d(TAG,"key:" + me.getKey().toString());
                if (me.getKey().equals(altiName)){
                    Log.d(TAG,"name:" +altiName);
                    compatFirmwareList = me.getValue().toString();
                    Log.d(TAG,"version:" +me.getValue().toString());
                    break;

                }
            }
            String firmwareVersion[] = compatFirmwareList.split(",");
            for (int j = 0; j < firmwareVersion.length; j++ ){
                if(firmwareVersion[j].equals(ver)) {
                    compatible = true;
                    Log.d(TAG,"compatible");
                }
            }

            return compatible;
        }
    }
}
