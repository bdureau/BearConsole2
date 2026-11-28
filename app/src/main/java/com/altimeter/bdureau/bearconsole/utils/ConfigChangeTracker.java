package com.altimeter.bdureau.bearconsole.utils;

/*
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: ConfigChangeTracker
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Detects the unsaved changes of a configuration screen made of tab fragments
 *  (application settings, altimeter configuration), so that the screen can ask for a
 *  confirmation before it is left without saving.
 *   - When the view of a tab fragment is created, its fields have just been filled with the
 *     stored configuration (and not yet with a restored view state): the state of its input
 *     widgets is recorded as the reference (snapshot). The fragment lifecycle callbacks of
 *     the activity are used, so the fragments need no change.
 *   - The state of a fragment is read from its view tree, whatever the field: text of the
 *     EditText fields (an invalid value typed by the user is also a change), position of
 *     the Spinners, state of the check boxes and switches, position of the SeekBars.
 *     Hidden fields are read in the same way in the reference and now, so they never
 *     create a false change.
 *   - isModified(): a field differs from its reference, or markModified() was called (for
 *     example after a restore of the default values).
 *   - resetReference(): after a save, or when a fragment changes a field by program (list
 *     of voices loaded later), the current state becomes the reference.
 *  The tab fragments must keep their view while the screen is displayed (ViewPager
 *  setOffscreenPageLimit() >= number of tabs - 1): the fields of a destroyed view can not be
 *  read.
 * ============================================================================
 */

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ConfigChangeTracker {

    /** Reference state of each tab fragment, recorded when its view was created. */
    private final Map<Fragment, String> references = new IdentityHashMap<>();
    /** Change that is not visible in the fields (e.g. default values restored). */
    private boolean forcedModified = false;

    /**
     * Starts tracking the tab fragments of the activity. To be called in onCreate(), after
     * super.onCreate(): the fragments re-created by the system are tracked too.
     */
    public void attach(@NonNull FragmentActivity activity) {
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                                      @NonNull View v, @Nullable Bundle savedInstanceState) {
                        // Fields just filled from the stored configuration: reference state
                        references.put(f, stateOf(v));
                    }

                    @Override
                    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        references.remove(f);
                    }
                }, false);
    }

    /** True if a field differs from the stored configuration, or markModified() was called. */
    public boolean isModified() {
        if (forcedModified) return true;
        for (Map.Entry<Fragment, String> e : references.entrySet()) {
            View v = e.getKey().getView();
            if (v != null && !e.getValue().equals(stateOf(v))) return true;
        }
        return false;
    }

    /** Records a change that the fields do not show (e.g. default values restored). */
    public void markModified() {
        forcedModified = true;
    }

    /** The current state of every tab becomes the reference (after a save). */
    public void resetReference() {
        forcedModified = false;
        for (Map.Entry<Fragment, String> e : references.entrySet()) {
            View v = e.getKey().getView();
            if (v != null) e.setValue(stateOf(v));
        }
    }

    /**
     * The current state of one tab becomes its reference: used when the fragment changes a
     * field by program after its creation (e.g. list of the voices loaded later).
     */
    public void resetReference(@Nullable Fragment fragment) {
        if (fragment != null && fragment.getView() != null && references.containsKey(fragment)) {
            references.put(fragment, stateOf(fragment.getView()));
        }
    }

    /** State of all the input widgets of a view tree, in the order of the tree. */
    private static String stateOf(View root) {
        StringBuilder sb = new StringBuilder();
        append(root, sb);
        return sb.toString();
    }

    private static void append(View v, StringBuilder sb) {
        if (v instanceof EditText) {
            sb.append("E").append(((EditText) v).getText()).append('\u0001');
        } else if (v instanceof Spinner) {
            sb.append("S").append(((Spinner) v).getSelectedItemPosition()).append('\u0001');
        } else if (v instanceof CompoundButton) {
            sb.append("C").append(((CompoundButton) v).isChecked()).append('\u0001');
        } else if (v instanceof SeekBar) {
            sb.append("B").append(((SeekBar) v).getProgress()).append('\u0001');
        }
        if (v instanceof ViewGroup && !(v instanceof Spinner)) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) append(g.getChildAt(i), sb);
        }
    }
}
