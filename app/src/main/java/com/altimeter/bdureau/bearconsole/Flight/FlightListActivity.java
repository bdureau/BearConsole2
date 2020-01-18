package com.altimeter.bdureau.bearconsole.Flight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @description: This retrieve the flight list
 * @author: boris.dureau@neuf.fr
 **/
public class FlightListActivity extends AppCompatActivity {
    public static String SELECTED_FLIGHT = "MyFlight";

    ListView flightList = null;
    ConsoleApplication myBT;
    List<String> flightNames = null;
    private FlightData myflight = null;
    private ProgressDialog progress;

    private Button buttonDismiss;

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the flight name
            String currentFlight = ((TextView) v).getText().toString();
            Intent i;
            // Make an intent to start next activity.
            if (myBT.getAppConf().getGraphicsLibType().equals("0"))
                i = new Intent(FlightListActivity.this, FlightViewActivity.class);
            else
                i = new Intent(FlightListActivity.this, FlightViewMpActivity.class);

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
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_list);
        buttonDismiss = (Button) findViewById(R.id.butDismiss);
        new RetrieveFlights().execute();
        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });
    }

    private void getFlights() {

        //get flights
        if (myBT.getConnected()) {
            //clear anything on the connection
            myBT.flush();
            myBT.clearInput();
            myBT.getFlightData().ClearFlight();
            // Send command to retrieve the config
            myBT.write("a;\n".toString());
            myBT.flush();

            try {
                //wait for data to arrive
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {
                // msg("Failed to retrieve flights");
            }
            // if (myBT.getConnected()) {
            String myMessage = "";
            myBT.setDataReady(false);
            myBT.initFlightData();
            myMessage = myBT.ReadResult(60000);

            if (myMessage.equals("start end")) {

                flightNames = new ArrayList<String>();

                myflight = myBT.getFlightData();
                flightNames = myflight.getAllFlightNames2();
            }
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    private class RetrieveFlights extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder = null;
        private AlertDialog alert;
        private Boolean canceled = false;

        @Override
        protected void onPreExecute() {
            //"Retrieving flights..."
            //"Please wait!!!"
           /* progress = ProgressDialog.show(FlightListActivity.this,
                    getResources().getString(R.string.msg7),
                    getResources().getString(R.string.msg8));  //show a progress dialog
*/

            builder = new AlertDialog.Builder(FlightListActivity.this);
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
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            getFlights();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            if (!canceled) {
                final ArrayAdapter adapter = new ArrayAdapter(FlightListActivity.this, android.R.layout.simple_list_item_1, flightNames);
                adapter.sort(new Comparator<String>() {
                    public int compare(String object1, String object2) {
                        return object1.compareTo(object2);
                    }
                });

                flightList = (ListView) findViewById(R.id.listViewFlightList);


                flightList.setAdapter(adapter);
                flightList.setOnItemClickListener(myListClickListener);
            }
            //progress.dismiss();
            alert.dismiss();

            if (canceled)
                msg(getResources().getString(R.string.flight_retrieval_canceled));

            if (myflight == null && !canceled)
                msg(getResources().getString(R.string.flight_have_been_recorded));
        }
    }
}
