package com.altimeter.bdureau.bearconsole.config;

import androidx.appcompat.app.AppCompatActivity;


//import android.content.Context;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import com.altimeter.bdureau.bearconsole.Flash.FlashFirmware;
import com.altimeter.bdureau.bearconsole.R;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.programmer.avr.UploadErrors;


public class Config3DR extends AppCompatActivity {

    Physicaloid mPhysicaloid;


    private String[] itemsBaudRate, itemsAirSpeed, itemsNetID, itemsTXPower,
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

    //index in an array
    public int arrayIndexPartial(String stringArray[], String pattern) {

        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].substring(0, 2).equals(pattern)) {
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
        int baud = 0;

        // go to AT mode
        boolean success = false;
        Log.d("Flight win", "retrieving config");

        baud = Connect(itemsBaudRate, 38400);

        //getAllConfig();
        new getAllConfigAsyc().execute();

    }

    public void onClickSaveConfig(View v) {
        int baud = 0;
        baud = Connect(itemsBaudRate, 38400);

        saveAllConfig();
    }

    private void close() {
        if (mPhysicaloid.isOpened()) {
            mPhysicaloid.close();
        }
    }

    public void saveAllConfig() {
        String value = "";
        long error = 0;

        //serial speed
        value = mInfo.runCommand("ATS1=" + dropdownBaudRate.getSelectedItem().toString().substring(0, 2));
        String ATS1[] = value.split("\n");
        Log.d("Flight win", "ATS1?:" + value);
        Log.d("Flight win", "ATS1=" + dropdownBaudRate.getSelectedItem());
        if (!ATS1[1].trim().equals("OK"))
            error++;
        //air speed
        value = mInfo.runCommand("ATS2=" + dropdownAirSpeed.getSelectedItem());
        String ATS2[] = value.split("\n");
        Log.d("Flight win", "ATS2?:" + value);
        Log.d("Flight win", "ATS2?:" + dropdownAirSpeed.getSelectedItem());

        if (!ATS2[1].trim().equals("OK"))
            error++;

        //net id
        value = mInfo.runCommand("ATS3=" + dropdownNetID.getSelectedItem());
        String ATS3[] = value.split("\n");
        Log.d("Flight win", "ATS3?:" + value);
        Log.d("Flight win", "ATS3?:" + dropdownNetID.getSelectedItem());
        if (!ATS3[1].trim().equals("OK"))
            error++;

        //TX power
        value = mInfo.runCommand("ATS4=" + dropdownTXPower.getSelectedItem());
        String ATS4[] = value.split("\n");
        Log.d("Flight win", "ATS4?:" + value);
        Log.d("Flight win", "ATS4?:" + dropdownTXPower.getSelectedItem());
        if (!ATS4[1].trim().equals("OK"))
            error++;

        //ECC
        if (checkBoxECC.isChecked())
            value = mInfo.runCommand("ATS5=1");
        else
            value = mInfo.runCommand("ATS5=0");
        String ATS5[] = value.split("\n");
        Log.d("Flight win", "ATS5?:" + value);
        if (!ATS5[1].trim().equals("OK"))
            error++;

        //Mav Link
        value = mInfo.runCommand("ATS6=" + dropdownMavLink.getSelectedItem());
        String ATS6[] = value.split("\n");
        Log.d("Flight win", "ATS6?:" + value);
        Log.d("Flight win", "ATS6?:" + dropdownMavLink.getSelectedItem());
        if (!ATS6[1].trim().equals("OK"))
            error++;

        //OPPRESEND
        if (checkBoxOpResend.isChecked())
            value = mInfo.runCommand("ATS7=1");
        else
            value = mInfo.runCommand("ATS7=0");
        String ATS7[] = value.split("\n");
        Log.d("Flight win", "ATS7?:" + value);
        if (!ATS7[1].trim().equals("OK"))
            error++;

        //MIN_FREQ
        value = mInfo.runCommand("ATS8=" + dropdownMinFreq.getSelectedItem());
        String ATS8[] = value.split("\n");
        Log.d("Flight win", "ATS8?:" + value);
        Log.d("Flight win", "ATS8?:" + dropdownMinFreq.getSelectedItem());
        if (!ATS8[1].trim().equals("OK"))
            error++;

        //MAX_FREQ
        value = mInfo.runCommand("ATS9=" + dropdownMaxFreq.getSelectedItem());
        String ATS9[] = value.split("\n");
        Log.d("Flight win", "ATS9?:" + value);
        Log.d("Flight win", "ATS9?:" + dropdownMaxFreq.getSelectedItem());
        if (!ATS9[1].trim().equals("OK"))
            error++;

        //NUM_CHANNELS
        value = mInfo.runCommand("ATS10=" + dropdownNbrOfChannel.getSelectedItem());
        String ATS10[] = value.split("\n");
        Log.d("Flight win", "ATS10?:" + value);
        Log.d("Flight win", "ATS10?:" + dropdownNbrOfChannel.getSelectedItem());
        if (!ATS10[1].trim().equals("OK"))
            error++;

        //DUTY_CYCLE
        value = mInfo.runCommand("ATS11=" + dropdownDutyCycle.getSelectedItem());
        String ATS11[] = value.split("\n");
        Log.d("Flight win", "ATS11?:" + value);
        Log.d("Flight win", "ATS11?:" + dropdownDutyCycle.getSelectedItem());
        if (!ATS11[1].trim().equals("OK"))
            error++;

        //LBT_RSSI
        value = mInfo.runCommand("ATS12=" + dropdownLBTRSSI.getSelectedItem());
        String ATS12[] = value.split("\n");
        Log.d("Flight win", "ATS12?:" + value);
        Log.d("Flight win", "ATS12?:" + dropdownLBTRSSI.getSelectedItem());
        if (!ATS12[1].trim().equals("OK"))
            error++;

        //MANCHESTER
        /*value = mInfo.runCommand("ATS13?");
        String ATS13[] = value.split("\n");
        Log.d("Flight win", "ATS13?:" + value);
        Log.d("Flight win", "ATS13?:" + ATS13[1]);*/
        //RTSCTS
        if (checkBoxRTSCTS.isChecked())
            value = mInfo.runCommand("ATS14=1");
        else
            value = mInfo.runCommand("ATS14=0");

        String ATS14[] = value.split("\n");
        Log.d("Flight win", "ATS14?:" + value);
        Log.d("Flight win", "ATS14?:" + ATS14[1]);
        if (!ATS14[1].trim().equals("OK"))
            error++;

        //MAX_WINDOW
        value = mInfo.runCommand("ATS15=" + dropdownMaxWindow.getSelectedItem());
        String ATS15[] = value.split("\n");
        Log.d("Flight win", "ATS15?:" + value);
        Log.d("Flight win", "ATS15?:" + dropdownMaxWindow.getSelectedItem());
        if (!ATS15[1].trim().equals("OK"))
            error++;

        if (error == 0) {
            value = mInfo.runCommand("AT&W");
            Log.d("Flight win", "ATS15?:" + value);
        }
        //Exit AT mode
        value = mInfo.runCommand("ATO");
        Log.d("Flight win", "ATS15?:" + value);
        DisableUI();
        btSaveConfig.setEnabled(false);
    }

    /*
    GetAllConfig

     */
    private class getAllConfigAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        String value = "";
        long error = 0;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(Config3DR.this);
            //Recover firmware...
            builder.setMessage("Loading:")
                    .setTitle("Retrieving config")
                    .setCancelable(false)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                            dialog.cancel();

                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            value = mInfo.runCommand("ATI");
            Log.d("Flight win", "ATI:" + value);
            String ATI[] = value.split("\n");
            //txtVersion.setText(ATI[1]);
            dialogAppend("running ATI");
            setVersion(ATI[1]);
            if (!ATI[1].trim().equals("OK"))
                error++;

            value = mInfo.runCommand("ATI2");
            String ATI2[] = value.split("\n");
            Log.d("Flight win", "ATI2:" + value);
            dialogAppend("running ATI2");
            if (!ATI2[1].trim().equals("OK"))
                error++;

            value = mInfo.runCommand("ATI3");
            Log.d("Flight win", "ATI3:" + value);
            dialogAppend("running ATI3");
            String ATI3[] = value.split("\n");
            if (!ATI3[1].trim().equals("OK"))
                error++;

            value = mInfo.runCommand("ATI4");
            Log.d("Flight win", "ATI4:" + value);
            dialogAppend("running ATI4");
            String ATI4[] = value.split("\n");
            if (!ATI4[1].trim().equals("OK"))
                error++;

            //format
            value = mInfo.runCommand("ATS0?");
            Log.d("Flight win", "ATS0?:" + value);
            //value = value.substring(0,value.indexOf('\n'));
            String ATS0[] = value.split("\n");
            Log.d("Flight win", "ATS0?:" + ATS0[1]);
            //txtFormat.setText(ATS0[1]);
            setFormat(ATS0[1]);
            dialogAppend("running ATS0");
            if (!ATS0[1].trim().equals("OK"))
                error++;

            //serial speed
            value = mInfo.runCommand("ATS1?");
            Log.d("Flight win", "ATS1?:" + value);
            String ATS1[] = value.split("\n");
            dialogAppend("running ATS1");
            Log.d("Flight win", "ATS1?:" + ATS1[1]);
            //dropdownBaudRate.setSelection(arrayIndexPartial(itemsBaudRate, ATS1[1].trim()));
            setBaudRate(arrayIndexPartial(itemsBaudRate, ATS1[1].trim()));
            //air speed
            value = mInfo.runCommand("ATS2?");
            String ATS2[] = value.split("\n");
            Log.d("Flight win", "ATS2?:" + value);
            dialogAppend("running ATS2");
            Log.d("Flight win", "ATS2?:" + ATS2[1] + "toto");
            //dropdownAirSpeed.setSelection(arrayIndex(itemsAirSpeed,ATS2[1] ));
            //dropdownAirSpeed.setSelection(arrayIndex(itemsAirSpeed, ATS2[1].trim()));
            setAirSpeed(arrayIndex(itemsAirSpeed, ATS2[1].trim()));
            //net id
            value = mInfo.runCommand("ATS3?");
            String ATS3[] = value.split("\n");
            Log.d("Flight win", "ATS3?:" + value);
            Log.d("Flight win", "ATS3?:" + ATS3[1]);
            dialogAppend("running ATS3");
            //dropdownNetID.setSelection(arrayIndex(itemsNetID, ATS3[1].trim()));
            setNetID(arrayIndex(itemsNetID, ATS3[1].trim()));
            //TX power
            value = mInfo.runCommand("ATS4?");
            String ATS4[] = value.split("\n");
            Log.d("Flight win", "ATS4?:" + value);
            Log.d("Flight win", "ATS4?:" + ATS4[1]);
            dialogAppend("running ATS4");
            //dropdownTXPower.setSelection(arrayIndex(itemsTXPower, ATS4[1].trim()));
            setTXPower(arrayIndex(itemsTXPower, ATS4[1].trim()));
            //ECC
            value = mInfo.runCommand("ATS5?");
            String ATS5[] = value.split("\n");
            Log.d("Flight win", "ATS5?:" + value);
            Log.d("Flight win", "ATS5?:" + ATS5[1].trim());
            dialogAppend("running ATS5");
            if (ATS5[1].trim().equals("1")) {
                //checkBoxECC.setSelected(true);
                setBoxECC(true);
            } else {
                //checkBoxECC.setSelected(false);
                setBoxECC(false);
            }

            //Mav Link
            value = mInfo.runCommand("ATS6?");
            String ATS6[] = value.split("\n");
            Log.d("Flight win", "ATS6?:" + value);
            Log.d("Flight win", "ATS6?:" + ATS6[1].trim());
            dialogAppend("running ATS6");
            //dropdownMavLink.setSelection(arrayIndex(itemsMavLink, ATS6[1].trim()));
            setMavLink(arrayIndex(itemsMavLink, ATS6[1].trim()));

            //OPPRESEND
            value = mInfo.runCommand("ATS7?");
            String ATS7[] = value.split("\n");
            Log.d("Flight win", "ATS7?:" + value);
            Log.d("Flight win", "ATS7?:" + ATS7[1].trim());
            dialogAppend("running ATS7");
            if (ATS7[1].trim().equals("1")) {
                //checkBoxOpResend.setSelected(true);
                setOpResend(true);
            } else {
                //checkBoxOpResend.setSelected(false);
                setOpResend(false);
            }

            //MIN_FREQ
            value = mInfo.runCommand("ATS8?");
            String ATS8[] = value.split("\n");
            Log.d("Flight win", "ATS8?:" + value);
            Log.d("Flight win", "ATS8?:" + ATS8[1]);
            dialogAppend("running ATS8");
            //dropdownMinFreq.setSelection(arrayIndex(itemsMinFreq, ATS8[1].trim()));
            setMinFreq(arrayIndex(itemsMinFreq, ATS8[1].trim()));

            //MAX_FREQ
            value = mInfo.runCommand("ATS9?");
            String ATS9[] = value.split("\n");
            Log.d("Flight win", "ATS9?:" + value);
            Log.d("Flight win", "ATS9?:" + ATS9[1]);
            dialogAppend("running ATS9");
            //dropdownMaxFreq.setSelection(arrayIndex(itemsMinFreq, ATS9[1].trim()));
            setMaxFreq(arrayIndex(itemsMinFreq, ATS9[1].trim()));

            //NUM_CHANNELS
            value = mInfo.runCommand("ATS10?");
            String ATS10[] = value.split("\n");
            Log.d("Flight win", "ATS10?:" + value);
            Log.d("Flight win", "ATS10?:" + ATS10[1]);
            dialogAppend("running ATS10");
            //dropdownNbrOfChannel.setSelection(arrayIndex(itemsNbrOfChannel, ATS10[1].trim()));
            setNbrOfChannel(arrayIndex(itemsNbrOfChannel, ATS10[1].trim()));

            //DUTY_CYCLE
            value = mInfo.runCommand("ATS11?");
            String ATS11[] = value.split("\n");
            Log.d("Flight win", "ATS11?:" + value);
            Log.d("Flight win", "ATS11?:" + ATS11[1]);
            dialogAppend("running ATS11");
            //dropdownDutyCycle.setSelection(arrayIndex(itemsDutyCycle, ATS11[1].trim()));
            setDutyCycle(arrayIndex(itemsDutyCycle, ATS11[1].trim()));

            //LBT_RSSI
            value = mInfo.runCommand("ATS12?");
            String ATS12[] = value.split("\n");
            Log.d("Flight win", "ATS12?:" + value);
            Log.d("Flight win", "ATS12?:" + ATS12[1]);
            dialogAppend("running ATS12");
            //dropdownLBTRSSI.setSelection(arrayIndex(itemsLBTRSSI, ATS12[1].trim()));
            setLBTRSSI(arrayIndex(itemsLBTRSSI, ATS12[1].trim()));

            //MANCHESTER
            value = mInfo.runCommand("ATS13?");
            String ATS13[] = value.split("\n");
            Log.d("Flight win", "ATS13?:" + value);
            Log.d("Flight win", "ATS13?:" + ATS13[1]);
            dialogAppend("running ATS13");

            //RTSCTS
            value = mInfo.runCommand("ATS14?");
            String ATS14[] = value.split("\n");
            Log.d("Flight win", "ATS14?:" + value);
            Log.d("Flight win", "ATS14?:" + ATS14[1]);
            dialogAppend("running ATS14");
            if (ATS14[1].trim().equals("1)")) {
                //checkBoxRTSCTS.setSelected(true);
                setBoxRTSCTS(true);
            } else {
                //checkBoxRTSCTS.setSelected(false);
                setBoxRTSCTS(false);
            }

            //MAX_WINDOW
            value = mInfo.runCommand("ATS15?");
            String ATS15[] = value.split("\n");
            Log.d("Flight win", "ATS15?:" + value);
            Log.d("Flight win", "ATS15?:" + ATS15[1]);
            //dropdownMaxWindow.setSelection(arrayIndex(itemsMaxWindow, ATS15[1].trim()));
            dialogAppend("running ATS15");
            setMaxWindow(arrayIndex(itemsMaxWindow, ATS15[1].trim()));

            //Exit AT mode
            value = mInfo.runCommand("ATO");
            Log.d("Flight win", "ATO:" + value);
            dialogAppend("running AT0");

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            EnableUI();
            btSaveConfig.setEnabled(true);
        }
    }


    public int Connect(String[] baudrate, int brate) {
        int baud = 0;


        if (brate == 0) {
            for (String rate : baudrate) {
                if (!mPhysicaloid.isOpened()) {
                    mPhysicaloid.open();
                }
                mPhysicaloid.setBaudrate(Integer.valueOf(rate));
                if (mInfo.ATMode()) {
                    baud = Integer.valueOf(rate);
                    Log.d("Flight win", "Success:" + rate);
                    break;
                } else {
                    Log.d("Flight win", "Failed:" + rate);

                }
            }
        } else {
            if (!mPhysicaloid.isOpened()) {
                if (mPhysicaloid.open()) {
                    mPhysicaloid.setBaudrate(brate);
                }
            }
            if (mInfo.ATMode()) {
                baud = brate;
            }
        }
        return baud;
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

            retval = recv(buf, 200);

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
            String value = null;
            command = command + "\n\r";
            drain();
            drain();
            byte[] cmd0 = command.getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);


            byte buf[] = new byte[200];
            int retval = 0;

            retval = recv(buf, 200);

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

        private int recv(byte[] buf, int length) {
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
                if ((endTime - startTime) > 500) {
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
                checkBoxRTSCTS.setChecked(true);
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
}
