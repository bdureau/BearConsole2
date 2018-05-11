package com.altimeter.bdureau.bearconsole;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

//import com.felhr.usbserial.SerialInputStream;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
/**
 *   @description:
 *   @author: boris.dureau@neuf.fr
 **/

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

        public void appendLog(String text)
        {
            File logFile = new File("sdcard/debugfileStream.txt");
            if (!logFile.exists())
            {
                try
                {
                    logFile.createNewFile();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try
            {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(text);
                buf.newLine();
                buf.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
