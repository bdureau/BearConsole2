package com.altimeter.bdureau.bearconsole.Flight;


/**
 *   @description: This class has all the flight arrays and methods to add or remove flight data
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.content.Context;
import android.util.Log;

import com.altimeter.bdureau.bearconsole.R;

import java.util.*;


import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

public class FlightData {
    //context so that we can use the translations
    private Context context;
    //pass the altimeter name so that we can do specific
    private String altimeterName;
    // Create a hash map
    public static HashMap<String,XYSeriesCollection > hm;// = new HashMap<String,XYSeriesCollection>();
    public FlightData (Context current, String name)
    {
        this.context = current;
        this.altimeterName = name;
        hm = new HashMap<String,XYSeriesCollection>();
    }
    //this might be a usefull function that I will write later
    public int getNbrOfFlight()
    {
        return hm.entrySet().size();
    }

    public String getFlightName(int FlightNumber)
    {
        String flightName = null;
        return flightName;
    }


    public List<String> getAllFlightNames2()
    {
        List<String> flightNames = new ArrayList<String>();
        Log.d("FlightList", "in getAllFlightNames2");
        Set set = hm.entrySet();
        // Get an iterator
        Iterator i = set.iterator();

        // Display elements
        while(i.hasNext()) {
            Log.d("FlightList","in While");
            Map.Entry me = (Map.Entry)i.next();
            flightNames.add((String)me.getKey());
            Log.d("FlightList", (String)me.getKey());
        }
        return flightNames;
    }

    public void ClearFlight()
    {
        hm =null;
        hm = new HashMap();

    }

    public XYSeriesCollection  GetFlightData(String flightName)
    {
        XYSeriesCollection  flightData=null;

        Set set = hm.entrySet();
        // Get an iterator
        Iterator i = set.iterator();


        // Display elements
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();
            if (me.getKey().equals(flightName))
                flightData = (XYSeriesCollection ) me.getValue();
        }
        return flightData;
    }
    public boolean FlightExist(String flightName)
    {
        boolean exist = false;

        Set set = hm.entrySet();
        // Get an iterator
        Iterator i = set.iterator();

        // Display elements
        while(i.hasNext()) {
            Map.Entry me = (Map.Entry)i.next();

            if (me.getKey().equals(flightName))
                exist=true;
        }

        return exist;
    }
    public void AddToFlight (float X, float Y, String flightName)
    {

        //Find out if the flight exist
        //If it exist append the data to the flight and if not create a new flight
        XYSeriesCollection  flightData=null;
        if (!FlightExist(flightName))
        {

            //if the flight name does not exist let's create it first
            hm.put(flightName, createFlight(flightName));
        }

        flightData = GetFlightData(flightName);
        flightData.getSeries(0).add(X, Y);
    }

    public void AddToFlight (float X, float Y, String flightName, int serie)
    {
        //Find out if the flight exist
        //If it exist append the data to the flight and if not create a new flight
        XYSeriesCollection  flightData=null;
        if (!FlightExist(flightName))
        {
            //if the flight name does not exist let's create it first
            hm.put(flightName, createFlight(flightName));
        }

        flightData = GetFlightData(flightName);
        flightData.getSeries(serie).add(X, Y);
    }
    //not sure that I will be using that one
    public void AddFlightData (XYSeriesCollection  flightData, String flightName)
    {
        hm.put(flightName, flightData);
    }

    public void AddData (int flightNbr , int X, int Y )
    {

    }


    private XYSeriesCollection  createFlight(final String name) {
        XYSeriesCollection ret;

        //altitude
        final XYSeries series = new XYSeries(context.getResources().getString(R.string.curve_altitude)) ;
        ret = new XYSeriesCollection (series);
        //temperature
        ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_temperature)));
        //pressure
        ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_pressure)));
        //speed
        ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_speed)));
        //acceleration
        ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_accel)));
        //voltage
        if(altimeterName.equals("AltiGPS") || altimeterName.equals("AltiMultiSTM32") ||
                altimeterName.equals("AltiMultiESP32") || altimeterName.equals("AltiMultiESP32_accel")||
                altimeterName.equals("AltiMultiESP32_accel_345")|| altimeterName.equals("AltiMultiESP32_accel_375")||
                altimeterName.equals("TTGOBearAltimeter") || altimeterName.equals("TTGOMiniBearAltimeter")) {
            ret.addSeries(new XYSeries("voltage"));
        }
        //humidity
        if(altimeterName.equals("UltimateAltimeter")) {
            ret.addSeries(new XYSeries("humidity"));
        }

        // if Alti GPS
        if(altimeterName.equals("AltiGPS")) {
            //latitude
            ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_latitude)));
            //longitude
            ret.addSeries(new XYSeries(context.getResources().getString(R.string.curve_longitude)));
            //GPS altitude
            ret.addSeries(new XYSeries("gps altitude"));
            //GPS sat
            ret.addSeries(new XYSeries("satelites"));
            //GPS speed
            ret.addSeries(new XYSeries("gps speed"));
            //sea altitude
            ret.addSeries(new XYSeries("sea altitude"));
        }

        if(altimeterName.equals("UltimateAltimeter")){
            //accel345x
            ret.addSeries(new XYSeries("accelX"));
            //accel345y
            ret.addSeries(new XYSeries("accelX"));
            //accel345z
            ret.addSeries(new XYSeries("accelX"));
        }
        if(altimeterName.equals("TTGOBearAltimeter")|| altimeterName.equals("TTGOMiniBearAltimeter")){
            //accel345x
            ret.addSeries(new XYSeries("accel345x"));
            //accel345y
            ret.addSeries(new XYSeries("accel345y"));
            //accel345z
            ret.addSeries(new XYSeries("accel345z"));
        }

        // if AltiMultiESP32_accel we also have 6 curves that contains accel values
        if(altimeterName.equals("AltiMultiESP32_accel")||
                altimeterName.equals("AltiMultiESP32_accel_345")|| altimeterName.equals("AltiMultiESP32_accel_375")) {
            //accel375x
            ret.addSeries(new XYSeries("accel375x"));
            //accel375y
            ret.addSeries(new XYSeries("accel375y"));
            //accel375z
            ret.addSeries(new XYSeries("accel375z"));
            //accel345x
            ret.addSeries(new XYSeries("accel345x"));
            //accel345y
            ret.addSeries(new XYSeries("accel345y"));
            //accel345z
            ret.addSeries(new XYSeries("accel345z"));
        }
        return ret; // new XYSeriesCollection (series);
    }

}
