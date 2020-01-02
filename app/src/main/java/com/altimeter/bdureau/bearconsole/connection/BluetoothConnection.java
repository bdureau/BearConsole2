package com.altimeter.bdureau.bearconsole.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 *   @description: This class has all the methods to deal with your bluetooth connection
 *   @author: boris.dureau@neuf.fr
 **/
public class BluetoothConnection {
    BluetoothDevice device;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private String address = null;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public boolean connect(String address) {
        boolean state;
        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            BluetoothDevice btDevice = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
            //setBtSocket(btDevice.createInsecureRfcommSocketToServiceRecord(myUUID));//create a RFCOMM (SPP) connection
            setBtSocket(btDevice.createRfcommSocketToServiceRecord(myUUID));
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            getBtSocket().connect();//start connection
            state = true;
            setBTConnected(true);

        } catch (IOException e) {
            state = false;//if the try failed, you can check the exception here
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


    public InputStream getInputStream () {
        InputStream tmpIn = null;
        try {
            tmpIn = btSocket.getInputStream();
        } catch (IOException e) {

        }
        return tmpIn;
    }
    public void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {


            }
            setBTConnected(false);
        }

    }

    public void write (String data) {
        try {
            btSocket.getOutputStream().write(data.getBytes());
        } catch (IOException e) {
        }
    }
    public void flush() {
        try {
        btSocket.getOutputStream().flush();
        } catch (IOException e) {
        }
    }
    public void clearInput() {
        try {
            btSocket.getInputStream().skip(btSocket.getInputStream().available());
        } catch (IOException e) {
        }
    }

}
