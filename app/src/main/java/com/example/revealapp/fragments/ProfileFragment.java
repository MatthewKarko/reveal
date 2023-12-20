package com.example.revealapp.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.revealapp.R;
import com.example.revealapp.adapter.ProfilePostAdapter;
import com.example.revealapp.model.Post;
import com.example.revealapp.modules.GlideApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProfilePostAdapter profilePostAdapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private RecyclerView recyclerView;
    private TextView usernameText;
    private TextView challengesText;
    private TextView likesText;
    private TextView friendsText;
    private TextView nameText;
    private TextView descriptionText;
    private Button editProfileButton;
    private View alertDialogView;
    private ImageView profileImage;

    private String userId;
    private boolean loggedUser;

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private List<InputStream> imageStreams;

    public ProfileFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageStreams = new ArrayList<>();
        pickMedia = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    imageStreams.clear();
                    if (uri != null) {
                        try {
                            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                            imageStreams.add(inputStream);

                            TextView filenameText = alertDialogView.findViewById(R.id.filenameText);
                            File file = new File(uri.getPath());
                            filenameText.setText(file.getName());
                        } catch (FileNotFoundException e) {
                            Toast.makeText(getActivity(), "Failed to locate image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Bundle args = getArguments();
        if (args != null) {
            userId = args.getString("userId");
        }

        if (userId == null) {
            userId = mAuth.getCurrentUser().getUid();
            loggedUser = true;
        }
        else {
            loggedUser = false;
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        usernameText = view.findViewById(R.id.usernameText);
        challengesText = view.findViewById(R.id.challengesText);
        likesText = view.findViewById(R.id.likesText);
        friendsText = view.findViewById(R.id.friendsText);
        nameText = view.findViewById(R.id.nameText);
        descriptionText = view.findViewById(R.id.descriptionText);
        profileImage = view.findViewById(R.id.userProfileImage);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        if (loggedUser) {
            editProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogView = inflater.inflate(R.layout.fragment_edit_profile, null);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.setView(alertDialogView);

                    setupDialogListeners(alertDialog, alertDialogView);

                    alertDialog.show();
                }
            });
        }
        else {
            editProfileButton.setText("Add friend");
            setupAddFriendButtonState();
            editProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupAddFriendListener();
                }
            });
        }

        loadUserDetails();
        loadPosts();
        return view;
    }

    private void loadUserDetails() {
//        FirebaseUser user = mAuth.getCurrentUser();
        db.collection("Users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    String username = "@" + documentSnapshot.getString("username");
                    usernameText.setText(username);

                    nameText.setText(documentSnapshot.getString("name"));

                    String bio = documentSnapshot.getString("description");
                    if (bio != null) {
                        descriptionText.setText(bio);
                    }

                    Object friendsObj = documentSnapshot.get("friends");
                    if (friendsObj != null) {
                        List<Object> friends = (List<Object>) friendsObj;
                        friendsText.setText(String.valueOf(friends.size()));
                    }

                    String pfp = documentSnapshot.getString("userProfilePhoto");
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                    if (pfp != null && !Objects.equals(pfp, "")){
                        StorageReference picReference = storageReference.child(pfp);

                        GlideApp.with(this)
                                .load(picReference)
                                .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image
                                .error(R.drawable.default_user_image) // Error image if loading fails
                                .into(profileImage);
                    }
                    else {
                        GlideApp.with(this)
                                .load(R.drawable.default_user_image)
                                .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image
                                .into(profileImage);
                    }

                });
    }

    private void loadPosts() {
//        FirebaseUser user = mAuth.getCurrentUser();
        List<Post> posts = new ArrayList<>();

        CollectionReference postsRef = db.collection("Posts");
        postsRef.get()
                .addOnCompleteListener(task -> {
                    QuerySnapshot querySnapshot = task.getResult();
                    int likes = 0;

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        DocumentReference documentReference = document.getDocumentReference("User");
                        if (documentReference.getId().equals(userId)) {
                            Post post = document.toObject(Post.class);
                            likes += post.getNumOfLike();
                            Log.i("Likes", String.valueOf(post.getNumOfLike()));
                            posts.add(post);
                        }
                    }
//                    Log.i("POSTS SIZE", String.valueOf(posts.size()));

                    profilePostAdapter = new ProfilePostAdapter(posts);
                    recyclerView.setAdapter(profilePostAdapter);

                    challengesText.setText(String.valueOf(posts.size()));
                    likesText.setText(String.valueOf(likes));
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), "No posts found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupDialogListeners(AlertDialog alertDialog, View alertDialogView) {
        Button uploadButton = alertDialogView.findViewById(R.id.uploadButton);
        Button cancelButton = alertDialogView.findViewById(R.id.cancelButton);
        Button saveButton = alertDialogView.findViewById(R.id.saveButton);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // PickVisualMedia bug
                ActivityResultContracts.PickVisualMedia.VisualMediaType mediaType;
                mediaType = (ActivityResultContracts.PickVisualMedia.VisualMediaType) ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE;
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(mediaType)
                        .build());
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> userMap = new HashMap<>();
                UploadTask uploadTask = null;
                if (!imageStreams.isEmpty()) {
                    String imageName = "pfp/" + UUID.randomUUID().toString() + ".jpg";
                    StorageReference storageReference = storage.getReference().child(imageName);
                    uploadTask = storageReference.putStream(imageStreams.get(0));
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            userMap.put("userProfilePhoto", imageName);
                            uploadDetails(userMap, alertDialog);
                        }
                    });
                }
                else {
                    uploadDetails(userMap, alertDialog);
                }
            }
        });
    }

    private void uploadDetails(Map<String, Object> userMap, AlertDialog alertDialog) {
        EditText nameTextBox = alertDialogView.findViewById(R.id.nameTextBox);
        EditText bioTextBox = alertDialogView.findViewById(R.id.bioEditText);

        String nameText = nameTextBox.getText().toString().trim();
        String bioText = bioTextBox.getText().toString().trim();

        if (!nameText.isEmpty()) {
            userMap.put("name", nameText);
        }
        if (!bioText.isEmpty()) {
            userMap.put("description", bioText);
        }

        if (!userMap.isEmpty()) {
//            FirebaseUser user = mAuth.getCurrentUser();
            db.collection("Users")
                    .document(userId)
                    .set(userMap, SetOptions.merge())
                    .addOnSuccessListener(task -> {
                        Toast.makeText(getActivity(), "Profile updated", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    })
                    .addOnFailureListener(task -> {
                        Toast.makeText(getActivity(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    });
        }
        else {
            Toast.makeText(getActivity(), "At least one field must be filled", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAddFriendButtonState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot documentSnapshot = task.getResult();

                    db.collection("Users")
                            .document(userId)
                            .get()
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    DocumentSnapshot viewingUserSnapshot = task1.getResult();

                                    List<Map<String, String>> friends = (List<Map<String, String>>) documentSnapshot.get("friends");
                                    if (friends == null) {
                                        friends = new ArrayList<>();
                                    }

                                    List<Map<String, String>> friendRequests = (List<Map<String, String>>) documentSnapshot.get("incomingFriendRequests");
                                    if (friendRequests != null) {
                                        friends.addAll(friendRequests);
                                    }
                                    friendRequests = (List<Map<String, String>>) documentSnapshot.get("outboundFriendRequests");
                                    if (friendRequests != null) {
                                        friends.addAll(friendRequests);
                                    }

                                    for (Map<String, String> friend : friends) {
                                        if (friend.get("email").equals(viewingUserSnapshot.getString("email"))) {
                                            editProfileButton.setEnabled(false);
                                            break;
                                        }
                                    }
                                }
                            });
                });
    }

    private void setupAddFriendListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        db.collection("Users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot viewingUserDocument = task.getResult();
                        String toEmail = viewingUserDocument.getString("email");
                        Map<String, String> requestMap = new HashMap<>();
                        requestMap.put("email", toEmail);
                        db.collection("Users")
                                .document(currentUser.getUid())
                                .update("outboundFriendRequests", FieldValue.arrayUnion(requestMap))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FriendAdapter", "Updated outbound friend request successfully.");
                                    editProfileButton.setEnabled(false);
                                })
                                .addOnFailureListener(e -> Log.d("FriendAdapter", "Error updating outbound friend request.", e));
                    }
                });

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("email", currentUser.getEmail());
        db.collection("Users")
                .document(userId)
                .update("incomingFriendRequests", FieldValue.arrayUnion(requestMap))
                .addOnSuccessListener(aVoid -> {
                    Log.d("FriendAdapter", "Updated incoming friend request successfully.");
                    editProfileButton.setEnabled(false);
                })
                .addOnFailureListener(e -> Log.d("FriendAdapter", "Error updating outbound friend request.", e));
    }

}
