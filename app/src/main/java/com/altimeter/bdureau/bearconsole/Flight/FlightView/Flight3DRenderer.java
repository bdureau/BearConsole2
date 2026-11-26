package com.altimeter.bdureau.bearconsole.Flight.FlightView;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: Flight3DRenderer
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  OpenGL ES 2.0 renderer of the 3D flight view of the AltiGPS (FlightView3DFragment).
 *  Scene axes, in meters: X = east, Y = up, Z = south (so that north is "into the screen"
 *  with the default camera). Origin = launch pad.
 *
 *  Drawn objects:
 *   - the trajectory, as a thin shaded tube colored by flight phase;
 *   - its dashed shadow on the ground (horizontal drift);
 *   - vertical lines from the trajectory to the ground every 2 s (depth reading);
 *   - the GPS fixes (green points) and the events (pad, apogee, main, landing);
 *   - the ground: the OpenStreetMap snapshot of the field when available, else a grid;
 *   - during the replay, the current rocket position.
 *
 *  All geometry is built once on the CPU from the Flight3DTrack, uploaded in vertex buffer
 *  objects, and uploaded again if the GL context is lost. The replay only changes how many
 *  vertices are drawn (prefix of each buffer).
 *  The camera (orbit around a target) and the display options are written by the UI thread
 *  (volatile fields) and read by the GL thread. The screen positions of the event labels are
 *  sent back to the UI through a LabelListener after each frame.
 * ============================================================================
 */

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Flight3DRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "Flight3DRenderer";

    /** Receives, on the GL thread, the screen position of the labels after each frame. */
    public interface LabelListener {
        /**
         * @param xy      x, y in pixels for each label (LABEL_* order)
         * @param visible false when the anchor is behind the camera or does not exist
         */
        void onLabelPositions(float[] xy, boolean[] visible);
    }

    // ---- Labels anchored in the scene (order of the arrays sent to the LabelListener) ----
    public static final int LABEL_PAD = 0;
    public static final int LABEL_APOGEE = 1;
    public static final int LABEL_MAIN = 2;
    public static final int LABEL_LANDING = 3;
    public static final int LABEL_NORTH = 4;
    public static final int LABEL_COUNT = 5;

    /** Colors of the flight phases (Flight3DTrack.PHASE_*), RGBA. */
    public static final int[] PHASE_COLORS = {
            0xFF607D8B, // pad
            0xFFE53935, // boost
            0xFFFB8C00, // coast
            0xFF8E24AA, // drogue (or single parachute)
            0xFF1E88E5, // main
            0xFF607D8B  // landed
    };
    public static final int GPS_COLOR = 0xFF43A047;
    private static final int PAD_COLOR = 0xFF263238;
    private static final int LANDING_COLOR = 0xFF43A047;
    private static final int SKY_COLOR = 0xFFDFE9F3;
    private static final int GROUND_COLOR = 0xFFE3EBD9;
    private static final int GRID_COLOR = 0x8090A4AE;
    private static final int SHADOW_COLOR = 0x9937474F;
    private static final int DROP_COLOR = 0x73607D8B;

    /** Sides of the tube cross-section. */
    private static final int TUBE_SIDES = 6;
    /** Interval between two vertical drop lines, seconds. */
    private static final double DROP_INTERVAL_SEC = 2.0;
    /** Light direction used to shade the tube (normalized in the constructor). */
    private static final float[] LIGHT = {0.4f, 0.85f, 0.35f};

    private static final int FLOATS_PER_COLOR_VERTEX = 7; // x, y, z, r, g, b, a
    private static final int FLOATS_PER_TEX_VERTEX = 5;   // x, y, z, u, v

    // ---- Shaders ----
    private static final String COLOR_VS =
            "uniform mat4 uMvp;\n" +
            "uniform float uPointSize;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec4 aColor;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  gl_Position = uMvp * vec4(aPos, 1.0);\n" +
            "  gl_PointSize = uPointSize;\n" +
            "  vColor = aColor;\n" +
            "}\n";
    private static final String COLOR_FS =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "uniform float uRound;\n" +
            "uniform vec4 uTint;\n" +
            "void main() {\n" +
            "  if (uRound > 0.5) {\n" +
            "    vec2 c = gl_PointCoord - vec2(0.5);\n" +
            "    if (dot(c, c) > 0.25) discard;\n" +
            "  }\n" +
            "  gl_FragColor = vColor * uTint;\n" +
            "}\n";
    private static final String TEX_VS =
            "uniform mat4 uMvp;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec2 aUv;\n" +
            "varying vec2 vUv;\n" +
            "void main() {\n" +
            "  gl_Position = uMvp * vec4(aPos, 1.0);\n" +
            "  vUv = aUv;\n" +
            "}\n";
    private static final String TEX_FS =
            "precision mediump float;\n" +
            "varying vec2 vUv;\n" +
            "uniform sampler2D uTex;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTex, vUv);\n" +
            "}\n";

    // ---- Scene built from the track (CPU side, kept for GL context re-creation) ----
    private final Flight3DTrack track;
    private final float density;
    /** Tube: 6 * TUBE_SIDES vertices per segment between two rings. */
    private final FloatBuffer tubeBuf;
    /** Sample index (in the track) of each ring of the tube. */
    private final int[] ringSample;
    private final FloatBuffer shadowBuf;
    /** shadowPrefix[k] = number of shadow vertices before tube segment k. */
    private final int[] shadowPrefix;
    private final FloatBuffer dropBuf;
    private final int[] dropSample;
    private final FloatBuffer gpsBuf;
    private final int[] gpsSample;
    private final FloatBuffer markerBuf;
    private final int markerCount;
    private final FloatBuffer rocketBuf;
    /** Ground quad, plain color (2 triangles) and grid lines. */
    private FloatBuffer groundPlainBuf;
    private FloatBuffer gridBuf;
    private int gridVertexCount;
    /** Ground quad with texture coordinates (from the map snapshot). */
    private FloatBuffer groundTexBuf;
    /** Positions of the labels in the scene (x, y, z per LABEL_*), NaN if unused. */
    private final float[] labelAnchors = new float[LABEL_COUNT * 3];

    // ---- Scene dimensions (meters) ----
    private final float groundCenterX, groundCenterZ, groundHalf;
    private final float sceneRadius;

    // ---- GL objects (re-created with the context) ----
    private int colorProgram, texProgram;
    private int cPos, cColor, cMvp, cPointSize, cRound, cTint;
    private int tPos, tUv, tMvp, tTex;
    private final int[] vbo = new int[9];
    private static final int VBO_TUBE = 0, VBO_SHADOW = 1, VBO_DROP = 2, VBO_GPS = 3, VBO_MARKER = 4,
            VBO_GROUND = 5, VBO_GRID = 6, VBO_GROUND_TEX = 7, VBO_ROCKET = 8;
    private int groundTexture = 0;
    private boolean groundGeometryDirty = false;

    // ---- State written by the UI thread ----
    private volatile float yawDeg, pitchDeg, distance;
    private volatile float targetX, targetY, targetZ;
    private volatile int replaySample;
    private volatile boolean showDropLines = true;
    private volatile boolean showMap = true;
    /** Map snapshot waiting for upload, and the ground rectangle it covers (scene meters). */
    private volatile Bitmap pendingGroundBitmap;
    private volatile float[] pendingGroundRect;
    private Bitmap groundBitmap;

    private LabelListener labelListener;

    // ---- Matrices ----
    private final float[] proj = new float[16], view = new float[16], mvp = new float[16];
    private int viewportW = 1, viewportH = 1;
    private final float[] labelXY = new float[LABEL_COUNT * 2];
    private final boolean[] labelVisible = new boolean[LABEL_COUNT];

    /**
     * @param track   flight track (never null)
     * @param density screen density (DisplayMetrics.density), for point sizes and line widths
     */
    public Flight3DRenderer(Flight3DTrack track, float density) {
        this.track = track;
        this.density = density;

        // ---- Scene extent: ground square around the horizontal track ----
        double spanE = track.maxEast - track.minEast;
        double spanN = track.maxNorth - track.minNorth;
        double half = Math.max(200.0, Math.max(0.8 * Math.max(spanE, spanN), 0.45 * track.maxUp));
        groundCenterX = (float) ((track.minEast + track.maxEast) / 2.0);
        groundCenterZ = (float) (-(track.minNorth + track.maxNorth) / 2.0);
        groundHalf = (float) half;
        sceneRadius = (float) Math.max(half, track.maxUp);
        float tubeRadius = (float) Math.min(6.0, Math.max(0.6, sceneRadius / 300.0));

        // ---- Tube along the rendered samples between liftoff and landing ----
        int[] idx = ringIndices(track);
        ringSample = idx;
        tubeBuf = buildTube(track, idx, tubeRadius);
        shadowPrefix = new int[Math.max(1, idx.length)];
        shadowBuf = buildShadow(track, idx, tubeRadius, shadowPrefix);

        // ---- Drop lines every DROP_INTERVAL_SEC ----
        int[] drops = new int[track.n];
        int dropCount = 0;
        double nextT = 0;
        for (int i = track.liftoffIndex; i <= track.landingIndex; i++) {
            if (track.t[i] >= nextT && track.up[i] > 2.0) {
                drops[dropCount++] = i;
                nextT = track.t[i] + DROP_INTERVAL_SEC;
            }
        }
        dropSample = Arrays.copyOf(drops, dropCount);
        dropBuf = newBuffer(dropCount * 2 * FLOATS_PER_COLOR_VERTEX);
        for (int k = 0; k < dropCount; k++) {
            int i = dropSample[k];
            putColorVertex(dropBuf, (float) track.east[i], 0f, (float) -track.north[i], DROP_COLOR, 1f);
            putColorVertex(dropBuf, (float) track.east[i], (float) track.up[i], (float) -track.north[i], DROP_COLOR, 1f);
        }
        dropBuf.position(0);

        // ---- GPS fixes after liftoff, up to the landing ----
        int[] gps = new int[track.fixCount];
        int gpsCount = 0;
        for (int k = 0; k < track.fixCount; k++) {
            int s = track.fixSampleIndex[k];
            if (s >= track.liftoffIndex && s <= track.landingIndex) gps[gpsCount++] = k;
        }
        gpsSample = new int[gpsCount];
        gpsBuf = newBuffer(gpsCount * FLOATS_PER_COLOR_VERTEX);
        for (int j = 0; j < gpsCount; j++) {
            int k = gps[j];
            gpsSample[j] = track.fixSampleIndex[k];
            putColorVertex(gpsBuf, (float) track.fixEast[k], (float) track.fixUp[k], (float) -track.fixNorth[k], GPS_COLOR, 1f);
        }
        gpsBuf.position(0);

        // ---- Event markers and label anchors ----
        Arrays.fill(labelAnchors, Float.NaN);
        markerBuf = newBuffer(4 * FLOATS_PER_COLOR_VERTEX);
        int m = 0;
        m += addMarker(LABEL_PAD, track.liftoffIndex, PAD_COLOR);
        m += addMarker(LABEL_APOGEE, track.apogeeIndex, PHASE_COLORS[Flight3DTrack.PHASE_COAST]);
        if (track.mainIndex >= 0) m += addMarker(LABEL_MAIN, track.mainIndex, PHASE_COLORS[Flight3DTrack.PHASE_MAIN]);
        m += addMarker(LABEL_LANDING, track.landingIndex, LANDING_COLOR);
        markerCount = m;
        markerBuf.position(0);
        rocketBuf = newBuffer(FLOATS_PER_COLOR_VERTEX);

        // ---- Ground (plain) and grid; the map texture replaces them when available ----
        setGroundRect(groundCenterX - groundHalf, groundCenterZ - groundHalf,
                groundCenterX + groundHalf, groundCenterZ + groundHalf);

        // ---- Default camera ----
        replaySample = track.n - 1;
        resetCamera();
    }

    // ------------------------------------------------------------------------------------
    // API used by the UI thread
    // ------------------------------------------------------------------------------------

    public void setLabelListener(LabelListener l) {
        labelListener = l;
    }

    /** Ground square requested to the map snapshot: center (east, north) and half size (m). */
    public double getGroundCenterEast() { return groundCenterX; }
    public double getGroundCenterNorth() { return -groundCenterZ; }
    public double getGroundHalfSize() { return groundHalf; }

    /** Default oblique view, from the south-west. */
    public void resetCamera() {
        yawDeg = -35f;
        pitchDeg = 22f;
        distance = 2.4f * Math.max(groundHalf * 0.9f, (float) track.maxUp * 0.75f);
        targetX = groundCenterX;
        targetY = (float) track.maxUp * 0.35f;
        targetZ = groundCenterZ;
    }

    /** View from above, north up: shows the drift. */
    public void setTopView() {
        yawDeg = 0f;
        pitchDeg = 89f;
        distance = 2.6f * groundHalf;
        targetX = groundCenterX;
        targetY = 0f;
        targetZ = groundCenterZ;
    }

    /** Rotation of the camera around the target (degrees). */
    public void rotate(float dYawDeg, float dPitchDeg) {
        yawDeg = yawDeg + dYawDeg;
        pitchDeg = Math.max(3f, Math.min(89f, pitchDeg + dPitchDeg));
    }

    /** Zoom: distance divided by the scale factor of the pinch gesture. */
    public void zoom(float scaleFactor) {
        if (scaleFactor <= 0) return;
        distance = Math.max(sceneRadius * 0.15f, Math.min(sceneRadius * 12f, distance / scaleFactor));
    }

    /**
     * Pan: moves the target in the ground plane, by a screen displacement in pixels
     * (two-finger drag).
     */
    public void pan(float dxPx, float dyPx) {
        float metersPerPixel = 2f * distance * (float) Math.tan(Math.toRadians(22.5)) / Math.max(1, viewportH);
        double yaw = Math.toRadians(yawDeg);
        // Screen right = (cos yaw, 0, -sin yaw); screen "up" on the ground = (-sin yaw, 0, -cos yaw)
        float rx = (float) Math.cos(yaw), rz = (float) -Math.sin(yaw);
        float fx = (float) -Math.sin(yaw), fz = (float) -Math.cos(yaw);
        targetX = targetX - (dxPx * rx - dyPx * fx) * metersPerPixel;
        targetZ = targetZ - (dxPx * rz - dyPx * fz) * metersPerPixel;
    }

    /** Last sample drawn (replay), 0..n-1. */
    public void setReplaySample(int sampleIndex) {
        replaySample = Math.max(0, Math.min(track.n - 1, sampleIndex));
    }

    public void setShowDropLines(boolean show) { showDropLines = show; }

    public void setShowMap(boolean show) { showMap = show; }

    /**
     * Map snapshot of the field (any thread). The bitmap covers the rectangle
     * [west, north] .. [east, south] given in meters from the pad.
     */
    public void setGroundMap(Bitmap bitmap, double westM, double northM, double eastM, double southM) {
        pendingGroundRect = new float[]{(float) westM, (float) -northM, (float) eastM, (float) -southM};
        pendingGroundBitmap = bitmap;
    }

    // ------------------------------------------------------------------------------------
    // GLSurfaceView.Renderer
    // ------------------------------------------------------------------------------------

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // New context (first time or after a loss): every GL object is created again
        colorProgram = buildProgram(COLOR_VS, COLOR_FS);
        cPos = GLES20.glGetAttribLocation(colorProgram, "aPos");
        cColor = GLES20.glGetAttribLocation(colorProgram, "aColor");
        cMvp = GLES20.glGetUniformLocation(colorProgram, "uMvp");
        cPointSize = GLES20.glGetUniformLocation(colorProgram, "uPointSize");
        cRound = GLES20.glGetUniformLocation(colorProgram, "uRound");
        cTint = GLES20.glGetUniformLocation(colorProgram, "uTint");
        texProgram = buildProgram(TEX_VS, TEX_FS);
        tPos = GLES20.glGetAttribLocation(texProgram, "aPos");
        tUv = GLES20.glGetAttribLocation(texProgram, "aUv");
        tMvp = GLES20.glGetUniformLocation(texProgram, "uMvp");
        tTex = GLES20.glGetUniformLocation(texProgram, "uTex");

        GLES20.glGenBuffers(vbo.length, vbo, 0);
        upload(VBO_TUBE, tubeBuf);
        upload(VBO_SHADOW, shadowBuf);
        upload(VBO_DROP, dropBuf);
        upload(VBO_GPS, gpsBuf);
        upload(VBO_MARKER, markerBuf);
        upload(VBO_GROUND, groundPlainBuf);
        upload(VBO_GRID, gridBuf);
        upload(VBO_ROCKET, rocketBuf);
        groundTexture = 0;
        if (groundTexBuf != null) upload(VBO_GROUND_TEX, groundTexBuf);
        if (groundBitmap != null && !groundBitmap.isRecycled()) groundTexture = createTexture(groundBitmap);

        GLES20.glClearColor(red(SKY_COLOR), green(SKY_COLOR), blue(SKY_COLOR), 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // LEQUAL: a marker drawn twice at the same depth (outline, then color) stays visible
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        viewportW = Math.max(1, width);
        viewportH = Math.max(1, height);
        GLES20.glViewport(0, 0, viewportW, viewportH);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        consumePendingGround();

        // ---- Camera ----
        float yaw = (float) Math.toRadians(yawDeg), pitch = (float) Math.toRadians(pitchDeg);
        float d = distance;
        float ex = targetX + d * (float) (Math.cos(pitch) * Math.sin(yaw));
        float ey = targetY + d * (float) Math.sin(pitch);
        float ez = targetZ + d * (float) (Math.cos(pitch) * Math.cos(yaw));
        Matrix.setLookAtM(view, 0, ex, ey, ez, targetX, targetY, targetZ, 0f, 1f, 0f);
        float far = d * 4f + sceneRadius * 6f;
        Matrix.perspectiveM(proj, 0, 45f, viewportW / (float) viewportH, Math.max(0.5f, d * 0.01f), far);
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        int replay = replaySample;

        // ---- Ground ----
        boolean textured = showMap && groundTexture != 0 && groundTexBuf != null;
        if (textured) {
            GLES20.glUseProgram(texProgram);
            GLES20.glUniformMatrix4fv(tMvp, 1, false, mvp, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, groundTexture);
            GLES20.glUniform1i(tTex, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[VBO_GROUND_TEX]);
            int stride = FLOATS_PER_TEX_VERTEX * 4;
            GLES20.glEnableVertexAttribArray(tPos);
            GLES20.glVertexAttribPointer(tPos, 3, GLES20.GL_FLOAT, false, stride, 0);
            GLES20.glEnableVertexAttribArray(tUv);
            GLES20.glVertexAttribPointer(tUv, 2, GLES20.GL_FLOAT, false, stride, 12);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
            GLES20.glDisableVertexAttribArray(tPos);
            GLES20.glDisableVertexAttribArray(tUv);
        }

        GLES20.glUseProgram(colorProgram);
        GLES20.glUniformMatrix4fv(cMvp, 1, false, mvp, 0);
        GLES20.glUniform1f(cRound, 0f);
        GLES20.glUniform4f(cTint, 1f, 1f, 1f, 1f);
        GLES20.glUniform1f(cPointSize, 1f);
        GLES20.glLineWidth(Math.max(1f, density));
        if (!textured) {
            drawColor(VBO_GROUND, GLES20.GL_TRIANGLES, 6);
            // The grid lies on the ground: drawn without depth test to avoid z-fighting
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            drawColor(VBO_GRID, GLES20.GL_LINES, gridVertexCount);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }

        // ---- Number of tube segments reached by the replay ----
        int rings = upperBound(ringSample, replay);           // rings with sample <= replay
        int segments = Math.max(0, rings - 1);

        // ---- Shadow (on the ground, no depth write so that it never hides the tube) ----
        GLES20.glDepthMask(false);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        drawColor(VBO_SHADOW, GLES20.GL_TRIANGLES, shadowPrefix[Math.min(segments, shadowPrefix.length - 1)]);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (showDropLines) {
            drawColor(VBO_DROP, GLES20.GL_LINES, 2 * upperBound(dropSample, replay));
        }
        GLES20.glDepthMask(true);

        // ---- Trajectory ----
        drawColor(VBO_TUBE, GLES20.GL_TRIANGLES, segments * TUBE_SIDES * 6);

        // ---- Points: GPS fixes, events (white outline), rocket ----
        GLES20.glUniform1f(cRound, 1f);
        GLES20.glUniform1f(cPointSize, 6f * density);
        drawColor(VBO_GPS, GLES20.GL_POINTS, upperBound(gpsSample, replay));
        GLES20.glUniform1f(cPointSize, 16f * density);
        GLES20.glUniform4f(cTint, 10f, 10f, 10f, 1f); // white outline (color saturated)
        drawColor(VBO_MARKER, GLES20.GL_POINTS, markerCount);
        GLES20.glUniform4f(cTint, 1f, 1f, 1f, 1f);
        GLES20.glUniform1f(cPointSize, 11f * density);
        drawColor(VBO_MARKER, GLES20.GL_POINTS, markerCount);
        if (replay < track.landingIndex && replay >= track.liftoffIndex) {
            rocketBuf.position(0);
            putColorVertex(rocketBuf, (float) track.east[replay], (float) track.up[replay],
                    (float) -track.north[replay], 0xFF212121, 1f);
            rocketBuf.position(0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[VBO_ROCKET]);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, FLOATS_PER_COLOR_VERTEX * 4, rocketBuf);
            GLES20.glUniform1f(cPointSize, 12f * density);
            drawColor(VBO_ROCKET, GLES20.GL_POINTS, 1);
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        publishLabels();
    }

    // ------------------------------------------------------------------------------------
    // Drawing helpers
    // ------------------------------------------------------------------------------------

    private void drawColor(int vboIndex, int mode, int vertexCount) {
        if (vertexCount <= 0) return;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[vboIndex]);
        int stride = FLOATS_PER_COLOR_VERTEX * 4;
        GLES20.glEnableVertexAttribArray(cPos);
        GLES20.glVertexAttribPointer(cPos, 3, GLES20.GL_FLOAT, false, stride, 0);
        GLES20.glEnableVertexAttribArray(cColor);
        GLES20.glVertexAttribPointer(cColor, 4, GLES20.GL_FLOAT, false, stride, 12);
        GLES20.glDrawArrays(mode, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(cPos);
        GLES20.glDisableVertexAttribArray(cColor);
    }

    /** Projects the label anchors to the screen and sends them to the UI. */
    private void publishLabels() {
        LabelListener l = labelListener;
        if (l == null) return;
        float[] in = new float[4], out = new float[4];
        for (int k = 0; k < LABEL_COUNT; k++) {
            float x = labelAnchors[k * 3];
            if (Float.isNaN(x)) {
                labelVisible[k] = false;
                continue;
            }
            in[0] = x;
            in[1] = labelAnchors[k * 3 + 1];
            in[2] = labelAnchors[k * 3 + 2];
            in[3] = 1f;
            Matrix.multiplyMV(out, 0, mvp, 0, in, 0);
            if (out[3] <= 0f) {
                labelVisible[k] = false;
                continue;
            }
            float nx = out[0] / out[3], ny = out[1] / out[3];
            labelXY[k * 2] = (nx * 0.5f + 0.5f) * viewportW;
            labelXY[k * 2 + 1] = (1f - (ny * 0.5f + 0.5f)) * viewportH;
            labelVisible[k] = Math.abs(nx) <= 1.2f && Math.abs(ny) <= 1.2f;
        }
        l.onLabelPositions(labelXY.clone(), labelVisible.clone());
    }

    /** Uploads a map snapshot received from another thread (GL thread). */
    private void consumePendingGround() {
        Bitmap bmp = pendingGroundBitmap;
        float[] rect = pendingGroundRect;
        if (bmp == null || rect == null) {
            if (groundGeometryDirty) {
                upload(VBO_GROUND, groundPlainBuf);
                upload(VBO_GRID, gridBuf);
                groundGeometryDirty = false;
            }
            return;
        }
        pendingGroundBitmap = null;
        pendingGroundRect = null;
        if (groundTexture != 0) {
            GLES20.glDeleteTextures(1, new int[]{groundTexture}, 0);
        }
        groundBitmap = bmp;
        groundTexture = createTexture(bmp);
        // Textured quad: (u, v) = (0, 0) at the north-west corner of the snapshot
        float x0 = rect[0], z0 = rect[1], x1 = rect[2], z1 = rect[3];
        groundTexBuf = newBuffer(6 * FLOATS_PER_TEX_VERTEX);
        float[] q = {
                x0, 0, z0, 0, 0,   x0, 0, z1, 0, 1,   x1, 0, z1, 1, 1,
                x0, 0, z0, 0, 0,   x1, 0, z1, 1, 1,   x1, 0, z0, 1, 0};
        groundTexBuf.put(q).position(0);
        upload(VBO_GROUND_TEX, groundTexBuf);
        // The plain ground and the grid follow the same rectangle
        setGroundRect(x0, z0, x1, z1);
        upload(VBO_GROUND, groundPlainBuf);
        upload(VBO_GRID, gridBuf);
        groundGeometryDirty = false;
    }

    private int createTexture(Bitmap bmp) {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return tex[0];
    }

    private void upload(int vboIndex, FloatBuffer data) {
        if (data == null) return;
        data.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[vboIndex]);
        // At least one float, so that an empty object still gets a valid buffer
        int bytes = Math.max(4, data.capacity() * 4);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bytes, data.capacity() > 0 ? data : null,
                vboIndex == VBO_ROCKET ? GLES20.GL_DYNAMIC_DRAW : GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private static int buildProgram(String vs, String fs) {
        int v = compile(GLES20.GL_VERTEX_SHADER, vs);
        int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "Program link error: " + GLES20.glGetProgramInfoLog(p));
        }
        GLES20.glDeleteShader(v);
        GLES20.glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            Log.e(TAG, "Shader compile error: " + GLES20.glGetShaderInfoLog(s));
        }
        return s;
    }

    // ------------------------------------------------------------------------------------
    // Geometry construction (CPU)
    // ------------------------------------------------------------------------------------

    /**
     * Samples used as tube rings: the rendered samples from liftoff to landing, without the
     * points that do not move (zero-length segments have no direction).
     */
    private static int[] ringIndices(Flight3DTrack tr) {
        int[] out = new int[tr.renderIndex.length];
        int k = 0;
        double px = Double.NaN, py = 0, pz = 0;
        for (int i : tr.renderIndex) {
            if (i < tr.liftoffIndex || i > tr.landingIndex) continue;
            double x = tr.east[i], y = tr.up[i], z = -tr.north[i];
            if (k > 0 && Math.abs(x - px) + Math.abs(y - py) + Math.abs(z - pz) < 0.05 && i != tr.landingIndex) {
                continue;
            }
            out[k++] = i;
            px = x;
            py = y;
            pz = z;
        }
        return Arrays.copyOf(out, k);
    }

    /**
     * Tube made of rings of TUBE_SIDES vertices. The ring frame is carried along the curve
     * by parallel transport, so that the tube does not twist where the direction changes
     * (vertical boost, apogee turn).
     */
    private static FloatBuffer buildTube(Flight3DTrack tr, int[] idx, float radius) {
        int rings = idx.length;
        int segments = Math.max(0, rings - 1);
        FloatBuffer buf = newBuffer(segments * TUBE_SIDES * 6 * FLOATS_PER_COLOR_VERTEX);
        if (rings < 2) {
            buf.position(0);
            return buf;
        }
        float[][] ringPos = new float[rings][TUBE_SIDES * 3];
        float[][] ringCol = new float[rings][TUBE_SIDES * 4];
        float[] light = normalize(LIGHT.clone());
        float[] normal = null;
        float[] prevTangent = null;
        for (int r = 0; r < rings; r++) {
            float[] p = point(tr, idx[r]);
            float[] a = point(tr, idx[Math.max(0, r - 1)]);
            float[] b = point(tr, idx[Math.min(rings - 1, r + 1)]);
            float[] tan = normalize(new float[]{b[0] - a[0], b[1] - a[1], b[2] - a[2]});
            if (tan == null) tan = prevTangent != null ? prevTangent : new float[]{0, 1, 0};
            if (normal == null) {
                float[] ref = Math.abs(tan[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
                normal = normalize(cross(tan, ref));
            } else {
                // Parallel transport: remove from the previous normal its component along tan
                float dot = normal[0] * tan[0] + normal[1] * tan[1] + normal[2] * tan[2];
                float[] nn = normalize(new float[]{normal[0] - dot * tan[0], normal[1] - dot * tan[1], normal[2] - dot * tan[2]});
                if (nn != null) normal = nn;
            }
            float[] binormal = cross(tan, normal);
            prevTangent = tan;
            int color = PHASE_COLORS[tr.phase[idx[r]]];
            for (int j = 0; j < TUBE_SIDES; j++) {
                double th = 2 * Math.PI * j / TUBE_SIDES;
                float c = (float) Math.cos(th), s = (float) Math.sin(th);
                float nx = c * normal[0] + s * binormal[0];
                float ny = c * normal[1] + s * binormal[1];
                float nz = c * normal[2] + s * binormal[2];
                ringPos[r][j * 3] = p[0] + radius * nx;
                ringPos[r][j * 3 + 1] = p[1] + radius * ny;
                ringPos[r][j * 3 + 2] = p[2] + radius * nz;
                float shade = 0.55f + 0.45f * Math.max(0f, nx * light[0] + ny * light[1] + nz * light[2]);
                ringCol[r][j * 4] = red(color) * shade;
                ringCol[r][j * 4 + 1] = green(color) * shade;
                ringCol[r][j * 4 + 2] = blue(color) * shade;
                ringCol[r][j * 4 + 3] = 1f;
            }
        }
        for (int r = 0; r < segments; r++) {
            for (int j = 0; j < TUBE_SIDES; j++) {
                int j1 = (j + 1) % TUBE_SIDES;
                putRingVertex(buf, ringPos, ringCol, r, j);
                putRingVertex(buf, ringPos, ringCol, r + 1, j);
                putRingVertex(buf, ringPos, ringCol, r + 1, j1);
                putRingVertex(buf, ringPos, ringCol, r, j);
                putRingVertex(buf, ringPos, ringCol, r + 1, j1);
                putRingVertex(buf, ringPos, ringCol, r, j1);
            }
        }
        buf.position(0);
        return buf;
    }

    /**
     * Dashed ribbon under the trajectory, on the ground. prefix[k] receives the number of
     * vertices of the segments before segment k (replay).
     */
    private static FloatBuffer buildShadow(Flight3DTrack tr, int[] idx, float radius, int[] prefix) {
        int segments = Math.max(0, idx.length - 1);
        float[] tmp = new float[segments * 6 * FLOATS_PER_COLOR_VERTEX];
        int count = 0;
        double dash = Math.max(3.0, radius * 6.0);
        double along = 0;
        float half = radius * 0.8f;
        float y = 0.0f;
        for (int k = 0; k < segments; k++) {
            prefix[k] = count;
            int i0 = idx[k], i1 = idx[k + 1];
            float x0 = (float) tr.east[i0], z0 = (float) -tr.north[i0];
            float x1 = (float) tr.east[i1], z1 = (float) -tr.north[i1];
            double len = Math.hypot(x1 - x0, z1 - z0);
            boolean on = ((long) Math.floor(along / dash)) % 2 == 0;
            along += len;
            if (len < 1e-3 || !on) continue;
            float px = (float) (-(z1 - z0) / len) * half, pz = (float) ((x1 - x0) / len) * half;
            float[][] v = {{x0 + px, y, z0 + pz}, {x0 - px, y, z0 - pz}, {x1 - px, y, z1 - pz},
                    {x0 + px, y, z0 + pz}, {x1 - px, y, z1 - pz}, {x1 + px, y, z1 + pz}};
            for (float[] p : v) {
                int o = count * FLOATS_PER_COLOR_VERTEX;
                tmp[o] = p[0];
                tmp[o + 1] = p[1];
                tmp[o + 2] = p[2];
                tmp[o + 3] = red(SHADOW_COLOR);
                tmp[o + 4] = green(SHADOW_COLOR);
                tmp[o + 5] = blue(SHADOW_COLOR);
                tmp[o + 6] = alpha(SHADOW_COLOR);
                count++;
            }
        }
        if (prefix.length > segments) prefix[segments] = count;
        FloatBuffer buf = newBuffer(count * FLOATS_PER_COLOR_VERTEX);
        buf.put(tmp, 0, count * FLOATS_PER_COLOR_VERTEX).position(0);
        return buf;
    }

    /** Plain ground rectangle and its grid (spacing 10, 20, 50, 100... m). */
    private void setGroundRect(float x0, float z0, float x1, float z1) {
        groundPlainBuf = newBuffer(6 * FLOATS_PER_COLOR_VERTEX);
        float[][] q = {{x0, z0}, {x0, z1}, {x1, z1}, {x0, z0}, {x1, z1}, {x1, z0}};
        for (float[] p : q) putColorVertex(groundPlainBuf, p[0], 0f, p[1], GROUND_COLOR, 1f);
        groundPlainBuf.position(0);

        float size = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        float step = niceStep(size / 10f);
        int nx = (int) Math.floor(Math.abs(x1 - x0) / step) + 2;
        int nz = (int) Math.floor(Math.abs(z1 - z0) / step) + 2;
        gridBuf = newBuffer((nx + nz) * 2 * FLOATS_PER_COLOR_VERTEX);
        int v = 0;
        float minX = Math.min(x0, x1), maxX = Math.max(x0, x1), minZ = Math.min(z0, z1), maxZ = Math.max(z0, z1);
        // Lines aligned on multiples of the step from the pad, so the pad is on a line
        for (float x = (float) Math.ceil(minX / step) * step; x <= maxX && v < (nx + nz) * 2 - 1; x += step) {
            putColorVertex(gridBuf, x, 0f, minZ, GRID_COLOR, 1f);
            putColorVertex(gridBuf, x, 0f, maxZ, GRID_COLOR, 1f);
            v += 2;
        }
        for (float z = (float) Math.ceil(minZ / step) * step; z <= maxZ && v < (nx + nz) * 2 - 1; z += step) {
            putColorVertex(gridBuf, minX, 0f, z, GRID_COLOR, 1f);
            putColorVertex(gridBuf, maxX, 0f, z, GRID_COLOR, 1f);
            v += 2;
        }
        gridVertexCount = v;
        gridBuf.position(0);
        // North label at the north edge of the ground, in line with the pad
        labelAnchors[LABEL_NORTH * 3] = Math.max(minX, Math.min(maxX, 0f));
        labelAnchors[LABEL_NORTH * 3 + 1] = 0f;
        labelAnchors[LABEL_NORTH * 3 + 2] = minZ;
        groundGeometryDirty = true;
    }

    private int addMarker(int label, int sample, int color) {
        if (sample < 0 || sample >= track.n) return 0;
        float x = (float) track.east[sample], y = (float) track.up[sample], z = (float) -track.north[sample];
        putColorVertex(markerBuf, x, y, z, color, 1f);
        labelAnchors[label * 3] = x;
        labelAnchors[label * 3 + 1] = y;
        labelAnchors[label * 3 + 2] = z;
        return 1;
    }

    // ------------------------------------------------------------------------------------
    // Small math / buffer helpers
    // ------------------------------------------------------------------------------------

    /** Number of values <= v in a sorted array. */
    private static int upperBound(int[] sorted, int v) {
        int lo = 0, hi = sorted.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] <= v) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    private static float niceStep(float raw) {
        float[] steps = {5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000};
        for (float s : steps) if (s >= raw) return s;
        return 10000;
    }

    private static float[] point(Flight3DTrack tr, int i) {
        return new float[]{(float) tr.east[i], (float) tr.up[i], (float) -tr.north[i]};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** Normalized copy, or null for a (nearly) zero vector. */
    private static float[] normalize(float[] v) {
        if (v == null) return null;
        float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (l < 1e-6f) return null;
        return new float[]{v[0] / l, v[1] / l, v[2] / l};
    }

    private static void putRingVertex(FloatBuffer b, float[][] pos, float[][] col, int r, int j) {
        b.put(pos[r][j * 3]).put(pos[r][j * 3 + 1]).put(pos[r][j * 3 + 2]);
        b.put(col[r][j * 4]).put(col[r][j * 4 + 1]).put(col[r][j * 4 + 2]).put(col[r][j * 4 + 3]);
    }

    private static void putColorVertex(FloatBuffer b, float x, float y, float z, int argb, float shade) {
        b.put(x).put(y).put(z).put(red(argb) * shade).put(green(argb) * shade).put(blue(argb) * shade).put(alpha(argb));
    }

    private static FloatBuffer newBuffer(int floats) {
        return ByteBuffer.allocateDirect(Math.max(0, floats) * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    private static float alpha(int c) { return ((c >>> 24) & 0xFF) / 255f; }
    private static float red(int c) { return ((c >> 16) & 0xFF) / 255f; }
    private static float green(int c) { return ((c >> 8) & 0xFF) / 255f; }
    private static float blue(int c) { return (c & 0xFF) / 255f; }
}
