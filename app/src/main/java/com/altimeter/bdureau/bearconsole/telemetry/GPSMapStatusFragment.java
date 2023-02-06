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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class GPSMapStatusFragment extends Fragment {
    private static final String TAG = "Tab3StatusFragment";
    private boolean ViewCreated = false;

    private GoogleMap lMap = null;
    private ConsoleApplication lBT;
    private ViewPager lPager;
    private Button butBack, butShareMap;

    public GPSMapStatusFragment(ConsoleApplication bt, ViewPager pager) {
        lBT = bt;
        lPager = pager;
    }

    public GoogleMap getlMap() {
        return lMap;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_altimeter_status_tab3, container, false);

        butBack = (Button) view.findViewById(R.id.butBack);
        butShareMap = (Button) view.findViewById(R.id.butShareMap);
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapStatus);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (lMap == null) {
                    lMap = googleMap;
                    lMap.setMapType(Integer.parseInt(lBT.getAppConf().getMapType()));
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
        lMap.snapshot(callback);
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