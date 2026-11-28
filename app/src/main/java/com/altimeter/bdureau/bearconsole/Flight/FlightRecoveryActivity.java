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
 *  was not closed properly (power loss, crash) can still be read. The samples are stored in
 *  the shared FlightData repository and the recovered flight can then be displayed like any
 *  other flight.
 *  The download itself is done by FlightDownloadViewModel: it is started only once even if
 *  the screen is re-created, and it is cancelled when the screen is left.
 * ============================================================================
 */

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.altimeter.bdureau.bearconsole.BaseActivity;
import com.altimeter.bdureau.bearconsole.R;

public class FlightRecoveryActivity extends BaseActivity {
    public static final String SELECTED_FLIGHT = "MyFlight";

    private ListView flightList = null;
    private AlertDialog alert;
    private FlightDownloadViewModel download;

    private final AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Open the detailed analysis screen of the selected flight
            String currentFlight = ((TextView) v).getText().toString();
            Intent i = new Intent(FlightRecoveryActivity.this, FlightViewTabActivity.class);
            i.putExtra(SELECTED_FLIGHT, currentFlight);
            startActivity(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_list);
        flightList = findViewById(R.id.listViewFlightList);
        Button buttonDismiss = findViewById(R.id.butDismiss);
        // Leaving the screen cancels the download (FlightDownloadViewModel.onCleared())
        buttonDismiss.setOnClickListener(v -> finish());

        // Kept across the re-creations of the screen: the download is started only once
        download = new ViewModelProvider(this).get(FlightDownloadViewModel.class);
        if (download.getResult().getValue() == null) {
            showProgressDialog();
            download.startRecovery(getIntent().getIntExtra("samples", 0));
        }
        download.getProgress().observe(this, text -> {
            if (text != null && alert != null && alert.isShowing()) {
                alert.setMessage(text);
            }
        });
        download.getResult().observe(this, this::showResult);
    }

    /** Progress dialog with the Cancel button (also shown again after a re-creation). */
    private void showProgressDialog() {
        alert = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.msg7))
                .setTitle(getString(R.string.msg8))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.flight_list_cancel), (dialog, id) -> {
                    download.cancel();
                    dialog.cancel();
                })
                .create();
        alert.show();
        String text = download.getProgress().getValue();
        if (text != null) alert.setMessage(text);
    }

    /** End of the download: list of the flights, sorted by number, and final message. */
    private void showResult(FlightDownloadViewModel.Result result) {
        if (result == null || isFinishing() || isDestroyed()) return;
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, result.flightNames);
        // Sorted by flight number ("Flight 9" before "Flight 10" and "Flight 100")
        adapter.sort(FlightData.FLIGHT_NAME_ORDER);
        flightList.setAdapter(adapter);
        flightList.setOnItemClickListener(myListClickListener);

        // The message is shown once, not again when the screen is re-created
        if (download.consumeEndMessage()) {
            if (result.canceled) {
                msg(getString(R.string.flight_retrieval_canceled));
            } else if (result.flightNames.isEmpty()) {
                // Nothing downloaded (no flight recorded, or connection lost)
                msg(getString(R.string.flight_have_been_recorded));
            }
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        // The dialog belongs to this instance of the screen (a new one is shown after a
        // re-creation): close it to avoid a leaked window
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        super.onDestroy();
    }
}
