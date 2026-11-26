package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlightView3DFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  "3D" tab of the flight view, added only for the AltiGPS: the recorded trajectory in
 *  three dimensions above the launch field.
 *   - Height: barometric altitude; horizontal position: GPS fixes (see Flight3DTrack).
 *   - Trajectory colored by flight phase (boost, coast, drogue, main), with its shadow on
 *     the ground, vertical lines every 2 s, the GPS fixes and the main events.
 *   - Ground: OpenStreetMap image of the field (Flight3DMapLoader), or a grid when offline.
 *   - Replay: play / pause and a time slider; the trajectory is drawn up to the chosen
 *     instant, with the time, height and vertical speed at that instant.
 *   - Options: vertical lines on / off, map on / off, view from above.
 *  Distances are displayed in the unit of the application (meters or feet).
 *
 *  The fragment takes its data from the host FlightViewTabActivity, so it is re-created
 *  correctly by the framework (no data given by a constructor).
 * ============================================================================
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.osmdroid.config.Configuration;

import java.util.Locale;

public class FlightView3DFragment extends Fragment {

    /** Indices of the curves of an AltiGPS flight (see FlightData.createFlight). */
    private static final int SERIES_ALTITUDE = 0;
    private static final int SERIES_LATITUDE = 6;
    private static final int SERIES_LONGITUDE = 7;

    private static final double FEET_PER_METER = 3.28084;
    /** Resolution of the replay slider. */
    private static final int SEEK_MAX = 1000;
    /** Replay frame period (ms) and wanted replay duration of a whole flight (s). */
    private static final long REPLAY_FRAME_MS = 33;
    private static final double REPLAY_TARGET_DURATION_SEC = 12.0;

    private ConsoleApplication myBT;
    private Flight3DTrack track;
    private Flight3DRenderer renderer;
    private Flight3DMapLoader mapLoader;

    private Flight3DView glView;
    private Flight3DLabelOverlay labels;
    private TextView statsText, liveText, hintText;
    private ImageButton playButton;
    private SeekBar seekBar;

    private boolean isMeters = true;
    /** Replay time window: from liftoff to landing (seconds from liftoff). */
    private double replayStartSec, replayEndSec, replaySpeed = 1.0;
    private double replayTimeSec;
    private boolean playing;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Zero-argument constructor required by the framework (re-creation). */
    public FlightView3DFragment() {
        super();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        myBT = (ConsoleApplication) context.getApplicationContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flight_view_3d, container, false);
        glView = view.findViewById(R.id.flight3dView);
        labels = view.findViewById(R.id.flight3dLabels);
        statsText = view.findViewById(R.id.flight3dStats);
        liveText = view.findViewById(R.id.flight3dLive);
        hintText = view.findViewById(R.id.flight3dHint);
        playButton = view.findViewById(R.id.flight3dPlay);
        seekBar = view.findViewById(R.id.flight3dSeek);
        TextView legend = view.findViewById(R.id.flight3dLegend);

        isMeters = myBT == null || myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS;
        track = buildTrack();

        if (track == null) {
            // No usable GPS data: explanation instead of an empty scene
            glView.setVisibility(View.GONE);
            labels.setVisibility(View.GONE);
            statsText.setVisibility(View.GONE);
            liveText.setVisibility(View.GONE);
            hintText.setVisibility(View.GONE);
            legend.setVisibility(View.GONE);
            view.findViewById(R.id.flight3dReplayBar).setVisibility(View.GONE);
            view.findViewById(R.id.flight3dOptionsBar).setVisibility(View.GONE);
            view.findViewById(R.id.flight3dNoGps).setVisibility(View.VISIBLE);
            return view;
        }

        renderer = new Flight3DRenderer(track, getResources().getDisplayMetrics().density);
        renderer.setLabelListener(labels);
        glView.setFlightRenderer(renderer);

        setupLabels();
        // Event labels never cover the information boxes drawn over the scene
        labels.setObstacles(statsText, liveText, hintText);
        statsText.setText(buildStatsText());
        legend.setText(buildLegend());
        hintText.setText(R.string.flight3d_hint);

        // ---- Replay ----
        replayStartSec = track.t[track.liftoffIndex];
        replayEndSec = track.t[track.landingIndex];
        replaySpeed = Math.max(1.0, (replayEndSec - replayStartSec) / REPLAY_TARGET_DURATION_SEC);
        replayTimeSec = replayEndSec;
        seekBar.setMax(SEEK_MAX);
        seekBar.setProgress(SEEK_MAX);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;
                setPlaying(false);
                showTime(replayStartSec + (replayEndSec - replayStartSec) * progress / SEEK_MAX);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
        playButton.setOnClickListener(v -> {
            if (!playing && replayTimeSec >= replayEndSec) {
                replayTimeSec = replayStartSec; // replay from the start
            }
            setPlaying(!playing);
        });
        showTime(replayTimeSec);

        // ---- Display options ----
        ToggleButton drops = view.findViewById(R.id.flight3dDrops);
        ToggleButton map = view.findViewById(R.id.flight3dMap);
        ToggleButton top = view.findViewById(R.id.flight3dTop);
        drops.setOnCheckedChangeListener((b, checked) -> {
            renderer.setShowDropLines(checked);
            glView.requestRender();
        });
        map.setOnCheckedChangeListener((b, checked) -> {
            renderer.setShowMap(checked);
            glView.requestRender();
        });
        top.setOnCheckedChangeListener((b, checked) -> {
            if (checked) renderer.setTopView(); else renderer.resetCamera();
            glView.requestRender();
        });

        loadGroundMap();
        return view;
    }

    // ------------------------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------------------------

    /** Builds the 3D track from the displayed flight, or null if it has no usable GPS data. */
    @Nullable
    private Flight3DTrack buildTrack() {
        if (!(getActivity() instanceof FlightViewTabActivity)) return null;
        XYSeriesCollection data = ((FlightViewTabActivity) getActivity()).getAllFlightData();
        if (data == null || data.getSeriesCount() <= SERIES_LONGITUDE) return null;

        XYSeries alt = data.getSeries(SERIES_ALTITUDE);
        XYSeries lat = data.getSeries(SERIES_LATITUDE);
        XYSeries lon = data.getSeries(SERIES_LONGITUDE);
        int n = alt.getItemCount();
        if (n < 3) return null;

        // Altitude in meters (the curves are stored in the unit of the application)
        double toMeters = isMeters ? 1.0 : 1.0 / FEET_PER_METER;
        double[] timeMs = new double[n], altM = new double[n];
        for (int i = 0; i < n; i++) {
            timeMs[i] = alt.getX(i).doubleValue();
            altM[i] = alt.getY(i).doubleValue() * toMeters;
        }

        // GPS samples: latitude and longitude are written together for each frame
        int m = Math.min(lat.getItemCount(), lon.getItemCount());
        double[] fixMs = new double[m], la = new double[m], lo = new double[m];
        for (int i = 0; i < m; i++) {
            fixMs[i] = lat.getX(i).doubleValue();
            la[i] = lat.getY(i).doubleValue();
            lo[i] = lon.getY(i).doubleValue();
        }

        // Flight events from the statistics calculator of the "Info" tab (same results)
        FlightViewInfoFragment.FlightStatsCalculator.FlightStats stats =
                FlightViewInfoFragment.FlightStatsCalculator.compute(timeMs, altM, true);
        Flight3DTrack.Events ev = new Flight3DTrack.Events();
        ev.liftoffIndex = stats.liftoffIndex;
        ev.burnoutIndex = stats.burnoutIndex;
        ev.apogeeIndex = stats.apogeeIndex;
        ev.touchdownIndex = stats.touchdownIndex;
        ev.groundAltitudeM = Double.isNaN(stats.groundAltitude) ? 0.0 : stats.groundAltitude;
        if (!Double.isNaN(stats.mainDeployTimeSec) && stats.liftoffIndex >= 0) {
            double tMain = timeMs[stats.liftoffIndex] + stats.mainDeployTimeSec * 1000.0;
            for (int i = Math.max(0, stats.apogeeIndex); i < n; i++) {
                if (timeMs[i] >= tMain) {
                    ev.mainDeployIndex = i;
                    break;
                }
            }
        }
        return Flight3DTrack.build(timeMs, altM, fixMs, la, lo, ev);
    }

    // ------------------------------------------------------------------------------------
    // Texts
    // ------------------------------------------------------------------------------------

    private void setupLabels() {
        labels.setLabelText(Flight3DRenderer.LABEL_PAD, getString(R.string.flight3d_pad));
        labels.setLabelText(Flight3DRenderer.LABEL_APOGEE,
                getString(R.string.flight3d_apogee) + " " + formatDistance(track.apogeeHeightM));
        if (track.mainIndex >= 0) {
            labels.setLabelText(Flight3DRenderer.LABEL_MAIN,
                    getString(R.string.flight3d_main) + " " + formatDistance(track.up[track.mainIndex]));
        }
        labels.setLabelText(Flight3DRenderer.LABEL_LANDING, getString(R.string.flight3d_landing));
        labels.setLabelText(Flight3DRenderer.LABEL_NORTH, getString(R.string.flight3d_north));
    }

    private String buildStatsText() {
        String[] compass = getResources().getStringArray(R.array.flight3d_compass);
        int sector = (int) Math.round(track.driftBearingDeg / 45.0) % 8;
        return getString(R.string.flight3d_stats,
                formatDistance(track.apogeeHeightM),
                formatDistance(track.driftM),
                compass[sector],
                String.format(Locale.getDefault(), "%.0f", track.flightDurationSec));
    }

    /** Legend with a colored bullet per phase. */
    private CharSequence buildLegend() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendLegend(sb, Flight3DRenderer.PHASE_COLORS[Flight3DTrack.PHASE_BOOST], getString(R.string.flight3d_phase_boost));
        appendLegend(sb, Flight3DRenderer.PHASE_COLORS[Flight3DTrack.PHASE_COAST], getString(R.string.flight3d_phase_coast));
        appendLegend(sb, Flight3DRenderer.PHASE_COLORS[Flight3DTrack.PHASE_DROGUE], getString(R.string.flight3d_phase_drogue));
        if (track.mainIndex >= 0) {
            appendLegend(sb, Flight3DRenderer.PHASE_COLORS[Flight3DTrack.PHASE_MAIN], getString(R.string.flight3d_phase_main));
        }
        appendLegend(sb, Flight3DRenderer.GPS_COLOR, getString(R.string.flight3d_gps_points));
        return sb;
    }

    private static void appendLegend(SpannableStringBuilder sb, int color, String label) {
        if (sb.length() > 0) sb.append("   ");
        int start = sb.length();
        sb.append("●");
        sb.setSpan(new ForegroundColorSpan(color), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(' ').append(label);
    }

    /** Distance in the unit of the application, rounded to the unit. */
    private String formatDistance(double meters) {
        if (isMeters) {
            return String.format(Locale.getDefault(), "%.0f %s", meters, getString(R.string.Meters_fview));
        }
        return String.format(Locale.getDefault(), "%.0f %s", meters * FEET_PER_METER, getString(R.string.Feet_fview));
    }

    // ------------------------------------------------------------------------------------
    // Replay
    // ------------------------------------------------------------------------------------

    private final Runnable replayStep = new Runnable() {
        @Override
        public void run() {
            if (!playing || track == null) return;
            double next = replayTimeSec + replaySpeed * REPLAY_FRAME_MS / 1000.0;
            if (next >= replayEndSec) {
                showTime(replayEndSec);
                setPlaying(false);
                return;
            }
            showTime(next);
            handler.postDelayed(this, REPLAY_FRAME_MS);
        }
    };

    private void setPlaying(boolean play) {
        playing = play;
        handler.removeCallbacks(replayStep);
        if (playButton != null) {
            playButton.setImageResource(play ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
        if (play) handler.postDelayed(replayStep, REPLAY_FRAME_MS);
    }

    /** Displays the trajectory up to the given time (seconds from liftoff). */
    private void showTime(double timeSec) {
        replayTimeSec = Math.max(replayStartSec, Math.min(replayEndSec, timeSec));
        int i = (replayTimeSec >= replayEndSec) ? track.n - 1 : track.indexAtTime(replayTimeSec);
        renderer.setReplaySample(i);
        glView.requestRender();
        double span = replayEndSec - replayStartSec;
        if (span > 0) {
            seekBar.setProgress((int) Math.round((replayTimeSec - replayStartSec) / span * SEEK_MAX));
        }
        int s = Math.min(i, track.n - 1);
        double vz = isMeters ? track.vz[s] : track.vz[s] * FEET_PER_METER;
        liveText.setText(getString(R.string.flight3d_live,
                String.format(Locale.getDefault(), "%.1f", replayTimeSec),
                formatDistance(track.up[s]),
                String.format(Locale.getDefault(), "%.0f", vz),
                getString(isMeters ? R.string.Meters_fview : R.string.Feet_fview)));
    }

    // ------------------------------------------------------------------------------------
    // Ground map
    // ------------------------------------------------------------------------------------

    private void loadGroundMap() {
        Context context = requireContext();
        // Same osmdroid configuration (user agent, tile cache) as the OpenStreetMap tab
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        mapLoader = new Flight3DMapLoader();
        mapLoader.load(context, track, renderer.getGroundCenterEast(), renderer.getGroundCenterNorth(),
                renderer.getGroundHalfSize(), new Flight3DMapLoader.Callback() {
                    @Override
                    public void onGroundMap(Bitmap bitmap, double westM, double northM, double eastM, double southM) {
                        if (renderer == null || glView == null) return;
                        renderer.setGroundMap(bitmap, westM, northM, eastM, southM);
                        glView.requestRender();
                    }

                    @Override
                    public void onGroundMapUnavailable() {
                        if (hintText != null && isAdded()) {
                            hintText.setText(getString(R.string.flight3d_hint) + "\n"
                                    + getString(R.string.flight3d_map_unavailable));
                        }
                    }
                });
    }

    // ------------------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        if (renderer != null && glView != null) glView.onResume();
    }

    @Override
    public void onPause() {
        setPlaying(false);
        if (renderer != null && glView != null) glView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (mapLoader != null) {
            mapLoader.cancel();
            mapLoader = null;
        }
        if (renderer != null) renderer.setLabelListener(null);
        renderer = null;
        glView = null;
        labels = null;
        statsText = null;
        liveText = null;
        hintText = null;
        playButton = null;
        seekBar = null;
        super.onDestroyView();
    }
}
