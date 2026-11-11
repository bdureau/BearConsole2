package com.altimeter.bdureau.bearconsole.connection;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: BluetoothConnection
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Bluetooth Classic transport to the altimeter telemetry module (HC-05 / HC-06 or the
 *  ESP32 built-in Bluetooth). It opens an RFCOMM socket using the Serial Port Profile
 *  (SPP) UUID and exposes raw write / flush / read primitives to ConsoleApplication.
 *  Every method is safe to call after a disconnection: it then does nothing instead of
 *  throwing a NullPointerException.
 * ============================================================================
 */

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BluetoothConnection {
    private static final String TAG = "BluetoothConnection";

    // Standard Serial Port Profile UUID used by serial Bluetooth modules
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter myBluetooth = null;
    private volatile BluetoothSocket btSocket = null;
    private volatile boolean isBtConnected = false;

    /**
     * Opens the RFCOMM link to the module. Blocking call (several seconds when the module
     * is out of range): must be called from a background thread.
     *
     * @param address MAC address of the paired module
     * @param c       context used to check the BLUETOOTH_CONNECT permission
     * @return true if the socket is connected
     */
    public boolean connect(String address, Context c) {
        boolean state;
        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();
            if (myBluetooth == null || address == null) {
                setBTConnected(false);
                return false;
            }
            // Android 12+: the runtime permission is requested by the UI before connecting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            BluetoothDevice btDevice = myBluetooth.getRemoteDevice(address);
            setBtSocket(btDevice.createRfcommSocketToServiceRecord(myUUID));

            // Discovery slows down the connection: make sure it is stopped
            myBluetooth.cancelDiscovery();

            getBtSocket().connect();
            state = true;
            setBTConnected(true);

        } catch (IOException | IllegalArgumentException | SecurityException e) {
            Log.w(TAG, "Bluetooth connection failed", e);
            state = false;
            setBTConnected(false);
        }
        return state;
    }

    public void setBTConnected(boolean BtConnected) {
        isBtConnected = BtConnected;
    }

    public boolean getBTConnected() {
        return isBtConnected;
    }

    public void setBtSocket(BluetoothSocket mybtSocket) {
        btSocket = mybtSocket;
    }

    public BluetoothSocket getBtSocket() {
        return btSocket;
    }

    /** Input stream of the socket, or null when there is no socket. */
    public InputStream getInputStream() {
        BluetoothSocket socket = btSocket;
        if (socket == null) return null;
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    /** Closes the socket; a blocked read on the other side then fails and returns. */
    public void Disconnect() {
        BluetoothSocket socket = btSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error while closing the socket", e);
            }
        }
        setBTConnected(false);
    }

    public void write(String data) {
        BluetoothSocket socket = btSocket;
        if (socket == null) return;
        try {
            socket.getOutputStream().write(data.getBytes());
        } catch (IOException e) {
            Log.w(TAG, "Write failed", e);
        }
    }

    public void flush() {
        BluetoothSocket socket = btSocket;
        if (socket == null) return;
        try {
            socket.getOutputStream().flush();
        } catch (IOException e) {
            Log.w(TAG, "Flush failed", e);
        }
    }

    /** Discards the bytes already received and not read yet. */
    public void clearInput() {
        BluetoothSocket socket = btSocket;
        if (socket == null) return;
        try {
            InputStream in = socket.getInputStream();
            in.skip(in.available());
        } catch (IOException e) {
            Log.w(TAG, "clearInput failed", e);
        }
    }
}
