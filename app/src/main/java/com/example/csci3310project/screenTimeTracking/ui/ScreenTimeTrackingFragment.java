package com.example.csci3310project.screenTimeTracking.ui;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.csci3310project.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;

public class ScreenTimeTrackingFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1, container, false);

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if(itemId == R.id.page_1a) {
                fragment = new DashboardFragment();
            } else if(itemId == R.id.page_1b) {
                fragment = new RewardFragment();
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
                    .replace(R.id.section_container, new DashboardFragment())
                    .commit();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!hasUsagePermission()) {
            Log.d("Section1Fragment", "Usage permission not granted, now requesting");
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("Permission Required");
            builder.setMessage("Please enable usage access permission to use the screen tracking feature.");
            builder.setPositiveButton("Confirm", (dialog, which) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        } else {
            Log.d("Section1Fragment", "Usage permission granted");
        }

        if (!hasQueryAllPackagesPermission()) {
            Log.d("Section1Fragment", "QUERY_ALL_PACKAGES permission not granted, now requesting");
            AlertDialog.Builder builder2 = new AlertDialog.Builder(requireActivity());
            builder2.setTitle("Permission Required");
            builder2.setMessage("Please enable QUERY_ALL_PACKAGES permission to use the screen tracking feature.");
            builder2.setPositiveButton("Confirm", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)));
            builder2.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder2.show();
        } else {
            Log.d("Section1Fragment", "QUERY_ALL_PACKAGES permission granted");
        }
    }

    private boolean hasUsagePermission() {
        // Check if the PACKAGE_USAGE_STATS permission is granted
        try {
            AppOpsManager appOps = (AppOpsManager) requireActivity().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireActivity().getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.e("Section1Fragment", "Error checking usage permission", e);
            return false;
        }
    }

    private boolean hasQueryAllPackagesPermission() {
        return requireActivity().checkSelfPermission("android.permission.QUERY_ALL_PACKAGES") == PackageManager.PERMISSION_GRANTED;
    }
}