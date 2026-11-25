package com.altimeter.bdureau.bearconsole.connection;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: SearchBluetooth
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Selection of the Bluetooth module of the altimeter among the devices paired with the phone.
 *  It requests the BLUETOOTH_CONNECT permission (Android 12+), then asks to enable Bluetooth
 *  if needed, then lists the paired devices; each step resumes from the result of the previous
 *  one (Activity Result API). The address and name of the chosen module are stored in
 *  ConsoleApplication.
 * ============================================================================
 */

import com.altimeter.bdureau.bearconsole.BaseActivity;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;

import java.util.ArrayList;
import java.util.Set;

public class SearchBluetooth extends BaseActivity {
    //widgets
    Button btnPaired, btnDismiss;
    ListView devicelist;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    ConsoleApplication myBT;

    /**
     * Android 12+: runtime permission needed to read the paired devices and to connect.
     * Listing the paired devices does not need BLUETOOTH_SCAN (no discovery is done here).
     */
    private final ActivityResultLauncher<String> connectPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    // Permission now granted: continue (enable Bluetooth if needed, then list)
                    ensureBluetoothEnabledThenList();
                } else {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.permission_need_to_be_granted), Toast.LENGTH_LONG).show();
                }
            });

    /** System dialog asking the user to switch Bluetooth on. */
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (myBluetooth != null && myBluetooth.isEnabled()) {
                    pairedDevicesList();
                } else {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.bt_not_enabled), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.select_bluetooth_device);
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_search_bluetooth);

        btnPaired = findViewById(R.id.button);
        btnDismiss = findViewById(R.id.butSearchBTDismiss);
        devicelist = findViewById(R.id.listView);

        // The buttons are always wired first, so that the screen can be left whatever
        // happens with the permission or the Bluetooth state
        btnPaired.setOnClickListener(v -> ensureBluetoothEnabledThenList());
        btnDismiss.setOnClickListener(v -> finish());
        devicelist.setOnItemClickListener(myListClickListener);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (myBluetooth == null) {
            // The phone has no Bluetooth adapter
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.BT_msg1), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ensureBluetoothEnabledThenList();
    }

    /** True if the app may use the Bluetooth API (always true before Android 12). */
    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Sequence: permission (Android 12+) -> Bluetooth switched on -> list of paired devices.
     * Each step that needs the user resumes the sequence from its result callback.
     */
    private void ensureBluetoothEnabledThenList() {
        if (myBluetooth == null) return;
        if (!hasConnectPermission()) {
            connectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            return;
        }
        if (!myBluetooth.isEnabled()) {
            try {
                enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (SecurityException e) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.permission_need_to_be_granted), Toast.LENGTH_LONG).show();
            }
            return;
        }
        pairedDevicesList();
    }

    /** Fills the list with the devices paired with the phone ("name\nMAC address"). */
    private void pairedDevicesList() {
        if (!hasConnectPermission()) return;
        ArrayList<String> list = new ArrayList<>();
        try {
            pairedDevices = myBluetooth.getBondedDevices();
        } catch (SecurityException e) {
            pairedDevices = null;
        }

        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice bt : pairedDevices) {
                String name;
                try {
                    name = bt.getName();
                } catch (SecurityException e) {
                    name = null;
                }
                list.add((name != null ? name : "?") + "\n" + bt.getAddress());
            }
        } else {
            // No paired Bluetooth device found
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.BT_msg2), Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            myBT.setAddress(address);
            String moduleName = info.substring(0, info.length() - 17);
            myBT.setModuleName(moduleName);
            finish();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
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

        //open application settings screen
        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i= new Intent(SearchBluetooth.this, HelpActivity.class);
            i.putExtra("help_file", "help_bluetooth");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i= new Intent(SearchBluetooth.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
