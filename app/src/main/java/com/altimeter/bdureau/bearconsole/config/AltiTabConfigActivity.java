package com.altimeter.bdureau.bearconsole.config;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltiTabConfigActivity
 *
 * @description:
 *  Retrieves altimeter hardware EEPROM configuration asynchronously and displays
 *  it across modular category tabs. Sends modified parameters back to the
 *  altimeter in the background without locking the Android UI thread.
 *
 * @author: boris.dureau@neuf.fr
 * ============================================================================
 */

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltiConfig1Fragment;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltiConfig2Fragment;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltiConfig3Fragment;
import com.altimeter.bdureau.bearconsole.config.AltimeterConfig.AltiConfig4Fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AltiTabConfigActivity extends AppCompatActivity {
    private static final String TAG = "AltiTabConfigActivity";

    private ViewPager mViewPager;
    private SectionsPageAdapter adapter;

    private AltiConfig1Fragment configPage1 = null;
    private AltiConfig2Fragment configPage2 = null;
    private AltiConfig3Fragment configPage3 = null;
    private AltiConfig4Fragment configPage4 = null;

    private ConsoleApplication myBT;
    private AltiConfigData altiCfg = null;
    private ProgressDialog progressDialog;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Read language configuration directly from SharedPreferences to ensure
        // context localization is configured before Activity inflation
        SharedPreferences prefs = newBase.getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(ConsoleApplication.updateBaseContextLocale(newBase, lang));
    }
    // Background executor for serial communication
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.altimeter_config);
        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_altimeter_tab_config);

        mViewPager = findViewById(R.id.container);
        Button btnDismiss = findViewById(R.id.butDismiss);
        Button btnUpload = findViewById(R.id.butUpload);

        btnDismiss.setOnClickListener(v -> finish());
        btnUpload.setOnClickListener(v -> validateAndSendConfig());

        // Asynchronous config read on launch
        retrieveConfigAsync();
    }

    public AltiConfigData getAltiConfigData() {
        if (altiCfg == null && myBT != null) {
            altiCfg = myBT.getAltiConfigData();
        }
        return altiCfg;
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!isFinishing() && !isDestroyed()) {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing() && !isFinishing() && !isDestroyed()) {
            progressDialog.dismiss();
        }
    }

    private void retrieveConfigAsync() {
        showProgress(getString(R.string.msg7));

        ioExecutor.execute(() -> {
            boolean success = false;
            if (myBT.getConnected()) {
                success = performReadConfig();
            }

            final boolean isSuccess = success;
            mainHandler.post(() -> {
                hideProgress();
                if (isFinishing() || isDestroyed()) return;

                if (isSuccess && altiCfg != null) {
                    setupViewPager(mViewPager);
                } else {
                    msg("Failed to retrieve altimeter configuration");
                    finish();
                }
            });
        });
    }

    private boolean performReadConfig() {
        boolean success = false;
        try {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("m0;");

            waitForStreamData(3000);
            String response = myBT.ReadResult(3000);

            if ("OK".equals(response)) {
                for (int attempt = 1; attempt <= 2 && !success; attempt++) {
                    myBT.setDataReady(false);
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("b;");
                    myBT.flush();

                    waitForStreamData(3000);
                    String configMsg = myBT.ReadResult(3000);

                    if ("start alticonfig end".equals(configMsg)) {
                        altiCfg = myBT.getAltiConfigData();
                        success = (altiCfg != null);
                    }
                }

                // Resume main loop on device
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                myBT.write("m1;");
                waitForStreamData(3000);
                myBT.ReadResult(3000);
                myBT.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during config retrieval", e);
            success = false;
        }
        return success;
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());

        configPage1 = new AltiConfig1Fragment();
        configPage2 = new AltiConfig2Fragment();
        configPage3 = new AltiConfig3Fragment();

        String altiName = myBT.getAltiConfigData().getAltimeterName();

        if (!"TTGOBearAltimeter".equals(altiName)) {
            adapter.addFragment(configPage1, "TAB1");
            adapter.addFragment(configPage2, "TAB2");
        }
        adapter.addFragment(configPage3, "TAB3");

        if ("AltiServo".equals(altiName)) {
            configPage4 = new AltiConfig4Fragment();
            adapter.addFragment(configPage4, "TAB4");
        }

        linearDots = findViewById(R.id.idAltiConfigLinearDots);
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

    private final ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {}

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    };

    private void validateAndSendConfig() {
        if (altiCfg == null) return;

        // Pull values from active fragments
        if (configPage1 != null && configPage1.isViewCreated()) {
            altiCfg.setOutput1Delay(configPage1.getOutDelay1());
            altiCfg.setOutput2Delay(configPage1.getOutDelay2());
            altiCfg.setOutput3Delay(configPage1.getOutDelay3());
            altiCfg.setOutput4Delay(configPage1.getOutDelay4());
            altiCfg.setOutput1(configPage1.getDropdownOut1());
            altiCfg.setOutput2(configPage1.getDropdownOut2());
            altiCfg.setOutput3(configPage1.getDropdownOut3());
            altiCfg.setOutput4(configPage1.getDropdownOut4());
            altiCfg.setMainAltitude(configPage1.getMainAltitude());
        }

        if (configPage2 != null && configPage2.isViewCreated()) {
            altiCfg.setNbrOfMeasuresForApogee(configPage2.getApogeeMeasures());
            altiCfg.setAltimeterResolution(configPage2.getAltimeterResolution());
            altiCfg.setEepromSize(configPage2.getEEpromSize());
            altiCfg.setBeepOnOff(configPage2.getBeepOnOff());
            altiCfg.setEndRecordAltitude(configPage2.getEndRecordAltitude());
            altiCfg.setTelemetryType(configPage2.getRecordTempOnOff());
            altiCfg.setSupersonicDelay(configPage2.getSupersonicDelayOnOff());
            altiCfg.setLiftOffAltitude(configPage2.getLiftOffAltitude());
            altiCfg.setBatteryType(configPage2.getBatteryType());
            altiCfg.setRecordingTimeout(configPage2.getRecordingTimeout());
        }

        if (configPage3 != null && configPage3.isViewCreated()) {
            altiCfg.setBeepingFrequency(configPage3.getFreq());
            altiCfg.setBeepingMode(configPage3.getDropdownBipMode());
            altiCfg.setUnits(configPage3.getDropdownUnits());
            if ("AltiServo".equals(altiCfg.getAltimeterName())) {
                altiCfg.setServoStayOn(configPage3.getServoStayOn());
                altiCfg.setServoSwitch(configPage3.getServoSwitch());
            }
            altiCfg.setAltiID(configPage3.getAltiID());
            altiCfg.setUseTelemetryPort(configPage3.getDropdownUseTelemetryPort());

            String altiName = altiCfg.getAltimeterName();
            if ((("AltiMultiESP32".equals(altiName) || "AltiMultiESP32_accel".equals(altiName)
                    || "AltiMultiESP32_accel_375".equals(altiName) || "AltiMultiESP32_accel_345".equals(altiName))
                    && altiCfg.getAltiMajorVersion() >= 2 && altiCfg.getAltiMinorVersion() > 0)
                    || ("TTGOBearAltimeter".equals(altiName) && altiCfg.getAltiMajorVersion() >= 0 && altiCfg.getAltiMinorVersion() > 4)) {
                altiCfg.setBluetoothName(configPage3.getBluetoothName());
            }
        }

        if ("AltiServo".equals(altiCfg.getAltimeterName()) && configPage4 != null && configPage4.isViewCreated()) {
            altiCfg.setServo1OnPos(configPage4.getServo1OnPos());
            altiCfg.setServo2OnPos(configPage4.getServo2OnPos());
            altiCfg.setServo3OnPos(configPage4.getServo3OnPos());
            altiCfg.setServo4OnPos(configPage4.getServo4OnPos());
            altiCfg.setServo1OffPos(configPage4.getServo1OffPos());
            altiCfg.setServo2OffPos(configPage4.getServo2OffPos());
            altiCfg.setServo3OffPos(configPage4.getServo3OffPos());
            altiCfg.setServo4OffPos(configPage4.getServo4OffPos());
        }

        // Validate deployment outputs
        int nbrOfMain = 0;
        int nbrOfDrogue = 0;
        if (altiCfg.getOutput1() == 0) nbrOfMain++;
        if (altiCfg.getOutput2() == 0) nbrOfMain++;
        if (altiCfg.getOutput3() == 0) nbrOfMain++;
        if (altiCfg.getOutput4() == 0) nbrOfMain++;

        if (altiCfg.getOutput1() == 1) nbrOfDrogue++;
        if (altiCfg.getOutput2() == 1) nbrOfDrogue++;
        if (altiCfg.getOutput3() == 1) nbrOfDrogue++;
        if (altiCfg.getOutput4() == 1) nbrOfDrogue++;

        if (!myBT.getAppConf().getAllowMultipleDrogueMain() && !"TTGOBearAltimeter".equals(altiCfg.getAltimeterName())) {
            if (nbrOfMain > 1) {
                msg(getString(R.string.msg1));
                return;
            }
            if (nbrOfDrogue > 1) {
                msg(getString(R.string.msg2));
                return;
            }
        }

        // Baud rate change confirmation
        if (configPage2 != null && configPage2.isViewCreated() && altiCfg.getConnectionSpeed() != configPage2.getBaudRate()) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.msg9))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.Yes), (dialog, id) -> {
                        dialog.dismiss();
                        altiCfg.setConnectionSpeed(configPage2.getBaudRate());
                        executeSendConfigAsync();
                    })
                    .setNegativeButton(getString(R.string.No), (dialog, id) -> dialog.dismiss())
                    .show();
        } else {
            executeSendConfigAsync();
        }
    }

    private void executeSendConfigAsync() {
        showProgress("Uploading configuration...");

        ioExecutor.execute(() -> {
            boolean success = performSendConfig();
            mainHandler.post(() -> {
                hideProgress();
                if (isFinishing() || isDestroyed()) return;

                if (success) {
                    msg("Configuration updated successfully");
                    finish();
                } else {
                    msg("Failed to write altimeter configuration");
                }
            });
        });
    }

    private boolean performSendConfig() {
        if (!myBT.getConnected()) return false;

        try {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("m0;");

            waitForStreamData(3000);
            myBT.ReadResult(3000);

            sendParamCommand("p,1," + altiCfg.getUnits());
            sendParamCommand("p,2," + altiCfg.getBeepingMode());
            sendParamCommand("p,3," + altiCfg.getOutput1());
            sendParamCommand("p,4," + altiCfg.getOutput2());
            sendParamCommand("p,5," + altiCfg.getOutput3());
            sendParamCommand("p,6," + altiCfg.getMainAltitude());
            sendParamCommand("p,7," + altiCfg.getSupersonicYesNo());
            sendParamCommand("p,8," + altiCfg.getOutput1Delay());
            sendParamCommand("p,9," + altiCfg.getOutput2Delay());
            sendParamCommand("p,10," + altiCfg.getOutput3Delay());
            sendParamCommand("p,11," + altiCfg.getBeepingFrequency());
            sendParamCommand("p,12," + altiCfg.getNbrOfMeasuresForApogee());
            sendParamCommand("p,13," + altiCfg.getEndRecordAltitude());
            sendParamCommand("p,14," + altiCfg.getTelemetryType());
            sendParamCommand("p,15," + altiCfg.getSupersonicDelay());
            sendParamCommand("p,16," + altiCfg.getConnectionSpeed());
            sendParamCommand("p,17," + altiCfg.getAltimeterResolution());
            sendParamCommand("p,18," + altiCfg.getEepromSize());
            sendParamCommand("p,19," + altiCfg.getBeepOnOff());
            sendParamCommand("p,20," + altiCfg.getOutput4());
            sendParamCommand("p,21," + altiCfg.getOutput4Delay());
            sendParamCommand("p,22," + altiCfg.getLiftOffAltitude());
            sendParamCommand("p,23," + altiCfg.getBatteryType());
            sendParamCommand("p,24," + altiCfg.getRecordingTimeout());
            sendParamCommand("p,25," + altiCfg.getAltiID());
            sendParamCommand("p,26," + altiCfg.getUseTelemetryPort());

            String altiName = altiCfg.getAltimeterName();
            if ((("AltiMultiESP32".equals(altiName) || "AltiMultiESP32_accel".equals(altiName)
                    || "AltiMultiESP32_accel_375".equals(altiName) || "AltiMultiESP32_accel_345".equals(altiName))
                    && altiCfg.getAltiMajorVersion() >= 2 && altiCfg.getAltiMinorVersion() > 0)
                    || ("TTGOBearAltimeter".equals(altiName) && altiCfg.getAltiMajorVersion() >= 0 && altiCfg.getAltiMinorVersion() > 4)) {
                sendParamCommand("z," + altiCfg.getBluetoothName());
            }

            if ("AltiServo".equals(altiName)) {
                sendParamCommand("p,27," + altiCfg.getServoSwitch());
                sendParamCommand("p,28," + altiCfg.getServo1OnPos());
                sendParamCommand("p,29," + altiCfg.getServo2OnPos());
                sendParamCommand("p,30," + altiCfg.getServo3OnPos());
                sendParamCommand("p,31," + altiCfg.getServo4OnPos());
                sendParamCommand("p,32," + altiCfg.getServo1OffPos());
                sendParamCommand("p,33," + altiCfg.getServo2OffPos());
                sendParamCommand("p,34," + altiCfg.getServo3OffPos());
                sendParamCommand("p,35," + altiCfg.getServo4OffPos());
                sendParamCommand("p,36," + altiCfg.getServoStayOn());
            }

            // Write EEPROM command
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("q;");
            waitForStreamData(3000);
            myBT.ReadResult(3000);

            // Re-enable main loop
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            myBT.write("m1;");
            waitForStreamData(3000);
            myBT.ReadResult(3000);
            myBT.flush();

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error writing config", e);
            return false;
        }
    }

    private void sendParamCommand(String command) {
        String stripped = command.replace("p", "").replace(",", "");
        String payload = command + "," + generateCheckSum(stripped) + ";";

        if (myBT.getConnected()) {
            myBT.flush();
            myBT.clearInput();
            myBT.setDataReady(false);
            myBT.write(payload);
            myBT.flush();

            waitForStreamData(3000);
            myBT.ReadResult(3000);
        }
    }

    private void waitForStreamData(long timeoutMs) {
        long start = System.currentTimeMillis();
        try {
            while (myBT.getInputStream() != null
                    && myBT.getInputStream().available() <= 0
                    && (System.currentTimeMillis() - start) < timeoutMs) {
                Thread.sleep(20);
            }
        } catch (IOException | InterruptedException ignored) {}
    }

    public static int generateCheckSum(String value) {
        byte[] data = value.getBytes();
        int checksum = 0;
        for (byte b : data) {
            checksum += (b & 0xFF);
        }
        return checksum % 256;
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgress();
        ioExecutor.shutdown();
    }

    public static class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_config_alti");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}