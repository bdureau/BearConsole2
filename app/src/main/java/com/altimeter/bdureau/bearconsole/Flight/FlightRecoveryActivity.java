package com.altimeter.bdureau.bearconsole.Flight;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlightRecoveryActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Flight recovery screen (main menu "Recover flight"). It asks the altimeter for a raw dump of
 *  the number of samples typed by the user ("R<samples>;"), so that a flight whose recording
 *  was not closed properly (power loss, crash) can still be read. The samples are stored in the shared FlightData repository
 *  and the recovered flight can then be displayed like any other flight.
 * ============================================================================
 */

import com.altimeter.bdureau.bearconsole.BaseActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import java.util.ArrayList;
import java.util.List;

public class FlightRecoveryActivity extends BaseActivity {
    public static String SELECTED_FLIGHT = "MyFlight";

    /**
     * Silence after which a download is considered finished or lost (ms). 60 s, not less:
     * an ATmega328 altimeter does not read its serial port while it beeps the last altitudes
     * (up to ~45 s after a high flight), so the first answer can come that late.
     */
    private static final long DOWNLOAD_IDLE_TIMEOUT_MS = 60000;
    /** Safety limit for the whole recovery (ms); the user can cancel before. */
    private static final long DOWNLOAD_MAX_DURATION_MS = 30 * 60 * 1000;

    ListView flightList = null;
    ConsoleApplication myBT;
    // Never null: an empty list is displayed when nothing could be recovered
    List<String> flightNames = new ArrayList<>();
    private FlightData myflight = null;
    private AlertDialog alert;
    private long nbrOfSamples= 0;

    private Button buttonDismiss;

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the flight name
            String currentFlight = ((TextView) v).getText().toString();
            Intent i;
            i = new Intent(FlightRecoveryActivity.this, FlightViewTabActivity.class);

            //Change the activity.
            i.putExtra(SELECTED_FLIGHT, currentFlight);
            startActivity(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed

        setContentView(R.layout.activity_flight_list);

        Intent newint = getIntent();

        //int nbrOfSamples
        nbrOfSamples = newint.getIntExtra("samples", 0);

        buttonDismiss = (Button) findViewById(R.id.butDismiss);
        new RetrieveFlights().execute();
        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    private class RetrieveFlights extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;

        // Written by the UI thread (Cancel button), read by the download thread
        private volatile boolean canceled = false;
        private String myMessage = "";
        private int NbrOfFlight = 0;

        @Override
        protected void onPreExecute() {


            builder = new AlertDialog.Builder(FlightRecoveryActivity.this);
            //Retrieving flights...
            builder.setMessage(getResources().getString(R.string.msg7))
                    .setTitle(getResources().getString(R.string.msg8))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.flight_list_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            myBT.setExit(true);
                            canceled = true;
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        //while the progress dialog is shown, the connection is done in background
        protected Void doInBackground(Void... devices)
        {
            //get flights
            if (myBT.getConnected()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                //clear anything on the connection
                myBT.flush();
                myBT.clearInput();
                myBT.setNbrOfFlight(0);
                myBT.getFlightData().ClearFlight();

                // Send command to retrieve the number of flights
                myBT.write("n;".toString());
                myBT.flush();

                // Bounded wait for the first bytes of the answer (never blocks forever)
                myBT.waitForData(10000);

                myBT.setDataReady(false);
                myBT.initFlightData();

                myMessage = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS, null);

                if (myMessage.equals("start nbrOfFlight end")) {
                    NbrOfFlight = myBT.getNbrOfFlight();
                }

                        dialogAppend(getString(R.string.retrieving_flight) + NbrOfFlight);
                        Log.d("FlightList", "FlightNbr:" + nbrOfSamples);
                        myBT.flush();
                        myBT.clearInput();

                        myBT.write("R" + nbrOfSamples + ";".toString());
                        myBT.flush();

                        // Bounded wait for the first bytes of the answer (never blocks forever)
                        myBT.waitForData(10000);
                        myBT.setDataReady(false);

                        // Inactivity timeout: a long dump sent at a low speed is never cut
                        myMessage = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS, null);

                        if (canceled && (myMessage == null || !myMessage.endsWith("end"))) {
                            // The dump was interrupted: only the flight being received when the
                            // user cancelled is incomplete. Drop it from the repository.
                            Log.d("FlightList", "Canceled retrieval");
                            if (myBT.currentFlightNbr > 0) {
                                myBT.getFlightData().removeFlight(myBT.getFlightName(myBT.currentFlightNbr));
                            }
                        }

                myflight = myBT.getFlightData();
                flightNames = myflight.getAllFlightNames2();
                Log.d("FlightList", "flightNames:" + flightNames);


                // Speed and acceleration: same computation as the flight list screen (signed
                // centred differences, no division by zero, no truncation of the values)
                for (String flight : flightNames) {
                    Log.d("FlightList", "calculate the speed and acceleration");
                    myflight.computeVelocityAndAcceleration(flight);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (isFinishing() || isDestroyed()) return;

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(FlightRecoveryActivity.this,
                    android.R.layout.simple_list_item_1, flightNames);
            // Sorted by flight number ("Flight 9" before "Flight 10" and "Flight 100")
            adapter.sort(FlightData.FLIGHT_NAME_ORDER);

            flightList = (ListView) findViewById(R.id.listViewFlightList);
            flightList.setAdapter(adapter);
            flightList.setOnItemClickListener(myListClickListener);

            if (alert != null && alert.isShowing()) {
                alert.dismiss();
            }

            if (canceled)
                msg(getResources().getString(R.string.flight_retrieval_canceled));

            // Nothing recovered (no flight recorded, or connection lost)
            if (flightNames.isEmpty() && !canceled)
                msg(getResources().getString(R.string.flight_have_been_recorded));
        }
    }

    // Posts progress messages to the UI thread; purged in onDestroy() to avoid leaks
    Handler mHandler = new Handler(Looper.getMainLooper());

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Drop the pending progress updates: the activity must not be referenced any more
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
