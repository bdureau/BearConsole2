package com.altimeter.bdureau.bearconsole.Flight;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
//import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;


import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.altimeter.bdureau.bearconsole.R;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;

public class FlightViewMapsActivity extends FragmentActivity implements OnMapReadyCallback {
//public class FlightViewMapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    String FlightName = null;
    ConsoleApplication myBT ;
    private FlightData myflight=null;
    public String TAG = "MAP_ACTIVITY";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        XYSeriesCollection flightData;
        flightData = myflight.GetFlightData(FlightName);
        //flightData.getSeries()
        // Add a marker in Sydney and move the camera
       /* LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.684, 133.903), 4));
*/
        Log.d(TAG, "nbr of item:" +flightData.getSeries(3).getItemCount());
        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(false));
                /*.add(
                        new LatLng(-35.016, 143.321),
                        new LatLng(-34.747, 145.592),
                        new LatLng(-34.364, 147.891),
                        new LatLng(-33.501, 150.217),
                        new LatLng(-32.306, 149.248),
                        new LatLng(-32.491, 147.309)));*/
        List<LatLng> coord;
        coord =  new ArrayList();
        int j=0;
        for (int i=0; i< flightData.getSeries(3).getItemCount(); i++)   {

            double res = (double)flightData.getSeries(3).getY(i);
            Log.d(TAG, "Res: " +res);
            if(res >0.0d) {

                LatLng c = new LatLng((double) flightData.getSeries(3).getY(i) / 1000, (double) flightData.getSeries(4).getY(i) / 1000);
                //polyline1.add(coor );
                coord.add(j, c);
                j++;
                Log.d(TAG, "Latitude:" + (flightData.getSeries(3).getY(i)));
                Log.d(TAG, "Longitude:" + (flightData.getSeries(4).getY(i)));
                Log.d(TAG, "Latitude:" + (double) (flightData.getSeries(3).getY(i)) / (double) 1000);
                Log.d(TAG, "Longitude:" + (double) (flightData.getSeries(4).getY(i)) / (double) 1000);
           }
        }
        polyline1.setPoints(coord);
        // Position the map's camera near Alice Springs in the center of Australia,
        // and set the zoom factor so most of Australia shows on the screen.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((double)flightData.getSeries(3).getMaxY()/(double)1000,(double)flightData.getSeries(4).getMaxY()/(double)1000), 20));


    }
}
