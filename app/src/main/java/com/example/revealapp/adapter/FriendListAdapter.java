package com.example.revealapp.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.revealapp.R;
import com.example.revealapp.model.User;
import com.example.revealapp.modules.GlideApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Map;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    private List<User> friendList;
    private Context context;

    public FriendListAdapter(List<User> friendList, Context context) {
        this.friendList = friendList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friendList.get(position);

        holder.friendName.setText(user.getName());
        holder.friendUsername.setText("@" + user.getUsername());

        String userProfilePhotoPath = user.getUserProfilePhoto();
        if (userProfilePhotoPath != null && !userProfilePhotoPath.isEmpty()) {
            StorageReference ref = FirebaseStorage.getInstance().getReference(userProfilePhotoPath);
            GlideApp.with(context).load(ref).into(holder.friendImage);
        } else {
            GlideApp.with(context).load(R.drawable.default_user_image).into(holder.friendImage);
        }

        holder.unfriendButton.setOnClickListener(v -> {

            String friendEmail = user.getEmail();
            Log.d("FriendListAdapter", "Attempting to unfriend user with email: " + friendEmail);


            String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            removeFromFriendList(currentUserEmail, friendEmail);
            removeFromFriendList(friendEmail, currentUserEmail);
            friendList.remove(position);
            notifyDataSetChanged();
            Toast.makeText(context, "Friend removed", Toast.LENGTH_SHORT).show();
        });
    }

    private void removeFromFriendList(String userEmailToRemoveFrom, String emailToRemove) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("FriendListAdapter", "Attempting to remove friend with email: " + emailToRemove + " from user's list: " + userEmailToRemoveFrom);

        db.collection("Users").whereEqualTo("email", userEmailToRemoveFrom)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        List<Map<String, String>> friends = (List<Map<String, String>>) documentSnapshot.get("friends");
                        if (friends != null) {
                            for (int i = 0; i < friends.size(); i++) {
                                if (friends.get(i).get("email").equals(emailToRemove)) {
                                    friends.remove(i);
                                    Log.d("FriendListAdapter", "Removing email from friends list: " + emailToRemove);
                                    break;
                                }
                            }
                            // Now update the friends list in Firestore using the document ID
                            db.collection("Users").document(documentSnapshot.getId()).update("friends", friends);
                            Log.d("FriendListAdapter", "Updated friends list for user: " + userEmailToRemoveFrom);
                        }
                    } else {
                        Log.d("FriendListAdapter", "No document found for user: " + userEmailToRemoveFrom);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendListAdapter", "Error fetching/updating document for user: " + userEmailToRemoveFrom, e);
                });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView friendImage;
        TextView friendName;
        TextView friendUsername;
        Button unfriendButton;

        public ViewHolder(View itemView) {
            super(itemView);
            friendImage = itemView.findViewById(R.id.friendImage);
            friendName = itemView.findViewById(R.id.friendName);
            friendUsername = itemView.findViewById(R.id.friendUsername);
            unfriendButton = itemView.findViewById(R.id.unfriendButton);
        }
    }
}
