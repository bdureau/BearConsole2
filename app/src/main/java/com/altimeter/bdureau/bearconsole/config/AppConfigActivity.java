package com.altimeter.bdureau.bearconsole.config;
/**
 *   @description: In this activity you should be able to choose the application languages and looks and feel.
 *   Still a lot to do but it is a good start
 *
 *   @author: boris.dureau@neuf.fr
 **/
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;

import java.util.ArrayList;
import java.util.List;


public class AppConfigActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    private Tab1Fragment appConfigPage1 = null;
    private Tab2Fragment appConfigPage2 = null;


    private static AppConfigData appConfigData =null;

    ConsoleApplication myBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();

        myBT.getAppConf().ReadConfig();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        // get the data for all the drop down
        appConfigData = new AppConfigData(this);
        setContentView(R.layout.activity_app_config);

        mViewPager = (ViewPager) findViewById(R.id.container_config);
        setupViewPager(mViewPager);

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

    }

    private void SaveConfig(){
        myBT.getAppConf().setApplicationLanguage(""+appConfigPage1.getAppLanguage()+"");
        myBT.getAppConf().setGraphColor(""+appConfigPage1.getGraphColor()+"");
        myBT.getAppConf().setUnits(""+appConfigPage1.getAppUnit()+"");
        myBT.getAppConf().setGraphBackColor(""+appConfigPage1.getGraphBackColor()+"");
        myBT.getAppConf().setFontSize(""+appConfigPage1.getFontSize()+"");
        myBT.getAppConf().setBaudRate(""+appConfigPage1.getBaudRate()+"");
        myBT.getAppConf().setConnectionType(""+appConfigPage1.getConnectionType()+"");
        myBT.getAppConf().setGraphicsLibType(""+appConfigPage1.getGraphicsLibType()+"");
        myBT.getAppConf().setAllowMultipleDrogueMain(appConfigPage1.getAllowMainDrogue());
        myBT.getAppConf().setFullUSBSupport(appConfigPage1.getFullUSBSupport());
        myBT.getAppConf().SaveConfig();
        finish();
    }

    private void RestoreToDefault(){
        myBT.getAppConf().ResetDefaultConfig();
        appConfigPage1.setAppLanguage(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        appConfigPage1.setAppUnit(Integer.parseInt(myBT.getAppConf().getUnits()));
        appConfigPage1.setGraphColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        appConfigPage1.setGraphBackColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        appConfigPage1.setFontSize(Integer.parseInt(myBT.getAppConf().getFontSize())-8);
        appConfigPage1.setBaudRate(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        appConfigPage1.setConnectionType(Integer.parseInt(myBT.getAppConf().getConnectionType()));
        appConfigPage1.setGraphicsLibType(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));

        if (myBT.getAppConf().getAllowMultipleDrogueMain().equals("true") ) {
            appConfigPage1.setAllowMainDrogue(true);
        } else {
            appConfigPage1.setAllowMainDrogue(false);
        }
        if (myBT.getAppConf().getFullUSBSupport().equals("true") ) {
            appConfigPage1.setFullUSBSupport(true);
        } else {
            appConfigPage1.setFullUSBSupport(false);
        }
    }


    private void setupViewPager(ViewPager viewPager) {
        adapter = new AppConfigActivity.SectionsPageAdapter(getSupportFragmentManager());
        appConfigPage1 = new AppConfigActivity.Tab1Fragment(myBT);
        appConfigPage2 = new AppConfigActivity.Tab2Fragment();

        adapter.addFragment(appConfigPage1, "TAB1");
        adapter.addFragment(appConfigPage2, "TAB2");

        viewPager.setAdapter(adapter);
    }

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1Fragment extends Fragment {
        private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
        private Spinner spConnectionType, spGraphicsLibType;
        private CheckBox cbAllowMainDrogue, cbFullUSBSupport;
        private ConsoleApplication BT;

        public Tab1Fragment(ConsoleApplication lBT) {
            BT = lBT;
        }
        public int getAppLanguage() {
            return (int) this.spAppLanguage.getSelectedItemId();
        }
        public void setAppLanguage(int value) {
             this.spAppLanguage.setSelection(value);
        }

        public int getGraphColor() {
            return (int) this.spGraphColor.getSelectedItemId();
        }
        public void setGraphColor(int value) {
            this.spGraphColor.setSelection(value);
        }

        public int getAppUnit() {
            return (int) this.spAppUnit.getSelectedItemId();
        }
        public void setAppUnit(int value) {
            this.spAppUnit.setSelection(value);
        }

        public int getGraphBackColor() {
            return (int) this.spGraphBackColor.getSelectedItemId();
        }
        public void setGraphBackColor(int value) {
            this.spGraphBackColor.setSelection(value);
        }

        public int getFontSize() {
            return (int) this.spFontSize.getSelectedItemId();
        }
        public void setFontSize(int value) {
            this.spFontSize.setSelection(value);
        }

        public int getBaudRate() {
            return (int) this.spBaudRate.getSelectedItemId();
        }
        public void setBaudRate(int value) {
            this.spBaudRate.setSelection(value);
        }

        public int getConnectionType() {
            return (int) this.spConnectionType.getSelectedItemId();
        }
        public void setConnectionType(int value) {
            this.spConnectionType.setSelection(value);
        }

        public int getGraphicsLibType() {
            return (int) this.spGraphicsLibType.getSelectedItemId();
        }
        public void setGraphicsLibType(int value) {
            this.spGraphicsLibType.setSelection(value);
        }

        public String getAllowMainDrogue() {
            if(cbAllowMainDrogue.isChecked())
                return "true";
            else
                return "false";
        }
        public void setAllowMainDrogue(boolean value){
                cbAllowMainDrogue.setChecked(value);
        }

        public String getFullUSBSupport() {
            if(cbFullUSBSupport.isChecked())
                return "true";
            else
                return "false";
        }
        public void setFullUSBSupport(boolean value){
            cbFullUSBSupport.setChecked(value);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.activity_app_config_part1, container, false);
            //Language
            spAppLanguage = (Spinner)view.findViewById(R.id.spinnerLanguage);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
            spAppLanguage.setAdapter(adapter);

            // graph color
            spGraphColor = (Spinner)view.findViewById(R.id.spinnerGraphColor);
            // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};

            ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
            spGraphColor.setAdapter(adapterColor);
            // graph back color
            spGraphBackColor = (Spinner)view.findViewById(R.id.spinnerGraphBackColor);
            ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

            spGraphBackColor.setAdapter(adapterGraphColor);
            //units
            spAppUnit = (Spinner)view.findViewById(R.id.spinnerUnits);

            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
            spAppUnit.setAdapter(adapter2);

            //font size
            spFontSize = (Spinner)view.findViewById(R.id.spinnerFontSize);

            ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
            spFontSize.setAdapter(adapterFontSize);

            //Baud Rate
            spBaudRate = (Spinner)view.findViewById(R.id.spinnerBaudRate);

            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
            spBaudRate.setAdapter(adapterBaudRate);

            //connection type
            spConnectionType = (Spinner)view.findViewById(R.id.spinnerConnectionType);

            ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
            spConnectionType.setAdapter(adapterConnectionType);

            //Graphics lib type
            spGraphicsLibType = (Spinner)view.findViewById(R.id.spinnerGraphicLibType);
            ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
            spGraphicsLibType.setAdapter(adapterGraphicsLibType);

            //Allow multiple main and drogue
            cbAllowMainDrogue = (CheckBox) view.findViewById(R.id.checkBoxAllowMultipleMainDrogue);

            //Allow only telemetry via USB
            cbFullUSBSupport = (CheckBox) view.findViewById(R.id.checkBoxFullUSBSupport);

            spAppLanguage.setSelection(Integer.parseInt(BT.getAppConf().getApplicationLanguage()));
            spAppUnit.setSelection(Integer.parseInt(BT.getAppConf().getUnits()));
            spGraphColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphColor()));
            spGraphBackColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphBackColor()));
            spFontSize.setSelection((Integer.parseInt(BT.getAppConf().getFontSize())-8));
            spBaudRate.setSelection(Integer.parseInt(BT.getAppConf().getBaudRate()));
            spConnectionType.setSelection(Integer.parseInt(BT.getAppConf().getConnectionType()));
            spGraphicsLibType.setSelection(Integer.parseInt(BT.getAppConf().getGraphicsLibType()));
            if (BT.getAppConf().getAllowMultipleDrogueMain().equals("true") ) {
                cbAllowMainDrogue.setChecked(true);
            } else {
                cbAllowMainDrogue.setChecked(false);
            }
            if (BT.getAppConf().getFullUSBSupport().equals("true") ) {
                cbFullUSBSupport.setChecked(true);
            } else {
                cbFullUSBSupport.setChecked(false);
            }
            return view;
        }

    }
    public static class Tab2Fragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.activity_app_config_part2, container, false);
            return view;
        }
    }
}
