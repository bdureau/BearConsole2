package com.altimeter.bdureau.bearconsole.connection;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: TestConnection
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Link quality test between the phone and the altimeter (Bluetooth or telemetry radio). The
 *  test thread repeatedly sends the "o;" command and counts the packets sent, received, valid
 *  (checksum OK) and invalid, which gives the packet loss of the link. The thread stops when
 *  the screen is left.
 * ============================================================================
 */

import androidx.appcompat.app.AppCompatDelegate;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.BaseActivity;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;


import java.io.IOException;


public class TestConnection extends BaseActivity {

    Thread testTelemetry;

    ConsoleApplication myBT;


    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private TextView tvRead;
    private Button butStartStop, butDismiss;
    // Written by the UI thread, read by the test thread: must be volatile
    private volatile boolean testRunning = false;
    private TextView textNbrPacketsSentVal, textNbrPacketsReceivedVal, textNbrPacketsReceivedValOK;
    private TextView textNbrPacketsReceivedValNotOK, textNbrPacketsReceivedValPercentageError;
    private long packetsSent = 0;
    private long packetsReceived = 0;
    private long packetsReceivedOK = 0;
    private long packetsReceivedNotOK = 0;
    private long packetsReceivedPercentageError = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_test_connection);


        tvRead = (TextView) findViewById(R.id.tvRead);
        butDismiss = (Button) findViewById(R.id.butDismiss);
        butStartStop =(Button) findViewById(R.id.butStartStop);

        textNbrPacketsSentVal = (TextView) findViewById(R.id.textNbrPacketsSentVal);
        textNbrPacketsReceivedVal = (TextView) findViewById(R.id.textNbrPacketsReceivedVal);
        textNbrPacketsReceivedValOK = (TextView) findViewById(R.id.textNbrPacketsReceivedValOK);
        textNbrPacketsReceivedValNotOK = (TextView) findViewById(R.id.textNbrPacketsReceivedValNotOK);
        textNbrPacketsReceivedValPercentageError = (TextView) findViewById(R.id.textNbrPacketsReceivedValPercentageError);


        builder = new AlertDialog.Builder(this);
        //Display info message
        builder.setMessage(R.string.msg_test_connection)
                .setTitle(R.string.msg_title_info)
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

                    butStartStop.setText(R.string.test_connection_start);

                }
                else {
                    testRunning = true;
                    startTestTelemetry();
                    butStartStop.setText(R.string.test_connection_stop);
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
                    packetsSent ++;
                    setPacketSent(""+packetsSent);
                    myBT.setDataReady(false);
                    //get the results
                    //wait for the result to come back
                        // Bounded wait for the first bytes of the answer (never blocks forever)
                    myBT.waitForData(3000);
                    myMessage = myBT.ReadResult(3000, () -> testRunning);
                    if (myBT.getDataReady()) {
                        Log.d("test conection", myMessage +"\n");
                        //reading the config
                        if (myMessage.equals("start testTrame end")) {
                            ConsoleApplication.TestTrame testTrame = myBT.getTestTrame();
                            tvAppend(tvRead, testTrame.getCurrentTrame()+"\n");
                            packetsReceived++;
                            setPacketReceived("" + packetsReceived);
                            if (testTrame.getTrameStatus()) {
                                packetsReceivedOK++;
                                setPacketReceivedOK("" + packetsReceivedOK);
                            } else {
                                packetsReceivedNotOK++;
                                setPacketReceivedNotOK("" + packetsReceivedNotOK);
                            }
                        }
                    }
                    else {
                        packetsReceivedNotOK++;
                        setPacketReceivedNotOK("" + packetsReceivedNotOK);
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

    // Posts the test results to the UI thread; purged in onDestroy()
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onDestroy() {
        // Stop the test thread (also when leaving with the Back button) and drop pending updates
        testRunning = false;
        myBT.setExit(true);
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);

            }
        });
    }

    private void setPacketSent(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textNbrPacketsSentVal.setText(ftext);
            }
        });
    }

    private void setPacketReceived(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textNbrPacketsReceivedVal.setText(ftext);
            }
        });
    }

    private void setPacketReceivedOK(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textNbrPacketsReceivedValOK.setText(ftext);
            }
        });
    }
    private void setPacketReceivedNotOK(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textNbrPacketsReceivedValNotOK.setText(ftext);
            }
        });
    }
    private void setPacketReceivedPercentageError(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textNbrPacketsReceivedValPercentageError.setText(ftext);
            }
        });
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
            i.putExtra("help_file", "help_test_connection");
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
