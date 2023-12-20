package com.example.revealapp.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.revealapp.R;
import com.example.revealapp.model.User;
import com.example.revealapp.modules.GlideApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<User> requestList;

    public FriendRequestAdapter(List<User> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = requestList.get(position);

        holder.friendName.setText(user.getName());
        holder.friendUsername.setText("@" + user.getUsername());

        String userProfilePhotoPath = user.getUserProfilePhoto();
        if (userProfilePhotoPath != null && !userProfilePhotoPath.isEmpty()) {
            StorageReference ref = FirebaseStorage.getInstance().getReference(userProfilePhotoPath);
            GlideApp.with(holder.itemView.getContext()).load(ref).into(holder.friendImage);
        } else {
            GlideApp.with(holder.itemView.getContext()).load(R.drawable.default_user_image).into(holder.friendImage);
        }

        holder.declineFriendRequest.setOnClickListener(v -> {
            removeFriendRequest(user.getEmail());
            requestList.remove(position);
            notifyDataSetChanged();
            Toast.makeText(holder.itemView.getContext(), "Friend request declined", Toast.LENGTH_SHORT).show();
        });
        holder.addFriendRequest.setOnClickListener(v -> {
            acceptFriendRequest(user.getEmail());
            requestList.remove(position);
            notifyDataSetChanged();
            Toast.makeText(holder.itemView.getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
        });
    }
    private void acceptFriendRequest(String requesterEmail) {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        removeFriendRequest(requesterEmail);
        Map<String, String> friendToAddForCurrentUser = new HashMap<>();
        friendToAddForCurrentUser.put("email", requesterEmail);

        db.collection("Users").whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot currentUserDoc = querySnapshot.getDocuments().get(0);
                        Map<String, String> friendToAdd = new HashMap<>();
                        friendToAdd.put("email", requesterEmail);
                        currentUserDoc.getReference().update("friends", FieldValue.arrayUnion(friendToAdd))
                                .addOnSuccessListener(aVoid -> Log.d("FriendRequestAdapter", "Successfully added requester to current user's friend list"))
                                .addOnFailureListener(e -> Log.e("FriendRequestAdapter", "Error adding requester to current user's friend list", e));
                    }
                });

        db.collection("Users").whereEqualTo("email", requesterEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot requesterDoc = querySnapshot.getDocuments().get(0);
                        Map<String, String> friendToAdd = new HashMap<>();
                        friendToAdd.put("email", currentUserEmail);
                        requesterDoc.getReference().update("friends", FieldValue.arrayUnion(friendToAdd))
                                .addOnSuccessListener(aVoid -> Log.d("FriendRequestAdapter", "Successfully added current user to requester's friend list"))
                                .addOnFailureListener(e -> Log.e("FriendRequestAdapter", "Error adding current user to requester's friend list", e));
                    }
                });
    }
    private void removeFriendRequest(String requesterEmail) {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot currentUserDoc = querySnapshot.getDocuments().get(0);
                        List<Map<String, String>> incomingRequestsList = (List<Map<String, String>>) currentUserDoc.get("incomingFriendRequests");
                        if (incomingRequestsList != null) {
                            incomingRequestsList.removeIf(request -> request.get("email").equals(requesterEmail));
                            currentUserDoc.getReference().update("incomingFriendRequests", incomingRequestsList)
                                    .addOnSuccessListener(aVoid -> Log.d("FriendRequestAdapter", "Removed incoming friend request successfully for user: " + currentUserEmail))
                                    .addOnFailureListener(e -> Log.e("FriendRequestAdapter", "Error removing incoming friend request for user: " + currentUserEmail, e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FriendRequestAdapter", "Error fetching document for current user email: " + currentUserEmail, e));

        db.collection("Users").whereEqualTo("email", requesterEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot requesterDoc = querySnapshot.getDocuments().get(0);
                        List<Map<String, String>> outboundRequestsList = (List<Map<String, String>>) requesterDoc.get("outboundFriendRequests");
                        if (outboundRequestsList != null) {
                            outboundRequestsList.removeIf(request -> request.get("email").equals(currentUserEmail));
                            requesterDoc.getReference().update("outboundFriendRequests", outboundRequestsList)
                                    .addOnSuccessListener(aVoid -> Log.d("FriendRequestAdapter", "Removed outbound friend request successfully for user: " + requesterEmail))
                                    .addOnFailureListener(e -> Log.e("FriendRequestAdapter", "Error removing outbound friend request for user: " + requesterEmail, e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FriendRequestAdapter", "Error fetching document for requester email: " + requesterEmail, e));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView friendImage;          // This remains unchanged
        TextView friendName;
        TextView friendUsername;
        ImageButton declineFriendRequest;   // Changed ImageView to Button
        ImageButton addFriendRequest;       // Changed ImageView to Button

        public ViewHolder(View itemView) {
            super(itemView);
            friendImage = itemView.findViewById(R.id.friendImage);
            friendName = itemView.findViewById(R.id.friendName);
            friendUsername = itemView.findViewById(R.id.friendUsername);
            declineFriendRequest = itemView.findViewById(R.id.declineFriendRequest); // Make sure the id matches the Button id in the XML
            addFriendRequest = itemView.findViewById(R.id.addFriendRequest);       // Make sure the id matches the Button id in the XML
        }
    }

}
