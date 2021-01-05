package com.altimeter.bdureau.bearconsole;
/**
 * @description: This class instanciate pretty much everything including the connection
 * @author: boris.dureau@neuf.fr
 **/

import android.app.Application;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.Handler;

import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AppConfigData;
import com.altimeter.bdureau.bearconsole.connection.BluetoothConnection;
import com.altimeter.bdureau.bearconsole.connection.UsbConnection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.Locale;

/**
 * @description: This is quite a major class used everywhere because it can point to your connection,
 * appconfig
 * @author: boris.dureau@neuf.fr
 **/
public class ConsoleApplication extends Application {
    private boolean isConnected = false;
    // Store number of flight
    public int NbrOfFlight = 0;
    public int currentFlightNbr = 0;
    private FlightData MyFlight = null;
    private AltiConfigData AltiCfg = null;

    private static boolean DataReady = false;
    public long lastReceived = 0;
    public String commandRet = "";

    private double FEET_IN_METER = 1;
    private boolean exit = false;
    private GlobalConfig AppConf = null;
    private String address;
    private String myTypeOfConnection = "bluetooth";// "USB";//"bluetooth";

    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;

    private Handler mHandler;

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public String lastReadResult;
    public String lastData;

    @Override
    public void onCreate() {

        super.onCreate();
        AltiCfg = new AltiConfigData();
        MyFlight = new FlightData();
        AppConf = new GlobalConfig();
        AppConf.ReadConfig();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();
        /*if (AppConf.getConnectionType().equals("0"))
            //bluetooth
            myTypeOfConnection= "bluetooth";
        else
            myTypeOfConnection ="usb";*/
        myTypeOfConnection = AppConf.getConnectionTypeValue();

    }

    public void setConnectionType(String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }

    public String getConnectionType() {
        return myTypeOfConnection;
    }

    public void setAddress(String bTAddress) {

        address = bTAddress;

    }

    public String getAddress() {
        return address;
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

    public void setConnected(boolean Connected) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        } else {
            UsbCon.setUSBConnected(Connected);
        }
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

    public void setAltiConfigData(AltiConfigData configData) {
        AltiCfg = configData;
    }

    public AltiConfigData getAltiConfigData() {
        return AltiCfg;
    }

    public void setFlightData(FlightData fData) {
        MyFlight = fData;
    }

    public FlightData getFlightData() {
        return MyFlight;
    }

    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state = false;
       //appendLog("connect:");
        if (myTypeOfConnection.equals("bluetooth")) {
            state = BTCon.connect(address);
            setConnectionType("bluetooth");
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    // connect to the USB
    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    public boolean connectFirmware(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");
            /*if (!isConnectionValid()) {
                Disconnect();
                state = false;
            }*/
        }
        return state;
    }

    public boolean isConnectionValid() {
        boolean valid = false;
        //if(getConnected()) {

        setDataReady(false);
       /* try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        flush();
        clearInput();
        //  send 2 commands to get read of the module connection string on some modules
        write("h;\n".toString());

        flush();
        clearInput();
        write("h;\n".toString());

        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();
        long diffTime = 0;
        //get the results
        //wait for the result to come back
        try {
            /*while (getInputStream().available() <= 0 || diffTime < timeOut) {
                diffTime = System.currentTimeMillis() - startTime;}*/
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
        //}

        //valid = true;
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
        MyFlight = new FlightData();

        //if(AppConf.getUnits().equals("0"))
        if (AppConf.getUnitsValue().equals("Meters")) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
    }

   /* public void appendLog(String text) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        //File logFile = new File(path+"/debugfile2.txt");
        File logFile = new File("/mnt/sdcard0/debugfile2.txt");
        //File logFile = new File("MicroSD/debugfile2.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
*/

    public void setExit(boolean b) {
        this.exit = b;
    }


    public long calculateSentenceCHK(String currentSentence[]) {
        long chk = 0;
        String sentence = "";

        for (int i = 0; i < currentSentence.length - 1; i++) {
            sentence = sentence + currentSentence[i] + ",";
        }
        //Log.d("calculateSentenceCHK", sentence);
        chk = generateCheckSum(sentence);
        return chk;
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

    public String ReadResult(long timeout) {

        // Reads in data while data is available

        //setDataReady(false);
        this.exit = false;
        lastData = "";
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {


            while (this.exit == false) {
                if ((System.currentTimeMillis() - lastReceived) > timeout)
                    this.exit = true;
                if (getInputStream().available() > 0) {
                    // Read in the available character
                    char ch = (char) getInputStream().read();
                    lastData = lastData + ch;
                    if (ch == '$') {

                        // read entire sentence until the end
                        String tempBuff = "";
                        while (ch != ';') {
                            // this is not the end of our command
                            ch = (char) getInputStream().read();
                            if (ch != '\r')
                                if (ch != '\n')
                                    if (ch != ';')
                                        tempBuff = tempBuff
                                                + Character.toString(ch);
                        }
                        if (ch == ';') {
                            ch = (char) getInputStream().read();
                        }

                        // Sentence currentSentence = null;
                        String currentSentence[] = new String[50];
                        if (!tempBuff.isEmpty()) {
                            //currentSentence = readSentence(tempBuff);
                            currentSentence = tempBuff.split(",");

                            fullBuff = fullBuff + tempBuff;
                        }

                        long chk = 0;
                        switch (currentSentence[0]) {
                            case "telemetry":
                                if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                    chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                if (calculateSentenceCHK(currentSentence) == chk) {
                                    if (mHandler != null) {
                                        // Value 1 contain the current altitude
                                        if (currentSentence.length > 1)
                                            if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(1, String.valueOf(currentSentence[1])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(1, String.valueOf(0)).sendToTarget();
                                        // Value 2 lift off yes/no
                                        if (currentSentence.length > 2)
                                            if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(2, String.valueOf(currentSentence[2])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(2, String.valueOf(0)).sendToTarget();

                                        // Value 3 apogee fired yes/no
                                        if (currentSentence.length > 3)
                                            if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(3, String.valueOf(currentSentence[3])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(3, String.valueOf(0)).sendToTarget();
                                        //Value 4 apogee altitude
                                        if (currentSentence.length > 4)
                                            if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(4, String.valueOf(currentSentence[4])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(4, String.valueOf(0)).sendToTarget();
                                        // Value 5 main fired yes/no
                                        if (currentSentence.length > 5)
                                            if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(5, String.valueOf(currentSentence[5])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(5, String.valueOf(0)).sendToTarget();
                                        // Value 6 main altitude
                                        if (currentSentence.length > 6)
                                            if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(6, String.valueOf(currentSentence[6])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(6, String.valueOf(0)).sendToTarget();
                                        // Value 7 landed
                                        if (currentSentence.length > 7)
                                            if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(7, String.valueOf(currentSentence[7])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(7, String.valueOf(0)).sendToTarget();
                                        // value 8 time
                                        if (currentSentence.length > 8)
                                            if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(8, String.valueOf(currentSentence[8])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(8, String.valueOf(0)).sendToTarget();
                                        // Value 9 contains the output 1 status
                                        if (currentSentence.length > 9)
                                            if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(9, String.valueOf(currentSentence[9])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(9, String.valueOf(0)).sendToTarget();
                                        // Value 10 contains the output 2 status
                                        if (currentSentence.length > 10)
                                            if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(10, String.valueOf(currentSentence[10])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(10, String.valueOf(0)).sendToTarget();
                                        // Value 11 contains the output 3 status
                                        if (currentSentence.length > 11)
                                            if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(11, String.valueOf(currentSentence[11])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(11, String.valueOf(0)).sendToTarget();
                                        // Value 12 contains the output 4 status
                                        if (currentSentence.length > 12)
                                            if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(12, String.valueOf(currentSentence[12])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(12, String.valueOf(0)).sendToTarget();
                                        // Value 13 contains the battery voltage
                                        if (currentSentence.length > 13)
                                            if (currentSentence[13].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(13, String.valueOf(currentSentence[13])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(13, String.valueOf(0)).sendToTarget();
                                        // Value 14 contains the temperature
                                        if (currentSentence.length > 14)
                                            if (currentSentence[14].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(14, String.valueOf(currentSentence[14])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(14, String.valueOf(0)).sendToTarget();
                                        // Value 15 contains the eeprom
                                        if (currentSentence.length > 15)
                                            if (currentSentence[15].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(15, String.valueOf(currentSentence[15])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(15, String.valueOf(0)).sendToTarget();
                                        // Value 16 contains the number of flight
                                        if (currentSentence.length > 16)
                                            if (currentSentence[16].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(16, String.valueOf(currentSentence[16])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(16, String.valueOf(0)).sendToTarget();

                                        // Value 17 contains the latitude
                                        if (currentSentence.length > 17)
                                            if (currentSentence[17].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(17, String.valueOf(currentSentence[17])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(17, String.valueOf(0)).sendToTarget();
                                        // Value 18 contains the number of flight
                                        if (currentSentence.length > 18)
                                            if (currentSentence[18].matches("\\d+(?:\\.\\d+)?"))
                                                mHandler.obtainMessage(18, String.valueOf(currentSentence[18])).sendToTarget();
                                            else
                                                mHandler.obtainMessage(18, String.valueOf(0)).sendToTarget();
                                    }
                                }
                                break;

                            case "data":
                                if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                    chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                String flightName = "FlightXX";
                                if (calculateSentenceCHK(currentSentence) == chk) {
                                    // Value 1 contain the flight number
                                    if (currentSentence.length > 1)
                                        if (currentSentence[1].matches("\\d+(?:\\.\\d+)?")) {
                                            currentFlightNbr = Integer.valueOf(currentSentence[1]) + 1;
                                            if (currentFlightNbr < 10)
                                                flightName = "Flight " + "0" + currentFlightNbr;
                                            else
                                                flightName = "Flight " + currentFlightNbr;
                                        }
                                    // Value 2 contain the time
                                    // Value 3 contain the altitude
                                    // To do
                                    int value2 = 0, value3 = 0, value4 = 0, value5 = 0, value6 = 0, value7 = 0;
                                    if (currentSentence.length > 2)
                                        if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                            value2 = Integer.valueOf(currentSentence[2]);
                                        else
                                            value2 = 0;
                                    if (currentSentence.length > 3) {
                                        if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                            value3 = Integer.valueOf(currentSentence[3]);
                                        else
                                            value3 = 0;
                                        //add the altitude
                                        MyFlight.AddToFlight(value2,
                                                (long) (value3 * FEET_IN_METER), flightName, 0);

                                    }
                                    //temperature
                                    if (currentSentence.length > 4) {
                                        if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                            value4 = Integer.valueOf(currentSentence[4]);
                                        else
                                            value4 = 0;
                                        //add the temperature
                                        MyFlight.AddToFlight(value2,
                                                (long) (value4), flightName, 1);
                                    }

                                    //Alti GPS does a lot more !!!!
                                    //pressure
                                    if (currentSentence.length > 5) {
                                        if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                            value5 = Integer.valueOf(currentSentence[5]);
                                        else
                                            value5 = 0;
                                        //add the temperature
                                        MyFlight.AddToFlight(value2,
                                                (long) (value5), flightName, 2);
                                    }

                                    //Latitude
                                    if (currentSentence.length > 6) {
                                        if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                            value6 = Integer.valueOf(currentSentence[6]);
                                        else
                                            value6 = 0;
                                        //add the latitude
                                        MyFlight.AddToFlight(value2,
                                                (long) (value6), flightName, 3);
                                    }

                                    //longitude
                                    if (currentSentence.length > 7) {
                                        if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                            value7 = Integer.valueOf(currentSentence[7]);
                                        else
                                            value7 = 0;
                                        //add the longitude
                                        MyFlight.AddToFlight(value2,
                                                (long) (value7), flightName, 4);
                                    }
                                }
                                break;
                            case "alticonfig":
                                if (currentSentence[currentSentence.length - 1].matches("\\d+(?:\\.\\d+)?"))
                                    chk = Long.valueOf(currentSentence[currentSentence.length - 1]);
                                if (calculateSentenceCHK(currentSentence) == chk) {
                                    // Value 1 contains the units
                                    if (currentSentence.length > 1)
                                        if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setUnits(Integer.valueOf(currentSentence[1]));
                                        else
                                            AltiCfg.setUnits(0);
                                    // Value 2 contains beepingMode
                                    if (currentSentence.length > 2)
                                        if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setBeepingMode(Integer.valueOf(currentSentence[2]));
                                        else
                                            AltiCfg.setBeepingMode(0);
                                    // Value 3 contains output1
                                    if (currentSentence.length > 3)
                                        if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput1(Integer.valueOf(currentSentence[3]));
                                        else
                                            AltiCfg.setOutput1(0);
                                    // Value 4 contains output2
                                    if (currentSentence.length > 4)
                                        if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput2(Integer.valueOf(currentSentence[4]));
                                        else
                                            AltiCfg.setOutput2(0);

                                    // Value 5 contains output3
                                    if (currentSentence.length > 5)
                                        if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput3(Integer.valueOf(currentSentence[5]));
                                        else
                                            AltiCfg.setOutput3(0);

                                    // Value 6 contains supersonicYesNo
                                    if (currentSentence.length > 6)
                                        if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setSupersonicYesNo(Integer.valueOf(currentSentence[6]));
                                        else
                                            AltiCfg.setSupersonicYesNo(0);
                                    // Value 7 contains mainAltitude
                                    if (currentSentence.length > 7)
                                        if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setMainAltitude(Integer.valueOf(currentSentence[7]));
                                        else
                                            AltiCfg.setMainAltitude(50);
                                    // Value 8 contains AltimeterName
                                    if (currentSentence.length > 8)
                                        AltiCfg.setAltimeterName(currentSentence[8]);

                                    // Value 9 contains the altimeter major version
                                    if (currentSentence.length > 9)
                                        if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setAltiMajorVersion(Integer.valueOf(currentSentence[9]));
                                        else
                                            AltiCfg.setAltiMajorVersion(0);
                                    // Value 10 contain the altimeter minor version
                                    if (currentSentence.length > 10)
                                        if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setAltiMinorVersion(Integer.valueOf(currentSentence[10]));
                                        else
                                            AltiCfg.setAltiMinorVersion(0);
                                    // Value 11 contains the output 1 delay
                                    if (currentSentence.length > 11)
                                        if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput1Delay(Integer.valueOf(currentSentence[11]));
                                        else
                                            AltiCfg.setOutput1Delay(0);
                                    // Value 12 contains the output 2 delay
                                    if (currentSentence.length > 12)
                                        if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput2Delay(Integer.valueOf(currentSentence[12]));
                                        else
                                            AltiCfg.setOutput2Delay(0);
                                    // Value 13 contains the output 3 delay
                                    if (currentSentence.length > 13)
                                        if (currentSentence[13].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput3Delay(Integer.valueOf(currentSentence[13]));
                                        else
                                            AltiCfg.setOutput3Delay(0);
                                    // Value 14 contains the altimeter beeping frequency
                                    if (currentSentence.length > 14)
                                        if (currentSentence[14].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setBeepingFrequency(Integer.valueOf(currentSentence[14]));
                                        else
                                            AltiCfg.setBeepingFrequency(440);
                                    // Value 15 contains the number of measures for apogee
                                    if (currentSentence.length > 15)
                                        if (currentSentence[15].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setNbrOfMeasuresForApogee(Integer.valueOf(currentSentence[15]));
                                        else
                                            AltiCfg.setNbrOfMeasuresForApogee(0);
                                    // Value 16 contains the min recording altitude
                                    if (currentSentence.length > 16)
                                        if (currentSentence[16].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setEndRecordAltitude(Integer.valueOf(currentSentence[16]));
                                        else
                                            AltiCfg.setEndRecordAltitude(5);
                                    // Value 17 contains the record temp flag
                                    if (currentSentence.length > 17)
                                        if (currentSentence[17].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setRecordTemperature(Integer.valueOf(currentSentence[17]));
                                        else
                                            AltiCfg.setRecordTemperature(0);
                                    // Value 18 contains the supersonic delay flag
                                    if (currentSentence.length > 18)
                                        if (currentSentence[18].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setSupersonicDelay(Integer.valueOf(currentSentence[18]));
                                        else
                                            AltiCfg.setSupersonicDelay(0);
                                    // Value 19 contains the connection speed
                                    if (currentSentence.length > 19)
                                        if (currentSentence[19].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setConnectionSpeed(Integer.valueOf(currentSentence[19]));
                                        else
                                            AltiCfg.setConnectionSpeed(38400);
                                    // Value 20 contains the altimeter resolution
                                    if (currentSentence.length > 20)
                                        if (currentSentence[20].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setAltimeterResolution(Integer.valueOf(currentSentence[20]));
                                        else
                                            AltiCfg.setAltimeterResolution(0);
                                    // Value 21 contains the eeprom size
                                    if (currentSentence.length > 21)
                                        if (currentSentence[21].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setEepromSize(Integer.valueOf(currentSentence[21]));
                                        else
                                            AltiCfg.setEepromSize(512);
                                    // Value 22 contains switch beeps on or off
                                    if (currentSentence.length > 22)
                                        if (currentSentence[22].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setBeepOnOff(Integer.valueOf(currentSentence[22]));
                                        else
                                            AltiCfg.setBeepOnOff(0);
                                    // Value 23 contains the 4th output
                                    if (currentSentence.length > 23)
                                        if (currentSentence[23].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput4(Integer.valueOf(currentSentence[23]));
                                        else
                                            //We do not have a fourth output
                                            AltiCfg.setOutput4(-1);
                                    // Value 24 contains the output4 delay
                                    if (currentSentence.length > 24)
                                        if (currentSentence[24].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setOutput4Delay(Integer.valueOf(currentSentence[24]));
                                        else
                                            AltiCfg.setOutput4Delay(-1);
                                    // Value 25 contains liftOff Altitude
                                    if (currentSentence.length > 25)
                                        if (currentSentence[25].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setLiftOffAltitude(Integer.valueOf(currentSentence[25]));
                                        else
                                            //if you cannot read it, set it to 20 meters
                                            AltiCfg.setLiftOffAltitude(20);

                                    // value 26 contains battery type
                                    if (currentSentence.length > 26)
                                        if (currentSentence[26].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setBatteryType(Integer.valueOf(currentSentence[26]));
                                        else
                                            AltiCfg.setBatteryType(0);


                                    // Value 27 contains the servo 1 On position
                                    if (currentSentence.length > 27)
                                        if (currentSentence[27].matches("\\d+(?:\\.\\d+)?"))

                                            AltiCfg.setServo1OnPos(Integer.valueOf(currentSentence[27]));
                                        else
                                            AltiCfg.setServo1OnPos(-1);
                                    // Value 28 contains the servo 2 On position
                                    if (currentSentence.length > 28)
                                        if (currentSentence[28].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo2OnPos(Integer.valueOf(currentSentence[28]));
                                        else
                                            AltiCfg.setServo2OnPos(-1);
                                    // Value 29 contains the servo 3 On position
                                    if (currentSentence.length > 29)
                                        if (currentSentence[29].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo3OnPos(Integer.valueOf(currentSentence[29]));
                                        else
                                            AltiCfg.setServo3OnPos(-1);
                                    // Value 30 contains the servo 4 On position
                                    if (currentSentence.length > 30)
                                        if (currentSentence[30].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo4OnPos(Integer.valueOf(currentSentence[30]));
                                        else
                                            AltiCfg.setServo4OnPos(-1);
                                    // Value 31 contains servo 1 off position
                                    if (currentSentence.length > 31)
                                        if (currentSentence[31].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo1OffPos(Integer.valueOf(currentSentence[31]));
                                        else
                                            AltiCfg.setServo1OffPos(-1);
                                    // Value 32 contains servo 2 off position
                                    if (currentSentence.length > 32)
                                        if (currentSentence[32].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo2OffPos(Integer.valueOf(currentSentence[32]));
                                        else
                                            AltiCfg.setServo2OffPos(-1);
                                    // Value 33 contains servo 3 off position
                                    if (currentSentence.length > 33)
                                        if (currentSentence[33].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo3OffPos(Integer.valueOf(currentSentence[33]));
                                        else
                                            AltiCfg.setServo3OffPos(-1);
                                    // Value 34 contains servo 4 off position
                                    if (currentSentence.length > 34)
                                        if (currentSentence[34].matches("\\d+(?:\\.\\d+)?"))
                                            AltiCfg.setServo4OffPos(Integer.valueOf(currentSentence[34]));
                                        else
                                            AltiCfg.setServo4OffPos(-1);
                                    myMessage = myMessage + " " + "alticonfig";
                                } else {
                                    myMessage = myMessage + "KO" + "alticonfig";
                                }

                                break;

                            case "nbrOfFlight":
                                // Value 1 contains the number of flight
                                if (currentSentence.length > 1)
                                    if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                        NbrOfFlight = (Integer.valueOf(currentSentence[1]));
                                myMessage = myMessage + " " + "nbrOfFlight";
                                break;
                            case "start":
                                // We are starting reading data
                                setDataReady(false);
                                myMessage = "start";
                                break;
                            case "end":
                                // We have finished reading data
                                setDataReady(true);
                                myMessage = myMessage + " " + "end";
                                exit = true;
                                break;
                            case "OK":
                                setDataReady(true);
                                commandRet = currentSentence[0];
                                myMessage = "OK";
                                exit = true;
                                break;
                            case "KO":
                                setDataReady(true);
                                commandRet = currentSentence[0];
                                break;
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
            myMessage = myMessage + " " + "error:" + e.getMessage();
        }
        return myMessage;
    }

    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }


    public Configuration getAppLocal() {

        Locale locale = null;
        if (AppConf.getApplicationLanguage().equals("1")) {
            locale = Locale.FRENCH;//new Locale("fr_FR");
        } else if (AppConf.getApplicationLanguage().equals("2")) {
            locale = Locale.ENGLISH;//new Locale("en_US");
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

    public class GlobalConfig {

        SharedPreferences appConfig = null;
        SharedPreferences.Editor edit = null;
        AppConfigData appCfgData = null;
        //application language
        private String applicationLanguage = "0";
        //Graph units
        private String units = "0";

        //flight retrieval timeout
        private long flightRetrievalTimeout;
        //data retrieval timeout
        private long configRetrievalTimeout;

        //graph background color
        private String graphBackColor = "1";
        //graph color
        private String graphColor = "0";
        //graph font size
        private String fontSize = "10";
        // connection type is bluetooth
        private String connectionType = "0";
        // default baud rate for USB is 57600
        private String baudRate = "9";
        private String graphicsLibType = "0";

        public GlobalConfig() {
            appConfig = getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
            edit = appConfig.edit();
            appCfgData = new AppConfigData();

        }

        public void ResetDefaultConfig() {

            applicationLanguage = "0";
            graphBackColor = "1";
            graphColor = "0";
            fontSize = "10";
            units = "0";
            baudRate = "9";
            connectionType = "0";
            graphicsLibType = "0";
            /*edit.clear();
            edit.putString("AppLanguage","0");
            edit.putString("Units", "0");
            edit.putString("GraphColor", "0");
            edit.putString("GraphBackColor", "1");
            edit.putString("FontSize", "10");*/

        }

        public void ReadConfig() {
            try {
                String appLang;
                appLang = appConfig.getString("AppLanguage", "");
                if (!appLang.equals(""))
                    setApplicationLanguage(appLang);

                //Application Units
                String appUnit;
                appUnit = appConfig.getString("Units", "");
                if (!appUnit.equals(""))
                    setUnits(appUnit);

                //Graph color
                String graphColor;
                graphColor = appConfig.getString("GraphColor", "");
                if (!graphColor.equals(""))
                    setGraphColor(graphColor);

                //Graph Background color
                String graphBackColor;
                graphBackColor = appConfig.getString("GraphBackColor", "");
                if (!graphBackColor.equals(""))
                    setGraphBackColor(graphBackColor);

                //Font size
                String fontSize;
                fontSize = appConfig.getString("FontSize", "");
                if (!fontSize.equals(""))
                    setFontSize(fontSize);

                //Baud rate
                String baudRate;
                baudRate = appConfig.getString("BaudRate", "");
                if (!baudRate.equals(""))
                    setBaudRate(baudRate);

                //Connection type
                String connectionType;
                connectionType = appConfig.getString("ConnectionType", "");
                if (!connectionType.equals(""))
                    setConnectionType(connectionType);

                //Graphics Lib Type
                String graphicsLibType;
                graphicsLibType = appConfig.getString("GraphicsLibType", "1");
                if (!graphicsLibType.equals(""))
                    setGraphicsLibType(graphicsLibType);
            } catch (Exception e) {

            }
        }

        public void SaveConfig() {
            edit.putString("AppLanguage", getApplicationLanguage());
            edit.putString("Units", getUnits());
            edit.putString("GraphColor", getGraphColor());
            edit.putString("GraphBackColor", getGraphBackColor());
            edit.putString("FontSize", getFontSize());
            edit.putString("BaudRate", getBaudRate());
            edit.putString("ConnectionType", getConnectionType());
            edit.putString("GraphicsLibType", getGraphicsLibType());
            edit.commit();

        }

        public String getFontSize() {
            return fontSize;
        }

        public void setFontSize(String value) {
            fontSize = value;
        }

        public String getApplicationLanguage() {
            return applicationLanguage;
        }

        public void setApplicationLanguage(String value) {
            applicationLanguage = value;
        }

        //return the unit id
        public String getUnits() {
            return units;
        }

        public String getUnitsValue() {
            return appCfgData.getUnitsByNbr(Integer.parseInt(units));
        }

        //set the unit by id
        public void setUnits(String value) {
            units = value;
        }

        public String getGraphColor() {
            return graphColor;
        }

        public void setGraphColor(String value) {
            graphColor = value;
        }

        public String getGraphBackColor() {
            return graphBackColor;
        }

        public void setGraphBackColor(String value) {
            graphBackColor = value;
        }

        //get the id of the current connection type
        public String getConnectionType() {
            return connectionType;
        }

        //get the name of the current connection type
        public String getConnectionTypeValue() {
            return appCfgData.getConnectionTypeByNbr(Integer.parseInt(connectionType));
        }

        public void setConnectionType(String value) {
            connectionType = value;
        }

        public String getGraphicsLibType() {
            return graphicsLibType;
        }

        public String getGraphicsLibTypeValue() {
            return appCfgData.getGraphicsLibTypeByNbr(Integer.parseInt(graphicsLibType));
        }

        public void setGraphicsLibType(String value) {
            graphicsLibType = value;
        }

        public String getBaudRate() {
            return baudRate;
        }

        public String getBaudRateValue() {
            return appCfgData.getBaudRateByNbr(Integer.parseInt(baudRate));
        }

        public void setBaudRate(String value) {

            baudRate = value;
        }

        public int ConvertFont(int font) {
            return font + 8;
        }

        public int ConvertColor(int col) {

            int myColor = 0;

            switch (col) {

                case 0:
                    myColor = Color.BLACK;
                    break;

                case 1:
                    myColor = Color.WHITE;
                    break;
                case 2:
                    myColor = Color.MAGENTA;
                    break;
                case 3:
                    myColor = Color.BLUE;
                    break;
                case 4:
                    myColor = Color.YELLOW;
                    break;
                case 5:
                    myColor = Color.GREEN;
                    break;
                case 6:
                    myColor = Color.GRAY;
                    break;
                case 7:
                    myColor = Color.CYAN;
                    break;
                case 8:
                    myColor = Color.DKGRAY;
                    break;
                case 9:
                    myColor = Color.LTGRAY;
                    break;
                case 10:
                    myColor = Color.RED;
                    break;
            }
            return myColor;
        }
    }
}
