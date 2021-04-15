package com.altimeter.bdureau.bearconsole.Flash;

import com.physicaloid.lib.Physicaloid;

public class FirmwareInfo {
    //UploadSTM32CallBack mUpCallback;
    Physicaloid lPhysicaloid;

    FirmwareInfo(/*UploadSTM32CallBack UpCallback,*/ Physicaloid mPhysi) {
        // mUpCallback = UpCallback;
        lPhysicaloid = mPhysi;
    }

    public void open(int baudRate) {
        lPhysicaloid.open();
        lPhysicaloid.setBaudrate(baudRate);
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

    public String getFirmwarVersion() {
        String version = "";
        drain();

        byte[] cmd = "b;".getBytes();
        lPhysicaloid.write(cmd, cmd.length);

        byte buf[]= new byte[200];
        int retval=0;

        //String buffer = "";
        retval =recv(buf, 200);

        if (retval > 0) {
            String str= null;

            str = new String(buf);

            //tvAppend(tvRead, "My String: " + str+"\n");
            String config[] = str.split(",");
            if (config.length > 9)
                version = config[8];
        }

        return version;
    }

    public void close() {
        lPhysicaloid.close();
    }
}

