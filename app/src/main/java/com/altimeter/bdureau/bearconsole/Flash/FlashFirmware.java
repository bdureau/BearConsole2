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
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

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
    public RadioButton rbAltiMulti, rbAltiMultiV2, rbAltiServo, rbAltiDuo, rbAltiMultiSTM32;
    public RadioButton rbAltiGPS, rbAltiMultiESP32;
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
    private static final String ASSET_FILE_NAME_ALTIGPS = "firmwares/2022-08-25-RocketGPSLoggerV1.5.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE2 = "firmwares/ESP32/RocketFlightLoggerV1_27.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE3 = "firmwares/ESP32/RocketFlightLoggerV1_27.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE4 = "firmwares/ESP32/RocketFlightLoggerV1_27.ino.partitions.bin";
    /*private static final String ASSET_FILE_NAME_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE2 = "firmwares/ESP32/Blink.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE3 = "firmwares/ESP32/Blink.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE4 = "firmwares/ESP32/Blink.ino.partitions.bin";*/

    private static final String ASSET_FILE_RESET_ALTIDUO = "recover_firmwares/ResetAltiConfigAltiDuo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTIMULTI = "recover_firmwares/ResetAltiConfigAltimulti.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISERVO = "recover_firmwares/ResetAltiConfigAltiServo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISTM32 = "recover_firmwares/ResetAltiConfigAltimultiSTM32.ino.bin";

    private static final String ASSET_FILE_RESET_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE2 = "firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.bootloader.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE3 = "firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE4 = "firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.partitions.bin";

    private String[] itemsBaudRate;
    private Spinner dropdownBaudRate;

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /*if(AppCompatDelegate.getDefaultNightMode()== AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_firmware);

        btFlash = (Button) findViewById(R.id.btFlash);
        tvRead = (TextView) findViewById(R.id.tvRead);
        rbAltiMulti = (RadioButton) findViewById(R.id.radioButAltiMulti);
        rbAltiMultiV2 = (RadioButton) findViewById(R.id.radioButAltiMultiV2);
        rbAltiServo = (RadioButton) findViewById(R.id.radioButAltiServo);
        rbAltiDuo = (RadioButton) findViewById(R.id.radioButAltiDuo);
        rbAltiMulti.setChecked(true);
        rbAltiMultiSTM32 = (RadioButton) findViewById(R.id.radioButAltiMultiSTM32);
        rbAltiGPS = (RadioButton) findViewById(R.id.radioButAltiGPS);
        rbAltiMultiESP32 = (RadioButton) findViewById(R.id.radioButAltiESP32);

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
        if (rbAltiMulti.isChecked())
            recoverFileName = ASSET_FILE_RESET_ALTIMULTI;
        if (rbAltiMultiV2.isChecked())
            recoverFileName = ASSET_FILE_RESET_ALTIMULTI;
        if (rbAltiServo.isChecked())
            recoverFileName = ASSET_FILE_RESET_ALTISERVO;
        if (rbAltiDuo.isChecked())
            recoverFileName = ASSET_FILE_RESET_ALTIDUO;
        if (rbAltiMultiSTM32.isChecked())
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;
        if (rbAltiGPS.isChecked())
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;

        tvRead.setText("");
        tvRead.setText(getResources().getString(R.string.after_complete_upload));
        if (rbAltiMulti.isChecked()|| rbAltiMultiV2.isChecked() || rbAltiServo.isChecked() || rbAltiDuo.isChecked()) {
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
        } else if(rbAltiMultiSTM32.isChecked()|| rbAltiGPS.isChecked()){
            recorverFirmware = true;
            new UploadSTM32Asyc().execute();
        } else if (rbAltiMultiESP32.isChecked()) {
            recorverFirmware = true;
            new UploadESP32Asyc().execute();
        }
    }

    public void onClickDetect(View v) {
        new DetectAsyc().execute();
    }


    public void onClickFlash(View v) {
        String firmwareFileName ;

        firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;

        if (rbAltiMulti.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;
        if (rbAltiMultiV2.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTIV2;
        if (rbAltiServo.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTISERVO;
        if (rbAltiDuo.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTIDUO;
        if (rbAltiMultiSTM32.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTISTM32;
        if (rbAltiMultiESP32.isChecked()) {
            /*firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_FILE1;
            firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_FILE2;
            firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_FILE3;
            firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_FILE4;*/
        }

        tvRead.setText("");

        if (rbAltiMulti.isChecked()|| rbAltiMultiV2.isChecked() || rbAltiServo.isChecked() || rbAltiDuo.isChecked()) {
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
        } else if (rbAltiMultiSTM32.isChecked()|| rbAltiGPS.isChecked()){
            tvRead.setText("Loading firmware:"+ firmwareFileName);
            recorverFirmware = false;
            new UploadSTM32Asyc().execute();
        } else if (rbAltiMultiESP32.isChecked()) {
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
                setRadioButton (rbAltiMulti,true);
            }
            if(version.equals("AltiMultiV2")) {
                setRadioButton (rbAltiMultiV2,true);
            }
            if(version.equals("AltiServo")) {
                setRadioButton (rbAltiServo,true);
            }
            if(version.equals("AltiDuo")) {
                setRadioButton (rbAltiDuo,true);
            }
            if(version.equals("AltiMultiSTM32")) {
                setRadioButton (rbAltiMultiSTM32,true);
            }
            if(version.equals("AltiGPS")) {
                setRadioButton (rbAltiGPS,true);
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
                if (rbAltiMultiSTM32.isChecked())
                    uploadSTM32(ASSET_FILE_NAME_ALTIMULTISTM32, mUploadSTM32Callback);
                if (rbAltiGPS.isChecked())
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