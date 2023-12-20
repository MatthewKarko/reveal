package com.example.revealapp.adapter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.revealapp.fragments.ProfileFragment;
import com.example.revealapp.modules.GlideApp;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Objects;

import com.example.revealapp.R;
import com.example.revealapp.model.Dislike;
import com.example.revealapp.model.Like;
import com.example.revealapp.model.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder>{

    private List<Post> postList;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.post_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Post post = postList.get(position);

        // Bind data to the views in your ViewHolder
        holder.themeTitle.setText(post.getThemeTitle());
        holder.likeCount.setText(String.valueOf(post.getNumOfLike()));
        holder.dislikeCount.setText(String.valueOf(post.getNumOfDislike()));
        holder.userName.setText(post.getUserName());
        holder.postTitle.setText(post.getPostTitle());
        holder.postDescription.setText(post.getPostDescription());

        holder.thumbUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    FirebaseUser currentUser = auth.getCurrentUser();

                    if (currentUser == null) {
                        Toast.makeText(v.getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String currentUserId = currentUser.getUid();
                    Post clickedPost = postList.get(adapterPosition);

                    if (clickedPost.getId() == null) {
                        return;
                    }

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference postRef = db.collection("Posts").document(clickedPost.getId());
                    int currentLikeCount = clickedPost.getNumOfLike();

                    CollectionReference likesCollection = FirebaseFirestore.getInstance().collection("Likes");
                    CollectionReference dislikesCollection = FirebaseFirestore.getInstance().collection("Dislikes");
                    DocumentReference currentUserRef = db.document("Users/" + currentUserId);

                    Query checkDislikeQuery = dislikesCollection.whereEqualTo("userRef", currentUserRef).whereEqualTo("postRef", postRef);

                    checkDislikeQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot dislikeDocument = task.getResult().getDocuments().get(0);
                                dislikeDocument.getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        clickedPost.setNumOfDislike(clickedPost.getNumOfDislike() - 1);
                                        notifyItemChanged(adapterPosition);
                                        postRef.update("NumDislikes", clickedPost.getNumOfDislike());
                                    }
                                });
                            }

                            Query likeQuery = likesCollection.whereEqualTo("userRef", currentUserRef).whereEqualTo("postRef", postRef);

                            likeQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        if (!task.getResult().isEmpty()) {
                                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                            documentSnapshot.getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    clickedPost.setNumOfLike(currentLikeCount-1);
                                                    notifyItemChanged(adapterPosition);
                                                    postRef.update("NumLikes", clickedPost.getNumOfLike())
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    Toast.makeText(v.getContext(), "You removed your like", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Toast.makeText(v.getContext(), "Failed to update like count", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }
                                            });
                                        } else {
                                            clickedPost.setNumOfLike(currentLikeCount+1);
                                            notifyItemChanged(adapterPosition);
                                            postRef.update("NumLikes", clickedPost.getNumOfLike())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Like like = new Like(currentUserRef, postRef);
                                                            db.collection("Likes").add(like)
                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                        @Override
                                                                        public void onSuccess(DocumentReference documentReference) {
                                                                            Toast.makeText(v.getContext(), "You liked this post", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    })
                                                                    .addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception e) {
                                                                            Toast.makeText(v.getContext(), "Failed to add like: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(v.getContext(), "Failed to update like count", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(v.getContext(), "Error checking likes", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });

        holder.thumbDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    FirebaseUser currentUser = auth.getCurrentUser();

                    if (currentUser == null) {
                        Toast.makeText(v.getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String currentUserId = currentUser.getUid();
                    Post clickedPost = postList.get(adapterPosition);

                    if (clickedPost.getId() == null) {
                        return;
                    }

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference postRef = db.collection("Posts").document(clickedPost.getId());
                    int currentDislikeCount = clickedPost.getNumOfDislike();

                    CollectionReference dislikesCollection = FirebaseFirestore.getInstance().collection("Dislikes");
                    CollectionReference likesCollection = FirebaseFirestore.getInstance().collection("Likes");
                    DocumentReference currentUserRef = db.document("Users/" + currentUserId);

                    Query checkLikeQuery = likesCollection.whereEqualTo("userRef", currentUserRef).whereEqualTo("postRef", postRef);

                    checkLikeQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot likeDocument = task.getResult().getDocuments().get(0);
                                likeDocument.getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        clickedPost.setNumOfLike(clickedPost.getNumOfLike() - 1);
                                        notifyItemChanged(adapterPosition);
                                        postRef.update("NumLikes", clickedPost.getNumOfLike());
                                    }
                                });
                            }

                            Query dislikeQuery = dislikesCollection.whereEqualTo("userRef", currentUserRef).whereEqualTo("postRef", postRef);

                            dislikeQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        if (!task.getResult().isEmpty()) {
                                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                                            documentSnapshot.getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    clickedPost.setNumOfDislike(currentDislikeCount-1);
                                                    notifyItemChanged(adapterPosition);
                                                    postRef.update("NumDislikes", clickedPost.getNumOfDislike())
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    Toast.makeText(v.getContext(), "You removed your dislike", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    Toast.makeText(v.getContext(), "Failed to update dislike count", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }
                                            });
                                        } else {
                                            clickedPost.setNumOfDislike(currentDislikeCount+1);
                                            notifyItemChanged(adapterPosition);
                                            postRef.update("NumDislikes", clickedPost.getNumOfDislike())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Dislike dislike = new Dislike(currentUserRef, postRef);
                                                            db.collection("Dislikes").add(dislike)
                                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                        @Override
                                                                        public void onSuccess(DocumentReference documentReference) {
                                                                            Toast.makeText(v.getContext(), "You disliked this post", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    })
                                                                    .addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception e) {
                                                                            Toast.makeText(v.getContext(), "Failed to add dislike: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(v.getContext(), "Failed to update dislike count", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(v.getContext(), "Error checking dislikes", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });


        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        if (post.getPicture() != null && !Objects.equals(post.getPicture(), "")){
            StorageReference picReference= storageReference.child(post.getPicture());

            GlideApp.with(holder.itemView.getContext())
                    .load(picReference)
                    .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image
                    .error(R.drawable.default_homepage_image) // Error image if loading fails
                    .into(holder.picture);
        }else{
            GlideApp.with(holder.itemView.getContext())
                    .load(R.drawable.default_homepage_image)
                    .error(R.drawable.default_homepage_image) // Error image if loading fails
                    .into(holder.picture);
        }


        if (post.getUserPhotoProfile() != null&& !Objects.equals(post.getPicture(), "")){
            StorageReference profileReference = storageReference.child(post.getUserPhotoProfile());

            GlideApp.with(holder.itemView.getContext())
                    .load(profileReference)
                    .placeholder(R.drawable.ic_launcher_foreground) // Placeholder image
                    .error(R.drawable.default_user_image) // Error image if loading fails
                    .into(holder.userProfileImage);

        }else{
            GlideApp.with(holder.itemView.getContext())
                    .load(R.drawable.default_user_image)
                    .error(R.drawable.default_user_image) // Error image if loading fails
                    .into(holder.userProfileImage);
        }





        /*
        Log.i(photoReference.toString(),"path: " + photoReference.toString());

        final long ONE_MEGABYTE = 1024 * 1024;
        photoReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.picture.setImageBitmap(bmp);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        });


         */

/*

        final long MAX_IMAGE_SIZE_BYTES = 2048 * 2048; // Set your preferred max image size
        picReference.getBytes(MAX_IMAGE_SIZE_BYTES).addOnSuccessListener(new OnSuccessListener<byte[]>(){
            @Override
            public void onSuccess(byte[] bytes) {
                // Successfully downloaded the image as a byte array
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                // Now, load the bitmap into your ImageView using Glide
                Glide.with(holder.itemView.getContext())
                        .load(bitmap)
                        .into(holder.picture);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle the failure to download the image

                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.default_homepage_image)
                        .into(holder.picture);
            }
        });


        final long MAX_IMAGE_SIZE_BYTES_PROFILE = 2048 * 2048; // Set your preferred max image size


        if (post.getUserPhotoProfile() != null) {

            profileReference.getBytes(MAX_IMAGE_SIZE_BYTES_PROFILE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Successfully downloaded the image as a byte array
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    // Now, load the bitmap into your ImageView using Glide
                    Glide.with(holder.itemView.getContext())
                            .load(bitmap)
                            .into(holder.userProfileImage);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle the failure to download the image
                    Log.e("FirebaseStorage", "Failed to download image: " + exception.getMessage());

                    // Load a default image here
                    Glide.with(holder.itemView.getContext())
                            .load(R.drawable.default_user_image) // Replace with your default image resource
                            .into(holder.userProfileImage);
                }
            });
        } else {
            // Load a default image because profileImageRef is null
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.default_user_image) // Replace with your default image resource
                    .into(holder.userProfileImage);
        }

 */

    }


    @Override
    public int getItemCount() {
        return postList.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView picture;
        ImageView userProfileImage;
        TextView userName;
        TextView likeCount;
        TextView dislikeCount;
        TextView themeTitle;
        ImageButton thumbUpBtn;
        ImageButton thumbDownBtn;

        TextView postTitle;
        TextView postDescription;


        public ViewHolder(View itemView) {
            super(itemView);
            // Initialize your views by finding them in the itemView
            picture = itemView.findViewById(R.id.uploadedImage);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            userName = itemView.findViewById(R.id.uploaderName);
            likeCount = itemView.findViewById(R.id.likeCount);
            dislikeCount = itemView.findViewById(R.id.dislikeCount);
            themeTitle = itemView.findViewById(R.id.challengeName);
            thumbUpBtn = itemView.findViewById(R.id.thumbUpBtn);
            thumbDownBtn = itemView.findViewById(R.id.thumbDownBtn);
            postTitle = itemView.findViewById(R.id.postTitle);
            postDescription = itemView.findViewById(R.id.postDescription);

            userName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Get the username for the clicked position
                        String userId = postList.get(position).getUserId().getId();

                        // Start the ProfileFragment with the username as an argument
                        FragmentManager fragmentManager = ((FragmentActivity) v.getContext()).getSupportFragmentManager();
                        ProfileFragment profileFragment = new ProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("userId", userId);
                        profileFragment.setArguments(args);

                        fragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, profileFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            });

            userProfileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Get the username for the clicked position
                        String userId = postList.get(position).getUserId().getId();

                        // Start the ProfileFragment with the username as an argument
                        FragmentManager fragmentManager = ((FragmentActivity) v.getContext()).getSupportFragmentManager();
                        ProfileFragment profileFragment = new ProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("userId", userId);
                        profileFragment.setArguments(args);

                        fragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, profileFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            });
        }
    }
}
