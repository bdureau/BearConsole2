package com.altimeter.bdureau.bearconsole.telemetry;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AltimeterStatus extends AppCompatActivity {
    private ViewPager mViewPager;
    SectionsStatusPageAdapter adapter;
    Tab1StatusFragment statusPage1 =null;
    Tab2StatusFragment statusPage2 =null;

    Button btnDismiss, btnRecording;
    private static ConsoleApplication myBT;
    Thread altiStatus;
    boolean status = true;
    boolean recording = false;



    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9:
                    // Value 9 contains the output 1 status
                    //txtViewOutput1Status.setText(outputStatus((String) msg.obj));
                    statusPage1.setOutput1Status((String) msg.obj);
                    break;
                case 10:
                    // Value 10 contains the output 2 status
                    //txtViewOutput2Status.setText(outputStatus((String) msg.obj));
                    statusPage1.setOutput2Status((String) msg.obj);
                    break;
                case 11:
                    // Value 11 contains the output 3 status
                    //txtViewOutput3Status.setText(outputStatus((String) msg.obj));
                    statusPage1.setOutput3Status((String) msg.obj);
                    break;
                case 12:
                    //Value 12 contains the output 4 status
                    //txtViewOutput4Status.setText(outputStatus((String) msg.obj));
                    statusPage1.setOutput4Status((String) msg.obj);
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
                    //txtViewAltitude.setText((String) msg.obj + " " + myUnits);
                    statusPage1.setAltitude((String) msg.obj + " " + myUnits);
                    break;
                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        /*double batVolt;

                        batVolt =  (3.1972*((Double.parseDouble(voltage) * 3300) / 4096)/1000);
                        txtViewVoltage.setText(String.format("%.2f",batVolt)+ " Volts");*/
                        //txtViewVoltage.setText(voltage + " Volts");
                        statusPage1.setVoltage(voltage + " Volts");
                    } else {
                        //txtViewVoltage.setText("NA");
                        statusPage1.setVoltage("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the temperature
                    //txtTemperature.setText((String) msg.obj + "°C");
                    statusPage1.setTemperature((String) msg.obj + "°C");
                    break;

                case 15:
                    //Value 15 contains the EEprom usage
                    //txtEEpromUsage.setText((String) msg.obj + " %");
                    statusPage1.setEEpromUsage((String) msg.obj + " %");
                    break;

                case 16:
                    //Value 16 contains the number of flight
                    //txtNbrOfFlight.setText((String) msg.obj );
                    statusPage1.setNbrOfFlight((String) msg.obj );
                    break;
                case 18:
                    //Value 18 contains the latitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String latitude = (String) msg.obj;
                        if (latitude.matches("\\d+(?:\\.\\d+)?")) {
                            double latitudeVal = Double.parseDouble(latitude) / 100000;
                            //statusPage1.setLatitudeValue((String) msg.obj);
                            statusPage2.setLatitudeValue("" + latitudeVal);
                        }
                    }
                    break;
                case 19:
                    //Value 19 contains the longitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String longitude = (String) msg.obj;
                        if (longitude.matches("\\d+(?:\\.\\d+)?")) {
                            double longitudeVal = Double.parseDouble(longitude) / 100000;
                            //statusPage1.setLongitudeValue((String) msg.obj);
                            statusPage2.setLongitudeValue("" + longitudeVal);
                        }
                    }
                    break;
                case 20:
                    //Value 20 contains the number of satellites
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String nbrOfSatellite = (String) msg.obj;
                        if (nbrOfSatellite.matches("\\d+(?:\\.\\d+)?")) {
                            int nbrOfSatelliteVal = Integer.parseInt(nbrOfSatellite);
                            statusPage2.setSatellitesVal("" + nbrOfSatelliteVal);
                        }
                    }
                    break;
                case 21:
                    //Value 21 contains the hdop
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String hdop = (String) msg.obj;
                        if (hdop.matches("\\d+(?:\\.\\d+)?")) {
                            int hdopVal = Integer.parseInt(hdop);
                            statusPage2.setHdopVal("" + hdopVal);
                        }
                    }
                    break;
                case 22:
                    //Value 22 contains the location age
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String locationAge = (String) msg.obj;
                        if (locationAge.matches("\\d+(?:\\.\\d+)?")) {
                            int locationAgeVal = Integer.parseInt(locationAge);
                            statusPage2.setLocationAgeValue("" + locationAgeVal);
                        }
                    }
                    break;
                case 23:
                    //Value 23 contains the GPS altitude
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSAltitude = (String) msg.obj;
                        if (GPSAltitude.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSAltitudeVal = Integer.parseInt(GPSAltitude);
                            statusPage2.setGPSAltitudeVal("" + GPSAltitudeVal);
                        }
                    }
                    break;
                case 24:
                    // Value 24 contains the GPS Speed
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String GPSSpeed = (String) msg.obj;
                        if (GPSSpeed.matches("\\d+(?:\\.\\d+)?")) {
                            int GPSSpeedVal = Integer.parseInt(GPSSpeed);
                            statusPage2.setGPSSpeedVal("" + GPSSpeedVal);
                        }
                    }
                    break;
                case 25:
                    // Value 25 contains the time for sat acquisition
                    if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
                        String TimeSat = (String) msg.obj;
                        if (TimeSat.matches("\\d+(?:\\.\\d+)?")) {
                            int TimeSatVal = Integer.parseInt(TimeSat);
                            statusPage2.setTimeSatValue("" + TimeSatVal);
                        }
                    }
                    break;
            }
        }
    };
    /*
    public void setHdopVal(String value) {
            this.txtViewHdopVal.setText(value );
        }
        public void setGPSAltitudeVal(String value) {
            this.txtViewGPSAltitudeVal.setText(value );
        }
        public void setGPSSpeedVal(String value) {
            this.txtViewGPSSpeedVal.setText(value );
        }
        public void setLocationAgeValue(String value) {
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_altimeter_status_tab1);
        myBT = (ConsoleApplication) getApplication();

        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_altimeter_status);

        mViewPager =(ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        myBT.setHandler(handler);
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
                if (statusPage1.switchOutput1.isChecked()) {
                    myBT.write("k1F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if (statusPage1.switchOutput2.isChecked()) {
                    myBT.write("k2F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if (statusPage1.switchOutput3.isChecked()) {
                    myBT.write("k3F;\n".toString());
                    myBT.clearInput();
                    myBT.flush();
                }
                if (statusPage1.switchOutput4.isChecked()) {
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
    public void onResume() {
        super.onResume();

        if(myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            status = true;
            altiStatus.start();
        }
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

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        statusPage1 = new Tab1StatusFragment();
        statusPage2 = new Tab2StatusFragment();

        adapter.addFragment(statusPage1, "TAB1");
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            adapter.addFragment(statusPage2, "TAB2");
        }


        viewPager.setAdapter(adapter);
    }

    public class SectionsStatusPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList= new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
        public SectionsStatusPageAdapter (FragmentManager fm){
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }
        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }
        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1StatusFragment extends Fragment {
        private static final String TAG = "Tab1StatusFragment";
        private boolean ViewCreated = false;

        private TextView txtViewOutput1Status, txtViewOutput2Status, txtViewOutput3Status, txtViewOutput4Status;
        private TextView txtViewAltitude, txtViewVoltage, txtViewLink, txtTemperature, txtEEpromUsage,txtNbrOfFlight;
        private TextView txtViewOutput4, txtViewBatteryVoltage, txtViewOutput3, txtViewEEprom, txtViewFlight;

        private Switch switchOutput1, switchOutput2, switchOutput3, switchOutput4;

        public void setOutput1Status(String value) {
            this.txtViewOutput1Status.setText(outputStatus(value));
        }
        public void setOutput2Status(String value) {
            this.txtViewOutput2Status.setText(outputStatus(value));
        }
        public void setOutput3Status(String value) {
            this.txtViewOutput3Status.setText(outputStatus(value));
        }
        public void setOutput4Status(String value) {
            this.txtViewOutput4Status.setText(outputStatus(value));
        }
        public void setAltitude(String value) {
            this.txtViewAltitude.setText(value);
        }
        public void setVoltage(String value) {
            this.txtViewVoltage.setText(value);
        }
        public void setTemperature(String value) {
            this.txtTemperature.setText(value );
        }
        public void setEEpromUsage(String value) {
            this.txtEEpromUsage.setText(value );
        }
        public void setNbrOfFlight(String value) {
            this.txtNbrOfFlight.setText(value );
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab1,container,false);

            ViewCreated = true;
            txtViewOutput1Status = (TextView) view.findViewById(R.id.txtViewOutput1Status);
            txtViewOutput2Status = (TextView) view.findViewById(R.id.txtViewOutput2Status);
            txtViewOutput3Status = (TextView) view.findViewById(R.id.txtViewOutput3Status);
            txtViewOutput4Status = (TextView) view.findViewById(R.id.txtViewOutput4Status);
            txtViewAltitude = (TextView) view.findViewById(R.id.txtViewAltitude);
            txtViewVoltage = (TextView) view.findViewById(R.id.txtViewVoltage);
            txtViewLink = (TextView) view.findViewById(R.id.txtViewLink);
            txtViewOutput3 = (TextView) view.findViewById(R.id.txtViewOutput3);
            txtViewOutput4 = (TextView) view.findViewById(R.id.txtViewOutput4);
            txtViewBatteryVoltage = (TextView) view.findViewById(R.id.txtViewBatteryVoltage);
            txtTemperature = (TextView) view.findViewById(R.id.txtViewTemperature);
            txtEEpromUsage= (TextView) view.findViewById(R.id.txtViewEEpromUsage);
            txtNbrOfFlight= (TextView) view.findViewById(R.id.txtViewNbrOfFlight);
            txtViewEEprom = (TextView) view.findViewById(R.id.txtViewEEprom);
            txtViewFlight = (TextView) view.findViewById(R.id.txtViewFlight);


            if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32")||myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS") ) {
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

            txtViewLink.setText(myBT.getConnectionType());

            switchOutput1 = (Switch) view.findViewById(R.id.switchOutput1);
            switchOutput2 = (Switch) view.findViewById(R.id.switchOutput2);
            switchOutput3 = (Switch) view.findViewById(R.id.switchOutput3);
            switchOutput4 = (Switch) view.findViewById(R.id.switchOutput4);
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
            return view;
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
    }

    public static class Tab2StatusFragment extends Fragment {
        private static final String TAG = "Tab2StatusFragment";
        private boolean ViewCreated = false;
        private TextView txtViewLatitude, txtViewLongitude, txtViewLatitudeValue, txtViewLongitudeValue;
        private TextView txtViewSatellitesVal, txtViewHdopVal,txtViewGPSAltitudeVal, txtViewGPSSpeedVal;
        private TextView txtViewLocationAgeValue, txtViewTimeSatValue;

        public void setLatitudeValue(String value) {
            this.txtViewLatitudeValue.setText(value );
        }
        public void setLongitudeValue(String value) {
            this.txtViewLongitudeValue.setText(value );
        }

        public void setSatellitesVal(String value) {
            this.txtViewSatellitesVal.setText(value );
        }
        public void setHdopVal(String value) {
            this.txtViewHdopVal.setText(value );
        }
        public void setGPSAltitudeVal(String value) {
            this.txtViewGPSAltitudeVal.setText(value );
        }
        public void setGPSSpeedVal(String value) {
            this.txtViewGPSSpeedVal.setText(value );
        }
        public void setLocationAgeValue(String value) {
            this.txtViewLocationAgeValue.setText(value );
        }
        public void setTimeSatValue(String value) {
            this.txtViewTimeSatValue.setText(value );
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_altimeter_status_tab2,container,false);

            txtViewLatitude = (TextView) view.findViewById(R.id.txtViewLatitude);
            txtViewLongitude = (TextView) view.findViewById(R.id.txtViewLongitude);
            txtViewLatitudeValue = (TextView) view.findViewById(R.id.txtViewLatitudeValue);
            txtViewLongitudeValue = (TextView) view.findViewById(R.id.txtViewLongitudeValue);
            txtViewSatellitesVal = (TextView) view.findViewById(R.id.txtViewSatellitesVal);
            txtViewHdopVal= (TextView) view.findViewById(R.id.txtViewHdopVal);
            txtViewGPSAltitudeVal= (TextView) view.findViewById(R.id.txtViewGPSAltitudeVal);
            txtViewGPSSpeedVal= (TextView) view.findViewById(R.id.txtViewGPSSpeedVal);
            txtViewLocationAgeValue = (TextView) view.findViewById(R.id.txtViewLocationAgeValue);
            txtViewTimeSatValue= (TextView) view.findViewById(R.id.txtViewTimeSatValue);

            //hide GPS
           /* if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")){
                txtViewLatitude.setVisibility(View.VISIBLE);
                txtViewLongitude.setVisibility(View.VISIBLE);
                txtViewLatitudeValue.setVisibility(View.VISIBLE);
                txtViewLongitudeValue.setVisibility(View.VISIBLE);
            } else {
                txtViewLatitude.setVisibility(View.INVISIBLE);
                txtViewLongitude.setVisibility(View.INVISIBLE);
                txtViewLatitudeValue.setVisibility(View.INVISIBLE);
                txtViewLongitudeValue.setVisibility(View.INVISIBLE);
            }*/
            ViewCreated = true;
            return view;
        }
    }



    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
