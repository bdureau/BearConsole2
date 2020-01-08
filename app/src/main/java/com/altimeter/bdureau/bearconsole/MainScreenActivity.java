package com.altimeter.bdureau.bearconsole;
/**
 * @description: Main screen of the altimeter console
 * @author: boris.dureau@neuf.fr
 **/

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//import com.felhr.usbserial.UsbSerialDevice;
//import com.felhr.usbserial.UsbSerialInterface;
import com.altimeter.bdureau.bearconsole.Flight.FlightListActivity;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AltimeterTabConfigActivity;
import com.altimeter.bdureau.bearconsole.config.AppConfigActivity;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.telemetry.AltimeterStatus;
import com.altimeter.bdureau.bearconsole.telemetry.Telemetry;
import com.altimeter.bdureau.bearconsole.telemetry.TelemetryMp;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainScreenActivity extends AppCompatActivity {
    String address = null;
    Button btnAltiSettings, btnReadFlights, btnConnectDisconnect, btnContinuityOnOff, btnReset;
    Button btnTelemetry, btnStatus, btnFlashFirmware;
    boolean flashFimware =false;
    //Button btnTest;
    private ProgressDialog progress;
    UsbManager usbManager;
    UsbDevice device;
    public final String ACTION_USB_PERMISSION = "com.altimeter.bdureau.bearconsole.USB_PERMISSION";

    ConsoleApplication myBT;
    private AltiConfigData AltiCfg = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    //if (!flashFimware) {
                        if (myBT.connect(usbManager, device, Integer.parseInt(myBT.getAppConf().getBaudRateValue()))) {
                            myBT.setConnected(true);
                            EnableUI();
                            btnFlashFirmware.setEnabled(false);
                            myBT.setConnectionType("usb");
                            btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                        }
                   /* }
                    else {
                        if (myBT.connectFirmware(usbManager, device, Integer.parseInt("115200"))) {
                            myBT.setConnected(true);
                            EnableUI();
                            myBT.setConnectionType("usb");
                            btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                            Intent i = new Intent(MainScreenActivity.this, FlashFirmware.class);
                            //Change the activity.
                            startActivity(i);
                        }
                    }*/
                } else {
                    msg("PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                msg("I can connect via usb");
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (myBT.getConnectionType().equals("usb"))
                    if (myBT.getConnected()) {
                        myBT.Disconnect();
                        btnConnectDisconnect.setText(getResources().getString(R.string.connect));
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_main_screen);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        btnAltiSettings = (Button) findViewById(R.id.butAltiSettings);
        btnReadFlights = (Button) findViewById(R.id.butReadFlight);
        btnTelemetry = (Button) findViewById(R.id.butTelemetry);
        btnStatus = (Button) findViewById(R.id.butStatus);
        btnFlashFirmware = (Button) findViewById(R.id.butFlashFirmware);
        btnConnectDisconnect = (Button) findViewById(R.id.butDisconnect);

        btnContinuityOnOff = (Button) findViewById(R.id.butContinuityOnOff);

        btnReset = (Button) findViewById(R.id.butReset);

        if (myBT.getConnected()) {
            EnableUI();
            btnFlashFirmware.setEnabled(false);
        } else {
            DisableUI();
            btnConnectDisconnect.setText(R.string.connect);
            btnFlashFirmware.setEnabled(true);
        }


        //commands to be sent to bluetooth
        btnAltiSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Intent i = new Intent(MainScreenActivity.this, AltimeterConfigActivity.class);
                Intent i = new Intent(MainScreenActivity.this, AltimeterTabConfigActivity.class);
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

                    myBT.write("y1;\n".toString());
                }
                Intent i;
                if (myBT.getAppConf().getGraphicsLibType().equals("0"))
                    i = new Intent(MainScreenActivity.this, Telemetry.class);
                else
                    i = new Intent(MainScreenActivity.this, TelemetryMp.class);
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

                    myBT.write("y1;\n".toString());
                }
                Intent i = new Intent(MainScreenActivity.this, AltimeterStatus.class);
                startActivity(i);
            }
        });
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.getAppConf().ReadConfig();
                if (myBT.getAppConf().getConnectionType().equals("0"))
                    myBT.setConnectionType("bluetooth");
                else
                    myBT.setConnectionType("usb");

                if (myBT.getConnected()) {
                    Disconnect(); //close connection
                    DisableUI();
                    // we are disconnected enable flash firmware
                    btnFlashFirmware.setEnabled(true);
                    btnConnectDisconnect.setText(R.string.connect);
                } else {
                    if (myBT.getConnectionType().equals("bluetooth")) {
                        address = myBT.getAddress();

                        if (address != null) {
                            new ConnectBT().execute(); //Call the class to connect
                            if (myBT.getConnected()) {
                                EnableUI();
                                // cannot flash firmware if connected
                                btnFlashFirmware.setEnabled(false);
                                btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainScreenActivity.this, MainActivity.class);
                            startActivity(i);
                        }
                    } else {
                        //this is a USB connection
                        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                        if (!usbDevices.isEmpty()) {
                            boolean keep = true;
                            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                                device = entry.getValue();
                                int deviceVID = device.getVendorId();

                                PendingIntent pi = PendingIntent.getBroadcast(MainScreenActivity.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                                usbManager.requestPermission(device, pi);
                                keep = false;


                                if (!keep)
                                    break;
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
                    // Send command to change the continuity
                    myBT.write("c;\n".toString());
                    myBT.flush();
                    myBT.clearInput();
                }
                msg("Continuity changed");
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void DisableUI() {
        btnAltiSettings.setEnabled(false);
        btnReadFlights.setEnabled(false);
        btnTelemetry.setEnabled(false);
        btnContinuityOnOff.setEnabled(false);
        btnReset.setEnabled(false);
        btnStatus.setEnabled(false);
    }

    private void EnableUI() {
        btnAltiSettings.setEnabled(true);
        readConfig();
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            btnContinuityOnOff.setEnabled(false);
        } else {
            btnContinuityOnOff.setEnabled(true);
        }

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")) {
            btnReadFlights.setEnabled(false);
        } else {
            btnReadFlights.setEnabled(true);
        }
        btnTelemetry.setEnabled(true);

        btnReset.setEnabled(true);
        btnStatus.setEnabled(true);
        btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void Disconnect() {
        myBT.Disconnect();
    }

    private void readConfig() {
        // ask for config
        if (myBT.getConnected()) {

            //msg("Retreiving altimeter config...");
            myBT.setDataReady(false);

            myBT.flush();
            myBT.clearInput();

            myBT.write("b;\n".toString());

            myBT.flush();


            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
        }
        //reading the config
        if (myBT.getConnected()) {
            String myMessage = "";
            long timeOut = 10000;
            long startTime = System.currentTimeMillis();

            myMessage = myBT.ReadResult(10000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myMessage = myBT.ReadResult(10000);
                /*try {
                    Thread.sleep(1000);                 //1000 milliseconds is one second.
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }*/
            }
            if (myMessage.equals("start alticonfig end")) {
                try {
                    AltiCfg = myBT.getAltiConfigData();
                    //msg(AltiCfg.getAltimeterName());
                } catch (Exception e) {
                    msg("pb ready data");
                }
            } else {
                msg("data not ready: " + myMessage);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainScreenActivity.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(MainScreenActivity.this, HelpActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_bluetooth) {
            // choose the bluetooth device
            Intent i = new Intent(MainScreenActivity.this, MainActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            Intent i = new Intent(MainScreenActivity.this, AboutActivity.class);
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
          /*  progress = ProgressDialog.show(MainScreenActivity.this,
                    getResources().getString(R.string.MS_msg1),
                    getResources().getString(R.string.MS_msg2));  //show a progress dialog
                    */
            builder = new AlertDialog.Builder(MainScreenActivity.this);
            //Connecting...
            builder.setMessage(getResources().getString(R.string.MS_msg1))
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

            //if (myBT.getBtSocket() == null || !myBT.getConnected()) {
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
            //msg(myBT.lastData);
            if (!ConnectSuccess) {
                //msg(myBT.lastReadResult);
                //msg(myBT.lastData);
                //Connection Failed. Is it a SPP Bluetooth? Try again.
                //msg(getResources().getString(R.string.MS_msg3));
                //finish();
            } else {
                //Connected.
                msg(getResources().getString(R.string.MS_msg4));
                //isBtConnected = true;
                myBT.setConnected(true);
                EnableUI();
                btnConnectDisconnect.setText(getResources().getString(R.string.disconnect));
                btnFlashFirmware.setEnabled(false);
            }
            //progress.dismiss();
            alert.dismiss();
        }
    }
}
