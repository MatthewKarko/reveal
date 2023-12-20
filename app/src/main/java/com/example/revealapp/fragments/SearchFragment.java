package com.example.revealapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.revealapp.R;
import com.example.revealapp.adapter.FriendAdapter;
import com.example.revealapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";

    private RecyclerView recyclerView;
    private FriendAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private EditText searchEditText;

    private ImageView viewFriendRequests;

    private String currentUserName = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: View being created");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("Users").document(currentUserId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentUserName = documentSnapshot.getString("username"); // Assign the value here
                } else {
                    Log.d(TAG, "User document does not exist");
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user document", e);
            });
        }
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the views
        recyclerView = view.findViewById(R.id.friendsRecyclerView);
        searchEditText = view.findViewById(R.id.textSearchUser);
        viewFriendRequests = view.findViewById(R.id.viewFriendRequests);

        // Initialize the RecyclerView and Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendAdapter(userList);
        recyclerView.setAdapter(adapter);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String searchQuery = searchEditText.getText().toString();
                Log.d(TAG, "onEditorAction: Search triggered for query: " + searchQuery);
                searchUsers(searchQuery);
                return true;
            }
            return false;
        });

        viewFriendRequests.setOnClickListener(v -> navigateToFriendRequestsFragment());

    }
    private void navigateToFriendRequestsFragment() {
        FriendRequestFragment friendRequestsFragment = new FriendRequestFragment();
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, friendRequestsFragment)  // Replace with your container ID
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void searchUsers(String query) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "searchUsers: Searching users with Username: " + query);

        db.collection("Users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .whereNotEqualTo("username", currentUserName)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    userList.addAll(queryDocumentSnapshots.toObjects(User.class));
                    Log.d(TAG, "searchUsers: Successfully fetched " + userList.size() + " users");
                    adapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "searchUsers: Error fetching users", e);
                });
    }
}
