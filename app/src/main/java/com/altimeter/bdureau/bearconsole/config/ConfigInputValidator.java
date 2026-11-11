package com.altimeter.bdureau.bearconsole.config;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: ConfigInputValidator
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Validation of the values typed in the altimeter configuration tabs before they are sent
 *  to the altimeter. A wrong deployment setting can make a parachute open at the wrong time,
 *  so an empty, non numeric or out of range value is never replaced silently by a default:
 *  the field is flagged with an error message showing the accepted range, and the upload is
 *  refused until it is corrected.
 *
 *  The accepted ranges are grouped below as constants so that they can be adjusted to the
 *  limits of the firmware. Fields that are hidden (not used by the connected board) are not
 *  checked.
 * ============================================================================
 */

import android.view.View;
import android.widget.EditText;

import com.altimeter.bdureau.bearconsole.R;

public final class ConfigInputValidator {

    // ---- Accepted ranges (altitudes are in the unit configured on the altimeter) ----
    /** Altitude of the main parachute deployment. */
    public static final int MAIN_ALTITUDE_MIN = 1;
    public static final int MAIN_ALTITUDE_MAX = 9999;
    /** Delay added to an output event, in milliseconds. */
    public static final int OUTPUT_DELAY_MIN = 0;
    public static final int OUTPUT_DELAY_MAX = 60000;
    /** Number of consecutive descending samples confirming the apogee. */
    public static final int APOGEE_MEASURES_MIN = 1;
    public static final int APOGEE_MEASURES_MAX = 100;
    /** Altitude below which the recording stops. */
    public static final int END_RECORD_ALTITUDE_MIN = 0;
    public static final int END_RECORD_ALTITUDE_MAX = 500;
    /** Altitude confirming the lift off. */
    public static final int LIFTOFF_ALTITUDE_MIN = 1;
    public static final int LIFTOFF_ALTITUDE_MAX = 500;
    /** Maximum recording duration, in seconds. */
    public static final int RECORDING_TIMEOUT_MIN = 1;
    public static final int RECORDING_TIMEOUT_MAX = 3600;
    /** Frequency of the beeper, in Hz. */
    public static final int BEEP_FREQUENCY_MIN = 100;
    public static final int BEEP_FREQUENCY_MAX = 10000;
    /** Identifier of the altimeter. */
    public static final int ALTI_ID_MIN = 0;
    public static final int ALTI_ID_MAX = 65535;
    /** Servo position, in degrees. */
    public static final int SERVO_POSITION_MIN = 0;
    public static final int SERVO_POSITION_MAX = 180;
    /** Bluetooth name length; ',' ';' and '$' are forbidden because they delimit the frames. */
    public static final int BLUETOOTH_NAME_MAX_LENGTH = 20;

    private ConfigInputValidator() {
    }

    /**
     * Checks that a field contains an integer within [min, max].
     * On error the field shows a message with the accepted range.
     *
     * @return true if the value is valid or the field is not displayed
     */
    public static boolean checkInt(EditText field, int min, int max) {
        if (field == null || field.getVisibility() != View.VISIBLE) return true;
        String text = field.getText() == null ? "" : field.getText().toString().trim();
        boolean valid;
        try {
            int value = Integer.parseInt(text);
            valid = value >= min && value <= max;
        } catch (NumberFormatException e) {
            valid = false;
        }
        if (!valid) {
            field.setError(field.getContext().getString(R.string.config_error_range, min, max));
        } else {
            field.setError(null);
        }
        return valid;
    }

    /**
     * Checks the Bluetooth module name: not empty, not too long, and free of the characters
     * used by the serial protocol.
     *
     * @return true if the name is valid or the field is not displayed
     */
    public static boolean checkBluetoothName(EditText field) {
        if (field == null || field.getVisibility() != View.VISIBLE) return true;
        String text = field.getText() == null ? "" : field.getText().toString().trim();
        boolean valid = !text.isEmpty()
                && text.length() <= BLUETOOTH_NAME_MAX_LENGTH
                && text.indexOf(',') < 0 && text.indexOf(';') < 0 && text.indexOf('$') < 0;
        if (!valid) {
            field.setError(field.getContext().getString(R.string.config_error_bluetooth_name,
                    BLUETOOTH_NAME_MAX_LENGTH));
        } else {
            field.setError(null);
        }
        return valid;
    }
}
