package com.altimeter.bdureau.bearconsole.config;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

/**
 *   @description: In this activity you should be able to choose the application languages and looks and feel.
 *   Still a lot to do but it is a good start
 *   @author: boris.dureau@neuf.fr
 **/

public class AppConfigActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
    private Spinner spConnectionType, spGraphicsLibType;
    private AppConfigData appConfigData;


    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        // get the data for all the drop down
        appConfigData = new AppConfigData();
        setContentView(R.layout.activity_app_config);


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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
        spAppLanguage.setAdapter(adapter);

        // graph color
        spGraphColor = (Spinner)findViewById(R.id.spinnerGraphColor);
       // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};

        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spGraphColor.setAdapter(adapterColor);
        // graph back color
        spGraphBackColor = (Spinner)findViewById(R.id.spinnerGraphBackColor);
        ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

        spGraphBackColor.setAdapter(adapterGraphColor);
        //units
        spAppUnit = (Spinner)findViewById(R.id.spinnerUnits);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
        spAppUnit.setAdapter(adapter2);

        //font size
        spFontSize = (Spinner)findViewById(R.id.spinnerFontSize);

        ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
        spFontSize.setAdapter(adapterFontSize);

        //Baud Rate
        spBaudRate = (Spinner)findViewById(R.id.spinnerBaudRate);

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
        spBaudRate.setAdapter(adapterBaudRate);

        //connection type
        spConnectionType = (Spinner)findViewById(R.id.spinnerConnectionType);

        ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
        spConnectionType.setAdapter(adapterConnectionType);

        //Graphics lib type
        spGraphicsLibType = (Spinner)findViewById(R.id.spinnerGraphicLibType);
        ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
        spGraphicsLibType.setAdapter(adapterGraphicsLibType);
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
        spGraphicsLibType.setSelection(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));
    }

    void SaveConfig() {
        myBT.getAppConf().setApplicationLanguage(""+spAppLanguage.getSelectedItemId()+"");
        myBT.getAppConf().setUnits(""+spAppUnit.getSelectedItemId()+"");
        myBT.getAppConf().setGraphColor(""+spGraphColor.getSelectedItemId()+"");
        myBT.getAppConf().setGraphBackColor(""+spGraphBackColor.getSelectedItemId()+"");
        myBT.getAppConf().setFontSize(""+(spFontSize.getSelectedItemId()+8)+"");
        myBT.getAppConf().setBaudRate(""+spBaudRate.getSelectedItemId()+"");
        myBT.getAppConf().setConnectionType(""+spConnectionType.getSelectedItemId()+"");
        myBT.getAppConf().setGraphicsLibType(""+ spGraphicsLibType.getSelectedItemId()+"");
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
        spGraphicsLibType.setSelection(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));
    }

}
