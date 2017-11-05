package com.altimeter.bdureau.bearconsole;

/**
*   @description: Retrieve altimeter configuration
*   @author: boris.dureau@neuf.fr
**/
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class AltimeterConfigActivity extends AppCompatActivity {
    Button btnUpload, btnDismiss;
    ConsoleApplication myBT ;
    private Spinner dropdownBipMode;
    private Spinner dropdownUnits;
    private EditText Freq;
    private EditText MainAltitude;
    private EditText OutDelay1, OutDelay2,OutDelay3;
    private Spinner dropdownOut1, dropdownOut2, dropdownOut3;
    private TextView altiName;
    private AltiConfigData AltiCfg = null;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_altimeter_config);

        btnUpload = (Button)findViewById(R.id.butUpload);
        btnDismiss = (Button)findViewById(R.id.butDismiss);

        btnUpload.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {

                //send back the config to the altimeter and exit if successful
                if(sendConfig())
                    finish();
            }
        });
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the activity
            }
        });


        //Bip mode
        dropdownBipMode = (Spinner)findViewById(R.id.spinnerBipMode);
        String[] items = new String[]{"Mode1", "Mode2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(AltimeterConfigActivity.this,
                android.R.layout.simple_spinner_dropdown_item, items);
        dropdownBipMode.setAdapter(adapter);


        //units
        dropdownUnits = (Spinner)findViewById(R.id.spinnerUnit);
        String[] items2 = new String[]{"Meters", "Feet"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(AltimeterConfigActivity.this,
                android.R.layout.simple_spinner_dropdown_item, items2);
        dropdownUnits.setAdapter(adapter2);



        //Output 1
        dropdownOut1 = (Spinner)findViewById(R.id.spinnerOut1);
        String[] items3 = new String[]{"Main", "Drogue", "Timer", "Disabled"};
        ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(AltimeterConfigActivity.this,
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownOut1.setAdapter(adapter3);

        //Output 2
        dropdownOut2 = (Spinner)findViewById(R.id.spinnerOut2);

        ArrayAdapter<String> adapter4 = new ArrayAdapter<String>(AltimeterConfigActivity.this,
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownOut2.setAdapter(adapter4);

        //Output 3
        dropdownOut3 = (Spinner)findViewById(R.id.spinnerOut3);

        ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(AltimeterConfigActivity.this,
                android.R.layout.simple_spinner_dropdown_item, items3);
        dropdownOut3.setAdapter(adapter5);

        //Altimeter name
        altiName = (TextView)findViewById(R.id.txtAltiNameValue);

        OutDelay1 = (EditText)findViewById(R.id.editTxtDelay1);

        OutDelay2 = (EditText)findViewById(R.id.editTxtDelay2);
        OutDelay3 =(EditText)findViewById(R.id.editTxtDelay3);

        MainAltitude= (EditText)findViewById(R.id.editTxtMainAltitude);
        //her you can set the bip frequency
        Freq=(EditText)findViewById(R.id.editTxtBipFreq);


        //readConfig();
        new RetrieveConfig().execute();

    }
    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }
    private boolean sendConfig()
    {

        long nbrOfMain=0;
        long nbrOfDrogue=0;
        AltiCfg.setOutput1Delay(Integer.parseInt(OutDelay1.getText().toString()));
        AltiCfg.setOutput2Delay(Integer.parseInt(OutDelay2.getText().toString()));
        AltiCfg.setOutput3Delay(Integer.parseInt(OutDelay3.getText().toString()));
        AltiCfg.setOutput1((int)dropdownOut1.getSelectedItemId());
        AltiCfg.setOutput2((int)dropdownOut2.getSelectedItemId());
        AltiCfg.setOutput3((int)dropdownOut3.getSelectedItemId());
        AltiCfg.setMainAltitude(Integer.parseInt(MainAltitude.getText().toString()));
        AltiCfg.setBeepingFrequency(Integer.parseInt(Freq.getText().toString()));
        AltiCfg.setBeepingMode((int)dropdownBipMode.getSelectedItemId());
        AltiCfg.setUnits((int)dropdownUnits.getSelectedItemId());

        if (AltiCfg.getOutput1() == 0)
            nbrOfMain++;
        if (AltiCfg.getOutput2() == 0)
            nbrOfMain++;
        if (AltiCfg.getOutput3() == 0)
            nbrOfMain++;


        if (AltiCfg.getOutput1() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getOutput2() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getOutput3() == 1)
            nbrOfDrogue++;

        if (nbrOfMain > 1)
        {
            //Only one main is allowed Please review your config
            msg(getResources().getString(R.string.msg1));
            return false;
        }
        if (nbrOfDrogue > 1)
        {
            //Only one Drogue is allowed Please review your config
            msg(getResources().getString(R.string.msg2));
            return false;
        }
        String altiCfgStr="";

        altiCfgStr = "s," +
                AltiCfg.getUnits() +","+
                AltiCfg.getBeepingMode()+","+
                AltiCfg.getOutput1()+","+
                AltiCfg.getOutput2()+","+
                AltiCfg.getOutput3()+","+
                AltiCfg.getMainAltitude()+","+
                AltiCfg.getSupersonicYesNo() +","+
                AltiCfg.getOutput1Delay() +","+
                AltiCfg.getOutput2Delay() +","+
                AltiCfg.getOutput3Delay() +","+
                AltiCfg.getBeepingFrequency() + ","+
                AltiCfg.getNbrOfMeasuresForApogee()+ ","+
                AltiCfg.getEndRecordAltitude()+ ","+
                AltiCfg.getRecordTemperature()+ ","+
                AltiCfg.getSupersonicDelay()+ ","+
                AltiCfg.getConnectionSpeed()+ ","+
                AltiCfg.getAltimeterResolution()+ ","+
                AltiCfg.getEepromSize()+
                ";\n";

        if(myBT.getConnected())

                //send back the config
                myBT.write(altiCfgStr.toString());

                msg(getResources().getString(R.string.msg3));

                myBT.flush();

        return true;
    }

    private void readConfig()
    {
        // ask for config
        if(myBT.getConnected()) {

            //msg("Retreiving altimeter config...");
            myBT.setDataReady(false);

            myBT.flush();
            myBT.clearInput();

            myBT.write("b;\n".toString());

            myBT.flush();


            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
        }
        //reading the config
        if(myBT.getConnected()) {
            String myMessage = "";
            long timeOut = 10000;
            long startTime = System.currentTimeMillis();

            myMessage =myBT.ReadResult();

            if (myMessage.equals( "start alticonfig end") )
            {
                try {
                    AltiCfg= myBT.getAltiConfigData();

                }
                catch (Exception e) {
                  //  msg("pb ready data");
                }
            }
            else
            {
               // msg("data not ready");

            }
        }

    }
    private class RetrieveConfig extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute()
        {
            //"Retrieving Altimeter config...", "Please wait!!!"
            progress = ProgressDialog.show(AltimeterConfigActivity.this,
                    getResources().getString(R.string.msg5),
                    getResources().getString(R.string.msg6));  //show a progress dialog
           /* progress = ProgressDialog.show(AltimeterConfigActivity.this,
                    "Retrieving Altimeter config...", "Please wait!!!");*/
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            readConfig();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);


            ///

            altiName.setText(AltiCfg.getAltimeterName()+ " ver: " +
                    AltiCfg.getAltiMajorVersion()+"."+AltiCfg.getAltiMinorVersion());


            dropdownBipMode.setSelection(AltiCfg.getBeepingMode());

            dropdownUnits.setSelection(AltiCfg.getUnits());
            dropdownOut1.setSelection(AltiCfg.getOutput1());
            dropdownOut2.setSelection(AltiCfg.getOutput2());
            dropdownOut3.setSelection(AltiCfg.getOutput3());
            OutDelay1.setText(String.valueOf(AltiCfg.getOutput1Delay()));
            OutDelay2.setText(String.valueOf(AltiCfg.getOutput2Delay()));
            OutDelay3.setText(String.valueOf(AltiCfg.getOutput3Delay()));
            MainAltitude.setText(String.valueOf(AltiCfg.getMainAltitude()));
            Freq.setText(String.valueOf(AltiCfg.getBeepingFrequency()));

            progress.dismiss();
        }
    }

}
