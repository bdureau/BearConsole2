package com.altimeter.bdureau.bearconsole.Flight.FlightView;
/**
 * @description: This will display altimeter MAP that has been recorded when using the altiGPS
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
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.R;
import com.google.android.gms.maps.CameraUpdateFactory;

import org.afree.data.xy.XYSeriesCollection;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;


public class FlightViewOpenMapFragment extends Fragment {
    private static final String TAG = "Tab4StatusFragment";
    private boolean ViewCreated = false;

    private MapView mMap= null;
    private ConsoleApplication myBT;
    private ViewPager lPager;
    private Button butBack, butShareMap;
    private FlightData myflight;
    private String FlightName;

    public FlightViewOpenMapFragment(ConsoleApplication bt,
                                       ViewPager pager,
                                       FlightData pFlight,
                                       String pFlightName) {
        myBT = bt;
        lPager = pager;
        myflight = pFlight;
        FlightName = pFlightName;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().load(this.getContext(),
                PreferenceManager.getDefaultSharedPreferences(this.getContext()));
        View view = inflater.inflate(R.layout.activity_flight_view_open_map, container, false);

        butBack = (Button) view.findViewById(R.id.butBack);
        butShareMap = (Button) view.findViewById(R.id.butShareMap);

        mMap = (MapView) view.findViewById(R.id.mapOpenMapFlight);
        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setBuiltInZoomControls(true);
        mMap.setMultiTouchControls(true);

        drawMap();


        butBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lPager.setCurrentItem(0);

            }
        });
        butShareMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    private void drawMap() {
        XYSeriesCollection flightData;
        flightData = myflight.GetFlightData(FlightName);
        GeoPoint firstPoint = null;

        Polyline polyline;
        ArrayList<GeoPoint> pathPoints=new ArrayList();

        polyline = new Polyline();
        polyline.setColor(myBT.getAppConf().ConvertColor(myBT.getAppConf().getMapColor()));
        polyline.setWidth(5);
        mMap.getOverlays().add(polyline);

        int j = 0;
        for (int i = 0; i < flightData.getSeries(6).getItemCount(); i++) {
            double res = (double) flightData.getSeries(6).getY(i);
            Log.d("map", "lat:" + (double) flightData.getSeries(6).getY(i));
            Log.d("map", "long:" + (double) flightData.getSeries(7).getY(i));
            if (res > 0.0d) {
                GeoPoint c = new GeoPoint((double) flightData.getSeries(6).getY(i), (double) flightData.getSeries(7).getY(i));
                pathPoints.add(c);
                if(firstPoint == null)
                    firstPoint = c;
                j++;
            }
        }

        polyline.setPoints(pathPoints);
        IMapController mapController = mMap.getController();

        if(firstPoint !=null) {
            GeoPoint startPoint = new GeoPoint(firstPoint);
            if (pathPoints.size() > 0)
                mapController.setCenter(pathPoints.get((int) (pathPoints.size()/2)));
            mapController.setZoom(15.0);

        }
    }

    private  void takeMapScreenshot(View view) {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilShare");
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
        intent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.bearconsole_has_shared));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, getString(R.string.share_with)));
        } catch (ActivityNotFoundException e) {
            //Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }
}