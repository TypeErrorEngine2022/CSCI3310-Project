package com.example.csci3310project;

import android.os.Bundle;

import androidx.appcompat.app.*;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.csci3310project.screenTimeTracking.ui.ScreenTimeTrackingFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigation(item.getItemId());
            return true;
        });

        // Load default section
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new ScreenTimeTrackingFragment())
                    .commit();
        }
    }

    private void handleNavigation(int itemId) {
        Fragment fragment = null;

        if(itemId == R.id.nav_section1) {
            fragment = new ScreenTimeTrackingFragment();
        } else if(itemId == R.id.nav_section2) {
            fragment = new Section2Fragment();
        } else if(itemId == R.id.nav_section3) {
            fragment = new Section3Fragment();
        }

        if(fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }
}