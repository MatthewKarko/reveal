package com.example.revealapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.revealapp.R;
import com.example.revealapp.adapter.FriendListAdapter; // Ensure you import the correct Adapter
import com.example.revealapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FriendListAdapter adapter;
    private List<User> friendList = new ArrayList<>();
    private static final String TAG = "FriendListFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view");
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.friendsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendListAdapter(friendList, getContext());
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "onViewCreated: RecyclerView and Adapter initialized");

        ImageView backArrow = view.findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        fetchFriendList();
    }

    private void fetchFriendList() {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Log.d(TAG, "fetchFriendList: Fetching for current user: " + currentUserEmail);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        List<Map<String, String>> friendListMap = (List<Map<String, String>>) documentSnapshot.get("friends");

                        if (friendListMap != null && !friendListMap.isEmpty()) {
                            Log.d(TAG, "fetchFriendList: Found friends: " + friendListMap.toString());
                            for (Map<String, String> map : friendListMap) {
                                String email = map.get("email");
                                if (email != null) {
                                    fetchUserDetailsByEmail(email);
                                }
                            }
                        } else {
                            Log.d(TAG, "fetchFriendList: No friends found");
                        }
                    } else {
                        Log.d(TAG, "fetchFriendList: No document found for user with email: " + currentUserEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchFriendList: Error fetching document for user email: " + currentUserEmail, e);
                });
    }

    private void fetchUserDetailsByEmail(String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "fetchUserDetailsByEmail: Fetching details for email: " + email);

        db.collection("Users").whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        if (user != null) {
                            friendList.add(user);
                            adapter.notifyDataSetChanged();
                            Log.d(TAG, "fetchUserDetailsByEmail: User details added to list: " + user.getName());
                        } else {
                            Log.d(TAG, "fetchUserDetailsByEmail: User object creation failed for email: " + email);
                        }
                    } else {
                        Log.d(TAG, "fetchUserDetailsByEmail: No document found for email: " + email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchUserDetailsByEmail: Error fetching user details for email: " + email, e);
                });
    }
}
