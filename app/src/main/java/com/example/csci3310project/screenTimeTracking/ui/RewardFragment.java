package com.example.csci3310project.screenTimeTracking.ui;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.data.CommentManager;
import com.example.csci3310project.screenTimeTracking.domain.LlmInferenceManager;
import com.example.csci3310project.screenTimeTracking.domain.PromptGenerator;
import java.util.concurrent.atomic.AtomicBoolean;

public class RewardFragment extends Fragment {
    private final String TAG = "RewardFragment";

    private TextView outputTextView;
    private ProgressBar progressBar;

    private Button retryButton;
    private final AtomicBoolean isGeneratingComment = new AtomicBoolean(false);
    private LlmInferenceManager llmInferenceManager;
    private String resultReceivedWhilePaused = null;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_reward, container, false);
        Log.d(TAG, "onCreateView");
        outputTextView = view.findViewById(R.id.reward_output_text);
        progressBar = view.findViewById(R.id.progress_bar);
        retryButton = view.findViewById(R.id.reward_retry_button);
        return view;
    }

    private void showLoading(String message) {
        progressBar.setVisibility(View.VISIBLE);
        outputTextView.setText(message);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void initialize() {
        llmInferenceManager = LlmInferenceManager.getInstance(requireContext());

        // Only generate comment if the model is ready and not already generating
        llmInferenceManager.getModelReadyState().observe(getViewLifecycleOwner(), isReady -> {
            if (isReady && !isGeneratingComment.get()) {
                generateComment(false);
            }
        });

        showLoading("Initializing AI model...\nYou can leave this page while waiting.");

        // if the model is already initialized, generate comment immediately
        if (llmInferenceManager.isModelReady() && !isGeneratingComment.get()) {
            generateComment(false);
        }

        retryButton.setOnClickListener(v -> {
            outputTextView.setText("");
            showLoading("Getting your personalized analysis...\nYou can leave this page while waiting.");
            generateComment(true);
            retryButton.setVisibility(View.GONE);
        });
    }

    @SuppressLint("SetTextI18n")
    private void generateComment(boolean force) {
        // Prevent multiple simultaneous calls
        // getAndSet returns the previous value and sets it to a new value
        if (isGeneratingComment.getAndSet(true)) {
            return;
        }

        if (!force) {
            String latestComment = CommentManager.getCommentIfValid(requireContext());
            if (latestComment != null) {
                Log.d(TAG, "Using cached comment: " + latestComment);
                hideLoading();
                outputTextView.setText(latestComment);
                isGeneratingComment.set(false);
                retryButton.setVisibility(View.VISIBLE);
                return;
            }

            // if user go back and forth between fragments, we need to prevent sending multiple inference requests
            if (!CommentManager.isCommentRequestAllowed(requireContext())) {
                // do nothing, the last request will update the UI when it is done
                isGeneratingComment.set(true);
                retryButton.setVisibility(View.VISIBLE);
                return;
            }
        } else {
            Log.d(TAG, "Force generating comment");
        }

        CommentManager.setCommentRequestTimestamp(requireContext());

        showLoading("Getting your personalized analysis...\nYou can leave this page while waiting.");

        PromptGenerator.getPromptAsync(requireContext(), prompt -> {
            Log.d(TAG, "Prompt ready:\n" + prompt);

            llmInferenceManager.generateResponseAsync(prompt, new LlmInferenceManager.ResponseCallback() {
                @Override
                public void onResponse(String response) {
                    Context appContext = llmInferenceManager.getAppContext();
                    CommentManager.saveComment(appContext, response);
                    // Fragment is visible, update UI directly
                    if (isAdded() && isResumed()) {
                        requireActivity().runOnUiThread(() -> {
                            hideLoading();
                            outputTextView.setText(response);
                            isGeneratingComment.set(false);
                            retryButton.setVisibility(View.VISIBLE);
                        });
                    }
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void onError(String errorMessage) {
                    // these are not ui related, so we can directly run them
                    isGeneratingComment.set(false);
                    resultReceivedWhilePaused = "ERROR: " + errorMessage;

                    if (isAdded() && isResumed()) { // Fragment is visible, update UI directly
                        requireActivity().runOnUiThread(() -> {
                            hideLoading();
                            outputTextView.setText("Sorry, couldn't generate analysis: " + errorMessage);
                            retryButton.setVisibility(View.VISIBLE);
                        });
                    }

                    // Do not save error message in CommentManager
                    // only show error message if resume
                    // if the fragment is destroyed, let user generate comment again when they come back
                }
            });
        });
    }

    private boolean checkPermissionsAndStartTracking() {
        boolean hasAllPermissions = true;

        if (!hasUsagePermission()) {
            Log.d(TAG, "Usage permission not granted, now requesting");
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle("Permission Required");
            builder.setMessage("Please enable usage access permission to use the screen tracking feature.");
            builder.setPositiveButton("Confirm", (dialog, which) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
            hasAllPermissions = false;
        } else {
            Log.d(TAG, "Usage permission granted");
        }

        if (!hasQueryAllPackagesPermission()) {
            Log.d(TAG, "QUERY_ALL_PACKAGES permission not granted, now requesting");
            AlertDialog.Builder builder2 = new AlertDialog.Builder(requireActivity());
            builder2.setTitle("Permission Required");
            builder2.setMessage("Please enable QUERY_ALL_PACKAGES permission to use the screen tracking feature.");
            builder2.setPositiveButton("Confirm", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)));
            builder2.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder2.show();
            hasAllPermissions = false;
        } else {
            Log.d(TAG, "QUERY_ALL_PACKAGES permission granted");
        }

        return hasAllPermissions;
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume called");

        boolean permissionsGranted = checkPermissionsAndStartTracking();
        if (!permissionsGranted) {
            llmInferenceManager = null;
            hideLoading();
            outputTextView.setText("Please grant usage access permissions to view your productivity analysis.");
            retryButton.setVisibility(View.GONE);
            return;
        } else {
            if (llmInferenceManager == null) {
                initialize();
            }
        }

        // If we received a result while paused, update the UI now
        if (resultReceivedWhilePaused != null) {
            hideLoading();

            if (resultReceivedWhilePaused.startsWith("ERROR: ")) {
                String errorMessage = resultReceivedWhilePaused.substring(7); // Remove "ERROR: " prefix
                outputTextView.setText("Sorry, couldn't generate analysis: " + errorMessage);
            } else {
                outputTextView.setText(resultReceivedWhilePaused);
            }
            retryButton.setVisibility(View.VISIBLE);
            resultReceivedWhilePaused = null;
            return;
        }

        // If we were not generating when paused, and the model is ready, start generating a comment
        Log.d(TAG, "onResume: isGeneratingComment = " + isGeneratingComment.get());
        if (llmInferenceManager.isModelReady() && !isGeneratingComment.get() && outputTextView.getText().toString().contains("Getting your personalized analysis")) {
            generateComment(false);
        }
    }
}