package com.altimeter.bdureau.bearconsole.Flash;
/**
 *   @description: This is used to flash the STM32 altimeter firmware from the Android device using an OTG cable
 *   so that the store Android application is compatible with the altimeter.
 *   Not that this is an Android port of the stm32loader.py done by myself as I could not find anything on the internet!!!
 *   This has been only tested with STM32F103 chips
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import com.google.android.gms.common.util.IOUtils;
import com.physicaloid.lib.Physicaloid;

import java.io.IOException;
import java.io.InputStream;

import static com.physicaloid.misc.Misc.toHexStr;


public class CommandInterface {
    UploadSTM32CallBack mUpCallback;
    Physicaloid mPhysicaloid;

    CommandInterface(UploadSTM32CallBack UpCallback, Physicaloid mPhysi) {
        mUpCallback = UpCallback;
        mPhysicaloid = mPhysi;
    }
    public void open (int baudRate) {
        mPhysicaloid.open();
        mPhysicaloid.setBaudrate(baudRate);
        mPhysicaloid.setParity(2); // 2 = parity even
        mPhysicaloid.setStopBits(1);
    }

    private byte[] intToBytes(final int data) {
        return new byte[] {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 0) & 0xff),
        };
    }

    public int drain() {
        byte[] buf = new byte[1];
        int retval = 0;
        long endTime;
        long startTime = System.currentTimeMillis();
        while(true) {
            retval = mPhysicaloid.read(buf,1);
            if(retval > 0) {

            }
            endTime = System.currentTimeMillis();
            if((endTime - startTime) > 1000) {break;}
        }
        return retval;
    }
    private int recv(byte[] buf, int length) {
        int retval=0;
        int totalRetval=0;
        long endTime;
        long startTime = System.currentTimeMillis();
        byte[] tmpbuf = new byte[length];

        while(true) {
            retval = mPhysicaloid.read(tmpbuf,length);

            if(retval > 0) {
                System.arraycopy(tmpbuf, 0, buf, totalRetval, retval);
                totalRetval += retval;
                startTime = System.currentTimeMillis();

            }
            if(totalRetval >= length){break;}

            endTime = System.currentTimeMillis();
            if((endTime - startTime) > 250) {
                break;
            }
        }
        return totalRetval;
    }
    private int _wait_for_ack(String info, long timeout) {
        long stop = System.currentTimeMillis() + timeout;
        byte got[] =new byte[1];
        while (mPhysicaloid.read(got) <1) {
            if (System.currentTimeMillis() > stop)
                break;
        }
        if (got[0] == 0x79)
            return 1;
        else if (got[0] == 0x1F) {
            //tvAppend(tvRead,info+ " This is 0x1F");
            mUpCallback.onInfo(info+ " This is 0x1F");
            return -1;
        }
        else {
            //tvAppend(tvRead,info + " Not 0x79");
            mUpCallback.onInfo(info+ "This is Not 0x79");
            return -1;
        }
    }

    public void reset() {
        mPhysicaloid.setDtrRts(false, false);
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        mPhysicaloid.setDtrRts(true, false);
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }

    public int initChip() {
        drain();
        mPhysicaloid.setDtrRts(true, true);
        //set boot
        mPhysicaloid.setDtrRts(true, false);
        reset();
        long stop = System.currentTimeMillis() + 5000;
        byte got[] = new byte[10];
        int i=0;
        while (System.currentTimeMillis() <= stop) {
            int retval=0;

            byte buf[]= new byte[32];
            mPhysicaloid.write(new byte[]{(byte) 0x7f});
            retval =recv(buf, 32);
            if (retval > 0) {
                for (int a=0; a <retval; a++) {
                    got[i] = buf[a];
                    i++;
                }
            }
            for (int j = 0; j <(i-1) ; j++ ) {
                if (got[j] == 0x79 && got[j+1] == 0x1f) {
                    return 1;
                }
            }

        }
        //tvAppend(tvRead, " got:" + toHexStr(got, i));
        mUpCallback.onInfo(" got:" + toHexStr(got, i));
        return -1;
    }

    public void releaseChip() {
        mPhysicaloid.setDtrRts(false, true);
        reset();
    }

    private int cmdGeneric(byte [] cmd) {

        mPhysicaloid.write(cmd);
        mPhysicaloid.write(new byte[]{(byte) (cmd[0] ^ 0xFF)});
        return _wait_for_ack(toHexStr(cmd,1),50);
    }

    public int cmdGet () {
        byte cmd []= new byte[1];
        byte buf[] =new byte[50];
        int version = -1;
        drain();
        cmd[0]=0x00;
        if (cmdGeneric(cmd)==1) {
            recv(buf, 1);
            int len =(int)buf[0];

            recv(buf, 1);
            version = (int)buf[0];
            recv(buf, len);

            mUpCallback.onInfo("version:" + version + "\n");

            _wait_for_ack("0x00 end",500);
        }
        return version;
    }


    public int cmdGetVersion () {
        byte cmd []= new byte[1];
        byte buf[] =new byte[1];
        int version = -1;
        cmd[0]=0x01;
        if (cmdGeneric(cmd)==1) {
            recv(buf, 1);
            version = (int)buf[0];
            _wait_for_ack("0x01 end",500);
        }
        return version;
    }
    public byte[] cmdGetID () {

        byte cmd []= new byte[1];
        byte buf[] =new byte[50];
        int id = -1;
        cmd[0]=0x02;
        if (cmdGeneric(cmd)==1) {
            recv(buf, 1);
            int len = (int)buf[0];
            //tvAppend(tvRead, "id all:" + toHexStr(buf, len) + "\n");
            recv(buf, len+1);
            //tvAppend(tvRead, "id all:" + toHexStr(buf, len));
            id = (int)buf[0];
            _wait_for_ack("0x02 end",500);
        }
        return buf;
    }

    private byte [] _encode_addr (int addr) {
        return new byte[]{
                (byte) ((addr >> 24) & 0xff),
                (byte) ((addr >> 16) & 0xff),
                (byte) ((addr >> 8) & 0xff),
                (byte) ((addr >> 0) & 0xff),
                (byte) ((byte) ((addr >> 24) & 0xff)^(byte) ((addr >> 16) & 0xff)^(byte) ((addr >> 8) & 0xff)^(byte) ((addr >> 0) & 0xff))
        };
    }

    private int cmdReadMemory (int addr, int lng) {
        byte cmd []= new byte[1];
        cmd [0]=0x11;
        if(cmdGeneric(cmd)==1) {
            mPhysicaloid.write(_encode_addr(addr));
            _wait_for_ack("0x11 address failed",0);
            int N = (lng -1) & 0xFF;
            int crc = N ^ 0xFF;
            byte cmd2[] = new byte[2];
            cmd2[0] = (byte) N;
            cmd2[1] = (byte) crc;
            mPhysicaloid.write(cmd2);
            _wait_for_ack("0x11 length failed",0);
            return 1;
        } else {
            return -1;
        }
    }

    public int cmdGo (int addr) {
        byte cmd []= new byte[1];
        cmd [0]=0x21;
        if(cmdGeneric(cmd)==1) {
            mPhysicaloid.write(_encode_addr(addr));
            _wait_for_ack("0x21 go failed",100);
            return 1;
        }
        else {
            return - 1;
        }
    }

    //writeMemory
    private int cmdWriteMemory (int addr,byte buf[]) {
        int lng=0;

        byte cmd []= new byte[1];
        cmd [0]=0x31;
        if(cmdGeneric(cmd)==1) {
            int ret = mPhysicaloid.write(_encode_addr(addr));
            _wait_for_ack("0x31 address failed",100);
            lng = (buf.length - 1) & 0xFF;

            mPhysicaloid.write(new byte[]{(byte) lng });
            byte crc = (byte) 0xFF;
            for (byte c : buf) {
                crc = (byte) (crc ^ c);
                mPhysicaloid.write(new byte[]{ c });
            }

            mPhysicaloid.write(new byte[]{ crc });
            _wait_for_ack("0x31 programming failed " +
                    toHexStr(new byte[]{(byte) crc }, 1) + " lng: " +
                    toHexStr(new byte[]{(byte) lng }, 1),200);
            return 1;
        }else{
            //tvAppend(tvRead, "*** Write memory command failed"  + "\n");
            mUpCallback.onInfo("*** Write memory command failed"  + "\n");
            return -1;
        }

    }

    public int cmdEraseMemory ( ) {

        byte cmd []= new byte[1];
        cmd [0]=0x43;
        if(cmdGeneric(cmd)==1) {
            cmd[0]= (byte) 0xFF;//-1;//0xFF in unsigned byte
            mPhysicaloid.write(cmd);
            cmd[0]= 0x00;
            mPhysicaloid.write(cmd);
            _wait_for_ack("0x43 erasing failed", 50);
            return 1;
        } else {
            return -1;
        }
    }

    public int cmdExtendedEraseMemory () {
        byte cmd []= new byte[1];
        cmd [0]=0x44;
        if(cmdGeneric(cmd)==1) {
            cmd[0]= (byte) 0xFF;//-1;//0xFF in unsigned byte
            mPhysicaloid.write(cmd);
            mPhysicaloid.write(cmd);
            cmd[0]= 0x00;
            mPhysicaloid.write(cmd);
            _wait_for_ack("0x44 extended erase failed",20000);
            return 1;
        }
        else {
            return -1;
        }

    }

    private int cmdWriteProtect () {
        byte cmd []= new byte[1];
        cmd [0]=0x63;
        if(cmdGeneric(cmd)==1) {

        }
        return 1;
    }

    private int cmdWriteUnprotect () {

        return 1;
    }

    private int cmdReadoutProtect () {

        return 1;
    }

    private int readMemory () {

        return 1;
    }

    public int writeMemory(int addr, InputStream is) {
        int lng = -1;
        int tot = 0;

        byte[] data=null;

        try {
            data = IOUtils.toByteArray(is);
            lng = data.length;

        } catch (IOException e) {
            //e.printStackTrace();
            //tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_ALTIMULTISTM32+ "\n");
            mUpCallback.onInfo("file not found: " +e.getMessage() + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            //tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_ALTIMULTISTM32+ "\n");
            mUpCallback.onInfo("file not found: " +e.getMessage() + "\n");
        }


        //tvAppend(tvRead, "lng:" + lng + "\n");
        int offs = 0;

        tot = lng;

        while (lng > 256) {
            byte buf[] = new byte[256];
            for (int i =0; i< 256; i++) {
                buf[i]= data[(i+offs)];
            }
            int ret = cmdWriteMemory(addr, buf);
            if(ret !=1)
                mUpCallback.onInfo("error writing to mem:" + lng + "\n");
                //tvAppend(tvRead, "error writing to mem:" + lng + "\n");
            //mUpCallback.onPreUpload("error writing to mem:" + lng + "\n");

            offs = offs + 256;
            addr = addr + 256;
            lng = lng - 256;
            //dialogAppend(((offs*100)/tot)+"%" );
            mUpCallback.onUploading((offs*100)/tot);
        }
        byte buf2[] = new byte[256];
        for (int i =0; i< lng; i++) {
            buf2[i]= data[(i+offs)];
        }
        for (int i =lng; i< 256; i++) {
            buf2[i]= (byte)0xFF;
        }

        cmdWriteMemory(addr, buf2);
        return 1;
    }
}