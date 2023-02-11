package com.altimeter.bdureau.bearconsole.Flight;

/**
 * @description: This will display the altimeter flights
 * This is a tabbed activity that will display several fragments for each tab of the flights
 * - FlightViewMpFragment => display altimeter flight curves
 * - FlightViewInfoFragment => display flight informations
 * - FlightViewMapFragment => will display the Map that has been recorded when using the altiGPS
 * This has the ability to take screen shot of each tab and share them using Whats app, email etc ...
 * @author: boris.dureau@neuf.fr
 **/
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;



public class FlightViewTabActivity extends AppCompatActivity {
    private FlightData myflight = null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    // These are the activity tabs
    private FlightViewMpFragment flightPage1 = null;
    private FlightViewInfoFragment flightPage2 = null;
    private FlightViewGoogleMapFragment flightPage3 = null;
    private FlightViewOpenMapFragment flightPage4 = null;

    private Button btnDismiss,  butSelectCurves;
    private ConsoleApplication myBT;

    private String curvesNames[] = null;
    private String currentCurvesNames[] = null;
    private boolean[] checkedItems = null;
    private XYSeriesCollection allFlightData = null;

    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};
    static Font font;
    private String FlightName = null;

    private String[] units = null;
    public String SELECTED_FLIGHT = "MyFlight";
    public int numberOfCurves = 0;
    //File imagePath;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSION_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
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
        outState.putBooleanArray("CHECKED_ITEMS_KEY", checkedItems);
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
        if (Build.VERSION.SDK_INT >= 23)
            verifyStoragePermission(FlightViewTabActivity.this);

        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the connection pointer
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_flight_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        butSelectCurves = (Button) findViewById(R.id.butSelectCurves);

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            numberOfCurves = 8;
        }
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32")) {
            numberOfCurves = 6;
        }
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiV2") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMulti")) {
            numberOfCurves = 5;
        }


        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = null; //new XYSeriesCollection();
        allFlightData = myflight.GetFlightData(FlightName);

        Log.d("numberOfCurves", "numberOfCurves:" + allFlightData.getSeries().size());
        curvesNames = new String[numberOfCurves];
        units = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            curvesNames[i] = allFlightData.getSeries(i).getKey().toString();
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();
        //metrics
        if (myBT.getAppConf().getUnits().equals("0")) {
            //Meters
            units[0] = "(" + getResources().getString(R.string.Meters_fview) + ")";
            units[3] = "(m/secs)";
            units[4] = "(m/secs²)";
        }
        //imperial
        else {
            //Feet
            units[0] = getResources().getString(R.string.Feet_fview);
            //(feet/secs)
            units[3] = "(" + getResources().getString(R.string.unit_feet_per_secs) + ")";
            //(feet/secs²)
            units[4] = "(" + getResources().getString(R.string.unit_feet_per_square_secs) + ")";
        }
        units[1] = "(°C)";
        units[2] = "(mbar)";

        if(numberOfCurves > 5)
            units[5] = "(volts)";


        if (currentCurvesNames == null) {
            //This is the first time so only display the altitude
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] = this.getResources().getString(R.string.curve_altitude);
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        setupViewPager(mViewPager);


        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allFlightData = null;
                finish();      //exit the application configuration activity
            }
        });

        butSelectCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewTabActivity.this);

                if(curvesNames.length>0)
                    checkedItems = new boolean[curvesNames.length];
                else {
                    checkedItems = new boolean[1];
                    checkedItems[0]= true;
                }

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
                        flightPage1.setCheckedItems(checkedItems);
                        int nbrOfItems =0;
                        for (int i = 0; i < checkedItems.length; i++) {

                            //count nbr of selected
                            if(checkedItems[i]) {
                                nbrOfItems++;
                            }
                        }
                        if(nbrOfItems > 0)
                            currentCurvesNames = new String[nbrOfItems];
                        else {
                            currentCurvesNames = new String[1];
                            checkedItems[0] = true;
                        }

                        int k=0;
                        for(int j=0; j< curvesNames.length; j++){
                            if(checkedItems[j]){
                                currentCurvesNames[k] = curvesNames[j];
                                k++;
                            }
                        }
                        flightPage1.drawGraph();
                        flightPage1.drawAllCurves(allFlightData);
                    }
                });
                //cancel
                builder.setNegativeButton(getResources().getString(R.string.fv_cancel), null);

                // Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    public static void verifyStoragePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSION_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());

        flightPage1 = new FlightViewMpFragment(allFlightData,
                myBT,
                curvesNames,
                checkedItems,
                units);

        flightPage2 = new FlightViewInfoFragment(myflight,
                allFlightData,
                myBT,
                units,
                FlightName);



        adapter.addFragment(flightPage1, "TAB1");
        adapter.addFragment(flightPage2, "TAB2");

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            if (!myBT.getAppConf().getUseOpenMap()) {
                flightPage3 = new FlightViewGoogleMapFragment(myBT,
                        mViewPager,
                        myflight,
                        FlightName);
                adapter.addFragment(flightPage3, "TAB3");
            } else {
                flightPage4 = new FlightViewOpenMapFragment(myBT,
                        mViewPager,
                        myflight,
                        FlightName);
                adapter.addFragment(flightPage4, "TAB4");
            }
        }

        linearDots=findViewById(R.id.idFlightLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr){
        dotsSlide =new TextView[nbr];
        linearDots.removeAllViews();

        for (int i=0; i< dotsSlide.length; i++){
            dotsSlide[i]=new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if(dotsSlide.length>0){
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener=new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

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


    private  void takeScreenShot(View view) {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilShare");
            if (!mainDir.exists()) {
                boolean mkdir = mainDir.mkdir();
            }

            String path = mainDir + "/" + "AltiMultiCurve" + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);


            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            shareScreenShot(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Share ScreenShot
    private  void shareScreenShot(File imageFile ) {
        Log.d("Package Name", "Package Name" + this.getPackageName());
        Uri uri = FileProvider.getUriForFile(
                this,
                this.getPackageName() +  ".provider",
                imageFile);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "BearConsole has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, "Share With"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_flights, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //share screen
        if (id == R.id.action_share) {
            takeScreenShot(findViewById(android.R.id.content).getRootView());
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_flight");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}