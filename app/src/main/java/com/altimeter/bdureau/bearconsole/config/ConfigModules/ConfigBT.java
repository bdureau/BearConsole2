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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.AppTabConfigActivity;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

/**
 * @description: This allows the configuration of bluetooth modules from the phone using a
 * ttl cable.
 * This is not perfect code but should work. Feel free to re-use it for your own project.
 * Make sure that you report any bugs or suggestions so that it can be improved
 * @author: boris.dureau@neuf.fr
 **/
public class ConfigBT extends AppCompatActivity {

    Physicaloid mPhysicaloid;
    private static final String TAG = "ConfigBT";

    private String[] itemsBaudRate, itemsBaudRateShort, itemsModules;
    private Spinner dropdownBaudRate, dropdownModules;
    private TextView txtVersion;
    private TextView txtModuleName, txtPin;
    TextView tvRead;

    private Button btRetrieveConfig, btSaveConfig;
    ModuleInfo mInfo;
    ConsoleApplication myBT;
    int currentBaudRate = 38400;
    String cmdTer = "";

    private AlertDialog.Builder builder = null;
    private AlertDialog alert;

    private UartConfig uartConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_config_bt);

        txtVersion = (TextView) findViewById(R.id.textversionentry);
        txtModuleName = (TextView) findViewById(R.id.textNameEntry);
        txtPin = (TextView) findViewById(R.id.textPinEntry);
        tvRead = (TextView) findViewById(R.id.tvRead);

        btRetrieveConfig = (Button) findViewById(R.id.butRetrieveConfig);
        btSaveConfig = (Button) findViewById(R.id.butSaveConfig);
        //baud rate
        dropdownBaudRate = (Spinner) findViewById(R.id.spinnerBTBaudRate);
        itemsBaudRate = new String[]{
                "1200",
                "2400",
                "4800",
                "9600",
                "19200",
                "38400",
                "57600",
                "115200",
                "230400",
                "460800",
                "921600",
                "1382400"};

        itemsBaudRateShort = new String[]{
                "1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "7",
                "8",
                "9",
                "A",
                "B",
                "C"};

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsBaudRate);

        adapterBaudRate.setDropDownViewResource(android.R.layout.simple_spinner_item);
        dropdownBaudRate.setAdapter(adapterBaudRate);

        dropdownModules = (Spinner) findViewById(R.id.spinnerModules);
        itemsModules = new String[]{
                "HC-05",
                "HC-06",
                "JDY-30",
                "JDY-31",
                "SPP",
                "AT-09",
                "Unknown",
        };
        ArrayAdapter<String> adapterModules = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemsModules);

        adapterModules.setDropDownViewResource(android.R.layout.simple_spinner_item);
        dropdownModules.setAdapter(adapterModules);

        mPhysicaloid = new Physicaloid(this);
        uartConfig = new UartConfig(38400, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);


        mPhysicaloid.open();
        mPhysicaloid.setConfig(uartConfig);

        mInfo = new ModuleInfo(mPhysicaloid);

        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);
        builder = new AlertDialog.Builder(ConfigBT.this);
        //Display info message
        builder.setMessage(R.string.bt_info_msg)
                .setTitle(R.string.bt_info_title)
                .setCancelable(false)
                .setPositiveButton(R.string.bt_info_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        alert = builder.create();
        alert.show();
    }

    public void EnableUI() {
        dropdownBaudRate.setEnabled(true);
        txtPin.setEnabled(true);
        txtModuleName.setEnabled(true);
    }

    public void DisableUI() {
        dropdownBaudRate.setEnabled(false);
        txtPin.setEnabled(false);
        txtModuleName.setEnabled(false);
    }

    //index in an array
    public int arrayIndex(String stringArray[], String pattern) {

        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].equals(pattern)) {
            Log.d(TAG, pattern + ":" + i);
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
        tvRead.setText("");
        // go to AT mode
        Log.d(TAG, "retrieving config");

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
        boolean cancelled = false;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigBT.this);
            //Recover firmware...
            builder.setMessage(getResources().getString(R.string.loading_bt_config_msg))
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
                if (dropdownModules.getSelectedItem().equals("HC-05") ||
                        dropdownModules.getSelectedItem().equals("HC-06") ||
                        dropdownModules.getSelectedItem().equals("unknown")) {
                    dialogAppend(getResources().getString(R.string.m3DR_run_command) + " AT");

                    value = mInfo.runCommand("AT" + cmdTer);
                    Log.d(TAG, "AT:" + value);
                    tvAppend(tvRead, "AT\n");
                    String AT[] = value.split("\n");
                    if (AT.length > 0) {
                        tvAppend(tvRead, AT[0] + "\n");
                        if (AT[0].trim().equals(""))
                            error++;
                    } else
                        error++;
                }
            }
            //AT VERSION
            if (!cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " AT+VERSION");
                value = mInfo.runCommand("AT+VERSION" + cmdTer);
                Log.d(TAG, "AT+VERSION:" + value);

                String ATVERSION[] = value.split("\n");
                if (ATVERSION.length > 0) {
                    setVersion(ATVERSION[0]);
                    tvAppend(tvRead, ATVERSION[0] + "\n");
                    if (ATVERSION[0].trim().equals(""))
                        error++;
                } else
                    error++;
            }


            if (cancelled) {
                dialogAppend(getResources().getString(R.string.m3DR_cancel_retrieve));
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
        String baudID = "";
        String baudRate, Name, PIN;
        String cancelMsg = "";

        boolean cancelled = false;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigBT.this);
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

            baudRate = dropdownBaudRate.getSelectedItem().toString();
            baudID = itemsBaudRateShort[(int) dropdownBaudRate.getSelectedItemId()];
            Name = txtModuleName.getText().toString();
            PIN = txtPin.getText().toString();

            if (Name.trim().equals("")) {
                cancelMsg = getString(R.string.module_name_cannot_be_empty_msg);
                cancelled = true;
            }
            if (PIN.trim().equals("")) {
                cancelMsg = cancelMsg + getString(R.string.module_pin_cannot_be_empty_msg);
                cancelled = true;
            }
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {


            //change PIN
            if (!cancelled) {
                //serial speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " AT+PIN");
                value = mInfo.runCommand("AT+PIN" + PIN.trim() + cmdTer);
                String ATPIN[] = value.split("\n");
                Log.d(TAG, value);
                Log.d(TAG, "AT+PIN=" + PIN);
                if (ATPIN.length > 0) {
                    tvAppend(tvRead, value + "\n");
                    Log.d(TAG, value);
                    if (!ATPIN[0].substring(0, 2).equals("OK"))
                        error++;
                } else
                    error++;
            }

            //change Name
            if (!cancelled) {
                //serial speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " AT+NAME");
                value = mInfo.runCommand("AT+NAME" + Name.trim() + cmdTer);
                String ATNAME[] = value.split("\n");
                Log.d(TAG, "AT+NAME" + Name);
                if (ATNAME.length > 0) {
                    tvAppend(tvRead, value + "\n");
                    Log.d(TAG, ATNAME[0]);
                    if (!ATNAME[0].substring(0, 2).equals("OK"))
                        error++;
                } else
                    error++;
            }

            //change baud rate
            if (!cancelled) {
                //serial speed
                dialogAppend(getResources().getString(R.string.m3DR_run_command) + " AT+BAUD");
                value = mInfo.runCommand("AT+BAUD" + baudID + cmdTer);
                String ATBAUD[] = value.split("\n");
                Log.d("Flight win", "AT+BAUD" + baudID);
                Log.d("Flight win val", value);
                if (ATBAUD.length > 0) {
                    tvAppend(tvRead, value + "\n");
                    if (!ATBAUD[0].substring(0, 2).equals("OK"))
                        error++;
                } else
                    error++;
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
            builder = new AlertDialog.Builder(ConfigBT.this);
            //
            builder.setMessage(R.string.attempting_connect_msg)
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
                        Log.d(TAG, "Success:" + rate);
                        dialogAppend(getResources().getString(R.string.m3DR_connect_ok) + rate);
                        Log.d(TAG, "arrayIndex:" + arrayIndex(itemsBaudRate, rate));
                        setBaudRate(arrayIndex(itemsBaudRate, rate));
                        break;
                    } else {
                        Log.d(TAG, "Failed:" + rate);

                    }
                }
            } else {
                if (!mPhysicaloid.isOpened()) {
                    if (mPhysicaloid.open()) {
                        mPhysicaloid.setBaudrate(currentBaudRate);
                    }
                }
                if (mInfo.ATMode()) {
                    setBaudRate(arrayIndex(itemsBaudRate, "" + currentBaudRate));
                } else {
                    Log.d(TAG, "Failed:" + currentBaudRate);
                    currentBaudRate = 0;
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();

            if (currentBaudRate > 0) {
                new getAllConfigAsyc().execute();

            } else {
                msg(getResources().getString(R.string.m3DR_failed_con));

            }

        }
    }

    private class connectSaveAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(ConfigBT.this);
            //.
            builder.setMessage(getResources().getString(R.string.attempting_connect_bt_msg))
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
                        Log.d(TAG, "Success:" + rate);
                        dialogAppend(getResources().getString(R.string.m3DR_connect_ok) + rate);
                        break;
                    } else {
                        Log.d(TAG, "Failed:" + rate);

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
                    Log.d(TAG, "Failed:" + currentBaudRate);
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
            if (currentBaudRate > 0) {

                new saveAllConfigAsyc().execute();

            } else {
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
            byte[] cmd0;
            if (dropdownModules.getSelectedItem().equals("HC-05") ||
                    dropdownModules.getSelectedItem().equals("HC-06")
            ) {
                Log.d(TAG, "AT\n");
                cmd0 = "AT".getBytes();
                cmdTer = "";
            } else if (dropdownModules.getSelectedItem().equals("JDY-30") ||
                    dropdownModules.getSelectedItem().equals("JDY-31")) {
                Log.d(TAG, "AT+VERSION\n");
                cmd0 = "AT+VERSION\r\n".getBytes();
                cmdTer = "\r\n";
            } else if (dropdownModules.getSelectedItem().equals("Unknown")) {
                Log.d(TAG, "AT\n");
                cmd0 = "AT\r\n".getBytes();
                cmdTer = "\r\n";
            } else if (dropdownModules.getSelectedItem().equals("SPP")) {
                Log.d(TAG, "AT\n");
                cmd0 = "AT\r\n".getBytes();
                cmdTer = "\r\n";
            } else {
                Log.d(TAG, "AT\n");
                cmd0 = "AT\r".getBytes();
            }


            mPhysicaloid.write(cmd0, cmd0.length);


            byte buf[] = new byte[200];
            int retval = 0;

            retval = recv(buf, 200, 2000);

            Log.d(TAG, "retval:" + retval);
            if (retval > 0) {
                String str = null;

                str = new String(buf);
                Log.d(TAG, str);
                String Ret[] = str.split("\n");

                if (Ret.length > 0) {
                    Log.d(TAG, "RET0:"+ Ret[0] + "\n");
                    at = true;
                    Log.d(TAG, "connected!!!");
                }
            }

            return at;
        }

        public String runCommand(String command) {
            String value = "";
            //command = command + "\n\r";
            drain();
            drain();
            byte[] cmd0 = command.getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);


            byte buf[] = new byte[200];
            int retval = 0;

            retval = recv(buf, 200, 1000);

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

    private void setVersion(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                txtVersion.setText(ftext);
            }
        });
    }

    private void setBaudRate(int index) {
        final int findex = index;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                dropdownBaudRate.setSelection(findex);
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
            Intent i = new Intent(ConfigBT.this, AppTabConfigActivity.class);
            startActivity(i);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(ConfigBT.this, HelpActivity.class);
            i.putExtra("help_file", "help_configBT");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(ConfigBT.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
