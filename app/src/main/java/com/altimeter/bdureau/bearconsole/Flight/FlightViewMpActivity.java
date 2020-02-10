package com.altimeter.bdureau.bearconsole.Flight;
/**
 *   @description: This will display each flight using a the MPchart graphics library
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.content.Intent;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.Flight.FlightListActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;

public class FlightViewMpActivity extends AppCompatActivity {
    String FlightName = null;
    ConsoleApplication myBT ;
    private LineChart mChart;
    private FlightData myflight=null;
    private Button buttonDismiss,buttonMap;
    public static String SELECTED_FLIGHT = "MyFlight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_view_mp);
        buttonDismiss =  (Button) findViewById(R.id.butDismiss);
        buttonMap = (Button) findViewById(R.id.butMap);
        if (myBT.getAltiConfigData().getAltimeterName().equals( "AltiGPS"))
            buttonMap.setVisibility(View.VISIBLE);
        else
            buttonMap.setVisibility(View.INVISIBLE);
        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        myflight= myBT.getFlightData();
        XYSeriesCollection flightData;
        flightData = myflight.GetFlightData(FlightName);
        // Read the application config
        myBT.getAppConf().ReadConfig();
        int graphBackColor;//= Color.WHITE;
        graphBackColor =myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

        int axisColor;//=Color.BLACK;
        axisColor=myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor= Color.BLACK;

        int nbrColor=Color.BLACK;
        String myUnits="";
        if (myBT.getAppConf().getUnits().equals("0"))
            //Meters
            myUnits=getResources().getString(R.string.Meters_fview);
        else
            //Feet
            myUnits = getResources().getString(R.string.Feet_fview);

        //font
        //Font font = new Font("Dialog", Typeface.NORMAL,fontSize);


        mChart  = (LineChart) findViewById(R.id.linechart);

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        int nbrData = flightData.getSeries(0).getItemCount();
        //flightData.getSeries(0).getX(0).longValue()
        ArrayList<Entry> yValues = new ArrayList <>();

        for (int i = 0; i < nbrData; i++) {
            yValues.add(new Entry(flightData.getSeries(0).getX(i).longValue(), flightData.getSeries(0).getY(i).longValue()));
        }

        LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");

        //set1.setFillAlpha(110);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        set1.setLabel("Altitude");

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        Description desc = new Description();
        desc.setText("Altitude/Time");
        mChart.setDescription(desc);
        buttonDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the activity
            }
        });
        //FlightViewMapsActivity
        buttonMap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent i;
                // Make an intent to start next activity.
                i = new Intent(FlightViewMpActivity.this, FlightViewMapsActivity.class);

                //Change the activity.
                i.putExtra(SELECTED_FLIGHT, FlightName);
                startActivity(i);
            }
        });
    }
}
