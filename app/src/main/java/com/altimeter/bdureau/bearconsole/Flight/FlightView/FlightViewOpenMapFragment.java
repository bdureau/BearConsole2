package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Flight Computer Management
 * Class: FlightViewOpenMapFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Displays recorded GPS flight trajectories on OpenStreetMap tiles using osmdroid.
 *  Includes asynchronous map screenshot generation, safe lifecycle management,
 *  and sharing via FileProvider compliant with modern Android standards.
 * ============================================================================
 */

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlightViewOpenMapFragment extends Fragment {
    private static final String TAG = "FlightViewOpenMap";

    private MapView mMap = null;
    private ConsoleApplication myBT;
    private ViewPager lPager;
    private Button butBack, butShareMap;
    private FlightData myflight;
    private String flightName;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Default zero-argument constructor required for Fragment lifecycle restoration.
     */
    public FlightViewOpenMapFragment() {
        super();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // After a re-creation by the framework the dependencies were not given by a
        // constructor: the application object is always available from the context
        if (myBT == null) {
            myBT = (ConsoleApplication) context.getApplicationContext();
        }
    }

    /**
     * After a re-creation by the framework the flight data were not given by a constructor:
     * they are taken again from the host FlightViewTabActivity. Called from onCreateView(),
     * when the activity has finished its own onCreate().
     */
    private void restoreFromHost() {
        if (!(getActivity() instanceof FlightViewTabActivity)) return;
        FlightViewTabActivity host = (FlightViewTabActivity) getActivity();
        if (myflight == null) myflight = host.getFlightRepository();
        if (flightName == null) flightName = host.getFlightName();
    }

    /**
     * ViewPager of the host activity, used to go back to the first tab. After a re-creation
     * it was not given by the constructor and is looked up in the activity layout.
     */
    private ViewPager getPager() {
        if (lPager == null && getActivity() != null) {
            lPager = getActivity().findViewById(R.id.container);
        }
        return lPager;
    }

    public FlightViewOpenMapFragment(ConsoleApplication bt,
                                     ViewPager pager,
                                     FlightData pFlight,
                                     String pFlightName) {
        this.myBT = bt;
        this.lPager = pager;
        this.myflight = pFlight;
        this.flightName = pFlightName;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        restoreFromHost();
        Context context = requireContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        View view = inflater.inflate(R.layout.fragment_flight_view_open_map, container, false);

        butBack = view.findViewById(R.id.butBack);
        butShareMap = view.findViewById(R.id.butShareMap);
        mMap = view.findViewById(R.id.mapOpenMapFlight);

        if (mMap != null) {
            mMap.setTileSource(TileSourceFactory.MAPNIK);
            mMap.setBuiltInZoomControls(true);
            mMap.setMultiTouchControls(true);
            drawMap();
        }

        if (butBack != null) {
            butBack.setOnClickListener(v -> {
                if (getPager() != null) {
                    getPager().setCurrentItem(0);
                }
            });
        }

        if (butShareMap != null) {
            butShareMap.setOnClickListener(v -> takeMapScreenshotAsync(view));
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        }
        if (mMap != null) {
            mMap.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            Configuration.getInstance().save(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        }
        if (mMap != null) {
            mMap.onPause();
        }
    }

    /**
     * Extracts coordinates from flight dataset and plots the polyline route.
     */
    private void drawMap() {
        if (myflight == null || flightName == null || mMap == null) {
            return;
        }

        XYSeriesCollection flightData = myflight.GetFlightData(flightName);
        if (flightData == null || flightData.getSeriesCount() < 8) {
            return;
        }

        XYSeries latSeries = flightData.getSeries(6);
        XYSeries lonSeries = flightData.getSeries(7);
        int itemCount = Math.min(latSeries.getItemCount(), lonSeries.getItemCount());

        ArrayList<GeoPoint> pathPoints = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            double lat = latSeries.getY(i).doubleValue();
            double lon = lonSeries.getY(i).doubleValue();
            if (lat > 0.0 && lon > 0.0) {
                pathPoints.add(new GeoPoint(lat, lon));
            }
        }

        if (pathPoints.isEmpty()) {
            return;
        }

        Polyline polyline = new Polyline();
        if (myBT != null && myBT.getAppConf() != null) {
            polyline.setColor(myBT.getAppConf().ConvertColor(myBT.getAppConf().getMapColor()));
        }
        polyline.setWidth(5f);
        polyline.setPoints(pathPoints);

        mMap.getOverlays().clear();
        mMap.getOverlays().add(polyline);

        IMapController mapController = mMap.getController();
        if (mapController != null) {
            GeoPoint centerPoint = pathPoints.get(pathPoints.size() / 2);
            mapController.setCenter(centerPoint);
            mapController.setZoom(15.0);
        }

        mMap.invalidate();
    }

    /**
     * Captures view bitmap on UI thread, then writes and compresses in background.
     */
    private void takeMapScreenshotAsync(View view) {
        if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) {
            return;
        }

        // Capture modern Canvas rendering without deprecated getDrawingCache()
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        Context context = getContext();
        if (context == null) return;

        ioExecutor.execute(() -> {
            CharSequence format = DateFormat.format("MM-dd-yyyy_hh-mm-ss", new Date());
            File shareDir = new File(context.getCacheDir(), "MapShare");
            if (!shareDir.exists()) {
                shareDir.mkdirs();
            }

            File imageFile = new File(shareDir, "AltiMultiMap-" + format + ".png");
            try (FileOutputStream fos = new FileOutputStream(imageFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bos.flush();

                mainHandler.post(() -> {
                    if (isAdded()) {
                        shareScreenshot(imageFile);
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Failed to write map screenshot", e);
            }
        });
    }

    /**
     * Launches system share sheet with proper URI grant permissions.
     */
    private void shareScreenshot(File imageFile) {
        Context context = getContext();
        if (context == null || imageFile == null || !imageFile.exists()) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                imageFile);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.bearconsole_has_shared));
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_with)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, getString(R.string.no_app_available), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMap != null) {
            mMap.onDetach(); // Clean up osmdroid workers and avoid leaks
            mMap = null;
        }
        butBack = null;
        butShareMap = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}