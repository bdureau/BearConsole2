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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.physicaloid.lib.Physicaloid;
/**
 * @description: This allows the configuration of Lora Ebytes telemetry modules from an Android
 * phone or tablet using a ttl cable.
 * This is not perfect code but should work. Feel free to re-use it for your own project.
 * Make sure that you report any bugs or suggestions so that it can be improved
 * @author: boris.dureau@neuf.fr
 **/
public class ConfigLora extends AppCompatActivity {
    Physicaloid mPhysicaloid;

    private Button btRetrieveConfig, btSaveConfig;

    private Spinner spinnerLoraBaudRate, spinnerLoraTransMode, spinnerLoraChannelRSSI,
            spinnerLoraParity, spinnerLoraWoreCycle, spinnerLoraLBT, spinnerLoraAirRate,
            spinnerLoraPower, spinnerLoraPacketRSSI, spinnerLoraPacketSize;
    private String[] itemsLoraBaudRate, itemsLoraTransMode, itemsLoraChannelRSSI,
            itemsLoraParity, itemsLoraWoreCycle, itemsLoraLBT, itemsLoraAirRate, itemsLoraPower,
            itemsLoraPacketRSSI, itemsLoraPacketSize;

    private StringBit[] strBLoraBaudRateVal, strBLoraTransModeVal, strBLoraChannelRSSIVal,
            strBLoraParityVal, strBLoraWoreCycleVal, strBLoraLBTVal, strBLoraAirRateVal,
            strBLoraPowerVal, strBLoraPacketRSSIVal, strBLoraPacketSizeVal;

    private TextView textLoraModuleValue;

    private EditText textLoraAddressValue, textLoraChannelValue;

    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    ConfigLora.ModuleInfo mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_lora);

        btRetrieveConfig = (Button) findViewById(R.id.butLoraRetrieveConfig);
        btSaveConfig = (Button) findViewById(R.id.butLoraSaveConfig);
        spinnerLoraBaudRate = (Spinner) findViewById(R.id.spinnerLoraBaudRate);
        spinnerLoraTransMode = (Spinner) findViewById(R.id.spinnerLoraTransMode);
        spinnerLoraChannelRSSI = (Spinner) findViewById(R.id.spinnerLoraChannelRSSI);
        spinnerLoraParity = (Spinner) findViewById(R.id.spinnerLoraParity);
        spinnerLoraWoreCycle = (Spinner) findViewById(R.id.spinnerLoraWoreCycle);
        spinnerLoraLBT = (Spinner) findViewById(R.id.spinnerLoraLBT);
        spinnerLoraAirRate = (Spinner) findViewById(R.id.spinnerLoraAirRate);
        spinnerLoraPower = (Spinner) findViewById(R.id.spinnerLoraPower);
        spinnerLoraPacketRSSI = (Spinner) findViewById(R.id.spinnerLoraPacketRSSI);
        spinnerLoraPacketSize = (Spinner) findViewById(R.id.spinnerLoraPacketSize);

        textLoraModuleValue = (TextView) findViewById(R.id.textLoraModuleValue);

        textLoraAddressValue = (EditText) findViewById(R.id.textLoraAddressValue);
        textLoraChannelValue = (EditText) findViewById(R.id.textLoraChannelValue);

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

        // Trans Mode
        itemsLoraTransMode = new String[]{
                "Normal",
                "Fixed"
        };
        strBLoraTransModeVal = new StringBit[]{
                new StringBit("Normal", 0b0),
                new StringBit("Fixed", 0b1)
        };
        ArrayAdapter<String> adapterLoraTransModeVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraTransMode);
        adapterLoraTransModeVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraTransMode.setAdapter(adapterLoraTransModeVal);

        // Channel RSSI
        itemsLoraChannelRSSI = new String[]{
                "Disable",
                "Enable"
        };
        strBLoraChannelRSSIVal = new StringBit[]{
                new StringBit("Disable", 0b0),
                new StringBit("Enable", 0b1)
        };
        ArrayAdapter<String> adapterLoraChannelRSSIVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraChannelRSSI);
        adapterLoraChannelRSSIVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraChannelRSSI.setAdapter(adapterLoraChannelRSSIVal);

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

        // Wore Cycle
        itemsLoraWoreCycle = new String[]{
                "500ms",
                "1000ms",
                "1500ms",
                "2000ms",
                "2500ms",
                "3000ms",
                "3500ms",
                "4000ms"
        };
        strBLoraWoreCycleVal = new StringBit[]{
                new StringBit("500ms", 0b000),
                new StringBit("1000ms", 0b001),
                new StringBit("1500ms", 0b010),
                new StringBit("2000ms", 0b011),
                new StringBit("2500ms", 0b100),
                new StringBit("3000ms", 0b101),
                new StringBit("3500ms", 0b110),
                new StringBit("4000ms", 0b111)
        };
        ArrayAdapter<String> adapterLoraWoreCycleVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraWoreCycle);
        adapterLoraWoreCycleVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraWoreCycle.setAdapter(adapterLoraWoreCycleVal);

        //LBT
        itemsLoraLBT = new String[]{
                "Disable",
                "Enable"
        };
        strBLoraLBTVal = new StringBit[]{
                new StringBit("Disable", 0b0),
                new StringBit("Enable", 0b1)
        };
        ArrayAdapter<String> adapterLoraLBTVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraLBT);
        adapterLoraLBTVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraLBT.setAdapter(adapterLoraLBTVal);

        // AirRate
        itemsLoraAirRate = new String[]{
                "2.4Kbps",
                "4.8Kbps",
                "9.6Kbps",
                "19.2Kbps",
                "38.4Kbps",
                "65.2Kbps"
        };
        strBLoraAirRateVal = new StringBit[]{
                new StringBit("2.4Kbps", 0b000),
                new StringBit("2.4Kbps", 0b001),
                new StringBit("2.4Kbps", 0b010),
                new StringBit("4.8Kbps", 0b011),
                new StringBit("9.6Kbps", 0b100),
                new StringBit("19.2Kbps", 0b101),
                new StringBit("38.4Kbps", 0b110),
                new StringBit("65.2Kbps", 0b111)
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

        // PacketRSSI
        itemsLoraPacketRSSI = new String[]{
                "Disable",
                "Enable"
        };
        strBLoraPacketRSSIVal = new StringBit[]{
                new StringBit("Disable", 0b0),
                new StringBit("Enable", 0b1)
        };
        ArrayAdapter<String> adapterPacketRSSIVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraPacketRSSI);
        adapterPacketRSSIVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraPacketRSSI.setAdapter(adapterPacketRSSIVal);

        // Packet Size
        itemsLoraPacketSize = new String[]{
                "200 Bytes",
                "128 Bytes",
                "64 Bytes",
                "32 Bytes"
        };
        strBLoraPacketSizeVal = new StringBit[]{
                new StringBit("200 Bytes", 0b00),
                new StringBit("128 Bytes", 0b01),
                new StringBit("64 Bytes", 0b10),
                new StringBit("32 Bytes", 0b11)
        };
        ArrayAdapter<String> adapterPacketSizeVal = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsLoraPacketSize);
        adapterPacketSizeVal.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLoraPacketSize.setAdapter(adapterPacketSizeVal);

        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.open();
        mInfo = new ConfigLora.ModuleInfo(mPhysicaloid);

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
        spinnerLoraTransMode.setEnabled(true);
        spinnerLoraChannelRSSI.setEnabled(true);
        spinnerLoraParity.setEnabled(true);
        spinnerLoraWoreCycle.setEnabled(true);
        spinnerLoraLBT.setEnabled(true);
        spinnerLoraAirRate.setEnabled(true);
        spinnerLoraPower.setEnabled(true);
        spinnerLoraPacketRSSI.setEnabled(true);
        spinnerLoraPacketSize.setEnabled(true);
    }

    public void DisableUI() {
        spinnerLoraBaudRate.setEnabled(false);
        spinnerLoraTransMode.setEnabled(false);
        spinnerLoraChannelRSSI.setEnabled(false);
        spinnerLoraParity.setEnabled(false);
        spinnerLoraWoreCycle.setEnabled(false);
        spinnerLoraLBT.setEnabled(false);
        spinnerLoraAirRate.setEnabled(false);
        spinnerLoraPower.setEnabled(false);
        spinnerLoraPacketRSSI.setEnabled(false);
        spinnerLoraPacketSize.setEnabled(false);
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
        //close();
        finish();
    }

    public void onClickRetrieveConfig(View v) {
        // go to AT mode
        Log.d("Config Lora", "retrieving config");
        new ConfigLora.connectRetrieveAsyc().execute();
    }

    public void onClickSaveConfig(View v) {
        new ConfigLora.connectSaveAsyc().execute();
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
            builder = new AlertDialog.Builder(ConfigLora.this);
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
                byte[] cmd = {(byte) 0xC1, 0x00, 0x08};
                value = mInfo.runCommand(cmd);
                if (value[0] == (byte) 0xC1) {
                    Log.d("Lora config", "We have a good config return");
                }
                int module_address = (value[3] << 8) | value[4];
                Log.d("Lora Config", "module_address:" + module_address);
                setLoraAddressValue(module_address + "");

                //first 3 bits are the baud rate
                int baud_rate = (value[5] & 0xFF) >> 5;
                Log.d("Lora Config", "baud_rate:" + baud_rate);
                String sBaud = searchStringFromBit(strBLoraBaudRateVal, baud_rate);
                int iBaud = arrayIndex(itemsLoraBaudRate, sBaud);
                setLoraBaudRate(iBaud);

                //next 2 bits are the parity
                int parity = ((value[5] & 0xFF) & 0b00011000) >> 3;
                Log.d("Lora Config", "parity:" + parity);
                String sParity = searchStringFromBit(strBLoraParityVal, parity);
                int iParity = arrayIndex(itemsLoraParity, sParity);
                setLoraParity(iParity);

                //next 3 bits are the air data rate
                int air_data_rate = (value[5] & 0xFF) & 0b00000111;
                Log.d("Lora Config", "air_data_rate:" + air_data_rate);
                String sAirRate = searchStringFromBit(strBLoraAirRateVal, air_data_rate);
                int iAirRate = arrayIndex(itemsLoraAirRate, sAirRate);
                setLoraAirRate(iAirRate);

                // next 2 are the packet
                int packet = (value[6] & 0xFF) >> 6;
                Log.d("Lora Config", "packet:" + packet);
                String sPacketSize = searchStringFromBit(strBLoraPacketSizeVal, packet);
                int iPacketSize = arrayIndex(itemsLoraPacketSize, sPacketSize);
                setLoraPacketSize(iPacketSize);

                // next 1 RSSI
                int RSSI_ambient_noise = ((value[6] & 0xFF) & 0x00100000) >> 5;
                Log.d("Lora Config", "RSSI_ambient_noise:" + RSSI_ambient_noise);
                String sChannelRSSI = searchStringFromBit(strBLoraChannelRSSIVal, RSSI_ambient_noise);
                int iChannelRSSI = arrayIndex(itemsLoraChannelRSSI, sChannelRSSI);
                setLoraChannelRSSI(iChannelRSSI);

                // next 3 are reserved so let's live them
                //next 2 are the transmitting power
                int transmitting_power = (value[6] & 0xFF) & 0b00000011;
                Log.d("Lora Config", "transmitting_power:" + transmitting_power);
                String sPower = searchStringFromBit(strBLoraPowerVal, transmitting_power);
                int iPower = arrayIndex(itemsLoraPower, sPower);
                setLoraPower(iPower);

                // value[7] frequency (0-83)
                int channel = value[7] & 0xFF;
                Log.d("Lora Config", "channel:" + channel);
                setLoraChannelValue(channel + "");
                // next 1 is RSSI byte
                int RSSI_byte = ((value[8] & 0xFF) & 0b10000000) >> 7;
                Log.d("Lora Config", "RSSI_byte:" + RSSI_byte);
                String sRSSI_byte = searchStringFromBit(strBLoraPacketRSSIVal, RSSI_byte);
                int iRSSI_byte = arrayIndex(itemsLoraPacketRSSI, sRSSI_byte);
                setLoraPacketRSSI(iRSSI_byte);

                // next 1 is transmission method
                int transmission_method = ((value[8] & 0xFF) & 0b01000000) >> 6;
                Log.d("Lora Config", "transmission_method:" + transmission_method);
                String sTransMode = searchStringFromBit(strBLoraTransModeVal, transmission_method);
                int iTransMode = arrayIndex(itemsLoraTransMode, sTransMode);
                setLoraTransMode(iTransMode);

                // next 1  is reserved
                // next 1 is LBT
                int LBT = ((value[8] & 0xFF) & 0b00010000) >> 4;
                Log.d("Lora Config", "LBT:" + LBT);
                String sLBT = searchStringFromBit(strBLoraLBTVal, LBT);
                int iLBT = arrayIndex(itemsLoraLBT, sLBT);
                setLoraLBT(iLBT);

                // next 1  is reserved
                // next 1 is WOR Cycle
                int wor_cycle = (value[8] & 0xFF) & 0b00000111;
                Log.d("Lora Config", "wor_cycle:" + wor_cycle);
                String sWorCycle = searchStringFromBit(strBLoraWoreCycleVal, wor_cycle);
                int iWorCycle = arrayIndex(itemsLoraWoreCycle, sWorCycle);
                setLoraWoreCycle(iWorCycle);
                // ignore value[7] and value[8]
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

        String sBaudRate, sTransMode, sChannelRSSI, sParity, sWoreCycle, sLBT,
                sAiRate, sPower, sPacketRSSI, sPacketSize, sChannel, sAddress;

        String cancelMsg = "";

        boolean cancelled = false;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigLora.this);
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
            sTransMode = spinnerLoraTransMode.getSelectedItem().toString();
            sChannelRSSI = spinnerLoraChannelRSSI.getSelectedItem().toString();
            sParity = spinnerLoraParity.getSelectedItem().toString();
            sWoreCycle = spinnerLoraWoreCycle.getSelectedItem().toString();
            sLBT = spinnerLoraLBT.getSelectedItem().toString();
            sAiRate = spinnerLoraAirRate.getSelectedItem().toString();
            sPower = spinnerLoraPower.getSelectedItem().toString();
            sPacketRSSI = spinnerLoraPacketRSSI.getSelectedItem().toString();
            sPacketSize = spinnerLoraPacketSize.getSelectedItem().toString();
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
            // change address
            if (!cancelled) {
                //first split the address in 2 bytes
                //sAddress
                byte addh = 0;
                byte addl = 3;
                dialogAppend(getString(R.string.lora_module_updating_address_msg));
                byte cmd[] = {(byte) 0xC2, 0x00, 0x02, (byte) addh, (byte) addl};
                byte[] value = mInfo.runCommand(cmd);
                if (value[0] != (byte) 0xC1)
                    error++;
            }
            //change Baud Rate, parity and air rate
            if (!cancelled) {
                int c;
                //serial speed, parity and air rate
                dialogAppend(getString(R.string.lora_module_updating_msg2));

                int iBaudeRate = searchBitFromString(strBLoraBaudRateVal, sBaudRate);
                int iParity = searchBitFromString(strBLoraParityVal, sParity);
                int iAiRate = searchBitFromString(strBLoraAirRateVal, sAiRate);
                c = iBaudeRate << 5 | iParity << 3 | iAiRate;
                byte cmd[] = {(byte) 0xC2, 0x02, 0x01, (byte) c};
                byte[] value = mInfo.runCommand(cmd);
                if (value[0] != (byte) 0xC1)
                    error++;
            }
            //updating packet , RSSI noise and transmitting power
            if (!cancelled) {
                dialogAppend(getString(R.string.lora_module_updating_msg3));
                //sPacketSize
                int iPacketSize = searchBitFromString(strBLoraPacketSizeVal, sPacketSize);
                //sChannelRSSI
                int iChannelRSSI = searchBitFromString(strBLoraPacketRSSIVal, sChannelRSSI);
                //sPower
                int iPower = searchBitFromString(strBLoraPowerVal, sPower);
                int c;
                c = iPacketSize << 6 | iChannelRSSI << 5 | iPower;
                byte cmd[] = {(byte) 0xC2, 0x03, 0x01, (byte) c};
                byte[] value = mInfo.runCommand(cmd);
                if (value[0] != (byte) 0xC1)
                    error++;
            }
            //updating channel 0-83
            if (!cancelled) {
                dialogAppend(getString(R.string.lora_module_updating_msg4));
                Log.d("Lora Config:", sChannel);
                int channel = Integer.valueOf(sChannel);
                byte cmd[] = {(byte) 0xC2, 0x04, 0x01, (byte) channel};
                byte[] value = mInfo.runCommand(cmd);
                if (value[0] != (byte) 0xC1)
                    error++;
            }

            //updating RSSI Byte, trans Mode, LBT and WOR Cycle
            if (!cancelled) {
                dialogAppend(getString(R.string.lora_module_updating_msg5));
                //sPacketRSSI
                int iPacketRSSI = searchBitFromString(strBLoraPacketRSSIVal, sPacketRSSI);
                //sTransMode
                int iTransMode = searchBitFromString(strBLoraTransModeVal, sTransMode);
                //sLBT
                int iLBT = searchBitFromString(strBLoraLBTVal, sLBT);
                // sWoreCycle
                int iWoreCycle = searchBitFromString(strBLoraWoreCycleVal, sWoreCycle);
                int c;
                c = iPacketRSSI << 7 | iTransMode << 6 | iLBT << 4 | iWoreCycle;
                byte cmd[] = {(byte) 0xC2, 0x05, 0x01, (byte) c};
                byte[] value = mInfo.runCommand(cmd);
                if (value[0] != (byte) 0xC1)
                    error++;
            }

            //updating Key
            if (!cancelled) {
                dialogAppend(getString(R.string.lora_module_updating_msg6));
            }

            //Exit AT mode
            //value = mInfo.runCommand("ATO");
            //Log.d("Flight win", "ATO" + value);

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
            builder = new AlertDialog.Builder(ConfigLora.this);
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
            builder = new AlertDialog.Builder(ConfigLora.this);
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

    private void setLoraTransMode(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraTransMode.setSelection(findex);
            }
        });
    }

    private void setLoraChannelRSSI(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraChannelRSSI.setSelection(findex);
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

    private void setLoraWoreCycle(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraWoreCycle.setSelection(findex);
            }
        });
    }

    private void setLoraLBT(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraLBT.setSelection(findex);
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

    private void setLoraPacketRSSI(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraPacketRSSI.setSelection(findex);
            }
        });
    }

    private void setLoraPacketSize(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                spinnerLoraPacketSize.setSelection(findex);
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

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }

        //open application settings screen
        if (id == R.id.action_settings) {
            Intent i = new Intent(ConfigLora.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open Lora config help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(ConfigLora.this, HelpActivity.class);
            i.putExtra("help_file", "help_configLora");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(ConfigLora.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}