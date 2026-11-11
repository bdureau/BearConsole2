package com.altimeter.bdureau.bearconsole.Help;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AboutActivity
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  "About" screen: application name, version, author and copyright.
 * ============================================================================
 */

import com.altimeter.bdureau.bearconsole.BaseActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.altimeter.bdureau.bearconsole.BuildConfig;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;


public class AboutActivity extends BaseActivity {

    Button btnDismiss;
    TextView txtViewVersion;
    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setTitle(R.string.about_screen);

        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_about);

        btnDismiss = (Button)findViewById(R.id.butDismiss);
        txtViewVersion = (TextView) findViewById(R.id.txtViewVersion);
        txtViewVersion.setText(BuildConfig.VERSION_NAME);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the about activity
            }
        });


    }


}
