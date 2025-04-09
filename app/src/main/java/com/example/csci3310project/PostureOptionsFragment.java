package com.example.csci3310project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3310project.neckPostureDatabase.*;

public class PostureOptionsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page3b, container, false);
    }

    private SettingsViewModel viewModel;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Log.d("My debug","3B Fragment created");
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        Switch postureSwitch = view.findViewById(R.id.posture_switch);
        Spinner intervalSpinner = view.findViewById(R.id.interval_spinner);
        Switch notificationSwitch = view.findViewById(R.id.notification_switch);
        Spinner toleranceSpinner = view.findViewById(R.id.tolerance_spinner);

        // Set up spinners
        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.interval_options, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(intervalAdapter);

        ArrayAdapter<CharSequence> toleranceAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.tolerance_options, android.R.layout.simple_spinner_item);
        toleranceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toleranceSpinner.setAdapter(toleranceAdapter);

        viewModel.getSettings().observe(getViewLifecycleOwner(), settings -> {
            Log.d("My_debug","settings ? null");
            Log.d("My_debug", settings.toString());
            if (settings == null) {
                Log.d("My_debug","settings detected as null, ensuring defaults");
                viewModel.ensureDefaults();
                Log.d("My_debug", settings.toString());
            } else {
                Log.d("My_debug", "settings not null");
                postureSwitch.setChecked(settings.postureAnalysisEnabled);
                intervalSpinner.setSelection(getIndex(intervalSpinner, settings.checkInterval));
                notificationSwitch.setChecked(settings.notificationsEnabled);
                toleranceSpinner.setSelection(getIndex(toleranceSpinner, settings.toleranceLevel));

                // Enable/disable other options based on posture switch
                intervalSpinner.setEnabled(settings.postureAnalysisEnabled);
                notificationSwitch.setEnabled(settings.postureAnalysisEnabled);
                toleranceSpinner.setEnabled(settings.postureAnalysisEnabled);
            }
        });

        // Listeners to update settings
        postureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserSettings current = viewModel.getSettings().getValue();
            if (current != null) {
                current.postureAnalysisEnabled = isChecked;
                viewModel.updateSettings(current);
                Log.d("My_debug","Posture switch listener activated");
                Log.d("My_debug", current.toString());
                if (isChecked) {
                    intervalSpinner.setEnabled(true);
                    notificationSwitch.setEnabled(true);
                    toleranceSpinner.setEnabled(true);

                    startPostureService();
                } else {
                    intervalSpinner.setEnabled(false);
                    notificationSwitch.setEnabled(false);
                    toleranceSpinner.setEnabled(false);
                    stopPostureService();
                }
            }
        });

        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                UserSettings current = viewModel.getSettings().getValue();
                if (current != null) {
                    current.checkInterval = parent.getItemAtPosition(position).toString();
                    viewModel.updateSettings(current);
                    Intent intent = new Intent(getActivity(), PostureService.class);
                    intent.setAction("UPDATE_SETTINGS");
                    getActivity().startService(intent);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserSettings current = viewModel.getSettings().getValue();
            if (current != null) {
                current.notificationsEnabled = isChecked;
                viewModel.updateSettings(current);
                Intent intent = new Intent(getActivity(), PostureService.class);
                intent.setAction("UPDATE_SETTINGS");
                getActivity().startService(intent);
            }
        });

        toleranceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                UserSettings current = viewModel.getSettings().getValue();
                if (current != null) {
                    current.toleranceLevel = parent.getItemAtPosition(position).toString();
                    viewModel.updateSettings(current);
                    Intent intent = new Intent(getActivity(), PostureService.class);
                    intent.setAction("UPDATE_SETTINGS");
                    getActivity().startService(intent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int getIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void startPostureService() {
        Log.d("My_debug","startPostureService method started");
        Intent intent = new Intent(getActivity(), PostureService.class);
        getActivity().startForegroundService(intent);
    }

    private void stopPostureService() {
        getActivity().stopService(new Intent(getActivity(), PostureService.class));
    }
}