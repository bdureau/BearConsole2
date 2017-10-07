package com.altimeter.bdureau.bearconsole;

import android.app.Application;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.os.Handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
//import java.io.OutputStream;
import java.util.Locale;

/**
 * Created by BDUREAU on 07/09/2017.
 */
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
    private boolean exit =false;
    private globalConfig AppConf=null;
    private String address;
    private String myTypeOfConnection ="bluetooth";// "USB";//"bluetooth";

    private bluetoothConnection BTCon = null;
    private usbConnection UsbCon =null;

    private Handler mHandler;
    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }
    @Override
    public void onCreate() {

        super.onCreate();
        AltiCfg = new AltiConfigData();
        MyFlight = new FlightData();
        AppConf = new globalConfig();
        AppConf.ReadConfig();
        BTCon = new bluetoothConnection();
        UsbCon = new usbConnection();
        if (AppConf.getConnectionType().equals("0"))
            //bluetooth
            myTypeOfConnection= "bluetooth";
        else
            myTypeOfConnection ="usb";



    }

    public void setConnectionType (String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }
    public String getConnectionType () {
        return myTypeOfConnection;
    }
    public void setAddress(String bTAddress) {

        address = bTAddress;

    }
    public String getAddress () {return address;}

    public InputStream getInputStream(){
        InputStream tmpIn=null;
        if(myTypeOfConnection.equals("bluetooth")) {
            tmpIn= BTCon.getInputStream();
        }
        else {
            tmpIn= UsbCon.getInputStream();
        }
        return tmpIn;
    }
    public void setConnected(boolean Connected) {
        if(myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        }
        else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public boolean getConnected() {
        boolean ret = false;
        if(myTypeOfConnection.equals("bluetooth")) {
            ret= BTCon.getBTConnected();
        }
        else {
            ret=UsbCon.getUSBConnected();
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
    public boolean connect() {
        boolean state=false;
        if(myTypeOfConnection.equals( "bluetooth")) {
            state = BTCon.connect(address);
        }
        return state;
    }

    public boolean connect(UsbManager usbManager,UsbDevice device, int baudRate) {
        boolean state=false;
        if(myTypeOfConnection.equals( "usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
        }
        return state;
    }
    public void Disconnect() {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.Disconnect();
        }
        else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.write(data);
        }
        else {
            UsbCon.write(data);
        }
    }
    public void clearInput() {
        if(myTypeOfConnection.equals( "bluetooth")) {
            BTCon.clearInput();
        }
        else {
            UsbCon.clearInput();
        }
    }
    public void initFlightData() {
        MyFlight = new FlightData();

        if(AppConf.getUnits().equals("0"))
        {
            FEET_IN_METER =1;
        }
        else
        {
            FEET_IN_METER = 3.28084;
        }
    }

    public void appendLog(String text)
    {
        File logFile = new File("sdcard/debugfile.txt");
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


    public void setExit(boolean b) {this.exit=b;};

    public String ReadResult() {

        // Reads in data while data is available

        //setDataReady(false);
        this.exit =false;
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {


            while (this.exit==false) {
                //appendLog("log1");
                if (getInputStream().available() > 0) {
                    //appendLog("log2");
                    // Read in the available character
                    char ch = (char) getInputStream().read();
                    //appendLog("log3");
                    if (ch == '$') {

                        // read entire sentence until the end
                        String tempBuff = "";
                        while (ch != ';') {
                            // this is not the end of our command
                            //appendLog("log4");
                            ch = (char) getInputStream().read();
                            //appendLog("log5");
                            if (ch != '\r')
                                if (ch != '\n')
                                    if (ch != ';')
                                        tempBuff = tempBuff
                                                + Character.toString(ch);
                        }
                        if (ch == ';') {
                           // appendLog("log6");
                            ch = (char) getInputStream().read();
                           // appendLog("log7");
                            //appendLog("log7-0:"+tempBuff);
                        }

                        Sentence currentSentence = null;
                        if (!tempBuff.isEmpty()) {
                           // appendLog("log7-1:");
                            currentSentence = readSentence(tempBuff);

                            fullBuff = fullBuff + tempBuff;
                           // appendLog("log7-2:");
                        }
                        //appendLog("log8:"+ currentSentence.keyword );
                        switch (currentSentence.keyword) {
                            case "telemetry":
                                if (mHandler != null) {
                                    // Value 1 contain the current altitude
                                    // TelemetryDT.setCurrentAltitude((long)currentSentence.value1);
                                    mHandler.obtainMessage(1, String.valueOf(currentSentence.value1)).sendToTarget();
                                    // Value 2 lift off yes/no
                                    //TelemetryDT.setLiftOff((long)currentSentence.value2);
                                    mHandler.obtainMessage(2, String.valueOf(currentSentence.value2)).sendToTarget();

                                    // Value 3 apogee fired yes/no
                                    //TelemetryDT.setApogeeFired((long)currentSentence.value3);
                                    mHandler.obtainMessage(3, String.valueOf(currentSentence.value3)).sendToTarget();

                                    //Value 4 apogee altitude
                                    mHandler.obtainMessage(4, String.valueOf(currentSentence.value4)).sendToTarget();
                                    //TelemetryDT.setApogeeAltitude((long)currentSentence.value4);
                                    // Value 5 main fired yes/no
                                    mHandler.obtainMessage(5, String.valueOf(currentSentence.value5)).sendToTarget();
                                    //TelemetryDT.setApogeeFired((long)currentSentence.value5);
                                    // Value 6 main altitude
                                    //TelemetryDT.setMainAltitude((long)currentSentence.value6);
                                    mHandler.obtainMessage(6, String.valueOf(currentSentence.value6)).sendToTarget();
                                    // Value 7 landed
                                    //TelemetryDT.setApogeeFired((long)currentSentence.value7);
                                    mHandler.obtainMessage(7, String.valueOf(currentSentence.value7)).sendToTarget();
                                }
                                break;
                            case "data":
                                // Value 1 contain the flight number
                                currentFlightNbr = (int) currentSentence.value1 + 1;
                                // Value 2 contain the time
                                // Value 3 contain the altitude
                                // To do
                                if (currentFlightNbr < 10)
                                    MyFlight.AddToFlight(currentSentence.value2,
                                            (long) (currentSentence.value3 * FEET_IN_METER), "Flight "
                                                    + "0" + currentFlightNbr);
                                else
                                    MyFlight.AddToFlight(currentSentence.value2,
                                            (long) (currentSentence.value3 * FEET_IN_METER), "Flight "
                                                    + currentFlightNbr);


                                //appendLog("data "+ currentFlightNbr + " " + currentSentence.value2 + " " + currentSentence.value3 );

                                break;
                            case "alticonfig":

                                // Value 1 contain the units
                                AltiCfg.setUnits((int) currentSentence.value1);

                                // Value 2 contain beepingMode
                                AltiCfg.setBeepingMode((int) currentSentence.value2);

                                // Value 3 contain output1
                                AltiCfg.setOutput1((int) currentSentence.value3);

                                // Value 4 contain output2
                                AltiCfg.setOutput2((int) currentSentence.value4);

                                // Value 5 contain output3
                                AltiCfg.setOutput3((int) currentSentence.value5);

                                // Value 6 contain supersonicYesNo
                                AltiCfg.setSupersonicYesNo((int) currentSentence.value6);

                                // Value 7 contain mainAltitude
                                AltiCfg.setMainAltitude((int) currentSentence.value7);

                                // Value 8 contain AltimeterName
                                AltiCfg.setAltimeterName(currentSentence.value8);

                                // Value 9 contain the altimeter major version
                                AltiCfg.setAltiMajorVersion((int) currentSentence.value9);

                                // Value 10 contain the altimeter minor version
                                AltiCfg.setAltiMinorVersion((int) currentSentence.value10);


                                AltiCfg.setOutput1Delay((int) currentSentence.value11);
                                AltiCfg.setOutput2Delay((int) currentSentence.value12);
                                AltiCfg.setOutput3Delay((int) currentSentence.value13);
                                AltiCfg.setBeepingFrequency((int) currentSentence.value14);

                                //DataReady = true;
                                myMessage = myMessage + " " + "alticonfig";
                                break;
                            case "nbrOfFlight":
                                // Value 1 contains the number of flight
                                NbrOfFlight = (int) currentSentence.value1;
                                break;
                            case "start":
                                appendLog("Start");
                                // We are starting reading data
                                setDataReady(false);
                                myMessage = "start";
                                break;
                            case "end":
                                appendLog("end");
                                // We have finished reading data
                                setDataReady(true);
                                myMessage = myMessage + " " + "end";
                                exit=true;
                                break;
                            case "OK":
                                setDataReady(true);
                                commandRet = currentSentence.keyword;

                                break;
                            case "KO":
                                setDataReady(true);
                                commandRet = currentSentence.keyword;

                                break;
                            case "UNKNOWN":
                                setDataReady(true);
                                commandRet = currentSentence.keyword;

                                break;
                            default:

                                break;
                        }

                    }
                }
            }
        } catch (IOException e) {
            //lastTempBuf = fullBuff;
            myMessage = myMessage + " " + "error";
        }
        return myMessage;
    }
    public void setDataReady(boolean value) {
        DataReady = value;
    }

    public boolean getDataReady() {
        return DataReady;
    }

    public Sentence readSentence(String tempBuff) {
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
        return sentence;
    }


    class Sentence {

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
    }
    public Configuration getAppLocal() {

        Locale locale=null;
        if (AppConf.getApplicationLanguage().equals("1")) {
            locale = Locale.FRENCH;//new Locale("fr_FR");
        }
        else if(AppConf.getApplicationLanguage().equals("2")) {
            locale = Locale.ENGLISH;//new Locale("en_US");
        }
        else if(AppConf.getApplicationLanguage().equals("2"))  {
            locale =Locale.getDefault();
        }


        Configuration config = new Configuration();

        config.locale= locale;
        return config;

    }


    public globalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(globalConfig value) {
        AppConf=value;
    }

    public class globalConfig {

        SharedPreferences appConfig =null;
        SharedPreferences.Editor edit = null;
        //application language
        private String applicationLanguage ="0";
        //Graph units
        private String units="0";

        //flight retrieval timeout
        private long flightRetrievalTimeout;
        //data retrieval timeout
        private long configRetrievalTimeout;

        //graph background color
        private String graphBackColor="1";
        //graph color
        private String graphColor="0";
        //graph font size
        private String fontSize="10";
        // connection type is bluetooth
        private String connectionType = "0";
        // default baud rate for USB is 57600
        private String baudRate = "9";

        public globalConfig ()
        {
            appConfig  = getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
            edit = appConfig.edit();
        }

        public void ResetDefaultConfig() {

            applicationLanguage ="0";
            graphBackColor="1";
            graphColor="0";
            fontSize="10";
            units="0";
            baudRate="9";
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
                fontSize =appConfig.getString("FontSize","");
                if (!fontSize.equals(""))
                    setFontSize(fontSize);

                //Baud rate
                String baudRate;
                baudRate = appConfig.getString("BaudRate","");
                if(!baudRate.equals(""))
                    setBaudRate(baudRate);

                //Connection type
                String connectionType;
                connectionType = appConfig.getString("ConnectionType","");
                if(!connectionType.equals(""))
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

        public void setFontSize(String value){
            fontSize=value;
        }

        public String getApplicationLanguage() {

            return applicationLanguage;
        }

        public void setApplicationLanguage(String value) {

            applicationLanguage=value;
        }

        public String getUnits() {

            return units;
        }

        public void setUnits(String value) {

            units=value;
        }
        public String getGraphColor() {

            return graphColor;
        }

        public void setGraphColor(String value) {

            graphColor=value;
        }
        public String getGraphBackColor() {

            return graphBackColor;
        }

        public void setGraphBackColor(String value) {

            graphBackColor=value;
        }

        public String getConnectionType() {
            return connectionType;
        }
        public void setConnectionType(String value) {

            connectionType=value;
        }

        public String getBaudRate() {
            return baudRate;
        }
        public void setBaudRate(String value) {

            baudRate=value;
        }

        public int ConvertFont(int font) {
            return font+8;
        }
        public int ConvertColor(int col) {

            int mycolor=0;

            switch (col) {

                case 0:
                    mycolor = Color.BLACK;
                    break;

                case 1:
                    mycolor = Color.WHITE;
                    break;
                case 2:
                    mycolor = Color.MAGENTA;
                    break;
                case 3:
                    mycolor = Color.BLUE;
                    break;
                case 4:
                    mycolor = Color.YELLOW;
                    break;
                case 5:
                    mycolor = Color.GREEN;
                    break;
                case 6:
                    mycolor = Color.GRAY;
                    break;
                case 7:
                    mycolor = Color.CYAN;
                    break;
                case 8:
                    mycolor =  Color.DKGRAY;
                    break;
                case 9:
                    mycolor =  Color.LTGRAY;
                    break;
                case 10:
                    mycolor =  Color.RED;
                    break;
            }
            return mycolor;
        }
    }
}
