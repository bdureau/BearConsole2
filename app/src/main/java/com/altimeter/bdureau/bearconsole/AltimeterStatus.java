package com.altimeter.bdureau.bearconsole;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.afree.data.xy.XYSeriesCollection;

import java.io.IOException;

public class AltimeterStatus extends AppCompatActivity {
    Button btnDismiss;
    ConsoleApplication myBT;
    Thread altiStatus;
    boolean status = true;

    private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
    private TextView txtViewAltitude,txtViewVoltage,txtViewLink;

    Handler handler = new Handler () {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9:
                    // Value 9 contain the output 1 status
                    txtViewOutput1Status.setText(outputStatus((String)msg.obj));
                    break;
                case 10:
                    // Value 10 contain the output 2 status
                    txtViewOutput2Status.setText(outputStatus((String)msg.obj));
                    break;
                case 11:
                    // Value 11 contain the output 3 status
                    txtViewOutput3Status.setText(outputStatus((String)msg.obj));
                    break;
                case 12:
                    //Value 12 contain the output 4 status
                    txtViewOutput4Status.setText(outputStatus((String)msg.obj));
                    break;
                case 1:
                    //Value 1 contain the current altitude
                    txtViewAltitude.setText((String)msg.obj);
                    break;
                case 13:
                    //Value 13 contain the battery voltage
                    String voltage = (String)msg.obj;
                    if(voltage.matches("\\d+(?:\\.\\d+)?")) {
                        double batVolt;

                        batVolt =  (3.05*((Double.parseDouble(voltage) * 3300) / 4096)/1000);
                        txtViewVoltage.setText(String.format("%.2f",batVolt));
                    }
                    else {
                        txtViewVoltage.setText("NA");
                    }
                    break;

            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_altimeter_status);
        myBT = (ConsoleApplication) getApplication();

        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_altimeter_status);

        btnDismiss = (Button)findViewById(R.id.butDismiss);

        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (status) {
                    status = false;
                    myBT.write("h;\n".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                finish();      //exit the  activity
            }
        });

        txtViewOutput1Status =(TextView)findViewById(R.id.txtViewOutput1Status);
        txtViewOutput2Status =(TextView)findViewById(R.id.txtViewOutput2Status);
        txtViewOutput3Status =(TextView)findViewById(R.id.txtViewOutput3Status);
        txtViewOutput4Status =(TextView)findViewById(R.id.txtViewOutput4Status);
        txtViewAltitude =(TextView)findViewById(R.id.txtViewAltitude);
        txtViewVoltage=(TextView)findViewById(R.id.txtViewVoltage);
        txtViewLink=(TextView)findViewById(R.id.txtViewLink);

        txtViewLink.setText(myBT.getConnectionType());
        myBT.setHandler(handler);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true){
                    if(!status) break;
                    myBT.ReadResult();
                }
            }
        };

        altiStatus = new Thread(r);
        altiStatus.start();


    }

    @Override
    protected void onStop() {
        super.onStop();
        //msg("On stop");
        if (status) {
            status = false;
            myBT.write("h;\n".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
            //finish();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;\n".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage =myBT.ReadResult();
    }
    private String outputStatus(String msg) {
        String res="";
        switch (msg) {
            case "0":
                res ="No continuity";
                break;
            case "1":
                res ="Continuity";
                break;
            case "-1":
                res ="Disabled";
                break;
        }
        return res;
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
