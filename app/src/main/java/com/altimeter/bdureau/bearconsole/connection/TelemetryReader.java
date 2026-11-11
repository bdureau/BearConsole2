package com.altimeter.bdureau.bearconsole.connection;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: TelemetryReader
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Owns the background loop that continuously decodes the frames streamed by the altimeter
 *  while a live screen (telemetry, status, rocket tracking) is displayed, and every command
 *  those screens send to the altimeter.
 *
 *  Why this class exists:
 *    - The serial link is blocking. Starting, stopping and commanding the stream used to be
 *      done from the UI thread (onStop / onDestroy), which froze the screen for up to 100 s.
 *      Here every I/O operation runs on the single-thread I/O executor of the application,
 *      in the order in which it was requested. The executor is shared by all screens, so the
 *      "stream off" sent by a screen being destroyed can never overtake the "stream on" sent
 *      by the screen that replaces it (e.g. after a rotation).
 *    - The stop request is carried by a volatile flag that is also checked inside
 *      ConsoleApplication.ReadResult(), so it can never be lost, and the reading thread is
 *      joined before the next command is sent: two threads never read the stream together.
 *
 *  All public methods are non blocking and may be called from the UI thread.
 * ============================================================================
 */

import android.util.Log;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class TelemetryReader {
    private static final String TAG = "TelemetryReader";

    /** Maximum duration of one read call of the loop (it restarts immediately). */
    private static final long READ_SLICE_MS = 10000;
    /** Maximum time given to the reading thread to finish after a stop request. */
    private static final long JOIN_TIMEOUT_MS = 3000;
    /** Maximum time waited for the acknowledgement of a stop command. */
    private static final long ACK_TIMEOUT_MS = 3000;

    private final ConsoleApplication app;
    private final ExecutorService io;

    // Read by the reading thread and by ReadResult(); written by the I/O executor and the UI
    private volatile boolean running = false;
    // Last state requested by the screen: a start queued before a stop must not run
    private volatile boolean wanted = false;
    // Only accessed from the I/O executor
    private Thread readerThread;

    public TelemetryReader(ConsoleApplication app) {
        this.app = app;
        this.io = app.getIoExecutor();
    }

    /** True while the reading loop is (or is about to be) running. */
    public boolean isRunning() {
        return wanted;
    }

    /**
     * Sends the optional start command (e.g. "y1;") then starts the reading loop.
     * Does nothing if the loop is already running.
     *
     * @param startCommand command enabling the stream, or null if it is already enabled
     */
    public void start(final String startCommand) {
        wanted = true;
        execute(() -> {
            if (!wanted) return;
            if (readerThread != null && readerThread.isAlive()) return;
            if (startCommand != null && app.getConnected()) {
                app.flush();
                app.clearInput();
                app.write(startCommand);
            }
            running = true;
            readerThread = new Thread(() -> {
                while (running) {
                    app.ReadResult(READ_SLICE_MS, () -> running);
                }
            }, "telemetry-reader");
            readerThread.start();
        });
    }

    /**
     * Stops the reading loop, then sends the optional stop commands.
     *
     * @param waitAck      true to wait (at most a few seconds) for the "OK" answer to the last command
     * @param stopCommands commands sent once the loop has stopped (e.g. "h;", "y0;")
     */
    public void stop(final boolean waitAck, final String... stopCommands) {
        // Raise the flags immediately so that the read in progress ends as soon as possible
        wanted = false;
        running = false;
        app.setExit(true);
        execute(() -> {
            joinReader();
            if (!app.getConnected() || stopCommands == null) return;
            for (String cmd : stopCommands) {
                app.flush();
                app.clearInput();
                app.write(cmd);
            }
            if (waitAck) {
                app.waitForData(ACK_TIMEOUT_MS);
                app.ReadResult(ACK_TIMEOUT_MS);
            }
        });
    }

    /**
     * Sends a command to the altimeter without stopping the reading loop
     * (e.g. output test, recording on / off).
     */
    public void send(final String command) {
        execute(() -> {
            if (!app.getConnected()) return;
            app.write(command);
            app.flush();
        });
    }

    /**
     * Marks the end of use of this reader (call it from onDestroy(), after the last stop()).
     * The I/O executor belongs to the application and keeps running: the queued commands
     * are still executed after the screen is gone.
     */
    public void release() {
        wanted = false;
    }

    private void joinReader() {
        Thread t = readerThread;
        if (t != null) {
            try {
                t.join(JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (t.isAlive()) {
                Log.w(TAG, "Reading thread did not stop in time");
            }
        }
        readerThread = null;
    }

    private void execute(Runnable task) {
        try {
            io.execute(task);
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Command ignored: reader already released");
        }
    }
}
