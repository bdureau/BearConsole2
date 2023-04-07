package com.altimeter.bdureau.bearconsole.config.ConfigModules;

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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.AppTabConfigActivity;
import com.physicaloid.lib.Physicaloid;

import java.nio.ByteBuffer;

/**
 * @description: This allows the configuration of Lora E32 Ebytes telemetry modules from an Android
 * phone or tablet using a ttl cable.
 * This is not perfect code but should work. Feel free to re-use it for your own project.
 * Make sure that you report any bugs or suggestions so that it can be improved
 * @author: boris.dureau@neuf.fr
 **/
public class ConfigLoraE32 extends AppCompatActivity {
    Physicaloid mPhysicaloid;
    public String TAG = "ConfigLoraE32.class";
    private Button btRetrieveConfig, btSaveConfig;

    private Spinner spinnerLoraBaudRate, spinnerLoraIOMode, spinnerLoraFixedMode,
            spinnerLoraParity, spinnerLoraWakeupTime, spinnerLoraFECswitch, spinnerLoraAirRate,
            spinnerLoraPower;
    private String[] itemsLoraBaudRate, itemsLoraIOMode, itemsLoraFixedMode,
            itemsLoraParity, itemsLoraWakeupTime, itemsLoraFECswitch, itemsLoraAirRate, itemsLoraPower;


    private StringBit[] strBLoraBaudRateVal, strBLoraIOModeVal, strBLoraFixedModeVal,
            strBLoraParityVal, strBLoraWakeupTimeVal, strBLoraFECswitchVal, strBLoraAirRateVal,
            strBLoraPowerVal;

    private TextView textLoraModuleValue;

    private EditText textLoraAddressValue, textLoraChannelValue;

    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    ConfigLoraE32.ModuleInfo mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_lorae32);

        btRetrieveConfig = (Button) findViewById(R.id.butLoraRetrieveConfig);
        btSaveConfig = (Button) findViewById(R.id.butLoraSaveConfig);
        spinnerLoraBaudRate = (Spinner) findViewById(R.id.spinnerLoraBaudRate);
        spinnerLoraFixedMode = (Spinner) findViewById(R.id.spinnerLoraFixedMode);
        spinnerLoraParity = (Spinner) findViewById(R.id.spinnerLoraParity);
        spinnerLoraFECswitch = (Spinner) findViewById(R.id.spinnerLoraFECswitch);
        spinnerLoraAirRate = (Spinner) findViewById(R.id.spinnerLoraAirRate);
        spinnerLoraPower = (Spinner) findViewById(R.id.spinnerLoraPower);
        spinnerLoraIOMode = (Spinner) findViewById(R.id.spinnerLoraIOMode);
        textLoraAddressValue = (EditText) findViewById(R.id.textLoraAddressValue);
        textLoraChannelValue = (EditText) findViewById(R.id.textLoraChannelValue);
        spinnerLoraWakeupTime = (Spinner) findViewById(R.id.spinnerLoraWakeupTime);

        textLoraModuleValue = (TextView) findViewById(R.id.textLoraModuleValue);

        // baud rate
        itemsLoraBaudRate = new String[]{
                "1200",
                "2400",
                "4800",
                "9600",
                "19200",
                "38400",
                "57600",
                "115200"
        };
        strBLoraBaudRateVal = new StringBit[]{
                new StringBit("1200", 0b000),
                new StringBit("2400", 0b001),
                new StringBit("4800", 0b010),
                new StringBit("9600", 0b011),
                new StringBit("19200", 0b100),
                new StringBit("38400", 0b101),
                new StringBit("57600", 0b110),
                new StringBit("115200", 0b111)

        };
        ArrayAdapter<String> adapterLoraBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraBaudRate);

        adapterLoraBaudRate.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraBaudRate.setAdapter(adapterLoraBaudRate);

        // IO Mode
        itemsLoraIOMode = new String[]{
                "OpenDrain",
                "PushPull"
        };
        strBLoraIOModeVal = new StringBit[]{
                new StringBit("OpenDrain", 0b0),
                new StringBit("PushPull", 0b1)
        };
        ArrayAdapter<String> adapterLoraIOModeVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraIOMode);
        adapterLoraIOModeVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraIOMode.setAdapter(adapterLoraIOModeVal);

        // Fixed mode
        itemsLoraFixedMode = new String[]{
                "Disable",
                "Enable"
        };
        strBLoraFixedModeVal = new StringBit[]{
                new StringBit("Disable", 0b0),
                new StringBit("Enable", 0b1)
        };
        ArrayAdapter<String> adapterLoraFixedModeVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraFixedMode);
        adapterLoraFixedModeVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraFixedMode.setAdapter(adapterLoraFixedModeVal);

        // Parity
        itemsLoraParity = new String[]{
                "8N1",
                "8O1",
                "8E1"
        };
        strBLoraParityVal = new StringBit[]{
                new StringBit("8N1", 0b00),
                new StringBit("8O1", 0b01),
                new StringBit("8E1", 0b10),
                new StringBit("8N1", 0b11)
        };
        ArrayAdapter<String> adapterLoraParityVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraParity);
        adapterLoraParityVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraParity.setAdapter(adapterLoraParityVal);

        // Wireless wake-up time
        itemsLoraWakeupTime = new String[]{
                "250ms",
                "500ms",
                "750ms",
                "1000ms",
                "1250ms",
                "1500ms",
                "1750ms",
                "2000ms"
        };
        strBLoraWakeupTimeVal = new StringBit[]{
                new StringBit("250ms", 0b000),
                new StringBit("500ms", 0b001),
                new StringBit("750ms", 0b010),
                new StringBit("1000ms", 0b011),
                new StringBit("1250ms", 0b100),
                new StringBit("1500ms", 0b101),
                new StringBit("1750ms", 0b110),
                new StringBit("2000ms", 0b111)
        };
        ArrayAdapter<String> adapterLoraWakeupTimeVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraWakeupTime);
        adapterLoraWakeupTimeVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraWakeupTime.setAdapter(adapterLoraWakeupTimeVal);

        //FEC switch
        itemsLoraFECswitch = new String[]{
                "Disable",
                "Enable"
        };
        strBLoraFECswitchVal = new StringBit[]{
                new StringBit("Disable", 0b0),
                new StringBit("Enable", 0b1)
        };
        ArrayAdapter<String> adapterLoraLBTVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraFECswitch);
        adapterLoraLBTVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraFECswitch.setAdapter(adapterLoraLBTVal);

        // AirRate
        itemsLoraAirRate = new String[]{
                "0.3Kbps",
                "1.2Kbps",
                "2.4Kbps",
                "4.8Kbps",
                "9.6Kbps",
                "19.2Kbps"
        };
        strBLoraAirRateVal = new StringBit[]{
                new StringBit("0.3Kbps", 0b000),
                new StringBit("1.2Kbps", 0b001),
                new StringBit("2.4Kbps", 0b010),
                new StringBit("4.8Kbps", 0b011),
                new StringBit("9.6Kbps", 0b100),
                new StringBit("19.2Kbps", 0b101),
                new StringBit("19.2Kbps", 0b110),
                new StringBit("19.2Kbps", 0b111)
        };
        ArrayAdapter<String> adapterAirRateVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraAirRate);
        adapterAirRateVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraAirRate.setAdapter(adapterAirRateVal);

        // Power
        itemsLoraPower = new String[]{
                "30dbm",
                "27dbm",
                "24dbm",
                "21dbm"
        };
        strBLoraPowerVal = new StringBit[]{
                new StringBit("30dbm", 0b00),
                new StringBit("27dbm", 0b01),
                new StringBit("24dbm", 0b10),
                new StringBit("21dbm", 0b11)
        };
        ArrayAdapter<String> adapterPowerVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraPower);
        adapterPowerVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraPower.setAdapter(adapterPowerVal);

        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.open();
        mInfo = new ConfigLoraE32.ModuleInfo(mPhysicaloid);

        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);

        builder = new AlertDialog.Builder(this);
        //Display info message
        builder.setMessage(R.string.lora_module_config_msg)
                .setTitle(R.string.lora_module_config_msg_title)
                .setCancelable(false)
                .setPositiveButton(R.string.lora_module_config_button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        alert = builder.create();
        alert.show();
    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void EnableUI() {
        spinnerLoraBaudRate.setEnabled(true);
        spinnerLoraIOMode.setEnabled(true);
        spinnerLoraFixedMode.setEnabled(true);
        spinnerLoraParity.setEnabled(true);
        spinnerLoraWakeupTime.setEnabled(true);
        spinnerLoraFECswitch.setEnabled(true);
        spinnerLoraAirRate.setEnabled(true);
        spinnerLoraPower.setEnabled(true);
    }

    public void DisableUI() {
        spinnerLoraBaudRate.setEnabled(false);
        spinnerLoraIOMode.setEnabled(false);
        spinnerLoraFixedMode.setEnabled(false);
        spinnerLoraParity.setEnabled(false);
        spinnerLoraWakeupTime.setEnabled(false);
        spinnerLoraFECswitch.setEnabled(false);
        spinnerLoraAirRate.setEnabled(false);
        spinnerLoraPower.setEnabled(false);
    }

    public static String searchStringFromBit(StringBit[] in, int search) {
        String ret = "";
        for (int i = 0; i < in.length; i++) {
            if (in[i].val == search) {
                ret = in[i].str;
                break;
            }
        }
        return ret;
    }

    public static int searchBitFromString(StringBit[] in, String search) {
        int ret = 0;
        for (int i = 0; i < in.length; i++) {
            if (in[i].str == search) {
                ret = in[i].val;
                break;
            }
        }
        return ret;
    }

    //index in an array
    public int arrayIndex(String stringArray[], String pattern) {
        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].equals(pattern)) {
                //Log.d("Config Lora", pattern + ":" + i);
                return i;
            }
        }
        return -1;
    }


    public void onClickDismiss(View v) {
        finish();
    }
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        finish();
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if(mPhysicaloid.isOpened())
            mPhysicaloid.close();
    }

    public void onClickRetrieveConfig(View v) {
        // go to AT mode
        Log.d("Config Lora", "retrieving config");
        new ConfigLoraE32.connectRetrieveAsyc().execute();
    }

    public void onClickSaveConfig(View v) {
        new ConfigLoraE32.connectSaveAsyc().execute();
    }


    public class StringBit {
        public String str;
        public int val;

        StringBit(String s, int i) {
            str = s;
            val = i;
        }
    }

    public class ModuleInfo {

        ModuleInfo(Physicaloid mPhysi) {

        }

        public void open() {
            mPhysicaloid.open();
        }

        public void setBaudrate(int baudRate) {
            mPhysicaloid.setBaudrate(baudRate);
        }


        public byte[] runCommand(byte command[]) {
            drain();
            drain();
            //byte[] cmd0 = command.getBytes();
            mPhysicaloid.write(command, command.length);

            byte buf[] = new byte[200];
            int retval = 0;
            retval = recv(buf, 200, 1000);

            return buf;
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


    /*
        GetAllConfig

    */
    private class getAllConfigAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        byte[] value;
        long error = 0;
        boolean cancelled = false;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigLoraE32.this);
            //Recover firmware...
            builder.setMessage(R.string.lora_module_loading_config_msg)
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
            if (!cancelled) {
                //byte[] cmd = {(byte) 0xC1, 0x00, 0x08};
                byte[] cmd = {(byte) 0xC1, (byte)0xC1, (byte)0xC1};
                value = mInfo.runCommand(cmd);
                if (value[0] == (byte) 0xC1) {
                    Log.d("Lora config", "We have a good config return");
                }
                //int module_address = ((value[1] & 0xFF<< 4) | (value[2] & 0xFF));
                byte [] bytes = {value[1] , value[2]};
                int module_address = ByteBuffer.wrap(bytes).getShort();
                Log.d("Lora Config", "module_address:" + module_address);
                setLoraAddressValue(module_address + "");

                //first 2 bits are the parity
                //int parity = ((value[5] & 0xFF) & 0b00011000) >> 3;
                int parity = (value[3] & 0xFF)  >> 6;
                Log.d("Lora Config", "parity:" + parity);
                String sParity = searchStringFromBit(strBLoraParityVal, parity);
                int iParity = arrayIndex(itemsLoraParity, sParity);
                setLoraParity(iParity);

                //next 3 bits are the baud rate
                //int baud_rate = (value[5] & 0xFF) >> 5;
                int baud_rate = ((value[3] & 0xFF) & 0b00111000)>> 3;
                Log.d("Lora Config", "baud_rate:" + baud_rate);
                String sBaud = searchStringFromBit(strBLoraBaudRateVal, baud_rate);
                int iBaud = arrayIndex(itemsLoraBaudRate, sBaud);
                setLoraBaudRate(iBaud);

                //next 3 bits are the air data rate
                int air_data_rate = (value[3] & 0xFF) & 0b00000111;
                Log.d("Lora Config", "air_data_rate:" + air_data_rate);
                String sAirRate = searchStringFromBit(strBLoraAirRateVal, air_data_rate);
                int iAirRate = arrayIndex(itemsLoraAirRate, sAirRate);
                setLoraAirRate(iAirRate);

                // byte 5 general specifications
                //first 3 are reserved
                int genSpec = (value[4] & 0xFF) >> 5;
                // next 5 are the communication channel
                int comChannel = (value[4] & 0xFF) & 0b00011111;
                Log.d("Lora Config", "channel:" + comChannel);
                setLoraChannelValue(comChannel + "");

                //byte 6
                //first 1 is fixed transmission enabling bit
                Log.d("Lora Config", "byte 6:" + (value[5] & 0xFF));
                int fixedTransEnabling = (value[5] & 0xFF)>>7;
                Log.d("Lora Config", "fixedTransEnabling:" + fixedTransEnabling);
                setLoraFixedMode(fixedTransEnabling);

                int ioDriveMode = ((value[5] & 0xFF)& 0b01000000) >>6;
                Log.d("Lora Config", "ioDriveMode:" + ioDriveMode);
                setLoraIOMode(ioDriveMode);

                int wirelessWakeupTime = ((value[5] & 0xFF)& 0b00111000) >>3;
                Log.d("Lora Config", "wirelessWakeupTime:" + wirelessWakeupTime);
                setLoraWakeupTime(wirelessWakeupTime);

                int FECswitch = ((value[5] & 0xFF)& 0b00000100) >>2;
                Log.d("Lora Config", "FECswitch:" + FECswitch);
                setLoraFECswitch(FECswitch);

                int transmissionPower = (value[5] & 0xFF)& 0b00000011;
                Log.d("Lora Config", "transmitting_power:" + transmissionPower);
                String sPower = searchStringFromBit(strBLoraPowerVal, transmissionPower);
                int iPower = arrayIndex(itemsLoraPower, sPower);
                setLoraPower(iPower);

            }
            if (cancelled) {
                //dialogAppend(getResources().getString(R.string.m3DR_cancel_retrieve));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            if (!cancelled & error == 0) {
                EnableUI();
                btSaveConfig.setEnabled(true);
            }
        }
    }

    private class saveAllConfigAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        String value = "";
        long error = 0;

        String sBaudRate, sIOMode, sFixedMode, sParity, sWakeupTime, sFECswitch,
                sAiRate, sPower,  sChannel, sAddress;

        String cancelMsg = "";

        boolean cancelled = false;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigLoraE32.this);
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

            sBaudRate = spinnerLoraBaudRate.getSelectedItem().toString();
            sIOMode = spinnerLoraIOMode.getSelectedItem().toString();
            sFixedMode = spinnerLoraFixedMode.getSelectedItem().toString();
            sParity = spinnerLoraParity.getSelectedItem().toString();
            sWakeupTime = spinnerLoraWakeupTime.getSelectedItem().toString();
            sFECswitch = spinnerLoraFECswitch.getSelectedItem().toString();
            sAiRate = spinnerLoraAirRate.getSelectedItem().toString();
            sPower = spinnerLoraPower.getSelectedItem().toString();
            sChannel = textLoraChannelValue.getText().toString();
            sAddress = textLoraAddressValue.getText().toString();

            //check channel
            if (sChannel.trim().equals("")) {
                cancelMsg = getString(R.string.lora_module_channel_msg);
                cancelled = true;
            } else {
                // check that we have a channel between 0 and 83
                if (sAddress.matches("\\d+(?:\\.\\d+)?")) {
                    if (!(Integer.valueOf(sChannel) >= 0 & Integer.valueOf(sChannel) < 84)) {
                        cancelMsg = cancelMsg + getString(R.string.lora_module_channel_value_msg);
                        cancelled = true;
                    }
                } else {
                    cancelMsg = cancelMsg + getString(R.string.lora_module_invalid_channel);
                    cancelled = true;
                }
            }

            // check the address
            if (sAddress.trim().equals("")) {
                cancelMsg = cancelMsg + getString(R.string.lora_module_address_empty_msg);
                cancelled = true;
            } else {
                //check that the non empty address is a valid one
                // ie 0 to FFFF (65535)
                if (sAddress.matches("\\d+(?:\\.\\d+)?")) {
                    if (!(Integer.valueOf(sAddress) >= 0 & Integer.valueOf(sAddress) < 65535)) {
                        cancelMsg = cancelMsg + getString(R.string.lora_module_address_range);
                        cancelled = true;
                    }
                } else {
                    cancelMsg = cancelMsg + getString(R.string.lora_module_address_invalid);
                    cancelled = true;
                }
            }
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //first split the address in 2 bytes
            //sAddress
            byte addh = 0;
            byte addl = 0;
            // change address
            //first split the address in 2 bytes
            //sAddress
            int iAddress = Integer.valueOf(sAddress);
            byte [] add = new byte[] {(byte)(iAddress >> 8),  (byte)iAddress };
            addh = add[0];
            addl = add[1];

            Log.d(TAG,"addh:" +addh );
            Log.d(TAG,"addl:" +addl );
            dialogAppend(getString(R.string.lora_module_updating_address_msg));


            //change parity,Baud Rate and air rate
            int byte4;
            dialogAppend(getString(R.string.lora_module_updating_msg2));

            int iBaudeRate = searchBitFromString(strBLoraBaudRateVal, sBaudRate);
            int iParity = searchBitFromString(strBLoraParityVal, sParity);
            int iAiRate = searchBitFromString(strBLoraAirRateVal, sAiRate);
            byte4 = iParity << 6 |iBaudeRate << 3 |  iAiRate;

            //updating packet , RSSI noise and transmitting power
            int byte5;
            dialogAppend(getString(R.string.lora_module_updating_msg4));
            Log.d("Lora Config:", sChannel);
            int channel = Integer.valueOf(sChannel);
            byte5 = channel;

            int byte6;
            int iFixedMode = searchBitFromString(strBLoraFixedModeVal, sFixedMode);
            int iIOMode = searchBitFromString(strBLoraIOModeVal, sIOMode);
            // sWakeupTime
            int iWakeupTime = searchBitFromString(strBLoraWakeupTimeVal, sWakeupTime);
            //sFECswitch
            int iFECswitch = searchBitFromString(strBLoraFECswitchVal, sFECswitch);
            //sPower
            int iPower = searchBitFromString(strBLoraPowerVal, sPower);
            byte6 = iFixedMode<<7 | iIOMode <<6 | iWakeupTime <<3 | iFECswitch <<2 |iPower;

           //saving
            if (!cancelled) {
                //dialogAppend(getString(R.string.lora_module_updating_msg6));
                byte cmd[] = {(byte) 0xC0,  (byte) addh, (byte) addl, (byte)byte4, (byte)byte5, (byte)byte6};
                byte[] value = mInfo.runCommand(cmd);
                if (value[0] != (byte) 0xC1)
                    error++;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            if (!cancelled & error == 0) {
                DisableUI();
                btSaveConfig.setEnabled(false);
            }
            if (cancelled)
                msg(cancelMsg);
        }
    }

    private class connectRetrieveAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigLoraE32.this);
            //
            builder.setMessage(R.string.lora_module_connecting_msg)
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
            if (!mPhysicaloid.isOpened()) {
                mPhysicaloid.open();
            }
            mPhysicaloid.setBaudrate(9600);
            dialogAppend(getResources().getString(R.string.m3DR_attempt_con2) + 9600);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            if (mPhysicaloid.isOpened())
                new getAllConfigAsyc().execute();
            else
                msg("Not connected!!");
        }
    }

    private class connectSaveAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigLoraE32.this);
            //.
            builder.setMessage(R.string.lora_module_connecting_msg)
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
            if (!mPhysicaloid.isOpened()) {
                mPhysicaloid.open();
            }

            if (mPhysicaloid.isOpened())
                mPhysicaloid.setBaudrate(9600);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            new saveAllConfigAsyc().execute();
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

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);

            }
        });
    }

    private void setLoraAddressValue(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textLoraAddressValue.setText(ftext);
            }
        });
    }

    private void setLoraChannelValue(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textLoraChannelValue.setText(ftext);
            }
        });
    }

    private void setLoraBaudRate(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraBaudRate.setSelection(findex);
            }
        });
    }

    private void setLoraIOMode(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraIOMode.setSelection(findex);
            }
        });
    }

    private void setLoraFixedMode(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraFixedMode.setSelection(findex);
            }
        });
    }

    private void setLoraParity(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraParity.setSelection(findex);
            }
        });
    }

    private void setLoraWakeupTime(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraWakeupTime.setSelection(findex);
            }
        });
    }

    private void setLoraFECswitch(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraFECswitch.setSelection(findex);
            }
        });
    }

    private void setLoraAirRate(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraAirRate.setSelection(findex);
            }
        });
    }

    private void setLoraPower(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraPower.setSelection(findex);
            }
        });
    }

    /*private void setLoraPacketRSSI(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraPacketRSSI.setSelection(findex);
            }
        });
    }*/

   /* private void setLoraPacketSize(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraPacketSize.setSelection(findex);
            }
        });
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

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }

        //open application settings screen
        if (id == R.id.action_settings) {
            Intent i = new Intent(ConfigLoraE32.this, AppTabConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open Lora config help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(ConfigLoraE32.this, HelpActivity.class);
            i.putExtra("help_file", "help_configLora");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(ConfigLoraE32.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}