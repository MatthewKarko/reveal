package com.example.revealapp.model;

import com.google.firebase.firestore.DocumentReference;

public interface LikeInterface {
    DocumentReference getUserRef();
    DocumentReference getPostRef();
}
