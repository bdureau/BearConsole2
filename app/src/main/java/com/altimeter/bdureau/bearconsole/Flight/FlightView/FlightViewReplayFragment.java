package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlightViewReplayFragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  "Replay" tab of the flight view for the altimeters with an accelerometer (AccelBoards.
 *  hasFlightAccel()): the flight is played again in 3D with the Processing rocket of the
 *  status screen (utils.Rocket, replay mode), like the replay of the MotorGimbalConsole.
 *   - The rocket moves along a graduated altitude scale: barometric altitude of the flight,
 *     interpolated at the replay time, with the highest altitude reached so far.
 *   - Flight phase from the events detected on the altitude curve (FlightEvents, the same
 *     instants as the curves and the Info tab): on the pad, powered flight (flame), coasting,
 *     descent under the drogue then the main parachute, landed.
 *   - HUD: time since liftoff, altitude, vertical speed (computed from the altitude), and the
 *     acceleration measured by the accelerometer (norm of the 3 axes, in g; the high g ADXL375
 *     is used when the low g sensor is near its 16 g limit).
 *   - Replay bar: play / pause, time slider, speed x1 / x2 / x5 / x10.
 *  Physics: these altimeters record an accelerometer only, no gyroscope. In flight the
 *  accelerometer measures the thrust then the drag, both along the body: it gives no
 *  attitude and no roll, so the rocket is drawn vertical, without invented rotation (a note
 *  says so under the scene). The checkered body and the white fin are there for the status
 *  screen, where the tilt on the pad is measured.
 *  The fragment takes its data from the host FlightViewTabActivity (no data given by a
 *  constructor), so it is re-created correctly by the framework.
 * ============================================================================
 */

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Flight.FlightViewTabActivity;
import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.altimeter.bdureau.bearconsole.utils.AccelBoards;
import com.altimeter.bdureau.bearconsole.utils.Rocket;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.util.List;
import java.util.Locale;

import processing.android.PFragment;

public class FlightViewReplayFragment extends Fragment {

    private static final int SEEK_MAX = 1000;
    private static final long FRAME_MS = 33;                 // ~30 images per second
    private static final int[] SPEEDS = {1, 2, 5, 10};
    /** Above this measured value (g) the low g sensor (+/-16 g) is replaced by the ADXL375. */
    private static final double LOW_G_LIMIT = 14.0;
    /** Index of the computed vertical speed in the flight series (see FlightData). */
    private static final int SPEED_SERIES = 3;

    private ConsoleApplication myBT;
    private boolean isMeters = true;

    // Flight data
    private XYSeries altitude, speed;
    private XYSeries[] lowG, highG;          // X Y Z of each sensor, null if absent
    private double[] peakAltitude;           // highest altitude up to each sample
    private double startMs, endMs, liftoffMs;
    private double burnoutMs = Double.NaN, apogeeMs = Double.NaN, mainMs = Double.NaN,
            landingMs = Double.NaN;
    private double maxAltitude = 1;
    private String[] scaleLabels;

    // Replay
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean playing = false;
    private double timeMs;
    private int speedIndex = 0;

    // Views
    private Rocket sketch;
    private PFragment sketchFragment;
    private ImageButton playButton;
    private SeekBar seekBar;
    private Button speedButton;

    /** Required by the framework to re-create the fragment. */
    public FlightViewReplayFragment() {
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
        View view = inflater.inflate(R.layout.fragment_flight_view_replay, container, false);
        playButton = view.findViewById(R.id.replayPlay);
        seekBar = view.findViewById(R.id.replaySeek);
        speedButton = view.findViewById(R.id.replaySpeed);
        TextView note = view.findViewById(R.id.replayNote);

        isMeters = myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS;
        if (!loadFlight()) {
            note.setText(R.string.replay_no_data);
            view.findViewById(R.id.replayBar).setVisibility(View.GONE);
            return view;
        }
        timeMs = startMs;

        seekBar.setMax(SEEK_MAX);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;
                setPlaying(false);
                showTime(startMs + (endMs - startMs) * progress / SEEK_MAX);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
        playButton.setOnClickListener(v -> {
            if (!playing && timeMs >= endMs) {
                timeMs = startMs; // replay from the start
            }
            setPlaying(!playing);
        });
        speedButton.setOnClickListener(v -> {
            speedIndex = (speedIndex + 1) % SPEEDS.length;
            speedButton.setText("x" + SPEEDS[speedIndex]);
        });

        // The sketch is created with the size of its container, once it is laid out
        View host = view.findViewById(R.id.replayContainer);
        host.post(() -> createSketch(host.getWidth(), host.getHeight()));
        return view;
    }

    private void createSketch(int width, int height) {
        if (!isAdded() || sketch != null || width <= 0 || height <= 0) return;
        sketch = new Rocket(width, height);
        sketchFragment = new PFragment(sketch);
        getChildFragmentManager().beginTransaction().add(R.id.replayContainer, sketchFragment).commit();
        showTime(timeMs);
    }

    // ------------------------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------------------------

    /** Reads the displayed flight from the host activity; false if there is no altitude. */
    private boolean loadFlight() {
        if (!(getActivity() instanceof FlightViewTabActivity)) return false;
        XYSeriesCollection data = ((FlightViewTabActivity) getActivity()).getAllFlightData();
        if (data == null || data.getSeriesCount() == 0) return false;
        altitude = data.getSeries(0);
        if (altitude.getItemCount() < 3) return false;
        speed = data.getSeriesCount() > SPEED_SERIES ? data.getSeries(SPEED_SERIES) : null;
        lowG = findAxes(data, "accel345x", "accel345y", "accel345z");
        if (lowG == null) lowG = findAxes(data, "accelX", "accelY", "accelZ");
        highG = findAxes(data, "accel375x", "accel375y", "accel375z");

        int n = altitude.getItemCount();
        startMs = altitude.getX(0).doubleValue();
        endMs = altitude.getX(n - 1).doubleValue();
        peakAltitude = new double[n];
        double peak = 0;
        for (int i = 0; i < n; i++) {
            peak = Math.max(peak, altitude.getY(i).doubleValue());
            peakAltitude[i] = peak;
        }
        maxAltitude = Math.max(1, peak);

        // Events: the same instants as the curves and the Info tab
        liftoffMs = startMs;
        List<FlightEvents.Event> events = FlightEvents.compute(altitude, isMeters);
        for (FlightEvents.Event ev : events) {
            liftoffMs = ev.timeMs - ev.sinceLiftoffSec * 1000.0;
            switch (ev.type) {
                case BURNOUT: burnoutMs = ev.timeMs; break;
                case APOGEE: apogeeMs = ev.timeMs; break;
                case MAIN: mainMs = ev.timeMs; break;
                case LANDING: landingMs = ev.timeMs; break;
            }
        }

        // Graduations of the altitude scale
        scaleLabels = new String[5];
        for (int i = 0; i <= 4; i++) {
            scaleLabels[i] = String.format(Locale.US, "%.0f %s", maxAltitude * i / 4, altUnit());
        }
        return true;
    }

    @Nullable
    private static XYSeries[] findAxes(XYSeriesCollection data, String x, String y, String z) {
        XYSeries sx = find(data, x), sy = find(data, y), sz = find(data, z);
        return (sx != null && sy != null && sz != null && sx.getItemCount() > 0) ? new XYSeries[]{sx, sy, sz} : null;
    }

    /** Series with this name, or null (AFreeChart's getSeries(key) throws when absent). */
    @Nullable
    private static XYSeries find(XYSeriesCollection data, String key) {
        for (int i = 0; i < data.getSeriesCount(); i++) {
            if (key.equals(String.valueOf(data.getSeriesKey(i)))) return data.getSeries(i);
        }
        return null;
    }

    /** Index of the last sample at or before t (series sorted by time). */
    private static int indexAt(XYSeries s, double t) {
        int lo = 0, hi = s.getItemCount() - 1;
        if (t <= s.getX(0).doubleValue()) return 0;
        if (t >= s.getX(hi).doubleValue()) return hi;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (s.getX(mid).doubleValue() <= t) lo = mid; else hi = mid;
        }
        return lo;
    }

    /** Value of the series at t, linearly interpolated between the two closest samples. */
    private static double valueAt(XYSeries s, double t) {
        int i = indexAt(s, t);
        if (i >= s.getItemCount() - 1) return s.getY(i).doubleValue();
        double t0 = s.getX(i).doubleValue(), t1 = s.getX(i + 1).doubleValue();
        double v0 = s.getY(i).doubleValue(), v1 = s.getY(i + 1).doubleValue();
        if (t1 <= t0 || t <= t0) return v0;
        return v0 + (v1 - v0) * Math.min(1, (t - t0) / (t1 - t0));
    }

    /** Norm of the measured acceleration (g) at t, NaN without accelerometer curves. */
    private double measuredAccelG(double t) {
        double low = lowG != null ? normG(lowG, t) : Double.NaN;
        if (highG != null && (lowG == null || low > LOW_G_LIMIT)) {
            return normG(highG, t);
        }
        return low;
    }

    private static double normG(XYSeries[] axes, double t) {
        double x = valueAt(axes[0], t), y = valueAt(axes[1], t), z = valueAt(axes[2], t);
        // curves in m/s^2 (see ConsoleApplication.decodeFlightData)
        return Math.sqrt(x * x + y * y + z * z) / AccelBoards.STANDARD_GRAVITY;
    }

    private int phaseAt(double t) {
        if (t < liftoffMs || Double.isNaN(apogeeMs)) return Rocket.PHASE_PAD;
        if (!Double.isNaN(landingMs) && t >= landingMs) return Rocket.PHASE_LANDED;
        if (t >= apogeeMs) {
            return (!Double.isNaN(mainMs) && t >= mainMs) ? Rocket.PHASE_MAIN : Rocket.PHASE_DROGUE;
        }
        if (!Double.isNaN(burnoutMs) && t < burnoutMs) return Rocket.PHASE_BOOST;
        return Rocket.PHASE_COAST;
    }

    private String phaseLabel(int phase) {
        switch (phase) {
            case Rocket.PHASE_BOOST: return getString(R.string.replay_phase_boost);
            case Rocket.PHASE_COAST: return getString(R.string.replay_phase_coast);
            case Rocket.PHASE_DROGUE: return getString(R.string.replay_phase_drogue);
            case Rocket.PHASE_MAIN: return getString(R.string.replay_phase_main);
            case Rocket.PHASE_LANDED: return getString(R.string.replay_phase_landed);
            case Rocket.PHASE_PAD:
            default: return getString(R.string.replay_phase_pad);
        }
    }

    private String altUnit() {
        return isMeters ? "m" : "ft";
    }

    // ------------------------------------------------------------------------------------
    // Replay
    // ------------------------------------------------------------------------------------

    /** Shows the flight at time t (ms of the recording): sketch, HUD and slider. */
    private void showTime(double t) {
        timeMs = Math.max(startMs, Math.min(endMs, t));
        if (seekBar != null && endMs > startMs) {
            seekBar.setProgress((int) Math.round(SEEK_MAX * (timeMs - startMs) / (endMs - startMs)));
        }
        if (sketch == null || !isAdded()) return;

        double alt = valueAt(altitude, timeMs);
        double peak = peakAltitude[indexAt(altitude, timeMs)];
        int phase = phaseAt(timeMs);
        double sinceLiftoff = (timeMs - liftoffMs) / 1000.0;

        String l1 = String.format(Locale.US, "T%s%.1f s", sinceLiftoff < 0 ? "" : "+", sinceLiftoff);
        String r1 = String.format(Locale.US, "%s %.0f %s", getString(R.string.replay_altitude), alt, altUnit());
        String l2 = speed != null && speed.getItemCount() > 0
                ? String.format(Locale.US, "%s %+.0f %s/s", getString(R.string.replay_speed),
                valueAt(speed, timeMs), altUnit())
                : "";
        double g = measuredAccelG(timeMs);
        String r2 = Double.isNaN(g) ? ""
                : String.format(Locale.US, "%s %.1f g", getString(R.string.replay_accel), g);

        sketch.setReplayState((float) (Math.max(0, alt) / maxAltitude), (float) (peak / maxAltitude),
                phase, scaleLabels, l1, r1, l2, r2, phaseLabel(phase));
    }

    private final Runnable replayStep = new Runnable() {
        @Override
        public void run() {
            if (!playing) return;
            double next = timeMs + SPEEDS[speedIndex] * FRAME_MS;
            if (next >= endMs) {
                showTime(endMs);
                setPlaying(false);
                return;
            }
            showTime(next);
            handler.postDelayed(this, FRAME_MS);
        }
    };

    private void setPlaying(boolean play) {
        playing = play;
        handler.removeCallbacks(replayStep);
        if (playButton != null) {
            playButton.setImageResource(play ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
        if (play) handler.postDelayed(replayStep, FRAME_MS);
    }

    // ------------------------------------------------------------------------------------
    // Lifecycle (same handling of the Processing sketch as the status screen)
    // ------------------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        if (sketchFragment != null) {
            sketchFragment.onResume();
            // Only way to redraw the sketch after leaving the tab (same as the gimbal console)
            getChildFragmentManager().beginTransaction().replace(R.id.replayContainer, sketchFragment).commit();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        setPlaying(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (sketchFragment != null) sketchFragment.onStop();
        if (sketch != null) sketch.onStop();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(replayStep);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sketchFragment != null) sketchFragment.onDestroy();
        if (sketch != null) sketch.onDestroy();
    }
}
