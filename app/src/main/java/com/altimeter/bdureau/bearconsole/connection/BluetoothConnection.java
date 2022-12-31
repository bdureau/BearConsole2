package com.altimeter.bdureau.bearconsole.connection;
/**
 *
 *   @description: This class has all the methods to deal with your bluetooth connection
 *   @author: boris.dureau@neuf.fr
 *
 **/

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class BluetoothConnection {
    BluetoothDevice device;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private String address = null;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public boolean connect(String address, Context c) {
        boolean state;
        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    //getApplicationContext()

                    //ActivityCompat.requestPermissions(this.get, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return false;

                }
            }
            BluetoothDevice btDevice = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
            //setBtSocket(btDevice.createInsecureRfcommSocketToServiceRecord(myUUID));//create a RFCOMM (SPP) connection

            setBtSocket(btDevice.createRfcommSocketToServiceRecord(myUUID));
            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return TODO;
            }*/
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
