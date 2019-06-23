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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

//import org.afree.data.xy.XYSeriesCollection;

import java.io.IOException;

public class AltimeterStatus extends AppCompatActivity {
    Button btnDismiss;
    ConsoleApplication myBT;
    Thread altiStatus;
    boolean status = true;

    private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
    private TextView txtViewAltitude,txtViewVoltage,txtViewLink, txtTemperature;
    private TextView txtViewOutput4,txtViewBatteryVoltage;
    private Switch switchOutput1, switchOutput2, switchOutput3, switchOutput4;

    Handler handler = new Handler () {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9:
                    // Value 9 contains the output 1 status
                    txtViewOutput1Status.setText(outputStatus((String)msg.obj));
                    break;
                case 10:
                    // Value 10 contains the output 2 status
                    txtViewOutput2Status.setText(outputStatus((String)msg.obj));
                    break;
                case 11:
                    // Value 11 contains the output 3 status
                    txtViewOutput3Status.setText(outputStatus((String)msg.obj));
                    break;
                case 12:
                    //Value 12 contains the output 4 status
                    txtViewOutput4Status.setText(outputStatus((String)msg.obj));
                    break;
                case 1:
                    String myUnits;
                    //Value 1 contains the current altitude
                    if (myBT.getAppConf().getUnits().equals("0"))
                        //Meters
                        myUnits=getResources().getString(R.string.Meters_fview);
                    else
                        //Feet
                        myUnits = getResources().getString(R.string.Feet_fview);
                    txtViewAltitude.setText((String)msg.obj+ " " +myUnits);
                    break;
                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String)msg.obj;
                    if(voltage.matches("\\d+(?:\\.\\d+)?")) {
                        double batVolt;

                        batVolt =  (3.05*((Double.parseDouble(voltage) * 3300) / 4096)/1000);
                        txtViewVoltage.setText(String.format("%.2f",batVolt)+ " Volts");
                    }
                    else {
                        txtViewVoltage.setText("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the temperature
                    txtTemperature.setText((String) msg.obj + "°C");
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
                //switch off output
                if(switchOutput1.isChecked()) {
                    myBT.write("k1F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if(switchOutput2.isChecked()) {
                    myBT.write("k2F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if(switchOutput3.isChecked()) {
                    myBT.write("k3F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if(switchOutput4.isChecked()) {
                    myBT.write("k4F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                //turn off telemetry
                myBT.flush();
                myBT.clearInput();
                myBT.write("y0;\n".toString());
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
        txtViewOutput4=(TextView)findViewById(R.id.txtViewOutput4);
        txtViewBatteryVoltage=(TextView)findViewById(R.id.txtViewBatteryVoltage);
        txtTemperature = (TextView)findViewById(R.id.txtViewTemperature);

        if(myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")) {
            txtViewVoltage.setVisibility(View.VISIBLE);
            txtViewBatteryVoltage.setVisibility(View.VISIBLE);
        }
        else {
            txtViewVoltage.setVisibility(View.INVISIBLE);
            txtViewBatteryVoltage.setVisibility(View.INVISIBLE);
        }
        if(myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")|| myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            txtViewOutput4Status.setVisibility(View.VISIBLE);
            txtViewOutput4.setVisibility(View.VISIBLE);
        }
        else {
            txtViewOutput4Status.setVisibility(View.INVISIBLE);
            txtViewOutput4.setVisibility(View.INVISIBLE);
        }
        txtViewLink.setText(myBT.getConnectionType());

        switchOutput1  =(Switch)findViewById(R.id.switchOutput1);
        switchOutput2  =(Switch)findViewById(R.id.switchOutput2);
        switchOutput3  =(Switch)findViewById(R.id.switchOutput3);
        switchOutput4  =(Switch)findViewById(R.id.switchOutput4);
        if(!myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo"))
            switchOutput3.setVisibility(View.VISIBLE);
        else
            switchOutput3.setVisibility(View.INVISIBLE);
        if(myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiServo"))
            switchOutput4.setVisibility(View.VISIBLE);
        else
            switchOutput4.setVisibility(View.INVISIBLE);

        switchOutput1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(switchOutput1.isChecked())
                    myBT.write("k1T;\n".toString());
                else
                    myBT.write("k1F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        switchOutput1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(switchOutput1.isChecked())
                    myBT.write("k1T;\n".toString());
                else
                    myBT.write("k1F;\n".toString());

                myBT.flush();
                myBT.clearInput();
                return false;
            }
        });
       /* switchOutput1.setOnScrollChangeListener(new View.OnScrollChangeListener() {
        }
        });*/


        switchOutput2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(switchOutput2.isChecked())
                    myBT.write("k2T;\n".toString());
                else
                    myBT.write("k2F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        switchOutput3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(switchOutput3.isChecked())
                    myBT.write("k3T;\n".toString());
                else
                    myBT.write("k3F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        switchOutput4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(switchOutput4.isChecked())
                    myBT.write("k4T;\n".toString());
                else
                    myBT.write("k4F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        myBT.setHandler(handler);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true){
                    if(!status) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        altiStatus = new Thread(r);
        altiStatus.start();

       // msg(myBT.getAltiConfigData().getAltimeterName());
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

        myMessage =myBT.ReadResult(10000);
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
