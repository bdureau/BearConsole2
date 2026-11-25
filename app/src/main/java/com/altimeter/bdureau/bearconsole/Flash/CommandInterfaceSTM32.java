package com.altimeter.bdureau.bearconsole.Flash;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: CommandInterfaceSTM32
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Android port of the STM32 system bootloader protocol (stm32loader.py / stm32flash) used to
 *  flash the AltiMultiSTM32 altimeter through a USB OTG serial cable. It implements the
 *  bootloader handshake, the get / get-ID commands, the flash erase (standard and extended)
 *  and the memory write by 256 byte blocks with checksum.
 * ============================================================================
 */

import com.google.android.gms.common.util.IOUtils;
import com.physicaloid.lib.Physicaloid;

import java.io.IOException;
import java.io.InputStream;

import static com.physicaloid.misc.Misc.toHexStr;


public class CommandInterfaceSTM32 {
    UploadSTM32CallBack mUpCallback;
    Physicaloid mPhysicaloid;

    /** writeMemory() result: every block was written and acknowledged. */
    public static final int WRITE_OK = 1;
    /** writeMemory() result: no data, or a block was not acknowledged by the bootloader. */
    public static final int WRITE_FAILED = -1;
    /** writeMemory() result: stopped by cancel() between two blocks. */
    public static final int WRITE_CANCELLED = 0;

    // Set from the UI thread by cancel(), read by the flashing thread between two blocks
    private volatile boolean cancelRequested = false;

    /**
     * Requests the flashing to stop. writeMemory() finishes the current 256 byte block, then
     * returns WRITE_CANCELLED. The STM32 system bootloader is in ROM, so the board can always
     * be flashed again. The frames sent to the bootloader are not changed.
     */
    public void cancel() {
        cancelRequested = true;
    }

    /** @return true once cancel() has been called */
    public boolean isCancelRequested() {
        return cancelRequested;
    }

    CommandInterfaceSTM32(UploadSTM32CallBack UpCallback, Physicaloid mPhysi) {
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
            mUpCallback.onInfo(info+ " This is 0x1F");
            return -1;
        }
        else {
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
                    // Keep only the last got.length bytes: more than 10 bytes (repeated 0x1F,
                    // or data sent by a running firmware) used to overflow this array and
                    // crash the application
                    if (i < got.length) {
                        got[i] = buf[a];
                        i++;
                    } else {
                        System.arraycopy(got, 1, got, 0, got.length - 1);
                        got[got.length - 1] = buf[a];
                    }
                }
            }
            for (int j = 0; j <(i-1) ; j++ ) {
                if (got[j] == 0x79 && got[j+1] == 0x1f) {
                    return 1;
                }
            }

        }
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
            // Length announced by the bootloader, limited to the buffer size (a wrong byte
            // used to crash the application)
            int len = Math.min(buf[0] & 0xFF, buf.length);

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
            // Length announced by the bootloader, limited to the buffer size (a wrong byte
            // used to crash the application)
            int len = Math.min(buf[0] & 0xFF, buf.length - 1);
            recv(buf, len+1);
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
            // The block is valid only if the bootloader acknowledges the programming (0x79)
            return _wait_for_ack("0x31 programming failed " +
                    toHexStr(new byte[]{(byte) crc }, 1) + " lng: " +
                    toHexStr(new byte[]{(byte) lng }, 1),200);
        }else{
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

    /**
     * Writes the firmware by blocks of 256 bytes (last block padded with 0xFF).
     *
     * @return WRITE_OK if every block was acknowledged, WRITE_FAILED if the file could not be
     *         read or a block was not acknowledged (the writing goes on, as before, so that the
     *         log shows every failing block), WRITE_CANCELLED if cancel() was called
     */
    public int writeMemory(int addr, InputStream is) {
        int lng = -1;
        int tot = 0;

        byte[] data=null;

        try {
            data = IOUtils.toByteArray(is);
            lng = data.length;

        } catch (IOException e) {
            mUpCallback.onInfo("file not found: " +e.getMessage() + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            mUpCallback.onInfo("file not found: " +e.getMessage() + "\n");
        }

        // Nothing to write: never send an empty / invalid block
        if (data == null) return WRITE_FAILED;

        int offs = 0;
        // Number of blocks not acknowledged by the bootloader
        int failedBlocks = 0;

        tot = lng;


        while (lng > 256) {
            if (cancelRequested) {
                mUpCallback.onInfo("Flashing cancelled\n");
                return WRITE_CANCELLED;
            }
            byte buf[] = new byte[256];
            for (int i =0; i< 256; i++) {
                buf[i]= data[(i+offs)];
            }
            int ret = cmdWriteMemory(addr, buf);
            if(ret !=1) {
                failedBlocks++;
                mUpCallback.onInfo("error writing to mem:" + lng + "\n");
            }

            offs = offs + 256;
            addr = addr + 256;
            lng = lng - 256;
            mUpCallback.onUploading((offs*100)/tot);
        }
        byte buf2[] = new byte[256];
        for (int i =0; i< lng; i++) {
            buf2[i]= data[(i+offs)];
        }
        for (int i =lng; i< 256; i++) {
            buf2[i]= (byte)0xFF;
        }

        if (cancelRequested) {
            mUpCallback.onInfo("Flashing cancelled\n");
            return WRITE_CANCELLED;
        }
        if (cmdWriteMemory(addr, buf2) != 1) {
            failedBlocks++;
            mUpCallback.onInfo("error writing to mem: last block\n");
        }
        return failedBlocks == 0 ? WRITE_OK : WRITE_FAILED;
    }
}