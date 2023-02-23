package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will allow rocket tracking using the latest rocket altimeter position
 * This is using the OpenMap
 *
 * @author: boris.dureau@neuf.fr
 **/
import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.LocationService;
import com.altimeter.bdureau.bearconsole.LocationUtils;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.ShareHandler;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class RocketTrackOpenMap extends AppCompatActivity {
    private MapView mMap= null;
    Marker marker,markerDest;
    Polyline polyline= null;
    IMapController mapController  = null;
    Thread altiStatus;
    boolean status = true;
    float rocketLatitude=48.8698f, rocketLongitude=2.2190f;
    private TextView textViewdistance;

    Button btnDismiss, butShareMap;
    LocationBroadCastReceiver receiver=null;

    GeoPoint dest = new GeoPoint(rocketLatitude, rocketLongitude);


    private ConsoleApplication myBT;
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
            float val = Float.parseFloat(value);
            Log.d("track", "latitude:"+ value);
            if(val !=0.0f) {
                rocketLatitude = Float.parseFloat(value) / 100000;
                myBT.getAppConf().setRocketLatitude(rocketLatitude);

            }
        }
    }

    private void setLongitudeValue(String value) {
        if (value.matches("\\d+(?:\\.\\d+)?")) {
            float val = Float.parseFloat(value);
            Log.d("track", "longitude:"+ value);
            if(val !=0.0f) {
                rocketLongitude = Float.parseFloat(value) / 100000;
                myBT.getAppConf().setRocketLongitude( rocketLongitude);
                myBT.getAppConf().SaveConfig();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiver = new LocationBroadCastReceiver();
        myBT = (ConsoleApplication) getApplication();
        setContentView(R.layout.activity_rocket_track_open_map);
        myBT.setHandler(handler);

        textViewdistance = (TextView) findViewById(R.id.textViewdistance);

        rocketLatitude = myBT.getAppConf().getRocketLatitude(); //Double.parseDouble(myBT.getAppConf().getRocketLatitude());

        rocketLongitude = myBT.getAppConf().getRocketLongitude();

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

        mMap = (MapView) findViewById(R.id.mapOpenMap);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setBuiltInZoomControls(true);
        mMap.setMultiTouchControls(true);

        marker = new Marker(mMap);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_person_map));
        mMap.getOverlays().add(marker);

        markerDest = new Marker(mMap);
        markerDest.setIcon(getResources().getDrawable(R.drawable.ic_rocket_map));
        mMap.getOverlays().add(markerDest);

        polyline = new Polyline();
        polyline.setColor(myBT.getAppConf().ConvertColor(myBT.getAppConf().getMapColor()));
        polyline.setWidth(10);
        mMap.getOverlays().add(polyline);

        mapController = mMap.getController();
        mapController.setZoom(18.0);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        butShareMap = (Button) findViewById(R.id.butShareMap);

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


    private void startService() {

        IntentFilter filter = new IntentFilter("ACT_LOC");
        registerReceiver(receiver, filter);

        Intent intent = new Intent( RocketTrackOpenMap.this, LocationService.class);
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
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (mMap != null) {
            mMap.onResume();
        }
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
        Configuration.getInstance().save(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (mMap != null) {
            mMap.onPause();
        }
        if (receiver !=null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            receiver = null;
        }
    }

    public class LocationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d ("coordinate",intent.getAction());


            dest = new GeoPoint(rocketLatitude, rocketLongitude);
            if(intent.getAction().equals("ACT_LOC")) {
                double latitude = intent.getDoubleExtra("latitude", 0f);
                double longitude = intent.getDoubleExtra("longitude", 0f);
                double distance = LocationUtils.distanceBetweenCoordinate(latitude, rocketLatitude, longitude, rocketLongitude);
                textViewdistance.setText(String.format("%.2f",distance )+ " " + myBT.getAppConf().getUnitsValue());
                Log.d ("coordinate","latitude is:" + latitude + " longitude is: " + longitude );
                Log.d ("coordinate","rocketLatitude is:" + latitude + " rocketLongitude is: " + longitude );
                if(mMap !=null){

                    GeoPoint latLng = new GeoPoint(latitude, longitude);
                    mapController.setCenter(latLng);
                    if(marker != null) {
                       marker.setPosition(latLng);
                       marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    }
                    if(markerDest != null) {
                        markerDest.setPosition(dest);
                    }

                    if (polyline != null) {
                        ArrayList<GeoPoint> pathPoints=new ArrayList();
                        pathPoints.add(latLng);
                        pathPoints.add(dest);
                        polyline.setPoints(pathPoints);
                        if(mMap.getZoomLevelDouble() > 10.0) {
                            mapController.setCenter(latLng);
                        } else {
                            mapController.setCenter(latLng);
                            mapController.setZoom(15.0);
                        }
                    }
                }
            }

        }
    }

    private  void takeMapScreenshot() {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilShare");
            if (!mainDir.exists()) {
                boolean mkdir = mainDir.mkdir();
            }

            String path = mainDir + "/" + "AltiMultiCurve" + "-" + format + ".jpeg";
            findViewById(android.R.id.content).getRootView().setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(findViewById(android.R.id.content).getRootView().getDrawingCache());
            findViewById(android.R.id.content).getRootView().setDrawingCacheEnabled(false);


            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            //shareScreenShot(imageFile);
            ShareHandler.shareScreenShot(imageFile, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*private  void shareScreenShot(File imageFile ) {
        Log.d("Package Name", "Package Name" + this.getPackageName());
        Uri uri = FileProvider.getUriForFile(
                this,
                this.getPackageName() +  ".provider",
                imageFile);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.bearconsole_has_shared3));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, getString(R.string.share_with3)));
        } catch (ActivityNotFoundException e) {
            //Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }*/
}
