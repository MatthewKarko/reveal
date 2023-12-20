package com.example.revealapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.revealapp.R;

public class BlankHomeFragment extends Fragment {


    private String themeTitle;
    private TextView defaultText;


    public BlankHomeFragment() {
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        defaultText = (TextView) view.findViewById(R.id.default_text);
        Bundle args = getArguments();
        if (args != null) {
            themeTitle = args.getString("challengeName");
        }
        if (themeTitle == null){
            defaultText.setText("No challenge today");
        }else{
            defaultText.setText("Today's challenge: "+ themeTitle+"\nNo posts today! Be the first to post!");
        }

    }
}