package com.altimeter.bdureau.bearconsole;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;

import java.io.IOException;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;


/**
 * Created by BDUREAU on 24/06/2016.
 */
public class BluetoothApplication extends Application {
    BluetoothDevice device;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private String address = null;

    // Store number of flight
    public int NbrOfFlight = 0;
    public int currentFlightNbr = 0;
    private FlightData MyFlight = null;
    private AltiConfigData AltiCfg = null;
    //private TelemetryData TelemetryDT=null;
    private static boolean DataReady = false;
    public long lastReceived = 0;
    public String commandRet = "";
    //public String lastTempBuf;
    //private String AltiUnit ="Meters";
    private double FEET_IN_METER = 1;
    private boolean exit =false;
    // input and output streams for sending and receiving data
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private globalConfig AppConf=null;
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


    public void setAddress(String bTAddress) {address = bTAddress;}
    public String getAddress () {return address;}
    public globalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(globalConfig value) {
        AppConf=value;
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

   /* public TelemetryData getTelemetryData() {
        return TelemetryDT;
    }*/

    public boolean connect(String address) {
        boolean state;
        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
            setBtSocket(dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID));//create a RFCOMM (SPP) connection
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


    public void clearInput() {
        try {
            btSocket.getInputStream().skip(btSocket.getInputStream().available());
        } catch (IOException e) {
        }
    }

    public void initFlightData() {
        MyFlight = new FlightData();
        //System.out.println(pAltiUnit);
        if(AppConf.getUnits().equals("0"))
        {
           FEET_IN_METER =1;
        }
        else
        {
            FEET_IN_METER = 3.28084;
        }
    }


    public void setExit(boolean b) {this.exit=b;};

    public String ReadResult() {

        // Reads in data while data is available
        // while (event.getEventType()== SerialPortEvent.DATA_AVAILABLE )
        // if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
        //setDataReady(false);
        this.exit =false;
        String fullBuff = "";
        String myMessage = "";
        lastReceived = System.currentTimeMillis();
        try {
            /*InputStream tmpIn = null;
            tmpIn=btSocket.getInputStream();*/

            //while (btSocket.getInputStream().available() > 0) {
            while (this.exit==false) {

                //if (tmpIn.available() > 0) {
                if (btSocket.getInputStream().available() > 0) {

                    // Read in the available character
                    //char ch = (char) tmpIn.read();
                    char ch = (char) btSocket.getInputStream().read();

                    if (ch == '$') {

                        // read entire sentence until the end
                        String tempBuff = "";
                        while (ch != ';') {
                            // this is not the end of our command
                            //ch = (char) tmpIn.read();
                            ch = (char) btSocket.getInputStream().read();
                            if (ch != '\r')
                                if (ch != '\n')
                                    if (ch != ';')
                                        tempBuff = tempBuff
                                                + Character.toString(ch);
                        }
                        if (ch == ';')
                            //ch = (char) tmpIn.read();
                            ch = (char) btSocket.getInputStream().read();

                        Sentence currentSentence = null;
                        if (!tempBuff.isEmpty()) {
                            currentSentence = readSentence(tempBuff);

                            fullBuff = fullBuff + tempBuff;
                        }

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
                                // System.out.println(tempBuff);
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
                                // We are starting reading data
                                setDataReady(false);
                                myMessage = "start";
                                break;
                            case "end":
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
            sentence.value1 = Integer.parseInt(tempArray[1]);

        if (tempArray.length > 2)
            sentence.value2 = Integer.parseInt(tempArray[2]);

        if (tempArray.length > 3)
            sentence.value3 = Integer.parseInt(tempArray[3]);
        if (tempArray.length > 4)
            sentence.value4 = Integer.parseInt(tempArray[4]);
        if (tempArray.length > 5)
            sentence.value5 = Integer.parseInt(tempArray[5]);
        if (tempArray.length > 6)
            sentence.value6 = Integer.parseInt(tempArray[6]);
        if (tempArray.length > 7)
            sentence.value7 = Integer.parseInt(tempArray[7]);
        if (tempArray.length > 8)
            sentence.value8 = tempArray[8];
        if (tempArray.length > 9)
            sentence.value9 = Integer.parseInt(tempArray[9]);
        if (tempArray.length > 10)
            sentence.value10 = Integer.parseInt(tempArray[10]);
        if (tempArray.length > 11)
            sentence.value11 = Integer.parseInt(tempArray[11]);
        if (tempArray.length > 12)
            sentence.value12 = Integer.parseInt(tempArray[12]);
        if (tempArray.length > 13)
            sentence.value13 = Integer.parseInt(tempArray[13]);
        if (tempArray.length > 14)
            sentence.value14 = Integer.parseInt(tempArray[14]);
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

    public class globalConfig {

        SharedPreferences appConfig =null;
        SharedPreferences.Editor edit = null;
        //application language
        private String applicationLanguage ="0";
        //Graph units
        private String units="0";
        private String BaudRate = "9600";

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

        public globalConfig ()
        {
            appConfig  = getSharedPreferences("ConsoleCfg", MODE_PRIVATE);
            edit = appConfig.edit();
        }

        public void ResetDefaultConfig() {

            applicationLanguage ="0";
            graphBackColor="1";
            graphColor="0";
            fontSize="10";
            units="0";
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

        public String getBaudRate() {
            return BaudRate;
        }
        public void setBaudRate(String value) {

            BaudRate=value;
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
