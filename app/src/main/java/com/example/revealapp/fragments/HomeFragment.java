package com.example.revealapp.fragments;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import android.widget.Toast;
import android.util.Log;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.revealapp.adapter.PostAdapter;
import com.example.revealapp.model.Post;

import com.example.revealapp.R;

public class HomeFragment extends Fragment {


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private PostAdapter adapter;
    private ViewPager2 viewPager;

    private String currentUsername;
    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    String currentUserId = user.getUid();

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference userRef = db.collection("Users").document(currentUserId);

                    userRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUsername = documentSnapshot.getString("username"); // Assign the value here
                        } else {
                            Log.d(TAG, "User document does not exist");
                        }
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user document", e);
                    });
                } else {
                    // User is signed out
                    toastMessage("Successfully signed out.");
                }
            }
        };

        FirestoreManager firestoreManager = new FirestoreManager();

        // Call the method to retrieve the current theme and handle the result
        firestoreManager.getCurrentTheme(new CurrentThemeCallback() {
            @Override
            public void onCurrentThemeLoaded(DocumentReference currentThemeRef) {
                if (currentThemeRef != null) {
                    currentThemeRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot themeDocument = task.getResult();
                                //if current theme exist
                                if (themeDocument != null && themeDocument.exists()) {
                                    //get all posts filtered by current theme
                                    firestoreManager.getPosts(currentThemeRef).addOnCompleteListener(new OnCompleteListener<List<Post>>() {
                                        @Override
                                        public void onComplete(@NonNull Task<List<Post>> task) {
                                            if (task.isSuccessful()) {

                                                List<Post> posts = task.getResult();
                                                if (posts.size() > 0) {
                                                    adapter = new PostAdapter(posts);

                                                    // Set the adapter to your ViewPager
                                                    viewPager = view.findViewById(R.id.dashBoardViewPager);
                                                    viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
                                                    viewPager.setAdapter(adapter);
                                                } else {
                                                    redirectToBlankHomeFragment(view, themeDocument.getString("Title"));
                                                }

                                            } else {
                                                // Handle any errors of getting posts
                                                redirectToBlankHomeFragment(view, themeDocument.getString("Title"));
                                            }
                                        }
                                    });

                                    //if current theme does not exist
                                } else {
                                    redirectToBlankHomeFragment(view, null);
                                }
                            } else {
                                redirectToBlankHomeFragment(view, null);
                            }
                        }
                    });
                }else{
                    redirectToBlankHomeFragment(view, null);
                }
            }

            @Override
            public void onError(Exception e) {
                redirectToBlankHomeFragment(view, null);
            }
        });

        return view;
    }


    public void redirectToBlankHomeFragment(View view, String themeTitle) {

        FragmentManager fragmentManager = ((FragmentActivity) view.getContext()).getSupportFragmentManager();
        BlankHomeFragment blankHomeFragment = new BlankHomeFragment();
        Bundle args = new Bundle();

        args.putString("challengeName", themeTitle);
        blankHomeFragment.setArguments(args);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, blankHomeFragment)
                .addToBackStack(null)
                .commit();

    }


    public class FirestoreManager {
        private FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Retrieve posts from Firestore and return a Task<List<Post>>
        public Task<List<Post>> getPosts(DocumentReference currentThemeRef) {

            CollectionReference postsRef = db.collection("Posts");

            return postsRef.get().continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                List<Task<Post>> postTasks = new ArrayList<>();

                for (DocumentSnapshot document : task.getResult()) {

                    DocumentReference postThemeRef = null;
                    Object themeObject = document.get("Theme");

                    if (themeObject != null) {
                        if (themeObject instanceof DocumentReference) {
                            postThemeRef = (DocumentReference) themeObject;
                        }
                    }

                    if (postThemeRef == null || !postThemeRef.getPath().equals(currentThemeRef.getPath())) {
                        continue; // Skip this post because its theme is different from the current theme
                    }


                    Post p = new Post();

                    p.setTheme(postThemeRef);

                    String id = document.getId().toString();
                    p.setId(id);

                    String postTitle = document.getString("Title");
                    p.setPostTitle(postTitle);

                    String postDescription = document.getString("Description");
                    p.setPostDescription(postDescription);


                    DocumentReference userId = (DocumentReference) document.get("User");
                    p.setUserId(userId);

                    String pictureName = document.getString("Picture");
                    Log.d("HomeFragment", "Post Picture Name: " + pictureName);
                    p.setPicture(pictureName);


                    final Object numLikesObj = document.get("NumLikes");
                    final Object numDislikesObj = document.get("NumDislikes");

                    int numLikes = (numLikesObj instanceof Long) ? ((Long) numLikesObj).intValue() : 0;
                    int numDislikes = (numDislikesObj instanceof Long) ? ((Long) numDislikesObj).intValue() : 0;

                    p.setNumOfLike(numLikes);
                    p.setNumOfDislike(numDislikes);
                    

                    getUserProfilePathForUserId(userId).addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String userProfilePath) {
                            if (userProfilePath != null) {
                                Log.d("HomeFragment", "UserProfile FileName: " + userProfilePath);

                                p.setUserPhotoProfile(userProfilePath);
                            } else {
                                // The userProfilePath is null, which means the field is not found or an error occurred
                                p.setUserPhotoProfile(null);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            p.setUserPhotoProfile(null);
                        }
                    });


                    getThemeTitleForCurrentTheme(currentThemeRef).addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String themeTitle) {
                            if (themeTitle != null ) {
                                p.setThemeTitle(themeTitle);
                            } else {
                                p.setThemeTitle("default theme title");
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            p.setThemeTitle("default theme title");
                        }
                    });

                    Task<String> usernameTask = getUsernameForUserId(userId).continueWith(task1 -> {
                        if (task1.isSuccessful()) {
                            return task1.getResult();
                        } else {
                            throw task1.getException();
                        }
                    });
                    postTasks.add(usernameTask.continueWith(task3 -> {
                        if (task3.isSuccessful()) {
                            p.setUserName(task3.getResult());

                        }
                        return p;
                    }));

                }
                return Tasks.whenAllSuccess(postTasks);
            });
        }


        // Retrieve username from Firestore using userId
        private Task<String> getUsernameForUserId(DocumentReference userId) {

            final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

            userId.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String username = document.getString("username");
                            tcs.setResult(username);
                        } else {
                            tcs.setResult("User");
                        }
                    } else {
                        tcs.setResult("User");
                    }
                }
            });
            return tcs.getTask();
        }

        private Task<String> getThemeTitleForCurrentTheme(DocumentReference themeId){
            final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

            themeId.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String username = document.getString("Title");
                            tcs.setResult(username);
                        } else {
                            tcs.setResult("Default Title");
                        }
                    } else {
                        tcs.setResult("Default Title");
                    }
                }
            });
            return tcs.getTask();
        }



        private Task<String> getUserProfilePathForUserId(DocumentReference userId){

            final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

            userId.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userProfileFileName = document.getString("userProfilePhoto");
                            tcs.setResult(userProfileFileName);
                        } else {
                            tcs.setResult(null);
                        }
                    } else {
                        tcs.setResult(null);
                    }
                }
            });
            return tcs.getTask();
        }


        private void getCurrentTheme(final CurrentThemeCallback callback) {
            CollectionReference themesRef = db.collection("Themes");
            themesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            Date currentDate = new Date();
                            boolean foundMatchingDate = false;  // Flag to indicate if a matching date was found

                            for (QueryDocumentSnapshot document : querySnapshot) {
                                Date date = document.getDate("Date");

                                // Log for debugging
                                Log.d("Themes", "Comparing theme date: " + date + " with current date: " + currentDate);

                                // Extract year, month, and day components from the current date
                                Calendar currentCal = Calendar.getInstance();
                                currentCal.setTime(currentDate);
                                int currentYear = currentCal.get(Calendar.YEAR);
                                int currentMonth = currentCal.get(Calendar.MONTH);
                                int currentDay = currentCal.get(Calendar.DAY_OF_MONTH);

                                // Extract year, month, and day components from the date field to compare
                                Calendar compareCal = Calendar.getInstance();
                                compareCal.setTime(date);
                                int compareYear = compareCal.get(Calendar.YEAR);
                                int compareMonth = compareCal.get(Calendar.MONTH);
                                int compareDay = compareCal.get(Calendar.DAY_OF_MONTH);

                                // Check if it's the same date
                                if (currentYear == compareYear && currentMonth == compareMonth && currentDay == compareDay) {
                                    // Log for debugging
                                    Log.d("Themes", "Found matching theme for current date: " + date);
                                    Log.d("Themes", "Theme found: " + document.getReference());
                                    Log.d("Themes", "Theme ID found: " + document.getId());

                                    // Set the result and exit the loop
                                    callback.onCurrentThemeLoaded(document.getReference());
                                    foundMatchingDate = true;
                                    break;  // No need to continue iterating
                                }
                            }

                            // If no matching date was found, notify the callback with null
                            if (!foundMatchingDate) {
                                Log.w("Themes", "No theme found for current date.");
                                callback.onCurrentThemeLoaded(null);
                            }

                        } else {
                            Log.i("Themes", "querySnapshot is null.");
                        }
                    } else {
                        Log.e("Themes", "Error getting documents: " + task.getException());
                    }
                }
            });
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void toastMessage(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    interface CurrentThemeCallback {
        void onCurrentThemeLoaded(DocumentReference currentThemeRef);

        void onError(Exception e);
    }

}
