package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlightEvents
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Flight events displayed on the flight curves (MPAndroidChart and AFreeChart tabs):
 *  burnout, apogee, main parachute deployment and landing. The liftoff is not drawn (it is at
 *  the start of the curve); it is the reference of the "T+" times of the other events.
 *  The events are not recorded by the altimeters: they are detected on the altitude curve
 *  by the statistics calculator of the "Info" tab (FlightViewInfoFragment.
 *  FlightStatsCalculator), so the curves, the Info tab and the 3D view always show the
 *  same instants. Each event carries its time on the X axis of the curves (milliseconds
 *  since the start of the recording, as stored in the flight series), the altitude of the
 *  rocket at that instant and its time relative to liftoff.
 *  An event that cannot be determined (e.g. no main deployment detected, recording stopped
 *  before landing) is simply not returned.
 * ============================================================================
 */

import android.content.Context;
import android.graphics.Color;

import com.altimeter.bdureau.bearconsole.R;

import org.afree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FlightEvents {

    /** Kind of flight event, in chronological order. */
    public enum Type {
        BURNOUT(R.string.curve_event_burnout, Color.rgb(239, 108, 0)),
        APOGEE(R.string.curve_event_apogee, Color.rgb(198, 40, 40)),
        MAIN(R.string.curve_event_main, Color.rgb(21, 101, 192)),
        LANDING(R.string.curve_event_landing, Color.rgb(106, 27, 154));

        /** Name of the event displayed next to its marker. */
        public final int labelRes;
        /** Color of the marker (same color on both chart libraries). */
        public final int color;

        Type(int labelRes, int color) {
            this.labelRes = labelRes;
            this.color = color;
        }
    }

    /** One detected event. */
    public static final class Event {
        public final Type type;
        /** Position on the X axis of the curves: milliseconds since the start of the recording. */
        public final double timeMs;
        /** Seconds since liftoff. */
        public final double sinceLiftoffSec;
        /** Altitude of the rocket at the event, in the altitude unit of the application. */
        public final double altitude;

        Event(Type type, double timeMs, double sinceLiftoffSec, double altitude) {
            this.type = type;
            this.timeMs = timeMs;
            this.sinceLiftoffSec = sinceLiftoffSec;
            this.altitude = altitude;
        }

        /**
         * Text displayed next to the marker: event name, time since liftoff and, for the
         * apogee and the main deployment, the altitude (e.g. "Apogee T+8.4 s 312 m").
         */
        public String label(Context context, String altitudeUnit) {
            StringBuilder sb = new StringBuilder(context.getString(type.labelRes));
            sb.append(String.format(Locale.US, " T+%.1f s", sinceLiftoffSec));
            if (type == Type.APOGEE || type == Type.MAIN) {
                sb.append(String.format(Locale.US, " %.0f %s", altitude, altitudeUnit));
            }
            return sb.toString();
        }
    }

    private FlightEvents() {
    }

    /**
     * Detects the events of a flight.
     *
     * @param altitudeSeries altitude curve of the flight (series 0 of the flight data: X in
     *                       milliseconds, Y in the altitude unit of the application)
     * @param isMeters       true if the altitudes are in meters, false if in feet
     * @return the events in chronological order (empty if the curve is too short)
     */
    public static List<Event> compute(XYSeries altitudeSeries, boolean isMeters) {
        if (altitudeSeries == null || altitudeSeries.getItemCount() < 3) {
            return Collections.emptyList();
        }
        int n = altitudeSeries.getItemCount();
        double[] timeMs = new double[n];
        double[] altitude = new double[n];
        for (int i = 0; i < n; i++) {
            timeMs[i] = altitudeSeries.getX(i).doubleValue();
            altitude[i] = altitudeSeries.getY(i).doubleValue();
        }

        FlightViewInfoFragment.FlightStatsCalculator.FlightStats s =
                FlightViewInfoFragment.FlightStatsCalculator.compute(timeMs, altitude, isMeters);
        List<Event> events = new ArrayList<>(4);
        if (s.liftoffIndex < 0 || s.liftoffIndex >= n) {
            return events;
        }
        double liftoffMs = timeMs[s.liftoffIndex];

        // The rocket has not flown (no apogee above the pad): no event to display
        if (s.apogeeIndex <= s.liftoffIndex) {
            return events;
        }

        if (s.burnoutIndex > s.liftoffIndex && s.burnoutIndex < s.apogeeIndex) {
            addAtIndex(events, Type.BURNOUT, s.burnoutIndex, timeMs, altitude, liftoffMs);
        }
        // The apogee instant is where the smoothed vertical speed crosses zero; its altitude is
        // the (median filtered) apogee altitude of the Info tab
        events.add(new Event(Type.APOGEE, timeMs[s.apogeeIndex],
                (timeMs[s.apogeeIndex] - liftoffMs) / 1000.0,
                Double.isNaN(s.apogeeAltitude) ? altitude[s.apogeeIndex] : s.apogeeAltitude));
        // Main deployment: breakpoint between the drogue and main descent rates
        if (!Double.isNaN(s.mainDeployTimeSec)) {
            events.add(new Event(Type.MAIN, liftoffMs + s.mainDeployTimeSec * 1000.0,
                    s.mainDeployTimeSec, s.mainDeployAltitude));
        }
        // Landing only if the recording really reached the ground
        if (s.touchdownRecorded && s.touchdownIndex > s.apogeeIndex) {
            addAtIndex(events, Type.LANDING, s.touchdownIndex, timeMs, altitude, liftoffMs);
        }
        return events;
    }

    private static void addAtIndex(List<Event> events, Type type, int index,
                                   double[] timeMs, double[] altitude, double liftoffMs) {
        events.add(new Event(type, timeMs[index], (timeMs[index] - liftoffMs) / 1000.0,
                altitude[index]));
    }
}
