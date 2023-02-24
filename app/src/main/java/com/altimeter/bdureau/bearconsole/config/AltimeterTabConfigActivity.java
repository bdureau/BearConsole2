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

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//tooltip library
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltimeterConfig1Fragment;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltimeterConfig2Fragment;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltimeterConfig3Fragment;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltimeterConfig4Fragment;


public class AltimeterTabConfigActivity extends AppCompatActivity {
    private static final String TAG = "AltimeterTabConfigActivity";

    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    AltimeterConfig1Fragment configPage1 = null;
    AltimeterConfig2Fragment configPage2 = null;
    AltimeterConfig3Fragment configPage3 = null;
    AltimeterConfig4Fragment configPage4 = null;

    private Button btnDismiss, btnUpload;
    ConsoleApplication myBT;
    private AltiConfigData AltiCfg = null;
    private ProgressDialog progress;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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

        configPage1 = new AltimeterConfig1Fragment(AltiCfg);
        configPage2 = new AltimeterConfig2Fragment(AltiCfg);
        configPage3 = new AltimeterConfig3Fragment(AltiCfg);
        adapter.addFragment(configPage1, "TAB1");
        adapter.addFragment(configPage2, "TAB2");
        adapter.addFragment(configPage3, "TAB3");

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo")) {
            configPage4 = new AltimeterConfig4Fragment(AltiCfg);
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
            AltiCfg.setEndRecordAltitude(configPage2.getEndRecordAltitude());
            AltiCfg.setTelemetryType(configPage2.getRecordTempOnOff());
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
        //if (myBT.getAppConf().getAllowMultipleDrogueMain().equals("false")) {
        if (!myBT.getAppConf().getAllowMultipleDrogueMain()) {
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

        SendParam("p,1,"+ AltiCfg.getUnits());
        SendParam("p,2,"+AltiCfg.getBeepingMode());
        SendParam("p,3,"+AltiCfg.getOutput1());
        SendParam("p,4,"+ AltiCfg.getOutput2());
        SendParam("p,5,"+AltiCfg.getOutput3());
        SendParam("p,6,"+AltiCfg.getMainAltitude());
        SendParam("p,7,"+AltiCfg.getSupersonicYesNo());
        SendParam("p,8,"+AltiCfg.getOutput1Delay());
        SendParam("p,9,"+AltiCfg.getOutput2Delay());
        SendParam("p,10,"+AltiCfg.getOutput3Delay());
        SendParam("p,11,"+AltiCfg.getBeepingFrequency());
        SendParam("p,12,"+AltiCfg.getNbrOfMeasuresForApogee());
        SendParam("p,13,"+AltiCfg.getEndRecordAltitude());
        SendParam("p,14,"+AltiCfg.getTelemetryType());
        SendParam("p,15,"+AltiCfg.getSupersonicDelay());
        SendParam("p,16,"+AltiCfg.getConnectionSpeed());
        SendParam("p,17,"+AltiCfg.getAltimeterResolution());
        SendParam("p,18,"+AltiCfg.getEepromSize());
        SendParam("p,19,"+AltiCfg.getBeepOnOff());
        SendParam("p,20,"+AltiCfg.getOutput4());
        SendParam("p,21,"+ AltiCfg.getOutput4Delay());
        SendParam("p,22,"+AltiCfg.getLiftOffAltitude());
        SendParam("p,23,"+AltiCfg.getBatteryType());
        SendParam("p,24,"+AltiCfg.getRecordingTimeout());
        SendParam("p,25,"+AltiCfg.getAltiID());
        Log.d("UseTelemetryPort", "before getUseTelemetryPort" );
        SendParam("p,26,"+AltiCfg.getUseTelemetryPort());
        Log.d("UseTelemetryPort", "after getUseTelemetryPort" );

        if (AltiCfg.getAltimeterName().equals("AltiServo")) {
            SendParam("p,27,"+AltiCfg.getServoSwitch());
            SendParam("p,28,"+AltiCfg.getServo1OnPos());
            SendParam("p,29,"+AltiCfg.getServo2OnPos());
            SendParam("p,30,"+AltiCfg.getServo3OnPos());
            SendParam("p,31,"+AltiCfg.getServo4OnPos());
            SendParam("p,32,"+AltiCfg.getServo1OffPos());
            SendParam("p,33,"+AltiCfg.getServo2OffPos());
            SendParam("p,34,"+AltiCfg.getServo3OffPos());
            SendParam("p,35,"+AltiCfg.getServo4OffPos());
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

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
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
