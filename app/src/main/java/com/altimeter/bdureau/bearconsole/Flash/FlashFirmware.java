package com.altimeter.bdureau.bearconsole.Flash;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlashFirmware
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Firmware flashing screen. The user selects the firmware matching his altimeter (it can be
 *  detected automatically) and the application flashes it through a USB OTG cable:
 *    - ATmega328 boards (AltiMulti, AltiMultiV2, AltiServo, AltiDuo, AltiGPS): Intel HEX file
 *      uploaded with the Arduino bootloader protocol (Physicaloid library);
 *    - STM32 board: binary uploaded with the STM32 system bootloader (CommandInterfaceSTM32);
 *    - ESP32 boards: four binary images uploaded with the ESP32 bootloader (CommandInterfaceESP32).
 *  A "recovery" firmware can also be flashed to reset a corrupted altimeter configuration.
 *  The firmware images are stored in the application assets.
 * ============================================================================
 */

import com.altimeter.bdureau.bearconsole.BaseActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;


import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.util.ArrayList;

import static com.physicaloid.misc.Misc.toHexStr;


public class FlashFirmware extends BaseActivity {
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

    private static final String ASSET_FILE_NAME_ALTIMULTIV2 = "firmwares/Altimulti/AltiMultiV2-2.3.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTI = "firmwares/Altimulti/AltiMulti-2.0.hex";
    private static final String ASSET_FILE_NAME_ALTISERVO = "firmwares/AltiServo/AltiServoV1_7.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIDUO = "firmwares/2023-02-27-AltiDuoV1_9_console.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTISTM32 = "firmwares/STM32/RocketFlightLoggerV2.2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIGPS = "firmwares/AltiGPS/RocketGPSLoggerV1.8.ino.bin";
    // ESP32
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE2 = "firmwares/ESP32/RocketFlightLoggerV2.2.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE3 = "firmwares/ESP32/RocketFlightLoggerV2.2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_FILE4 = "firmwares/ESP32/RocketFlightLoggerV2.2.ino.partitions.bin";

    private static final String ASSET_FILE_NAME_ALTIESP32V2_FILE1 = "firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32V2_FILE2 = "firmwares/ESP32/RocketFlightLoggerV2.4_ESP32V2.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32V2_FILE3 = "firmwares/ESP32/RocketFlightLoggerV2.4_ESP32V2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32V2_FILE4 = "firmwares/ESP32/RocketFlightLoggerV2.4_ESP32V2.ino.partitions.bin";

    //ESP32 accelero
    private static final String ASSET_FILE_NAME_ALTIESP32_accelero_FILE1 = "firmwares/ESP32_accelero/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_accelero_FILE2 = "firmwares/ESP32_accelero/RocketFlightLoggerV2.2.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_accelero_FILE3 = "firmwares/ESP32_accelero/RocketFlightLoggerV2.2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_accelero_FILE4 = "firmwares/ESP32_accelero/RocketFlightLoggerV2.2.ino.partitions.bin";
    //ESP32 ADXL345
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE1 = "firmwares/ESP32_ADXL345/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE2 = "firmwares/ESP32_ADXL345/RocketFlightLoggerV2.2.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE3 = "firmwares/ESP32_ADXL345/RocketFlightLoggerV2.2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE4 = "firmwares/ESP32_ADXL345/RocketFlightLoggerV2.2.ino.partitions.bin";
    //ESP32 ADXL375
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE1 = "firmwares/ESP32_ADXL375/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE2 = "firmwares/ESP32_ADXL375/RocketFlightLoggerV2.2.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE3 = "firmwares/ESP32_ADXL375/RocketFlightLoggerV2.2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE4 = "firmwares/ESP32_ADXL375/RocketFlightLoggerV2.2.ino.partitions.bin";

    //TTGO BearAltimeter
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_FILE1 = "firmwares/TTGO/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_FILE2 = "firmwares/TTGO/TTGOBearAltimeter0.5.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_FILE3 = "firmwares/TTGO/TTGOBearAltimeter0.5.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_FILE4 = "firmwares/TTGO/TTGOBearAltimeter0.5.ino.partitions.bin";

    //LILYGO T-QT Mini BearAltimeter
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE1 = "firmwares/TTGO_T-QT_Mini/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE2 = "firmwares/TTGO_T-QT_Mini/TTGOMiniBearAltimeter0.2.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE3 = "firmwares/TTGO_T-QT_Mini/TTGOMiniBearAltimeter0.2.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE4 = "firmwares/TTGO_T-QT_Mini/TTGOMiniBearAltimeter0.2.ino.partitions.bin";

    //LILYGO T-QT Simple
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE1 = "firmwares/TTGO_T-QT/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE2 = "firmwares/TTGO_T-QT/TTGOSimpleAltimeter0.1.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE3 = "firmwares/TTGO_T-QT/TTGOSimpleAltimeter0.1.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE4 = "firmwares/TTGO_T-QT/TTGOSimpleAltimeter0.1.ino.partitions.bin";

    //UltimateAltimeter
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE1 = "firmwares/UltimateAltimeter/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE2 = "firmwares/UltimateAltimeter/UltimateAltimeterLittleFS0.5.nosleep.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE3 = "firmwares/UltimateAltimeter/UltimateAltimeterLittleFS0.5.nosleep.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE4 = "firmwares/UltimateAltimeter/UltimateAltimeterLittleFS0.5.nosleep.partitions.bin";

    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE1 = "firmwares/UltimateAltimeter/boot_app0.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE2 = "firmwares/UltimateAltimeter/UltimateAltimeterLittleFS0.5.ino.bootloader.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE3 = "firmwares/UltimateAltimeter/UltimateAltimeterLittleFS0.5.ino.bin";
    private static final String ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE4 = "firmwares/UltimateAltimeter/UltimateAltimeterLittleFS0.5.ino.partitions.bin";

    private static final String ASSET_FILE_RESET_ALTIDUO = "recover_firmwares/ResetAltiConfigAltiDuo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTIMULTI = "recover_firmwares/ResetAltiConfigAltimulti.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISERVO = "recover_firmwares/ResetAltiConfigAltiServo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISTM32 = "recover_firmwares/ResetAltiConfigAltimultiSTM32.ino.bin";

    private static final String ASSET_FILE_RESET_ALTIESP32_FILE1 = "recover_firmwares/ESP32/boot_app0.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE2 = "recover_firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.bootloader.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE3 = "recover_firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.bin";
    private static final String ASSET_FILE_RESET_ALTIESP32_FILE4 = "recover_firmwares/ESP32/ResetAltiConfigAltimultiESP32.ino.partitions.bin";


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
                "AltiESP32",
                "AltiESP32V2",
                "AltiESP32_accelero",
                "AltiESP32_ADXL345",
                "AltiESP32_ADXL375",
                "TTGOAltimeter",
                "TTGOMiniAltimeter",
                "TTGOSimpleAltimeter",
                "UltimateAltimeter",
                "UltimateAltimeterNoSleep"

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
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiv1_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiv2_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altiservo_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altiduo_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32") ||
                        itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultistm32_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32V2"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_accelero"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_accelero_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_ADXL345"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_adxl345_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_ADXL375"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altimultiesp32_adxl375_small, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOAltimeter"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.ttgo_altimeter_plash_screen315x160, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOMiniAltimeter"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.ttgo_t_qt_mini_altimeter, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOSimpleAltimeter"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.ttgo_t_qt_simple_altimeter, getApplicationContext().getTheme()));

                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("UltimateAltimeter"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.ultimate_altimeter_small, getApplicationContext().getTheme()));
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("UltimateAltimeterNoSleep"))
                    imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.ultimate_altimeter_small_nosleep, getApplicationContext().getTheme()));

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
        mHandler.removeCallbacksAndMessages(null);
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

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti")) {
            recoverFileName = ASSET_FILE_RESET_ALTIMULTI;
        }
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2")) {
            recoverFileName = ASSET_FILE_RESET_ALTIMULTI;
        }
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo")) {
            recoverFileName = ASSET_FILE_RESET_ALTISERVO;
        }
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo")) {
            recoverFileName = ASSET_FILE_RESET_ALTIDUO;
        }
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")) {
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
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
            } catch (IOException e) {
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")) {
            recorverFirmware = true;
            new UploadSTM32Asyc().execute();
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32V2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_accelero") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_ADXL345") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_ADXL375") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOMiniAltimeter") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOSimpleAltimeter") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("UltimateAltimeter") ||
        itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("UltimateAltimeterNoSleep")
        ) {
            recorverFirmware = true;
            new UploadESP32Asyc().execute();
        }
    }

    public void onClickDetect(View v) {
        new DetectAsyc().execute();
    }

    public void onClickFirmwareInfo(View v) {
        tvRead.setText(getString(R.string.flash_available_firmwares));
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
        tvRead.append(ASSET_FILE_NAME_ALTIESP32V2_FILE2);
        tvRead.append("\n");
    }

    public void onClickFlash(View v) {
        String firmwareFileName;

        firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTIV2;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo"))
            firmwareFileName = ASSET_FILE_NAME_ALTISERVO;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo"))
            firmwareFileName = ASSET_FILE_NAME_ALTIDUO;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32"))
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTISTM32;
        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS"))
            firmwareFileName = ASSET_FILE_NAME_ALTIGPS;

        tvRead.setText("");

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMulti") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiV2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiServo") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiDuo")) {
            tvRead.setText(getString(R.string.flash_loading_firmware, firmwareFileName));
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
            } catch (IOException e) {
            }
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiGPS") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiMultiSTM32")) {
            tvRead.setText(getString(R.string.flash_loading_firmware, firmwareFileName));
            recorverFirmware = false;
            new UploadSTM32Asyc().execute();
        } else if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32V2") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_accelero") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_ADXL345") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("AltiESP32_ADXL375") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOAltimeter") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOMiniAltimeter") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("UltimateAltimeter") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("UltimateAltimeterNoSleep") ||
                itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("TTGOSimpleAltimeter")) {
            tvRead.setText(getString(R.string.flash_loading_esp32) + "\n");
            recorverFirmware = false;
            new UploadESP32Asyc().execute();
        }
    }

    /**
     * Result of a STM32 / ESP32 flashing, shown to the user when the task ends.
     * "UNTOUCHED" means that nothing was written: the altimeter keeps its firmware.
     */
    private enum FlashResult {
        SUCCESS,
        FAILED_UNTOUCHED,
        FAILED,
        CANCELLED_UNTOUCHED,
        CANCELLED
    }

    // Command interface of the flashing in progress (null when none), used by the Cancel button
    private volatile CommandInterfaceSTM32 currentStm32Cmd = null;
    private volatile CommandInterfaceESP32 currentEsp32Cmd = null;
    // Cancel pressed: also covers the moment before the command interface is created
    private volatile boolean flashCancelRequested = false;

    /**
     * Called by the Cancel button of the flashing dialog (UI thread): the flashing thread stops
     * after the current block. Nothing else is sent to the bootloader, so the frames are
     * unchanged. The ROM bootloaders of the STM32 and of the ESP32 are never erased: a
     * cancelled board can always be flashed again.
     */
    private void requestFlashCancel() {
        flashCancelRequested = true;
        CommandInterfaceSTM32 stm32 = currentStm32Cmd;
        if (stm32 != null) stm32.cancel();
        CommandInterfaceESP32 esp32 = currentEsp32Cmd;
        if (esp32 != null) esp32.cancel();
        tvAppend(tvRead, "\n" + getString(R.string.flash_cancel_requested) + "\n");
    }

    // Firmware and baud rate selected when the flashing started. They are read from the
    // spinners on the UI thread (showFlashingDialog(), called by onPreExecute()) because the
    // flashing thread must not touch Views (lint "Call must be from @UiThread").
    private volatile String selectedFirmware = "";
    private volatile int selectedBaudRate = 115200;

    /**
     * Creates the "Flashing..." dialog whose Cancel button really stops the flashing, and
     * records the selected firmware and baud rate (UI thread).
     */
    private void showFlashingDialog() {
        flashCancelRequested = false;
        selectedFirmware = itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()];
        selectedBaudRate = Integer.parseInt(itemsBaudRate[(int) dropdownBaudRate.getSelectedItemId()]);
        builder = new AlertDialog.Builder(FlashFirmware.this);
        //Flashing firmware...
        builder.setMessage(getResources().getString(R.string.msg10))
                .setTitle(getResources().getString(R.string.msg11))
                .setCancelable(false)
                .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        requestFlashCancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    /** Shows the result of a STM32 / ESP32 flashing (UI thread). */
    private void showFlashResult(FlashResult result) {
        alert.dismiss();
        if (isFinishing() || isDestroyed()) return;
        int message;
        switch (result) {
            case SUCCESS:
                // "Done" has already been written in the log by the flashing thread
                return;
            case FAILED_UNTOUCHED:
                message = R.string.flash_failed_untouched;
                break;
            case CANCELLED_UNTOUCHED:
                message = R.string.flash_cancelled_untouched;
                break;
            case CANCELLED:
                message = R.string.flash_cancelled;
                break;
            case FAILED:
            default:
                message = R.string.flash_failed;
                break;
        }
        tvRead.append("\n" + getString(message) + "\n");
        // A dialog, so that a board left without a valid firmware cannot go unnoticed
        new AlertDialog.Builder(FlashFirmware.this)
                .setTitle(getString(R.string.flash_result_title))
                .setMessage(getString(message))
                .setPositiveButton(getString(R.string.close), (dialog, id) -> dialog.dismiss())
                .show();
    }

    /**
     * Maps the name returned by the altimeter to the position of its firmware in the
     * firmware spinner.
     *
     * @return the position, or -1 if the name is not one of the detected firmwares
     */
    private static int firmwareIndexFromVersion(String version) {
        switch (version) {
            case "AltiMulti":
                return 0;
            case "AltiMultiV2":
                return 1;
            case "AltiServo":
                return 2;
            case "AltiDuo":
                return 3;
            case "AltiMultiSTM32":
                return 4;
            case "AltiGPS":
                return 5;
            default:
                return -1;
        }
    }

    private class DetectAsyc extends AsyncTask<Void, Void, Integer>  // UI thread
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

        /**
         * Worker thread: reads the firmware name only. The spinner is a View and must not be
         * touched here: its selection is changed in onPostExecute(), on the UI thread.
         */
        @Override
        protected Integer doInBackground(Void... voids) {

            String version = "";

            FirmwareInfo firm = new FirmwareInfo(mPhysicaloid);
            firm.open(38400);
            version = firm.getFirmwarVersion();

            // tvAppend() posts the text to the UI thread
            tvAppend(tvRead, getString(R.string.firmware_version_not_detected) + version + "\n");

            return firmwareIndexFromVersion(version);
        }

        @Override
        protected void onPostExecute(Integer firmwareIndex) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
            // UI thread: select the detected firmware (this also updates the altimeter picture)
            if (firmwareIndex != null && firmwareIndex >= 0) {
                spinnerFirmware.setSelection(firmwareIndex);
            }
        }
    }


    /**
     * Last safety net of a flashing task: an unexpected exception in the worker thread used to
     * close the application. It is now logged and reported as a failed flashing.
     */
    private FlashResult reportUnexpectedFlashError(RuntimeException e) {
        Log.e("FlashFirmware", "Unexpected error while flashing", e);
        tvAppend(tvRead, "\nUnexpected error: " + e + "\n");
        currentStm32Cmd = null;
        currentEsp32Cmd = null;
        return FlashResult.FAILED;
    }

    private class UploadSTM32Asyc extends AsyncTask<Void, Void, FlashResult>  // UI thread
    {
        @Override
        protected void onPreExecute() {
            showFlashingDialog();
        }

        @Override
        protected FlashResult doInBackground(Void... voids) {
            // Nothing is flashed if the selected firmware is not a STM32 one
            FlashResult result = FlashResult.FAILED_UNTOUCHED;
            try {
                if (!recorverFirmware) {
                    if (selectedFirmware.equals("AltiMultiSTM32"))
                        result = uploadSTM32(ASSET_FILE_NAME_ALTIMULTISTM32, mUploadSTM32Callback);
                    if (selectedFirmware.equals("AltiGPS"))
                        result = uploadSTM32(ASSET_FILE_NAME_ALTIGPS, mUploadSTM32Callback);
                } else {
                    result = uploadSTM32(ASSET_FILE_RESET_ALTISTM32, mUploadSTM32Callback);
                }
            } catch (RuntimeException e) {
                result = reportUnexpectedFlashError(e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(FlashResult result) //after the doInBackground, it checks if everything went fine
        {
            showFlashResult(result);
        }
    }

    /**
     * Flashes a STM32 board (worker thread). The bootloader commands and their order are
     * unchanged; the Cancel button is checked before the erase and between the written
     * blocks, and the result of the writing is now reported.
     */
    public FlashResult uploadSTM32(String fileName, UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream is = null;

        try {
            is = getAssets().open(fileName);
        } catch (IOException e) {
            tvAppend(tvRead, getString(R.string.flash_file_not_found, ASSET_FILE_NAME_ALTIMULTISTM32) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32 + "\n");
        }

        dialogAppend(getString(R.string.flash_starting));
        CommandInterfaceSTM32 cmd;

        cmd = new CommandInterfaceSTM32(UpCallback, mPhysicaloid);
        currentStm32Cmd = cmd;
        // Cancel may have been pressed before the command interface existed
        if (flashCancelRequested) cmd.cancel();

        cmd.open(selectedBaudRate);
        int ret = cmd.initChip();
        if (ret == 1)
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend(getString(R.string.flash_chip_not_initiated, String.valueOf(ret)));
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            bootversion = cmd.cmdGet();
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

        // Last point where the firmware of the board is still intact
        if (!failed && cmd.isCancelRequested()) {
            cmd.releaseChip();
            currentStm32Cmd = null;
            return FlashResult.CANCELLED_UNTOUCHED;
        }
        if (failed) {
            // Nothing has been erased or written
            cmd.releaseChip();
            currentStm32Cmd = null;
            return FlashResult.FAILED_UNTOUCHED;
        }

        if (bootversion < 0x30) {
            tvAppend(tvRead, "Erase 1\n");
            cmd.cmdEraseMemory();
        } else {
            tvAppend(tvRead, "Erase 2\n");
            cmd.cmdExtendedEraseMemory();
        }

        cmd.drain();
        tvAppend(tvRead, "writeMemory" + "\n");
        ret = cmd.writeMemory(0x8000000, is);

        tvAppend(tvRead, "writeMemory finish" + "\n\n\n\n");
        if (ret == CommandInterfaceSTM32.WRITE_OK) {
            tvAppend(tvRead, "writeMemory success" + "\n\n\n\n");
        }

        // Do not start a firmware that was only partly written because of a cancel
        if (ret != CommandInterfaceSTM32.WRITE_CANCELLED) {
            cmd.cmdGo(0x8000000);
        }
        cmd.releaseChip();
        currentStm32Cmd = null;

        if (ret == CommandInterfaceSTM32.WRITE_CANCELLED) return FlashResult.CANCELLED;
        return ret == CommandInterfaceSTM32.WRITE_OK ? FlashResult.SUCCESS : FlashResult.FAILED;
    }

    private byte[] readFile(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = null;

        int i;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    private class UploadESP32Asyc extends AsyncTask<Void, Void, FlashResult>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            showFlashingDialog();
        }

        @Override
        protected FlashResult doInBackground(Void... voids) {
            try {
                return uploadESP32Selected();
            } catch (RuntimeException e) {
                return reportUnexpectedFlashError(e);
            }
        }

        /** Chooses the ESP32 files of the selected firmware (or of the recovery) and flashes them. */
        private FlashResult uploadESP32Selected() {
            FlashResult result;
            String firmwareFileName[] = new String[4];
            if (!recorverFirmware) {
                if (selectedFirmware.equals("AltiESP32")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_FILE4;
                } else if (selectedFirmware.equals("AltiESP32V2")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32V2_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32V2_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32V2_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32V2_FILE4;
                } else if (selectedFirmware.equals("AltiESP32_accelero")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_accelero_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_accelero_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_accelero_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_accelero_FILE4;
                } else if (selectedFirmware.equals("AltiESP32_ADXL345")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_ADXL345_FILE4;
                } else if (selectedFirmware.equals("AltiESP32_ADXL375")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_ADXL375_FILE4;
                } else if (selectedFirmware.equals("TTGOAltimeter")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_TTGO_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_TTGO_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_TTGO_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_TTGO_FILE4;
                }
                else if (selectedFirmware.equals("TTGOMiniAltimeter")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_MINI_FILE4;
                } else if (selectedFirmware.equals("TTGOSimpleAltimeter")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_TTGO_TQ_FILE4;
                } else if (selectedFirmware.equals("UltimateAltimeter")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_FILE4;
                } else if (selectedFirmware.equals("UltimateAltimeterNoSleep")) {
                    firmwareFileName[0] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE1;
                    firmwareFileName[1] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE2;
                    firmwareFileName[2] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE3;
                    firmwareFileName[3] = ASSET_FILE_NAME_ALTIESP32_ULTIMATE_NOSLEEP_FILE4;
                }
                result = uploadESP32(firmwareFileName, mUploadSTM32Callback);
            } else {
                firmwareFileName[0] = ASSET_FILE_RESET_ALTIESP32_FILE1;
                firmwareFileName[1] = ASSET_FILE_RESET_ALTIESP32_FILE2;
                firmwareFileName[2] = ASSET_FILE_RESET_ALTIESP32_FILE3;
                firmwareFileName[3] = ASSET_FILE_RESET_ALTIESP32_FILE4;

                result = uploadESP32(firmwareFileName, mUploadSTM32Callback);
            }
            return result;
        }

        @Override
        protected void onPostExecute(FlashResult result) //after the doInBackground, it checks if everything went fine
        {
            showFlashResult(result);
        }
    }

    /**
     * Flashes an ESP32 board (worker thread). The bootloader commands and their order are
     * unchanged; the Cancel button is checked before the first write and between the written
     * blocks, and the result of every file is now reported (a block not acknowledged even
     * after its retry makes the flashing fail).
     */
    public FlashResult uploadESP32(String fileName[], UploadSTM32CallBack UpCallback) {
        boolean failed = false;
        InputStream file1 = null;
        InputStream file2 = null;
        InputStream file3 = null;
        InputStream file4 = null;
        CommandInterfaceESP32 cmd;


        cmd = new CommandInterfaceESP32(UpCallback, mPhysicaloid);
        currentEsp32Cmd = cmd;
        // Cancel may have been pressed before the command interface existed
        if (flashCancelRequested) cmd.cancel();

        try {
            file1 = getAssets().open(fileName[0]);

        } catch (IOException e) {
            tvAppend(tvRead, getString(R.string.flash_file_not_found, ASSET_FILE_NAME_ALTIESP32_FILE1) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE1 + "\n");
        }

        try {
            file2 = getAssets().open(fileName[1]);

        } catch (IOException e) {
            tvAppend(tvRead, getString(R.string.flash_file_not_found, ASSET_FILE_NAME_ALTIESP32_FILE2) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE2 + "\n");
        }
        try {
            file3 = getAssets().open(fileName[2]);

        } catch (IOException e) {
            tvAppend(tvRead, getString(R.string.flash_file_not_found, ASSET_FILE_NAME_ALTIESP32_FILE3) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE3 + "\n");
        }

        try {
            file4 = getAssets().open(fileName[3]);

        } catch (IOException e) {
            tvAppend(tvRead, getString(R.string.flash_file_not_found, ASSET_FILE_NAME_ALTIESP32_FILE4) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIESP32_FILE4 + "\n");
        }

        dialogAppend(getString(R.string.flash_starting));


        boolean ret = cmd.initChip();
        if (ret)
            dialogAppend(getString(R.string.chip_has_not_been_init) + ret);
        else {
            dialogAppend(getString(R.string.flash_chip_not_initiated, String.valueOf(ret)));
            failed = true;
        }
        if (failed) {
            // The chip did not answer: nothing has been written
            currentEsp32Cmd = null;
            return FlashResult.FAILED_UNTOUCHED;
        }
        int bootversion = 0;
        if (!failed) {
            // let's detect the chip, not really required but I just want to make sure that
            // it is
            // an ESP32 because this is what the program is for
            int chip = cmd.detectChip();
            if (chip == cmd.ESP32)
                tvAppend(tvRead, "Chip is ESP32\n");
            else if (chip == cmd.ESP32S2)
                tvAppend(tvRead, "Chip is ESP32S2\n");
            else if (chip == cmd.ESP32S3)
                tvAppend(tvRead, "Chip is ESP32S3\n");
            else if (chip == cmd.ESP32C3)
                tvAppend(tvRead, "Chip is ESP32C3\n");
            else if (chip == cmd.ESP32C6)
                tvAppend(tvRead, "Chip is ESP32C6\n");
            else if (chip == cmd.ESP32H2)
                tvAppend(tvRead, "Chip is ESP32H2\n");
            else if (chip == cmd.ESP8266)
                tvAppend(tvRead, "Chip is ESP8266\n");
            else
                tvAppend(tvRead, chip + "is an unknown Chip\n");

            tvAppend(tvRead, selectedFirmware);

            // Last point where the firmware of the board is still intact
            if (cmd.isCancelRequested()) {
                // Restart the board on its current firmware
                cmd.reset();
                currentEsp32Cmd = null;
                return FlashResult.CANCELLED_UNTOUCHED;
            }
            // False as soon as one file is not completely acknowledged (or cancelled)
            boolean flashOk = true;
            // now that we have initialized the chip we can change the baud rate to 921600
            // first we tell the chip the new baud rate
            dialogAppend("Changing baudrate to 921600");
            cmd.changeBaudeRate();
            cmd.init();

            if (chip == cmd.ESP32) {
                // Those are the files you want to flash
                dialogAppend("Flashing file 1 0xe000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP32\n");
                flashOk &= cmd.flashData(readFile(file1), 0xe000, 0);
                dialogAppend("Flashing file 2 0x1000");
                tvAppend(tvRead, fileName[1] + "\n");
                flashOk &= cmd.flashData(readFile(file2), 0x1000, 0);

                dialogAppend("Flashing file 3 0x10000");
                tvAppend(tvRead, fileName[2] + "\n");
                flashOk &= cmd.flashData(readFile(file3), 0x10000, 0);
                dialogAppend("Flashing file 4 0x8000");
                tvAppend(tvRead, fileName[3] + "\n");
                flashOk &= cmd.flashData(readFile(file4), 0x8000, 0);
            } else if (chip == cmd.ESP32C3) {
                // Those are the files you want to flash
                dialogAppend("Flashing file 1 0xe000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP32C3\n");
                flashOk &= cmd.flashCompressedData(readFile(file1), 0xe000, 0);
                dialogAppend("Flashing file 2 0x0");
                tvAppend(tvRead, fileName[1] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file2), 0x0, 0);

                dialogAppend("Flashing file 3 0x10000");
                tvAppend(tvRead, fileName[2] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file3), 0x10000, 0);
                dialogAppend("Flashing file 4 0x8000");
                tvAppend(tvRead, fileName[3] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file4), 0x8000, 0);
            } else if (chip == cmd.ESP32C6) {
                // Those are the files you want to flash
                dialogAppend("Flashing file 1 0xe000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP32C6\n");
                flashOk &= cmd.flashCompressedData(readFile(file1), 0xe000, 0);
                dialogAppend("Flashing file 2 0x0");
                tvAppend(tvRead, fileName[1] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file2), 0x0, 0);

                dialogAppend("Flashing file 3 0x10000");
                tvAppend(tvRead, fileName[2] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file3), 0x10000, 0);
                dialogAppend("Flashing file 4 0x8000");
                tvAppend(tvRead, fileName[3] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file4), 0x8000, 0);
            } else if (chip == cmd.ESP32S2) {
                // Those are the files you want to flash
                dialogAppend("Flashing file 1 0xe000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP32S2\n");
                flashOk &= cmd.flashCompressedData(readFile(file1), 0xe000, 0);
                dialogAppend("Flashing file 2 0x0");
                tvAppend(tvRead, fileName[1] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file2), 0x1000, 0);

                dialogAppend("Flashing file 3 0x10000");
                tvAppend(tvRead, fileName[2] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file3), 0x10000, 0);
                dialogAppend("Flashing file 4 0x8000");
                tvAppend(tvRead, fileName[3] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file4), 0x8000, 0);
            } else if (chip == cmd.ESP32S3) {
                // Those are the files you want to flash
                dialogAppend("Flashing file 1 0xe000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP32S3\n");
                flashOk &= cmd.flashCompressedData(readFile(file1), 0xe000, 0);
                dialogAppend("Flashing file 2 0x0");
                tvAppend(tvRead, fileName[1] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file2), 0x0, 0);

                dialogAppend("Flashing file 3 0x10000");
                tvAppend(tvRead, fileName[2] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file3), 0x10000, 0);
                dialogAppend("Flashing file 4 0x8000");
                tvAppend(tvRead, fileName[3] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file4), 0x8000, 0);
            } else if (chip == cmd.ESP8266) {
                dialogAppend("Flashing file 1 0x0000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP8266\n");
                flashOk &= cmd.flashData(readFile(file1), 0x0000, 0);
            } else if (chip == cmd.ESP32H2) {
                // Those are the files you want to flash
                dialogAppend("Flashing file 1 0xe000");
                tvAppend(tvRead, fileName[0] + "\n");
                tvAppend(tvRead, "Chip is ESP32H2\n");
                flashOk &= cmd.flashCompressedData(readFile(file1), 0xe000, 0);
                dialogAppend("Flashing file 2 0x0");
                tvAppend(tvRead, fileName[1] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file2), 0x1000, 0);

                dialogAppend("Flashing file 3 0x10000");
                tvAppend(tvRead, fileName[2] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file3), 0x10000, 0);
                dialogAppend("Flashing file 4 0x8000");
                tvAppend(tvRead, fileName[3] + "\n");
                flashOk &= cmd.flashCompressedData(readFile(file4), 0x8000, 0);
            } else {
                // Unknown chip: nothing has been written, never report "Done"
                tvAppend(tvRead, "No firmware for this chip\n");
                cmd.reset();
                currentEsp32Cmd = null;
                return FlashResult.FAILED_UNTOUCHED;
            }

            // we have finish flashing lets reset the board so that the program can start
            cmd.reset();
            currentEsp32Cmd = null;

            if (cmd.isCancelRequested()) return FlashResult.CANCELLED;
            if (!flashOk) return FlashResult.FAILED;

            dialogAppend(getString(R.string.flash_done));
            tvAppend(tvRead, getString(R.string.flash_done));
        }
        return FlashResult.SUCCESS;
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
    // Posts progress messages to the UI thread; purged in onDestroy() to avoid leaks
    Handler mHandler = new Handler(Looper.getMainLooper());

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