package com.altimeter.bdureau.bearconsole.telemetry;
/**
 * @description: This will display altimeter MAP when using the altiGPS
 * @author: boris.dureau@neuf.fr
 **/
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class GPSGoogleMapStatusFragment extends Fragment {
    private static final String TAG = "Tab3StatusFragment";
    private boolean ViewCreated = false;
    Integer MapType;
    private GoogleMap mMap = null;
    private ConsoleApplication myBT;
    private ViewPager lPager;
    private Button butBack, butShareMap, butMapType;

    Marker marker, markerDest;
    Polyline polyline1 = null;
    public double rocketLatitude = 48.8698;
    public double rocketLongitude = 2.2190;
    LatLng dest = new LatLng(rocketLatitude, rocketLongitude);

    public GPSGoogleMapStatusFragment(ConsoleApplication bt, ViewPager pager) {
        myBT = bt;
        lPager = pager;
    }

    public GoogleMap getlMap() {
        return mMap;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_altimeter_status_google_map, container, false);

        butBack = (Button) view.findViewById(R.id.butBack);
        butShareMap = (Button) view.findViewById(R.id.butShareMap);
        butMapType = (Button) view.findViewById(R.id.butMap);

        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapStatus);
        MapType = myBT.getAppConf().getMapType();

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (mMap == null) {
                    mMap = googleMap;
                    mMap.setMapType(myBT.getAppConf().getMapType());
                }
            }
        });

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
                takeMapScreenshot();
            }
        });
        butMapType.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MapType = MapType + 1;
                if(MapType > 4)
                    MapType =0;

                mMap.setMapType(MapType);
            }
        });
        ViewCreated = true;
        return view;
    }

    public void updateLocation(double latitude, double longitude, double rocketLat, double rocketLong) {
        if (mMap != null) {
            LatLng latLng = new LatLng(latitude, longitude);
            if (marker != null) {
                marker.setPosition(latLng);
            } else {
                BitmapDescriptor manIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_map);
                marker = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng).icon(manIcon));
            }

            if (markerDest != null) {
                markerDest.setPosition(dest);
            } else {
                BitmapDescriptor rocketIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_rocket_map);
                markerDest = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(dest).icon(rocketIcon));
            }
            List<LatLng> coord;
            coord = new ArrayList();

            dest = new LatLng(rocketLat, rocketLong);
            coord.add(0, dest);

            coord.add(1, latLng);
            if (polyline1 == null)
                polyline1 = mMap.addPolyline(new PolylineOptions().clickable(false));
            //Get the line color from the config
            polyline1.setColor(myBT.getAppConf().ConvertColor(myBT.getAppConf().getMapColor()));
            polyline1.setPoints(coord);
            if (mMap.getCameraPosition().zoom > 10)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMap.getCameraPosition().zoom));
            else
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
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
            startActivity(Intent.createChooser(share, getString(R.string.share_map_screenshot3)));
        } catch (Exception e) {
            //Toast.makeText(this, "Error saving/sharing Map screenshot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}