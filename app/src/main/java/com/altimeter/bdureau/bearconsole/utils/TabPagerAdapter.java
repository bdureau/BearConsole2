package com.altimeter.bdureau.bearconsole.utils;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: TabPagerAdapter
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  ViewPager adapter shared by every tabbed screen (telemetry, status, flight view,
 *  configuration). When the activity is re-created (rotation, dark mode change, process
 *  restored by the system), the FragmentManager re-creates the tab fragments by itself and
 *  the ViewPager shows those restored instances instead of the ones the activity has just
 *  built. Without care, the activity would keep updating fragments that are not displayed.
 *
 *  This adapter detects that case in instantiateItem(), stores the displayed instance in its
 *  list and notifies the activity through {@link OnFragmentInstantiatedListener}, so that the
 *  activity can re-bind its references to the fragments really on screen.
 * ============================================================================
 */

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class TabPagerAdapter extends FragmentPagerAdapter {

    /** Callback telling the activity which fragment instance is displayed at a position. */
    public interface OnFragmentInstantiatedListener {
        void onFragmentInstantiated(int position, @NonNull Fragment fragment);
    }

    private final List<Fragment> fragments = new ArrayList<>();
    private final OnFragmentInstantiatedListener listener;

    /**
     * @param fm       fragment manager of the activity
     * @param behavior FragmentPagerAdapter behavior constant
     * @param listener receives the restored fragment instances (may be null)
     */
    public TabPagerAdapter(@NonNull FragmentManager fm, int behavior,
                           OnFragmentInstantiatedListener listener) {
        super(fm, behavior);
        this.listener = listener;
    }

    /** Appends a tab. Tabs must be added before the adapter is given to the ViewPager. */
    public void addFragment(@NonNull Fragment fragment) {
        fragments.add(fragment);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    /** Position of a tab, or -1 if this fragment is not one of the tabs. */
    public int indexOf(@NonNull Fragment fragment) {
        return fragments.indexOf(fragment);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment shown = (Fragment) super.instantiateItem(container, position);
        if (shown != fragments.get(position)) {
            // The FragmentManager restored its own instance: use it from now on
            fragments.set(position, shown);
            if (listener != null) {
                listener.onFragmentInstantiated(position, shown);
            }
        }
        return shown;
    }
}
