package com.altimeter.bdureau.bearconsole.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.IOException;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
/**
 * @description:  This display the rocket orientation
 * This is using a processing class so you need to add the processing-core.jar
 * library to your project. You will also need the font added to your assets directory
 *
 * @author: boris.dureau@neuf.fr
 **/
public class Rocket extends PApplet {
    public void settings() {
        //size(800, 600);
        // This might not work on all screens. So far it is ok on all my phones and tablets
        size(1200, 1200, P3D);
    }

    float[] q = new float[4];
    float[] hq = null;
    float[] Euler = new float[3]; // psi, theta, phi
    float[] RealEuler = new float[3];
    float[] Gravity = new float[3];
    float[] YPR = new float[3];
    long currentTime=0;
    float correct = 0;
    float servoX, servoY;

    boolean useQuaternion = true;
    int lf = 10; // 10 is '\n' in ASCII
    long run = 0;

    byte[] inBuffer = new byte[22]; // this is the number of chars on each line from the Arduino (including /r/n)

    PFont font;

    PImage img;
    final int VIEW_SIZE_X = 1200, VIEW_SIZE_Y = 1200;


    public void setInputString(String inputString) {
        String[] inputStringArr = split(inputString, ",");
        if (inputStringArr.length >= 5) { // q1,q2,q3,q4,\r\n so we have 5 elements
            q[0] = decodeFloat(inputStringArr[0]);
            q[1] = decodeFloat(inputStringArr[1]);
            q[2] = decodeFloat(inputStringArr[2]);
            q[3] = decodeFloat(inputStringArr[3]);
        }
        useQuaternion = true;
    }

    @SuppressLint("LongLogTag")
    public void setInputCorrect(String inputString) {
        if (inputString.matches("\\d+(?:\\.\\d+)?")) {
            correct = Float.valueOf(inputString);
        }
        else {
            Log.d("Rocket - setInputCorrect", inputString);
        }

    }
    public void setServoX(String inputString) {
        if (inputString.matches("\\d+(?:\\.\\d+)?")) {
            servoX = Float.valueOf(inputString);
        }else {
            Log.d("Rocket - setServoX", inputString);
        }
    }
    public void setServoY(String inputString) {
        if (inputString.matches("\\d+(?:\\.\\d+)?")) {
            servoY = Float.valueOf(inputString);
        }
        else {
            Log.d("Rocket - setServoY", inputString);
        }
    }
    public void setInputString(float X, float Y, float Z, long time) {
        Euler[2] = Z*(3.14f/180);
        Euler[1] = Y*(3.14f/180);
        Euler[0] = X*(3.14f/180);
        currentTime = time;
        useQuaternion = false;
    }

    public void setup() {
        //size(1200, 1200, P3D);
        fill(255);
        stroke(color(44, 48, 32));

        frameRate(400);


        // The font must be located in the sketch's "data" directory to load successfully
        font = loadFont("CourierNew36.vlw");

        // Loading the textures to the rocket
        img = loadImage("pattern.png");

        delay(100);

    }

    float decodeFloat(String inString) {
        byte[] inData = new byte[4];

        if (inString.length() == 8) {
            inData[0] = (byte) unhex(inString.substring(0, 2));
            inData[1] = (byte) unhex(inString.substring(2, 4));
            inData[2] = (byte) unhex(inString.substring(4, 6));
            inData[3] = (byte) unhex(inString.substring(6, 8));
        }

        int intbits = (inData[3] << 24) | ((inData[2] & 0xff) << 16) | ((inData[1] & 0xff) << 8) | (inData[0] & 0xff);
        return Float.intBitsToFloat(intbits);
    }


    void readQ() {


    }


    void drawRotatingRocket() {
        pushMatrix();
        translate(VIEW_SIZE_X / 2, VIEW_SIZE_Y / 2 + 50, 0);
        /*rotateZ(-Euler[2]);
        rotateX(-Euler[1]);
        rotateY(-Euler[0]);*/
        rotateZ(-Euler[2]);
        rotateX(-Euler[1]-correct);
        rotateY(-Euler[0]);
        /*rotateX(-Euler[2]);
        rotateY(-Euler[0]);
        rotateZ(-Euler[1]);*/
        drawRocket();
        popMatrix();
    }


    public void draw() {

        background(0, 128, 255);
        textFont(font, 30);
        textAlign(LEFT, TOP);

        if (useQuaternion) {
            if (hq != null) { // use home quaternion
                quaternionToEuler(quatProd(hq, q), Euler);
                text("Disable home position by pressing \"n\"", 20, VIEW_SIZE_Y - 30);
            } else {
                quaternionToEuler(q, Euler);

            }
            quaternionToEuler(q, RealEuler);
            quaternionToGravity(q, Gravity);
            quaternionToYawPitchRoll(q, Gravity, YPR);

            text("servoX: "+ servoX +"\nservoY: "+ servoY + "\ncorrection: " + correct+
                    "Q:\n" + q[0] + "\n" + q[1] + "\n" + q[2] + "\n" + q[3], 20, 20);
            text(/*"Euler Angles:\nYaw (psi)  : " + degrees(Euler[0]) +
                "\nPitch (theta): " + degrees(Euler[1]) +
                "\nRoll (phi)  : " + degrees(Euler[2])+*/

                            "\nYPR Angles:\nYaw (psi)  : " + degrees(YPR[0]) +
                            "\nPitch (theta): " + degrees(YPR[1]) +
                            "\nRoll (phi)  : " + degrees(YPR[2]) +
                            "\nRealYaw (psi)  : " + degrees(RealEuler[0]) +
                            "\nRealPitch (theta): " + degrees(RealEuler[1]) +
                            "\nRealRoll (phi)  : " + degrees(RealEuler[2]), 250, 30);
            //text("Euler Angles:\nYaw (psi)  : " + degrees(Euler[2]) + "\nPitch (theta): " + degrees(Euler[0]) + "\nRoll (phi)  : " + degrees(Euler[1]), 200, 20);
        }
        else {
            text("Time:"+ currentTime, 20, 20);
        }
        drawRotatingRocket();

    }


    public void setOrientation(char key) {

        if (key == 'h') {
            // set hq the home quaternion as the quaternion conjugate coming from the sensor fusion
            hq = quatConjugate(q);
        } else if (key == 'n') {
            hq = null;
        }
    }

    /*
    From what I understand, as long as you receive the quaternion from your Arduino board you can  get
    the Euler angles
    the gravity
    the yaw pitch  roll
     */

/*
Get Euler angles from quaternion
 */
    void quaternionToEuler(float[] q, float[] euler) {
        euler[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1); // psi
        euler[1] = -asin(2 * q[1] * q[3] + 2 * q[0] * q[2]); // theta
        euler[2] = atan2(2 * q[2] * q[3] - 2 * q[0] * q[1], 2 * q[0] * q[0] + 2 * q[3] * q[3] - 1); // phi
    }

    void quaternionToGravity(float[] q, float[] gravity) {
        //gravity[0] = 2 * (q[1] * q[3] - q[0] * q[0]);
        gravity[0] = 2 * (q[1] * q[3] - q[0] * q[2]);
        gravity[1] = 2 * (q[0] * q[1] + q[2] * q[3]);
        gravity[2] = q[0] * q[0] - q[1] * q[1] - q[2] * q[2] + q[3] * q[3];
    }

    void quaternionToYawPitchRoll(float[] q, float[] gravity, float[] ypr) {
        //yaw
        ypr[0] = atan2(2 * q[1] * q[2] - 2 * q[0] * q[3], 2 * q[0] * q[0] + 2 * q[1] * q[1] - 1);
        //pitch
        ypr[1] = atan(gravity[0] / sqrt(gravity[1] * gravity[1] + gravity[2] * gravity[2]));
        //roll
        ypr[2] = atan(gravity[1] / sqrt(gravity[0] * gravity[0] + gravity[2] * gravity[2]));
    }

    float[] quatProd(float[] a, float[] b) {
        float[] q = new float[4];

        q[0] = a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3];
        q[1] = a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2];
        q[2] = a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1];
        q[3] = a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0];

        return q;
    }

    // returns a quaternion from an axis angle representation
    float[] quatAxisAngle(float[] axis, float angle) {
        float[] q = new float[4];

        float halfAngle = (float) (angle / 2.0);
        float sinHalfAngle = sin(halfAngle);
        q[0] = cos(halfAngle);
        q[1] = -axis[0] * sinHalfAngle;
        q[2] = -axis[1] * sinHalfAngle;
        q[3] = -axis[2] * sinHalfAngle;

        return q;
    }

    // return the quaternion conjugate of quat
    float[] quatConjugate(float[] quat) {
        float[] conj = new float[4];

        conj[0] = quat[0];
        conj[1] = -quat[1];
        conj[2] = -quat[2];
        conj[3] = -quat[3];

        return conj;
    }

    void drawRocket() {
        //background(0, 128, 255);
        //rotateZ(PI / 2);
        rotateY(PI / 2);

        pushMatrix();

        drawTextureCylinder(36, 50, 50, 500, img);
        popMatrix();
        pushMatrix();
        translate(0, 0, -325);

        drawTextureCylinder(36, 0, 50, 150, img);
        popMatrix();

        beginShape();
        //start rocket fin set
        //fill(255,255,255);
        translate(0, 0, 100);
        rotateY(PI);
        //rotateZ(PI/3);
        vertex(-100, 0, -150);
        vertex(100, 0, -150);
        vertex(0, 0, 100);
        endShape();

        //start another rocket fin set
        fill(0);
        translate(0, 0, -150);
        beginShape();
        vertex(0, 100, 0);
        vertex(0, -100, 0);
        vertex(0, 0, 250);
        endShape();

    }

    void drawTextureCylinder(int sides, float r1, float r2, float h, PImage image) {
        float angle = 360 / sides;
        float halfHeight = h / 2;

        // top
        beginShape();
        //texture(topside);
        for (int i = 0; i < sides; i++) {
            float x = cos(radians(i * angle)) * r1;
            float y = sin(radians(i * angle)) * r1;
            vertex(x, y, -halfHeight);
        }
        endShape(CLOSE);
        // bottom
        beginShape();
        //texture(topside);
        for (int i = 0; i < sides; i++) {
            float x = cos(radians(i * angle)) * r2;
            float y = sin(radians(i * angle)) * r2;
            vertex(x, y, halfHeight);
        }
        endShape(CLOSE);
        // draw body
        //beginShape(TRIANGLE_STRIP);
        beginShape(QUAD_STRIP);
        texture(image);
        for (int i = 0; i < sides + 1; i++) {
            float x1 = cos(radians(i * angle)) * r1;
            float y1 = sin(radians(i * angle)) * r1;
            float x2 = cos(radians(i * angle)) * r2;
            float y2 = sin(radians(i * angle)) * r2;
            float u = image.width / sides * i;
        /*vertex( x1, y1, -halfHeight);
        vertex( x2, y2, halfHeight);*/
            vertex(x1, y1, -halfHeight, u, 0);
            vertex(x2, y2, halfHeight, u, image.height);
        }
        endShape(CLOSE);

    }
}
