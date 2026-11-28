package com.altimeter.bdureau.bearconsole.utils;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: Rocket
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Processing sketch drawing the live attitude of the rocket on the status screen of the
 *  accelerometer boards. Same renderer as the MotorGimbalConsole application: shaded rocket
 *  with a checkerboard body (roll reference), red nose cone and fins, nozzle, lights and a
 *  telemetry HUD at the top of the view.
 *  Accelerometer mode (setAccelAttitude()): the rocket stands on a ground ring, nose up, and
 *  is tilted in the direction measured by the accelerometer: on the pad the accelerometer only
 *  measures the 1 g reaction to gravity, so the angle between this vector and the long axis
 *  of the rocket is the tilt. The roll around the long axis cannot be measured by an
 *  accelerometer and is not drawn. The displayed direction is smoothed at each frame.
 *  The HUD shows the tilt from the vertical, the angle on each of the two board axes
 *  perpendicular to the body and the total acceleration; when the acceleration is far from
 *  1 g (rocket moving) the attitude is not meaningful and the HUD says so.
 *  The quaternion / Euler inputs of the gimbal console are kept (setInputString()).
 *  The body is checkered red and white (Tintin's moon rocket, generated texture) with one
 *  white fin, so that a rotation around the long axis is clearly seen.
 *  The sketch is created with the size of the view that shows it (Rocket(width, height)):
 *  the scene and the HUD are scaled to it, nothing is cropped.
 *  Needs the processing-core.jar library and the CourierNew36.vlw font of the assets
 *  (pattern.png is no longer used).
 * ============================================================================
 */

import android.annotation.SuppressLint;
import android.util.Log;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

public class Rocket extends PApplet {

    // Attitude variables of the gimbal (quaternion) mode, in radians
    private final float[] q = new float[4];
    private final float[] Euler = new float[3];     // Euler[0]=X, Euler[1]=Y, Euler[2]=Z

    private long currentTime = 0;
    private float correct = 0;
    private boolean useQuaternion = true;

    // ---------------------------------------------------------------------------------
    // Accelerometer mode
    // ---------------------------------------------------------------------------------
    private volatile boolean accelMode = false;
    // Measured direction of the nose in the ground frame (X = board axis b, Y = board axis c,
    // Z = up), unit vector, and the displayed (smoothed) direction
    private volatile float targetX = 0, targetY = 0, targetZ = 1;
    private float shownX = 0, shownY = 0, shownZ = 1;
    private volatile float tiltDeg = 0, angleB = 0, angleC = 0, totalG = 0;
    private volatile boolean attitudeValid = false;
    private volatile String axisNameB = "X", axisNameC = "Y";
    // HUD texts (localized by the fragment)
    private volatile String labelTilt = "Tilt", labelTotal = "Total", labelMoving = "Moving",
            labelWaiting = "Waiting";
    private volatile boolean hasAccelData = false;

    // ---------------------------------------------------------------------------------
    // Flight replay mode (trajectory)
    // ---------------------------------------------------------------------------------
    /** Flight phases of the replay (setReplayState()). */
    public static final int PHASE_PAD = 0, PHASE_BOOST = 1, PHASE_COAST = 2, PHASE_DROGUE = 3,
            PHASE_MAIN = 4, PHASE_LANDED = 5;
    private volatile boolean replayMode = false;
    private volatile float replayAltFrac = 0;     // altitude / maximum altitude (0..1)
    private volatile float replayPeakFrac = 0;    // highest altitude reached so far (0..1)
    private volatile int replayPhase = PHASE_PAD;
    private volatile String[] replayScale = {"0", "", "", "", ""}; // labels of the 5 graduations
    private volatile String hudL1 = "", hudR1 = "", hudL2 = "", hudR2 = "", hudL3 = "", hudR3 = "";
    // Measured attitude of the rocket (UltimateAltimeter 0.8): rotation of the upright rocket
    // in the Processing frame, row-major 3x3; null = drawn vertical (no gyroscope)
    private volatile float[] replayAttitude = null;

    /** Smoothing coefficient of the displayed direction at each frame (0..1). */
    private static final float SMOOTHING = 0.15f;
    /** Camera: the view looks slightly down on the pad and from the side. */
    private static final float VIEW_ELEVATION_DEG = 18f;
    private static final float VIEW_AZIMUTH_DEG = -30f;

    private PFont font;
    private PImage img;

    /**
     * Size of the sketch. It must be the size of the view that shows it: a larger sketch is
     * cropped by the view (the right column of the HUD was cut on the status screen). The
     * scene and the HUD are designed for 1200 x 1200 and scaled to the real size (ui()).
     */
    private final int viewSizeX;
    private final int viewSizeY;
    private static final float DESIGN_SIZE = 1200f;

    // Fuselage geometry
    private final float BODY_RADIUS = 50.0f;
    private final float BODY_HEIGHT = 500.0f;       // Extends from Z = -250 to Z = +250
    private final float CONE_HEIGHT = 150.0f;       // Base at Z = -250, apex at Z = -400
    private final float NOZZLE_HEIGHT = 50.0f;      // Extends from Z = +250 to Z = +300

    /** Sketch of 1200 x 1200 pixels (gimbal console size). */
    public Rocket() {
        this(1200, 1200);
    }

    /**
     * Sketch of the size of the view that shows it.
     *
     * @param width  width of the view (pixels)
     * @param height height of the view (pixels)
     */
    public Rocket(int width, int height) {
        viewSizeX = Math.max(200, width);
        viewSizeY = Math.max(200, height);
    }

    @Override
    public void settings() {
        size(viewSizeX, viewSizeY, P3D);
    }

    /** Scale of the scene and the HUD, designed for a 1200 x 1200 sketch. */
    private float ui() {
        return min(width, height) / DESIGN_SIZE;
    }

    @Override
    public void setup() {
        frameRate(60);
        smooth(4);
        try {
            font = loadFont("CourierNew36.vlw");
        } catch (Exception e) {
            font = null;
        }
        // Red and white checkered body (Tintin's moon rocket): 4 quarters around the body,
        // so that a rotation around the long axis is clearly seen
        img = generateBandTexture(256, 384, 4, 6);
    }

    /**
     * Checkered texture of the body: "columns" quarters around the body (90 degrees each with
     * 4) and "rows" bands along it, alternately red and white.
     */
    private PImage generateBandTexture(int w, int h, int columns, int rows) {
        PImage tex = createImage(w, h, RGB);
        tex.loadPixels();
        int red = color(205, 32, 38), white = color(242, 242, 245);
        for (int y = 0; y < h; y++) {
            int row = y * rows / h;
            for (int x = 0; x < w; x++) {
                int col = x * columns / w;
                tex.pixels[y * w + x] = ((row + col) % 2 == 0) ? red : white;
            }
        }
        tex.updatePixels();
        return tex;
    }

    // ---------------------------------------------------------------------------------
    // Inputs
    // ---------------------------------------------------------------------------------

    /**
     * Accelerometer attitude (status screen of the accelerometer boards).
     *
     * @param dirX     nose direction towards the board axis b (unit vector component)
     * @param dirY     nose direction towards the board axis c
     * @param dirZ     nose direction up (cosine of the tilt)
     * @param tilt     tilt from the vertical (degrees)
     * @param angB     angle on the board axis b (degrees)
     * @param angC     angle on the board axis c (degrees)
     * @param g        total acceleration (g)
     * @param valid    false when the acceleration is far from 1 g (angle not meaningful)
     * @param nameB    name of the board axis b, e.g. "+Y"
     * @param nameC    name of the board axis c, e.g. "+Z"
     */
    public void setAccelAttitude(float dirX, float dirY, float dirZ, float tilt, float angB,
                                 float angC, float g, boolean valid, String nameB, String nameC) {
        if (valid) {
            targetX = dirX;
            targetY = dirY;
            targetZ = dirZ;
        }
        tiltDeg = tilt;
        angleB = angB;
        angleC = angC;
        totalG = g;
        attitudeValid = valid;
        axisNameB = nameB;
        axisNameC = nameC;
        hasAccelData = true;
        accelMode = true;
    }

    /**
     * Flight replay: the rocket stands vertical (an accelerometer alone cannot measure the
     * attitude in flight, see FlightViewReplayFragment) and moves along a graduated altitude
     * scale. The texts are formatted by the fragment (units, language).
     *
     * @param altFrac   altitude / maximum altitude of the flight (0..1)
     * @param peakFrac  highest altitude reached so far / maximum altitude (0..1)
     * @param phase     PHASE_PAD .. PHASE_LANDED (flame, parachutes)
     * @param scale     labels of the graduations 0, 1/4, 1/2, 3/4 and 1 of the maximum altitude
     * @param l1 r1 l2 r2 l3 HUD texts: left / right of lines 1 and 2, line 3 (phase)
     */
    public void setReplayState(float altFrac, float peakFrac, int phase, String[] scale,
                               String l1, String r1, String l2, String r2, String l3) {
        replayAltFrac = constrain(altFrac, 0, 1);
        replayPeakFrac = constrain(peakFrac, 0, 1);
        replayPhase = phase;
        if (scale != null && scale.length == 5) replayScale = scale;
        hudL1 = l1;
        hudR1 = r1;
        hudL2 = l2;
        hudR2 = r2;
        hudL3 = l3;
        replayMode = true;
    }

    /**
     * Measured attitude for the replay of a flight recorded with a gyroscope.
     *
     * @param m   rotation of the upright rocket (nose up) in the Processing frame (x right,
     *            y down, z towards the viewer), row-major 3x3; null to draw it vertical
     * @param r3  HUD text of line 3, right (tilt and roll)
     */
    public void setReplayAttitude(float[] m, String r3) {
        replayAttitude = (m != null && m.length == 9) ? m.clone() : null;
        hudR3 = r3 == null ? "" : r3;
    }

    /** Localized texts of the accelerometer HUD. */
    public void setAccelLabels(String tilt, String total, String moving, String waiting) {
        labelTilt = tilt;
        labelTotal = total;
        labelMoving = moving;
        labelWaiting = waiting;
        accelMode = true;
    }

    /** Euler angles in degrees (gimbal console). */
    public synchronized void setInputString(float X, float Y, float Z, long time) {
        Euler[2] = Z * (PI / 180.0f);
        Euler[1] = Y * (PI / 180.0f);
        Euler[0] = X * (PI / 180.0f);
        currentTime = time;
        useQuaternion = false;
    }

    /** Quaternion as 4 hexadecimal floats "q0,q1,q2,q3," (gimbal console). */
    public synchronized void setInputString(String inputString) {
        String[] inputStringArr = split(inputString, ",");
        if (inputStringArr.length >= 4) {
            q[0] = decodeFloat(inputStringArr[0]);
            q[1] = decodeFloat(inputStringArr[1]);
            q[2] = decodeFloat(inputStringArr[2]);
            q[3] = decodeFloat(inputStringArr[3]);
        }
        useQuaternion = true;
    }

    @SuppressLint("LongLogTag")
    public void setInputCorrect(String inputString) {
        if (inputString != null && inputString.matches("-?\\d+(?:\\.\\d+)?")) {
            correct = Float.parseFloat(inputString);
        } else {
            Log.d("Rocket - setInputCorrect", String.valueOf(inputString));
        }
    }

    private float decodeFloat(String inString) {
        if (inString == null || inString.length() != 8) return 0.0f;
        byte[] inData = new byte[4];
        inData[0] = (byte) unhex(inString.substring(0, 2));
        inData[1] = (byte) unhex(inString.substring(2, 4));
        inData[2] = (byte) unhex(inString.substring(4, 6));
        inData[3] = (byte) unhex(inString.substring(6, 8));
        int intbits = (inData[3] << 24) | ((inData[2] & 0xff) << 16) | ((inData[1] & 0xff) << 8) | (inData[0] & 0xff);
        return Float.intBitsToFloat(intbits);
    }

    // ---------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------

    @Override
    public void draw() {
        background(15, 18, 25);
        ambientLight(80, 85, 100);
        directionalLight(255, 255, 255, 0.5f, 0.8f, -1.0f);
        lightSpecular(180, 180, 200);

        if (replayMode) {
            drawReplayScene();
            drawReplayHUD();
            return;
        }
        if (accelMode) {
            drawAccelScene();
            drawAccelHUD();
            return;
        }

        if (useQuaternion) {
            quaternionToEuler(q, Euler);
        }
        pushMatrix();
        translate(width / 2.0f, height / 2.0f + 40 * ui(), 0);
        scale(ui());
        rotateZ(-Euler[2]);
        rotateX(-Euler[1] - radians(correct));
        rotateY(-Euler[0]);
        rotateY(PI / 2);
        drawRocketBody();
        popMatrix();
        drawGimbalHUD();
    }

    /**
     * Ground ring with the two board axes, vertical reference, and the rocket standing on the
     * pad, tilted towards the measured direction. Ground frame in Processing coordinates after
     * the view rotation: board axis b = +x, up = -y, board axis c = -z (away from the viewer).
     */
    private void drawAccelScene() {
        // Smooth the displayed direction towards the measured one
        shownX += (targetX - shownX) * SMOOTHING;
        shownY += (targetY - shownY) * SMOOTHING;
        shownZ += (targetZ - shownZ) * SMOOTHING;
        float n = sqrt(shownX * shownX + shownY * shownY + shownZ * shownZ);
        if (n < 1e-6f) n = 1;
        float dx = shownX / n, dy = shownY / n, dz = shownZ / n;

        pushMatrix();
        // Pad low in the view, under the HUD; scene scaled to the size of the view
        translate(width / 2.0f, height * 0.86f, 0);
        rotateX(-radians(VIEW_ELEVATION_DEG));
        rotateY(radians(VIEW_AZIMUTH_DEG));
        scale(0.95f * ui());

        // Ground ring and board axes (on the horizontal plane)
        pushMatrix();
        rotateX(HALF_PI);
        noFill();
        strokeWeight(2f);
        stroke(0, 180, 255, 90);
        ellipse(0, 0, 520, 520);
        stroke(0, 180, 255, 50);
        ellipse(0, 0, 300, 300);
        popMatrix();
        strokeWeight(3f);
        stroke(239, 83, 80);
        line(0, 0, 0, 300, 0, 0);          // board axis b
        stroke(102, 187, 106);
        line(0, 0, 0, 0, 0, -300);         // board axis c
        if (font != null) textFont(font);
        textSize(30);
        fill(239, 83, 80);
        text(axisNameB, 310, 10, 0);
        fill(102, 187, 106);
        text(axisNameC, 0, 10, -310);

        // Vertical reference
        stroke(255, 255, 255, 120);
        strokeWeight(2f);
        line(0, 0, 0, 0, -760, 0);

        // Rocket: rotation taking "up" (0,-1,0) to the nose direction s = (dx, -dz, -dy).
        // Axis = up x s = (-sz, 0, sx) = (dy, 0, dx), angle = tilt
        float angle = acos(constrain(dz, -1, 1));
        float ax = dy, az = dx;
        if (angle > 1e-4f && (ax * ax + az * az) > 1e-10f) {
            rotate(angle, ax, 0, az);
        }
        // Body along local Z with the nose at -Z: stand it up with the nozzle on the pad
        rotateX(-HALF_PI);
        translate(0, 0, -(BODY_HEIGHT / 2.0f + NOZZLE_HEIGHT));
        drawRocketBody();
        popMatrix();
    }

    /** Top of the scene under the replay HUD (pixels). */
    private float replayTop() {
        return (16 + 200 + 40) * max(ui(), 0.55f);
    }

    /**
     * Replay scene: ground ring, graduated altitude scale on the left with the peak reached,
     * and the rocket (vertical, checkered) at its altitude; flame during the boost, small
     * parachute after the apogee, large one after the main deployment.
     */
    private void drawReplayScene() {
        float u = ui();
        float groundY = height * 0.93f;
        float topY = replayTop();
        float avail = groundY - topY;
        // Rocket length (700 design units) about 22 % of the scene height
        float s = min(0.30f * u, avail * 0.22f / 700f);
        float rocketLen = 700 * s;
        // Room kept above the nose for the drogue parachute at the apogee (170 x 2.3 units)
        float travel = max(1, avail - rocketLen - 400 * s);
        float baseY = groundY - replayAltFrac * travel;

        // Altitude scale
        float scaleX = width * 0.13f;
        stroke(0, 180, 255, 120);
        strokeWeight(max(1, 2 * u));
        line(scaleX, groundY, scaleX, groundY - travel);
        if (font != null) textFont(font);
        textSize(max(14, 26 * u));
        textAlign(LEFT, CENTER);
        String[] labels = replayScale;
        for (int i = 0; i <= 4; i++) {
            float y = groundY - travel * i / 4f;
            stroke(0, 180, 255, 120);
            line(scaleX - 10 * u, y, scaleX + 10 * u, y);
            fill(150, 200, 230);
            text(labels[i], scaleX + 16 * u, y);
        }
        // Peak reached so far
        float peakY = groundY - replayPeakFrac * travel;
        stroke(239, 83, 80, 200);
        line(scaleX - 18 * u, peakY, width * 0.92f, peakY);
        // Track of the rocket since the start
        stroke(255, 255, 255, 60);
        strokeWeight(max(1, 6 * s));
        line(width / 2.0f, groundY, width / 2.0f, peakY);

        // Ground
        pushMatrix();
        translate(width / 2.0f, groundY, 0);
        rotateX(HALF_PI * 0.82f);
        noFill();
        stroke(0, 180, 255, 90);
        strokeWeight(max(1, 2 * u));
        ellipse(0, 0, 560 * u, 560 * u);
        ellipse(0, 0, 300 * u, 300 * u);
        popMatrix();

        // Rocket at its altitude, seen slightly from above and from the side
        pushMatrix();
        translate(width / 2.0f, baseY, 0);
        rotateY(radians(VIEW_AZIMUTH_DEG));
        scale(s);
        float[] m = replayAttitude;
        // Nose tip of the upright rocket: (0, -700, 0); where the attitude puts it
        float noseLen = CONE_HEIGHT + BODY_HEIGHT + NOZZLE_HEIGHT;
        float nx = 0, ny = -noseLen, nz = 0;
        if (m != null) {
            nx = -noseLen * m[1];
            ny = -noseLen * m[4];
            nz = -noseLen * m[7];
        }
        if (replayPhase == PHASE_DROGUE || replayPhase == PHASE_MAIN) {
            drawParachute(replayPhase == PHASE_MAIN ? 300 : 170, nx, ny, nz);
        }
        if (m != null) {
            // measured attitude (gyroscope), the rocket turns around its base
            applyMatrix(m[0], m[1], m[2], 0,
                    m[3], m[4], m[5], 0,
                    m[6], m[7], m[8], 0,
                    0, 0, 0, 1);
        }
        rotateX(-HALF_PI);
        translate(0, 0, -(BODY_HEIGHT / 2.0f + NOZZLE_HEIGHT));
        drawRocketBody();
        if (replayPhase == PHASE_BOOST) {
            drawFlame();
        }
        popMatrix();
    }

    /** Motor flame under the nozzle (local frame of the rocket body, +Z = aft). */
    private void drawFlame() {
        pushMatrix();
        translate(0, 0, BODY_HEIGHT / 2.0f + NOZZLE_HEIGHT);
        noStroke();
        float len = 160 + 40 * sin(frameCount * 0.6f);
        fill(255, 170, 40, 220);
        beginShape(TRIANGLE_FAN);
        vertex(0, 0, len);
        for (int i = 0; i <= 16; i++) {
            float a = TWO_PI * i / 16;
            vertex(cos(a) * 28, sin(a) * 28, 0);
        }
        endShape();
        fill(255, 240, 160, 230);
        beginShape(TRIANGLE_FAN);
        vertex(0, 0, len * 0.55f);
        for (int i = 0; i <= 16; i++) {
            float a = TWO_PI * i / 16;
            vertex(cos(a) * 16, sin(a) * 16, 0);
        }
        endShape();
        popMatrix();
    }

    /**
     * Parachute above the nose (screen frame: up = -y), with its shroud lines.
     *
     * @param radius radius of the canopy (design units)
     * @param ax     attachment point (nose tip), screen frame of the rocket base
     */
    private void drawParachute(float radius, float ax, float ay, float az) {
        // The canopy stays above the highest point of the rocket
        float canopyY = min(ay, 0) - radius * 1.6f;
        // Shroud lines from the nose to the rim of the canopy
        stroke(220);
        strokeWeight(3);
        for (int i = 0; i < 8; i++) {
            float a = TWO_PI * i / 8;
            line(ax, ay, az, ax + cos(a) * radius, canopyY, az + sin(a) * radius);
        }
        pushMatrix();
        translate(ax, 0, az);
        // Canopy: half sphere, alternately orange and white gores
        noStroke();
        int gores = 12, rings = 6;
        for (int g = 0; g < gores; g++) {
            float a0 = TWO_PI * g / gores, a1 = TWO_PI * (g + 1) / gores;
            fill(g % 2 == 0 ? color(255, 120, 30) : color(245));
            beginShape(QUAD_STRIP);
            for (int r = 0; r <= rings; r++) {
                float phi = HALF_PI * r / rings;      // 0 = rim, PI/2 = top
                float rr = radius * cos(phi), y = canopyY - radius * 0.7f * sin(phi);
                vertex(cos(a0) * rr, y, sin(a0) * rr);
                vertex(cos(a1) * rr, y, sin(a1) * rr);
            }
            endShape();
        }
        popMatrix();
    }

    /** HUD of the replay: 2 lines of values and the flight phase. */
    private void drawReplayHUD() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        if (font != null) textFont(font);
        float u = max(ui(), 0.55f);
        float margin = 16 * u, pad = 20 * u;
        textSize(30 * u);
        noStroke();
        fill(10, 14, 20, 215);
        rect(margin, margin, width - 2 * margin, 200 * u, 12 * u);
        textAlign(LEFT, CENTER);
        fill(0, 215, 255);
        text(hudL1, margin + pad, margin + 36 * u);
        fill(255);
        text(hudL2, margin + pad, margin + 98 * u);
        textAlign(RIGHT, CENTER);
        fill(255);
        text(hudR1, width - margin - pad, margin + 36 * u);
        text(hudR2, width - margin - pad, margin + 98 * u);
        textAlign(LEFT, CENTER);
        fill(255, 190, 60);
        text(hudL3, margin + pad, margin + 160 * u);
        textAlign(RIGHT, CENTER);
        fill(255);
        text(hudR3, width - margin - pad, margin + 160 * u);
        hint(ENABLE_DEPTH_TEST);
    }

    /** Fuselage, nose cone, nozzle and fins along local Z (nose towards -Z). */
    private void drawRocketBody() {
        pushMatrix();
        drawTextureCylinder(36, BODY_RADIUS, BODY_RADIUS, BODY_HEIGHT, img);
        popMatrix();

        pushMatrix();
        translate(0, 0, -(BODY_HEIGHT / 2.0f + CONE_HEIGHT / 2.0f));
        drawTaperedCone(36, BODY_RADIUS, CONE_HEIGHT);
        popMatrix();

        pushMatrix();
        translate(0, 0, BODY_HEIGHT / 2.0f);
        drawNozzle(24, 38.0f, 24.0f, NOZZLE_HEIGHT);
        popMatrix();

        drawFins();
    }

    private void drawTaperedCone(int sides, float rBottom, float h) {
        float angle = 360.0f / sides;
        float halfHeight = h / 2.0f;
        noStroke();
        fill(225, 45, 35);
        beginShape(TRIANGLE_FAN);
        vertex(0, 0, -halfHeight);
        for (int i = 0; i <= sides; i++) {
            vertex(cos(radians(i * angle)) * rBottom, sin(radians(i * angle)) * rBottom, halfHeight);
        }
        endShape(CLOSE);
        beginShape();
        for (int i = 0; i < sides; i++) {
            vertex(cos(radians(i * angle)) * rBottom, sin(radians(i * angle)) * rBottom, halfHeight);
        }
        endShape(CLOSE);
    }

    private void drawNozzle(int sides, float rTop, float rBottom, float h) {
        float angle = 360.0f / sides;
        noStroke();
        fill(55, 60, 68);
        beginShape(QUAD_STRIP);
        for (int i = 0; i <= sides; i++) {
            float c = cos(radians(i * angle)), s = sin(radians(i * angle));
            vertex(c * rTop, s * rTop, 0);
            vertex(c * rBottom, s * rBottom, h);
        }
        endShape(CLOSE);
    }

    private void drawFins() {
        fill(225, 45, 35);
        stroke(20, 22, 28);
        strokeWeight(1.2f);
        for (int i = 0; i < 4; i++) {
            // One white fin: second reference of the rotation around the long axis
            fill(i == 0 ? color(242, 242, 245) : color(225, 45, 35));
            pushMatrix();
            rotateZ(HALF_PI * i);
            beginShape();
            vertex(BODY_RADIUS, 0, 100);
            vertex(BODY_RADIUS + 55, 0, 210);
            vertex(BODY_RADIUS + 55, 0, 250);
            vertex(BODY_RADIUS, 0, 245);
            endShape(CLOSE);
            popMatrix();
        }
    }

    private void drawTextureCylinder(int sides, float r1, float r2, float h, PImage image) {
        float angle = 360.0f / sides;
        float halfHeight = h / 2.0f;

        beginShape();
        fill(230);
        noStroke();
        for (int i = 0; i < sides; i++) {
            vertex(cos(radians(i * angle)) * r1, sin(radians(i * angle)) * r1, -halfHeight);
        }
        endShape(CLOSE);

        beginShape();
        fill(45);
        noStroke();
        for (int i = 0; i < sides; i++) {
            vertex(cos(radians(i * angle)) * r2, sin(radians(i * angle)) * r2, halfHeight);
        }
        endShape(CLOSE);

        beginShape(QUAD_STRIP);
        noStroke();
        if (image != null) {
            texture(image);
        } else {
            fill(240);
        }
        for (int i = 0; i <= sides; i++) {
            float c = cos(radians(i * angle)), s = sin(radians(i * angle));
            float u = (image != null) ? (image.width / (float) sides) * i : 0;
            vertex(c * r1, s * r1, -halfHeight, u, 0);
            vertex(c * r2, s * r2, halfHeight, u, (image != null) ? image.height : 0);
        }
        endShape(CLOSE);
    }

    /**
     * HUD of the accelerometer mode: tilt, angle on each axis and total acceleration. Sizes
     * follow the size of the view, the right column is aligned on the real right edge.
     */
    private void drawAccelHUD() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        if (font != null) textFont(font);
        float u = max(ui(), 0.55f);
        float margin = 16 * u, pad = 20 * u;
        float line1 = margin + 38 * u, line2 = margin + 102 * u;
        textSize(32 * u);
        noStroke();
        fill(10, 14, 20, 215);
        rect(margin, margin, width - 2 * margin, 140 * u, 12 * u);

        textAlign(LEFT, CENTER);
        if (!hasAccelData) {
            fill(0, 215, 255);
            text(labelWaiting, margin + pad, margin + 70 * u);
        } else {
            // Tilt colored green < 5 deg, orange < 15 deg, red above
            if (!attitudeValid) fill(150);
            else if (tiltDeg < 5) fill(76, 175, 80);
            else if (tiltDeg < 15) fill(255, 167, 38);
            else fill(239, 83, 80);
            text(attitudeValid ? String.format("%s %.1f°", labelTilt, tiltDeg) : labelMoving, margin + pad, line1);
            fill(0, 215, 255);
            text(String.format("%s %.2f g", labelTotal, totalG), margin + pad, line2);

            fill(255);
            textAlign(RIGHT, CENTER);
            text(String.format("%s: %+.1f°", axisNameB, angleB), width - margin - pad, line1);
            text(String.format("%s: %+.1f°", axisNameC, angleC), width - margin - pad, line2);
        }
        hint(ENABLE_DEPTH_TEST);
    }

    /** HUD of the gimbal mode (Euler angles), as in the gimbal console. */
    private void drawGimbalHUD() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        if (font != null) textFont(font);
        float u = max(ui(), 0.55f);
        textSize(32 * u);
        noStroke();
        fill(10, 14, 20, 215);
        rect(16 * u, 16 * u, width - 32 * u, 140 * u, 12 * u);
        fill(0, 215, 255);
        textAlign(LEFT, CENTER);
        text(String.format("T+ %.2f s", currentTime / 1000.0f), 36 * u, 86 * u);
        fill(255);
        textAlign(RIGHT, CENTER);
        text(String.format("X: %+.1f°", degrees(Euler[0])), width - 36 * u, 44 * u);
        text(String.format("Y: %+.1f°", degrees(Euler[1])), width - 36 * u, 86 * u);
        text(String.format("Z: %+.1f°", degrees(Euler[2])), width - 36 * u, 128 * u);
        hint(ENABLE_DEPTH_TEST);
    }

    /** Euler angles (psi, theta, phi) of a quaternion. */
    void quaternionToEuler(float[] q, float[] euler) {
        euler[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1);
        euler[1] = -asin(2 * q[1] * q[3] + 2 * q[0] * q[2]);
        euler[2] = atan2(2 * q[2] * q[3] - 2 * q[0] * q[1], 2 * q[0] * q[0] + 2 * q[3] * q[3] - 1);
    }
}
