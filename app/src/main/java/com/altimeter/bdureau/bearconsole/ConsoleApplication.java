package com.altimeter.bdureau.bearconsole;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: ConsoleApplication
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Application singleton shared by every screen of the console. It owns:
 *    - the active link to the altimeter (Bluetooth RFCOMM or USB serial) and exposes
 *      a single write / read API whatever the transport is;
 *    - the serial protocol parser ({@link #ReadResult(long)}), which decodes the
 *      "$sentence,...,checksum;" frames sent by the altimeter firmware (telemetry,
 *      flight data, configuration, test frames, command acknowledgements);
 *    - the shared data models (altimeter configuration, downloaded flights,
 *      application preferences).
 *
 *  Threading rules:
 *    - Every read of the input stream goes through {@link #ReadResult(long, BooleanSupplier)},
 *      which is serialized by an internal lock so that two threads can never consume
 *      the same stream at the same time.
 *    - All waits on the stream are bounded in time ({@link #waitForData(long)}); none of
 *      these methods may be called from the UI thread.
 *    - Malformed frames are logged and skipped: a corrupted packet can never crash the
 *      reading thread.
 * ============================================================================
 */

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.altimeter.bdureau.bearconsole.Flight.FlightData;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.GlobalConfig;
import com.altimeter.bdureau.bearconsole.connection.BluetoothConnection;
import com.altimeter.bdureau.bearconsole.connection.UsbConnection;
import com.altimeter.bdureau.bearconsole.utils.AccelBoards;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import com.altimeter.bdureau.bearconsole.config.AppLanguage;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class ConsoleApplication extends Application {
    private static final String TAG = "ConsoleApplication";

    /** Positive integer or decimal number, as sent by the firmware for most fields. */
    private static final Pattern UNSIGNED_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    /** Signed integer or decimal number (altitude, temperature, GPS coordinates). */
    private static final Pattern SIGNED_NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    /**
     * Telemetry messages ("what" index) whose value can be negative: current altitude (1),
     * apogee altitude (4), main altitude (6), temperature (14), GPS latitude (18) and
     * longitude (19), GPS altitude (23) and accelerometer axes (26..31).
     * Every other telemetry value is a positive number.
     */
    private static final Set<Integer> SIGNED_TELEMETRY_FIELDS = new HashSet<>(
            Arrays.asList(1, 4, 6, 14, 18, 19, 23, 26, 27, 28, 29, 30, 31));

    /**
     * Returns true if the text is a signed integer or decimal number ("-0.5", "12", "-4512").
     * Used by the telemetry screens for the values that can be negative.
     */
    public static boolean isSignedNumber(String value) {
        return value != null && SIGNED_NUMBER.matcher(value).matches();
    }

    /** Pause used while polling an empty stream, so that a reading thread does not spin a CPU core. */
    private static final long POLL_INTERVAL_MS = 2;

    // Number of flights reported by the altimeter ("nbrOfFlight" frame)
    public int NbrOfFlight = 0;
    // Flight number of the last "data" frame received (1-based)
    public int currentFlightNbr = 0;
    // Time stamp of the last activity of the current read (start of the read, then every
    // complete frame received), used for the inactivity timeout
    public long lastReceived = 0;
    // Time stamp of the beginning of the current read, used for the maximum duration
    private long readStart = 0;
    // Last acknowledgement received (OK / KO / UNKNOWN)
    public volatile String commandRet = "";
    // Summary returned by the last connection check
    public String lastReadResult;
    // Raw characters received during the last read (debugging aid)
    public String lastData;

    // Set by the "end" / "OK" frames to tell the waiting screens that the answer is complete
    private volatile boolean dataReady = false;
    // Cancel request for the read in progress (e.g. "Cancel" button while downloading flights)
    private volatile boolean exit = false;

    // Serializes every access to the input stream (one reader at a time)
    private final Object ioLock = new Object();
    // Single background thread on which every screen queues its serial commands, so that the
    // commands of successive screens are executed in order and never on the UI thread
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private FlightData MyFlight = null;
    private AltiConfigData AltiCfg = null;
    private TestTrame testTrame = null;
    // Conversion factor applied to the downloaded altitudes (1 = meters, 3.28084 = feet)
    private double FEET_IN_METER = 1;
    private GlobalConfig AppConf = null;
    private String address, moduleName;
    private volatile String myTypeOfConnection = "bluetooth";
    private BluetoothConnection BTCon = null;
    private UsbConnection UsbCon = null;
    // Handler of the screen currently displaying live telemetry (null when none)
    private volatile Handler mHandler;

    /**
     * Returns a context whose resources use the language selected in the application settings.
     * The language is also set as Locale.getDefault(), used by the help pages and the speech.
     *
     * @param context     base context
     * @param appLanguage setting value: 0 = phone language (English if the application is
     *                    not translated in it), 1.. = forced language (see AppLanguage)
     */
    public static Context updateBaseContextLocale(Context context, int appLanguage) {
        Locale locale = AppLanguage.resolveLocale(appLanguage);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    /**
     * Applies a new language setting to the application context itself (texts built by the
     * application object, such as the flight names), without restarting the application.
     * The screens are re-created by BaseActivity.
     */
    @SuppressWarnings("deprecation")
    public void applyLanguage(int appLanguage) {
        Locale locale = AppLanguage.resolveLocale(appLanguage);
        Locale.setDefault(locale);
        Resources res = getBaseContext().getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences("BearConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(updateBaseContextLocale(base, lang));
    }

    /**
     * Checksum used by the firmware: sum of the bytes of the sentence, modulo 256.
     */
    public static Integer generateCheckSum(String value) {
        byte[] data = value.getBytes();
        long checksum = 0L;

        for (byte b : data) {
            checksum += b;
        }

        checksum = checksum % 256;

        return (int) checksum;
    }

    /**
     * Converts a numeric field to an int without ever throwing.
     * Decimal values ("12.5") are rounded instead of crashing the reading thread.
     *
     * @param value        text received from the altimeter
     * @param defaultValue value returned when the text is not a number
     */
    public static int parseIntSafe(String value, int defaultValue) {
        if (value == null) return defaultValue;
        String v = value.trim();
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            try {
                return (int) Math.round(Double.parseDouble(v));
            } catch (NumberFormatException e2) {
                return defaultValue;
            }
        }
    }

    /**
     * Registers the handler that receives the live telemetry values (message "what" = field index).
     * Pass null to stop the dispatch when the screen is destroyed.
     */
    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * Unregisters the handler only if it is still the current one, so that a screen being
     * destroyed cannot remove the handler of the screen that replaced it.
     */
    public void clearHandler(Handler handler) {
        if (this.mHandler == handler) {
            this.mHandler = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AltiCfg = new AltiConfigData();
        MyFlight = new FlightData(this, AltiCfg.getAltimeterName());
        testTrame = new TestTrame();
        AppConf = new GlobalConfig(this);
        AppConf.ReadConfig();
        BTCon = new BluetoothConnection();
        UsbCon = new UsbConnection();

        myTypeOfConnection = AppConf.getConnectionTypeValue();

        // Detection of a lost Bluetooth link (module switched off, out of range, battery)
        BTCon.setLinkLostListener(this::onBluetoothLinkLost);
        ContextCompat.registerReceiver(this, bluetoothLinkWatcher,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    // ---------------------------------------------------------------------------------------
    // Lost Bluetooth link
    // ---------------------------------------------------------------------------------------

    /** Time of the last link loss (ms), observed by the main screen to refresh its buttons. */
    private final MutableLiveData<Long> linkLostEvent = new MutableLiveData<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Event posted each time the Bluetooth link is found lost (value = time in ms). */
    public LiveData<Long> getLinkLostEvent() {
        return linkLostEvent;
    }

    /**
     * Android reports the end of the Bluetooth link with the module (system broadcast, also
     * received with RECEIVER_NOT_EXPORTED). Before, the loss was never detected: write errors
     * were only logged and getConnected() stayed true, so the screens kept showing "Disconnect"
     * and waited for data that never came.
     */
    private final BroadcastReceiver bluetoothLinkWatcher = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) return;
            BluetoothDevice device = IntentCompat.getParcelableExtra(intent,
                    BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            // Another Bluetooth device of the phone (headset, watch...): not our link
            if (device != null && address != null && !address.equalsIgnoreCase(device.getAddress()))
                return;
            onBluetoothLinkLost();
        }
    };

    /**
     * The Bluetooth link is broken: close it (the reads in progress stop, getConnected()
     * returns false), tell the user and notify the main screen. Nothing is done when the
     * link was already closed (Disconnect button), so a normal disconnection is silent.
     */
    private void onBluetoothLinkLost() {
        if (!"bluetooth".equals(myTypeOfConnection) || !BTCon.getBTConnected()) return;
        Log.w(TAG, "Bluetooth link lost");
        exit = true;
        BTCon.Disconnect();
        linkLostEvent.postValue(System.currentTimeMillis());
        mainHandler.post(() -> Toast.makeText(this, R.string.bt_link_lost, Toast.LENGTH_LONG).show());
    }

    public String getConnectionType() {
        return myTypeOfConnection;
    }

    public void setConnectionType(String TypeOfConnection) {
        myTypeOfConnection = TypeOfConnection;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String bTAddress) {
        address = bTAddress;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String bTmoduleName) {
        moduleName = bTmoduleName;
    }

    /** Input stream of the active transport (may be null when not connected). */
    public InputStream getInputStream() {
        if (myTypeOfConnection.equals("bluetooth")) {
            return BTCon.getInputStream();
        } else {
            return UsbCon.getInputStream();
        }
    }

    /** Shared, ordered background executor for every blocking serial operation. */
    public ExecutorService getIoExecutor() {
        return ioExecutor;
    }

    public UsbConnection getUsbCon() {
        return UsbCon;
    }

    public boolean getConnected() {
        if (myTypeOfConnection.equals("bluetooth")) {
            return BTCon.getBTConnected();
        } else {
            return UsbCon.getUSBConnected();
        }
    }

    public void setConnected(boolean Connected) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.setBTConnected(Connected);
        } else {
            UsbCon.setUSBConnected(Connected);
        }
    }

    public AltiConfigData getAltiConfigData() {
        return AltiCfg;
    }

    public void setAltiConfigData(AltiConfigData configData) {
        AltiCfg = configData;
    }

    public TestTrame getTestTrame() {
        return testTrame;
    }

    public FlightData getFlightData() {
        return MyFlight;
    }

    public void setFlightData(FlightData fData) {
        MyFlight = fData;
    }

    public int getNbrOfFlight() {
        return NbrOfFlight;
    }

    public void setNbrOfFlight(int value) {
        NbrOfFlight = value;
    }

    /*
     * Opens the Bluetooth link to the module selected in {@link #setAddress(String)} and checks
     * that an altimeter answers. Blocking: must be called from a background thread.
     */
    public boolean connect() {
        boolean state = false;
        if (myTypeOfConnection.equals("bluetooth")) {
            if (ContextCompat.checkSelfPermission(ConsoleApplication.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return false;
                }
            }
            state = BTCon.connect(address, getApplicationContext());
            setConnectionType("bluetooth");
            // Long wait: an ATmega328 altimeter (AltiMulti, AltiMultiV2, AltiServo, AltiDuo)
            // does not read its serial port while it beeps the last altitude (see
            // isConnectionValid(long)). No check at all if the socket could not be opened.
            if (!state || !isConnectionValid(BT_CONNECTION_CHECK_TIMEOUT_MS)) {
                Disconnect();
                state = false;
            }
        }
        return state;
    }

    /**
     * Opens the USB serial link and checks that an altimeter answers.
     * Blocking: must be called from a background thread.
     */
    public boolean connect(UsbManager usbManager, UsbDevice device, int baudRate) {
        boolean state = false;
        if (myTypeOfConnection.equals("usb")) {
            state = UsbCon.connect(usbManager, device, baudRate);
            setConnectionType("usb");

            for (int i = 0; i < 3; i++) {
                if (isConnectionValid()) {
                    state = true;
                    break;
                }
            }
            if (!state)
                Disconnect();
        }
        return state;
    }

    /** Answer delay accepted for the "h;" handshake over USB (each of the 3 attempts). */
    private static final long USB_CONNECTION_CHECK_TIMEOUT_MS = 3000;
    /**
     * Answer delay accepted for the "h;" handshake over Bluetooth. The ATmega328 firmwares
     * (RocketFlightLogger for AltiMulti / AltiMultiV2, AltiServo, AltiDuo) beep the last apogee
     * and main altitudes every 10 s with blocking delays (1.5 s per long beep, 5 s pause after
     * each altitude): the altimeter can stay deaf for 15 s on the bench and for more than 45 s
     * after a 500 m flight. The command stays in the UART buffer and is answered when the
     * beeping ends, so the application must wait that long. The user can cancel.
     */
    public static final long BT_CONNECTION_CHECK_TIMEOUT_MS = 60000;

    /** Handshake with the USB delay (see {@link #isConnectionValid(long)}). */
    public boolean isConnectionValid() {
        return isConnectionValid(USB_CONNECTION_CHECK_TIMEOUT_MS);
    }

    /**
     * Sends the "h;" handshake and waits for the "OK" answer of the firmware, then switches
     * the telemetry stream off with "y0;".
     *
     * "h;" is sent twice and the "$UNKNOWN;" / "$KO;" answers are ignored here: the first
     * command can be corrupted by a byte received when the Bluetooth link opens (the ATmega
     * firmwares keep every byte except '\n' in the command), the second one is then answered
     * "OK". The wait ends as soon as "OK" arrives, on timeout, on cancel (setExit) or when
     * the link is closed.
     *
     * @param maxWaitMs maximum time to wait for the "OK" answer
     */
    public boolean isConnectionValid(long maxWaitMs) {
        boolean valid = false;
        setDataReady(false);
        flush();
        clearInput();
        write("h;");

        flush();
        clearInput();
        write("h;");

        String myMessage = "";
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) break;
            myMessage = ReadResult(remaining);
            if ("OK".equals(myMessage)) {
                valid = true;
                break;
            }
            // Cancelled by the user (setExit), or link closed: stop waiting
            if (this.exit || !isLinkOpen()) break;
            try {
                // ReadResult returned early (refused command): short pause before reading again
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        lastReadResult = myMessage;

        flush();
        clearInput();
        write("y0;");

        return valid;
    }

    /** True while the transport (Bluetooth socket or USB port) is open. */
    private boolean isLinkOpen() {
        if (myTypeOfConnection.equals("bluetooth")) {
            return BTCon.getBTConnected() && BTCon.getInputStream() != null;
        }
        return getInputStream() != null;
    }

    /**
     * Waits until at least one byte is available on the input stream, or until the timeout.
     * Replaces the former unbounded busy-wait loops.
     *
     * @param timeoutMs maximum waiting time in milliseconds
     * @return true if data is available, false on timeout, disconnection or interruption
     */
    public boolean waitForData(long timeoutMs) {
        long start = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - start < timeoutMs) {
                InputStream in = getInputStream();
                if (in == null) return false;
                if (in.available() > 0) return true;
                Thread.sleep(10);
            }
        } catch (IOException e) {
            Log.w(TAG, "waitForData: stream error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public void Disconnect() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.Disconnect();
        } else {
            UsbCon.Disconnect();
        }
    }

    public void flush() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.flush();
        }
    }

    public void write(String data) {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.write(data);
        } else {
            UsbCon.write(data);
        }
    }

    public void clearInput() {
        if (myTypeOfConnection.equals("bluetooth")) {
            BTCon.clearInput();
        } else {
            UsbCon.clearInput();
        }
    }

    /**
     * Resets the flight repository before a download and selects the altitude unit
     * used when storing the received samples.
     */
    public void initFlightData() {
        MyFlight = new FlightData(this, AltiCfg.getAltimeterName());
        // No flight received yet in this download
        currentFlightNbr = 0;
        if (AppConf.getUnits() == 0) {
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }
    }

    /**
     * Requests the read in progress to stop as soon as possible.
     * Reading loops must also check their own "keep running" flag (see
     * {@link #ReadResult(long, BooleanSupplier)}) so that the request is never lost.
     */
    public void setExit(boolean b) {
        this.exit = b;
    }

    /** Standard gravity (m/s^2), used to express an acceleration in g. */
    public static final double STANDARD_GRAVITY = 9.80665;

    /**
     * Converts an accelerometer value sent by the firmware to m/s^2. The firmware sends
     * thousandths of m/s^2 (the m/s^2 of the Adafruit sensor events, or of the calibrated
     * values of the 2.5 firmware, multiplied by 1000), not thousandths of g.
     */
    public static float accelToMs2(int milliMs2) {
        return (float) (milliMs2 / 1000.0);
    }

    /** Checksum of a split sentence: every field except the last one, each followed by a comma. */
    public long calculateSentenceCHK(String[] currentSentence) {
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < currentSentence.length - 1; i++) {
            sentence.append(currentSentence[i]).append(',');
        }
        return generateCheckSum(sentence.toString());
    }

    /**
     * Reads and decodes frames until an "end" / "OK" frame, the timeout, or a cancel request.
     *
     * @param timeout maximum duration of the read in milliseconds
     * @return summary of the frames received (e.g. "start alticonfig end", "OK")
     */
    public String ReadResult(long timeout) {
        return ReadResult(timeout, null);
    }

    /**
     * Same as {@link #ReadResult(long)}, with an additional stop condition evaluated
     * continuously. Live telemetry loops pass their own running flag here so that a stop
     * request issued between two reads can never be lost.
     *
     * @param timeout     maximum duration of the read in milliseconds
     * @param keepRunning optional condition; the read stops as soon as it returns false
     */
    public String ReadResult(long timeout, BooleanSupplier keepRunning) {
        // Short reads (acknowledgements, configuration, telemetry slices): the timeout is a
        // maximum duration, so a telemetry stream that keeps arriving cannot extend the wait
        return ReadResult(timeout, timeout, keepRunning);
    }

    /**
     * Long reads (download of recorded flights): the read only stops when the altimeter has
     * been silent for idleTimeout, so that a long flight sent at a low serial speed is never
     * cut after a fixed delay. maxDuration remains as a safety net against a link that keeps
     * sending without ever ending the answer.
     *
     * @param idleTimeout maximum time without a complete frame, in milliseconds
     * @param maxDuration maximum duration of the whole read, in milliseconds
     * @param keepRunning optional condition; the read stops as soon as it returns false
     */
    public String ReadResult(long idleTimeout, long maxDuration, BooleanSupplier keepRunning) {
        synchronized (ioLock) {
            this.exit = false;
            StringBuilder rawData = new StringBuilder();
            String myMessage = "";
            readStart = System.currentTimeMillis();
            lastReceived = readStart;
            final long timeout = idleTimeout;
            boolean done = false;
            try {
                while (!done && !this.exit && (keepRunning == null || keepRunning.getAsBoolean())) {
                    if (isReadExpired(timeout, maxDuration))
                        break;

                    InputStream in = getInputStream();
                    if (in == null)
                        break;

                    if (in.available() <= 0) {
                        // Nothing to read yet: yield the CPU instead of spinning
                        Thread.sleep(POLL_INTERVAL_MS);
                        continue;
                    }

                    int b = in.read();
                    if (b < 0)
                        break; // stream closed
                    char ch = (char) b;
                    rawData.append(ch);
                    if (ch != '$')
                        continue;

                    // A sentence starts: read it up to ';', with the same timeout and cancel rules
                    String tempBuff = readSentenceBody(in, timeout, maxDuration, keepRunning);
                    if (tempBuff == null)
                        break; // timeout, cancel or stream closed in the middle of a sentence
                    // A complete frame was received: the altimeter is still talking
                    lastReceived = System.currentTimeMillis();
                    if (tempBuff.isEmpty())
                        continue; // "$;" carries nothing

                    String[] currentSentence = tempBuff.split(",");
                    try {
                        SentenceResult r = processSentence(currentSentence, myMessage);
                        myMessage = r.message;
                        if (r.done) done = true;
                    } catch (RuntimeException e) {
                        // A malformed frame must never kill the reading thread
                        Log.w(TAG, "Malformed frame skipped: " + tempBuff, e);
                    }
                }
            } catch (IOException e) {
                myMessage = myMessage + " error:" + e.getMessage();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lastData = rawData.toString();
            return myMessage;
        }
    }

    /**
     * Reads one text line sent by the altimeter (up to '\n', CR dropped). Used by the
     * accelerometer calibration ("j;" command), during which the firmware sends plain text
     * messages instead of "$...;" frames.
     *
     * @param idleTimeoutMs maximum time without any received character
     * @param keepRunning   optional condition; the read stops as soon as it returns false
     * @return the line, or null on timeout, cancel request, error or closed stream
     */
    public String readLine(long idleTimeoutMs, BooleanSupplier keepRunning) {
        synchronized (ioLock) {
            StringBuilder sb = new StringBuilder();
            long last = System.currentTimeMillis();
            try {
                while (keepRunning == null || keepRunning.getAsBoolean()) {
                    InputStream in = getInputStream();
                    if (in == null)
                        return null;
                    if (in.available() <= 0) {
                        if (System.currentTimeMillis() - last > idleTimeoutMs)
                            return null;
                        Thread.sleep(POLL_INTERVAL_MS);
                        continue;
                    }
                    int b = in.read();
                    if (b < 0)
                        return null;
                    last = System.currentTimeMillis();
                    char ch = (char) b;
                    if (ch == '\n')
                        return sb.toString();
                    if (ch != '\r')
                        sb.append(ch);
                }
            } catch (IOException e) {
                Log.w(TAG, "readLine: stream error", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /**
     * Reads the characters of a sentence after the '$' and up to the terminating ';'.
     * CR / LF characters are dropped.
     *
     * @return the sentence content, or null on timeout, cancel request or closed stream
     */
    private String readSentenceBody(InputStream in, long timeout, long maxDuration,
                                    BooleanSupplier keepRunning)
            throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (this.exit || (keepRunning != null && !keepRunning.getAsBoolean()))
                return null;
            if (isReadExpired(timeout, maxDuration))
                return null;
            if (in.available() <= 0) {
                Thread.sleep(POLL_INTERVAL_MS);
                continue;
            }
            int b = in.read();
            if (b < 0)
                return null;
            char ch = (char) b;
            if (ch == ';')
                return sb.toString();
            if (ch != '\r' && ch != '\n')
                sb.append(ch);
        }
    }

    /**
     * True when the current read must stop: no complete frame for idleTimeout, or total
     * duration above maxDuration.
     */
    private boolean isReadExpired(long idleTimeout, long maxDuration) {
        long now = System.currentTimeMillis();
        return (now - lastReceived) > idleTimeout || (now - readStart) > maxDuration;
    }

    /** Outcome of the decoding of one sentence. */
    private static final class SentenceResult {
        final String message;
        final boolean done;

        SentenceResult(String message, boolean done) {
            this.message = message;
            this.done = done;
        }
    }

    // ---------------------------------------------------------------------------------------
    // Field helpers: every numeric field is validated before conversion
    // ---------------------------------------------------------------------------------------

    /** True if the sentence contains the field at this index. */
    private static boolean has(String[] s, int index) {
        return s.length > index && s[index] != null;
    }

    /** Positive numeric field, or the default value when missing or not a number. */
    private static int uField(String[] s, int index, int defaultValue) {
        if (has(s, index) && UNSIGNED_NUMBER.matcher(s[index]).matches())
            return parseIntSafe(s[index], defaultValue);
        return defaultValue;
    }

    /** Signed numeric field, or the default value when missing or not a number. */
    private static int sField(String[] s, int index, int defaultValue) {
        if (has(s, index) && SIGNED_NUMBER.matcher(s[index]).matches())
            return parseIntSafe(s[index], defaultValue);
        return defaultValue;
    }

    /** True if the last field (checksum) matches the checksum computed on the sentence. */
    private boolean isChecksumValid(String[] s) {
        String last = s[s.length - 1];
        long chk = 0;
        if (last != null && UNSIGNED_NUMBER.matcher(last).matches()) {
            chk = parseIntSafe(last, 0);
        }
        return calculateSentenceCHK(s) == chk;
    }

    /**
     * Forwards a telemetry field to the registered screen.
     * Non numeric values are replaced by "0", as the screens expect numbers.
     * Negative values are kept for the fields listed in {@link #SIGNED_TELEMETRY_FIELDS}
     * (altitudes, temperature, GPS coordinates, accelerations): a western longitude or a
     * southern latitude must not be turned into 0.
     */
    private static void sendTelemetryField(Handler handler, String[] s, int index, int what) {
        if (!has(s, index)) return;
        Pattern accepted = SIGNED_TELEMETRY_FIELDS.contains(what) ? SIGNED_NUMBER : UNSIGNED_NUMBER;
        String value = accepted.matcher(s[index]).matches() ? s[index] : "0";
        handler.obtainMessage(what, value).sendToTarget();
    }

    private boolean isAltimeter(String... names) {
        String current = AltiCfg.getAltimeterName();
        for (String n : names) {
            if (n.equals(current)) return true;
        }
        return false;
    }

    /**
     * Decodes one sentence and updates the data models.
     *
     * @param s         sentence split on commas (first field = sentence type)
     * @param myMessage summary built so far during the current read
     */
    private SentenceResult processSentence(String[] s, String myMessage) {
        if (s.length == 0 || s[0] == null)
            return new SentenceResult(myMessage, false);

        switch (s[0]) {
            case "telemetry":
                decodeTelemetry(s);
                return new SentenceResult(myMessage, false);

            case "data":
                decodeFlightData(s);
                return new SentenceResult(myMessage, false);

            case "alticonfig":
                if (isChecksumValid(s)) {
                    decodeAltiConfig(s);
                    return new SentenceResult(myMessage + " alticonfig", false);
                }
                return new SentenceResult(myMessage + "KO alticonfig", false);

            case "testTrame":
                testTrame.setTrameStatus(isChecksumValid(s));
                if (s.length > 1)
                    testTrame.setCurrentTrame(s[1]);
                else
                    testTrame.setCurrentTrame("Error reading packet");
                return new SentenceResult(myMessage + " testTrame", false);

            case "nbrOfFlight":
                if (has(s, 1) && UNSIGNED_NUMBER.matcher(s[1]).matches())
                    NbrOfFlight = parseIntSafe(s[1], NbrOfFlight);
                return new SentenceResult(myMessage + " nbrOfFlight", false);

            case "start":
                setDataReady(false);
                return new SentenceResult("start", false);

            case "end":
                setDataReady(true);
                return new SentenceResult(myMessage + " end", true);

            case "OK":
                setDataReady(true);
                commandRet = s[0];
                return new SentenceResult("OK", true);

            case "KO":
            case "UNKNOWN":
                // A refused or unknown command is a complete answer: stop waiting at once
                // instead of waiting for the whole timeout (the caller sees "not OK")
                setDataReady(true);
                commandRet = s[0];
                return new SentenceResult(myMessage, true);

            default:
                return new SentenceResult(myMessage, false);
        }
    }

    /** "telemetry" frame: live values forwarded to the screen registered with setHandler(). */
    private void decodeTelemetry(String[] s) {
        Handler handler = mHandler;
        if (handler == null || !isChecksumValid(s))
            return;

        // Fields 1..17 are common to every altimeter; message "what" = field index
        for (int i = 1; i <= 17; i++) {
            sendTelemetryField(handler, s, i, i);
        }

        // Accelerometer boards: fields 18..23 -> messages 26..31
        if (isAltimeter("AltiMultiESP32_accel", "AltiMultiESP32_accel_345", "AltiMultiESP32_accel_375")) {
            for (int i = 18; i <= 23; i++) {
                sendTelemetryField(handler, s, i, i + 8);
            }
        }

        // TTGO and Ultimate boards: one 3-axis accelerometer, fields 18..20 -> messages 26..28
        if (isAltimeter("TTGOBearAltimeter", "UltimateAltimeter")) {
            for (int i = 18; i <= 20; i++) {
                sendTelemetryField(handler, s, i, i + 8);
            }
        }

        // GPS board: fields 18..25 -> messages 18..25
        if (isAltimeter("AltiGPS")) {
            for (int i = 18; i <= 25; i++) {
                sendTelemetryField(handler, s, i, i);
            }
        }
    }

    /**
     * "data" frame: one sample of a recorded flight.
     * Field 1 = flight index, 2 = time (ms), 3 = altitude, 4 = temperature, 5 = pressure,
     * then board specific channels (battery, accelerometers, GPS).
     */
    private void decodeFlightData(String[] s) {
        if (!isChecksumValid(s))
            return;

        String flightName = "FlightXX";
        if (has(s, 1) && UNSIGNED_NUMBER.matcher(s[1]).matches()) {
            currentFlightNbr = parseIntSafe(s[1], 0) + 1;
            flightName = getFlightName(currentFlightNbr);
        }

        int time = uField(s, 2, 0);

        if (has(s, 3))
            MyFlight.AddToFlight(time, (long) (sField(s, 3, 0) * FEET_IN_METER), flightName, 0);
        if (has(s, 4))
            MyFlight.AddToFlight(time, (long) sField(s, 4, 0), flightName, 1);
        if (has(s, 5))
            MyFlight.AddToFlight(time, (long) uField(s, 5, 0), flightName, 2);

        // Battery voltage (hundredths of volt)
        if (isAltimeter("AltiGPS", "AltiMultiESP32", "AltiMultiESP32_accel", "AltiMultiESP32_accel_375",
                "AltiMultiESP32_accel_345", "AltiMultiSTM32", "TTGOBearAltimeter", "TTGOMiniBearAltimeter")) {
            if (has(s, 6))
                MyFlight.AddToFlight(time, (float) uField(s, 6, 0) / 100, flightName, 5);
        }

        // Accelerometer channels are signed (a deceleration is negative): read them with sField.
        // The firmware sends thousandths of m/s^2; they are stored in m/s^2 (value / 1000),
        // whatever the altitude unit of the application.
        // TTGO boards: one 3-axis accelerometer
        if (isAltimeter("TTGOBearAltimeter", "TTGOMiniBearAltimeter")) {
            for (int i = 7; i <= 9; i++) {
                if (has(s, i))
                    MyFlight.AddToFlight(time, accelToMs2(sField(s, i, 0)), flightName, i - 1);
            }
        }

        // Ultimate altimeter: one 3-axis accelerometer (QMI8658) in fields 6..8 (no battery
        // channel). Its firmware sends thousandths of g, not of m/s^2 (see AccelBoards).
        if (isAltimeter("UltimateAltimeter")) {
            for (int i = 6; i <= 8; i++) {
                if (has(s, i))
                    MyFlight.AddToFlight(time, AccelBoards.toMs2("UltimateAltimeter",
                            sField(s, i, 0)), flightName, i - 1);
            }
        }

        // ESP32 accelerometer boards: two 3-axis accelerometers in fields 7..12
        if (isAltimeter("AltiMultiESP32_accel", "AltiMultiESP32_accel_375", "AltiMultiESP32_accel_345")) {
            for (int i = 7; i <= 12; i++) {
                if (has(s, i))
                    MyFlight.AddToFlight(time, accelToMs2(sField(s, i, 0)), flightName, i - 1);
            }
        }

        // GPS board: latitude / longitude (1e-5 degree), GPS altitude, satellites, speed, sea altitude
        if (isAltimeter("AltiGPS")) {
            // Coordinates kept in double: a float only has 7 significant digits, which loses
            // up to ~1.7 m of precision on a 5-decimal longitude above 128 degrees
            if (has(s, 7))
                MyFlight.AddToFlight(time, sField(s, 7, 0) / 100000.0, flightName, 6);
            if (has(s, 8))
                MyFlight.AddToFlight(time, sField(s, 8, 0) / 100000.0, flightName, 7);
            if (has(s, 9))
                MyFlight.AddToFlight(time, (float) sField(s, 9, 0), flightName, 8);
            if (has(s, 10))
                MyFlight.AddToFlight(time, (float) uField(s, 10, 0), flightName, 9);
            if (has(s, 11))
                MyFlight.AddToFlight(time, (float) sField(s, 11, 0), flightName, 10);
            if (has(s, 12))
                MyFlight.AddToFlight(time, (float) sField(s, 12, 0), flightName, 11);
        }
    }

    /**
     * Name under which a downloaded flight is stored ("Flight 01", "Flight 12", ...).
     * Used by the decoder and by the screens that must find a flight from its number
     * (e.g. to drop the flight being downloaded when the download is cancelled).
     *
     * @param flightNbr 1-based flight number (firmware index + 1)
     */
    public String getFlightName(int flightNbr) {
        String prefix = getString(R.string.flight_name);
        return (flightNbr < 10) ? prefix + " 0" + flightNbr : prefix + " " + flightNbr;
    }

    /** "alticonfig" frame: full configuration stored in the altimeter EEPROM. */
    private void decodeAltiConfig(String[] s) {
        if (has(s, 1)) AltiCfg.setUnits(uField(s, 1, 0));
        if (has(s, 2)) AltiCfg.setBeepingMode(uField(s, 2, 0));
        if (has(s, 3)) AltiCfg.setOutput1(uField(s, 3, 0));
        if (has(s, 4)) AltiCfg.setOutput2(uField(s, 4, 0));
        if (has(s, 5)) AltiCfg.setOutput3(uField(s, 5, 0));
        if (has(s, 6)) AltiCfg.setSupersonicYesNo(uField(s, 6, 0));
        if (has(s, 7)) AltiCfg.setMainAltitude(uField(s, 7, 50));
        if (has(s, 8)) AltiCfg.setAltimeterName(s[8]);
        if (has(s, 9)) AltiCfg.setAltiMajorVersion(uField(s, 9, 0));
        if (has(s, 10)) AltiCfg.setAltiMinorVersion(uField(s, 10, 0));
        if (has(s, 11)) AltiCfg.setOutput1Delay(uField(s, 11, 0));
        if (has(s, 12)) AltiCfg.setOutput2Delay(uField(s, 12, 0));
        if (has(s, 13)) AltiCfg.setOutput3Delay(uField(s, 13, 0));
        if (has(s, 14)) AltiCfg.setBeepingFrequency(uField(s, 14, 440));
        if (has(s, 15)) AltiCfg.setNbrOfMeasuresForApogee(uField(s, 15, 0));
        if (has(s, 16)) AltiCfg.setEndRecordAltitude(uField(s, 16, 5));
        if (has(s, 17)) AltiCfg.setTelemetryType(uField(s, 17, 0));
        if (has(s, 18)) AltiCfg.setSupersonicDelay(uField(s, 18, 0));
        if (has(s, 19)) AltiCfg.setConnectionSpeed(uField(s, 19, 38400));
        if (has(s, 20)) AltiCfg.setAltimeterResolution(uField(s, 20, 0));
        if (has(s, 21)) AltiCfg.setEepromSize(uField(s, 21, 512));
        if (has(s, 22)) AltiCfg.setBeepOnOff(uField(s, 22, 0));
        if (has(s, 23)) AltiCfg.setOutput4(uField(s, 23, -1));
        if (has(s, 24)) AltiCfg.setOutput4Delay(uField(s, 24, -1));
        if (has(s, 25)) AltiCfg.setLiftOffAltitude(uField(s, 25, 20));
        if (has(s, 26)) AltiCfg.setBatteryType(uField(s, 26, 0));
        if (has(s, 27)) AltiCfg.setRecordingTimeout(uField(s, 27, 120));
        if (has(s, 28)) AltiCfg.setAltiID(uField(s, 28, 0));
        if (has(s, 29)) AltiCfg.setUseTelemetryPort(uField(s, 29, 0));

        // Field 30: servo switch on AltiServo, Bluetooth name on recent ESP32 / TTGO firmwares.
        // The name and version (fields 8..10) are decoded above, so the version rule can be
        // applied here. Same rule as the configuration screen and the upload.
        if (AltiCfg.getAltimeterName().equals("AltiServo")) {
            if (has(s, 30))
                AltiCfg.setServoSwitch(uField(s, 30, 0));
        } else if (AltiCfg.supportsBluetoothName()) {
            if (has(s, 30))
                AltiCfg.setBluetoothName(s[30]);
        }

        // Servo positions (AltiServo only, -1 when not used)
        if (has(s, 31)) AltiCfg.setServo1OnPos(uField(s, 31, -1));
        if (has(s, 32)) AltiCfg.setServo2OnPos(uField(s, 32, -1));
        if (has(s, 33)) AltiCfg.setServo3OnPos(uField(s, 33, -1));
        if (has(s, 34)) AltiCfg.setServo4OnPos(uField(s, 34, -1));
        if (has(s, 35)) AltiCfg.setServo1OffPos(uField(s, 35, -1));
        if (has(s, 36)) AltiCfg.setServo2OffPos(uField(s, 36, -1));
        if (has(s, 37)) AltiCfg.setServo3OffPos(uField(s, 37, -1));
        if (has(s, 38)) AltiCfg.setServo4OffPos(uField(s, 38, -1));
        if (has(s, 39)) AltiCfg.setServoStayOn(uField(s, 39, 0));
    }

    public boolean getDataReady() {
        return dataReady;
    }

    public void setDataReady(boolean value) {
        dataReady = value;
    }

    public GlobalConfig getAppConf() {
        return AppConf;
    }

    public void setAppConf(GlobalConfig value) {
        AppConf = value;
    }

    /**
     * Last "testTrame" frame received, used by the connection test screen to display
     * the frame and whether its checksum was valid.
     */
    public class TestTrame {
        private volatile String currentTrame = "";
        private volatile boolean trameStatus = false;

        public String getCurrentTrame() {
            return currentTrame;
        }

        public void setCurrentTrame(String trame) {
            currentTrame = trame;
        }

        public boolean getTrameStatus() {
            return trameStatus;
        }

        public void setTrameStatus(boolean val) {
            trameStatus = val;
        }
    }
}
