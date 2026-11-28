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
 *  Needs the processing-core.jar library and the CourierNew36.vlw font of the assets
 *  (pattern.png is optional: a checkerboard texture is generated when it is missing).
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

    /** Smoothing coefficient of the displayed direction at each frame (0..1). */
    private static final float SMOOTHING = 0.15f;
    /** Camera: the view looks slightly down on the pad and from the side. */
    private static final float VIEW_ELEVATION_DEG = 18f;
    private static final float VIEW_AZIMUTH_DEG = -30f;

    private PFont font;
    private PImage img;

    // Viewport dimensions (same as the gimbal console)
    private final int VIEW_SIZE_X = 1200;
    private final int VIEW_SIZE_Y = 1200;

    // Fuselage geometry
    private final float BODY_RADIUS = 50.0f;
    private final float BODY_HEIGHT = 500.0f;       // Extends from Z = -250 to Z = +250
    private final float CONE_HEIGHT = 150.0f;       // Base at Z = -250, apex at Z = -400
    private final float NOZZLE_HEIGHT = 50.0f;      // Extends from Z = +250 to Z = +300

    @Override
    public void settings() {
        size(VIEW_SIZE_X, VIEW_SIZE_Y, P3D);
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
        try {
            img = loadImage("pattern.png");
        } catch (Exception e) {
            img = null;
        }
        if (img == null) {
            img = generateCheckerboardTexture(256, 256, 8);
        }
    }

    /** High-contrast checkerboard texture used when pattern.png is missing. */
    private PImage generateCheckerboardTexture(int w, int h, int tiles) {
        PImage tex = createImage(w, h, RGB);
        tex.loadPixels();
        int tileSize = w / tiles;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean isWhite = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                tex.pixels[y * w + x] = isWhite ? color(245, 245, 248) : color(20, 22, 26);
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

        if (accelMode) {
            drawAccelScene();
            drawAccelHUD();
            return;
        }

        if (useQuaternion) {
            quaternionToEuler(q, Euler);
        }
        pushMatrix();
        translate(width / 2.0f, height / 2.0f + 40, 0);
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
        translate(width / 2.0f, height * 0.80f, 0);
        rotateX(-radians(VIEW_ELEVATION_DEG));
        rotateY(radians(VIEW_AZIMUTH_DEG));
        scale(0.95f);

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

    /** HUD of the accelerometer mode: tilt, angle on each axis and total acceleration. */
    private void drawAccelHUD() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        if (font != null) textFont(font);
        textSize(32);
        noStroke();
        fill(10, 14, 20, 215);
        rect(16, 16, width - 32, 140, 12);

        textAlign(LEFT, CENTER);
        if (!hasAccelData) {
            fill(0, 215, 255);
            text(labelWaiting, 36, 86);
        } else {
            // Tilt colored green < 5 deg, orange < 15 deg, red above
            if (!attitudeValid) fill(150);
            else if (tiltDeg < 5) fill(76, 175, 80);
            else if (tiltDeg < 15) fill(255, 167, 38);
            else fill(239, 83, 80);
            text(attitudeValid ? String.format("%s %.1f°", labelTilt, tiltDeg) : labelMoving, 36, 54);
            fill(0, 215, 255);
            text(String.format("%s %.2f g", labelTotal, totalG), 36, 118);

            fill(255);
            textAlign(RIGHT, CENTER);
            text(String.format("%s: %+.1f°", axisNameB, angleB), width - 60, 54);
            text(String.format("%s: %+.1f°", axisNameC, angleC), width - 60, 118);
        }
        hint(ENABLE_DEPTH_TEST);
    }

    /** HUD of the gimbal mode (Euler angles), as in the gimbal console. */
    private void drawGimbalHUD() {
        hint(DISABLE_DEPTH_TEST);
        camera();
        if (font != null) textFont(font);
        textSize(32);
        noStroke();
        fill(10, 14, 20, 215);
        rect(16, 16, width - 32, 140, 12);
        fill(0, 215, 255);
        textAlign(LEFT, CENTER);
        text(String.format("T+ %.2f s", currentTime / 1000.0f), 36, 86);
        fill(255);
        textAlign(RIGHT, CENTER);
        text(String.format("X: %+.1f°", degrees(Euler[0])), width - 140, 44);
        text(String.format("Y: %+.1f°", degrees(Euler[1])), width - 140, 86);
        text(String.format("Z: %+.1f°", degrees(Euler[2])), width - 140, 128);
        hint(ENABLE_DEPTH_TEST);
    }

    /** Euler angles (psi, theta, phi) of a quaternion. */
    void quaternionToEuler(float[] q, float[] euler) {
        euler[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1);
        euler[1] = -asin(2 * q[1] * q[3] + 2 * q[0] * q[2]);
        euler[2] = atan2(2 * q[2] * q[3] - 2 * q[0] * q[1], 2 * q[0] * q[0] + 2 * q[3] * q[3] - 1);
    }
}
