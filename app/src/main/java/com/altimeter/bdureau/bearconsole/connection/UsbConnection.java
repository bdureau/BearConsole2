package com.altimeter.bdureau.bearconsole.connection;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: UsbConnection
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  USB serial transport (USB OTG cable to the altimeter or to a radio modem), based on the
 *  felHR85 UsbSerial library. The library input stream is replaced by {@link SerialInputStream}
 *  because its available() method always returns 0, which the protocol parser relies on.
 *  Every method is safe to call when the port is closed.
 * ============================================================================
 */

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

public class UsbConnection {
    private static final String TAG = "UsbConnection";

    private volatile UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;
    private volatile SerialInputStream usbInputStream;
    private volatile boolean isUSBConnected = false;

    /**
     * Opens the serial port with 8 data bits, 1 stop bit, no parity, no flow control.
     *
     * @param usbManager system USB service
     * @param device     USB device the user granted access to
     * @param baudRate   serial speed configured in the application settings
     * @return true if the port is open
     */
    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean connected = false;
        connection = usbManager.openDevice(device);
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) {
                serialPort.setBaudRate(baudRate);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                usbInputStream = new SerialInputStream(serialPort);

                connected = true;
            } else {
                Log.d(TAG, "Serial port could not be opened");
            }
        } else {
            Log.d(TAG, "Device is not a supported USB serial chip");
        }
        setUSBConnected(connected);
        return connected;
    }

    /** Closes the port and wakes up any thread blocked on the input stream. */
    public void Disconnect() {
        SerialInputStream in = usbInputStream;
        if (in != null) {
            in.close();
        }
        UsbSerialDevice port = serialPort;
        if (port != null) {
            try {
                port.close();
            } catch (RuntimeException e) {
                Log.w(TAG, "Error while closing the serial port", e);
            }
        }
        setUSBConnected(false);
    }

    public void write(String data) {
        UsbSerialDevice port = serialPort;
        if (port != null) {
            port.write(data.getBytes());
        }
    }

    public void flush() {
        // Not needed: the library sends the data immediately
    }

    /** Discards the bytes already received and not read yet. */
    public void clearInput() {
        SerialInputStream in = usbInputStream;
        if (in == null) return;
        try {
            in.skip(in.available());
        } catch (IOException e) {
            Log.w(TAG, "clearInput failed", e);
        }
    }

    public InputStream getInputStream() {
        return usbInputStream;
    }

    public void setUSBConnected(boolean UsbConnected) {
        isUSBConnected = UsbConnected;
    }

    public boolean getUSBConnected() {
        return isUSBConnected;
    }

    /**
     * InputStream fed by the asynchronous read callback of the USB library.
     * Received bytes are stored in a bounded queue; available() returns the queue size.
     * After close(), read() returns -1 immediately and available() returns 0.
     */
    public static class SerialInputStream extends InputStream implements UsbSerialInterface.UsbReadCallback {
        protected final UsbSerialInterface device;
        protected final ArrayBlockingQueue<Integer> data = new ArrayBlockingQueue<>(256);

        protected volatile boolean is_open;

        public SerialInputStream(UsbSerialInterface mydevice) {
            this.device = mydevice;
            is_open = true;
            device.read(this);
        }

        @Override
        public int available() {
            return is_open ? data.size() : 0;
        }

        @Override
        public int read() {
            while (is_open) {
                try {
                    Integer value = data.take();
                    return is_open ? value : -1;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
            return -1;
        }

        @Override
        public void close() {
            is_open = false;
            // Unblock a reader waiting in take()
            data.offer(-1);
        }

        @Override
        public void onReceivedData(byte[] new_data) {
            if (!is_open) return;
            for (byte b : new_data) {
                try {
                    data.put(((int) b) & 0xff);
                } catch (InterruptedException e) {
                    // The callback runs on the library reading thread: do not propagate the
                    // interruption to it; the remaining bytes of this chunk are dropped
                    return;
                }
            }
        }
    }
}
