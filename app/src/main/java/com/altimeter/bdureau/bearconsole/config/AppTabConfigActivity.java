package com.altimeter.bdureau.bearconsole.config;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AppTabConfigActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Application settings editor (stored on the phone, not in the altimeter). Tabs:
 *    - AppConfig1Fragment: language, units, graph colors and font, connection type and speed,
 *      graph library, multiple drogue / main outputs, full USB support;
 *    - AppConfig2Fragment: voice announcements during telemetry and the voice used;
 *    - AppConfig3Fragment: map provider, map type and color, manual GPS recording.
 *  "Save" writes the values to the shared preferences; "Default" restores the defaults in
 *  the fields after a warning and a confirmation (they are stored only by "Save").
 *  Leaving the screen ("Dismiss" or the back key) with unsaved changes asks what to do:
 *  save, leave without saving, or stay (changes detected by utils.ConfigChangeTracker).
 *  The text to speech engine is started only to list the voices available on the phone.
 * ============================================================================
 */

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.BaseActivity;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.utils.TabPagerAdapter;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.utils.ConfigChangeTracker;
import com.altimeter.bdureau.bearconsole.config.AppConfig.AppConfig1Fragment;
import com.altimeter.bdureau.bearconsole.config.AppConfig.AppConfig2Fragment;
import com.altimeter.bdureau.bearconsole.config.AppConfig.AppConfig3Fragment;

import java.util.Locale;

public class AppTabConfigActivity extends BaseActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private ViewPager mViewPager;
    private TabPagerAdapter adapter;
    private TextToSpeech mTTS;

    private AppConfig1Fragment appConfigPage1 = null;
    private AppConfig2Fragment appConfigPage2 = null;
    private AppConfig3Fragment appConfigPage3 = null;

    private AppConfigData appConfigData = null;
    // Unsaved changes of the fields, checked before leaving the screen
    private final ConfigChangeTracker changeTracker = new ConfigChangeTracker();

    private ConsoleApplication myBT;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Before the tabs are created (or re-created by the system): their fields are tracked
        changeTracker.attach(this);
        setTitle(R.string.titre_application_pref);
        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();

        myBT.getAppConf().ReadConfig();
        // get the data for all the drop down
        appConfigData = new AppConfigData(this);
        setContentView(R.layout.activity_app_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container_config);
        setupViewPager(mViewPager);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //exit the application configuration activity, after a confirmation if needed
                confirmLeave();
            }
        });
        // The back key asks the same confirmation as "Dismiss"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
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
                //restore the application default configuration, after a warning
                confirmRestoreToDefault();
            }
        });
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;

                    // Voice in the language of the application (English if not available)
                    result = AppLanguage.applyToTextToSpeech(mTTS);
                    try {
                        String[] itemsVoices;
                        String items = "";
                        for (Voice tmpVoice : mTTS.getVoices()) {
                            if (tmpVoice.getName().startsWith(Locale.getDefault().getLanguage())) {
                                if (items.equals(""))
                                    items = tmpVoice.getName();
                                else
                                    items = items + "," + tmpVoice.getName();
                                Log.d("Voice", tmpVoice.getName());
                            }
                        }

                        itemsVoices = items.split(",");

                        appConfigPage2.setVoices(itemsVoices);
                        // The voice selected by program is not a change of the user
                        changeTracker.resetReference(appConfigPage2);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "Language not supported");
                        } else {

                        }
                    } catch (Exception e) {

                    }
                } else {
                    Log.e("TTS", "Init failed");
                }
            }
        }, "com.google.android.tts");

    }

    private void SaveConfig() {
        boolean languageChanged = false;
        // Checked before anything is changed: a refused save leaves the configuration intact
        if (appConfigPage1.isViewCreated()
                && appConfigPage1.getGraphColor() == appConfigPage1.getGraphBackColor()) {
            msg(getString(R.string.app_config_same_colors));
            return;
        }
        if(appConfigPage1.isViewCreated()) {
            int newLanguage = appConfigPage1.getAppLanguage();
            if (newLanguage != myBT.getAppConf().getApplicationLanguage()) {
                // New language: applied to the application object at once; every screen is
                // re-created in this language when it comes back (BaseActivity.onResume)
                languageChanged = true;
            }
            myBT.getAppConf().setApplicationLanguage(newLanguage);
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
        if (languageChanged) {
            myBT.applyLanguage(myBT.getAppConf().getApplicationLanguage());
        }
        changeTracker.resetReference();
        invalidateOptionsMenu();
        finish();
    }

    /**
     * Leaves the screen: at once if nothing was changed, otherwise after asking whether the
     * changes must be saved, discarded, or the user stays on the screen.
     */
    private void confirmLeave() {
        if (!changeTracker.isModified()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.config_unsaved_title)
                .setMessage(R.string.app_config_unsaved_msg)
                .setPositiveButton(R.string.config_unsaved_save, (d, w) -> SaveConfig())
                .setNegativeButton(R.string.config_unsaved_discard, (d, w) -> {
                    // "Default" changes the configuration in memory: reload the stored one
                    myBT.getAppConf().ReadConfig();
                    finish();
                })
                .setNeutralButton(R.string.config_unsaved_stay, (d, w) -> d.dismiss())
                .show();
    }

    /** Warns that every setting is replaced by its default value, then restores them. */
    private void confirmRestoreToDefault() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.config_default_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.app_config_default_msg)
                .setPositiveButton(R.string.config_default_confirm, (d, w) -> {
                    RestoreToDefault();
                    // Not stored yet: leaving the screen now asks for a confirmation
                    changeTracker.markModified();
                })
                .setNegativeButton(R.string.No, (d, w) -> d.dismiss())
                .show();
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
        adapter = new TabPagerAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_SET_USER_VISIBLE_HINT, this::onTabRestored);

        appConfigPage1 = new AppConfig1Fragment(myBT, appConfigData);
        appConfigPage2 = new AppConfig2Fragment(myBT);
        appConfigPage3 = new AppConfig3Fragment(myBT, appConfigData);

        adapter.addFragment(appConfigPage1);
        adapter.addFragment(appConfigPage2);
        adapter.addFragment(appConfigPage3);

        linearDots = findViewById(R.id.idAppConfigLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        // Every tab keeps its view: its fields are read by "Save" and by the unsaved changes check
        viewPager.setOffscreenPageLimit(adapter.getCount() - 1);
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

    /**
     * Called by the adapter when the ViewPager displays a fragment re-created by the system
     * (activity re-creation): the references used to read the settings are re-bound to it.
     */
    private void onTabRestored(int position, Fragment fragment) {
        if (fragment instanceof AppConfig1Fragment) appConfigPage1 = (AppConfig1Fragment) fragment;
        else if (fragment instanceof AppConfig2Fragment) appConfigPage2 = (AppConfig2Fragment) fragment;
        else if (fragment instanceof AppConfig3Fragment) appConfigPage3 = (AppConfig3Fragment) fragment;
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
