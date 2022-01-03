package com.altimeter.bdureau.bearconsole.telemetry;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class RocketTrack extends AppCompatActivity implements OnMapReadyCallback {
    SupportMapFragment mapFragment;
    GoogleMap mMap;
    Marker marker, markerDest;
    Polyline polyline1 = null;
    Thread altiStatus;
    boolean status = true;
    Double rocketLatitude=48.8698, rocketLongitude=2.2190;

    Button btnDismiss;
    LocationBroadCastReceiver receiver=null;
    LatLng dest = new LatLng(rocketLatitude, rocketLongitude);

    private static ConsoleApplication myBT;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 18:
                    //Value 18 contains the latitude
                    setLatitudeValue((String) msg.obj );
                    break;
                case 19:
                    //Value 19 contains the longitude
                    setLongitudeValue((String) msg.obj );
                    break;
            }
        }
    };

    private void setLatitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Double val = Double.parseDouble(value);
            if(val !=0)
            rocketLatitude = Double.parseDouble(value);
        }
    }

    private void setLongitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Double val = Double.parseDouble(value);
            if(val !=0)
            rocketLongitude = Double.parseDouble(value);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new LocationBroadCastReceiver();
        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_rocket_track);
        myBT.setHandler(handler);

        if(Build.VERSION.SDK_INT>=23) {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            } else {
                startService();
            }
        } else {
            startService();
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);
        btnDismiss = (Button) findViewById(R.id.butDismiss);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (receiver !=null) {
                    unregisterReceiver(receiver);
                    receiver = null;
                }
                if (status & myBT.getConnected()) {

                    status = false;
                    myBT.write("h;\n".toString());

                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                if (myBT.getConnected()) {
                    //turn off telemetry
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y0;\n".toString());
                }
                finish();

            }
        });
    }
    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);

        Intent intent = new Intent( RocketTrack.this, LocationService.class);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService();
                } else {
                    Toast.makeText(this, "permission need to be granted", Toast.LENGTH_LONG).show();
                }
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        if(myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            status = true;
            altiStatus.start();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        if (receiver !=null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(this.mMap == null) {
            this.mMap = googleMap;
            this.mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    }

    public class LocationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d ("coordinate",intent.getAction());
            if(intent.getAction().equals("ACT_LOC")) {
                double latitude = intent.getDoubleExtra("latitude", 0f);
                double longitude = intent.getDoubleExtra("longitude", 0f);
                Toast.makeText(RocketTrack.this, "latitude is:" + latitude + " longitude is: " + longitude, Toast.LENGTH_LONG).show();
                Log.d ("coordinate","latitude is:" + latitude + " longitude is: " + longitude );
                if(mMap != null) {
                    LatLng latLng = new LatLng(latitude, longitude);
                    if(marker != null) {
                        marker.setPosition(latLng);
                    } else {
                        MarkerOptions markerOptions = new MarkerOptions();
                        //markerOptions.position(latLng);
                        BitmapDescriptor manIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_map);
                        marker = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng).icon(manIcon));
                        //marker = googleMap.addMarker(markerOptions);
                    }

                    if(markerDest != null) {
                        markerDest.setPosition(dest);
                    } else {
                        //MarkerOptions markerOptions = new MarkerOptions();
                        //markerOptions.position(dest);
                        BitmapDescriptor rocketIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_rocket_map);
                        markerDest = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(dest).icon(rocketIcon));
                        //markerDest = googleMap.addMarker(markerOptions);
                    }
                    List<LatLng> coord;
                    coord = new ArrayList();

                    //LatLng c = new LatLng((double) flightData.getSeries(3).getY(i) / 100000, (double) flightData.getSeries(4).getY(i) / 100000);
                    dest = new LatLng(rocketLatitude, rocketLongitude);
                    coord.add(0, dest);
                    // coord.add(1, map.getCameraPosition().target);
                    coord.add(1, latLng);
                    if (polyline1 == null)
                        polyline1 = mMap.addPolyline(new PolylineOptions()
                                .clickable(false));
                    polyline1.setColor(Color.YELLOW);
                    polyline1.setPoints(coord);
                    if(mMap.getCameraPosition().zoom > 10)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMap.getCameraPosition().zoom));
                    else
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            }
        }
    }
}
