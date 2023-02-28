package com.altimeter.bdureau.bearconsole.Flight.FlightView;
/**
 * @description: This will display altimeter flight summary
 * @author: boris.dureau@neuf.fr
 **/
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.LocationUtils;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.google.android.gms.maps.model.LatLng;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FlightViewInfoFragment extends Fragment {

    private FlightData myflight;
    private XYSeriesCollection allFlightData;
    private TextView nbrOfSamplesValue, flightNbrValue;
    private TextView apogeeAltitudeValue, flightDurationValue, burnTimeValue, maxVelociyValue, maxAccelerationValue;
    private TextView timeToApogeeValue, mainAltitudeValue, maxDescentValue, landingSpeedValue, flightDistanceValue;
    private double FEET_IN_METER = 1;

    private AlertDialog.Builder builder = null;
    private AlertDialog alert;

    private String SavedCurves = "";
    private boolean SavedCurvesOK = false;
    private Button buttonExportToCsv, butExportAndShare;
    private String units[] = null;
    private String FlightName = null;
    private ConsoleApplication myBT;
    private XYSeries altitude;
    private XYSeries speed;
    private XYSeries accel;
    private int numberOfCurves = 0;

    int nbrSeries;

    public FlightViewInfoFragment(FlightData data,
                                  XYSeriesCollection data2,
                                  ConsoleApplication BT,
                                  String pUnits[],
                                  String pFlightName) {
        myflight = data;
        this.allFlightData = data2;
        this.units = pUnits;
        this.FlightName = pFlightName;
        this.myBT = BT;
    }

    public void msg(String s) {
        Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_flight_info, container, false);

        buttonExportToCsv = (Button) view.findViewById(R.id.butExportToCsv);
        butExportAndShare = (Button) view.findViewById(R.id.butExportAndShare);
        apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
        flightDurationValue = view.findViewById(R.id.flightDurationValue);
        burnTimeValue = view.findViewById(R.id.burnTimeValue);
        maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
        maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);
        timeToApogeeValue = view.findViewById(R.id.timeToApogeeValue);
        mainAltitudeValue = view.findViewById(R.id.mainAltitudeValue);
        maxDescentValue = view.findViewById(R.id.maxDescentValue);
        landingSpeedValue = view.findViewById(R.id.landingSpeedValue);
        nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
        flightNbrValue = view.findViewById(R.id.flightNbrValue);
        flightDistanceValue = view.findViewById(R.id.flightDistanceValue);
        if (!myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            flightDistanceValue.setVisibility(View.INVISIBLE);
        }
        XYSeriesCollection flightData;

        flightData = myflight.GetFlightData(FlightName);
        int nbrData = flightData.getSeries(0).getItemCount();
        nbrSeries = flightData.getSeriesCount();

        speed = allFlightData.getSeries(getResources().getString(R.string.curve_speed));
        accel = allFlightData.getSeries(getResources().getString(R.string.curve_accel));
        altitude = allFlightData.getSeries(getResources().getString(R.string.curve_altitude));

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            numberOfCurves = 8;
        }
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiSTM32") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiESP32")) {
            numberOfCurves = 6;
        }
        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiServo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiDuo") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMultiV2") ||
                myBT.getAltiConfigData().getAltimeterName().equals("AltiMulti")) {
            numberOfCurves = 5;
        }

        if (myBT.getAppConf().getUnits()== GlobalConfig.AltitudeUnit.METERS) {//meters
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }

        // flight nbr
        flightNbrValue.setText(FlightName + "");

        //nbr of samples
        nbrOfSamplesValue.setText(nbrData + "");

        //flight duration
        double flightDuration = flightData.getSeries(0).getMaxX() / 1000;
        if(flightDuration < 60.0)
            flightDurationValue.setText(String.format("%.2f", flightDuration) + " secs");
        else
            flightDurationValue.setText(String.format("%.2f", flightDuration/60) + " minutes");
        //apogee altitude
        double apogeeAltitude = flightData.getSeries(0).getMaxY();
        apogeeAltitudeValue.setText(String.format("%.0f", apogeeAltitude) + " " + myBT.getAppConf().getUnitsValue());

        //apogee time
        int pos = searchX(flightData.getSeries(0), apogeeAltitude);
        Log.d("apogeetime", "pos:"+pos);
        double apogeeTime = 0;
        if (pos !=-1) {
            apogeeTime = (double) flightData.getSeries(0).getX(pos);
            timeToApogeeValue.setText(String.format("%.2f", apogeeTime / 1000) + " secs");
        }
        //calculate max speed
        double maxSpeed = speed.getMaxY();
        //speed.getMaxY()
        Log.d("maxSpeed", "maxSpeed:"+maxSpeed);
        maxVelociyValue.setText((long) maxSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");

        //landing speed
        double landingSpeed = 0;
        int timeBeforeLanding =searchXBack(altitude, 30);
        if (timeBeforeLanding != -1)
            if (searchY(speed, altitude.getX(timeBeforeLanding).doubleValue() )!= -1) {
                int posLanding =searchY(speed, altitude.getX(timeBeforeLanding).doubleValue() );
                landingSpeed = speed.getY(posLanding).doubleValue();
                Log.d("landingSpeed", "landingSpeed:"+landingSpeed);
                landingSpeedValue.setText((long) landingSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                landingSpeedValue.setText("N/A");
            }

        //max descente speed
        double maxDescentSpeed = 0;

        if (searchY(speed, apogeeTime + 100) != -1) {
            int pos1 = searchY(speed, apogeeTime + 100);
            XYSeries partialSpeed;
            partialSpeed = new XYSeries("partial_speed");
            for(int i = pos1; i < speed.getItemCount(); i++ ) {
                partialSpeed.add(speed.getX(i), speed.getY(i));
            }
            maxDescentSpeed = partialSpeed.getMaxY();
            //maxDescentSpeed = speed.getY(searchY(speed, apogeeTime + 500)).doubleValue();
            maxDescentValue.setText((long) maxDescentSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
        } else {
            maxDescentValue.setText("N/A");
        }

        //max acceleration value
        double maxAccel = accel.getMaxY();
        maxAccel = (maxAccel * FEET_IN_METER) / 9.80665;

        maxAccelerationValue.setText((long) maxAccel + " G");

        //burntime value
        double burnTime = 0;
        if (searchX(speed, maxSpeed) != -1)
            burnTime = speed.getX(searchX(speed, maxSpeed)).doubleValue();
        if (burnTime != 0)
            burnTimeValue.setText(String.format("%.2f", burnTime / 1000) + " secs");
        else
            burnTimeValue.setText("N/A");
        //main value
        // remain TODO!!!
        mainAltitudeValue.setText(" " + myBT.getAppConf().getUnitsValue());

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            // flight distance
            GeoPoint coord[];
            coord = new GeoPoint[flightData.getSeries(6).getItemCount()];

            for (int i = 0; i < flightData.getSeries(6).getItemCount(); i++) {
                double res = (double) flightData.getSeries(6).getY(i);

                if ((double)flightData.getSeries(6).getY(i) > 0.0d & (double)flightData.getSeries(7).getY(i) > 0.0d) {
                    GeoPoint c = new GeoPoint((double) flightData.getSeries(6).getY(i), (double) flightData.getSeries(7).getY(i));
                    coord[i] = c;
                }
            }

            flightDistanceValue.setText(String.format("%.2f", LocationUtils.distanceBetweenCoordinates(coord)) + " " + myBT.getAppConf().getUnitsValue());

        }

        //butExportAndShare
        butExportAndShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavedCurves = "";
                SavedCurvesOK = true;

                // Create a file for the zip file
                File zipFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "flightData.zip");
                try {
                    // Create a zip output stream to write to the zip file
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    //export the data to a csv file
                    for (int j = 0; j < numberOfCurves; j++) {
                        Log.d("Flight win", "Saving curve:" + j);
                        String fileName =saveData(j, allFlightData);
                        // Create a new zip entry with the file's name
                        ZipEntry ze = new ZipEntry(fileName);
                        // Add the zip entry to the zip output stream
                        zos.putNextEntry(ze);
                        // Read the file and write it to the zip output stream
                        File filetoZip = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BearConsoleFlights/"+ fileName);
                        FileInputStream fis = new FileInputStream(filetoZip);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        // Close the zip entry and the file input stream
                        zos.closeEntry();
                        fis.close();
                    }

                    // Close the zip output stream
                    zos.close();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                shareFile(zipFile);
            }
        });

        buttonExportToCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavedCurves = "";
                SavedCurvesOK = true;
                //export the data to a csv file
                for (int j = 0; j < numberOfCurves; j++) {
                    Log.d("Flight win", "Saving curve:" + j);
                    saveData(j, allFlightData);
                }
                builder = new AlertDialog.Builder(view.getContext());
                //Running Saving commands
                //getResources().getString(R.string.flight_time)
                if (SavedCurvesOK) {
                    builder.setMessage(getResources().getString(R.string.save_curve_msg) + Environment.DIRECTORY_DOWNLOADS + "\\BearConsoleFlights \n" + SavedCurves)
                            .setTitle(getResources().getString(R.string.save_curves_title))
                            .setCancelable(false)
                            .setPositiveButton(R.string.save_curve_ok, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                }
                            });

                    alert = builder.create();
                    alert.show();
                    msg(getResources().getString(R.string.curves_saved_msg));
                } else {
                    msg("Failed saving flights");
                }
            }
        });

        return view;
    }

    private String saveData(int nbr, XYSeriesCollection Data) {
        String fileName ="";
        String valHeader = "altitude";

        if (nbr == 0) {
            valHeader = getResources().getString(R.string.curve_altitude);
        } else if (nbr == 1) {
            valHeader = getResources().getString(R.string.curve_temperature);
        } else if (nbr == 2) {
            valHeader = getResources().getString(R.string.curve_pressure);
        } else if (nbr == 3) {
            valHeader = getResources().getString(R.string.curve_speed);
        } else if (nbr == 4) {
            valHeader = getResources().getString(R.string.curve_accel);
        } else if (nbr == 5) {
            valHeader = "voltage";
        } else if (nbr == 6) {
            valHeader = getResources().getString(R.string.curve_latitude);
        } else if (nbr == 7) {
            valHeader = getResources().getString(R.string.curve_longitude);
        }


        String csv_data = "time(ms)," + valHeader + " " + units[nbr] + "\n";/// your csv data as string;

        int nbrData = Data.getSeries(nbr).getItemCount();
        for (int i = 0; i < nbrData; i++) {
            csv_data = csv_data + (double) Data.getSeries(nbr).getX(i) + "," + (double) Data.getSeries(nbr).getY(i) + "\n";
        }
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        //if you want to create a sub-dir
        root = new File(root, "BearConsoleFlights");
        root.mkdir();

        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        String date = sdf.format(System.currentTimeMillis());

        fileName = FlightName + "-" + Data.getSeries(nbr).getKey().toString() + date + ".csv";
        // select the name for your file
        root = new File(root, fileName);
        Log.d("Flight win", fileName);
        try {
            Log.d("Flight win", "attempt to write");
            FileOutputStream fout = new FileOutputStream(root);
            fout.write(csv_data.getBytes());
            fout.close();
            Log.d("Flight win", "write done");
            SavedCurves = SavedCurves + fileName + "\n";

        } catch (FileNotFoundException e) {
            e.printStackTrace();

            boolean bool = false;
            try {
                // try to create the file
                bool = root.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (bool) {
                // call the method again
                saveData(nbr, Data);
            } else {
                Log.d("Flight win", "Failed to create flight files");
                //msg("failed");
                SavedCurvesOK = false;
                //throw new IllegalStateException(getString(R.string.failed_create_flight_file_msg));// DOES NOT WORK !!
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    //Share file
    private void shareFile(File file) {

        Uri uri = FileProvider.getUriForFile(
                this.getContext(),
                this.getContext().getPackageName() +  ".provider",
                file);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("file/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.bearconsole_has_shared2));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, getString(R.string.share_with5)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), getString(R.string.no_app_available), Toast.LENGTH_SHORT).show();
        }
    }
    /*
    Return the position of the first X value it finds from the beginning
     */
    public int searchX(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            if ((searchVal >= serie.getY(i - 1).doubleValue()) && (searchVal <= serie.getY(i).doubleValue())) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    /*
    Return the position of the first X value it finds from the beginning
     */
    public int searchXBack(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            Log.d("I","i="+ i + " nbrData= "+ nbrData + "  Y = " + serie.getY((nbrData - i) - 1));
            if ((searchVal <= serie.getY((nbrData - i) - 1).doubleValue()) && (searchVal >= serie.getY(nbrData - i).doubleValue())) {
                pos = (nbrData - i);
                break;
            }
        }
        Log.d("I","pos = " + pos);
        return pos;
    }

    /*
    Return the position of the first Y value it finds from the beginning
     */
    public int searchY(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            if ((searchVal >= serie.getX(i - 1).doubleValue()) && (searchVal <= serie.getX(i).doubleValue())) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    /*
    Return the position of the first Y value it finds from the end
    */
    public int searchYBack(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            if ((searchVal >= serie.getX((nbrData -i)+1 ).doubleValue()) && (searchVal <= serie.getX(nbrData-i).doubleValue())) {
                pos = (nbrData -i);
                break;
            }
        }
        return pos;
    }
}

