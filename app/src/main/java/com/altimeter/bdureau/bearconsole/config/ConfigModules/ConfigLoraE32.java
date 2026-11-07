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
 * Configuration des modules de télémesure LoRa E32 Ebyte via câble TTL.
 * Optimisé contre les blocages d'interface et les incohérences de masquage binaire.
 *
 * @author boris.dureau@neuf.fr
 */
public class ConfigLoraE32 extends AppCompatActivity {
    private static final String TAG = "ConfigLoraE32";

    private Physicaloid mPhysicaloid;
    private Button btRetrieveConfig, btSaveConfig;

    private Spinner spinnerLoraBaudRate, spinnerLoraIOMode, spinnerLoraFixedMode,
            spinnerLoraParity, spinnerLoraWakeupTime, spinnerLoraFECswitch,
            spinnerLoraAirRate, spinnerLoraPower;
    private EditText textLoraAddressValue, textLoraChannelValue;
    private TextView textLoraModuleValue;

    private final String[] itemsLoraBaudRate = {"1200", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
    private final int[] valuesLoraBaudRate = {0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111};

    private final String[] itemsLoraIOMode = {"OpenDrain", "PushPull"};
    private final int[] valuesLoraIOMode = {0b0, 0b1};

    private final String[] itemsLoraFixedMode = {"Disable", "Enable"};
    private final int[] valuesLoraFixedMode = {0b0, 0b1};

    private final String[] itemsLoraParity = {"8N1", "8O1", "8E1"};
    private final int[] valuesLoraParity = {0b00, 0b01, 0b10};

    private final String[] itemsLoraWakeupTime = {"250ms", "500ms", "750ms", "1000ms", "1250ms", "1500ms", "1750ms", "2000ms"};
    private final int[] valuesLoraWakeupTime = {0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111};

    private final String[] itemsLoraFECswitch = {"Disable", "Enable"};
    private final int[] valuesLoraFECswitch = {0b0, 0b1};

    private final String[] itemsLoraAirRate = {"0.3Kbps", "1.2Kbps", "2.4Kbps", "4.8Kbps", "9.6Kbps", "19.2Kbps"};
    private final int[] valuesLoraAirRate = {0b000, 0b001, 0b010, 0b011, 0b100, 0b101};

    private final String[] itemsLoraPower = {"30dbm", "27dbm", "24dbm", "21dbm"};
    private final int[] valuesLoraPower = {0b00, 0b01, 0b10, 0b11};

    private AlertDialog alert;
    private ModuleInfo mInfo;

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
        setContentView(R.layout.activity_config_lorae32);

        initViews();
        setupAdapters();

        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.open();
        mInfo = new ModuleInfo(mPhysicaloid);

        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);

        showInitialDialog();
    }

    private void initViews() {
        btRetrieveConfig = findViewById(R.id.butLoraRetrieveConfig);
        btSaveConfig = findViewById(R.id.butLoraSaveConfig);
        spinnerLoraBaudRate = findViewById(R.id.spinnerLoraBaudRate);
        spinnerLoraFixedMode = findViewById(R.id.spinnerLoraFixedMode);
        spinnerLoraParity = findViewById(R.id.spinnerLoraParity);
        spinnerLoraFECswitch = findViewById(R.id.spinnerLoraFECswitch);
        spinnerLoraAirRate = findViewById(R.id.spinnerLoraAirRate);
        spinnerLoraPower = findViewById(R.id.spinnerLoraPower);
        spinnerLoraIOMode = findViewById(R.id.spinnerLoraIOMode);
        textLoraAddressValue = findViewById(R.id.textLoraAddressValue);
        textLoraChannelValue = findViewById(R.id.textLoraChannelValue);
        spinnerLoraWakeupTime = findViewById(R.id.spinnerLoraWakeupTime);
        textLoraModuleValue = findViewById(R.id.textLoraModuleValue);
    }

    private void setupAdapters() {
        setupSpinner(spinnerLoraBaudRate, itemsLoraBaudRate);
        setupSpinner(spinnerLoraIOMode, itemsLoraIOMode);
        setupSpinner(spinnerLoraFixedMode, itemsLoraFixedMode);
        setupSpinner(spinnerLoraParity, itemsLoraParity);
        setupSpinner(spinnerLoraWakeupTime, itemsLoraWakeupTime);
        setupSpinner(spinnerLoraFECswitch, itemsLoraFECswitch);
        setupSpinner(spinnerLoraAirRate, itemsLoraAirRate);
        setupSpinner(spinnerLoraPower, itemsLoraPower);
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showInitialDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setMessage(R.string.lora_module_config_msg)
                .setTitle(R.string.lora_module_config_msg_title)
                .setCancelable(false)
                .setPositiveButton(R.string.lora_module_config_button_ok, (dialog, id) -> dialog.dismiss())
                .show();
    }

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

    public void EnableUI() {
        spinnerLoraBaudRate.setEnabled(true);
        spinnerLoraIOMode.setEnabled(true);
        spinnerLoraFixedMode.setEnabled(true);
        spinnerLoraParity.setEnabled(true);
        spinnerLoraWakeupTime.setEnabled(true);
        spinnerLoraFECswitch.setEnabled(true);
        spinnerLoraAirRate.setEnabled(true);
        spinnerLoraPower.setEnabled(true);
        textLoraAddressValue.setEnabled(true);
        textLoraChannelValue.setEnabled(true);
    }

    public void DisableUI() {
        spinnerLoraBaudRate.setEnabled(false);
        spinnerLoraIOMode.setEnabled(false);
        spinnerLoraFixedMode.setEnabled(false);
        spinnerLoraParity.setEnabled(false);
        spinnerLoraWakeupTime.setEnabled(false);
        spinnerLoraFECswitch.setEnabled(false);
        spinnerLoraAirRate.setEnabled(false);
        spinnerLoraPower.setEnabled(false);
        textLoraAddressValue.setEnabled(false);
        textLoraChannelValue.setEnabled(false);
    }

    private int findIndexForValue(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) return i;
        }
        return 0;
    }

    public void onClickRetrieveConfig(View v) {
        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_connecting), getString(R.string.lora_module_connecting_msg));

        ioExecutor.execute(() -> {
            if (!mPhysicaloid.isOpened()) {
                mPhysicaloid.open();
            }
            mPhysicaloid.setBaudrate(9600);

            updateProgressMessage(getString(R.string.lora_module_loading_config_msg));

            byte[] cmd = {(byte) 0xC1, (byte) 0xC1, (byte) 0xC1};
            byte[] value = mInfo.runCommand(cmd);

            boolean valid = value.length >= 6 && (value[0] & 0xFF) == 0xC0 || (value[0] & 0xFF) == 0xC1;

            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;

                if (valid && !isTaskCancelled) {
                    // Adresse (16 bits non signés)
                    int moduleAddress = ((value[1] & 0xFF) << 8) | (value[2] & 0xFF);
                    textLoraAddressValue.setText(String.valueOf(moduleAddress));

                    // Octet 3: Parité [7:6], Baud [5:3], Air Rate [2:0]
                    int parity = (value[3] & 0xFF) >> 6;
                    spinnerLoraParity.setSelection(findIndexForValue(valuesLoraParity, parity));

                    int baudRate = ((value[3] & 0xFF) >> 3) & 0x07;
                    spinnerLoraBaudRate.setSelection(findIndexForValue(valuesLoraBaudRate, baudRate));

                    int airRate = value[3] & 0x07;
                    spinnerLoraAirRate.setSelection(findIndexForValue(valuesLoraAirRate, airRate));

                    // Octet 4: Canal de communication [4:0]
                    int comChannel = value[4] & 0x1F;
                    textLoraChannelValue.setText(String.valueOf(comChannel));

                    // Octet 5: Mode Fixe [7], I/O [6], Wakeup [5:3], FEC [2], Transmission Power [1:0]
                    int fixedTrans = (value[5] & 0xFF) >> 7;
                    spinnerLoraFixedMode.setSelection(findIndexForValue(valuesLoraFixedMode, fixedTrans));

                    int ioDrive = ((value[5] & 0xFF) >> 6) & 0x01;
                    spinnerLoraIOMode.setSelection(findIndexForValue(valuesLoraIOMode, ioDrive));

                    int wakeup = ((value[5] & 0xFF) >> 3) & 0x07;
                    spinnerLoraWakeupTime.setSelection(findIndexForValue(valuesLoraWakeupTime, wakeup));

                    int fec = ((value[5] & 0xFF) >> 2) & 0x01;
                    spinnerLoraFECswitch.setSelection(findIndexForValue(valuesLoraFECswitch, fec));

                    int power = value[5] & 0x03;
                    spinnerLoraPower.setSelection(findIndexForValue(valuesLoraPower, power));

                    EnableUI();
                    btSaveConfig.setEnabled(true);
                } else {
                    Toast.makeText(ConfigLoraE32.this, "Erreur de lecture du module", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public void onClickSaveConfig(View v) {
        String sAddress = textLoraAddressValue.getText().toString().trim();
        String sChannel = textLoraChannelValue.getText().toString().trim();

        // Validation de l'adresse (0 - 65535)
        if (!sAddress.matches("^\\d+$") || Integer.parseInt(sAddress) > 65535) {
            Toast.makeText(this, R.string.lora_module_address_range, Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation du canal (0 - 83)
        if (!sChannel.matches("^\\d+$") || Integer.parseInt(sChannel) > 83) {
            Toast.makeText(this, R.string.lora_module_channel_value_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        int iAddress = Integer.parseInt(sAddress);
        byte addh = (byte) ((iAddress >> 8) & 0xFF);
        byte addl = (byte) (iAddress & 0xFF);

        int byte4 = (valuesLoraParity[spinnerLoraParity.getSelectedItemPosition()] << 6)
                | (valuesLoraBaudRate[spinnerLoraBaudRate.getSelectedItemPosition()] << 3)
                | valuesLoraAirRate[spinnerLoraAirRate.getSelectedItemPosition()];

        int byte5 = Integer.parseInt(sChannel) & 0x1F;

        int byte6 = (valuesLoraFixedMode[spinnerLoraFixedMode.getSelectedItemPosition()] << 7)
                | (valuesLoraIOMode[spinnerLoraIOMode.getSelectedItemPosition()] << 6)
                | (valuesLoraWakeupTime[spinnerLoraWakeupTime.getSelectedItemPosition()] << 3)
                | (valuesLoraFECswitch[spinnerLoraFECswitch.getSelectedItemPosition()] << 2)
                | valuesLoraPower[spinnerLoraPower.getSelectedItemPosition()];

        byte[] cmd = {(byte) 0xC0, addh, addl, (byte) byte4, (byte) byte5, (byte) byte6};

        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_save_cfg), getString(R.string.m3DR_run_save));

        ioExecutor.execute(() -> {
            byte[] response = mInfo.runCommand(cmd);
            boolean success = response.length >= 6 && (response[0] & 0xFF) == 0xC1;

            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;

                if (success) {
                    DisableUI();
                    btSaveConfig.setEnabled(false);
                    Toast.makeText(ConfigLoraE32.this, "Configuration enregistrée", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConfigLoraE32.this, "Échec de sauvegarde", Toast.LENGTH_SHORT).show();
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

    public class ModuleInfo {
        ModuleInfo(Physicaloid mPhysi) {}

        public byte[] runCommand(byte[] command) {
            drain();
            mPhysicaloid.write(command, command.length);
            byte[] buf = new byte[64];
            int retval = recv(buf, buf.length, 600);
            byte[] result = new byte[retval];
            System.arraycopy(buf, 0, result, 0, retval);
            return result;
        }

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