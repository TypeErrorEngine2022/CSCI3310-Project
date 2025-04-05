package com.example.csci3310project.screenTimeTracking.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.data.CommentManager;
import com.example.csci3310project.screenTimeTracking.data.UsageRepository;
import com.example.csci3310project.screenTimeTracking.data.UsageStatsDataSource;
import com.example.csci3310project.screenTimeTracking.domain.LlmInferenceManager;
import com.example.csci3310project.screenTimeTracking.domain.UsageStatsUseCase;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RewardFragment extends Fragment {
    private final String TAG = "RewardFragment";

    private TextView outputTextView;
    private ProgressBar progressBar;
    private final AtomicBoolean isGeneratingComment = new AtomicBoolean(false);
    private LlmInferenceManager llmInferenceManager;
    private String resultReceivedWhilePaused = null;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_reward, container, false);

        outputTextView = view.findViewById(R.id.reward_output_text);
        progressBar = view.findViewById(R.id.progress_bar);
        
        llmInferenceManager = LlmInferenceManager.getInstance(requireContext());

        // Only generate comment if the model is ready and not already generating
        llmInferenceManager.getModelReadyState().observe(getViewLifecycleOwner(), isReady -> {
            if (isReady && !isGeneratingComment.get()) {
                generateComment();
            }
        });
        
        showLoading("Initializing AI model...");

        Log.d(TAG, "onCreateView");

        // if the model is already initialized, generate comment immediately
        if (llmInferenceManager.isModelReady() && !isGeneratingComment.get()) {
            generateComment();
        }

        return view;
    }

    private void showLoading(String message) {
        progressBar.setVisibility(View.VISIBLE);
        outputTextView.setText(message);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }
    
    private void generateComment() {
        // Prevent multiple simultaneous calls
        // getAndSet returns the previous value and sets it to a new value
        if (isGeneratingComment.getAndSet(true)) {
            return;
        }

        String latestComment = CommentManager.getCommentIfValid(requireContext());
        if (latestComment != null) {
            Log.d(TAG, "Using cached comment: " + latestComment);
            hideLoading();
            outputTextView.setText(latestComment);
            isGeneratingComment.set(false);
            return;
        }
        
        showLoading("Getting your personalized analysis...");
        
        StringBuilder prompt = getPrompt(requireContext());
        Log.d(TAG, "Prompt: " + prompt);
        
        llmInferenceManager.generateResponseAsync(prompt.toString(), new LlmInferenceManager.ResponseCallback() {
            @Override
            public void onResponse(String response) {
                // Fragment is visible, update UI directly
                if (isAdded() && isResumed()) {
                    requireActivity().runOnUiThread(() -> {
                        hideLoading();
                        outputTextView.setText(response);
                        isGeneratingComment.set(false);
                    });
                }

                // Regardless of visibility, save the comment
                // So, even if the fragment is not visible, the comment will be saved for later use
                CommentManager.saveComment(requireContext(), response);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onError(String errorMessage) {
                // Fragment is visible, update UI directly
                if (isAdded() && isResumed()) {
                    requireActivity().runOnUiThread(() -> {
                        hideLoading();
                        outputTextView.setText("Sorry, couldn't generate analysis: " + errorMessage);
                        isGeneratingComment.set(false);
                    });
                } else if (isAdded()) { // Fragment exists but is not visible, store error for later
                    requireActivity().runOnUiThread(() -> {
                        // Store error message in resultReceivedWhilePaused with error prefix
                        resultReceivedWhilePaused = "ERROR: " + errorMessage;
                        isGeneratingComment.set(false);
                    });
                }

                // Do not save error message in CommentManager
                // only show error message if resume
                // if the fragment is destroyed, let user generate comment again when they come back
            }
        });
    }

    @NonNull
    private static StringBuilder getPrompt(Context context) {
        StringBuilder prompt = new StringBuilder("Assume the user's current usage time in the format [app, time] is as follows:\n{");
        UsageStatsUseCase usageStatsUseCase = new UsageStatsUseCase(new UsageRepository(new UsageStatsDataSource(context)), context);
        List<UsageStatUIModel> usageStatsData = usageStatsUseCase.getDailyUsageStats();
        for (UsageStatUIModel usageStat : usageStatsData) {
            prompt
                    .append(usageStat.getPackageName())
                    .append(" ")
                    .append(usageStat.getFormattedTime())
                    .append(",");
        }
        prompt.setLength(prompt.length() - 1);  // Remove last comma
        prompt.append("}\nYou are an AI assistant. Give advice on the user's productivity.\n");
        prompt.append("Sample advice: Your screen time today is quite interesting! You spent {time} hours on {app}, which shows you are very dedicated to your work—well done! However, your usage of {app} is also a bit high, which might affect your focus. Try to shift some time to more productive activities, and you'll be even more efficient! Keep it up; you can do it!\n");
        prompt.append("Your comment:\n");
        return prompt;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume called");

        // If we received a result while paused, update the UI now
        if (resultReceivedWhilePaused != null) {
            hideLoading();

            if (resultReceivedWhilePaused.startsWith("ERROR: ")) {
                String errorMessage = resultReceivedWhilePaused.substring(7); // Remove "ERROR: " prefix
                outputTextView.setText("Sorry, couldn't generate analysis: " + errorMessage);
            } else {
                outputTextView.setText(resultReceivedWhilePaused);
            }

            resultReceivedWhilePaused = null;
            return;
        }

        // If we were not generating when paused, and the model is ready, start generating a comment
        Log.d(TAG, "onResume: isGeneratingComment = " + isGeneratingComment.get());
        if (llmInferenceManager.isModelReady() && !isGeneratingComment.get() && outputTextView.getText().toString().contains("Getting your personalized analysis")) {
            generateComment();
        }
    }
}