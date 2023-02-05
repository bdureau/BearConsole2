package com.altimeter.bdureau.bearconsole.telemetry;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;
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
    TextView textViewRecording;

    Button btnDismiss, butShareMap;
    LocationBroadCastReceiver receiver=null;
    LatLng dest = new LatLng(rocketLatitude, rocketLongitude);
    boolean recording = false;

    private static ConsoleApplication myBT;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 18:
                    //Value 18 contains the latitude
                    Log.d("status", "latitude");
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
            Log.d("track", "latitude:"+ value);
            if(val !=0) {
                rocketLatitude = Double.parseDouble(value) / 100000;
                myBT.getAppConf().setRocketLatitude(""+ rocketLatitude);

            }
        }
    }

    private void setLongitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            Double val = Double.parseDouble(value);
            Log.d("track", "longitude:"+ value);
            if(val !=0) {
                rocketLongitude = Double.parseDouble(value) / 100000;
                myBT.getAppConf().setRocketLongitude(""+ rocketLongitude);
                myBT.getAppConf().SaveConfig();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new LocationBroadCastReceiver();
        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_rocket_track);
        myBT.setHandler(handler);

        // See if we are called from the status activity
        try {
            Intent newint = getIntent();

            if (newint.getStringExtra("recording").equals("true"))
                recording = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (myBT.getAppConf().getRocketLatitude().matches("\\d+(?:\\.\\d+)?"))
            rocketLatitude = Double.parseDouble(myBT.getAppConf().getRocketLatitude());

        if (myBT.getAppConf().getRocketLongitude().matches("\\d+(?:\\.\\d+)?"))
            rocketLongitude = Double.parseDouble(myBT.getAppConf().getRocketLongitude());

        if(Build.VERSION.SDK_INT>=23) {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            } else {
                startService();
            }
        } else {
            startService();
        }

        LocationManager lm = (LocationManager)this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
//this.getBaseContext()
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.Cancel,null)
                    .show();
        }
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        butShareMap = (Button) findViewById(R.id.butShareMap);
        textViewRecording = (TextView) findViewById(R.id.textViewRecording);

        if(!recording)
            textViewRecording.setVisibility(View.INVISIBLE);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        butShareMap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                takeMapScreenshot();
            }
        });
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!status) break;
                    if (! myBT.getConnected()) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        altiStatus = new Thread(r);
        altiStatus.start();
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
            startActivity(Intent.createChooser(share, "Share Map screenshot"));
        } catch (Exception e) {
            Toast.makeText(this, "Error saving/sharing Map screenshot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);

        Intent intent = new Intent( RocketTrack.this, LocationService.class);
        startService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            receiver = null;
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(this.mMap == null) {
            this.mMap = googleMap;
            //this.mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            this.mMap.setMapType(Integer.parseInt(myBT.getAppConf().getMapType()));
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
                    //Get the line color from the config
                    polyline1.setColor(myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getMapColor())));
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
