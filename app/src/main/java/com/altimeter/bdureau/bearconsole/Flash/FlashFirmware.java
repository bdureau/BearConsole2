package com.altimeter.bdureau.bearconsole.Flash;

/**
 * @description: This is used to flash the altimeter firmware from the Android device using an OTG cable
 * so that the store Android application is compatible with altimeter. This works with the
 * ATMega328 based altimeters as well as the STM32 based altimeters
 * @author: boris.dureau@neuf.fr
 **/

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.ConfigBT;
import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static com.physicaloid.misc.Misc.toHexStr;


public class FlashFirmware extends AppCompatActivity {
    Physicaloid mPhysicaloid;

    boolean recorverFirmware = false;
    Boards mSelectedBoard;
    Button btFlash;

    public Spinner spinnerFirmware;
    public ImageView imageAlti;
    TextView tvRead;
    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private ArrayList<Boards> mBoardList;
    private UartConfig uartConfig;

    private static final String ASSET_FILE_NAME_ALTIMULTIV2 = "firmwares/2022-08-25-V1_27.altimultiV2.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTI = "firmwares/2022-08-25-V1_27.altimulti.hex";
    private static final String ASSET_FILE_NAME_ALTISERVO = "firmwares/2022-06-25-AltiServoV1_5.hex";
    private static final String ASSET_FILE_NAME_ALTIDUO = "firmwares/2022-06-25-V1_8.AltiDuo.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTISTM32 = "firmwares/2022-08-26-V1_27.altimultiSTM32.bin";
    private static final String ASSET_FILE_NAME_ALTIGPS = "firmwares/2023-01-07-RocketGPSLoggerV1.5.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE2 = "firmwares/ESP32/RocketFlightLoggerV1_27.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE3 = "firmwares/ESP32/RocketFlightLoggerV1_27.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE4 = "firmwares/ESP32/RocketFlightLoggerV1_27.ino.partitions.bin";

    private static final String ASSET_FILE_RESET_ALTIDUO = "recover_firmwares/ResetAltiConfigAltiDuo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTIMULTI = "recover_firmwares/ResetAltiConfigAltimulti.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISERVO = "recover_firmwares/ResetAltiConfigAltiServo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISTM32 = "recover_firmwares/ResetAltiConfigAltimultiSTM32.ino.bin";

    private static final String ASSET_FILE_RESET_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE2 = "firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.bootloader.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE3 = "firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE4 = "firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.partitions.bin";

    private String[] itemsBaudRate;
    private String[] itemsFirmwares;
    private Spinner dropdownBaudRate;

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_firmware);

        spinnerFirmware = (Spinner) findViewById(R.id.spinnerFirmware);
        itemsFirmwares = new String[]{
                "AltiMulti",
                "AltiMultiV2",
                "AltiServo",
                "AltiDuo",
                "AltiMultiSTM32",
                "AltiGPS",
                "AltiESP32"
                };

        ArrayAdapter<String> adapterFirmware = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsFirmwares);
        spinnerFirmware.setAdapter(adapterFirmware);
        spinnerFirmware.setSelection(0);



        btFlash = (Button) findViewById(R.id.btFlash);
        tvRead = (TextView) findViewById(R.id.tvRead);
        imageAlti = (ImageView) findViewById(R.id.imageAlti);


        mPhysicaloid = new Physicaloid(this);
        mBoardList = new ArrayList<Boards>();
        for (Boards board : Boards.values()) {
            if (board.support > 0) {
                mBoardList.add(board);
            }
        }

        mSelectedBoard = mBoardList.get(0);
        uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);

        btFlash.setEnabled(true);
        if (mPhysicaloid.open()) {
            mPhysicaloid.setConfig(uartConfig);

        } else {
            //cannot open
            Toast.makeText(this, getResources().getString(R.string.msg13), Toast.LENGTH_LONG).show();
            //btFlash.setEnabled(false);
        }


        //baud rate
        dropdownBaudRate = (Spinner) findViewById(R.id.spinnerBaud);
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


        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        dropdownBaudRate.setSelection(10);

        spinnerFirmware.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiv1_small, getApplicationContext().getTheme()));

                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiv2_small, getApplicationContext().getTheme()));

                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altiservo_small, getApplicationContext().getTheme()));

                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altiduo_small, getApplicationContext().getTheme()));

                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32") ||
                        itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultistm32_small, getApplicationContext().getTheme()));

                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_small, getApplicationContext().getTheme()));

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        builder = new AlertDialog.Builder(this);
        //Running Saving commands
        builder.setMessage(R.string.flash_firmware_msg)
                .setTitle(R.string.flash_firmware_title)
                .setCancelable(false)
                .setPositiveButton(R.string.flash_firmware_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        alert = builder.create();
        alert.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

    public void onClickDismiss(View v) {
        close();
        finish();
    }

    public void onClickRecover(View v) {
        String recoverFileName;
        recoverFileName = ASSET_FILE_RESET_ALTIMULTI;

        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti")){
            recoverFileName = ASSET_FILE_RESET_ALTIMULTI;
        }
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2")) {
            recoverFileName = ASSET_FILE_RESET_ALTIMULTI;
        }
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo")) {
            recoverFileName = ASSET_FILE_RESET_ALTISERVO;
        }
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo")) {
            recoverFileName = ASSET_FILE_RESET_ALTIDUO;
        }
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")){
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;
        }

        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;

        tvRead.setText("");
        tvRead.setText(getResources().getString(R.string.after_complete_upload));
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo")) {
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Recover firmware...
                builder.setMessage(getResources().getString(R.string.msg18))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {

                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();
                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(recoverFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS") ||
                    itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")) {
            recorverFirmware = true;
            new UploadSTM32Asyc().execute();
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiESP32")) {
            recorverFirmware = true;
            new UploadESP32Asyc().execute();
        }
    }

    public void onClickDetect(View v) {
        new DetectAsyc().execute();
    }

    public void onClickFirmwareInfo(View v) {
        tvRead.setText("The following firmwares are available:");
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIMULTIV2);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIMULTI);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTISERVO);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIDUO);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIMULTISTM32);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIGPS);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_ALTIESP32_FILE2);
        tvRead.append("\n");
    }
    public void onClickFlash(View v) {
        String firmwareFileName ;

        firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;

        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti"))
            firmwareFileName = ASSET_FILE_RESET_ALTIMULTI;
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTIV2;
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo"))
            firmwareFileName = ASSET_FILE_RESET_ALTISERVO;
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo"))
            firmwareFileName = ASSET_FILE_RESET_ALTIDUO;
        if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32"))
            firmwareFileName = ASSET_FILE_RESET_ALTISTM32;

        tvRead.setText("");

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo")) {
            tvRead.setText("Loading firmware:"+ firmwareFileName);
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Flashing firmware...
                builder.setMessage(getResources().getString(R.string.msg10))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();

                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(firmwareFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS") ||
                    itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")) {
            tvRead.setText("Loading firmware:"+ firmwareFileName);
            recorverFirmware = false;
            new UploadSTM32Asyc().execute();
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiESP32")) {
            tvRead.setText("Loading ESP32 firmware\n");
            recorverFirmware = false;
            new UploadESP32Asyc().execute();
        }


    }

    private class DetectAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {
        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Attempting to detect firmware...
            builder.setMessage(getResources().getString(R.string.detect_firmware))
                    .setTitle(getResources().getString(R.string.msg_detect_firmware))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            String version="";

            FirmwareInfo firm = new FirmwareInfo(mPhysicaloid);
            firm.open(38400);
            version =firm.getFirmwarVersion();

            tvAppend(tvRead, getString(R.string.firmware_version_not_detected)+ version +"\n");

            if(version.equals("AltiMulti")) {
                //setRadioButton (rbAltiMulti,true);
                spinnerFirmware.setSelection(0);
            }
            if(version.equals("AltiMultiV2")) {
                //setRadioButton (rbAltiMultiV2,true);
                spinnerFirmware.setSelection(1);
            }
            if(version.equals("AltiServo")) {
                //setRadioButton (rbAltiServo,true);
                spinnerFirmware.setSelection(2);
            }
            if(version.equals("AltiDuo")) {
                //setRadioButton (rbAltiDuo,true);
                spinnerFirmware.setSelection(3);
            }
            if(version.equals("AltiMultiSTM32")) {
                //setRadioButton (rbAltiMultiSTM32,true);
                spinnerFirmware.setSelection(4);
            }
            if(version.equals("AltiGPS")) {
                //setRadioButton (rbAltiGPS,true);
                spinnerFirmware.setSelection(5);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }


    private class UploadSTM32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {
        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!recorverFirmware) {
                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32"))
                    uploadSTM32(ASSET_FILE_NAME_ALTIMULTISTM32, mUploadSTM32Callback);
                if(itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
                    uploadSTM32(ASSET_FILE_NAME_ALTIGPS, mUploadSTM32Callback);
            } else {
                uploadSTM32(ASSET_FILE_RESET_ALTISTM32, mUploadSTM32Callback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }

    public void uploadSTM32(String fileName, UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream is = null;

        try {
            is = getAssets().open(fileName);
        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }

        dialogAppend("Starting ...");
        CommandInterface cmd;

        cmd = new CommandInterface(UpCallback, mPhysicaloid);

        cmd.open(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
        int ret = cmd.initChip();
        if (ret == 1)
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend("Chip has not been initiated:" + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            bootversion = cmd.cmdGet();
            //dialogAppend("bootversion:"+ bootversion);
            tvAppend(tvRead, " bootversion:" + bootversion + "\n");
            if (bootversion < 20 || bootversion >= 100) {
                tvAppend(tvRead, " bootversion not good:" + bootversion + "\n");
                failed = true;
            }
        }

        if (!failed) {
            byte chip_id[]; // = new byte [4];
            chip_id = cmd.cmdGetID();
            tvAppend(tvRead, " chip id:" + toHexStr(chip_id, 2) + "\n");
        }

        if (!failed) {
            if (bootversion < 0x30) {
                tvAppend(tvRead, "Erase 1\n");
                cmd.cmdEraseMemory();
            } else {
                tvAppend(tvRead, "Erase 2\n");
                cmd.cmdExtendedEraseMemory();
            }
        }
        if (!failed) {
            cmd.drain();
            tvAppend(tvRead, "writeMemory" + "\n");
            ret = cmd.writeMemory(0x8000000, is);

            tvAppend(tvRead, "writeMemory finish" + "\n\n\n\n");
            if (ret == 1) {
                tvAppend(tvRead, "writeMemory success" + "\n\n\n\n");
            }
        }
        if (!failed) {
            cmd.cmdGo(0x8000000);
        }
        cmd.releaseChip();
    }

    private byte [] readFile(InputStream inputStream){
        ByteArrayOutputStream byteArrayOutputStream=null;

        int i;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    private class UploadESP32Asyc extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                            dialog.cancel();

                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String firmwareFileName[] = new String[4];
            if (!recorverFirmware) {
                firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_FILE1;
                firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_FILE2;
                firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_FILE3;
                firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_FILE4;
                uploadESP32(firmwareFileName, mUploadSTM32Callback);
            } else {
                firmwareFileName[0] = ASSET_FILE_RESET_ALTIESP32_FILE1;
                firmwareFileName[1] = ASSET_FILE_RESET_ALTIESP32_FILE2;
                firmwareFileName[2] = ASSET_FILE_RESET_ALTIESP32_FILE3;
                firmwareFileName[3] = ASSET_FILE_RESET_ALTIESP32_FILE4;
                uploadESP32(firmwareFileName, mUploadSTM32Callback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }
    public void uploadESP32(String fileName[], UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream file1 = null;
        InputStream file2 = null;
        InputStream file3 = null;
        InputStream file4 = null;
        CommandInterfaceESP32 cmd;


        cmd = new CommandInterfaceESP32(UpCallback, mPhysicaloid);

        try {
            file1 = getAssets().open(fileName[0]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }

        try {
            file2 = getAssets().open(fileName[1]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }
        try {
            file3 = getAssets().open(fileName[2]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }

        try {
            file4 = getAssets().open(fileName[3]);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }

        dialogAppend("Starting ...");


        boolean ret = cmd.initChip();
        if (ret )
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend("Chip has not been initiated:" + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            // let's detect the chip, not really required but I just want to make sure that
            // it is
            // an ESP32 because this is what the program is for
            int chip = cmd.detectChip();
            if (chip == cmd.ESP32)
                //dialogAppend("Chip is ESP32");
                tvAppend(tvRead,"Chip is ESP32\n");

            // now that we have initialized the chip we can change the baud rate to 921600
            // first we tell the chip the new baud rate
            dialogAppend("Changing baudrate to 921600");
            cmd.changeBaudeRate();
            cmd.init();

            // Those are the files you want to flush
            dialogAppend("Flashing file 1 0xe000");
            cmd.flashData(readFile(file1), 0xe000, 0);
            dialogAppend("Flashing file 2 0x1000");
            cmd.flashData(readFile(file2), 0x1000, 0);

            dialogAppend("Flashing file 3 0x10000");
            cmd.flashData(readFile(file3), 0x10000, 0);
            dialogAppend("Flashing file 4 0x8000");
            cmd.flashData(readFile(file4), 0x8000, 0);

            // we have finish flashing lets reset the board so that the program can start
            cmd.reset();

            dialogAppend("done ");
            tvAppend(tvRead, "done");
        }
    }


    Physicaloid.UploadCallBack mUploadCallback = new Physicaloid.UploadCallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12) + value + " %");
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPostUpload(boolean success) {
            if (success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }
            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadErrors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18) + err.toString() + "\n");
        }

    };

    UploadSTM32CallBack mUploadSTM32Callback = new UploadSTM32CallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12) + value + " %");
        }

        @Override
        public void onInfo(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPostUpload(boolean success) {
            if (success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }
            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadSTM32Errors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18) + err.toString() + "\n");
        }

    };
    Handler mHandler = new Handler();

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

    private void setRadioButton (RadioButton rb, boolean state) {
        final RadioButton frb = rb;
        final boolean fstate = state;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                frb.setChecked(fstate);
            }
        });
    }
    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }

    private void close() {
        if (mPhysicaloid.close()) {

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
            Intent i = new Intent(FlashFirmware.this, HelpActivity.class);
            i.putExtra("help_file", "help_flash_firmware");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(FlashFirmware.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}