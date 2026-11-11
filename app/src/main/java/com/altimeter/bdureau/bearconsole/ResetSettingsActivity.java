package com.altimeter.bdureau.bearconsole;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: ResetSettingsActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Maintenance screen of the altimeter: erase all the recorded flights ("e;"), reset the
 *  altimeter configuration to the factory defaults ("d;") or delete the last flight ("x;").
 *  Each action asks for a confirmation.
 * ============================================================================
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class ResetSettingsActivity extends BaseActivity {

    Button btnClearAltiConfig, btnClearFlights, btnClearLastFlight, btnDismiss;
    ConsoleApplication myBT;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.reset_settings);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        setContentView(R.layout.activity_reset_settings);


        btnDismiss = (Button) findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the application configuration activity
            }
        });

        btnClearLastFlight = (Button) findViewById(R.id.butDeleteLastFlight);
        btnClearLastFlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteLastFlight();      //delete last flight
            }
        });

        btnClearAltiConfig = (Button) findViewById(R.id.butRestoreAltiCfg);
        btnClearAltiConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAltiConfig();
            }
        });
        btnClearFlights = (Button) findViewById(R.id.butClearFlights);
        btnClearFlights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFlights();

            }
        });
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo"))
            btnClearFlights.setVisibility(View.INVISIBLE);
        else
            btnClearFlights.setVisibility(View.VISIBLE);
    }

    public void clearFlights() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to erase all flight data, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg1))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if (myBT.getConnected()) {
                            //erase the config
                            myBT.write("e;".toString());
                            myBT.flush();
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void clearAltiConfig() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to reset your altimeter config, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg2))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if (myBT.getConnected()) {
                            //erase the config
                            myBT.write("d;".toString());
                            myBT.flush();
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void deleteLastFlight() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to erase your last flight data, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg3))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if (myBT.getConnected()) {
                            //erase the config
                            myBT.write("x;".toString());
                            myBT.flush();
                        }
                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
