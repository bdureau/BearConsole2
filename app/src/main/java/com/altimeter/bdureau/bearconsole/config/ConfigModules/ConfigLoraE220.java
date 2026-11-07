package com.altimeter.bdureau.bearconsole.config.ConfigModules;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
 * @author Boris Dureau (boris.dureau@neuf.fr)
 * @description Activity dedicated to reading, validating, and writing configuration
 *              parameters for Ebyte LoRa E220 telemetry modules over a USB-TTL serial link.
 *              Executes I/O operations asynchronously to prevent UI freeze and memory leaks.
 */
public class ConfigLoraE220 extends AppCompatActivity {
    private static final String TAG = "ConfigLoraE220";

    // USB serial communication driver handle
    private Physicaloid mPhysicaloid;

    // UI action triggers and telemetry configuration controls
    private Button btRetrieveConfig, btSaveConfig;
    private Spinner spinnerLoraBaudRate, spinnerLoraTransMode, spinnerLoraChannelRSSI,
            spinnerLoraParity, spinnerLoraWoreCycle, spinnerLoraLBT, spinnerLoraAirRate,
            spinnerLoraPower, spinnerLoraPacketRSSI, spinnerLoraPacketSize;
    private TextView textLoraModuleValue;
    private EditText textLoraAddressValue, textLoraChannelValue;

    // Display labels and corresponding register bitmask arrays for LoRa configuration
    private final String[] itemsLoraBaudRate = {"1200", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
    private final int[] valuesLoraBaudRate = {0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111};

    private final String[] itemsLoraTransMode = {"Normal", "Fixed"};
    private final int[] valuesLoraTransMode = {0b0, 0b1};

    private final String[] itemsLoraChannelRSSI = {"Disable", "Enable"};
    private final int[] valuesLoraChannelRSSI = {0b0, 0b1};

    private final String[] itemsLoraParity = {"8N1", "8O1", "8E1"};
    private final int[] valuesLoraParity = {0b00, 0b01, 0b10};

    private final String[] itemsLoraWoreCycle = {"500ms", "1000ms", "1500ms", "2000ms", "2500ms", "3000ms", "3500ms", "4000ms"};
    private final int[] valuesLoraWoreCycle = {0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111};

    private final String[] itemsLoraLBT = {"Disable", "Enable"};
    private final int[] valuesLoraLBT = {0b0, 0b1};

    private final String[] itemsLoraAirRate = {"2.4Kbps", "4.8Kbps", "9.6Kbps", "19.2Kbps", "38.4Kbps", "62.5Kbps"};
    private final int[] valuesLoraAirRate = {0b010, 0b011, 0b100, 0b101, 0b110, 0b111};

    private final String[] itemsLoraPower = {"22dbm", "17dbm", "13dbm", "10dbm"};
    private final int[] valuesLoraPower = {0b00, 0b01, 0b10, 0b11};

    private final String[] itemsLoraPacketRSSI = {"Disable", "Enable"};
    private final int[] valuesLoraPacketRSSI = {0b0, 0b1};

    private final String[] itemsLoraPacketSize = {"200 Bytes", "128 Bytes", "64 Bytes", "32 Bytes"};
    private final int[] valuesLoraPacketSize = {0b00, 0b01, 0b10, 0b11};

    private AlertDialog alert;
    private ModuleInfo mInfo;

    // Single-thread pool for serialized UART operations and main looper handler for UI synchronization
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isTaskCancelled = false;
    @Override
    protected void attachBaseContext(Context newBase) {
        // Read language configuration directly from SharedPreferences to ensure
        // context localization is configured before Activity inflation
        SharedPreferences prefs = newBase.getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(ConsoleApplication.updateBaseContextLocale(newBase, lang));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTitle(R.string.telemetry_screen);
        setContentView(R.layout.activity_config_lora);

        initViews();
        setupAdapters();

        // Initialize USB serial peripheral interface
        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.open();
        mInfo = new ModuleInfo(mPhysicaloid);

        // Lock form inputs until configuration has been read from hardware
        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);

        showInitialDialog();
    }

    /**
     * Binds view elements from layout XML.
     */
    private void initViews() {
        btRetrieveConfig = findViewById(R.id.butLoraRetrieveConfig);
        btSaveConfig = findViewById(R.id.butLoraSaveConfig);
        spinnerLoraBaudRate = findViewById(R.id.spinnerLoraBaudRate);
        spinnerLoraTransMode = findViewById(R.id.spinnerLoraTransMode);
        spinnerLoraChannelRSSI = findViewById(R.id.spinnerLoraChannelRSSI);
        spinnerLoraParity = findViewById(R.id.spinnerLoraParity);
        spinnerLoraWoreCycle = findViewById(R.id.spinnerLoraWoreCycle);
        spinnerLoraLBT = findViewById(R.id.spinnerLoraLBT);
        spinnerLoraAirRate = findViewById(R.id.spinnerLoraAirRate);
        spinnerLoraPower = findViewById(R.id.spinnerLoraPower);
        spinnerLoraPacketRSSI = findViewById(R.id.spinnerLoraPacketRSSI);
        spinnerLoraPacketSize = findViewById(R.id.spinnerLoraPacketSize);

        textLoraModuleValue = findViewById(R.id.textLoraModuleValue);
        textLoraAddressValue = findViewById(R.id.textLoraAddressValue);
        textLoraChannelValue = findViewById(R.id.textLoraChannelValue);
    }

    /**
     * Populates all configuration dropdown selectors.
     */
    private void setupAdapters() {
        setupSpinner(spinnerLoraBaudRate, itemsLoraBaudRate);
        setupSpinner(spinnerLoraTransMode, itemsLoraTransMode);
        setupSpinner(spinnerLoraChannelRSSI, itemsLoraChannelRSSI);
        setupSpinner(spinnerLoraParity, itemsLoraParity);
        setupSpinner(spinnerLoraWoreCycle, itemsLoraWoreCycle);
        setupSpinner(spinnerLoraLBT, itemsLoraLBT);
        setupSpinner(spinnerLoraAirRate, itemsLoraAirRate);
        setupSpinner(spinnerLoraPower, itemsLoraPower);
        setupSpinner(spinnerLoraPacketRSSI, itemsLoraPacketRSSI);
        setupSpinner(spinnerLoraPacketSize, itemsLoraPacketSize);
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * Displays initial instructions advising the user on module wiring and configuration mode (M0/M1).
     */
    private void showInitialDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setMessage(R.string.lora_module_config_msg)
                .setTitle(R.string.lora_module_config_msg_title)
                .setCancelable(false)
                .setPositiveButton(R.string.lora_module_config_button_ok, (dialog, id) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays an asynchronous progress dialog with cancellation capability.
     */
    private void showProgressDialog(String title, String message) {
        dismissProgressDialog();
        if (isFinishing() || isDestroyed()) return;

        alert = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(R.string.m3DR_Cancel, (dialog, which) -> {
                    isTaskCancelled = true;
                    dialog.dismiss();
                })
                .create();
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

    /**
     * Unlocks UI controls once valid parameters have been read.
     */
    public void EnableUI() {
        spinnerLoraBaudRate.setEnabled(true);
        spinnerLoraTransMode.setEnabled(true);
        spinnerLoraChannelRSSI.setEnabled(true);
        spinnerLoraParity.setEnabled(true);
        spinnerLoraWoreCycle.setEnabled(true);
        spinnerLoraLBT.setEnabled(true);
        spinnerLoraAirRate.setEnabled(true);
        spinnerLoraPower.setEnabled(true);
        spinnerLoraPacketRSSI.setEnabled(true);
        spinnerLoraPacketSize.setEnabled(true);
        textLoraAddressValue.setEnabled(true);
        textLoraChannelValue.setEnabled(true);
    }

    /**
     * Disables UI controls during transactions or before read operations.
     */
    public void DisableUI() {
        spinnerLoraBaudRate.setEnabled(false);
        spinnerLoraTransMode.setEnabled(false);
        spinnerLoraChannelRSSI.setEnabled(false);
        spinnerLoraParity.setEnabled(false);
        spinnerLoraWoreCycle.setEnabled(false);
        spinnerLoraLBT.setEnabled(false);
        spinnerLoraAirRate.setEnabled(false);
        spinnerLoraPower.setEnabled(false);
        spinnerLoraPacketRSSI.setEnabled(false);
        spinnerLoraPacketSize.setEnabled(false);
        textLoraAddressValue.setEnabled(false);
        textLoraChannelValue.setEnabled(false);
    }

    /**
     * Maps binary register values to matching dropdown item positions.
     */
    private int findIndexForValue(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) return i;
        }
        return 0;
    }

    /**
     * Queries configuration registers over serial:
     * Sends command 0xC1 0x00 0x08 (Read 8 bytes starting from address 0x00).
     */
    public void onClickRetrieveConfig(View v) {
        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_connecting), getString(R.string.lora_module_connecting_msg));

        ioExecutor.execute(() -> {
            if (!mPhysicaloid.isOpened()) {
                mPhysicaloid.open();
            }
            // E220 configuration mode operates at 9600 8N1
            mPhysicaloid.setBaudrate(9600);

            updateProgressMessage(getString(R.string.lora_module_loading_config_msg));

            byte[] cmd = {(byte) 0xC1, 0x00, 0x08};
            byte[] response = mInfo.runCommand(cmd);

            // Valid response starts with 0xC1 and must contain at least 9 bytes (Header + start + length + 6 payload bytes)
            boolean valid = response.length >= 9 && (response[0] & 0xFF) == 0xC1;

            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;

                if (valid && !isTaskCancelled) {
                    // Module Address: 16-bit unsigned (Bytes 3 and 4)
                    int moduleAddress = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
                    textLoraAddressValue.setText(String.valueOf(moduleAddress));

                    // Byte 5 (REG0): UART Baud [7:5], Parity [4:3], Air Data Rate [2:0]
                    int baudRate = (response[5] & 0xFF) >> 5;
                    spinnerLoraBaudRate.setSelection(findIndexForValue(valuesLoraBaudRate, baudRate));

                    int parity = ((response[5] & 0xFF) >> 3) & 0x03;
                    spinnerLoraParity.setSelection(findIndexForValue(valuesLoraParity, parity));

                    int airRate = response[5] & 0x07;
                    spinnerLoraAirRate.setSelection(findIndexForValue(valuesLoraAirRate, airRate));

                    // Byte 6 (REG1): Sub-packet size [7:6], Ambient noise RSSI enable [5], TX Power [1:0]
                    int packetSize = (response[6] & 0xFF) >> 6;
                    spinnerLoraPacketSize.setSelection(findIndexForValue(valuesLoraPacketSize, packetSize));

                    int channelRssi = (response[6] >> 5) & 0x01;
                    spinnerLoraChannelRSSI.setSelection(findIndexForValue(valuesLoraChannelRSSI, channelRssi));

                    int power = response[6] & 0x03;
                    spinnerLoraPower.setSelection(findIndexForValue(valuesLoraPower, power));

                    // Byte 7 (REG2): Channel frequency offset (0 - 83)
                    int channel = response[7] & 0xFF;
                    textLoraChannelValue.setText(String.valueOf(channel));

                    // Byte 8 (REG3): RSSI byte [7], Transmission method [6], LBT [4], WOR cycle [2:0]
                    int packetRssi = (response[8] & 0xFF) >> 7;
                    spinnerLoraPacketRSSI.setSelection(findIndexForValue(valuesLoraPacketRSSI, packetRssi));

                    int transMode = ((response[8] & 0xFF) >> 6) & 0x01;
                    spinnerLoraTransMode.setSelection(findIndexForValue(valuesLoraTransMode, transMode));

                    int lbt = ((response[8] & 0xFF) >> 4) & 0x01;
                    spinnerLoraLBT.setSelection(findIndexForValue(valuesLoraLBT, lbt));

                    int worCycle = response[8] & 0x07;
                    spinnerLoraWoreCycle.setSelection(findIndexForValue(valuesLoraWoreCycle, worCycle));

                    EnableUI();
                    btSaveConfig.setEnabled(true);
                } else {
                    Toast.makeText(ConfigLoraE220.this, "Erreur de lecture du module", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Validates input values and writes parameters into EEPROM/registers using single atomic command:
     * 0xC0 + Start (0x00) + Length (0x06) + 6 configuration payload bytes.
     */
    public void onClickSaveConfig(View v) {
        String sAddress = textLoraAddressValue.getText().toString().trim();
        String sChannel = textLoraChannelValue.getText().toString().trim();

        // Strict numeric check for address range (0 to 65535)
        if (!sAddress.matches("^\\d+$") || Integer.parseInt(sAddress) > 65535) {
            Toast.makeText(this, R.string.lora_module_address_range, Toast.LENGTH_SHORT).show();
            return;
        }

        // Strict numeric check for channel range (0 to 83)
        if (!sChannel.matches("^\\d+$") || Integer.parseInt(sChannel) > 83) {
            Toast.makeText(this, R.string.lora_module_channel_value_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        int iAddress = Integer.parseInt(sAddress);
        byte addh = (byte) ((iAddress >> 8) & 0xFF);
        byte addl = (byte) (iAddress & 0xFF);

        // Build REG0 byte
        int byteReg0 = (valuesLoraBaudRate[spinnerLoraBaudRate.getSelectedItemPosition()] << 5)
                | (valuesLoraParity[spinnerLoraParity.getSelectedItemPosition()] << 3)
                | valuesLoraAirRate[spinnerLoraAirRate.getSelectedItemPosition()];

        // Build REG1 byte
        int byteReg1 = (valuesLoraPacketSize[spinnerLoraPacketSize.getSelectedItemPosition()] << 6)
                | (valuesLoraChannelRSSI[spinnerLoraChannelRSSI.getSelectedItemPosition()] << 5)
                | valuesLoraPower[spinnerLoraPower.getSelectedItemPosition()];

        // Build REG2 byte (Channel)
        int byteReg2 = Integer.parseInt(sChannel) & 0xFF;

        // Build REG3 byte
        int byteReg3 = (valuesLoraPacketRSSI[spinnerLoraPacketRSSI.getSelectedItemPosition()] << 7)
                | (valuesLoraTransMode[spinnerLoraTransMode.getSelectedItemPosition()] << 6)
                | (valuesLoraLBT[spinnerLoraLBT.getSelectedItemPosition()] << 4)
                | valuesLoraWoreCycle[spinnerLoraWoreCycle.getSelectedItemPosition()];

        // 0xC0 signifies saving configuration permanently to EEPROM
        byte[] cmd = {
                (byte) 0xC0, 0x00, 0x06,
                addh, addl,
                (byte) byteReg0, (byte) byteReg1, (byte) byteReg2, (byte) byteReg3
        };

        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_save_cfg), getString(R.string.m3DR_run_save));

        ioExecutor.execute(() -> {
            byte[] response = mInfo.runCommand(cmd);
            // Module acknowledges persistent write with 0xC1 echo header
            boolean success = response.length >= 3 && (response[0] & 0xFF) == 0xC1;

            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;

                if (success) {
                    DisableUI();
                    btSaveConfig.setEnabled(false);
                    Toast.makeText(ConfigLoraE220.this, "Configuration enregistrée", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConfigLoraE220.this, "Échec de sauvegarde", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public void onClickDismiss(View v) {
        finish();
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

    /**
     * Low-level helper encapsulating serial flush, write, and timeout-based reception.
     */
    public class ModuleInfo {
        ModuleInfo(Physicaloid mPhysi) {}

        /**
         * Clears input buffer, transmits command, and captures returned byte stream.
         */
        public byte[] runCommand(byte[] command) {
            drain();
            mPhysicaloid.write(command, command.length);
            byte[] buf = new byte[64];
            int retval = recv(buf, buf.length, 600);
            byte[] result = new byte[retval];
            System.arraycopy(buf, 0, result, 0, retval);
            return result;
        }

        /**
         * Flushes stale serial data before issuing a new command.
         */
        public void drain() {
            byte[] buf = new byte[64];
            long startTime = System.currentTimeMillis();
            try {
                while ((System.currentTimeMillis() - startTime) < 150) {
                    int r = mPhysicaloid.read(buf, buf.length);
                    if (r <= 0) Thread.sleep(15);
                }
            } catch (InterruptedException ignored) {}
        }

        /**
         * Reads serial bytes up to length or until timeout expires.
         */
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
            i.putExtra("help_file", "help_configLora");
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