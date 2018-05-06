package com.altimeter.bdureau.bearconsole;

/**
 *
 *   @description: Altimeter configuration
 *   @author: boris.dureau@neuf.fr
 *
 **/
public class AltiConfigData {

    //Altimeter config variable
    private int units = 0;
    private int beepingMode = 0;
    private int output1 =0;
    private int output2 =1;
    private int output3 =2;
    private int output4 =-1;
    private int supersonicYesNo =0;
    private int mainAltitude = 100;
    private String AltimeterName = "AltiToto";
    private int output1Delay =0;
    private int output2Delay =0;
    private int output3Delay =0;
    private int output4Delay =-1;
    private int minorVersion =0;
    private int majorVersion =0;
    private int beepingFrequency = 440;
    private int nbrOfMeasuresForApogee=5;
    private long connectionSpeed= 57600;
    private int altimeterResolution = 0;
    private int eepromSize= 512;
    private int endRecordAltitude =3;
    private int recordTemperature = 0;
    private int supersonicDelay=0;
    private int beepOnOff = 0;


    public AltiConfigData()
    {

    }

    public void setUnits(int value)
    {
        units = value;
    }

    public int getUnits()
    {
        return units;
    }

    public void setBeepingMode(int value)
    {
        beepingMode = value;
    }

    public int getBeepingMode()
    {
        return beepingMode;
    }

    public int getAltiMinorVersion()
    {
        return minorVersion;
    }
    public int getAltiMajorVersion()
    {
        return majorVersion;
    }
    public void setAltiMinorVersion(int value)
    {
        minorVersion=value;
    }
    public void setAltiMajorVersion(int value)
    {
        majorVersion=value;
    }

    public void setMainAltitude(int value)
    {
        mainAltitude = value;
    }

    public int getMainAltitude()
    {
        return mainAltitude;
    }

    public void setOutput1(int value)
    {
        output1 = value;
    }

    public int getOutput1()
    {
        return output1;
    }

    public void setOutput2(int value)
    {
        output2 = value;
    }

    public int getOutput2()
    {
        return output2;
    }

    public void setOutput3(int value)
    {
        output3 = value;
    }

    public int getOutput3()
    {
        return output3;
    }

    public void setOutput4(int value)
    {
        output4 = value;
    }

    public int getOutput4()
    {
        return output4;
    }

    public void setSupersonicYesNo(int value)
    {
        supersonicYesNo = value;
    }

    public int getSupersonicYesNo()
    {
        return supersonicYesNo;
    }

    public void setOutput1Delay(int value)
    {
        output1Delay  = value;
    }
    public int getOutput1Delay()
    {
        return output1Delay;
    }

    public void setOutput2Delay(int value)
    {
        output2Delay = value;
    }
    public int getOutput2Delay()
    {
        return output2Delay;
    }

    public void setOutput3Delay(int value)
    {
        output3Delay = value;
    }
    public int getOutput3Delay()
    {
        return output3Delay;
    }

    public void setOutput4Delay(int value)
    {
        output4Delay = value;
    }
    public int getOutput4Delay()
    {
        return output4Delay;
    }

    public void setAltimeterName(String value)
    {
        AltimeterName = value;
    }

    public String getAltimeterName()
    {
        return AltimeterName;
    }

    //beepingFrequency
    public void setBeepingFrequency(int value)
    {
        beepingFrequency = value;
    }
    public int getBeepingFrequency()
    {
        return beepingFrequency;
    }

    //Minimum recording Altitude
    public void setEndRecordAltitude(int value) {endRecordAltitude =value;}
    public int getEndRecordAltitude(){return endRecordAltitude;}

    // Record temperature
    public void setRecordTemperature(int value){recordTemperature=value;}
    public int getRecordTemperature(){return recordTemperature;}

   // supersonic delay
    public void setSupersonicDelay(int value) {supersonicDelay=value;}
    public int getSupersonicDelay() {return supersonicDelay;}

    //nbr of measures to do to determine apogee
    public void setNbrOfMeasuresForApogee (int value) {nbrOfMeasuresForApogee = value;}
    public int getNbrOfMeasuresForApogee() {return nbrOfMeasuresForApogee;}

    //altimeter baud rate
    public void setConnectionSpeed(long value) {connectionSpeed = value;}
    public long getConnectionSpeed() {return connectionSpeed;}

    //Sensor resolution
    public void setAltimeterResolution(int value) {altimeterResolution = value;}
    public int getAltimeterResolution() {return altimeterResolution;}

    //eeprom size
    public void setEepromSize(int value) {eepromSize = value;}
    public int getEepromSize() {return eepromSize;}

    // beep on /off
    public void setBeepOnOff(int value) {beepOnOff = value;}
    public int getBeepOnOff() {
        return beepOnOff;
    }

    //index in an array
    public int arrayIndex (String stringArray[], String pattern) {

        for (int i =0; i < stringArray.length ; i++) {
            if(stringArray[i].equals(pattern))
                return i;
        }
        return -1;
    }
}
