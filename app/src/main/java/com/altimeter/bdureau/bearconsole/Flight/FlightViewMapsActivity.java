package com.altimeter.bdureau.bearconsole.Flight;

/**
 *   @description: This class display the latitude and longitude on a google map. This is used when
 *   retrieving data for the AltiGPS
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.altimeter.bdureau.bearconsole.R;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;

public class FlightViewMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String FlightName = null;
    ConsoleApplication myBT ;
    private FlightData myflight=null;
    public String TAG = "MAP_ACTIVITY";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*if(AppCompatDelegate.getDefaultNightMode()== AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.AppTheme);
        }*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_view_maps);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();

        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);

        myflight= myBT.getFlightData();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        XYSeriesCollection flightData;
        flightData = myflight.GetFlightData(FlightName);

        Log.d(TAG, "nbr of item:" +flightData.getSeries(3).getItemCount());
        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(false));

        List<LatLng> coord;
        coord =  new ArrayList();
        int j=0;
        for (int i=0; i< flightData.getSeries(3).getItemCount(); i++)   {

            double res = (double)flightData.getSeries(3).getY(i);

            if(res >0.0d) {

                LatLng c = new LatLng((double) flightData.getSeries(3).getY(i) / 100000, (double) flightData.getSeries(4).getY(i) / 100000);

                coord.add(j, c);
                j++;

           }
        }

        polyline1.setPoints(coord);


        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((double)flightData.getSeries(3).getMaxY()/(double)100000,(double)flightData.getSeries(4).getMaxY()/(double)100000), 20));

        if(coord.size() > 0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord.get((int)(j/2)),15));
    }
}
