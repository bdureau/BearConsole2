package com.altimeter.bdureau.bearconsole.config;

import androidx.appcompat.app.AppCompatActivity;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import com.physicaloid.lib.Physicaloid;


public class Config3DR extends AppCompatActivity {

    Physicaloid mPhysicaloid;


    private String[] itemsBaudRate, itemsBaudRateShort, itemsAirSpeed, itemsNetID, itemsTXPower,
            itemsMavLink, itemsMinFreq, itemsMaxFreq, itemsNbrOfChannel,
            itemsDutyCycle, itemsLBTRSSI, itemsMaxWindow;
    private Spinner dropdownBaudRate, dropdownAirSpeed, dropdownNetID, dropdownTXPower,
            dropdownMavLink, dropdownMinFreq, dropdownMaxFreq, dropdownNbrOfChannel,
            dropdownDutyCycle, dropdownLBTRSSI, dropdownMaxWindow;
    private TextView txtVersion, txtRSSI, txtFormat;
    private CheckBox checkBoxECC, checkBoxOpResend, checkBoxRTSCTS;
    private Button btRetrieveConfig, btSaveConfig;
    ModuleInfo mInfo;
    ConsoleApplication myBT;
    int currentBaudRate =0;

    private AlertDialog.Builder builder = null;
    private AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_config3_dr);

        txtVersion = (TextView) findViewById(R.id.textversionentry);
        txtRSSI = (TextView) findViewById(R.id.textRSSIentry);
        txtFormat = (TextView) findViewById(R.id.textFormatentry);
        checkBoxECC = (CheckBox) findViewById(R.id.checkBoxECC);
        checkBoxOpResend = (CheckBox) findViewById(R.id.checkBoxOpResend);
        checkBoxRTSCTS = (CheckBox) findViewById(R.id.checkBoxRTSCTS);
        btRetrieveConfig = (Button) findViewById(R.id.btRetrieveConfig);
        btSaveConfig = (Button) findViewById(R.id.btSaveConfig);
        //baud rate
        dropdownBaudRate = (Spinner) findViewById(R.id.spinnerBaudRate);
        itemsBaudRate = new String[]{
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

        itemsBaudRateShort = new String[]{
                "1",
                "2",
                "4",
                "9",
                "14",
                "19",
                "28",
                "38",
                "57",
                "115",
                "230"};

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsBaudRate);

        adapterBaudRate.setDropDownViewResource(android.R.layout.simple_spinner_item);
        dropdownBaudRate.setAdapter(adapterBaudRate);

        //air Speed
        dropdownAirSpeed = (Spinner) findViewById(R.id.spinnerAirSpeed);
        itemsAirSpeed = new String[]{"2", "4", "8", "16", "19", "24", "32", "48", "64", "96", "128", "192", "250"};

        ArrayAdapter<String> adapterAirSpeed = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsAirSpeed);
        dropdownAirSpeed.setAdapter(adapterAirSpeed);

        //NetID
        dropdownNetID = (Spinner) findViewById(R.id.spinnerNetID);
        //0 to 499
        itemsNetID = new String[500];
        for (int i = 0; i < 500; i++) {
            itemsNetID[i] = Integer.toString(i);
        }
        ArrayAdapter<String> adapterNetID = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsNetID);
        dropdownNetID.setAdapter(adapterNetID);


        //TX Power
        dropdownTXPower = (Spinner) findViewById(R.id.spinnerTXPower);
        //0 to 20
        itemsTXPower = new String[21];
        for (int i = 0; i < 21; i++) {
            itemsTXPower[i] = Integer.toString(i);
        }

        ArrayAdapter<String> adapterTXPower = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsTXPower);
        dropdownTXPower.setAdapter(adapterTXPower);

        //MavLink
        dropdownMavLink = (Spinner) findViewById(R.id.spinnerMavLink);
        itemsMavLink = new String[]{"RawData", "MavLink", "LowLatency"};

        ArrayAdapter<String> adapterMavLink = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsMavLink);
        dropdownMavLink.setAdapter(adapterMavLink);


        //414000 à 460000 step 50
        dropdownMinFreq = (Spinner) findViewById(R.id.spinnerMinFreq);
        itemsMinFreq = new String[921];
        int a = 0;
        for (int i = 414000; i < 460050; i = i + 50) {
            itemsMinFreq[a] = Integer.toString(i);
            a = a + 1;
        }

        ArrayAdapter<String> adapterMinFreq = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsMinFreq);
        dropdownMinFreq.setAdapter(adapterMinFreq);

        //414000 à 460000 step 50
        dropdownMaxFreq = (Spinner) findViewById(R.id.spinnerMaxFreq);
        //itemsMaxFreq = new String[921];

        ArrayAdapter<String> adapterMaxFreq = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsMinFreq);
        dropdownMaxFreq.setAdapter(adapterMaxFreq);

        //nbr of channel
        dropdownNbrOfChannel = (Spinner) findViewById(R.id.spinnerNbrOfChannel);
        itemsNbrOfChannel = new String[]{"5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
                "16", "17", "18", "19", "20", "30", "40", "50"};

        ArrayAdapter<String> adapterNbrOfChannel = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsNbrOfChannel);
        dropdownNbrOfChannel.setAdapter(adapterNbrOfChannel);

        // Duty Cycle
        dropdownDutyCycle = (Spinner) findViewById(R.id.spinnerDutyCycle);
        itemsDutyCycle = new String[]{"10", "20", "30", "40", "50", "60", "70", "80", "90", "100"};

        ArrayAdapter<String> adapterDutyCycle = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsDutyCycle);
        dropdownDutyCycle.setAdapter(adapterDutyCycle);

        //LBT Rssi
        dropdownLBTRSSI = (Spinner) findViewById(R.id.spinnerLBTRSSI);
        itemsLBTRSSI = new String[]{"0", "1"};

        ArrayAdapter<String> adapterLBTRSSI = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLBTRSSI);
        dropdownLBTRSSI.setAdapter(adapterLBTRSSI);

        //33 to 131
        dropdownMaxWindow = (Spinner) findViewById(R.id.spinnerMaxWindow);
        itemsMaxWindow = new String[99];
        for (int i = 33; i < 132; i++) {
            itemsMaxWindow[i - 33] = Integer.toString(i);
        }
        ArrayAdapter<String> adapterMaxWindow = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsMaxWindow);
        dropdownMaxWindow.setAdapter(adapterMaxWindow);

        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.open();
        /*if (mPhysicaloid.open()) {

            mPhysicaloid.setBaudrate(38400);
        }*/
        mInfo = new ModuleInfo(mPhysicaloid);
        //mInfo.open();
        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);

    }

    public void EnableUI() {

        dropdownBaudRate.setEnabled(true);
        dropdownAirSpeed.setEnabled(true);
        dropdownNetID.setEnabled(true);
        dropdownTXPower.setEnabled(true);
        dropdownMavLink.setEnabled(true);
        dropdownMinFreq.setEnabled(true);
        dropdownMaxFreq.setEnabled(true);
        dropdownNbrOfChannel.setEnabled(true);
        dropdownDutyCycle.setEnabled(true);
        dropdownLBTRSSI.setEnabled(true);
        dropdownMaxWindow.setEnabled(true);

        checkBoxECC.setEnabled(true);
        checkBoxOpResend.setEnabled(true);
        checkBoxRTSCTS.setEnabled(true);

    }

    public void DisableUI() {

        dropdownBaudRate.setEnabled(false);
        dropdownAirSpeed.setEnabled(false);
        dropdownNetID.setEnabled(false);
        dropdownTXPower.setEnabled(false);
        dropdownMavLink.setEnabled(false);
        dropdownMinFreq.setEnabled(false);
        dropdownMaxFreq.setEnabled(false);
        dropdownNbrOfChannel.setEnabled(false);
        dropdownDutyCycle.setEnabled(false);
        dropdownLBTRSSI.setEnabled(false);
        dropdownMaxWindow.setEnabled(false);

        checkBoxECC.setEnabled(false);
        checkBoxOpResend.setEnabled(false);
        checkBoxRTSCTS.setEnabled(false);
    }

    //index in an array
    public int arrayIndex(String stringArray[], String pattern) {

        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].equals(pattern)) {
                Log.d("Flight win", pattern + ":" + i);
                return i;
            }
        }
        return -1;
    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void onClickDismiss(View v) {
        close();
        finish();
    }

    public void onClickRetrieveConfig(View v) {
        // go to AT mode
        Log.d("Flight win", "retrieving config");

        new connectRetrieveAsyc().execute();

    }

    public void onClickSaveConfig(View v) {

        new connectSaveAsyc().execute();
    }

    private void close() {
        if (mPhysicaloid.isOpened()) {
            mPhysicaloid.close();
        }
    }


    /*
    GetAllConfig

     */
    private class getAllConfigAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        String value = "";
        long error = 0;
        boolean cancelled =false;
        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(Config3DR.this);
            //Recover firmware...
            builder.setMessage(getResources().getString(R.string.m3DR_load_cfg))
                    .setTitle(getResources().getString(R.string.m3DR_retrieving_cfg))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.m3DR_Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            cancelled = true;
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }



        @Override
        protected Void doInBackground(Void... voids) {
            if(!cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATI");
                value = mInfo.runCommand("ATI");
                Log.d("Flight win", "ATI:" + value);

                String ATI[] = value.split("\n");
                if(ATI.length>1) {
                    setVersion(ATI[1]);
                    if (ATI[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATI2");
                value = mInfo.runCommand("ATI2");
                String ATI2[] = value.split("\n");
                Log.d("Flight win", "ATI2:" + value);
                if(ATI2.length>1) {
                    if (ATI2[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATI3");
                value = mInfo.runCommand("ATI3");
                Log.d("Flight win", "ATI3:" + value);
                String ATI3[] = value.split("\n");
                if(ATI3.length>1) {
                    if (ATI3[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATI4");
                value = mInfo.runCommand("ATI4");
                Log.d("Flight win", "ATI4:" + value);
                String ATI4[] = value.split("\n");
                if(ATI4.length>1) {
                    if (ATI4[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }
            /*dialogAppend("running ATI5");
            value = mInfo.runCommand("ATI5");
            Log.d("Flight win", "ATI5:" + value);
            String ATI5[] = value.split("\n");
            if (ATI5[1].trim().equals("ERROR"))
                error++;*/

            if(!cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATI6");
                value = mInfo.runCommand("ATI6");
                Log.d("Flight win", "ATI6:" + value);
                String ATI6[] = value.split("\n");
                if(ATI6.length>1) {
                    if (ATI6[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }
            if(!cancelled) {
                //RSSI
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATI7");
                value = mInfo.runCommand("ATI7");
                Log.d("Flight win", "ATI7:" + value);
                String ATI7[] = value.split("\n");
                if(ATI7.length>1) {
                    setRSSI(ATI7[1].trim());
                    if (ATI7[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }
            if(!cancelled) {
                //format
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS0");
                value = mInfo.runCommand("ATS0?");
                Log.d("Flight win", "ATS0?:" + value);
                String ATS0[] = value.split("\n");

                if(ATS0.length>1) {
                    Log.d("Flight win", "ATS0?:" + ATS0[1]);
                    setFormat(ATS0[1]);
                    if (ATS0[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //serial speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS1");
                value = mInfo.runCommand("ATS1?");
                Log.d("Flight win", "ATS1?:" + value);
                String ATS1[] = value.split("\n");
                if(ATS1.length>1) {
                    Log.d("Flight win", "ATS1?:" + ATS1[1]);

                    setBaudRate(arrayIndex(itemsBaudRateShort, ATS1[1].trim()));
                    if (ATS1[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //air speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS2");
                value = mInfo.runCommand("ATS2?");
                String ATS2[] = value.split("\n");
                Log.d("Flight win", "ATS2?:" + value);
                if(ATS2.length>1) {
                    Log.d("Flight win", "ATS2?:" + ATS2[1]);

                    setAirSpeed(arrayIndex(itemsAirSpeed, ATS2[1].trim()));
                    if (ATS2[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //net id
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS3");
                value = mInfo.runCommand("ATS3?");
                String ATS3[] = value.split("\n");
                Log.d("Flight win", "ATS3?:" + value);

                if(ATS3.length>1) {
                    Log.d("Flight win", "ATS3?:" + ATS3[1]);

                    setNetID(arrayIndex(itemsNetID, ATS3[1].trim()));
                    if (ATS3[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //TX power
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS4");
                value = mInfo.runCommand("ATS4?");
                String ATS4[] = value.split("\n");
                Log.d("Flight win", "ATS4?:" + value);
                if(ATS4.length>1) {
                    Log.d("Flight win", "ATS4?:" + ATS4[1]);

                    setTXPower(arrayIndex(itemsTXPower, ATS4[1].trim()));
                    if (ATS4[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //ECC
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS5");
                value = mInfo.runCommand("ATS5?");
                String ATS5[] = value.split("\n");
                Log.d("Flight win", "ATS5?:" + value);


                if(ATS5.length>1) {
                    Log.d("Flight win", "ATS5?:" + ATS5[1].trim());
                    if (ATS5[1].trim().equals("1")) {
                        setBoxECC(true);
                    } else {
                        setBoxECC(false);
                    }
                    if (ATS5[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //Mav Link
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS6");
                value = mInfo.runCommand("ATS6?");
                String ATS6[] = value.split("\n");
                Log.d("Flight win", "ATS6?:" + value);

                if(ATS6.length>1) {
                    Log.d("Flight win", "ATS6?:" + ATS6[1].trim());

                    setMavLink(arrayIndex(itemsMavLink, ATS6[1].trim()));
                    if (ATS6[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //OPPRESEND
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS7");
                value = mInfo.runCommand("ATS7?");
                String ATS7[] = value.split("\n");
                Log.d("Flight win", "ATS7?:" + value);
                if(ATS7.length>1) {
                    Log.d("Flight win", "ATS7?:" + ATS7[1].trim());

                    if (ATS7[1].trim().equals("1")) {
                        setOpResend(true);
                    } else {
                        setOpResend(false);
                    }
                    if (ATS7[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MIN_FREQ
                dialogAppend(getResources().getString(R.string.m3DR_run_command)+" ATS8");
                value = mInfo.runCommand("ATS8?");
                String ATS8[] = value.split("\n");
                Log.d("Flight win", "ATS8?:" + value);
                if(ATS8.length>1) {
                    Log.d("Flight win", "ATS8?:" + ATS8[1]);

                    setMinFreq(arrayIndex(itemsMinFreq, ATS8[1].trim()));
                    if (ATS8[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MAX_FREQ
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS9");
                value = mInfo.runCommand("ATS9?");
                String ATS9[] = value.split("\n");
                Log.d("Flight win", "ATS9?:" + value);
                if(ATS9.length>1) {
                    Log.d("Flight win", "ATS9?:" + ATS9[1]);

                    setMaxFreq(arrayIndex(itemsMinFreq, ATS9[1].trim()));
                    if (ATS9[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //NUM_CHANNELS
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS10");
                value = mInfo.runCommand("ATS10?");
                String ATS10[] = value.split("\n");
                Log.d("Flight win", "ATS10?:" + value);
                if(ATS10.length>1) {
                    Log.d("Flight win", "ATS10?:" + ATS10[1]);

                    setNbrOfChannel(arrayIndex(itemsNbrOfChannel, ATS10[1].trim()));
                    if (ATS10[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //DUTY_CYCLE
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS11");
                value = mInfo.runCommand("ATS11?");
                String ATS11[] = value.split("\n");
                Log.d("Flight win", "ATS11?:" + value);
                if(ATS11.length>1) {
                    Log.d("Flight win", "ATS11?:" + ATS11[1]);

                    setDutyCycle(arrayIndex(itemsDutyCycle, ATS11[1].trim()));
                    if (ATS11[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //LBT_RSSI
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS12");
                value = mInfo.runCommand("ATS12?");
                String ATS12[] = value.split("\n");
                Log.d("Flight win", "ATS12?:" + value);

                if(ATS12.length>1) {
                    Log.d("Flight win", "ATS12?:" + ATS12[1]);

                    setLBTRSSI(arrayIndex(itemsLBTRSSI, ATS12[1].trim()));
                    if (ATS12[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MANCHESTER
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS13");
                value = mInfo.runCommand("ATS13?");
                String ATS13[] = value.split("\n");
                Log.d("Flight win", "ATS13?:" + value);
                if(ATS13.length>1) {
                    Log.d("Flight win", "ATS13?:" + ATS13[1]);

                    if (ATS13[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //RTSCTS
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS14");
                value = mInfo.runCommand("ATS14?");
                String ATS14[] = value.split("\n");
                Log.d("Flight win", "ATS14?:" + value);
                if(ATS14.length>1) {
                    Log.d("Flight win", "ATS14?:" + ATS14[1]);

                    if (ATS14[1].trim().equals("1")) {
                        setBoxRTSCTS(true);
                    } else {
                        setBoxRTSCTS(false);
                    }
                    if (ATS14[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MAX_WINDOW
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " ATS15");
                value = mInfo.runCommand("ATS15?");
                String ATS15[] = value.split("\n");
                Log.d("Flight win", "ATS15?:" + value);
                if(ATS15.length>1) {
                    Log.d("Flight win", "ATS15?:" + ATS15[1]);

                    setMaxWindow(arrayIndex(itemsMaxWindow, ATS15[1].trim()));
                    if (ATS15[1].trim().equals("ERROR"))
                        error++;
                }
                else
                    error++;
            }
            if(cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_cancel_retrieve));
            }
            //Exit AT mode
            dialogAppend(getResources().getString(R.string.m3DR_run_at0));
            value = mInfo.runCommand("ATO");
            Log.d("Flight win", "ATO:" + value);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            if(!cancelled & error == 0) {
                EnableUI();
                btSaveConfig.setEnabled(true);
            }
        }
    }

    private class saveAllConfigAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        String value = "";
        long error = 0;
        String baudRate, airSpeed, netID, txPower, mavLink, minFreq, maxFreq, nbrOfChannel, dutyCycle, LBTRSSI, maxWindow;
        boolean boxECC, opResend, RTSCTS;
        boolean cancelled = false;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(Config3DR.this);
            //Running Saving commands
            builder.setMessage(getResources().getString(R.string.m3DR_run_save))
                    .setTitle(getResources().getString(R.string.m3DR_save_cfg))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.m3DR_Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                            cancelled = true;
                            dialog.cancel();

                        }
                    });

            baudRate = dropdownBaudRate.getSelectedItem().toString().substring(0, 2);
            airSpeed = dropdownAirSpeed.getSelectedItem().toString();
            netID = dropdownNetID.getSelectedItem().toString();
            txPower = dropdownTXPower.getSelectedItem().toString();

            if(checkBoxECC.isChecked())
                boxECC =true;
            else
                boxECC = false;

            mavLink = dropdownMavLink.getSelectedItem().toString();

            if(checkBoxOpResend.isChecked())
                opResend = true;
            else
                opResend = false;

            minFreq = dropdownMinFreq.getSelectedItem().toString();
            maxFreq = dropdownMaxFreq.getSelectedItem().toString();
            nbrOfChannel = dropdownNbrOfChannel.getSelectedItem().toString();
            dutyCycle = dropdownDutyCycle.getSelectedItem().toString();
            LBTRSSI = dropdownLBTRSSI.getSelectedItem().toString();

            if(checkBoxRTSCTS.isChecked())
                RTSCTS = true;
            else
                RTSCTS = false;

            maxWindow=dropdownMaxWindow.getSelectedItem().toString();
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            if(!cancelled) {
                //serial speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command)+" ATS1");
                value = mInfo.runCommand("ATS1=" + baudRate);
                String ATS1[] = value.split("\n");
                Log.d("Flight win", "ATS1?:" + value);
                Log.d("Flight win", "ATS1=" + baudRate);
                if(ATS1.length>1) {
                    if (!ATS1[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //air speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command)+" ATS2");
                value = mInfo.runCommand("ATS2=" + airSpeed);
                String ATS2[] = value.split("\n");
                Log.d("Flight win", "ATS2?:" + value);
                Log.d("Flight win", "ATS2?:" + airSpeed);
                if(ATS2.length>1) {
                    if (!ATS2[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //net id
                dialogAppend(getResources().getString(R.string.m3DR_run_command)+" ATS3");
                value = mInfo.runCommand("ATS3=" + netID);
                String ATS3[] = value.split("\n");
                Log.d("Flight win", "ATS3?:" + value);
                Log.d("Flight win", "ATS3?:" + netID);
                if(ATS3.length>1) {
                    if (!ATS3[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //TX power
                dialogAppend(getResources().getString(R.string.m3DR_run_command)+" ATS4");
                value = mInfo.runCommand("ATS4=" + txPower);
                String ATS4[] = value.split("\n");
                Log.d("Flight win", "ATS4?:" + value);
                Log.d("Flight win", "ATS4?:" + txPower);
                if(ATS4.length>1) {
                    if (!ATS4[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //ECC
                dialogAppend(getResources().getString(R.string.m3DR_run_command)+" ATS5");
                if (boxECC)
                    value = mInfo.runCommand("ATS5=1");
                else
                    value = mInfo.runCommand("ATS5=0");

                String ATS5[] = value.split("\n");
                Log.d("Flight win", "ATS5?:" + value);
                if(ATS5.length>1) {
                    if (!ATS5[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //Mav Link
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS6");
                value = mInfo.runCommand("ATS6=" + mavLink);
                String ATS6[] = value.split("\n");
                Log.d("Flight win", "ATS6?:" + value);
                Log.d("Flight win", "ATS6?:" + mavLink);
                if(ATS6.length>1) {
                    if (!ATS6[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //OPPRESEND
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS7");
                if (opResend)
                    value = mInfo.runCommand("ATS7=1");
                else
                    value = mInfo.runCommand("ATS7=0");
                String ATS7[] = value.split("\n");
                Log.d("Flight win", "ATS7?:" + value);
                if(ATS7.length>1) {
                    if (!ATS7[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MIN_FREQ
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS8");
                value = mInfo.runCommand("ATS8=" + minFreq);
                String ATS8[] = value.split("\n");
                Log.d("Flight win", "ATS8?:" + value);
                Log.d("Flight win", "ATS8?:" + minFreq);
                if(ATS8.length>1) {
                    if (!ATS8[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MAX_FREQ
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS9");
                value = mInfo.runCommand("ATS9=" + maxFreq);
                String ATS9[] = value.split("\n");
                Log.d("Flight win", "ATS9?:" + value);
                Log.d("Flight win", "ATS9?:" + maxFreq);
                if(ATS9.length>1) {
                    if (!ATS9[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //NUM_CHANNELS
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS10");
                value = mInfo.runCommand("ATS10=" + nbrOfChannel);
                String ATS10[] = value.split("\n");
                Log.d("Flight win", "ATS10?:" + value);
                Log.d("Flight win", "ATS10?:" + nbrOfChannel);
                if(ATS10.length>1) {
                    if (!ATS10[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //DUTY_CYCLE
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS11");
                value = mInfo.runCommand("ATS11=" + dutyCycle);
                String ATS11[] = value.split("\n");
                Log.d("Flight win", "ATS11?:" + value);
                Log.d("Flight win", "ATS11?:" + dutyCycle);
                if(ATS11.length>1) {
                    if (!ATS11[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //LBT_RSSI
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS12");
                value = mInfo.runCommand("ATS12=" + LBTRSSI);
                String ATS12[] = value.split("\n");
                Log.d("Flight win", "ATS12?:" + value);
                Log.d("Flight win", "ATS12?:" + LBTRSSI);
                if(ATS12.length>1) {
                    if (!ATS12[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MANCHESTER
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS13");
                value = mInfo.runCommand("ATS13?");
                String ATS13[] = value.split("\n");
                Log.d("Flight win", "ATS13?:" + value);
                if(ATS13.length>1) {
                    Log.d("Flight win", "ATS13?:" + ATS13[1]);
                }
                else
                    error++;
            }

            if(!cancelled) {
                //RTSCTS
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS14");
                if (RTSCTS)
                    value = mInfo.runCommand("ATS14=1");
                else
                    value = mInfo.runCommand("ATS14=0");

                String ATS14[] = value.split("\n");
                Log.d("Flight win", "ATS14?:" + value);
               // Log.d("Flight win", "ATS14?:" + ATS14[1]);
                if(ATS14.length>1) {
                    if (!ATS14[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                //MAX_WINDOW
                dialogAppend(getResources().getString(R.string.m3DR_run_command) +" ATS15");
                value = mInfo.runCommand("ATS15=" + maxWindow);
                String ATS15[] = value.split("\n");
                Log.d("Flight win", "ATS15?:" + value);
                Log.d("Flight win", "ATS15?:" + maxWindow);
                if(ATS15.length>1) {
                    if (!ATS15[1].trim().equals("OK"))
                        error++;
                }
                else
                    error++;
            }

            if(!cancelled) {
                if (error == 0) {
                    dialogAppend(getResources().getString(R.string.m3DR_no_errors));
                    value = mInfo.runCommand("AT&W");
                    Log.d("Flight win", "AT&W:" + value);
                } else {
                    dialogAppend(getResources().getString(R.string.m3DR_we_have_errors));
                }
            }
            else {
                dialogAppend("Saving has been cancelled");
            }
            //Exit AT mode
            value = mInfo.runCommand("ATO");
            Log.d("Flight win", "ATO" + value);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            if(!cancelled & error == 0) {
                DisableUI();
                btSaveConfig.setEnabled(false);
            }
        }
    }



    private class connectRetrieveAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(Config3DR.this);
            //
            builder.setMessage(getResources().getString(R.string.m3DR_attempt_con))
                    .setTitle(getResources().getString(R.string.m3DR_connecting))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.m3DR_Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();

        }

        @Override
        protected Void doInBackground(Void... voids) {

            if (currentBaudRate == 0) {
                for (String rate : itemsBaudRate) {
                    if (!mPhysicaloid.isOpened()) {
                        mPhysicaloid.open();
                    }
                    mPhysicaloid.setBaudrate(Integer.valueOf(rate));
                    dialogAppend(getResources().getString(R.string.m3DR_attempt_con2) + rate);
                    if (mInfo.ATMode()) {
                        currentBaudRate = Integer.valueOf(rate);
                        Log.d("Flight win", "Success:" + rate);
                        dialogAppend(getResources().getString(R.string.m3DR_connect_ok) + rate);
                        //setBaudRate(arrayIndex(itemsBaudRate, rate));
                        break;
                    } else {
                        Log.d("Flight win", "Failed:" + rate);

                    }
                }
            } else {
                if (!mPhysicaloid.isOpened()) {
                    if (mPhysicaloid.open()) {
                        mPhysicaloid.setBaudrate(currentBaudRate);
                    }
                }
                if (mInfo.ATMode()) {

                } else {
                    Log.d("Flight win", "Failed:" + currentBaudRate);
                    currentBaudRate = 0;
                }

            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
            {
                alert.dismiss();

                if(currentBaudRate > 0) {
                    new getAllConfigAsyc().execute();

                }
                else {
                    msg(getResources().getString(R.string.m3DR_failed_con));

                }

            }
        }

    private class connectSaveAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(Config3DR.this);
            //.
            builder.setMessage(getResources().getString(R.string.m3DR_attempt_con))
                    .setTitle(getResources().getString(R.string.m3DR_connecting))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.m3DR_Cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();

        }

        @Override
        protected Void doInBackground(Void... voids) {

            if (currentBaudRate == 0) {
                for (String rate : itemsBaudRate) {
                    if (!mPhysicaloid.isOpened()) {
                        mPhysicaloid.open();
                    }
                    mPhysicaloid.setBaudrate(Integer.valueOf(rate));
                    dialogAppend(getResources().getString(R.string.m3DR_attempt_con2) + rate);
                    if (mInfo.ATMode()) {
                        currentBaudRate = Integer.valueOf(rate);
                        Log.d("Flight win", "Success:" + rate);
                        dialogAppend(getResources().getString(R.string.m3DR_connect_ok) + rate);
                        break;
                    } else {
                        Log.d("Flight win", "Failed:" + rate);

                    }
                }
            } else {
                if (!mPhysicaloid.isOpened()) {
                    if (mPhysicaloid.open()) {
                        mPhysicaloid.setBaudrate(currentBaudRate);
                    }
                }
                if (mInfo.ATMode()) {
                    //baud = brate;
                } else {
                    Log.d("Flight win", "Failed:" + currentBaudRate);
                    currentBaudRate = 0;
                }

            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            //connectingDone =true;
            if(currentBaudRate > 0) {
                //new getAllConfigAsyc().execute();
                new saveAllConfigAsyc().execute();
                //currentBaudRate = baud;
            }
            else {
                msg(getResources().getString(R.string.m3DR_failed_con));
            }

        }
    }

    public class ModuleInfo {
        // Physicaloid lPhysicaloid;

        ModuleInfo(Physicaloid mPhysi) {
            //lPhysicaloid = mPhysi;
        }

        public void open() {
            mPhysicaloid.open();

        }

        public void setBaudrate(int baudRate) {

            mPhysicaloid.setBaudrate(baudRate);
        }

        public boolean ATMode() {
            boolean at = false;
            //fast reading
            drain();
            drain();
            byte[] cmd0 = "+++".getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);


            byte buf[] = new byte[200];
            int retval = 0;

            retval = recv(buf, 200, 1000);

            Log.d("Flight win", "retval:" + retval);
            if (retval > 1) {
                String str = null;

                str = new String(buf);
                Log.d("Flight win", str);
                if (str.substring(0, 2).equals("OK")) {
                    at = true;
                    Log.d("Flight win", "connected!!!");
                }
            }

            return at;
        }

        public String runCommand(String command) {
            String value = "";
            command = command + "\n\r";
            drain();
            drain();
            byte[] cmd0 = command.getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);


            byte buf[] = new byte[200];
            int retval = 0;

            retval = recv(buf, 200, 500);

            if (retval > 0) {
                value = new String(buf);
            }

            return value;
        }

        public int drain() {
            byte[] buf = new byte[1];
            int retval = 0;
            long endTime;
            long startTime = System.currentTimeMillis();
            while (true) {
                retval = mPhysicaloid.read(buf, 1);
                if (retval > 0) {

                }
                endTime = System.currentTimeMillis();
                if ((endTime - startTime) > 1000) {
                    break;
                }
            }
            return retval;
        }

        private int recv(byte[] buf, int length, int timeout) {
            int retval = 0;
            int totalRetval = 0;
            long endTime;
            long startTime = System.currentTimeMillis();
            byte[] tmpbuf = new byte[length];

            while (true) {
                retval = mPhysicaloid.read(tmpbuf, length);

                if (retval > 0) {
                    System.arraycopy(tmpbuf, 0, buf, totalRetval, retval);
                    totalRetval += retval;
                    startTime = System.currentTimeMillis();

                }
                if (totalRetval >= length) {
                    break;
                }

                endTime = System.currentTimeMillis();
                if ((endTime - startTime) > timeout) {
                    break;
                }
            }
            return totalRetval;
        }

        public void close() {
            mPhysicaloid.close();
        }

    }

    Handler mHandler = new Handler();

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }

    private void setVersion( CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txtVersion.setText(ftext);
            }
        });
    }
    private void setRSSI( CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txtRSSI.setText(ftext);
            }
        });
    }

    private void setFormat( CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txtFormat.setText(ftext);
            }
        });
    }

    private void setBaudRate( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownBaudRate.setSelection(findex);
            }
        });
    }
    private void setAirSpeed( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownAirSpeed.setSelection(findex);
            }
        });
    }
    private void setNetID( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownNetID.setSelection(findex);
            }
        });
    }
    private void setTXPower( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownTXPower.setSelection(findex);
            }
        });
    }

    private void setBoxECC( boolean val) {
        final boolean fval = val;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkBoxECC.setChecked(fval);
            }
        });
    }

    private void setMavLink( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownMavLink.setSelection(findex);
            }
        });
    }

    private void setOpResend( boolean val) {
        final boolean fval = val;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkBoxOpResend.setChecked(fval);
            }
        });
    }

    private void setMinFreq( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownMinFreq.setSelection(findex);
            }
        });
    }
    private void setMaxFreq( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownMaxFreq.setSelection(findex);
            }
        });
    }
    private void setNbrOfChannel( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownNbrOfChannel.setSelection(findex);
            }
        });
    }
    private void setDutyCycle( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownDutyCycle.setSelection(findex);
            }
        });
    }
    private void setLBTRSSI( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownLBTRSSI.setSelection(findex);
            }
        });
    }
    private void setBoxRTSCTS( boolean index) {
        final boolean findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkBoxRTSCTS.setSelected(findex);
            }
        });
    }
    private void setMaxWindow( int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownMaxWindow.setSelection(findex);
            }
        });
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

        //open application settings screen
        if (id == R.id.action_settings) {
            Intent i= new Intent(Config3DR.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i= new Intent(Config3DR.this, HelpActivity.class);
            i.putExtra("help_file", "help_config3DR");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i= new Intent(Config3DR.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
