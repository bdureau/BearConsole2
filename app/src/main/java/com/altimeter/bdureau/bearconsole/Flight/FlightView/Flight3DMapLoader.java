package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: Flight3DMapLoader
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Builds the ground texture of the 3D flight view: an off-screen OpenStreetMap image of
 *  the launch field, made with the osmdroid MapSnapshot (same tiles and same disk cache as
 *  the OpenStreetMap tab, so it also works offline when the field was displayed before).
 *
 *  The requested square (center and half size in meters from the pad) is converted to a
 *  geographic center and a zoom level giving about SNAPSHOT_SIZE pixels for the square.
 *  The exact area covered by the image is read back from the projection, so the texture is
 *  placed exactly on the ground whatever the zoom rounding.
 *
 *  If the tiles cannot be obtained within TIMEOUT_MS (no network and no cache), the loader
 *  gives up and the 3D view keeps its plain ground with a grid.
 * ============================================================================
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.drawing.MapSnapshot;

import java.util.Collections;

public class Flight3DMapLoader {
    private static final String TAG = "Flight3DMapLoader";

    /** Size of the ground image (pixels, square, power of two for old GPUs). */
    private static final int SNAPSHOT_SIZE = 1024;
    /** Highest zoom level of the OpenStreetMap tiles. */
    private static final double MAX_ZOOM = 19.0;
    /** Time allowed to get the tiles. */
    private static final long TIMEOUT_MS = 20000;
    /** Ground resolution at zoom 0 at the equator, meters per pixel (256-pixel tiles). */
    private static final double METERS_PER_PIXEL_ZOOM0 = 156543.03392;

    /** Result, delivered on the main thread. */
    public interface Callback {
        /**
         * @param bitmap ground image (north up)
         * @param westM  west edge, meters east of the pad (negative = west)
         * @param northM north edge, meters north of the pad
         * @param eastM  east edge, meters east of the pad
         * @param southM south edge, meters north of the pad
         */
        void onGroundMap(Bitmap bitmap, double westM, double northM, double eastM, double southM);

        /** The map could not be obtained (offline without cache, error). */
        void onGroundMapUnavailable();
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MapSnapshot snapshot;
    private MapTileProviderBasic tileProvider;
    private Callback callback;
    private boolean finished;

    /**
     * Starts the loading (main thread).
     *
     * @param centerEastM  center of the square, meters east of the pad
     * @param centerNorthM center of the square, meters north of the pad
     * @param halfSizeM    half size of the square, meters
     */
    public void load(Context context, Flight3DTrack track, double centerEastM, double centerNorthM,
                     double halfSizeM, Callback cb) {
        cancel();
        callback = cb;
        finished = false;
        try {
            final double centerLat = track.latitudeOf(centerNorthM);
            final double centerLon = track.longitudeOf(centerEastM);
            double metersPerPixel = 2.0 * halfSizeM / SNAPSHOT_SIZE;
            double zoom = Math.log(METERS_PER_PIXEL_ZOOM0 * Math.cos(Math.toRadians(centerLat)) / metersPerPixel)
                    / Math.log(2.0);
            zoom = Math.max(2.0, Math.min(MAX_ZOOM, zoom));

            final Projection projection = new Projection(zoom, SNAPSHOT_SIZE, SNAPSHOT_SIZE,
                    new GeoPoint(centerLat, centerLon), 0f, false, false, 0, 0);
            tileProvider = new MapTileProviderBasic(context.getApplicationContext(), TileSourceFactory.MAPNIK);

            // Area really covered by the image (zoom limits, rounding)
            IGeoPoint nw = projection.fromPixels(0, 0);
            IGeoPoint se = projection.fromPixels(SNAPSHOT_SIZE, SNAPSHOT_SIZE);
            final double west = track.eastOf(nw.getLongitude());
            final double north = track.northOf(nw.getLatitude());
            final double east = track.eastOf(se.getLongitude());
            final double south = track.northOf(se.getLatitude());

            // Only complete images: up-to-date or expired tiles, no "scaled" placeholder
            snapshot = new MapSnapshot(s -> {
                if (s.getStatus() != MapSnapshot.Status.CANVAS_OK) return;
                final Bitmap bmp = s.getBitmap();
                mainHandler.post(() -> {
                    if (finished || bmp == null) return;
                    finished = true;
                    Callback c = callback;
                    release();
                    if (c != null) c.onGroundMap(bmp, west, north, east, south);
                });
            }, MapSnapshot.INCLUDE_FLAG_UPTODATE | MapSnapshot.INCLUDE_FLAG_EXPIRED,
                    tileProvider, Collections.emptyList(), projection);
            new Thread(snapshot, "Flight3DMapSnapshot").start();

            mainHandler.postDelayed(this::onTimeout, TIMEOUT_MS);
        } catch (RuntimeException e) {
            Log.w(TAG, "Map snapshot failed", e);
            fail();
        }
    }

    /** Stops the loading; no callback will be called afterwards (main thread). */
    public void cancel() {
        finished = true;
        callback = null;
        release();
    }

    private void onTimeout() {
        if (!finished) {
            Log.d(TAG, "Map tiles not available in time");
            fail();
        }
    }

    private void fail() {
        if (finished) return;
        finished = true;
        Callback c = callback;
        release();
        if (c != null) c.onGroundMapUnavailable();
    }

    private void release() {
        mainHandler.removeCallbacksAndMessages(null);
        if (snapshot != null) {
            try {
                // Also detaches the tile provider
                snapshot.onDetach();
            } catch (RuntimeException e) {
                Log.w(TAG, "Error while releasing the snapshot", e);
            }
            snapshot = null;
            tileProvider = null;
        } else if (tileProvider != null) {
            tileProvider.detach();
            tileProvider = null;
        }
    }
}
