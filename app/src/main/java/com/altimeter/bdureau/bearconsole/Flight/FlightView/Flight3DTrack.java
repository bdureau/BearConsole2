package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: Flight3DTrack
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Data model of the 3D flight view of the AltiGPS (FlightView3DFragment).
 *  It merges the two position sources recorded by the altimeter:
 *   - the barometric altitude (accurate to ~1 m, one value per sample) gives the height;
 *   - the GPS fixes (accurate to a few meters horizontally, lower rate, often lost or
 *     late during the boost) give the horizontal position.
 *  The GPS altitude is deliberately not used for the height: it is typically 10-30 m off.
 *
 *  Processing:
 *   1. GPS fixes: "no fix" points (0, 0) and repeated (stale) positions are dropped, as well
 *      as fixes that would need an impossible horizontal speed (> 400 m/s) to be reached.
 *   2. The origin (launch pad) is the average of the fixes received before liftoff, or the
 *      first fix if there is none.
 *   3. Positions are converted to local east / north meters (equirectangular projection
 *      around the pad, error well below the GPS error over a few kilometers).
 *   4. The horizontal position of each altitude sample is interpolated in time between two
 *      fixes. It is pinned to the pad before liftoff and to the landing point after
 *      touchdown, so that the GPS jitter on the ground does not draw scribbles.
 *   5. Each sample gets a flight phase (boost, coast, drogue, main...), from the event
 *      indices found by the flight statistics calculator.
 *
 *  Pure Java (no Android dependency) so that it can be tested on a desktop JVM.
 * ============================================================================
 */
public final class Flight3DTrack {

    // ---- Flight phases (also used as color index by the renderer) ----
    public static final byte PHASE_PAD = 0;
    public static final byte PHASE_BOOST = 1;
    public static final byte PHASE_COAST = 2;
    public static final byte PHASE_DROGUE = 3;
    public static final byte PHASE_MAIN = 4;
    public static final byte PHASE_LANDED = 5;

    /** Mean Earth radius (m), used by the local projection. */
    private static final double EARTH_RADIUS_M = 6371008.8;
    /** Fixes that would need a higher horizontal speed to be reached are GPS errors. */
    private static final double MAX_HORIZONTAL_SPEED_MS = 400.0;
    /** Fixes received this long before liftoff are averaged to find the pad. */
    private static final double PAD_AVERAGE_WINDOW_SEC = 10.0;
    /** Half window of the regression used for the displayed vertical speed. */
    private static final double VZ_HALF_WINDOW_SEC = 0.5;
    /** Upper bound of the number of samples sent to the renderer. */
    public static final int MAX_RENDERED_SAMPLES = 3000;

    /**
     * Flight events, as indices in the altitude samples (from the flight statistics
     * calculator). -1 when the event was not found.
     */
    public static final class Events {
        public int liftoffIndex = -1;
        public int burnoutIndex = -1;
        public int apogeeIndex = -1;
        public int mainDeployIndex = -1;
        public int touchdownIndex = -1;
        /** Altitude of the ground before liftoff, in meters (subtracted from the samples). */
        public double groundAltitudeM = 0.0;
    }

    // ---- Samples (one per altitude sample) ----
    /** Number of altitude samples. */
    public final int n;
    /** Time from liftoff, in seconds (negative before liftoff). */
    public final double[] t;
    /** Local coordinates in meters: east, north, and height above the pad. */
    public final double[] east, north, up;
    /** Vertical speed in m/s (local regression of the barometric altitude). */
    public final double[] vz;
    /** Flight phase of each sample (PHASE_*). */
    public final byte[] phase;

    // ---- GPS fixes kept after filtering ----
    public final int fixCount;
    public final double[] fixEast, fixNorth, fixUp;
    /** Index of the altitude sample at (or just before) each fix: replay gating. */
    public final int[] fixSampleIndex;

    // ---- Origin and events ----
    public final double padLat, padLon;
    public final int liftoffIndex, apogeeIndex, mainIndex, landingIndex;

    // ---- Summary values (meters / seconds / degrees) ----
    public final double apogeeHeightM;
    public final double driftM;
    /** Direction of the landing point seen from the pad, 0 = north, 90 = east. */
    public final double driftBearingDeg;
    public final double flightDurationSec;

    // ---- Extent of the horizontal track, meters ----
    public final double minEast, maxEast, minNorth, maxNorth, maxUp;

    /** Indices of the samples sent to the renderer (decimated, events always included). */
    public final int[] renderIndex;

    private Flight3DTrack(Builder b) {
        n = b.n;
        t = b.t;
        east = b.east;
        north = b.north;
        up = b.up;
        vz = b.vz;
        phase = b.phase;
        fixCount = b.fixCount;
        fixEast = b.fixEast;
        fixNorth = b.fixNorth;
        fixUp = b.fixUp;
        fixSampleIndex = b.fixSampleIndex;
        padLat = b.padLat;
        padLon = b.padLon;
        liftoffIndex = b.liftoff;
        apogeeIndex = b.apogee;
        mainIndex = b.main;
        landingIndex = b.landing;
        apogeeHeightM = up[apogeeIndex];
        driftM = Math.hypot(east[landingIndex], north[landingIndex]);
        double bearing = Math.toDegrees(Math.atan2(east[landingIndex], north[landingIndex]));
        driftBearingDeg = bearing < 0 ? bearing + 360.0 : bearing;
        flightDurationSec = t[landingIndex] - t[liftoffIndex];
        double mnE = 0, mxE = 0, mnN = 0, mxN = 0, mxU = 0;
        for (int i = 0; i < n; i++) {
            mnE = Math.min(mnE, east[i]);
            mxE = Math.max(mxE, east[i]);
            mnN = Math.min(mnN, north[i]);
            mxN = Math.max(mxN, north[i]);
            mxU = Math.max(mxU, up[i]);
        }
        minEast = mnE;
        maxEast = mxE;
        minNorth = mnN;
        maxNorth = mxN;
        maxUp = mxU;
        renderIndex = b.renderIndex;
    }

    // ------------------------------------------------------------------------------------
    // Local projection
    // ------------------------------------------------------------------------------------

    /** East offset (m) of a longitude from the pad. */
    public double eastOf(double lonDeg) {
        return Math.toRadians(lonDeg - padLon) * EARTH_RADIUS_M * Math.cos(Math.toRadians(padLat));
    }

    /** North offset (m) of a latitude from the pad. */
    public double northOf(double latDeg) {
        return Math.toRadians(latDeg - padLat) * EARTH_RADIUS_M;
    }

    /** Latitude of a point at the given north offset (m) from the pad. */
    public double latitudeOf(double northM) {
        return padLat + Math.toDegrees(northM / EARTH_RADIUS_M);
    }

    /** Longitude of a point at the given east offset (m) from the pad. */
    public double longitudeOf(double eastM) {
        return padLon + Math.toDegrees(eastM / (EARTH_RADIUS_M * Math.cos(Math.toRadians(padLat))));
    }

    /** Index of the last sample whose time is <= the given time from liftoff. */
    public int indexAtTime(double timeSec) {
        int lo = 0, hi = n - 1;
        if (timeSec <= t[0]) return 0;
        if (timeSec >= t[hi]) return hi;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (t[mid] <= timeSec) lo = mid; else hi = mid;
        }
        return lo;
    }

    // ------------------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------------------

    /**
     * Builds the track.
     *
     * @param timeMs    time of the altitude samples (ms), increasing
     * @param altitudeM barometric altitude of the samples, in meters
     * @param fixTimeMs time of the GPS samples (ms), increasing
     * @param latDeg    GPS latitude of the GPS samples (decimal degrees, 0 = no fix)
     * @param lonDeg    GPS longitude of the GPS samples (decimal degrees, 0 = no fix)
     * @param events    flight events found by the statistics calculator
     * @return the track, or null if there are not enough altitude samples or GPS fixes
     */
    public static Flight3DTrack build(double[] timeMs, double[] altitudeM,
                                      double[] fixTimeMs, double[] latDeg, double[] lonDeg,
                                      Events events) {
        Builder b = new Builder();
        return b.build(timeMs, altitudeM, fixTimeMs, latDeg, lonDeg, events) ? new Flight3DTrack(b) : null;
    }

    /** Mutable state used while building the track. */
    private static final class Builder {
        int n;
        double[] t, east, north, up, vz;
        byte[] phase;
        int fixCount;
        double[] fixT, fixE, fixN;
        double[] fixEast, fixNorth, fixUp;
        int[] fixSampleIndex;
        double padLat, padLon;
        int liftoff, apogee, main, landing, burnout;
        int[] renderIndex;

        boolean build(double[] timeMs, double[] altM, double[] fixTimeMs, double[] lat, double[] lon,
                      Events ev) {
            n = Math.min(timeMs.length, altM.length);
            if (n < 3 || ev == null) return false;

            // ---- Events (with safe defaults) ----
            liftoff = clamp(ev.liftoffIndex >= 0 ? ev.liftoffIndex : 0, 0, n - 1);
            apogee = ev.apogeeIndex;
            if (apogee < liftoff || apogee >= n) {
                apogee = liftoff;
                for (int i = liftoff; i < n; i++) if (altM[i] > altM[apogee]) apogee = i;
            }
            burnout = (ev.burnoutIndex > liftoff && ev.burnoutIndex <= apogee) ? ev.burnoutIndex : -1;
            main = (ev.mainDeployIndex > apogee && ev.mainDeployIndex < n) ? ev.mainDeployIndex : -1;
            landing = (ev.touchdownIndex > apogee && ev.touchdownIndex < n) ? ev.touchdownIndex : n - 1;

            // ---- Time from liftoff and height above the pad ----
            t = new double[n];
            up = new double[n];
            double t0 = timeMs[liftoff];
            for (int i = 0; i < n; i++) {
                t[i] = (timeMs[i] - t0) / 1000.0;
                up[i] = Math.max(0.0, altM[i] - ev.groundAltitudeM);
            }
            // Nothing below the pad before liftoff and after touchdown
            for (int i = 0; i < liftoff; i++) up[i] = 0.0;
            for (int i = landing; i < n; i++) up[i] = 0.0;

            if (!filterFixes(fixTimeMs, lat, lon, t0)) return false;

            // ---- Horizontal position of every sample ----
            east = new double[n];
            north = new double[n];
            for (int i = 0; i < n; i++) {
                if (i < liftoff) continue; // on the pad
                double ts = (i >= landing) ? t[landing] : t[i];
                interpolateFix(ts, i);
            }

            // ---- GPS fixes as 3D points (height taken from the barometric curve) ----
            fixEast = new double[fixCount];
            fixNorth = new double[fixCount];
            fixUp = new double[fixCount];
            fixSampleIndex = new int[fixCount];
            for (int k = 0; k < fixCount; k++) {
                int i = indexAt(fixT[k]);
                fixSampleIndex[k] = i;
                fixEast[k] = fixE[k];
                fixNorth[k] = fixN[k];
                fixUp[k] = heightAt(fixT[k], i);
            }

            computePhases();
            computeVerticalSpeed();
            computeRenderIndex();
            return true;
        }

        /**
         * Keeps the valid, new, physically possible fixes, finds the pad and converts the
         * fixes to local meters. Fix times are converted to seconds from liftoff.
         */
        private boolean filterFixes(double[] fixTimeMs, double[] lat, double[] lon, double t0Ms) {
            int m = Math.min(fixTimeMs.length, Math.min(lat.length, lon.length));
            double[] ft = new double[m], fla = new double[m], flo = new double[m];
            int k = 0;
            double prevLat = Double.NaN, prevLon = Double.NaN;
            for (int i = 0; i < m; i++) {
                double la = lat[i], lo = lon[i];
                boolean noFix = Math.abs(la) < 1e-7 && Math.abs(lo) < 1e-7;
                if (noFix || Double.isNaN(la) || Double.isNaN(lo) || Math.abs(la) > 90 || Math.abs(lo) > 180)
                    continue;
                // A repeated position is the last fix sent again (stale): keep the first one only
                if (la == prevLat && lo == prevLon) continue;
                prevLat = la;
                prevLon = lo;
                ft[k] = (fixTimeMs[i] - t0Ms) / 1000.0;
                fla[k] = la;
                flo[k] = lo;
                k++;
            }
            if (k < 2) return false;

            // Pad: average of the fixes received shortly before liftoff, else the first fix
            double sLat = 0, sLon = 0;
            int cnt = 0;
            for (int i = 0; i < k; i++) {
                if (ft[i] <= 0 && ft[i] >= -PAD_AVERAGE_WINDOW_SEC) {
                    sLat += fla[i];
                    sLon += flo[i];
                    cnt++;
                }
            }
            if (cnt > 0) {
                padLat = sLat / cnt;
                padLon = sLon / cnt;
            } else {
                padLat = fla[0];
                padLon = flo[0];
            }

            // Local coordinates, then removal of the impossible jumps
            double cosLat = Math.cos(Math.toRadians(padLat));
            fixT = new double[k];
            fixE = new double[k];
            fixN = new double[k];
            int kept = 0;
            for (int i = 0; i < k; i++) {
                double e = Math.toRadians(flo[i] - padLon) * EARTH_RADIUS_M * cosLat;
                double no = Math.toRadians(fla[i] - padLat) * EARTH_RADIUS_M;
                if (kept > 0) {
                    double dt = ft[i] - fixT[kept - 1];
                    if (dt <= 0) continue;
                    double d = Math.hypot(e - fixE[kept - 1], no - fixN[kept - 1]);
                    if (d / dt > MAX_HORIZONTAL_SPEED_MS) continue;
                }
                fixT[kept] = ft[i];
                fixE[kept] = e;
                fixN[kept] = no;
                kept++;
            }
            fixCount = kept;
            return kept >= 2;
        }

        /** Horizontal position at time ts (seconds), written in east[i] / north[i]. */
        private void interpolateFix(double ts, int i) {
            if (ts <= fixT[0]) {
                // Before the first fix: between the pad and the first fix
                if (fixT[0] > 0 && ts >= 0) {
                    double f = ts / fixT[0];
                    east[i] = fixE[0] * f;
                    north[i] = fixN[0] * f;
                } else {
                    east[i] = ts < 0 ? 0 : fixE[0];
                    north[i] = ts < 0 ? 0 : fixN[0];
                }
                return;
            }
            int last = fixCount - 1;
            if (ts >= fixT[last]) {
                east[i] = fixE[last];
                north[i] = fixN[last];
                return;
            }
            int lo = 0, hi = last;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (fixT[mid] <= ts) lo = mid; else hi = mid;
            }
            double f = (ts - fixT[lo]) / (fixT[hi] - fixT[lo]);
            east[i] = fixE[lo] + f * (fixE[hi] - fixE[lo]);
            north[i] = fixN[lo] + f * (fixN[hi] - fixN[lo]);
        }

        private int indexAt(double ts) {
            if (ts <= t[0]) return 0;
            if (ts >= t[n - 1]) return n - 1;
            int lo = 0, hi = n - 1;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (t[mid] <= ts) lo = mid; else hi = mid;
            }
            return lo;
        }

        /** Height at time ts, interpolated between sample i and i + 1. */
        private double heightAt(double ts, int i) {
            if (i >= n - 1 || t[i + 1] <= t[i]) return up[i];
            double f = Math.max(0, Math.min(1, (ts - t[i]) / (t[i + 1] - t[i])));
            return up[i] + f * (up[i + 1] - up[i]);
        }

        private void computePhases() {
            phase = new byte[n];
            for (int i = 0; i < n; i++) {
                byte p;
                if (i < liftoff) p = PHASE_PAD;
                else if (i >= landing) p = PHASE_LANDED;
                else if (i <= apogee) p = (burnout >= 0 && i > burnout) ? PHASE_COAST
                        : (burnout >= 0 ? PHASE_BOOST : PHASE_COAST);
                else if (main >= 0 && i >= main) p = PHASE_MAIN;
                else p = PHASE_DROGUE;
                phase[i] = p;
            }
            // Without a burnout instant, the first samples of the ascent are still the boost
            if (burnout < 0) {
                for (int i = liftoff; i <= apogee && t[i] - t[liftoff] < 0.5; i++) phase[i] = PHASE_BOOST;
            }
        }

        /** Vertical speed: slope of a least-squares line over +/- VZ_HALF_WINDOW_SEC. */
        private void computeVerticalSpeed() {
            vz = new double[n];
            int lo = 0, hi = 0;
            for (int i = 0; i < n; i++) {
                while (t[i] - t[lo] > VZ_HALF_WINDOW_SEC) lo++;
                if (hi < i) hi = i;
                while (hi + 1 < n && t[hi + 1] - t[i] <= VZ_HALF_WINDOW_SEC) hi++;
                int cnt = hi - lo + 1;
                if (cnt < 2) {
                    vz[i] = 0;
                    continue;
                }
                double st = 0, sy = 0, stt = 0, sty = 0;
                for (int k = lo; k <= hi; k++) {
                    double u = t[k] - t[i];
                    st += u;
                    sy += up[k];
                    stt += u * u;
                    sty += u * up[k];
                }
                double den = cnt * stt - st * st;
                vz[i] = den > 1e-12 ? (cnt * sty - st * sy) / den : 0;
            }
        }

        /** Decimated list of samples for the renderer; the event samples are always kept. */
        private void computeRenderIndex() {
            int step = Math.max(1, (int) Math.ceil(n / (double) MAX_RENDERED_SAMPLES));
            int[] tmp = new int[n];
            int k = 0;
            for (int i = 0; i < n; i++) {
                boolean event = i == liftoff || i == apogee || i == main || i == landing || i == burnout
                        || i == n - 1 || (i > 0 && phase[i] != phase[i - 1]);
                if (event || i % step == 0) tmp[k++] = i;
            }
            renderIndex = java.util.Arrays.copyOf(tmp, k);
        }

        private static int clamp(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}
