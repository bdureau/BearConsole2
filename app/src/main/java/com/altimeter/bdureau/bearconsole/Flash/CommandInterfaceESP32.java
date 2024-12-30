package com.altimeter.bdureau.bearconsole.Flash;
/**
 *   @description: This is used to flash the ESP32 altimeter firmware from the Android device using an OTG cable
 *   so that the store Android application is compatible with the altimeter.
 *   Note that this is an Android port of the ESPLoader.py done by myself
 *   as I could not find anything on the internet!!!
 *   It uses the Physicaloid library but should be easy to use with any other USB library
 *   It could also be written in pure Java so that it could be integrated with the Arduino env
 *   This has been only tested with ESP32 chips. Feel free to re-use it for your own project
 *   Please make sure that you do report any bugs so that I can fix them
 *   So far it is reliable enough for me
 *
 *   @author: boris.dureau@neuf.fr
 *
 *  Note: the flashing is very slow due to the delay en the send command. This needs to be improved
 *  Also the reset() and enterBootLoader() do not currently work, you will need to enter the bootloader mode using the reset
 *  and boot buttons on the board
 **/
import android.util.Log;

import com.physicaloid.lib.Physicaloid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;



public class CommandInterfaceESP32 {
    public static int ESP_FLASH_BLOCK = 0x400;
    public String TAG = "CommandInterfaceESP32";
    private static final int ESP_ROM_BAUD = 115200;
    private static final int FLASH_WRITE_SIZE = 0x400;
    private static final int STUBLOADER_FLASH_WRITE_SIZE = 0x4000;
    private static final int FLASH_SECTOR_SIZE = 0x1000; // Flash sector size, minimum unit of erase.

    private static final int CHIP_DETECT_MAGIC_REG_ADDR = 0x40001000;
    public static final int ESP8266 = 0x8266;
    public static final int ESP32 = 0x32;
    public static final int ESP32S2 = 0x3252;
    public static final int ESP32S3 = 0x3253;
    public static final int ESP32H2 = 0x3282;
    public static final int ESP32C2 = 0x32C2;
    public static final int ESP32C3 = 0x32C3;
    public static final int ESP32C6 = 0x32C6;
    private static final int ESP32_DATAREGVALUE = 0x15122500;
    private static final int ESP8266_DATAREGVALUE = 0x00062000;
    private static final int ESP32S2_DATAREGVALUE = 0x500;

    private static final int BOOTLOADER_FLASH_OFFSET = 0x1000;
    private static final int ESP_IMAGE_MAGIC = 0xe9;

    // Commands supported by ESP8266 ROM bootloader
    private static final int ESP_FLASH_BEGIN = 0x02;
    private static final int ESP_FLASH_DATA = 0x03;
    private static final int ESP_FLASH_END = 0x04;
    private static final int ESP_MEM_BEGIN = 0x05;
    private static final int ESP_MEM_END = 0x06;
    private static final int ESP_MEM_DATA = 0x07;
    private static final int ESP_SYNC = 0x08;
    private static final int ESP_WRITE_REG = 0x09;
    private static final int ESP_READ_REG = 0x0A;

    // Some comands supported by ESP32 ROM bootloader (or -8266 w/ stub)
    private static final int ESP_SPI_SET_PARAMS = 0x0B; // 11
    private static final int ESP_SPI_ATTACH = 0x0D; // 13
    private static final int ESP_READ_FLASH_SLOW = 0x0E; // 14 // ROM only, much slower than the stub flash read
    private static final int ESP_CHANGE_BAUDRATE = 0x0F; // 15
    private static final int ESP_FLASH_DEFL_BEGIN = 0x10; // 16
    private static final int ESP_FLASH_DEFL_DATA = 0x11; // 17
    private static final int ESP_FLASH_DEFL_END = 0x12; // 18
    private static final int ESP_SPI_FLASH_MD5 = 0x13; // 19

    // Commands supported by ESP32-S2/S3/C3/C6 ROM bootloader only
    private static final int ESP_GET_SECURITY_INFO = 0x14;

    // Some commands supported by stub only
    private static final int ESP_ERASE_FLASH = 0xD0;
    private static final int ESP_ERASE_REGION = 0xD1;
    private static final int ESP_READ_FLASH = 0xD2;
    private static final int ESP_RUN_USER_CODE = 0xD3;

    // Response code(s) sent by ROM
    private static final int ROM_INVALID_RECV_MSG = 0x05;

    // Initial state for the checksum routine
    private static final byte ESP_CHECKSUM_MAGIC = (byte) 0xEF;

    private static final int UART_DATE_REG_ADDR = 0x60000078;

    private static final int USB_RAM_BLOCK = 0x800;
    private static final int ESP_RAM_BLOCK = 0x1800;

    // Timeouts
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int CHIP_ERASE_TIMEOUT = 120000; // timeout for full chip erase in ms
    private static final int MAX_TIMEOUT = CHIP_ERASE_TIMEOUT * 2; // longest any command can run in ms
    private static final int SYNC_TIMEOUT = 100; // timeout for syncing with bootloader in ms
    private static final int ERASE_REGION_TIMEOUT_PER_MB = 30000; // timeout (per megabyte) for erasing a region in ms
    private static final int MEM_END_ROM_TIMEOUT = 500;
    private static final int MD5_TIMEOUT_PER_MB = 8000;
    private static int chip;

    private static boolean IS_STUB = false;

    static class cmdRet {
        int retCode;
        byte retValue[] = new byte[512];

    }
    UploadSTM32CallBack mUpCallback;
    Physicaloid mPhysicaloid;

    CommandInterfaceESP32(UploadSTM32CallBack UpCallback, Physicaloid mPhysi) {
        mUpCallback = UpCallback;
        mPhysicaloid = mPhysi;
    }


    /*
    *  Init chip
    *
    */
    public boolean initChip() {
        boolean syncSuccess = false;
        // initalize at 115200 bauds
        mPhysicaloid.open();
        mPhysicaloid.setBaudrate(115200);

        mPhysicaloid.setParity(0); // 2 = parity even
        mPhysicaloid.setStopBits(1);

        drain();

        // let's put the ship in boot mode
        enterBootLoader();

        drain();

        mUpCallback.onInfo("Sync"  + "\n");
        // first do the sync
        for (int i = 0; i < 6; i++) {
            mUpCallback.onInfo("Sync attempt:"  + (i+1)+ "\n");
            if (sync() == 0) {

            } else {
                syncSuccess = true;
                mUpCallback.onInfo("Sync Success!!!"  + "\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                break;
            }
        }
        return syncSuccess;
    }

    public void changeBaudeRate() {
        byte pkt[] = _appendArray(_int_to_bytearray(921600),_int_to_bytearray(0));
        sendCommand((byte) ESP_CHANGE_BAUDRATE, pkt, 0, 100);

        // second we change the comport baud rate
        mPhysicaloid.setBaudrate(921600);
        mUpCallback.onInfo("Changing baud rate to 921600"  + "\n");
        // let's wait
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        // flush anything on the port
        drain();
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

    /*
     * This will initialise the chip
     */
    public int sync() {
        int x;
        int response = 0;
        byte cmddata[] = new byte[36];

        cmddata[0] = (byte) (0x07);
        cmddata[1] = (byte) (0x07);
        cmddata[2] = (byte) (0x12);
        cmddata[3] = (byte) (0x20);
        for (x = 4; x < 36; x++) {
            cmddata[x] = (byte) (0x55);
        }

        for (x = 0; x < 7; x++) {
            cmdRet ret = sendCommand((byte) ESP_SYNC, cmddata, 0, 100);
            if (ret.retCode == 1) {
                response = 1;
                break;
            }
        }
        return response;
    }

    /*
     * This will send a command to the chip
     */
    public cmdRet sendCommand(byte opcode, byte buffer[], int chk, int timeout) {

        cmdRet retVal = new cmdRet();
        int i = 0;
        byte data[] = new byte[8 + buffer.length];
        data[0] = 0x00;
        data[1] = opcode;
        data[2] = (byte) ((buffer.length) & 0xFF);
        data[3] = (byte) ((buffer.length >> 8) & 0xFF);
        data[4] = (byte) ((chk & 0xFF));
        data[5] = (byte) ((chk >> 8) & 0xFF);
        data[6] = (byte) ((chk >> 16) & 0xFF);
        data[7] = (byte) ((chk >> 24) & 0xFF);

        for (i = 0; i < buffer.length; i++) {
            data[8 + i] = buffer[i];
        }

        // System.out.println("opcode:"+opcode);
        int ret = 0;
        retVal.retCode = 0;
        byte buf[] = slipEncode(data);

        ret = mPhysicaloid.write(buf, buf.length);
        try { Thread.sleep(5); } catch (InterruptedException e) {}
        
        int numRead = 0;
        
        for (i = 0; i < 10; i++) {
            numRead = recv(retVal.retValue, retVal.retValue.length, timeout/5);
            if (numRead == 0) {
                retVal.retCode = -1;
                continue;
            } else if (numRead == -1) {
                retVal.retCode = -1;
                continue;
            }

            if (retVal.retValue[0] != (byte) 0xC0) {
                //mUpCallback.onInfo("invalid packet\n");
                // System.out.println("Packet: " + printHex(retVal.retValue));
                retVal.retCode = -1;
                continue;
            }

            if (retVal.retValue[0] == (byte) 0xC0) {
                mUpCallback.onInfo("This is correct!!!\n");
                // System.out.println("Packet: " + printHex(retVal.retValue));
                retVal.retCode = 1;
                break;
            }
        }

        return retVal;
    }
    private int recv(byte[] buf, int length, long timeout) {
        int retval = 0;
        int totalRetval = 0;
        long endTime;
        long startTime = System.currentTimeMillis();
        byte[] tmpbuf = new byte[length];

        while (true) {
            retval = mPhysicaloid.read(tmpbuf, length);

            if (retval > 0) {
                System.arraycopy(tmpbuf, 0, buf, totalRetval, retval);
                totalRetval += retval;
                startTime = System.currentTimeMillis();
                //if (DEBUG_SHOW_RECV) {
                //    Log.d(TAG, "recv(" + retval + ") : " + toHexStr(buf, totalRetval));
                //}
            }
            
            if (totalRetval >= 8) { 
                break;
            }

            endTime = System.currentTimeMillis();
            if ((endTime - startTime) > timeout) {
                //Log.e(TAG, "recv timeout.");
                break;
            }
        }
        return retval;
    }
    /*private int _wait_for_ack( long timeout, byte readVal[]) {
        long stop = System.currentTimeMillis() + timeout;
        byte got[] =new byte[1];
        while (mPhysicaloid.read(got) <1) {
            if (System.currentTimeMillis() > stop)
                break;
        }
        if (got[0] == (byte) 0xC0) {
            mUpCallback.onInfo("We have a start byte\n");
            byte readByte[] = new byte[1];
            readByte[0] = got[0];
            while(mPhysicaloid.read(got,1)>0) {
                mUpCallback.onInfo("We have another one\n");
                readByte =_appendArray(readByte,got);
                if (System.currentTimeMillis() > stop)
                    break;
            }
            mUpCallback.onInfo(readByte.toString());
            readVal = readByte;
            return readVal.length;
        }
        else {
            //tvAppend(tvRead,info + " Not 0x79");
            //mUpCallback.onInfo(info+ "This is Not 0x79");
            mUpCallback.onInfo("not receiving!!!\n");
            return 0;
        }
    }*/
    /*
     * This will do a SLIP encode
     */
    public byte[] slipEncode(byte buffer[]) {
        byte encoded[] = new byte[] {(byte) (0xC0)};

        for (int x = 0; x < buffer.length; x++) {
            if (buffer[x] == (byte) (0xC0)) {
                encoded = _appendArray(encoded, new byte[] {(byte) (0xDB)});
                encoded = _appendArray(encoded, new byte[] {(byte) (0xDC)});

            } else if (buffer[x] == (byte) (0xDB)) {
                encoded = _appendArray(encoded, new byte[] {(byte) (0xDB)});
                encoded = _appendArray(encoded, new byte[] {(byte) (0xDD)});
            } else {
                encoded = _appendArray(encoded,new byte[] {buffer[x]});
            }
        }
        encoded = _appendArray(encoded,new byte[] {(byte) (0xC0)});

        return encoded;
    }

    /*
     * This does a reset in order to run the prog after flash
     * currently does not work on the Android port
     */
    public void reset() {
        
        mPhysicaloid.setDtrRts(false, true);
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        mPhysicaloid.setDtrRts(true, false);

    }

    /*
     * enter bootloader mode
     * does not currently work on Android
     */
    public void enterBootLoader() {
        // reset bootloader

        mPhysicaloid.setDtrRts(false, true);
        mUpCallback.onInfo("Entering bootloader mode"  + "\n");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        mPhysicaloid.setDtrRts(true, false);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        mPhysicaloid.setDtrRts(false, false);
    }

    /**
     * @name flash_defl_block Send one compressed block of data to program into SPI Flash memory
     */

    public cmdRet flash_defl_block(byte data[], int seq, int timeout) {
        cmdRet retVal;
        byte pkt[] = _appendArray(_int_to_bytearray(data.length),_int_to_bytearray(seq));
        pkt = _appendArray(pkt,_int_to_bytearray(0));
        pkt = _appendArray(pkt,_int_to_bytearray(0));
        pkt = _appendArray(pkt, data);

        retVal = sendCommand((byte) ESP_FLASH_DEFL_DATA, pkt, _checksum(data), timeout);
        return retVal;
    }

    public cmdRet flash_block(byte data[], int seq, int timeout) {
        cmdRet retVal;
        byte pkt[] = _appendArray(_int_to_bytearray(data.length),_int_to_bytearray(seq));
        pkt = _appendArray(pkt,_int_to_bytearray(0));
        pkt = _appendArray(pkt,_int_to_bytearray(0));
        pkt = _appendArray(pkt, data);

        retVal = sendCommand((byte) ESP_FLASH_DATA, pkt, _checksum(data), timeout);
        return retVal;
    }

    public void init() {

        int _flashsize = 4 * 1024 * 1024;

        if (!IS_STUB) {
            //System.out.println("No stub...");
            byte pkt[] = _appendArray(_int_to_bytearray(0),_int_to_bytearray(0));
            sendCommand((byte) ESP_SPI_ATTACH, pkt, 0, 100);
        }

        // We are hardcoding 4MB flash for an ESP32
        //System.out.println("Configuring flash size...");
        mUpCallback.onInfo("Configuring flash size..."  + "\n");

        byte pkt2[] = _appendArray(_int_to_bytearray(0),_int_to_bytearray(_flashsize));
        pkt2 = _appendArray(pkt2,_int_to_bytearray(0x10000));
        pkt2 = _appendArray(pkt2,_int_to_bytearray(4096));
        pkt2 = _appendArray(pkt2,_int_to_bytearray(256));
        pkt2 = _appendArray(pkt2,_int_to_bytearray(0xFFFF));

        sendCommand((byte) ESP_SPI_SET_PARAMS, pkt2, 0, 100);

    }


    /**
     * @name flashData Program a full, uncompressed binary file into SPI Flash at a
     *       given offset. If an ESP32 and md5 string is passed in, will also verify
     *       memory. ESP8266 does not have checksum memory verification in ROM
     */
    public void flashCompressedData(byte binaryData[], int offset, int part) {
        int filesize = binaryData.length;

        mUpCallback.onInfo("\nWriting data with filesize: " + filesize);

        byte image[] = compressBytes(binaryData);
        int blocks = flash_defl_begin(filesize, image.length, offset);

        int seq = 0;
        int written = 0;
        int address = offset;
        int position = 0;

        long t1 = System.currentTimeMillis();

        while (image.length - position > 0) {

            double percentage = Math.floor((double)(100 * (seq + 1)) / (double)blocks);

            mUpCallback.onInfo("percentage: " + percentage + "\n");
            mUpCallback.onUploading( (int) percentage);

            byte block[];

            if (image.length - position >= FLASH_WRITE_SIZE) {
                block = _subArray(image, position, FLASH_WRITE_SIZE);
            } else {
                // Pad the last block
                block = _subArray(image, position, image.length - position);

                // we have an incomplete block (ie: less than 1024) so let pad the missing block
                // with 0xFF
                /*byte tempArray[] = new byte[FLASH_WRITE_SIZE - block.length];
                for (int i = 0; i < tempArray.length; i++) {
                    tempArray[i] = (byte) 0xFF;
                }
                block = _appendArray(block, tempArray);*/
            }

            cmdRet retVal = flash_defl_block(block, seq, 100);
            if (retVal.retCode ==-1) {
                //This should fix issue when writing is incorrect by trying again
                mUpCallback.onInfo("Retry because Ret code:" + retVal.retCode +"\n");
                retVal = flash_defl_block(block, seq, /*block_timeout*/ 100);
            }
            seq += 1;
            written += block.length;
            position += FLASH_WRITE_SIZE;
        }

        long t2 = System.currentTimeMillis();
        mUpCallback.onInfo("Took " + (t2 - t1) + "ms to write " + filesize + " bytes" + "\n");
    }
    /*
    Flash uncompressed data
     */
    public void flashData(byte binaryData[], int offset, int part) {
        int filesize = binaryData.length;

        mUpCallback.onInfo("\nWriting data with filesize: " + filesize);

        byte image[] = binaryData;
        int blocks = flash_begin(filesize, image.length, offset);

        int seq = 0;
        int written = 0;
        int address = offset;
        int position = 0;

        long t1 = System.currentTimeMillis();

        while (image.length - position > 0) {

            double percentage = Math.floor((double)(100 * (seq + 1)) / (double)blocks);

            mUpCallback.onInfo("percentage: " + percentage + "\n");
            mUpCallback.onUploading( (int) percentage);

            byte block[];

            if (image.length - position >= FLASH_WRITE_SIZE) {
                block = _subArray(image, position, FLASH_WRITE_SIZE);
            } else {
                // Pad the last block
                block = _subArray(image, position, image.length - position);

                // we have an incomplete block (ie: less than 1024) so let pad the missing block
                // with 0xFF
                /*byte tempArray[] = new byte[FLASH_WRITE_SIZE - block.length];
                for (int i = 0; i < tempArray.length; i++) {
                    tempArray[i] = (byte) 0xFF;
                }
                block = _appendArray(block, tempArray);*/
            }

            cmdRet retVal = flash_block(block, seq, 100);
            if (retVal.retCode ==-1) {
                //This should fix issue when writing is incorrect by trying again
                mUpCallback.onInfo("Retry because Ret code:" + retVal.retCode +"\n");
                retVal = flash_block(block, seq, /*block_timeout*/ 100);
            }
            seq += 1;
            written += block.length;
            position += FLASH_WRITE_SIZE;
        }

        long t2 = System.currentTimeMillis();
        mUpCallback.onInfo("Took " + (t2 - t1) + "ms to write " + filesize + " bytes" + "\n");
    }

    private int flash_defl_begin(int size, int compsize, int offset) {

        int num_blocks = (int) Math.floor((double) (compsize + FLASH_WRITE_SIZE - 1) / (double) FLASH_WRITE_SIZE);
        int erase_blocks = (int) Math.floor((double) (size + FLASH_WRITE_SIZE - 1) / (double) FLASH_WRITE_SIZE);
        // Start time
        long t1 = System.currentTimeMillis();

        int write_size, timeout;
        if (IS_STUB) {
            //using a stub (will use it in the future)
            write_size = size;
            timeout = 3000;
        } else {
            write_size = erase_blocks * FLASH_WRITE_SIZE;
            timeout = timeout_per_mb(ERASE_REGION_TIMEOUT_PER_MB, write_size);
        }

        mUpCallback.onInfo("Compressed " + size + " bytes to " + compsize + "..."+ "\n");

        byte pkt[] = _appendArray(_int_to_bytearray(write_size), _int_to_bytearray(num_blocks));
        pkt = _appendArray(pkt, _int_to_bytearray(FLASH_WRITE_SIZE));
        pkt = _appendArray(pkt, _int_to_bytearray(offset));
        if (chip == ESP32S3 || chip == ESP32C2|| chip == ESP32C3 || chip == ESP32C6 || chip == ESP32S2 || chip == ESP32H2 )
            pkt = _appendArray(pkt, _int_to_bytearray(0));


        sendCommand((byte) ESP_FLASH_DEFL_BEGIN, pkt, 0, timeout);

        // end time
        long t2 = System.currentTimeMillis();
        if (size != 0 && IS_STUB == false) {
            System.out.println("Took " + ((t2 - t1) / 1000) + "." + ((t2 - t1) % 1000) + "s to erase flash block");
            mUpCallback.onInfo("Took " + ((t2 - t1) / 1000) + "." + ((t2 - t1) % 1000) + "s to erase flash block\n");
        }
        return num_blocks;
    }

    private int flash_begin(int size, int compsize, int offset) {

        int num_blocks = (int) Math.floor((double) (compsize + FLASH_WRITE_SIZE - 1) / (double) FLASH_WRITE_SIZE);
        int erase_blocks = (int) Math.floor((double) (size + FLASH_WRITE_SIZE - 1) / (double) FLASH_WRITE_SIZE);
        // Start time
        long t1 = System.currentTimeMillis();

        int write_size, timeout;
        if (IS_STUB) {
            //using a stub (will use it in the future)
            write_size = size;
            timeout = 3000;
        } else {
            write_size = erase_blocks * FLASH_WRITE_SIZE;
            timeout = timeout_per_mb(ERASE_REGION_TIMEOUT_PER_MB, write_size);
        }

        mUpCallback.onInfo("Compressed " + size + " bytes to " + compsize + "..."+ "\n");

        byte pkt[] = _appendArray(_int_to_bytearray(write_size), _int_to_bytearray(num_blocks));
        pkt = _appendArray(pkt, _int_to_bytearray(FLASH_WRITE_SIZE));
        pkt = _appendArray(pkt, _int_to_bytearray(offset));
        if (chip == ESP32S3 || chip == ESP32C2 || chip == ESP32C3 || chip == ESP32C6 || chip == ESP32S2 || chip == ESP32H2 )
            pkt = _appendArray(pkt, _int_to_bytearray(0));


        sendCommand((byte) ESP_FLASH_BEGIN, pkt, 0, timeout);

        // end time
        long t2 = System.currentTimeMillis();
        if (size != 0 && IS_STUB == false) {
            System.out.println("Took " + ((t2 - t1) / 1000) + "." + ((t2 - t1) % 1000) + "s to erase flash block");
            mUpCallback.onInfo("Took " + ((t2 - t1) / 1000) + "." + ((t2 - t1) % 1000) + "s to erase flash block\n");
        }
        return num_blocks;
    }

    /*
     * Send a command to the chip to find out what type it is
     * This is usefull for sending specific commands
     */
    public int detectChip() {
        int chipMagicValue =readRegister(CHIP_DETECT_MAGIC_REG_ADDR);
        Log.d(TAG, "chipMagicValue:" + chipMagicValue);
        int ret = 0;
        if (chipMagicValue == 0xfff0c101)
            ret = ESP8266;
        if (chipMagicValue == 0x00f01d83)
            ret = ESP32;
        if (chipMagicValue == 0x000007c6)
            ret = ESP32S2;
        if ((chipMagicValue == 0x9) | (chipMagicValue == 538052359))
            ret = ESP32S3;
        if (chipMagicValue == 0x6f51306f)
            ret = ESP32C2;
        if ((chipMagicValue == 0x6921506f) | (chipMagicValue == 0x1b31506f))
            ret = ESP32C3;
        if (chipMagicValue == 0x0da1806f)
            ret = ESP32C6;
        if (chipMagicValue == 0xca26cc22)
            ret = ESP32H2;
        chip = ret;
        return ret;
    }
    ////////////////////////////////////////////////
    // Some utility functions
    ////////////////////////////////////////////////

    /*
     * Just usefull for debuging to check what I am sending or receiving
     */
    public String printHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    /*
     * This takes 2 arrays as params and return a concatenate array
     */
    private byte[] _appendArray(byte arr1[], byte arr2[]) {

        byte c[] = new byte[arr1.length + arr2.length];

        for (int i = 0; i < arr1.length; i++) {
            c[i] = arr1[i];
        }
        for (int j = 0; j < arr2.length; j++) {
            c[arr1.length + j] = arr2[j];
        }
        return c;
    }

    /*
     * get part of an array
     */
    private byte[] _subArray(byte arr1[], int pos, int length) {

        byte c[] = new byte[length];

        for (int i = 0; i < (length); i++) {
            c[i] = arr1[i + pos];
        }
        return c;
    }

    /*
     * Calculate the checksum.
     */
    public int _checksum(byte[] data) {
        int chk = ESP_CHECKSUM_MAGIC;
        int x = 0;
        for (x = 0; x < data.length; x++) {
            chk ^= data[x];
        }
        return chk;
    }

    public int read_reg(int addr, int timeout) {
        cmdRet val;
        byte pkt[] = _int_to_bytearray(addr);
        val = sendCommand((byte)ESP_READ_REG, pkt, 0, timeout);
        return val.retValue[0];
    }

    /**
     * @name readRegister Read a register within the ESP chip RAM, returns a
     *       4-element list
     */
    public int readRegister(int reg) {

        long retVals[] = { 0 };
        //int retVals = 0;
        cmdRet ret;

        Struct struct = new Struct();

        try {

            byte packet[] = _int_to_bytearray(reg);

            ret = sendCommand((byte) ESP_READ_REG, packet, 0, 0);
            Struct myRet = new Struct();

            byte subArray[] = new byte[4];
            subArray[0] = ret.retValue[5];
            subArray[1] = ret.retValue[6];
            subArray[2] = ret.retValue[7];
            subArray[3] = ret.retValue[8];

            retVals = myRet.unpack("I", subArray);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return (int) retVals[0];
    }



    /**
     * @name timeoutPerMb Scales timeouts which are size-specific
     */
    private  int timeout_per_mb(int seconds_per_mb, int size_bytes) {
        int result = seconds_per_mb * (size_bytes / 1000000);
        if (result < 3000) {
            return 3000;
        } else {
            return result;
        }
    }

    private byte[] _int_to_bytearray(int i) {
        byte ret[] = { (byte) (i & 0xff), (byte) ((i >> 8) & 0xff), (byte) ((i >> 16) & 0xff),
                (byte) ((i >> 24) & 0xff) };
        return ret;
    }

    private int _bytearray_to_int(byte i, byte j, byte k, byte l) {
        return ((int)i | (int)(j << 8) | (int)(k << 16) | (int)(l << 24));
    }



    /**
     * Compress a byte array using ZLIB compression
     *
     * @param uncompressedData byte array of uncompressed data
     * @return byte array of compressed data
     */
    public byte[] compressBytes(byte[] uncompressedData) {
        // Create the compressor with highest level of compression
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);

        // Give the compressor the data to compress
        compressor.setInput(uncompressedData);
        compressor.finish();

        // Create an expandable byte array to hold the compressed data.
        // You cannot use an array that's the same size as the orginal because
        // there is no guarantee that the compressed data will be smaller than
        // the uncompressed data.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(uncompressedData.length);

        // Compress the data
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            bos.close();
        } catch (IOException e) {
        }

        // Get the compressed data
        byte[] compressedData = bos.toByteArray();
        return compressedData;
    }
}
