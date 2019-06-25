package com.altimeter.bdureau.bearconsole;
/**
 * @description:
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
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.Locale;

/**
 *   @description: This is quite a major class used everywhere because it can point to your connection, appconfig
 *   @author: boris.dureau@neuf.fr
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
        appendLog("connect:");
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
        //write("h;".toString());

        //flush();
        flush();
        clearInput();
        write("h;\n".toString());

        //appendLog("isConnectionValid: sent command\n");

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

        //appendLog("isConnectionValid:"+myMessage);
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

    public void appendLog(String text) {
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


    public void setExit(boolean b) {
        this.exit = b;
    }

    ;

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

                        //switch (currentSentence.keyword) {
                        switch (currentSentence[0]) {
                            case "telemetry":
                                if (mHandler != null) {
                                    // Value 1 contain the current altitude
                                    if (currentSentence.length > 1)
                                    if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                        mHandler.obtainMessage(1, String.valueOf(currentSentence[1])).sendToTarget();
                                    else
                                        mHandler.obtainMessage(1, String.valueOf(0)).sendToTarget();
                                    // Value 2 lift off yes/no
                                    //mHandler.obtainMessage(2, String.valueOf(currentSentence.value2)).sendToTarget();
                                    if (currentSentence.length > 2)
                                    if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                        mHandler.obtainMessage(2, String.valueOf(currentSentence[2])).sendToTarget();
                                    else
                                        mHandler.obtainMessage(2, String.valueOf(0)).sendToTarget();

                                    // Value 3 apogee fired yes/no
                                    //mHandler.obtainMessage(3, String.valueOf(currentSentence.value3)).sendToTarget();
                                    if (currentSentence.length > 3)
                                    if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                        mHandler.obtainMessage(3, String.valueOf(currentSentence[3])).sendToTarget();
                                    else
                                        mHandler.obtainMessage(3, String.valueOf(0)).sendToTarget();
                                    //Value 4 apogee altitude
                                    if (currentSentence.length > 4)
                                        //mHandler.obtainMessage(4, String.valueOf(currentSentence.value4)).sendToTarget();
                                        if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(4, String.valueOf(currentSentence[4])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(4, String.valueOf(0)).sendToTarget();
                                    // Value 5 main fired yes/no
                                    if (currentSentence.length > 5)
                                        //mHandler.obtainMessage(5, String.valueOf(currentSentence.value5)).sendToTarget();
                                        if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(5, String.valueOf(currentSentence[5])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(5, String.valueOf(0)).sendToTarget();
                                    // Value 6 main altitude
                                    if (currentSentence.length > 6)
                                        //mHandler.obtainMessage(6, String.valueOf(currentSentence.value6)).sendToTarget();
                                        if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(6, String.valueOf(currentSentence[6])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(6, String.valueOf(0)).sendToTarget();
                                    // Value 7 landed
                                    if (currentSentence.length > 7)
                                        //mHandler.obtainMessage(7, String.valueOf(currentSentence.value7)).sendToTarget();
                                        if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(7, String.valueOf(currentSentence[7])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(7, String.valueOf(0)).sendToTarget();
                                    // value 8 time
                                    if (currentSentence.length > 8)
                                        //                                   mHandler.obtainMessage(8, String.valueOf(currentSentence.value8)).sendToTarget();
                                        if (currentSentence[8].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(8, String.valueOf(currentSentence[8])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(8, String.valueOf(0)).sendToTarget();
                                    // Value 9 contains the output 1 status
                                    //mHandler.obtainMessage(9, String.valueOf(currentSentence.value9)).sendToTarget();
                                    if (currentSentence.length > 9)
                                        if (currentSentence[9].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(9, String.valueOf(currentSentence[9])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(9, String.valueOf(0)).sendToTarget();
                                    // Value 10 contains the output 2 status
                                    if (currentSentence.length > 10)
                                        //mHandler.obtainMessage(10, String.valueOf(currentSentence.value10)).sendToTarget();
                                        if (currentSentence[10].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(10, String.valueOf(currentSentence[10])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(10, String.valueOf(0)).sendToTarget();
                                    // Value 11 contains the output 3 status
                                    //mHandler.obtainMessage(11, String.valueOf(currentSentence.value11)).sendToTarget();
                                    if (currentSentence.length > 11)
                                        if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(11, String.valueOf(currentSentence[11])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(11, String.valueOf(0)).sendToTarget();
                                    // Value 12 contains the output 4 status
                                    if (currentSentence.length > 12)
                                        //mHandler.obtainMessage(12, String.valueOf(currentSentence.value12)).sendToTarget();
                                        if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(12, String.valueOf(currentSentence[12])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(12, String.valueOf(0)).sendToTarget();
                                    // Value 13 contains the battery voltage
                                    if (currentSentence.length > 13)
                                        //mHandler.obtainMessage(13, String.valueOf(currentSentence.value13)).sendToTarget();
                                        if (currentSentence[13].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(13, String.valueOf(currentSentence[13])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(13, String.valueOf(0)).sendToTarget();
                                    // Value 14 contains the temperature
                                    //mHandler.obtainMessage(14, String.valueOf(currentSentence.value14)).sendToTarget();
                                    if (currentSentence.length > 14)
                                        if (currentSentence[14].matches("\\d+(?:\\.\\d+)?"))
                                            mHandler.obtainMessage(14, String.valueOf(currentSentence[14])).sendToTarget();
                                        else
                                            mHandler.obtainMessage(14, String.valueOf(0)).sendToTarget();
                                }
                                break;

                            case "data":
                                // Value 1 contain the flight number
                                if (currentSentence.length > 1)
                                if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                    //currentFlightNbr = (int) currentSentence.value1 + 1;
                                    currentFlightNbr = Integer.valueOf(currentSentence[1]) + 1;
                                // Value 2 contain the time
                                // Value 3 contain the altitude
                                // To do
                                int value2=0, value3=0;
                                if (currentSentence.length > 2)
                                if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                    value2 = Integer.valueOf(currentSentence[2]);
                                else
                                    value2 = 0;
                                if (currentSentence.length > 3)
                                if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                    value3 = Integer.valueOf(currentSentence[3]);
                                else
                                    value3 = 0;
                                if (currentFlightNbr < 10)
                                    MyFlight.AddToFlight(value2,
                                            (long) (value3 * FEET_IN_METER), "Flight "
                                                    + "0" + currentFlightNbr);
                                else
                                    MyFlight.AddToFlight(value2,
                                            (long) (value3 * FEET_IN_METER), "Flight "
                                                    + currentFlightNbr);

                                break;
                            case "alticonfig":

                                // Value 1 contain the units
                                if (currentSentence.length > 1)
                                if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setUnits(Integer.valueOf(currentSentence[1]));
                                else
                                    AltiCfg.setUnits(0);
                                // Value 2 contain beepingMode
                                if (currentSentence.length > 2)
                                if (currentSentence[2].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setBeepingMode(Integer.valueOf(currentSentence[2]));
                                else
                                    AltiCfg.setBeepingMode(0);
                                // Value 3 contain output1
                                if (currentSentence.length > 3)
                                if (currentSentence[3].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setOutput1(Integer.valueOf(currentSentence[3]));
                                else
                                    AltiCfg.setOutput1(0);
                                // Value 4 contain output2
                                if (currentSentence.length > 4)
                                if (currentSentence[4].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setOutput2(Integer.valueOf(currentSentence[4]));
                                else
                                    AltiCfg.setOutput2(0);

                                // Value 5 contain output3
                                if (currentSentence.length > 5)
                                if (currentSentence[5].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setOutput3(Integer.valueOf(currentSentence[5]));
                                else
                                    AltiCfg.setOutput3(0);

                                // Value 6 contain supersonicYesNo
                                if (currentSentence.length > 6)
                                if (currentSentence[6].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setSupersonicYesNo(Integer.valueOf(currentSentence[6]));
                                else
                                    AltiCfg.setSupersonicYesNo(0);
                                // Value 7 contain mainAltitude
                                if (currentSentence.length > 7)
                                if (currentSentence[7].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setMainAltitude(Integer.valueOf(currentSentence[7]));
                                else
                                    AltiCfg.setMainAltitude(50);
                                // Value 8 contain AltimeterName
                                if (currentSentence.length > 8)
                                AltiCfg.setAltimeterName(currentSentence[8]);

                                // Value 9 contain the altimeter major version
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
                                // Value 11 contain the output 1 delay
                                if (currentSentence.length > 11)
                                if (currentSentence[11].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setOutput1Delay(Integer.valueOf(currentSentence[11]));
                                else
                                    AltiCfg.setOutput1Delay(0);
                                // Value 12 contain the output 2 delay
                                if (currentSentence.length > 12)
                                if (currentSentence[12].matches("\\d+(?:\\.\\d+)?"))
                                    AltiCfg.setOutput2Delay(Integer.valueOf(currentSentence[12]));
                                else
                                    AltiCfg.setOutput2Delay(0);
                                // Value 13 contain the output 3 delay
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
                                // Value 15
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
                                    //if ((int) currentSentence.value20 !=-1)
                                    AltiCfg.setAltimeterResolution(Integer.valueOf(currentSentence[20]));
                                else
                                    AltiCfg.setAltimeterResolution(0);
                                // Value 21 contains the eeprom size
                                if (currentSentence.length > 21)
                                if (currentSentence[21].matches("\\d+(?:\\.\\d+)?"))
                                    //if ((int) currentSentence.value21 !=-1)
                                    AltiCfg.setEepromSize(Integer.valueOf(currentSentence[21]));
                                else
                                    AltiCfg.setEepromSize(512);
                                // switch beeps on or off
                                if (currentSentence.length > 22)
                                if (currentSentence[22].matches("\\d+(?:\\.\\d+)?"))
                                    //if ((int) currentSentence.value22 !=-1)
                                    AltiCfg.setBeepOnOff(Integer.valueOf(currentSentence[22]));
                                else
                                    AltiCfg.setBeepOnOff(0);
                                if (currentSentence.length > 23)
                                    if (currentSentence[23].matches("\\d+(?:\\.\\d+)?"))
                                        AltiCfg.setOutput4(Integer.valueOf(currentSentence[23]));
                                    else
                                        //We do not have a fourth output
                                        AltiCfg.setOutput4(-1);
                                // Value 24 contain the output4 delay
                                if (currentSentence.length > 24)
                                    if (currentSentence[24].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value24 !=-1)
                                        AltiCfg.setOutput4Delay(Integer.valueOf(currentSentence[24]));
                                    else
                                        AltiCfg.setOutput4Delay(-1);
                                // Value 25 contain
                                if (currentSentence.length > 25)
                                    if (currentSentence[25].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value25 !=-1)
                                        AltiCfg.setServo1OnPos(Integer.valueOf(currentSentence[25]));
                                    else
                                        AltiCfg.setServo1OnPos(-1);
                                // Value 26 contain
                                if (currentSentence.length > 26)
                                    if (currentSentence[26].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value26 !=-1)
                                        AltiCfg.setServo2OnPos(Integer.valueOf(currentSentence[26]));
                                    else
                                        AltiCfg.setServo2OnPos(-1);
                                // Value 27 contain
                                if (currentSentence.length > 27)
                                    if (currentSentence[27].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value27 !=-1)
                                        AltiCfg.setServo3OnPos(Integer.valueOf(currentSentence[27]));
                                    else
                                        AltiCfg.setServo3OnPos(-1);
                                // Value 28 contain
                                if (currentSentence.length > 28)
                                    if (currentSentence[28].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value28 !=-1)
                                        AltiCfg.setServo4OnPos(Integer.valueOf(currentSentence[28]));
                                    else
                                        AltiCfg.setServo4OnPos(-1);
                                // Value 29 contain
                                if (currentSentence.length > 29)
                                    if (currentSentence[29].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value29 !=-1)
                                        AltiCfg.setServo1OffPos(Integer.valueOf(currentSentence[29]));
                                    else
                                        AltiCfg.setServo1OffPos(-1);
                                // Value 30 contain
                                if (currentSentence.length > 30)
                                    if (currentSentence[30].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value30 !=-1)
                                        AltiCfg.setServo2OffPos(Integer.valueOf(currentSentence[30]));
                                    else
                                        AltiCfg.setServo2OffPos(-1);
                                // Value 31 contain
                                if (currentSentence.length > 31)
                                    if (currentSentence[31].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value31 !=-1)
                                        AltiCfg.setServo3OffPos(Integer.valueOf(currentSentence[31]));
                                    else
                                        AltiCfg.setServo3OffPos(-1);
                                // Value 32 contain
                                if (currentSentence.length > 32)
                                    if (currentSentence[32].matches("\\d+(?:\\.\\d+)?"))
                                        //if ((int) currentSentence.value32 !=-1)
                                        AltiCfg.setServo4OffPos(Integer.valueOf(currentSentence[32]));
                                    else
                                        AltiCfg.setServo4OffPos(-1);

                                //DataReady = true;
                                myMessage = myMessage + " " + "alticonfig";
                                break;
                            case "nbrOfFlight":
                                // Value 1 contains the number of flight
                                if (currentSentence[1].matches("\\d+(?:\\.\\d+)?"))
                                    NbrOfFlight = Integer.valueOf(currentSentence[1]);
                                break;
                            case "start":
                                //appendLog("Start");
                                // We are starting reading data
                                setDataReady(false);
                                myMessage = "start";
                                break;
                            case "end":
                                //appendLog("end");
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
            //lastTempBuf = fullBuff;
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

    /* public Sentence readSentence(String tempBuff) {
         // we have a sentence let's find out what it is
         String tempArray[] = tempBuff.split(",");
         Sentence sentence = new Sentence();

         sentence.keyword = tempArray[0];

         if (tempArray.length > 1)
             if(tempArray[1].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value1 = Integer.parseInt(tempArray[1]);
             else
                 sentence.value1 =-1;
         if (tempArray.length > 2)
             if(tempArray[2].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value2 = Integer.parseInt(tempArray[2]);
             else
                 sentence.value2 =-1;

         if (tempArray.length > 3)
             if(tempArray[3].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value3 = Integer.parseInt(tempArray[3]);
             else
                 sentence.value3 =-1;

         if (tempArray.length > 4)
             if(tempArray[4].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value4 = Integer.parseInt(tempArray[4]);
             else
                 sentence.value4 = -1;

         if (tempArray.length > 5)
             if(tempArray[5].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value5 = Integer.parseInt(tempArray[5]);
             else
                 sentence.value5 = -1;

         if (tempArray.length > 6)
             if(tempArray[6].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value6 = Integer.parseInt(tempArray[6]);
             else
                 sentence.value6 = -1;

         if (tempArray.length > 7)
             if(tempArray[7].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value7 = Integer.parseInt(tempArray[7]);
             else
                 sentence.value7 = -1;

         if (tempArray.length > 8)
             sentence.value8 = tempArray[8];
         if (tempArray.length > 9)
             if(tempArray[9].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value9 = Integer.parseInt(tempArray[9]);
             else
                 sentence.value9 = -1;

         if (tempArray.length > 10)
             if(tempArray[10].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value10 = Integer.parseInt(tempArray[10]);
             else
                 sentence.value10 = -1;

         if (tempArray.length > 11)
             if(tempArray[11].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value11 = Integer.parseInt(tempArray[11]);
             else
                 sentence.value11 = -1;

         if (tempArray.length > 12)
             if(tempArray[12].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value12 = Integer.parseInt(tempArray[12]);
             else
                 sentence.value12 = -1;

         if (tempArray.length > 13)
             if(tempArray[13].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value13 = Integer.parseInt(tempArray[13]);
             else
                 sentence.value13 = -1;

         if (tempArray.length > 14)
             if(tempArray[14].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value14 = Integer.parseInt(tempArray[14]);
             else
                 sentence.value14 = -1;
         if (tempArray.length > 15)
             if(tempArray[15].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value15 = Integer.parseInt(tempArray[15]);
             else
                 sentence.value15 = -1;
         if (tempArray.length > 16)
             if(tempArray[16].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value16 = Integer.parseInt(tempArray[16]);
             else
                 sentence.value16 = -1;
         if (tempArray.length > 17)
             if(tempArray[17].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value17 = Integer.parseInt(tempArray[17]);
             else
                 sentence.value17 = -1;
         if (tempArray.length > 18)
             if(tempArray[18].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value18 = Integer.parseInt(tempArray[18]);
             else
                 sentence.value18 = -1;
         if (tempArray.length > 19)
             if(tempArray[19].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value19 = Integer.parseInt(tempArray[19]);
             else
                 sentence.value19 = -1;
         if (tempArray.length > 20)
             if(tempArray[20].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value20 = Integer.parseInt(tempArray[20]);
             else
                 sentence.value20 = -1;
         if (tempArray.length > 21)
             if(tempArray[21].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value21 = Integer.parseInt(tempArray[21]);
             else
                 sentence.value21 = -1;
         if (tempArray.length > 22)
             if(tempArray[22].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value22 = Integer.parseInt(tempArray[22]);
             else
                 sentence.value22 = -1;
         if (tempArray.length > 23)
             if(tempArray[23].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value23 = Integer.parseInt(tempArray[23]);
             else
                 sentence.value23 = -1;
         if (tempArray.length > 24)
             if(tempArray[24].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value24 = Integer.parseInt(tempArray[24]);
             else
                 sentence.value24 = -1;
         if (tempArray.length > 25)
             if(tempArray[25].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value25 = Integer.parseInt(tempArray[25]);
             else
                 sentence.value25 = -1;
         if (tempArray.length > 26)
             if(tempArray[26].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value26 = Integer.parseInt(tempArray[26]);
             else
                 sentence.value26 = -1;
         if (tempArray.length > 27)
             if(tempArray[27].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value27 = Integer.parseInt(tempArray[27]);
             else
                 sentence.value27 = -1;
         if (tempArray.length > 28)
             if(tempArray[28].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value28 = Integer.parseInt(tempArray[28]);
             else
                 sentence.value28 = -1;
         if (tempArray.length > 29)
             if(tempArray[29].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value29 = Integer.parseInt(tempArray[29]);
             else
                 sentence.value29 = -1;
         if (tempArray.length > 30)
             if(tempArray[30].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value30 = Integer.parseInt(tempArray[30]);
             else
                 sentence.value30 = -1;

         if (tempArray.length > 31)
             if(tempArray[31].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value31 = Integer.parseInt(tempArray[31]);
             else
                 sentence.value31 = -1;
         if (tempArray.length > 32)
             if(tempArray[32].matches("\\d+(?:\\.\\d+)?"))
                 sentence.value32 = Integer.parseInt(tempArray[32]);
             else
                 sentence.value32 = -1;

         return sentence;
     }


     public class Sentence {

         public String keyword;
         public long value1;
         public long value2;
         public long value3;
         public long value4;
         public long value5;
         public long value6;
         public long value7;
         public String value8;
         public long value9;
         public long value10;
         public long value11;
         public long value12;
         public long value13;
         public long value14;
         public long value15;
         public long value16;
         public long value17;
         public long value18;
         public long value19;
         public long value20;
         public long value21;
         public long value22;
         public long value23;
         public long value24;
         public long value25;
         public long value26;
         public long value27;
         public long value28;
         public long value29;
         public long value30;
         public long value31;
         public long value32;
     }*/
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
