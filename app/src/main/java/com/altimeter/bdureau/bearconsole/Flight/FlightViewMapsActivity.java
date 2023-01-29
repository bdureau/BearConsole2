package com.altimeter.bdureau.bearconsole.Flight;

/**
 *   @description: This class display the latitude and longitude on a google map. This is used when
 *   retrieving data for the AltiGPS
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.ShareHandler;
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

public class FlightViewMapsActivity extends /*FragmentActivity*/ AppCompatActivity implements OnMapReadyCallback {

    SupportMapFragment mapFragment;
    GoogleMap mMap;
    String FlightName = null;
    ConsoleApplication myBT ;
    private FlightData myflight=null;
    Button butDismissViewMap, butShareMap;

    public String TAG = "MAP_ACTIVITY";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_view_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        butDismissViewMap = (Button) findViewById(R.id.butDismissViewMap);
        butDismissViewMap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the activity
            }
        });

        butShareMap = (Button) findViewById(R.id.butShareMap);
        butShareMap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(),  mapFragment.getContext());
                takeMapScreenshot();
            }
        });

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();

        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);

        myflight= myBT.getFlightData();

    }

    private void takeMapScreenshot() {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            Bitmap bitmap;

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                // Callback is called from the main thread, so we can modify the ImageView safely.
                bitmap = snapshot;
                shareScreenshot(bitmap);
            }
        };
        mMap.snapshot(callback);
    }

    private void shareScreenshot(Bitmap bitmap) {
        try {
            // Save the screenshot to a file
            //String fileName = "screenshot.jpg";
            String filePath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
            Uri fileUri = Uri.parse(filePath);
            // Share the screenshot
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivity(Intent.createChooser(share, "Share screenshot"));
        } catch (Exception e) {
            Toast.makeText(this, "Error saving/sharing Map screenshot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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
        //this.mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
         /* MAP_TYPE_NONE = 0
        MAP_TYPE_NORMAL =1
        MAP_TYPE_SATELLITE = 2
        MAP_TYPE_TERRAIN = 3
        MAP_TYPE_HYBRID = 4
        */
        this.mMap.setMapType(Integer.parseInt(myBT.getAppConf().getMapType()));



        XYSeriesCollection flightData;
        flightData = myflight.GetFlightData(FlightName);

        Log.d(TAG, "nbr of item:" +flightData.getSeries(6).getItemCount());
        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(false));

        List<LatLng> coord;
        coord =  new ArrayList();
        int j=0;
        for (int i=0; i< flightData.getSeries(6).getItemCount(); i++)   {
            double res = (double)flightData.getSeries(6).getY(i);
            Log.d("map", "lat:"+ (double) flightData.getSeries(6).getY(i) );
            Log.d("map", "long:"+ (double) flightData.getSeries(7).getY(i) );
            if(res >0.0d) {
                LatLng c = new LatLng((double) flightData.getSeries(6).getY(i) , (double) flightData.getSeries(7).getY(i) );
                coord.add(j, c);
                j++;
           }
        }

        polyline1.setColor(myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getMapColor())));
        polyline1.setPoints(coord);

        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((double)flightData.getSeries(3).getMaxY()/(double)100000,(double)flightData.getSeries(4).getMaxY()/(double)100000), 20));

        if(coord.size() > 0)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord.get((int)(j/2)),15));
    }
}
