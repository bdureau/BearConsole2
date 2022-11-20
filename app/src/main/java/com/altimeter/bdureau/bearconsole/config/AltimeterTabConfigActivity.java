package com.altimeter.bdureau.bearconsole.config;
/**
 * @description: Retrieve altimeter configuration and show it in tabs
 * The user can then load it back to the altimeter
 * @author: boris.dureau@neuf.fr
 **/

import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.Nullable;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
/*import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;*/
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flash.FlashFirmware;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//tooltip library
import com.github.florent37.viewtooltip.ViewTooltip;

/*import static android.graphics.Color.BLUE;
import static android.graphics.Color.GREEN;*/

public class AltimeterTabConfigActivity extends AppCompatActivity {
    private static final String TAG = "AltimeterTabConfigActivity";

    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    Tab1Fragment configPage1 = null;
    Tab2Fragment configPage2 = null;
    Tab3Fragment configPage3 = null;
    Tab4Fragment configPage4 = null;
    private Button btnDismiss, btnUpload;
    ConsoleApplication myBT;
    private AltiConfigData AltiCfg = null;
    private ProgressDialog progress;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*if(AppCompatDelegate.getDefaultNightMode()== AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }*/
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        readConfig();
        //Assync config does not work so do not use it for now
        //new RetrieveConfig().execute();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_altimeter_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        btnUpload = (Button) findViewById(R.id.butUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //send back the config to the altimeter and exit if successful
                sendConfig();
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        configPage1 = new Tab1Fragment(AltiCfg);
        configPage2 = new Tab2Fragment(AltiCfg);
        configPage3 = new Tab3Fragment(AltiCfg);

        adapter.addFragment(configPage1, "TAB1");
        adapter.addFragment(configPage2, "TAB2");
        adapter.addFragment(configPage3, "TAB3");

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            configPage4 = new Tab4Fragment(AltiCfg);
            adapter.addFragment(configPage4, "TAB4");
        }

        linearDots=findViewById(R.id.idAltiConfigLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr){
        dotsSlide =new TextView[nbr];
        linearDots.removeAllViews();

        for (int i=0; i< dotsSlide.length; i++){
            dotsSlide[i]=new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if(dotsSlide.length>0){
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }

    }

    ViewPager.OnPageChangeListener viewListener=new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    private boolean readConfig() {
        // ask for config
        boolean  success =false;
        if (myBT.getConnected()) {
            //msg("Retreiving altimeter config...");
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {
                success = false;
            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("b;".toString());
                myBT.flush();

                //get the results
                //wait for the result to come back
                try {
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {
                    success = false;
                }
                myMessage = myBT.ReadResult(3000);
                //reading the config
                if (myMessage.equals("start alticonfig end")) {
                    Log.d("AltiConfig", "attempt 1");
                    try {
                        AltiCfg = myBT.getAltiConfigData();
                        success =true;
                    } catch (Exception e) {
                        //  msg("pb ready data");
                        success = false;
                    }
                } else {
                    //try again
                    myBT.setDataReady(false);
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("b;".toString());
                    myBT.flush();
                    //get the results
                    //wait for the result to come back
                    try {
                        while (myBT.getInputStream().available() <= 0) ;
                    } catch (IOException e) {
                        success = false;
                    }
                    myMessage = myBT.ReadResult(3000);
                    //reading the config
                    if (myMessage.equals("start alticonfig end")) {
                        Log.d("AltiConfig", "attempt 2");
                        try {
                            AltiCfg = myBT.getAltiConfigData();
                            if(AltiCfg == null )
                                Log.d("AltiConfig", "AltiCfg is null");
                            success = true;
                        } catch (Exception e) {
                            //  msg("pb ready data");
                            success = false;
                        }
                    }
                }
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                //switch on the main loop before sending the config
                myBT.write("m1;".toString());


                //wait for the result to come back
                try {
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {
                    success = false;
                }
                myMessage = myBT.ReadResult(3000);
                myBT.flush();
            }
        }

    return success;
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private boolean sendConfig() {
        //final boolean exit_no_save = false;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        long prevBaudRate = AltiCfg.getConnectionSpeed();
        long nbrOfMain = 0;
        long nbrOfDrogue = 0;
        // check if the baud rate has changed

        if (configPage1.isViewCreated()) {
            AltiCfg.setOutput1Delay(configPage1.getOutDelay1());
            AltiCfg.setOutput2Delay(configPage1.getOutDelay2());
            AltiCfg.setOutput3Delay(configPage1.getOutDelay3());
            AltiCfg.setOutput4Delay(configPage1.getOutDelay4());
            AltiCfg.setOutput1(configPage1.getDropdownOut1());
            AltiCfg.setOutput2(configPage1.getDropdownOut2());
            AltiCfg.setOutput3(configPage1.getDropdownOut3());
            AltiCfg.setOutput4(configPage1.getDropdownOut4());
            AltiCfg.setMainAltitude(configPage1.getMainAltitude());
        }
        if (configPage2.isViewCreated()) {

            // AltiCfg.setConnectionSpeed(configPage2.getBaudRate());
            AltiCfg.setNbrOfMeasuresForApogee(configPage2.getApogeeMeasures());
            AltiCfg.setAltimeterResolution(configPage2.getAltimeterResolution());
            AltiCfg.setEepromSize(configPage2.getEEpromSize());
            AltiCfg.setBeepOnOff(configPage2.getBeepOnOff());
            // msg("ret" +configPage2.getRecordTempOnOff());
            AltiCfg.setEndRecordAltitude(configPage2.getEndRecordAltitude());
            AltiCfg.setRecordTemperature(configPage2.getRecordTempOnOff());
            AltiCfg.setSupersonicDelay(configPage2.getSupersonicDelayOnOff());
            AltiCfg.setLiftOffAltitude(configPage2.getLiftOffAltitude());
            AltiCfg.setBatteryType(configPage2.getBatteryType());
            AltiCfg.setRecordingTimeout(configPage2.getRecordingTimeout());
        }

        if (configPage3.isViewCreated()) {
            AltiCfg.setBeepingFrequency(configPage3.getFreq());
            AltiCfg.setBeepingMode(configPage3.getDropdownBipMode());
            AltiCfg.setUnits(configPage3.getDropdownUnits());
            if (AltiCfg.getAltimeterName().equals("AltiServo")) {
                AltiCfg.setServoStayOn(configPage3.getServoStayOn());
                AltiCfg.setServoSwitch(configPage3.getServoSwitch());
            }
            AltiCfg.setAltiID(configPage3.getAltiID());
            AltiCfg.setUseTelemetryPort(configPage3.getDropdownUseTelemetryPort());
        }

        if (AltiCfg.getAltimeterName().equals("AltiServo")) {
            if (configPage4.isViewCreated()) {
                AltiCfg.setServo1OnPos(configPage4.getServo1OnPos());
                AltiCfg.setServo2OnPos(configPage4.getServo2OnPos());
                AltiCfg.setServo3OnPos(configPage4.getServo3OnPos());
                AltiCfg.setServo4OnPos(configPage4.getServo4OnPos());
                AltiCfg.setServo1OffPos(configPage4.getServo1OffPos());
                AltiCfg.setServo2OffPos(configPage4.getServo2OffPos());
                AltiCfg.setServo3OffPos(configPage4.getServo3OffPos());
                AltiCfg.setServo4OffPos(configPage4.getServo4OffPos());
            }
        }
        if (AltiCfg.getOutput1() == 0)
            nbrOfMain++;
        if (AltiCfg.getOutput2() == 0)
            nbrOfMain++;

        if (AltiCfg.getAltimeterName().equals("AltiMultiSTM32") ||
                AltiCfg.getAltimeterName().equals("AltiMultiESP32") ||
                AltiCfg.getAltimeterName().equals("AltiServo") ||
                AltiCfg.getAltimeterName().equals("AltiMultiV2") ||
                AltiCfg.getAltimeterName().equals("AltiGPS") ||
                AltiCfg.getAltimeterName().equals("AltiMulti")) {
            if (AltiCfg.getOutput3() == 0)
                nbrOfMain++;
        }
        if (AltiCfg.getAltimeterName().equals("AltiMultiSTM32") ||
                AltiCfg.getAltimeterName().equals("AltiGPS") ||
                AltiCfg.getAltimeterName().equals("AltiServo")) {
            if (AltiCfg.getOutput4() == 0)
                nbrOfMain++;
        }

        if (AltiCfg.getOutput1() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getOutput2() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getAltimeterName().equals("AltiMultiSTM32") ||
                AltiCfg.getAltimeterName().equals("AltiMultiESP32") ||
                AltiCfg.getAltimeterName().equals("AltiServo") ||
                AltiCfg.getAltimeterName().equals("AltiMultiV2") ||
                AltiCfg.getAltimeterName().equals("AltiGPS") ||
                AltiCfg.getAltimeterName().equals("AltiMulti")) {
            if (AltiCfg.getOutput3() == 1)
                nbrOfDrogue++;
        }
        if (AltiCfg.getAltimeterName().equals("AltiMultiSTM32") ||
                AltiCfg.getAltimeterName().equals("AltiGPS") ||
                AltiCfg.getAltimeterName().equals("AltiServo")) {
            if (AltiCfg.getOutput4() == 1)
                nbrOfDrogue++;
        }
        if (myBT.getAppConf().getAllowMultipleDrogueMain().equals("false")) {
            if (nbrOfMain > 1) {
                //Only one main is allowed Please review your config
                msg(getResources().getString(R.string.msg1));
                return false;
            }
            if (nbrOfDrogue > 1) {
                //Only one Drogue is allowed Please review your config
                msg(getResources().getString(R.string.msg2));
                return false;
            }
        }

        if (configPage2.isViewCreated()) {
            if (AltiCfg.getConnectionSpeed() != configPage2.getBaudRate()) {
                //final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //You are about to change the baud rate, are you sure you want to do it?
                builder.setMessage(getResources().getString(R.string.msg9))
                        .setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                AltiCfg.setConnectionSpeed(configPage2.getBaudRate());
                                sendAltiCfgV2();
                                finish();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();

                            }
                        });
                final AlertDialog alert = builder.create();
                alert.show();
            } else {
                sendAltiCfgV2();
                finish();
            }
        } else {
            sendAltiCfgV2();
            finish();
        }

        return true;
    }

    /*private void sendAltiCfg() {
        String altiCfgStr = "";

        altiCfgStr = "s," +
                AltiCfg.getUnits() + "," +
                AltiCfg.getBeepingMode() + "," +
                AltiCfg.getOutput1() + "," +
                AltiCfg.getOutput2() + "," +
                AltiCfg.getOutput3() + "," +
                AltiCfg.getMainAltitude() + "," +
                AltiCfg.getSupersonicYesNo() + "," +
                AltiCfg.getOutput1Delay() + "," +
                AltiCfg.getOutput2Delay() + "," +
                AltiCfg.getOutput3Delay() + "," +
                AltiCfg.getBeepingFrequency() + "," +
                AltiCfg.getNbrOfMeasuresForApogee() + "," +
                AltiCfg.getEndRecordAltitude() + "," +
                AltiCfg.getRecordTemperature() + "," +
                AltiCfg.getSupersonicDelay() + "," +
                AltiCfg.getConnectionSpeed() + "," +
                AltiCfg.getAltimeterResolution() + "," +
                AltiCfg.getEepromSize() + "," +
                AltiCfg.getBeepOnOff();
        altiCfgStr = altiCfgStr + "," + AltiCfg.getOutput4();
        altiCfgStr = altiCfgStr + "," + AltiCfg.getOutput4Delay();
        altiCfgStr = altiCfgStr + "," + AltiCfg.getLiftOffAltitude();
        altiCfgStr = altiCfgStr + "," + AltiCfg.getBatteryType();
        altiCfgStr = altiCfgStr + "," + AltiCfg.getRecordingTimeout();
        altiCfgStr = altiCfgStr + ","+ AltiCfg.getAltiID(); //for ESP32
        altiCfgStr = altiCfgStr + ","+ AltiCfg.getTelemetryPort(); //for ESP32 and STM32

        if (AltiCfg.getAltimeterName().equals("AltiServo")) {
            altiCfgStr = altiCfgStr + ",0";//Servo switch
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo1OnPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo2OnPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo3OnPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo4OnPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo1OffPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo2OffPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo3OffPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServo4OffPos();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getServoStayOn();
        }

        String cfg = altiCfgStr;
        cfg = cfg.replace("s", "");
        cfg = cfg.replace(",", "");
        Log.d("conftab", cfg.toString());

        altiCfgStr = altiCfgStr + "," + generateCheckSum(cfg) + ";";


        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
            myBT.flush();
            myBT.clearInput();
            myBT.setDataReady(false);
            msg("Sent :" + altiCfgStr.toString());
            //send back the config
            myBT.write(altiCfgStr.toString());
            Log.d("conftab", altiCfgStr.toString());
            myBT.flush();
            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                //msg("Sent OK:" + altiCfgStr.toString());
                Log.d("conftab", "config sent succesfully");

            } else {
                //  msg(myMessage);
                Log.d("conftab", "config not sent succesfully");
                Log.d("conftab", myMessage);
            }
            if (myMessage.equals("KO")) {
                msg(getResources().getString(R.string.msg2));
            }


            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            msg(getResources().getString(R.string.msg3));

            myBT.flush();
        }

    }*/

    private void sendAltiCfgV2() {

        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
        }
        String altiCfgStr = "";

        //altiCfgStr = "s," +
        //        AltiCfg.getUnits() + "," +
        SendParam("p,1,"+ AltiCfg.getUnits());
        //        AltiCfg.getBeepingMode() + "," +
        SendParam("p,2,"+AltiCfg.getBeepingMode());
        //        AltiCfg.getOutput1() + "," +
        SendParam("p,3,"+AltiCfg.getOutput1());
        //       AltiCfg.getOutput2() + "," +
        SendParam("p,4,"+ AltiCfg.getOutput2());
        //        AltiCfg.getOutput3() + "," +
        SendParam("p,5,"+AltiCfg.getOutput3());
        //        AltiCfg.getMainAltitude() + "," +
        SendParam("p,6,"+AltiCfg.getMainAltitude());
        //        AltiCfg.getSupersonicYesNo() + "," +
        SendParam("p,7,"+AltiCfg.getSupersonicYesNo());
        //        AltiCfg.getOutput1Delay() + "," +
        SendParam("p,8,"+AltiCfg.getOutput1Delay());
        //        AltiCfg.getOutput2Delay() + "," +
        SendParam("p,9,"+AltiCfg.getOutput2Delay());
        //        AltiCfg.getOutput3Delay() + "," +
        SendParam("p,10,"+AltiCfg.getOutput3Delay());
        //        AltiCfg.getBeepingFrequency() + "," +
        SendParam("p,11,"+AltiCfg.getBeepingFrequency());
        //        AltiCfg.getNbrOfMeasuresForApogee() + "," +
        SendParam("p,12,"+AltiCfg.getNbrOfMeasuresForApogee());
        //        AltiCfg.getEndRecordAltitude() + "," +
        SendParam("p,13,"+AltiCfg.getEndRecordAltitude());
        //        AltiCfg.getRecordTemperature() + "," +
        SendParam("p,14,"+AltiCfg.getRecordTemperature());
        //        AltiCfg.getSupersonicDelay() + "," +
        SendParam("p,15,"+AltiCfg.getSupersonicDelay());
        //        AltiCfg.getConnectionSpeed() + "," +
        SendParam("p,16,"+AltiCfg.getConnectionSpeed());
        //        AltiCfg.getAltimeterResolution() + "," +
        SendParam("p,17,"+AltiCfg.getAltimeterResolution());
        //        AltiCfg.getEepromSize() + "," +
        SendParam("p,18,"+AltiCfg.getEepromSize());
        //        AltiCfg.getBeepOnOff();
        SendParam("p,19,"+AltiCfg.getBeepOnOff());
        //altiCfgStr = altiCfgStr + "," + AltiCfg.getOutput4();
        SendParam("p,20,"+AltiCfg.getOutput4());
        //altiCfgStr = altiCfgStr + "," + AltiCfg.getOutput4Delay();
        SendParam("p,21,"+ AltiCfg.getOutput4Delay());
        //altiCfgStr = altiCfgStr + "," + AltiCfg.getLiftOffAltitude();
        SendParam("p,22,"+AltiCfg.getLiftOffAltitude());
        //altiCfgStr = altiCfgStr + "," + AltiCfg.getBatteryType();
        SendParam("p,23,"+AltiCfg.getBatteryType());
        //altiCfgStr = altiCfgStr + "," + AltiCfg.getRecordingTimeout();
        SendParam("p,24,"+AltiCfg.getRecordingTimeout());
        //    altiCfgStr = altiCfgStr + ",0";
        SendParam("p,25,"+AltiCfg.getAltiID());
//    altiCfgStr = altiCfgStr + ",0";//reserved 2
        Log.d("UseTelemetryPort", "before getUseTelemetryPort" );
        SendParam("p,26,"+AltiCfg.getUseTelemetryPort());
        Log.d("UseTelemetryPort", "after getUseTelemetryPort" );

        if (AltiCfg.getAltimeterName().equals("AltiServo")) {
        //    altiCfgStr = altiCfgStr + ",0";
            SendParam("p,27,"+AltiCfg.getServoSwitch());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo1OnPos();
            SendParam("p,28,"+AltiCfg.getServo1OnPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo2OnPos();
            SendParam("p,29,"+AltiCfg.getServo2OnPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo3OnPos();
            SendParam("p,30,"+AltiCfg.getServo3OnPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo4OnPos();
            SendParam("p,31,"+AltiCfg.getServo4OnPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo1OffPos();
            SendParam("p,32,"+AltiCfg.getServo1OffPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo2OffPos();
            SendParam("p,33,"+AltiCfg.getServo2OffPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo3OffPos();
            SendParam("p,34,"+AltiCfg.getServo3OffPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServo4OffPos();
            SendParam("p,35,"+AltiCfg.getServo4OffPos());
        //    altiCfgStr = altiCfgStr + "," + AltiCfg.getServoStayOn();
            SendParam("p,36,"+AltiCfg.getServoStayOn());
        }

        if (myBT.getConnected()) {

            String myMessage = "";

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //Write the config structure
            myBT.write("q;".toString());
            Log.d("conftab", "write config");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.flush();
        }
    }

    private void SendParam (String altiCfgStr) {
        String cfg = altiCfgStr;
        cfg = cfg.replace("p", "");
        cfg = cfg.replace(",", "");
        Log.d("conftab", cfg.toString());

        altiCfgStr = altiCfgStr + "," + generateCheckSum(cfg) + ";";


        if (myBT.getConnected()) {

            String myMessage = "";

            myBT.flush();
            myBT.clearInput();
            myBT.setDataReady(false);

            //send back the config
            myBT.write(altiCfgStr.toString());
            Log.d("conftab", altiCfgStr.toString());
            myBT.flush();
            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                //msg("Sent OK:" + altiCfgStr.toString());
                Log.d("conftab", "config sent succesfully");

            } else {
                //  msg(myMessage);
                Log.d("conftab", "config not sent succesfully");
                Log.d("conftab", myMessage);
            }
            if (myMessage.equals("KO")) {
             //   msg(getResources().getString(R.string.msg2));
            }
        }
    }

    public static Integer generateCheckSum(String value) {

        byte[] data = value.getBytes();
        long checksum = 0L;

        for (byte b : data) {
            checksum += b;
        }

        checksum = checksum % 256;

        return new Long(checksum).intValue();

    }

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
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

    public static class Tab1Fragment extends Fragment {
        private static final String TAG = "Tab1Fragment";
        private AltiConfigData lAltiCfg;

        private Spinner dropdownOut1, dropdownOut2, dropdownOut3, dropdownOut4;
        private EditText OutDelay1, OutDelay2, OutDelay3, OutDelay4;
        private EditText MainAltitude;
        private TextView txtOut1, txtOut2, txtOut3, txtViewDelay3, txtOut4, txtViewDelay4, txtViewDelay2, txtViewDelay1;

        private boolean ViewCreated = false;

        public Tab1Fragment(AltiConfigData cfg) {
            lAltiCfg=cfg;
        }
        public boolean isViewCreated() {
            return ViewCreated;
        }

        public int getDropdownOut1() {
            return (int) this.dropdownOut1.getSelectedItemId();
        }

        public void setDropdownOut1(int Out1) {
            this.dropdownOut1.setSelection(Out1);
        }

        public int getDropdownOut2() {
            return (int) this.dropdownOut2.getSelectedItemId();
        }

        public void setDropdownOut2(int Out2) {
            this.dropdownOut2.setSelection(Out2);
        }

        public int getDropdownOut3() {
            return (int) this.dropdownOut3.getSelectedItemId();
        }

        public void setDropdownOut3(int Out3) {
            this.dropdownOut3.setSelection(Out3);
        }

        public int getDropdownOut4() {
            if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo"))
                return (int) this.dropdownOut4.getSelectedItemId();
            else
                return -1;
        }

        public void setDropdownOut4(int Out4) {
            if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                this.dropdownOut4.setSelection(Out4);
                this.dropdownOut4.setVisibility(View.VISIBLE);
                txtOut4.setVisibility(View.VISIBLE);
            } else {
                this.dropdownOut4.setVisibility(View.INVISIBLE);
                txtOut4.setVisibility(View.INVISIBLE);
            }

        }

        public void setOutDelay1(int OutDelay1) {
            this.OutDelay1.setText(String.valueOf(OutDelay1));
        }

        public int getOutDelay1() {
            int ret;
            try {
                ret = Integer.parseInt(OutDelay1.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setOutDelay2(int OutDelay2) {
            this.OutDelay2.setText(String.valueOf(OutDelay2));
        }

        public int getOutDelay2() {
            int ret;
            try {
                ret = Integer.parseInt(OutDelay2.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setOutDelay3(int OutDelay3) {
            this.OutDelay3.setText(String.valueOf(OutDelay3));
        }

        public int getOutDelay3() {
            int ret;
            try {
                ret = Integer.parseInt(OutDelay3.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setOutDelay4(int OutDelay4) {
            if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                this.OutDelay4.setText(String.valueOf(OutDelay4));
                this.OutDelay4.setVisibility(View.VISIBLE);
                txtViewDelay4.setVisibility(View.VISIBLE);
            } else {
                this.OutDelay4.setVisibility(View.INVISIBLE);
                txtViewDelay4.setVisibility(View.INVISIBLE);
            }

        }

        public int getOutDelay4() {
            if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                int ret;
                try {
                    ret = Integer.parseInt(OutDelay4.getText().toString());
                } catch (Exception e) {
                    ret = 0;
                }
                return ret;
            } else
                return -1;
        }

        public void setMainAltitude(int MainAltitude) {
            this.MainAltitude.setText(String.valueOf(MainAltitude));
        }

        public int getMainAltitude() {
            int ret;
            try {
                ret = Integer.parseInt(MainAltitude.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart1_fragment, container, false);

            //Output 1
            dropdownOut1 = (Spinner) view.findViewById(R.id.spinnerOut1);
            //String[] items3 = new String[]{"Main", "Drogue", "Timer", "Disabled", "Landing", "Liftoff","Altitude"};
            String[] items3 = new String[]{getResources().getString(R.string.main_config),
                    getResources().getString(R.string.drogue_config),
                    getResources().getString(R.string.timer_config),
                    getResources().getString(R.string.disabled_config),
                    getResources().getString(R.string.landing_config),
                    getResources().getString(R.string.liftoff_config),
                    getResources().getString(R.string.altitude_config)
            };
            ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);

            dropdownOut1.setAdapter(adapter3);
            dropdownOut1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    //Altitude
                    if(selectedItem.equals(getResources().getString(R.string.altitude_config)))
                    {
                        // Altitude1
                        txtViewDelay1.setText(getResources().getString(R.string.altitude1));
                    } else {
                        //Delay1
                        txtViewDelay1.setText(getResources().getString(R.string.delay1));
                    }
                } // to close the onItemSelected
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });

            //Output 2
            dropdownOut2 = (Spinner) view.findViewById(R.id.spinnerOut2);

            ArrayAdapter<String> adapter4 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut2.setAdapter(adapter4);
            dropdownOut2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    //Altitude
                    if(selectedItem.equals(getResources().getString(R.string.altitude_config)))
                    {
                        // Altitude2
                        txtViewDelay2.setText(getResources().getString(R.string.altitude2));
                    } else {
                        //Delay2
                        txtViewDelay2.setText(getResources().getString(R.string.delay2));
                    }
                } // to close the onItemSelected
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });
            //Output 3
            dropdownOut3 = (Spinner) view.findViewById(R.id.spinnerOut3);

            ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut3.setAdapter(adapter5);
            dropdownOut3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    //Altitude
                    if(selectedItem.equals(getResources().getString(R.string.altitude_config)))
                    {
                        // Altitude3
                        txtViewDelay3.setText(getResources().getString(R.string.altitude3));
                    } else {
                        //Delay2
                        txtViewDelay3.setText(getResources().getString(R.string.delay3));
                    }
                } // to close the onItemSelected
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });
            //Output 4
            dropdownOut4 = (Spinner) view.findViewById(R.id.spinnerOut4);

            ArrayAdapter<String> adapter6 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut4.setAdapter(adapter6);
            dropdownOut4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    //Altitude
                    if(selectedItem.equals(getResources().getString(R.string.altitude_config)))
                    {
                        // Altitude4
                        txtViewDelay4.setText(getResources().getString(R.string.altitude4));
                    } else {
                        //Delay2
                        txtViewDelay4.setText(getResources().getString(R.string.delay4));
                    }
                } // to close the onItemSelected
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });
            OutDelay1 = (EditText) view.findViewById(R.id.editTxtDelay1);
            OutDelay2 = (EditText) view.findViewById(R.id.editTxtDelay2);
            OutDelay3 = (EditText) view.findViewById(R.id.editTxtDelay3);
            OutDelay4 = (EditText) view.findViewById(R.id.editTxtDelay4);

            MainAltitude = (EditText) view.findViewById(R.id.editTxtMainAltitude);
            // Tool tip
            MainAltitude.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Enter the altitude for the main chute
                            .text(getResources().getString(R.string.main_altitude_tooltip))
                            .show();
                }
            });
            // Tool tip
            OutDelay1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Enter the firing delay in ms for the 1st output
                            .text(getResources().getString(R.string.OutDelay1_tooltip))
                            .show();
                }
            });

            // Tool tip
            OutDelay2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Enter the firing delay in ms for the 2nd output
                            .text(getResources().getString(R.string.OutDelay2_tooltip))
                            .show();
                }
            });

            txtOut1 = (TextView) view.findViewById(R.id.txtOut1);
            txtOut2 = (TextView) view.findViewById(R.id.txtOut2);
            txtOut3 = (TextView) view.findViewById(R.id.txtOut3);
            txtViewDelay1 = (TextView) view.findViewById(R.id.txtViewDelay1);
            txtViewDelay2 = (TextView) view.findViewById(R.id.txtViewDelay2);
            txtViewDelay3 = (TextView) view.findViewById(R.id.txtViewDelay3);
            txtOut4 = (TextView) view.findViewById(R.id.txtOut4);
            txtViewDelay4 = (TextView) view.findViewById(R.id.txtViewDelay4);

            // Tool tip
            txtOut1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose what you want to do with the 1st output
                            .text(getResources().getString(R.string.txtOut1_tooltip))
                            .show();
                }
            });
            // Tool tip
            txtOut2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose what you want to do with the 2nd output
                            .text(getResources().getString(R.string.txtOut2_tooltip))
                            .show();
                }
            });

            Log.d("AltiConfig", "Before Attempting to set config");
            if (lAltiCfg != null) {
                Log.d("AltiConfig", "Attempting to set config");
                dropdownOut1.setSelection(lAltiCfg.getOutput1());
                dropdownOut2.setSelection(lAltiCfg.getOutput2());
                if (!lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                    dropdownOut3.setSelection(lAltiCfg.getOutput3());
                    dropdownOut3.setVisibility(View.VISIBLE);
                    txtOut3.setVisibility(View.VISIBLE);

                    // Tool tip
                    txtOut3.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ViewTooltip
                                    .on(v)
                                    .color(Color.BLACK)
                                    .position(ViewTooltip.Position.TOP)
                                    //Choose what you want to do with the 3rd output
                                    .text(getResources().getString(R.string.txtOut3_tooltip))
                                    .show();
                        }
                    });
                } else {
                    dropdownOut3.setVisibility(View.INVISIBLE);
                    txtOut3.setVisibility(View.INVISIBLE);
                }
                if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                    dropdownOut4.setSelection(lAltiCfg.getOutput4());
                    dropdownOut4.setVisibility(View.VISIBLE);
                    txtOut4.setVisibility(View.VISIBLE);
                    // Tool tip
                    txtOut4.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ViewTooltip
                                    .on(v)
                                    .color(Color.BLACK)
                                    .position(ViewTooltip.Position.TOP)
                                    //Choose what you want to do with the 4th output
                                    .text(getResources().getString(R.string.txtOut4_tooltip))
                                    .show();
                        }
                    });
                } else {
                    dropdownOut4.setVisibility(View.INVISIBLE);
                    txtOut4.setVisibility(View.INVISIBLE);
                }
                OutDelay1.setText(String.valueOf(lAltiCfg.getOutput1Delay()));
                OutDelay2.setText(String.valueOf(lAltiCfg.getOutput2Delay()));
                if (!lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                    OutDelay3.setText(String.valueOf(lAltiCfg.getOutput3Delay()));
                    // Tool tip
                    OutDelay3.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ViewTooltip
                                    .on(v)
                                    .color(Color.BLACK)
                                    .position(ViewTooltip.Position.TOP)
                                    //Enter the firing delay in ms for the 3rd output
                                    .text(getResources().getString(R.string.OutDelay3_tooltip))
                                    .show();
                        }
                    });
                } else {
                    OutDelay3.setVisibility(View.INVISIBLE);
                    txtViewDelay3.setVisibility(View.INVISIBLE);
                }
                if (lAltiCfg.getAltimeterName().equals("AltiMultiSTM32") || lAltiCfg.getAltimeterName().equals("AltiGPS") || lAltiCfg.getAltimeterName().equals("AltiServo")) {
                    OutDelay4.setText(String.valueOf(lAltiCfg.getOutput4Delay()));
                    OutDelay4.setVisibility(View.VISIBLE);
                    txtViewDelay4.setVisibility(View.VISIBLE);
                    //Tooltip
                    OutDelay4.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ViewTooltip
                                    .on(v)
                                    .color(Color.BLACK)
                                    .position(ViewTooltip.Position.TOP)
                                    //Enter the firing delay in ms for the 4th output
                                    .text(getResources().getString(R.string.OutDelay4_tooltip))
                                    .show();
                        }
                    });
                } else {
                    OutDelay4.setVisibility(View.INVISIBLE);
                    txtViewDelay4.setVisibility(View.INVISIBLE);
                }

                MainAltitude.setText(String.valueOf(lAltiCfg.getMainAltitude()));
            }
            ViewCreated = true;
            return view;
        }
        /*private Paint createPaint() {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new LinearGradient(0, 0, 0, 600, BLUE, GREEN, Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.FILL);
            return paint;
        }*/
    }


    public static class Tab2Fragment extends Fragment {
        private static final String TAG = "Tab2Fragment";
        private AltiConfigData lAltiCfg=null;
        private String[] itemsBaudRate;
        private String[] itemsAltimeterResolution;
        private String[] itemsEEpromSize;
        private String[] itemsBeepOnOff;
        private String[] itemsRecordTempOnOff;
        private String[] itemsSupersonicDelayOnOff;
        private String[] itemsBatteryType;

        private Spinner dropdownBeepOnOff;
        private Spinner dropdownBaudRate;
        private EditText ApogeeMeasures;
        private Spinner dropdownAltimeterResolution, dropdownEEpromSize;
        private Spinner dropdownRecordTemp;
        private Spinner dropdownSupersonicDelay;
        private Spinner dropdownBatteryType;
        private EditText EndRecordAltitude;
        private EditText LiftOffAltitude, RecordingTimeout;
        private boolean ViewCreated = false;
        private TextView txtViewRecordTemp, txtViewEEpromSize;

        public Tab2Fragment (AltiConfigData cfg) {
            lAltiCfg=cfg;
        }
        public boolean isViewCreated() {
            return ViewCreated;
        }

        public int getApogeeMeasures() {
            int ret;
            try {
                ret = Integer.parseInt(this.ApogeeMeasures.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setApogeeMeasures(int apogeeMeasures) {
            ApogeeMeasures.setText(String.valueOf(apogeeMeasures));
        }

        public int getAltimeterResolution() {
            return (int) this.dropdownAltimeterResolution.getSelectedItemId();
        }

        public void setAltimeterResolution(int AltimeterResolution) {
            this.dropdownAltimeterResolution.setSelection(AltimeterResolution);
        }

        public int getEEpromSize() {
            int ret;
            try {
                ret = Integer.parseInt(itemsEEpromSize[(int) dropdownEEpromSize.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setEEpromSize(int EEpromSize) {
            this.dropdownEEpromSize.setSelection(lAltiCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
        }

        public long getBaudRate() {
            int ret;
            try {
                ret = Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setBaudRate(long BaudRate) {
            this.dropdownBaudRate.setSelection(lAltiCfg.arrayIndex(itemsBaudRate, String.valueOf(BaudRate)));
        }

        public int getBeepOnOff() {
            return (int) this.dropdownBeepOnOff.getSelectedItemId();
        }

        public void setBeepOnOff(int BeepOnOff) {
            dropdownBeepOnOff.setSelection(BeepOnOff);
        }

        public int getRecordTempOnOff() {
            return (int) this.dropdownRecordTemp.getSelectedItemId();
        }

        public void setRecordTempOnOff(int RecordTempOnOff) {
            this.dropdownRecordTemp.setSelection(RecordTempOnOff);
        }

        public int getSupersonicDelayOnOff() {
            return (int) this.dropdownSupersonicDelay.getSelectedItemId();
        }

        public void setSupersonicDelayOnOff(int SupersonicDelayOnOff) {
            this.dropdownSupersonicDelay.setSelection(SupersonicDelayOnOff);
        }

        public int getEndRecordAltitude() {
            int ret;
            try {
                ret = Integer.parseInt(this.EndRecordAltitude.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setEndRecordAltitude(int EndRecordAltitude) {
            this.EndRecordAltitude.setText(String.valueOf(EndRecordAltitude));
        }

        public int getLiftOffAltitude() {
            int ret;
            try {
                ret = Integer.parseInt(this.LiftOffAltitude.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setLiftOffAltitude(int LiftOffAltitude) {
            this.LiftOffAltitude.setText(String.valueOf(LiftOffAltitude));
        }

        public int getBatteryType() {
            return (int) this.dropdownBatteryType.getSelectedItemId();
        }

        public void setBatteryType(int BatteryType) {
            dropdownBatteryType.setSelection(BatteryType);
        }

        public int getRecordingTimeout() {
            int ret;
            try {
                ret = Integer.parseInt(this.RecordingTimeout.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setRecordingTimeout(int RecordingTimeout) {
            this.RecordingTimeout.setText(String.valueOf(RecordingTimeout));
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart2_fragment, container, false);

            //baud rate
            dropdownBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);
            itemsBaudRate = new String[]{"300",
                    "1200",
                    "2400",
                    "4800",
                    "9600",
                    "14400",
                    "19200",
                    "28800",
                    "38400",
                    "57600",
                    "115200",
                    "230400"};
            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
            dropdownBaudRate.setAdapter(adapterBaudRate);
            // Tool tip
            view.findViewById(R.id.txtViewBaudRate).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the altimeter baud rate. Be carefull you might not be able to communicate
                            .text(getResources().getString(R.string.txtViewBaudRate_tooltip))
                            .show();
                }
            });
            // nbr of measures to determine apogee
            ApogeeMeasures = (EditText) view.findViewById(R.id.editTxtApogeeMeasures);
            // Tool tip
            ApogeeMeasures.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Number of measures to determine the apogee
                            .text(getResources().getString(R.string.editTxtApogeeMeasures_tooltip))
                            .show();
                }
            });
            // altimeter resolution
            dropdownAltimeterResolution = (Spinner) view.findViewById(R.id.spinnerAltimeterResolution);
            itemsAltimeterResolution = new String[]{"ULTRALOWPOWER", "STANDARD", "HIGHRES", "ULTRAHIGHRES"};
            ArrayAdapter<String> adapterAltimeterResolution = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsAltimeterResolution);
            dropdownAltimeterResolution.setAdapter(adapterAltimeterResolution);

            // Tool tip
            view.findViewById(R.id.txtViewAltimeterResolution).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the altimeter resolution. The faster you rocket goes the lower it has to be
                            .text(getResources().getString(R.string.txtViewAltimeterResolution_tooltip))
                            .show();
                }
            });
            //Altimeter external eeprom size
            txtViewEEpromSize = (TextView) view.findViewById(R.id.txtViewEEpromSize);
            dropdownEEpromSize = (Spinner) view.findViewById(R.id.spinnerEEpromSize);
            itemsEEpromSize = new String[]{"32", "64", "128", "256", "512", "1024"};
            ArrayAdapter<String> adapterEEpromSize = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
            dropdownEEpromSize.setAdapter(adapterEEpromSize);

            if (lAltiCfg != null) {
                if (lAltiCfg.getAltimeterName().equals("AltiServo") ||
                        lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                    dropdownEEpromSize.setVisibility(View.INVISIBLE);
                    txtViewEEpromSize.setVisibility(View.INVISIBLE);
                } else {
                    dropdownEEpromSize.setVisibility(View.VISIBLE);
                    txtViewEEpromSize.setVisibility(View.VISIBLE);
                }
            }
            // Tool tip
            view.findViewById(R.id.txtViewEEpromSize).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the altimeter eeprom size depending on which eeprom is used
                            .text(getResources().getString(R.string.txtViewEEpromSize_tooltip))
                            .show();
                }
            });
            // Switch beeping on or off
            // 0 is On and 1 is Off
            dropdownBeepOnOff = (Spinner) view.findViewById(R.id.spinnerBeepOnOff);
            //On
            //Off
            itemsBeepOnOff = new String[]{getResources().getString(R.string.config_on),
                    getResources().getString(R.string.config_off)};
            ArrayAdapter<String> adapterBeepOnOff = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBeepOnOff);
            dropdownBeepOnOff.setAdapter(adapterBeepOnOff);

            // Record temperature on or off
            txtViewRecordTemp = (TextView) view.findViewById(R.id.txtViewRecordTemp);
            dropdownRecordTemp = (Spinner) view.findViewById(R.id.spinnerRecordTemp);
            //Off
            //on
            itemsRecordTempOnOff = new String[]{getResources().getString(R.string.config_off),
                    getResources().getString(R.string.config_on)};
            ArrayAdapter<String> adapterRecordTemp = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsRecordTempOnOff);
            dropdownRecordTemp.setAdapter(adapterRecordTemp);

            if (lAltiCfg != null) {
                if (lAltiCfg.getAltimeterName().equals("AltiServo") ||
                        lAltiCfg.getAltimeterName().equals("AltiDuo")) {
                    dropdownRecordTemp.setVisibility(View.INVISIBLE);
                    txtViewRecordTemp.setVisibility(View.INVISIBLE);
                } else {
                    dropdownRecordTemp.setVisibility(View.VISIBLE);
                    txtViewRecordTemp.setVisibility(View.VISIBLE);
                }
            }

            // Tool tip
            txtViewRecordTemp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose if you want to record the temperature
                            .text(getResources().getString(R.string.txtViewRecordTemp_tooltip))
                            .show();
                }
            });
            // supersonic delay on/off
            dropdownSupersonicDelay = (Spinner) view.findViewById(R.id.spinnerSupersonicDelay);
            //Off
            //On
            itemsSupersonicDelayOnOff = new String[]{getResources().getString(R.string.config_off), getResources().getString(R.string.config_on)};
            ArrayAdapter<String> adapterSupersonicDelayOnOff = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsSupersonicDelayOnOff);
            dropdownSupersonicDelay.setAdapter(adapterSupersonicDelayOnOff);

            // nbr of meters to stop recording altitude
            EndRecordAltitude = (EditText) view.findViewById(R.id.editTxtEndRecordAltitude);
            // Tool tip
            EndRecordAltitude.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //At which altitude do you consider that we have landed
                            .text(getResources().getString(R.string.EndRecordAltitude_tooltip))
                            .show();
                }
            });
            // nbr of meters to consider that we have a lift off
            LiftOffAltitude = (EditText) view.findViewById(R.id.editTxtLiftOffAltitude);

            // Tool tip
            LiftOffAltitude.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //At which altitude do you consider that we have lift off
                            .text(getResources().getString(R.string.LiftOffAltitude_tooltip))
                            .show();
                }
            });
            dropdownBatteryType = (Spinner) view.findViewById(R.id.spinnerBatteryType);
            //"Unknown",
            itemsBatteryType = new String[]{getResources().getString(R.string.config_unknown),
                    "2S (7.4 Volts)", "9 Volts", "3S (11.1 Volts)"};
            ArrayAdapter<String> adapterBatteryType = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBatteryType);
            dropdownBatteryType.setAdapter(adapterBatteryType);

            // Tool tip
            view.findViewById(R.id.txtViewBatteryType).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Enter battery type used to make sure that we do not discharge it too much
                            .text(getResources().getString(R.string.txtViewBatteryType_tooltip))
                            .show();
                }
            });

            //Max recording time in seconds
            RecordingTimeout =(EditText) view.findViewById(R.id.editTxtRecordingTimeOut);
            if (lAltiCfg != null) {
                dropdownBaudRate.setSelection(lAltiCfg.arrayIndex(itemsBaudRate, String.valueOf(lAltiCfg.getConnectionSpeed())));
                ApogeeMeasures.setText(String.valueOf(lAltiCfg.getNbrOfMeasuresForApogee()));
                dropdownAltimeterResolution.setSelection(lAltiCfg.getAltimeterResolution());
                dropdownEEpromSize.setSelection(lAltiCfg.arrayIndex(itemsEEpromSize, String.valueOf(lAltiCfg.getEepromSize())));
                dropdownBeepOnOff.setSelection(lAltiCfg.getBeepOnOff());
                dropdownRecordTemp.setSelection(lAltiCfg.getRecordTemperature());
                dropdownSupersonicDelay.setSelection(lAltiCfg.getSupersonicDelay());
                EndRecordAltitude.setText(String.valueOf(lAltiCfg.getEndRecordAltitude()));
                LiftOffAltitude.setText(String.valueOf(lAltiCfg.getLiftOffAltitude()));
                dropdownBatteryType.setSelection(lAltiCfg.getBatteryType());
                RecordingTimeout.setText(String.valueOf(lAltiCfg.getRecordingTimeout()));
            }
            ViewCreated = true;
            return view;
        }
    }

    public static class Tab3Fragment extends Fragment {
        private static final String TAG = "Tab3Fragment";
        private TextView altiName;
        private Spinner dropdownUnits;
        private Spinner dropdownBipMode;
        private EditText Freq;
        private EditText altiID;
        private Spinner dropdownUseTelemetryPort;
        private Spinner dropdownServoStayOn;
        private TextView txtServoStayOn;
        private Spinner dropdownServoSwitch;
        private TextView txtServoSwitch;
        private AltiConfigData lAltiCfg=null;

        public Tab3Fragment(AltiConfigData cfg) {
            lAltiCfg = cfg;
        }
        private boolean ViewCreated = false;

        public boolean isViewCreated() {
            return ViewCreated;
        }

        public int getFreq() {
            int ret;
            try {
                ret = Integer.parseInt(this.Freq.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;

        }

        public void setFreq(int freq) {
            Freq.setText(freq);
        }

        public void setAltiName(String altiName) {
            this.altiName.setText(altiName);
        }

        public String getAltiName() {
            return (String) this.altiName.getText();
        }

        public void setAltiID(int ID) {
            this.altiID.setText(ID);
        }

        public int getAltiID() {

            int ret;
            try {
                ret = Integer.parseInt(this.altiID.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getDropdownUseTelemetryPort() {
            return (int) this.dropdownUseTelemetryPort.getSelectedItemId();
        }

        public void setDropdownUseTelemetryPort(int UseTelemetryPort) {
            this.dropdownUseTelemetryPort.setSelection(UseTelemetryPort);
        }

        public int getDropdownUnits() {
            return (int) this.dropdownUnits.getSelectedItemId();
        }

        public void setDropdownUnits(int Units) {
            this.dropdownUnits.setSelection(Units);
        }

        public int getDropdownBipMode() {
            return (int) this.dropdownBipMode.getSelectedItemId();
        }

        public void setDropdownBipMode(int BipMode) {
            this.dropdownBipMode.setSelection(BipMode);
        }

        public int getServoStayOn() {
            return (int) this.dropdownServoStayOn.getSelectedItemId();
        }

        public void setServoStayOn(int ServoStayOn) {
            this.dropdownServoStayOn.setSelection(ServoStayOn);
        }
        public int getServoSwitch() {
            return (int) this.dropdownServoSwitch.getSelectedItemId();
        }

        public void setServoSwitch(int ServoSwitch) {
            this.dropdownServoSwitch.setSelection(ServoSwitch);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart3_fragment, container, false);
            //Beep mode
            dropdownBipMode = (Spinner) view.findViewById(R.id.spinnerBipMode);
            //"Mode1", "Mode2", "Off"
            String[] items = new String[]{"Mode1", "Mode2",
                    getResources().getString(R.string.config_off)};
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items);
            dropdownBipMode.setAdapter(adapter);

            Log.d("UseTelemetryPort","before UseTelemetryPort" );
            //UseTelemetryPort
            dropdownUseTelemetryPort = (Spinner) view.findViewById(R.id.spinnerUseTelemetryPort);
            //"No", "Yes"
            String[] itemsUseTelemetryPort = new String[]{"No",
                    "Yes"};
            ArrayAdapter<String> adapterUseTelemetryPort = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsUseTelemetryPort);
            dropdownUseTelemetryPort.setAdapter(adapterUseTelemetryPort);

            Log.d("UseTelemetryPort","after UseTelemetryPort" );
            //units
            dropdownUnits = (Spinner) view.findViewById(R.id.spinnerUnit);
            //"Meters", "Feet"
            String[] items2 = new String[]{getResources().getString(R.string.unit_meter),
                    getResources().getString(R.string.unit_feet)};
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items2);
            dropdownUnits.setAdapter(adapter2);
            Log.d("UseTelemetryPort","after dropdownUnits" );
            // Tool tip
            view.findViewById(R.id.txtAltiUnit).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the altitude units you are familiar with
                            .text(getResources().getString(R.string.txtAltiUnit_tooltip))
                            .show();
                }
            });
            //Altimeter name
            altiName = (TextView) view.findViewById(R.id.txtAltiNameValue);
            //here you can set the beep frequency
            Freq = (EditText) view.findViewById(R.id.editTxtBipFreq);

            // Tool tip
            Freq.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewTooltip
                            .on(v)
                            .color(Color.BLACK)
                            .position(ViewTooltip.Position.TOP)
                            //Choose the beeping frequency in Hz
                            .text(getResources().getString(R.string.editTxtBipFreq_tooltip))
                            .show();
                }
            });

            altiID = (EditText) view.findViewById(R.id.editTxtAltiIDValue);
            dropdownServoStayOn= (Spinner) view.findViewById(R.id.spinnerServoStayOn);
            //"No", "Yes"
            String[] items3 = new String[]{getResources().getString(R.string.config_no),
                    getResources().getString(R.string.config_yes)};
            ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownServoStayOn.setAdapter(adapter3);

            txtServoStayOn = (TextView) view.findViewById(R.id.txtServoStayOn);
            if(lAltiCfg.getAltimeterName().equals("AltiServo")) {
                dropdownServoStayOn.setVisibility(View.VISIBLE);
                txtServoStayOn.setVisibility(View.VISIBLE);
            } else {
                dropdownServoStayOn.setVisibility(View.INVISIBLE);
                txtServoStayOn.setVisibility(View.INVISIBLE);
            }

            //ServoSwitch
            dropdownServoSwitch = (Spinner) view.findViewById(R.id.spinnerServoSwitch);
            dropdownServoSwitch.setAdapter(adapter3);

            txtServoSwitch = (TextView) view.findViewById(R.id.txtServoSwitch);

            if(lAltiCfg.getAltimeterName().equals("AltiServo")) {
                dropdownServoSwitch.setVisibility(View.VISIBLE);
                txtServoSwitch.setVisibility(View.VISIBLE);
            } else {
                dropdownServoSwitch.setVisibility(View.INVISIBLE);
                txtServoSwitch.setVisibility(View.INVISIBLE);
            }

            if (lAltiCfg != null) {
                altiName.setText(lAltiCfg.getAltimeterName() + " ver: " +
                        lAltiCfg.getAltiMajorVersion() + "." + lAltiCfg.getAltiMinorVersion());

                dropdownBipMode.setSelection(lAltiCfg.getBeepingMode());
                dropdownUnits.setSelection(lAltiCfg.getUnits());
                Freq.setText(String.valueOf(lAltiCfg.getBeepingFrequency()));
                altiID.setText(String.valueOf(lAltiCfg.getAltiID()));
                if (lAltiCfg.getAltimeterName().equals("AltiServo")) {
                    dropdownServoStayOn.setSelection(lAltiCfg.getServoStayOn());
                    dropdownServoSwitch.setSelection(lAltiCfg.getServoSwitch());
                }
                Log.d("UseTelemetryPort","before  getting config" );
                dropdownUseTelemetryPort.setSelection(lAltiCfg.getUseTelemetryPort());
                Log.d("UseTelemetryPort","after getting config" );
            }
            ViewCreated = true;
            return view;
        }

    }

    public static class Tab4Fragment extends Fragment {
        private static final String TAG = "Tab4Fragment";

        private EditText servo1OnPos, servo2OnPos, servo3OnPos, servo4OnPos;
        private EditText servo1OffPos, servo2OffPos, servo3OffPos, servo4OffPos;
        private TextView txtServo1OnPos, txtServo2OnPos, txtServo3OnPos, txtServo4OnPos;
        private TextView txtServo1OffPos, txtServo2OffPos, txtServo3OffPos, txtServo4OffPos;

        private boolean ViewCreated = false;
        private AltiConfigData lAltiCfg;

        public Tab4Fragment(AltiConfigData cfg) {
            lAltiCfg = cfg;
        }
        public boolean isViewCreated() {
            return ViewCreated;
        }


        public int getServo1OnPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo1OnPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo2OnPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo2OnPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo3OnPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo3OnPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo4OnPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo4OnPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo1OffPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo1OffPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo2OffPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo2OffPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo3OffPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo3OffPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getServo4OffPos() {
            int ret;
            try {
                ret = Integer.parseInt(this.servo4OffPos.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setServo1OnPos(int pos) {
            this.servo1OnPos.setText(pos);
        }

        public void setServo2OnPos(int pos) {
            this.servo2OnPos.setText(pos);
        }

        public void setServo3OnPos(int pos) {
            this.servo3OnPos.setText(pos);
        }

        public void setServo4OnPos(int pos) {
            this.servo4OnPos.setText(pos);
        }

        public void setServo1OffPos(int pos) {
            this.servo1OffPos.setText(pos);
        }

        public void setServo2OffPos(int pos) {
            this.servo2OffPos.setText(pos);
        }

        public void setServo3OffPos(int pos) {
            this.servo3OffPos.setText(pos);
        }

        public void setServo4OffPos(int pos) {
            this.servo4OffPos.setText(pos);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart4_fragment, container, false);


            servo1OnPos = (EditText) view.findViewById(R.id.editTxtServo1OnPos);
            servo2OnPos = (EditText) view.findViewById(R.id.editTxtServo2OnPos);
            servo3OnPos = (EditText) view.findViewById(R.id.editTxtServo3OnPos);
            servo4OnPos = (EditText) view.findViewById(R.id.editTxtServo4OnPos);

            txtServo1OnPos = (TextView) view.findViewById(R.id.servo1OnPos);
            txtServo2OnPos = (TextView) view.findViewById(R.id.servo2OnPos);
            txtServo3OnPos = (TextView) view.findViewById(R.id.servo3OnPos);
            txtServo4OnPos = (TextView) view.findViewById(R.id.servo4OnPos);

            servo1OffPos = (EditText) view.findViewById(R.id.editTxtServo1OffPos);
            servo2OffPos = (EditText) view.findViewById(R.id.editTxtServo2OffPos);
            servo3OffPos = (EditText) view.findViewById(R.id.editTxtServo3OffPos);
            servo4OffPos = (EditText) view.findViewById(R.id.editTxtServo4OffPos);

            txtServo1OffPos = (TextView) view.findViewById(R.id.servo1OffPos);
            txtServo2OffPos = (TextView) view.findViewById(R.id.servo2OffPos);
            txtServo3OffPos = (TextView) view.findViewById(R.id.servo3OffPos);
            txtServo4OffPos = (TextView) view.findViewById(R.id.servo4OffPos);

            if (lAltiCfg != null) {

                //Freq.setText(String.valueOf(AltiCfg.getBeepingFrequency()));
                servo1OnPos.setText(String.valueOf(lAltiCfg.getServo1OnPos()));
                servo2OnPos.setText(String.valueOf(lAltiCfg.getServo2OnPos()));
                servo3OnPos.setText(String.valueOf(lAltiCfg.getServo3OnPos()));
                servo4OnPos.setText(String.valueOf(lAltiCfg.getServo4OnPos()));

                servo1OffPos.setText(String.valueOf(lAltiCfg.getServo1OffPos()));
                servo2OffPos.setText(String.valueOf(lAltiCfg.getServo2OffPos()));
                servo3OffPos.setText(String.valueOf(lAltiCfg.getServo3OffPos()));
                servo4OffPos.setText(String.valueOf(lAltiCfg.getServo4OffPos()));
            }
            if (lAltiCfg.getAltimeterName().equals("AltiServo")) {
                servo1OnPos.setVisibility(View.VISIBLE);
                servo2OnPos.setVisibility(View.VISIBLE);
                servo3OnPos.setVisibility(View.VISIBLE);
                servo4OnPos.setVisibility(View.VISIBLE);
                servo1OffPos.setVisibility(View.VISIBLE);
                servo2OffPos.setVisibility(View.VISIBLE);
                servo3OffPos.setVisibility(View.VISIBLE);
                servo4OffPos.setVisibility(View.VISIBLE);

                txtServo1OnPos.setVisibility(View.VISIBLE);
                txtServo2OnPos.setVisibility(View.VISIBLE);
                txtServo3OnPos.setVisibility(View.VISIBLE);
                txtServo4OnPos.setVisibility(View.VISIBLE);
                txtServo1OffPos.setVisibility(View.VISIBLE);
                txtServo2OffPos.setVisibility(View.VISIBLE);
                txtServo3OffPos.setVisibility(View.VISIBLE);
                txtServo4OffPos.setVisibility(View.VISIBLE);
            } else {
                servo1OnPos.setVisibility(View.INVISIBLE);
                servo2OnPos.setVisibility(View.INVISIBLE);
                servo3OnPos.setVisibility(View.INVISIBLE);
                servo4OnPos.setVisibility(View.INVISIBLE);
                servo1OffPos.setVisibility(View.INVISIBLE);
                servo2OffPos.setVisibility(View.INVISIBLE);
                servo3OffPos.setVisibility(View.INVISIBLE);
                servo4OffPos.setVisibility(View.INVISIBLE);

                txtServo1OnPos.setVisibility(View.INVISIBLE);
                txtServo2OnPos.setVisibility(View.INVISIBLE);
                txtServo3OnPos.setVisibility(View.INVISIBLE);
                txtServo4OnPos.setVisibility(View.INVISIBLE);
                txtServo1OffPos.setVisibility(View.INVISIBLE);
                txtServo2OffPos.setVisibility(View.INVISIBLE);
                txtServo3OffPos.setVisibility(View.INVISIBLE);
                txtServo4OffPos.setVisibility(View.INVISIBLE);
            }
            ViewCreated = true;
            return view;
        }

    }

    /*private class RetrieveConfig extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            //"Retrieving Altimeter config...", "Please wait!!!"
            progress = ProgressDialog.show(AltimeterTabConfigActivity.this,
                    getResources().getString(R.string.msg5),
                    getResources().getString(R.string.msg6));  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            boolean success = false;
            success = readConfig();
            //second attempt
            if(!success)
                success = readConfig();
            //third attempt
            if(!success)
                success = readConfig();
            //fourth and last
            if(!success)
                success = readConfig();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            //AltiCfg =
            if (AltiCfg != null && configPage1 != null && configPage2 !=null && configPage3 !=null ) {

                //Config Tab 1
               if(configPage1.isViewCreated()) {
                    configPage1.setDropdownOut1(AltiCfg.getOutput1());
                    configPage1.setDropdownOut2(AltiCfg.getOutput2());
                    configPage1.setDropdownOut3(AltiCfg.getOutput3());
                    configPage1.setOutDelay1(AltiCfg.getOutput1Delay());
                    configPage1.setOutDelay2(AltiCfg.getOutput2Delay());
                    configPage1.setOutDelay3(AltiCfg.getOutput3Delay());
                    configPage1.setMainAltitude(AltiCfg.getMainAltitude());
                    configPage1.setDropdownOut4(AltiCfg.getOutput4());
                    configPage1.setOutDelay4(AltiCfg.getOutput4Delay());
                }
                //Config Tab 2
                if(configPage2.isViewCreated()) {
                    configPage2.setBaudRate(AltiCfg.getConnectionSpeed());
                    configPage2.setApogeeMeasures(AltiCfg.getNbrOfMeasuresForApogee());
                    configPage2.setAltimeterResolution(AltiCfg.getAltimeterResolution());
                    configPage2.setEEpromSize(AltiCfg.getEepromSize());
                    configPage2.setBeepOnOff(AltiCfg.getBeepOnOff());
                    configPage2.setRecordTempOnOff(AltiCfg.getRecordTemperature());
                    configPage2.setSupersonicDelayOnOff(AltiCfg.getSupersonicDelay());
                    configPage2.setEndRecordAltitude(AltiCfg.getEndRecordAltitude());
                }

                if(configPage3.isViewCreated()) {
                    configPage3.setAltiName(AltiCfg.getAltimeterName()+ " ver: " +
                            AltiCfg.getAltiMajorVersion()+"."+AltiCfg.getAltiMinorVersion());
                    configPage3.setDropdownBipMode(AltiCfg.getBeepingMode());
                    configPage3.setDropdownUnits(AltiCfg.getUnits());
                    configPage3.setFreq(AltiCfg.getBeepingFrequency());
                    configPage3.setServoStayOn(AltiCfg.getServoStayOn());
                    configPage3.setServoSwitch(AltiCfg.getServoSwitch());
                    configPage3.setAltiID(AltiCfg.getAltiID());
                    configPage3.setDropdownUseTelemetryPort(AltiCfg.getTelemetryPort());
                }


                if(AltiCfg.getAltimeterName().equals("AltiServo") && configPage4 !=null) {
                    if (configPage4.isViewCreated()) {
                        configPage4.setServo1OnPos(AltiCfg.getServo1OnPos());
                        configPage4.setServo2OnPos(AltiCfg.getServo2OnPos());
                        configPage4.setServo3OnPos(AltiCfg.getServo3OnPos());
                        configPage4.setServo4OnPos(AltiCfg.getServo4OnPos());

                        configPage4.setServo1OffPos(AltiCfg.getServo1OffPos());
                        configPage4.setServo2OffPos(AltiCfg.getServo2OffPos());
                        configPage4.setServo3OffPos(AltiCfg.getServo3OffPos());
                        configPage4.setServo4OffPos(AltiCfg.getServo4OffPos());
                    }
                }
            }
            progress.dismiss();
        }
    }*/
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
            Intent i = new Intent(AltimeterTabConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_config_alti");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(AltimeterTabConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
