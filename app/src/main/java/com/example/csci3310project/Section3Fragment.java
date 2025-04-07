package com.example.csci3310project;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.csci3310project.screenTimeTracking.ui.DashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Section3Fragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section3, container, false);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if(itemId == R.id.page_3a) {
                fragment = new Page3AFragment();
            } else if(itemId == R.id.page_3b) {
                fragment = new Page3BFragment();
            }

            if(fragment != null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.section_container, fragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Load initial fragment
        if(savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.section_container, new Page3AFragment())
                    .commit();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
}