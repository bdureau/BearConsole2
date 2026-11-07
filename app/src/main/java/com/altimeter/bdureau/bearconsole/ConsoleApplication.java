package com.altimeter.bdureau.bearconsole;

/**
 * @description: This class instanciate pretty much everything including the connection
 * @author: boris.dureau@neuf.fr
 **/

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.altimeter.bdureau.bearconsole.connection.BluetoothConnection;
import com.altimeter.bdureau.bearconsole.connection.UsbConnection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * @description: This is quite a major class used everywhere because it can point to your connection,
 * appconfig
 * @author: boris.dureau@neuf.fr
 **/
public class ConsoleApplication extends Application {
    private static final String TAG = "ConsoleApplication";
    private static boolean DataReady = false;
    // Store number of flight
    public int NbrOfFlight = 0;
    public int currentFlightNbr = 0;
    public long lastReceived = 0;
    public String commandRet = "";
    public String lastReadResult;
    public String lastData;
    private boolean isConnected = false;
    private FlightData MyFlight = null;
    private AltiConfigData AltiCfg = null;
    private TestTrame testTrame = null;
    private double FEET_IN_METER = 1;
    private boolean exit = false;
    private GlobalConfig AppConf = null;
    private String address, moduleName;
    private String myTypeOfConnection = "bluetooth"; // "USB";//"bluetooth";
    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;
    private Handler mHandler;

    /**
     * Met à jour la locale du contexte en fonction de la configuration de l'application.
     * 0 = Langue du téléphone, 1 = Anglais
     */
    public static Context updateBaseContextLocale(Context context, int appLanguage) {
        Locale locale;
        if (appLanguage == 1) {
            locale = Locale.ENGLISH;
        } else {
            locale = Locale.getDefault();
        }
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(updateBaseContextLocale(base, lang));
    }

    public static Integer generateCheckSum(String value) {
        byte[] data = value.getBytes();
        long checksum = 0L;

        for (byte b : data) {
            checksum += b;
        }

        checksum = checksum % 256;

        return new Long(checksum).intValue();
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AltiCfg = new AltiConfigData();
        MyFlight = new FlightData(this, AltiCfg.getAltimeterName());
        testTrame = new TestTrame();
        AppConf = new GlobalConfig(this);
        AppConf.ReadConfig();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();

        myTypeOfConnection = AppConf.getConnectionTypeValue();
    }

    public String getConnectionType() {
        return myTypeOfConnection;
    }

    public void setConnectionType(String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String bTAddress) {
        address = bTAddress;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String bTmoduleName) {
        moduleName = bTmoduleName;
    }

    public InputStream getInputStream() {
        InputStream tmpIn = null;
        if (myTypeOfConnection.equals("bluetooth")) {
            tmpIn = BTCon.getInputStream();
        } else {
            tmpIn = UsbCon.getInputStream();
        }
        return tmpIn;
    }

    public UsbConnection getUsbCon() {
        return UsbCon;
    }

    public boolean getConnected() {
        boolean ret = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            ret = BTCon.getBTConnected();
        } else {
            ret = UsbCon.getUSBConnected();
        }
        return ret;
    }

    public void setConnected(boolean Connected) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        } else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public AltiConfigData getAltiConfigData() {
        return AltiCfg;
    }

    public void setAltiConfigData(AltiConfigData configData) {
        AltiCfg = configData;
    }

    public TestTrame getTestTrame() {
        return testTrame;
    }

    public FlightData getFlightData() {
        return MyFlight;
    }

    public void setFlightData(FlightData fData) {
        MyFlight = fData;
    }

    public int getNbrOfFlight() {
        return NbrOfFlight;
    }

    public void setNbrOfFlight(int value) {
        NbrOfFlight = value;
    }

    public boolean connect() {
        boolean state = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            if (ContextCompat.checkSelfPermission(ConsoleApplication.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return false;
                }
            }
            state = BTCon.connect(address, getApplicationContext());
            setConnectionType("bluetooth");
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");

            for (int i = 0; i < 3; i++) {
                if (isConnectionValid()) {
                    state = true;
                    break;
                }
            }
            if (!state)
                Disconnect();
        }
        return state;
    }

    public boolean isConnectionValid() {
        boolean valid = false;
        setDataReady(false);
        flush();
        clearInput();
        write("h;");

        flush();
        clearInput();
        write("h;");

        String myMessage = "";
        try {
            while (getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }

        myMessage = ReadResult(3000);

        if (myMessage.equals("OK")) {
            lastReadResult = myMessage;
            valid = true;
        } else {
            lastReadResult = myMessage;
            valid = false;
        }
        flush();
        clearInput();
        write("y0;");

        return valid;
    }

    public void Disconnect() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.Disconnect();
        } else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.write(data);
        } else {
            UsbCon.write(data);
        }
    }

    public void clearInput() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.clearInput();
        } else {
            UsbCon.clearInput();
        }
    }

    public void initFlightData() {
        MyFlight = new FlightData(this, AltiCfg.getAltimeterName());
        if (AppConf.getUnits() == 0) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
    }

    public void setExit(boolean b) {
        this.exit = b;
    }

    public long calculateSentenceCHK(String currentSentence[]) {
        long chk = 0;
        String sentence = "";

        for (int i = 0; i < currentSentence.length - 1; i++) {
            sentence = sentence + currentSentence[i] + ",";
        }
        chk = generateCheckSum(sentence);
        return chk;
    }

    public String ReadResult(long timeout) {
        this.exit = false;
        lastData = "";
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {
            while (!this.exit) {
                if ((System.currentTimeMillis() - lastReceived) > timeout)
                    this.exit = true;
                if (getInputStream().available() > 0) {
                    char ch = (char) getInputStream().read();
                    lastData = lastData + ch;
                    if (ch == '$') {
                        String tempBuff = "";
                        while (ch != ';') {
                            ch = (char) getInputStream().read();
                            if (ch != '\r' && ch != '\n' && ch != ';') {
                                tempBuff = tempBuff + ch;
                            }
                        }
                        if (ch == ';') {
                            ch = (char) getInputStream().read();
                        }

                        String[] currentSentence = new String[50];
                        if (!tempBuff.isEmpty()) {
                            currentSentence = tempBuff.split(",");
                            fullBuff = fullBuff + tempBuff;
                        }

                        long chk = 0;
                        if (currentSentence != null)
                            switch (currentSentence[0]) {
                                case "telemetry":
                                    if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                        chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                    if (calculateSentenceCHK(currentSentence) == chk) {
                                        if (mHandler != null) {
                                            if (currentSentence.length > 1)
                                                if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(1, String.valueOf(currentSentence[1])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(1, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 2)
                                                if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(2, String.valueOf(currentSentence[2])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(2, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 3)
                                                if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(3, String.valueOf(currentSentence[3])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(3, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 4)
                                                if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(4, String.valueOf(currentSentence[4])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(4, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 5)
                                                if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(5, String.valueOf(currentSentence[5])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(5, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 6)
                                                if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(6, String.valueOf(currentSentence[6])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(6, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 7)
                                                if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(7, String.valueOf(currentSentence[7])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(7, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 8)
                                                if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(8, String.valueOf(currentSentence[8])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(8, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 9)
                                                if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(9, String.valueOf(currentSentence[9])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(9, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 10)
                                                if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(10, String.valueOf(currentSentence[10])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(10, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 11)
                                                if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(11, String.valueOf(currentSentence[11])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(11, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 12)
                                                if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(12, String.valueOf(currentSentence[12])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(12, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 13)
                                                if (currentSentence[13].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(13, String.valueOf(currentSentence[13])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(13, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 14)
                                                if (currentSentence[14].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(14, String.valueOf(currentSentence[14])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(14, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 15)
                                                if (currentSentence[15].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(15, String.valueOf(currentSentence[15])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(15, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 16)
                                                if (currentSentence[16].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(16, String.valueOf(currentSentence[16])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(16, String.valueOf(0)).sendToTarget();

                                            if (currentSentence.length > 17)
                                                if (currentSentence[17].matches("\\d+(?:\\.\\d+)?"))
                                                    mHandler.obtainMessage(17, String.valueOf(currentSentence[17])).sendToTarget();
                                                else
                                                    mHandler.obtainMessage(17, String.valueOf(0)).sendToTarget();

                                            if (AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel") ||
                                                    AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                                                    AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_375")) {
                                                if (currentSentence.length > 18)
                                                    if (currentSentence[18].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(26, String.valueOf(currentSentence[18])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(26, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 19)
                                                    if (currentSentence[19].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(27, String.valueOf(currentSentence[19])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(27, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 20)
                                                    if (currentSentence[20].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(28, String.valueOf(currentSentence[20])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(28, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 21)
                                                    if (currentSentence[21].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(29, String.valueOf(currentSentence[21])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(29, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 22)
                                                    if (currentSentence[22].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(30, String.valueOf(currentSentence[22])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(30, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 23)
                                                    if (currentSentence[23].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(31, String.valueOf(currentSentence[23])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(31, String.valueOf(0)).sendToTarget();
                                            }

                                            if (AltiCfg.getAltimeterName().equals("TTGOBearAltimeter")) {
                                                if (currentSentence.length > 18)
                                                    if (currentSentence[18].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(26, String.valueOf(currentSentence[18])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(26, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 19)
                                                    if (currentSentence[19].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(27, String.valueOf(currentSentence[19])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(27, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 20)
                                                    if (currentSentence[20].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(28, String.valueOf(currentSentence[20])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(28, String.valueOf(0)).sendToTarget();
                                            }

                                            if (AltiCfg.getAltimeterName().equals("AltiGPS")) {
                                                if (currentSentence.length > 18)
                                                    if (currentSentence[18].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(18, String.valueOf(currentSentence[18])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(18, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 19)
                                                    if (currentSentence[19].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(19, String.valueOf(currentSentence[19])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(19, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 20)
                                                    if (currentSentence[20].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(20, String.valueOf(currentSentence[20])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(20, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 21)
                                                    if (currentSentence[21].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(21, String.valueOf(currentSentence[21])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(21, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 22)
                                                    if (currentSentence[22].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(22, String.valueOf(currentSentence[22])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(22, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 23)
                                                    if (currentSentence[23].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(23, String.valueOf(currentSentence[23])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(23, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 24)
                                                    if (currentSentence[24].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(24, String.valueOf(currentSentence[24])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(24, String.valueOf(0)).sendToTarget();

                                                if (currentSentence.length > 25)
                                                    if (currentSentence[25].matches("\\d+(?:\\.\\d+)?"))
                                                        mHandler.obtainMessage(25, String.valueOf(currentSentence[25])).sendToTarget();
                                                    else
                                                        mHandler.obtainMessage(25, String.valueOf(0)).sendToTarget();
                                            }
                                        }
                                    }
                                    break;

                                case "data":
                                    if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                        chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                    String flightName = "FlightXX";
                                    if (calculateSentenceCHK(currentSentence) == chk) {
                                        if (currentSentence.length > 1)
                                            if (currentSentence[1].matches("\\d+(?:\\.\\d+)?")) {
                                                currentFlightNbr = Integer.valueOf(currentSentence[1]) + 1;
                                                if (currentFlightNbr < 10)
                                                    flightName = getResources().getString(R.string.flight_name) + " 0" + currentFlightNbr;
                                                else
                                                    flightName = getResources().getString(R.string.flight_name) + " " + currentFlightNbr;
                                            }

                                        int value2 = 0, value3 = 0, value4 = 0, value5 = 0;
                                        int value6 = 0, value7 = 0, value8 = 0, value9 = 0;
                                        int value10 = 0, value11 = 0, value12 = 0;
                                        if (currentSentence.length > 2)
                                            if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                                value2 = Integer.valueOf(currentSentence[2]);

                                        if (currentSentence.length > 3) {
                                            if (currentSentence[3].matches("^-?\\d+(?:\\.\\d+)?"))
                                                value3 = Integer.valueOf(currentSentence[3]);
                                            MyFlight.AddToFlight(value2, (long) (value3 * FEET_IN_METER), flightName, 0);
                                        }

                                        if (currentSentence.length > 4) {
                                            if (currentSentence[4].matches("^-?\\d+(?:\\.\\d+)?"))
                                                value4 = Integer.valueOf(currentSentence[4]);
                                            MyFlight.AddToFlight(value2, (long) (value4), flightName, 1);
                                        }

                                        if (currentSentence.length > 5) {
                                            if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                                value5 = Integer.valueOf(currentSentence[5]);
                                            MyFlight.AddToFlight(value2, (long) (value5), flightName, 2);
                                        }

                                        if (AltiCfg.getAltimeterName().equals("AltiGPS") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiESP32") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_375") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_345") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiSTM32") ||
                                                AltiCfg.getAltimeterName().equals("TTGOBearAltimeter") ||
                                                AltiCfg.getAltimeterName().equals("TTGOMiniBearAltimeter")) {
                                            if (currentSentence.length > 6) {
                                                if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                                    value6 = Integer.valueOf(currentSentence[6]);
                                                MyFlight.AddToFlight(value2, (float) (value6) / 100, flightName, 5);
                                            }
                                        }

                                        if (AltiCfg.getAltimeterName().equals("TTGOBearAltimeter") ||
                                                AltiCfg.getAltimeterName().equals("TTGOMiniBearAltimeter")) {
                                            if (currentSentence.length > 7) {
                                                if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                                    value7 = Integer.valueOf(currentSentence[7]);
                                                MyFlight.AddToFlight(value2, (float) (value7) / (float) 1000, flightName, 6);
                                            }
                                            if (currentSentence.length > 8) {
                                                if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                                    value8 = Integer.valueOf(currentSentence[8]);
                                                MyFlight.AddToFlight(value2, (float) (value8) / (float) 1000, flightName, 7);
                                            }
                                            if (currentSentence.length > 9) {
                                                if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                                    value9 = Integer.valueOf(currentSentence[9]);
                                                MyFlight.AddToFlight(value2, (float) (value9) / (float) 1000, flightName, 8);
                                            }
                                        }
                                        if (AltiCfg.getAltimeterName().equals("UltimateAltimeter")) {
                                            if (currentSentence.length > 6) {
                                                if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                                    value7 = Integer.valueOf(currentSentence[6]);
                                                MyFlight.AddToFlight(value2, (float) (value7) / (float) 1000, flightName, 5);
                                            }
                                            if (currentSentence.length > 7) {
                                                if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                                    value8 = Integer.valueOf(currentSentence[7]);
                                                MyFlight.AddToFlight(value2, (float) (value8) / (float) 1000, flightName, 6);
                                            }
                                            if (currentSentence.length > 8) {
                                                if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                                    value9 = Integer.valueOf(currentSentence[8]);
                                                MyFlight.AddToFlight(value2, (float) (value9) / (float) 1000, flightName, 7);
                                            }
                                        }
                                        if (AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_375") ||
                                                AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_345")) {
                                            if (currentSentence.length > 7) {
                                                if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                                    value7 = Integer.valueOf(currentSentence[7]);
                                                MyFlight.AddToFlight(value2, (float) (value7) / (float) 1000, flightName, 6);
                                            }
                                            if (currentSentence.length > 8) {
                                                if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                                    value8 = Integer.valueOf(currentSentence[8]);
                                                MyFlight.AddToFlight(value2, (float) (value8) / (float) 1000, flightName, 7);
                                            }
                                            if (currentSentence.length > 9) {
                                                if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                                    value9 = Integer.valueOf(currentSentence[9]);
                                                MyFlight.AddToFlight(value2, (float) (value9) / (float) 1000, flightName, 8);
                                            }
                                            if (currentSentence.length > 10) {
                                                if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                                    value10 = Integer.valueOf(currentSentence[10]);
                                                MyFlight.AddToFlight(value2, (float) (value10) / (float) 1000, flightName, 9);
                                            }
                                            if (currentSentence.length > 11) {
                                                if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                                    value11 = Integer.valueOf(currentSentence[11]);
                                                MyFlight.AddToFlight(value2, (float) (value11) / (float) 1000, flightName, 10);
                                            }
                                            if (currentSentence.length > 12) {
                                                if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                                    value12 = Integer.valueOf(currentSentence[12]);
                                                MyFlight.AddToFlight(value2, (float) (value12) / (float) 1000, flightName, 11);
                                            }
                                        }
                                        if (AltiCfg.getAltimeterName().equals("AltiGPS")) {
                                            if (currentSentence.length > 7) {
                                                if (currentSentence[7].matches("^-?\\d+(?:\\.\\d+)?"))
                                                    value7 = Integer.valueOf(currentSentence[7]);
                                                MyFlight.AddToFlight(value2, (float) (value7) / 100000, flightName, 6);
                                            }
                                            if (currentSentence.length > 8) {
                                                if (currentSentence[8].matches("^-?\\d+(?:\\.\\d+)?"))
                                                    value8 = Integer.valueOf(currentSentence[8]);
                                                MyFlight.AddToFlight(value2, (float) (value8) / 100000, flightName, 7);
                                            }
                                            if (currentSentence.length > 9) {
                                                if (currentSentence[9].matches("^-?\\d+(?:\\.\\d+)?"))
                                                    value9 = Integer.valueOf(currentSentence[9]);
                                                MyFlight.AddToFlight(value2, (float) (value9), flightName, 8);
                                            }
                                            if (currentSentence.length > 10) {
                                                if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                                    value10 = Integer.valueOf(currentSentence[10]);
                                                MyFlight.AddToFlight(value2, (float) (value10), flightName, 9);
                                            }
                                            if (currentSentence.length > 11) {
                                                if (currentSentence[11].matches("^-?\\d+(?:\\.\\d+)?"))
                                                    value11 = Integer.valueOf(currentSentence[11]);
                                                MyFlight.AddToFlight(value2, (float) (value11), flightName, 10);
                                            }
                                            if (currentSentence.length > 12) {
                                                if (currentSentence[12].matches("^-?\\d+(?:\\.\\d+)?")) {
                                                    value12 = Integer.valueOf(currentSentence[12]);
                                                    Log.d(TAG, value12 + "");
                                                }
                                                MyFlight.AddToFlight(value2, (float) (value12), flightName, 11);
                                            }
                                        }
                                    }
                                    break;

                                case "alticonfig":
                                    if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                        chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                    if (calculateSentenceCHK(currentSentence) == chk) {
                                        if (currentSentence.length > 1)
                                            if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setUnits(Integer.valueOf(currentSentence[1]));
                                            else
                                                AltiCfg.setUnits(0);

                                        if (currentSentence.length > 2)
                                            if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setBeepingMode(Integer.valueOf(currentSentence[2]));
                                            else
                                                AltiCfg.setBeepingMode(0);

                                        if (currentSentence.length > 3)
                                            if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput1(Integer.valueOf(currentSentence[3]));
                                            else
                                                AltiCfg.setOutput1(0);

                                        if (currentSentence.length > 4)
                                            if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput2(Integer.valueOf(currentSentence[4]));
                                            else
                                                AltiCfg.setOutput2(0);

                                        if (currentSentence.length > 5)
                                            if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput3(Integer.valueOf(currentSentence[5]));
                                            else
                                                AltiCfg.setOutput3(0);

                                        if (currentSentence.length > 6)
                                            if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setSupersonicYesNo(Integer.valueOf(currentSentence[6]));
                                            else
                                                AltiCfg.setSupersonicYesNo(0);

                                        if (currentSentence.length > 7)
                                            if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setMainAltitude(Integer.valueOf(currentSentence[7]));
                                            else
                                                AltiCfg.setMainAltitude(50);

                                        if (currentSentence.length > 8)
                                            AltiCfg.setAltimeterName(currentSentence[8]);

                                        if (currentSentence.length > 9)
                                            if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setAltiMajorVersion(Integer.valueOf(currentSentence[9]));
                                            else
                                                AltiCfg.setAltiMajorVersion(0);

                                        if (currentSentence.length > 10)
                                            if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setAltiMinorVersion(Integer.valueOf(currentSentence[10]));
                                            else
                                                AltiCfg.setAltiMinorVersion(0);

                                        if (currentSentence.length > 11)
                                            if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput1Delay(Integer.valueOf(currentSentence[11]));
                                            else
                                                AltiCfg.setOutput1Delay(0);

                                        if (currentSentence.length > 12)
                                            if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput2Delay(Integer.valueOf(currentSentence[12]));
                                            else
                                                AltiCfg.setOutput2Delay(0);

                                        if (currentSentence.length > 13)
                                            if (currentSentence[13].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput3Delay(Integer.valueOf(currentSentence[13]));
                                            else
                                                AltiCfg.setOutput3Delay(0);

                                        if (currentSentence.length > 14)
                                            if (currentSentence[14].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setBeepingFrequency(Integer.valueOf(currentSentence[14]));
                                            else
                                                AltiCfg.setBeepingFrequency(440);

                                        if (currentSentence.length > 15)
                                            if (currentSentence[15].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setNbrOfMeasuresForApogee(Integer.valueOf(currentSentence[15]));
                                            else
                                                AltiCfg.setNbrOfMeasuresForApogee(0);

                                        if (currentSentence.length > 16)
                                            if (currentSentence[16].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setEndRecordAltitude(Integer.valueOf(currentSentence[16]));
                                            else
                                                AltiCfg.setEndRecordAltitude(5);

                                        if (currentSentence.length > 17)
                                            if (currentSentence[17].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setTelemetryType(Integer.valueOf(currentSentence[17]));
                                            else
                                                AltiCfg.setTelemetryType(0);

                                        if (currentSentence.length > 18)
                                            if (currentSentence[18].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setSupersonicDelay(Integer.valueOf(currentSentence[18]));
                                            else
                                                AltiCfg.setSupersonicDelay(0);

                                        if (currentSentence.length > 19)
                                            if (currentSentence[19].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setConnectionSpeed(Integer.valueOf(currentSentence[19]));
                                            else
                                                AltiCfg.setConnectionSpeed(38400);

                                        if (currentSentence.length > 20)
                                            if (currentSentence[20].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setAltimeterResolution(Integer.valueOf(currentSentence[20]));
                                            else
                                                AltiCfg.setAltimeterResolution(0);

                                        if (currentSentence.length > 21)
                                            if (currentSentence[21].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setEepromSize(Integer.valueOf(currentSentence[21]));
                                            else
                                                AltiCfg.setEepromSize(512);

                                        if (currentSentence.length > 22)
                                            if (currentSentence[22].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setBeepOnOff(Integer.valueOf(currentSentence[22]));
                                            else
                                                AltiCfg.setBeepOnOff(0);

                                        if (currentSentence.length > 23)
                                            if (currentSentence[23].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput4(Integer.valueOf(currentSentence[23]));
                                            else
                                                AltiCfg.setOutput4(-1);

                                        if (currentSentence.length > 24)
                                            if (currentSentence[24].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setOutput4Delay(Integer.valueOf(currentSentence[24]));
                                            else
                                                AltiCfg.setOutput4Delay(-1);

                                        if (currentSentence.length > 25)
                                            if (currentSentence[25].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setLiftOffAltitude(Integer.valueOf(currentSentence[25]));
                                            else
                                                AltiCfg.setLiftOffAltitude(20);

                                        if (currentSentence.length > 26)
                                            if (currentSentence[26].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setBatteryType(Integer.valueOf(currentSentence[26]));
                                            else
                                                AltiCfg.setBatteryType(0);

                                        if (currentSentence.length > 27)
                                            if (currentSentence[27].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setRecordingTimeout(Integer.valueOf(currentSentence[27]));
                                            else
                                                AltiCfg.setRecordingTimeout(120);

                                        if (currentSentence.length > 28)
                                            if (currentSentence[28].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setAltiID(Integer.valueOf(currentSentence[28]));
                                            else
                                                AltiCfg.setAltiID(0);

                                        if (currentSentence.length > 29) {
                                            Log.d(TAG, "currentSentence:" + currentSentence[29]);
                                            if (currentSentence[29].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setUseTelemetryPort(Integer.valueOf(currentSentence[29]));
                                            else
                                                AltiCfg.setUseTelemetryPort(0);
                                        }

                                        if (AltiCfg.getAltimeterName().equals("AltiServo")) {
                                            if (currentSentence.length > 30) {
                                                if (currentSentence[30].matches("\\d+(?:\\.\\d+)?"))
                                                    AltiCfg.setServoSwitch(Integer.valueOf(currentSentence[30]));
                                                else
                                                    AltiCfg.setServoSwitch(0);
                                            }
                                        } else {
                                            if ((AltiCfg.getAltimeterName().equals("AltiMultiESP32") ||
                                                    AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel") ||
                                                    AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_375") ||
                                                    AltiCfg.getAltimeterName().equals("AltiMultiESP32_accel_345") &&
                                                            (AltiCfg.getAltiMajorVersion() == 2 && AltiCfg.getAltiMinorVersion() > 0)) ||
                                                    (AltiCfg.getAltimeterName().equals("TTGOBearAltimeter") &&
                                                            (AltiCfg.getAltiMajorVersion() == 0 && AltiCfg.getAltiMinorVersion() > 4))) {
                                                if (currentSentence.length > 30) {
                                                    Log.d(TAG, "currentSentence:" + currentSentence[30]);
                                                    AltiCfg.setBluetoothName(currentSentence[30]);
                                                }
                                            }
                                        }

                                        if (currentSentence.length > 31)
                                            if (currentSentence[31].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo1OnPos(Integer.valueOf(currentSentence[31]));
                                            else
                                                AltiCfg.setServo1OnPos(-1);

                                        if (currentSentence.length > 32)
                                            if (currentSentence[32].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo2OnPos(Integer.valueOf(currentSentence[32]));
                                            else
                                                AltiCfg.setServo2OnPos(-1);

                                        if (currentSentence.length > 33)
                                            if (currentSentence[33].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo3OnPos(Integer.valueOf(currentSentence[33]));
                                            else
                                                AltiCfg.setServo3OnPos(-1);

                                        if (currentSentence.length > 34)
                                            if (currentSentence[34].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo4OnPos(Integer.valueOf(currentSentence[34]));
                                            else
                                                AltiCfg.setServo4OnPos(-1);

                                        if (currentSentence.length > 35)
                                            if (currentSentence[35].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo1OffPos(Integer.valueOf(currentSentence[35]));
                                            else
                                                AltiCfg.setServo1OffPos(-1);

                                        if (currentSentence.length > 36)
                                            if (currentSentence[36].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo2OffPos(Integer.valueOf(currentSentence[36]));
                                            else
                                                AltiCfg.setServo2OffPos(-1);

                                        if (currentSentence.length > 37)
                                            if (currentSentence[37].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo3OffPos(Integer.valueOf(currentSentence[37]));
                                            else
                                                AltiCfg.setServo3OffPos(-1);

                                        if (currentSentence.length > 38)
                                            if (currentSentence[38].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServo4OffPos(Integer.valueOf(currentSentence[38]));
                                            else
                                                AltiCfg.setServo4OffPos(-1);

                                        if (currentSentence.length > 39)
                                            if (currentSentence[39].matches("\\d+(?:\\.\\d+)?"))
                                                AltiCfg.setServoStayOn(Integer.valueOf(currentSentence[39]));
                                            else
                                                AltiCfg.setServoStayOn(0);

                                        myMessage = myMessage + " alticonfig";
                                    } else {
                                        myMessage = myMessage + "KO alticonfig";
                                    }
                                    break;

                                case "testTrame":
                                    if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                        chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                    if (calculateSentenceCHK(currentSentence) == chk) {
                                        testTrame.setTrameStatus(true);
                                    } else {
                                        testTrame.setTrameStatus(false);
                                    }
                                    if (currentSentence.length > 1)
                                        testTrame.setCurrentTrame(currentSentence[1]);
                                    else
                                        testTrame.setCurrentTrame("Error reading packet");
                                    myMessage = myMessage + " testTrame";
                                    break;

                                case "nbrOfFlight":
                                    if (currentSentence.length > 1)
                                        if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                            NbrOfFlight = (Integer.valueOf(currentSentence[1]));
                                    myMessage = myMessage + " nbrOfFlight";
                                    break;

                                case "start":
                                    setDataReady(false);
                                    myMessage = "start";
                                    break;

                                case "end":
                                    setDataReady(true);
                                    myMessage = myMessage + " end";
                                    exit = true;
                                    break;

                                case "OK":
                                    setDataReady(true);
                                    commandRet = currentSentence[0];
                                    myMessage = "OK";
                                    exit = true;
                                    break;

                                case "KO":
                                case "UNKNOWN":
                                    setDataReady(true);
                                    commandRet = currentSentence[0];
                                    break;

                                default:
                                    break;
                            }
                    }
                }
            }
        } catch (IOException e) {
            myMessage = myMessage + " error:" + e.getMessage();
        }
        return myMessage;
    }

    public boolean getDataReady() {
        return DataReady;
    }

    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public Configuration getAppLocal() {
        Locale locale = null;
        if (AppConf.getApplicationLanguage() == 1) {
            locale = Locale.ENGLISH;
        } else {
            locale = Locale.getDefault();
        }

        Configuration config = new Configuration();
        config.locale = locale;
        return config;
    }

    public GlobalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(GlobalConfig value) {
        AppConf = value;
    }

    public class TestTrame {
        private String currentTrame = "";
        private boolean trameStatus = false;

        public String getCurrentTrame() {
            return currentTrame;
        }

        public void setCurrentTrame(String trame) {
            currentTrame = trame;
        }

        public boolean getTrameStatus() {
            return trameStatus;
        }

        public void setTrameStatus(boolean val) {
            trameStatus = val;
        }
    }
}