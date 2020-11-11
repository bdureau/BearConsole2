package com.altimeter.bdureau.bearconsole.telemetry;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import java.io.IOException;

public class AltimeterStatus extends AppCompatActivity {
    Button btnDismiss, btnRecording;
    ConsoleApplication myBT;
    Thread altiStatus;
    boolean status = true;
    boolean recording = false;

    private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
    private TextView txtViewAltitude, txtViewVoltage, txtViewLink, txtTemperature, txtEEpromUsage,txtNbrOfFlight;
    private TextView txtViewOutput4, txtViewBatteryVoltage, txtViewOutput3, txtViewEEprom, txtViewFlight;
    private TextView txtViewLatitude, txtViewLongitude, txtViewLatitudeValue, txtViewLongitudeValue;
    private Switch switchOutput1, switchOutput2, switchOutput3, switchOutput4;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9:
                    // Value 9 contains the output 1 status
                    txtViewOutput1Status.setText(outputStatus((String) msg.obj));
                    break;
                case 10:
                    // Value 10 contains the output 2 status
                    txtViewOutput2Status.setText(outputStatus((String) msg.obj));
                    break;
                case 11:
                    // Value 11 contains the output 3 status
                    txtViewOutput3Status.setText(outputStatus((String) msg.obj));
                    break;
                case 12:
                    //Value 12 contains the output 4 status
                    txtViewOutput4Status.setText(outputStatus((String) msg.obj));
                    break;
                case 1:
                    String myUnits;
                    //Value 1 contains the current altitude
                    if (myBT.getAppConf().getUnits().equals("0"))
                        //Meters
                        myUnits = getResources().getString(R.string.Meters_fview);
                    else
                        //Feet
                        myUnits = getResources().getString(R.string.Feet_fview);
                    txtViewAltitude.setText((String) msg.obj + " " + myUnits);
                    break;
                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        /*double batVolt;

                        batVolt =  (3.1972*((Double.parseDouble(voltage) * 3300) / 4096)/1000);
                        txtViewVoltage.setText(String.format("%.2f",batVolt)+ " Volts");*/
                        txtViewVoltage.setText(voltage + " Volts");
                    } else {
                        txtViewVoltage.setText("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the temperature
                    txtTemperature.setText((String) msg.obj + "°C");
                    break;

                case 15:
                    //Value 15 contains the EEprom usage
                    txtEEpromUsage.setText((String) msg.obj + " %");
                    break;

                case 16:
                    //Value 16 contains the numberof flight
                    txtNbrOfFlight.setText((String) msg.obj );
                    break;
                case 17:
                    //Value 17 contains the latitude
                    txtViewLatitudeValue.setText((String) msg.obj );
                    break;
                case 18:
                    //Value 18 contains the longitude
                    txtViewLongitudeValue.setText((String) msg.obj );
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

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnRecording = (Button) findViewById(R.id.butRecording);
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS"))
            btnRecording.setVisibility(View.VISIBLE);
        else
            btnRecording.setVisibility(View.INVISIBLE);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status) {

                    status = false;
                    myBT.write("h;\n".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                //switch off output
                if (switchOutput1.isChecked()) {
                    myBT.write("k1F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if (switchOutput2.isChecked()) {
                    myBT.write("k2F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if (switchOutput3.isChecked()) {
                    myBT.write("k3F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if (switchOutput4.isChecked()) {
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

        btnRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {

                    recording = false;
                    myBT.write("w0;\n".toString());

                    //myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                    btnRecording.setText("Start Rec");
                } else {
                    recording = true;
                    myBT.write("w1;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                    btnRecording.setText("Stop");
                }

            }
        });
        txtViewOutput1Status = (TextView) findViewById(R.id.txtViewOutput1Status);
        txtViewOutput2Status = (TextView) findViewById(R.id.txtViewOutput2Status);
        txtViewOutput3Status = (TextView) findViewById(R.id.txtViewOutput3Status);
        txtViewOutput4Status = (TextView) findViewById(R.id.txtViewOutput4Status);
        txtViewAltitude = (TextView) findViewById(R.id.txtViewAltitude);
        txtViewVoltage = (TextView) findViewById(R.id.txtViewVoltage);
        txtViewLink = (TextView) findViewById(R.id.txtViewLink);
        txtViewOutput3 = (TextView) findViewById(R.id.txtViewOutput3);
        txtViewOutput4 = (TextView) findViewById(R.id.txtViewOutput4);
        txtViewBatteryVoltage = (TextView) findViewById(R.id.txtViewBatteryVoltage);
        txtTemperature = (TextView) findViewById(R.id.txtViewTemperature);
        txtEEpromUsage= (TextView) findViewById(R.id.txtViewEEpromUsage);
        txtNbrOfFlight= (TextView) findViewById(R.id.txtViewNbrOfFlight);
        txtViewEEprom = (TextView) findViewById(R.id.txtViewEEprom);
        txtViewFlight = (TextView) findViewById(R.id.txtViewFlight);
        txtViewLatitude = (TextView) findViewById(R.id.txtViewLatitude);
        txtViewLongitude = (TextView) findViewById(R.id.txtViewLongitude);
        txtViewLatitudeValue = (TextView) findViewById(R.id.txtViewLatitudeValue);
        txtViewLongitudeValue = (TextView) findViewById(R.id.txtViewLongitudeValue);

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")) {
            txtViewVoltage.setVisibility(View.VISIBLE);
            txtViewBatteryVoltage.setVisibility(View.VISIBLE);
        } else {
            txtViewVoltage.setVisibility(View.INVISIBLE);
            txtViewBatteryVoltage.setVisibility(View.INVISIBLE);
        }
        if (!myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo")) {
            txtViewOutput3Status.setVisibility(View.VISIBLE);
            txtViewOutput3.setVisibility(View.VISIBLE);
        } else {
            txtViewOutput3Status.setVisibility(View.INVISIBLE);
            txtViewOutput3.setVisibility(View.INVISIBLE);
        }

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS") || myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            txtViewOutput4Status.setVisibility(View.VISIBLE);
            txtViewOutput4.setVisibility(View.VISIBLE);
        } else {
            txtViewOutput4Status.setVisibility(View.INVISIBLE);
            txtViewOutput4.setVisibility(View.INVISIBLE);
        }
        //hide eeprom
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo") || myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            txtViewEEprom.setVisibility(View.INVISIBLE);
            txtViewFlight.setVisibility(View.INVISIBLE);
            txtEEpromUsage.setVisibility(View.INVISIBLE);
            txtNbrOfFlight.setVisibility(View.INVISIBLE);
        }
        else {
            txtViewEEprom.setVisibility(View.VISIBLE);
            txtViewFlight.setVisibility(View.VISIBLE);
            txtEEpromUsage.setVisibility(View.VISIBLE);
            txtNbrOfFlight.setVisibility(View.VISIBLE);
        }
        //hide GPS
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")){
            txtViewLatitude.setVisibility(View.VISIBLE);
            txtViewLongitude.setVisibility(View.VISIBLE);
            txtViewLatitudeValue.setVisibility(View.VISIBLE);
            txtViewLongitudeValue.setVisibility(View.VISIBLE);
        } else {
            txtViewLatitude.setVisibility(View.INVISIBLE);
            txtViewLongitude.setVisibility(View.INVISIBLE);
            txtViewLatitudeValue.setVisibility(View.INVISIBLE);
            txtViewLongitudeValue.setVisibility(View.INVISIBLE);
        }
        txtViewLink.setText(myBT.getConnectionType());

        switchOutput1 = (Switch) findViewById(R.id.switchOutput1);
        switchOutput2 = (Switch) findViewById(R.id.switchOutput2);
        switchOutput3 = (Switch) findViewById(R.id.switchOutput3);
        switchOutput4 = (Switch) findViewById(R.id.switchOutput4);
        if (!myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo"))
            switchOutput3.setVisibility(View.VISIBLE);
        else
            switchOutput3.setVisibility(View.INVISIBLE);
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS"))
            switchOutput4.setVisibility(View.VISIBLE);
        else
            switchOutput4.setVisibility(View.INVISIBLE);

        switchOutput1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput1.isChecked())
                    myBT.write("k1T;\n".toString());
                else
                    myBT.write("k1F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });


        switchOutput1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput1.isChecked())
                    myBT.write("k1T;\n".toString());
                else
                    myBT.write("k1F;\n".toString());

                myBT.flush();
                myBT.clearInput();

            }
        });

        switchOutput2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput2.isChecked())
                    myBT.write("k2T;\n".toString());
                else
                    myBT.write("k2F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        switchOutput2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput2.isChecked())
                    myBT.write("k2T;\n".toString());
                else
                    myBT.write("k2F;\n".toString());

                myBT.flush();
                myBT.clearInput();

            }
        });
        switchOutput3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput3.isChecked())
                    myBT.write("k3T;\n".toString());
                else
                    myBT.write("k3F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        switchOutput3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput3.isChecked())
                    myBT.write("k3T;\n".toString());
                else
                    myBT.write("k3F;\n".toString());

                myBT.flush();
                myBT.clearInput();

            }
        });
        switchOutput4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchOutput4.isChecked())
                    myBT.write("k4T;\n".toString());
                else
                    myBT.write("k4F;\n".toString());

                myBT.flush();
                myBT.clearInput();
            }
        });
        switchOutput4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchOutput4.isChecked())
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
                while (true) {
                    if (!status) break;
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

        myMessage = myBT.ReadResult(10000);
    }

    private String outputStatus(String msg) {
        String res = "";
        switch (msg) {
            case "0":
                res = getResources().getString(R.string.no_continuity);
                break;
            case "1":
                res = getResources().getString(R.string.continuity);
                break;
            case "-1":
                res = getResources().getString(R.string.disabled);
                break;
        }
        return res;
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
