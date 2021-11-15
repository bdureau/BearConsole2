package com.altimeter.bdureau.bearconsole.connection;

import androidx.appcompat.app.AppCompatActivity;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.io.IOException;


public class TestConnection extends AppCompatActivity {



    Thread testTelemetry;

    ConsoleApplication myBT;


    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private TextView tvRead;
    private Button butStartStop, butDismiss;
    private boolean testRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_test_connection);


        tvRead = (TextView) findViewById(R.id.tvRead);
        butDismiss = (Button) findViewById(R.id.butDismiss);
        butStartStop =(Button) findViewById(R.id.butStartStop);


        builder = new AlertDialog.Builder(this);
        //Display info message
        builder.setMessage("This will allow to test the connection reliability")
                .setTitle("Info")
                .setCancelable(false)
                .setPositiveButton(R.string.bt_info_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {


                        dialog.cancel();

                    }
                });

        alert = builder.create();
        alert.show();

        butStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testRunning) {
                    testRunning = false;

                    //myBT.write("h;\n".toString());
                    butStartStop.setText("Start");
                    //myBT.setExit(true);
                    //myBT.clearInput();
                    //myBT.flush();
                }
                else {
                    testRunning = true;
                    startTestTelemetry();
                    butStartStop.setText("Stop");
                }

            }
        });

        butDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testRunning) {
                    testRunning = false;

                }

                finish();      //exit the activity
            }
        });
    }

    public void startTestTelemetry() {
        testRunning = true;

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!testRunning) break;

                    String myMessage = "";
                    myBT.write("o;".toString());
                    myBT.flush();
                    //get the results
                    //wait for the result to come back
                    try {
                        while (myBT.getInputStream().available() <= 0) ;
                    } catch (IOException e) {
                        //success = false;
                    }
                    myMessage = myBT.ReadResult(3000);
                    //reading the config
                    if (myMessage.equals("start testTrame end")) {
                       ConsoleApplication.TestTrame testTrame = myBT.getTestTrame();
                        testTrame.getCurrentTrame();
                    }

                }
            }
        };

        testTelemetry = new Thread(r);
        testTelemetry.start();


    }
    public void EnableUI() {


    }

    public void DisableUI() {


    }



    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void onClickDismiss(View v) {
        finish();
    }

    public void onClickStartStop(View v) {
        finish();
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


        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_configBT");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
