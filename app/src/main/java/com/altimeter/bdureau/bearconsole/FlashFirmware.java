package com.altimeter.bdureau.bearconsole;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;

//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.io.IOException;

import java.util.ArrayList;


public class FlashFirmware extends AppCompatActivity {
    Physicaloid mPhysicaloid;


    Boards mSelectedBoard;
    Button btOpen;
    RadioButton rdbAltiMulti,rdbAltiMultiV2, rbAltiServo, rbAltiDuo;
    TextView tvRead;
    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private ArrayList<Boards> mBoardList;
    private UartConfig uartConfig;

    private static final String ASSET_FILE_NAME_ALTIMULTIV2       = "firmwares/2019-12-28-V1_19_altimultiV2.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTI         = "firmwares/2019-12-28-V1_19_altimulti.ino.hex";
    private static final String ASSET_FILE_NAME_ALTISERVO         = "firmwares/2019-12-28-AltiServoV1_0.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIDUO         = "firmwares/2019-12-28-AltiDuoV1_5_console.ino.hex";

    private static final String ASSET_FILE_RESET_ALTIDUO = "recover_firmwares/ResetAltiConfigAltiDuo.ino.hex";
    private static final String ASSET_FILE_RESET_ALTIMULTI = "recover_firmwares/ResetAltiConfigAltimulti.ino.hex";
    private static final String ASSET_FILE_RESET_ALTISERVO = "recover_firmwares/ResetAltiConfigAltiServo.ino.hex";

    private String[] itemsBaudRate;
    private Spinner dropdownBaudRate;

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_firmware);

        btOpen          = (Button) findViewById(R.id.btFlash);
        tvRead          = (TextView) findViewById(R.id.tvRead);
        rdbAltiMulti = (RadioButton) findViewById(R.id.radioButAltiMulti);
        rdbAltiMultiV2 = (RadioButton) findViewById(R.id.radioButAltiMultiV2);
        rbAltiServo = (RadioButton) findViewById(R.id.radioButAltiServo);
        rbAltiDuo = (RadioButton) findViewById(R.id.radioButAltiDuo);
        rdbAltiMulti.setChecked(true);
        mPhysicaloid = new Physicaloid(this);
                mBoardList = new ArrayList<Boards>();
        for(Boards board : Boards.values()) {
            if(board.support>0) {
                mBoardList.add(board);
            }
        }

        mSelectedBoard = mBoardList.get(0);
        uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);

        btOpen.setEnabled(true);
        if(mPhysicaloid.open()) {
            mPhysicaloid.setConfig(uartConfig);

        } else {
            //cannot open
            Toast.makeText(this, getResources().getString(R.string.msg13), Toast.LENGTH_LONG).show();
            //btOpen.setEnabled(false);
        }


        //baud rate
        dropdownBaudRate = (Spinner)findViewById(R.id.spinnerBaud);
        itemsBaudRate = new String[]{ "300",
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
        if (rdbAltiMulti.isChecked())
            recoverFileName =ASSET_FILE_RESET_ALTIMULTI;
        if (rdbAltiMultiV2.isChecked())
            recoverFileName =ASSET_FILE_RESET_ALTIMULTI;
        if (rbAltiServo.isChecked())
            recoverFileName =ASSET_FILE_RESET_ALTISERVO;
        if (rbAltiDuo.isChecked())
            recoverFileName =ASSET_FILE_RESET_ALTIDUO;

        tvRead.setText("");
        tvRead.setText(getResources().getString(R.string.after_complete_upload));
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
            mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int)this.dropdownBaudRate.getSelectedItemId()]));
            mPhysicaloid.upload(mSelectedBoard,getResources().getAssets().open(recoverFileName), mUploadCallback);
        } catch (RuntimeException e) {
            //Log.e(TAG, e.toString());
        } catch (IOException e) {
            //Log.e(TAG, e.toString());
        }
    }

    public void onClickFlash(View v) {
        String firmwareFileName;

        firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;

        if (rdbAltiMulti.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTI;
        if (rdbAltiMultiV2.isChecked())
            firmwareFileName = ASSET_FILE_NAME_ALTIMULTIV2;
        if (rbAltiServo.isChecked())
            firmwareFileName =ASSET_FILE_NAME_ALTISERVO;
        if (rbAltiDuo.isChecked())
            firmwareFileName =ASSET_FILE_NAME_ALTIDUO;

        tvRead.setText("");
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
            mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int)this.dropdownBaudRate.getSelectedItemId()]));
             mPhysicaloid.upload(mSelectedBoard,getResources().getAssets().open(firmwareFileName), mUploadCallback);
        } catch (RuntimeException e) {
            //Log.e(TAG, e.toString());
        } catch (IOException e) {
            //Log.e(TAG, e.toString());
        }
    }


    Physicaloid.UploadCallBack mUploadCallback = new Physicaloid.UploadCallBack() {



        @Override
        public void onUploading(int value) {

            dialogAppend(getResources().getString(R.string.msg12)+value+" %");
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));

        }

        @Override
        public void onPostUpload(boolean success) {
            if(success) {
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
            tvAppend(tvRead, getResources().getString(R.string.msg18)+err.toString()+"\n");
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
        if(mPhysicaloid.close()) {

        }
    }

}
