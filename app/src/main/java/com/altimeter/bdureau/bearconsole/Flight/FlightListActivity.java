package com.altimeter.bdureau.bearconsole.Flight;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightListActivity
 *
 * @description:
 *  Retrieves recorded flight data curves from the altimeter via Bluetooth or USB.
 *  Stores datasets into a local FlightData repository and computes derived flight
 *  telemetry metrics (velocity and vertical acceleration) directly from the
 *  altitude time-series using zero-phase centered finite differences.
 *
 * @author: boris.dureau@neuf.fr
 * ============================================================================
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FlightListActivity extends AppCompatActivity {
    public static final String SELECTED_FLIGHT = "MyFlight";
    private static final String TAG = "FlightListActivity";

    private ListView flightList = null;
    private ConsoleApplication myBT;
    private List<String> flightNames = null;
    private FlightData myflight = null;
    private AlertDialog alert;
    private Button buttonDismiss;

    private final Handler mHandler = new Handler();

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
        private Boolean canceled = false;
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

                try {
                    // Wait until response bytes are available in buffer
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException ignored) {}

                myBT.setDataReady(false);
                myBT.initFlightData();

                myMessage = myBT.ReadResult(60000);

                if ("start nbrOfFlight end".equals(myMessage)) {
                    nbrOfFlight = myBT.getNbrOfFlight();
                }

                // Query and download telemetry samples for each recorded flight
                if (nbrOfFlight > 0) {
                    for (int j = 0; j < nbrOfFlight; j++) {
                        dialogAppend(getString(R.string.retrieving_flight) + (j + 1));
                        Log.d(TAG, "Retrieving Flight Nbr: " + j);
                        myBT.flush();
                        myBT.clearInput();

                        // Command to dump samples for flight index j
                        myBT.write("r" + j + ";");
                        myBT.flush();

                        try {
                            while (myBT.getInputStream().available() <= 0) ;
                        } catch (IOException ignored) {}

                        myMessage = "";
                        myBT.setDataReady(false);
                        myMessage = myBT.ReadResult(60000);

                        if (canceled) {
                            Log.d(TAG, "Canceled retrieval by user");
                            break;
                        }
                    }
                }

                flightNames = new ArrayList<>();
                myflight = myBT.getFlightData();
                flightNames = myflight.getAllFlightNames2();

                // If operation was cancelled mid-flight, drop incomplete dataset
                if (canceled) {
                    Collections.sort(flightNames);
                    try {
                        if (!flightNames.isEmpty()) {
                            flightNames.remove(flightNames.size() - 1);
                        }
                    } catch (Exception ignored) {}
                }

                // Compute vertical speed and acceleration profiles using centered differences
                for (String flight : flightNames) {
                    computeVelocityAndAcceleration(flight);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(FlightListActivity.this,
                    android.R.layout.simple_list_item_1, flightNames);
            adapter.sort(new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return object1.compareTo(object2);
                }
            });

            flightList = findViewById(R.id.listViewFlightList);
            flightList.setAdapter(adapter);
            flightList.setOnItemClickListener(myListClickListener);

            if (alert != null && alert.isShowing()) {
                alert.dismiss();
            }

            if (canceled) {
                msg(getResources().getString(R.string.flight_retrieval_canceled));
            }

            if (myflight == null && !canceled) {
                msg(getResources().getString(R.string.flight_have_been_recorded));
            }
        }
    }

    /**
     * Numerically computes vertical velocity (m/s or ft/s) and vertical acceleration (m/s² or ft/s²)
     * from altitude time-series using second-order centered finite differences.
     * Preserves physical velocity sign (positive ascent, negative descent) and eliminates
     * phase lag at peak velocity (burnout) and apogee.
     *
     * @param flight Unique identifier name of the target flight dataset
     */
    private void computeVelocityAndAcceleration(String flight) {
        if (myflight == null) return;

        XYSeriesCollection flightCollection = myflight.GetFlightData(flight);
        if (flightCollection == null) return;

        XYSeries altSeries = flightCollection.getSeries(getResources().getString(R.string.curve_altitude));
        if (altSeries == null) return;

        int nbrData = altSeries.getItemCount();
        if (nbrData < 2) return;

        double[] timeSec = new double[nbrData];
        double[] altVal = new double[nbrData];
        double[] speeds = new double[nbrData];

        // Cache time in seconds and altitude values
        for (int i = 0; i < nbrData; i++) {
            timeSec[i] = altSeries.getX(i).doubleValue() / 1000.0; // Convert ms to seconds
            altVal[i] = altSeries.getY(i).doubleValue();
        }

        // --- 1. VELOCITY COMPUTATION (Centered Differences) ---
        for (int i = 0; i < nbrData; i++) {
            double v = 0.0;
            if (i == 0) {
                // Forward difference for the initial sample boundary
                double dt = timeSec[1] - timeSec[0];
                if (dt > 0.0001) v = (altVal[1] - altVal[0]) / dt;
            } else if (i == nbrData - 1) {
                // Backward difference for the final sample boundary
                double dt = timeSec[nbrData - 1] - timeSec[nbrData - 2];
                if (dt > 0.0001) v = (altVal[nbrData - 1] - altVal[nbrData - 2]) / dt;
            } else {
                // Second-order central difference evaluated at the exact midpoint t[i]
                double dt = timeSec[i + 1] - timeSec[i - 1];
                if (dt > 0.0001) {
                    v = (altVal[i + 1] - altVal[i - 1]) / dt;
                } else {
                    v = speeds[i - 1]; // Safeguard against zero dt
                }
            }
            speeds[i] = v;

            // Index 3 corresponds to curve_speed
            myflight.AddToFlight((float) altSeries.getX(i).doubleValue(), (float) v, flight, 3);
        }

        // --- 2. VERTICAL ACCELERATION COMPUTATION (Derivative of Velocity) ---
        for (int i = 0; i < nbrData; i++) {
            double a = 0.0;
            if (i == 0) {
                // Forward difference at initial boundary
                double dt = timeSec[1] - timeSec[0];
                if (dt > 0.0001) a = (speeds[1] - speeds[0]) / dt;
            } else if (i == nbrData - 1) {
                // Backward difference at final boundary
                double dt = timeSec[nbrData - 1] - timeSec[nbrData - 2];
                if (dt > 0.0001) a = (speeds[nbrData - 1] - speeds[nbrData - 2]) / dt;
            } else {
                // Centered difference on the derived velocity profile
                double dt = timeSec[i + 1] - timeSec[i - 1];
                if (dt > 0.0001) {
                    a = (speeds[i + 1] - speeds[i - 1]) / dt;
                } else if (i > 0) {
                    a = 0.0;
                }
            }

            // Index 4 corresponds to curve_accel
            myflight.AddToFlight((float) altSeries.getX(i).doubleValue(), (float) a, flight, 4);
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
}