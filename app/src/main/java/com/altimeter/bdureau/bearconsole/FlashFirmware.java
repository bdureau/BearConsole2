package com.altimeter.bdureau.bearconsole;


import android.os.Handler;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;

import android.widget.RadioButton;
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
    RadioButton rdbAltiMulti,rdbAltiMultiV2, rbAltiServo;
    TextView tvRead;

    private ArrayList<Boards> mBoardList;


    private static final String ASSET_FILE_NAME_ALTIMULTIV2       = "2019-12-26-V1_19_altimultiV2.ino.hex";
    private static final String ASSET_FILE_NAME_ALTIMULTI         = "2019-12-26-V1_19_altimulti.ino.hex";
    private static final String ASSET_FILE_NAME_ALTISERVO         = "2019-12-26-AltiServoV1_0.ino.hex";


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
        rdbAltiMulti.setChecked(true);
        mPhysicaloid = new Physicaloid(this);
                mBoardList = new ArrayList<Boards>();
        for(Boards board : Boards.values()) {
            if(board.support>0) {
                mBoardList.add(board);
            }
        }

        mSelectedBoard = mBoardList.get(0);
        UartConfig uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);
        btOpen.setEnabled(true);
        if(mPhysicaloid.open()) {
            mPhysicaloid.setConfig(uartConfig);

        } else {
            Toast.makeText(this, "Cannot open", Toast.LENGTH_LONG).show();
            //btOpen.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }
    public void onClickDismiss(View v) {
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


        //tvRead.clearComposingText();
        tvRead.setText("");
        try {

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
            tvAppend(tvRead, "Upload : "+value+" %\n");
        }

        @Override
        public void onPreUpload() {
            tvAppend(tvRead, "Upload : Start\n");
        }

        @Override
        public void onPostUpload(boolean success) {
            if(success) {
                tvAppend(tvRead, "Upload : Successful\n");
            } else {
                tvAppend(tvRead, "Upload fail\n");
            }
        }

        @Override
        public void onCancel() {
            tvAppend(tvRead, "Cancel uploading\n");
        }

        @Override
        public void onError(UploadErrors err) {
            tvAppend(tvRead, "Error  : "+err.toString()+"\n");
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


    private void close() {
        if(mPhysicaloid.close()) {

        }
    }

}
