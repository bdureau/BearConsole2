package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: AltiConfig4Fragment
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Fourth tab of the altimeter configuration, shown for the AltiServo only: ON and OFF
 *  positions (degrees) of the four servo outputs. The typed values are validated by
 *  validateInputs() before upload.
 * ============================================================================
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.ConfigInputValidator;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;
import com.altimeter.bdureau.bearconsole.config.AltiTabConfigActivity;

public class AltiConfig4Fragment extends Fragment {
    private EditText servo1OnPos, servo2OnPos, servo3OnPos, servo4OnPos;
    private EditText servo1OffPos, servo2OffPos, servo3OffPos, servo4OffPos;
    private TextView txtServo1OnPos, txtServo2OnPos, txtServo3OnPos, txtServo4OnPos;
    private TextView txtServo1OffPos, txtServo2OffPos, txtServo3OffPos, txtServo4OffPos;

    private boolean viewCreated = false;
    private AltiConfigData lAltiCfg;

    public AltiConfig4Fragment() {
        super();
    }

    public AltiConfig4Fragment(AltiConfigData cfg) {
        this.lAltiCfg = cfg;
    }

    public boolean isViewCreated() {
        return viewCreated;
    }

    /**
     * Checks every servo position typed in this tab before upload; invalid fields are flagged.
     *
     * @return true if all the values can be sent to the altimeter
     */
    public boolean validateInputs() {
        boolean ok = true;
        EditText[] positions = {servo1OnPos, servo2OnPos, servo3OnPos, servo4OnPos,
                servo1OffPos, servo2OffPos, servo3OffPos, servo4OffPos};
        for (EditText position : positions) {
            ok &= ConfigInputValidator.checkInt(position, ConfigInputValidator.SERVO_POSITION_MIN, ConfigInputValidator.SERVO_POSITION_MAX);
        }
        return ok;
    }

    public int getServo1OnPos() { return parseInteger(servo1OnPos, 0); }
    public int getServo2OnPos() { return parseInteger(servo2OnPos, 0); }
    public int getServo3OnPos() { return parseInteger(servo3OnPos, 0); }
    public int getServo4OnPos() { return parseInteger(servo4OnPos, 0); }

    public int getServo1OffPos() { return parseInteger(servo1OffPos, 0); }
    public int getServo2OffPos() { return parseInteger(servo2OffPos, 0); }
    public int getServo3OffPos() { return parseInteger(servo3OffPos, 0); }
    public int getServo4OffPos() { return parseInteger(servo4OffPos, 0); }

    public void setServo1OnPos(int pos) { if (servo1OnPos != null) servo1OnPos.setText(String.valueOf(pos)); }
    public void setServo2OnPos(int pos) { if (servo2OnPos != null) servo2OnPos.setText(String.valueOf(pos)); }
    public void setServo3OnPos(int pos) { if (servo3OnPos != null) servo3OnPos.setText(String.valueOf(pos)); }
    public void setServo4OnPos(int pos) { if (servo4OnPos != null) servo4OnPos.setText(String.valueOf(pos)); }

    public void setServo1OffPos(int pos) { if (servo1OffPos != null) servo1OffPos.setText(String.valueOf(pos)); }
    public void setServo2OffPos(int pos) { if (servo2OffPos != null) servo2OffPos.setText(String.valueOf(pos)); }
    public void setServo3OffPos(int pos) { if (servo3OffPos != null) servo3OffPos.setText(String.valueOf(pos)); }
    public void setServo4OffPos(int pos) { if (servo4OffPos != null) servo4OffPos.setText(String.valueOf(pos)); }

    private int parseInteger(EditText editText, int fallback) {
        if (editText == null || editText.getText() == null) return fallback;
        try {
            return Integer.parseInt(editText.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab4, container, false);

        if (lAltiCfg == null && getActivity() instanceof AltiTabConfigActivity) {
            lAltiCfg = ((AltiTabConfigActivity) getActivity()).getAltiConfigData();
        }

        servo1OnPos = view.findViewById(R.id.editTxtServo1OnPos);
        servo2OnPos = view.findViewById(R.id.editTxtServo2OnPos);
        servo3OnPos = view.findViewById(R.id.editTxtServo3OnPos);
        servo4OnPos = view.findViewById(R.id.editTxtServo4OnPos);

        txtServo1OnPos = view.findViewById(R.id.servo1OnPos);
        txtServo2OnPos = view.findViewById(R.id.servo2OnPos);
        txtServo3OnPos = view.findViewById(R.id.servo3OnPos);
        txtServo4OnPos = view.findViewById(R.id.servo4OnPos);

        servo1OffPos = view.findViewById(R.id.editTxtServo1OffPos);
        servo2OffPos = view.findViewById(R.id.editTxtServo2OffPos);
        servo3OffPos = view.findViewById(R.id.editTxtServo3OffPos);
        servo4OffPos = view.findViewById(R.id.editTxtServo4OffPos);

        txtServo1OffPos = view.findViewById(R.id.servo1OffPos);
        txtServo2OffPos = view.findViewById(R.id.servo2OffPos);
        txtServo3OffPos = view.findViewById(R.id.servo3OffPos);
        txtServo4OffPos = view.findViewById(R.id.servo4OffPos);

        populateFields();

        viewCreated = true;
        return view;
    }

    private void populateFields() {
        if (lAltiCfg == null) return;

        servo1OnPos.setText(String.valueOf(lAltiCfg.getServo1OnPos()));
        servo2OnPos.setText(String.valueOf(lAltiCfg.getServo2OnPos()));
        servo3OnPos.setText(String.valueOf(lAltiCfg.getServo3OnPos()));
        servo4OnPos.setText(String.valueOf(lAltiCfg.getServo4OnPos()));

        servo1OffPos.setText(String.valueOf(lAltiCfg.getServo1OffPos()));
        servo2OffPos.setText(String.valueOf(lAltiCfg.getServo2OffPos()));
        servo3OffPos.setText(String.valueOf(lAltiCfg.getServo3OffPos()));
        servo4OffPos.setText(String.valueOf(lAltiCfg.getServo4OffPos()));

        int visibility = "AltiServo".equals(lAltiCfg.getAltimeterName()) ? View.VISIBLE : View.INVISIBLE;

        servo1OnPos.setVisibility(visibility);
        servo2OnPos.setVisibility(visibility);
        servo3OnPos.setVisibility(visibility);
        servo4OnPos.setVisibility(visibility);
        servo1OffPos.setVisibility(visibility);
        servo2OffPos.setVisibility(visibility);
        servo3OffPos.setVisibility(visibility);
        servo4OffPos.setVisibility(visibility);

        txtServo1OnPos.setVisibility(visibility);
        txtServo2OnPos.setVisibility(visibility);
        txtServo3OnPos.setVisibility(visibility);
        txtServo4OnPos.setVisibility(visibility);
        txtServo1OffPos.setVisibility(visibility);
        txtServo2OffPos.setVisibility(visibility);
        txtServo3OffPos.setVisibility(visibility);
        txtServo4OffPos.setVisibility(visibility);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
        servo1OnPos = null;
        servo2OnPos = null;
        servo3OnPos = null;
        servo4OnPos = null;
        servo1OffPos = null;
        servo2OffPos = null;
        servo3OffPos = null;
        servo4OffPos = null;
        txtServo1OnPos = null;
        txtServo2OnPos = null;
        txtServo3OnPos = null;
        txtServo4OnPos = null;
        txtServo1OffPos = null;
        txtServo2OffPos = null;
        txtServo3OffPos = null;
        txtServo4OffPos = null;
    }
}