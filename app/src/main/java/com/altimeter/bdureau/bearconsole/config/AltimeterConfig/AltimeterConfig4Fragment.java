package com.altimeter.bdureau.bearconsole.config.AltimeterConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.config.AltiConfigData;

public class AltimeterConfig4Fragment extends Fragment {
    private static final String TAG = "AltimeterConfig4Fragment";

    private EditText servo1OnPos, servo2OnPos, servo3OnPos, servo4OnPos;
    private EditText servo1OffPos, servo2OffPos, servo3OffPos, servo4OffPos;
    private TextView txtServo1OnPos, txtServo2OnPos, txtServo3OnPos, txtServo4OnPos;
    private TextView txtServo1OffPos, txtServo2OffPos, txtServo3OffPos, txtServo4OffPos;

    private boolean ViewCreated = false;
    private AltiConfigData lAltiCfg;

    public AltimeterConfig4Fragment(AltiConfigData cfg) {
        lAltiCfg = cfg;
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }


    public int getServo1OnPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo1OnPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo2OnPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo2OnPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo3OnPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo3OnPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo4OnPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo4OnPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo1OffPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo1OffPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo2OffPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo2OffPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo3OffPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo3OffPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public int getServo4OffPos() {
        int ret;
        try {
            ret = Integer.parseInt(this.servo4OffPos.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setServo1OnPos(int pos) {
        this.servo1OnPos.setText(pos);
    }

    public void setServo2OnPos(int pos) {
        this.servo2OnPos.setText(pos);
    }

    public void setServo3OnPos(int pos) {
        this.servo3OnPos.setText(pos);
    }

    public void setServo4OnPos(int pos) {
        this.servo4OnPos.setText(pos);
    }

    public void setServo1OffPos(int pos) {
        this.servo1OffPos.setText(pos);
    }

    public void setServo2OffPos(int pos) {
        this.servo2OffPos.setText(pos);
    }

    public void setServo3OffPos(int pos) {
        this.servo3OffPos.setText(pos);
    }

    public void setServo4OffPos(int pos) {
        this.servo4OffPos.setText(pos);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_altimeter_config_tab4, container, false);


        servo1OnPos = (EditText) view.findViewById(R.id.editTxtServo1OnPos);
        servo2OnPos = (EditText) view.findViewById(R.id.editTxtServo2OnPos);
        servo3OnPos = (EditText) view.findViewById(R.id.editTxtServo3OnPos);
        servo4OnPos = (EditText) view.findViewById(R.id.editTxtServo4OnPos);

        txtServo1OnPos = (TextView) view.findViewById(R.id.servo1OnPos);
        txtServo2OnPos = (TextView) view.findViewById(R.id.servo2OnPos);
        txtServo3OnPos = (TextView) view.findViewById(R.id.servo3OnPos);
        txtServo4OnPos = (TextView) view.findViewById(R.id.servo4OnPos);

        servo1OffPos = (EditText) view.findViewById(R.id.editTxtServo1OffPos);
        servo2OffPos = (EditText) view.findViewById(R.id.editTxtServo2OffPos);
        servo3OffPos = (EditText) view.findViewById(R.id.editTxtServo3OffPos);
        servo4OffPos = (EditText) view.findViewById(R.id.editTxtServo4OffPos);

        txtServo1OffPos = (TextView) view.findViewById(R.id.servo1OffPos);
        txtServo2OffPos = (TextView) view.findViewById(R.id.servo2OffPos);
        txtServo3OffPos = (TextView) view.findViewById(R.id.servo3OffPos);
        txtServo4OffPos = (TextView) view.findViewById(R.id.servo4OffPos);

        if (lAltiCfg != null) {

            //Freq.setText(String.valueOf(AltiCfg.getBeepingFrequency()));
            servo1OnPos.setText(String.valueOf(lAltiCfg.getServo1OnPos()));
            servo2OnPos.setText(String.valueOf(lAltiCfg.getServo2OnPos()));
            servo3OnPos.setText(String.valueOf(lAltiCfg.getServo3OnPos()));
            servo4OnPos.setText(String.valueOf(lAltiCfg.getServo4OnPos()));

            servo1OffPos.setText(String.valueOf(lAltiCfg.getServo1OffPos()));
            servo2OffPos.setText(String.valueOf(lAltiCfg.getServo2OffPos()));
            servo3OffPos.setText(String.valueOf(lAltiCfg.getServo3OffPos()));
            servo4OffPos.setText(String.valueOf(lAltiCfg.getServo4OffPos()));
        }
        if (lAltiCfg.getAltimeterName().equals("AltiServo")) {
            servo1OnPos.setVisibility(View.VISIBLE);
            servo2OnPos.setVisibility(View.VISIBLE);
            servo3OnPos.setVisibility(View.VISIBLE);
            servo4OnPos.setVisibility(View.VISIBLE);
            servo1OffPos.setVisibility(View.VISIBLE);
            servo2OffPos.setVisibility(View.VISIBLE);
            servo3OffPos.setVisibility(View.VISIBLE);
            servo4OffPos.setVisibility(View.VISIBLE);

            txtServo1OnPos.setVisibility(View.VISIBLE);
            txtServo2OnPos.setVisibility(View.VISIBLE);
            txtServo3OnPos.setVisibility(View.VISIBLE);
            txtServo4OnPos.setVisibility(View.VISIBLE);
            txtServo1OffPos.setVisibility(View.VISIBLE);
            txtServo2OffPos.setVisibility(View.VISIBLE);
            txtServo3OffPos.setVisibility(View.VISIBLE);
            txtServo4OffPos.setVisibility(View.VISIBLE);
        } else {
            servo1OnPos.setVisibility(View.INVISIBLE);
            servo2OnPos.setVisibility(View.INVISIBLE);
            servo3OnPos.setVisibility(View.INVISIBLE);
            servo4OnPos.setVisibility(View.INVISIBLE);
            servo1OffPos.setVisibility(View.INVISIBLE);
            servo2OffPos.setVisibility(View.INVISIBLE);
            servo3OffPos.setVisibility(View.INVISIBLE);
            servo4OffPos.setVisibility(View.INVISIBLE);

            txtServo1OnPos.setVisibility(View.INVISIBLE);
            txtServo2OnPos.setVisibility(View.INVISIBLE);
            txtServo3OnPos.setVisibility(View.INVISIBLE);
            txtServo4OnPos.setVisibility(View.INVISIBLE);
            txtServo1OffPos.setVisibility(View.INVISIBLE);
            txtServo2OffPos.setVisibility(View.INVISIBLE);
            txtServo3OffPos.setVisibility(View.INVISIBLE);
            txtServo4OffPos.setVisibility(View.INVISIBLE);
        }
        ViewCreated = true;
        return view;
    }

}
