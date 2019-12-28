package com.altimeter.bdureau.bearconsole;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String ASSET_FILE_NAME_ALTIMULTIV2       = "2019-12-26-V1_19_altimultiV2.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTI         = "2019-12-26-V1_19_altimulti.ino.hex";
    private static final String ASSET_FILE_NAME_ALTISERVO         = "2019-12-26-AltiServoV1_0.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIDUO         = "2019-12-27-AltiDuoV1_5_console.ino.hex";

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
    public void onClickFlash(View v) {
        String assetFileName;

        assetFileName = ASSET_FILE_NAME_ALTIMULTI;

        if (rdbAltiMulti.isChecked())
            assetFileName = ASSET_FILE_NAME_ALTIMULTI;
        if (rdbAltiMultiV2.isChecked())
            assetFileName = ASSET_FILE_NAME_ALTIMULTIV2;
        if (rbAltiServo.isChecked())
            assetFileName =ASSET_FILE_NAME_ALTISERVO;
        if (rbAltiDuo.isChecked())
            assetFileName =ASSET_FILE_NAME_ALTIDUO;


        //tvRead.clearComposingText();
        tvRead.setText("");
        try {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Retrieving flights...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                            dialog.cancel();
                            mPhysicaloid.cancelUpload();
                        }
                    });
            alert = builder.create();
            alert.show();
            mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int)this.dropdownBaudRate.getSelectedItemId()]));
             mPhysicaloid.upload(mSelectedBoard,getResources().getAssets().open(assetFileName), mUploadCallback);
        } catch (RuntimeException e) {
            //Log.e(TAG, e.toString());
        } catch (IOException e) {
            //Log.e(TAG, e.toString());
        }
    }


    Physicaloid.UploadCallBack mUploadCallback = new Physicaloid.UploadCallBack() {



        @Override
        public void onUploading(int value) {
            //tvAppend(tvRead, "Upload : "+value+" %\n");
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
