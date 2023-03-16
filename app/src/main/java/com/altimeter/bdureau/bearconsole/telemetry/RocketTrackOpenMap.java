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
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
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
import org.osmdroid.views.overlay.compass.CompassOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RocketTrackOpenMap extends AppCompatActivity {
    private static final String TAG = "RocketTrackOpenMap";
    private MapView mMap= null;
    private Marker marker,markerDest;
    private Polyline polyline= null;
    private IMapController mapController  = null;
    private Thread altiStatus;
    boolean status = true;
    private float rocketLatitude=48.8698f, rocketLongitude=2.2190f;
    private TextView textViewdistance;
    Intent locIntent = null;

    private Button btnDismiss, butShareMap, butAudio;
    private LocationBroadCastReceiver receiver=null;

    private GeoPoint dest = new GeoPoint(rocketLatitude, rocketLongitude);

    private TextToSpeech mTTS = null;
    private long lastSpeakTime = 1000;
    private long distanceTime = 0;
    private boolean soundOn=true;


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

        CompassOverlay compassOverlay = new CompassOverlay(this, mMap);
        compassOverlay.enableCompass();
        mMap.getOverlays().add(compassOverlay);

        //init text to speech
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;

                    if (Locale.getDefault().getLanguage().equals("en"))
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if (Locale.getDefault().getLanguage().equals("fr"))
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else if (Locale.getDefault().getLanguage().equals("tr"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("nl"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("es"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("it"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("hu"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("ru"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);

                    Log.d("Voice", myBT.getAppConf().getTelemetryVoice() + "");

                    int i = 0;
                    try {
                        for (Voice tmpVoice : mTTS.getVoices()) {

                            if (tmpVoice.getName().startsWith(Locale.getDefault().getLanguage())) {
                                Log.d("Voice", tmpVoice.getName());
                                if (myBT.getAppConf().getTelemetryVoice() == i) {
                                    mTTS.setVoice(tmpVoice);
                                    Log.d("Voice", "Found voice");
                                    break;
                                }
                                i++;
                            }
                        }
                    } catch (Exception e) {

                    }


                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {

                    }
                } else {
                    Log.e("TTS", "Init failed");
                }
            }
        });
        mTTS.setPitch(1.0f);
        mTTS.setSpeechRate(1.0f);


        btnDismiss = (Button) findViewById(R.id.butDismiss);
        butShareMap = (Button) findViewById(R.id.butShareMap);
        butAudio= (Button) findViewById(R.id.butAudio);
        butAudio.setCompoundDrawablesWithIntrinsicBounds(R.drawable.audio_on32x32,
                0,0,0);

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

        butAudio.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(soundOn) {
                    soundOn = false;
                    butAudio.setCompoundDrawablesWithIntrinsicBounds(R.drawable.audio_off_32x32,
                            0,0,0);
                }
                else {
                    soundOn = true;
                    butAudio.setCompoundDrawablesWithIntrinsicBounds(R.drawable.audio_on32x32,
                            0,0,0);
                }
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

        //Intent intent = new Intent( RocketTrackOpenMap.this, LocationService.class);
        //startService(intent);
        locIntent = new Intent( RocketTrackOpenMap.this, LocationService.class);
        startService(locIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()" );
        if (locIntent !=null)
            stopService(locIntent);
        if (receiver !=null) {
            Log.d(TAG,"unregisterReceiver(receiver)" );
            unregisterReceiver(receiver);
            receiver = null;
        }
        try {
            mTTS.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
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
        Log.d(TAG,"onResume()" );
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
        if (receiver ==null) {
            try {
                Log.d(TAG,"onPause() - registerReceiver(receiver)" );
                startService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause()" );
        Configuration.getInstance().save(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (mMap != null) {
            mMap.onPause();
        }
        if (receiver !=null) {
            try {
                Log.d(TAG,"onPause() - unregisterReceiver(receiver)" );
                unregisterReceiver(receiver);
                receiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class LocationBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d ("coordinate",intent.getAction());
            distanceTime = System.currentTimeMillis();
            dest = new GeoPoint(rocketLatitude, rocketLongitude);
            if(intent.getAction().equals("ACT_LOC")) {
                double latitude = intent.getDoubleExtra("latitude", 0f);
                double longitude = intent.getDoubleExtra("longitude", 0f);
                double distance = LocationUtils.distanceBetweenCoordinate(latitude, rocketLatitude, longitude, rocketLongitude);
                textViewdistance.setText(String.format("%.2f",distance )+ " " + myBT.getAppConf().getUnitsValue());
                // Tell distance every 15 secondes
                if ((distanceTime - lastSpeakTime) > 15000 ) {
                    if (soundOn) {
                        mTTS.speak("Distance" + " " + String.valueOf((int) distance) + " "
                                + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                    }
                    lastSpeakTime = distanceTime;
                }
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
                            //mapController.setCenter(latLng);
                            //mapController.animateTo(latLng, 10.0, 10L);
                            mapController.animateTo(latLng);
                            //mMap.set
                        } else {
                            //mapController.setCenter(latLng);
                            //mapController.setZoom(15.0);
                            mapController.animateTo(latLng, 15.0, 10L);
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

            ShareHandler.shareScreenShot(imageFile, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
