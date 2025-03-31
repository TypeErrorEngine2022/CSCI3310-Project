package com.example.csci3310project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Section2Fragment extends Fragment {

    private long productivityWorkDuration = 60 * 60 * 1000; // Default 1 hour
    private long productivityBreakDuration = 30 * 60 * 1000; // Default 30 minutes
    private long entertainmentWorkDuration = 30 * 60 * 1000; // Default 30 minutes
    private long entertainmentBreakDuration = 10 * 60 * 1000; // Default 10 minutes

    private boolean isMonitoringActive = false;
    private BottomNavigationView bottomNav;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section2, container, false);

        bottomNav = view.findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Prevent navigation to settings when monitoring is active
            if (isMonitoringActive && itemId == R.id.page_2b) {
                Toast.makeText(getContext(), "Cannot change settings while monitoring is active. Stop monitoring first.",
                        Toast.LENGTH_LONG).show();
                return false;
            }

            Fragment fragment = null;
            if (itemId == R.id.page_2a) {
                Page2AFragment page2AFragment = new Page2AFragment();
                page2AFragment.setParentFragment(this);
                fragment = page2AFragment;
            } else if (itemId == R.id.page_2b) {
                Page2BFragment page2BFragment = new Page2BFragment();
                page2BFragment.setParentFragment(this);
                fragment = page2BFragment;
            }

            if (fragment != null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.section_container, fragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Load initial fragment
        if (savedInstanceState == null) {
            Page2AFragment page2AFragment = new Page2AFragment();
            page2AFragment.setParentFragment(this);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.section_container, page2AFragment)
                    .commit();
        }

        return view;
    }

    public void setWorkAndBreakDurations(long productivityWorkDur, long productivityBreakDur,
                                         long entertainmentWorkDur, long entertainmentBreakDur) {
        this.productivityWorkDuration = productivityWorkDur;
        this.productivityBreakDuration = productivityBreakDur;
        this.entertainmentWorkDuration = entertainmentWorkDur;
        this.entertainmentBreakDuration = entertainmentBreakDur;

        Toast.makeText(getContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }

    public void setMonitoringActive(boolean active) {
        this.isMonitoringActive = active;

        // Optional: visually disable settings tab when monitoring is active
        if (bottomNav != null) {
            bottomNav.getMenu().findItem(R.id.page_2b).setEnabled(!active);
        }
    }

    public long getProductivityWorkDuration() {
        return productivityWorkDuration;
    }

    public long getProductivityBreakDuration() {
        return productivityBreakDuration;
    }

    public long getEntertainmentWorkDuration() {
        return entertainmentWorkDuration;
    }

    public long getEntertainmentBreakDuration() {
        return entertainmentBreakDuration;
    }
}