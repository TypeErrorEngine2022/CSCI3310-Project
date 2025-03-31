package com.example.csci3310project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class Page2BFragment extends Fragment {

    private EditText productivityWorkDurationInput;
    private EditText productivityBreakDurationInput;
    private EditText entertainmentWorkDurationInput;
    private EditText entertainmentBreakDurationInput;
    private Button saveButton;
    private Section2Fragment parentFragment;

    public Page2BFragment() {
        // Required empty constructor
    }

    public void setParentFragment(Section2Fragment parentFragment) {
        this.parentFragment = parentFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.page2b, container, false);

        productivityWorkDurationInput = view.findViewById(R.id.productivity_work_duration_input);
        productivityBreakDurationInput = view.findViewById(R.id.productivity_break_duration_input);
        entertainmentWorkDurationInput = view.findViewById(R.id.entertainment_work_duration_input);
        entertainmentBreakDurationInput = view.findViewById(R.id.entertainment_break_duration_input);
        saveButton = view.findViewById(R.id.save_button);

        // Populate fields with current values if parent exists
        if (parentFragment != null) {
            productivityWorkDurationInput.setText(String.valueOf(parentFragment.getProductivityWorkDuration() / (60 * 1000)));
            productivityBreakDurationInput.setText(String.valueOf(parentFragment.getProductivityBreakDuration() / (60 * 1000)));
            entertainmentWorkDurationInput.setText(String.valueOf(parentFragment.getEntertainmentWorkDuration() / (60 * 1000)));
            entertainmentBreakDurationInput.setText(String.valueOf(parentFragment.getEntertainmentBreakDuration() / (60 * 1000)));
        }

        saveButton.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void saveSettings() {
        try {
            // Convert input to milliseconds
            int productivityWorkDuration = Integer.parseInt(productivityWorkDurationInput.getText().toString()) * 60 * 1000;
            int productivityBreakDuration = Integer.parseInt(productivityBreakDurationInput.getText().toString()) * 60 * 1000;
            int entertainmentWorkDuration = Integer.parseInt(entertainmentWorkDurationInput.getText().toString()) * 60 * 1000;
            int entertainmentBreakDuration = Integer.parseInt(entertainmentBreakDurationInput.getText().toString()) * 60 * 1000;

            // Validate inputs
            if (productivityWorkDuration <= 0 || productivityBreakDuration <= 0 ||
                    entertainmentWorkDuration <= 0 || entertainmentBreakDuration <= 0) {
                Toast.makeText(getContext(), "All durations must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (parentFragment != null) {
                parentFragment.setWorkAndBreakDurations(
                        productivityWorkDuration,
                        productivityBreakDuration,
                        entertainmentWorkDuration,
                        entertainmentBreakDuration
                );
            } else {
                Toast.makeText(getContext(), "Error: Cannot save settings", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter valid numbers for all fields", Toast.LENGTH_SHORT).show();
        }
    }
}