package com.altimeter.bdureau.bearconsole.config;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.altimeter.bdureau.bearconsole.R;
import com.physicaloid.lib.Physicaloid;

public class Config3DR extends AppCompatActivity {
    Physicaloid mPhysicaloid;
    private String[] itemsBaudRate;
    private Spinner dropdownBaudRate;
    ModuleInfo mInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config3_dr);
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
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        mPhysicaloid = new Physicaloid(this);
        mInfo = new ModuleInfo(mPhysicaloid);
    }

    public void onClickDismiss(View v) {
        close();
        finish();
    }
    public void onClickRetrieveConfig(View v) {

    }
    public void onClickSaveConfig(View v) {

    }
    private void close() {
        if (mPhysicaloid.close()) {

        }
    }
    public void getAllInfo() {
        //format
        mInfo.getValue("ATS0?");
        //serial speed
        mInfo.getValue("ATS1?");
        //air speed
        mInfo.getValue("ATS2?");
        //net id
        mInfo.getValue("ATS3?");
        //TX power
        mInfo.getValue("ATS4?");
        //ECC
        mInfo.getValue("ATS5?");
        //Mav Link
        mInfo.getValue("ATS6?");
    }


    public long Connect(String [] baudrate) {
        long baud = 0;



        for (String rate : baudrate) {
            mInfo.open(Integer.valueOf(rate));
            if(mInfo.ATMode()) {
                baud = Integer.valueOf(rate);
                //mInfo.close();
                break;
            } else {
                mInfo.close();
            }
        }
        return baud;
    }

    public class ModuleInfo {
        Physicaloid lPhysicaloid;

        ModuleInfo( Physicaloid mPhysi) {
            lPhysicaloid = mPhysi;
        }

        public void open(int baudRate) {
            lPhysicaloid.open();
            lPhysicaloid.setBaudrate(baudRate);
        }

        public boolean ATMode() {
            boolean at = false;
            //fast reading
            drain();
            drain();
            byte[] cmd0 = "+++".getBytes();
            lPhysicaloid.write(cmd0, cmd0.length);


            byte buf[] = new byte[200];
            int retval = 0;

            retval = recv(buf, 200);

            if (retval > 0) {
                String str = null;

                str = new String(buf);

                if(str.equals("OK"))
                    at = true;
            }

            return at;
        }
        public String getValue(String command) {
            String value =null;
            command = command +"\n\r";
            drain();
            drain();
            byte[] cmd0 = command.getBytes();
            lPhysicaloid.write(cmd0, cmd0.length);


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
                retval = lPhysicaloid.read(buf, 1);
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
                retval = lPhysicaloid.read(tmpbuf, length);

                if (retval > 0) {
                    System.arraycopy(tmpbuf, 0, buf, totalRetval, retval);
                    totalRetval += retval;
                    startTime = System.currentTimeMillis();

                }
                if (totalRetval >= length) {
                    break;
                }

                endTime = System.currentTimeMillis();
                if ((endTime - startTime) > 250) {
                    break;
                }
            }
            return totalRetval;
        }

        public void close() {
            lPhysicaloid.close();
        }

    }

}
