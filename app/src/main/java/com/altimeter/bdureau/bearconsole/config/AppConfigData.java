package com.altimeter.bdureau.bearconsole.config;

/**
 *   @description: This class define all the values for the application conf drop down
 *   @author: boris.dureau@neuf.fr
 **/
public class AppConfigData {
    private String[] itemsLanguages = new String[]{ "English", "French", "Phone language"};
    private String[] itemsColor = new String[]{"BLACK",
            "WHITE",
            "MAGENTA",
            "BLUE",
            "YELLOW",
            "GREEN",
            "GRAY",
            "CYAN",
            "DKGRAY",
            "LTGRAY",
            "RED"};
    private String[] itemsUnits = new String[]{"Meters", "Feet"};
    private String[] itemsFontSize = new String[]{"8","9", "10", "11", "12","13",
            "14", "15", "16", "17", "18", "19", "20"};

    private String[] itemsBaudRate = new String[]{ "300",
            "1200",
            "2400",
            "4800",
            "9600",
            "14400",
            "19200",
            "28800",
            "38400",
            "57600",
            "115200",
            "230400"};
    private String[] itemsConnectionType = new String[]{ "bluetooth",
            "usb"};
    private String[] itemsGraphicsLib = new String[] {"AFreeChart",
            "MPAndroidChart"};

    public AppConfigData()
    {

    }
    public String [] getItemsLanguages() {
        return itemsLanguages;
    }
    public String getLanguageByNbr(int langNbr) {
        return itemsLanguages [langNbr];
    }

    public String [] getItemsColor() {
        return itemsColor;
    }

    public String  getColorByNbr(int colorNbr) {
        return itemsColor[colorNbr];
    }

    public String[] getItemsUnits() {
        return itemsUnits;
    }
    public String getUnitsByNbr(int unitNbr) {
        return itemsUnits[unitNbr];
    }

    public String[] getItemsFontSize() {
        return itemsFontSize;
    }
    public String getFontSizeByNbr(int fontSize) {
        return itemsFontSize[fontSize];
    }
    public String[] getItemsBaudRate () {
        return itemsBaudRate;
    }

    public String getBaudRateByNbr(int baudRateNbr) {
        return itemsBaudRate[baudRateNbr];
    }

    public String[] getItemsConnectionType () {
        return itemsConnectionType;
    }

    public String getConnectionTypeByNbr (int connectionTypeNbr) {
        return itemsConnectionType[connectionTypeNbr];
    }
    public String[] getItemsGraphicsLib() {return itemsGraphicsLib;}
    public String getGraphicsLibTypeByNbr(int graphicsLibNbr) {return itemsGraphicsLib[graphicsLibNbr];}
}
