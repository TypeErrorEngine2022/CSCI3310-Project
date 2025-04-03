package com.example.csci3310project.screenTimeTracking.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.example.csci3310project.R;
import com.example.csci3310project.screenTimeTracking.domain.AppClassifier;

public class RewardFragment extends Fragment {
    private EditText inputEditText;
    private TextView outputTextView;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.section1_reward, container, false);

        inputEditText = view.findViewById(R.id.reward_input);
        outputTextView = view.findViewById(R.id.reward_output_text);
        progressBar = view.findViewById(R.id.progress_bar);
        Button classifyButton = view.findViewById(R.id.classify_button);
        Button commentButton = view.findViewById(R.id.comment_button);

        // Initialize LLM in background, because it takes a lot of time
        // in my Samsung 23, it takes about 10 seconds !!
        new Thread(new InitializeAppClassifierRunnable()).start();

        classifyButton.setOnClickListener(v -> {
            String appName = inputEditText.getText().toString().trim();
            if (!appName.isEmpty()) {
                boolean isProductive = AppClassifier.classifyApp(appName);
                outputTextView.setText(appName + " 是生產力應用？ " + (isProductive ? "是" : "否"));
            }
        });

        commentButton.setOnClickListener(v -> {
            String comment = AppClassifier.generateComment(getContext());
            outputTextView.setText(comment);
        });

        return view;
    }

    private class InitializeAppClassifierRunnable implements Runnable {
        @Override
        public void run() {
            handler.post(() -> progressBar.setVisibility(View.VISIBLE));
            AppClassifier.initialize(getContext());
            handler.post(() -> progressBar.setVisibility(View.GONE));
        }
    }
}