package com.altimeter.bdureau.bearconsole.Flight;

import android.app.ProgressDialog;
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

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;

public class FlightViewTabActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    Tab1Fragment flightPage1 = null;
    Tab2Fragment flightPage2 = null;
    private Button btnDismiss,buttonMap;
    private static ConsoleApplication myBT;
    private static AltiConfigData AltiCfg = null;
    private ProgressDialog progress;
    //private static Intent newint;
    private static String FlightName = null;


    //private Button buttonDismiss,buttonMap;
    public static String SELECTED_FLIGHT = "MyFlight";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        buttonMap = (Button) findViewById(R.id.butMap);

        if (myBT.getAltiConfigData().getAltimeterName().equals( "AltiGPS"))
            buttonMap.setVisibility(View.VISIBLE);
        else
            buttonMap.setVisibility(View.INVISIBLE);

        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
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
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            //View view = inflater.inflate(R.layout.tabflight_info_fragment, container, false);
            View view = inflater.inflate(R.layout.tabflight_view_mp_fragment, container, false);

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

            mChart  = (LineChart) view.findViewById(R.id.linechart);
            mChart.setBackgroundColor(graphBackColor);

            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            XYSeriesCollection flightData;
            myflight= myBT.getFlightData();
            flightData = myflight.GetFlightData(FlightName);
            int nbrData = flightData.getSeries(0).getItemCount();

            ArrayList<Entry> yValues = new ArrayList <>();

            for (int i = 0; i < nbrData; i++) {
                yValues.add(new Entry(flightData.getSeries(0).getX(i).longValue(), flightData.getSeries(0).getY(i).longValue()));
            }

            LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");

            set1.setDrawValues(false);
            set1.setDrawCircles(false);
            set1.setLabel("Altitude");
            set1.setValueTextSize(fontSize); //test


            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            LineData data = new LineData(dataSets);
            data.setValueTextSize(fontSize);//test

            mChart.setData(data);

            Description desc = new Description();
            desc.setText("Altitude/Time");
            mChart.setDescription(desc);

            return view;
        }
    }

    public static class Tab2Fragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabflight_info_fragment, container, false);

            return view;
        }
    }
}