package com.altimeter.bdureau.bearconsole;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

/*
In this activity you should be able to choose the application languages and looks and feel
 */
public class AppConfigActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
    private Spinner spConnectionType;
    //BluetoothApplication myBT ;
    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth Application pointer
        //myBT = (BluetoothApplication) getApplication();
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        //
        setContentView(R.layout.activity_app_config);


       // myBT = (BluetoothApplication) getApplication();

        myBT.getAppConf().ReadConfig();
        btnDismiss = (Button)findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the application configuration activity
            }
        });

        btnSave = (Button)findViewById(R.id.butSave);
        btnSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //save the application configuration
                SaveConfig();

            }
        });

        bdtDefault= (Button)findViewById(R.id.butDefault);
        bdtDefault.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //restore the application default configuration
               RestoreToDefault();


            }
        });


        //Language
        spAppLanguage = (Spinner)findViewById(R.id.spinnerLanguage);
        String[] items = new String[]{ "English", "French", "Phone language"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spAppLanguage.setAdapter(adapter);

        // graph color
        spGraphColor = (Spinner)findViewById(R.id.spinnerGraphColor);
       // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};
        String[] itemsColor = new String[]{"BLACK",
                "WHITE",
                "MAGENTA",
                "BLUE",
                "YELLOW",
                "GREEN",
                "GRAY",
                "CYAN",
                "DKGRAY",
                "LTGRAY",
                "RED"};
        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itemsColor);
        spGraphColor.setAdapter(adapterColor);
        // graph back color
        spGraphBackColor = (Spinner)findViewById(R.id.spinnerGraphBackColor);
        ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itemsColor);

        spGraphBackColor.setAdapter(adapterGraphColor);
        //units
        spAppUnit = (Spinner)findViewById(R.id.spinnerUnits);
        String[] items2 = new String[]{"Meters", "Feet"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items2);
        spAppUnit.setAdapter(adapter2);

        //font size
        spFontSize = (Spinner)findViewById(R.id.spinnerFontSize);
        String[] itemsFontSize = new String[]{"8","9", "10", "11", "12","13",
                "14", "15", "16", "17", "18", "19", "20"};
        ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, itemsFontSize);
        spFontSize.setAdapter(adapterFontSize);

        //Baud Rate
        spBaudRate = (Spinner)findViewById(R.id.spinnerBaudRate);
        String[] items3 = new String[]{ "300",
                "1200",
                "2400",
                "4800",
                "9600",
                "14400",
                "19200",
                "28800",
                "38400",
                "57600",
                "115200",
                "230400"};
        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items3);
        spBaudRate.setAdapter(adapterBaudRate);

        //connection type
        spConnectionType = (Spinner)findViewById(R.id.spinnerConnectionType);
        String[] items4 = new String[]{ "bluetooth",
                "usb"};
        ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items4);
        spConnectionType.setAdapter(adapterConnectionType);
        ReadConfig();
    }

    void ReadConfig() {
        myBT.getAppConf().ReadConfig();
        spAppLanguage.setSelection(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        spAppUnit.setSelection(Integer.parseInt(myBT.getAppConf().getUnits()));
        spGraphColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        spGraphBackColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        spFontSize.setSelection((Integer.parseInt(myBT.getAppConf().getFontSize())-8));
        spBaudRate.setSelection(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        spConnectionType.setSelection(Integer.parseInt(myBT.getAppConf().getConnectionType()));
    }

    void SaveConfig() {
        myBT.getAppConf().setApplicationLanguage(""+spAppLanguage.getSelectedItemId()+"");
        myBT.getAppConf().setUnits(""+spAppUnit.getSelectedItemId()+"");
        myBT.getAppConf().setGraphColor(""+spGraphColor.getSelectedItemId()+"");
        myBT.getAppConf().setGraphBackColor(""+spGraphBackColor.getSelectedItemId()+"");
        myBT.getAppConf().setFontSize(""+(spFontSize.getSelectedItemId()+8)+"");
        myBT.getAppConf().setBaudRate(""+spBaudRate.getSelectedItemId()+"");
        myBT.getAppConf().setConnectionType(""+spConnectionType.getSelectedItemId()+"");
        myBT.getAppConf().SaveConfig();
        finish();
    }

    void RestoreToDefault() {
        myBT.getAppConf().ResetDefaultConfig();
        spAppLanguage.setSelection(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        spAppUnit.setSelection(Integer.parseInt(myBT.getAppConf().getUnits()));
        spGraphColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        spGraphBackColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        spFontSize.setSelection(Integer.parseInt(myBT.getAppConf().getFontSize())-8);
        spBaudRate.setSelection(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        spConnectionType.setSelection(Integer.parseInt(myBT.getAppConf().getConnectionType()));
    }

}
