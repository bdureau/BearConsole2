package com.altimeter.bdureau.bearconsole.Flight;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: FlightDownloadViewModel
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Download of the recorded flights, shared by the flight list screen (FlightListActivity,
 *  commands "n;" then "r<index>;" for each flight) and the flight recovery screen
 *  (FlightRecoveryActivity, commands "n;" then "R<samples>;").
 *  It replaces the former AsyncTask of each screen, which had two problems:
 *    - leaving the screen did not stop the download: it kept reading every remaining flight
 *      and kept the serial link busy for the next screen (telemetry, status...);
 *    - the download was started in onCreate(): when the screen was re-created (tablet
 *      rotation, where Android 16 ignores the portrait lock, language or theme change) a
 *      second download started while the first one was still reading the same link, and the
 *      flight data of both were mixed.
 *  The ViewModel survives the re-creation of the screen, so the download is started only
 *  once (start() does nothing if it already ran) and the new screen simply observes the
 *  progress and the result. When the screen is really left, onCleared() cancels the download:
 *  the read in progress stops at once (exit flag and "keep running" condition) and the flight
 *  being received is dropped as incomplete.
 *  The download runs on the shared serial I/O executor of the application, so it can never
 *  run at the same time as another serial operation.
 * ============================================================================
 */

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlightDownloadViewModel extends AndroidViewModel {
    private static final String TAG = "FlightDownload";

    /**
     * Silence after which a download is considered finished or lost (ms). 60 s, not less:
     * an ATmega328 altimeter does not read its serial port while it beeps the last altitudes
     * (up to ~45 s after a high flight), so the first answer can come that late.
     */
    private static final long DOWNLOAD_IDLE_TIMEOUT_MS = 60000;
    /** Safety limit for the download of one flight (ms); the user can cancel before. */
    private static final long DOWNLOAD_MAX_DURATION_MS = 30 * 60 * 1000;

    /** End of the download. */
    public static final class Result {
        /** Names of the flights now in the repository (never null). */
        public final List<String> flightNames;
        /** true if the user (or the closing of the screen) cancelled the download. */
        public final boolean canceled;

        Result(List<String> flightNames, boolean canceled) {
            this.flightNames = flightNames;
            this.canceled = canceled;
        }
    }

    private final ConsoleApplication myBT;
    private final MutableLiveData<String> progress = new MutableLiveData<>();
    private final MutableLiveData<Result> result = new MutableLiveData<>();
    private boolean started = false;
    private volatile boolean running = false;
    private volatile boolean canceled = false;
    private boolean endMessageShown = false;

    public FlightDownloadViewModel(@NonNull Application application) {
        super(application);
        myBT = (ConsoleApplication) application;
    }

    /** Progress text ("Retrieving flight: 3"), null before the first flight. */
    public LiveData<String> getProgress() {
        return progress;
    }

    /** Result, set once when the download has ended. */
    public LiveData<Result> getResult() {
        return result;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * true the first time it is called after the end of the download: the final message
     * ("cancelled", "no flight") is shown once, not again by a re-created screen.
     */
    public boolean consumeEndMessage() {
        if (endMessageShown) return false;
        endMessageShown = true;
        return true;
    }

    /** Downloads every recorded flight ("n;" then "r<index>;"). Ignored if already started. */
    public void startFlightList() {
        if (started) return;
        started = true;
        running = true;
        myBT.getIoExecutor().execute(() -> run(-1));
    }

    /** Recovers a raw dump of the given number of samples ("R<samples>;"). Ignored if already started. */
    public void startRecovery(long nbrOfSamples) {
        if (started) return;
        started = true;
        running = true;
        myBT.getIoExecutor().execute(() -> run(nbrOfSamples));
    }

    /** Cancel button, or screen left: the read in progress stops at once. */
    public void cancel() {
        if (!running) return;
        canceled = true;
        myBT.setExit(true);
    }

    @Override
    protected void onCleared() {
        // The screen is really closed (not re-created): stop the download
        cancel();
        super.onCleared();
    }

    /**
     * Download, on the serial I/O executor.
     *
     * @param recoverySamples -1 for the flight list, otherwise the number of samples of the
     *                        recovery dump
     */
    private void run(long recoverySamples) {
        List<String> names = Collections.emptyList();
        try {
            if (myBT.getConnected() && !canceled) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Flush serial I/O buffers before polling
                myBT.flush();
                myBT.clearInput();
                myBT.setNbrOfFlight(0);
                myBT.getFlightData().ClearFlight();

                // Number of recorded flights
                myBT.write("n;");
                myBT.flush();
                myBT.waitForData(10000);
                myBT.setDataReady(false);
                myBT.initFlightData();
                String message = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS,
                        () -> !canceled);
                int nbrOfFlight = 0;
                if ("start nbrOfFlight end".equals(message)) {
                    nbrOfFlight = myBT.getNbrOfFlight();
                }

                if (recoverySamples < 0) {
                    downloadFlights(nbrOfFlight);
                } else if (!canceled) {
                    recover(nbrOfFlight, recoverySamples);
                }

                names = myBT.getFlightData().getAllFlightNames2();
                // Speed and acceleration computed from the altitude curve of each flight
                for (String flight : names) {
                    myBT.getFlightData().computeVelocityAndAcceleration(flight);
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Flight download failed", e);
        } finally {
            running = false;
            result.postValue(new Result(new ArrayList<>(names), canceled));
        }
    }

    /** "r<index>;" for each flight; a flight interrupted by a cancel is dropped. */
    private void downloadFlights(int nbrOfFlight) {
        for (int j = 0; j < nbrOfFlight && !canceled; j++) {
            progress.postValue(getApplication().getString(R.string.retrieving_flight) + (j + 1));
            Log.d(TAG, "Retrieving Flight Nbr: " + j);
            myBT.flush();
            myBT.clearInput();
            myBT.write("r" + j + ";");
            myBT.flush();
            myBT.waitForData(10000);
            myBT.setDataReady(false);
            // Inactivity timeout: a long flight sent at a low speed is never cut
            String message = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS,
                    () -> !canceled);
            if (canceled && (message == null || !message.endsWith("end"))) {
                // Only the flight being received when the download was cancelled is partial
                String partial = myBT.getFlightName(j + 1);
                if (myBT.getFlightData().removeFlight(partial)) {
                    Log.d(TAG, "Incomplete flight dropped: " + partial);
                }
            }
        }
    }

    /** "R<samples>;": raw dump of a flight whose recording was not closed properly. */
    private void recover(int nbrOfFlight, long nbrOfSamples) {
        progress.postValue(getApplication().getString(R.string.retrieving_flight) + nbrOfFlight);
        Log.d(TAG, "Recovery of " + nbrOfSamples + " samples");
        myBT.flush();
        myBT.clearInput();
        myBT.write("R" + nbrOfSamples + ";");
        myBT.flush();
        myBT.waitForData(10000);
        myBT.setDataReady(false);
        // Inactivity timeout: a long dump sent at a low speed is never cut
        String message = myBT.ReadResult(DOWNLOAD_IDLE_TIMEOUT_MS, DOWNLOAD_MAX_DURATION_MS,
                () -> !canceled);
        if (canceled && (message == null || !message.endsWith("end"))) {
            // The dump was interrupted: the flight being received is incomplete
            if (myBT.currentFlightNbr > 0) {
                myBT.getFlightData().removeFlight(myBT.getFlightName(myBT.currentFlightNbr));
            }
        }
    }
}
