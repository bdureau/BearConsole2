package com.altimeter.bdureau.bearconsole;

/**
 * Created by BDUREAU on 28/06/2016.
 */
public class AltiConfigData {

    //Altimeter config variable
    private int units = 0;
    private int beepingMode = 0;
    private int output1 =0;
    private int output2 =1;
    private int output3 =2;
    private int supersonicYesNo =0;
    private int mainAltitude = 100;
    private String AltimeterName = "AltiToto";
    private int output1Delay =0;
    private int output2Delay =0;
    private int output3Delay =0;
    private int minorVersion =0;
    private int majorVersion =0;
    private int beepingFrequency = 440;


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
}
