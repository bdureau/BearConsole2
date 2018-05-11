package com.altimeter.bdureau.bearconsole;
/**
 *   @description: Retrieve altimeter configuration and show it in tabs
 *                  The user can then load it back to the altimeter
 *   @author: boris.dureau@neuf.fr
 **/
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AltimeterTabConfigActivity extends AppCompatActivity {
    private static final String TAG ="AltimeterTabConfigActivity";
    ///private SectionsPageAdapter mSectionsPageAdapter;
    private ViewPager mViewPager;
    Tab1Fragment configPage1 =null;
    Tab2Fragment configPage2 =null;
    Tab3Fragment configPage3 =null;
    private Button btnDismiss, btnUpload;
    ConsoleApplication myBT ;
    private static AltiConfigData AltiCfg = null;
    private ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_altimeter_tab_config);

        mViewPager =(ViewPager) findViewById(R.id.container);
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

        btnUpload = (Button)findViewById(R.id.butUpload);
        btnUpload.setOnClickListener(new View.OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    //send back the config to the altimeter and exit if successful
                    sendConfig();
                }
            });
        new RetrieveConfig().execute();
    }
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        configPage1 = new Tab1Fragment();
        configPage2 = new Tab2Fragment();
        configPage3 = new Tab3Fragment();
        adapter.addFragment(configPage1, "TAB1");
        adapter.addFragment(configPage2, "TAB2");
        adapter.addFragment(configPage3, "TAB3");
        viewPager.setAdapter(adapter);
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

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }
    private boolean sendConfig()
    {
        //final boolean exit_no_save = false;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        long prevBaudRate = AltiCfg.getConnectionSpeed();
        long nbrOfMain=0;
        long nbrOfDrogue=0;
        // check if the baud rate has changed

        if(configPage1.isViewCreated()) {
            AltiCfg.setOutput1Delay(configPage1.getOutDelay1());
            AltiCfg.setOutput2Delay(configPage1.getOutDelay2());
            AltiCfg.setOutput3Delay(configPage1.getOutDelay3());
            AltiCfg.setOutput4Delay(configPage1.getOutDelay4());
            AltiCfg.setOutput1(configPage1.getDropdownOut1());
            AltiCfg.setOutput2(configPage1.getDropdownOut2());
            AltiCfg.setOutput3(configPage1.getDropdownOut3());
            AltiCfg.setOutput4(configPage1.getDropdownOut4());
            AltiCfg.setMainAltitude(configPage1.getMainAltitude());
        }
        if(configPage2.isViewCreated()) {

           // AltiCfg.setConnectionSpeed(configPage2.getBaudRate());
            AltiCfg.setNbrOfMeasuresForApogee(configPage2.getApogeeMeasures());
            AltiCfg.setAltimeterResolution(configPage2.getAltimeterResolution());
            AltiCfg.setEepromSize(configPage2.getEEpromSize());
            AltiCfg.setBeepOnOff(configPage2.getBeepOnOff());
          // msg("ret" +configPage2.getRecordTempOnOff());
            AltiCfg.setEndRecordAltitude(configPage2.getEndRecordAltitude());
            AltiCfg.setRecordTemperature(configPage2.getRecordTempOnOff());
            AltiCfg.setSupersonicDelay(configPage2.getSupersonicDelayOnOff());
        }

        if(configPage3.isViewCreated()) {
            AltiCfg.setBeepingFrequency(configPage3.getFreq());
            AltiCfg.setBeepingMode(configPage3.getDropdownBipMode());
            AltiCfg.setUnits(configPage3.getDropdownUnits());
        }

        if (AltiCfg.getOutput1() == 0)
            nbrOfMain++;
        if (AltiCfg.getOutput2() == 0)
            nbrOfMain++;
        if (AltiCfg.getOutput3() == 0)
            nbrOfMain++;
        if (AltiCfg.getOutput4() == 0)
            nbrOfMain++;


        if (AltiCfg.getOutput1() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getOutput2() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getOutput3() == 1)
            nbrOfDrogue++;
        if (AltiCfg.getOutput4() == 1)
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

        if(configPage2.isViewCreated()) {
            if(AltiCfg.getConnectionSpeed() != configPage2.getBaudRate())
            {
                //final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //You are about to change the baud rate, are you sure you want to do it?
                builder.setMessage(getResources().getString(R.string.msg9))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            AltiCfg.setConnectionSpeed(configPage2.getBaudRate());
                            sendAltiCfg ();
                            finish();
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
            else
            {
                sendAltiCfg ();
                finish();
            }
        }
        else
        {
            sendAltiCfg ();
            finish();
        }

        return true;
    }

    private void sendAltiCfg () {
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
                AltiCfg.getEepromSize()+"," +
                AltiCfg.getBeepOnOff();
        if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32")) {
            altiCfgStr = altiCfgStr + "," + AltiCfg.getOutput4();
            altiCfgStr = altiCfgStr + "," + AltiCfg.getOutput4Delay();
        }

        altiCfgStr = altiCfgStr +  ";\n";
        // msg(altiCfgStr.toString());

        if(myBT.getConnected())
            //send back the config
            myBT.write(altiCfgStr.toString());

        msg(getResources().getString(R.string.msg3));//+altiCfgStr.toString());

        myBT.flush();

        //return true;
    }

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList= new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
        public SectionsPageAdapter (FragmentManager fm){
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
        private static final String TAG = "Tab1Fragment";


        private Spinner dropdownOut1, dropdownOut2, dropdownOut3, dropdownOut4;
        private EditText OutDelay1, OutDelay2,OutDelay3, OutDelay4;
        private EditText MainAltitude;
        private TextView txtOut4, txtViewDelay4 ;

        private boolean ViewCreated = false;
        public boolean isViewCreated() {
            return ViewCreated;
        }

        public int getDropdownOut1() {
            return (int)this.dropdownOut1.getSelectedItemId();
        }
        public void setDropdownOut1(int Out1) {
            this.dropdownOut1.setSelection(Out1);
        }
        public int getDropdownOut2() {
            return (int)this.dropdownOut2.getSelectedItemId();
        }
        public void setDropdownOut2(int Out2) {
            this.dropdownOut2.setSelection(Out2);
        }
        public int getDropdownOut3() {
            return (int)this.dropdownOut3.getSelectedItemId();
        }
        public void setDropdownOut3(int Out3) {
            this.dropdownOut3.setSelection(Out3);
        }
        public int getDropdownOut4() {
            if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32"))
                return (int)this.dropdownOut4.getSelectedItemId();
            else
                return -1;
        }
        public void setDropdownOut4(int Out4) {
            if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32"))  {
                this.dropdownOut4.setSelection(Out4);
                this.dropdownOut4.setVisibility(View.VISIBLE);
                txtOut4.setVisibility(View.VISIBLE);
            }
            else {
                this.dropdownOut4.setVisibility(View.INVISIBLE);
                txtOut4.setVisibility(View.INVISIBLE);
            }

        }
        public void setOutDelay1(int OutDelay1) {
            this.OutDelay1.setText(String.valueOf(OutDelay1));
        }
        public int getOutDelay1() {
            int ret;
            try {ret = Integer.parseInt(OutDelay1.getText().toString());}
            catch (Exception e) {
                ret =0;
            }
            return ret;
        }
        public void setOutDelay2(int OutDelay2) {
            this.OutDelay2.setText(String.valueOf(OutDelay2));
        }
        public int getOutDelay2() {
            int ret;
            try {ret = Integer.parseInt(OutDelay2.getText().toString());}
            catch (Exception e) {
                ret =0;
            }
            return ret;
        }
        public void setOutDelay3(int OutDelay3) {
            this.OutDelay3.setText(String.valueOf(OutDelay3));
        }
        public int getOutDelay3() {
            int ret;
            try {ret = Integer.parseInt(OutDelay3.getText().toString());}
            catch (Exception e) {
                ret =0;
            }
            return ret;
        }
        public void setOutDelay4(int OutDelay4) {
            if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32"))  {
                this.OutDelay4.setText(String.valueOf(OutDelay4));
                this.OutDelay4.setVisibility(View.VISIBLE);
                txtViewDelay4.setVisibility(View.VISIBLE);
            }
            else {
                this.OutDelay4.setVisibility(View.INVISIBLE);
                txtViewDelay4.setVisibility(View.INVISIBLE);
            }

        }
        public int getOutDelay4() {
            if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32")) {
                int ret;
                try {
                    ret = Integer.parseInt(OutDelay4.getText().toString());
                } catch (Exception e) {
                    ret = 0;
                }
                return ret;
            }
            else
                return -1;
        }

        public void setMainAltitude(int MainAltitude) {
            this.MainAltitude.setText(String.valueOf(MainAltitude));
        }
        public int getMainAltitude() {
            int ret;
            try {
                ret = Integer.parseInt(MainAltitude.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart1_fragment,container,false);

            //Output 1
            dropdownOut1 = (Spinner)view.findViewById(R.id.spinnerOut1);
            String[] items3 = new String[]{"Main", "Drogue", "Timer", "Disabled"};
            ArrayAdapter<String> adapter3 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut1.setAdapter(adapter3);

            //Output 2
            dropdownOut2 = (Spinner)view.findViewById(R.id.spinnerOut2);

            ArrayAdapter<String> adapter4 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut2.setAdapter(adapter4);

            //Output 3
            dropdownOut3 = (Spinner)view.findViewById(R.id.spinnerOut3);

            ArrayAdapter<String> adapter5 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut3.setAdapter(adapter5);

            //Output 4
            dropdownOut4 = (Spinner)view.findViewById(R.id.spinnerOut4);

            ArrayAdapter<String> adapter6 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items3);
            dropdownOut4.setAdapter(adapter6);

            OutDelay1 = (EditText)view.findViewById(R.id.editTxtDelay1);
            OutDelay2 = (EditText)view.findViewById(R.id.editTxtDelay2);
            OutDelay3 = (EditText)view.findViewById(R.id.editTxtDelay3);
            OutDelay4 = (EditText)view.findViewById(R.id.editTxtDelay4);

            MainAltitude= (EditText)view.findViewById(R.id.editTxtMainAltitude);

            txtOut4 = (TextView)view.findViewById(R.id.txtOut4);
            txtViewDelay4 = (TextView)view.findViewById(R.id.txtViewDelay4);
            if (AltiCfg !=null) {
                dropdownOut1.setSelection(AltiCfg.getOutput1());
                dropdownOut2.setSelection(AltiCfg.getOutput2());
                dropdownOut3.setSelection(AltiCfg.getOutput3());
                if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32"))  {
                    dropdownOut4.setSelection(AltiCfg.getOutput4());
                    dropdownOut4.setVisibility(View.VISIBLE);
                    txtOut4.setVisibility(View.VISIBLE);
                }
                else {
                    dropdownOut4.setVisibility(View.INVISIBLE);
                    txtOut4.setVisibility(View.INVISIBLE);
                }
                OutDelay1.setText(String.valueOf(AltiCfg.getOutput1Delay()));
                OutDelay2.setText(String.valueOf(AltiCfg.getOutput2Delay()));
                OutDelay3.setText(String.valueOf(AltiCfg.getOutput3Delay()));

                if(AltiCfg.getAltimeterName().equals("AltiMultiSTM32")) {
                    OutDelay4.setText(String.valueOf(AltiCfg.getOutput4Delay()));
                    OutDelay4.setVisibility(View.VISIBLE);
                    txtViewDelay4.setVisibility(View.VISIBLE);
                }
                else {
                    OutDelay4.setVisibility(View.INVISIBLE);
                    txtViewDelay4.setVisibility(View.INVISIBLE);
                }

                MainAltitude.setText(String.valueOf(AltiCfg.getMainAltitude()));
            }
            ViewCreated = true;
            return view;
        }
    }
    public static class Tab2Fragment extends Fragment {
        private static final String TAG = "Tab2Fragment";

        private String[] itemsBaudRate;
        private String[] itemsAltimeterResolution;
        private String[] itemsEEpromSize;
        private String[] itemsBeepOnOff;
        private String[] itemsRecordTempOnOff;
        private String[] itemsSupersonicDelayOnOff;

        private Spinner dropdownBeepOnOff;
        private Spinner dropdownBaudRate;
        private EditText ApogeeMeasures;
        private Spinner dropdownAltimeterResolution, dropdownEEpromSize;
        private Spinner dropdownRecordTemp;
        private Spinner dropdownSupersonicDelay;
        private EditText EndRecordAltitude;
        private boolean ViewCreated = false;

        public boolean isViewCreated() {
            return ViewCreated;
        }
        public int getApogeeMeasures() {
            int ret;
            try {
                ret = Integer.parseInt(this.ApogeeMeasures.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setApogeeMeasures(int apogeeMeasures) {
            ApogeeMeasures.setText( String.valueOf(apogeeMeasures));
        }

        public int getAltimeterResolution() {
            return (int)this.dropdownAltimeterResolution.getSelectedItemId();
        }
        public void setAltimeterResolution(int AltimeterResolution ) {
            this.dropdownAltimeterResolution.setSelection(AltimeterResolution);
        }
        public int getEEpromSize() {
            int ret;
            try {
                ret = Integer.parseInt(itemsEEpromSize[(int)dropdownEEpromSize.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setEEpromSize(int EEpromSize ) {
            this.dropdownEEpromSize.setSelection(AltiCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
        }
        public long getBaudRate() {
            int ret;
            try {
                ret = Integer.parseInt(itemsBaudRate[(int)this.dropdownBaudRate.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setBaudRate(long BaudRate ) {
            this.dropdownBaudRate.setSelection(AltiCfg.arrayIndex(itemsBaudRate,String.valueOf(BaudRate)));
        }

        public int getBeepOnOff() {
             return (int)this.dropdownBeepOnOff.getSelectedItemId();
        }
        public void setBeepOnOff(int BeepOnOff ) {
            dropdownBeepOnOff.setSelection(BeepOnOff);
        }

        public int getRecordTempOnOff() {
             return (int)this.dropdownRecordTemp.getSelectedItemId();
        }

        public void setRecordTempOnOff(int RecordTempOnOff ) {
            this.dropdownRecordTemp.setSelection(RecordTempOnOff);
        }

        public int getSupersonicDelayOnOff() {
            return (int)this.dropdownSupersonicDelay.getSelectedItemId();
        }

        public void setSupersonicDelayOnOff(int SupersonicDelayOnOff ) {
            this.dropdownSupersonicDelay.setSelection(SupersonicDelayOnOff);
        }

        public int getEndRecordAltitude() {
            int ret;
            try {
                ret = Integer.parseInt(this.EndRecordAltitude.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setEndRecordAltitude(int EndRecordAltitude ) {
            this.EndRecordAltitude.setText(String.valueOf(EndRecordAltitude));
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart2_fragment,container,false);

            //baud rate
            dropdownBaudRate = (Spinner)view.findViewById(R.id.spinnerBaudRate);
            itemsBaudRate = new String[]{ "300",
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
            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
            dropdownBaudRate.setAdapter(adapterBaudRate);

            // nbr of measures to determine apogee
            ApogeeMeasures = (EditText)view.findViewById(R.id.editTxtApogeeMeasures);

            // altimeter resolution
            dropdownAltimeterResolution = (Spinner)view.findViewById(R.id.spinnerAltimeterResolution);
            itemsAltimeterResolution = new String[]{"ULTRALOWPOWER", "STANDARD","HIGHRES","ULTRAHIGHRES"};
            ArrayAdapter<String> adapterAltimeterResolution= new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsAltimeterResolution);
            dropdownAltimeterResolution.setAdapter(adapterAltimeterResolution);

            //Altimeter external eeprom size
            dropdownEEpromSize = (Spinner)view.findViewById(R.id.spinnerEEpromSize);
            itemsEEpromSize = new String[]{"32", "64","128","256","512", "1024"};
            ArrayAdapter<String> adapterEEpromSize= new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
            dropdownEEpromSize.setAdapter(adapterEEpromSize);

            // Switch beeping on or off
            // 0 is On and 1 is Off
            dropdownBeepOnOff= (Spinner)view.findViewById(R.id.spinnerBeepOnOff);
            itemsBeepOnOff= new String[]{"On","Off"};
            ArrayAdapter<String> adapterBeepOnOff = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBeepOnOff);
            dropdownBeepOnOff.setAdapter(adapterBeepOnOff);

            // Record temperature on or off
            dropdownRecordTemp = (Spinner)view.findViewById(R.id.spinnerRecordTemp);
            itemsRecordTempOnOff= new String[]{"Off","On"};
            ArrayAdapter<String> adapterRecordTemp = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsRecordTempOnOff);
            dropdownRecordTemp.setAdapter(adapterRecordTemp);

            // supersonic delay on/off
            dropdownSupersonicDelay = (Spinner)view.findViewById(R.id.spinnerSupersonicDelay);
            itemsSupersonicDelayOnOff= new String[]{"Off","On"};
            ArrayAdapter<String> adapterSupersonicDelayOnOff = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsSupersonicDelayOnOff);
            dropdownSupersonicDelay.setAdapter(adapterSupersonicDelayOnOff);

            // nbr of meters to stop recording altitude
            EndRecordAltitude = (EditText)view.findViewById(R.id.editTxtEndRecordAltitude);

            if (AltiCfg != null) {
                dropdownBaudRate.setSelection(AltiCfg.arrayIndex(itemsBaudRate,String.valueOf(AltiCfg.getConnectionSpeed())));
                ApogeeMeasures.setText(String.valueOf(AltiCfg.getNbrOfMeasuresForApogee()));
                dropdownAltimeterResolution.setSelection(AltiCfg.getAltimeterResolution());
                dropdownEEpromSize.setSelection(AltiCfg.arrayIndex(itemsEEpromSize, String.valueOf(AltiCfg.getEepromSize())));
                dropdownBeepOnOff.setSelection(AltiCfg.getBeepOnOff());
                dropdownRecordTemp.setSelection(AltiCfg.getRecordTemperature());
                dropdownSupersonicDelay.setSelection(AltiCfg.getSupersonicDelay());
                EndRecordAltitude.setText(String.valueOf(AltiCfg.getEndRecordAltitude()));
            }
            ViewCreated = true;
            return view;
        }
    }
    public static class Tab3Fragment extends Fragment{
        private static final String TAG = "Tab3Fragment";

        private TextView altiName;
        private Spinner dropdownUnits;
        private Spinner dropdownBipMode;
        private EditText Freq;

        private boolean ViewCreated = false;
        public boolean isViewCreated() {
            return ViewCreated;
        }

        public int getFreq() {
            int ret;
            try {
                ret = Integer.parseInt(this.Freq.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;

        }
        public void setFreq(int freq) {
            Freq.setText(freq);
        }
        public void setAltiName(String altiName) {
            this.altiName.setText(altiName);
        }
        public String getAltiName() {
            return (String) this.altiName.getText();
        }

        public int getDropdownUnits() {
            return (int)this.dropdownUnits.getSelectedItemId();
        }

        public void setDropdownUnits(int Units) {
            this.dropdownUnits.setSelection(Units);
        }

        public int getDropdownBipMode() {
            return (int)this.dropdownBipMode.getSelectedItemId();
        }

        public void setDropdownBipMode(int BipMode) {
            this.dropdownBipMode.setSelection(BipMode);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate (R.layout.tabconfigpart3_fragment, container,false);
            //Beep mode
            dropdownBipMode = (Spinner)view.findViewById(R.id.spinnerBipMode);
            String[] items = new String[]{"Mode1", "Mode2"};
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items);
            dropdownBipMode.setAdapter(adapter);


            //units
            dropdownUnits = (Spinner)view.findViewById(R.id.spinnerUnit);
            String[] items2 = new String[]{"Meters", "Feet"};
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items2);
            dropdownUnits.setAdapter(adapter2);

            //Altimeter name
            altiName = (TextView)view.findViewById(R.id.txtAltiNameValue);
            //here you can set the beep frequency
            Freq=(EditText)view.findViewById(R.id.editTxtBipFreq);

            if (AltiCfg != null) {
                altiName.setText(AltiCfg.getAltimeterName()+ " ver: " +
                        AltiCfg.getAltiMajorVersion()+"."+AltiCfg.getAltiMinorVersion());

                dropdownBipMode.setSelection(AltiCfg.getBeepingMode());
                dropdownUnits.setSelection(AltiCfg.getUnits());
                Freq.setText(String.valueOf(AltiCfg.getBeepingFrequency()));
            }
            ViewCreated = true;
            return view;
        }

    }
    private class RetrieveConfig extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute()
        {
            //"Retrieving Altimeter config...", "Please wait!!!"
            progress = ProgressDialog.show(AltimeterTabConfigActivity.this,
                    getResources().getString(R.string.msg5),
                    getResources().getString(R.string.msg6));  //show a progress dialog
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

            if(AltiCfg!=null) {
                //Config Tab 1
                if(configPage1.isViewCreated()) {
                    configPage1.setDropdownOut1(AltiCfg.getOutput1());
                    configPage1.setDropdownOut2(AltiCfg.getOutput2());
                    configPage1.setDropdownOut3(AltiCfg.getOutput3());
                    configPage1.setOutDelay1(AltiCfg.getOutput1Delay());
                    configPage1.setOutDelay2(AltiCfg.getOutput2Delay());
                    configPage1.setOutDelay3(AltiCfg.getOutput3Delay());
                    configPage1.setMainAltitude(AltiCfg.getMainAltitude());
                    configPage1.setDropdownOut4(AltiCfg.getOutput4());
                    configPage1.setOutDelay4(AltiCfg.getOutput4Delay());
                }
                //Config Tab 2
                if(configPage2.isViewCreated()) {
                    configPage2.setBaudRate(AltiCfg.getConnectionSpeed());
                    configPage2.setApogeeMeasures(AltiCfg.getNbrOfMeasuresForApogee());
                    configPage2.setAltimeterResolution(AltiCfg.getAltimeterResolution());
                    configPage2.setEEpromSize(AltiCfg.getEepromSize());
                    configPage2.setBeepOnOff(AltiCfg.getBeepOnOff());
                    configPage2.setRecordTempOnOff(AltiCfg.getRecordTemperature());
                    configPage2.setSupersonicDelayOnOff(AltiCfg.getSupersonicDelay());
                    configPage2.setEndRecordAltitude(AltiCfg.getEndRecordAltitude());
                }

                if(configPage3.isViewCreated()) {
                    configPage3.setAltiName(AltiCfg.getAltimeterName()+ " ver: " +
                            AltiCfg.getAltiMajorVersion()+"."+AltiCfg.getAltiMinorVersion());
                    configPage3.setDropdownBipMode(AltiCfg.getBeepingMode());
                    configPage3.setDropdownUnits(AltiCfg.getUnits());
                    configPage3.setFreq(AltiCfg.getBeepingFrequency());
                }
            }
            progress.dismiss();
        }
    }


}
