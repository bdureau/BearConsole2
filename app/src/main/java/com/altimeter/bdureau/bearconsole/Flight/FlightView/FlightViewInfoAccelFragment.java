package com.altimeter.bdureau.bearconsole.Flight.FlightView;
/**
 * @description: This will display altimeter information such as current altitude, battery voltage etc...
 * @author: boris.dureau@neuf.fr
 **/

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.R;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

public class FlightViewInfoAccelFragment extends Fragment {
    private static final String TAG = "Tab2StatusFragment";
    private boolean ViewCreated = false;
    private FlightData myflight;
    private XYSeriesCollection allFlightData;
    private TextView txtViewAccel375x, txtViewAccel375y, txtViewAccel375z,
            txtViewAccel345x, txtViewAccel345y, txtViewAccel345z;
    //ConsoleApplication lBT;

    private XYSeries accel375x, accel375y, accel375z, accel345x, accel345y, accel345z;

    /*public FlightViewInfoAccelFragment(ConsoleApplication bt) {
        lBT = bt;
    }*/
    private String units[] = null;
    private String FlightName = null;
    private ConsoleApplication myBT;
    public FlightViewInfoAccelFragment(FlightData data,
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
    public void setAccel375x(String value) {
        if (ViewCreated)
            this.txtViewAccel375x.setText(value);
    }

    public void setAccel375y(String value) {
        if (ViewCreated)
            this.txtViewAccel375y.setText(value);
    }

    public void setAccel375z(String value) {
        if (ViewCreated)
            this.txtViewAccel375z.setText(value);
    }

    public void setAccel345x(String value) {
        if (ViewCreated)
            this.txtViewAccel345x.setText(value);
    }

    public void setAccel345y(String value) {
        if (ViewCreated)
            this.txtViewAccel345y.setText(value);
    }

    public void setAccel345z(String value) {
        if (ViewCreated)
            this.txtViewAccel345z.setText(value);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_telemetry_mp_tab2, container, false);
        txtViewAccel375x = (TextView) view.findViewById(R.id.txtViewAccel375xValue);
        txtViewAccel375y = (TextView) view.findViewById(R.id.txtViewAccel375yValue);
        txtViewAccel375z = (TextView) view.findViewById(R.id.txtViewAccel375zValue);
        txtViewAccel345x = (TextView) view.findViewById(R.id.txtViewAccel345xValue);
        txtViewAccel345y = (TextView) view.findViewById(R.id.txtViewAccel345yValue);
        txtViewAccel345z = (TextView) view.findViewById(R.id.txtViewAccel345zValue);

        accel375x = allFlightData.getSeries("accel375x");
        accel375y = allFlightData.getSeries("accel375y");
        accel375z = allFlightData.getSeries("accel375z");

        accel345x = allFlightData.getSeries("accel345x");
        accel345y = allFlightData.getSeries("accel345y");
        accel345z = allFlightData.getSeries("accel345z");

        this.txtViewAccel375x.setText(String.format("%.2f",accel375x.getMaxY()/9.80665) + " G");
        this.txtViewAccel375y.setText(String.format("%.2f",accel375y.getMaxY()/9.80665) + " G");
        this.txtViewAccel375z.setText(String.format("%.2f",accel375z.getMaxY()/9.80665) + " G");
        this.txtViewAccel345x.setText(String.format("%.2f",accel345x.getMaxY()/9.80665) + " G");
        this.txtViewAccel345y.setText(String.format("%.2f",accel345y.getMaxY()/9.80665) + " G");
        this.txtViewAccel345z.setText(String.format("%.2f",accel345z.getMaxY()/9.80665) + " G");
        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ViewCreated = false;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

}