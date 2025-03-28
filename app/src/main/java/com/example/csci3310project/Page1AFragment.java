package com.example.csci3310project;

import android.os.Bundle;
import android.view.*;

import androidx.fragment.app.Fragment;
public class Page1AFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.section1_dashboard, container, false);
    }
}