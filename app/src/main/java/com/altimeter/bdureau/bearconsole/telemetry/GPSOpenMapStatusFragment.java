package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display altimeter MAP when using the altiGPS
 * This is using OpenMap
 * @author: boris.dureau@neuf.fr
 **/
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

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

public class GPSOpenMapStatusFragment extends Fragment {
    private static final String TAG = "Tab3StatusFragment";
    private boolean ViewCreated = false;

    private MapView mMap= null;
    private ConsoleApplication myBT;
    private ViewPager lPager;
    private Button butBack, butShareMap;

    Marker marker,markerDest;
    Polyline polyline= null;
    IMapController mapController  = null;
    double rocketLatitude=48.8698, rocketLongitude=2.2190;
    GeoPoint dest = new GeoPoint(rocketLatitude, rocketLongitude);

    public GPSOpenMapStatusFragment(ConsoleApplication bt, ViewPager pager) {
        myBT = bt;
        lPager = pager;
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(this.getContext(),
                PreferenceManager.getDefaultSharedPreferences(this.getContext()));
        View view = inflater.inflate(R.layout.activity_altimeter_status_open_map, container, false);

        butBack = (Button) view.findViewById(R.id.butBack);
        butShareMap = (Button) view.findViewById(R.id.butShareMap);

        mMap = (MapView) view.findViewById(R.id.mapOpenMapStatus);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setBuiltInZoomControls(true);
        mMap.setMultiTouchControls(true);

        marker = new Marker(mMap);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_person_map));
        mMap.getOverlays().add(marker);

        markerDest = new Marker(mMap);
        markerDest.setIcon(getResources().getDrawable(R.drawable.ic_rocket_map));
        mMap.getOverlays().add(markerDest);

        polyline = new Polyline(mMap);
        polyline.setColor(myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getMapColor())));
        polyline.setWidth(10);
        mMap.getOverlays().add(polyline);

        mapController = mMap.getController();
        mapController.setZoom(15.0);

        butBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lPager.setCurrentItem(0);

            }
        });

        butShareMap.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                takeMapScreenshot(view);
            }
        });

        ViewCreated = true;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Configuration.getInstance().load(getView().getContext(),
                PreferenceManager.getDefaultSharedPreferences(getView().getContext()));
        if (mMap != null) {
            mMap.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Configuration.getInstance().save(getView().getContext(),
                PreferenceManager.getDefaultSharedPreferences(getView().getContext()));
        if (mMap != null) {
            mMap.onPause();
        }
    }
    public void updateLocation(double latitude, double longitude, double rocketLat, double rocketLong) {
        dest = new GeoPoint(rocketLat, rocketLong);
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
                try  {
                    polyline.setPoints(pathPoints);
                    //mMap.animate().
                    if(mMap.getZoomLevelDouble() > 10.0) {
                        mapController.setCenter(latLng);
                    } else {
                        mapController.setCenter(latLng);
                        mapController.setZoom(15.0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private  void takeMapScreenshot(View view) {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FileShare");
            if (!mainDir.exists()) {
                boolean mkdir = mainDir.mkdir();
            }

            String path = mainDir + "/" + "AltiMultiCurve" + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);


            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            shareScreenShot(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private  void shareScreenShot(File imageFile ) {
        Log.d("Package Name", "Package Name" + this.getContext().getPackageName());
        Uri uri = FileProvider.getUriForFile(
                this.getContext(),
                this.getContext().getPackageName() +  ".provider",
                imageFile);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.bear_console_has_shared6));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, getString(R.string.share_with6)));
        } catch (ActivityNotFoundException e) {
            //Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

}