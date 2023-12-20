package com.example.revealapp.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.revealapp.R;
import com.example.revealapp.model.User;
import com.example.revealapp.modules.GlideApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private List<User> userList;
    private Map<String, String> friendsMap = new HashMap<>();
    private Map<String, String> outboundFriendRequests = new HashMap<>();


    public FriendAdapter(List<User> userList) {
        this.userList = userList;
        fetchFriendsMap();
        fetchOutboundFriendRequests();
    }
    private void fetchFriendsMap() {
        String currentEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore.getInstance().collection("Users").whereEqualTo("email", currentEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        List<Map<String, String>> friendsList = (List<Map<String, String>>) userDoc.get("friends");
                        if (friendsList != null && !friendsList.isEmpty()) {
                            for (Map<String, String> map : friendsList) {
                                String email = map.get("email");
                                if (email != null) {
                                    friendsMap.put(email, email);
                                }
                            }
                        }
                        Log.d("FriendAdapter", "Fetched friends: " + friendsMap.toString());
                    } else {
                        Log.d("FriendAdapter", "No document found for user with email: " + currentEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendAdapter", "Error fetching friends list for user with email: " + currentEmail, e);
                });
    }

    private void fetchOutboundFriendRequests() {
        String currentEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        fetchDocIdByEmail(currentEmail, new OnDocIdFetchedCallback() {
            @Override
            public void onSuccess(String docId) {
                FirebaseFirestore.getInstance().collection("Users").document(docId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                List<Map<String, String>> outboundRequestsList = (List<Map<String, String>>) documentSnapshot.get("outboundFriendRequests");

                                if (outboundRequestsList != null && !outboundRequestsList.isEmpty()) {
                                    for (Map<String, String> map : outboundRequestsList) {
                                        String email = map.get("email");
                                        if (email != null) {
                                            outboundFriendRequests.put("email", email);
                                        }
                                    }
                                }
                                Log.d("FriendAdapter", "Fetched outbound friend requests: " + outboundFriendRequests.toString());
                            } else {
                                Log.d("FriendAdapter", "No such document.");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FriendAdapter", "Error fetching outbound friend requests.", e);
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FriendAdapter", "Failed to fetch doc ID.", e);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        Log.d("FriendAdapter", "Binding view for user: " + user.getName());
        holder.friendName.setText(user.getName());
        holder.friendUsername.setText("@" + user.getUsername());

        String userProfilePhotoPath = user.getUserProfilePhoto();
        if (userProfilePhotoPath != null && !userProfilePhotoPath.isEmpty()) {
            StorageReference ref = FirebaseStorage.getInstance().getReference(userProfilePhotoPath);
            GlideApp.with(holder.itemView.getContext()).load(ref).into(holder.friendImage);
        } else {
            GlideApp.with(holder.itemView.getContext()).load(R.drawable.default_user_image).into(holder.friendImage);
        }

        if (friendsMap.containsValue(user.getEmail()) || outboundFriendRequests.containsValue(user.getEmail())) {
            holder.addFriendIconImageView.setImageResource(R.drawable.user_minus);
            holder.addFriendTextView.setText("Remove");  // if you want to change the text when friends
        } else {
            holder.addFriendIconImageView.setImageResource(R.drawable.user_plus);
            holder.addFriendTextView.setText("Add");
        }
        holder.addFriendButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (outboundFriendRequests.containsValue(user.getEmail())) {
                    removeFriendRequest(user.getEmail());
                    outboundFriendRequests.remove(user.getEmail());
                    Toast.makeText(holder.itemView.getContext(), "Friend request removed", Toast.LENGTH_SHORT).show();
                    Log.d("FriendAdapter", "Friend request to " + user.getName() + " is removed.");
                    holder.addFriendIconImageView.setImageResource(R.drawable.user_plus);
                    holder.addFriendTextView.setText("Add");
                    holder.addFriendButtonLayout.setBackgroundResource(R.drawable.accept_button);
                    holder.addFriendTextView.setTextColor(Color.parseColor("#111111"));
                    holder.addFriendIconImageView.setColorFilter(Color.parseColor("#111111"));
                } else {
                    sendFriendRequest(user.getEmail());
                    outboundFriendRequests.put(user.getEmail(), user.getEmail());
                    Toast.makeText(holder.itemView.getContext(), "Friend request sent", Toast.LENGTH_SHORT).show();
                    Log.d("FriendAdapter", "Friend request sent to " + user.getName());
                    holder.addFriendIconImageView.setImageResource(R.drawable.user_minus);
                    holder.addFriendTextView.setText("Remove");
                    holder.addFriendButtonLayout.setBackgroundResource(R.drawable.decline_button);
                    holder.addFriendTextView.setTextColor(Color.parseColor("#ffffff"));
                    holder.addFriendIconImageView.setColorFilter(Color.parseColor("#ffffff"));
                }
            }
        });
    }

    private void sendFriendRequest(String toEmail) {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        fetchDocIdByEmail(currentUserEmail, new OnDocIdFetchedCallback() {
            @Override
            public void onSuccess(String docId) {
                handleOutboundRequest(docId, FirebaseFirestore.getInstance(), toEmail);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("FriendAdapter", "Failed to fetch doc ID.", e);
            }
        });
        db.collection("Users").whereEqualTo("email", toEmail).limit(1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot userDocument = task.getResult().getDocuments().get(0);
                        String userDocId = userDocument.getId();
                        handleInboundRequest(FirebaseAuth.getInstance().getCurrentUser().getEmail(), userDocId, db);
                    } else {
                        Log.d("FriendAdapter", "Error finding user with email: " + toEmail);
                    }
                });
    }

    private void handleOutboundRequest(String currentUserId, FirebaseFirestore db, String toEmail) {
        DocumentReference currentUserRef = db.collection("Users").document(currentUserId);
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("email", toEmail);
        currentUserRef.update("outboundFriendRequests", FieldValue.arrayUnion(requestMap))
                .addOnSuccessListener(aVoid -> Log.d("FriendAdapter", "Updated outbound friend request successfully."))
                .addOnFailureListener(e -> Log.d("FriendAdapter", "Error updating outbound friend request.", e));
    }

    private void handleInboundRequest(String currentUserEmail, String userDocId, FirebaseFirestore db) {
        DocumentReference selectedUserRef = db.collection("Users").document(userDocId);
        Map<String, String> inboundRequest = new HashMap<>();
        inboundRequest.put("email", currentUserEmail);
        selectedUserRef.update("incomingFriendRequests", FieldValue.arrayUnion(inboundRequest))
                .addOnSuccessListener(aVoid -> Log.d("FriendAdapter", "Updated incoming friend request successfully."))
                .addOnFailureListener(e -> Log.d("FriendAdapter", "Error updating incoming friend request.", e));
    }

    private void removeFriendRequest(String toEmail) {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        fetchDocIdByEmail(currentUserEmail, new OnDocIdFetchedCallback() {
            @Override
            public void onSuccess(String docId) {
                handleRemoveOutboundRequest(docId, db, toEmail);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("FriendAdapter", "Failed to fetch doc ID.", e);
            }
        });
        fetchDocIdByEmail(toEmail, new OnDocIdFetchedCallback() {
            @Override
            public void onSuccess(String docId) {
                handleRemoveInboundRequest(currentUserEmail, docId, db);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("FriendAdapter", "Failed to fetch doc ID.", e);
            }
        });
    }

    private void handleRemoveOutboundRequest(String currentUserId, FirebaseFirestore db, String toEmail) {
        DocumentReference currentUserRef = db.collection("Users").document(currentUserId);
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("email", toEmail);
        currentUserRef.update("outboundFriendRequests", FieldValue.arrayRemove(requestMap))
                .addOnSuccessListener(aVoid -> Log.d("FriendAdapter", "Removed outbound friend request successfully."))
                .addOnFailureListener(e -> Log.e("FriendAdapter", "Error removing outbound friend request.", e));
    }

    private void handleRemoveInboundRequest(String currentUserEmail, String userDocId, FirebaseFirestore db) {
        DocumentReference selectedUserRef = db.collection("Users").document(userDocId);
        Map<String, String> inboundRequest = new HashMap<>();
        inboundRequest.put("email", currentUserEmail);
        selectedUserRef.update("incomingFriendRequests", FieldValue.arrayRemove(inboundRequest))
                .addOnSuccessListener(aVoid -> Log.d("FriendAdapter", "Removed incoming friend request successfully."))
                .addOnFailureListener(e -> Log.e("FriendAdapter", "Error removing incoming friend request.", e));
    }

    private void fetchDocIdByEmail(String email, OnDocIdFetchedCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        callback.onSuccess(document.getId());
                    } else {
                        callback.onFailure(new Exception("User not found or error occurred."));
                    }
                });
    }

    interface OnDocIdFetchedCallback {
        void onSuccess(String docId);
        void onFailure(Exception e);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView friendImage;
        TextView friendName;
        TextView friendUsername;
        ConstraintLayout addFriendButtonLayout;
        TextView addFriendTextView;
        ImageView addFriendIconImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            friendImage = itemView.findViewById(R.id.friendImage);
            friendName = itemView.findViewById(R.id.friendName);
            friendUsername = itemView.findViewById(R.id.friendUsername);
            addFriendButtonLayout = itemView.findViewById(R.id.addFriendButton);
            addFriendTextView = itemView.findViewById(R.id.text);
            addFriendIconImageView = itemView.findViewById(R.id.icon);
        }
    }
}
