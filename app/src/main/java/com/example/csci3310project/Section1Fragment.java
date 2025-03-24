package com.example.csci3310project;

import android.os.Bundle;
import android.view.*;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Section1Fragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1, container, false);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if(itemId == R.id.page_1a) {
                fragment = new Page1AFragment();
            } else if(itemId == R.id.page_1b) {
                fragment = new Page1BFragment();
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
                    .replace(R.id.section_container, new Page1AFragment())
                    .commit();
        }

        return view;
    }
}