package com.altimeter.bdureau.bearconsole.config;
/**
 * @description: In this activity you should be able to choose the application languages and looks and feel.
 * Still a lot to do but it is a good start
 * @author: boris.dureau@neuf.fr
 **/

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.AppConfig.AppConfig1Fragment;
import com.altimeter.bdureau.bearconsole.config.AppConfig.AppConfig2Fragment;
import com.altimeter.bdureau.bearconsole.config.AppConfig.AppConfig3Fragment;

import java.util.ArrayList;
import java.util.List;

public class AppTabConfigActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private ViewPager mViewPager;
    private SectionsPageAdapter adapter;

    private AppConfig1Fragment appConfigPage1 = null;
    private AppConfig2Fragment appConfigPage2 = null;
    private AppConfig3Fragment appConfigPage3 = null;

    private AppConfigData appConfigData = null;

    private ConsoleApplication myBT;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();

        myBT.getAppConf().ReadConfig();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        // get the data for all the drop down
        appConfigData = new AppConfigData(this);
        setContentView(R.layout.activity_app_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container_config);
        setupViewPager(mViewPager);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        btnSave = (Button) findViewById(R.id.butSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save the application configuration
                SaveConfig();
            }
        });

        bdtDefault = (Button) findViewById(R.id.butDefault);
        bdtDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //restore the application default configuration
                RestoreToDefault();
            }
        });
    }

    private void SaveConfig() {
        if(appConfigPage1.isViewCreated()) {
            myBT.getAppConf().setApplicationLanguage(appConfigPage1.getAppLanguage());
            myBT.getAppConf().setGraphColor(appConfigPage1.getGraphColor());
            myBT.getAppConf().setUnits(appConfigPage1.getAppUnit());
            myBT.getAppConf().setGraphBackColor(appConfigPage1.getGraphBackColor());
            myBT.getAppConf().setFontSize(appConfigPage1.getFontSize());
            myBT.getAppConf().setBaudRate(appConfigPage1.getBaudRate());
            myBT.getAppConf().setConnectionType(appConfigPage1.getConnectionType());
            myBT.getAppConf().setGraphicsLibType(appConfigPage1.getGraphicsLibType());
            myBT.getAppConf().setAllowMultipleDrogueMain(appConfigPage1.getAllowMainDrogue());
            myBT.getAppConf().setFullUSBSupport(appConfigPage1.getFullUSBSupport());
        }
        //page2
        if(appConfigPage2.isViewCreated()) {
            myBT.getAppConf().setAltitude_event(appConfigPage2.getAltitudeEvent());
            myBT.getAppConf().setApogee_altitude(appConfigPage2.getApogeeAltitude());
            myBT.getAppConf().setBurnout_event(appConfigPage2.getBurnoutEvent());
            myBT.getAppConf().setMain_altitude(appConfigPage2.getMainAltitude());
            myBT.getAppConf().setDrogue_event(appConfigPage2.getDrogueEvent());
            myBT.getAppConf().setLanding_event(appConfigPage2.getLandingEvent());
            myBT.getAppConf().setWarning_event(appConfigPage2.getWarningEvent());
            myBT.getAppConf().setMain_event(appConfigPage2.getMainEvent());
            myBT.getAppConf().setLiftOff_event(appConfigPage2.getLiftOffEvent());
            myBT.getAppConf().setTelemetryVoice(appConfigPage2.getTelemetryVoice());
        }
        //page3
        if(appConfigPage3.isViewCreated()) {
            myBT.getAppConf().setMapColor(appConfigPage3.getMapColor());
            myBT.getAppConf().setMapType(appConfigPage3.getMapType());
            myBT.getAppConf().setManualRecording(appConfigPage3.getAllowManualRecording());
            myBT.getAppConf().setUseOpenMap(appConfigPage3.getUseOpenMap());
        }
        myBT.getAppConf().SaveConfig();
        invalidateOptionsMenu();
        finish();
    }

    private void RestoreToDefault() {
        myBT.getAppConf().ResetDefaultConfig();
        if(appConfigPage1.isViewCreated()) {
            appConfigPage1.setAppLanguage(myBT.getAppConf().getApplicationLanguage());
            appConfigPage1.setAppUnit(myBT.getAppConf().getUnits());
            appConfigPage1.setGraphColor(myBT.getAppConf().getGraphColor());
            appConfigPage1.setGraphBackColor(myBT.getAppConf().getGraphBackColor());
            appConfigPage1.setFontSize(myBT.getAppConf().getFontSize() );
            appConfigPage1.setBaudRate(myBT.getAppConf().getBaudRate());
            appConfigPage1.setConnectionType(myBT.getAppConf().getConnectionType());
            appConfigPage1.setGraphicsLibType(myBT.getAppConf().getGraphicsLibType());
            appConfigPage1.setAllowMainDrogue(myBT.getAppConf().getAllowMultipleDrogueMain());
            appConfigPage1.setFullUSBSupport(myBT.getAppConf().getFullUSBSupport());
        }
        if(appConfigPage2.isViewCreated()) {
            //config page 2
            appConfigPage2.setAltitudeEvent(myBT.getAppConf().getAltitude_event());
            appConfigPage2.setApogeeAltitude(myBT.getAppConf().getApogee_altitude());
            appConfigPage2.setBurnoutEvent(myBT.getAppConf().getBurnout_event());
            appConfigPage2.setMainAltitude(myBT.getAppConf().getMain_altitude());
            appConfigPage2.setDrogueEvent(myBT.getAppConf().getDrogue_event());
            appConfigPage2.setLandingEvent(myBT.getAppConf().getDrogue_event());
            appConfigPage2.setMainEvent(myBT.getAppConf().getMain_event());
            appConfigPage2.setWarningEvent(myBT.getAppConf().getWarning_event());
            appConfigPage2.setLiftOffEvent(myBT.getAppConf().getLiftOff_event());
            appConfigPage2.setTelemetryVoice(myBT.getAppConf().getTelemetryVoice());
        }
        //config page 3
        if(appConfigPage3.isViewCreated()) {
            appConfigPage3.setMapColor(myBT.getAppConf().getMapColor());
            appConfigPage3.setMapType(myBT.getAppConf().getMapType());
            appConfigPage3.setAllowManualRecording(myBT.getAppConf().getManualRecording());
            appConfigPage3.setUseOpenMap(myBT.getAppConf().getUseOpenMap());
        }
    }


    private void setupViewPager(ViewPager viewPager) {
        adapter = new AppTabConfigActivity.SectionsPageAdapter(getSupportFragmentManager());

        appConfigPage1 = new AppConfig1Fragment(myBT, appConfigData);
        appConfigPage2 = new AppConfig2Fragment(myBT);
        appConfigPage3 = new AppConfig3Fragment(myBT, appConfigData);

        adapter.addFragment(appConfigPage1, "TAB1");
        adapter.addFragment(appConfigPage2, "TAB2");
        adapter.addFragment(appConfigPage3, "TAB3");

        linearDots = findViewById(R.id.idAppConfigLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr) {
        dotsSlide = new TextView[nbr];
        linearDots.removeAllViews();

        for (int i = 0; i < dotsSlide.length; i++) {
            dotsSlide[i] = new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if (dotsSlide.length > 0) {
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }

    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
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
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(AppTabConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_config_application");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(AppTabConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
