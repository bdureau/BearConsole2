package com.altimeter.bdureau.bearconsole.connection;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: UsbConnection
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  USB serial transport (USB OTG cable to the altimeter or to a radio modem), based on the
 *  felHR85 UsbSerial library. The library input stream is replaced by {@link SerialInputStream}
 *  because its available() method always returns 0, which the protocol parser relies on.
 *  Every method is safe to call when the port is closed. Disconnect() also closes the
 *  UsbDeviceConnection, so that the device can be opened again after being plugged back.
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
        if (connection == null) {
            // No permission, or the device was unplugged
            Log.d(TAG, "USB device could not be opened");
            setUSBConnected(false);
            return false;
        }
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
        if (!connected) {
            // Do not keep the device opened when the serial port is not usable
            closeDeviceConnection();
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
        serialPort = null;
        // usbInputStream is kept: once closed it returns -1 / 0, never null for the callers
        // Release the device: without this, opening it again could fail (device busy)
        closeDeviceConnection();
        setUSBConnected(false);
    }

    /** Closes the UsbDeviceConnection opened by connect(), if any. */
    private void closeDeviceConnection() {
        UsbDeviceConnection conn = connection;
        connection = null;
        if (conn != null) {
            try {
                conn.close();
            } catch (RuntimeException e) {
                Log.w(TAG, "Error while closing the USB device", e);
            }
        }
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
     * The callback never blocks: when nobody reads (e.g. telemetry still streaming while no
     * screen reads it) and the queue is full, the new bytes are dropped instead of blocking
     * the reading thread of the USB library. The commands always clear the input before
     * waiting for their answer, so these dropped bytes were never going to be used.
     */
    public static class SerialInputStream extends InputStream implements UsbSerialInterface.UsbReadCallback {
        /** Queue size: about 5 s of data at 115200 bauds, much more than any burst. */
        private static final int QUEUE_CAPACITY = 64 * 1024;

        protected final UsbSerialInterface device;
        protected final ArrayBlockingQueue<Integer> data = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        // Number of bytes dropped because the queue was full (diagnostic only)
        private long droppedBytes = 0;

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
            if (!is_open || new_data == null) return;
            for (byte b : new_data) {
                // offer() never blocks the reading thread of the USB library
                if (!data.offer(((int) b) & 0xff)) {
                    droppedBytes++;
                    if (droppedBytes == 1 || droppedBytes % 4096 == 0) {
                        Log.w(TAG, "USB input queue full, bytes dropped: " + droppedBytes);
                    }
                }
            }
        }
    }
}
