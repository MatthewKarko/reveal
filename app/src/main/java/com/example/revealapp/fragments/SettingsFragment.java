package com.example.revealapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.revealapp.LoginActivity;
import com.example.revealapp.R;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private FirebaseAuth mAuth;

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Logout Button
        Button logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();

                if (getContext() != null) {
                    Toast.makeText(getContext(), "Successfully logged out", Toast.LENGTH_SHORT).show();
                }

                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        // Friends Button
        Button friendsButton = view.findViewById(R.id.friendsButton);
        friendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToFriendListFragment();
            }
        });

        // Account Information Button
        Button accountInfoButton = view.findViewById(R.id.accountInfoButton);
        accountInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAccountInfoFragment();
            }
        });

        // Notifications Button
        Button notificationsButton = view.findViewById(R.id.notificationsButton);
        notificationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToNotificationsFragment();
            }
        });

        // About Button
        Button aboutButton = view.findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToAboutFragment();
            }
        });

        return view;
    }

    // Navigation Methods:

    private void navigateToFriendListFragment() {
        FriendListFragment friendListFragment = new FriendListFragment();
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, friendListFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void navigateToAccountInfoFragment() {
        AccountInfoFragment accountInfoFragment = new AccountInfoFragment();
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, accountInfoFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void navigateToNotificationsFragment() {
        NotificationsFragment notificationsFragment = new NotificationsFragment();
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, notificationsFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void navigateToAboutFragment() {
        AboutFragment aboutFragment = new AboutFragment();
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, aboutFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}
