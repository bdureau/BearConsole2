package com.altimeter.bdureau.bearconsole.Flight;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AltimeterTabConfigActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlightViewTabActivity extends AppCompatActivity {
    private static FlightData myflight=null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    Tab1Fragment flightPage1 = null;
    Tab2Fragment flightPage2 = null;
    private Button btnDismiss,buttonMap, butSelectCurves;
    private static ConsoleApplication myBT;
    private static AltiConfigData AltiCfg = null;
    //private ProgressDialog progress;
    private static  String curvesNames[] = null;
    private static String currentCurvesNames[] =null;
    static boolean[] checkedItems = null;
    static XYSeriesCollection allFlightData;
    public static XYSeriesCollection flightData;
    public static ArrayList<ILineDataSet> dataSets;
    static int colors []= {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW,Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};
    static Font font;
    private static String FlightName = null;

    public static String SELECTED_FLIGHT = "MyFlight";

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("CURRENT_CURVES_NAMES_KEY", currentCurvesNames);
        outState.putBooleanArray("CHECKED_ITEMS_KEY",checkedItems);

    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
        checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);


        btnDismiss = (Button) findViewById(R.id.butDismiss);
        buttonMap = (Button) findViewById(R.id.butMap);
        butSelectCurves = (Button) findViewById(R.id.butSelectCurves);

        if (myBT.getAltiConfigData().getAltimeterName().equals( "AltiGPS"))
            buttonMap.setVisibility(View.VISIBLE);
        else
            buttonMap.setVisibility(View.INVISIBLE);

        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = myflight.GetFlightData(FlightName);

        XYSeries speed;
        speed =getSpeedSerie(allFlightData.getSeries("altitude"));
        allFlightData.addSeries(speed);

        XYSeries accel;
        accel = getAccelSerie(speed);
        allFlightData.addSeries(accel);
        // by default we will display the altitude
        // but then the user will be able to change the data
        flightData = new XYSeriesCollection();
        flightData.addSeries(allFlightData.getSeries("altitude"));

        // get a list of all the curves that have been recorded
        //List allCurves = allFlightData.getSeries();
        int numberOfCurves = allFlightData.getSeries().size();
        curvesNames = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
               curvesNames[i] = allFlightData.getSeries(i).getKey().toString();
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();

        if (currentCurvesNames == null) {
            //This is the first time so only display the altitude
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] ="altitude";
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        setupViewPager(mViewPager);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        butSelectCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int numberOfCurves = flightData.getSeries().size();
                //currentCurvesNames = new String[numberOfCurves];

                for (int i = 0; i < numberOfCurves; i++) {
                        curvesNames[i] = allFlightData.getSeries(i).getKey().toString();
                }
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewTabActivity.this);
                //builder.setTitle(getResources().getString(R.string.flight_data_title));
                //checkedItems = new boolean[curvesNames.length];
                // Add a checkbox list
                for (int i = 0; i < curvesNames.length; i++) {
                    if (Arrays.asList(currentCurvesNames).contains(curvesNames[i]))
                        checkedItems[i] = true;
                    else
                        checkedItems[i] = false;
                }


                builder.setMultiChoiceItems(curvesNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // The user checked or unchecked a box
                    }
                });
                // Add OK and Cancel buttons
                builder.setPositiveButton(getResources().getString(R.string.fv_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The user clicked OK
                        flightPage1.drawGraph();
                        flightPage1.drawAllCurves();
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.fv_cancel), null);

                // Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
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
                i = new Intent(FlightViewTabActivity.this, FlightViewMapsActivity.class);

                //Change the activity.
                i.putExtra(SELECTED_FLIGHT, FlightName);
                startActivity(i);
            }
        });
    }
    /*
          Calculate the speed curve
    */
    public XYSeries getSpeedSerie (XYSeries serie) {
        XYSeries speed;
        speed = new XYSeries("speed");
        int nbrData = serie.getItemCount();
        for (int i = 1; i < nbrData; i++) {
            double X,Y;
            X = serie.getX(i).doubleValue();
            if(i==0)
                Y= (serie.getY(i).doubleValue())/(serie.getX(i).doubleValue()/1000);
            else
                Y= (serie.getY(i).doubleValue() - serie.getY(i-1).doubleValue())/((serie.getX(i).doubleValue() - serie.getX(i-1).doubleValue())/1000);

            speed.add(X, Y);
        }

        return speed;
    }
    public XYSeries getAccelSerie (XYSeries serie) {
        XYSeries accel;
        accel = new XYSeries("accel");
        int nbrData = serie.getItemCount();
        for (int i = 1; i < nbrData; i++) {
            double X,Y;
            X = serie.getX(i).doubleValue();
            double V = (serie.getY(i).doubleValue() - serie.getY(i-1).doubleValue());
            double T = ((serie.getX(i).doubleValue() - serie.getX(i-1).doubleValue()))/1000;
            Y=  V/T/(9.806);

            accel.add(X, Y);
        }
        return accel;
    }
    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        flightPage1 = new Tab1Fragment();
        flightPage2 = new Tab2Fragment();

        adapter.addFragment(flightPage1, "TAB1");
        adapter.addFragment(flightPage2, "TAB2");

        viewPager.setAdapter(adapter);
    }

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1Fragment extends Fragment {
        private LineChart mChart;
        private FlightData myflight=null;
        int graphBackColor, fontSize, axisColor, labelColor, nbrColor;
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabflight_view_mp_fragment, container, false);

            // Read the application config
            myBT.getAppConf().ReadConfig();
            XYSeriesCollection flightData;
            myflight= myBT.getFlightData();
            flightData = myflight.GetFlightData(FlightName);

            drawGraph();

            mChart  = (LineChart) view.findViewById(R.id.linechart);
            mChart.setBackgroundColor(graphBackColor);

            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);

            int nbrData = flightData.getSeries(0).getItemCount();

            ArrayList<Entry> yValues = new ArrayList <>();

            for (int i = 0; i < nbrData; i++) {
                yValues.add(new Entry(flightData.getSeries(0).getX(i).longValue(), flightData.getSeries(0).getY(i).longValue()));
            }

            LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");

            set1.setDrawValues(false);
            set1.setDrawCircles(false);
            set1.setLabel("Altitude");
            set1.setValueTextColor(labelColor);

            //set1.setColor(labelColor);
            set1.setValueTextSize(fontSize); //test


            //ArrayList<ILineDataSet>
                    dataSets = new ArrayList<>();
            dataSets.add(set1);

            LineData data = new LineData(dataSets);
            data.setValueTextSize(fontSize);//test

            mChart.setData(data);

            Description desc = new Description();
            desc.setText("Altitude/Time");
            mChart.setDescription(desc);

            return view;
        }
        private void drawGraph() {

            graphBackColor =myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));


            fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

            axisColor=myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

            labelColor= Color.BLACK;

            nbrColor=Color.BLACK;
            String myUnits="";
            if (myBT.getAppConf().getUnits().equals("0"))
                //Meters
                myUnits=getResources().getString(R.string.Meters_fview);
            else
                //Feet
                myUnits = getResources().getString(R.string.Feet_fview);
        }
        private void drawAllCurves() {
            dataSets.clear();

            flightData = new XYSeriesCollection();
            for (int i = 0; i < curvesNames.length; i++) {
                if (checkedItems[i]) {
                    flightData.addSeries(allFlightData.getSeries(curvesNames[i]));

                    int nbrData = allFlightData.getSeries(i).getItemCount();

                    ArrayList<Entry> yValues = new ArrayList<>();

                    for (int k = 0; k < nbrData; k++) {
                        yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue(), allFlightData.getSeries(i).getY(k).floatValue()));
                    }

                    LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");
                    set1.setColor(colors[i]);

                    set1.setDrawValues(false);
                    set1.setDrawCircles(false);
                    set1.setLabel(curvesNames[i]);

                    dataSets.add(set1);

                }
            }

            LineData data = new LineData(dataSets);
            mChart.clear();
            mChart.setData(data);
        }


    }

    public static class Tab2Fragment extends Fragment {

        private TextView nbrOfSamplesValue, flightNbrValue;
        private TextView apogeeAltitudeValue, flightDurationValue, burnTimeValue, maxVelociyValue, maxAccelerationValue;
        private TextView timeToApogeeValue, mainAltitudeValue, maxDescentValue, landingSpeedValue;
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabflight_info_fragment, container, false);

            apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
            flightDurationValue = view.findViewById(R.id.flightDurationValue);
            burnTimeValue = view.findViewById(R.id.burnTimeValue);
            maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
            maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);
            timeToApogeeValue = view.findViewById(R.id.timeToApogeeValue);
            mainAltitudeValue = view.findViewById(R.id.mainAltitudeValue);
            maxDescentValue = view.findViewById(R.id.maxDescentValue);
            landingSpeedValue = view.findViewById(R.id.landingSpeedValue);
            nbrOfSamplesValue= view.findViewById(R.id.nbrOfSamplesValue);
            flightNbrValue = view.findViewById(R.id.flightNbrValue);

            XYSeriesCollection flightData;
            myflight= myBT.getFlightData();
            flightData = myflight.GetFlightData(FlightName);
            int nbrData = flightData.getSeries(0).getItemCount();

            // flight nbr
            flightNbrValue.setText(FlightName + "");

            //nbr of samples
            nbrOfSamplesValue.setText(nbrData +"");

            //flight duration
            double flightDuration = flightData.getSeries(0).getMaxX()/1000;
            flightDurationValue.setText(flightDuration +" secs");
            //apogee altitude
            double apogeeAltitude = flightData.getSeries(0).getMaxY();
            apogeeAltitudeValue.setText(apogeeAltitude +" m");
            //apogee time
            int pos = searchX (flightData.getSeries(0),apogeeAltitude);
            double apogeeTime = (double) flightData.getSeries(0).getX(pos);
            timeToApogeeValue.setText(apogeeTime/1000 + " secs");

            //calculate the speed
            XYSeries speed;
            speed =getSpeedSerie(flightData.getSeries(0));
            double maxSpeed =speed.getMaxY();
            maxVelociyValue.setText( (long) maxSpeed +" m/secs");

            //landing speed
            double landingSpeed = searchY(speed, flightData.getSeries(0).getMaxX()-3000) ;
            landingSpeedValue.setText(landingSpeed + " m/secs");
            double maxDescentSpeed = searchY(speed, apogeeTime +2000);
            maxDescentValue.setText(maxDescentSpeed+ " m/secs" );

            //max acceleration value
            XYSeries accel;
            accel = getAccelSerie(speed);
            double maxAccel =accel.getMaxY();
            maxAccelerationValue.setText(String.format("%.2f",maxAccel) + " G");

            //burntime value

            //main value



            return view;
        }

        /*
        Return the position of the first X value it finds from the beginning
         */
        public int searchX (XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 0; i < nbrData; i++) {
                if(serie.getY(i).doubleValue() == searchVal) {
                    pos =i;
                    break;
                }
            }
            return pos;
        }

        /*
        Return the position of the first Y value it finds from the beginning
         */
        public int searchY (XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 0; i < nbrData; i++) {
                if(serie.getX(i).doubleValue() == searchVal) {
                    pos =i;
                    break;
                }
            }
            return pos;
        }
        /*
        Calculate the speed curve
         */
        public XYSeries getSpeedSerie (XYSeries serie) {
            XYSeries speed;
            speed = new XYSeries("Speed");
            int nbrData = serie.getItemCount();
            for (int i = 1; i < nbrData; i++) {
                double X,Y;
                X = serie.getX(i).doubleValue();
                if(i==0)
                    Y= (serie.getY(i).doubleValue())/(serie.getX(i).doubleValue()/1000);
                else
                    Y= (serie.getY(i).doubleValue() - serie.getY(i-1).doubleValue())/((serie.getX(i).doubleValue() - serie.getX(i-1).doubleValue())/1000);

                speed.add(X, Y);
            }

            return speed;
        }

        public XYSeries getAccelSerie (XYSeries serie) {
            XYSeries accel;
            accel = new XYSeries("accel");
            int nbrData = serie.getItemCount();
            for (int i = 1; i < nbrData; i++) {
                double X,Y;
                X = serie.getX(i).doubleValue();
                double V = (serie.getY(i).doubleValue() - serie.getY(i-1).doubleValue());
                double T = ((serie.getX(i).doubleValue() - serie.getX(i-1).doubleValue()))/1000;
                Y=  V/T/(9.806);

                accel.add(X, Y);
            }
            return accel;
        }
    }
}