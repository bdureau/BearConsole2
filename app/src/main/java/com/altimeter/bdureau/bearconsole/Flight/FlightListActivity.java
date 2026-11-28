package com.altimeter.bdureau.bearconsole.Flight;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightListActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Retrieves recorded flight data curves from the altimeter via Bluetooth or USB.
 *  Stores datasets into a local FlightData repository and computes derived flight
 *  telemetry metrics (velocity and vertical acceleration) directly from the
 *  altitude time-series using zero-phase centered finite differences.
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

public class FlightListActivity extends BaseActivity {
    public static final String SELECTED_FLIGHT = "MyFlight";
    private static final String TAG = "FlightListActivity";

    /**
     * Silence after which a download is considered finished or lost (ms). 60 s, not less:
     * an ATmega328 altimeter does not read its serial port while it beeps the last altitudes
     * (up to ~45 s after a high flight), so the first answer can come that late.
     */
    private static final long DOWNLOAD_IDLE_TIMEOUT_MS = 60000;
    /** Safety limit for the download of one flight (ms); the user can cancel before. */
    private static final long DOWNLOAD_MAX_DURATION_MS = 30 * 60 * 1000;

    private ListView flightList = null;
    private ConsoleApplication myBT;
    // Never null: an empty list is displayed when nothing could be downloaded
    private List<String> flightNames = new ArrayList<>();
    private FlightData myflight = null;
    private AlertDialog alert;
    private Button buttonDismiss;

    // Posts progress messages to the UI thread; purged in onDestroy() to avoid leaks
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Retrieve selected flight identifier
            String currentFlight = ((TextView) v).getText().toString();
            Intent i = new Intent(FlightListActivity.this, FlightViewTabActivity.class);

            // Pass flight identifier and open the detailed analysis screen
            i.putExtra(SELECTED_FLIGHT, currentFlight);
            startActivity(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.Flight_list);
        // Retrieve global application state handle
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_flight_list);
        buttonDismiss = findViewById(R.id.butDismiss);

        // Start background retrieval task
        new RetrieveFlights().execute();

        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Exit activity and return to parent screen
            }
        });
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    /**
     * Asynchronous background worker responsible for serial communication with the
     * altimeter flight computer, downloading recorded logs and computing numerical derivatives.
     */
    private class RetrieveFlights extends AsyncTask<Void, Void, Void> {
        private AlertDialog.Builder builder = null;
        // Written by the UI thread (Cancel button), read by the download thread
        private volatile boolean canceled = false;
        private String myMessage = "";
        private int nbrOfFlight = 0;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlightListActivity.this);
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
        protected Void doInBackground(Void... devices) {
            if (myBT.getConnected()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                // Flush serial I/O buffers before polling
                myBT.flush();
                myBT.clearInput();
                myBT.setNbrOfFlight(0);
                myBT.getFlightData().ClearFlight();

                // Send command to retrieve total count of logged flights
                myBT.write("n;");
                myBT.flush();

                // Bounded wait for the first bytes of the answer (never blocks forever)
                myBT.waitForData(10000);

                myBT.setDataReady(false);
                myBT.initFlightData();

                myMessage = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS, null);

                if ("start nbrOfFlight end".equals(myMessage)) {
                    nbrOfFlight = myBT.getNbrOfFlight();
                }

                // Query and download telemetry samples for each recorded flight
                if (nbrOfFlight > 0) {
                    for (int j = 0; j < nbrOfFlight && !canceled; j++) {
                        dialogAppend(getString(R.string.retrieving_flight) + (j + 1));
                        Log.d(TAG, "Retrieving Flight Nbr: " + j);
                        myBT.flush();
                        myBT.clearInput();

                        // Command to dump samples for flight index j
                        myBT.write("r" + j + ";");
                        myBT.flush();

                        // Bounded wait for the first bytes of the answer (never blocks forever)
                        myBT.waitForData(10000);

                        myBT.setDataReady(false);
                        // Inactivity timeout: a long flight sent at a low speed is never cut
                        myMessage = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS, null);

                        if (canceled) {
                            Log.d(TAG, "Canceled retrieval by user");
                            // The answer of flight j ends with "end" only if it was complete.
                            // Otherwise drop this flight (and only this one): its data are partial.
                            if (myMessage == null || !myMessage.endsWith("end")) {
                                String partial = myBT.getFlightName(j + 1);
                                if (myBT.getFlightData().removeFlight(partial)) {
                                    Log.d(TAG, "Incomplete flight dropped: " + partial);
                                }
                            }
                        }
                    }
                }

                myflight = myBT.getFlightData();
                flightNames = myflight.getAllFlightNames2();

                // Compute vertical speed and acceleration profiles using centered differences
                for (String flight : flightNames) {
                    myflight.computeVelocityAndAcceleration(flight);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (isFinishing() || isDestroyed()) return;

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(FlightListActivity.this,
                    android.R.layout.simple_list_item_1, flightNames);
            // Sorted by flight number ("Flight 9" before "Flight 10" and "Flight 100")
            adapter.sort(FlightData.FLIGHT_NAME_ORDER);

            flightList = findViewById(R.id.listViewFlightList);
            flightList.setAdapter(adapter);
            flightList.setOnItemClickListener(myListClickListener);

            if (alert != null && alert.isShowing()) {
                alert.dismiss();
            }

            if (canceled) {
                msg(getResources().getString(R.string.flight_retrieval_canceled));
            }

            // Nothing downloaded (no flight recorded, or connection lost)
            if (flightNames.isEmpty() && !canceled) {
                msg(getResources().getString(R.string.flight_have_been_recorded));
            }
        }
    }

    /**
     * Thread-safe helper to update progress dialog message from worker threads.
     */
    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (alert != null && alert.isShowing()) {
                    alert.setMessage(ftext);
                }
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