package com.altimeter.bdureau.bearconsole.config.ConfigModules;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.config.AppTabConfigActivity;
import com.physicaloid.lib.Physicaloid;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @description: Configuration of 3DR telemetry modules via TTL cable without memory leaks or UI lockups.
 * @author: boris.dureau@neuf.fr
 **/
public class Config3DR extends AppCompatActivity {
    private static final String TAG = "Config3DR";

    private Physicaloid mPhysicaloid;

    private String[] itemsBaudRate, itemsBaudRateShort, itemsAirSpeed, itemsNetID, itemsTXPower,
            itemsMavLink, itemsMinFreq, itemsNbrOfChannel, itemsDutyCycle, itemsLBTRSSI, itemsMaxWindow;
    private Spinner dropdownBaudRate, dropdownAirSpeed, dropdownNetID, dropdownTXPower,
            dropdownMavLink, dropdownMinFreq, dropdownMaxFreq, dropdownNbrOfChannel,
            dropdownDutyCycle, dropdownLBTRSSI, dropdownMaxWindow;
    private TextView txtVersion, txtRSSI, txtFormat;
    private CheckBox checkBoxECC, checkBoxOpResend, checkBoxRTSCTS;
    private Button btRetrieveConfig, btSaveConfig;

    private ModuleInfo mInfo;
    private ConsoleApplication myBT;
    private int currentBaudRate = 0;

    private AlertDialog alert;

    // Background thread executor and main thread Handler
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isTaskCancelled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_config3_dr);

        txtVersion = findViewById(R.id.textversionentry);
        txtRSSI = findViewById(R.id.textRSSIentry);
        txtFormat = findViewById(R.id.textFormatentry);
        checkBoxECC = findViewById(R.id.checkBoxECC);
        checkBoxOpResend = findViewById(R.id.checkBoxOpResend);
        checkBoxRTSCTS = findViewById(R.id.checkBoxRTSCTS);
        btRetrieveConfig = findViewById(R.id.btRetrieveConfig);
        btSaveConfig = findViewById(R.id.btSaveConfig);

        setupSpinners();

        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.open();
        mInfo = new ModuleInfo(mPhysicaloid);

        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);

        showInitialDialog();
    }

    private void setupSpinners() {
        dropdownBaudRate = findViewById(R.id.spinnerBaudRate);
        itemsBaudRate = new String[]{"1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200", "230400"};
        itemsBaudRateShort = new String[]{"1", "2", "4", "9", "14", "19", "28", "38", "57", "115", "230"};
        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsBaudRate);
        adapterBaudRate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownBaudRate.setAdapter(adapterBaudRate);

        dropdownAirSpeed = findViewById(R.id.spinnerAirSpeed);
        itemsAirSpeed = new String[]{"2", "4", "8", "16", "19", "24", "32", "48", "64", "96", "128", "192", "250"};
        ArrayAdapter<String> adapterAirSpeed = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsAirSpeed);
        dropdownAirSpeed.setAdapter(adapterAirSpeed);

        dropdownNetID = findViewById(R.id.spinnerNetID);
        itemsNetID = new String[500];
        for (int i = 0; i < 500; i++) itemsNetID[i] = Integer.toString(i);
        ArrayAdapter<String> adapterNetID = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsNetID);
        dropdownNetID.setAdapter(adapterNetID);

        dropdownTXPower = findViewById(R.id.spinnerTXPower);
        itemsTXPower = new String[21];
        for (int i = 0; i < 21; i++) itemsTXPower[i] = Integer.toString(i);
        ArrayAdapter<String> adapterTXPower = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsTXPower);
        dropdownTXPower.setAdapter(adapterTXPower);

        dropdownMavLink = findViewById(R.id.spinnerMavLink);
        itemsMavLink = new String[]{"RawData", "MavLink", "LowLatency"};
        ArrayAdapter<String> adapterMavLink = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsMavLink);
        dropdownMavLink.setAdapter(adapterMavLink);

        dropdownMinFreq = findViewById(R.id.spinnerMinFreq);
        dropdownMaxFreq = findViewById(R.id.spinnerMaxFreq);
        populateInitialFrequencies();

        dropdownNbrOfChannel = findViewById(R.id.spinnerNbrOfChannel);
        itemsNbrOfChannel = new String[]{"5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "30", "40", "50"};
        ArrayAdapter<String> adapterNbrOfChannel = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsNbrOfChannel);
        dropdownNbrOfChannel.setAdapter(adapterNbrOfChannel);

        dropdownDutyCycle = findViewById(R.id.spinnerDutyCycle);
        itemsDutyCycle = new String[]{"10", "20", "30", "40", "50", "60", "70", "80", "90", "100"};
        ArrayAdapter<String> adapterDutyCycle = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsDutyCycle);
        dropdownDutyCycle.setAdapter(adapterDutyCycle);

        dropdownLBTRSSI = findViewById(R.id.spinnerLBTRSSI);
        itemsLBTRSSI = new String[]{"0", "1"};
        ArrayAdapter<String> adapterLBTRSSI = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsLBTRSSI);
        dropdownLBTRSSI.setAdapter(adapterLBTRSSI);

        dropdownMaxWindow = findViewById(R.id.spinnerMaxWindow);
        itemsMaxWindow = new String[99];
        for (int i = 33; i < 132; i++) itemsMaxWindow[i - 33] = Integer.toString(i);
        ArrayAdapter<String> adapterMaxWindow = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsMaxWindow);
        dropdownMaxWindow.setAdapter(adapterMaxWindow);
    }

    private void populateInitialFrequencies() {
        itemsMinFreq = new String[921];
        int a = 0;
        for (int i = 414000; i < 460050; i += 50) {
            itemsMinFreq[a++] = Integer.toString(i);
        }
        ArrayAdapter<String> adapterFreq = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsMinFreq);
        dropdownMinFreq.setAdapter(adapterFreq);
        dropdownMaxFreq.setAdapter(adapterFreq);
    }

    private void showInitialDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.dr_info_msg))
                .setTitle(R.string.dr_info_title)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                .show();
    }

    private void showProgressDialog(String title, String message, Runnable onCancel) {
        dismissProgressDialog();
        if (isFinishing() || isDestroyed()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.m3DR_Cancel), (dialog, which) -> {
                    isTaskCancelled = true;
                    dialog.dismiss();
                    if (onCancel != null) onCancel.run();
                });

        alert = builder.create();
        alert.show();
    }

    private void dismissProgressDialog() {
        if (alert != null && alert.isShowing() && !isFinishing() && !isDestroyed()) {
            try {
                alert.dismiss();
            } catch (Exception ignored) {}
        }
        alert = null;
    }

    private void updateProgressMessage(String message) {
        mHandler.post(() -> {
            if (alert != null && alert.isShowing() && !isFinishing() && !isDestroyed()) {
                alert.setMessage(message);
            }
        });
    }

    public void EnableUI() {
        dropdownBaudRate.setEnabled(true);
        dropdownAirSpeed.setEnabled(true);
        dropdownNetID.setEnabled(true);
        dropdownTXPower.setEnabled(true);
        dropdownMavLink.setEnabled(true);
        dropdownMinFreq.setEnabled(true);
        dropdownMaxFreq.setEnabled(true);
        dropdownNbrOfChannel.setEnabled(true);
        dropdownDutyCycle.setEnabled(true);
        dropdownLBTRSSI.setEnabled(true);
        dropdownMaxWindow.setEnabled(true);
        checkBoxECC.setEnabled(true);
        checkBoxOpResend.setEnabled(true);
        checkBoxRTSCTS.setEnabled(true);
    }

    public void DisableUI() {
        dropdownBaudRate.setEnabled(false);
        dropdownAirSpeed.setEnabled(false);
        dropdownNetID.setEnabled(false);
        dropdownTXPower.setEnabled(false);
        dropdownMavLink.setEnabled(false);
        dropdownMinFreq.setEnabled(false);
        dropdownMaxFreq.setEnabled(false);
        dropdownNbrOfChannel.setEnabled(false);
        dropdownDutyCycle.setEnabled(false);
        dropdownLBTRSSI.setEnabled(false);
        dropdownMaxWindow.setEnabled(false);
        checkBoxECC.setEnabled(false);
        checkBoxOpResend.setEnabled(false);
        checkBoxRTSCTS.setEnabled(false);
    }

    public int arrayIndex(String[] stringArray, String pattern) {
        if (stringArray == null || pattern == null) return -1;
        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].equals(pattern)) return i;
        }
        return -1;
    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void onClickDismiss(View v) {
        finish();
    }

    public void onClickRetrieveConfig(View v) {
        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_connecting), getString(R.string.m3DR_attempt_con), null);

        ioExecutor.execute(() -> {
            boolean connected = connectToModule();
            mHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (connected) {
                    executeGetAllConfig();
                } else {
                    dismissProgressDialog();
                    msg(getString(R.string.m3DR_failed_con));
                }
            });
        });
    }

    private boolean connectToModule() {
        if (currentBaudRate == 0) {
            for (String rate : itemsBaudRate) {
                if (isTaskCancelled) return false;
                if (!mPhysicaloid.isOpened()) mPhysicaloid.open();
                mPhysicaloid.setBaudrate(Integer.parseInt(rate));
                updateProgressMessage(getString(R.string.m3DR_attempt_con2) + rate);
                if (mInfo.ATMode()) {
                    currentBaudRate = Integer.parseInt(rate);
                    updateProgressMessage(getString(R.string.m3DR_connect_ok) + rate);
                    return true;
                }
            }
        } else {
            if (!mPhysicaloid.isOpened()) {
                if (mPhysicaloid.open()) mPhysicaloid.setBaudrate(currentBaudRate);
            }
            if (mInfo.ATMode()) return true;
            currentBaudRate = 0;
        }
        return false;
    }

    private void executeGetAllConfig() {
        updateProgressMessage(getString(R.string.m3DR_load_cfg));

        ioExecutor.execute(() -> {
            long error = 0;
            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATI");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    mHandler.post(() -> txtVersion.setText(parts[1]));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                mInfo.runCommand("ATI2");
                mInfo.runCommand("ATI3");
                mInfo.runCommand("ATI4");
                mInfo.runCommand("ATI6");
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATI7");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    mHandler.post(() -> txtRSSI.setText(parts[1].trim()));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS0?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    mHandler.post(() -> txtFormat.setText(parts[1]));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS1?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsBaudRateShort, parts[1].trim());
                    mHandler.post(() -> dropdownBaudRate.setSelection(idx));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS2?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsAirSpeed, parts[1].trim());
                    mHandler.post(() -> dropdownAirSpeed.setSelection(idx));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS3?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsNetID, parts[1].trim());
                    mHandler.post(() -> dropdownNetID.setSelection(idx));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS4?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsTXPower, parts[1].trim());
                    mHandler.post(() -> dropdownTXPower.setSelection(idx));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS5?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    boolean checked = "1".equals(parts[1].trim());
                    mHandler.post(() -> checkBoxECC.setChecked(checked));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS6?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsMavLink, parts[1].trim());
                    mHandler.post(() -> dropdownMavLink.setSelection(idx));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS7?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    boolean checked = "1".equals(parts[1].trim());
                    mHandler.post(() -> checkBoxOpResend.setChecked(checked));
                    if (parts[1].trim().equals("ERROR")) error++;
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS8?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int freq = 0;
                    if (parts[1].trim().matches("\\d+")) {
                        freq = Integer.parseInt(parts[1].trim());
                    }
                    int a = 0;
                    if (freq < 800000) {
                        itemsMinFreq = new String[921];
                        for (int i = 414000; i < 460050; i += 50) itemsMinFreq[a++] = Integer.toString(i);
                    } else {
                        itemsMinFreq = new String[41];
                        for (int i = 895000; i < 935050; i += 1000) itemsMinFreq[a++] = Integer.toString(i);
                    }
                    int idx = arrayIndex(itemsMinFreq, parts[1].trim());
                    mHandler.post(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(Config3DR.this, android.R.layout.simple_spinner_item, itemsMinFreq);
                        dropdownMinFreq.setAdapter(adapter);
                        dropdownMaxFreq.setAdapter(adapter);
                        dropdownMinFreq.setSelection(idx);
                    });
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS9?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsMinFreq, parts[1].trim());
                    mHandler.post(() -> dropdownMaxFreq.setSelection(idx));
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS10?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsNbrOfChannel, parts[1].trim());
                    mHandler.post(() -> dropdownNbrOfChannel.setSelection(idx));
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS11?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsDutyCycle, parts[1].trim());
                    mHandler.post(() -> dropdownDutyCycle.setSelection(idx));
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS12?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsLBTRSSI, parts[1].trim());
                    mHandler.post(() -> dropdownLBTRSSI.setSelection(idx));
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS14?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    boolean checked = "1".equals(parts[1].trim());
                    mHandler.post(() -> checkBoxRTSCTS.setChecked(checked));
                } else error++;
            }

            if (!isTaskCancelled) {
                String val = mInfo.runCommand("ATS15?");
                String[] parts = val.split("\n");
                if (parts.length > 1) {
                    int idx = arrayIndex(itemsMaxWindow, parts[1].trim());
                    mHandler.post(() -> dropdownMaxWindow.setSelection(idx));
                } else error++;
            }

            mInfo.runCommand("ATO");

            final boolean success = !isTaskCancelled && (error == 0);
            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    EnableUI();
                    btSaveConfig.setEnabled(true);
                } else {
                    msg("Failed to retrieve complete 3DR configuration");
                }
            });
        });
    }

    public void onClickSaveConfig(View v) {
        final String baudRate = dropdownBaudRate.getSelectedItem().toString().substring(0, 2);
        final String airSpeed = dropdownAirSpeed.getSelectedItem().toString();
        final String netID = dropdownNetID.getSelectedItem().toString();
        final String txPower = dropdownTXPower.getSelectedItem().toString();
        final boolean boxECC = checkBoxECC.isChecked();
        final String mavLink = dropdownMavLink.getSelectedItem().toString();
        final boolean opResend = checkBoxOpResend.isChecked();
        final String minFreq = dropdownMinFreq.getSelectedItem().toString();
        final String maxFreq = dropdownMaxFreq.getSelectedItem().toString();
        final String nbrOfChannel = dropdownNbrOfChannel.getSelectedItem().toString();
        final String dutyCycle = dropdownDutyCycle.getSelectedItem().toString();
        final String LBTRSSI = dropdownLBTRSSI.getSelectedItem().toString();
        final boolean RTSCTS = checkBoxRTSCTS.isChecked();
        final String maxWindow = dropdownMaxWindow.getSelectedItem().toString();

        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_save_cfg), getString(R.string.m3DR_run_save), null);

        ioExecutor.execute(() -> {
            long error = 0;
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS1=" + baudRate));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS2=" + airSpeed));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS3=" + netID));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS4=" + txPower));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS5=" + (boxECC ? "1" : "0")));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS6=" + mavLink));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS7=" + (opResend ? "1" : "0")));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS8=" + minFreq));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS9=" + maxFreq));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS10=" + nbrOfChannel));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS11=" + dutyCycle));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS12=" + LBTRSSI));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS14=" + (RTSCTS ? "1" : "0")));
            if (!isTaskCancelled) error += checkResponse(mInfo.runCommand("ATS15=" + maxWindow));

            if (!isTaskCancelled && error == 0) {
                mInfo.runCommand("AT&W");
            }
            mInfo.runCommand("ATO");

            final boolean success = !isTaskCancelled && (error == 0);
            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    DisableUI();
                    btSaveConfig.setEnabled(false);
                    msg("Configuration saved successfully");
                } else {
                    msg("Error saving 3DR configuration");
                }
            });
        });
    }

    private int checkResponse(String response) {
        if (response == null) return 1;
        String[] parts = response.split("\n");
        return (parts.length > 1 && parts[1].trim().equals("OK")) ? 0 : 1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        ioExecutor.shutdown();
        if (mPhysicaloid != null && mPhysicaloid.isOpened()) {
            mPhysicaloid.close();
        }
    }

    public class ModuleInfo {
        ModuleInfo(Physicaloid mPhysi) {}

        public boolean ATMode() {
            drain();
            byte[] cmd0 = "+++".getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);

            byte[] buf = new byte[200];
            int retval = recv(buf, 200, 1000);
            if (retval > 1) {
                String str = new String(buf, 0, retval);
                return str.contains("OK");
            }
            return false;
        }

        public String runCommand(String command) {
            command = command + "\n\r";
            drain();
            byte[] cmd0 = command.getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);

            byte[] buf = new byte[200];
            int retval = recv(buf, 200, 500);
            return retval > 0 ? new String(buf, 0, retval) : "";
        }

        public void drain() {
            byte[] buf = new byte[64];
            long startTime = System.currentTimeMillis();
            try {
                while ((System.currentTimeMillis() - startTime) < 200) {
                    int r = mPhysicaloid.read(buf, buf.length);
                    if (r <= 0) Thread.sleep(15);
                }
            } catch (InterruptedException ignored) {}
        }

        private int recv(byte[] buf, int length, int timeout) {
            int total = 0;
            long startTime = System.currentTimeMillis();
            byte[] tmp = new byte[length];

            try {
                while ((System.currentTimeMillis() - startTime) < timeout && total < length) {
                    int r = mPhysicaloid.read(tmp, length - total);
                    if (r > 0) {
                        System.arraycopy(tmp, 0, buf, total, r);
                        total += r;
                        startTime = System.currentTimeMillis();
                    } else {
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException ignored) {}
            return total;
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
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, AppTabConfigActivity.class));
            return true;
        }
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_config3DR");
            startActivity(i);
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}