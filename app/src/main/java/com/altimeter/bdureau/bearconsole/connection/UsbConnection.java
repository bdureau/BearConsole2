package com.altimeter.bdureau.bearconsole.connection;
/**
 *
 *   @description: This is the USB connection class. Not that the input strean has been overwritten
 *   from the USB library to fix a bug
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;


import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.ArrayBlockingQueue;


public class UsbConnection {

    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    SerialInputStream usbInputStream;
    private boolean isUSBConnected = false;

    public boolean connect( UsbManager usbManager,UsbDevice device, int baudRate) {
        boolean connected = false;
        connection = usbManager.openDevice(device);
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) { //Set Serial Connection Parameters.

                serialPort.setBaudRate(baudRate);
                //serialPort.setBaudRate(38400);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);//ok
                //serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_DSR_DTR);//non
                //serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_RTS_CTS);//ok
                //serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_XON_XOFF);//ok

                usbInputStream = new SerialInputStream(serialPort);

                connected = true;
            } else {
                Log.d("SERIAL", "PORT NOT OPEN");
            }
        } else {
            Log.d("SERIAL", "PORT IS NULL");
        }
        setUSBConnected(connected);
        return connected;

    }
    public void Disconnect() {
        serialPort.close();
        setUSBConnected(false);
    }

    public void write (String data) {

        serialPort.write(data.getBytes());

    }

    public void flush () {
     //not implemented
    }

    public void clearInput() {
        try {
        usbInputStream.skip(usbInputStream.available());
        } catch (IOException e) {
        }
    }

    public InputStream getInputStream () {
        return usbInputStream;
    }
    public void setUSBConnected(boolean UsbConnected) {
        isUSBConnected = UsbConnected;
    }

    public boolean getUSBConnected() {
        return isUSBConnected;
    }
    /**
     * Created by BDUREAU on 29/07/2017.
     * Do not use the class from USB serial because the available method always return 0
     */
   public class SerialInputStream extends InputStream implements UsbSerialInterface.UsbReadCallback
    {
        protected final UsbSerialInterface device;
        protected ArrayBlockingQueue data = new ArrayBlockingQueue<Integer>(256);

        protected volatile boolean is_open;

        public SerialInputStream(UsbSerialInterface mydevice)
        {
            this.device = mydevice;
            is_open = true;
            device.read(this);
        }

        @Override
        public int available() throws IOException {

            return data.size();
        }

        @Override
        public int read()
        {
            while (is_open)
            {
                try
                {
                    return (Integer)data.take();
                } catch (InterruptedException e)
                {
                    // ignore, will be retried by while loop
                }
            }
            return -1;
        }

        public void close()
        {
            is_open = false;
            try
            {
                data.put(-1);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        public void onReceivedData(byte[] new_data)
        {
            for (byte b : new_data)
            {
                try
                {
                    data.put(((int)b) & 0xff);
                } catch (InterruptedException e)
                {
                    // ignore, possibly losing bytes when buffer is full
                }
            }
        }
    }

}
