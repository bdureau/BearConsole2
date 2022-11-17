package com.altimeter.bdureau.bearconsole;
/**
 *   @description: Allow the user to reset the altimeter setting to factory default
 *   as well as clearing the flight list
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

//import java.io.IOException;

public class ResetSettingsActivity extends AppCompatActivity {

    Button btnClearAltiConfig, btnClearFlights, btnClearLastFlight, btnDismiss;
    ConsoleApplication myBT ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*if(AppCompatDelegate.getDefaultNightMode()== AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }*/
        super.onCreate(savedInstanceState);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_reset_settings);


        btnDismiss = (Button)findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the application configuration activity
            }
        });

        btnClearLastFlight = (Button)findViewById(R.id.butDeleteLastFlight);
        btnClearLastFlight.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteLastFlight();      //delete last flight
            }
        });

        btnClearAltiConfig = (Button)findViewById(R.id.butRestoreAltiCfg);
        btnClearAltiConfig.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)      {
                clearAltiConfig();
            }
        });
        btnClearFlights = (Button)findViewById(R.id.butClearFlights);
        btnClearFlights.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                clearFlights();

            }
        });
        if(myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") )
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
                     if(myBT.getConnected())
                      //   try  {
                             //erase the config
                             myBT.write("e;".toString());
                             myBT.flush();
                         //}
                        /* catch (IOException e) {

                         }*/

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
                        if(myBT.getConnected())
                            //try  {
                                //erase the config
                                myBT.write("d;".toString());
                                myBT.flush();
                           /* }
                            catch (IOException e) {

                            }*/
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
                        if(myBT.getConnected())
                            //erase the config
                            myBT.write("x;".toString());
                            myBT.flush();

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
