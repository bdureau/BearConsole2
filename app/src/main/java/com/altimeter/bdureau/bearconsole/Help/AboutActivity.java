package com.altimeter.bdureau.bearconsole.Help;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.altimeter.bdureau.bearconsole.BuildConfig;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;


/**
*   @description: just an about screen
*   @author: boris.dureau@neuf.fr
**/
public class AboutActivity extends AppCompatActivity {

    Button btnDismiss;
    TextView txtViewVersion;
    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        myBT = (ConsoleApplication) getApplication();

        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
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
