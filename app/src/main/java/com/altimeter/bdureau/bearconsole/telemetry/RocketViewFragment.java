package com.altimeter.bdureau.bearconsole.telemetry;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.altimeter.bdureau.bearconsole.R;
import com.altimeter.bdureau.bearconsole.utils.Rocket;

import processing.android.PFragment;
import processing.core.PApplet;
/*
    This is the third tab
    it displays the rocket orientation
     */
public class RocketViewFragment extends Fragment {
    private static final String TAG = "Tab3StatusFragment";
    private PApplet myRocket;
    boolean ViewCreated = false;
    private PFragment fragment;
    private View view;
    public boolean isViewCreated() {
        return ViewCreated;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_altimeter_status_part3,container,false);

        myRocket = new Rocket();
        fragment = new PFragment(myRocket);

        getChildFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragment.onDestroy();
        myRocket.onDestroy();
    }
    @Override
    public void onStop() {

        Log.d(TAG, "onStop");
        super.onStop();
        fragment.onStop();
        myRocket.onStop();

    }
    @Override
    public void onStart() {
        super.onStart();
        fragment.requestDraw();
        view.refreshDrawableState();
        view.bringToFront();
    }
    @Override
    public void onResume() {
        super.onResume();
        fragment.onResume(); //perhaps we can remove that
        //This is the only way I can redraw the rocket after leaving the tab
        getChildFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
    }
    public void setInputString(double value[]) {
        double x,y,z;
        x = value[0];
        y = value[1];
        z = value[2];
        //convert accel to quaternion
        double R =  sqrt((x*x)+(y*y)+(z*z));
        double roll = acos(x/R)*180/Math.PI;
        double pitch = acos(y/R)*180/Math.PI;
        double yaw = acos(z/R)*180/Math.PI;
        double qx = (roll/2) * cos(pitch/2) * cos(yaw/2) - cos(roll/2) * sin(pitch/2) * sin(yaw/2);
        double qy = cos(roll/2) * sin(pitch/2) * cos(yaw/2) + sin(roll/2) * cos(pitch/2) * sin(yaw/2);
        double qz = cos(roll/2) * cos(pitch/2) * sin(yaw/2) - sin(roll/2) * sin(pitch/2) * cos(yaw/2);
        double qw = cos(roll/2) * cos(pitch/2) * cos(yaw/2) + sin(roll/2) * sin(pitch/2) * sin(yaw/2);
        /*setInputString(String.format("%.2f",qx)+","+String.format("%.2f",qy)+","+String.format("%.2f",qz)+
                ","+String.format("%.2f",qw)+ ",\\r\\n");*/
        String value2 = String.format("%.2f",qx)+","+String.format("%.2f",qy)+","+String.format("%.2f",qz)+
                ","+String.format("%.2f",qw)+ ",\\r\\n";
        if(view != null)
            //((Rocket) myRocket).setInputString2(value2);
            ((Rocket) myRocket).setInputString((float) x, (float) y, (float) z, 0);
    }
    //send the quaternion to the processing widget
    public void setInputString(String value) {
        //if (ViewCreated)
        if(view != null)
            ((Rocket) myRocket).setInputString(value);
    }

    public void setInputCorrect(String value) {
        if(view != null)
            ((Rocket) myRocket).setInputCorrect(value);
    }
    public void setServoX(String value) {
        if(view != null)
            ((Rocket) myRocket).setServoX(value);
    }
    public void setServoY(String value) {
        if(view != null)
            ((Rocket) myRocket).setServoY(value);
    }
}
