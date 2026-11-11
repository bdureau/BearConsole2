package com.altimeter.bdureau.bearconsole.config.ConfigModules;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: ConfigBT
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Configuration of a serial Bluetooth module (HC-05 / HC-06) connected with a USB-TTL cable,
 *  using AT commands (AT+NAME, AT+PIN, AT+BAUD, AT+VERSION): module name, PIN code and
 *  serial speed. All the serial I/O runs in the
 *  background.
 * ============================================================================
 */

import com.altimeter.bdureau.bearconsole.BaseActivity;
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
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigBT extends BaseActivity {
    private static final String TAG = "ConfigBT";

    private Physicaloid mPhysicaloid;

    private String[] itemsBaudRate, itemsBaudRateShort, itemsModules;
    private Spinner dropdownBaudRate, dropdownModules;
    private TextView txtVersion, txtModuleName, txtPin, tvRead;
    private Button btRetrieveConfig, btSaveConfig;

    private ModuleInfo mInfo;
    private ConsoleApplication myBT;
    private int currentBaudRate = 38400;
    private String cmdTer = "";

    private AlertDialog alert;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isTaskCancelled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_config_bt);

        txtVersion = findViewById(R.id.textversionentry);
        txtModuleName = findViewById(R.id.textNameEntry);
        txtPin = findViewById(R.id.textPinEntry);
        tvRead = findViewById(R.id.tvRead);

        btRetrieveConfig = findViewById(R.id.butRetrieveConfig);
        btSaveConfig = findViewById(R.id.butSaveConfig);

        setupSpinners();

        mPhysicaloid = new Physicaloid(this);
        UartConfig uartConfig = new UartConfig(38400, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);
        mPhysicaloid.open();
        mPhysicaloid.setConfig(uartConfig);

        mInfo = new ModuleInfo(mPhysicaloid);

        DisableUI();
        btRetrieveConfig.setEnabled(true);
        btSaveConfig.setEnabled(false);

        showInitialDialog();
    }

    private void setupSpinners() {
        dropdownBaudRate = findViewById(R.id.spinnerBTBaudRate);
        itemsBaudRate = new String[]{"1200", "2400", "4800", "9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600", "1382400"};
        itemsBaudRateShort = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C"};

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsBaudRate);
        adapterBaudRate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownBaudRate.setAdapter(adapterBaudRate);

        dropdownModules = findViewById(R.id.spinnerModules);
        itemsModules = new String[]{"HC-05", "HC-06", "JDY-30", "JDY-31", "SPP", "AT-09", "Unknown"};
        ArrayAdapter<String> adapterModules = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsModules);
        adapterModules.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownModules.setAdapter(adapterModules);
    }

    private void showInitialDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setMessage(R.string.bt_info_msg)
                .setTitle(R.string.bt_info_title)
                .setCancelable(false)
                .setPositiveButton(R.string.bt_info_ok, (dialog, id) -> dialog.dismiss())
                .show();
    }

    private void showProgressDialog(String title, String message) {
        dismissProgressDialog();
        if (isFinishing() || isDestroyed()) return;

        alert = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.m3DR_Cancel), (dialog, which) -> {
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
        dropdownBaudRate.setEnabled(true);
        txtPin.setEnabled(true);
        txtModuleName.setEnabled(true);
    }

    public void DisableUI() {
        dropdownBaudRate.setEnabled(false);
        txtPin.setEnabled(false);
        txtModuleName.setEnabled(false);
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
        tvRead.setText("");
        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_connecting), getString(R.string.attempting_connect_msg));

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
                    final int idx = arrayIndex(itemsBaudRate, rate);
                    mHandler.post(() -> dropdownBaudRate.setSelection(idx));
                    return true;
                }
            }
        } else {
            if (!mPhysicaloid.isOpened()) {
                if (mPhysicaloid.open()) mPhysicaloid.setBaudrate(currentBaudRate);
            }
            if (mInfo.ATMode()) {
                final int idx = arrayIndex(itemsBaudRate, String.valueOf(currentBaudRate));
                mHandler.post(() -> dropdownBaudRate.setSelection(idx));
                return true;
            }
            currentBaudRate = 0;
        }
        return false;
    }

    private void executeGetAllConfig() {
        updateProgressMessage(getString(R.string.loading_bt_config_msg));

        ioExecutor.execute(() -> {
            long error = 0;
            String module = dropdownModules.getSelectedItem().toString();

            if (!isTaskCancelled) {
                if ("HC-05".equals(module) || "HC-06".equals(module) || "Unknown".equals(module)) {
                    String value = mInfo.runCommand("AT" + cmdTer);
                    mHandler.post(() -> tvRead.append("AT\n" + value + "\n"));
                    if (value.trim().isEmpty()) error++;
                }
            }

            if (!isTaskCancelled) {
                String value = mInfo.runCommand("AT+VERSION" + cmdTer);
                String[] parts = value.split("\n");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    mHandler.post(() -> {
                        txtVersion.setText(parts[0]);
                        tvRead.append(parts[0] + "\n");
                    });
                } else error++;
            }

            final boolean success = !isTaskCancelled && (error == 0);
            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    EnableUI();
                    btSaveConfig.setEnabled(true);
                } else {
                    msg(getString(R.string.module_config_read_failed));
                }
            });
        });
    }

    public void onClickSaveConfig(View v) {
        final String baudRate = dropdownBaudRate.getSelectedItem().toString();
        final String baudID = itemsBaudRateShort[dropdownBaudRate.getSelectedItemPosition()];
        final String name = txtModuleName.getText().toString().trim();
        final String pin = txtPin.getText().toString().trim();

        if (name.isEmpty()) {
            msg(getString(R.string.module_name_cannot_be_empty_msg));
            return;
        }
        if (pin.isEmpty()) {
            msg(getString(R.string.module_pin_cannot_be_empty_msg));
            return;
        }

        isTaskCancelled = false;
        showProgressDialog(getString(R.string.m3DR_save_cfg), getString(R.string.m3DR_run_save));

        ioExecutor.execute(() -> {
            long error = 0;

            if (!isTaskCancelled) {
                String resp = mInfo.runCommand("AT+PIN" + pin + cmdTer);
                mHandler.post(() -> tvRead.append(resp + "\n"));
                if (!resp.contains("OK")) error++;
            }

            if (!isTaskCancelled) {
                String resp = mInfo.runCommand("AT+NAME" + name + cmdTer);
                mHandler.post(() -> tvRead.append(resp + "\n"));
                if (!resp.contains("OK")) error++;
            }

            if (!isTaskCancelled) {
                String resp = mInfo.runCommand("AT+BAUD" + baudID + cmdTer);
                mHandler.post(() -> tvRead.append(resp + "\n"));
                if (!resp.contains("OK")) error++;
            }

            final boolean success = !isTaskCancelled && (error == 0);
            mHandler.post(() -> {
                dismissProgressDialog();
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    DisableUI();
                    btSaveConfig.setEnabled(false);
                    msg(getString(R.string.module_config_saved));
                } else {
                    msg(getString(R.string.module_config_save_failed));
                }
            });
        });
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
            byte[] cmd0;
            String mod = dropdownModules.getSelectedItem().toString();

            if ("HC-05".equals(mod) || "HC-06".equals(mod)) {
                cmd0 = "AT".getBytes();
                cmdTer = "";
            } else if ("JDY-30".equals(mod) || "JDY-31".equals(mod) || "Unknown".equals(mod) || "SPP".equals(mod)) {
                cmd0 = ("AT+VERSION\r\n").getBytes();
                cmdTer = "\r\n";
            } else {
                cmd0 = "AT\r".getBytes();
                cmdTer = "\r";
            }

            mPhysicaloid.write(cmd0, cmd0.length);

            byte[] buf = new byte[200];
            int retval = recv(buf, 200, 1000);
            return retval > 0;
        }

        public String runCommand(String command) {
            drain();
            byte[] cmd0 = command.getBytes();
            mPhysicaloid.write(cmd0, cmd0.length);

            byte[] buf = new byte[200];
            int retval = recv(buf, 200, 1000);
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
            i.putExtra("help_file", "help_configBT");
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