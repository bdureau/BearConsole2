package com.altimeter.bdureau.bearconsole.connection;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AccelCalibrationSession
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Drives the accelerometer calibration of the RocketFlightLogger firmware (version 2.5 and
 *  later, accelerometer boards), command "j;" (accelero.cpp, accelCalibrate()):
 *    - the board is put on its 6 faces (X up, X down, Y up, Y down, Z up, Z down). With an
 *      ADXL345 the faces can be done in any order (the face is detected); with an ADXL375
 *      alone the order is imposed: Z up, Z down, X up, X down, Y up, Y down;
 *    - on each face the board must stay still 0.5 s, then it is averaged 1 s; a short beep
 *      says that the face is done and the board can be turned; a long beep ends a
 *      successful calibration, 5 short beeps a failed one; 2 minutes maximum;
 *    - ANY character received by the altimeter during the calibration stops it. So nothing
 *      else must be sent while it runs: the telemetry stream is switched off before, and the
 *      stream is switched on again only once the calibration has ended;
 *    - the firmware reports in plain text lines (not "$...;" frames):
 *        "Accelerometer calibration", "Put the board on its 6 faces in any order ...",
 *        "Put the board on its 6 faces in this order: ...", "Face 2/6: Z down",
 *        "Z up OK (1/6)", "Calibration saved" (followed by the calibration values),
 *        "Calibration failed..." (preceded by the reason), "Calibration stopped",
 *        "No accelerometer ..." (board without accelerometer).
 *  The session runs on its own thread (it lasts up to 2 minutes and must not block the
 *  shared I/O executor) and reports to the UI thread through Listener. It is queued on the
 *  I/O executor after the stop of the telemetry reader, so the two never read together.
 *  A firmware that does not know "j" (older than 2.5) does not answer: reported as
 *  NOT_SUPPORTED after a few seconds.
 * ============================================================================
 */

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccelCalibrationSession {
    private static final String TAG = "AccelCalibration";

    /** End of the session. */
    public enum Result { SAVED, FAILED, STOPPED, NOT_SUPPORTED, NO_ANSWER }

    /** Face indexes, same numbering as the firmware: X+ X- Y+ Y- Z+ Z-. */
    public static final String[] FIRMWARE_FACE_NAMES = {"X up", "X down", "Y up", "Y down", "Z up", "Z down"};

    /** Events of the session, always called on the UI thread. */
    public interface Listener {
        /** The firmware started; anyOrder = false when the faces must be done in order. */
        void onCalibrationStarted(boolean anyOrder);

        /** Fixed order (ADXL375 only): face to put up now (index in FIRMWARE_FACE_NAMES). */
        void onNextFace(int face, int number);

        /** A face has been measured (index in FIRMWARE_FACE_NAMES), count = faces done. */
        void onFaceDone(int face, int count);

        /** End of the session; details = text sent by the firmware (values or reason). */
        void onCalibrationFinished(Result result, String details);
    }

    /** Time given to the firmware to start answering "j;" (older firmwares never answer). */
    private static final long START_TIMEOUT_MS = 5000;
    /** Silence accepted during the calibration (firmware timeout is 120 s). */
    private static final long RUN_TIMEOUT_MS = 130000;
    /** Silence after "Calibration saved" that ends the list of printed values. */
    private static final long TAIL_TIMEOUT_MS = 1500;
    /** Time left to the firmware to process "y0;" and to send its last telemetry frames. */
    private static final long STREAM_OFF_DELAY_MS = 400;

    private static final Pattern FACE_DONE = Pattern.compile("^(X|Y|Z) (up|down) OK \\((\\d)/6\\)");
    private static final Pattern NEXT_FACE = Pattern.compile("^Face (\\d)/6: (X|Y|Z) (up|down)");

    private final ConsoleApplication app;
    private final Listener listener;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private volatile boolean cancelSent = false;

    public AccelCalibrationSession(ConsoleApplication app, Listener listener) {
        this.app = app;
        this.listener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Queues the session on the I/O executor: it runs after the commands already queued
     * (stop of the telemetry reader), switches the stream off, sends "j;" and starts the
     * reading thread.
     */
    public void start() {
        running = true;
        app.getIoExecutor().execute(() -> {
            if (!running) return;
            if (!app.getConnected()) {
                finish(Result.NO_ANSWER, "");
                return;
            }
            app.write("y0;\n");
            app.flush();
            sleep(STREAM_OFF_DELAY_MS);
            app.clearInput();
            app.write("j;\n");
            app.flush();
            new Thread(this::run, "accel-calibration").start();
        });
    }

    /**
     * Stops the calibration: one character is enough for the firmware ("Calibration
     * stopped" is then received and reported as STOPPED).
     */
    public void cancel() {
        if (!running || cancelSent) return;
        cancelSent = true;
        app.getIoExecutor().execute(() -> {
            app.write("x");
            app.flush();
        });
    }

    /** The screen is destroyed: stop the calibration and the reading thread without reporting. */
    public void release() {
        if (running) {
            cancel();
        }
        running = false;
    }

    private void run() {
        StringBuilder details = new StringBuilder();
        boolean started = false;
        try {
            while (running) {
                String line = app.readLine(started ? RUN_TIMEOUT_MS : START_TIMEOUT_MS, () -> running);
                if (!running) return;
                if (line == null) {
                    finish(started ? Result.NO_ANSWER : Result.NOT_SUPPORTED, details.toString());
                    return;
                }
                line = line.trim();
                // Telemetry frames or acknowledgements still in the buffer, empty lines
                if (line.isEmpty() || line.startsWith("$")) continue;
                Log.d(TAG, line);

                if (line.startsWith("No accelerometer")) {
                    finish(Result.FAILED, line);
                    return;
                }
                if (line.startsWith("Accelerometer calibration") && !started) {
                    started = true;
                    continue;
                }
                if (line.startsWith("Put the board on its 6 faces")) {
                    started = true;
                    final boolean anyOrder = line.contains("any order");
                    ui.post(() -> { if (running) listener.onCalibrationStarted(anyOrder); });
                    continue;
                }
                Matcher next = NEXT_FACE.matcher(line);
                if (next.find()) {
                    final int face = faceIndex(next.group(2), next.group(3));
                    final int number = Integer.parseInt(next.group(1));
                    ui.post(() -> { if (running) listener.onNextFace(face, number); });
                    continue;
                }
                Matcher done = FACE_DONE.matcher(line);
                if (done.find()) {
                    final int face = faceIndex(done.group(1), done.group(2));
                    final int count = Integer.parseInt(done.group(3));
                    ui.post(() -> { if (running) listener.onFaceDone(face, count); });
                    continue;
                }
                if (line.startsWith("Calibration saved")) {
                    // The firmware then prints the new values: keep them for the user
                    String tail;
                    while ((tail = app.readLine(TAIL_TIMEOUT_MS, () -> running)) != null) {
                        tail = tail.trim();
                        if (!tail.isEmpty() && !tail.startsWith("$")) details.append(tail).append('\n');
                    }
                    finish(Result.SAVED, details.toString());
                    return;
                }
                if (line.startsWith("Calibration stopped")) {
                    finish(Result.STOPPED, "");
                    return;
                }
                if (line.startsWith("Calibration failed")) {
                    details.append(line).append('\n');
                    finish(Result.FAILED, details.toString());
                    return;
                }
                // Other lines (instructions, reasons of a failure): kept for the report
                if (!line.startsWith("Send any character")) {
                    details.append(line).append('\n');
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Calibration session error", e);
            finish(Result.NO_ANSWER, details.toString());
        }
    }

    private void finish(Result result, String details) {
        if (!running) return;
        running = false;
        final String text = details == null ? "" : details.trim();
        ui.post(() -> listener.onCalibrationFinished(result, text));
    }

    private static int faceIndex(String axis, String dir) {
        int k = axis.charAt(0) - 'X';
        return 2 * k + ("up".equals(dir) ? 0 : 1);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
