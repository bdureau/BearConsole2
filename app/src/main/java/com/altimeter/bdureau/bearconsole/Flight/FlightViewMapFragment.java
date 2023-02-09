package com.altimeter.bdureau.bearconsole.Flight;
/**
 * @description: This will display altimeter MAP that has been recorded when using the altiGPS
 * @author: boris.dureau@neuf.fr
 **/
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;
import java.util.List;

public class FlightViewMapFragment extends Fragment {
    private static final String TAG = "Tab3StatusFragment";
    private boolean ViewCreated = false;
    Integer MapType;
    private GoogleMap mMap = null;
    private ConsoleApplication myBT;
    private ViewPager lPager;
    private Button butBack, butShareMap, butMapType;
    private FlightData myflight;
    private String FlightName;

    public FlightViewMapFragment(ConsoleApplication bt,
                                 ViewPager pager,
                                 FlightData pFlight,
                                 String pFlightName) {
        myBT = bt;
        lPager = pager;
        myflight = pFlight;
        FlightName = pFlightName;
    }

    public GoogleMap getlMap() {
        return mMap;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_flight_view_maps, container, false);

        butBack = (Button) view.findViewById(R.id.butBack);
        butShareMap = (Button) view.findViewById(R.id.butShareMap);
        butMapType = (Button) view.findViewById(R.id.butMap);

        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapFlight);
        MapType = Integer.parseInt(myBT.getAppConf().getMapType());

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                /* MAP_TYPE_NONE = 0
                MAP_TYPE_NORMAL =1
                MAP_TYPE_SATELLITE = 2
                MAP_TYPE_TERRAIN = 3
                MAP_TYPE_HYBRID = 4
                */
                mMap.setMapType(Integer.parseInt(myBT.getAppConf().getMapType()));


                XYSeriesCollection flightData;
                flightData = myflight.GetFlightData(FlightName);

                Log.d(TAG, "nbr of item:" + flightData.getSeries(6).getItemCount());
                Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                        .clickable(false));

                List<LatLng> coord;
                coord = new ArrayList();
                int j = 0;
                for (int i = 0; i < flightData.getSeries(6).getItemCount(); i++) {
                    double res = (double) flightData.getSeries(6).getY(i);
                    Log.d("map", "lat:" + (double) flightData.getSeries(6).getY(i));
                    Log.d("map", "long:" + (double) flightData.getSeries(7).getY(i));
                    if (res > 0.0d) {
                        LatLng c = new LatLng((double) flightData.getSeries(6).getY(i), (double) flightData.getSeries(7).getY(i));
                        coord.add(j, c);
                        j++;
                    }
                }

                polyline1.setColor(myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getMapColor())));
                polyline1.setPoints(coord);

                if (coord.size() > 0)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord.get((int) (j / 2)), 15));
            }
        });

        butBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lPager.setCurrentItem(0);

            }
        });
        butShareMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeMapScreenshot();
            }
        });
        butMapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapType = MapType + 1;
                if (MapType > 4)
                    MapType = 0;

                mMap.setMapType(MapType);
            }
        });
        ViewCreated = true;
        return view;
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
            String filePath = MediaStore.Images.Media.insertImage(this.getContext().getContentResolver(),
                    bitmap, "Title", null);
            Uri fileUri = Uri.parse(filePath);
            // Share the screenshot
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, fileUri);
            startActivity(Intent.createChooser(share, "Share Map screenshot"));
        } catch (Exception e) {
            //Toast.makeText(this, "Error saving/sharing Map screenshot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}